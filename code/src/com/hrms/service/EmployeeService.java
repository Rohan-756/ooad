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
 * Column mapping aligned with com.hrms.db.entities.Employee (hrms-database.jar):
 *   emp_id              → empId          (String PK)
 *   name                → name
 *   date_of_joining     → dateOfJoining  (was: hire_date / date_of_joining)
 *   department          → department
 *   attendance_rate     → attendanceRate (was: attendance_pct)
 *   years_of_service    → yearsOfService
 *   months_since_promotion → monthsSincePromotion (was: promotion_count)
 *   employment_status   → employmentStatus
 *
 * NOTE: There is NO `termination_date` column in the DB schema.
 *       Employee exit is tracked via the ExitInterview table (exit_date).
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
     * @param employee Employee object with data filled.
     * @throws HRMSException.InvalidInputException if any required field is invalid.
     * @return The SQLite rowid of the inserted row (integer auto-id).
     */
    public int addEmployee(Employee employee) {
        validateEmployee(employee);

        // emp_id is the DB team's varchar PK — generate a unique string.
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
            ps.setString(3, employee.getDateOfJoining());
            ps.setString(4, employee.getDepartment());
            ps.setDouble(5, employee.getAttendanceRate());
            ps.setInt(6, employee.getYearsOfService());
            ps.setInt(7, employee.getMonthsSincePromotion());
            ps.setString(8, employee.getEmploymentStatus().name());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int generatedId = keys.getInt(1); // SQLite rowid
                System.out.println("[EmployeeService] Employee added with rowid: " + generatedId + ", emp_id: " + empId);
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
     * Retrieves an employee by rowid (integer).
     *
     * @param rowId the SQLite rowid.
     * @return Employee object.
     * @throws HRMSException.InvalidEmployeeIdException if not found.
     */
    public Employee getEmployeeById(int rowId) {
        String sql = "SELECT rowid AS row_id, * FROM employees WHERE rowid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rowId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToEmployee(rs);
            } else {
                throw new HRMSException.InvalidEmployeeIdException(
                        "Employee rowid " + rowId + " not found in the system.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error fetching employee: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves an employee by emp_id (String PK from DB team).
     *
     * @param empId the varchar emp_id.
     * @return Employee object.
     * @throws HRMSException.InvalidEmployeeIdException if not found.
     */
    public Employee getEmployeeByEmpId(String empId) {
        String sql = "SELECT rowid AS row_id, * FROM employees WHERE emp_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, empId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToEmployee(rs);
            } else {
                throw new HRMSException.InvalidEmployeeIdException(
                        "Employee emp_id '" + empId + "' not found in the system.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error fetching employee by emp_id: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all employees from the database.
     */
    public List<Employee> getAllEmployees() {
        if (dummyMode) return getDummyEmployees();

        List<Employee> employees = new ArrayList<>();
        String sql = "SELECT rowid AS row_id, * FROM employees ORDER BY rowid";
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
        list.add(new Employee(1, "Alice Johnson",   "2020-01-15", null, "Engineering", 92.5, 4, 2, Employee.EmploymentStatus.ACTIVE));
        list.add(new Employee(2, "Bob Smith",       "2021-03-10", null, "Sales",       60.0, 2, 0, Employee.EmploymentStatus.EXITED));
        list.add(new Employee(3, "Charlie Davis",   "2019-06-22", null, "Marketing",   88.0, 5, 1, Employee.EmploymentStatus.ACTIVE));
        list.add(new Employee(4, "Diana Prince",    "2022-11-05", null, "Engineering", 55.0, 1, 0, Employee.EmploymentStatus.EXITED));
        list.add(new Employee(5, "Ethan Hunt",      "2023-02-28", null, "Operations",  98.0, 1, 1, Employee.EmploymentStatus.ACTIVE));
        list.add(new Employee(6, "Fiona Gallagher", "2020-05-12", null, "Sales",       75.0, 4, 0, Employee.EmploymentStatus.ACTIVE));
        list.add(new Employee(7, "George Costanza", "2018-09-01", null, "HR",          40.0, 5, 0, Employee.EmploymentStatus.EXITED));
        list.add(new Employee(8, "Hannah Abbott",   "2021-07-30", null, "Engineering", 91.0, 3, 1, Employee.EmploymentStatus.ACTIVE));
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
     * @param employee Employee object with updated data (rowid must be valid).
     * @throws HRMSException.InvalidEmployeeIdException if employee doesn't exist.
     * @throws HRMSException.InvalidInputException if data is invalid.
     */
    public void updateEmployee(Employee employee) {
        // Verify the employee exists first (by integer rowid for backward-compat)
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
            ps.setString(2, employee.getDateOfJoining());
            ps.setString(3, employee.getDepartment());
            ps.setDouble(4, employee.getAttendanceRate());
            ps.setInt(5, employee.getYearsOfService());
            ps.setInt(6, employee.getMonthsSincePromotion());
            ps.setString(7, employee.getEmploymentStatus().name());
            ps.setInt(8, employee.getEmployeeId());
            ps.executeUpdate();
            System.out.println("[EmployeeService] Employee rowid " + employee.getEmployeeId() + " updated.");
        } catch (SQLException e) {
            throw new RuntimeException("DB error updating employee: " + e.getMessage(), e);
        }
    }

    /**
     * Marks an employee as EXITED (soft delete / status change).
     */
    public void markEmployeeAsExited(int rowId) {
        // Verify existence
        getEmployeeById(rowId);

        String sql = "UPDATE employees SET employment_status = 'EXITED' WHERE rowid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rowId);
            ps.executeUpdate();
            System.out.println("[EmployeeService] Employee rowid " + rowId + " marked as EXITED.");
        } catch (SQLException e) {
            throw new RuntimeException("DB error marking employee as exited: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Permanently deletes an employee record.
     * @param rowId the SQLite rowid to delete.
     */
    public void deleteEmployee(int rowId) {
        getEmployeeById(rowId); // Existence check

        String sql = "DELETE FROM employees WHERE rowid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rowId);
            ps.executeUpdate();
            System.out.println("[EmployeeService] Employee rowid " + rowId + " deleted.");
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

    /**
     * Maps a ResultSet row to an Employee model.
     *
     * Column names match the Hibernate entity (com.hrms.db.entities.Employee):
     *   date_of_joining, attendance_rate, months_since_promotion, employment_status
     */
    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        String empId = rs.getString("emp_id");
        // Expose integer rowid via the legacy getEmployeeId() path
        int rowId = rs.getInt("row_id");

        Employee e = new Employee(
                empId,
                rs.getString("name"),
                rs.getString("date_of_joining"),
                rs.getString("department"),
                rs.getDouble("attendance_rate"),
                rs.getInt("years_of_service"),
                rs.getInt("months_since_promotion"),
                parseStatus(rs.getString("employment_status"))
        );
        // Override legacy int id with actual rowid so getEmployeeId() works
        // (setEmpId already set above; we additionally ensure hashcode path doesn't fire)
        e.setEmpId("EMP_" + rowId);
        return e;
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
