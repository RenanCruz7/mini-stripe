package account.account.service.service;

import account.account.service.domain.dto.AccountResponse;
import account.account.service.domain.entity.Account;
import account.account.service.domain.enums.AccountStatus;
import account.account.service.domain.mapper.AccountMapper;
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
    private final AccountMapper mapper;

    @Transactional
    public AccountResponse createAccount(UUID userId) {
        if (repository.existsByUserId(userId)) {
            throw new BusinessException("Account already exists for this user");
        }

        var account = Account.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        var saved = repository.save(account);
        log.info("Account created for userId={}", userId);
        return mapper.toResponse(saved);
    }

    @Cacheable(value = "accounts", key = "#userId")
    @Transactional(readOnly = true)
    public AccountResponse getByUserId(UUID userId) {
        var account = repository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Account not found"));
        return mapper.toResponse(account);
    }

    @CacheEvict(value = "accounts", key = "#userId")
    @Transactional
    public AccountResponse deposit(UUID userId, BigDecimal amount) {
        var account = repository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new BusinessException("Account not found"));

        validateAccountActive(account);

        account.setBalance(account.getBalance().add(amount));
        var saved = repository.save(account);

        log.info("Deposit of {} performed on account userId={}", amount, userId);
        return mapper.toResponse(saved);
    }

    @CacheEvict(value = "accounts", key = "#userId")
    @Transactional
    public AccountResponse withdraw(UUID userId, BigDecimal amount) {
        var account = repository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new BusinessException("Account not found"));

        validateAccountActive(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        account.setBalance(account.getBalance().subtract(amount));
        var saved = repository.save(account);

        log.info("Withdrawal of {} performed on account userId={}", amount, userId);
        return mapper.toResponse(saved);
    }

    @CacheEvict(value = "accounts", key = "#userId")
    @Transactional
    public AccountResponse blockAccount(UUID userId) {
        var account = repository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Account not found"));

        account.setStatus(AccountStatus.BLOCKED);
        var saved = repository.save(account);

        log.info("Account blocked for userId={}", userId);
        return mapper.toResponse(saved);
    }

    private void validateAccountActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Account is not active");
        }
    }
}