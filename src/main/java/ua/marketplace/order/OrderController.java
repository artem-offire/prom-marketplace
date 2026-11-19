package ua.marketplace.order;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.marketplace.common.web.PageResponse;
import ua.marketplace.order.dto.CheckoutRequest;
import ua.marketplace.order.dto.OrderDto;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderDto> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.checkout(request));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.get(id));
    }

    @GetMapping("/sellers/{id}/orders")
    public ResponseEntity<PageResponse<OrderDto>> listBySeller(
            @PathVariable Long id,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.listBySeller(id, status, page, size));
    }

    @PostMapping("/orders/{id}/confirm")
    public ResponseEntity<OrderDto> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.apply(id, OrderAction.CONFIRM));
    }

    @PostMapping("/orders/{id}/ship")
    public ResponseEntity<OrderDto> ship(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.apply(id, OrderAction.SHIP));
    }

    @PostMapping("/orders/{id}/complete")
    public ResponseEntity<OrderDto> complete(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.apply(id, OrderAction.COMPLETE));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<OrderDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.apply(id, OrderAction.CANCEL));
    }

    @PostMapping("/orders/{id}/return")
    public ResponseEntity<OrderDto> returnOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.apply(id, OrderAction.RETURN));
    }
}
