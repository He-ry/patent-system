---
name: "report-generator"
description: "报告生成编排器，协调多个专业Skills协作完成高质量专利分析报告的生成。当用户要求生成完整报告时调用此技能（作为主入口）。"
---

# Report Generator (报告生成编排器)

## ⚠️ 重要说明
**这是一个编排器Skill，不直接执行具体任务，而是协调以下子Skills协作完成：**

| 子Skill | 职责 | 触发时机 |
|---------|------|----------|
| **report-planner** | 智能规划报告结构 | 报告生成开始 |
| **data-analyzer** | 执行数据查询和分析 | 每个章节处理时 |
| **chart-creator** | 生成可视化图表 | 数据就绪后 |
| **analysis-writer** | 撰写深度分析文本 | 图表和数据就绪后 |
| **report-assembler** | 组装最终文档 | 所有章节完成后 |

## 工作流程

```
用户请求
    ↓
[report-planner] 分析需求 + 规划结构
    ↓
循环处理每个章节:
  ├─ [data-analyzer] 执行SQL + 统计分析
  ├─ [chart-creator] 生成最优图表
  └─ [analysis-writer] 撰写深度分析
    ↓
[report-assembler] 整合所有内容 → 输出文档
```

## 调用方式

### 方式1: 完全自动模式 (推荐)
```json
{
  "needsSkill": true,
  "skillName": "report-generator",
  "reason": "用户需要生成完整的专利分析报告",
  "execution": {
    "mode": "auto",
    "userQuery": "生成一份关于我校专利情况的综合分析报告",
    "outputFormat": "word",
    "requirements": {
      "depth": "comprehensive",
      "includeCharts": true,
      "includeTables": false,
      "language": "zh-CN"
    }
  }
}
```

### 方式2: 半自动模式 (指定部分参数)
```json
{
  "needsSkill": true,
  "skillName": "report-generator",
  "reason": "按用户指定维度生成报告",
  "execution": {
    "mode": "semi-auto",
    "userQuery": "分析计算机学院的专利情况",
    "outputFormat": "word",
    "customConfig": {
      "title": "计算机学院专利情报分析报告",
      "focusEntity": "computer_science_college",
      "specifiedDimensions": [
        "产出趋势",
        "技术领域分布",
        "高价值专利",
        "发明人团队"
      ]
    }
  }
}
```

### 方式3: 手动模式 (完全自定义)
```json
{
  "needsSkill": true,
  "skillName": "report-generator",
  "reason": "使用预定义章节生成报告",
  "execution": {
    "mode": "manual",
    "title": "自定义分析报告",
    "outputFormat": "word",
    "sections": [
      {
        "title": "自定义章节1",
        "sql": "SELECT ...",
        "chartType": "bar",
        "analysisFocus": "重点分析XXX"
      }
    ]
  }
}
```

## 编排策略

### 错误处理
- 单个章节失败不影响整体
- 自动降级：图表失败 → 表格 → 纯文本
- 重试机制：最多重试1次

### 进度管理
- 总进度 = 100%
- 规划阶段: 10%
- 每个章节: (80% / 章芽数)
- 文档组装: 10%

### 质量控制
- 最少有效章节数: 3个
- 每章节最少字数: 200字
- 图表成功率 > 60%才算合格

## 输出格式

```json
{
  "success": true,
  "skillName": "report-generator",
  "content": "🎉 报告已成功生成！\n\n📊 报告信息\n• 标题：xxx\n• 维度：8个\n• 格式：Word\n\n📥 点击上方链接下载...",
  "report": {
    "id": "uuid",
    "title": "报告标题",
    "filePath": "./reports/xxx.docx",
    "downloadUrl": "/api/reports/download/xxx.docx"
  },
  "executionStats": {
    "totalTime": "45.2s",
    "chaptersCompleted": 8,
    "chartsGenerated": 7,
    "totalWords": 5200,
    "skillsInvoked": [
      {"name": "report-planner", "time": "2.3s", "status": "success"},
      {"name": "data-analyzer", "time": "15.6s", "status": "success"},
      {"name": "chart-creator", "time": "12.8s", "status": "partial"},
      {"name": "analysis-writer", "time": "28.4s", "status": "success"},
      {"name": "report-assembler", "time": "3.2s", "status": "success"}
    ]
  }
}
```

## 最佳实践

1. **优先使用auto模式** - 让AI智能规划最合适的分析维度
2. **提供清晰的上下文** - 帮助planner做出更好的决策
3. **不要过度指定** - 让各子Skill发挥专业能力
4. **关注输出质量** - 检查生成的报告是否符合预期
