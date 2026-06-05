package com.elderlycare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.elderlycare.entity.Information;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InformationMapper extends BaseMapper<Information> {
}