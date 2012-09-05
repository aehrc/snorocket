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

import java.util.Iterator;

public interface IMonotonicCollection<T> extends Iterable<T> {

    public void add(T element);
    
    public int size();

}

final class DuoMonotonicCollection<T> implements IMonotonicCollection<T> {

    final private IMonotonicCollection<T> base;
    final private IMonotonicCollection<T> overlay;

    public DuoMonotonicCollection(IMonotonicCollection<T> base, IMonotonicCollection<T> overlay) {
        this.base = base;
        this.overlay = overlay;
    }
    
    IMonotonicCollection<T> getOverlay() {
        return overlay;
    }
    
    public void add(T element) {
        overlay.add(element);
    }

    public int size() {
        return base.size() + overlay.size();
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            
            final Iterator<T> baseItr = base.iterator();
            final Iterator<T> overlayItr = overlay.iterator();

            public boolean hasNext() {
                return baseItr.hasNext() || overlayItr.hasNext();
            }

            public T next() {
                return baseItr.hasNext()
                        ? baseItr.next()
                        : overlayItr.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
            
        };
    }
    
}
