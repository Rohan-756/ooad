package com.hrms.service;

import com.hrms.model.Employee;
import com.hrms.model.RiskAssessment;
import com.hrms.model.RiskLevel;

import java.time.Instant;

/**
 * HighRiskStrategy — assigned when the employee has:
 *   - Attendance < 65%
 *
 * Design Pattern: Strategy (concrete implementation)
 */
public class HighRiskStrategy implements RiskStrategy {

    @Override
    public RiskAssessment evaluate(Employee employee) {
        double attendance  = employee.getAttendancePercentage();
        int    promotions  = employee.getPromotionCount();
        double absenteeism = 100.0 - attendance;

        String reason = String.format(
                "High risk: attendance %.1f%% is critically low (<65%%). " +
                "Absenteeism rate %.1f%% suggests disengagement or external job search. " +
                "%s",
                attendance, absenteeism,
                promotions == 0
                        ? "Combined with 0 promotions, immediate HR intervention recommended."
                        : "Immediate HR intervention recommended.");

        RiskAssessment ra = new RiskAssessment();
        ra.setEmployeeId(employee.getEmployeeId());
        ra.setRiskLevel(RiskLevel.HIGH);
        ra.setReason(reason);
        ra.setPerformanceScore(attendance);
        ra.setAbsenteeismRate(absenteeism);
        ra.setPromotionGap(promotions);
        ra.setEvaluatedAt(Instant.now().toString());
        return ra;
    }
}
