package com.pcm.kms.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pcm.kms.domain.model.KeyMaterial;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KeyMaterialMapper extends BaseMapper<KeyMaterial> {
}
