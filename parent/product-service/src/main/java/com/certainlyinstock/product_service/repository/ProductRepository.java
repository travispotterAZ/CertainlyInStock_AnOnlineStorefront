package com.certainlyinstock.product_service.repository;

import com.certainlyinstock.product_service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides product CRUD operations through Spring Data JPA:
 * {@code save(product)} creates or updates a product,
 * {@code findById(id)} and {@code findAll()} read products, and
 * {@code deleteById(id)} deletes a product.
 */
public interface ProductRepository extends JpaRepository<Product, Integer> {
}
