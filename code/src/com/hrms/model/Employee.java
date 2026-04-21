package com.hrms.model;

/**
 * Model class representing an Employee entity.
 * Follows Single Responsibility Principle (SRP) - only holds employee data.
 *
 * Schema aligned with com.hrms.db.entities.Employee from hrms-database.jar.
 * Key columns in `employees` table (Hibernate snake_case):
 *   emp_id, name, email, phone, address, department, designation,
 *   grade_level, basic_pay, years_of_service, role, gender,
 *   employment_status, date_of_joining, salary_band, employment_type,
 *   office_location, country_code, currency_code, tax_regime, state_name,
 *   filing_status, tax_code, national_id_number, candidate_id,
 *   attendance_rate, performance_score, months_since_promotion,
 *   tenure_years, created_at, updated_at
 */
public class Employee {

    public enum EmploymentStatus {
        ACTIVE, EXITED
    }

    // Primary key — varchar in DB (e.g. "EMP_<timestamp>")
    private String empId;

    // Basic info
    private String name;
    private String department;

    // Date columns — stored as text ISO dates in SQLite
    private String dateOfJoining;   // maps to date_of_joining column

    // Metrics — matches DB column names exactly
    private double attendanceRate;       // attendance_rate column
    private int    yearsOfService;       // years_of_service column
    private int    monthsSincePromotion; // months_since_promotion column

    // Status
    private EmploymentStatus employmentStatus;

    // ─────────────────────────────────────────────────────────────────────────
    // Constructors
    // ─────────────────────────────────────────────────────────────────────────

    /** Default constructor */
    public Employee() {
        this.employmentStatus = EmploymentStatus.ACTIVE;
    }

    /** Full constructor matching the columns used by EmployeeService. */
    public Employee(String empId, String name, String dateOfJoining,
                    String department, double attendanceRate, int yearsOfService,
                    int monthsSincePromotion, EmploymentStatus employmentStatus) {
        this.empId               = empId;
        this.name                = name;
        this.dateOfJoining       = dateOfJoining;
        this.department          = department;
        this.attendanceRate      = attendanceRate;
        this.yearsOfService      = yearsOfService;
        this.monthsSincePromotion = monthsSincePromotion;
        this.employmentStatus    = employmentStatus;
    }

    /**
     * Legacy int-id constructor kept for backward-compatibility with dummy data.
     * The rowid integer is converted to a string emp_id ("EMP_<rowid>").
     */
    public Employee(int rowId, String name, String dateOfJoining,
                    String ignoredTermDate,
                    String department, double attendanceRate, int yearsOfService,
                    int monthsSincePromotion, EmploymentStatus employmentStatus) {
        this("EMP_" + rowId, name, dateOfJoining, department,
             attendanceRate, yearsOfService, monthsSincePromotion, employmentStatus);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────────────────────────────────

    public String getEmpId()               { return empId; }
    public String getName()                { return name; }
    public String getDepartment()          { return department; }
    public String getDateOfJoining()       { return dateOfJoining; }
    public double getAttendanceRate()      { return attendanceRate; }
    public int    getYearsOfService()      { return yearsOfService; }
    public int    getMonthsSincePromotion(){ return monthsSincePromotion; }
    public EmploymentStatus getEmploymentStatus() { return employmentStatus; }

    // ─────────────────────────────────────────────────────────────────────────
    // Legacy getters — kept for backward-compatibility with existing callers
    // ─────────────────────────────────────────────────────────────────────────

    /** @deprecated Use {@link #getEmpId()} instead. Returns the numeric part of empId or -1. */
    @Deprecated
    public int getEmployeeId() {
        if (empId == null) return -1;
        try {
            return Integer.parseInt(empId.replace("EMP_", ""));
        } catch (NumberFormatException e) {
            return empId.hashCode();
        }
    }

    /** @deprecated Use {@link #getDateOfJoining()} instead. */
    @Deprecated
    public String getHireDate()            { return dateOfJoining; }

    /** @deprecated No termination_date column in DB schema; always returns null. */
    @Deprecated
    public String getTerminationDate()     { return null; }

    /** @deprecated Use {@link #getAttendanceRate()} instead. */
    @Deprecated
    public double getAttendancePercentage() { return attendanceRate; }

    /** @deprecated Use {@link #getMonthsSincePromotion()} instead. */
    @Deprecated
    public int getPromotionCount()         { return monthsSincePromotion; }

    // ─────────────────────────────────────────────────────────────────────────
    // Setters
    // ─────────────────────────────────────────────────────────────────────────

    public void setEmpId(String empId)                       { this.empId = empId; }
    public void setName(String name)                         { this.name = name; }
    public void setDepartment(String department)             { this.department = department; }
    public void setDateOfJoining(String dateOfJoining)       { this.dateOfJoining = dateOfJoining; }
    public void setAttendanceRate(double attendanceRate)     { this.attendanceRate = attendanceRate; }
    public void setYearsOfService(int yearsOfService)        { this.yearsOfService = yearsOfService; }
    public void setMonthsSincePromotion(int m)               { this.monthsSincePromotion = m; }
    public void setEmploymentStatus(EmploymentStatus s)      { this.employmentStatus = s; }

    // Legacy setters
    /** @deprecated Use {@link #setEmpId(String)} instead. */
    @Deprecated
    public void setEmployeeId(int id) { this.empId = "EMP_" + id; }

    /** @deprecated Use {@link #setDateOfJoining(String)} instead. */
    @Deprecated
    public void setHireDate(String d) { this.dateOfJoining = d; }

    /** @deprecated No termination_date in DB schema; this is a no-op. */
    @Deprecated
    public void setTerminationDate(String d) { /* no-op */ }

    /** @deprecated Use {@link #setAttendanceRate(double)} instead. */
    @Deprecated
    public void setAttendancePercentage(double v) { this.attendanceRate = v; }

    /** @deprecated Use {@link #setMonthsSincePromotion(int)} instead. */
    @Deprecated
    public void setPromotionCount(int c) { this.monthsSincePromotion = c; }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Employee{" +
                "empId='" + empId + '\'' +
                ", name='" + name + '\'' +
                ", dateOfJoining='" + dateOfJoining + '\'' +
                ", dept='" + department + '\'' +
                ", attendanceRate=" + attendanceRate +
                "%, monthsSincePromotion=" + monthsSincePromotion +
                ", status=" + employmentStatus +
                '}';
    }
}
