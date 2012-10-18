package au.csiro.snorocket.core.util;

public final class ReadonlyConceptSet implements IConceptSet {

    private IConceptSet set;

    public ReadonlyConceptSet(IConceptSet set) {
        this.set = set;
    }

    public void add(int concept) {
        throw new UnsupportedOperationException();
    }

    public void addAll(IConceptSet set) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean contains(int concept) {
        return set.contains(concept);
    }

    public boolean containsAll(IConceptSet concepts) {
        return set.containsAll(concepts);
    }

    public IntIterator iterator() {
        return set.iterator();
    }

    public void remove(int concept) {
        throw new UnsupportedOperationException();
    }

    public void removeAll(IConceptSet set) {
        throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

    public int size() {
        return set.size();
    }

    public void grow(int increment) {
        throw new UnsupportedOperationException(
                "Cannot grow the EmptyConceptSet!");
    }

    public String toString() {
        return String.valueOf(set);
    }

    @Override
    public int[] toArray() {
        return set.toArray();
    }
}