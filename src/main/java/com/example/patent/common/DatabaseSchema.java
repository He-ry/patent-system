package com.example.patent.common;

public class DatabaseSchema {

    private DatabaseSchema() {}

    public static final String PATENT_INFO_TABLE = """
            ### patent_info (主表)
            | 字段名 | 类型 | 说明 |
            |---|---|---|
            | id | varchar(50) | 主键ID |
            | serial_number | varchar(50) | 序号 / 数据编号 |
            | title | varchar(255) | 专利标题 |
            | application_date | date | 申请日 |
            | inventor_count | int | 发明人数量 |
            | college | varchar(100) | 所属学院 / 单位 |
            | legal_status | varchar(20) | 法律状态 |
            | patent_type | varchar(20) | 专利类型：发明、实用新型、外观设计 |
            | application_number | varchar(50) | 申请号 |
            | grant_date | date | 授权日 |
            | current_assignee | varchar(255) | 当前申请人 |
            | original_assignee | varchar(255) | 原始申请人 |
            | agency | varchar(255) | 代理机构 |
            | current_assignee_province | varchar(50) | 当前申请人省份 |
            | original_assignee_province | varchar(50) | 原始申请人省份 |
            | original_assignee_type | varchar(50) | 原始申请人类型 |
            | current_assignee_type | varchar(50) | 当前申请人类型 |
            | application_year | int | 申请年份 |
            | publication_date | date | 公开日 |
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
            | deleted | tinyint | 逻辑删除标志，0未删除，1已删除 |
            """;

    public static final String PATENT_INFO_FIELD_TABLE = """
            ### patent_info_field (字段表)
            | 字段名 | 类型 | 说明 |
            |---|---|---|
            | id | varchar(50) | 主键 |
            | patent_id | varchar(50) | 专利ID(外键) |
            | field_type | enum | 字段类型 |
            | field_value | varchar(255) | 字段值 |
            | seq | int | 顺序 |
            
            field_type 枚举值：
            - inventor：发明人
            - technical_problem：技术问题短语
            - technical_effect：技术功效短语
            - ipc_classification：IPC分类号
            - cpc_classification：CPC分类号
            - technical_subject_classification：技术主题分类
            - application_field_classification：应用领域分类
            - ipc_main_class_interpretation：IPC主分类号释义
            - strategic_industry_classification：战略新兴产业分类
            - technical_field：技术领域
            """;

    public static final String FULL_SCHEMA = PATENT_INFO_TABLE + "\n" + PATENT_INFO_FIELD_TABLE;

    public static final String TABLE_RELATION = """
            表关系说明：
            - patent_info_field.patent_id 关联 patent_info.id
            - 查询发明人等扩展字段时需要 JOIN patent_info_field 表
            - 示例：SELECT p.title, f.field_value AS inventor 
                    FROM patent_info p 
                    JOIN patent_info_field f ON p.id = f.patent_id 
                    WHERE f.field_type = 'inventor'
            """;
}
