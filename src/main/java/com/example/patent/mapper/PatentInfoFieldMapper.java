package com.example.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.patent.entity.PatentInfoField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PatentInfoFieldMapper extends BaseMapper<PatentInfoField> {

    @Select("SELECT field_value FROM patent_info_field WHERE patent_id = #{patentId} AND field_type = #{fieldType} ORDER BY seq")
    List<String> selectFieldValuesByPatentIdAndType(@Param("patentId") String patentId, @Param("fieldType") String fieldType);

    @Select("SELECT DISTINCT patent_id FROM patent_info_field WHERE field_type = 'inventor' AND field_value LIKE CONCAT('%', #{inventorName}, '%')")
    List<String> selectPatentIdsByInventor(@Param("inventorName") String inventorName);

    @Select("SELECT DISTINCT patent_id FROM patent_info_field WHERE field_type = #{fieldType} AND field_value LIKE CONCAT('%', #{keyword}, '%')")
    List<String> selectPatentIdsByFieldTypeLike(@Param("fieldType") String fieldType, @Param("keyword") String keyword);

    @Select("SELECT DISTINCT patent_id FROM patent_info_field WHERE field_value LIKE CONCAT('%', #{keyword}, '%')")
    List<String> selectPatentIdsByAnyFieldLike(@Param("keyword") String keyword);

    @Select("SELECT field_value as name, COUNT(*) as value FROM patent_info_field WHERE field_type = #{fieldType} GROUP BY field_value ORDER BY value DESC LIMIT #{limit}")
    List<Map<String, Object>> selectStatisticsByFieldType(@Param("fieldType") String fieldType, @Param("limit") Integer limit);
}
