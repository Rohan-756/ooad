package com.hrms.service;

import com.hrms.db.DBConnection;
import com.hrms.exception.HRMSException;
import com.hrms.model.AttritionRecord;
import com.hrms.model.PeriodType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * AttritionService — implements business logic to compute attrition and trends.
 *
 * Schema note (aligned with hrms-database.jar entities):
 *   - employees table uses `date_of_joining` (not `hire_date`).
 *   - There is NO `termination_date` column in the employees table.
 *   - Employee exits are tracked via the `ExitInterview` table using the `exit_date` column.
 *     Hibernate table name for the entity is `ExitInterview` (class name, not snake_case).
 */
public class AttritionService implements IAttritionRate {

    private final Connection conn;
    private final boolean dummyMode;

    public AttritionService() {
        this.conn = DBConnection.getInstance().getConnection();
        this.dummyMode = DBConnection.getInstance().isDummyMode();
    }

    @Override
    public AttritionRecord calculateAttritionRate(PeriodType periodType, LocalDate startDate, LocalDate endDate)
            throws HRMSException {
        if (startDate == null || endDate == null)
            throw new HRMSException.InvalidDateRangeException("Start date and end date cannot be null.");
        if (endDate.isBefore(startDate))
            throw new HRMSException.InvalidDateRangeException("End date must be on or after start date.");

        // For an overall period, compute totals by SQL
        int total = countEmployeesEmployedDuring(startDate, endDate);
        int left  = countExitsWithin(startDate, endDate);

        if (total == 0)
            throw new HRMSException.DivideByZeroException(
                    "No employees found in the period " + startDate + " to " + endDate +
                    ". Cannot compute attrition rate.");
        double rate = (left / (double) total) * 100.0;

        AttritionRecord result = new AttritionRecord(startDate, endDate, total, left, rate);

        // Notify dashboard observers — Observer Pattern
        DataEventBus.getInstance().notifyObservers("AttritionService");

        return result;
    }

    @Override
    public List<AttritionRecord> generateTrendData(PeriodType periodType, LocalDate startDate, LocalDate endDate)
            throws HRMSException {
        if (startDate == null || endDate == null)
            throw new HRMSException.InvalidDateRangeException("Start date and end date cannot be null.");
        if (endDate.isBefore(startDate))
            throw new HRMSException.InvalidDateRangeException("End date must be on or after start date.");

        List<AttritionRecord> trend = new ArrayList<>();

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            LocalDate bucketStart = cursor;
            LocalDate bucketEnd;
            switch (periodType) {
                case MONTHLY:
                    bucketEnd = cursor.with(TemporalAdjusters.lastDayOfMonth());
                    break;
                case QUARTERLY:
                    int currentMonth = cursor.getMonthValue();
                    int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
                    bucketStart = LocalDate.of(cursor.getYear(), quarterStartMonth, 1);
                    bucketEnd = bucketStart.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
                    break;
                case ANNUAL:
                    bucketStart = LocalDate.of(cursor.getYear(), 1, 1);
                    bucketEnd = LocalDate.of(cursor.getYear(), 12, 31);
                    break;
                default:
                    throw new HRMSException.InvalidDateRangeException("Unsupported period type: " + periodType);
            }

            if (bucketEnd.isBefore(startDate)) {
                cursor = bucketEnd.plusDays(1);
                continue;
            }

            if (bucketStart.isBefore(startDate)) bucketStart = startDate;
            if (bucketEnd.isAfter(endDate)) bucketEnd = endDate;

            int total = countEmployeesEmployedDuring(bucketStart, bucketEnd);
            int left  = countExitsWithin(bucketStart, bucketEnd);
            double rate = (total > 0) ? (left / (double) total) * 100.0 : 0.0;

            trend.add(new AttritionRecord(bucketStart, bucketEnd, total, left, rate));

            // advance cursor
            cursor = bucketEnd.plusDays(1);
        }

        // Notify dashboard observers — Observer Pattern
        DataEventBus.getInstance().notifyObservers("AttritionService");

        return trend;
    }

    /**
     * Count employees employed at any time during [bucketStart, bucketEnd].
     *
     * Uses `date_of_joining` (column in hrms-database.jar Employee entity) as the
     * hire-date proxy. There is no `termination_date` column; an employee is considered
     * to still be employed unless they have a row in the ExitInterview table.
     */
    private int countEmployeesEmployedDuring(LocalDate bucketStart, LocalDate bucketEnd) {
        if (dummyMode || conn == null) {
            // Dummy fallback for development when DB is unavailable.
            int base = 100;
            int monthFactor = bucketStart.getMonthValue() - 1;
            return base + monthFactor * 2;
        }
        // An employee is "employed during" the period if they joined on or before
        // the bucket end AND either haven't exited yet, or exited after the bucket start.
        String sql = """
                SELECT COUNT(*) FROM employees e
                WHERE (e.date_of_joining IS NULL OR date(e.date_of_joining) <= ?)
                  AND (
                      NOT EXISTS (
                          SELECT 1 FROM exit_interviews ei
                          WHERE ei.emp_id = e.emp_id
                      )
                      OR EXISTS (
                          SELECT 1 FROM exit_interviews ei
                          WHERE ei.emp_id = e.emp_id
                            AND date(ei.exit_date) > ?
                      )
                  )
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bucketEnd.toString());
            ps.setString(2, bucketStart.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("DB error counting employed during period: " + e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Count exits (ExitInterview rows) whose exit_date falls within [bucketStart, bucketEnd].
     *
     * Uses the `ExitInterview` table and the `exit_date` column (per hrms-database.jar schema).
     * The old code incorrectly referenced `termination_date` on the employees table.
     */
    private int countExitsWithin(LocalDate bucketStart, LocalDate bucketEnd) {
        if (dummyMode || conn == null) {
            // Dummy fallback: small varying number of terminations per bucket.
            return 5 + (bucketStart.getMonthValue() % 5); // 5..9
        }
        String sql = """
                SELECT COUNT(DISTINCT emp_id) FROM exit_interviews
                WHERE exit_date >= ? AND exit_date <= ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bucketStart.toString());
            ps.setString(2, bucketEnd.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("DB error counting exits: " + e.getMessage(), e);
        }
        return 0;
    }
}
