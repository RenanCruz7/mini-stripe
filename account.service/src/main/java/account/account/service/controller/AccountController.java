package account.account.service.controller;

import account.account.service.domain.dto.AccountResponse;
import account.account.service.domain.dto.CreateAccountRequest;
import account.account.service.domain.dto.DepositRequest;
import account.account.service.domain.dto.WithdrawRequest;
import account.account.service.exception.AccessDeniedException;
import account.account.service.service.AccountService;
import account.account.service.service.JwtService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;
    private final JwtService jwtService;

    @RateLimiter(name = "accountCreation")
    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @Valid @RequestBody CreateAccountRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String authenticatedUserId = extractUserIdFromToken(authHeader);
        log.info("Account creation request for userId: {} by authenticated user: {}", request.userId(), authenticatedUserId);

        validateOwnership(request.userId(), authenticatedUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAccount(request.userId()));
    }

    @RateLimiter(name = "accountOperations")
    @GetMapping("/{userId}")
    public ResponseEntity<AccountResponse> getByUserId(
            @PathVariable UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String authenticatedUserId = extractUserIdFromToken(authHeader);

        validateOwnership(userId, authenticatedUserId);

        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @RateLimiter(name = "accountOperations")
    @PostMapping("/{userId}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable UUID userId,
            @Valid @RequestBody DepositRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String authenticatedUserId = extractUserIdFromToken(authHeader);

        validateOwnership(userId, authenticatedUserId);

        return ResponseEntity.ok(service.deposit(userId, request.amount()));
    }

    @RateLimiter(name = "accountOperations")
    @PostMapping("/{userId}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            @PathVariable UUID userId,
            @Valid @RequestBody WithdrawRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String authenticatedUserId = extractUserIdFromToken(authHeader);

        validateOwnership(userId, authenticatedUserId);

        return ResponseEntity.ok(service.withdraw(userId, request.amount()));
    }

    @RateLimiter(name = "accountOperations")
    @PatchMapping("/{userId}/block")
    public ResponseEntity<AccountResponse> block(
            @PathVariable UUID userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String authenticatedUserId = extractUserIdFromToken(authHeader);

        validateOwnership(userId, authenticatedUserId);

        return ResponseEntity.ok(service.blockAccount(userId));
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

    private void validateOwnership(UUID userId, String authenticatedUserId) {
        if (!userId.toString().equals(authenticatedUserId)) {
            log.warn("Authorization failed: User {} attempted to access account of user {}", authenticatedUserId, userId);
            throw new AccessDeniedException("You are not authorized to perform operations on this account");
        }
    }
}