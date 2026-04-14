package com.hrms.model;

import java.time.LocalDate;

/**
 * Report containing correlation coefficients between employee attributes
 * and attrition, computed by AnalyticsService.
 *
 * Correlation values are in the range [-1.0, 1.0]:
 *   -1.0 = strong inverse correlation (e.g. higher attendance → lower attrition)
 *    0.0 = no correlation
 *   +1.0 = strong positive correlation (attribute increases together with attrition)
 */
public class CorrelationReport {

    private int        reportId;
    private LocalDate  startDate;
    private LocalDate  endDate;

    /** Correlation between low attendance and attrition probability. */
    private double attendanceCorrelation;

    /** Correlation between lack of promotions and attrition probability. */
    private double promotionCorrelation;

    /** Correlation between shorter tenure and attrition probability. */
    private double tenureCorrelation;

    private String summary;
    private String createdAt;  // ISO timestamp

    // Snapshot counters used to build the summary
    private int totalSample;
    private int exitedSample;

    public CorrelationReport() {}

    // Getters
    public int       getReportId()              { return reportId; }
    public LocalDate getStartDate()             { return startDate; }
    public LocalDate getEndDate()               { return endDate; }
    public double    getAttendanceCorrelation() { return attendanceCorrelation; }
    public double    getPromotionCorrelation()  { return promotionCorrelation; }
    public double    getTenureCorrelation()     { return tenureCorrelation; }
    public String    getSummary()               { return summary; }
    public String    getCreatedAt()             { return createdAt; }
    public int       getTotalSample()           { return totalSample; }
    public int       getExitedSample()          { return exitedSample; }

    // Setters
    public void setReportId(int id)                         { this.reportId = id; }
    public void setStartDate(LocalDate d)                   { this.startDate = d; }
    public void setEndDate(LocalDate d)                     { this.endDate = d; }
    public void setAttendanceCorrelation(double c)          { this.attendanceCorrelation = c; }
    public void setPromotionCorrelation(double c)           { this.promotionCorrelation = c; }
    public void setTenureCorrelation(double c)              { this.tenureCorrelation = c; }
    public void setSummary(String s)                        { this.summary = s; }
    public void setCreatedAt(String t)                      { this.createdAt = t; }
    public void setTotalSample(int n)                       { this.totalSample = n; }
    public void setExitedSample(int n)                      { this.exitedSample = n; }

    @Override
    public String toString() {
        return String.format(
            "CorrelationReport{id=%d, attendance=%.2f, promotion=%.2f, tenure=%.2f}",
            reportId, attendanceCorrelation, promotionCorrelation, tenureCorrelation);
    }
}
