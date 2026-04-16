package payment.payment.service.controller;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import payment.payment.service.domain.dto.CreatePaymentRequest;
import payment.payment.service.domain.dto.PaymentResponse;
import payment.payment.service.service.PaymentService;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @RateLimiter(name = "paymentCreation")
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("Authorization") String authorizationHeader,
            Authentication authentication
    ) {
        // Validate user ownership - prevent IDOR vulnerability
        String authenticatedUserId = authentication.getName();
        if (!authenticatedUserId.equals(request.userId().toString())) {
            log.warn("Authorization failed: User {} attempted to create payment for user {}",
                    authenticatedUserId, request.userId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Payment request received for userId: {}", request.userId());

        PaymentResponse response = paymentService.createPayment(request, authorizationHeader);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

