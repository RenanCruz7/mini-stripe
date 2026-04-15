package account.account.service.service;

import account.account.service.domain.dto.AccountResponse;
import account.account.service.domain.entity.Account;
import account.account.service.domain.enums.AccountStatus;
import account.account.service.exception.BusinessException;
import account.account.service.exception.InsufficientBalanceException;
import account.account.service.repository.AccountRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;

    @Transactional
    public AccountResponse createAccount(UUID userId) {
        if (repository.existsByUserId(userId)) {
            throw new BusinessException("Já existe uma conta para este usuário");
        }

        var account = Account.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        var saved = repository.save(account);
        log.info("Conta criada para userId={}", userId);
        return AccountResponse.from(saved);
    }

    @Cacheable(value = "accounts", key = "#userId")
    @Transactional(readOnly = true)
    public AccountResponse getByUserId(UUID userId) {
        var account = repository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Conta não encontrada"));
        return AccountResponse.from(account);
    }

    @CacheEvict(value = "accounts", key = "#userId")
    @Transactional
    public AccountResponse deposit(UUID userId, BigDecimal amount) {
        var account = repository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new BusinessException("Conta não encontrada"));

        validateAccountActive(account);

        account.setBalance(account.getBalance().add(amount));
        var saved = repository.save(account);

        log.info("Depósito de {} realizado na conta userId={}", amount, userId);
        return AccountResponse.from(saved);
    }

    @CacheEvict(value = "accounts", key = "#userId")
    @Transactional
    public AccountResponse withdraw(UUID userId, BigDecimal amount) {
        var account = repository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new BusinessException("Conta não encontrada"));

        validateAccountActive(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        account.setBalance(account.getBalance().subtract(amount));
        var saved = repository.save(account);

        log.info("Saque de {} realizado na conta userId={}", amount, userId);
        return AccountResponse.from(saved);
    }

    @CacheEvict(value = "accounts", key = "#userId")
    @Transactional
    public AccountResponse blockAccount(UUID userId) {
        var account = repository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Conta não encontrada"));

        account.setStatus(AccountStatus.BLOCKED);
        var saved = repository.save(account);

        log.info("Conta bloqueada para userId={}", userId);
        return AccountResponse.from(saved);
    }

    private void validateAccountActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Conta não está ativa");
        }
    }
}