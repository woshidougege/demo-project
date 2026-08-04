# 项目记忆

## 项目概述
Java Maven 演示项目，三层架构（Controller → Service → Model），JDK 11。

## 构建命令
```bash
mvn clean compile      # 编译
mvn test               # 跑测试
mvn package            # 打包
```

## 架构说明
- **model 层**: User（用户实体）、Order（订单实体）
- **service 层**: UserService（用户服务）、OrderService（订单服务），用 ConcurrentHashMap 模拟数据库
- **controller 层**: UserController，REST 风格接口
- **util 层**: StringUtils 字符串工具

## 代码规范
- 方法命名：驼峰
- 注释：中文
- 测试命名：`方法名_场景_期望结果`

## 注意事项
- 请用中文回答所有问题
- 测试使用 JUnit 5 + Mockito
- 代码里标了 `BUG:` 的地方是已知问题
