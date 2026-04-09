package com.hrms.service;

import com.hrms.db.DBConnection;
import com.hrms.exception.HRMSException;
import com.hrms.model.ExitInterview;
import com.hrms.model.Employee;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ExitInterviewService — Business Logic Layer for Exit Interview Management.
 *
 * Handles recording and retrieval of exit interviews, validates employee
 * existence via EmployeeService, and enforces feedback rules.
 *
 * SOLID: SRP — Only manages exit interview business logic.
 */
public class ExitInterviewService {

    private final Connection conn;
    private final EmployeeService employeeService;

    public ExitInterviewService() {
        this.conn = DBConnection.getInstance().getConnection();
        this.employeeService = new EmployeeService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAVE / CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Records an exit interview for an employee.
     *
     * - Validates that the employee exists (throws InvalidEmployeeIdException if not).
     * - Warns (throws MissingFeedbackException) if feedback is empty,
     *   but allows submission to proceed if caller chooses.
     * - Marks the employee as EXITED automatically.
     *
     * @param interview ExitInterview object to persist.
     * @return The generated interview ID.
     * @throws HRMSException.InvalidEmployeeIdException if the employee doesn't exist.
     * @throws HRMSException.MissingFeedbackException   if feedback is empty (WARNING level).
     */
    public int saveExitInterview(ExitInterview interview) {
        // Validate employee exists
        Employee employee = employeeService.getEmployeeById(interview.getEmployeeId());

        // Validate exit reason
        if (interview.getExitReason() == null || interview.getExitReason().trim().isEmpty()) {
            throw new HRMSException.InvalidInputException("Exit reason cannot be empty.");
        }

        // WARNING: feedback is empty (can be caught & handled by caller)
        if (interview.getFeedback() == null || interview.getFeedback().trim().isEmpty()) {
            throw new HRMSException.MissingFeedbackException(
                    "Feedback text is empty for employee ID " + interview.getEmployeeId() +
                    ". Submission allowed with warning.");
        }

        // Set date if not provided
        if (interview.getInterviewDate() == null || interview.getInterviewDate().isEmpty()) {
            interview.setInterviewDate(LocalDate.now().toString());
        }

        String sql = """
                INSERT INTO exit_interviews (employee_id, exit_reason, feedback, interview_date)
                VALUES (?, ?, ?, ?)
                """;

        int generatedId = -1;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, interview.getEmployeeId());
            ps.setString(2, interview.getExitReason());
            ps.setString(3, interview.getFeedback() != null ? interview.getFeedback() : "");
            ps.setString(4, interview.getInterviewDate());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                generatedId = keys.getInt(1);
            }
            System.out.println("[ExitInterviewService] Exit interview saved with ID: " + generatedId);
        } catch (SQLException e) {
            throw new RuntimeException("DB error saving exit interview: " + e.getMessage(), e);
        }

        // Mark employee as EXITED
        employeeService.markEmployeeAsExited(interview.getEmployeeId());

        return generatedId;
    }

    /**
     * Saves an exit interview even without feedback (after WARNING was shown to user).
     * Use this when the user acknowledges the missing feedback warning.
     */
    public int saveExitInterviewWithoutFeedback(ExitInterview interview) {
        // Validate employee exists
        employeeService.getEmployeeById(interview.getEmployeeId());

        if (interview.getExitReason() == null || interview.getExitReason().trim().isEmpty()) {
            throw new HRMSException.InvalidInputException("Exit reason cannot be empty.");
        }

        if (interview.getInterviewDate() == null || interview.getInterviewDate().isEmpty()) {
            interview.setInterviewDate(LocalDate.now().toString());
        }

        String sql = """
                INSERT INTO exit_interviews (employee_id, exit_reason, feedback, interview_date)
                VALUES (?, ?, ?, ?)
                """;

        int generatedId = -1;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, interview.getEmployeeId());
            ps.setString(2, interview.getExitReason());
            ps.setString(3, "");
            ps.setString(4, interview.getInterviewDate());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                generatedId = keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error saving exit interview (no feedback): " + e.getMessage(), e);
        }

        employeeService.markEmployeeAsExited(interview.getEmployeeId());
        return generatedId;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves the exit interview for a specific employee.
     *
     * @param employeeId the employee ID.
     * @return The most recent ExitInterview for that employee, or null if none.
     * @throws HRMSException.InvalidEmployeeIdException if employee doesn't exist.
     */
    public ExitInterview getInterviewByEmployee(int employeeId) {
        // Validate employee exists
        employeeService.getEmployeeById(employeeId);

        String sql = """
                SELECT * FROM exit_interviews
                WHERE employee_id = ?
                ORDER BY interview_id DESC LIMIT 1
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToInterview(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error fetching exit interview: " + e.getMessage(), e);
        }
        return null; // No interview found
    }

    /**
     * Retrieves all exit interviews from the database.
     */
    public List<ExitInterview> getAllInterviews() {
        List<ExitInterview> list = new ArrayList<>();
        String sql = "SELECT * FROM exit_interviews ORDER BY interview_id DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToInterview(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error fetching all interviews: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Returns the count of employees who have exited — used by AttritionService.
     */
    public int getExitedEmployeeCount() {
        String sql = "SELECT COUNT(DISTINCT employee_id) FROM exit_interviews";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("DB error counting exits: " + e.getMessage(), e);
        }
        return 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private ExitInterview mapResultSetToInterview(ResultSet rs) throws SQLException {
        return new ExitInterview(
                rs.getInt("interview_id"),
                rs.getInt("employee_id"),
                rs.getString("exit_reason"),
                rs.getString("feedback"),
                rs.getString("interview_date")
        );
    }
}
