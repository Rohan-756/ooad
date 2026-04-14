package com.hrms.controller;

import com.hrms.exception.HRMSException;
import com.hrms.model.*;
import com.hrms.service.*;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DashboardController — MVC Controller + Observer for the Analytics Dashboard.
 *
 * Design Patterns:
 *   Observer  → implements DataChangeObserver; registered with DataEventBus so
 *               the dashboard auto-refreshes when AttritionService, RiskService,
 *               or AnalyticsService publish data change events.
 *   Facade    → provides a single entry point for all Step-4 operations.
 *
 * SOLID:
 *   SRP — only orchestrates dashboard data retrieval and refresh coordination.
 *   DIP — depends on service interfaces, not concrete classes.
 *   OCP — new analytics features can be added without modifying the controller.
 *
 * Usage (from Swing UI):
 *   DashboardController ctrl = new DashboardController();
 *   ctrl.setRefreshCallback(() -> SwingUtilities.invokeLater(myRefreshMethod));
 *   DashboardSnapshot snap = ctrl.handleBuildDashboard(filter);
 */
public class DashboardController implements DataChangeObserver {

    private final IDashboardService    dashboardService;
    private final ISegmentationService segmentationService;
    private final IAnalyticsService    analyticsService;

    /**
     * Optional callback invoked on the Swing EDT when any observed service
     * fires a data-changed event.  Set by the UI layer via setRefreshCallback().
     */
    private Runnable refreshCallback;

    public DashboardController() {
        this.dashboardService    = new DashboardService();
        this.segmentationService = new SegmentationService();
        this.analyticsService    = new AnalyticsService();

        // Register this controller with the DataEventBus — Observer pattern
        DataEventBus.getInstance().addObserver(this);
        System.out.println("[DashboardController] Registered with DataEventBus.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OBSERVER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called automatically by DataEventBus when any service data changes.
     * Triggers a UI refresh on the Swing EDT if a callback is registered.
     */
    @Override
    public void onDataChanged(String eventSource) {
        System.out.println("[DashboardController] Observer notified by: " + eventSource);
        if (refreshCallback != null) {
            SwingUtilities.invokeLater(refreshCallback);
        }
    }

    /**
     * Registers the UI refresh runnable to be called when data changes.
     * Should be set immediately after construction by the Swing view.
     *
     * @param callback Runnable that triggers dashboard panel repaint.
     */
    public void setRefreshCallback(Runnable callback) {
        this.refreshCallback = callback;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DASHBOARD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the full dashboard snapshot for the given filter.
     *
     * @throws HRMSException.DataNotAvailableException if data cannot be loaded.
     */
    public DashboardSnapshot handleBuildDashboard(DashboardFilter filter) {
        return dashboardService.buildDashboard(filter);
    }

    /**
     * Exports the dashboard as a CSV file to the given path.
     */
    public void handleExport(Path outputPath) {
        dashboardService.exportExecutiveDashboard(outputPath);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEGMENTATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Applies a filter to the employee dataset.
     *
     * @throws HRMSException.InvalidFilterException if filterSpec is null.
     */
    public List<Employee> handleApplySegmentation(FilterSpec filterSpec) {
        return segmentationService.applySegmentation(filterSpec);
    }

    /**
     * Compares attrition rates between two segments.
     *
     * @throws HRMSException.InsufficientDataException if a segment is empty.
     */
    public SegmentComparison handleCompareSegments(FilterSpec first, FilterSpec second) {
        return segmentationService.compareSegments(first, second);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANALYTICS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Runs correlation analysis for the given date window.
     *
     * @throws HRMSException.InsufficientDataException if no exits exist.
     * @throws HRMSException.InvalidDateRangeException if dates are invalid.
     */
    public CorrelationReport handleRunCorrelation(LocalDate start, LocalDate end) {
        return analyticsService.runCorrelationAnalysis(start, end);
    }

    /**
     * Identifies root causes of attrition ordered by impact score.
     */
    public List<RootCauseFinding> handleIdentifyRootCauses(LocalDate start, LocalDate end) {
        return analyticsService.identifyRootCauses(start, end);
    }

    /**
     * Returns per-department attrition rates for the bar chart.
     */
    public Map<String, double[]> handleGetDepartmentRates() {
        return analyticsService.getDepartmentAttritionRates();
    }
}
