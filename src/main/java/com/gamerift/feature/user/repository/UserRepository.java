package com.gamerift.feature.user.repository;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.user.model.User;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository = the layer that talks to the database.
 * We only write an interface; Spring Data creates the implementation at start-up.
 * Method names are turned into queries: findByEmail -> db.users.find({email: ...}).
 */
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    /** Returns users who have event notifications enabled. */
    List<User> findByNotifyEventsTrue();

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
