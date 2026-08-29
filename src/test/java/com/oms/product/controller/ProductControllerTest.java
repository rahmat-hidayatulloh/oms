package com.oms.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.product.dto.ProductRequest;
import com.oms.product.dto.ProductResponse;
import com.oms.product.entity.Category;
import com.oms.exception.BadRequestException;
import com.oms.exception.ResourceNotFoundException;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductServiceInterface productService;

    private ProductRequest sampleRequest() {
        return new ProductRequest(
                "Buku Tulis",
                "Buku tulis 100 lembar",
                new BigDecimal("15000.00"),
                100,
                Category.BOOKS);
    }

    private ProductResponse sampleResponse(UUID id) {
        return new ProductResponse(
                id,
                "Buku Tulis",
                "Buku tulis 100 lembar",
                new BigDecimal("15000.00"),
                100,
                Category.BOOKS,
                null,
                null);
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_shouldReturn201() throws Exception {
        ProductRequest request = sampleRequest();
        UUID id = UUID.randomUUID();
        when(productService.create(any(ProductRequest.class))).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Buku Tulis"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createProduct_shouldReturn403ForCustomer() throws Exception {
        ProductRequest request = sampleRequest();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_shouldReturn403WhenUnauthenticated() throws Exception {
        ProductRequest request = sampleRequest();

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllProducts_shouldReturn200() throws Exception {
        when(productService.findAll()).thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAllProducts_shouldAllowCustomer() throws Exception {
        when(productService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getProductById_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.findById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getProductById_shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.findById(id)).thenThrow(new ResourceNotFoundException("Product", "id", id));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        ProductRequest request = sampleRequest();
        when(productService.update(eq(id), any(ProductRequest.class))).thenReturn(sampleResponse(id));

        mockMvc.perform(put("/api/v1/products/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateProduct_shouldReturn403ForCustomer() throws Exception {
        UUID id = UUID.randomUUID();
        ProductRequest request = sampleRequest();

        mockMvc.perform(put("/api/v1/products/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_shouldReturn400WhenDuplicateName() throws Exception {
        UUID id = UUID.randomUUID();
        ProductRequest request = sampleRequest();
        when(productService.update(eq(id), any(ProductRequest.class)))
                .thenThrow(new BadRequestException("Product name 'Buku Tulis' already exists"));

        mockMvc.perform(put("/api/v1/products/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(productService).delete(id);

        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteProduct_shouldReturn403ForCustomer() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Product", "id", id)).when(productService).delete(id);

        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound());
    }
}