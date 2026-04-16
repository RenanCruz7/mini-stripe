package payment.payment.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.payment.service.client.AccountServiceClient;
import payment.payment.service.domain.dto.CreatePaymentRequest;
import payment.payment.service.domain.dto.PaymentEvent;
import payment.payment.service.domain.dto.PaymentResponse;
import payment.payment.service.domain.entity.OutboxEvent;
import payment.payment.service.domain.entity.Payment;
import payment.payment.service.domain.enums.PaymentStatus;
import payment.payment.service.exception.AccountServiceException;
import payment.payment.service.exception.DuplicatePaymentException;
import payment.payment.service.exception.PaymentProcessingException;
import payment.payment.service.repository.OutboxEventRepository;
import payment.payment.service.repository.PaymentRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AccountServiceClient accountServiceClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String authorizationHeader) {
        log.info("Creating payment for userId: {} with idempotencyKey: {}", request.userId(), request.idempotencyKey());

        // Check idempotency
        var existingPayment = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingPayment.isPresent()) {
            log.warn("Duplicate payment attempt with idempotencyKey: {}", request.idempotencyKey());
            throw new DuplicatePaymentException("Payment with this idempotency key already exists");
        }

        // Create payment with PENDING status
        Payment payment = Payment.builder()
                .userId(request.userId())
                .amount(request.amount())
                .idempotencyKey(request.idempotencyKey())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment created with id: {} and status: PENDING", payment.getId());

        PaymentStatus finalStatus = PaymentStatus.PENDING;

        // Try to withdraw from account service
        try {
            accountServiceClient.withdraw(request.userId(), request.amount(), authorizationHeader);
            finalStatus = PaymentStatus.SUCCESS;
            log.info("Payment successful for payment id: {}", payment.getId());
        } catch (AccountServiceException ex) {
            finalStatus = PaymentStatus.FAILED;
            log.error("Payment failed for payment id: {} - {}", payment.getId(), ex.getMessage());
        } catch (Exception ex) {
            finalStatus = PaymentStatus.FAILED;
            log.error("Payment failed for payment id: {} - {}", payment.getId(), ex.getMessage());
        }

        // Update payment status
        payment.setStatus(finalStatus);
        payment = paymentRepository.save(payment);
        log.info("Payment updated with status: {}", finalStatus);

        // Create outbox event
        createOutboxEvent(payment);

        return mapToResponse(payment);
    }

    private void createOutboxEvent(Payment payment) {
        try {
            PaymentEvent paymentEvent = new PaymentEvent(
                    payment.getId(),
                    payment.getUserId(),
                    payment.getAmount(),
                    payment.getStatus(),
                    payment.getCreatedAt()
            );

            String payload = objectMapper.writeValueAsString(paymentEvent);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(payment.getId())
                    .eventType("PaymentCreated")
                    .payload(payload)
                    .published(false)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event created for payment id: {}", payment.getId());
        } catch (Exception ex) {
            log.error("Error creating outbox event for payment id: {} - {}", payment.getId(), ex.getMessage());
            throw new PaymentProcessingException("Error creating outbox event: " + ex.getMessage());
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getIdempotencyKey(),
                payment.getCreatedAt()
        );
    }
}

