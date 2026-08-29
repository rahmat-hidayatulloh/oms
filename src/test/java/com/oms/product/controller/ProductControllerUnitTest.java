package com.oms.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.oms.product.dto.ProductRequest;
import com.oms.product.dto.ProductResponse;
import com.oms.product.entity.Category;
import com.oms.product.service.ProductServiceInterface;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration test for ProductController using @SpringBootTest.
 * Tests the controller with mocked service layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductServiceInterface productService;


    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_shouldReturn201_whenValidRequest() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductResponse response = new ProductResponse(
                productId,
                "Kaos Polos Hitam",
                "Kaos katun premium",
                new BigDecimal("85000.00"),
                100,
                Category.CLOTHING,
                null,
                null);

        when(productService.create(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Kaos Polos Hitam",
                            "description": "Kaos katun premium",
                            "price": 85000.00,
                            "stock": 100,
                            "category": "CLOTHING"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("Kaos Polos Hitam"))
                .andExpect(jsonPath("$.price").value(85000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_shouldReturn400_whenValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "AB",
                            "description": "Test",
                            "price": -1,
                            "stock": 50,
                            "category": "CLOTHING"
                        }
                        """))
                .andExpect(status().isBadRequest());
    }


    @Test
    @WithMockUser(roles = { "CUSTOMER", "ADMIN" })
    void getAllProducts_shouldReturn200WithProducts() throws Exception {
        ProductResponse product1 = new ProductResponse(
                UUID.randomUUID(), "Product 1", "Desc 1", new BigDecimal("50000.00"), 10, Category.CLOTHING, null,
                null);
        ProductResponse product2 = new ProductResponse(
                UUID.randomUUID(), "Product 2", "Desc 2", new BigDecimal("100000.00"), 20, Category.ELECTRONICS, null,
                null);

        when(productService.findAll()).thenReturn(List.of(product1, product2));

        mockMvc.perform(get("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Product 1"))
                .andExpect(jsonPath("$[1].name").value("Product 2"));
    }


    @Test
    @WithMockUser(roles = { "CUSTOMER", "ADMIN" })
    void getProductById_shouldReturn200_whenProductExists() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductResponse response = new ProductResponse(
                productId, "Kaos Polos", "Desc", new BigDecimal("85000.00"), 100, Category.CLOTHING, null, null);

        when(productService.findById(productId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/{id}", productId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("Kaos Polos"));
    }


    @Test
    void createProduct_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Test Product",
                            "description": "Test",
                            "price": 50000.00,
                            "stock": 10,
                            "category": "CLOTHING"
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createProduct_shouldReturn403_whenCustomerTriesToCreate() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Test Product",
                            "description": "Test",
                            "price": 50000.00,
                            "stock": 10,
                            "category": "CLOTHING"
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAllProducts_shouldReturn200_forCustomer() throws Exception {
        when(productService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}