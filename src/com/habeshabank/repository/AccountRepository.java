package com.habeshabank.repository;

import com.habeshabank.model.Account;

import java.util.Optional;

/**
 * Contract for all Account persistence operations.
 *
 * TransactionService and AuthService depend on this interface.
 */
public interface AccountRepository {

    /**
     * Persists a new account and sets the generated id on the returned object.
     *
     * @param account a fully populated Account (without id)
     * @return the same Account with id set
     */
    Account save(Account account);

    /**
     * Persists balance changes and status updates to an existing account.
     * The account must already have a valid id.
     */
    void update(Account account);

    /**
     * Finds an account by its formatted account number (e.g. "ETH-2026-00142").
     *
     * @return Optional.empty() if no match
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Finds the primary account belonging to a user.
     * In this version each user has exactly one account.
     *
     * @return Optional.empty() if no account is linked to this user id
     */
    Optional<Account> findByUserId(long userId);

    /**
     * Returns true if the account number is already in use.
     * Used during account creation to guarantee uniqueness.
     */
    boolean existsByAccountNumber(String accountNumber);
}
