# ECharts 渲染服务

基于 Node.js + Puppeteer 的 ECharts 服务端渲染服务，用于生成高质量的统计图表。

## 安装

```bash
cd echarts-server
npm install
```

## 启动服务

```bash
npm start
```

服务默认运行在 `http://localhost:3001`

## API 接口

### 1. 渲染并返回图片

**POST** `/render`

请求体：
```json
{
  "chartType": "bar",
  "title": "图表标题",
  "data": [
    {"name": "类别1", "value": 100},
    {"name": "类别2", "value": 200}
  ],
  "width": 900,
  "height": 550
}
```

响应：PNG 图片二进制数据

### 2. 渲染并保存图片

**POST** `/render-and-save`

请求体：
```json
{
  "chartType": "bar",
  "title": "图表标题",
  "data": [...],
  "filename": "my-chart.png"
}
```

响应：
```json
{
  "success": true,
  "filename": "my-chart.png",
  "path": "/path/to/reports/charts/my-chart.png",
  "url": "/reports/charts/my-chart.png"
}
```

### 3. 渲染并返回 Base64

**POST** `/render-base64`

响应：
```json
{
  "success": true,
  "base64": "iVBORw0KGgo...",
  "dataUrl": "data:image/png;base64,iVBORw0KGgo..."
}
```

### 4. 健康检查

**GET** `/health`

响应：
```json
{
  "status": "ok",
  "timestamp": "2026-04-18T12:00:00.000Z"
}
```

## 支持的图表类型

| 类型 | chartType | 说明 |
|------|-----------|------|
| 柱状图 | `bar` | 垂直柱状图，适合分类对比 |
| 水平柱状图 | `horizontal_bar` | 水平柱状图，适合类别名称较长 |
| 折线图 | `line` | 趋势分析，带面积填充 |
| 饼图 | `pie` | 占比分析，环形图样式 |
| 散点图 | `scatter` | 相关性分析 |

## 自定义 ECharts 配置

也可以直接传递 ECharts option：

```json
{
  "option": {
    "title": { "text": "自定义图表" },
    "xAxis": { "type": "category", "data": ["A", "B", "C"] },
    "yAxis": { "type": "value" },
    "series": [{ "type": "bar", "data": [10, 20, 30] }]
  }
}
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| PORT | 3001 | 服务端口 |
| OUTPUT_DIR | ../reports/charts | 图片输出目录 |

## 注意事项

1. 首次启动会下载 Chromium，可能需要几分钟
2. 建议在生产环境使用 `--no-sandbox` 模式
3. 服务会复用 Puppeteer 实例以提高性能
