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

import java.io.Serializable;

import au.csiro.snorocket.core.util.MonotonicCollection;

/**
 * A LIFO queue. Used to be a LIFO set to avoid duplicate entries but,
 * <ol>
 * <li>this has huge space overheads, and</li>
 * <li>empirical evidence indicates they don't happen and thus this is redundant
 * effort</li>
 * </ol>
 * 
 * @author Michael Lawley
 * 
 * @param <QueueEntry>
 */
public final class QueueImpl<QueueEntry> implements IQueue<QueueEntry>, Serializable {
    
    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;
    
    private static final int DEFAULT_ALLOC_SIZE = 4;
    private static final Object[] EMPTY = {};

    /**
     * Index of next free slot in items array.
     */
    protected int counter = 0;
    protected QueueEntry[] items;

    static int number = 0;

    /**
     * See chapter 8 of http://java.sun.com/j2se/1.5/pdf/generics-tutorial.pdf
     * for why we need the typeToken parameter
     * 
     * @param typeToken
     */
    @SuppressWarnings("unchecked")
    public QueueImpl(Class<QueueEntry> typeToken) {
        items = (QueueEntry[]) EMPTY;

        number++;
    }

    public void add(QueueEntry entry) {
        checkSize(1);
        items[counter++] = entry;
    }

    public void addAll(MonotonicCollection<? extends QueueEntry> queue) {
        try {
            final int numberOfNewElements = queue.size();
            checkSize(numberOfNewElements);
            System.arraycopy(queue.data, 0, items, counter, numberOfNewElements);
            counter += numberOfNewElements;
        } catch (OutOfMemoryError e) {
            System.err.println(number);
            throw e;
        }
    }

    public int size() {
        return counter;
    }

    @SuppressWarnings("unchecked")
    private void checkSize(final int numberOfNewElements) {
        final int size = counter + numberOfNewElements;
        final int len = items.length;

        if (size >= len) {
            final int newsize = size > 0 ? (int) size + 4 : DEFAULT_ALLOC_SIZE;
            final QueueEntry[] newItems = (QueueEntry[]) new Object[newsize];
            System.arraycopy(items, 0, newItems, 0, counter);
            items = newItems;
        } else if (len > 2048 && size < (len >> 2)) {
            final int newsize = len >> 1;
            final QueueEntry[] newItems = (QueueEntry[]) new Object[newsize];
            System.arraycopy(items, 0, newItems, 0, counter);
            items = newItems;
        }
    }

    public QueueEntry remove() {
        counter--;
        return items[counter];
    }

    @SuppressWarnings("unchecked")
    public boolean isEmpty() {
        if (0 == counter) {
            // clean up space
            items = (QueueEntry[]) EMPTY;
            return true;
        } else {
            return false;
        }
    }

}
