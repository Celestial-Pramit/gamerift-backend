package com.gamerift.feature.user.repository;

import java.util.List;

import com.gamerift.feature.user.model.CountryCount;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

/**
 * Statistics queries on the users collection.
 *
 * An AGGREGATION PIPELINE is a list of steps that MongoDB runs on the server, like an assembly line:
 *   1. match   keep only users that have a country
 *   2. group   one row per country, counting the users ("count()")
 *   3. project rename the group key (_id) to "country"
 *   4. sort    biggest country first
 * Only the small result travels to our app, not every user.
 */
@Repository
public class UserStatsRepository {

    private final MongoTemplate mongoTemplate;

    public UserStatsRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<CountryCount> countUsersByCountry() {
        Aggregation pipeline = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("country").ne(null)),
                Aggregation.group("country").count().as("users"),
                Aggregation.project("users").and("country").previousOperation(),
                Aggregation.sort(Sort.Direction.DESC, "users"));

        AggregationResults<CountryCount> results = mongoTemplate.aggregate(pipeline, "users", CountryCount.class);
        return results.getMappedResults();
    }
}
