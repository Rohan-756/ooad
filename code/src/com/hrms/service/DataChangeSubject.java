package com.hrms.service;

/**
 * Subject interface for the Dashboard Observer Pattern.
 *
 * Design Pattern: Observer (GoF)
 *
 * Services (AttritionService, RiskService, AnalyticsService) implement this
 * so that interested parties (DashboardController) can register to receive
 * notifications when their data changes.
 */
public interface DataChangeSubject {

    /**
     * Registers an observer to be notified on data changes.
     *
     * @param observer The observer to add.
     */
    void addObserver(DataChangeObserver observer);

    /**
     * Removes a previously registered observer.
     *
     * @param observer The observer to remove.
     */
    void removeObserver(DataChangeObserver observer);

    /**
     * Notifies all registered observers that data has changed.
     *
     * @param eventSource Identifier of the event source (e.g. "AttritionService").
     */
    void notifyObservers(String eventSource);
}
