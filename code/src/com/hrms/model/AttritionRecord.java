package com.hrms.model;

import java.time.LocalDate;

/**
 * Represents one computed attrition record for a time bucket.
 */
public class AttritionRecord {
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalEmployees;
    private int employeesLeft;
    private double attritionRate;

    public AttritionRecord(LocalDate startDate, LocalDate endDate,
                           int totalEmployees, int employeesLeft, double attritionRate) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalEmployees = totalEmployees;
        this.employeesLeft = employeesLeft;
        this.attritionRate = attritionRate;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getTotalEmployees() { return totalEmployees; }
    public int getEmployeesLeft() { return employeesLeft; }
    public double getAttritionRate() { return attritionRate; }

    @Override
    public String toString() {
        return "AttritionRecord{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", totalEmployees=" + totalEmployees +
                ", employeesLeft=" + employeesLeft +
                ", attritionRate=" + String.format("%.2f", attritionRate) +
                "%}";
    }
}
