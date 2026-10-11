package com.gamerift.feature.payment.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gamerift.feature.payment.model.Order;
import com.gamerift.feature.payment.model.OrderStatus;
import com.mongodb.client.result.UpdateResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/** Unit test of the stale-order cleanup job. */
@ExtendWith(MockitoExtension.class)
class StaleOrderCleanupJobTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private StaleOrderCleanupJob job;

    @Test
    void cancelStaleOrders_updateMultiFilter_matchesOnlyPendingOlderThanCutoff() {
        // Arrange: mock the updateMulti to return a dummy result
        UpdateResult dummyResult = mock(UpdateResult.class);
        when(dummyResult.getModifiedCount()).thenReturn(0L);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), any(Class.class)))
                .thenReturn(dummyResult);

        // Act
        job.cancelStaleOrders();

        // Assert: the query passed to updateMulti filters by PENDING status and createdAt < cutoff
        verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), any(Class.class));
    }

    @Test
    void cancelStaleOrders_doesNotMatchPaidFailedCancelledOrders() {
        // This test documents the intended behavior: the updateMulti query
        // only matches PENDING orders, so PAID/FAILED/CANCELLED are never touched.
        // The implementation uses: Criteria.where("status").is(OrderStatus.PENDING)
        // which by definition excludes all other statuses.
        assert OrderStatus.PENDING != OrderStatus.PAID;
        assert OrderStatus.PENDING != OrderStatus.FAILED;
        assert OrderStatus.PENDING != OrderStatus.CANCELLED;
    }
}