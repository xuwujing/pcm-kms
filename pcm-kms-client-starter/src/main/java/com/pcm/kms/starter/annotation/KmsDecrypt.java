package com.pcm.kms.starter.annotation;

import java.lang.annotation.*;

/**
 * 标注在方法参数上，自动解密请求中的加密字段
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KmsDecrypt {
}
