package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 本地文件夹配置实体类
 * @author Liam
 * @version 1.0
 * @date 2023/3/22 22:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_local_folder_config")
@ApiModel("本地文件夹配置实体")
public class LocalFolderConfig extends BaseEntity {

    /**
     * 配置名称
     */
    @ApiModelProperty("配置名称")
    private String configName;

    /**
     * 机器IP或主机名
     */
    @ApiModelProperty("机器IP或主机名")
    private String machineIp;

    /**
     * 文件夹路径
     */
    @ApiModelProperty("文件夹路径")
    private String folderPath;

    /**
     * 是否启用：0-禁用，1-启用
     */
    @ApiModelProperty("是否启用：0-禁用，1-启用")
    private Integer enabled;

    /**
     * 扫描间隔（分钟）
     */
    @ApiModelProperty("扫描间隔（分钟）")
    private Integer scanInterval;

    /**
     * 最后扫描时间
     */
    @ApiModelProperty("最后扫描时间")
    private java.util.Date lastScanTime;

    /**
     * 扫描状态：0-未扫描，1-扫描中，2-扫描完成，3-扫描失败
     */
    @ApiModelProperty("扫描状态：0-未扫描，1-扫描中，2-扫描完成，3-扫描失败")
    private Integer scanStatus;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String remark;
}
