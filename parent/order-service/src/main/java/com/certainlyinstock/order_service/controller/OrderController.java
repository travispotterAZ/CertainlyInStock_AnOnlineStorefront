package com.certainlyinstock.order_service.controller;

import com.certainlyinstock.order_service.entity.Payment;
import com.certainlyinstock.order_service.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<Payment> getAllPayments() {
        return orderService.getAllPayments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        return orderService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Payment>> getPaymentsByOrderId(@PathVariable Long orderId) {
        List<Payment> payments = orderService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(payments);
    }

    @PostMapping
    public ResponseEntity<Payment> processPayment(@RequestBody Payment payment) {
        Payment savedPayment = orderService.processPayment(payment);
        System.out.println("=== SAVED PAYMENT ID: " + savedPayment.getId() + " ===");
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPayment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Payment> updatePayment(@PathVariable Long id, @RequestBody Payment payment) {
        payment.setId(id);
        Payment updatedPayment = orderService.processPayment(payment);
        return ResponseEntity.ok(updatedPayment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        orderService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }
}
