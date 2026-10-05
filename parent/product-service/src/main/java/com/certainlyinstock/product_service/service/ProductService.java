package com.certainlyinstock.product_service.service;

import java.time.LocalDate;
import java.util.List;

import com.certainlyinstock.product_service.model.Product;
import com.certainlyinstock.product_service.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public Product findById(int pid) {
        return repository.findById(pid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @Transactional
    public Product create(Product request) {
        Product product = new Product();
        applyFields(product, request);
        LocalDate today = LocalDate.now();
        product.setDateAdded(today);
        product.setLastUpdated(today);
        return repository.save(product);
    }

    @Transactional
    public Product update(int pid, Product request) {
        Product product = findById(pid);
        applyFields(product, request);
        product.setLastUpdated(LocalDate.now());
        return repository.save(product);
    }

    @Transactional
    public void delete(int pid) {
        repository.delete(findById(pid));
        repository.flush();
    }

    private void applyFields(Product product, Product request) {
        // Only these fields are writable; IDs and audit dates remain service-managed.
        product.setCategory(request.getCategory());
        product.setImage(request.getImage());
        product.setPrice(request.getPrice());
        product.setStatus(request.getStatus());
    }
}
