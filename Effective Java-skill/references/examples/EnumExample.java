import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/**
 * EJ 34/35/36/37/38：枚举替代 int 常量、常量特定行为、EnumSet/EnumMap、
 * wire 格式不用 ordinal、JDK17 switch 表达式。
 */
public final class EnumExample {

    public enum Operation {
        PLUS("+") {
            @Override double apply(double x, double y) { return x + y; }
        },
        MINUS("-") {
            @Override double apply(double x, double y) { return x - y; }
        },
        TIMES("*") {
            @Override double apply(double x, double y) { return x * y; }
        },
        DIVIDE("/") {
            @Override double apply(double x, double y) { return x / y; }
        };

        private final String symbol;

        Operation(String symbol) {
            this.symbol = symbol;
        }

        abstract double apply(double x, double y); // 34：行为挂到常量上，消灭散落 switch

        public String symbol() { return symbol; }

        /** 35：对外 wire 格式用显式 code 或 name，绝不用 ordinal。 */
        public static Operation fromWire(int wire) {
            return switch (wire) { // JDK14+ switch 表达式
                case 1 -> PLUS;
                case 2 -> MINUS;
                case 3 -> TIMES;
                case 4 -> DIVIDE;
                default -> throw new IllegalArgumentException("wire: " + wire);
            };
        }
    }

    /** 36/37：EnumSet 取代位域，EnumMap 取代 ordinal 索引数组。 */
    public static final class Flags {
        private final Set<Operation> enabled = EnumSet.noneOf(Operation.class);
        private final EnumMap<Operation, Integer> counts = new EnumMap<>(Operation.class);

        public void enable(Operation op) {
            enabled.add(op);
            counts.merge(op, 1, Integer::sum);
        }

        public Set<Operation> enabled() { return Set.copyOf(enabled); }
        public int count(Operation op) { return counts.getOrDefault(op, 0); }
    }

    public static void main(String[] args) {
        var flags = new Flags();
        flags.enable(Operation.PLUS);
        flags.enable(Operation.TIMES);
        System.out.println(flags.enabled() + " count=" + flags.count(Operation.PLUS));
        System.out.println("6*7=" + Operation.fromWire(3).apply(6, 7));
    }
}
