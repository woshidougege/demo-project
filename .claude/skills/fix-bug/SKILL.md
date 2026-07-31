---
name: fix-bug
description: 分析并修复指定代码中的 bug。用户说 /fix-bug 或要求修 bug 时使用。
argument-hint: <file-path>
allowed-tools: [Read, Glob, Grep, Write, Edit, Bash]
user-invocable: true
---

请修复 $ARGUMENTS 中的 bug：
1. 先读相关代码理解上下文
2. 分析可能的原因
3. 修复并解释改了什么
4. 写对应的回归测试
