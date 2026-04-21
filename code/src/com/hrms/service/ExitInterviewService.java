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
 * Column mapping aligned with com.hrms.db.entities.ExitInterview (hrms-database.jar):
 *   interview_id       (String PK)
 *   emp_id             (String FK → employees.emp_id)
 *   primary_reason     (was: exit_reason)
 *   feedback_text      (was: feedback)
 *   satisfaction_rating (Integer)
 *   issues_reported
 *   interviewer_notes
 *   exit_date          (was: interview_date)
 *
 * SOLID: SRP — Only manages exit interview business logic.
 */
public class ExitInterviewService {

    private final Connection conn;
    private final EmployeeService employeeService;
    private final boolean         dummyMode;

    public ExitInterviewService() {
        this.conn            = DBConnection.getInstance().getConnection();
        this.employeeService = new EmployeeService();
        this.dummyMode       = DBConnection.getInstance().isDummyMode();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAVE / CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Records an exit interview for an employee.
     *
     * - Validates that the employee exists (throws InvalidEmployeeIdException if not).
     * - Warns (throws MissingFeedbackException) if feedbackText is empty.
     * - Marks the employee as EXITED automatically.
     *
     * @param interview ExitInterview object to persist.
     * @return The SQLite rowid of the inserted row.
     * @throws HRMSException.InvalidEmployeeIdException if the employee doesn't exist.
     * @throws HRMSException.MissingFeedbackException   if feedbackText is empty (WARNING level).
     */
    public int saveExitInterview(ExitInterview interview) {
        // Validate employee exists (by integer rowid via legacy path)
        Employee employee = employeeService.getEmployeeById(interview.getEmployeeId());

        // Validate primary reason
        if (interview.getPrimaryReason() == null || interview.getPrimaryReason().trim().isEmpty()) {
            throw new HRMSException.InvalidInputException("Exit reason (primary_reason) cannot be empty.");
        }

        // WARNING: feedbackText is empty (can be caught & handled by caller)
        if (interview.getFeedbackText() == null || interview.getFeedbackText().trim().isEmpty()) {
            throw new HRMSException.MissingFeedbackException(
                    "Feedback text is empty for employee ID " + interview.getEmployeeId() +
                    ". Submission allowed with warning.");
        }

        // Set exit_date if not provided
        if (interview.getExitDate() == null || interview.getExitDate().isEmpty()) {
            interview.setExitDate(LocalDate.now().toString());
        }

        // Generate a unique interview_id string
        String interviewId = "EI_" + System.currentTimeMillis();

        String sql = """
                INSERT INTO exit_interviews
                    (interview_id, emp_id, primary_reason, feedback_text, exit_date)
                VALUES (?, ?, ?, ?, ?)
                """;

        int generatedRowId = -1;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, interviewId);
            ps.setString(2, employee.getEmpId());
            ps.setString(3, interview.getPrimaryReason());
            ps.setString(4, interview.getFeedbackText() != null ? interview.getFeedbackText() : "");
            ps.setString(5, interview.getExitDate());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                generatedRowId = keys.getInt(1);
            }
            System.out.println("[ExitInterviewService] Exit interview saved: id=" + interviewId +
                               ", rowid=" + generatedRowId);
        } catch (SQLException e) {
            throw new RuntimeException("DB error saving exit interview: " + e.getMessage(), e);
        }

        // Mark employee as EXITED
        employeeService.markEmployeeAsExited(interview.getEmployeeId());

        return generatedRowId;
    }

    /**
     * Saves an exit interview even without feedbackText (after WARNING was shown to user).
     * Use this when the user acknowledges the missing feedback warning.
     */
    public int saveExitInterviewWithoutFeedback(ExitInterview interview) {
        // Validate employee exists
        Employee employee = employeeService.getEmployeeById(interview.getEmployeeId());

        if (interview.getPrimaryReason() == null || interview.getPrimaryReason().trim().isEmpty()) {
            throw new HRMSException.InvalidInputException("Exit reason (primary_reason) cannot be empty.");
        }

        if (interview.getExitDate() == null || interview.getExitDate().isEmpty()) {
            interview.setExitDate(LocalDate.now().toString());
        }

        String interviewId = "EI_" + System.currentTimeMillis();

        String sql = """
                INSERT INTO exit_interviews
                    (interview_id, emp_id, primary_reason, feedback_text, exit_date)
                VALUES (?, ?, ?, ?, ?)
                """;

        int generatedRowId = -1;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, interviewId);
            ps.setString(2, employee.getEmpId());
            ps.setString(3, interview.getPrimaryReason());
            ps.setString(4, "");
            ps.setString(5, interview.getExitDate());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                generatedRowId = keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error saving exit interview (no feedback): " + e.getMessage(), e);
        }

        employeeService.markEmployeeAsExited(interview.getEmployeeId());
        return generatedRowId;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves the most recent exit interview for a specific employee (by integer rowid).
     *
     * @param rowId the employee's SQLite rowid.
     * @return The most recent ExitInterview for that employee, or null if none.
     * @throws HRMSException.InvalidEmployeeIdException if employee doesn't exist.
     */
    public ExitInterview getInterviewByEmployee(int rowId) {
        // Validate employee exists
        Employee employee = employeeService.getEmployeeById(rowId);

        String sql = """
                SELECT rowid AS row_id, * FROM exit_interviews
                WHERE emp_id = ?
                ORDER BY row_id DESC LIMIT 1
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employee.getEmpId());
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
        if (dummyMode) {
            List<ExitInterview> list = new ArrayList<>();
            list.add(new ExitInterview(1, 2, "BETTER_OPPORTUNITY", "Moving to a larger firm.", "2023-08-15"));
            list.add(new ExitInterview(2, 4, "WORK_LIFE_BALANCE",  "Commute was too long.",   "2024-01-10"));
            list.add(new ExitInterview(3, 7, "CAREER_GROWTH",      "Felt stagnant in role.",  "2023-11-20"));
            return list;
        }

        List<ExitInterview> list = new ArrayList<>();
        String sql = "SELECT rowid AS row_id, * FROM exit_interviews ORDER BY row_id DESC";
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
     * Returns the count of employees who have at least one exit interview.
     * Used by AttritionService.
     */
    public int getExitedEmployeeCount() {
        if (dummyMode) return 3;

        // emp_id is the PK FK — count distinct employees with an exit interview
        String sql = "SELECT COUNT(DISTINCT emp_id) FROM exit_interviews";
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

    /**
     * Maps a ResultSet row to an ExitInterview model.
     *
     * Columns match com.hrms.db.entities.ExitInterview Hibernate entity:
     *   interview_id, emp_id, primary_reason, feedback_text,
     *   satisfaction_rating, issues_reported, interviewer_notes, exit_date
     */
    private ExitInterview mapResultSetToInterview(ResultSet rs) throws SQLException {
        return new ExitInterview(
                rs.getString("interview_id"),
                rs.getString("emp_id"),
                rs.getString("primary_reason"),
                rs.getString("feedback_text"),
                rs.getInt("satisfaction_rating") == 0 ? null : rs.getInt("satisfaction_rating"),
                rs.getString("issues_reported"),
                rs.getString("interviewer_notes"),
                rs.getString("exit_date")
        );
    }
}
