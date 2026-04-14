package com.hrms.service;

import com.hrms.model.Employee;
import com.hrms.model.FilterSpec;
import com.hrms.model.SegmentComparison;

import java.util.List;

/**
 * Contract for the Data Segmentation & Filtering subsystem.
 *
 * Aligned with the shared ISegmentedReports interface spec.
 */
public interface ISegmentationService {

    /**
     * Filters the employee dataset according to the given specification.
     *
     * @param filterSpec Criteria to apply.
     * @return Matching employees (never null, may be empty).
     * @throws com.hrms.exception.HRMSException.InvalidFilterException if filterSpec is null.
     */
    List<Employee> applySegmentation(FilterSpec filterSpec);

    /**
     * Computes attrition rates for two segments and returns a comparison.
     *
     * @param first  FilterSpec defining Segment A.
     * @param second FilterSpec defining Segment B.
     * @return SegmentComparison with rates and difference.
     * @throws com.hrms.exception.HRMSException.InsufficientDataException if a segment has no employees.
     */
    SegmentComparison compareSegments(FilterSpec first, FilterSpec second);
}
