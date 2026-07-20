package com.example.controller;

import com.example.model.User;
import com.example.model.Order;
import com.example.service.UserService;
import com.example.service.OrderService;

import java.math.BigDecimal;
import java.util.*;

/**
 * 用户控制器 - 演示用的 REST 接口
 * 注意：这是演示代码，故意包含一些审查要点
 */
public class UserController {

    private final UserService userService;
    private final OrderService orderService;

    public UserController(UserService userService, OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }

    /**
     * GET /api/users
     * 查询用户列表，支持按角色过滤
     */
    public Map<String, Object> listUsers(String role) {
        List<User> users;
        if (role != null && !role.isEmpty()) {
            users = userService.findByRole(role);
        } else {
            users = userService.findAll();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", users);
        result.put("total", users.size());
        return result;
    }

    /**
     * GET /api/users/{id}
     * 查询单个用户详情
     */
    public Map<String, Object> getUser(Long id) {
        User user = userService.findById(id);
        Map<String, Object> result = new HashMap<>();
        if (user == null) {
            result.put("code", 404);
            result.put("message", "用户不存在");
        } else {
            result.put("code", 200);
            result.put("data", user);
        }
        return result;
    }

    /**
     * POST /api/users
     * 创建用户
     * BUG: 没有校验输入参数
     */
    public Map<String, Object> createUser(String username, String email, String phone, String role) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPhone(phone);
            user.setRole(role);
            User created = userService.create(user);
            result.put("code", 200);
            result.put("data", created);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * PUT /api/users/{id}
     * 更新用户
     */
    public Map<String, Object> updateUser(Long id, String username, String email, String phone) {
        Map<String, Object> result = new HashMap<>();
        User updateData = new User();
        updateData.setUsername(username);
        updateData.setEmail(email);
        updateData.setPhone(phone);
        // BUG: update 方法可能 NPE，这里没做异常处理
        User updated = userService.update(id, updateData);
        result.put("code", 200);
        result.put("data", updated);
        return result;
    }

    /**
     * DELETE /api/users/{id}
     * 删除用户
     */
    public Map<String, Object> deleteUser(Long id) {
        Map<String, Object> result = new HashMap<>();
        boolean success = userService.delete(id);
        result.put("code", success ? 200 : 404);
        result.put("message", success ? "删除成功" : "用户不存在");
        return result;
    }

    /**
     * GET /api/users/search?keyword=xxx
     * 搜索用户
     */
    public Map<String, Object> searchUsers(String keyword) {
        Map<String, Object> result = new HashMap<>();
        // BUG: keyword 为空时直接传给 service，会 NPE
        List<User> users = userService.search(keyword);
        result.put("code", 200);
        result.put("data", users);
        result.put("total", users.size());
        return result;
    }

    /**
     * POST /api/users/{id}/orders
     * 为用户创建订单
     */
    public Map<String, Object> createOrder(Long userId, BigDecimal amount, String remark) {
        Map<String, Object> result = new HashMap<>();
        // BUG: 没有校验用户是否存在就创建订单
        Order order = orderService.createOrder(userId, amount, remark);
        result.put("code", 200);
        result.put("data", order);
        return result;
    }

    /**
     * GET /api/users/{id}/orders
     * 查询用户的订单列表
     */
    public Map<String, Object> getUserOrders(Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<Order> orders = orderService.findByUserId(userId);
        result.put("code", 200);
        result.put("data", orders);
        result.put("total", orders.size());
        return result;
    }

    /**
     * POST /api/users/batch-update-role
     * 批量更新用户角色
     * BUG: 没有权限校验，任意用户都能调
     */
    public Map<String, Object> batchUpdateRole(List<Long> userIds, String newRole) {
        Map<String, Object> result = new HashMap<>();
        int count = userService.batchUpdateRole(userIds, newRole);
        result.put("code", 200);
        result.put("message", "更新了 " + count + " 个用户");
        return result;
    }

    /**
     * GET /api/users/stats
     * 用户统计
     */
    public Map<String, Object> getUserStats() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Long> stats = userService.countByRole();
        result.put("code", 200);
        result.put("data", stats);
        return result;
    }
}
