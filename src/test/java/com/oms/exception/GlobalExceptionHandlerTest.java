package com.oms.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for exception handlers verifying RFC 7807 ProblemDetail responses
 * for all handled exception types. Tests run against SecurityExceptionHandler,
 * ValidationExceptionHandler, BusinessExceptionHandler, and InfrastructureExceptionHandler
 * which are auto-discovered by Spring MVC via @RestControllerAdvice.
 *
 * <p>Notes on authentication tests:
 * <ul>
 *   <li>For unauthenticated requests (no @WithMockUser), Spring Security returns a default
 *       non-ProblemDetail response because the filter chain rejects the request before
 *       reaching the controller. The exception handler ProblemDetail responses
 *       are only used for authenticated requests where the controller is actually invoked.</li>
 *   <li>Unauthenticated access is tested only for status code, not for RFC 7807 fields.</li>
 *   <li>AccessDeniedException occurs only when the user IS authenticated but lacks
 *       the required role. This is handled by SecurityExceptionHandler.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:global_exception_handler_test;DB_CLOSE_DELAY=-1"
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WithRfc7807ForResourceNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/products/12345678-1234-1234-1234-123456789012"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.oms.com/errors/404"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(
                        "Product not found with id: '12345678-1234-1234-1234-123456789012'"))
                .andExpect(jsonPath("$.instance").value("/api/v1/products/12345678-1234-1234-1234-123456789012"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldReturn400WithRfc7807ForBusinessException() throws Exception {
        // First creation: should succeed
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "UniqueProductName",
                                    "description": "First creation",
                                    "price": 50000,
                                    "stock": 10,
                                    "category": "ELECTRONICS"
                                }
                                """))
                .andExpect(status().isCreated());

        // Second creation with same name: should trigger BusinessException via BadRequestException
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "UniqueProductName",
                                    "description": "Duplicate",
                                    "price": 50000,
                                    "stock": 10,
                                    "category": "ELECTRONICS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.oms.com/errors/400"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(
                        "Product name 'UniqueProductName' already exists"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400WithFieldErrorsForValidationFailure() throws Exception {
        // Use empty name and negative price, but VALID category enum to trigger
        // MethodArgumentNotValidException (not HttpMessageNotReadableException)
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "AB",
                                    "description": "Name too short and price negative",
                                    "price": -1,
                                    "stock": 50,
                                    "category": "ELECTRONICS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.oms.com/errors/400"))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.instance").value("/api/v1/products"))
                .andExpect(jsonPath("$.fieldErrors").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void shouldReturn403ForAccessDenied() throws Exception {
        // CUSTOMER role accessing ADMIN endpoint triggers AccessDeniedException
        // Note: Spring Security's filter handles this before reaching the controller,
        // so we only verify the status code, not ProblemDetail fields.
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenUnauthenticated() throws Exception {
        // Unauthenticated request: Spring Security's filter returns default 403
        // without ProblemDetail structure
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400ForMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.oms.com/errors/400"))
                .andExpect(jsonPath("$.title").value("Malformed Request Body"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400ForTypeMismatch() throws Exception {
        mockMvc.perform(get("/api/v1/products/invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.oms.com/errors/400"))
                .andExpect(jsonPath("$.title").value("Type Mismatch"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/v1/products/invalid-uuid"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
