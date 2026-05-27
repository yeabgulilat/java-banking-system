package com.habeshabank.model;

import java.time.LocalDateTime;

/**
 * Represents a rotating savings group (Equb).
 * One group has many members; rounds cycle through all members until each
 * member has received the pot exactly once.
 */
public class EqubGroup {

    public enum Status { ACTIVE, COMPLETED, CANCELLED }

    private long          id;
    private String        name;
    private String        groupCode;          // 6-char uppercase invite code
    private double        contributionAmount; // per-member per-round
    private int           totalRounds;        // == member count at group close
    private int           currentRound;
    private Status        status;
    private long          createdByUserId;
    private LocalDateTime createdAt;

    public EqubGroup() {
        this.currentRound = 1;
        this.status       = Status.ACTIVE;
        this.createdAt    = LocalDateTime.now();
    }

    public EqubGroup(String name, String groupCode, double contributionAmount,
                     int totalRounds, long createdByUserId) {
        this();
        this.name               = name;
        this.groupCode          = groupCode;
        this.contributionAmount = contributionAmount;
        this.totalRounds        = totalRounds;
        this.createdByUserId    = createdByUserId;
    }

    // ── Business rules ────────────────────────────────────────────────────────

    public boolean isActive()    { return status == Status.ACTIVE; }
    public boolean isCompleted() { return status == Status.COMPLETED; }

    public double potAmount(int memberCount) {
        return contributionAmount * memberCount;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public long          getId()                  { return id; }
    public String        getName()                { return name; }
    public String        getGroupCode()           { return groupCode; }
    public double        getContributionAmount()  { return contributionAmount; }
    public int           getTotalRounds()         { return totalRounds; }
    public int           getCurrentRound()        { return currentRound; }
    public Status        getStatus()              { return status; }
    public long          getCreatedByUserId()     { return createdByUserId; }
    public LocalDateTime getCreatedAt()           { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(long id)                                { this.id = id; }
    public void setName(String name)                          { this.name = name; }
    public void setGroupCode(String groupCode)                { this.groupCode = groupCode; }
    public void setContributionAmount(double amt)             { this.contributionAmount = amt; }
    public void setTotalRounds(int totalRounds)               { this.totalRounds = totalRounds; }
    public void setCurrentRound(int currentRound)             { this.currentRound = currentRound; }
    public void setStatus(Status status)                      { this.status = status; }
    public void setCreatedByUserId(long id)                   { this.createdByUserId = id; }
    public void setCreatedAt(LocalDateTime createdAt)         { this.createdAt = createdAt; }
}