package com.hrms.service;

import com.hrms.model.RiskAssessment;
import com.hrms.model.RiskLevel;

import java.util.List;

/**
 * Service contract for the Risk Evaluation subsystem.
 *
 * Implementations must:
 *  - Evaluate a single employee's risk using the Strategy + Factory pattern.
 *  - Return all assessments filtered by a given RiskLevel.
 *  - Batch-evaluate every active employee.
 */
public interface IRiskClassification {

    /**
     * Evaluates and persists the risk level for a single employee.
     *
     * @param employeeId The employee to evaluate.
     * @return The resulting RiskAssessment.
     * @throws com.hrms.exception.HRMSException.InvalidEmployeeIdException if not found.
     * @throws com.hrms.exception.HRMSException.MissingDataException if employee data is incomplete.
     */
    RiskAssessment evaluateRisk(int employeeId);

    /**
     * Returns all stored assessments for a specific risk level.
     *
     * @param level LOW, MEDIUM, or HIGH.
     * @return List of matching RiskAssessments (may be empty, never null).
     */
    List<RiskAssessment> getFlaggedEmployees(RiskLevel level);

    /**
     * Evaluates every active employee and stores results.
     *
     * @return List of all resulting RiskAssessments.
     */
    List<RiskAssessment> evaluateAll();
}
