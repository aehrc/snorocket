/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.owlapi;

import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;

import au.csiro.ontology.util.IProgressMonitor;

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

    public void taskStarted(String taskName) {
        reasonerTaskStarted(taskName);
        
    }

    public void taskEnded() {
        reasonerTaskStopped();
        
    }

    public void step(int value, int max) {
        reasonerTaskProgressChanged(value, max);
        
    }

    public void taskBusy() {
        reasonerTaskBusy();
        
    }

    public void reasonerTaskStarted(String taskName) {
        base.reasonerTaskStarted(taskName);
    }

    public void reasonerTaskStopped() {
        base.reasonerTaskStopped();
        
    }

    public void reasonerTaskProgressChanged(int value, int max) {
        base.reasonerTaskProgressChanged(value, max);
    }

    public void reasonerTaskBusy() {
        base.reasonerTaskBusy();
    }

}
