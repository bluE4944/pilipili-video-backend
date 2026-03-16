package com.pilipili.entity.in;

import lombok.Data;

/**
 * 字典查询/编辑条件
 */
@Data
public class SysDictCondition {
    /**
     * 字典编码
     */
    private String dictCode;

    /**
     * 字典名称
     */
    private String dictName;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否启用：0-禁用，1-启用
     */
    private Integer enabled;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
