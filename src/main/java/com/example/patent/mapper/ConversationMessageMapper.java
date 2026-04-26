package com.example.patent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.patent.entity.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {
}