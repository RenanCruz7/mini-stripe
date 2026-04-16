package payment.payment.service.controller;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import payment.payment.service.domain.dto.CreatePaymentRequest;
import payment.payment.service.domain.dto.PaymentResponse;
import payment.payment.service.exception.AccessDeniedException;
import payment.payment.service.service.JwtService;
import payment.payment.service.service.PaymentService;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtService jwtService;

    @RateLimiter(name = "paymentCreation")
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String authenticatedUserId = extractUserIdFromToken(authHeader);
        log.info("Payment request received for userId: {} by authenticated user: {}", request.userId(), authenticatedUserId);

        PaymentResponse response = paymentService.createPayment(request, authHeader, authenticatedUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            return jwtService.extractUsername(token);
        } catch (Exception e) {
            log.error("Failed to extract user ID from token: {}", e.getMessage());
            throw new AccessDeniedException("Invalid token");
        }
    }
}



