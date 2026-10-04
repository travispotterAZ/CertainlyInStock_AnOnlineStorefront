package com.certainlyinstock.order_service.service;

import com.certainlyinstock.order_service.entity.Payment;
import com.certainlyinstock.order_service.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Payment> getAllPayments() {
        return orderRepository.findAll();
    }

    public Optional<Payment> getPaymentById(Long id) {
        return orderRepository.findById(id);
    }
    
    public List<Payment> getPaymentsByOrderId(Long orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    @Transactional
    public Payment processPayment(Payment payment) {
        payment.setId(null);
        if (payment.getStatus() == null) {
            payment.setStatus("COMPLETED");
        }
        return orderRepository.save(payment);
    }

    public void deletePayment(Long id) {
        orderRepository.deleteById(id);
    }

    
}
