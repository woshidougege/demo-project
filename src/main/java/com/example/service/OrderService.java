package com.example.service;

import com.example.model.Order;
import com.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 订单服务 - 处理订单相关的业务逻辑
 * 演示用代码，故意包含一些常见问题供 Claude 审查
 */
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final Map<Long, Order> orderDb = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final UserService userService;

    public OrderService(UserService userService) {
        this.userService = userService;
    }

    /**
     * 创建订单
     */
    public Order createOrder(Long userId, BigDecimal amount, String remark) {
        // BUG: 没有校验用户是否存在
        Order order = new Order();
        order.setId(idGenerator.getAndIncrement());
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setTotalAmount(amount);
        order.setRemark(remark);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        orderDb.put(order.getId(), order);
        log.info("创建订单: orderNo={}, userId={}, amount={}", order.getOrderNo(), userId, amount);
        return order;
    }

    /**
     * 查询用户的订单列表
     * 性能问题：遍历所有订单做筛选，应该用索引
     */
    public List<Order> findByUserId(Long userId) {
        return orderDb.values().stream()
                .filter(o -> o.getUserId().equals(userId))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 查询订单详情（含用户信息）
     */
    public Map<String, Object> getOrderDetail(Long orderId) {
        Order order = orderDb.get(orderId);
        if (order == null) {
            return null;
        }

        // BUG: 每次查详情都调一次 userService，N+1 问题
        User user = userService.findById(order.getUserId());

        Map<String, Object> detail = new HashMap<>();
        detail.put("order", order);
        detail.put("user", user);
        return detail;
    }

    /**
     * 支付订单
     */
    public Order payOrder(Long orderId) {
        Order order = orderDb.get(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在: " + orderId);
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许支付: " + order.getStatus());
        }
        order.setStatus("PAID");
        order.setUpdatedAt(LocalDateTime.now());
        log.info("订单已支付: orderId={}", orderId);
        return order;
    }

    /**
     * 取消订单
     */
    public Order cancelOrder(Long orderId, String reason) {
        Order order = orderDb.get(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在: " + orderId);
        }
        // BUG: 已支付的订单也能取消，应该校验状态
        order.setStatus("CANCELLED");
        order.setRemark(reason);
        order.setUpdatedAt(LocalDateTime.now());
        log.info("订单已取消: orderId={}, reason={}", orderId, reason);
        return order;
    }

    /**
     * 查询指定时间范围内的订单
     */
    public List<Order> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return orderDb.values().stream()
                .filter(o -> o.getCreatedAt().isAfter(start) && o.getCreatedAt().isBefore(end))
                .collect(Collectors.toList());
    }

    /**
     * 统计用户订单总额
     */
    public BigDecimal sumUserOrderAmount(Long userId) {
        return orderDb.values().stream()
                .filter(o -> o.getUserId().equals(userId))
                .filter(o -> !"CANCELLED".equals(o.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 查询订单统计信息
     */
    public Map<String, Object> getOrderStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", orderDb.size());

        Map<String, Long> statusCount = orderDb.values().stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        stats.put("byStatus", statusCount);

        BigDecimal totalAmount = orderDb.values().stream()
                .filter(o -> !"CANCELLED".equals(o.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalAmount", totalAmount);

        return stats;
    }

    /**
     * 批量发货
     */
    public int batchShip(List<Long> orderIds) {
        int count = 0;
        for (Long id : orderIds) {
            Order order = orderDb.get(id);
            if (order != null && "PAID".equals(order.getStatus())) {
                order.setStatus("SHIPPED");
                order.setUpdatedAt(LocalDateTime.now());
                count++;
            }
        }
        return count;
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", (int) (Math.random() * 10000));
    }
}
