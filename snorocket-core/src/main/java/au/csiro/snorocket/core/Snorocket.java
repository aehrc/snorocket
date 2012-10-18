/**
 * Copyright (c) 2009 International Health Terminology Standards Development
 * Organisation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */

package au.csiro.snorocket.core;

import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

final public class Snorocket {

    private final static Logger LOGGER = Logger
            .getLogger("au.csiro.snorocket.Logger");

    private static Handler rootHandler = null;

    private final static Handler snorocketHandler = new ConsoleHandler();

    public static final String ISA_ROLE = "116680003";
    public static final String ROLE_GROUP = "roleGroup";

    static {
        snorocketHandler.setFormatter(new Formatter() {
            public String format(LogRecord rec) {
                StringBuffer buf = new StringBuffer(1000);
                buf.append(new java.util.Date());
                buf.append('\t');
                buf.append(rec.getLevel());
                buf.append('\t');
                buf.append(formatMessage(rec));
                buf.append('\n');
                return buf.toString();
            }
        });
    }

    public static void installLoggingHander() {
        // first check if it's already installed
        if (!Arrays.asList(getLogger().getHandlers())
                .contains(snorocketHandler)) {
            getLogger().addHandler(snorocketHandler);

            if (null == rootHandler) {
                Logger rootLogger = Logger.getLogger("");
                Handler[] handlers = rootLogger.getHandlers();
                if (handlers.length > 0
                        && handlers[0] instanceof ConsoleHandler) {
                    rootHandler = handlers[0];
                    rootLogger.removeHandler(rootHandler);
                }
            }
        }
    }

    public static void uninstallLoggingHander() {
        if (null != rootHandler) {
            Logger rootLogger = Logger.getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            if (handlers.length == 0) {
                rootLogger.addHandler(rootHandler);
                rootHandler = null;
            }
        }
        getLogger().removeHandler(snorocketHandler);
    }

    /**
     * For internal use only
     */
    public static boolean DEBUGGING = false;
    public static boolean DEBUG_DUMP = false;

    public static Logger getLogger() {
        return LOGGER;
    }

}
