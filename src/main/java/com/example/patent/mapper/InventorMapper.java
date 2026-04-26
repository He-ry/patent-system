package com.example.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.patent.entity.Inventor;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventorMapper extends BaseMapper<Inventor> {
}
