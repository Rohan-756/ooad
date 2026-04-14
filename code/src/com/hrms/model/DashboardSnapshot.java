package com.hrms.model;

import java.util.List;
import java.util.Map;

/**
 * Aggregated snapshot of the executive dashboard at a point in time.
 * Built by DashboardService and consumed by DashboardController and the UI.
 */
public class DashboardSnapshot {

    private double overallAttritionRate;     // percentage 0-100
    private int    totalEmployees;
    private int    totalExits;
    private int    activeEmployees;
    private int    highRiskCount;

    private List<AttritionRecord>    trendData;
    private List<RiskAssessment>     highRiskEmployees;
    private CorrelationReport        correlationReport;
    private List<RootCauseFinding>   rootCauses;

    /** Department name → {totalCount, exitCount, attritionRate} */
    private Map<String, double[]> departmentAttritionRates;

    private String generatedAt;  // ISO timestamp

    public DashboardSnapshot() {}

    // Getters
    public double                  getOverallAttritionRate()     { return overallAttritionRate; }
    public int                     getTotalEmployees()            { return totalEmployees; }
    public int                     getTotalExits()                { return totalExits; }
    public int                     getActiveEmployees()           { return activeEmployees; }
    public int                     getHighRiskCount()             { return highRiskCount; }
    public List<AttritionRecord>   getTrendData()                 { return trendData; }
    public List<RiskAssessment>    getHighRiskEmployees()         { return highRiskEmployees; }
    public CorrelationReport       getCorrelationReport()         { return correlationReport; }
    public List<RootCauseFinding>  getRootCauses()                { return rootCauses; }
    public Map<String, double[]>   getDepartmentAttritionRates()  { return departmentAttritionRates; }
    public String                  getGeneratedAt()               { return generatedAt; }

    // Setters
    public void setOverallAttritionRate(double r)                    { this.overallAttritionRate = r; }
    public void setTotalEmployees(int n)                             { this.totalEmployees = n; }
    public void setTotalExits(int n)                                 { this.totalExits = n; }
    public void setActiveEmployees(int n)                            { this.activeEmployees = n; }
    public void setHighRiskCount(int n)                              { this.highRiskCount = n; }
    public void setTrendData(List<AttritionRecord> d)                { this.trendData = d; }
    public void setHighRiskEmployees(List<RiskAssessment> e)         { this.highRiskEmployees = e; }
    public void setCorrelationReport(CorrelationReport r)            { this.correlationReport = r; }
    public void setRootCauses(List<RootCauseFinding> rc)             { this.rootCauses = rc; }
    public void setDepartmentAttritionRates(Map<String, double[]> m) { this.departmentAttritionRates = m; }
    public void setGeneratedAt(String t)                             { this.generatedAt = t; }
}
