---
name: "analysis-writer"
description: "专业分析撰写器，基于数据和图表生成深度、专业的分析文本。当需要为报告章节撰写分析内容时调用此技能。"
---

# Analysis Writer (专业分析撰写器)

## 核心职责
- 生成深度、专业的数据分析文本
- 提供有价值的洞察和建议
- 确保分析逻辑清晰、论证有力
- 适配不同风格要求

## 输入参数

```json
{
  "chapterTitle": "学院专利分布与排名",
  "data": {
    "rawData": [...],
    "statistics": { ... },
    "insights": [...]
  },
  "chartInfo": {
    "type": "horizontal_bar",
    "filePath": "./charts/college_ranking.png",
    "visualInsights": [...]
  },
  "context": {
    "globalInsights": [...],
    "relatedChapters": ["技术领域热点"],
    "previousFindings": {...}
  },
  "requirements": {
    "minWords": 400,
    "style": "professional|academic|executive|narrative",
    "focusAreas": ["排名差异", "集中度", "发展趋势"],
    "mustInclude": ["数据支撑", "业务解读", "行动建议"],
    "language": "zh-CN"
  }
}
```

## 分析框架 (四层结构)

### Layer 1: 数据特征描述 (What)
- 基本统计量描述
- 分布特征说明
- 关键数据点标注

### Layer 2: 趋势与规律 (How)
- 变化趋势识别
- 模式和规律发现
- 异常点分析

### Layer 3: 深层原因 (Why)
- 业务背景关联
- 因果关系推断
- 外部因素影响

### Layer 4: 价值与建议 (So What)
- 战略意义阐述
- 行动建议提供
- 风险提示

## 输出格式

```json
{
  "needsSkill": true,
  "skillName": "analysis-writer",
  "reason": "分析文本生成完成",
  "execution": {
    "analysis": {
      "title": "学院专利分布与排名深度分析",
      "wordCount": 580,
      "readingTime": "2.5分钟",
      "content": "**【数据概览】**\n\n本章节对全校15个学院的专利产出情况进行了全面统计分析...",
      "structure": {
        "dataDescription": "120字",
        "trendAnalysis": "180字",
        "causalInference": "150字",
        "recommendations": "130字"
      },
      "keyFindings": [
        {
          "finding": "计算机学院以320项专利稳居榜首",
          "evidence": "占总量的21.3%，是第二名(185项)的1.73倍",
          "significance": "high",
          "confidence": 0.95
        },
        {
          "finding": "专利产出呈现明显的马太效应",
          "evidence": "前5名贡献68%，后5名仅占12%",
          "significance": "medium",
          "confidence": 0.88
        }
      ],
      "actionableInsights": [
        "建议加大对中游学院(排名6-10)的扶持力度",
        "可考虑建立跨学院合作机制促进知识溢出",
        "关注尾部学院的研发瓶颈并提供针对性支持"
      ],
      "crossReferences": [
        "与技术领域热点分析: 计算机学院优势集中在AI/软件领域",
        "与高价值专利榜单: 前20名高价值专利中45%来自头部3个学院"
      ]
    },
    "qualityMetrics": {
      "depthScore": 0.89,
      "coherenceScore": 0.92,
      "actionabilityScore": 0.85,
      "overallQuality": "A-"
    }
  }
}
```

## 写作风格指南

### professional (专业风格)
- 客观严谨，数据驱动
- 使用专业术语但不晦涩
- 结构化呈现，逻辑清晰

### academic (学术风格)
- 引用规范，论证严密
- 文献对比，理论支撑
- 局限性声明

### executive (高管摘要)
- 结论先行，要点突出
- 一页纸原则
- 关注决策价值

### narrative (叙事风格)
- 故事化叙述
- 从问题到解决方案
- 引人入胜的洞察

## 与其他Skill的交互
← data-analyzer: 接收统计数据和特征
← chart-creator: 接收可视化洞察
← report-planner: 接收分析要求定义
→ report-assembler: 提供完整分析内容
