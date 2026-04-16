package account.account.service.controller;

import account.account.service.domain.dto.*;
import account.account.service.exception.AccessDeniedException;
import account.account.service.service.AccountService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @RateLimiter(name = "accountCreation")
    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request, Authentication authentication) {
        String authenticatedUserId = authentication.getName();
        log.info("Account creation request for userId: {} by authenticated user: {}", request.userId(), authenticatedUserId);

        if (!request.userId().toString().equals(authenticatedUserId)) {
            throw new AccessDeniedException("Usuário não autorizado para criar conta de outro usuário");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAccount(request.userId()));
    }

    @RateLimiter(name = "accountOperations")
    @GetMapping("/{userId}")
    public ResponseEntity<AccountResponse> getByUserId(@PathVariable UUID userId, Authentication authentication) {
        String authenticatedUserId = authentication.getName();

        if (!userId.toString().equals(authenticatedUserId)) {
            throw new AccessDeniedException("Usuário não autorizado para acessar esta conta");
        }

        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @RateLimiter(name = "accountOperations")
    @PostMapping("/{userId}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable UUID userId,
            @Valid @RequestBody DepositRequest request,
            Authentication authentication
    ) {
        String authenticatedUserId = authentication.getName();

        if (!userId.toString().equals(authenticatedUserId)) {
            throw new AccessDeniedException("Usuário não autorizado para realizar operações nesta conta");
        }

        return ResponseEntity.ok(service.deposit(userId, request.amount()));
    }

    @RateLimiter(name = "accountOperations")
    @PostMapping("/{userId}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            @PathVariable UUID userId,
            @Valid @RequestBody WithdrawRequest request,
            Authentication authentication
    ) {
        String authenticatedUserId = authentication.getName();

        if (!userId.toString().equals(authenticatedUserId)) {
            throw new AccessDeniedException("Usuário não autorizado para realizar operações nesta conta");
        }

        return ResponseEntity.ok(service.withdraw(userId, request.amount()));
    }

    @RateLimiter(name = "accountOperations")
    @PatchMapping("/{userId}/block")
    public ResponseEntity<AccountResponse> block(@PathVariable UUID userId, Authentication authentication) {
        String authenticatedUserId = authentication.getName();

        if (!userId.toString().equals(authenticatedUserId)) {
            throw new AccessDeniedException("Usuário não autorizado para bloquear esta conta");
        }

        return ResponseEntity.ok(service.blockAccount(userId));
    }
}