package payment.payment.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.payment.service.client.AccountServiceClient;
import payment.payment.service.domain.dto.CreatePaymentRequest;
import payment.payment.service.domain.dto.PaymentResponse;
import payment.payment.service.domain.entity.OutboxEvent;
import payment.payment.service.domain.entity.Payment;
import payment.payment.service.domain.enums.PaymentStatus;
import payment.payment.service.domain.mapper.PaymentMapper;
import payment.payment.service.exception.AccessDeniedException;
import payment.payment.service.exception.AccountServiceException;
import payment.payment.service.exception.DuplicatePaymentException;
import payment.payment.service.exception.PaymentProcessingException;
import payment.payment.service.repository.OutboxEventRepository;
import payment.payment.service.repository.PaymentRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountServiceClient accountServiceClient;
    private final ObjectMapper objectMapper;
    private final PaymentMapper paymentMapper;

    private static final String PAYMENT_CREATED_EVENT = "PaymentCreated";
    private static final String PAYMENT_FAILED_EVENT = "PaymentFailed";
    private static final String PAYMENT_SUCCEEDED_EVENT = "PaymentSucceeded";

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String authorizationHeader, String authenticatedUserId) {
        log.info("Creating payment with idempotencyKey: {}", request.idempotencyKey());

        // Validate user authorization
        if (!authenticatedUserId.equals(request.userId().toString())) {
            log.warn("Authorization failed: User {} attempted to create payment for user {}",
                    authenticatedUserId, request.userId());
            throw new AccessDeniedException("You are not authorized to create payments for this user");
        }

        // Check idempotency
        var existingPayment = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingPayment.isPresent()) {
            log.warn("Duplicate payment attempt with idempotencyKey");
            throw new DuplicatePaymentException("Payment with this idempotency key already exists");
        }

        // Create payment with PENDING status
        Payment payment = paymentMapper.toDomainEntity(request);
        payment = paymentRepository.save(payment);
        log.info("Payment created with id: {} and status: PENDING", payment.getId());

        PaymentStatus finalStatus;

        // Try to withdraw from account service
        try {
            accountServiceClient.withdraw(request.userId(), request.amount(), authorizationHeader);
            finalStatus = PaymentStatus.SUCCESS;
            log.info("Payment successful for payment id: {}", payment.getId());
        } catch (AccountServiceException ex) {
            finalStatus = PaymentStatus.FAILED;
            log.error("Payment failed for payment id: {} - {}", payment.getId(), ex.getMessage(), ex);
        } catch (Exception ex) {
            finalStatus = PaymentStatus.FAILED;
            log.error("Unexpected error during payment processing for payment id: {} - {}", payment.getId(), ex.getMessage(), ex);
        }

        // Update payment status
        payment.setStatus(finalStatus);
        payment = paymentRepository.save(payment);
        log.info("Payment updated with status: {}", finalStatus);

        // Create outbox event
        createOutboxEvent(payment, finalStatus);

        return paymentMapper.toResponse(payment);
    }

    private void createOutboxEvent(Payment payment, PaymentStatus finalStatus) {
        try {
            var paymentEvent = paymentMapper.toEvent(payment);
            String payload = objectMapper.writeValueAsString(paymentEvent);

            String eventType = determineEventType(finalStatus);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(payment.getId())
                    .eventType(eventType)
                    .payload(payload)
                    .published(false)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event created for payment id: {} with event type: {}", payment.getId(), eventType);
        } catch (Exception ex) {
            log.error("Error creating outbox event for payment id: {} - {}", payment.getId(), ex.getMessage(), ex);
            throw new PaymentProcessingException("Error creating outbox event");
        }
    }

    private String determineEventType(PaymentStatus status) {
        return switch (status) {
            case SUCCESS -> PAYMENT_SUCCEEDED_EVENT;
            case FAILED -> PAYMENT_FAILED_EVENT;
            default -> PAYMENT_CREATED_EVENT;
        };
    }
}

