package payment.payment.service.domain.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import payment.payment.service.domain.dto.CreatePaymentRequest;
import payment.payment.service.domain.dto.PaymentEvent;
import payment.payment.service.domain.dto.PaymentResponse;
import payment.payment.service.domain.entity.Payment;
import payment.payment.service.domain.enums.PaymentStatus;

@Component
@RequiredArgsConstructor
public class PaymentMapper {

    public Payment toDomainEntity(CreatePaymentRequest request) {
        return Payment.builder()
                .userId(request.userId())
                .amount(request.amount())
                .idempotencyKey(request.idempotencyKey())
                .status(PaymentStatus.PENDING)
                .build();
    }

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getIdempotencyKey(),
                payment.getCreatedAt()
        );
    }

    public PaymentEvent toEvent(Payment payment) {
        return new PaymentEvent(
                payment.getId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}

