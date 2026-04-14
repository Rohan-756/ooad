package com.hrms.model;

/**
 * Specification object used to define a data segment filter.
 * Passed to SegmentationService to slice the employee dataset.
 *
 * Follows the Specification pattern — carries filter criteria without
 * containing business logic, keeping SegmentationService clean.
 */
public class FilterSpec {

    private String department;         // null = all departments
    private String employmentStatus;   // null = all statuses ("ACTIVE" / "EXITED")
    private Integer minYearsOfService; // null = no lower bound
    private Integer maxYearsOfService; // null = no upper bound
    private Integer minPromotions;     // null = no lower bound
    private PeriodType periodType;     // optional, for time-window filtering
    private String label;              // human-readable label for this segment

    // Default constructor
    public FilterSpec() {}

    // Convenience builder-style static factory
    public static FilterSpec byDepartment(String dept) {
        FilterSpec f = new FilterSpec();
        f.setDepartment(dept);
        f.setLabel("Department: " + dept);
        return f;
    }

    public static FilterSpec byStatus(String status) {
        FilterSpec f = new FilterSpec();
        f.setEmploymentStatus(status);
        f.setLabel("Status: " + status);
        return f;
    }

    // Getters
    public String     getDepartment()       { return department; }
    public String     getEmploymentStatus() { return employmentStatus; }
    public Integer    getMinYearsOfService(){ return minYearsOfService; }
    public Integer    getMaxYearsOfService(){ return maxYearsOfService; }
    public Integer    getMinPromotions()    { return minPromotions; }
    public PeriodType getPeriodType()       { return periodType; }
    public String     getLabel()            { return label; }

    // Setters
    public void setDepartment(String department)             { this.department = department; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }
    public void setMinYearsOfService(Integer min)            { this.minYearsOfService = min; }
    public void setMaxYearsOfService(Integer max)            { this.maxYearsOfService = max; }
    public void setMinPromotions(Integer min)                { this.minPromotions = min; }
    public void setPeriodType(PeriodType periodType)         { this.periodType = periodType; }
    public void setLabel(String label)                       { this.label = label; }

    @Override
    public String toString() {
        return "FilterSpec{dept='" + department + "', status='" + employmentStatus +
                "', yearsRange=[" + minYearsOfService + "," + maxYearsOfService + "]}";
    }
}
