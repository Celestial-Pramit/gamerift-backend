package com.gamerift.feature.stream.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * A repository for the Discover page query. The filters are optional, so a method name like
 * findByXAndY... cannot express it. Here we build the MongoDB query ourselves with Criteria.
 * Every filter that is given adds one condition; all conditions are combined with AND.
 */
@Repository
public class StreamSearchRepository {

    private final MongoTemplate mongoTemplate;

    public StreamSearchRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /** Any parameter may be null / 0 / empty = "no filter". Only published streams are ever returned. */
    public Page<LiveStream> search(String text, String categorySlug, List<String> tags, int minViewers,
                                   StreamPhase phase, Pageable pageable) {
        List<Criteria> conditions = new ArrayList<>();
        conditions.add(Criteria.where("published").is(true));

        if (phase != null) {
            conditions.add(Criteria.where("phase").is(phase));
        }
        if (categorySlug != null && !categorySlug.isBlank()) {
            conditions.add(Criteria.where("categorySlug").is(categorySlug.trim().toLowerCase()));
        }
        if (tags != null && !tags.isEmpty()) {
            // the stream must have ALL the given tags
            conditions.add(Criteria.where("tags").all(tags));
        }
        if (minViewers > 0) {
            conditions.add(Criteria.where("baseViewers").gte(minViewers));
        }
        if (text != null && !text.isBlank()) {
            // Pattern.quote: the user's text is searched literally (no regex tricks); "i" = ignore case
            String pattern = Pattern.quote(text.trim());
            conditions.add(new Criteria().orOperator(
                    Criteria.where("title").regex(pattern, "i"),
                    Criteria.where("channelName").regex(pattern, "i"),
                    Criteria.where("tags").regex(pattern, "i")));
        }

        Query query = new Query(new Criteria().andOperator(conditions.toArray(new Criteria[0])));
        // 1. count everything that matches (before paging), 2. then load just one page
        long total = mongoTemplate.count(query, LiveStream.class);
        query.with(pageable);
        List<LiveStream> items = mongoTemplate.find(query, LiveStream.class);
        return new PageImpl<>(items, pageable, total);
    }
}
