package com.hrms.model;

import java.time.Instant;

/**
 * Model representing the risk evaluation result for a single employee.
 *
 * Produced by RiskService via the Strategy + Factory pattern.
 * Stored in the risk_assessments table.
 */
public class RiskAssessment {

    private int riskAssessmentId;
    private int employeeId;
    private RiskLevel riskLevel;
    private String reason;

    private double performanceScore;   // attendance % used as proxy
    private double absenteeismRate;    // 100 - attendance %
    private int    promotionGap;       // promotionCount (inverse indicator)
    private String evaluatedAt;        // ISO date-time string

    // Default constructor
    public RiskAssessment() {}

    // Parameterized constructor
    public RiskAssessment(int riskAssessmentId, int employeeId, RiskLevel riskLevel,
                          String reason, double performanceScore, double absenteeismRate,
                          int promotionGap, String evaluatedAt) {
        this.riskAssessmentId = riskAssessmentId;
        this.employeeId       = employeeId;
        this.riskLevel        = riskLevel;
        this.reason           = reason;
        this.performanceScore = performanceScore;
        this.absenteeismRate  = absenteeismRate;
        this.promotionGap     = promotionGap;
        this.evaluatedAt      = evaluatedAt;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int       getRiskAssessmentId() { return riskAssessmentId; }
    public int       getEmployeeId()       { return employeeId; }
    public RiskLevel getRiskLevel()        { return riskLevel; }
    public String    getReason()           { return reason; }
    public double    getPerformanceScore() { return performanceScore; }
    public double    getAbsenteeismRate()  { return absenteeismRate; }
    public int       getPromotionGap()     { return promotionGap; }
    public String    getEvaluatedAt()      { return evaluatedAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setRiskAssessmentId(int riskAssessmentId) { this.riskAssessmentId = riskAssessmentId; }
    public void setEmployeeId(int employeeId)             { this.employeeId = employeeId; }
    public void setRiskLevel(RiskLevel riskLevel)         { this.riskLevel = riskLevel; }
    public void setReason(String reason)                  { this.reason = reason; }
    public void setPerformanceScore(double s)             { this.performanceScore = s; }
    public void setAbsenteeismRate(double r)              { this.absenteeismRate = r; }
    public void setPromotionGap(int g)                    { this.promotionGap = g; }
    public void setEvaluatedAt(String evaluatedAt)        { this.evaluatedAt = evaluatedAt; }

    @Override
    public String toString() {
        return "RiskAssessment{" +
                "id=" + riskAssessmentId +
                ", employeeId=" + employeeId +
                ", level=" + riskLevel +
                ", score=" + String.format("%.1f", performanceScore) +
                ", reason='" + reason + '\'' +
                '}';
    }
}
