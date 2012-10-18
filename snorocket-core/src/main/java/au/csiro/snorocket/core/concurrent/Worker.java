/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.concurrent;

import java.util.Queue;

/**
 * Represents a worker in charge of deriving axioms in a {@link Context}.
 * 
 * @author Alejandro Metke
 * 
 */
public class Worker implements Runnable {

    private final Queue<Context> todo;

    /**
	 * 
	 */
    public Worker(Queue<Context> todo) {
        this.todo = todo;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        // Process contexts until there are no more left in the queue
        while (true) {
            Context ctx = todo.poll();
            if (ctx == null)
                break;
            ctx.processOntology();
        }
    }

}
