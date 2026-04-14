package com.hrms.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DataEventBus — central event hub implementing the Observer Pattern.
 *
 * Design Pattern: Observer (GoF) + Singleton
 *
 * Acts as both a DataChangeSubject and a singleton registry so that services
 * (AttritionService, RiskService, AnalyticsService) can publish events via a
 * single call to DataEventBus.getInstance().notifyObservers("..."), while
 * DashboardController registers itself once to receive all updates.
 *
 * Using CopyOnWriteArrayList ensures thread-safety when observers are added
 * or removed during iteration (e.g., from the Swing EDT and background threads).
 */
public class DataEventBus implements DataChangeSubject {

    private static final DataEventBus INSTANCE = new DataEventBus();

    private final List<DataChangeObserver> observers = new CopyOnWriteArrayList<>();

    /** Private constructor — Singleton. */
    private DataEventBus() {}

    /**
     * Returns the single application-wide DataEventBus instance.
     */
    public static DataEventBus getInstance() {
        return INSTANCE;
    }

    @Override
    public void addObserver(DataChangeObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            System.out.println("[DataEventBus] Observer registered: " +
                    observer.getClass().getSimpleName());
        }
    }

    @Override
    public void removeObserver(DataChangeObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notifies all registered observers that data from {@code eventSource} has changed.
     *
     * @param eventSource Name of the service that triggered the event.
     */
    @Override
    public void notifyObservers(String eventSource) {
        System.out.println("[DataEventBus] Broadcasting change from: " + eventSource +
                " to " + observers.size() + " observer(s).");
        for (DataChangeObserver obs : observers) {
            obs.onDataChanged(eventSource);
        }
    }
}
