package com.pilipili.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    private Long id;

    /**
     * 创建者id
     */
    @TableField(value = "create_id")
    private Long createId;

    /**
     * 创建者名
     */
    @TableField(value = "create_name")
    private String createName;

    /**
     * 创建时间
     */
    @JsonFormat( pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新者id
     */
    @TableField(value = "update_id")
    private Long updateId;

    /**
     * 更新者名
     */
    @TableField(value = "update_name")
    private String updateName;

    /**
     * 更新时间
     */
    @JsonFormat( pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time")
    private Date updateTime;

    public BaseEntity() {
    }

    public BaseEntity(Long id, Long createId, String createName, Date createTime, Long updateId, String updateName, Date updateTime) {
        this.id = id;
        this.createId = createId;
        this.createName = createName;
        this.createTime = createTime;
        this.updateId = updateId;
        this.updateName = updateName;
        this.updateTime = updateTime;
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
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof BaseEntity)) {return false;}
        BaseEntity that = (BaseEntity) o;
        return getId().equals(that.getId()) &&
                Objects.equals(getCreateId(), that.getCreateId()) &&
                Objects.equals(getCreateName(), that.getCreateName()) &&
                Objects.equals(getCreateTime(), that.getCreateTime()) &&
                Objects.equals(getUpdateId(), that.getUpdateId()) &&
                Objects.equals(getUpdateName(), that.getUpdateName()) &&
                Objects.equals(getUpdateTime(), that.getUpdateTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCreateId(), getCreateName(), getCreateTime(), getUpdateId(), getUpdateName(), getUpdateTime());
    }
}
