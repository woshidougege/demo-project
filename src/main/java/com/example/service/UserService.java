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
    // 登录失败计数
    private final Map<String, Integer> loginFailCount = new ConcurrentHashMap<>();
    private static final int MAX_LOGIN_ATTEMPTS = 5;

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
        if (id == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (updateData == null) {
            throw new IllegalArgumentException("更新数据不能为空");
        }
        User existing = userDb.get(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在: id=" + id);
        }
        if (updateData.getUsername() != null) {
            existing.setUsername(updateData.getUsername());
        }
        if (updateData.getEmail() != null) {
            existing.setEmail(updateData.getEmail());
        }
        if (updateData.getPhone() != null) {
            existing.setPhone(updateData.getPhone());
        }
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
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String lowerKeyword = keyword.toLowerCase().trim();
        return userDb.values().stream()
                .filter(u -> u.getUsername().toLowerCase().contains(lowerKeyword))
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
     * 验证用户登录（含失败次数限制）
     */
    public User login(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        // 检查是否已被锁定
        Integer failCount = loginFailCount.getOrDefault(username, 0);
        if (failCount >= MAX_LOGIN_ATTEMPTS) {
            throw new RuntimeException("账号已锁定，请 30 分钟后再试");
        }
        for (User user : userDb.values()) {
            if (user.getUsername().equals(username)) {
                if (user.getPasswordHash().equals(password)) {
                    loginFailCount.remove(username);
                    return user;
                }
                loginFailCount.merge(username, 1, Integer::sum);
                throw new RuntimeException("密码错误，还剩 " + (MAX_LOGIN_ATTEMPTS - failCount - 1) + " 次机会");
            }
        }
        throw new RuntimeException("用户不存在: " + username);
    }

    private Long generateId() {
        return userDb.keySet().stream()
                .max(Long::compareTo)
                .orElse(0L) + 1;
    }
}

// DEMO: temporary change for @git diff demo
