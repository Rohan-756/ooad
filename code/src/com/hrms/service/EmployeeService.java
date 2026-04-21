package com.hrms.service;

import com.hrms.db.DBConnection;
import com.hrms.exception.HRMSException;
import com.hrms.model.Employee;
import com.hrms.service.validation.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * EmployeeService — Business Logic Layer for Employee Management.
 *
 * Handles all CRUD operations and validation for Employee entities.
 * Uses the DBConnection Singleton for database access.
 *
 * SOLID: SRP — Only manages employee-related business logic.
 * SOLID: DIP — Depends on DBConnection abstraction, not concrete drivers.
 */
public class EmployeeService {

    private final Connection conn;
    private final boolean    dummyMode;

    public EmployeeService() {
        this.conn      = DBConnection.getInstance().getConnection();
        this.dummyMode = DBConnection.getInstance().isDummyMode();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds a new employee to the database.
     *
     * @param employee Employee object with data filled (ID will be auto-assigned).
     * @throws HRMSException.InvalidInputException if any required field is invalid.
     */
    public int addEmployee(Employee employee) {
        validateEmployee(employee);

        // emp_id is the DB team's varchar PK — generate a unique string.
        // We use the SQLite rowid (auto-assigned) as our integer employee_id.
        String empId = "EMP_" + System.currentTimeMillis();

        String sql = """
                INSERT INTO employees
                    (emp_id, name, date_of_joining, department,
                     attendance_rate, years_of_service, months_since_promotion, employment_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, empId);
            ps.setString(2, employee.getName());
            ps.setString(3, employee.getHireDate());
            ps.setString(4, employee.getDepartment());
            ps.setDouble(5, employee.getAttendancePercentage());
            ps.setInt(6, employee.getYearsOfService());
            ps.setInt(7, employee.getPromotionCount());
            ps.setString(8, employee.getEmploymentStatus().name());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int generatedId = keys.getInt(1); // SQLite rowid
                System.out.println("[EmployeeService] Employee added with ID: " + generatedId);
                return generatedId;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error adding employee: " + e.getMessage(), e);
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves an employee by ID.
     *
     * @param employeeId the ID to search for.
     * @return Employee object.
     * @throws HRMSException.InvalidEmployeeIdException if not found.
     */
    public Employee getEmployeeById(int employeeId) {
        // Use rowid as the stable integer PK into the DB team's employees table
        String sql = "SELECT rowid AS employee_id, * FROM employees WHERE rowid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToEmployee(rs);
            } else {
                throw new HRMSException.InvalidEmployeeIdException(
                        "Employee ID " + employeeId + " not found in the system.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error fetching employee: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all employees from the database.
     */
    public List<Employee> getAllEmployees() {
        if (dummyMode) return getDummyEmployees();

        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT rowid AS employee_id, * FROM employees ORDER BY rowid";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error fetching all employees: " + e.getMessage(), e);
        }
        return employees;
    }

    private List<Employee> getDummyEmployees() {
        List<Employee> list = new ArrayList<>();
        list.add(new Employee(1, "Alice Johnson",   "2020-01-15", null,         "Engineering", 92.5, 4, 2, Employee.EmploymentStatus.ACTIVE));
        list.add(new Employee(2, "Bob Smith",       "2021-03-10", "2023-08-15", "Sales",       60.0, 2, 0, Employee.EmploymentStatus.EXITED));
        list.add(new Employee(3, "Charlie Davis",   "2019-06-22", null,         "Marketing",   88.0, 5, 1, Employee.EmploymentStatus.ACTIVE));
        list.add(new Employee(4, "Diana Prince",    "2022-11-05", "2024-01-10", "Engineering", 55.0, 1, 0, Employee.EmploymentStatus.EXITED));
        list.add(new Employee(5, "Ethan Hunt",      "2023-02-28", null,         "Operations",  98.0, 1, 1, Employee.EmploymentStatus.ACTIVE));
        list.add(new Employee(6, "Fiona Gallagher", "2020-05-12", null,         "Sales",       75.0, 4, 0, Employee.EmploymentStatus.ACTIVE));
        list.add(new Employee(7, "George Costanza", "2018-09-01", "2023-11-20", "HR",          40.0, 5, 0, Employee.EmploymentStatus.EXITED));
        list.add(new Employee(8, "Hannah Abbott",   "2021-07-30", null,         "Engineering", 91.0, 3, 1, Employee.EmploymentStatus.ACTIVE));
        return list;
    }

    /**
     * Returns a count of active employees — used by AttritionService.
     */
    public int getActiveEmployeeCount() {
        if (dummyMode) return (int) getDummyEmployees().stream()
                .filter(e -> e.getEmploymentStatus() == Employee.EmploymentStatus.ACTIVE).count();

        String sql = "SELECT COUNT(*) FROM employees WHERE employment_status = 'ACTIVE'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("DB error counting employees: " + e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Returns total employee count — used by AttritionService.
     */
    public int getTotalEmployeeCount() {
        if (dummyMode) return getDummyEmployees().size();

        String sql = "SELECT COUNT(*) FROM employees";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("DB error counting total employees: " + e.getMessage(), e);
        }
        return 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates an existing employee's information.
     *
     * @param employee Employee object with updated data (ID must be valid).
     * @throws HRMSException.InvalidEmployeeIdException if employee doesn't exist.
     * @throws HRMSException.InvalidInputException if data is invalid.
     */
    public void updateEmployee(Employee employee) {
        // Verify the employee exists first
        getEmployeeById(employee.getEmployeeId());
        validateEmployee(employee);

        String sql = """
                UPDATE employees SET
                    name = ?, date_of_joining = ?, department = ?,
                    attendance_rate = ?, years_of_service = ?, months_since_promotion = ?,
                    employment_status = ?
                WHERE rowid = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employee.getName());
            ps.setString(2, employee.getHireDate());
            ps.setString(3, employee.getDepartment());
            ps.setDouble(4, employee.getAttendancePercentage());
            ps.setInt(5, employee.getYearsOfService());
            ps.setInt(6, employee.getPromotionCount());
            ps.setString(7, employee.getEmploymentStatus().name());
            ps.setInt(8, employee.getEmployeeId());
            ps.executeUpdate();
            System.out.println("[EmployeeService] Employee ID " + employee.getEmployeeId() + " updated.");
        } catch (SQLException e) {
            throw new RuntimeException("DB error updating employee: " + e.getMessage(), e);
        }
    }

    /**
     * Marks an employee as EXITED (soft delete / status change).
     */
    public void markEmployeeAsExited(int employeeId) {
        // Verify existence
        getEmployeeById(employeeId);

        String sql = "UPDATE employees SET employment_status = 'EXITED' WHERE rowid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.executeUpdate();
            System.out.println("[EmployeeService] Employee ID " + employeeId + " marked as EXITED.");
        } catch (SQLException e) {
            throw new RuntimeException("DB error marking employee as exited: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Permanently deletes an employee record.
     * @param employeeId the ID to delete.
     */
    public void deleteEmployee(int employeeId) {
        getEmployeeById(employeeId); // Existence check

        String sql = "DELETE FROM employees WHERE rowid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.executeUpdate();
            System.out.println("[EmployeeService] Employee ID " + employeeId + " deleted.");
        } catch (SQLException e) {
            throw new RuntimeException("DB error deleting employee: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates an Employee object before persistence.
     * Throws InvalidInputException for any invalid field.
     */
    public void validateEmployee(Employee employee) {
        EmployeeValidator validator = new BasicInfoValidator();
        validator.linkWith(new MetricsValidator())
                 .linkWith(new StatusValidator());
                 
        validator.check(employee);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getInt("employee_id"),           // rowid alias
                rs.getString("name"),
                rs.getString("date_of_joining"),    // was hire_date
                null,                               // termination_date not in hrms.db; tracked via exit_interviews
                rs.getString("department"),
                rs.getDouble("attendance_rate"),    // was attendance_pct
                rs.getInt("years_of_service"),
                rs.getInt("months_since_promotion"), // was promotion_count
                parseStatus(rs.getString("employment_status"))
        );
    }

    /**
     * Safely maps employment_status string to enum.
     * Defaults to ACTIVE for any DB team status values not in our enum
     * (e.g. TERMINATED, RESIGNED, etc.).
     */
    private Employee.EmploymentStatus parseStatus(String s) {
        try {
            return Employee.EmploymentStatus.valueOf(s);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Employee.EmploymentStatus.ACTIVE;
        }
    }
}
