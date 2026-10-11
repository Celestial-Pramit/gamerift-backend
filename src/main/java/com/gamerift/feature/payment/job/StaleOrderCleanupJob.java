package com.gamerift.feature.payment.job;

import java.time.Duration;
import java.time.Instant;

import com.gamerift.feature.payment.model.Order;
import com.gamerift.feature.payment.model.OrderStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A scheduled job that cleans up abandoned checkouts.
 *
 * If a user opens the payment page and never pays, the order stays PENDING forever.
 * Every 10 minutes this job cancels PENDING orders older than 30 minutes.
 *
 * The update is ONE atomic database command (updateMulti), not read-then-write, so it can
 * never overwrite an order that a payment callback just marked PAID in the meantime.
 * @Scheduled needs @EnableScheduling (see SchedulingConfig).
 */
@Component
public class StaleOrderCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(StaleOrderCleanupJob.class);
    private static final Duration MAX_AGE = Duration.ofMinutes(30);

    private final MongoTemplate mongoTemplate;

    public StaleOrderCleanupJob(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 600_000)
    public void cancelStaleOrders() {
        Instant cutoff = Instant.now().minus(MAX_AGE);
        Query query = Query.query(Criteria.where("status").is(OrderStatus.PENDING)
                .and("createdAt").lt(cutoff));
        Update update = new Update().set("status", OrderStatus.CANCELLED);

        long changed = mongoTemplate.updateMulti(query, update, Order.class).getModifiedCount();
        if (changed > 0) {
            log.info("Cancelled {} stale PENDING orders (older than {} minutes)", changed, MAX_AGE.toMinutes());
        }
    }
}
