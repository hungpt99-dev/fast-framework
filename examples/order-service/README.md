# Order Service Example

A complete example demonstrating Fast Framework usage.

## How Controller → Handler Works

```
HTTP POST /api/orders/get
    ↓
Request Body: {"id": "ORD-001"}
    ↓
OrderController.getOrder(GetOrderQuery query)
    ↓
CqrsDispatcher extracts GetOrderQuery from @RequestBody
    ↓
QueryBus finds handler for GetOrderQuery type
    ↓
GetOrderHandler.handle(query)
    ↓
OrderRepository.findById(query.id())
    ↓
Response: OrderDto
```

**Key Point:** The `@RequestBody` object type determines which handler is invoked.

## Structure

```
order-service/
├── build.gradle
├── src/main/java/com/example/order/
│   ├── OrderApplication.java
│   ├── controller/
│   │   └── OrderController.java    # Interface with @Query/@Command
│   ├── dto/
│   │   ├── OrderDto.java
│   │   ├── CreateOrderCmd.java     # Handler: CreateOrderHandler
│   │   ├── GetOrderQuery.java      # Handler: GetOrderHandler
│   │   └── GetOrdersByCustomerQuery.java
│   ├── handler/
│   │   ├── GetOrderHandler.java         # QueryHandler<GetOrderQuery, OrderDto>
│   │   ├── GetOrdersByCustomerHandler.java
│   │   └── CreateOrderHandler.java      # CommandHandler<CreateOrderCmd>
│   └── repository/
│       └── OrderRepository.java
└── src/main/resources/
    ├── application.yml
    └── schema.sql
```

## Running

```bash
cd examples/order-service
./gradlew bootRun
```

## API Endpoints

```bash
# Get order by ID (POST with query body)
curl -X POST http://localhost:8080/api/orders/get \
  -H "Content-Type: application/json" \
  -d '{"id": "ORD-001"}'

# Get orders by customer (POST with query body)
curl -X POST http://localhost:8080/api/orders/by-customer \
  -H "Content-Type: application/json" \
  -d '{"customerId": "CUST-001"}'

# Create new order (POST with command body)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "CUST-003", "total": 99.99}'
```

## H2 Console

Access at: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:orderdb`
- Username: `sa`
- Password: (empty)
