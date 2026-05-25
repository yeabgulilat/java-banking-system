package com.habeshabank.service;

import com.habeshabank.exception.AuthenticationException;
import com.habeshabank.exception.ValidationException;
import com.habeshabank.model.Account;
import com.habeshabank.model.User;
import com.habeshabank.model.UserSession;
import com.habeshabank.repository.AccountRepository;
import com.habeshabank.repository.SqliteAccountRepository;
import com.habeshabank.repository.SqliteUserRepository;
import com.habeshabank.repository.UserRepository;
import com.habeshabank.util.AccountNumberGenerator;
import com.habeshabank.util.PasswordUtil;

import java.util.Optional;

/**
 * Handles authentication, session lifecycle, and user/account registration.
 *
 * Phase 4 changes
 * ───────────────
 * • In-memory Maps replaced by {@link UserRepository} and {@link AccountRepository}.
 * • All user/account state is now read from and written to SQLite on every call.
 * • seedDemoUser() removed — seeding is handled by {@link com.habeshabank.database.DatabaseSeeder}.
 * • Public API (authenticate, logout, register) signatures are unchanged —
 *   no UI class needs modification.
 */
public class AuthService {

    // ── Repositories ──────────────────────────────────────────────────────────

    private final UserRepository    userRepo;
    private final AccountRepository accountRepo;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static AuthService instance;

    public static AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    private AuthService() {
        this.userRepo    = new SqliteUserRepository();
        this.accountRepo = new SqliteAccountRepository();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Validates credentials and populates the {@link UserSession} singleton.
     *
     * @param username the account number or username entered by the user
     * @param password the plain-text password (cleared from memory after use)
     * @throws AuthenticationException if credentials are wrong or account is locked
     * @throws ValidationException     if either field is blank
     */
    public void authenticate(String username, char[] password)
            throws AuthenticationException, ValidationException {

        // ── Input validation ──────────────────────────────────────────────────
        if (username == null || username.isBlank())
            throw new ValidationException("username", "Account number must not be empty.");
        if (password == null || password.length == 0)
            throw new ValidationException("password", "Password must not be empty.");

        String input = username.trim();

        // ── User lookup: try username first, then account number ──────────────
        User user = findUser(input)
                .orElseThrow(() -> new AuthenticationException(
                        "No account found for: " + input));

        if (!user.canLogin()) {
            String reason = user.isLocked()
                    ? "Your account has been locked after too many failed attempts. "
                      + "Please contact Habesha Bank support."
                    : "Your account is inactive. Please contact Habesha Bank.";
            throw new AuthenticationException(reason);
        }

        // ── Password check ────────────────────────────────────────────────────
        boolean match = PasswordUtil.verify(new String(password), user.getPasswordHash());
        clearPassword(password);

        if (!match) {
            user.recordFailedLogin();
            userRepo.update(user);          // persist lock state + failed count
            int remaining = Math.max(0, 5 - user.getFailedLoginCount());
            throw new AuthenticationException(
                    "Incorrect password. " + remaining + " attempt(s) remaining.");
        }

        user.recordSuccessfulLogin();
        userRepo.update(user);              // persist last_login_at + reset failed count

        // ── Load account ──────────────────────────────────────────────────────
        Account account = accountRepo.findByUserId(user.getId())
                .orElseThrow(() -> new AuthenticationException(
                        "No account linked to user: " + input));

        // ── Populate session ──────────────────────────────────────────────────
        populateSession(user, account);
    }

    /**
     * Clears the current session. Called by MainFrame logout.
     */
    public void logout() {
        UserSession.clearSession();
    }

    /**
     * Registers a new user and creates their default savings account.
     *
     * @return the generated account number
     * @throws ValidationException if any field is invalid or username is taken
     */
    public String register(String username, String plainPassword,
                           String fullName, String email, String phone)
            throws ValidationException {

        validateRegistrationFields(username, plainPassword, fullName, email);

        if (userRepo.existsByUsername(username.toLowerCase()))
            throw new ValidationException("username", "Username already taken: " + username);

        // ── Create and persist User ───────────────────────────────────────────
        User user = new User(username.toLowerCase(), fullName, email, phone);
        user.setPasswordHash(PasswordUtil.hash(plainPassword));
        userRepo.save(user);  // id assigned inside save()

        // ── Create and persist Account ────────────────────────────────────────
        String  accountNumber = AccountNumberGenerator.next();
        Account account = new Account(user.getId(), accountNumber,
                Account.AccountType.SAVINGS, 0.00);
        accountRepo.save(account);

        return accountNumber;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Looks up a user by username OR by account number — whichever matches first.
     * Mirrors LoginFrame's field label: "Account Number / Username".
     */
    private Optional<User> findUser(String input) {
        // 1. Direct username match
        Optional<User> byUsername = userRepo.findByUsername(input);
        if (byUsername.isPresent()) return byUsername;

        // 2. Treat input as account number → look up account → load user
        Optional<Account> byAccount = accountRepo.findByAccountNumber(input);
        if (byAccount.isPresent()) {
            return userRepo.findById(byAccount.get().getUserId());
        }

        return Optional.empty();
    }

    private void populateSession(User user, Account account) {
        UserSession session = UserSession.getInstance();
        session.setAccountNumber(account.getAccountNumber());
        session.setFullName(user.getFullName());
        session.setEmail(user.getEmail());
        session.setBalance(account.getBalance());
        session.setAccountType(account.getAccountType().getDisplayName());
        session.setLiveUser(user);
        session.setLiveAccount(account);
    }

    private void validateRegistrationFields(String username, String password,
                                            String fullName, String email)
            throws ValidationException {
        if (username == null || username.isBlank())
            throw new ValidationException("username", "Username is required.");
        if (password == null || password.isBlank())
            throw new ValidationException("password", "Password is required.");
        if (fullName == null || fullName.isBlank())
            throw new ValidationException("fullName", "Full name is required.");
        if (email    == null || email.isBlank())
            throw new ValidationException("email", "Email is required.");
        if (!email.contains("@"))
            throw new ValidationException("email", "Email address appears invalid.");
        if (password.length() < 6)
            throw new ValidationException("password", "Password must be at least 6 characters.");
    }

    private void clearPassword(char[] password) {
        if (password != null) java.util.Arrays.fill(password, '\0');
    }
}
