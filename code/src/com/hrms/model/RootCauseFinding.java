package com.hrms.model;

/**
 * A single root-cause finding produced by AnalyticsService.identifyRootCauses().
 * Represents one factor that statistically contributes to attrition.
 */
public class RootCauseFinding {

    private int    findingId;
    private String causeType;    // "attendance", "promotion", "tenure", "workload"
    private String description;  // Human-readable explanation
    private double impactScore;  // 0–100, higher = greater contributor to attrition

    public RootCauseFinding() {}

    public RootCauseFinding(int findingId, String causeType,
                            String description, double impactScore) {
        this.findingId   = findingId;
        this.causeType   = causeType;
        this.description = description;
        this.impactScore = impactScore;
    }

    public int    getFindingId()   { return findingId; }
    public String getCauseType()   { return causeType; }
    public String getDescription() { return description; }
    public double getImpactScore() { return impactScore; }

    public void setFindingId(int id)         { this.findingId = id; }
    public void setCauseType(String t)       { this.causeType = t; }
    public void setDescription(String d)     { this.description = d; }
    public void setImpactScore(double score) { this.impactScore = score; }

    @Override
    public String toString() {
        return String.format("RootCauseFinding{type='%s', impact=%.1f, desc='%s'}",
                causeType, impactScore, description);
    }
}
