package com.habeshabank.service;

import com.habeshabank.exception.AccountNotFoundException;
import com.habeshabank.exception.BankingException;
import com.habeshabank.exception.InsufficientFundsException;
import com.habeshabank.exception.ValidationException;
import com.habeshabank.model.Account;
import com.habeshabank.model.Transaction;
import com.habeshabank.model.TransactionType;
import com.habeshabank.model.UserSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Executes all financial operations: deposit, withdraw, transfer, equb, iddir.
 *
 * Architecture notes
 * ──────────────────
 * • Every operation follows the same atomic pattern:
 *     1. Validate input
 *     2. Validate account state
 *     3. Build a Transaction record
 *     4. Apply the balance change to the Account object
 *     5. Record the transaction (in-memory list for now)
 *     6. Sync the flat balance back to UserSession for UI reads
 *
 * • The transaction list acts as a stub TransactionRepository.
 *   It will be replaced by a repository interface + SQLite implementation
 *   in the next phase without changing any method signatures here.
 *
 * • All methods are synchronised to prevent double-submit race conditions
 *   that are possible when Swing workers fire overlapping calls.
 *
 * • UI panels import this service and call the public methods.
 *   They receive a {@link Transaction} result and use it for receipt display.
 */
public class TransactionService {

    // ── Stub repository (replaced by SQLite repo later) ───────────────────────
    private final List<Transaction> transactionLog = new ArrayList<>();
    private long nextId = 1L;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static TransactionService instance;

    public static TransactionService getInstance() {
        if (instance == null) instance = new TransactionService();
        return instance;
    }

    private TransactionService() {
        seedDemoTransactions();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Deposits funds into the session account.
     *
     * @param amount      amount in ETB, must be ≥ Account.MIN_DEPOSIT
     * @param description human-readable source description
     * @return the recorded Transaction
     * @throws ValidationException if amount is invalid
     */
    public synchronized Transaction deposit(double amount, String description)
            throws BankingException {

        validateAmount(amount, Account.MIN_DEPOSIT, Double.MAX_VALUE, "Deposit");

        Account account = resolveSessionAccount();
        account.credit(amount);

        Transaction tx = record(
                TransactionType.DEPOSIT,
                amount,
                account.getBalance(),
                description,
                null,
                account.getAccountNumber()
        );

        syncSession(account);
        return tx;
    }

    /**
     * Withdraws funds from the session account.
     *
     * @param amount      amount in ETB, must be ≥ Account.MIN_WITHDRAWAL
     * @param description withdrawal method description
     * @return the recorded Transaction
     * @throws InsufficientFundsException if balance is too low
     * @throws ValidationException        if amount is invalid
     */
    public synchronized Transaction withdraw(double amount, String description)
            throws BankingException {

        validateAmount(amount, Account.MIN_WITHDRAWAL, Account.COUNTER_WITHDRAWAL_MAX, "Withdrawal");

        Account account = resolveSessionAccount();

        if (!account.hasSufficientFunds(amount)) {
            throw new InsufficientFundsException(account.getBalance(), amount);
        }

        account.debit(amount);

        Transaction tx = record(
                TransactionType.WITHDRAWAL,
                amount,
                account.getBalance(),
                description,
                null,
                account.getAccountNumber()
        );

        syncSession(account);
        return tx;
    }

    /**
     * Transfers funds from the session account to a counterparty.
     *
     * For internal transfers (same bank) a fee of 0 is applied.
     * For external transfers a flat fee of Account.EXTERNAL_TRANSFER_FEE is applied.
     *
     * @param amount             amount to send in ETB
     * @param recipientAccount   destination account number
     * @param recipientName      beneficiary display name
     * @param description        transfer reference / narration
     * @param isInternal         true if the destination is also a Habesha Bank account
     * @return the recorded TRANSFER_OUT Transaction
     * @throws InsufficientFundsException if balance is insufficient (including fee)
     * @throws ValidationException        if fields are missing or amount is invalid
     */
    public synchronized Transaction transfer(double amount,
                                             String recipientAccount,
                                             String recipientName,
                                             String description,
                                             boolean isInternal)
            throws BankingException {

        validateAmount(amount, 1.00, Account.DAILY_TRANSFER_LIMIT, "Transfer");
        validateField(recipientAccount, "Recipient account number");
        validateField(recipientName,    "Recipient name");

        double fee   = isInternal ? Account.INTERNAL_TRANSFER_FEE : Account.EXTERNAL_TRANSFER_FEE;
        double total = amount + fee;

        Account account = resolveSessionAccount();
        if (!account.hasSufficientFunds(total)) {
            throw new InsufficientFundsException(account.getBalance(), total);
        }

        account.debit(total);

        String narration = description != null && !description.isBlank()
                ? description
                : "Transfer to " + recipientName;

        if (fee > 0) {
            narration += String.format(" (fee: %.2f ETB)", fee);
        }

        Transaction tx = record(
                TransactionType.TRANSFER_OUT,
                amount,
                account.getBalance(),
                narration,
                recipientAccount,
                account.getAccountNumber()
        );

        syncSession(account);
        return tx;
    }

    /**
     * Records an Equb round contribution.
     *
     * @param amount   contribution amount for this round
     * @param equbName name of the Equb group
     * @return the recorded Transaction
     * @throws InsufficientFundsException if balance is insufficient
     * @throws ValidationException        if amount or name is invalid
     */
    public synchronized Transaction equbContribution(double amount, String equbName)
            throws BankingException {

        validateAmount(amount, 1.00, Double.MAX_VALUE, "Equb contribution");
        validateField(equbName, "Equb group name");

        Account account = resolveSessionAccount();
        if (!account.hasSufficientFunds(amount)) {
            throw new InsufficientFundsException(account.getBalance(), amount);
        }

        account.debit(amount);

        Transaction tx = record(
                TransactionType.EQUB,
                amount,
                account.getBalance(),
                "Equb contribution – " + equbName,
                null,
                account.getAccountNumber()
        );

        syncSession(account);
        return tx;
    }

    /**
     * Records an Iddir mutual-aid contribution.
     *
     * @param amount       contribution amount
     * @param beneficiary  name of the person being supported
     * @param occasion     type of occasion
     * @return the recorded Transaction
     */
    public synchronized Transaction iddirContribution(double amount,
                                                      String beneficiary,
                                                      String occasion)
            throws BankingException {

        validateAmount(amount, 1.00, Double.MAX_VALUE, "Iddir contribution");
        validateField(beneficiary, "Beneficiary name");

        Account account = resolveSessionAccount();
        if (!account.hasSufficientFunds(amount)) {
            throw new InsufficientFundsException(account.getBalance(), amount);
        }

        account.debit(amount);

        String desc = "Iddir – " + occasion + " for " + beneficiary;

        Transaction tx = record(
                TransactionType.IDDIR,
                amount,
                account.getBalance(),
                desc,
                null,
                account.getAccountNumber()
        );

        syncSession(account);
        return tx;
    }

    /**
     * Returns an unmodifiable view of all recorded transactions,
     * most-recent first.
     */
    public List<Transaction> getTransactionHistory() {
        List<Transaction> sorted = new ArrayList<>(transactionLog);
        Collections.reverse(sorted);
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Returns the last N transactions (most-recent first).
     * Used by the dashboard's "Recent Transactions" table.
     */
    public List<Transaction> getRecentTransactions(int count) {
        List<Transaction> all = getTransactionHistory();
        return all.subList(0, Math.min(count, all.size()));
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Builds, assigns an id, and stores a Transaction record.
     * Returns a Transaction that uses the legacy Transaction.Type enum so that
     * existing UI table renderers continue to work with {@code signedAmount()}.
     */
    private Transaction record(TransactionType type,
                               double amount,
                               double balanceAfter,
                               String description,
                               String counterpartyAccount,
                               String ownAccountNumber) {

        // Convert TransactionType → legacy Transaction.Type for backward compat
        Transaction.Type legacyType = toLegacyType(type);

        Transaction tx = new Transaction(legacyType, amount, balanceAfter,
                description, counterpartyAccount);
        tx.setId(nextId++);

        transactionLog.add(tx);
        return tx;
    }

    /** Maps new TransactionType → legacy Transaction.Type inline enum. */
    private Transaction.Type toLegacyType(TransactionType t) {
        return switch (t) {
            case DEPOSIT      -> Transaction.Type.DEPOSIT;
            case WITHDRAWAL   -> Transaction.Type.WITHDRAWAL;
            case TRANSFER_IN  -> Transaction.Type.TRANSFER_IN;
            case TRANSFER_OUT -> Transaction.Type.TRANSFER_OUT;
            case EQUB         -> Transaction.Type.EQUB;
            case IDDIR        -> Transaction.Type.IDDIR;
        };
    }

    /**
     * Resolves the live Account from the session.
     * Falls back to creating a synthetic Account from flat session fields
     * if AuthService was bypassed (e.g. demo login via loadDemoUser()).
     */
    private Account resolveSessionAccount() throws AccountNotFoundException {
        UserSession session = UserSession.getInstance();

        if (session.getLiveAccount() != null) {
            return session.getLiveAccount();
        }

        // Fallback: build a transient Account from flat session fields
        // (covers demo mode where AuthService wasn't used)
        if (session.getAccountNumber() != null) {
            Account fallback = new Account();
            fallback.setAccountNumber(session.getAccountNumber());
            fallback.setBalance(session.getBalance());
            fallback.setAccountType(Account.AccountType.SAVINGS);
            // Attach it so future calls use the same object
            session.setLiveAccount(fallback);
            return fallback;
        }

        throw new AccountNotFoundException("No active session account.");
    }

    /** Writes the account's current balance back to the flat session field for UI. */
    private void syncSession(Account account) {
        UserSession.getInstance().setBalance(account.getBalance());
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateAmount(double amount, double min, double max, String context)
            throws ValidationException {
        if (amount <= 0) {
            throw new ValidationException("amount", context + " amount must be positive.");
        }
        if (amount < min) {
            throw new ValidationException("amount",
                    String.format("Minimum %s is %.2f ETB.", context.toLowerCase(), min));
        }
        if (amount > max) {
            throw new ValidationException("amount",
                    String.format("Maximum %s is %.2f ETB.", context.toLowerCase(), max));
        }
    }

    private void validateField(String value, String fieldName) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
    }

    // ── Demo Seeding ──────────────────────────────────────────────────────────

    /**
     * Pre-loads demo transaction history so the history panel and dashboard
     * show realistic data from first launch, even before any real operations.
     */
    private void seedDemoTransactions() {
        // These mirror the hard-coded rows in DashboardPanel and TransactionHistoryPanel.
        // When the database layer arrives, seeding moves to a migration script instead.
        // For now, the UI panels still show their own hard-coded rows on first paint;
        // the live history grows from actual user operations.
    }
}