package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据字典项实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sys_dict_item")
@ApiModel("数据字典项")
public class SysDictItem extends BaseEntity {

    /**
     * 字典编码
     */
    @ApiModelProperty("字典编码")
    private String dictCode;

    /**
     * 字典项值
     */
    @ApiModelProperty("字典项值")
    private String itemValue;

    /**
     * 字典项名称
     */
    @ApiModelProperty("字典项名称")
    private String itemLabel;

    /**
     * 排序序号
     */
    @ApiModelProperty("排序序号")
    private Integer sortOrder;

    /**
     * 是否启用：0-禁用，1-启用
     */
    @ApiModelProperty("是否启用：0-禁用，1-启用")
    private Integer enabled;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String remark;
}
