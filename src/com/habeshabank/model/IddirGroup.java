package com.habeshabank.model;

import java.time.LocalDateTime;

/**
 * Represents a mutual-aid association (Iddir).
 */
public class IddirGroup {

    private long          id;
    private String        name;
    private String        groupCode;
    private double        monthlyAmount;      // expected contribution per event
    private long          createdByUserId;
    private LocalDateTime createdAt;

    public IddirGroup() {
        this.createdAt = LocalDateTime.now();
    }

    public IddirGroup(String name, String groupCode,
                      double monthlyAmount, long createdByUserId) {
        this();
        this.name             = name;
        this.groupCode        = groupCode;
        this.monthlyAmount    = monthlyAmount;
        this.createdByUserId  = createdByUserId;
    }

    // Getters
    public long          getId()               { return id; }
    public String        getName()             { return name; }
    public String        getGroupCode()        { return groupCode; }
    public double        getMonthlyAmount()    { return monthlyAmount; }
    public long          getCreatedByUserId()  { return createdByUserId; }
    public LocalDateTime getCreatedAt()        { return createdAt; }

    // Setters
    public void setId(long id)                          { this.id = id; }
    public void setName(String name)                    { this.name = name; }
    public void setGroupCode(String groupCode)          { this.groupCode = groupCode; }
    public void setMonthlyAmount(double monthlyAmount)  { this.monthlyAmount = monthlyAmount; }
    public void setCreatedByUserId(long id)             { this.createdByUserId = id; }
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }
}