package au.csiro.snorocket.core.util;

import java.util.Iterator;

public final class DuoMonotonicCollection<T> implements IMonotonicCollection<T> {

    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;
    
    final private IMonotonicCollection<T> base;
    final private IMonotonicCollection<T> overlay;

    public DuoMonotonicCollection(IMonotonicCollection<T> base,
            IMonotonicCollection<T> overlay) {
        this.base = base;
        this.overlay = overlay;
    }

    public IMonotonicCollection<T> getOverlay() {
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
                return baseItr.hasNext() ? baseItr.next() : overlayItr.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

}