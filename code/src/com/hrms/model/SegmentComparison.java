package com.hrms.model;

/**
 * Result of comparing two employee segments on attrition metrics.
 * Produced by SegmentationService.compareSegments().
 */
public class SegmentComparison {

    private String segmentA;
    private String segmentB;
    private double attritionRateA;   // percentage 0-100
    private double attritionRateB;
    private double difference;       // rateA - rateB (positive = A has higher attrition)
    private int    totalA;           // employee count in segment A
    private int    totalB;
    private int    exitsA;
    private int    exitsB;

    public SegmentComparison() {}

    public SegmentComparison(String segmentA, String segmentB,
                             double attritionRateA, double attritionRateB,
                             int totalA, int totalB, int exitsA, int exitsB) {
        this.segmentA       = segmentA;
        this.segmentB       = segmentB;
        this.attritionRateA = attritionRateA;
        this.attritionRateB = attritionRateB;
        this.difference     = attritionRateA - attritionRateB;
        this.totalA         = totalA;
        this.totalB         = totalB;
        this.exitsA         = exitsA;
        this.exitsB         = exitsB;
    }

    public String getSegmentA()       { return segmentA; }
    public String getSegmentB()       { return segmentB; }
    public double getAttritionRateA() { return attritionRateA; }
    public double getAttritionRateB() { return attritionRateB; }
    public double getDifference()     { return difference; }
    public int    getTotalA()         { return totalA; }
    public int    getTotalB()         { return totalB; }
    public int    getExitsA()         { return exitsA; }
    public int    getExitsB()         { return exitsB; }

    public void setSegmentA(String s)       { this.segmentA = s; }
    public void setSegmentB(String s)       { this.segmentB = s; }
    public void setAttritionRateA(double r) { this.attritionRateA = r; this.difference = r - attritionRateB; }
    public void setAttritionRateB(double r) { this.attritionRateB = r; this.difference = attritionRateA - r; }
    public void setTotalA(int t)            { this.totalA = t; }
    public void setTotalB(int t)            { this.totalB = t; }
    public void setExitsA(int e)            { this.exitsA = e; }
    public void setExitsB(int e)            { this.exitsB = e; }

    @Override
    public String toString() {
        return String.format("SegmentComparison{%s=%.1f%% vs %s=%.1f%%, diff=%.1f%%}",
                segmentA, attritionRateA, segmentB, attritionRateB, difference);
    }
}
