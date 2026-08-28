# User Instruction Memory

This file records user instructions, preferences, and teachings for reference in future interactions.

## Format

### User Instruction Entry
User instruction entries should follow this format:

[User Instruction Summary]
- Date: [YYYY-MM-DD]
- Context: [Mentioned scenario or time]
- Instructions:
  - [Content of user teaching or instruction, described line by line]

### Project Knowledge Entry
Entries discovered by the Agent during task execution should follow this format:

[Project Knowledge Summary]
- Date: [YYYY-MM-DD]
- Context: Discovered by Agent while performing [specific task description]
- Category: [Operations & Deployment|Build Methods|Testing Methods|Troubleshooting & Debugging|Workflow & Collaboration|Environment Configuration]
- Instructions:
  - [Specific knowledge points, described line by line]

## Deduplication Strategy
- Before adding a new entry, check for similar or identical instructions.
- If a duplicate is found, skip the new entry or merge it with the existing one.
- When merging, update the context or date information.

## Entries

[Project Knowledge Summary]
- Date: 2026-08-28
- Context: Snapnotes-android 的 UI 迁移任务（MIUI → Glasense 液态玻璃）
- Category: Workflow & Collaboration
- Instructions:
  - 验证方式一律用推送触发 GitHub Actions CI，禁止本地 gradle 构建；用户明确要求过
  - CI 循环模式：push 后 `gh run list/watch/view --log-failed --repo vultra-c/Snapnotes-android` 提取 `e: file:` 行修错
  - GH_TOKEN 从 `git remote get-url origin` 用 sed 提取嵌入的 token，绝不展示 token 内容

[Project Knowledge Summary]
- Date: 2026-08-28
- Context: 从 AndroidLiquidGlass catalog 移植组件 + 编写 Glasense 玻璃组件时反复遇到的编译错误
- Category: Troubleshooting & Debugging
- Instructions:
  - backdrop 库 DrawScope lambda（effects/onDrawSurface）内禁止调用 @Composable getter（AppColors.* / GlasenseTheme.colors.*）；必须先在 Composable 作用域提取局部 val
  - drawBackdrop 的 effects 块里用 `effect(it)` 需显式 import `com.kyant.backdrop.effects.effect`
  - 上游 AndroidLiquidGlass 的 `awaitFrame()` 是 expect/actual；Android 端实现为 `kotlinx.coroutines.android.awaitFrame`（注意是 android 子包），移植时直接内联该实现
  - Box 的 content 参数类型是 @Composable BoxScope.() -> Unit；传入 () -> Unit 形参时用 trailing lambda 包一层 `{ content() }`
