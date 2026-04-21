package com.hrms.service.validation;

import com.hrms.model.Employee;
import com.hrms.exception.HRMSException;

/**
 * EmployeeValidator — Chain of Responsibility Pattern implementation.
 *
 * Provides a base class for chaining validation rules for Employee objects.
 * Design Pattern: Chain of Responsibility (Behavioral)
 */
public abstract class EmployeeValidator {
    private EmployeeValidator next;

    /**
     * Builds chains of validator objects.
     */
    public EmployeeValidator linkWith(EmployeeValidator next) {
        this.next = next;
        return next;
    }

    /**
     * Subclasses will implement this method with concrete checks.
     */
    public abstract boolean check(Employee employee) throws HRMSException.InvalidInputException;

    /**
     * Runs check on the next object in chain or ends traversing if we're in
     * last object in chain.
     */
    protected boolean checkNext(Employee employee) throws HRMSException.InvalidInputException {
        if (next == null) {
            return true;
        }
        return next.check(employee);
    }
}
