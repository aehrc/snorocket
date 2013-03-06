/*
 * #%L
 * ELK Reasoner Protege Plug-in
 * 
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2011 - 2012 Department of Computer Science, University of Oxford
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package au.csiro.snorocket.protege;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Message appender based.
 * 
 * @author Alejandro Metke
 * 
 */
public class ProtegeMessageAppender extends AppenderSkeleton {
    
    public static final ProtegeMessageAppender INSTANCE = 
            new ProtegeMessageAppender();
    
    private ProtegeMessageAppender() {
        
    }
    
    @Override
    public void close() {
        
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    protected void append(LoggingEvent event) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        String messageText = event.getRenderedMessage();

        // Build panel
        JTextArea textArea = new JTextArea(10, 5);
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.setPreferredSize(new Dimension(640, 480));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        textArea.setText(messageText);
        
        // Show message
        int messageType = JOptionPane.INFORMATION_MESSAGE;
        Level level = event.getLevel();
        if(level == Level.WARN) {
            messageType = JOptionPane.WARNING_MESSAGE;
        } else if(level == Level.ERROR) {
            messageType = JOptionPane.ERROR_MESSAGE;
        }
        
        JOptionPane.showMessageDialog(null, panel, "Snorocket", messageType);
    }

}
