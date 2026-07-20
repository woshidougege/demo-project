package com.example.service;

import com.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 用户服务 - 处理用户相关的业务逻辑
 * 演示用代码，故意包含一些常见问题供 Claude 审查
 */
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    // 模拟数据库
    private final Map<Long, User> userDb = new ConcurrentHashMap<>();

    /**
     * 根据ID查询用户
     */
    public User findById(Long id) {
        return userDb.get(id);
    }

    /**
     * 查询所有用户
     */
    public List<User> findAll() {
        return new ArrayList<>(userDb.values());
    }

    /**
     * 按角色查询用户列表
     */
    public List<User> findByRole(String role) {
        List<User> result = new ArrayList<>();
        for (User user : userDb.values()) {
            if (user.getRole().equals(role)) {
                result.add(user);
            }
        }
        return result;
    }

    /**
     * 创建用户
     */
    public User create(User user) {
        // 检查用户名是否重复
        for (User existing : userDb.values()) {
            if (existing.getUsername().equals(user.getUsername())) {
                throw new RuntimeException("用户名已存在: " + user.getUsername());
            }
        }

        user.setId(generateId());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userDb.put(user.getId(), user);

        log.info("创建用户成功: id={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    /**
     * 更新用户信息
     */
    public User update(Long id, User updateData) {
        User existing = userDb.get(id);
        // BUG: 这里没有做 null 检查，NPE 风险
        existing.setUsername(updateData.getUsername());
        existing.setEmail(updateData.getEmail());
        existing.setPhone(updateData.getPhone());
        existing.setUpdatedAt(LocalDateTime.now());
        return existing;
    }

    /**
     * 删除用户（逻辑删除）
     */
    public boolean delete(Long id) {
        User user = userDb.get(id);
        if (user != null) {
            user.setActive(false);
            user.setUpdatedAt(LocalDateTime.now());
            return true;
        }
        return false;
    }

    /**
     * 搜索用户 - 按用户名模糊匹配
     */
    public List<User> search(String keyword) {
        // BUG: keyword 为 null 时会 NPE
        return userDb.values().stream()
                .filter(u -> u.getUsername().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * 批量更新用户角色
     */
    public int batchUpdateRole(List<Long> userIds, String newRole) {
        int count = 0;
        for (Long id : userIds) {
            User user = userDb.get(id);
            if (user != null) {
                user.setRole(newRole);
                user.setUpdatedAt(LocalDateTime.now());
                count++;
            }
        }
        return count;
    }

    /**
     * 统计各角色用户数量
     */
    public Map<String, Long> countByRole() {
        return userDb.values().stream()
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
    }

    /**
     * 验证用户登录
     * BUG: 密码比较应该用 BCrypt，这里直接用 equals
     */
    public User login(String username, String password) {
        for (User user : userDb.values()) {
            if (user.getUsername().equals(username)) {
                if (user.getPasswordHash().equals(password)) {
                    return user;
                }
                throw new RuntimeException("密码错误");
            }
        }
        return null; // 用户不存在时返回 null，调用方可能没处理
    }

    private Long generateId() {
        return userDb.keySet().stream()
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }
}
