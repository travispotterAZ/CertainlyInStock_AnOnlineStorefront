package com.certainlyinstock.order_service.repository;

import com.certainlyinstock.order_service.entity.Payment;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class OrderRepository {
    private final ConcurrentHashMap<Long, Payment> db = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public List<Payment> findAll() {
        return new ArrayList<>(db.values());
    }

    public Optional<Payment> findById(Long id) {
        return Optional.ofNullable(db.get(id));
    }

    public List<Payment> findByUserId(Long userId) {
        return db.values().stream()
                .filter(p -> userId.equals(p.getUserId()))
                .toList();
    }

    public List<Payment> findByOrderId(Long orderId) {
        return db.values().stream()
                .filter(p -> orderId.equals(p.getOrderId()))
                .toList();
    }

    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            payment.setId(idGenerator.getAndIncrement());
        }
        db.put(payment.getId(), payment);
        return payment;
    }

    public void deleteById(Long id) {
        db.remove(id);
    }
}