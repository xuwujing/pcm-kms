package com.pcm.kms.domain.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户
 */
@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 用户名 */
    private String username;
    /** 密码（MD5/BCrypt 加密） */
    private String password;
    /** 昵称 */
    private String nickname;
    /** 是否启用 */
    private Boolean enabled;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
