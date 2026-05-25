package com.habeshabank.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Core domain entity representing a registered bank customer.
 *
 * Encapsulates identity, credentials, and status.  Password is stored as a
 * hash string — plain-text passwords are never held in this object after
 * initial validation.
 *
 * Designed for future persistence via a UserRepository interface; no database
 * or UI dependencies exist in this class.
 */
public class User {

    // ── Identity ──────────────────────────────────────────────────────────────

    private long          id;
    private String        username;          // used at login (unique)
    private String        passwordHash;      // BCrypt / SHA-256 hash (set by AuthService)
    private String        fullName;
    private String        email;
    private String        phoneNumber;
    private LocalDate     dateOfBirth;
    private String        nationalIdNumber;  // Ethiopian Fayda ID or passport

    // ── Status ────────────────────────────────────────────────────────────────

    private boolean       active           = true;
    private boolean       locked           = false;
    private int           failedLoginCount = 0;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // ── Constructor ───────────────────────────────────────────────────────────

    public User() {
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String fullName, String email, String phoneNumber) {
        this();
        this.username    = username;
        this.fullName    = fullName;
        this.email       = email;
        this.phoneNumber = phoneNumber;
    }

    // ── Business Rules ────────────────────────────────────────────────────────

    /** Returns true if this account can attempt login. */
    public boolean canLogin() {
        return active && !locked;
    }

    /** Called by AuthService on each failed attempt. Auto-locks after 5. */
    public void recordFailedLogin() {
        this.failedLoginCount++;
        if (this.failedLoginCount >= 5) {
            this.locked = true;
        }
    }

    /** Called by AuthService on successful login. */
    public void recordSuccessfulLogin() {
        this.failedLoginCount = 0;
        this.lastLoginAt      = LocalDateTime.now();
    }

    /** Administrative unlock. */
    public void unlock() {
        this.locked           = false;
        this.failedLoginCount = 0;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public long          getId()               { return id; }
    public String        getUsername()         { return username; }
    public String        getPasswordHash()     { return passwordHash; }
    public String        getFullName()         { return fullName; }
    public String        getEmail()            { return email; }
    public String        getPhoneNumber()      { return phoneNumber; }
    public LocalDate     getDateOfBirth()      { return dateOfBirth; }
    public String        getNationalIdNumber() { return nationalIdNumber; }
    public boolean       isActive()            { return active; }
    public boolean       isLocked()            { return locked; }
    public int           getFailedLoginCount() { return failedLoginCount; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public LocalDateTime getLastLoginAt()      { return lastLoginAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(long id)                             { this.id = id; }
    public void setUsername(String username)               { this.username = username; }
    public void setPasswordHash(String passwordHash)       { this.passwordHash = passwordHash; }
    public void setFullName(String fullName)               { this.fullName = fullName; }
    public void setEmail(String email)                     { this.email = email; }
    public void setPhoneNumber(String phoneNumber)         { this.phoneNumber = phoneNumber; }
    public void setDateOfBirth(LocalDate dateOfBirth)      { this.dateOfBirth = dateOfBirth; }
    public void setNationalIdNumber(String nationalIdNumber){ this.nationalIdNumber = nationalIdNumber; }
    public void setActive(boolean active)                  { this.active = active; }
    public void setLocked(boolean locked)                  { this.locked = locked; }
    /** Used only by repositories to restore persisted state — never call from business logic. */
    public void setFailedLoginCount(int count)             { this.failedLoginCount = count; }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt)  { this.lastLoginAt = lastLoginAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', name='" + fullName + "'}";
    }
}
