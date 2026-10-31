import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * EJ 18/19：组合与转发优于继承——包装类只依赖委托对象的公开契约，
 * 不依赖其内部行为，超类演进不会打破调用方。
 */
public final class CompositionExample {

    /** 转发类：把全部 Set 操作转给委托，不覆写 equals/hashCode 语义之外的行为。 */
    public static class ForwardingSet<E> implements Set<E> {
        protected final Set<E> delegate;

        protected ForwardingSet(Set<E> delegate) {
            this.delegate = delegate;
        }

        @Override public int size() { return delegate.size(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public boolean contains(Object o) { return delegate.contains(o); }
        @Override public Iterator<E> iterator() { return delegate.iterator(); }
        @Override public Object[] toArray() { return delegate.toArray(); }
        @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
        @Override public boolean add(E e) { return delegate.add(e); }
        @Override public boolean remove(Object o) { return delegate.remove(o); }
        @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
        @Override public boolean addAll(Collection<? extends E> c) { return delegate.addAll(c); }
        @Override public boolean retainAll(Collection<?> c) { return delegate.retainAll(c); }
        @Override public boolean removeAll(Collection<?> c) { return delegate.removeAll(c); }
        @Override public void clear() { delegate.clear(); }
        @Override public boolean equals(Object o) { return delegate.equals(o); }
        @Override public int hashCode() { return delegate.hashCode(); }

        /** 增量增强：加一个横切关注点，不改算法。 */
        public static final class CountingSet<E> extends ForwardingSet<E> {
            private int added;

            public CountingSet(Set<E> delegate) {
                super(delegate);
            }

            @Override public boolean add(E e) {
                added++;
                return super.add(e);
            }

            @Override public boolean addAll(Collection<? extends E> c) {
                added += c.size();
                return super.addAll(c);
            }

            public int addedCount() { return added; }
        }
    }

    public static void main(String[] args) {
        var counting = new ForwardingSet.CountingSet<String>(new java.util.HashSet<>());
        counting.addAll(List.of("a", "b"));
        System.out.println(counting.addedCount() + " " + counting);
    }
}
