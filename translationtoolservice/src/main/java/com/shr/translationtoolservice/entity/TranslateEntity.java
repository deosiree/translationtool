package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 
 * @TableName t_translate
 */
@TableName(value ="t_translate")
@Data
public class TranslateEntity implements Serializable {
    /**
     * 主键
     */
    @TableField(value = "id")
    private String id;

    /**
     * 词条
     */
    @TableField(value = "entry")
    private String entry;

    /**
     * 翻译
     */
    @TableField(value = "translate")
    private String translate;

    /**
     * 唯一属性
     */
    @TableField(value = "`unique`")
    private String unique;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 翻译类型
     */
    @TableField(value = "type")
    private String type;

    /**
     * 可见范围
     */
    @TableField(value = "visual_range")
    private String visualRange;

    /**
     * 公共库（0否，1是）
     */
    @TableField(value = "public_state")
    private Integer publicState;

    /**
     * 删除状态（0否1是）
     */
    @TableField(value = "delete_state")
    private Integer deleteState;

    /**
     * 翻译状态（创建，待翻译，已翻译，已审核）
     */
    @TableField(value = "translate_state")
    private String translateState;


    /**
     * 版本id
     */
    @TableField(value = "version_id")
    private String versionID;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}