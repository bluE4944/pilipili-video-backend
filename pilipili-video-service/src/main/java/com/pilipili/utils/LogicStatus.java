package com.pilipili.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 逻辑删除状态枚举
 * @author Li
 * @version 1.0
 * @ClassName EnableStatus
 * @Description
 * @since 2023/4/14 15:44
 */
@AllArgsConstructor
public enum LogicStatus {
    /**
     * 启用
     */
    ENABLE(0,"启用"),
    /**
     * 禁用
     */
    DISABLED(1,"禁用");

    /**
     * 编码
     */
    @Getter
    private final Integer code;
    /**
     * 描述
     */
    @Getter
    private final String desc;
}
