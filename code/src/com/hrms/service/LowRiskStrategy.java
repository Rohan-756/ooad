package com.hrms.service;

import com.hrms.model.Employee;
import com.hrms.model.RiskAssessment;
import com.hrms.model.RiskLevel;

import java.time.Instant;

/**
 * LowRiskStrategy — assigned when the employee has:
 *   - Attendance >= 85%  AND
 *   - At least 1 promotion
 *
 * Design Pattern: Strategy (concrete implementation)
 */
public class LowRiskStrategy implements RiskStrategy {

    @Override
    public RiskAssessment evaluate(Employee employee) {
        double attendance    = employee.getAttendancePercentage();
        int    promotions    = employee.getPromotionCount();
        double absenteeism   = 100.0 - attendance;

        String reason = String.format(
                "Low risk: attendance %.1f%% (≥85%%) and %d promotion(s) — employee is engaged and stable.",
                attendance, promotions);

        RiskAssessment ra = new RiskAssessment();
        ra.setEmployeeId(employee.getEmployeeId());
        ra.setRiskLevel(RiskLevel.LOW);
        ra.setReason(reason);
        ra.setPerformanceScore(attendance);
        ra.setAbsenteeismRate(absenteeism);
        ra.setPromotionGap(promotions);
        ra.setEvaluatedAt(Instant.now().toString());
        return ra;
    }
}
