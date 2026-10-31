import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * EJ 85/86/87/88/90：新设计优先显式格式；被迫序列化时
 * serialVersionUID 显式声明 + 序列化代理 + readResolve 重建 + 不可信流 filter。
 */
public final class SerializationExample implements Serializable {

    private static final long serialVersionUID = 1L; // 86：显式声明，防自动漂移炸兼容

    private final String title;
    private final Instant openedAt; // 不可变类型天然免疫拷贝泄漏

    public SerializationExample(String title, Instant openedAt) {
        this.title = Objects.requireNonNull(title, "title");
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt");
    }

    @Override public String toString() {
        return "Window[" + title + " -> " + openedAt + "]";
    }

    /** 90：writeReplace 把序列化重定向到私有 SerialProxy，流格式 = 逻辑形式。 */
    private Object writeReplace() {
        return new SerialProxy(this);
    }

    /** 88/89：实例控制——拒绝任何伪造 Window 的默认反序列化路径。 */
    private void readObject(ObjectInputStream in) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }

    /** 私有静态代理：只有它能进出流，构造器校验所有不变量。 */
    private static final class SerialProxy implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String title;
        private final Instant openedAt;

        SerialProxy(SerializationExample w) {
            this.title = w.title;
            this.openedAt = w.openedAt;
        }

        /** 88：readResolve 经构造器重建 = 每个不变量都被重新校验。 */
        private Object readResolve() throws ObjectStreamException {
            return new SerializationExample(title, openedAt);
        }
    }

    public static void main(String[] args) throws Exception {
        var window = new SerializationExample("main", Instant.now());
        var bos = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bos)) {
            out.writeObject(window);
        }

        try (var in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            // 88：不可信流配 input filter（JDK9+），限制深度与引用数，挡反序列化炸弹。
            in.setObjectInputFilter(info -> info.depth() <= 3 && info.references() <= 20
                    ? ObjectInputFilter.Status.ALLOWED
                    : ObjectInputFilter.Status.REJECTED);
            System.out.println(in.readObject());
        }
    }
}
