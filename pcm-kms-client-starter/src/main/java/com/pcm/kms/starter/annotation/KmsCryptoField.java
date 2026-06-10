package com.pcm.kms.starter.annotation;

import java.lang.annotation.*;

/**
 * 标注在字段上，指定该字段需要通过 KMS 加解密
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KmsCryptoField {
    /** 密钥别名 */
    String alias();
}
