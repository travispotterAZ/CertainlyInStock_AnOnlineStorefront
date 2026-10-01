package com.certainlyinstock.product_service.model;
/*******************************************************************
 *  Author: Andreas Yubeta
 * 
 *  Date: 9/30/2026
 * 
 *  File: Product.java
 * 
 *  Description: This class represents a product in the product 
 *  service. It contains attributes and methods related to the 
 *  product entity.
 *
 *******************************************************************/
import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "product")
@Data                // Generates getters, setters, toString, equals, and hashCode methods
@NoArgsConstructor   // Generates a no-argument constructor
@AllArgsConstructor  // Generates a constructor with all arguments
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pid")
    private Integer pid;

    @Column(name = "category", columnDefinition = "varchar")
    private String category;

    @Column(name = "image", columnDefinition = "varchar")
    private String image;

    @Column(name = "price", columnDefinition = "numeric")
    private BigDecimal price;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "date_added", updatable = false)
    private LocalDate dateAdded;

    @Column(name = "last_updated")
    private LocalDate lastUpdated;
}
