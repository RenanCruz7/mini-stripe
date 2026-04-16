package payment.payment.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment.payment.service.domain.entity.OutboxEvent;
import payment.payment.service.repository.OutboxEventRepository;

import java.util.List;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void publishPendingEvents() {
        log.debug("Publishing pending outbox events");

        List<OutboxEvent> pendingEvents = outboxEventRepository.findByPublishedFalse();

        if (pendingEvents.isEmpty()) {
            log.debug("No pending outbox events to publish");
            return;
        }

        log.info("Found {} pending outbox events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send("payments", event.getAggregateId().toString(), event.getPayload());
                log.info("Event published to Kafka for aggregateId: {}", event.getAggregateId());

                event.setPublished(true);
                outboxEventRepository.save(event);
                log.info("Outbox event marked as published for aggregateId: {}", event.getAggregateId());
            } catch (Exception ex) {
                log.error("Error publishing event for aggregateId: {} - {}", event.getAggregateId(), ex.getMessage());
            }
        }
    }
}

