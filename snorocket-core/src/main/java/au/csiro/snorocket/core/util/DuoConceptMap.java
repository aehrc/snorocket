package au.csiro.snorocket.core.util;

public final class DuoConceptMap<V> implements IConceptMap<V> {

    final private IConceptMap<V> base;
    final private IConceptMap<V> overlay;

    public DuoConceptMap(final IConceptMap<V> base, final IConceptMap<V> overlay) {
        this.base = base;
        this.overlay = overlay;
    }

    public IConceptMap<V> getOverlay() {
        return overlay;
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean containsKey(int key) {
        return overlay.containsKey(key) || base.containsKey(key);
    }

    public V get(int key) {
        return base.containsKey(key) ? base.get(key) : overlay.get(key);
    }

    public void grow(int newSize) {
        throw new UnsupportedOperationException();
    }

    public IntIterator keyIterator() {
        return new IntIterator() {

            final IntIterator baseItr = base.keyIterator();
            final IntIterator overlayItr = overlay.keyIterator();

            public boolean hasNext() {
                return baseItr.hasNext() || overlayItr.hasNext();
            }

            public int next() {
                return baseItr.hasNext() ? baseItr.next() : overlayItr.next();
            }

        };
    }

    public void put(int key, V value) {
        if (base.containsKey(key)) {
            throw new UnsupportedOperationException();
        }
        overlay.put(key, value);
    }

    public void remove(int key) {
        if (base.containsKey(key)) {
            throw new UnsupportedOperationException();
        }
        overlay.remove(key);
    }

    public int size() {
        return base.size() + overlay.size();
    }

    @Override
    public String toString() {
        return overlay + " ==> " + base;
    }
}