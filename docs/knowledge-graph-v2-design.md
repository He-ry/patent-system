# 知识图谱 2.0 设计说明

## 目标

现有知识图谱已从“字段展示型图谱”重构为“关系分析型图谱”，核心目标是支持：

- 发明人协作分析
- 技术主题共现分析
- 发明人技术画像分析
- 技术主题到 IPC、应用领域、战略产业的映射分析

本次重构要求：

- 推翻旧的图谱模型
- 不影响专利管理、报告、智能问答等其他模块
- 保留 `/graph` 页面入口，但后端与前端实现全部切换到新模型

## 数据来源

### 主表 `patent_info`

作为专利事实主表，主要使用字段：

- `id`
- `title`
- `application_number`
- `application_date`
- `application_year`
- `grant_date`
- `grant_year`
- `patent_type`
- `legal_status`
- `ipc_main_class`
- `patent_value`
- `technical_value`
- `market_value`
- `cited_patents`
- `cited_in_5_years`
- `claims_count`
- `college`
- `current_assignee`
- `original_assignee`
- `transferor`
- `transferee`
- `license_type`
- `license_count`

### 子表 `patent_info_field`

作为多值属性表，当前图谱 2.0 使用字段类型：

- `inventor`
- `ipc_classification`
- `cpc_classification`
- `technical_subject_classification`
- `application_field_classification`
- `strategic_industry_classification`
- `technical_problem`
- `technical_effect`

## 图谱分层

### 1. 事实层

事实层只保留数据库中可以直接证明的实体和关系。

节点：

- `Patent`
- `Inventor`
- `IPC`
- `CPC`
- `TechTopic`
- `ApplicationField`
- `StrategicIndustry`
- `Problem`
- `Effect`
- `College`
- `AssigneeOrg`

关系：

- `(Inventor)-[:INVENTED]->(Patent)`
- `(Patent)-[:HAS_IPC]->(IPC)`
- `(Patent)-[:HAS_CPC]->(CPC)`
- `(Patent)-[:HAS_TOPIC]->(TechTopic)`
- `(Patent)-[:HAS_APPLICATION_FIELD]->(ApplicationField)`
- `(Patent)-[:BELONGS_TO_INDUSTRY]->(StrategicIndustry)`
- `(Patent)-[:SOLVES]->(Problem)`
- `(Patent)-[:ACHIEVES]->(Effect)`
- `(Patent)-[:BELONGS_TO_COLLEGE]->(College)`
- `(Patent)-[:ASSIGNED_TO_CURRENT]->(AssigneeOrg)`
- `(Patent)-[:ASSIGNED_TO_ORIGINAL]->(AssigneeOrg)`
- `(Patent)-[:TRANSFER_FROM]->(AssigneeOrg)`
- `(Patent)-[:TRANSFER_TO]->(AssigneeOrg)`

### 2. 分析层

分析层基于事实层聚合，用于真正的关系分析。

关系：

- `(Inventor)-[:CO_INVENTS]->(Inventor)`
- `(Inventor)-[:FOCUSES_ON]->(TechTopic)`
- `(TechTopic)-[:CO_OCCURS_WITH]->(TechTopic)`
- `(TechTopic)-[:MAPS_TO_IPC]->(IPC)`
- `(TechTopic)-[:APPLIES_TO]->(ApplicationField)`
- `(TechTopic)-[:BELONGS_TO_INDUSTRY_ANALYSIS]->(StrategicIndustry)`
- `(College)-[:RESEARCHES_TOPIC]->(TechTopic)`
- `(AssigneeOrg)-[:OWNS_TOPIC]->(TechTopic)`

## 节点模型

### Patent

唯一键：

- `id`

主要属性：

- `id`
- `title`
- `applicationNumber`
- `applicationDate`
- `applicationYear`
- `grantDate`
- `grantYear`
- `patentType`
- `legalStatus`
- `ipcMainClass`
- `patentValue`
- `technicalValue`
- `marketValue`
- `citedPatents`
- `citedIn5Years`
- `claimsCount`
- `college`
- `currentAssignee`
- `originalAssignee`

### Inventor

唯一键：

- `name`

主要属性：

- `name`
- `patentCount`
- `grantedCount`
- `highValuePatentCount`
- `avgPatentValue`

### TechTopic

唯一键：

- `name`

主要属性：

- `name`
- `patentCount`
- `highValuePatentCount`
- `avgPatentValue`

### IPC / CPC

唯一键：

- `code`

### ApplicationField / StrategicIndustry / Problem / Effect / College / AssigneeOrg

唯一键：

- `name`

## Neo4j 约束

```cypher
CREATE CONSTRAINT patent_id IF NOT EXISTS FOR (n:Patent) REQUIRE n.id IS UNIQUE;
CREATE CONSTRAINT inventor_name IF NOT EXISTS FOR (n:Inventor) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT ipc_code IF NOT EXISTS FOR (n:IPC) REQUIRE n.code IS UNIQUE;
CREATE CONSTRAINT cpc_code IF NOT EXISTS FOR (n:CPC) REQUIRE n.code IS UNIQUE;
CREATE CONSTRAINT topic_name IF NOT EXISTS FOR (n:TechTopic) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT app_field_name IF NOT EXISTS FOR (n:ApplicationField) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT industry_name IF NOT EXISTS FOR (n:StrategicIndustry) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT problem_name IF NOT EXISTS FOR (n:Problem) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT effect_name IF NOT EXISTS FOR (n:Effect) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT college_name IF NOT EXISTS FOR (n:College) REQUIRE n.name IS UNIQUE;
CREATE CONSTRAINT assignee_name IF NOT EXISTS FOR (n:AssigneeOrg) REQUIRE n.name IS UNIQUE;
```

## 构建流程

### 1. 提取

从 MySQL 中读取：

- `patent_info`
- `patent_info_field`

### 2. 过滤

仅保留图谱 2.0 支持的字段类型，忽略旧图谱依赖但不适合当前分析模型的字段。

### 3. 写入事实层

逐条专利写入 Neo4j，构建事实节点和关系。

### 4. 构建分析层

基于事实层聚合生成协作、偏好、共现、映射等边。

### 5. 回写统计属性

为 `Inventor`、`TechTopic` 等节点回写分析指标，供排序和摘要展示使用。

## 当前接口

### 同步接口

- `POST /api/graph/sync`
- `POST /api/graph/sync/incremental`
- `POST /api/graph/rebuild/analysis`
- `POST /api/graph/rebuild/stats`

### 总览接口

- `GET /api/graph/overview`
- `GET /api/graph/statistics`

### 分析视图接口

- `GET /api/graph/inventor/{name}`
- `GET /api/graph/topic/{name}`
- `GET /api/graph/network/co-inventor`
- `GET /api/graph/network/topic-cooccurrence`
- `GET /api/graph/network/high-value`
- `GET /api/graph/path`

### 搜索接口

- `GET /api/graph/search`

## 前端视图

当前 `/graph` 页面已切换为四种分析视图：

- 发明人协作网络
- 技术主题共现
- 发明人画像
- 主题画像

## 不影响其他模块的边界

本次改造仅替换以下范围：

- `GraphService`
- `GraphController`
- `GraphVisualization`
- 图谱相关 DTO

未修改以下模块逻辑：

- 专利管理
- 报告生成
- 聊天问答
- 模型配置
- 数据导入导出

因此图谱逻辑可独立演进，不会影响其他业务链路。

## 后续建议

下一阶段可继续扩展：

- 增量同步，不再全量清空图谱
- 统一搜索接口
- 高价值专利关系网络
- 主题到产业的多跳路径分析
- 更适合大图场景的前端布局与筛选器
