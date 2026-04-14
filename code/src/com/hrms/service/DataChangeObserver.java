package com.hrms.service;

/**
 * Observer interface for the Dashboard Observer Pattern.
 *
 * Design Pattern: Observer (GoF)
 *
 * Any component that wants to react to data changes in the HRMS system
 * (e.g., DashboardController refreshing after a new attrition calculation)
 * implements this interface and registers with a DataChangeSubject.
 */
public interface DataChangeObserver {

    /**
     * Called by a subject when its underlying data has changed.
     *
     * @param eventSource A string identifying which service triggered the event.
     *                    E.g. "AttritionService", "RiskService", "AnalyticsService".
     */
    void onDataChanged(String eventSource);
}
