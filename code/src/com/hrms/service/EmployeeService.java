package com.hrms.service;

import com.hrms.db.DBConnection;
import com.hrms.exception.HRMSException;
import com.hrms.model.Employee;

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

    public EmployeeService() {
        this.conn = DBConnection.getInstance().getConnection();
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

        String sql = """
                INSERT INTO employees (name, department, attendance_pct,
                    years_of_service, promotion_count, employment_status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, employee.getName());
            ps.setString(2, employee.getDepartment());
            ps.setDouble(3, employee.getAttendancePercentage());
            ps.setInt(4, employee.getYearsOfService());
            ps.setInt(5, employee.getPromotionCount());
            ps.setString(6, employee.getEmploymentStatus().name());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int generatedId = keys.getInt(1);
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
        String sql = "SELECT * FROM employees WHERE employee_id = ?";
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
        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT * FROM employees ORDER BY employee_id";
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

    /**
     * Returns a count of active employees — used by AttritionService.
     */
    public int getActiveEmployeeCount() {
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
                    name = ?, department = ?, attendance_pct = ?,
                    years_of_service = ?, promotion_count = ?, employment_status = ?
                WHERE employee_id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employee.getName());
            ps.setString(2, employee.getDepartment());
            ps.setDouble(3, employee.getAttendancePercentage());
            ps.setInt(4, employee.getYearsOfService());
            ps.setInt(5, employee.getPromotionCount());
            ps.setString(6, employee.getEmploymentStatus().name());
            ps.setInt(7, employee.getEmployeeId());
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

        String sql = "UPDATE employees SET employment_status = 'EXITED' WHERE employee_id = ?";
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

        String sql = "DELETE FROM employees WHERE employee_id = ?";
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
        if (employee == null) {
            throw new HRMSException.InvalidInputException("Employee object cannot be null.");
        }
        if (employee.getName() == null || employee.getName().trim().isEmpty()) {
            throw new HRMSException.InvalidInputException("Employee name cannot be empty.");
        }
        if (employee.getDepartment() == null || employee.getDepartment().trim().isEmpty()) {
            throw new HRMSException.InvalidInputException("Department cannot be empty.");
        }
        if (employee.getAttendancePercentage() < 0 || employee.getAttendancePercentage() > 100) {
            throw new HRMSException.InvalidInputException(
                    "Attendance percentage must be between 0 and 100. Got: " + employee.getAttendancePercentage());
        }
        if (employee.getYearsOfService() < 0) {
            throw new HRMSException.InvalidInputException(
                    "Years of service cannot be negative. Got: " + employee.getYearsOfService());
        }
        if (employee.getPromotionCount() < 0) {
            throw new HRMSException.InvalidInputException(
                    "Promotion count cannot be negative. Got: " + employee.getPromotionCount());
        }
        if (employee.getEmploymentStatus() == null) {
            throw new HRMSException.InvalidInputException("Employment status cannot be null.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getInt("employee_id"),
                rs.getString("name"),
                rs.getString("department"),
                rs.getDouble("attendance_pct"),
                rs.getInt("years_of_service"),
                rs.getInt("promotion_count"),
                Employee.EmploymentStatus.valueOf(rs.getString("employment_status"))
        );
    }
}
