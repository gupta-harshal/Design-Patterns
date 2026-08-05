import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

// ============================================================
// 0. DOMAIN ERRORS
// ============================================================

class ShopException extends RuntimeException {
    public ShopException(String message) {
        super(message);
    }
}

class OutOfStockException extends ShopException {
    public OutOfStockException(String productName, int requested, int available) {
        super("Out of stock: " + productName + " requested=" + requested + " available=" + available);
    }
}

// ============================================================
// 1. PRODUCT + CATALOG + INVENTORY
// ============================================================

class Product {
    private final String id;
    private final String name;
    private final BigDecimal price;

    public Product(String id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price.setScale(2, RoundingMode.HALF_UP);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return name + " (" + id + ") $" + price;
    }
}

// Stock levels live here, not on Product: availability is a warehouse fact,
// not a property of the product description.
class Inventory {
    private final Map<String, Integer> stockByProductId = new HashMap<>();

    public void setStock(String productId, int quantity) {
        stockByProductId.put(productId, quantity);
    }

    public int available(String productId) {
        return stockByProductId.getOrDefault(productId, 0);
    }

    public boolean hasStock(String productId, int quantity) {
        return available(productId) >= quantity;
    }

    public void decrement(String productId, int quantity) {
        int current = available(productId);
        if (current < quantity) {
            throw new ShopException("Cannot decrement " + productId + " below zero");
        }
        stockByProductId.put(productId, current - quantity);
    }

    public void increment(String productId, int quantity) {
        stockByProductId.put(productId, available(productId) + quantity);
    }
}

class Catalog {
    private final Map<String, Product> productsById = new LinkedHashMap<>();
    private final Inventory inventory = new Inventory();

    public void addProduct(Product product, int initialStock) {
        productsById.put(product.getId(), product);
        inventory.setStock(product.getId(), initialStock);
    }

    public Product getProduct(String productId) {
        Product product = productsById.get(productId);
        if (product == null) {
            throw new ShopException("Unknown product: " + productId);
        }
        return product;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public List<Product> listProducts() {
        return new ArrayList<>(productsById.values());
    }
}

// ============================================================
// 2. CART
// ============================================================

class CartLine {
    private final Product product;
    private int quantity;

    public CartLine(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal lineTotal() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return product.getName() + " x" + quantity + " = $" + lineTotal();
    }
}

class Cart {
    private final String customerId;
    private final Map<String, CartLine> linesByProductId = new LinkedHashMap<>();

    public Cart(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void add(Product product, int quantity) {
        requirePositive(quantity);
        CartLine line = linesByProductId.get(product.getId());
        if (line == null) {
            linesByProductId.put(product.getId(), new CartLine(product, quantity));
        } else {
            line.setQuantity(line.getQuantity() + quantity);
        }
    }

    // Setting quantity to 0 removes the line — one method, no special-case API.
    public void updateQuantity(String productId, int quantity) {
        CartLine line = requireLine(productId);
        if (quantity < 0) {
            throw new ShopException("Quantity cannot be negative");
        }
        if (quantity == 0) {
            linesByProductId.remove(productId);
        } else {
            line.setQuantity(quantity);
        }
    }

    public void remove(String productId) {
        requireLine(productId);
        linesByProductId.remove(productId);
    }

    public void clear() {
        linesByProductId.clear();
    }

    public boolean isEmpty() {
        return linesByProductId.isEmpty();
    }

    public List<CartLine> getLines() {
        return new ArrayList<>(linesByProductId.values());
    }

    public BigDecimal total() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : linesByProductId.values()) {
            total = total.add(line.lineTotal());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private CartLine requireLine(String productId) {
        CartLine line = linesByProductId.get(productId);
        if (line == null) {
            throw new ShopException("Product not in cart: " + productId);
        }
        return line;
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new ShopException("Quantity must be positive");
        }
    }

    public void print() {
        System.out.println("Cart of " + customerId + ":");
        for (CartLine line : linesByProductId.values()) {
            System.out.println("   " + line);
        }
        System.out.println("   TOTAL = $" + total());
    }
}

// ============================================================
// 3. ORDER + STATE MACHINE
// ============================================================

enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED =
            new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(CREATED, EnumSet.of(PAID, CANCELLED));
        ALLOWED.put(PAID, EnumSet.of(SHIPPED, CANCELLED));
        ALLOWED.put(SHIPPED, EnumSet.of(DELIVERED));
        ALLOWED.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canTransitionTo(OrderStatus next) {
        return ALLOWED.get(this).contains(next);
    }
}

// An order line is a SNAPSHOT: price is copied at checkout time so a later
// price change never rewrites what the customer agreed to pay.
class OrderLine {
    private final String productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;

    public OrderLine(String productId, String productName, BigDecimal unitPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return productName + " x" + quantity + " @ $" + unitPrice + " = $" + lineTotal();
    }
}

class Order {
    private final String id;
    private final String customerId;
    private final List<OrderLine> lines;
    private final BigDecimal total;
    private OrderStatus status;
    private String paymentReference;

    public Order(String id, String customerId, List<OrderLine> lines, BigDecimal total) {
        this.id = id;
        this.customerId = customerId;
        this.lines = lines;
        this.total = total;
        this.status = OrderStatus.CREATED;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public BigDecimal getTotal() {
        return total;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    // The only mutator for status — every illegal jump is caught in one place.
    public void moveTo(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new ShopException("Illegal transition " + status + " -> " + next
                    + " for order " + id);
        }
        status = next;
    }

    @Override
    public String toString() {
        return "Order{" + id + ", " + status + ", $" + total + "}";
    }
}

// ============================================================
// 4. PAYMENT (Strategy over a mocked gateway)
// ============================================================

class PaymentResult {
    private final boolean success;
    private final String reference;
    private final String failureReason;

    private PaymentResult(boolean success, String reference, String failureReason) {
        this.success = success;
        this.reference = reference;
        this.failureReason = failureReason;
    }

    public static PaymentResult success(String reference) {
        return new PaymentResult(true, reference, null);
    }

    public static PaymentResult failure(String reason) {
        return new PaymentResult(false, null, reason);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReference() {
        return reference;
    }

    public String getFailureReason() {
        return failureReason;
    }
}

interface PaymentMethod {
    PaymentResult pay(String orderId, BigDecimal amount);

    String name();
}

class MockCardPayment implements PaymentMethod {
    private final String cardLast4;
    private final BigDecimal limit;

    public MockCardPayment(String cardLast4, BigDecimal limit) {
        this.cardLast4 = cardLast4;
        this.limit = limit;
    }

    @Override
    public PaymentResult pay(String orderId, BigDecimal amount) {
        if (amount.compareTo(limit) > 0) {
            return PaymentResult.failure("Card ****" + cardLast4 + " declined: limit exceeded");
        }
        return PaymentResult.success("CARD-" + cardLast4 + "-" + orderId);
    }

    @Override
    public String name() {
        return "CARD ****" + cardLast4;
    }
}

class MockWalletPayment implements PaymentMethod {
    private BigDecimal balance;

    public MockWalletPayment(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public PaymentResult pay(String orderId, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            return PaymentResult.failure("Wallet balance $" + balance + " < $" + amount);
        }
        balance = balance.subtract(amount);
        return PaymentResult.success("WALLET-" + orderId);
    }

    @Override
    public String name() {
        return "WALLET";
    }
}

// ============================================================
// 5. ORDER SERVICE (orchestration)
// ============================================================

class OrderService {
    private final Catalog catalog;
    private final Map<String, Order> ordersById = new LinkedHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(1000);

    public OrderService(Catalog catalog) {
        this.catalog = catalog;
    }

    // Checkout validates stock and snapshots prices, but does NOT reserve stock.
    public Order checkout(Cart cart) {
        if (cart.isEmpty()) {
            throw new ShopException("Cannot checkout an empty cart");
        }
        Inventory inventory = catalog.getInventory();
        for (CartLine line : cart.getLines()) {
            String productId = line.getProduct().getId();
            if (!inventory.hasStock(productId, line.getQuantity())) {
                throw new OutOfStockException(line.getProduct().getName(),
                        line.getQuantity(), inventory.available(productId));
            }
        }

        List<OrderLine> orderLines = new ArrayList<>();
        for (CartLine line : cart.getLines()) {
            Product product = line.getProduct();
            orderLines.add(new OrderLine(product.getId(), product.getName(),
                    product.getPrice(), line.getQuantity()));
        }

        Order order = new Order("ORD-" + sequence.incrementAndGet(),
                cart.getCustomerId(), orderLines, cart.total());
        ordersById.put(order.getId(), order);
        cart.clear();
        return order;
    }

    // Confirm = charge, then decrement stock. Stock is re-checked here because
    // it may have been sold to someone else between checkout and payment.
    public Order confirm(String orderId, PaymentMethod paymentMethod) {
        Order order = requireOrder(orderId);
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ShopException("Only CREATED orders can be paid, was " + order.getStatus());
        }
        Inventory inventory = catalog.getInventory();
        for (OrderLine line : order.getLines()) {
            if (!inventory.hasStock(line.getProductId(), line.getQuantity())) {
                throw new OutOfStockException(line.getProductName(), line.getQuantity(),
                        inventory.available(line.getProductId()));
            }
        }

        PaymentResult result = paymentMethod.pay(order.getId(), order.getTotal());
        if (!result.isSuccess()) {
            throw new ShopException("Payment failed: " + result.getFailureReason());
        }

        for (OrderLine line : order.getLines()) {
            inventory.decrement(line.getProductId(), line.getQuantity());
        }
        order.setPaymentReference(result.getReference());
        order.moveTo(OrderStatus.PAID);
        return order;
    }

    public Order ship(String orderId) {
        Order order = requireOrder(orderId);
        order.moveTo(OrderStatus.SHIPPED);
        return order;
    }

    public Order deliver(String orderId) {
        Order order = requireOrder(orderId);
        order.moveTo(OrderStatus.DELIVERED);
        return order;
    }

    // Cancelling a PAID order returns stock to the shelf; a CREATED order never took any.
    public Order cancel(String orderId) {
        Order order = requireOrder(orderId);
        boolean stockWasTaken = order.getStatus() == OrderStatus.PAID;
        order.moveTo(OrderStatus.CANCELLED);
        if (stockWasTaken) {
            Inventory inventory = catalog.getInventory();
            for (OrderLine line : order.getLines()) {
                inventory.increment(line.getProductId(), line.getQuantity());
            }
        }
        return order;
    }

    public List<Order> allOrders() {
        return new ArrayList<>(ordersById.values());
    }

    private Order requireOrder(String orderId) {
        Order order = ordersById.get(orderId);
        if (order == null) {
            throw new ShopException("Unknown order: " + orderId);
        }
        return order;
    }
}

// ============================================================
// 6. CLIENT DEMO
// ============================================================

public class Main {
    public static void main(String[] args) {
        Catalog catalog = new Catalog();
        catalog.addProduct(new Product("P1", "Mechanical Keyboard", new BigDecimal("120.00")), 5);
        catalog.addProduct(new Product("P2", "USB-C Cable", new BigDecimal("9.50")), 20);
        catalog.addProduct(new Product("P3", "27\" Monitor", new BigDecimal("310.00")), 2);

        OrderService orderService = new OrderService(catalog);
        Inventory inventory = catalog.getInventory();

        System.out.println("=== 1. BUILD THE CART ===");
        Cart cart = new Cart("CUST-1");
        cart.add(catalog.getProduct("P1"), 1);
        cart.add(catalog.getProduct("P2"), 3);
        cart.add(catalog.getProduct("P3"), 1);
        cart.print();

        System.out.println();
        System.out.println("=== 2. UPDATE AND REMOVE ===");
        cart.updateQuantity("P2", 2);
        cart.remove("P3");
        cart.add(catalog.getProduct("P2"), 1);
        cart.print();

        System.out.println();
        System.out.println("=== 3. CHECKOUT -> CREATED ===");
        Order order = orderService.checkout(cart);
        System.out.println(order + " lines:");
        for (OrderLine line : order.getLines()) {
            System.out.println("   " + line);
        }
        System.out.println("Cart after checkout is empty: " + cart.isEmpty());

        System.out.println();
        System.out.println("=== 4. PAY -> INVENTORY DECREMENTS ===");
        System.out.println("stock before: P1=" + inventory.available("P1")
                + " P2=" + inventory.available("P2"));
        orderService.confirm(order.getId(), new MockCardPayment("4242", new BigDecimal("500.00")));
        System.out.println("after payment " + order + " ref=" + order.getPaymentReference());
        System.out.println("stock after : P1=" + inventory.available("P1")
                + " P2=" + inventory.available("P2"));

        System.out.println();
        System.out.println("=== 5. SHIP -> DELIVER ===");
        orderService.ship(order.getId());
        System.out.println("shipped   -> " + order.getStatus());
        orderService.deliver(order.getId());
        System.out.println("delivered -> " + order.getStatus());

        System.out.println();
        System.out.println("=== 6. FAILURE PATH: NOT ENOUGH INVENTORY ===");
        Cart greedyCart = new Cart("CUST-2");
        greedyCart.add(catalog.getProduct("P3"), 5);
        System.out.println("P3 available = " + inventory.available("P3") + ", cart wants 5");
        try {
            orderService.checkout(greedyCart);
        } catch (OutOfStockException e) {
            System.out.println("Rejected: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=== 7. FAILURE PATH: PAYMENT DECLINED ===");
        Cart bigCart = new Cart("CUST-3");
        bigCart.add(catalog.getProduct("P3"), 2);
        Order bigOrder = orderService.checkout(bigCart);
        try {
            orderService.confirm(bigOrder.getId(), new MockWalletPayment(new BigDecimal("100.00")));
        } catch (ShopException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
        System.out.println("order stays " + bigOrder.getStatus()
                + ", stock untouched P3=" + inventory.available("P3"));

        System.out.println();
        System.out.println("=== 8. CANCEL RESTOCKS A PAID ORDER ===");
        orderService.confirm(bigOrder.getId(), new MockWalletPayment(new BigDecimal("1000.00")));
        System.out.println("paid, P3 stock = " + inventory.available("P3"));
        orderService.cancel(bigOrder.getId());
        System.out.println("cancelled, P3 stock restored = " + inventory.available("P3"));

        System.out.println();
        System.out.println("=== 9. ILLEGAL TRANSITION IS BLOCKED ===");
        try {
            orderService.ship(bigOrder.getId());
        } catch (ShopException e) {
            System.out.println("Rejected: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=== ORDER BOOK ===");
        for (Order o : orderService.allOrders()) {
            System.out.println("   " + o);
        }
    }
}
