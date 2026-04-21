package com.hrms.service;

import com.hrms.exception.HRMSException;
import com.hrms.model.Employee;
import com.hrms.model.FilterSpec;
import com.hrms.model.SegmentComparison;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SegmentationService — filters and compares employee segments.
 *
 * Implements {@link ISegmentationService}.
 *
 * Uses Java Stream API for in-memory filtering of the employee dataset —
 * efficient for datasets of typical enterprise size (< 100 k employees).
 *
 * SOLID: SRP — only handles segmentation logic.
 * SOLID: DIP — depends on EmployeeService abstraction.
 */
public class SegmentationService implements ISegmentationService {

    private final EmployeeService employeeService;

    public SegmentationService() {
        this.employeeService = new EmployeeService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEGMENTATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Filters the employee dataset using all non-null criteria in filterSpec.
     *
     * @throws HRMSException.InvalidFilterException if filterSpec is null.
     */
    @Override
    public List<Employee> applySegmentation(FilterSpec filterSpec) {
        if (filterSpec == null) {
            throw new HRMSException.InvalidFilterException(
                    "FilterSpec cannot be null. Provide at least one filter criterion.");
        }

        List<Employee> all = employeeService.getAllEmployees();

        return all.stream()
                .filter(e -> filterSpec.getDepartment() == null ||
                             e.getDepartment().equalsIgnoreCase(filterSpec.getDepartment()))
                .filter(e -> filterSpec.getEmploymentStatus() == null ||
                             e.getEmploymentStatus().name()
                              .equalsIgnoreCase(filterSpec.getEmploymentStatus()))
                .filter(e -> filterSpec.getMinYearsOfService() == null ||
                             e.getYearsOfService() >= filterSpec.getMinYearsOfService())
                .filter(e -> filterSpec.getMaxYearsOfService() == null ||
                             e.getYearsOfService() <= filterSpec.getMaxYearsOfService())
                .filter(e -> filterSpec.getMinPromotions() == null ||
                             e.getMonthsSincePromotion() >= filterSpec.getMinPromotions())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEGMENT COMPARISON
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Computes attrition rates for two segments and returns the comparison.
     *
     * @throws HRMSException.InvalidFilterException    if either spec is null.
     * @throws HRMSException.InsufficientDataException if a segment has no employees.
     */
    @Override
    public SegmentComparison compareSegments(FilterSpec first, FilterSpec second) {
        if (first == null || second == null) {
            throw new HRMSException.InvalidFilterException(
                    "Both FilterSpec arguments must be non-null for segment comparison.");
        }

        List<Employee> segA = applySegmentation(first);
        List<Employee> segB = applySegmentation(second);

        if (segA.isEmpty()) {
            throw new HRMSException.InsufficientDataException(
                    "Segment A ('" + first.getLabel() + "') has no employees — cannot compare.");
        }
        if (segB.isEmpty()) {
            throw new HRMSException.InsufficientDataException(
                    "Segment B ('" + second.getLabel() + "') has no employees — cannot compare.");
        }

        long exitsA = segA.stream()
                .filter(e -> e.getEmploymentStatus() == Employee.EmploymentStatus.EXITED)
                .count();
        long exitsB = segB.stream()
                .filter(e -> e.getEmploymentStatus() == Employee.EmploymentStatus.EXITED)
                .count();

        double rateA = (exitsA * 100.0) / segA.size();
        double rateB = (exitsB * 100.0) / segB.size();

        String labelA = first.getLabel()  != null ? first.getLabel()  : first.toString();
        String labelB = second.getLabel() != null ? second.getLabel() : second.toString();

        return new SegmentComparison(
                labelA, labelB, rateA, rateB,
                segA.size(), segB.size(), (int) exitsA, (int) exitsB);
    }
}
