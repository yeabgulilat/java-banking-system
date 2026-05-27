package com.habeshabank.model;

import java.time.LocalDateTime;

/**
 * A single user's membership in an {@link EqubGroup}.
 */
public class EqubMember {

    private long          id;
    private long          groupId;
    private long          userId;
    private String        accountNumber;
    private String        fullName;           // denormalised for display
    private boolean       hasReceivedPot;
    private LocalDateTime joinedAt;

    public EqubMember() {
        this.hasReceivedPot = false;
        this.joinedAt       = LocalDateTime.now();
    }

    public EqubMember(long groupId, long userId, String accountNumber, String fullName) {
        this();
        this.groupId       = groupId;
        this.userId        = userId;
        this.accountNumber = accountNumber;
        this.fullName      = fullName;
    }

    // Getters
    public long          getId()             { return id; }
    public long          getGroupId()        { return groupId; }
    public long          getUserId()         { return userId; }
    public String        getAccountNumber()  { return accountNumber; }
    public String        getFullName()       { return fullName; }
    public boolean       hasReceivedPot()    { return hasReceivedPot; }
    public LocalDateTime getJoinedAt()       { return joinedAt; }

    // Setters
    public void setId(long id)                         { this.id = id; }
    public void setGroupId(long groupId)               { this.groupId = groupId; }
    public void setUserId(long userId)                 { this.userId = userId; }
    public void setAccountNumber(String acct)          { this.accountNumber = acct; }
    public void setFullName(String fullName)           { this.fullName = fullName; }
    public void setHasReceivedPot(boolean b)           { this.hasReceivedPot = b; }
    public void setJoinedAt(LocalDateTime joinedAt)    { this.joinedAt = joinedAt; }
}