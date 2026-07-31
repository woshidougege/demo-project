package com.example.service;

import com.example.model.User;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserService 单元测试
 * 覆盖正常、异常、边界场景，每个方法均以 方法名_场景_期望结果 命名
 */
class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        // 每个测试前重新创建 service，确保测试之间互不影响
        userService = new UserService();
    }

    // ==================== findById ====================

    @Test
    @DisplayName("findById - 用户存在 - 返回对应用户")
    void findById_用户存在_返回对应用户() {
        // 意图：验证能通过 ID 正确查询到已创建的用户
        User user = createAndSaveUser("alice", "alice@test.com", "13800000001", "USER");

        User found = userService.findById(user.getId());

        assertNotNull(found);
        assertEquals("alice", found.getUsername());
        assertEquals(user.getId(), found.getId());
    }

    @Test
    @DisplayName("findById - 用户不存在 - 返回 null")
    void findById_用户不存在_返回Null() {
        // 意图：查询不存在的 ID 应返回 null，而非抛异常
        User found = userService.findById(999L);

        assertNull(found);
    }

    @Test
    @DisplayName("findById - 数据库为空 - 返回 null")
    void findById_数据库为空_返回Null() {
        // 意图：空数据库场景下查询任意 ID 均应安全返回 null
        assertNull(userService.findById(1L));
    }

    // ==================== findAll ====================

    @Test
    @DisplayName("findAll - 有多个用户 - 返回全部用户列表")
    void findAll_有多个用户_返回全部用户列表() {
        // 意图：验证 findAll 返回所有已创建用户
        createAndSaveUser("alice", "alice@test.com", "13800000001", "USER");
        createAndSaveUser("bob", "bob@test.com", "13800000002", "ADMIN");

        List<User> all = userService.findAll();

        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("findAll - 无用户 - 返回空列表")
    void findAll_无用户_返回空列表() {
        // 意图：空数据库应返回空列表而非 null
        List<User> all = userService.findAll();

        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    // ==================== findByRole ====================

    @Test
    @DisplayName("findByRole - 存在该角色用户 - 返回匹配用户列表")
    void findByRole_存在该角色用户_返回匹配用户列表() {
        // 意图：按角色过滤应只返回指定角色的用户
        createAndSaveUser("alice", "alice@test.com", "13800000001", "USER");
        createAndSaveUser("bob", "bob@test.com", "13800000002", "ADMIN");
        createAndSaveUser("charlie", "charlie@test.com", "13800000003", "USER");

        List<User> users = userService.findByRole("USER");

        assertEquals(2, users.size());
        assertTrue(users.stream().allMatch(u -> "USER".equals(u.getRole())));
    }

    @Test
    @DisplayName("findByRole - 无匹配角色 - 返回空列表")
    void findByRole_无匹配角色_返回空列表() {
        // 意图：角色不存在时应返回空列表
        createAndSaveUser("alice", "alice@test.com", "13800000001", "USER");

        List<User> users = userService.findByRole("SUPERADMIN");

        assertTrue(users.isEmpty());
    }

    @Test
    @DisplayName("findByRole - 数据库为空 - 返回空列表")
    void findByRole_数据库为空_返回空列表() {
        // 意图：空数据库下按角色查询应安全返回空列表
        List<User> users = userService.findByRole("USER");

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    // ==================== create ====================

    @Test
    @DisplayName("create - 正常创建 - 用户被赋予 ID 和时间戳并存入数据库")
    void create_正常创建_用户被赋予ID和时间戳并存入数据库() {
        // 意图：验证创建流程自动分配 id、createdAt、updatedAt，并可被查询到
        User user = new User(null, "alice", "alice@test.com", "13800000001", "USER");

        User created = userService.create(user);

        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());
        assertEquals("alice", created.getUsername());
        // 确认可以通过 findById 查到
        assertEquals(created, userService.findById(created.getId()));
    }

    @Test
    @DisplayName("create - 连续创建多个 - ID 自增不重复")
    void create_连续创建多个_ID自增不重复() {
        // 意图：验证 generateId 逻辑能保证 ID 递增且唯一
        User user1 = userService.create(new User(null, "alice", "a@test.com", "13800000001", "USER"));
        User user2 = userService.create(new User(null, "bob", "b@test.com", "13800000002", "USER"));

        assertNotNull(user1.getId());
        assertNotNull(user2.getId());
        assertNotEquals(user1.getId(), user2.getId());
        assertTrue(user2.getId() > user1.getId());
    }

    @Test
    @DisplayName("create - 用户名重复 - 抛出 RuntimeException")
    void create_用户名重复_抛出RuntimeException() {
        // 意图：用户名唯一性校验，重复用户名应抛异常
        userService.create(new User(null, "alice", "a@test.com", "13800000001", "USER"));

        User duplicate = new User(null, "alice", "a2@test.com", "13800000002", "ADMIN");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.create(duplicate));
        assertTrue(ex.getMessage().contains("用户名已存在"));
    }

    @Test
    @DisplayName("create - 用户名相同大小写不同 - 视为不同用户（区分大小写）")
    void create_用户名大小写不同_视为不同用户() {
        // 意图：验证当前实现的用户名比较是区分大小写的
        userService.create(new User(null, "Alice", "a@test.com", "13800000001", "USER"));
        User user2 = new User(null, "alice", "a2@test.com", "13800000002", "USER");

        // 当前实现区分大小写，不应抛异常
        assertDoesNotThrow(() -> userService.create(user2));
        assertEquals(2, userService.findAll().size());
    }

    // ==================== update ====================

    @Test
    @DisplayName("update - 用户存在 - 成功更新用户名、邮箱、电话")
    void update_用户存在_成功更新用户名邮箱电话() {
        // 意图：验证更新操作能正确修改目标字段并刷新 updatedAt
        User user = createAndSaveUser("alice", "alice@test.com", "13800000001", "USER");

        User updateData = new User();
        updateData.setUsername("alice_updated");
        updateData.setEmail("new@test.com");
        updateData.setPhone("13900000001");

        User updated = userService.update(user.getId(), updateData);

        assertEquals("alice_updated", updated.getUsername());
        assertEquals("new@test.com", updated.getEmail());
        assertEquals("13900000001", updated.getPhone());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    @DisplayName("update - 用户不存在 - 抛出 NullPointerException（已知 BUG）")
    void update_用户不存在_抛出NPE() {
        // 意图：记录已知 BUG — update 方法缺少 null 检查，传入不存在的 ID 会 NPE
        User updateData = new User();
        updateData.setUsername("ghost");
        updateData.setEmail("ghost@test.com");
        updateData.setPhone("000");

        assertThrows(NullPointerException.class, () -> userService.update(999L, updateData));
    }

    @Test
    @DisplayName("update - 更新部分字段 - 其余字段保持不变")
    void update_更新部分字段_其余字段保持不变() {
        // 意图：update 方法只修改 username/email/phone，role 等字段应保持不变
        User user = createAndSaveUser("alice", "alice@test.com", "13800000001", "USER");

        User updateData = new User();
        updateData.setUsername("alice_new");
        updateData.setEmail(null);  // 传入 null
        updateData.setPhone(null);  // 传入 null

        User updated = userService.update(user.getId(), updateData);

        assertEquals("alice_new", updated.getUsername());
        // role 不受 update 影响
        assertEquals("USER", updated.getRole());
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete - 用户存在 - 逻辑删除成功返回 true")
    void delete_用户存在_逻辑删除成功返回True() {
        // 意图：验证逻辑删除将 active 设为 false 并更新 updatedAt
        User user = createAndSaveUser("alice", "alice@test.com", "13800000001", "USER");

        boolean result = userService.delete(user.getId());

        assertTrue(result);
        User deleted = userService.findById(user.getId());
        assertFalse(deleted.isActive());
        assertNotNull(deleted.getUpdatedAt());
    }

    @Test
    @DisplayName("delete - 用户不存在 - 返回 false")
    void delete_用户不存在_返回False() {
        // 意图：删除不存在的用户应返回 false，不抛异常
        boolean result = userService.delete(999L);

        assertFalse(result);
    }

    @Test
    @DisplayName("delete - 逻辑删除后用户仍可被 findById 查询到")
    void delete_逻辑删除后用户仍可被FindById查到() {
        // 意图：逻辑删除不等于物理删除，用户记录仍应存在于数据库中
        User user = createAndSaveUser("alice", "alice@test.com", "13800000001", "USER");

        userService.delete(user.getId());

        User stillThere = userService.findById(user.getId());
        assertNotNull(stillThere, "逻辑删除后用户应仍可查到");
        assertFalse(stillThere.isActive());
    }

    // ==================== search ====================

    @Test
    @DisplayName("search - 包含关键字的用户名 - 返回匹配列表")
    void search_包含关键字的用户名_返回匹配列表() {
        // 意图：模糊搜索能正确匹配用户名中包含关键字的用户
        createAndSaveUser("alice", "a@test.com", "13800000001", "USER");
        createAndSaveUser("bob_alice", "b@test.com", "13800000002", "USER");
        createAndSaveUser("charlie", "c@test.com", "13800000003", "USER");

        List<User> result = userService.search("alice");

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("search - 不区分大小写 - 大小写混合关键字也能匹配")
    void search_不区分大小写_大小写混合关键字也能匹配() {
        // 意图：search 方法使用 toLowerCase 进行比较，应不区分大小写
        createAndSaveUser("Alice", "a@test.com", "13800000001", "USER");

        List<User> result = userService.search("ALICE");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("search - 无匹配结果 - 返回空列表")
    void search_无匹配结果_返回空列表() {
        // 意图：没有匹配时应返回空列表
        createAndSaveUser("alice", "a@test.com", "13800000001", "USER");

        List<User> result = userService.search("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("search - 关键字为 null - 抛出 NullPointerException（已知 BUG）")
    void search_关键字为Null_抛出NPE() {
        // 意图：记录已知 BUG — search 方法缺少 null 检查，传入 null 会 NPE
        createAndSaveUser("alice", "a@test.com", "13800000001", "USER");

        assertThrows(NullPointerException.class, () -> userService.search(null));
    }

    @Test
    @DisplayName("search - 关键字为空字符串 - 返回所有用户")
    void search_关键字为空字符串_返回所有用户() {
        // 意图：空字符串是所有字符串的子串，应返回全部用户
        createAndSaveUser("alice", "a@test.com", "13800000001", "USER");
        createAndSaveUser("bob", "b@test.com", "13800000002", "USER");

        List<User> result = userService.search("");

        assertEquals(2, result.size());
    }

    // ==================== batchUpdateRole ====================

    @Test
    @DisplayName("batchUpdateRole - 批量更新多个存在的用户 - 返回更新成功数量")
    void batchUpdateRole_批量更新多个存在的用户_返回更新成功数量() {
        // 意图：批量更新应修改所有指定用户的角色并返回实际更新数量
        User alice = createAndSaveUser("alice", "a@test.com", "13800000001", "USER");
        User bob = createAndSaveUser("bob", "b@test.com", "13800000002", "USER");

        int count = userService.batchUpdateRole(Arrays.asList(alice.getId(), bob.getId()), "ADMIN");

        assertEquals(2, count);
        assertEquals("ADMIN", userService.findById(alice.getId()).getRole());
        assertEquals("ADMIN", userService.findById(bob.getId()).getRole());
    }

    @Test
    @DisplayName("batchUpdateRole - 部分用户不存在 - 只更新存在的用户")
    void batchUpdateRole_部分用户不存在_只更新存在的用户() {
        // 意图：混合存在和不存在的 ID 时，应跳过不存在的并返回实际更新数
        User alice = createAndSaveUser("alice", "a@test.com", "13800000001", "USER");

        int count = userService.batchUpdateRole(Arrays.asList(alice.getId(), 999L), "ADMIN");

        assertEquals(1, count);
        assertEquals("ADMIN", userService.findById(alice.getId()).getRole());
    }

    @Test
    @DisplayName("batchUpdateRole - 空 ID 列表 - 返回 0")
    void batchUpdateRole_空ID列表_返回0() {
        // 意图：空列表输入应安全处理，返回 0
        int count = userService.batchUpdateRole(Collections.emptyList(), "ADMIN");

        assertEquals(0, count);
    }

    // ==================== countByRole ====================

    @Test
    @DisplayName("countByRole - 多种角色 - 返回各角色正确数量")
    void countByRole_多种角色_返回各角色正确数量() {
        // 意图：验证按角色分组计数的正确性
        createAndSaveUser("alice", "a@test.com", "13800000001", "USER");
        createAndSaveUser("bob", "b@test.com", "13800000002", "ADMIN");
        createAndSaveUser("charlie", "c@test.com", "13800000003", "USER");
        createAndSaveUser("dave", "d@test.com", "13800000004", "GUEST");

        Map<String, Long> counts = userService.countByRole();

        assertEquals(2L, counts.get("USER"));
        assertEquals(1L, counts.get("ADMIN"));
        assertEquals(1L, counts.get("GUEST"));
    }

    @Test
    @DisplayName("countByRole - 无用户 - 返回空 Map")
    void countByRole_无用户_返回空Map() {
        // 意图：空数据库应返回空 Map
        Map<String, Long> counts = userService.countByRole();

        assertNotNull(counts);
        assertTrue(counts.isEmpty());
    }

    // ==================== login ====================

    @Test
    @DisplayName("login - 用户名和密码正确 - 返回用户对象")
    void login_用户名和密码正确_返回用户对象() {
        // 意图：正确的用户名和密码应成功登录并返回用户
        User user = new User(null, "alice", "a@test.com", "13800000001", "USER");
        user.setPasswordHash("correct_password");
        userService.create(user);

        User result = userService.login("alice", "correct_password");

        assertNotNull(result);
        assertEquals("alice", result.getUsername());
    }

    @Test
    @DisplayName("login - 密码错误 - 抛出 RuntimeException")
    void login_密码错误_抛出RuntimeException() {
        // 意图：密码不匹配时应抛出"密码错误"异常
        User user = new User(null, "alice", "a@test.com", "13800000001", "USER");
        user.setPasswordHash("correct_password");
        userService.create(user);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login("alice", "wrong_password"));
        assertTrue(ex.getMessage().contains("密码错误"));
    }

    @Test
    @DisplayName("login - 用户不存在 - 返回 null（已知设计缺陷）")
    void login_用户不存在_返回Null() {
        // 意图：记录已知设计缺陷 — 用户不存在时返回 null 而非抛异常，
        //       调用方如果不做 null 检查可能导致后续 NPE
        User result = userService.login("ghost", "any_password");

        assertNull(result);
    }

    @Test
    @DisplayName("login - 用户名为 null - 抛出 NullPointerException")
    void login_用户名为Null_抛出NPE() {
        // 意图：null 用户名会导致 equals 比较时 NPE（当前实现没有 null 保护）
        createAndSaveUserWithPassword("alice", "pass123");

        assertThrows(NullPointerException.class, () -> userService.login(null, "pass123"));
    }

    @Test
    @DisplayName("login - 密码为 null 且用户存在 - 抛出 NullPointerException")
    void login_密码为Null且用户存在_抛出NPE() {
        // 意图：密码为 null 时，String.equals(null) 返回 false 不会 NPE，
        //       但 passwordHash.equals(null) 也不会 NPE，所以会抛"密码错误"
        //       然而如果 passwordHash 本身为 null，则会 NPE
        User user = new User(null, "alice", "a@test.com", "13800000001", "USER");
        user.setPasswordHash(null);
        userService.create(user);

        // passwordHash 为 null，调用 equals 会 NPE
        assertThrows(NullPointerException.class, () -> userService.login("alice", "any"));
    }

    // ==================== 边界 & 综合场景 ====================

    @Test
    @DisplayName("create 后 findById - 新创建的用户可被立即查询到")
    void create后FindById_新创建的用户可被立即查询到() {
        // 意图：验证 create 操作的写入能立即对读操作可见（无异步延迟）
        User created = userService.create(new User(null, "alice", "a@test.com", "13800000001", "USER"));

        User found = userService.findById(created.getId());
        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
    }

    @Test
    @DisplayName("delete 后 findAll - 逻辑删除的用户仍出现在列表中")
    void delete后FindAll_逻辑删除的用户仍出现在列表中() {
        // 意图：逻辑删除不影响 findAll 的返回结果，用户记录仍然存在
        User user = createAndSaveUser("alice", "a@test.com", "13800000001", "USER");

        userService.delete(user.getId());

        List<User> all = userService.findAll();
        assertEquals(1, all.size());
        assertFalse(all.get(0).isActive());
    }

    @Test
    @DisplayName("generateId - 大量用户创建后 ID 不溢出不重复")
    void generateId_大量用户创建后ID不溢出不重复() {
        // 意图：压力测试 — 连续创建100个用户，验证 ID 始终唯一递增
        for (int i = 0; i < 100; i++) {
            userService.create(new User(null, "user" + i, "u" + i + "@test.com", "13800000000", "USER"));
        }

        List<User> all = userService.findAll();
        assertEquals(100, all.size());

        // 验证所有 ID 唯一
        long uniqueIds = all.stream().map(User::getId).distinct().count();
        assertEquals(100, uniqueIds);
    }

    // ==================== 辅助方法 ====================

    /**
     * 快捷创建用户并存入数据库
     */
    private User createAndSaveUser(String username, String email, String phone, String role) {
        return userService.create(new User(null, username, email, phone, role));
    }

    /**
     * 创建带密码的用户并存入数据库
     */
    private User createAndSaveUserWithPassword(String username, String password) {
        User user = new User(null, username, username + "@test.com", "13800000001", "USER");
        user.setPasswordHash(password);
        return userService.create(user);
    }
}
