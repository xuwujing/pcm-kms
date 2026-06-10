package com.pcm.kms.domain.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 密钥元数据
 */
@Data
public class KeyMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 应用组 */
    private String clientGroup;
    /** 关联密钥材料 ID */
    private String secretId;
    /** 是否启用 */
    private Boolean enabled;
    /** 加密算法：rsa/sm2/aes/sm4/sign/md5/sm3 */
    private String algorithm;
    /** 加密类型：symmetric/asymmetric/sign/digester */
    private String cryptoType;
    /** 密钥用途 */
    private String keyPurpose;
    /** 别名（应用组内唯一） */
    private String alias;
    /** 描述 */
    private String description;
    /** 密钥来源：system/manual */
    private String keySource;
    /** 密钥版本 */
    private Integer keyVersion;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
    /** 创建人 */
    private String creator;
}
