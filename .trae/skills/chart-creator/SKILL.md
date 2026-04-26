---
name: "chart-creator"
description: "智能图表创建器，根据数据特征自动选择最优可视化方案并生成高质量图表。当需要生成数据可视化图表时调用此技能。"
---

# Chart Creator (智能图表创建器)

## 核心职责
- 根据数据特征智能选择图表类型
- 生成美观、专业的ECharts配置
- 支持多种输出格式 (PNG/SVG/交互式)
- 自动优化图表样式和可读性

## 输入参数

```json
{
  "data": [...],
  "suggestedType": "auto|bar|line|pie|scatter|horizontal_bar|table",
  "title": "学院专利分布排名",
  "analysisFocus": "对比各学院的专利产出差异",
  "stylePreference": "professional|modern|minimal|colorful",
  "outputConfig": {
    "format": "png",
    "width": 900,
    "height": 550,
    "dpi": 150
  }
}
```

## 智能类型选择算法

### 数据特征 → 图表映射

| 数据特征 | 推荐类型 | 理由 |
|---------|---------|------|
| 时间序列 + 单数值 | line | 展示趋势变化 |
| 分类 + 数值 (≤15项) | bar | 清晰对比 |
| 分类 + 数值 (>15项) | horizontal_bar | 避免标签重叠 |
| 占比/构成分析 | pie/doughnut | 直观展示比例 |
| 双数值相关性 | scatter | 发现关联关系 |
| 多维对比 | radar | 综合评估 |
| 详细数据展示 | table | 精确数值 |

### 输出格式

```json
{
  "needsSkill": true,
  "skillName": "chart-creator",
  "reason": "图表生成完成",
  "execution": {
    "chartInfo": {
      "actualType": "horizontal_bar",
      "selectionReason": "类别数量较多(15)，横向柱状图更清晰",
      "title": "学院专利分布排名",
      "filePath": "./charts/college_ranking_20260421.png",
      "fileSize": "245KB"
    },
    "echartsOption": { ... },
    "visualInsights": [
      "计算机学院以320项专利领先，占总量的21.3%",
      "前5名学院贡献了68%的专利，呈现明显的头部集中效应"
    ],
    "accessibility": {
      "colorBlindSafe": true,
      "highContrast": true,
      "readableAtSmallSize": true
    }
  }
}
```

## 样式系统

### 配色方案
- **专业蓝**: #1A5276, #2980B9, #3498DB, #85C1E9
- **商务绿**: #196F3D, #27AE60, #2ECC71, #82E0AA
- **科技紫**: #4A235A, #7D3C98, #A569BD, #D2B4DE
- **活力橙**: #B9770E, #D35400, #E67E22, #F0B27A

### 自适应特性
- 根据数据量自动调整字体大小
- 长文本自动省略或旋转
- 数值自动格式化 (K/M/B单位)
- 图例位置优化

## 与其他Skill的交互
← data-analyzer: 接收数据 + 类型建议
← report-planner: 接收可视化要求
→ analysis-writer: 提供图表洞察
→ report-assembler: 提供图表文件路径
