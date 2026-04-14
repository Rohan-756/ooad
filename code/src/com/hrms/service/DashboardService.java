package com.hrms.service;

import com.hrms.db.DBConnection;
import com.hrms.exception.HRMSException;
import com.hrms.model.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DashboardService — aggregates data from all subsystems into a DashboardSnapshot.
 *
 * Implements {@link IDashboardService}.
 *
 * Acts as a facade over AttritionService, RiskService, and AnalyticsService.
 * Uses the DataEventBus to participate in the Observer pattern — notifies
 * observers after building a fresh snapshot so DashboardController can update.
 *
 * SOLID: SRP — only responsible for assembling the executive dashboard.
 * GoF Patterns used: Singleton (DBConnection), Observer (DataEventBus).
 */
public class DashboardService implements IDashboardService {

    private final AttritionService   attritionService;
    private final RiskService        riskService;
    private final AnalyticsService   analyticsService;
    private final EmployeeService    employeeService;
    private final boolean            dummyMode;

    public DashboardService() {
        this.dummyMode        = DBConnection.getInstance().isDummyMode();
        this.employeeService  = new EmployeeService();
        this.attritionService = new AttritionService();
        this.riskService      = new RiskService();
        this.analyticsService = new AnalyticsService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD DASHBOARD
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a fully-populated DashboardSnapshot for the given filter scope.
     *
     * Gracefully degrades each section — if one service fails (e.g. insufficient
     * data), others still populate.
     *
     * @throws HRMSException.DataNotAvailableException on catastrophic failure.
     */
    @Override
    public DashboardSnapshot buildDashboard(DashboardFilter filter) {
        if (filter == null) {
            throw new HRMSException.DataNotAvailableException(
                    "DashboardFilter cannot be null.");
        }

        DashboardSnapshot snap = new DashboardSnapshot();
        snap.setGeneratedAt(Instant.now().toString());

        // 1. Employee counts
        List<Employee> allEmployees = employeeService.getAllEmployees();
        int total  = allEmployees.size();
        int exited = (int) allEmployees.stream()
                .filter(e -> e.getEmploymentStatus() == Employee.EmploymentStatus.EXITED)
                .count();
        int active = total - exited;
        double overallRate = total > 0 ? (exited * 100.0 / total) : 0.0;

        snap.setTotalEmployees(total);
        snap.setTotalExits(exited);
        snap.setActiveEmployees(active);
        snap.setOverallAttritionRate(overallRate);

        // 2. Attrition trend
        try {
            List<AttritionRecord> trend = attritionService.generateTrendData(
                    filter.getPeriodType(), filter.getStartDate(), filter.getEndDate());
            snap.setTrendData(trend);
        } catch (HRMSException e) {
            System.err.println("[DashboardService] Trend data unavailable: " + e.getMessage());
            snap.setTrendData(Collections.emptyList());
        }

        // 3. High-risk employees
        try {
            List<RiskAssessment> highRisk = riskService.getFlaggedEmployees(RiskLevel.HIGH);
            snap.setHighRiskEmployees(highRisk);
            snap.setHighRiskCount(highRisk.size());
        } catch (Exception e) {
            System.err.println("[DashboardService] Risk data unavailable: " + e.getMessage());
            snap.setHighRiskEmployees(Collections.emptyList());
            snap.setHighRiskCount(0);
        }

        // 4. Correlation analysis
        try {
            CorrelationReport corr = analyticsService.runCorrelationAnalysis(
                    filter.getStartDate(), filter.getEndDate());
            snap.setCorrelationReport(corr);
        } catch (HRMSException e) {
            System.err.println("[DashboardService] Correlation unavailable: " + e.getMessage());
        }

        // 5. Root causes
        try {
            List<RootCauseFinding> causes = analyticsService.identifyRootCauses(
                    filter.getStartDate(), filter.getEndDate());
            snap.setRootCauses(causes);
        } catch (HRMSException e) {
            System.err.println("[DashboardService] Root causes unavailable: " + e.getMessage());
            snap.setRootCauses(Collections.emptyList());
        }

        // 6. Department rates
        Map<String, double[]> deptRates = analyticsService.getDepartmentAttritionRates();
        snap.setDepartmentAttritionRates(deptRates);

        // Notify dashboard observers
        DataEventBus.getInstance().notifyObservers("DashboardService");

        System.out.printf("[DashboardService] Snapshot built: %d employees, %.1f%% attrition%n",
                total, overallRate);
        return snap;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXPORT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Exports the dashboard as a simple CSV to the given path.
     * Contains: KPI summary + department attrition rates + correlation scores.
     */
    @Override
    public void exportExecutiveDashboard(Path outputFile) {
        DashboardFilter filter = new DashboardFilter(); // default last 12 months
        DashboardSnapshot snap = buildDashboard(filter);

        try (PrintWriter pw = new PrintWriter(outputFile.toFile())) {
            pw.println("HRMS Attrition Analysis — Executive Dashboard Export");
            pw.println("Generated: " + snap.getGeneratedAt());
            pw.println();
            pw.println("=== KPI Summary ===");
            pw.printf("Total Employees,%d%n",  snap.getTotalEmployees());
            pw.printf("Active Employees,%d%n", snap.getActiveEmployees());
            pw.printf("Total Exits,%d%n",      snap.getTotalExits());
            pw.printf("High Risk Count,%d%n",  snap.getHighRiskCount());
            pw.printf("Overall Attrition Rate,%.2f%%%n", snap.getOverallAttritionRate());
            pw.println();
            pw.println("=== Department Attrition Rates ===");
            pw.println("Department,Total,Exits,Rate%");
            if (snap.getDepartmentAttritionRates() != null) {
                snap.getDepartmentAttritionRates().forEach((dept, vals) ->
                        pw.printf("%s,%.0f,%.0f,%.2f%n", dept, vals[0], vals[1], vals[2]));
            }
            pw.println();
            if (snap.getCorrelationReport() != null) {
                CorrelationReport cr = snap.getCorrelationReport();
                pw.println("=== Correlation Analysis ===");
                pw.printf("Attendance Correlation,%.3f%n", cr.getAttendanceCorrelation());
                pw.printf("Promotion Correlation,%.3f%n",  cr.getPromotionCorrelation());
                pw.printf("Tenure Correlation,%.3f%n",     cr.getTenureCorrelation());
                pw.println("Summary," + cr.getSummary());
            }
            System.out.println("[DashboardService] Dashboard exported to: " + outputFile);
        } catch (IOException e) {
            throw new HRMSException.DataNotAvailableException(
                    "Failed to export dashboard: " + e.getMessage());
        }
    }
}
