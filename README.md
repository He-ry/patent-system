# 专利智能分析系统

基于 AI 的专利数据分析与可视化平台，集成知识图谱、多维统计分析、智能对话与报告生成能力。

## 功能概览

### 智能助手

支持自然语言交互，自动分析专利数据并生成专业分析报告。系统内置多轮对话、意图识别与流式输出。

![智能助手](docs/images/ai-chat.png)

### 专利知识图谱

基于 Neo4j 构建专利实体关系网络，支持多维度筛选（申请人、发明人、技术领域等），直观展示技术关联与演化路径。

![专利知识图谱](docs/images/knowledge-graph.png)

### 3D 文本词云分析

动态词云可视化，展示高频关键词分布，支持按申请人/发明人维度切换。

![3D 词云](docs/images/word-cloud.png)

### 多维图统计分析

提供柱状图、饼图、趋势折线图等多维度统计视图，支持自定义时间范围与分析维度。

![多维统计分析](docs/images/statistics.png)

## 技术栈

| 层级 | 技术选型 |
|------|----------|
| **后端框架** | Spring Boot 3.2 + Java 21 |
| **前端框架** | React 18 + TypeScript + Ant Design |
| **数据库** | MySQL 8（业务数据） |
| **图数据库** | Neo4j（知识图谱） |
| **搜索引擎** | Elasticsearch 8（全文检索） |
| **AI 引擎** | OpenAI API 兼容接口（DeepSeek 等） |
| **向量模型** | Ollama 本地部署 BGE-M3 |
| **图表渲染** | ECharts + Puppeteer（服务端） |
| **报告生成** | Apache POI（Word/PDF） |
| **容器化** | Docker + Docker Compose |

## 系统架构

```
┌─────────────────────────────────────────────┐
│              React 前端 (SPA)                │
│   智能助手 │ 知识图谱 │ 统计分析 │ 报告中心    │
└──────────────────┬──────────────────────────┘
                   │ HTTP / SSE
┌──────────────────▼──────────────────────────┐
│          Spring Boot 后端                    │
│  ┌─────────┐ ┌─────────┐ ┌──────────────┐   │
│  │ChatCtrl │ │GraphCtrl│ │ReportCtrl    │   │
│  └────┬────┘ └────┬────┘ └──────┬───────┘   │
│       │           │              │           │
│  ┌────▼────┐ ┌────▼────┐ ┌──────▼───────┐   │
│  │OpenAiSvc│ │GraphSvc │ │ReportBuilder │   │
│  └────┬────┘ └────┬────┘ └──────┬───────┘   │
└───────┼───────────┼──────────────┼───────────┘
        │           │              │
   ┌────▼───┐  ┌────▼───┐  ┌─────▼─────┐
   │ DeepSeek│  │ Neo4j  │  │ECharts Srv│
   │  API   │  │        │  │(Puppeteer)│
   └────────┘  └────────┘  └───────────┘
        │           │              │
   ┌────▼───┐  ┌────▼───┐  ┌─────▼─────┐
   │MySQL   │  │Neo4j   │  │Elasticsearch│
   └────────┘  └────────┘  └────────────┘
```

## 快速开始

### 环境要求

- Docker & Docker Compose
- Node.js >= 18（本地开发）
- JDK >= 21（本地开发）

### 配置文件

复制配置模板并填入实际值：

```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
cp config/ai-model.json.example config/ai-model.json
```

编辑 `application.yml`，配置以下关键项：

```yaml
spring:
  datasource:
    url: jdbc:mysql://host:3306/patent_db
    username: root
    password: YOUR_MYSQL_PASSWORD
  neo4j:
    uri: bolt://neo4j-host:7687
    authentication:
      username: neo4j
      password: YOUR_NEO4J_PASSWORD
elasticsearch:
  host: es-host
  port: 9200
openai:
  api-key: sk-YOUR_API_KEY
  base-url: https://api.deepseek.com
  model: deepseek-chat
```

编辑 `config/ai-model.json`（运行时可通过 Settings 页面修改）：

```json
{
  "apiKey": "sk-YOUR_API_KEY",
  "modelName": "deepseek-chat",
  "baseUrl": "https://api.deepseek.com"
}
```

### Docker 部署（推荐）

```bash
# 构建镜像
docker build -t patent-system-app:1.0.0 .

# 启动服务
docker compose up -d
```

访问地址：`http://localhost:3333`

### 本地开发

```bash
# 启动后端（端口 8080）
mvn spring-boot:run

# 启动前端（端口 5173）
cd patent-frontend && npm install && npm run dev

# 启动 ECharts 渲染服务（端口 3001）
cd echarts-server && npm install && node server.js
```

## 项目结构

```
patent-system/
├── src/main/java/com/example/patent/
│   ├── config/          # 配置类（ES、Neo4j、OpenAI、Swagger）
│   ├── controller/      # REST 控制器
│   ├── service/         # 业务服务层
│   │   └── impl/        # 服务实现
│   ├── report/          # 报告生成模块
│   │   ├── controller/
│   │   ├── domain/      # 报告领域对象
│   │   └── service/     # 报告构建器、编排器、AI生成器
│   ├── skill/           # Skill 插件体系
│   ├── entity/          # 数据库实体
│   ├── mapper/          # MyBatis-Plus Mapper
│   ├── dto/             # 数据传输对象
│   └── vo/              # 视图对象
├── patent-frontend/     # React 前端源码
├── echarts-server/      # ECharts 服务端渲染
├── docker-entrypoint.sh # 容器启动脚本
└── docs/                # 项目文档
```

## 核心功能说明

### AI 对话引擎
- 流式 SSE 输出，实时显示回复内容
- 多轮上下文记忆，支持会话管理
- SQL 安全校验，防止注入攻击
- 支持 JSON 结构化输出模式

### 知识图谱
- 动态布局算法（力导向）
- 节点/边类型着色区分
- 关系路径高亮追踪
- 统计面板实时更新

### 报告生成
- 自动识别用户意图（分析/对比/预测）
- 多章节结构化输出
- 内嵌图表（Word/PDF）
- 支持导出预览与下载

### 配置热更新
- 前端 Settings 页面修改 AI 参数后立即生效
- 无需重启容器，30 秒内自动刷新

## License

MIT
