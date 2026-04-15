package account.account.service.controller;

import account.account.service.domain.dto.*;
import account.account.service.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAccount(request.userId()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AccountResponse> getByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @PostMapping("/{userId}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable UUID userId,
            @Valid @RequestBody DepositRequest request
    ) {
        return ResponseEntity.ok(service.deposit(userId, request.amount()));
    }

    @PostMapping("/{userId}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            @PathVariable UUID userId,
            @Valid @RequestBody WithdrawRequest request
    ) {
        return ResponseEntity.ok(service.withdraw(userId, request.amount()));
    }

    @PatchMapping("/{userId}/block")
    public ResponseEntity<AccountResponse> block(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.blockAccount(userId));
    }
}