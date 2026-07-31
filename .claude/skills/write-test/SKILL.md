---
name: write-test
description: 为指定 Java 文件编写单元测试。用户说 /write-test 或要求写单元测试时使用。
argument-hint: <java-file-path>
allowed-tools: [Read, Glob, Grep, Write, Edit, Bash]
user-invocable: true
---

请为 $ARGUMENTS 编写单元测试，要求：
- 覆盖正常、异常、边界 case
- 使用 Mockito mock 外部依赖
- 测试命名：方法名_场景_期望结果
- 每个测试方法加中文注释说明意图
- 先读源文件理解逻辑，再写测试
