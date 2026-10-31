import java.util.Comparator;
import java.util.Objects;

/**
 * EJ 10/11/12/14：equals 契约、equals/hashCode 配对、toString 诊断价值、
 * compareTo 与 equals 一致的排序；60：long cents 精确表达货币。
 */
public final class EqualsHashCodeExample {

    public static final class Money implements Comparable<Money> {
        private final long cents;
        private final String currency;

        public Money(long cents, String currency) {
            this.cents = cents;
            this.currency = Objects.requireNonNull(currency, "currency");
        }

        @Override public boolean equals(Object o) { // 10：只在逻辑相等需要时覆写
            if (this == o) return true;
            if (!(o instanceof Money other)) return false; // 与不兼容类型永不等
            return cents == other.cents && currency.equals(other.currency);
        }

        @Override public int hashCode() { // 11：与 equals 字段保持一致
            return 31 * currency.hashCode() + Long.hashCode(cents);
        }

        @Override public String toString() { // 12：有诊断价值，不含敏感信息
            return cents / 100 + "." + String.format("%02d", cents % 100) + " " + currency;
        }

        @Override public int compareTo(Money other) { // 14：与 equals 一致的全序
            return Comparator.comparing((Money m) -> m.currency)
                    .thenComparingLong(m -> m.cents)
                    .compare(this, other);
        }
    }

    public static void main(String[] args) {
        var tree = new java.util.TreeMap<Money, String>();
        tree.put(new Money(1_50, "CNY"), "one-fifty");
        tree.put(new Money(1_00, "CNY"), "one");
        tree.put(new Money(1_00, "USD"), "dollar");
        System.out.println(tree); // compareTo 提供一致顺序，TreeMap 可用
        var set = new java.util.HashSet<Money>();
        set.add(new Money(1_50, "CNY"));
        set.add(new Money(1_50, "CNY"));
        System.out.println("set size = " + set.size()); // equals/hashCode 配对 → 去重成立
    }
}
