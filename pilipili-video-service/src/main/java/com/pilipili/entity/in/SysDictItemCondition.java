package com.pilipili.entity.in;

import lombok.Data;

/**
 * 字典项查询/编辑条件
 */
@Data
public class SysDictItemCondition {
    /**
     * 字典项值
     */
    private String itemValue;

    /**
     * 字典项名称
     */
    private String itemLabel;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 是否启用：0-禁用，1-启用
     */
    private Integer enabled;

    /**
     * 备注
     */
    private String remark;
}
