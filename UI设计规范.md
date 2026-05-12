# 灵感捕手 — UI 设计规范

## 1. 设计语言

**主题名称**: Warm Paper (暖纸)
**设计理念**: 温暖、呼吸感、类纸张质感，区别于传统 IDE 冷色风格
**基础框架**: AtlantaFX PrimerLight

### 配色系统

#### 主色 (Brand)
| Token | 色值 | 用途 |
|-------|------|------|
| `-fx-primary` | `#C4843C` | 主色 — 按钮/选中态/链接/强调 |
| `-fx-primary-dark` | `#A86828` | 主色悬停态 |
| `-fx-primary-light` | `#D4A76A` | 主色浅色变体 |
| `-fx-primary-bg` | `#F5ECD8` | 主色背景 — 选中行/标签底色/AI 卡片 |

#### 语义色 (Semantic)
| Token | 色值 | 用途 |
|-------|------|------|
| `-fx-green` | `#5B8C5A` | 成功/绿色标签 |
| `-fx-green-bg` | `#EAF2EA` | 绿色背景 |
| `-fx-red` | `#C45656` | 危险/删除 |
| `-fx-red-bg` | `#F5E8E8` | 红色背景 |
| `-fx-blue` | `#5B7FAF` | 信息/链接 |
| `-fx-blue-bg` | `#E8EDF3` | 蓝色背景 |
| `-fx-orange` | `#C4843C` | 警告 |
| `-fx-orange-bg` | `#F5ECD8` | 警告背景 |
| `-fx-purple` | `#8B6FAF` | 点缀 |
| `-fx-purple-bg` | `#EDE8F3` | 紫色背景 |

#### 中性色 (Neutral)
| Token | 色值 | 用途 |
|-------|------|------|
| `-fx-bg` | `#F7F4F0` | 主背景 — 暖米白 |
| `-fx-bg-subtle` | `#F0ECE6` | 次要背景 — 侧边栏/状态栏 |
| `-fx-bg-hover` | `#EBE6DE` | 悬停态背景 |
| `-fx-bg-active` | `#E2DDD4` | 点击态背景 |
| `-fx-bg-raised` | `#FFFFFF` | 凸起表面 — 卡片/对话框 |
| `-fx-bg-inset` | `#EEEAE4` | 内嵌表面 — 输入框/搜索栏 |

#### 文字色 (Text)
| Token | 色值 | 对比度 | 用途 |
|-------|------|--------|------|
| `-fx-text-primary` | `#2C2924` | 最高 | 正文/标题 |
| `-fx-text-secondary` | `#7A746E` | 中 | 次要信息 |
| `-fx-text-tertiary` | `#ADA7A0` | 低 | 占位符/提示 |
| `-fx-text-inverse` | `#FFFFFF` | — | 主色按钮上的文字 |

#### 边框色 (Border)
| Token | 色值 | 用途 |
|-------|------|------|
| `-fx-border-subtle` | `#E2DDD4` | 默认边框 — 卡片/输入框/分隔线 |
| `-fx-border-default` | `#CDC7BE` | 悬停态边框 |
| `-fx-border-focus` | `#C4843C` | 焦点态边框 |

#### 阴影
| Token | 定义 | 用途 |
|-------|------|------|
| `-fx-shadow-xs` | 2px blur, 4% opacity | 卡片默认 |
| `-fx-shadow-sm` | 4px blur, 6% opacity | 卡片悬停 |
| `-fx-shadow-md` | 8px blur, 8% opacity | 下拉菜单 |
| `-fx-shadow-lg` | 16px blur, 10% opacity | 模态对话框 |
| `-fx-shadow-glow` | 12px blur, 12% opacity amber | 输入框焦点发光 |

#### 圆角
| Token | 值 | 用途 |
|-------|-----|------|
| `-fx-radius-xs` | 4px | 小元素 |
| `-fx-radius-sm` | 6px | 按钮/输入框/卡片 |
| `-fx-radius-md` | 8px | 大卡片/面板 |
| `-fx-radius-lg` | 12px | 弹窗 |
| `-fx-radius-full` | 999px | 胶囊标签 |

---

## 2. 布局结构

### 整体框架

```
┌──────────────────────────────────────────────────────────────┐
│ [Nav 48px] | [Sidebar 260px] | [Workspace flex]             │
│            |                   |                             │
│  图标导航    | 项目选择器        | TabPane (填充剩余宽度)      │
│            | 搜索框            |   Tab1: 灵感列表            │
│            | 过滤器(TitledPane)|   Tab2: 思维导图            │
│            | 标签云            |   Tab3: 编辑器              │
│            | 统计信息          |                             │
├────────────┴──────────────────┴─────────────────────────────┤
│ Status Bar: 状态信息 · 最活跃标签                            │
└──────────────────────────────────────────────────────────────┘
```

### Navigation Rail (48px)
- 宽度固定 48px，不允许收缩
- 背景色 `#EBE6DE`
- 右侧 1px 分隔线 `#E2DDD4`
- 顶部 brand logo: 32x32 圆角 8px 的主色方块，白色 zap 图标
- 中部导航按钮: 48x44px，hover 时 6% 黑色透明背景
- 选中态: 左侧 2px `#C4843C` 边框 + 10% 主色背景
- 底部: 搜索(放大镜) + 设置(齿轮) 图标按钮

### Sidebar (260px, 可调范围 200-360px)
- 背景色 `#F0ECE6`
- 右侧 1px 分隔线 `#E2DDD4`
- **项目选择器**: ComboBox，140px 宽度
- **新建项目按钮**: 图标按钮 28x28
- **快速捕捉按钮**: 主色按钮 32x32，白色 + 图标
- **搜索框**: 内嵌背景 `#EEEAE4`，1px 边框，聚焦时主色边框 + glow
- **过滤器**: TitledPane 折叠面板，expanded=false，间距 6px
- **标签云**: FlowPane，hgap=5, vgap=5，高度最大 80px
- **统计**: 侧边栏底部，两行小字

### Workspace (填充剩余)
- 背景色 `#F7F4F0`
- **工具栏**: 36px 高度，底部 1px 分隔线
- **TabPane**: tab 高度 36px，underline 选中样式

### Status Bar (24px)
- 背景色 `#F0ECE6`
- 顶部 1px 分隔线
- 11px 三级文字色

---

## 3. Tab: 灵感列表

### 布局
```
┌─────────────────────────────────────────────────┐
│ "灵感列表" | 刷新 | 新建 | 编辑 | 删除 | 导出按钮 |
├─────────────────────────────────────────────────┤
│ TableView 填充剩余空间                           │
│ ┌──────┬────┬────┬────┬────┬────┬────┐         │
│ │ 标题 │ 类型│重要性│心情│标签 │创建 │更新│         │
│ ├──────┼────┼────┼────┼────┼────┼────┤         │
│ │ ...  │    │ ⭐  │    │    │    │    │         │
│ └──────┴────┴────┴────┴────┴────┴────┘         │
├─────────────────────────────────────────────────┤
│ [详情面板] (选择行时展开)                        │
└─────────────────────────────────────────────────┘
```

### TableView 规格
- 行高: 40px
- 行之间 1px `#E2DDD4` 分隔线
- 行悬停: 背景 `#F0ECE6`
- 行选中: 背景 `#F5ECD8`，文字 `#A86828` 加粗
- 列标题: 12px，加粗 700，`#7A746E`，下划线

### 星级 (重要性列)
- 使用 Ikonli `FontAwesomeSolid.STAR` 图标
- 填充星: `#E8A838` (亮金)，14px
- 空星: `#C8C0B8` (浅灰)，14px
- HBox 容器，间距 2px

### 空状态
- 占位符居中显示
- "还没有灵感" 18px 加粗，二级文字色
- 快捷键提示 13px，三级文字色

---

## 4. Tab: 思维导图

### 布局
```
┌──────────────┬──────────────────────────────────┐
│ 灵感卡片面板  │ 思维导图画布区域                  │
│ 240px        │ 工具栏: 添加灵感 | 自动布局 | 导出 │
│              ├──────────────────────────────────┤
│ 白色卡片列表  │ Canvas + Group 节点渲染          │
│ 可拖拽到画布  │                                  │
│              │                                  │
│ [添加概念]    │ 底部提示: "拖拽左侧卡片到画布..." │
│ [添加链接]    │                                  │
└──────────────┴──────────────────────────────────┘
```

### 左侧卡片面板
- 背景 `#FFFFFF`，右侧 1px 分隔线
- 间距 12px，内部 padding 12px
- 灵感卡片: 白色背景，1px `#E2DDD4` 边框，圆角 6px
- 卡片 hover: 背景 `#EBE6DE`，边框 `#CDC7BE`
- 卡片标题: 14px 加粗，`#C4843C`
- 卡片摘要: 12px，`#7A746E`，超过 80 字截断

### 右侧画布区域
- 背景 `#F0ECE6` (canvas 外围)
- Canvas 背景 `#FFFFFF`
- 底部操作提示栏: 11px 三级文字色
- "拖拽左侧卡片到画布添加节点 · 双击编辑 · 滚轮缩放"

---

## 5. Tab: 编辑器

### 布局
```
┌──────────────────────────────────────────────────┐
│ 标题输入框                    | 保存 | AI 扩展    │
│ 类型 | 重要性 | 心情 | 标签输入                  │
├──────────────────────┬───────────────────────────┤
│ Markdown 工具栏      │ 预览标题                  │
│ B I | H1 H2 | { • >  │                           │
├──────────────────────┤ WebView 预览              │
│ TextArea 编辑区      │                           │
│ (填充剩余)           │ (填充剩余)                │
├──────────────────────┴───────────────────────────┤
│ ▸ AI 建议 (折叠)                                 │
│ ▸ 相关灵感 (折叠)                                │
└──────────────────────────────────────────────────┘
```

### SplitPane 规格
- 初始比例 55%/45% (左/右)
- 分隔条 1px `#E2DDD4`，hover 变 `#D4A76A`

### Markdown 工具栏
- 间距 3px，padding 6px 0
- 按钮: 背景透明，圆角 4px，28x26px
- hover: 背景 `#EBE6DE`，文字 `#2C2924`

### 底部面板
- AI 建议: TitledPane 折叠，默认折叠
- TextArea 背景 `#F5ECD8`，圆角 8px，padding 10px
- 相关灵感: TitledPane 折叠，ListView 高度 150px max

---

## 6. 设置对话框

### 规格
- 标题: "灵感捕手 — 偏好设置"
- 尺寸: 520x400
- TabPane 无关闭按钮，3 个 Tab

### Tab 1: AI 设置
- API 密钥: PasswordField, 300px
- API 地址: TextField, 300px
- 模型: ComboBox 可编辑 (deepseek-chat/reasoner/gpt-4o/4o-mini)
- 最大 Token: Spinner 256-8192, step 256
- 温度(T): Slider 0-2, step 0.1, 带刻度
- 离线备用: CheckBox

### Tab 2: 通用
- 占位文字，说明字体调节方式

### Tab 3: 关于
- 标题 22px 加粗主色
- 副标题 14px 二级文字色
- 分隔线
- 版本号/技术栈/作者
- 分隔线
- 项目描述 (12px)
- 代码统计 (11px)

---

## 7. 快速捕捉对话框

### 规格
- 模态 Stage
- 背景 `-fx-bg`，padding 16px
- 标题 "快速捕捉灵感" 18px 加粗主色
- 类型/重要性/心情 行: HBox 间距 10px
- 标题输入: TextField, 14px
- 内容输入: TextArea, 14px, 160px 高度
- 标签输入: TextField + "添加"主色按钮
- 标签容器: FlowPane 显示已添加标签
- 操作: "取消"(ghost) + "保存"(success)

---

## 8. UI 组件详解

### 按钮状态
| 状态 | 默认按钮 | 主色按钮 | Ghost 按钮 | Danger 按钮 |
|------|---------|---------|-----------|------------|
| 默认 | bg `#F7F4F0`, 1px `#E2DDD4` | bg `#C4843C`, 白字 | 透明 | bg `#C45656`, 白字 |
| Hover | bg `#EBE6DE`, border `#CDC7BE` | bg `#A86828` | bg `#EBE6DE` | bg `#B04848` |
| Pressed | bg `#E2DDD4` | bg `#8B5828` | bg `#E2DDD4` | — |

### 输入框状态
| 状态 | TextField/TextArea |
|------|-------------------|
| 默认 | bg `#EEEAE4`, 1px `#E2DDD4`, 圆角 6px |
| Focus | border `#C4843C`, glow 阴影 |
| Placeholder | 颜色 `#ADA7A0` |

### Tab 样式
| 状态 | 样式 |
|------|------|
| 未选中 | 文字 `#7A746E`, 12px, 500 weight |
| 选中 | 文字 `#C4843C`, 600 weight, 底部 2px 主色边框 |
| Hover | 背景 `#EBE6DE` |

### TableView
| 元素 | 样式 |
|------|------|
| 列头 | 背景透明，11px 700 weight，`#ADA7A0`，下划线 |
| 普通行 | 背景透明，文字 `#2C2924`，13px |
| 悬停行 | 背景 `#F0ECE6` |
| 选中行 | 背景 `#F5ECD8`，文字 `#A86828` 加粗 |
| 行分隔 | 底部 1px `#E2DDD4` |

### Scrollbar
| 元素 | 样式 |
|------|------|
| Track | 透明 |
| Thumb | 8px 圆角，`#CDC7BE` |
| Thumb hover | `#ADA7A0` |
| 按钮 | 隐藏 (0x0) |

### 星级可视化
| 元素 | 样式 |
|------|------|
| 填充星 | Ikonli STAR, `#E8A838`, 14px |
| 空星 | Ikonli STAR, `#C8C0B8`, 14px |
| 容器 | HBox, spacing 2 |

### 标签 (Tag)
| 元素 | 样式 |
|------|------|
| 背景 | `-fx-primary-bg` (`#F5ECD8`) |
| 文字 | `#C4843C`, 11px, 500 weight |
| 圆角 | 4px |
| 填充 | 2px 6px |
| Hover | opacity 0.8 |
| 标签云中 | 圆角 10px, 白字, 颜色取 tag.color |

---

## 9. 字体系统

### 字族
- 界面: `"Microsoft YaHei", "PingFang SC", "Noto Sans SC", system-ui, sans-serif`
- 等宽: `"JetBrains Mono", "SF Mono", "Consolas", monospace`

### 字号层级
| Token | 值 | 用途 |
|-------|-----|------|
| `-fx-font-size-xs` | 11px | 状态栏/辅助文字 |
| `-fx-font-size-sm` | 12px | 按钮/标签/Tab 标题 |
| `-fx-font-size-base` | 13px | 正文/表格 |
| `-fx-font-size-lg` | 15px | 大号文字 |
| `-fx-font-size-xl` | 16px | 小标题 |
| `-fx-font-size-2xl` | 18px | 中标题 |
| `-fx-font-size-3xl` | 22px | 大标题 |

### 动态缩放
- 通过 `FontManager` 控制 root 的 `-fx-font-size` 属性
- 所有组件继承 root 字号
- 可选值: SMALL(12), MEDIUM(14), LARGE(16), XLARGE(18), XXLARGE(20)
- 通过工具栏 A− / A / A+ 按钮调节

---

## 10. 间距系统

基于 8px 网格：

| Token | 值 |
|-------|-----|
| `-fx-space-1` | 4px |
| `-fx-space-2` | 8px |
| `-fx-space-3` | 12px |
| `-fx-space-4` | 16px |
| `-fx-space-5` | 20px |
| `-fx-space-6` | 24px |
| `-fx-space-8` | 32px |
| `-fx-space-10` | 40px |
| `-fx-space-12` | 48px |

Utility CSS 类: `.p-*` (padding), `.gap-*` (spacing)

---

## 11. 图标系统

- 图标库: Ikonli FontAwesome 5 Solid + Feather
- Feather 图标用于 Navigation Rail (fth- 前缀)
- FontAwesome 用于思维导图卡片类型/心情图标
- 字体调节: `FontIcon` 组件，`setIconSize()` + `setIconColor()`

### 导航图标
| 按钮 | 图标 | 含义 |
|------|------|------|
| 顶部 Home | `fth-home` | 灵感列表 |
| 中部 1 | `fth-share-2` | 思维导图 |
| 中部 2 | `fth-edit-3` | 编辑器 |
| 底部 1 | `fth-search` | 搜索 |
| 底部 2 | `fth-settings` | 设置 |
| Brand | `fth-zap` | 应用品牌 |

---

## 12. 禁用/空状态

### 空状态 (Empty State)
- 居中显示
- 大图标或大标题: 18px 加粗 `#7A746E`
- 描述文字: 13px `#ADA7A0`
- 间距 12px
- 用于: 灵感列表无数据、思维导图无节点、标签云无标签

### 禁用按钮
- opacity 0.5
- cursor 保持默认 (非 hand)

---

## 13. 动画与过渡

- 输入框聚焦: 0.15s 边框颜色切换 + glow 阴影
- 按钮 hover: 0.1s 背景色切换
- Tab 切换: 无动画 (JavaFX 默认)
- 思维导图缩放: 即时重绘
- 暂无 skeleton loading 或 progress indicator

---

## 14. Markdown 预览样式 (WebView 内)

- 背景: `#FFFFFF`
- 正文: `#2C2924`, 16px, line-height 1.6
- 标题: `#C4843C` (h1/h2), h3 恢复正文色
- 标题下边框: `#E2DDD4`
- 引用: 左侧 4px `#E2DDD4` 边框, 文字 `#7A746E`
- 代码块: 背景 `#F7F4F0`, 1px `#E2DDD4` 边框, 圆角 6px
- 行内代码: 背景 rgba(196,132,60,0.08), 文字 `#A86828`
- 链接: `#C4843C`
- 表格: 边框 `#E2DDD4`, 表头背景 `#F0ECE6`, 主色文字
- 分隔线: 1px `#E2DDD4`

---

## 15. CSS 文件结构

单文件 `app.css` (641 行)，按章节组织：

| 章节 | 内容 |
|------|------|
| 1 | Design Tokens (CSS 变量) |
| 2 | Base Reset |
| 3 | Scrollbar |
| 4 | Navigation Rail |
| 5 | Sidebar |
| 6 | Workspace |
| 7 | Cards |
| 8 | Buttons (7 种变体) |
| 9 | Text Inputs |
| 10 | ComboBox |
| 11 | TabPane |
| 12 | TableView |
| 13 | AI & Chat |
| 14 | Tags & Badges |
| 15 | Mind Map Nodes |
| 16 | StatusBar |
| 17 | Slide Panel |
| 18 | Empty State |
| 19 | Utility Classes |
| 20 | Misc Components |
