package com.gamerift.feature.stats.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.gamerift.feature.category.model.Category;
import com.gamerift.feature.category.repository.CategoryRepository;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.comment.repository.CommentRepository;
import com.gamerift.feature.payment.model.Order;
import com.gamerift.feature.payment.model.OrderStatus;
import com.gamerift.feature.payment.repository.OrderRepository;
import com.gamerift.feature.stats.dto.AdminStatsResponse;
import com.gamerift.feature.stats.dto.CountryStatResponse;
import com.gamerift.feature.stats.dto.PublicStatsResponse;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.feature.user.model.CountryCount;
import com.gamerift.feature.user.repository.UserRepository;
import com.gamerift.feature.user.repository.UserStatsRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The aggregation itself runs inside MongoDB, so we test how the service shapes its result. */
@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private UserStatsRepository userStatsRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private LiveStreamRepository liveStreamRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private StatsServiceImpl statsService;

    private CountryCount row(String country, long users) {
        CountryCount c = new CountryCount();
        c.setCountry(country);
        c.setUsers(users);
        return c;
    }

    @Test
    void usersByCountry_keepsTheTopFiveAndGroupsTheRest() {
        List<CountryCount> rows = new ArrayList<>();
        String[] names = {"Bangladesh", "India", "United States", "Pakistan", "United Kingdom", "Germany", "France"};
        long[] counts = {10, 9, 8, 7, 6, 5, 4};
        for (int i = 0; i < names.length; i++) {
            rows.add(row(names[i], counts[i]));
        }
        when(userStatsRepository.countUsersByCountry()).thenReturn(rows);

        List<CountryStatResponse> result = statsService.getUsersByCountry();

        assertEquals(6, result.size());
        assertEquals("BD", result.get(0).getCode());
        assertEquals("Rest of world", result.get(5).getCountry());
        assertEquals("WW", result.get(5).getCode());
        assertEquals(9, result.get(5).getUsers());
    }

    @Test
    void usersByCountry_withFewCountries_hasNoRestRow() {
        when(userStatsRepository.countUsersByCountry()).thenReturn(List.of(row("Bangladesh", 3), row("India", 1)));

        List<CountryStatResponse> result = statsService.getUsersByCountry();

        assertEquals(2, result.size());
    }

    @Test
    void usersByCountry_unknownCountry_getsTheCodeXX() {
        when(userStatsRepository.countUsersByCountry()).thenReturn(List.of(row("Narnia", 2)));

        assertEquals("XX", statsService.getUsersByCountry().get(0).getCode());
    }

    @Test
    void publicStats_countsUsersChannelsLiveStreamsAndViewers() {
        LiveStream a = new LiveStream();
        a.setPhase(StreamPhase.LIVE);
        a.setBaseViewers(100);
        LiveStream b = new LiveStream();
        b.setPhase(StreamPhase.LIVE);
        b.setBaseViewers(50);
        when(liveStreamRepository.findByPublishedTrueAndPhase(StreamPhase.LIVE)).thenReturn(List.of(a, b));
        when(userRepository.count()).thenReturn(6L);
        when(channelRepository.count()).thenReturn(6L);

        PublicStatsResponse stats = statsService.getPublicStats();

        assertEquals(6, stats.getUsers());
        assertEquals(6, stats.getChannels());
        assertEquals(2, stats.getLiveStreams());
        assertEquals(150, stats.getViewersNow());
    }

    @Test
    void adminStats_groupsEveryLiveStreamByItsCategory() {
        Category esports = new Category();
        esports.setName("Esports");
        esports.setSlug("esports");
        when(categoryRepository.findAll()).thenReturn(List.of(esports));
        when(liveStreamRepository.findByCategorySlug("esports")).thenReturn(List.of(new LiveStream(), new LiveStream()));

        AdminStatsResponse stats = statsService.getAdminStats();

        assertEquals(1, stats.getStreamsByCategory().size());
        assertEquals("Esports", stats.getStreamsByCategory().get(0).getCategory());
        assertEquals("esports", stats.getStreamsByCategory().get(0).getSlug());
        assertEquals(2, stats.getStreamsByCategory().get(0).getCount());
    }

    @Test
    void getAdminStats_revenueIsAllTimeAndTheChartHasSevenDays() {
        Order recent = new Order();
        recent.setStatus(OrderStatus.PAID);
        recent.setTotal(200);
        recent.setCreatedAt(Instant.now());
        Order old = new Order();
        old.setStatus(OrderStatus.PAID);
        old.setTotal(100);
        old.setCreatedAt(Instant.now().minus(40, ChronoUnit.DAYS));
        Order failed = new Order();
        failed.setStatus(OrderStatus.FAILED);
        failed.setTotal(999);
        failed.setCreatedAt(Instant.now());
        when(orderRepository.findAll()).thenReturn(List.of(recent, old, failed));

        AdminStatsResponse stats = statsService.getAdminStats();

        assertEquals(300, stats.getRevenue());
        assertEquals(7, stats.getRevenueByDay().size());
        assertEquals(200L, stats.getRevenueByDay().get(6).longValue());
    }
}
