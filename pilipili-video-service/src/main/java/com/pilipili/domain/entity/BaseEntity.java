package com.pilipili.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * @author Liam
 * @version 1.0
 * @date 2023/3/10 22:26
 */
@JsonSerialize
public class BaseEntity implements Serializable {
    private static final Long serialVersionUID = 1L;

    /**
     * id 主键
     */
    @TableId(
            value = "id",
            type = IdType.ASSIGN_ID
    )
    @ApiModelProperty("id 主键")
    private Long id;

    /**
     * 创建者id
     */
    @ApiModelProperty("创建者id")
    @TableField(value = "create_id",fill = FieldFill.INSERT)
    private Long createId;

    /**
     * 创建者名
     */
    @ApiModelProperty("创建者名")
    @TableField(value = "create_name",fill = FieldFill.INSERT)
    private String createName;

    /**
     * 创建时间
     */
    @JsonFormat( pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("创建时间")
    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新者id
     */
    @ApiModelProperty("更新者id")
    @TableField(value = "update_id",fill = FieldFill.INSERT_UPDATE)
    private Long updateId;

    /**
     * 更新者名
     */
    @ApiModelProperty("更新者名")
    @TableField(value = "update_name",fill = FieldFill.INSERT_UPDATE)
    private String updateName;

    /**
     * 更新时间
     */
    @JsonFormat( pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("更新时间")
    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(value = "logic_del",fill = FieldFill.INSERT)
    @ApiModelProperty("是否删除标识 0：未删除  1：删除")
    @TableLogic(value = "0",delval = "1")
    private Integer logicDel;

    public BaseEntity() {
    }

    public BaseEntity(Long id, Long createId, String createName, Date createTime, Long updateId, String updateName, Date updateTime, Integer logicDel) {
        this.id = id;
        this.createId = createId;
        this.createName = createName;
        this.createTime = createTime;
        this.updateId = updateId;
        this.updateName = updateName;
        this.updateTime = updateTime;
        this.logicDel = logicDel;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCreateId() {
        return createId;
    }

    public void setCreateId(Long createId) {
        this.createId = createId;
    }

    public String getCreateName() {
        return createName;
    }

    public void setCreateName(String createName) {
        this.createName = createName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateId() {
        return updateId;
    }

    public void setUpdateId(Long updateId) {
        this.updateId = updateId;
    }

    public String getUpdateName() {
        return updateName;
    }

    public void setUpdateName(String updateName) {
        this.updateName = updateName;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getLogicDel() {
        return logicDel;
    }

    public void setLogicDel(Integer logicDel) {
        this.logicDel = logicDel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof BaseEntity)) { return false; }
        BaseEntity that = (BaseEntity) o;
        return getId().equals(that.getId()) &&
                Objects.equals(getCreateId(), that.getCreateId()) &&
                Objects.equals(getCreateName(), that.getCreateName()) &&
                Objects.equals(getCreateTime(), that.getCreateTime()) &&
                Objects.equals(getUpdateId(), that.getUpdateId()) &&
                Objects.equals(getUpdateName(), that.getUpdateName()) &&
                Objects.equals(getUpdateTime(), that.getUpdateTime()) &&
                getLogicDel().equals(that.getLogicDel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCreateId(), getCreateName(), getCreateTime(), getUpdateId(), getUpdateName(), getUpdateTime(), getLogicDel());
    }

    @Override
    public String toString() {
        return "BaseEntity{" +
                "id=" + id +
                ", createId=" + createId +
                ", createName='" + createName + '\'' +
                ", createTime=" + createTime +
                ", updateId=" + updateId +
                ", updateName='" + updateName + '\'' +
                ", updateTime=" + updateTime +
                ", logicDel=" + logicDel +
                '}';
    }
}
