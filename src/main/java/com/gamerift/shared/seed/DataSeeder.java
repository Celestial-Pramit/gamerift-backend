package com.gamerift.shared.seed;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gamerift.feature.category.model.Category;
import com.gamerift.feature.category.repository.CategoryRepository;
import com.gamerift.feature.channel.model.Channel;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.comment.model.Comment;
import com.gamerift.feature.comment.model.CommentLike;
import com.gamerift.feature.comment.repository.CommentLikeRepository;
import com.gamerift.feature.comment.repository.CommentRepository;
import com.gamerift.feature.event.model.GameEvent;
import com.gamerift.feature.event.repository.GameEventRepository;
import com.gamerift.feature.match.model.MatchCard;
import com.gamerift.feature.match.model.MatchStatus;
import com.gamerift.feature.match.repository.MatchCardRepository;
import com.gamerift.feature.notification.model.AppNotification;
import com.gamerift.feature.notification.repository.NotificationRepository;
import com.gamerift.feature.payment.model.Coupon;
import com.gamerift.feature.payment.model.CouponType;
import com.gamerift.feature.payment.model.Order;
import com.gamerift.feature.payment.model.OrderStatus;
import com.gamerift.feature.payment.model.Plan;
import com.gamerift.feature.payment.repository.CouponRepository;
import com.gamerift.feature.payment.repository.OrderRepository;
import com.gamerift.feature.payment.repository.PlanRepository;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;
import com.gamerift.feature.stream.model.VideoSource;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.feature.ticker.model.TickerMessage;
import com.gamerift.feature.ticker.repository.TickerMessageRepository;
import com.gamerift.feature.user.model.Role;
import com.gamerift.feature.user.model.User;
import com.gamerift.feature.user.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Fills an EMPTY database with starter data so the site is never empty on the first start.
 *
 * CommandLineRunner = Spring calls run() once, right after the application has started.
 * Every seed method first checks "is this collection empty?" so restarting the app never
 * creates duplicates (idempotent).
 *
 * Seeded: admin user, (dev only) demo viewers, categories, channels, streams, events, match
 * cards, Pro plans, coupons and ticker messages. YouTube links are read from
 * src/main/resources/seed/video-links.properties.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    // Password for the demo viewers. DEV PROFILE ONLY (demo users are switched off in prod).
    private static final String DEMO_PASSWORD = "Gamer@123";
    private static final Pattern YOUTUBE_ID = Pattern.compile("(?:v=|youtu\\.be/|embed/)([\\w-]{11})");

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ChannelRepository channelRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final GameEventRepository gameEventRepository;
    private final MatchCardRepository matchCardRepository;
    private final PlanRepository planRepository;
    private final CouponRepository couponRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final OrderRepository orderRepository;
    private final NotificationRepository notificationRepository;
    private final TickerMessageRepository tickerMessageRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminEmail;
    private final String adminPassword;
    private final boolean seedDemoUsers;
    private final boolean resetData;

    // Used only while seeding streams
    private Properties videoLinks = new Properties();
    private Map<String, Channel> channelsByName = new HashMap<>();

    // Constructor injection: Spring passes the repositories and the settings (@Value) in
    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      ChannelRepository channelRepository,
                      LiveStreamRepository liveStreamRepository,
                      GameEventRepository gameEventRepository,
                      MatchCardRepository matchCardRepository,
                      PlanRepository planRepository,
                      CouponRepository couponRepository,
                      CommentRepository commentRepository,
                      CommentLikeRepository commentLikeRepository,
                      OrderRepository orderRepository,
                      NotificationRepository notificationRepository,
                      TickerMessageRepository tickerMessageRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.admin.email}") String adminEmail,
                      @Value("${app.admin.password}") String adminPassword,
                      @Value("${app.seed.demo-users}") boolean seedDemoUsers,
                      @Value("${app.seed.reset}") boolean resetData) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.channelRepository = channelRepository;
        this.liveStreamRepository = liveStreamRepository;
        this.gameEventRepository = gameEventRepository;
        this.matchCardRepository = matchCardRepository;
        this.planRepository = planRepository;
        this.couponRepository = couponRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.orderRepository = orderRepository;
        this.notificationRepository = notificationRepository;
        this.tickerMessageRepository = tickerMessageRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.seedDemoUsers = seedDemoUsers;
        this.resetData = resetData;
    }

    @Override
    public void run(String... args) {
        if (resetData) {
            resetSeededCollections();
        }
        seedAdmin();
        if (seedDemoUsers) {
            seedDemoUsers();
        }
        seedCategories();
        seedChannels();
        seedStreams();
        seedEvents();
        seedMatches();
        seedPlans();
        seedCoupons();
        seedComments();
        seedOrders();
        seedNotifications();
        seedTicker();
        log.info("Seeding finished.");
    }

    // ------------------------------------------------------------------ users

    private void seedAdmin() {
        // The admin account comes from ADMIN_EMAIL / ADMIN_PASSWORD in the .env file
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.length() < 8) {
            log.warn("ADMIN_EMAIL / ADMIN_PASSWORD missing or shorter than 8 characters: admin NOT created.");
            return;
        }
        String email = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email) || userRepository.existsByUsername("admin")) {
            log.info("Admin user already exists, skipping.");
            return;
        }
        userRepository.save(newUser("admin", email, adminPassword, "Bangladesh", false));
        log.info("Admin user created: {}", email);
    }

    private void seedDemoUsers() {
        // username, email, country, banned
        String[][] demo = {
            {"gamer", "gamer@gamerift.tech", "Bangladesh", "false"},
            {"arif_92", "arif@example.com", "Bangladesh", "false"},
            {"nila", "nila@example.com", "India", "false"},
            {"sara", "sara@example.com", "United Kingdom", "false"},
            {"tanvir", "tanvir@example.com", "Pakistan", "true"},
        };
        for (String[] d : demo) {
            if (!userRepository.existsByEmail(d[1])) {
                userRepository.save(newUser(d[0], d[1], DEMO_PASSWORD, d[2], Boolean.parseBoolean(d[3])));
            }
        }
        log.info("Demo viewers ensured (dev only, password {}).", DEMO_PASSWORD);
    }

    private User newUser(String username, String email, String rawPassword, String country, boolean banned) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        // Only the BCrypt hash is stored, never the real password
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole("admin".equals(username) ? Role.ADMIN : Role.VIEWER);
        user.setCountry(country);
        user.setEmailVerified(true);
        user.setBanned(banned);
        return user;
    }

    // ------------------------------------------------------------- categories

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }
        categoryRepository.saveAll(List.of(
                category("Esports", "esports", "🏆"),
                category("FPS", "fps", "🎯"),
                category("IRL", "irl", "🎤"),
                category("MOBA", "moba", "⚔️"),
                category("Speedrun", "speedrun", "⏱️")));
        log.info("Categories seeded.");
    }

    private Category category(String name, String slug, String icon) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        c.setIcon(icon);
        return c;
    }

    // --------------------------------------------------------------- channels

    private void seedChannels() {
        if (channelRepository.count() > 0) {
            return;
        }
        channelRepository.saveAll(List.of(
                channel("NightRaid", 148000, true, false, true, "GRAND FINALS · MAP 3"),
                channel("Rozu", 42100, false, true, true, "RANKED GRIND"),
                channel("MimSays", 9600, false, false, true, "LATE NIGHT CHILL"),
                channel("SahaGaming", 18200, true, false, false, "LAST LIVE 2 DAYS AGO"),
                channel("KaziPlays", 31400, false, false, false, "STARTS IN 1H 10M"),
                channel("xenon_bd", 22800, false, false, true, "WATCHPARTY")));
        log.info("Channels seeded.");
    }

    private Channel channel(String name, long followers, boolean verified, boolean pro, boolean live, String note) {
        Channel c = new Channel();
        c.setName(name);
        c.setFollowersCount(followers);
        c.setVerified(verified);
        c.setPro(pro);
        c.setLive(live);
        c.setNote(note);
        return c;
    }

    // ---------------------------------------------------------------- streams

    private void seedStreams() {
        if (liveStreamRepository.count() > 0) {
            return;
        }
        // Remember channels by name so each stream can point to its channel id
        channelsByName = new HashMap<>();
        for (Channel c : channelRepository.findAll()) {
            channelsByName.put(c.getName(), c);
        }
        videoLinks = loadVideoLinks();

        liveStreamRepository.saveAll(List.of(
    stream("grand-finals", "NRG vs. FNC | MATCH HIGHLIGHTS | VALORANT Champions Paris 2025 - Grand Finals", "VALORANT Champions Tour", "esports", StreamPhase.LIVE, 72, 10112, 92, true, "tournament", "valorant"),
    stream("ranked-grind", "15 Unforgettable Plays From Masters London", "VALORANT Champions Tour", "esports", StreamPhase.LIVE, 184, 6145, 75, false, "valorant", "bangla"),
    stream("late-night-chill", "CS2 POV | DONK ON FIRE 60 KILLS! Faceit Ranked (DUST2) Voicecomms 60-27", "Siez CSGO", "fps", StreamPhase.LIVE, 140, 6752, 40, false, "chill", "bangla"),
    stream("mobile-moba-scrims", "Pramit aka Orion JUST DROPPED A DOUBLE ACE IN ONE VALORANT MATCH!", "Pramit Dutta", "fps", StreamPhase.SOON, 12, 3992, 10, false, "mobile"),
    stream("any-percent-run", "TOP 60 BEST LEAGUE OF LEGENDS PENTAKILLS OF 2024!", "Protatomonster", "moba", StreamPhase.LIVE, 44, 2765, 66, false, "speedrun"),
    stream("dhaka-major-watchparty", "Top 10 Dota 2 Plays of All Time", "hOlyhexOr", "esports", StreamPhase.LIVE, 310, 13153, 88, false, "tournament", "bangla"),
    stream("customs-night", "Minecraft Beaten In 53 Seconds [FWR]", "HanabiYaki", "fps", StreamPhase.ENDED, 0, 13565, 5, false, "valorant"),
    stream("aim-lab-bootcamp", "Best Speed Running Moments From GDQ", "GameSpot", "fps", StreamPhase.LIVE, 25, 5490, 55, false, "valorant"),
    stream("pocket-moba-ranked", "*NEW* WARZONE BEST HIGHLIGHTS! - Epic and Funny Moments #193", "Top WARZONE Moments", "moba", StreamPhase.LIVE, 95, 13563, 35, false, "mobile", "bangla"),
    stream("charity-flood-relief", "Battlefield 6 WTF - Epic Moments Of The Week #26", "BATTLEFIELD WTF", "irl", StreamPhase.LIVE, 210, 7826, 80, false, "chill", "bangla"),
    stream("speedrun-marathon-warmup", "Shroud Apex Legends Best Moments", "Supreme Gaming", "speedrun", StreamPhase.SOON, 180, 2443, 20, false, "speedrun"),
    stream("coach-reacts", "Black Ops 6 Most Wanted - Realistic Immersive Ultra Graphics Gameplay 4K UHD 60FPS Call of Duty", "HEDGEHOG x", "fps", StreamPhase.LIVE, 60, 12562, 60, false, "tournament")
));
        log.info("Streams seeded.");
    }

    /**
     * Builds one stream.
     * minutes = minutes since it started (LIVE) or until it starts (SOON); ignored for ENDED.
     */
    private LiveStream stream(String key, String title, String channelName, String categorySlug, StreamPhase phase,
                              int minutes, int baseViewers, double trend, boolean featured, String... tags) {
        Instant now = Instant.now();
        Channel channel = channelsByName.get(channelName);

        LiveStream s = new LiveStream();
        s.setTitle(title);
        s.setDescription(channelName + " streams: " + title + ".");
        s.setCategorySlug(categorySlug);
        if (channel != null) {
            s.setChannelId(channel.getId());
        }
        s.setChannelName(channelName);
        s.setVideoSource(VideoSource.YOUTUBE);
        String url = videoLinks.getProperty(key, "").trim();
        s.setVideoUrl(url);
        s.setThumbnailUrl(youtubeThumbnail(url));
        s.setPhase(phase);
        s.setPublished(true);
        s.setFeatured(featured);
        s.setBaseViewers(baseViewers);
        s.setTags(Arrays.asList(tags));
        s.setLikesCount(baseViewers / 6);
        s.setTrendScore(trend);
        if (phase == StreamPhase.LIVE) {
            s.setStartedAt(now.minus(minutes, ChronoUnit.MINUTES));
        } else if (phase == StreamPhase.SOON) {
            s.setScheduledAt(now.plus(minutes, ChronoUnit.MINUTES));
        } else {
            s.setStartedAt(now.minus(1, ChronoUnit.DAYS));
        }
        return s;
    }

    private Properties loadVideoLinks() {
        Properties props = new Properties();
        try (InputStream in = new ClassPathResource("seed/video-links.properties").getInputStream();
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            log.warn("seed/video-links.properties not found: streams will use the generated artwork.");
        }
        return props;
    }

    // "https://youtu.be/ID" or "...watch?v=ID"  ->  thumbnail image URL (empty when no valid link)
    private String youtubeThumbnail(String url) {
        Matcher m = YOUTUBE_ID.matcher(url);
        return m.find() ? "https://img.youtube.com/vi/" + m.group(1) + "/hqdefault.jpg" : "";
    }

    // ----------------------------------------------------------------- events

    private void seedEvents() {
        if (gameEventRepository.count() > 0) {
            return;
        }
        Instant now = Instant.now();
        gameEventRepository.saveAll(List.of(
                event("Dhaka Major — Semifinal A", "ESPORTS", "NightRaid vs Nova Ascent", now.plus(52, ChronoUnit.HOURS), "soon", 0, 0),
                event("Charity Stream · Flood Relief", "IRL", "Hosted by MimSays + 6", now.plus(122, ChronoUnit.HOURS), "fund", 200000, 128400),
                event("Speedrun Marathon · 12h", "SPEEDRUN", "Any% relay with 8 runners", now.plus(240, ChronoUnit.HOURS), "", 0, 0),
                event("Pro Workshop · Grow your channel", "WORKSHOP", "60 min + live Q&A", now.plus(384, ChronoUnit.HOURS), "pro", 0, 0),
                event("Valorant Community Cup · Finals", "ESPORTS", "Streaming now on NightRaid", now.minus(90, ChronoUnit.MINUTES), "", 0, 0),
                event("Dhaka Major — Quarterfinals", "ESPORTS", "Replay available", now.minus(144, ChronoUnit.HOURS), "", 0, 0),
                event("Aim Lab Bootcamp", "WORKSHOP", "Replay available", now.minus(288, ChronoUnit.HOURS), "", 0, 0)));
        log.info("Events seeded (times are relative to the first start).");
    }

    private GameEvent event(String title, String category, String note, Instant startsAt, String badge, long goal, long raised) {
        GameEvent e = new GameEvent();
        e.setTitle(title);
        e.setCategory(category);
        e.setNote(note);
        e.setStartsAt(startsAt);
        e.setBadge(badge);
        e.setGoal(goal);
        e.setRaised(raised);
        return e;
    }

    // ---------------------------------------------------------------- matches

    private void seedMatches() {
        if (matchCardRepository.count() > 0) {
            return;
        }
        matchCardRepository.saveAll(List.of(
                match("NightRaid", "Nova Ascent", 1, 2, "1.85", "6.20", "2.40", MatchStatus.LIVE, "MAP 3 · 12:04"),
                match("Dhaka Pulse", "Kazi Crew", 9, 9, "2.10", "5.50", "1.95", MatchStatus.HALF_TIME, "MAP 2 · 00:00")));
        log.info("Match cards seeded.");
    }

    private MatchCard match(String a, String b, int scoreA, int scoreB, String oddsA, String oddsDraw, String oddsB, MatchStatus status, String info) {
        MatchCard m = new MatchCard();
        m.setTeamA(a);
        m.setTeamB(b);
        m.setScoreA(scoreA);
        m.setScoreB(scoreB);
        m.setOddsA(oddsA);
        m.setOddsDraw(oddsDraw);
        m.setOddsB(oddsB);
        m.setStatus(status);
        m.setInfo(info);
        return m;
    }

    // ------------------------------------------------------------------ plans

    private void seedPlans() {
        if (planRepository.count() > 0) {
            return;
        }
        planRepository.saveAll(List.of(
                plan("Player One", 30, 99, "Try the gold life. Cancel anytime.", false,
                        "Gold name in chat", "PRO badge", "Zero ads"),
                plan("Squad", 90, 249, "Most popular with streamers and grinders.", true,
                        "Everything in Player One", "Pro-only workshops and events", "Early access to new features"),
                plan("Legend", 365, 899, "For the ones who already know.", false,
                        "Everything in Squad", "Custom chat colour", "Priority support queue")));
        log.info("Pro plans seeded (placeholder prices).");
    }

    private Plan plan(String name, int days, int priceBdt, String tagline, boolean bestValue, String... perks) {
        Plan p = new Plan();
        p.setName(name);
        p.setDays(days);
        p.setPriceBdt(priceBdt);
        p.setTagline(tagline);
        p.setBestValue(bestValue);
        p.setPerks(Arrays.asList(perks));
        return p;
    }

    // ---------------------------------------------------------------- coupons

    private void seedCoupons() {
        if (couponRepository.count() > 0) {
            return;
        }
        couponRepository.saveAll(List.of(
                coupon("GG10", CouponType.PERCENT, 10, 500, 42, LocalDate.now().plusMonths(3), true),
                coupon("WELCOME50", CouponType.FLAT, 50, 1000, 120, LocalDate.now().plusMonths(6), true),
                coupon("EID25", CouponType.PERCENT, 25, 200, 200, LocalDate.now().minusDays(30), false)));
        log.info("Coupons seeded.");
    }

    private Coupon coupon(String code, CouponType type, int value, int maxUses, int used, LocalDate expiresAt, boolean active) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setType(type);
        c.setValue(value);
        c.setMaxUses(maxUses);
        c.setUsedCount(used);
        c.setExpiresAt(expiresAt);
        c.setActive(active);
        return c;
    }

    // --------------------------------------------------------------- comments

    private void seedComments() {
        if (commentRepository.count() > 0) {
            return;
        }
        LiveStream stream = liveStreamRepository.findTop8ByPublishedTrueAndPhaseOrderByTrendScoreDesc(StreamPhase.LIVE)
                .stream().findFirst().orElse(null);
        if (stream == null) {
            return;
        }
        Map<String, User> byName = new HashMap<>();
        for (User u : userRepository.findAll()) {
            byName.put(u.getUsername(), u);
        }

        Comment arif = savedComment(byName.get("arif_92"), stream.getId(), null,
                "The retake on B site was textbook. Watched it back twice already.", false, 42);
        Comment roZu = savedComment(byName.get("Rozu") == null ? byName.get("gamer") : byName.get("Rozu"), stream.getId(), null,
                "If they keep forcing eco like that it's over by round 20. Calling it now.", false, 128);
        savedComment(byName.get("nila"), stream.getId(), null,
                "Dhaka represent! First time here, love the vibe.", false, 7);
        savedComment(byName.get("admin"), stream.getId(), null,
                "Pinned: finals schedule is on the Events page. Be nice in chat!", true, 210);
        if (arif != null) {
            savedComment(byName.get("nila"), stream.getId(), arif.getId(),
                    "Second watch incoming, that flick was illegal.", false, 11);
        }
        if (roZu != null) {
            CommentLike like = new CommentLike();
            like.setCommentId(roZu.getId());
            like.setUserId(byName.get("nila").getId());
            commentLikeRepository.save(like);
        }
        log.info("Comments seeded.");
    }

    // username may not exist in the demo set: skip it instead of crashing
    private Comment savedComment(User user, String streamId, String parentId, String text, boolean pinned, int likes) {
        if (user == null) {
            return null;
        }
        Comment c = new Comment();
        c.setStreamId(streamId);
        c.setUserId(user.getId());
        c.setUsername(user.getUsername());
        c.setText(text);
        c.setParentId(parentId);
        c.setPinned(pinned);
        c.setLikesCount(likes);
        c.setCreatedAt(Instant.now().minus(likes, ChronoUnit.MINUTES));
        return commentRepository.save(c);
    }

    // ------------------------------------------------------------------ orders

    private void seedOrders() {
        if (orderRepository.count() > 0) {
            return;
        }
        Map<String, User> byName = new HashMap<>();
        for (User u : userRepository.findAll()) {
            byName.put(u.getUsername(), u);
        }
        Plan squad = planRepository.findAll().stream().filter(p -> "Squad".equals(p.getName())).findFirst().orElse(null);
        Plan playerOne = planRepository.findAll().stream().filter(p -> "Player One".equals(p.getName())).findFirst().orElse(null);
        if (squad != null && playerOne != null) {
            order("GR-88213", byName.get("gamer"), squad, 249, 30);
            order("GR-87104", byName.get("nila"), playerOne, 99, 60);
            order("GR-86990", byName.get("gamer"), playerOne, 89, 91);
        }
        log.info("Orders seeded (users and orders are NEVER reset).");
    }

    private void order(String id, User user, Plan plan, int total, int daysAgo) {
        if (user == null) {
            return;
        }
        Order o = new Order();
        o.setId(id);
        o.setUserId(user.getId());
        o.setPlanId(plan.getId());
        o.setPlanName(plan.getName());
        o.setDays(plan.getDays());
        o.setPriceBdt(plan.getPriceBdt());
        o.setCode(null);
        o.setDiscount(plan.getPriceBdt() - total);
        o.setTotal(total);
        o.setMethod("SSLCommerz");
        o.setGateway("SSLCOMMERZ");
        // Demo orders are already finished payments, so they are PAID straight away
        // (NOT the PENDING default) and get a paidAt time like a real confirmed payment.
        o.setStatus(OrderStatus.PAID);
        o.setPaidAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        o.setCreatedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        orderRepository.save(o);
    }

    // ------------------------------------------------------------ notifications

    private void seedNotifications() {
        if (notificationRepository.count() > 0) {
            return;
        }
        Map<String, User> byName = new HashMap<>();
        for (User u : userRepository.findAll()) {
            byName.put(u.getUsername(), u);
        }
        notification(byName.get("gamer"), "pay", "Pro activated: receipt GR-88213", "/app/settings", 1);
        notification(byName.get("gamer"), "live", "Rozu is live: Ranked grind to Immortal", "/app/discover", 2);
        notification(byName.get("nila"), "reply", "arif_92 replied to your comment", "/app/stream", 6);
        notification(byName.get("nila"), "event", "Dhaka Major starts in 30 minutes", "/app/events", 9);
        notification(byName.get("sara"), "follow", "NightRaid followed you back", "/app/discover", 20);
        log.info("Notifications seeded.");
    }

    private void notification(User user, String type, String title, String to, int hoursAgo) {
        if (user == null) {
            return;
        }
        AppNotification n = new AppNotification();
        n.setUserId(user.getId());
        n.setType(type);
        n.setTitle(title);
        n.setTargetUrl(to);
        n.setCreatedAt(Instant.now().minus(hoursAgo, ChronoUnit.HOURS));
        notificationRepository.save(n);
    }

    // ----------------------------------------------------------------- ticker

    private void seedTicker() {
        if (tickerMessageRepository.count() > 0) {
            return;
        }
        for (String text : List.of("Live now: Valorant Champions Finals", "Upcoming: Fastest Fingers in Gaming", "Go Pro for a gold chat name")) {
            TickerMessage t = new TickerMessage();
            t.setText(text);
            tickerMessageRepository.save(t);
        }
        log.info("Ticker messages seeded.");
    }

    // ------------------------------------------------------------------ reset

    // Only when app.seed.reset=true (development): wipes the seeded content so it can be seeded again.
    // Users and orders are NEVER deleted here.
    private void resetSeededCollections() {
        log.warn("app.seed.reset=true: deleting seeded collections (users are kept).");
        liveStreamRepository.deleteAll();
        channelRepository.deleteAll();
        categoryRepository.deleteAll();
        gameEventRepository.deleteAll();
        matchCardRepository.deleteAll();
        planRepository.deleteAll();
        couponRepository.deleteAll();
        commentLikeRepository.deleteAll();
        commentRepository.deleteAll();
        notificationRepository.deleteAll();
        tickerMessageRepository.deleteAll();
    }
}
