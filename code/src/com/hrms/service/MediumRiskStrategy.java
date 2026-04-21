package com.hrms.service;

import com.hrms.model.Employee;
import com.hrms.model.RiskAssessment;
import com.hrms.model.RiskLevel;

import java.time.Instant;

/**
 * MediumRiskStrategy — assigned when the employee has:
 *   - Attendance between 65% and 84%  OR
 *   - Attendance >= 85% but zero promotions
 *
 * Design Pattern: Strategy (concrete implementation)
 */
public class MediumRiskStrategy implements RiskStrategy {

    @Override
    public RiskAssessment evaluate(Employee employee) {
        double attendance  = employee.getAttendanceRate();
        int    promotions  = employee.getMonthsSincePromotion();
        double absenteeism = 100.0 - attendance;

        StringBuilder sb = new StringBuilder("Medium risk: ");
        if (attendance >= 65 && attendance < 85) {
            sb.append(String.format("attendance %.1f%% is moderate (65–84%%).", attendance));
        }
        if (promotions == 0) {
            if (sb.length() > "Medium risk: ".length()) sb.append(" Also, ");
            sb.append("no promotions recorded — career stagnation may be a factor.");
        }

        RiskAssessment ra = new RiskAssessment();
        ra.setEmployeeId(employee.getEmployeeId());
        ra.setRiskLevel(RiskLevel.MEDIUM);
        ra.setReason(sb.toString());
        ra.setPerformanceScore(attendance);
        ra.setAbsenteeismRate(absenteeism);
        ra.setPromotionGap(promotions);
        ra.setEvaluatedAt(Instant.now().toString());
        return ra;
    }
}
