package com.hrms.model;

import java.time.LocalDate;

/**
 * Input filter for DashboardService.buildDashboard().
 * Allows the UI to scope the dashboard to a specific period and/or department.
 */
public class DashboardFilter {

    private LocalDate  startDate;
    private LocalDate  endDate;
    private PeriodType periodType;
    private String     department; // null = all departments

    public DashboardFilter() {
        this.startDate  = LocalDate.now().minusYears(1).withDayOfMonth(1);
        this.endDate    = LocalDate.now();
        this.periodType = PeriodType.MONTHLY;
    }

    public DashboardFilter(LocalDate startDate, LocalDate endDate,
                           PeriodType periodType, String department) {
        this.startDate  = startDate;
        this.endDate    = endDate;
        this.periodType = periodType;
        this.department = department;
    }

    public LocalDate  getStartDate()  { return startDate; }
    public LocalDate  getEndDate()    { return endDate; }
    public PeriodType getPeriodType() { return periodType; }
    public String     getDepartment() { return department; }

    public void setStartDate(LocalDate d)   { this.startDate = d; }
    public void setEndDate(LocalDate d)     { this.endDate = d; }
    public void setPeriodType(PeriodType p) { this.periodType = p; }
    public void setDepartment(String dept)  { this.department = dept; }
}
