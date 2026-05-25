package com.habeshabank.repository;

import com.habeshabank.model.Transaction;

import java.time.YearMonth;
import java.util.List;

/**
 * Contract for all Transaction persistence operations.
 *
 * TransactionService depends on this interface.
 */
public interface TransactionRepository {

    /**
     * Persists a new transaction and sets the generated id on the returned object.
     *
     * @param transaction fully populated Transaction (without database id)
     * @return the same Transaction with id set
     */
    Transaction save(Transaction transaction);

    /**
     * Returns all transactions for a given account number, most-recent first.
     *
     * @param accountNumber the account whose history to retrieve
     * @return ordered list; empty list if none found
     */
    List<Transaction> findByAccountNumber(String accountNumber);

    /**
     * Returns the most recent {@code limit} transactions for an account,
     * most-recent first. Used by the dashboard.
     */
    List<Transaction> findRecentByAccountNumber(String accountNumber, int limit);

    /**
     * Sums the amount of all transactions of a given type for an account
     * within a specific calendar month.
     *
     * @param accountNumber the account to query
     * @param type          the transaction type (e.g. "DEPOSIT")
     * @param month         the YearMonth to filter by
     * @return sum of amounts; 0.0 if no matching transactions
     */
    double sumAmountByTypeAndMonth(String accountNumber,
                                   String type,
                                   YearMonth month);

    /**
     * Sums the amount of ALL transactions of a given type for an account,
     * regardless of date. Used for all-time totals (e.g. Iddir contributions).
     */
    double sumAllByType(String accountNumber, String type);

    /**
     * Counts all transactions of a given type for an account.
     * Used by the dashboard equb contribution counter.
     */
    long countByType(String accountNumber, String type);
}
