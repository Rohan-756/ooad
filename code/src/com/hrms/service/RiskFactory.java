package com.hrms.service;

import com.hrms.model.Employee;

/**
 * RiskFactory — Factory Pattern implementation.
 *
 * Centralises the decision of which RiskStrategy to apply for a given
 * employee, keeping the selection logic in one place and decoupled from
 * both the strategies and the service layer.
 *
 * Design Pattern: Factory (GoF)
 *
 * Selection rules:
 *   - attendance < 65%             → HighRiskStrategy
 *   - attendance 65–84% OR 0 promos → MediumRiskStrategy
 *   - attendance >= 85% AND promos>0 → LowRiskStrategy
 */
public class RiskFactory {

    private RiskFactory() {} // utility class — no instantiation

    /**
     * Returns the appropriate RiskStrategy for the given employee.
     *
     * @param employee The employee to be evaluated.
     * @return A concrete RiskStrategy instance.
     */
    public static RiskStrategy getStrategy(Employee employee) {
        double attendance = employee.getAttendanceRate();
        int    promotions = employee.getMonthsSincePromotion();

        if (attendance < 65.0) {
            return new HighRiskStrategy();
        } else if (attendance < 85.0 || promotions == 0) {
            return new MediumRiskStrategy();
        } else {
            return new LowRiskStrategy();
        }
    }
}
