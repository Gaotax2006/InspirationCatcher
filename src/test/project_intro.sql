-- 灵感捕手 - 项目简介 SQL 文件（修复版）
-- 创建时间：当前时间
-- 注意：这个文件创建了一个完整的项目演示，使用更大的ID范围避免冲突

BEGIN TRANSACTION;

-- ============================================
-- 0. 先删除可能存在的旧数据（避免主键冲突）
-- ============================================
-- 注意：这个部分谨慎执行，如果你有重要数据，请先备份
-- 我们使用更大的ID范围避免冲突，所以通常不需要删除
-- 但如果需要确保干净环境，可以取消注释以下代码

DELETE FROM mindmap_connections WHERE project_id = 10000;
DELETE FROM mindmap_nodes WHERE project_id = 10000;
DELETE FROM idea_tags WHERE idea_id BETWEEN 20000 AND 20299;
DELETE FROM ideas WHERE project_id = 10000;
DELETE FROM tags WHERE id BETWEEN 10000 AND 10099;
DELETE FROM projects WHERE id = 10000;


-- ============================================
-- 1. 创建项目（使用更大的ID）
-- ============================================
INSERT OR REPLACE INTO projects (id, name, description, color, created_at, updated_at, tag_strategy)
VALUES (10000, '项目简介', '灵感捕手应用程序功能演示项目', '#4CAF50', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'GLOBAL');

-- ============================================
-- 2. 创建标签（按功能分类，使用更大的ID）
-- ============================================
INSERT OR IGNORE INTO tags (id, name, color, description, usage_count) VALUES
-- 核心功能标签（从10000开始）
(10001, '基础功能', '#2196F3', '应用程序基础功能', 0),
(10002, '灵感管理', '#4CAF50', '灵感创建、编辑、删除', 0),
(10003, '表格功能', '#FF9800', '表格显示和操作', 0),
(10004, '过滤搜索', '#9C27B0', '搜索和过滤功能', 0),
(10005, '编辑器', '#009688', 'Markdown编辑器', 0),
(10006, '思维导图', '#FF5722', '思维导图功能', 0),
(10007, '字体界面', '#795548', '字体和界面设置', 0),
(10008, '数据管理', '#607D8B', '数据管理功能', 0),
(10009, '性能测试', '#E91E63', '性能和稳定性', 0),
(10010, '综合场景', '#3F51B5', '完整工作流程', 0);

-- ============================================
-- 3. 插入所有功能灵感（使用更大的ID）
-- ============================================

-- 3.1 基础功能组
INSERT INTO ideas (id, project_id, title, content, idea_type, mood, importance, created_at, updated_at) VALUES
                                                                                                            (20001, 10000, '应用程序启动',
                                                                                                             '应用程序启动流程：
                                                                                                             1. 双击运行应用程序
                                                                                                             2. 主窗口正常显示
                                                                                                             3. 状态栏显示"就绪"
                                                                                                             4. 数据库连接正常
                                                                                                             5. 默认项目已加载

                                                                                                             预期结果：
                                                                                                             - 主窗口标题正确显示
                                                                                                             - 所有UI组件正常初始化
                                                                                                             - 数据库连接状态正常
                                                                                                             - 默认项目正确加载',
                                                                                                             'IDEA', 'NEUTRAL', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20002, 10000, '项目选择器',
                                                                                                             '项目选择器功能：
                                                                                                             1. 显示所有项目列表
                                                                                                             2. 支持项目切换
                                                                                                             3. 新项目创建后自动刷新
                                                                                                             4. 保持当前选中状态

                                                                                                             测试步骤：
                                                                                                             1. 点击项目选择器下拉菜单
                                                                                                             2. 选择不同项目
                                                                                                             3. 验证灵感列表更新
                                                                                                             4. 创建新项目验证自动添加',
                                                                                                             'IDEA', 'NEUTRAL', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3.2 灵感管理组
INSERT INTO ideas (id, project_id, title, content, idea_type, mood, importance, created_at, updated_at) VALUES
                                                                                                            (20003, 10000, '新建灵感',
                                                                                                             '新建灵感功能：
                                                                                                             1. 多种新建方式：
                                                                                                                - 菜单栏：文件→新建灵感
                                                                                                                - 工具栏：新建按钮
                                                                                                                - 快捷键：Ctrl+N
                                                                                                                - 快速捕捉：Ctrl+Q
                                                                                                             2. 自动标题生成
                                                                                                             3. 完整字段编辑
                                                                                                             4. 标签支持

                                                                                                             功能特点：
                                                                                                             - 支持Markdown格式
                                                                                                             - 自动保存草稿
                                                                                                             - 标签智能提示
                                                                                                             - 多项目支持',
                                                                                                             'IDEA', 'CREATIVE', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20004, 10000, '编辑灵感',
                                                                                                             '编辑灵感功能：
                                                                                                             1. 选择要编辑的灵感
                                                                                                             2. 双击或点击编辑按钮
                                                                                                             3. 快捷键：Ctrl+E
                                                                                                             4. 完整编辑器界面
                                                                                                             5. 实时预览

                                                                                                             编辑功能：
                                                                                                             - 标题修改
                                                                                                             - 内容编辑（Markdown）
                                                                                                             - 类型更改
                                                                                                             - 重要性调整
                                                                                                             - 心情设置
                                                                                                             - 标签管理',
                                                                                                             'IDEA', 'THOUGHTFUL', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20005, 10000, '删除灵感',
                                                                                                             '删除灵感功能：
                                                                                                             1. 选择要删除的灵感
                                                                                                             2. 点击删除按钮
                                                                                                             3. 快捷键：Delete键
                                                                                                             4. 确认对话框
                                                                                                             5. 安全删除机制

                                                                                                             安全特性：
                                                                                                             - 删除前确认
                                                                                                             - 可取消操作
                                                                                                             - 数据完整性保护
                                                                                                             - 防止误删除',
                                                                                                             'IDEA', 'CALM', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20006, 10000, '快速捕捉',
                                                                                                             '快速捕捉功能：
                                                                                                             1. 快速打开窗口：Ctrl+Q
                                                                                                             2. 简化输入界面
                                                                                                             3. 快速保存
                                                                                                             4. 最小化干扰

                                                                                                             设计理念：
                                                                                                             - 快速记录闪念
                                                                                                             - 最小化操作步骤
                                                                                                             - 保持专注
                                                                                                             - 后续可完善',
                                                                                                             'IDEA', 'EXCITED', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3.3 表格功能组
INSERT INTO ideas (id, project_id, title, content, idea_type, mood, importance, created_at, updated_at) VALUES
                                                                                                            (20007, 10000, '表格数据显示',
                                                                                                             '表格数据显示功能：
                                                                                                             1. 多列显示：
                                                                                                                - 标题
                                                                                                                - 类型
                                                                                                                - 重要性（星级）
                                                                                                                - 心情
                                                                                                                - 标签
                                                                                                                - 创建时间
                                                                                                                - 更新时间
                                                                                                             2. 数据实时更新
                                                                                                             3. 重要性星级显示

                                                                                                             显示优化：
                                                                                                             - 列宽自适应
                                                                                                             - 数据格式化
                                                                                                             - 视觉层次清晰
                                                                                                             - 性能优化',
                                                                                                             'IDEA', 'NEUTRAL', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20008, 10000, '表格排序功能',
                                                                                                             '表格排序功能：
                                                                                                             1. 点击列头排序
                                                                                                             2. 升序/降序切换
                                                                                                             3. 多列排序支持
                                                                                                             4. 排序状态保持

                                                                                                             排序支持：
                                                                                                             - 文本列：字母顺序
                                                                                                             - 数字列：数值大小
                                                                                                             - 日期列：时间先后
                                                                                                             - 自定义列：类型/心情',
                                                                                                             'IDEA', 'NEUTRAL', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20009, 10000, '选择与详情',
                                                                                                             '选择与详情功能：
                                                                                                             1. 表格行选择
                                                                                                             2. 高亮显示选中行
                                                                                                             3. 右侧详情面板显示
                                                                                                             4. 多格式内容展示

                                                                                                             详情内容：
                                                                                                             - 完整标题
                                                                                                             - 详细内容
                                                                                                             - 所有元数据
                                                                                                             - 标签列表
                                                                                                             - 创建/更新时间',
                                                                                                             'IDEA', 'NEUTRAL', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3.4 过滤搜索组
INSERT INTO ideas (id, project_id, title, content, idea_type, mood, importance, created_at, updated_at) VALUES
                                                                                                            (20010, 10000, '快速搜索',
                                                                                                             '快速搜索功能：
                                                                                                             1. 实时搜索框
                                                                                                             2. 快捷键：Ctrl+F
                                                                                                             3. 清除搜索：Esc键
                                                                                                             4. 搜索结果高亮

                                                                                                             搜索特性：
                                                                                                             - 实时过滤
                                                                                                             - 多字段匹配
                                                                                                             - 智能提示
                                                                                                             - 性能优化',
                                                                                                             'IDEA', 'CALM', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20011, 10000, '高级过滤',
                                                                                                             '高级过滤功能：
                                                                                                             1. 多种过滤条件：
                                                                                                                - 类型过滤
                                                                                                                - 重要性过滤
                                                                                                                - 心情过滤
                                                                                                                - 日期范围过滤
                                                                                                                - 标签过滤
                                                                                                             2. 多条件组合
                                                                                                             3. 过滤结果实时更新
                                                                                                             4. 重置所有过滤

                                                                                                             过滤器设计：
                                                                                                             - 折叠式界面
                                                                                                             - 条件直观
                                                                                                             - 操作简便
                                                                                                             - 状态保存',
                                                                                                             'IDEA', 'THOUGHTFUL', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20012, 10000, '热门标签过滤',
                                                                                                             '热门标签过滤功能：
                                                                                                             1. 标签云显示
                                                                                                             2. 点击标签快速过滤
                                                                                                             3. 标签使用频率统计
                                                                                                             4. 标签颜色编码

                                                                                                             标签云特性：
                                                                                                             - 使用频率决定大小
                                                                                                             - 颜色分类
                                                                                                             - 点击交互
                                                                                                             - 实时更新',
                                                                                                             'IDEA', 'CREATIVE', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3.5 编辑器功能组
INSERT INTO ideas (id, project_id, title, content, idea_type, mood, importance, created_at, updated_at) VALUES
                                                                                                            (20013, 10000, 'Markdown编辑器',
                                                                                                             'Markdown编辑器功能：
                                                                                                             1. 完整Markdown支持
                                                                                                             2. 编辑工具栏：
                                                                                                                - 加粗 (Ctrl+B)
                                                                                                                - 斜体 (Ctrl+I)
                                                                                                                - 标题H1/H2
                                                                                                                - 代码块
                                                                                                                - 列表
                                                                                                                - 引用
                                                                                                                - 链接
                                                                                                                - 图片
                                                                                                             3. 实时预览
                                                                                                             4. 语法高亮

                                                                                                             编辑器特点：
                                                                                                             - 双栏布局
                                                                                                             - 实时同步
                                                                                                             - 工具栏快捷操作
                                                                                                             - 可配置选项',
                                                                                                             'IDEA', 'INSPIRED', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20014, 10000, '编辑器预览',
                                                                                                             '编辑器预览功能：
                                                                                                             1. 实时Markdown渲染
                                                                                                             2. 刷新预览按钮
                                                                                                             3. 滚动同步
                                                                                                             4. 多格式支持

                                                                                                             预览特性：
                                                                                                             - HTML渲染
                                                                                                             - CSS样式美化
                                                                                                             - 代码高亮
                                                                                                             - 响应式布局',
                                                                                                             'IDEA', 'CALM', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20015, 10000, 'AI功能扩展',
                                                                                                             'AI功能扩展：
                                                                                                             1. AI建议生成
                                                                                                             2. AI扩展内容
                                                                                                             3. 大纲自动生成
                                                                                                             4. 智能提示

                                                                                                             AI集成：
                                                                                                             - API调用
                                                                                                             - 异步处理
                                                                                                             - 结果格式化
                                                                                                             - 缓存机制',
                                                                                                             'IDEA', 'EXCITED', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3.6 思维导图功能组
INSERT INTO ideas (id, project_id, title, content, idea_type, mood, importance, created_at, updated_at) VALUES
                                                                                                            (20016, 10000, '思维导图基础操作',
                                                                                                             '思维导图基础操作：
                                                                                                             1. 视图控制：
                                                                                                                - 缩放（鼠标滚轮）
                                                                                                                - 平移（中键拖拽）
                                                                                                                - 居中视图
                                                                                                                - 重置视图
                                                                                                             2. 工具栏操作
                                                                                                             3. 状态显示

                                                                                                             视图特性：
                                                                                                             - 平滑缩放
                                                                                                             - 流畅平移
                                                                                                             - 自适应布局
                                                                                                             - 性能优化',
                                                                                                             'IDEA', 'CREATIVE', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20017, 10000, '节点操作',
                                                                                                             '节点操作功能：
                                                                                                             1. 节点创建：
                                                                                                                - 双击空白处创建
                                                                                                                - 拖拽灵感创建
                                                                                                                - 工具栏创建
                                                                                                             2. 节点编辑：
                                                                                                                - 双击编辑
                                                                                                                - 右键菜单
                                                                                                                - 文本修改
                                                                                                             3. 节点删除：
                                                                                                                - Delete键
                                                                                                                - 右键删除
                                                                                                                - 确认对话框

                                                                                                             节点类型：
                                                                                                             - 概念节点
                                                                                                             - 灵感节点
                                                                                                             - 外部链接节点',
                                                                                                             'IDEA', 'THOUGHTFUL', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20018, 10000, '拖拽功能',
                                                                                                             '拖拽功能：
                                                                                                             1. 表格拖拽到导图
                                                                                                             2. 左侧列表拖拽
                                                                                                             3. 节点位置拖拽
                                                                                                             4. 连接创建拖拽

                                                                                                             拖拽交互：
                                                                                                             - 视觉反馈
                                                                                                             - 光标变化
                                                                                                             - 放置提示
                                                                                                             - 操作确认',
                                                                                                             'IDEA', 'EXCITED', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20019, 10000, '自动布局与导出',
                                                                                                             '自动布局与导出：
                                                                                                             1. 自动布局算法
                                                                                                             2. 树状结构排列
                                                                                                             3. 图片导出功能
                                                                                                             4. 多种格式支持

                                                                                                             导出功能：
                                                                                                             - PNG格式
                                                                                                             - JPEG格式
                                                                                                             - 高质量渲染
                                                                                                             - 自动裁剪',
                                                                                                             'IDEA', 'CALM', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3.7 字体界面组
INSERT INTO ideas (id, project_id, title, content, idea_type, mood, importance, created_at, updated_at) VALUES
                                                                                                            (20020, 10000, '字体调整',
                                                                                                             '字体调整功能：
                                                                                                             1. 字体大小调整：
                                                                                                                - A+ 增大字体
                                                                                                                - A- 减小字体
                                                                                                                - 重置字体大小
                                                                                                             2. 字体对话框
                                                                                                             3. 全局字体设置

                                                                                                             字体管理：
                                                                                                             - 统一字体控制
                                                                                                             - 多组件同步
                                                                                                             - 持久化设置
                                                                                                             - 重置功能',
                                                                                                             'IDEA', 'NEUTRAL', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20021, 10000, '界面布局',
                                                                                                             '界面布局功能：
                                                                                                             1. 窗口大小调整
                                                                                                             2. 分栏比例调整
                                                                                                             3. 标签页切换
                                                                                                             4. 面板显示控制

                                                                                                             布局特性：
                                                                                                             - 响应式设计
                                                                                                             - 用户自定义
                                                                                                             - 布局保存
                                                                                                             - 默认恢复',
                                                                                                             'IDEA', 'NEUTRAL', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3.8 数据管理组
INSERT INTO ideas (id, project_id, title, content, idea_type, mood, importance, created_at, updated_at) VALUES
                                                                                                            (20022, 10000, '标签管理',
                                                                                                             '标签管理功能：
                                                                                                             1. 标签统计查看
                                                                                                             2. 清理未使用标签
                                                                                                             3. 标签颜色设置
                                                                                                             4. 标签使用分析

                                                                                                             管理工具：
                                                                                                             - 统计面板
                                                                                                             - 清理工具
                                                                                                             - 颜色选择器
                                                                                                             - 使用报告',
                                                                                                             'IDEA', 'THOUGHTFUL', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20023, 10000, '数据持久化',
                                                                                                             '数据持久化功能：
                                                                                                             1. 自动保存
                                                                                                             2. 手动保存
                                                                                                             3. 数据备份
                                                                                                             4. 恢复功能

                                                                                                             持久化机制：
                                                                                                             - SQLite数据库
                                                                                                             - 事务处理
                                                                                                             - 数据完整性
                                                                                                             - 备份策略',
                                                                                                             'IDEA', 'CALM', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3.9 性能测试组
INSERT INTO ideas (id, project_id, title, content, idea_type, mood, importance, created_at, updated_at) VALUES
                                                                                                            (20024, 10000, '大量数据测试',
                                                                                                             '大量数据测试：
                                                                                                             1. 大量灵感测试
                                                                                                             2. 大量节点测试
                                                                                                             3. 过滤性能测试
                                                                                                             4. 内存使用监控

                                                                                                             性能优化：
                                                                                                             - 懒加载
                                                                                                             - 虚拟滚动
                                                                                                             - 缓存机制
                                                                                                             - 异步处理',
                                                                                                             'IDEA', 'NEUTRAL', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20025, 10000, '异常处理',
                                                                                                             '异常处理功能：
                                                                                                             1. 输入验证
                                                                                                             2. 错误提示
                                                                                                             3. 恢复机制
                                                                                                             4. 日志记录

                                                                                                             异常处理策略：
                                                                                                             - 用户友好提示
                                                                                                             - 详细错误信息
                                                                                                             - 自动恢复尝试
                                                                                                             - 问题诊断',
                                                                                                             'IDEA', 'CALM', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3.10 综合场景组
INSERT INTO ideas (id, project_id, title, content, idea_type, mood, importance, created_at, updated_at) VALUES
                                                                                                            (20026, 10000, '完整工作流',
                                                                                                             '完整工作流场景：
                                                                                                             1. 新建灵感
                                                                                                             2. 编辑内容
                                                                                                             3. 添加标签
                                                                                                             4. 保存灵感
                                                                                                             5. 思维导图组织
                                                                                                             6. 导出分享

                                                                                                             工作流特点：
                                                                                                             - 端到端流程
                                                                                                             - 功能集成
                                                                                                             - 数据连贯
                                                                                                             - 用户友好',
                                                                                                             'IDEA', 'INSPIRED', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

                                                                                                            (20027, 10000, '多项目管理',
                                                                                                             '多项目管理场景：
                                                                                                             1. 项目创建
                                                                                                             2. 项目切换
                                                                                                             3. 数据隔离
                                                                                                             4. 项目统计

                                                                                                             项目管理：
                                                                                                             - 独立数据空间
                                                                                                             - 快速切换
                                                                                                             - 统计信息
                                                                                                             - 项目归档',
                                                                                                             'IDEA', 'THOUGHTFUL', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================
-- 4. 添加灵感-标签关联
-- ============================================
-- 基础功能组
INSERT INTO idea_tags (idea_id, tag_id) VALUES
                                            (20001, 10001), (20002, 10001);

-- 灵感管理组
INSERT INTO idea_tags (idea_id, tag_id) VALUES
                                            (20003, 10002), (20004, 10002), (20005, 10002), (20006, 10002);

-- 表格功能组
INSERT INTO idea_tags (idea_id, tag_id) VALUES
                                            (20007, 10003), (20008, 10003), (20009, 10003);

-- 过滤搜索组
INSERT INTO idea_tags (idea_id, tag_id) VALUES
                                            (20010, 10004), (20011, 10004), (20012, 10004);

-- 编辑器功能组
INSERT INTO idea_tags (idea_id, tag_id) VALUES
                                            (20013, 10005), (20014, 10005), (20015, 10005);

-- 思维导图功能组
INSERT INTO idea_tags (idea_id, tag_id) VALUES
                                            (20016, 10006), (20017, 10006), (20018, 10006), (20019, 10006);

-- 字体界面组
INSERT INTO idea_tags (idea_id, tag_id) VALUES
                                            (20020, 10007), (20021, 10007);

-- 数据管理组
INSERT INTO idea_tags (idea_id, tag_id) VALUES
                                            (20022, 10008), (20023, 10008);

-- 性能测试组
INSERT INTO idea_tags (idea_id, tag_id) VALUES
                                            (20024, 10009), (20025, 10009);

-- 综合场景组
INSERT INTO idea_tags (idea_id, tag_id) VALUES
                                            (20026, 10010), (20027, 10010);

-- ============================================
-- 5. 创建思维导图节点（形成更分散的树状结构）
-- ============================================
-- 思维导图节点ID从30001开始
-- 画布大小假设为2000x1500，根节点在中心(1000, 150)

-- 5.1 根节点（项目简介）
INSERT INTO mindmap_nodes (id, project_id, text, description, node_type, x, y, width, height, color, is_root, is_expanded, font_size, font_weight) VALUES
    (30001, 10000, '灵感捕手功能导图', '应用程序完整功能展示', 'CONCEPT', 1000, 150, 220, 70, '#4CAF50', 1, 1, 18, 'BOLD');

-- 5.2 第一层分类节点（分布在根节点下方，形成扇形分布，更加分散）
-- 使用三角函数计算位置，形成扇形
INSERT INTO mindmap_nodes (id, project_id, text, description, node_type, x, y, width, height, color, is_expanded, font_size, font_weight) VALUES
-- 左侧扇形（5个节点）
(30002, 10000, '基础功能', '应用程序启动和基础操作', 'CONCEPT', 400, 350, 140, 50, '#2196F3', 1, 16, 'BOLD'),
(30003, 10000, '灵感管理', '灵感CRUD和快速捕捉', 'CONCEPT', 600, 300, 140, 50, '#4CAF50', 1, 16, 'BOLD'),
(30004, 10000, '表格功能', '数据显示和操作', 'CONCEPT', 800, 250, 140, 50, '#FF9800', 1, 16, 'BOLD'),
(30005, 10000, '过滤搜索', '搜索和过滤功能', 'CONCEPT', 1000, 200, 140, 50, '#9C27B0', 1, 16, 'BOLD'),
(30006, 10000, '编辑器', 'Markdown编辑和预览', 'CONCEPT', 1200, 250, 140, 50, '#009688', 1, 16, 'BOLD'),

-- 右侧扇形（5个节点）
(30007, 10000, '思维导图', '可视化思维组织', 'CONCEPT', 800, 500, 140, 50, '#FF5722', 1, 16, 'BOLD'),
(30008, 10000, '字体界面', '界面和字体设置', 'CONCEPT', 1000, 550, 140, 50, '#795548', 1, 16, 'BOLD'),
(30009, 10000, '数据管理', '标签和数据持久化', 'CONCEPT', 1200, 500, 140, 50, '#607D8B', 1, 16, 'BOLD'),
(30010, 10000, '性能测试', '性能和稳定性', 'CONCEPT', 1400, 450, 140, 50, '#E91E63', 1, 16, 'BOLD'),
(30011, 10000, '综合场景', '完整工作流程', 'CONCEPT', 1600, 400, 140, 50, '#3F51B5', 1, 16, 'BOLD');

-- 5.3 第二层功能节点（每个分类下的具体功能，分布在分类节点周围，更加分散）
-- 基础功能组的功能节点（分布在分类节点周围）
INSERT INTO mindmap_nodes (id, project_id, idea_id, text, description, node_type, x, y, width, height, color, is_expanded) VALUES
                                                                                                                               (30012, 10000, 20001, '应用程序启动', '启动流程和初始化', 'IDEA', 300, 450, 160, 60, '#2196F3', 1),
                                                                                                                               (30013, 10000, 20002, '项目选择器', '项目切换和管理', 'IDEA', 500, 450, 160, 60, '#2196F3', 1);

-- 灵感管理组的功能节点（垂直排列，间隔更大）
INSERT INTO mindmap_nodes (id, project_id, idea_id, text, description, node_type, x, y, width, height, color, is_expanded) VALUES
                                                                                                                               (30014, 10000, 20003, '新建灵感', '多种新建方式', 'IDEA', 500, 400, 160, 60, '#4CAF50', 1),
                                                                                                                               (30015, 10000, 20004, '编辑灵感', '完整编辑功能', 'IDEA', 600, 450, 160, 60, '#4CAF50', 1),
                                                                                                                               (30016, 10000, 20005, '删除灵感', '安全删除机制', 'IDEA', 500, 500, 160, 60, '#4CAF50', 1),
                                                                                                                               (30017, 10000, 20006, '快速捕捉', '快速记录闪念', 'IDEA', 600, 550, 160, 60, '#4CAF50', 1);

-- 表格功能组的功能节点（水平排列）
INSERT INTO mindmap_nodes (id, project_id, idea_id, text, description, node_type, x, y, width, height, color, is_expanded) VALUES
                                                                                                                               (30018, 10000, 20007, '数据显示', '多列信息展示', 'IDEA', 700, 200, 160, 60, '#FF9800', 1),
                                                                                                                               (30019, 10000, 20008, '排序功能', '列排序支持', 'IDEA', 850, 200, 160, 60, '#FF9800', 1),
                                                                                                                               (30020, 10000, 20009, '选择与详情', '详情面板显示', 'IDEA', 1000, 200, 160, 60, '#FF9800', 1);

-- 过滤搜索组的功能节点（三角形排列）
INSERT INTO mindmap_nodes (id, project_id, idea_id, text, description, node_type, x, y, width, height, color, is_expanded) VALUES
                                                                                                                               (30021, 10000, 20010, '快速搜索', '实时搜索功能', 'IDEA', 900, 100, 160, 60, '#9C27B0', 1),
                                                                                                                               (30022, 10000, 20011, '高级过滤', '多条件过滤', 'IDEA', 1050, 150, 160, 60, '#9C27B0', 1),
                                                                                                                               (30023, 10000, 20012, '标签过滤', '标签云和过滤', 'IDEA', 1200, 100, 160, 60, '#9C27B0', 1);

-- 编辑器功能组的功能节点（垂直排列）
INSERT INTO mindmap_nodes (id, project_id, idea_id, text, description, node_type, x, y, width, height, color, is_expanded) VALUES
                                                                                                                               (30024, 10000, 20013, 'Markdown编辑', '完整编辑器功能', 'IDEA', 1150, 350, 170, 60, '#009688', 1),
                                                                                                                               (30025, 10000, 20014, '实时预览', '编辑预览同步', 'IDEA', 1250, 350, 160, 60, '#009688', 1),
                                                                                                                               (30026, 10000, 20015, 'AI功能', '智能扩展建议', 'IDEA', 1200, 450, 160, 60, '#009688', 1);

-- 思维导图功能组的功能节点（方形排列）
INSERT INTO mindmap_nodes (id, project_id, idea_id, text, description, node_type, x, y, width, height, color, is_expanded) VALUES
                                                                                                                               (30027, 10000, 20016, '基础操作', '视图控制操作', 'IDEA', 700, 600, 160, 60, '#FF5722', 1),
                                                                                                                               (30028, 10000, 20017, '节点操作', '节点创建编辑', 'IDEA', 850, 600, 160, 60, '#FF5722', 1),
                                                                                                                               (30029, 10000, 20018, '拖拽功能', '拖拽交互操作', 'IDEA', 700, 700, 160, 60, '#FF5722', 1),
                                                                                                                               (30030, 10000, 20019, '导出功能', '图片导出功能', 'IDEA', 850, 700, 160, 60, '#FF5722', 1);

-- 字体界面组的功能节点（水平排列）
INSERT INTO mindmap_nodes (id, project_id, idea_id, text, description, node_type, x, y, width, height, color, is_expanded) VALUES
                                                                                                                               (30031, 10000, 20020, '字体调整', '字体大小控制', 'IDEA', 900, 650, 160, 60, '#795548', 1),
                                                                                                                               (30032, 10000, 20021, '界面布局', '布局调整控制', 'IDEA', 1100, 650, 160, 60, '#795548', 1);

-- 数据管理组的功能节点（垂直排列）
INSERT INTO mindmap_nodes (id, project_id, idea_id, text, description, node_type, x, y, width, height, color, is_expanded) VALUES
                                                                                                                               (30033, 10000, 20022, '标签管理', '标签统计清理', 'IDEA', 1150, 550, 160, 60, '#607D8B', 1),
                                                                                                                               (30034, 10000, 20023, '数据持久化', '数据保存恢复', 'IDEA', 1250, 550, 160, 60, '#607D8B', 1);

-- 性能测试组的功能节点（水平排列）
INSERT INTO mindmap_nodes (id, project_id, idea_id, text, description, node_type, x, y, width, height, color, is_expanded) VALUES
                                                                                                                               (30035, 10000, 20024, '大量数据测试', '性能压力测试', 'IDEA', 1350, 400, 160, 60, '#E91E63', 1),
                                                                                                                               (30036, 10000, 20025, '异常处理', '错误处理机制', 'IDEA', 1500, 400, 160, 60, '#E91E63', 1);

-- 综合场景组的功能节点（垂直排列）
INSERT INTO mindmap_nodes (id, project_id, idea_id, text, description, node_type, x, y, width, height, color, is_expanded) VALUES
                                                                                                                               (30037, 10000, 20026, '完整工作流', '端到端流程', 'IDEA', 1550, 300, 160, 60, '#3F51B5', 1),
                                                                                                                               (30038, 10000, 20027, '多项目管理', '项目管理功能', 'IDEA', 1650, 300, 160, 60, '#3F51B5', 1);

-- ============================================
-- 6. 创建思维导图连接（形成清晰的树状结构）
-- ============================================
-- 连接ID从40001开始

-- 6.1 根节点连接到所有分类节点（使用曲线，不同颜色）
INSERT INTO mindmap_connections (id, project_id, source_node_id, target_node_id, connection_type, label, color, width, style, strength) VALUES
-- 连接到左侧分支
(40001, 10000, 30001, 30002, 'RELATED', '包含', '#2196F3', 3, 'SOLID', 0.8),
(40002, 10000, 30001, 30003, 'RELATED', '包含', '#4CAF50', 3, 'SOLID', 0.8),
(40003, 10000, 30001, 30004, 'RELATED', '包含', '#FF9800', 3, 'SOLID', 0.8),
(40004, 10000, 30001, 30005, 'RELATED', '包含', '#9C27B0', 3, 'SOLID', 0.8),
(40005, 10000, 30001, 30006, 'RELATED', '包含', '#009688', 3, 'SOLID', 0.8),

-- 连接到右侧分支
(40006, 10000, 30001, 30007, 'RELATED', '包含', '#FF5722', 3, 'SOLID', 0.8),
(40007, 10000, 30001, 30008, 'RELATED', '包含', '#795548', 3, 'SOLID', 0.8),
(40008, 10000, 30001, 30009, 'RELATED', '包含', '#607D8B', 3, 'SOLID', 0.8),
(40009, 10000, 30001, 30010, 'RELATED', '包含', '#E91E63', 3, 'SOLID', 0.8),
(40010, 10000, 30001, 30011, 'RELATED', '包含', '#3F51B5', 3, 'SOLID', 0.8);

-- 6.2 分类节点连接到各自的功能节点（使用细线，同色系）
-- 基础功能组
INSERT INTO mindmap_connections (id, project_id, source_node_id, target_node_id, connection_type, label, color, width, style, strength) VALUES
                                                                                                                                            (40011, 10000, 30002, 30012, 'DEPENDS_ON', '实现', '#2196F3', 2, 'SOLID', 0.6),
                                                                                                                                            (40012, 10000, 30002, 30013, 'DEPENDS_ON', '实现', '#2196F3', 2, 'SOLID', 0.6);

-- 灵感管理组
INSERT INTO mindmap_connections (id, project_id, source_node_id, target_node_id, connection_type, label, color, width, style, strength) VALUES
                                                                                                                                            (40013, 10000, 30003, 30014, 'DEPENDS_ON', '实现', '#4CAF50', 2, 'SOLID', 0.6),
                                                                                                                                            (40014, 10000, 30003, 30015, 'DEPENDS_ON', '实现', '#4CAF50', 2, 'SOLID', 0.6),
                                                                                                                                            (40015, 10000, 30003, 30016, 'DEPENDS_ON', '实现', '#4CAF50', 2, 'SOLID', 0.6),
                                                                                                                                            (40016, 10000, 30003, 30017, 'DEPENDS_ON', '实现', '#4CAF50', 2, 'SOLID', 0.6);

-- 表格功能组
INSERT INTO mindmap_connections (id, project_id, source_node_id, target_node_id, connection_type, label, color, width, style, strength) VALUES
                                                                                                                                            (40017, 10000, 30004, 30018, 'DEPENDS_ON', '实现', '#FF9800', 2, 'SOLID', 0.6),
                                                                                                                                            (40018, 10000, 30004, 30019, 'DEPENDS_ON', '实现', '#FF9800', 2, 'SOLID', 0.6),
                                                                                                                                            (40019, 10000, 30004, 30020, 'DEPENDS_ON', '实现', '#FF9800', 2, 'SOLID', 0.6);

-- 过滤搜索组
INSERT INTO mindmap_connections (id, project_id, source_node_id, target_node_id, connection_type, label, color, width, style, strength) VALUES
                                                                                                                                            (40020, 10000, 30005, 30021, 'DEPENDS_ON', '实现', '#9C27B0', 2, 'SOLID', 0.6),
                                                                                                                                            (40021, 10000, 30005, 30022, 'DEPENDS_ON', '实现', '#9C27B0', 2, 'SOLID', 0.6),
                                                                                                                                            (40022, 10000, 30005, 30023, 'DEPENDS_ON', '实现', '#9C27B0', 2, 'SOLID', 0.6);

-- 编辑器功能组
INSERT INTO mindmap_connections (id, project_id, source_node_id, target_node_id, connection_type, label, color, width, style, strength) VALUES
                                                                                                                                            (40023, 10000, 30006, 30024, 'DEPENDS_ON', '实现', '#009688', 2, 'SOLID', 0.6),
                                                                                                                                            (40024, 10000, 30006, 30025, 'DEPENDS_ON', '实现', '#009688', 2, 'SOLID', 0.6),
                                                                                                                                            (40025, 10000, 30006, 30026, 'DEPENDS_ON', '实现', '#009688', 2, 'SOLID', 0.6);

-- 思维导图功能组
INSERT INTO mindmap_connections (id, project_id, source_node_id, target_node_id, connection_type, label, color, width, style, strength) VALUES
                                                                                                                                            (40026, 10000, 30007, 30027, 'DEPENDS_ON', '实现', '#FF5722', 2, 'SOLID', 0.6),
                                                                                                                                            (40027, 10000, 30007, 30028, 'DEPENDS_ON', '实现', '#FF5722', 2, 'SOLID', 0.6),
                                                                                                                                            (40028, 10000, 30007, 30029, 'DEPENDS_ON', '实现', '#FF5722', 2, 'SOLID', 0.6),
                                                                                                                                            (40029, 10000, 30007, 30030, 'DEPENDS_ON', '实现', '#FF5722', 2, 'SOLID', 0.6);

-- 字体界面组
INSERT INTO mindmap_connections (id, project_id, source_node_id, target_node_id, connection_type, label, color, width, style, strength) VALUES
                                                                                                                                            (40030, 10000, 30008, 30031, 'DEPENDS_ON', '实现', '#795548', 2, 'SOLID', 0.6),
                                                                                                                                            (40031, 10000, 30008, 30032, 'DEPENDS_ON', '实现', '#795548', 2, 'SOLID', 0.6);

-- 数据管理组
INSERT INTO mindmap_connections (id, project_id, source_node_id, target_node_id, connection_type, label, color, width, style, strength) VALUES
                                                                                                                                            (40032, 10000, 30009, 30033, 'DEPENDS_ON', '实现', '#607D8B', 2, 'SOLID', 0.6),
                                                                                                                                            (40033, 10000, 30009, 30034, 'DEPENDS_ON', '实现', '#607D8B', 2, 'SOLID', 0.6);

-- 性能测试组
INSERT INTO mindmap_connections (id, project_id, source_node_id, target_node_id, connection_type, label, color, width, style, strength) VALUES
                                                                                                                                            (40034, 10000, 30010, 30035, 'DEPENDS_ON', '实现', '#E91E63', 2, 'SOLID', 0.6),
                                                                                                                                            (40035, 10000, 30010, 30036, 'DEPENDS_ON', '实现', '#E91E63', 2, 'SOLID', 0.6);

-- 综合场景组
INSERT INTO mindmap_connections (id, project_id, source_node_id, target_node_id, connection_type, label, color, width, style, strength) VALUES
                                                                                                                                            (40036, 10000, 30011, 30037, 'DEPENDS_ON', '实现', '#3F51B5', 2, 'SOLID', 0.6),
                                                                                                                                            (40037, 10000, 30011, 30038, 'DEPENDS_ON', '实现', '#3F51B5', 2, 'SOLID', 0.6);

-- 6.3 添加一些交叉功能连接（显示功能间的关系，使用虚线）
INSERT INTO mindmap_connections (id, project_id, source_node_id, target_node_id, connection_type, label, color, width, style, strength) VALUES
-- 新建灵感与快速捕捉的关联
(40038, 10000, 30014, 30017, 'RELATED', '关联', '#888888', 1, 'DASHED', 0.3),
-- 表格与过滤的关联
(40039, 10000, 30018, 30021, 'RELATED', '协同', '#888888', 1, 'DASHED', 0.3),
-- 编辑器与预览的关联
(40040, 10000, 30024, 30025, 'EXTENDS', '扩展', '#888888', 1, 'DASHED', 0.3),
-- 思维导图节点与导出的关联
(40041, 10000, 30028, 30030, 'RELATED', '使用', '#888888', 1, 'DASHED', 0.3),
-- 完整工作流与多个功能的关联
(40042, 10000, 30037, 30014, 'USES', '使用', '#3F51B5', 1, 'DOTTED', 0.4),
(40043, 10000, 30037, 30024, 'USES', '使用', '#3F51B5', 1, 'DOTTED', 0.4),
(40044, 10000, 30037, 30027, 'USES', '使用', '#3F51B5', 1, 'DOTTED', 0.4);

-- ============================================
-- 7. 更新项目统计信息
-- ============================================
-- 更新灵感数量
UPDATE projects SET idea_count = (
    SELECT COUNT(*) FROM ideas WHERE project_id = 10000
) WHERE id = 10000;

-- 更新节点数量
UPDATE projects SET node_count = (
    SELECT COUNT(*) FROM mindmap_nodes WHERE project_id = 10000
) WHERE id = 10000;

-- 更新连接数量
UPDATE projects SET connection_count = (
    SELECT COUNT(*) FROM mindmap_connections WHERE project_id = 10000
) WHERE id = 10000;

-- 更新标签使用次数
UPDATE tags SET usage_count = (
    SELECT COUNT(*) FROM idea_tags WHERE tag_id = tags.id
) WHERE id BETWEEN 10001 AND 10010;

-- ============================================
-- 8. 最后确认所有操作
-- ============================================
-- 显示插入统计
SELECT '项目创建完成' as status;
SELECT '项目ID: 10000, 名称: 项目简介' as project_info;
SELECT COUNT(*) as total_ideas FROM ideas WHERE project_id = 10000;
SELECT COUNT(*) as total_tags FROM tags WHERE id BETWEEN 10001 AND 10010;
SELECT COUNT(*) as total_nodes FROM mindmap_nodes WHERE project_id = 10000;
SELECT COUNT(*) as total_connections FROM mindmap_connections WHERE project_id = 10000;

COMMIT;

-- SQL文件执行完成
-- 现在可以在应用程序中查看"项目简介"项目，其中包含了完整的灵感数据和更分散的思维导图