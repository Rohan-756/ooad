package com.hrms.service;

import com.hrms.exception.HRMSException;
import com.hrms.model.CorrelationReport;
import com.hrms.model.Employee;
import com.hrms.model.RootCauseFinding;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AnalyticsService — statistical analysis of employee attrition drivers.
 *
 * Implements {@link IAnalyticsService}.
 *
 * Notifies the DataEventBus after analyses complete so the DashboardController
 * can auto-refresh — this is the Observer pattern Subject side.
 *
 * SOLID: SRP — produces correlation and root-cause analytics only.
 *
 * Correlation methodology (simplified Pearson proxy):
 *   For each metric, compare the mean value in the exited cohort vs the active
 *   cohort, normalised to a [-1, 1] scale. Negative = lower metric → higher risk.
 */
public class AnalyticsService implements IAnalyticsService {

    private final EmployeeService employeeService;

    public AnalyticsService() {
        this.employeeService = new EmployeeService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORRELATION ANALYSIS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public CorrelationReport runCorrelationAnalysis(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new HRMSException.InvalidDateRangeException("Dates cannot be null.");
        }
        if (endDate.isBefore(startDate)) {
            throw new HRMSException.InvalidDateRangeException("End date must be >= start date.");
        }

        List<Employee> all     = employeeService.getAllEmployees();
        List<Employee> exited  = all.stream()
                .filter(e -> e.getEmploymentStatus() == Employee.EmploymentStatus.EXITED)
                .collect(Collectors.toList());
        List<Employee> active  = all.stream()
                .filter(e -> e.getEmploymentStatus() == Employee.EmploymentStatus.ACTIVE)
                .collect(Collectors.toList());

        if (exited.isEmpty()) {
            throw new HRMSException.InsufficientDataException(
                    "No exited employees found. Correlation analysis requires at least one exit record.");
        }

        // Mean attendance
        double avgExitedAtt = avg(exited, Employee::getAttendancePercentage);
        double avgActiveAtt  = avg(active, Employee::getAttendancePercentage);
        // Negative correlation: lower attendance in exited cohort → attendance is an attrition predictor
        double attendanceCorr = clamp((avgActiveAtt - avgExitedAtt) / 100.0, -1.0, 1.0);

        // Mean promotions
        double avgExitedProm = avg(exited, e -> (double) e.getPromotionCount());
        double avgActiveProm = avg(active, e -> (double) e.getPromotionCount());
        double promotionCorr = clamp(
                avgActiveProm > 0 ? (avgActiveProm - avgExitedProm) / avgActiveProm : 0.0,
                -1.0, 1.0);

        // Mean tenure
        double avgExitedYrs = avg(exited, e -> (double) e.getYearsOfService());
        double avgActiveYrs = avg(active, e -> (double) e.getYearsOfService());
        double tenureCorr = clamp(
                avgActiveYrs > 0 ? (avgActiveYrs - avgExitedYrs) / avgActiveYrs : 0.0,
                -1.0, 1.0);

        String summary = String.format(
                "Analysis on %d employees (%d exited, %d active). " +
                "Avg attendance — exited: %.1f%%, active: %.1f%%. " +
                "Avg promotions — exited: %.1f, active: %.1f. " +
                "Avg tenure (yrs) — exited: %.1f, active: %.1f.",
                all.size(), exited.size(), active.size(),
                avgExitedAtt, avgActiveAtt,
                avgExitedProm, avgActiveProm,
                avgExitedYrs, avgActiveYrs);

        CorrelationReport report = new CorrelationReport();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setAttendanceCorrelation(attendanceCorr);
        report.setPromotionCorrelation(promotionCorr);
        report.setTenureCorrelation(tenureCorr);
        report.setSummary(summary);
        report.setCreatedAt(Instant.now().toString());
        report.setTotalSample(all.size());
        report.setExitedSample(exited.size());

        // Notify observers — dashboard will auto-refresh
        DataEventBus.getInstance().notifyObservers("AnalyticsService");

        return report;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROOT CAUSE IDENTIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<RootCauseFinding> identifyRootCauses(LocalDate startDate, LocalDate endDate) {
        CorrelationReport report = runCorrelationAnalysis(startDate, endDate);

        List<RootCauseFinding> findings = new ArrayList<>();
        int id = 1;

        // Attendance
        double attImpact = Math.abs(report.getAttendanceCorrelation()) * 100.0;
        findings.add(new RootCauseFinding(id++, "attendance",
                String.format("Low attendance strongly correlates with exits " +
                        "(impact %.1f/100). Employees who left had %.1f%% lower average attendance.",
                        attImpact,
                        Math.abs((report.getAttendanceCorrelation() * 100.0))),
                attImpact));

        // Promotion gap
        double promImpact = Math.abs(report.getPromotionCorrelation()) * 100.0;
        findings.add(new RootCauseFinding(id++, "promotion",
                String.format("Lack of promotions is a significant driver " +
                        "(impact %.1f/100). Exited employees had fewer promotions on average.",
                        promImpact),
                promImpact));

        // Tenure
        double tenureImpact = Math.abs(report.getTenureCorrelation()) * 100.0;
        findings.add(new RootCauseFinding(id++, "tenure",
                String.format("Employees with shorter tenure are more likely to leave " +
                        "(impact %.1f/100). Focus onboarding and early engagement programs.",
                        tenureImpact),
                tenureImpact));

        // Sort highest impact first
        findings.sort((a, b) -> Double.compare(b.getImpactScore(), a.getImpactScore()));
        return findings;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEPARTMENT ATTRITION MAP
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Map<String, double[]> getDepartmentAttritionRates() {
        List<Employee> all = employeeService.getAllEmployees();

        if (all.isEmpty()) {
            return getDummyDeptRates();
        }

        // {department -> [total, exited, rate]}
        Map<String, long[]> raw = new LinkedHashMap<>();
        for (Employee e : all) {
            raw.computeIfAbsent(e.getDepartment(), k -> new long[]{0, 0});
            raw.get(e.getDepartment())[0]++;
            if (e.getEmploymentStatus() == Employee.EmploymentStatus.EXITED) {
                raw.get(e.getDepartment())[1]++;
            }
        }

        Map<String, double[]> result = new LinkedHashMap<>();
        for (Map.Entry<String, long[]> entry : raw.entrySet()) {
            long total  = entry.getValue()[0];
            long exited = entry.getValue()[1];
            double rate = total > 0 ? (exited * 100.0 / total) : 0.0;
            result.put(entry.getKey(), new double[]{total, exited, rate});
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private interface ToDouble<T> { double apply(T t); }

    private double avg(List<Employee> list, ToDouble<Employee> mapper) {
        if (list.isEmpty()) return 0.0;
        double sum = 0;
        for (Employee e : list) sum += mapper.apply(e);
        return sum / list.size();
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Dummy department rates for demo/dummy-mode. */
    private Map<String, double[]> getDummyDeptRates() {
        Map<String, double[]> m = new LinkedHashMap<>();
        m.put("Engineering", new double[]{40, 5,  12.5});
        m.put("Sales",       new double[]{30, 9,  30.0});
        m.put("Marketing",   new double[]{20, 3,  15.0});
        m.put("HR",          new double[]{15, 1,   6.7});
        m.put("Finance",     new double[]{25, 4,  16.0});
        m.put("Operations",  new double[]{20, 6,  30.0});
        m.put("Support",     new double[]{15, 2,  13.3});
        return m;
    }
}
