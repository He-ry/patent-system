---
name: "report-assembler"
description: "报告组装器，将所有章节内容整合为专业格式的最终文档。当需要生成Word/PDF报告文件时调用此技能。"
---

# Report Assembler (报告组装器)

## 核心职责
- 整合所有章节内容为完整报告
- 生成专业排版的文档
- 支持多种输出格式
- 确保视觉一致性和可读性

## 输入参数

```json
{
  "reportConfig": {
    "title": "2024年度专利情报深度分析报告",
    "subtitle": "基于专利数据库的全维度智能分析",
    "language": "zh-CN",
    "format": "word|pdf|html",
    "style": "professional|modern|academic|minimal"
  },
  "metadata": {
    "author": "AI智能分析引擎 V2.0",
    "generatedAt": "2024-01-15T10:30:00Z",
    "dataSource": "专利主库",
    "totalChapters": 8,
    "version": "2.0"
  },
  "sections": [
    {
      "id": "chapter_1",
      "order": 1,
      "title": "整体专利产出趋势分析",
      "chartPath": "./charts/trend_analysis.png",
      "chartCaption": "图1: 年度专利产出趋势",
      "dataTable": null,
      "tableCaption": null,
      "analysis": "**【数据概览】**\n\n...",
      "keyFindings": [...],
      "wordCount": 580
    }
  ],
  "executiveSummary": "本报告基于...",
  "appendix": {
    "methodology": "分析方法说明",
    "dataSources": "数据来源说明",
    "glossary": "术语表"
  }
}
```

## 文档结构模板

### 封面页
- 报告标题 (大标题)
- 副标题/描述
- 生成日期和版本
- 机构标识

### 目录页
- 章节列表 + 页码
- 图表索引
- 表格索引

### 执行摘要 (1页)
- 核心发现 (3-5个要点)
- 关键数据摘要
- 主要建议

### 正文章节 (每章2-3页)
- 章节标题 + 编号
- 数据可视化 (图表)
- 深度分析文本
- 要点总结

### 结论与建议
- 综合发现总结
- 战略性建议
- 后续行动项

### 附录
- 方法论说明
- 数据字典
- 完整数据表格

## 排版规范

### 字体系统
- **标题**: Microsoft YaHei Bold, 18-24pt, #1A5276
- **副标题**: Microsoft YaHei Bold, 14-16pt, #2980B9
- **正文**: Microsoft YaHei, 11-12pt, #2C3E50
- **注释**: Microsoft YaHei Italic, 9pt, #7F8C8D

### 配色方案
- **主色**: #1A5276 (深蓝)
- **辅助色**: #2980B9 (亮蓝)
- **强调色**: #27AE60 (绿色) / #E74C3C (红色)
- **中性色**: #34495E, #7F8C8D, #BDC3C7

### 间距规范
- 页边距: 上下2.54cm, 左右2.54cm(3.17cm)
- 行间距: 1.35-1.5倍
- 段前段后: 6-12pt
- 章节间距: 24pt

## 输出格式

```json
{
  "needsSkill": true,
  "skillName": "report-assembler",
  "reason": "报告文档生成完成",
  "execution": {
    "documentInfo": {
      "filePath": "./reports/patent_analysis_20240115.docx",
      "fileSize": "2.8MB",
      "pageCount": 24,
      "format": "docx",
      "downloadUrl": "/api/reports/download/patent_analysis_20240115.docx"
    },
    "qualityCheck": {
      "formattingScore": 0.95,
      "completenessScore": 1.0,
      "consistencyScore": 0.98,
      "accessibilityScore": 0.88
    },
    "statistics": {
      "totalWords": 5200,
      "chartCount": 8,
      "tableCount": 3,
      "chapterCount": 8
    }
  }
}
```

## 与其他Skill的交互
← analysis-writer: 接收所有章节的分析内容
← chart-creator: 接收图表文件路径
→ report-generator (编排器): 返回最终文档
