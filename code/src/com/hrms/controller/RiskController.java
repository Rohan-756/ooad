package com.hrms.controller;

import com.hrms.exception.HRMSException;
import com.hrms.model.RiskAssessment;
import com.hrms.model.RiskLevel;
import com.hrms.service.IRiskClassification;
import com.hrms.service.RiskService;

import java.util.List;

/**
 * RiskController — MVC Controller for the Risk Evaluation subsystem.
 *
 * Bridges the Swing UI and RiskService.
 * Parses raw UI inputs (strings, IDs) into typed domain objects,
 * delegates to the service, and surfaces structured results or exceptions.
 *
 * SOLID: SRP — only routes risk-related events.
 * GRASP: Controller pattern.
 */
public class RiskController {

    private final IRiskClassification service;

    public RiskController() {
        this.service = new RiskService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SINGLE EVALUATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Evaluates and persists the risk level for a single employee.
     *
     * @param employeeId Employee ID from the UI input field.
     * @return The resulting RiskAssessment.
     * @throws HRMSException.InvalidEmployeeIdException if employee not found.
     * @throws HRMSException.MissingDataException       if data is incomplete.
     * @throws HRMSException.InvalidInputException      if employeeId <= 0.
     */
    public RiskAssessment handleEvaluateRisk(int employeeId) {
        if (employeeId <= 0) {
            throw new HRMSException.InvalidInputException(
                    "Employee ID must be a positive integer. Got: " + employeeId);
        }
        return service.evaluateRisk(employeeId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BULK EVALUATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Evaluates risk for all employees in the system.
     *
     * @return List of all RiskAssessments produced.
     * @throws HRMSException.MissingDataException if no employees exist.
     */
    public List<RiskAssessment> handleEvaluateAll() {
        return service.evaluateAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY BY LEVEL
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns assessments filtered to the given risk level string from the UI.
     *
     * @param levelStr "HIGH", "MEDIUM", or "LOW" (case-insensitive).
     * @return Matching assessments.
     * @throws HRMSException.InvalidInputException if levelStr is unrecognised.
     */
    public List<RiskAssessment> handleGetFlagged(String levelStr) {
        RiskLevel level = parseLevel(levelStr);
        return service.getFlaggedEmployees(level);
    }

    /**
     * Returns all assessments regardless of level.
     */
    public List<RiskAssessment> handleGetAll() {
        return ((RiskService) service).getAllAssessments();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private RiskLevel parseLevel(String s) {
        if (s == null) throw new HRMSException.InvalidInputException("Risk level cannot be null.");
        switch (s.trim().toUpperCase()) {
            case "LOW":    return RiskLevel.LOW;
            case "MEDIUM": return RiskLevel.MEDIUM;
            case "HIGH":   return RiskLevel.HIGH;
            default:
                throw new HRMSException.InvalidInputException(
                        "Unrecognised risk level: \"" + s + "\". Use LOW, MEDIUM, or HIGH.");
        }
    }
}
