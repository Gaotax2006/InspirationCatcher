-- 1. 先删除所有现有数据（谨慎操作！）
DELETE FROM ideas;
DELETE FROM idea_tags;
DELETE FROM tags;
VACUUM;

-- 2. 插入大量测试数据（1000~100000条）
INSERT INTO ideas (title, content, idea_type, mood, importance, created_at, updated_at)
SELECT
    '测试灵感 ' || (ROW_NUMBER() OVER ()),
    '这是第 ' || (ROW_NUMBER() OVER ()) || ' 个测试灵感的内容。' ||'这是一个Markdown格式的测试内容。' ||'- 列表项1' ||'- 列表项2' ||'> 引用文本' ||'```' ||'代码块' ||'```',
    CASE (ROW_NUMBER() OVER () % 7)
        WHEN 0 THEN 'IDEA'
        WHEN 1 THEN 'QUOTE'
        WHEN 2 THEN 'QUESTION'
        WHEN 3 THEN 'TODO'
        WHEN 4 THEN 'DISCOVERY'
        WHEN 5 THEN 'CONFUSION'
        WHEN 6 THEN 'HYPOTHESIS'
        END,
    CASE (ROW_NUMBER() OVER () % 10)
        WHEN 0 THEN 'HAPPY'
        WHEN 1 THEN 'EXCITED'
        WHEN 2 THEN 'CALM'
        WHEN 3 THEN 'NEUTRAL'
        WHEN 4 THEN 'THOUGHTFUL'
        WHEN 5 THEN 'CREATIVE'
        WHEN 6 THEN 'INSPIRED'
        WHEN 7 THEN 'CURIOUS'
        WHEN 8 THEN 'CONFUSED'
        WHEN 9 THEN 'FRUSTRATED'
        END,
    (ROW_NUMBER() OVER () % 5) + 1,
    DATETIME('now', '-' || (ROW_NUMBER() OVER ()) || ' hours'),
    DATETIME('now', '-' || (ROW_NUMBER() OVER ()) || ' hours')
FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) a,
     (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) b,
     (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) c,
     (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) d,
     (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) e,
     (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) f,
     (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) g,
     (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) h
    LIMIT 0;

-- 3. 创建一些测试标签
INSERT INTO tags (name, color) VALUES
                                   ('编程', '#FF6B6B'),
                                   ('设计', '#4ECDC4'),
                                   ('思考', '#FFD166'),
                                   ('学习', '#06D6A0'),
                                   ('工作', '#118AB2'),
                                   ('生活', '#EF476F'),
                                   ('创意', '#FFD166'),
                                   ('问题', '#06D6A0'),
                                   ('计划', '#118AB2');

-- 4. 随机关联标签到灵感
INSERT INTO idea_tags (idea_id, tag_id)
SELECT
    i.id,
    t.id
FROM ideas i
         CROSS JOIN tags t
WHERE (i.id + t.id) % 7 = 0;

-- 5. 查看统计信息
SELECT COUNT(*) as 灵感总数 FROM ideas;
SELECT idea_type, COUNT(*) as 数量 FROM ideas GROUP BY idea_type;
SELECT mood, COUNT(*) as 数量 FROM ideas GROUP BY mood;
SELECT importance, COUNT(*) as 数量 FROM ideas GROUP BY importance;