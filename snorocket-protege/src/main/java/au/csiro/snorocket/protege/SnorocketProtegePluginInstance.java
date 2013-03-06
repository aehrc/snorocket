package au.csiro.snorocket.protege;

import org.apache.log4j.Logger;
import org.protege.editor.core.editorkit.plugin.EditorKitHook;

/**
 * Hooks Log4J logging to Protege.
 * 
 * @author Alejandro Metke
 * 
 */
public class SnorocketProtegePluginInstance extends EditorKitHook {

    @Override
    public void dispose() throws Exception {
    }

    @Override
    public void initialise() throws Exception {
        Logger.getLogger("au.csiro.snorocket.owlapi.SnorocketOWLReasoner")
                .addAppender(ProtegeMessageAppender.INSTANCE);
    }
}
