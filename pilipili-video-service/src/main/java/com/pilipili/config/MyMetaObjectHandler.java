package com.pilipili.config;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.system.UserInfo;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.pilipili.utils.LogicStatus;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author Li
 * @version 1.0
 * @ClassName MyMetaObjectHandler
 * @Description
 * @since 2023/4/14 14:26
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入元对象字段填充（用于插入时对公共字段的填充）
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        Date date = new Date();
        //创建
        Object createUserId = getFieldValByName(getCreateIdFieldName(), metaObject);
        Object createTime = getFieldValByName(getCreateTimeFieldName(), metaObject);
        Object createUser = getFieldValByName(getCreateNameFieldName(), metaObject);
        Object logicDel = getFieldValByName(getLogicDelFieldName(), metaObject);
        /*
         *   this.setFieldValByName(arg1,arg2,arg3 )
         *   arg1: 自动填充的字段名称
         *   arg2: 自动填充 的值
         *   arg3: metaObject固定写法
         */
        if(ObjectUtil.isNotNull(createUserId)){
            this.setFieldValByName(getCreateIdFieldName(), "-1", metaObject);
        }
        if(ObjectUtil.isNotNull(createTime)){
            this.setFieldValByName(getCreateTimeFieldName(), date, metaObject);
        }
        if(ObjectUtil.isNotNull(createUser)){
            this.setFieldValByName(getCreateNameFieldName(), "sys", metaObject);
        }
        if(ObjectUtil.isNotNull(logicDel)){
            this.setFieldValByName(getCreateNameFieldName(), LogicStatus.ENABLE.getCode(), metaObject);
        }

        //更新
        Object updateUserId = getFieldValByName(getUpdateIdFieldName(), metaObject);
        Object updateTime = getFieldValByName(getUpdateTimeFieldName(), metaObject);
        Object updateUser = getFieldValByName(getUpdateNameFieldName(), metaObject);

        if(ObjectUtil.isNotNull(updateUserId)){
            this.setFieldValByName(getUpdateIdFieldName(), "-1", metaObject);
        }
        if(ObjectUtil.isNotNull(updateTime)){
            this.setFieldValByName(getUpdateTimeFieldName(), date, metaObject);
        }
        if(ObjectUtil.isNotNull(updateUser)){
            this.setFieldValByName(getUpdateNameFieldName(), "sys", metaObject);
        }

    }

    /**
     * 更新元对象字段填充
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        Date date = new Date();
        //更新
        Object updateUserId = getFieldValByName(getUpdateIdFieldName(), metaObject);
        Object updateTime = getFieldValByName(getUpdateTimeFieldName(), metaObject);
        Object updateUser = getFieldValByName(getUpdateNameFieldName(), metaObject);

        if(ObjectUtil.isNotNull(updateUserId)){
            this.setFieldValByName(getUpdateIdFieldName(), "-1", metaObject);
        }
        if(ObjectUtil.isNotNull(updateTime)){
            this.setFieldValByName(getUpdateTimeFieldName(), date, metaObject);
        }
        if(ObjectUtil.isNotNull(updateUser)){
            this.setFieldValByName(getUpdateNameFieldName(), "sys", metaObject);
        }

    }

    /**
     * 创建时间字段名
     */
    private String getCreateTimeFieldName(){
        return "createTime";
    }

    /**
     * 创建人字段名
     */
    private String getCreateNameFieldName(){
        return "createName";
    }

    /**
     * 创建人id字段名
     */
    private String getCreateIdFieldName(){
        return "createId";
    }

    /**
     * 更新时间
     */
    private String getUpdateTimeFieldName(){
        return "updateTime";
    }

    /**
     * 更新人字段名
     */
    private String getUpdateNameFieldName(){
        return "updateName";
    }

    /**
     * 更新人id字段名
     */
    private String getUpdateIdFieldName(){
        return "updateId";
    }

    /**
     * 逻辑删除字段名
     */
    private String getLogicDelFieldName(){
        return "logicDel";
    }

}
