---
name: "report-planner"
description: "智能报告规划器，根据用户需求和数据特征自动设计报告结构和分析维度。当需要确定报告应该包含哪些分析章节时调用此技能。"
---

# Report Planner (智能报告规划器)

## 核心职责
- 分析用户意图和数据特征
- 自动设计最优报告结构
- 为每个分析维度生成执行计划

## 工作原理

### 1. 意图识别
```json
{
  "intentType": "global|entity|field|custom",
  "targetEntity": null,
  "analysisScope": "comprehensive|focused|comparison",
  "userConstraints": []
}
```

### 2. 维度推荐算法
基于以下因素动态选择分析维度：
- 数据库表结构和字段可用性
- 用户明确指定的需求
- 数据分布特征
- 业务价值优先级

### 3. 输出格式
返回章节计划数组：

```json
{
  "needsSkill": true,
  "skillName": "report-planner",
  "reason": "已规划报告结构",
  "execution": {
    "reportTitle": "智能分析报告标题",
    "totalChapters": 5,
    "chapters": [
      {
        "id": "chapter_1",
        "title": "专利产出趋势分析",
        "objective": "分析时间维度的专利数量变化",
        "priority": 1,
        "dependencies": [],
        "dataTask": {
          "sql": "SELECT ...",
          "expectedRows": "10-50"
        },
        "visualization": {
          "suggestedTypes": ["line", "bar"],
          "preferredType": "line",
          "focus": "趋势变化和增长率"
        },
        "analysisRequirements": {
          "minWords": 300,
          "keyInsights": ["趋势", "拐点", "增长率"],
          "shouldInclude": ["同比环比", "预测"]
        }
      }
    ]
  }
}
```

## 内置分析模板

### 全局分析模板 (默认)
1. 整体产出趋势 (时间序列)
2. 学院/机构分布 (排名)
3. 技术领域热点 (分类)
4. 高价值专利榜单 (TOP-N)
5. 法律状态统计 (占比)

### 发明人分析模板
1. 产出活跃度趋势
2. 技术研究方向
3. 合作网络图谱
4. 专利价值评估
5. 核心成果详情

### 学院分析模板
1. 研发活跃度趋势
2. 技术领域布局
3. 发明人团队构成
4. 成果质量分布
5. 转化情况分析

## 与其他Skill的交互
- → data-analyzer: 提供SQL查询任务
- → chart-creator: 建议图表类型
- → analysis-writer: 定义分析要求
