package com.hrms.service;

import com.hrms.model.DashboardFilter;
import com.hrms.model.DashboardSnapshot;

import java.nio.file.Path;

/**
 * Contract for the Executive Dashboard aggregation service.
 *
 * Aligned with the shared IExecutiveDashboard interface spec.
 */
public interface IDashboardService {

    /**
     * Builds a complete DashboardSnapshot for the given filter scope.
     *
     * Aggregates data from AttritionService, RiskService, and AnalyticsService.
     *
     * @param filter Scope filter (date range, department, period type).
     * @return Fully-populated DashboardSnapshot ready for UI rendering.
     * @throws com.hrms.exception.HRMSException.DataNotAvailableException if data cannot be loaded.
     */
    DashboardSnapshot buildDashboard(DashboardFilter filter);

    /**
     * Exports the executive dashboard as a CSV summary to the specified path.
     *
     * @param outputFile Target file path.
     */
    void exportExecutiveDashboard(Path outputFile);
}
