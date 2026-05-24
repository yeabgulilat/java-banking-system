package com.habeshabank.service;

import com.habeshabank.exception.AuthenticationException;
import com.habeshabank.exception.ValidationException;
import com.habeshabank.model.Account;
import com.habeshabank.model.User;
import com.habeshabank.model.UserSession;
import com.habeshabank.util.AccountNumberGenerator;
import com.habeshabank.util.PasswordUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles authentication, session lifecycle, and user/account registration.
 *
 * Architecture notes
 * ──────────────────
 * • In this phase, users and accounts are held in in-memory Maps that act as
 *   stub repositories.  Each Map will be replaced by a repository interface
 *   backed by SQLite in the next phase — the method signatures here will not
 *   change.
 * • AuthService is the sole entry point for login/logout.  UI classes call
 *   {@link #authenticate} and read the result from {@link UserSession}.
 * • Password hashing is delegated to {@link PasswordUtil} — AuthService never
 *   handles plain-text passwords beyond the single call to verify().
 *
 * Demo seeding
 * ────────────
 * The constructor pre-registers one demo user so the existing LoginFrame demo
 * hint ("any account number + any password") keeps working during development.
 */
public class AuthService {

    // ── Stub repositories (replaced by SQLite repos later) ────────────────────

    private final Map<String, User>    usersByUsername = new HashMap<>();
    private final Map<String, Account> accountsByNumber = new HashMap<>();
    private final Map<Long, Account>   accountsByUserId = new HashMap<>();

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static AuthService instance;

    public static AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    private AuthService() {
        seedDemoUser();
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
        if (username == null || username.isBlank()) {
            throw new ValidationException("username", "Account number must not be empty.");
        }
        if (password == null || password.length == 0) {
            throw new ValidationException("password", "Password must not be empty.");
        }

        // ── User lookup ───────────────────────────────────────────────────────
        User user = findUser(username.trim())
                .orElseThrow(() -> new AuthenticationException(
                        "No account found for: " + username));

        if (!user.canLogin()) {
            String reason = user.isLocked()
                    ? "Your account has been locked after too many failed attempts. Please contact support."
                    : "Your account is inactive. Please contact Habesha Bank.";
            throw new AuthenticationException(reason);
        }

        // ── Password check ────────────────────────────────────────────────────
        boolean match = PasswordUtil.verify(new String(password), user.getPasswordHash());
        clearPassword(password);          // wipe from memory immediately

        if (!match) {
            user.recordFailedLogin();
            int remaining = Math.max(0, 5 - user.getFailedLoginCount());
            throw new AuthenticationException(
                    "Incorrect password. " + remaining + " attempt(s) remaining.");
        }

        user.recordSuccessfulLogin();

        // ── Load account ──────────────────────────────────────────────────────
        Account account = accountsByUserId.get(user.getId());
        if (account == null) {
            throw new AuthenticationException("No account linked to user: " + username);
        }

        // ── Populate session ──────────────────────────────────────────────────
        populateSession(user, account);
    }

    /**
     * Clears the current session.
     * Called by MainFrame logout action — no change to existing UI flow needed.
     */
    public void logout() {
        UserSession.clearSession();
    }

    /**
     * Registers a new user and creates their default savings account.
     * Returns the generated account number.
     */
    public String register(String username, String plainPassword,
                           String fullName, String email, String phone)
            throws ValidationException {

        validateRegistrationFields(username, plainPassword, fullName, email);

        if (usersByUsername.containsKey(username.toLowerCase())) {
            throw new ValidationException("username", "Username already taken: " + username);
        }

        // Create User
        User user = new User(username.toLowerCase(), fullName, email, phone);
        user.setId(usersByUsername.size() + 1L);
        user.setPasswordHash(PasswordUtil.hash(plainPassword));
        usersByUsername.put(user.getUsername(), user);

        // Create default savings account
        String  accountNumber = AccountNumberGenerator.next();
        Account account       = new Account(
                user.getId(), accountNumber,
                Account.AccountType.SAVINGS, 0.00);
        account.setId(account.getUserId());
        accountsByNumber.put(accountNumber, account);
        accountsByUserId.put(user.getId(), account);

        return accountNumber;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Looks up a user by username OR by account number — whichever matches.
     * This mirrors the existing LoginFrame behaviour where the field label
     * reads "Account Number / Username".
     */
    private Optional<User> findUser(String input) {
        // Try direct username match first
        User byUsername = usersByUsername.get(input.toLowerCase());
        if (byUsername != null) return Optional.of(byUsername);

        // Fall back to account number lookup
        Account account = accountsByNumber.get(input);
        if (account != null) {
            return usersByUsername.values().stream()
                    .filter(u -> u.getId() == account.getUserId())
                    .findFirst();
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

        // Store refs for TransactionService to update the live Account object
        session.setLiveUser(user);
        session.setLiveAccount(account);
    }

    private void validateRegistrationFields(String username, String password,
                                            String fullName, String email)
            throws ValidationException {
        if (username  == null || username.isBlank())  throw new ValidationException("username",  "Username is required.");
        if (password  == null || password.isBlank())  throw new ValidationException("password",  "Password is required.");
        if (fullName  == null || fullName.isBlank())  throw new ValidationException("fullName",  "Full name is required.");
        if (email     == null || email.isBlank())     throw new ValidationException("email",     "Email is required.");
        if (!email.contains("@"))                     throw new ValidationException("email",     "Email address appears invalid.");
        if (password.length() < 6)                    throw new ValidationException("password",  "Password must be at least 6 characters.");
    }

    private void clearPassword(char[] password) {
        if (password != null) java.util.Arrays.fill(password, '\0');
    }

    // ── Demo Seeding ──────────────────────────────────────────────────────────

    /**
     * Pre-registers the demo user used during UI development.
     * Account: ETH-2024-00142  /  Password: demo1234
     *
     * The LoginFrame still shows the hint "Demo: any account number + any password"
     * but now routes through real AuthService logic; only the seeded credentials
     * will succeed unless additional users are registered.
     *
     * During development you can temporarily widen this to accept any input by
     * calling {@link UserSession#loadDemoUser()} directly from LoginFrame —
     * that path still compiles and works unchanged.
     */
    private void seedDemoUser() {
        // Seed with a fixed account number so existing demo data in UI matches
        AccountNumberGenerator.seed(141L);   // next() will return ETH-{year}-00142

        String accountNumber = AccountNumberGenerator.next();  // ETH-2024-00142 equivalent

        User demo = new User("tigist.alemu", "Tigist Alemu",
                "tigist.alemu@habeshabank.et", "+251911000001");
        demo.setId(1L);
        demo.setPasswordHash(PasswordUtil.hash("demo1234"));
        usersByUsername.put(demo.getUsername(), demo);
        // also index by account number for lookup
        usersByUsername.put(accountNumber.toLowerCase(), demo);

        Account acc = new Account(1L, accountNumber,
                Account.AccountType.PREMIUM_SAVINGS, 47_850.00);
        acc.setId(1L);
        accountsByNumber.put(accountNumber, acc);
        accountsByUserId.put(1L, acc);
    }
}