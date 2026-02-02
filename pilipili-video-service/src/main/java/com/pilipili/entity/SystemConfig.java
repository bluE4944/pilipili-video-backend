package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置实体类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_system_config")
@ApiModel("系统配置实体")
public class SystemConfig extends BaseEntity {

    /**
     * 配置键
     */
    @ApiModelProperty("配置键")
    private String configKey;

    /**
     * 配置值
     */
    @ApiModelProperty("配置值")
    private String configValue;

    /**
     * 配置描述
     */
    @ApiModelProperty("配置描述")
    private String description;

    /**
     * 配置类型：1-字符串，2-数字，3-布尔值，4-JSON
     */
    @ApiModelProperty("配置类型：1-字符串，2-数字，3-布尔值，4-JSON")
    private Integer configType;
}
