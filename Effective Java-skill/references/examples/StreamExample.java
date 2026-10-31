import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EJ 42/43/44/45/46/47/48：lambda 优于匿名类、方法引用择优、标准函数式接口、
 * 流无副作用、返回集合而非流、慎用并行流、原始流免装箱。
 */
public final class StreamExample {

    public record Person(String name, int age) {} // JDK16+：透明不可变载体

    public static List<String> activeNames(List<Person> people) {
        return people.stream()
                .filter(p -> p.age() >= 18)            // 42：小函数对象用 lambda
                .map(Person::name)                     // 43：比 lambda 更清晰时用方法引用
                .map(String::toUpperCase)
                .toList();                             // 47：JDK16+ toList → 不可变 List
    }

    /** 46：聚合走 collect/reduce，不在 lambda 里写外部状态。 */
    public static Map<String, Long> nameCounts(List<String> names) {
        return names.stream().collect(Collectors.groupingBy(
                String::toLowerCase, Collectors.counting()));
    }

    /** 45/48：原始流免装箱（AvoidingHeapPollution）；并行只在有证据时启用。 */
    public static long totalAgeParallel(List<Person> people) {
        return people.parallelStream()
                .mapToInt(Person::age)                 // 数据量大且源可分裂时才值得并行
                .sum();
    }

    /** 45：早退/复杂分支不用流硬写——循环更清晰。 */
    public static Person firstMinor(List<Person> people) {
        for (Person p : people) {                      // 有 return/继续条件的遍历，for 更好读
            if (p.age() < 18) return p;
        }
        return null;
    }

    public static void main(String[] args) {
        var people = List.of(new Person("alice", 20), new Person("Bob", 15));
        System.out.println(activeNames(people));
        System.out.println(nameCounts(List.of("Alice", "alice", "BOB")));
        System.out.println(totalAgeParallel(people));
        System.out.println(firstMinor(people));
    }
}
