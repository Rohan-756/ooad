package com.hrms.service.validation;

import com.hrms.model.Employee;
import com.hrms.exception.HRMSException;

public class StatusValidator extends EmployeeValidator {
    @Override
    public boolean check(Employee employee) throws HRMSException.InvalidInputException {
        if (employee.getEmploymentStatus() == null) {
            throw new HRMSException.InvalidInputException("Employment status cannot be null.");
        }
        return checkNext(employee);
    }
}
