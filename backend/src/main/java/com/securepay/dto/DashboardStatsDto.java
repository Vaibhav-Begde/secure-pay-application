package com.securepay.dto;

public class DashboardStatsDto {

    private long totalTransactions;
    private long lowRisk;
    private long mediumRisk;
    private long highRisk;
    private long blocked;
    private long pendingReview;

    public DashboardStatsDto() {}

    public DashboardStatsDto(long totalTransactions, long lowRisk, long mediumRisk,
                              long highRisk, long blocked, long pendingReview) {
        this.totalTransactions = totalTransactions;
        this.lowRisk = lowRisk;
        this.mediumRisk = mediumRisk;
        this.highRisk = highRisk;
        this.blocked = blocked;
        this.pendingReview = pendingReview;
    }

    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }

    public long getLowRisk() { return lowRisk; }
    public void setLowRisk(long lowRisk) { this.lowRisk = lowRisk; }

    public long getMediumRisk() { return mediumRisk; }
    public void setMediumRisk(long mediumRisk) { this.mediumRisk = mediumRisk; }

    public long getHighRisk() { return highRisk; }
    public void setHighRisk(long highRisk) { this.highRisk = highRisk; }

    public long getBlocked() { return blocked; }
    public void setBlocked(long blocked) { this.blocked = blocked; }

    public long getPendingReview() { return pendingReview; }
    public void setPendingReview(long pendingReview) { this.pendingReview = pendingReview; }
}
