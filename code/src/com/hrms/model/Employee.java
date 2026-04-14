package com.hrms.model;

/**
 * Model class representing an Employee entity.
 * Follows Single Responsibility Principle (SRP) - only holds employee data.
 */
public class Employee {

    public enum EmploymentStatus {
        ACTIVE, EXITED
    }

    private int employeeId;
    private String name;
    private String hireDate;
    private String terminationDate;
    private String department;
    private double attendancePercentage;
    private int yearsOfService;
    private int promotionCount;
    private EmploymentStatus employmentStatus;

    // Default constructor
    public Employee() {
        this.employmentStatus = EmploymentStatus.ACTIVE;
    }

    // Parameterized constructor
    public Employee(int employeeId, String name, String hireDate, String terminationDate,
                    String department, double attendancePercentage, int yearsOfService,
                    int promotionCount, EmploymentStatus employmentStatus) {
        this.employeeId = employeeId;
        this.name = name;
        this.hireDate = hireDate;
        this.terminationDate = terminationDate;
        this.department = department;
        this.attendancePercentage = attendancePercentage;
        this.yearsOfService = yearsOfService;
        this.promotionCount = promotionCount;
        this.employmentStatus = employmentStatus;
    }

    // Getters
    public int getEmployeeId() { return employeeId; }
    public String getName() { return name; }
    public String getHireDate() { return hireDate; }
    public String getTerminationDate() { return terminationDate; }
    public String getDepartment() { return department; }
    public double getAttendancePercentage() { return attendancePercentage; }
    public int getYearsOfService() { return yearsOfService; }
    public int getPromotionCount() { return promotionCount; }
    public EmploymentStatus getEmploymentStatus() { return employmentStatus; }

    // Setters
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public void setName(String name) { this.name = name; }
    public void setHireDate(String hireDate) { this.hireDate = hireDate; }
    public void setTerminationDate(String terminationDate) { this.terminationDate = terminationDate; }
    public void setDepartment(String department) { this.department = department; }
    public void setAttendancePercentage(double attendancePercentage) { this.attendancePercentage = attendancePercentage; }
    public void setYearsOfService(int yearsOfService) { this.yearsOfService = yearsOfService; }
    public void setPromotionCount(int promotionCount) { this.promotionCount = promotionCount; }
    public void setEmploymentStatus(EmploymentStatus employmentStatus) { this.employmentStatus = employmentStatus; }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + employeeId +
                ", name='" + name + '\'' +
                ", hire=" + hireDate +
                ", dept='" + department + '\'' +
                ", att=" + attendancePercentage +
                "%, status=" + employmentStatus +
                '}';
    }
}
