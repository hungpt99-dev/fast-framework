# Complete Example

A full example showing all framework features working together.

## Entity

```java
@Modal
public class Order {
    @ModalField
    private String id;

    @ModalField(name = "customer_id")
    private String customerId;

    @ModalField
    private BigDecimal total;

    @ModalField
    private String status;

    @ModalField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @ModalField
    private List<OrderItem> items;
}

@Modal
public class OrderItem {
    @ModalField
    private String productId;

    @ModalField
    private int quantity;

    @ModalField
    private BigDecimal price;
}
```

## DTOs / Commands / Queries

```java
public record OrderDto(
    String id,
    String customerId,
    BigDecimal total,
    String status,
    String createdAt,
    List<OrderItemDto> items
) {}

public record CreateOrderCmd(
    String customerId,
    List<OrderItemDto> items
) {}

public record GetOrderQuery(String id) {}
```

## Repository

```java
@SqlRepository
public interface OrderRepository {

    @Select("""
        SELECT id, customer_id, total, status, created_at
        FROM orders WHERE id = :id
    """)
    OrderDto findById(@Param("id") String id);

    @Select("SELECT * FROM orders WHERE customer_id = :customerId")
    List<OrderDto> findByCustomer(@Param("customerId") String customerId);

    @Execute("""
        INSERT INTO orders(id, customer_id, total, status, created_at)
        VALUES (:id, :customerId, :total, :status, :createdAt)
    """)
    void insert(@Param("id") String id,
                @Param("customerId") String customerId,
                @Param("total") BigDecimal total,
                @Param("status") String status,
                @Param("createdAt") LocalDateTime createdAt);
}
```

## Controller

```java
@HttpController
@RequestMapping("/api/orders")
public interface OrderController {

    @Query
    @GetMapping("/{id}")
    OrderDto getOrder(@PathVariable String id);

    @Query
    @GetMapping
    List<OrderDto> getCustomerOrders(@RequestParam String customerId);

    @Command
    @PostMapping
    void createOrder(@RequestBody CreateOrderCmd cmd);
}
```

## Handlers

```java
@Component
public class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderDto> {

    private final OrderRepository repository;

    public GetOrderHandler(OrderRepository repository) {
        this.repository = repository;
    }

    @TraceLog(slowMs = 50)
    @Override
    public OrderDto handle(GetOrderQuery query) {
        return repository.findById(query.id());
    }
}

@Component
public class CreateOrderHandler implements CommandHandler<CreateOrderCmd> {

    private final OrderRepository repository;

    public CreateOrderHandler(OrderRepository repository) {
        this.repository = repository;
    }

    @Loggable("Creating new order")
    @Override
    public void handle(CreateOrderCmd cmd) {
        String orderId = UUID.randomUUID().toString();
        BigDecimal total = calculateTotal(cmd.items());
        
        repository.insert(
            orderId,
            cmd.customerId(),
            total,
            "PENDING",
            LocalDateTime.now()
        );
    }

    private BigDecimal calculateTotal(List<OrderItemDto> items) {
        return items.stream()
            .map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

## Application

```java
@SpringBootApplication
@EnableCqrs(basePackages = "com.example.controller")
@EnableSqlRepositories(basePackages = "com.example.repository")
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

## API Requests

```bash
# Create order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "C001", "items": [{"productId": "P001", "quantity": 2, "price": 29.99}]}'

# Get order
curl http://localhost:8080/api/orders/ORD-001

# Get customer orders
curl http://localhost:8080/api/orders?customerId=C001
```
