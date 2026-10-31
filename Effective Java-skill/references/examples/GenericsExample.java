import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * EJ 26/27/29/30/31/33：泛型方法、PECS 通配符、类型令牌异构容器、
 * 最小作用域 @SuppressWarnings。
 */
public final class GenericsExample {

    /** 31：生产者用 extends——src 只被读取。 */
    public static <E> void pushAll(List<? extends E> src, Stack<E> dst) {
        for (E e : src) dst.push(e);
    }

    /** 31：消费者用 super——dst 只被写入。 */
    public static <E> void popAll(Stack<E> src, List<? super E> dst) {
        while (!src.isEmpty()) dst.add(src.pop());
    }

    /** 29/30：泛型静态工具方法——调用点零强转，编译期即类型安全。
     *  32：@SafeVarargs 仅在方法只迭代 varargs、不外泄不写入时使用。 */
    @SafeVarargs
    public static <E> Set<E> identitySet(E first, E... rest) {
        var set = new HashSet<E>();
        set.add(first);
        for (E e : rest) set.add(e);
        return set;
    }

    /** 33：类型令牌——Class<?> 为键的异构容器，远比 Map<String, Object> 安全。 */
    public static final class Favorites {
        private final Map<Class<?>, Object> favorites = new HashMap<>();

        public <T> void put(Class<T> key, T value) {
            favorites.put(key, value);
        }

        @SuppressWarnings("unchecked") // 27：最小作用域——put 的 key 与 value 类型已绑定
        public <T> T get(Class<T> key) {
            return (T) favorites.get(key); // 此强转等价于 Class<T> 契约，可证明安全
        }
    }

    public static final class Stack<E> {
        private final Deque<E> deque = new java.util.ArrayDeque<>();

        public void push(E e) { deque.push(e); }
        public E pop() { return deque.pop(); }
        public boolean isEmpty() { return deque.isEmpty(); }
    }

    public static void main(String[] args) {
        var dst = new Stack<Number>();
        pushAll(List.of(1, 2.5), dst); // List<Integer>/List<Double> 均可作 ? extends Number
        var out = new ArrayList<Object>();
        popAll(dst, out);              // List<Object> 是 ? super Number
        System.out.println(out);
        System.out.println(identitySet("a", "a", "b")); // 调用点无强转

        var favorites = new Favorites();
        favorites.put(String.class, "hello");
        favorites.put(Integer.class, 42);
        System.out.println(favorites.get(String.class).length()
                + favorites.get(Integer.class)); // 编译期已知类型
    }
}
