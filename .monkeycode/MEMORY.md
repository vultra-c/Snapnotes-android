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

[Project Knowledge Summary]
- Date: 2026-08-28
- Context: 真机崩溃排查：一打开即 SIGSEGV（RenderThread stack overflow，512 帧全为 hwui RenderNode::prepareTreeImpl 递归）
- Category: Troubleshooting & Debugging
- Instructions:
  - 铁律：layerBackdrop 挂载节点（内容层）内部绝不允许任何组件 drawBackdrop 采样同一个 backdrop，否则 RenderNode 树成环，hwui prepareTree 无限递归直接 SIGSEGV
  - 本项目采用双玻璃源架构：tabsBackdrop（LayerBackdrop）挂内容层，仅供内容层外的 LiquidBottomTabs 采样滚动内容；cardBackdrop（rememberCanvasBackdrop 纯色画布）供内容层内全部玻璃组件采样，无 RenderNode 依赖故无环
  - 新增玻璃组件时：内容层内用 cardBackdrop，浮动控件（内容层外）才可用 tabsBackdrop；页面 Screen 组件的 backdrop 参数类型应为宽接口 com.kyant.backdrop.Backdrop（兼容 CanvasBackdrop），不要写死 LayerBackdrop
  - 参考架构：Cresto 列表行卡片一律纯色 background（列表挂 layerBackdrop 供外部玻璃控件采样）；上游 catalog 的列表内玻璃卡片则挂在独立叶子源（壁纸 Image）上
  - 页面骨架（Cresto 式固定 header）：Box { PageContent(layerBackdrop(pageBackdrop), topPadding = header 实测高度) { 列表 }; 固定 header/底部操作栏（PageContent 兄弟，采样 pageBackdrop 真玻璃） }；列表内容滚过固定 header 区域被磨砂模糊
  - tab 页玻璃源上报：LocalActivePageBackdrop（MutableState<LayerBackdrop?>）由 ManualTabVisibility 内 LocalTabVisible 控制仅可见 tab 上报；底部 LiquidBottomTabs 读它
  - sed 批量改 MainActivity 传参后必须核对括号配平（曾删行导致大括号失衡，onCreate 之后的成员函数全变 local function）
  - 按压反馈标准（用户验收过的规格）：pressEffect = 缩小 0.97 + 黑色压暗 0.08，spring(0.85f, 900f) 快速响应；放大+白闪（Plus 混合）在浅色卡片上不可见，用户反馈"没反应"，已废弃
  - 固定 header 磨砂与内容的柔和过渡：磨砂层独立 Box + graphicsLayer(CompositingStrategy.Offscreen) + drawWithContent 内 BlendMode.DstIn 垂直渐隐（0.7→1.0 渐变），标题文字放磨砂层外不受渐隐影响
  - 大标题收起：GlasenseHeroHeader(collapseProgress)，页面用 lazyListState.firstVisibleItemScrollOffset/360f 计算进度；列表内 items 必须给稳定 key（items(count, key = { it })）否则 animateItem 新增动画不生效
