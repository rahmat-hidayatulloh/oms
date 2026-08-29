package com.oms.admin.service;

import com.oms.audit.annotation.Auditable;
import com.oms.order.repository.OrderRepository;
import com.oms.product.repository.ProductRepository;
import com.oms.auth.repository.UserRepository;
import com.oms.admin.DashboardConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service for admin dashboard statistics.
 * Fetches actual data from the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService implements AdminDashboardServiceInterface {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Override
    @Auditable(action = "READ", entityType = "Dashboard")
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        BigDecimal revenue = orderRepository.calculateTotalRevenue();

        log.debug("Dashboard stats: users={}, products={}, orders={}, revenue={}",
                totalUsers, totalProducts, totalOrders, revenue);

        return Map.of(
                DashboardConstants.TOTAL_USERS, totalUsers,
                DashboardConstants.TOTAL_PRODUCTS, totalProducts,
                DashboardConstants.TOTAL_ORDERS, totalOrders,
                DashboardConstants.REVENUE, revenue
        );
    }
}
