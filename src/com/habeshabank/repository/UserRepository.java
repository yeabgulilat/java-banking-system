package com.habeshabank.repository;

import com.habeshabank.model.User;

import java.util.Optional;

/**
 * Contract for all User persistence operations.
 *
 * AuthService depends on this interface, not on any concrete implementation.
 * Swapping SQLite for another database in the future requires only a new
 * implementation class — AuthService is untouched.
 */
public interface UserRepository {

    /**
     * Persists a new user and sets the generated id on the returned object.
     *
     * @param user a fully populated User (without id — it is assigned here)
     * @return the same User with id set
     */
    User save(User user);

    /**
     * Persists changes to an existing user (lock state, login timestamps, etc.).
     * The user must already have a valid id.
     */
    void update(User user);

    /**
     * Finds a user by their login username (case-insensitive).
     *
     * @return Optional.empty() if no match
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by their primary key id.
     *
     * @return Optional.empty() if no match
     */
    Optional<User> findById(long id);

    /**
     * Returns true if the username is already taken (case-insensitive).
     * Used during registration validation.
     */
    boolean existsByUsername(String username);
}
