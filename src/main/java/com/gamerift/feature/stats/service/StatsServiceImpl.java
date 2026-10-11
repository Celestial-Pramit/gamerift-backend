package com.gamerift.feature.stats.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gamerift.feature.category.model.Category;
import com.gamerift.feature.category.repository.CategoryRepository;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.comment.repository.CommentRepository;
import com.gamerift.feature.payment.model.Order;
import com.gamerift.feature.payment.model.OrderStatus;
import com.gamerift.feature.payment.repository.OrderRepository;
import com.gamerift.feature.stats.dto.AdminStatsResponse;
import com.gamerift.feature.stats.dto.CategoryCountResponse;
import com.gamerift.feature.stats.dto.CountryStatResponse;
import com.gamerift.feature.stats.dto.PublicStatsResponse;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.feature.user.model.CountryCount;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.feature.user.repository.UserStatsRepository;

import org.springframework.stereotype.Service;

/**
 * Business rules for statistics (layer: service).
 *  - the heavy counting is done by MongoDB (aggregation); here we only shape the result,
 *  - admin stats group every live stream by its category (counted in Java, the repository
 *    has no countByCategorySlug) and sum the PAID orders for the revenue.
 */
@Service
public class StatsServiceImpl implements IStatsService {

    private static final int TOP_COUNTRIES = 5;

    private static final Map<String, String> CODES = Map.of(
            "Bangladesh", "BD", "India", "IN", "United States", "US", "Pakistan", "PK", "United Kingdom", "GB",
            "Germany", "DE", "France", "FR", "Ukraine", "UA");

    private final UserStatsRepository userStatsRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final OrderRepository orderRepository;

    public StatsServiceImpl(UserStatsRepository userStatsRepository, UserRepository userRepository,
                            ChannelRepository channelRepository, LiveStreamRepository liveStreamRepository,
                            CategoryRepository categoryRepository, CommentRepository commentRepository,
                            OrderRepository orderRepository) {
        this.userStatsRepository = userStatsRepository;
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.liveStreamRepository = liveStreamRepository;
        this.categoryRepository = categoryRepository;
        this.commentRepository = commentRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public List<CountryStatResponse> getUsersByCountry() {
        List<CountryCount> counts = userStatsRepository.countUsersByCountry();

        List<CountryStatResponse> result = new ArrayList<>();
        long rest = 0;
        for (int i = 0; i < counts.size(); i++) {
            CountryCount row = counts.get(i);
            if (i < TOP_COUNTRIES) {
                result.add(new CountryStatResponse(CODES.getOrDefault(row.getCountry(), "XX"), row.getCountry(), row.getUsers()));
            } else {
                rest += row.getUsers();
            }
        }
        if (rest > 0) {
            result.add(new CountryStatResponse("WW", "Rest of world", rest));
        }
        return result;
    }

    @Override
    public PublicStatsResponse getPublicStats() {
        List<LiveStream> live = liveStreamRepository.findByPublishedTrueAndPhase(StreamPhase.LIVE);
        long viewers = 0;
        for (LiveStream stream : live) {
            viewers += stream.getBaseViewers();
        }
        return new PublicStatsResponse(userRepository.count(), live.size(), channelRepository.count(), viewers);
    }

    @Override
    public AdminStatsResponse getAdminStats() {
        AdminStatsResponse stats = new AdminStatsResponse();

        // 1. the four headline cards
        stats.setUsers(userRepository.count());
        stats.setLiveStreams(liveStreamRepository.findByPublishedTrueAndPhase(StreamPhase.LIVE).size());
        stats.setComments(commentRepository.count());
        stats.setRevenue(totalRevenue());
        stats.setRevenueByDay(revenueByDay());

        // 2. the two charts (the country aggregation is reused as-is)
        stats.setUsersByCountry(getUsersByCountry());
        List<CategoryCountResponse> catCounts = new ArrayList<>();
        for (Category category : categoryRepository.findAll()) {
            // the repository has no countByCategorySlug: count the list we already can fetch
            long count = liveStreamRepository.findByCategorySlug(category.getSlug()).size();
            catCounts.add(new CategoryCountResponse(category.getName(), category.getSlug(), count));
        }
        stats.setStreamsByCategory(catCounts);
        return stats;
    }

    // all-time income: every PAID order
    private long totalRevenue() {
        long total = 0;
        for (Order order : orderRepository.findAll()) {
            if (order.getStatus() == OrderStatus.PAID) {
                total += order.getTotal();
            }
        }
        return total;
    }

    // income of each of the last 7 days (UTC), oldest first; only PAID orders count
    private List<Long> revenueByDay() {
        List<Long> days = new ArrayList<>(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (Order order : orderRepository.findAll()) {
            if (order.getStatus() != OrderStatus.PAID || order.getCreatedAt() == null) {
                continue;
            }
            long diff = ChronoUnit.DAYS.between(order.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(), today);
            if (diff >= 0 && diff < 7) {
                int index = 6 - (int) diff;
                days.set(index, days.get(index) + order.getTotal());
            }
        }
        return days;
    }
}