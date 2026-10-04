package com.certainlyinstock.product_service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProductCrudTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clearData() {
        jdbc.update("DELETE FROM inventory");
        jdbc.update("DELETE FROM product");
    }

    @Test
    void createReadUpdateAndDeleteProduct() throws Exception {
        String location = createProduct();
        LocalDate today = LocalDate.now();

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Books"))
                .andExpect(jsonPath("$.image").value("https://example.com/book.png"))
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.dateAdded").value(today.toString()))
                .andExpect(jsonPath("$.lastUpdated").value(today.toString()));
        assertThat(jdbc.queryForObject("SELECT price FROM product", BigDecimal.class))
                .isEqualByComparingTo("1234567890.123456789");

        mvc.perform(get("/certainlyinstock/product-service/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Updating a product must retain its original creation date.
        LocalDate originalDate = today.minusDays(10);
        jdbc.update("UPDATE product SET date_added = ?, last_updated = ?",
                Date.valueOf(originalDate), Date.valueOf(originalDate));
        mvc.perform(put(location).contentType("application/json").content("""
                {"pid":99999,"category":"Games","image":"game.png","price":19.9876,"status":false,
                 "dateAdded":"2000-01-01","lastUpdated":"2000-01-01"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Games"))
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.dateAdded").value(originalDate.toString()))
                .andExpect(jsonPath("$.lastUpdated").value(today.toString()));
        mvc.perform(get(location))
                .andExpect(jsonPath("$.image").value("game.png"))
                .andExpect(jsonPath("$.status").value(false));
        assertThat(jdbc.queryForObject("SELECT price FROM product", BigDecimal.class))
                .isEqualByComparingTo("19.9876");

        mvc.perform(delete(location)).andExpect(status().isNoContent());
        mvc.perform(get(location)).andExpect(status().isNotFound());
        mvc.perform(get("/certainlyinstock/product-service/products")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void missingProductsReturnNotFoundWithoutCreatingRows() throws Exception {
        mvc.perform(get("/certainlyinstock/product-service/products/99999")).andExpect(status().isNotFound());
        mvc.perform(put("/certainlyinstock/product-service/products/99999").contentType("application/json")
                        .content("{\"category\":\"Books\",\"price\":10,\"status\":true}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/certainlyinstock/product-service/products/99999")).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product", Integer.class)).isZero();
    }

    @Test
    void inventoryForeignKeyPreventsProductDeletion() throws Exception {
        String location = createProduct();
        int pid = Integer.parseInt(location.substring(location.lastIndexOf('/') + 1));
        jdbc.update("INSERT INTO inventory (pid, qty, backorder) VALUES (?, ?, ?)", pid, 5, false);

        mvc.perform(delete(location)).andExpect(status().isConflict());
        mvc.perform(get(location)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT qty FROM inventory WHERE pid = ?", Integer.class, pid))
                .isEqualTo(5);
    }

    @Test
    void invalidPriceReturnsBadRequest() throws Exception {
        mvc.perform(post("/certainlyinstock/product-service/products").contentType("application/json")
                        .content("{\"price\":\"not-a-number\"}"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product", Integer.class)).isZero();
    }

    private String createProduct() throws Exception {
        return mvc.perform(post("/certainlyinstock/product-service/products").contentType("application/json").content("""
                {"pid":99999,"dateAdded":"2000-01-01","lastUpdated":"2000-01-01",
                 "category":"Books","image":"https://example.com/book.png",
                 "price":1234567890.123456789,"status":true}
                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.pid").isNumber())
                .andExpect(jsonPath("$.pid").value(org.hamcrest.Matchers.not(99999)))
                .andReturn().getResponse().getHeader("Location");
    }
}
