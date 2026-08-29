package com.oms.api;

import com.oms.admin.controller.AdminController;
import com.oms.auth.controller.AuthController;
import com.oms.system.CsrfController;
import com.oms.order.controller.OrderController;
import com.oms.product.controller.ProductController;
import com.oms.system.VersionController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test that validates controller mappings against the documented API.
 * Prevents drift between the actual controller endpoints and the API
 * documentation in {@code docs/current/api/api-index.md}.
 */
class ApiContractTest {

    private static final Set<String> EXPECTED_ENDPOINTS = Set.of(
            "POST /api/v1/auth/register",
            "POST /api/v1/auth/login",
            "POST /api/v1/auth/refresh",
            "POST /api/v1/auth/logout",
            "GET /api/v1/auth/csrf",
            "POST /api/v1/orders",
            "GET /api/v1/orders",
            "GET /api/v1/orders/{id}",
            "DELETE /api/v1/orders/{id}",
            "POST /api/v1/products",
            "GET /api/v1/products",
            "GET /api/v1/products/{id}",
            "GET /api/v1/products/search",
            "PUT /api/v1/products/{id}",
            "DELETE /api/v1/products/{id}",
            "GET /api/v1/admin/dashboard",
            "GET /api/v1/admin/users",
            "GET /api/version"
    );

    private static final Class<?>[] SCANNED_CONTROLLERS = {
            AuthController.class, OrderController.class,
            ProductController.class, AdminController.class,
            VersionController.class, CsrfController.class
    };

    @Test
    void documentedEndpointsShouldExistInControllers() {
        Set<String> actual = collectEndpoints(SCANNED_CONTROLLERS);
        for (String expected : EXPECTED_ENDPOINTS) {
            assertThat(actual)
                    .as("Endpoint %s must exist", expected)
                    .contains(expected);
        }
    }

    @Test
    void controllerEndpointsShouldBeDocumented() {
        Set<String> actual = collectEndpoints(SCANNED_CONTROLLERS);
        Set<String> undocumented = new HashSet<>(actual);
        undocumented.removeAll(EXPECTED_ENDPOINTS);
        assertThat(undocumented)
                .as("Undocumented endpoints: %s", undocumented)
                .isEmpty();
    }

    @Test
    void allMainControllersShouldHaveRateLimitAnnotation() {
        assertThat(AuthController.class.getAnnotation(
                com.oms.ratelimit.RateLimit.class)).isNotNull();
        assertThat(OrderController.class.getAnnotation(
                com.oms.ratelimit.RateLimit.class)).isNotNull();
        assertThat(ProductController.class.getAnnotation(
                com.oms.ratelimit.RateLimit.class)).isNotNull();
        assertThat(AdminController.class.getAnnotation(
                com.oms.ratelimit.RateLimit.class)).isNotNull();
    }

    @Test
    void apiControllersShouldUseV1Versioning() {
        for (Class<?> ctrl : new Class<?>[]{AuthController.class,
                OrderController.class, ProductController.class,
                AdminController.class}) {
            RequestMapping rm = ctrl.getAnnotation(RequestMapping.class);
            assertThat(rm).isNotNull();
            boolean hasV1 = false;
            for (String p : rm.value()) {
                if (p.startsWith("/api/v1")) {
                    hasV1 = true;
                    break;
                }
            }
            assertThat(hasV1).as("%s uses /api/v1", ctrl.getSimpleName()).isTrue();
        }
    }

    private Set<String> collectEndpoints(Class<?>... classes) {
        Set<String> result = new HashSet<>();
        for (Class<?> clazz : classes) {
            result.addAll(extractEndpoints(clazz));
        }
        return result;
    }

    private List<String> extractEndpoints(Class<?> clazz) {
        RequestMapping cm = clazz.getAnnotation(RequestMapping.class);
        String prefix = "";
        if (cm != null && cm.value().length > 0) {
            prefix = cm.value()[0];
        }
        List<String> endpoints = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            String http = httpMethod(m);
            if (http != null) {
                endpoints.add(http + " " + prefix + path(m));
            }
        }
        return endpoints;
    }

    private String httpMethod(Method m) {
        if (m.isAnnotationPresent(GetMapping.class)) return "GET";
        if (m.isAnnotationPresent(PostMapping.class)) return "POST";
        if (m.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (m.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        return null;
    }

    private String path(Method m) {
        if (m.isAnnotationPresent(GetMapping.class))
            return val(m.getAnnotation(GetMapping.class).value());
        if (m.isAnnotationPresent(PostMapping.class))
            return val(m.getAnnotation(PostMapping.class).value());
        if (m.isAnnotationPresent(PutMapping.class))
            return val(m.getAnnotation(PutMapping.class).value());
        if (m.isAnnotationPresent(DeleteMapping.class))
            return val(m.getAnnotation(DeleteMapping.class).value());
        return "";
    }

    private String val(String[] v) {
        return (v.length == 0 || v[0].isEmpty()) ? "" : v[0];
    }
}
