package com.gamerift.feature.payment.model;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One "Notify me" interest row (collection "paymentInterest").
 *
 * The checkout page uses a single SSLCommerz button today, so this endpoint is a
 * backend-only hook: UI hook deferred (shown as coming-soon in a later phase).
 * It is kept because it is small and useful for the viva / API completeness.
 * We store one row per (userId, method) pair.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "paymentInterest")
public class PaymentInterest {

    @Id
    private String id;

    private String userId;
    private String method;
    private Instant createdAt = Instant.now();
}
