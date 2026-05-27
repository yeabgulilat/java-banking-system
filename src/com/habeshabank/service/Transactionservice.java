package com.habeshabank.service;

import com.habeshabank.exception.AccountNotFoundException;
import com.habeshabank.exception.BankingException;
import com.habeshabank.exception.InsufficientFundsException;
import com.habeshabank.exception.ValidationException;
import com.habeshabank.model.Account;
import com.habeshabank.model.Transaction;
import com.habeshabank.model.TransactionType;
import com.habeshabank.model.UserSession;
import com.habeshabank.repository.AccountRepository;
import com.habeshabank.repository.SqliteAccountRepository;
import com.habeshabank.repository.SqliteTransactionRepository;
import com.habeshabank.repository.TransactionRepository;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

/**
 * Executes all financial operations: deposit, withdraw, transfer, equb, iddir.
 *
 * Phase 4 changes
 * ───────────────
 * • In-memory List<Transaction> replaced by {@link TransactionRepository}.
 * • Account balance updates are persisted to SQLite after every operation via
 *   {@link AccountRepository#update(Account)}.
 * • The private {@code record()} method now calls
 *   {@link SqliteTransactionRepository#save(Transaction, String)} with the
 *   account number so the foreign key is stored correctly.
 * • All public method signatures, validation logic, and service behaviour
 *   are identical to Phase 3 — no UI class needs modification.
 * • seedDemoTransactions() was a no-op in Phase 3; removed entirely here.
 */
public class TransactionService {

    // ── Repositories ──────────────────────────────────────────────────────────

    private final SqliteTransactionRepository txRepo;
    private final AccountRepository           accountRepo;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static TransactionService instance;

    public static TransactionService getInstance() {
        if (instance == null) instance = new TransactionService();
        return instance;
    }

    private TransactionService() {
        this.txRepo      = new SqliteTransactionRepository();
        this.accountRepo = new SqliteAccountRepository();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Deposits funds into the session account.
     *
     * @param amount      amount in ETB, must be ≥ Account.MIN_DEPOSIT
     * @param description human-readable source description
     * @return the persisted Transaction
     */
    public synchronized Transaction deposit(double amount, String description)
            throws BankingException {

        validateAmount(amount, Account.MIN_DEPOSIT, Double.MAX_VALUE, "Deposit");

        Account account = resolveSessionAccount();
        account.credit(amount);
        accountRepo.update(account);        // persist new balance

        Transaction tx = record(TransactionType.DEPOSIT, amount,
                account.getBalance(), description, null, account);

        syncSession(account);
        return tx;
    }

    /**
     * Withdraws funds from the session account.
     *
     * @param amount      amount in ETB, must be ≥ Account.MIN_WITHDRAWAL
     * @param description withdrawal method description
     * @return the persisted Transaction
     */
    public synchronized Transaction withdraw(double amount, String description)
            throws BankingException {

        validateAmount(amount, Account.MIN_WITHDRAWAL, Account.COUNTER_WITHDRAWAL_MAX, "Withdrawal");

        Account account = resolveSessionAccount();
        if (!account.hasSufficientFunds(amount))
            throw new InsufficientFundsException(account.getBalance(), amount);

        account.debit(amount);
        accountRepo.update(account);

        Transaction tx = record(TransactionType.WITHDRAWAL, amount,
                account.getBalance(), description, null, account);

        syncSession(account);
        return tx;
    }

    /**
     * Transfers funds from the session account to a counterparty.
     *
     * Internal transfers (same bank): 0 ETB fee.
     * External transfers: Account.EXTERNAL_TRANSFER_FEE (25 ETB).
     *
     * @param amount           amount to send in ETB
     * @param recipientAccount destination account number
     * @param recipientName    beneficiary display name
     * @param description      transfer reference / narration
     * @param isInternal       true if the destination is also a Habesha Bank account
     * @return the persisted TRANSFER_OUT Transaction
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
        if (!account.hasSufficientFunds(total))
            throw new InsufficientFundsException(account.getBalance(), total);

        account.debit(total);
        accountRepo.update(account);

        String narration = (description != null && !description.isBlank())
                ? description
                : "Transfer to " + recipientName;
        if (fee > 0) narration += String.format(" (fee: %.2f ETB)", fee);

        Transaction tx = record(TransactionType.TRANSFER_OUT, amount,
                account.getBalance(), narration, recipientAccount, account);

        syncSession(account);
        return tx;
    }

    /**
     * Records an Equb round contribution.
     *
     * @param amount   contribution amount for this round
     * @param equbName name of the Equb group
     * @return the persisted Transaction
     */
    public synchronized Transaction equbContribution(double amount, String equbName)
            throws BankingException {

        validateAmount(amount, 1.00, Double.MAX_VALUE, "Equb contribution");
        validateField(equbName, "Equb group name");

        Account account = resolveSessionAccount();
        if (!account.hasSufficientFunds(amount))
            throw new InsufficientFundsException(account.getBalance(), amount);

        account.debit(amount);
        accountRepo.update(account);

        Transaction tx = record(TransactionType.EQUB, amount,
                account.getBalance(), "Equb contribution – " + equbName, null, account);

        syncSession(account);
        return tx;
    }

    /**
     * Records an Iddir mutual-aid contribution.
     *
     * @param amount      contribution amount
     * @param beneficiary name of the person being supported
     * @param occasion    type of occasion
     * @return the persisted Transaction
     */
    public synchronized Transaction iddirContribution(double amount,
                                                      String beneficiary,
                                                      String occasion)
            throws BankingException {

        validateAmount(amount, 1.00, Double.MAX_VALUE, "Iddir contribution");
        validateField(beneficiary, "Beneficiary name");

        Account account = resolveSessionAccount();
        if (!account.hasSufficientFunds(amount))
            throw new InsufficientFundsException(account.getBalance(), amount);

        account.debit(amount);
        accountRepo.update(account);

        String desc = "Iddir – " + occasion + " for " + beneficiary;
        Transaction tx = record(TransactionType.IDDIR, amount,
                account.getBalance(), desc, null, account);

        syncSession(account);
        return tx;
    }

    // ── Query API (called by UI panels via Refreshable.refreshData()) ─────────

    /**
     * Returns all transactions for the session account, most-recent first.
     * Reads directly from SQLite — always current.
     */
    public List<Transaction> getTransactionHistory() {
        String acctNum = sessionAccountNumber();
        if (acctNum == null) return Collections.emptyList();
        return txRepo.findByAccountNumber(acctNum);
    }

    /**
     * Returns the last {@code count} transactions for the session account.
     * Used by the dashboard Recent Transactions table.
     */
    public List<Transaction> getRecentTransactions(int count) {
        String acctNum = sessionAccountNumber();
        if (acctNum == null) return Collections.emptyList();
        return txRepo.findRecentByAccountNumber(acctNum, count);
    }

    /** Sums all DEPOSIT amounts in the current calendar month. */
    public synchronized double getTotalDepositsThisMonth() {
        String acctNum = sessionAccountNumber();
        if (acctNum == null) return 0.0;
        return txRepo.sumAmountByTypeAndMonth(acctNum, "DEPOSIT", YearMonth.now());
    }

    /** Sums all WITHDRAWAL amounts in the current calendar month. */
    public synchronized double getTotalWithdrawalsThisMonth() {
        String acctNum = sessionAccountNumber();
        if (acctNum == null) return 0.0;
        return txRepo.sumAmountByTypeAndMonth(acctNum, "WITHDRAWAL", YearMonth.now());
    }

    /** Counts all EQUB contribution transactions for the session account. */
    public synchronized long getEqubContributionCount() {
        String acctNum = sessionAccountNumber();
        if (acctNum == null) return 0L;
        return txRepo.countByType(acctNum, "EQUB");
    }

    /** Sums all IDDIR contribution amounts for the session account (all time). */
    public synchronized double getTotalIddirContributions() {
        String acctNum = sessionAccountNumber();
        if (acctNum == null) return 0.0;
        // Sum across all months by using countByType's sibling: query without month filter
        return txRepo.sumAllByType(acctNum, "IDDIR");
    }

    /**
     * Credits the session account when the user wins an Equb pot.
     * Called by EqubService after the winner is drawn.
     */
    public synchronized Transaction equbPotReceived(double potAmount, String groupName)
            throws BankingException {
        validateAmount(potAmount, 0.01, Double.MAX_VALUE, "Equb pot");
        Account account = resolveSessionAccount();
        account.credit(potAmount);
        accountRepo.update(account);
        Transaction tx = record(TransactionType.TRANSFER_IN, potAmount,
                account.getBalance(), "Equb pot received – " + groupName, null, account);
        syncSession(account);
        return tx;
    }

    /**
     * Credits the session account when Iddir funds are distributed to the user.
     * Called by IddirService after distribution is confirmed.
     */
    public synchronized Transaction iddirDistributionReceived(double amount, String occasion)
            throws BankingException {
        validateAmount(amount, 0.01, Double.MAX_VALUE, "Iddir distribution");
        Account account = resolveSessionAccount();
        account.credit(amount);
        accountRepo.update(account);
        Transaction tx = record(TransactionType.TRANSFER_IN, amount,
                account.getBalance(), "Iddir distribution – " + occasion, null, account);
        syncSession(account);
        return tx;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Builds a Transaction, persists it, and returns it.
     * Uses the SqliteTransactionRepository overload that accepts accountNumber
     * so the FK column is set correctly.
     */
    private Transaction record(TransactionType type,
                               double amount,
                               double balanceAfter,
                               String description,
                               String counterpartyAccount,
                               Account account) {

        Transaction tx = new Transaction(
                toLegacyType(type), amount, balanceAfter,
                description, counterpartyAccount);

        txRepo.save(tx, account.getAccountNumber());
        return tx;
    }

    /** Maps TransactionType → legacy Transaction.Type (UI uses the legacy enum). */
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
     * Falls back to a DB lookup if the session was populated via loadDemoUser()
     * (i.e. AuthService was bypassed).
     */
    private Account resolveSessionAccount() throws AccountNotFoundException {
        UserSession session = UserSession.getInstance();

        if (session.getLiveAccount() != null) {
            return session.getLiveAccount();
        }

        // Fallback: load from DB by account number
        String acctNum = session.getAccountNumber();
        if (acctNum != null) {
            Account account = accountRepo.findByAccountNumber(acctNum)
                    .orElseThrow(() -> new AccountNotFoundException(acctNum));
            session.setLiveAccount(account);
            return account;
        }

        throw new AccountNotFoundException("No active session account.");
    }

    /** Writes the account's current balance back to the flat session field for UI reads. */
    private void syncSession(Account account) {
        UserSession.getInstance().setBalance(account.getBalance());
    }

    /** Returns the session account number, or null if no session is active. */
    private String sessionAccountNumber() {
        UserSession session = UserSession.getInstance();
        if (session.getLiveAccount() != null)
            return session.getLiveAccount().getAccountNumber();
        return session.getAccountNumber();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateAmount(double amount, double min, double max, String context)
            throws ValidationException {
        if (amount <= 0)
            throw new ValidationException("amount", context + " amount must be positive.");
        if (amount < min)
            throw new ValidationException("amount",
                    String.format("Minimum %s is %.2f ETB.", context.toLowerCase(), min));
        if (amount > max)
            throw new ValidationException("amount",
                    String.format("Maximum %s is %.2f ETB.", context.toLowerCase(), max));
    }

    private void validateField(String value, String fieldName) throws ValidationException {
        if (value == null || value.isBlank())
            throw new ValidationException(fieldName + " is required.");
    }
}