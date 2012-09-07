package au.csiro.snorocket.core.concurrent;

import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.IQueue;
import au.csiro.snorocket.core.axioms.IConjunctionQueueEntry;
import au.csiro.snorocket.core.axioms.IFeatureQueueEntry;
import au.csiro.snorocket.core.axioms.IRoleQueueEntry;

/**
 * Represents a context where derivations associated to one concept are 
 * executed.
 * 
 * @author Alejandro Metke
 *
 */
public class Context {
	/**
	 * The internal concept id.
	 */
	private final int concept;

	/**
	 * Flag to indicate if this context is active or not.
	 */
	private final AtomicBoolean active = new AtomicBoolean(false);
	
	/**
     * Queue (List) of ConjunctionQueueEntries indicating work to be done
     * for this concept.
     */
    protected final IQueue<IConjunctionQueueEntry> conceptQueues;
    
    /**
     * Queue (List) of RoleQueueEntries indicating work to be done for this
     * concept.
     */
    protected final IQueue<IRoleQueueEntry> roleQueues;
    
    /**
     * Queue (List) of FeatureQueueEntries indicating work to be done for this
     * concept. Queue entries of the form A [ f.(o, v).
     */
    protected final IQueue<IFeatureQueueEntry> featureQueues;
    
    /**
     * Reference to the parent context queue. Used to add this context back to
     * the queue when reactivated.
     */
    private static Deque<Context> parentTodo;
    
    /**
     * Reference to the global factory.
     */
    private static IFactory factory;
    
    /**
     * Sets the reference to the parent context queue.
     * 
     * @param parentTodo
     */
    public static void setParentTodo(Deque<Context> parentTodo) {
		Context.parentTodo = parentTodo;
	}
    
    /**
     * Sets the reference to the global factory.
     * @param factory
     */
    public static void setFactory(IFactory factory) {
    	Context.factory = factory;
    }
    
    /**
     * Constructor.
     * 
     * @param concept
     */
	public Context(int concept) {
		this.concept = concept;
		conceptQueues = null;
		roleQueues = null;
		featureQueues = null;
	}
	
}
