---
name: review
description: 审查指定代码文件。用户说 /review 或要求代码审查时使用。
argument-hint: <file-path>
allowed-tools: [Read, Glob, Grep, Bash]
user-invocable: true
---

请审查 $ARGUMENTS，重点关注：
- 空指针风险
- SQL 注入
- 异常处理是否合理
- 是否有性能问题（N+1查询等）
输出格式：按严重程度排序，每项包含问题描述和修复建议
