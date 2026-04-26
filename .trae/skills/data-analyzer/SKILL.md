---
name: "data-analyzer"
description: "数据分析器，执行SQL查询并生成数据摘要、统计信息和特征分析。当需要获取和分析数据时调用此技能。"
---

# Data Analyzer (数据分析器)

## 核心职责
- 执行SQL查询并返回结果
- 自动生成数据统计摘要
- 识别数据特征和异常值
- 为下游skill提供结构化数据

## 输入参数

```json
{
  "sql": "SELECT college, COUNT(*) as cnt FROM patent_info WHERE deleted = 0 GROUP BY college ORDER BY cnt DESC",
  "analysisDepth": "basic|detailed|comprehensive",
  "includeStatistics": true,
  "includeDistribution": true,
  "maxRows": 50
}
```

## 输出格式

```json
{
  "needsSkill": true,
  "skillName": "data-analyzer",
  "reason": "数据分析完成",
  "execution": {
    "queryResult": {
      "rowCount": 15,
      "columnCount": 2,
      "columns": ["college", "cnt"],
      "data": [...]
    },
    "statistics": {
      "summary": {
        "totalRecords": 1500,
        "uniqueValues": 15,
        "nullCount": 0
      },
      "numericFields": {
        "cnt": {
          "min": 45,
          "max": 320,
          "avg": 100,
          "median": 95,
          "stdDev": 78.5,
          "distribution": "right-skewed"
        }
      },
      "categoricalFields": {
        "college": {
          "uniqueCount": 15,
          "topValues": [...],
          "entropy": 2.8
        }
      }
    },
    "insights": [
      "数据呈现明显的右偏分布，头部学院贡献了60%的专利",
      "最大值是最小值的7倍，表明学院间研发投入差异显著"
    ],
    "qualityScore": 0.92,
    "recommendations": {
      "chartType": "horizontal_bar",
      "analysisFocus": "关注头部集中度和长尾分布"
    }
  }
}
```

## 分析深度级别

### basic (基础)
- 行数、列数统计
- 基本的最大/最小/平均值
- 空值检测

### detailed (详细)
- 基础 + 中位数、标准差
- 分布类型识别 (正态/偏态)
- 异常值检测
- Top-N 和 Bottom-N

### comprehensive (全面)
- 详细 + 分位数分析
- 相关性矩阵 (多数值字段)
- 趋势检测 (时间序列)
- 聚类建议

## 与其他Skill的交互
← report-planner: 接收SQL任务
→ chart-creator: 提供数据 + 图表类型建议
→ analysis-writer: 提供数据特征和分析建议
