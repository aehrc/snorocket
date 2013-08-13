/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.benchmark;

/**
 * Contains statistics gathered in the benchmarking process.
 * 
 * @author Alejandro Metke
 * 
 */
public class Stats {

    private long axiomTransformationTimeMs = 0;
    private long axiomLoadingTimeMs = 0;
    private long classificationTimeMs = 0;
    private long taxonomyBuildingTimeMs = 0;

    public Stats() {

    }

    public Stats(long axiomTransformationTimeMs, long axiomLoadingTimeMs,
            long classificationTimeMs, long taxonomyBuildingTimeMs) {
        super();
        this.axiomTransformationTimeMs = axiomTransformationTimeMs;
        this.axiomLoadingTimeMs = axiomLoadingTimeMs;
        this.classificationTimeMs = classificationTimeMs;
        this.taxonomyBuildingTimeMs = taxonomyBuildingTimeMs;
    }

    public long getAxiomTransformationTimeMs() {
        return axiomTransformationTimeMs;
    }

    public void setAxiomTransformationTimeMs(long axiomTransformationTimeMs) {
        this.axiomTransformationTimeMs = axiomTransformationTimeMs;
    }

    public long getAxiomLoadingTimeMs() {
        return axiomLoadingTimeMs;
    }

    public void setAxiomLoadingTimeMs(long axiomLoadingTimeMs) {
        this.axiomLoadingTimeMs = axiomLoadingTimeMs;
    }

    public long getClassificationTimeMs() {
        return classificationTimeMs;
    }

    public void setClassificationTimeMs(long classificationTimeMs) {
        this.classificationTimeMs = classificationTimeMs;
    }

    public long getTaxonomyBuildingTimeMs() {
        return taxonomyBuildingTimeMs;
    }

    public void setTaxonomyBuildingTimeMs(long taxonomyBuildingTimeMs) {
        this.taxonomyBuildingTimeMs = taxonomyBuildingTimeMs;
    }

    public long getTotalTime() {
        return axiomTransformationTimeMs + axiomLoadingTimeMs
                + classificationTimeMs + taxonomyBuildingTimeMs;
    }

}
