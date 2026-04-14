package com.hrms.service;

import com.hrms.model.Employee;
import com.hrms.model.RiskAssessment;

/**
 * Strategy interface for employee risk evaluation.
 *
 * Design Pattern: Strategy (GoF)
 * Implementations: LowRiskStrategy, MediumRiskStrategy, HighRiskStrategy
 * Selector:        RiskFactory
 *
 * Each concrete strategy encapsulates its own scoring logic and produces
 * a fully-populated RiskAssessment for the given employee.
 */
public interface RiskStrategy {

    /**
     * Evaluates the risk of the given employee and returns a populated
     * RiskAssessment (without a persisted ID — that is assigned by RiskService).
     *
     * @param employee The employee to evaluate.
     * @return A RiskAssessment with level, reason, and metric snapshots.
     */
    RiskAssessment evaluate(Employee employee);
}
