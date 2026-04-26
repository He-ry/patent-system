---
name: "sql-generator"
description: "Generates SQL queries for patent database. Invoke when user asks to query, count, aggregate, or analyze patent data using SQL."
---

# Patent Database SQL Generator

用于根据用户自然语言问题，生成高质量、可执行、规范的 MySQL SQL。


## Database Schema

### patent_info (主表)
| 字段名 | 类型 | 说明 |
|---|---|---|
| id | varchar(50) | 主键ID |
| serial_number | varchar(50) | 序号 / 数据编号 |
| title | varchar(255) | 专利标题 |
| application_date | date | 申请日 |
| inventor_count | int | 发明人数量 |
| college | varchar(100) | 所属学院 / 单位 |
| legal_status | varchar(20) | 法律状态 / 法律事件 |
| patent_type | varchar(20) | 专利类型，例如：发明、实用新型、外观设计 |
| application_number | varchar(50) | 申请号 |
| grant_date | date | 授权日 |
| current_assignee | varchar(255) | 当前申请人 / 当前专利权人 |
| original_assignee | varchar(255) | 原始申请人 / 原始专利权人 |
| agency | varchar(255) | 代理机构 |
| current_assignee_province | varchar(50) | 当前申请人省份 / 州 |
| original_assignee_province | varchar(50) | 原始申请人省份 / 州 |
| original_assignee_type | varchar(50) | 原始申请人类型 |
| current_assignee_type | varchar(50) | 当前申请人类型 |
| application_year | int | 申请年份 |
| publication_date | date | 公开 / 公告日 |
| grant_year | int | 授权年份 |
| ipc_main_class | varchar(50) | IPC主分类号 |
| application_field_classification | varchar(255) | 应用领域分类 |
| expiry_date | date | 失效日 |
| simple_family_cited_patents | int | 简单同族被引用专利数量 |
| cited_patents | int | 被引用专利数量 |
| cited_in_5_years | int | 5年内被引用数量 |
| claims_count | int | 权利要求数量 |
| patent_value | decimal(15,2) | 专利价值 |
| technical_value | decimal(15,2) | 技术价值 |
| market_value | decimal(15,2) | 市场价值 |
| transfer_effective_date | date | 权利转移生效日 |
| license_type | varchar(50) | 许可类型 |
| license_count | int | 许可次数 |
| license_effective_date | date | 许可生效日 |
| transferor | varchar(255) | 转让人 |
| transferee | varchar(255) | 受让人 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |
| deleted | tinyint | 逻辑删除标志，0 表示未删除，1 表示已删除 |

### patent_info_field (字段表)
| Column | Type | Description                                                                                                                          |
|--------|------|--------------------------------------------------------------------------------------------------------------------------------------|
| id | varchar(50) | 主键                                                                                                                                   |
| patent_id | varchar(50) | 专利ID(外键)                                                                                                                             |
| field_type | enum | 字段类型见下方“多值字段枚举” |
| field_value | varchar(255) | 字段值                                                                                                                                  |
| seq | int | 顺序                                                                                                                                   |

多值字段统一存储在 patent_info_field：
- inventor：发明人，按 `|` 拆分
- technical_problem：[标]技术问题短语，按 `|` 拆分
- technical_effect：[标]技术功效短语，按 `|` 拆分
- ipc_classification：IPC分类号，按 `|` 拆分
- cpc_classification：CPC分类号，按 `|` 拆分
- technical_subject_classification：技术主题分类，按 `|` 拆分
- application_field_classification：应用领域分类，按 `|` 拆分
- ipc_main_class_interpretation：IPC主分类号(部)释义，按 `；` 或 `;` 拆分
- strategic_industry_classification：战略新兴产业分类，按 `、` 拆分
- technical_field：技术领域，保留为兼容字段，按 `|` 拆分


## Usage Guidelines

1、所有查询必须添加 WHERE deleted = 0，用于过滤逻辑删除数据
必须使用表别名：
pi 表示 patent_info
pif 表示 patent_info_field
多值字段必须查询 patent_info_field，不要从 patent_info 主表查询这些字段。
示例：统计 IPC 分类号使用 `JOIN patent_info_field pif ON pif.patent_id = pi.id AND pif.field_type = 'ipc_classification'`。
日期字段包括：
application_date（申请日）
grant_date（授权日）
create_time（创建时间）
对于可能返回大量数据的查询，必须使用 LIMIT 限制返回条数
对于统计类查询：
必须使用 GROUP BY 分组
并使用 ORDER BY COUNT 进行排序
重要：查询专利详情时，应尽可能返回完整字段，包括但不限于：


必须使用 GROUP_CONCAT 子查询获取发明人信息，确保返回完整的发明人列表

## Response Format

Output the SQL query directly with brief explanation.
