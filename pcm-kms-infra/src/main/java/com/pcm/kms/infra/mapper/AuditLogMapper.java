package com.pcm.kms.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pcm.kms.domain.model.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
