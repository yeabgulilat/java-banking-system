package com.habeshabank.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents one round in an {@link EqubGroup}.
 * A round is OPEN while members contribute; PAID once the pot is distributed.
 */
public class EqubRound {

    public enum Status { OPEN, PAID }

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private long          id;
    private long          groupId;
    private String        groupName;          // denormalised for display
    private int           roundNumber;
    private long          winnerUserId;       // 0 = not yet drawn
    private String        winnerName;         // denormalised for display
    private double        potAmount;
    private int           contributionsIn;
    private Status        status;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;

    public EqubRound() {
        this.status          = Status.OPEN;
        this.contributionsIn = 0;
        this.winnerUserId    = 0;
    }

    // Display helpers
    public String formattedDueDate() {
        return dueDate != null ? dueDate.format(DISPLAY_FMT) : "—";
    }
    public String formattedPaidAt() {
        return paidAt != null ? paidAt.format(DISPLAY_FMT) : "—";
    }
    public boolean isPaid() { return status == Status.PAID; }

    // Getters
    public long          getId()               { return id; }
    public long          getGroupId()          { return groupId; }
    public String        getGroupName()        { return groupName; }
    public int           getRoundNumber()      { return roundNumber; }
    public long          getWinnerUserId()     { return winnerUserId; }
    public String        getWinnerName()       { return winnerName; }
    public double        getPotAmount()        { return potAmount; }
    public int           getContributionsIn()  { return contributionsIn; }
    public Status        getStatus()           { return status; }
    public LocalDateTime getDueDate()          { return dueDate; }
    public LocalDateTime getPaidAt()           { return paidAt; }

    // Setters
    public void setId(long id)                          { this.id = id; }
    public void setGroupId(long groupId)                { this.groupId = groupId; }
    public void setGroupName(String groupName)          { this.groupName = groupName; }
    public void setRoundNumber(int roundNumber)         { this.roundNumber = roundNumber; }
    public void setWinnerUserId(long winnerUserId)      { this.winnerUserId = winnerUserId; }
    public void setWinnerName(String winnerName)        { this.winnerName = winnerName; }
    public void setPotAmount(double potAmount)          { this.potAmount = potAmount; }
    public void setContributionsIn(int contributionsIn) { this.contributionsIn = contributionsIn; }
    public void setStatus(Status status)                { this.status = status; }
    public void setDueDate(LocalDateTime dueDate)       { this.dueDate = dueDate; }
    public void setPaidAt(LocalDateTime paidAt)         { this.paidAt = paidAt; }
}