package com.certainlyinstock.product_service.controller;

import java.net.URI;
import java.util.List;

import com.certainlyinstock.product_service.model.Product;
import com.certainlyinstock.product_service.service.ProductService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/certainlyinstock/product-service/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<Product> findAll() {
        return service.findAll();
    }

    @GetMapping("/{pid}")
    public Product findById(@PathVariable int pid) {
        return service.findById(pid);
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product request) {
        Product product = service.create(request);
        return ResponseEntity.created(URI.create("/certainlyinstock/product-service/products/" + product.getPid()))
                .body(product);
    }

    @PutMapping("/{pid}")
    public Product update(@PathVariable int pid, @RequestBody Product request) {
        return service.update(pid, request);
    }

    @DeleteMapping("/{pid}")
    public ResponseEntity<Void> delete(@PathVariable int pid) {
        service.delete(pid);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrityViolation(DataIntegrityViolationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The operation conflicts with existing data, such as inventory referencing this product.");
    }
}
