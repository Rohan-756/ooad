package com.hrms.service.validation;

import com.hrms.model.Employee;
import com.hrms.exception.HRMSException;

public class BasicInfoValidator extends EmployeeValidator {
    @Override
    public boolean check(Employee employee) throws HRMSException.InvalidInputException {
        if (employee == null) {
            throw new HRMSException.InvalidInputException("Employee object cannot be null.");
        }
        if (employee.getName() == null || employee.getName().trim().isEmpty()) {
            throw new HRMSException.InvalidInputException("Employee name cannot be empty.");
        }
        if (employee.getDepartment() == null || employee.getDepartment().trim().isEmpty()) {
            throw new HRMSException.InvalidInputException("Department cannot be empty.");
        }
        return checkNext(employee);
    }
}
