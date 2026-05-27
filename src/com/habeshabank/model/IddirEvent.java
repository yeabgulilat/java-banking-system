package com.habeshabank.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A mutual-aid event within an {@link IddirGroup}.
 * When an event is reported, every active member is expected to contribute.
 */
public class IddirEvent {

    public enum Status { PENDING, DISTRIBUTED }

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private long          id;
    private long          groupId;
    private String        groupName;
    private String        beneficiaryName;
    private String        occasion;
    private double        requestedAmount;
    private double        totalCollected;     // sum of contributions so far
    private int           contributionCount;  // how many members have contributed
    private Status        status;
    private long          createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime distributedAt;

    public IddirEvent() {
        this.status    = Status.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // Display helpers
    public String formattedDate() {
        return createdAt != null ? createdAt.format(DISPLAY_FMT) : "—";
    }
    public boolean isPending()     { return status == Status.PENDING; }
    public boolean isDistributed() { return status == Status.DISTRIBUTED; }

    // Getters
    public long          getId()                { return id; }
    public long          getGroupId()           { return groupId; }
    public String        getGroupName()         { return groupName; }
    public String        getBeneficiaryName()   { return beneficiaryName; }
    public String        getOccasion()          { return occasion; }
    public double        getRequestedAmount()   { return requestedAmount; }
    public double        getTotalCollected()    { return totalCollected; }
    public int           getContributionCount() { return contributionCount; }
    public Status        getStatus()            { return status; }
    public long          getCreatedByUserId()   { return createdByUserId; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public LocalDateTime getDistributedAt()     { return distributedAt; }

    // Setters
    public void setId(long id)                               { this.id = id; }
    public void setGroupId(long groupId)                     { this.groupId = groupId; }
    public void setGroupName(String groupName)               { this.groupName = groupName; }
    public void setBeneficiaryName(String beneficiaryName)   { this.beneficiaryName = beneficiaryName; }
    public void setOccasion(String occasion)                 { this.occasion = occasion; }
    public void setRequestedAmount(double requestedAmount)   { this.requestedAmount = requestedAmount; }
    public void setTotalCollected(double totalCollected)     { this.totalCollected = totalCollected; }
    public void setContributionCount(int contributionCount)  { this.contributionCount = contributionCount; }
    public void setStatus(Status status)                     { this.status = status; }
    public void setCreatedByUserId(long id)                  { this.createdByUserId = id; }
    public void setCreatedAt(LocalDateTime createdAt)        { this.createdAt = createdAt; }
    public void setDistributedAt(LocalDateTime distributedAt){ this.distributedAt = distributedAt; }
}