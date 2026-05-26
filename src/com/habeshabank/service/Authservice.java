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
 * Handles authentication, session lifecycle, user/account registration,
 * and PIN management.
 *
 * Phase 6 additions
 * ─────────────────
 * • register() now accepts a plainPin parameter (4 digits) and hashes it
 *   into User.pinHash before persisting.
 * • verifyPin(char[]) — called by WithdrawPanel before executing a withdrawal.
 *   Reads the live session user's pinHash; returns false on mismatch.
 *   Does NOT lock the account on PIN failure (different policy from password).
 * • changePin(char[] oldPin, char[] newPin) — called by SettingsPanel.
 *   Verifies old PIN first, then replaces the hash and persists.
 * • All Phase 4/5 signatures unchanged — no existing call-sites break.
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

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Validates credentials and populates {@link UserSession}.
     * Signature unchanged from Phase 4/5.
     */
    public void authenticate(String username, char[] password)
            throws AuthenticationException, ValidationException {

        if (username == null || username.isBlank())
            throw new ValidationException("username", "Account number must not be empty.");
        if (password == null || password.length == 0)
            throw new ValidationException("password", "Password must not be empty.");

        String input = username.trim();

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

        boolean match = PasswordUtil.verify(new String(password), user.getPasswordHash());
        clearChars(password);

        if (!match) {
            user.recordFailedLogin();
            userRepo.update(user);
            int remaining = Math.max(0, 5 - user.getFailedLoginCount());
            throw new AuthenticationException(
                    "Incorrect password. " + remaining + " attempt(s) remaining.");
        }

        user.recordSuccessfulLogin();
        userRepo.update(user);

        Account account = accountRepo.findByUserId(user.getId())
                .orElseThrow(() -> new AuthenticationException(
                        "No account linked to user: " + input));

        populateSession(user, account);
    }

    /** Clears the current session. Called by MainFrame logout and session timeout. */
    public void logout() {
        UserSession.clearSession();
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Registers a new user with a hashed password and hashed PIN.
     *
     * Phase 6: added plainPin parameter.
     * Old call-site in RegisterFrame is updated to pass the PIN.
     *
     * @param plainPin 4-digit PIN string
     * @return the generated account number
     */
    public String register(String username, String plainPassword, String plainPin,
                           String fullName, String email, String phone)
            throws ValidationException {

        validateRegistrationFields(username, plainPassword, plainPin, fullName, email);

        if (userRepo.existsByUsername(username.toLowerCase()))
            throw new ValidationException("username", "Username already taken: " + username);

        User user = new User(username.toLowerCase(), fullName, email, phone);
        user.setPasswordHash(PasswordUtil.hash(plainPassword));
        user.setPinHash(PasswordUtil.hash(plainPin));
        userRepo.save(user);

        String  accountNumber = AccountNumberGenerator.next();
        Account account = new Account(user.getId(), accountNumber,
                Account.AccountType.SAVINGS, 0.00);
        accountRepo.save(account);

        return accountNumber;
    }

    // ── PIN Operations ────────────────────────────────────────────────────────

    /**
     * Verifies the supplied PIN against the session user's stored hash.
     *
     * Called by WithdrawPanel before executing a withdrawal.
     * Does NOT increment failedLoginCount — PIN failures do not lock the account.
     *
     * @param pin plain-text PIN chars (cleared after verification)
     * @return true if the PIN matches, false otherwise
     * @throws AuthenticationException if no session user is available
     */
    public boolean verifyPin(char[] pin) throws AuthenticationException {
        User user = resolveSessionUser();

        if (user.getPinHash() == null) {
            clearChars(pin);
            // No PIN set — this shouldn't happen after Phase 6 seeding,
            // but if it does we fail safely rather than allowing open access.
            throw new AuthenticationException(
                    "No PIN is set for this account. Please set a PIN in Settings.");
        }

        boolean match = PasswordUtil.verify(new String(pin), user.getPinHash());
        clearChars(pin);
        return match;
    }

    /**
     * Changes the PIN for the session user.
     *
     * Called by SettingsPanel. Verifies the old PIN first.
     *
     * @param oldPin    current plain-text PIN
     * @param newPin    new plain-text PIN
     * @param newPinConfirm must equal newPin exactly
     * @throws AuthenticationException if the old PIN does not match
     * @throws ValidationException     if the new PIN is invalid or confirmation mismatches
     */
    public void changePin(char[] oldPin, char[] newPin, char[] newPinConfirm)
            throws AuthenticationException, ValidationException {

        // ── Validate new PIN format ───────────────────────────────────────────
        String newPinStr     = new String(newPin);
        String confirmPinStr = new String(newPinConfirm);

        if (newPinStr.length() != 4 || !newPinStr.matches("\\d{4}")) {
            clearChars(oldPin); clearChars(newPin); clearChars(newPinConfirm);
            throw new ValidationException("newPin", "PIN must be exactly 4 digits.");
        }
        if (!newPinStr.equals(confirmPinStr)) {
            clearChars(oldPin); clearChars(newPin); clearChars(newPinConfirm);
            throw new ValidationException("confirmPin", "PINs do not match.");
        }

        // ── Verify old PIN ────────────────────────────────────────────────────
        boolean oldMatch = verifyPin(oldPin);  // clears oldPin internally
        if (!oldMatch) {
            clearChars(newPin); clearChars(newPinConfirm);
            throw new AuthenticationException("Current PIN is incorrect.");
        }

        // ── Hash and persist new PIN ──────────────────────────────────────────
        User user = resolveSessionUser();
        user.setPinHash(PasswordUtil.hash(newPinStr));
        userRepo.update(user);

        clearChars(newPin);
        clearChars(newPinConfirm);

        System.out.println("[AuthService] PIN changed for user: " + user.getUsername());
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Optional<User> findUser(String input) {
        Optional<User> byUsername = userRepo.findByUsername(input);
        if (byUsername.isPresent()) return byUsername;

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

    /**
     * Resolves the live User from the session.
     * If only a username is available (demo mode), re-loads from the DB.
     */
    private User resolveSessionUser() throws AuthenticationException {
        UserSession session = UserSession.getInstance();

        if (session.getLiveUser() != null) return session.getLiveUser();

        // Fallback: re-load from DB by account number
        if (session.getAccountNumber() != null) {
            Optional<Account> acc = accountRepo.findByAccountNumber(session.getAccountNumber());
            if (acc.isPresent()) {
                Optional<User> user = userRepo.findById(acc.get().getUserId());
                if (user.isPresent()) {
                    session.setLiveUser(user.get());
                    return user.get();
                }
            }
        }

        throw new AuthenticationException("No active session. Please log in again.");
    }

    private void validateRegistrationFields(String username, String password,
                                            String pin, String fullName, String email)
            throws ValidationException {
        if (username == null || username.isBlank())
            throw new ValidationException("username", "Username is required.");
        if (password == null || password.isBlank())
            throw new ValidationException("password", "Password is required.");
        if (password.length() < 6)
            throw new ValidationException("password", "Password must be at least 6 characters.");
        if (pin == null || !pin.matches("\\d{4}"))
            throw new ValidationException("pin", "PIN must be exactly 4 digits.");
        if (fullName == null || fullName.isBlank())
            throw new ValidationException("fullName", "Full name is required.");
        if (email == null || email.isBlank())
            throw new ValidationException("email", "Email is required.");
        if (!email.contains("@"))
            throw new ValidationException("email", "Email address appears invalid.");
    }

    private void clearChars(char[] chars) {
        if (chars != null) java.util.Arrays.fill(chars, '\0');
    }
}
