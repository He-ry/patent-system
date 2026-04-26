package com.example.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.patent.entity.PatentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PatentInfoMapper extends BaseMapper<PatentInfo> {

    @Select("SELECT ${field} as name, COUNT(*) as value FROM patent_info GROUP BY ${field} ORDER BY value DESC LIMIT #{limit}")
    List<Map<String, Object>> selectStatisticsByField(String field, int limit);
}
