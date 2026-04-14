package com.hrms.service;

import com.hrms.db.DBConnection;
import com.hrms.exception.HRMSException;
import com.hrms.model.Employee;
import com.hrms.model.RiskAssessment;
import com.hrms.model.RiskLevel;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * RiskService — Business Logic Layer for the Risk Evaluation subsystem.
 *
 * Implements {@link IRiskClassification}.
 *
 * Design Patterns used:
 *   - Strategy  → selects scoring algorithm via RiskFactory
 *   - Factory   → RiskFactory.getStrategy(employee)
 *   - Singleton → DBConnection for DB access
 *
 * SOLID:
 *   - SRP: only evaluates and persists risk assessments.
 *   - OCP: new risk levels can be added by implementing RiskStrategy.
 *   - DIP: depends on IRiskClassification, RiskStrategy, and DBConnection.
 */
public class RiskService implements IRiskClassification {

    private final Connection conn;
    private final boolean    dummyMode;
    private final EmployeeService employeeService;

    public RiskService() {
        this.conn            = DBConnection.getInstance().getConnection();
        this.dummyMode       = DBConnection.getInstance().isDummyMode();
        this.employeeService = new EmployeeService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EVALUATE SINGLE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Evaluates and persists the risk for a single employee.
     * Delegates scoring to the strategy selected by RiskFactory.
     *
     * @throws HRMSException.InvalidEmployeeIdException if employee not found.
     * @throws HRMSException.MissingDataException       if attendance/promotion data is missing.
     */
    @Override
    public RiskAssessment evaluateRisk(int employeeId) {
        Employee employee = employeeService.getEmployeeById(employeeId);

        // Guard: employee must have valid numeric data
        if (employee.getAttendancePercentage() < 0) {
            throw new HRMSException.MissingDataException(
                    "Attendance data is missing for employee ID " + employeeId);
        }

        // Strategy Pattern: RiskFactory picks the right algorithm
        RiskStrategy strategy = RiskFactory.getStrategy(employee);
        RiskAssessment assessment = strategy.evaluate(employee);

        // Persist and return with generated ID
        int generatedId = persist(assessment);
        assessment.setRiskAssessmentId(generatedId);

        // Notify dashboard observers — Observer Pattern
        DataEventBus.getInstance().notifyObservers("RiskService");

        System.out.printf("[RiskService] Employee %d evaluated → %s%n",
                employeeId, assessment.getRiskLevel());
        return assessment;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EVALUATE ALL
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Evaluates every employee currently in the system and stores results.
     *
     * @return List of all RiskAssessments produced.
     */
    @Override
    public List<RiskAssessment> evaluateAll() {
        List<Employee> employees = employeeService.getAllEmployees();
        if (employees.isEmpty()) {
            throw new HRMSException.MissingDataException(
                    "No employees found in the system. Cannot run bulk risk evaluation.");
        }

        List<RiskAssessment> results = new ArrayList<>();
        for (Employee e : employees) {
            try {
                results.add(evaluateRisk(e.getEmployeeId()));
            } catch (HRMSException ex) {
                System.err.println("[RiskService] Skipping employee " +
                        e.getEmployeeId() + ": " + ex.getMessage());
            }
        }
        // Notify dashboard observers — Observer Pattern
        DataEventBus.getInstance().notifyObservers("RiskService");

        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY BY LEVEL
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all stored assessments matching the given RiskLevel.
     * Fetches the most recent assessment per employee.
     */
    @Override
    public List<RiskAssessment> getFlaggedEmployees(RiskLevel level) {
        if (level == null) {
            throw new HRMSException.InvalidInputException("RiskLevel cannot be null.");
        }

        List<RiskAssessment> list = new ArrayList<>();

        if (dummyMode || conn == null) {
            // Fallback: return dummy data when DB is unavailable
            return getDummyAssessments(level);
        }

        // Query: get the latest assessment per employee for the given level
        String sql = """
                SELECT * FROM risk_assessments
                WHERE risk_level = ?
                ORDER BY assessment_id DESC
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, level.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error querying risk assessments: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Returns all stored risk assessments regardless of level.
     */
    public List<RiskAssessment> getAllAssessments() {
        List<RiskAssessment> list = new ArrayList<>();

        if (dummyMode || conn == null) {
            List<RiskAssessment> all = new ArrayList<>();
            all.addAll(getDummyAssessments(RiskLevel.HIGH));
            all.addAll(getDummyAssessments(RiskLevel.MEDIUM));
            all.addAll(getDummyAssessments(RiskLevel.LOW));
            return all;
        }

        String sql = "SELECT * FROM risk_assessments ORDER BY assessment_id DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("DB error fetching all risk assessments: " + e.getMessage(), e);
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PERSISTENCE
    // ─────────────────────────────────────────────────────────────────────────

    private int persist(RiskAssessment ra) {
        if (dummyMode || conn == null) {
            // Dummy mode: return a fake ID
            return (int)(Math.random() * 10000);
        }

        String sql = """
                INSERT INTO risk_assessments
                    (employee_id, risk_level, reason, assessed_date)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,    ra.getEmployeeId());
            ps.setString(2, ra.getRiskLevel().name());
            ps.setString(3, ra.getReason());
            ps.setString(4, ra.getEvaluatedAt());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("DB error persisting risk assessment: " + e.getMessage(), e);
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private RiskAssessment mapRow(ResultSet rs) throws SQLException {
        RiskAssessment ra = new RiskAssessment();
        ra.setRiskAssessmentId(rs.getInt("assessment_id"));
        ra.setEmployeeId(rs.getInt("employee_id"));
        ra.setRiskLevel(RiskLevel.valueOf(rs.getString("risk_level")));
        ra.setReason(rs.getString("reason"));
        ra.setEvaluatedAt(rs.getString("assessed_date"));
        return ra;
    }

    /** Provides sensible dummy assessments when running without a DB. */
    private List<RiskAssessment> getDummyAssessments(RiskLevel level) {
        List<RiskAssessment> dummy = new ArrayList<>();
        String ts = Instant.now().toString();
        switch (level) {
            case HIGH:
                dummy.add(new RiskAssessment(1, 101, RiskLevel.HIGH,
                        "High risk: attendance 45.0% is critically low (<65%).", 45.0, 55.0, 0, ts));
                dummy.add(new RiskAssessment(2, 105, RiskLevel.HIGH,
                        "High risk: attendance 58.0% is critically low (<65%).", 58.0, 42.0, 1, ts));
                break;
            case MEDIUM:
                dummy.add(new RiskAssessment(3, 102, RiskLevel.MEDIUM,
                        "Medium risk: attendance 72.0% is moderate (65–84%).", 72.0, 28.0, 0, ts));
                dummy.add(new RiskAssessment(4, 106, RiskLevel.MEDIUM,
                        "Medium risk: no promotions recorded.", 88.0, 12.0, 0, ts));
                break;
            case LOW:
                dummy.add(new RiskAssessment(5, 103, RiskLevel.LOW,
                        "Low risk: attendance 92.0% (≥85%) and 3 promotion(s).", 92.0, 8.0, 3, ts));
                break;
        }
        return dummy;
    }
}
