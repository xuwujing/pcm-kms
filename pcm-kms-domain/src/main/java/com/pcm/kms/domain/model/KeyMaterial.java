package com.pcm.kms.domain.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 密钥材料（公钥、私钥、对称密钥）
 * 私钥和对称密钥加密存储
 */
@Data
public class KeyMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 密钥唯一ID */
    private String secretId;
    /** 公钥（非对称） */
    private String publicKey;
    /** 私钥（加密存储） */
    private String privateKey;
    /** 对称密钥（加密存储） */
    private String secretKey;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
