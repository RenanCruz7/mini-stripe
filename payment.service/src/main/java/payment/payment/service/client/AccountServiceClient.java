package payment.payment.service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import payment.payment.service.domain.dto.WithdrawRequest;
import payment.payment.service.exception.AccountServiceException;
import payment.payment.service.exception.PaymentProcessingException;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountServiceClient {

    private final WebClient webClient;

    @Value("${account-service.url}")
    private String accountServiceUrl;

    @CircuitBreaker(name = "accountServiceClient", fallbackMethod = "withdrawFallback")
    @Retry(name = "accountServiceClient")
    public void withdraw(UUID userId, BigDecimal amount, String authorizationHeader) {
        try {
            WithdrawRequest request = new WithdrawRequest(amount);

            webClient.post()
                    .uri(accountServiceUrl + "/api/accounts/{userId}/withdraw", userId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Withdraw successful for userId: {} amount: {}", userId, amount);
        } catch (WebClientResponseException.BadRequest ex) {
            log.error("Bad request when withdrawing from account service: {}", ex.getMessage());
            throw new AccountServiceException("Invalid withdraw request: " + ex.getMessage());
        } catch (WebClientResponseException.NotFound ex) {
            log.error("Account not found for userId: {}", userId);
            throw new AccountServiceException("Account not found");
        } catch (WebClientResponseException ex) {
            log.error("Account service error: {} - {}", ex.getStatusCode(), ex.getMessage());
            throw new AccountServiceException("Account service unavailable: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error during withdraw: {}", ex.getMessage(), ex);
            throw new PaymentProcessingException("Error communicating with account service: " + ex.getMessage());
        }
    }

    public void withdrawFallback(UUID userId, BigDecimal amount, String authorizationHeader, Exception ex) {
        log.warn("Circuit breaker triggered for withdraw. UserId: {}, Amount: {}", userId, amount);
        throw new AccountServiceException("Account service is currently unavailable. Please try again later.");
    }
}

