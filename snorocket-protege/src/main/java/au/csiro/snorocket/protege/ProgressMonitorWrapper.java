/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.protege;

import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;

import au.csiro.ontology.classification.IProgressMonitor;

/**
 * @author Alejandro Metke
 *
 */
public class ProgressMonitorWrapper implements IProgressMonitor, ReasonerProgressMonitor {
    
    private ReasonerProgressMonitor base;
    
    /**
     * Builds a new {@link ProgressMonitorWrapper}.
     */
    public ProgressMonitorWrapper(ReasonerProgressMonitor base) {
        this.base = base;
    }

    @Override
    public void taskStarted(String taskName) {
        reasonerTaskStarted(taskName);
        
    }

    @Override
    public void taskEnded() {
        reasonerTaskStopped();
        
    }

    @Override
    public void step(int value, int max) {
        reasonerTaskProgressChanged(value, max);
        
    }

    @Override
    public void taskBusy() {
        reasonerTaskBusy();
        
    }

    @Override
    public void reasonerTaskStarted(String taskName) {
        base.reasonerTaskStarted(taskName);
    }

    @Override
    public void reasonerTaskStopped() {
        base.reasonerTaskStopped();
        
    }

    @Override
    public void reasonerTaskProgressChanged(int value, int max) {
        base.reasonerTaskProgressChanged(value, max);
    }

    @Override
    public void reasonerTaskBusy() {
        base.reasonerTaskBusy();
    }

}
