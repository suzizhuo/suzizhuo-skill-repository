import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EJ 1/5/54/55/83：命名静态工厂、依赖注入槽位、空集合不返回 null、
 * Optional 只做返回类型、holder idiom 懒加载单例。
 */
public final class StaticFactoryExample {

    private final Map<String, String> users = new ConcurrentHashMap<>();

    private StaticFactoryExample() {}

    /** 1：命名工厂——调用点读得出意图。 */
    public static StaticFactoryExample empty() {
        return new StaticFactoryExample();
    }

    /** 55：Optional 只做返回；不用于字段/参数/集合元素。 */
    public Optional<String> findUser(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(users.get(id));
    }

    /** 54：空集合优于 null。JDK17+：List.copyOf 产出不可变快照。 */
    public List<String> allIds() {
        return List.copyOf(users.keySet());
    }

    public void add(String id, String name) {
        users.put(id, name);
    }

    /** 83：holder idiom——JVM 类初始化保证单次创建，无锁无 volatile。 */
    private static final class LazyHolder {
        static final StaticFactoryExample INSTANCE = StaticFactoryExample.empty();
    }

    public static StaticFactoryExample lazyInstance() {
        return LazyHolder.INSTANCE;
    }

    public static void main(String[] args) {
        var ex = StaticFactoryExample.empty();
        ex.add("u1", "alice");
        System.out.println(ex.findUser("u1"));
        System.out.println(ex.findUser("nope")); // Optional.empty 而非 null
        System.out.println(StaticFactoryExample.empty().allIds()); // [] 而非 null
        System.out.println(StaticFactoryExample.lazyInstance() == StaticFactoryExample.lazyInstance());
    }
}
