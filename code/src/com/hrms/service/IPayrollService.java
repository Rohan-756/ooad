package com.hrms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * IPayrollService — integration contract between the Attrition-Risk subsystem
 * and the Payroll Management subsystem.
 *
 * The Payroll team will provide the concrete implementation of this interface.
 * This subsystem consumes it to correlate compensation data with attrition risk
 * (e.g., flagging employees whose salary has not kept pace with tenure or
 * performance, which is a known attrition driver).
 *
 * SOLID: DIP — we depend on this abstraction, not on any concrete payroll class.
 */
public interface IPayrollService {

    /**
     * Returns the most recent net salary (after all taxes, deductions, and
     * allowances) for the given employee.
     *
     * @param employeeId Unique identifier of the employee.
     * @return Net salary amount in the organisation's base currency.
     * @throws com.hrms.exception.HRMSException if the employee is not found or
     *         payroll records are unavailable.
     */
    double getNetSalary(String employeeId);

    /**
     * Returns the full salary breakdown for a given employee and pay period,
     * including gross pay, individual deductions, bonuses, and allowances.
     * Keys are component names (e.g. "basic", "hra", "tax", "bonus");
     * values are the corresponding monetary amounts.
     *
     * @param employeeId Unique identifier of the employee.
     * @param payPeriod  The month/pay-period start date (day is ignored).
     * @return Immutable map of salary component name → amount.
     */
    Map<String, Double> getSalaryBreakdown(String employeeId, LocalDate payPeriod);

    /**
     * Returns the average net salary across all employees in the specified
     * department, used for compensation-vs-attrition correlation analytics.
     *
     * @param department Department name (must match the value stored in Employee).
     * @return Average net salary for that department, or 0.0 if no data exists.
     */
    double getAverageSalaryByDepartment(String department);

    /**
     * Returns payslip records for a given employee over a date range.
     * Each entry in the list represents one pay period's payslip summary,
     * containing at minimum the period start date and net salary paid.
     *
     * @param employeeId Unique identifier of the employee.
     * @param from       Start of the date range (inclusive).
     * @param to         End of the date range (inclusive).
     * @return List of payslip summaries ordered by pay period ascending.
     */
    List<PayslipSummary> getPayslipHistory(String employeeId, LocalDate from, LocalDate to);

    // ─────────────────────────────────────────────────────────────────────────
    // Nested DTO — deliberately minimal so neither team leaks domain details.
    // The Payroll team may extend this in their implementation.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lightweight summary of a single pay period's payslip.
     * Used only for integration data exchange — not for rendering a full payslip.
     */
    interface PayslipSummary {
        /** The first day of the pay period this payslip covers. */
        LocalDate getPayPeriodStart();

        /** Net amount paid to the employee after all deductions. */
        double getNetPaid();

        /** Total bonuses included in this payslip, if any. */
        double getTotalBonus();
    }
}
