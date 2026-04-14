package com.hrms.service;

import com.hrms.model.CorrelationReport;
import com.hrms.model.RootCauseFinding;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Contract for the Analytics subsystem — correlation analysis and root causes.
 *
 * Aligned with the shared ICorrelationReport interface spec.
 */
public interface IAnalyticsService {

    /**
     * Runs a correlation analysis between employee attributes and attrition
     * within the specified date window.
     *
     * @param startDate Start of the analysis window.
     * @param endDate   End of the analysis window.
     * @return A CorrelationReport with attendance, promotion, and tenure correlations.
     * @throws com.hrms.exception.HRMSException.InvalidDateRangeException if dates are invalid.
     * @throws com.hrms.exception.HRMSException.InsufficientDataException if no exited employees exist.
     */
    CorrelationReport runCorrelationAnalysis(LocalDate startDate, LocalDate endDate);

    /**
     * Identifies the top root-cause factors driving attrition, ordered by impact score.
     *
     * @param startDate Start of the analysis window.
     * @param endDate   End of the analysis window.
     * @return Ordered list of RootCauseFinding (highest impact first).
     */
    List<RootCauseFinding> identifyRootCauses(LocalDate startDate, LocalDate endDate);

    /**
     * Returns per-department attrition rates as a map.
     * Each key is a department name; value is {totalCount, exitCount, attritionRate%}.
     */
    Map<String, double[]> getDepartmentAttritionRates();
}
