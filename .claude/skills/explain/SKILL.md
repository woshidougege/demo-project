---
name: explain
description: 用中文解释指定代码。用户说 /explain 或要求解释代码时使用。
argument-hint: <file-path>
allowed-tools: [Read, Glob, Grep]
user-invocable: true
---

请解释 $ARGUMENTS 的代码逻辑：
- 整体功能是什么
- 核心流程走一遍
- 有哪些潜在风险
- 用中文，通俗易懂
