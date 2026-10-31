import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * EJ 2/17/49/50：不可变值对象 + Builder + 入口校验 + 防御性拷贝。
 * JDK17+：参数少且无复杂校验时可用 record 替代；此处因跨字段校验选择 Builder。
 */
public final class BuilderExample {

    public enum Status { NEW, PAID, SHIPPED, CANCELLED }

    public static final class Order {
        private final String orderId;
        private final String userId;
        private final List<String> items;
        private final Instant createdAt;
        private final Status status;
        private final String couponCode; // 可选项：null 即有值语义由 Optional 承载（55）

        private Order(Builder b) {
            this.orderId = Objects.requireNonNull(b.orderId, "orderId");
            this.userId = Objects.requireNonNull(b.userId, "userId");
            this.createdAt = Objects.requireNonNull(b.createdAt, "createdAt");
            this.status = Objects.requireNonNull(b.status, "status");
            if (b.items == null || b.items.isEmpty()) {
                throw new IllegalArgumentException("items 不能为空");
            }
            this.items = List.copyOf(b.items); // 50：不可变防御副本，调用方改源 list 不影响
            this.couponCode = b.couponCode;
        }

        public static Builder builder() { return new Builder(); }

        public String orderId() { return orderId; }
        public String userId() { return userId; }
        public List<String> items() { return items; } // 已是不可变视图，直接外发（50）
        public Instant createdAt() { return createdAt; }
        public Status status() { return status; }
        public String couponCode() { return couponCode; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Order other)) return false; // JDK16+ 模式匹配
            return orderId.equals(other.orderId)
                    && userId.equals(other.userId)
                    && items.equals(other.items)
                    && createdAt.equals(other.createdAt)
                    && status == other.status
                    && Objects.equals(couponCode, other.couponCode);
        }

        @Override public int hashCode() {
            return Objects.hash(orderId, userId, items, createdAt, status, couponCode); // 11
        }

        @Override public String toString() { // 12：有诊断价值、无敏感字段
            return "Order[orderId=" + orderId + ", userId=" + userId
                    + ", status=" + status + ", items=" + items.size() + "]";
        }
    }

    public static final class Builder {
        private String orderId;
        private String userId;
        private Instant createdAt;
        private Status status = Status.NEW;
        private List<String> items;
        private String couponCode;

        private Builder() {}

        public Builder orderId(String v) { this.orderId = v; return this; }
        public Builder userId(String v) { this.userId = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder status(Status v) { this.status = v; return this; }
        public Builder couponCode(String v) { this.couponCode = v; return this; }
        public Builder items(List<String> v) {
            this.items = v == null ? null : new ArrayList<>(v); return this;
        }
        public Order build() { return new Order(this); } // 校验收敛在构造器一处（2/49）
    }

    public static void main(String[] args) {
        var order = Order.builder()
                .orderId("O-1").userId("U-1").createdAt(Instant.now())
                .items(List.of("a", "b")).status(Status.PAID)
                .build();
        System.out.println(order);
        try {
            Order.builder().orderId("O-2").userId("U-2").createdAt(Instant.now())
                    .items(List.of()).build(); // 入口校验立即失败（49）
        } catch (IllegalArgumentException e) {
            System.out.println("caught: " + e.getMessage());
        }
        var set = new java.util.HashSet<Order>();
        set.add(order);
        set.add(order);
        System.out.println("set size = " + set.size()); // equals/hashCode 一致 → 去重成立
    }
}
