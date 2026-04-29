# 专利分析系统

基于 Spring Boot + React + Neo4j + Elasticsearch 的专利数据管理与知识图谱分析平台。

## 技术架构

| 层级 | 技术栈 |
|------|--------|
| **后端** | Spring Boot 3.2 + Java 21 + Maven |
| **前端** | React 18 + TypeScript + Vite + Tailwind CSS |
| **图数据库** | Neo4j 5.18 — 存储专利知识图谱、发明人协作网络 |
| **搜索引擎** | Elasticsearch 8.15 — 全文检索、专利查重 |
| **关系数据库** | MySQL 8.0 — 用户、专利元数据、系统配置 |
| **图表服务** | ECharts + Puppeteer — 服务端图表渲染导出 |
| **3D 可视化** | Three.js (React Three Fiber) — 专利图谱 3D 展示 |
| **容器化** | Docker + Docker Compose — 一键部署 |

## 项目结构

```
patent-system/
├── src/                     # Spring Boot 后端源码
│   └── main/java/com/example/patent/
│       ├── controller/      # REST API 控制器
│       ├── service/         # 业务逻辑层
│       ├── graph/           # Neo4j 图谱查询
│       ├── mapper/          # MyBatis 数据访问
│       ├── report/          # 报告生成
│       ├── entity/          # 实体类
│       └── config/          # Spring 配置
├── patent-frontend/         # React 前端
│   └── src/
│       ├── components/      # UI 组件
│       ├── chat/            # AI 对话模块
│       └── api.ts           # API 接口定义
├── echarts-server/          # ECharts 服务端渲染 (Node.js)
├── docker/                  # Docker 离线部署包
│   ├── images/              # 离线镜像包
│   ├── initdb/              # 数据库初始化脚本
│   ├── deploy.sh            # Linux 部署脚本
│   ├── deploy-offline.sh    # 离线部署脚本
│   └── docker-compose.yml   # 容器编排
├── config/                  # 应用配置文件
├── Dockerfile               # 应用多阶段构建
└── docker-compose.yml       # 开发环境容器编排
```

## 核心功能

- **专利数据管理** — 专利信息录入、导入、查询与统计分析
- **知识图谱** — 基于 Neo4j 的发明人协作网络、技术领域关联分析
- **全文检索** — Elasticsearch 驱动的专利全文搜索与相似度匹配
- **图表报告** — 服务端 ECharts 渲染，支持 PDF/图片导出
- **3D 可视化** — Three.js 驱动的专利图谱 3D 交互展示
- **AI 辅助** — 集成 LLM 对话，智能专利解读与分析

## 快速部署

### 离线环境（推荐）

```bash
tar -xzf patent-system-offline-*.tar.gz
cd docker
bash deploy.sh
```

### 在线环境

```bash
# 1. 构建应用镜像
docker build -t patent-system-app:1.0.0 .

# 2. 启动全部服务
cd docker && bash deploy.sh
```

### 开发环境

```bash
# 后端
mvn spring-boot:run

# 前端
cd patent-frontend && npm run dev

# 图表服务
cd echarts-server && npm start
```

## 访问地址

部署完成后访问: **http://localhost:3333**

## 环境要求

| 组件 | 版本要求 |
|------|----------|
| Docker Engine | ≥ 24.x |
| Docker Compose | ≥ 2.20 |
| 内存 | ≥ 8GB (推荐 16GB) |
| 磁盘 | ≥ 30GB |
| JDK (开发) | 21+ |
| Node.js (开发) | 22+ |
