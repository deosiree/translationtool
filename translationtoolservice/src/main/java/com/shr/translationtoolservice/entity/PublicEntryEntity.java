package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.shr.translationtoolservice.util.DefaultValue;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @title publicEntryEntity
 * @create 2024/4/8 15:13
 * @description <TODO description class purpose>
 **/
@Data
public class PublicEntryEntity {
    /**
     * 是否升级词条（f否，t是）
     */
    @TableField(exist = false)
    private boolean upgrade =false;
    //重复词条
    @TableField(exist = false)
    private List<EntryInfoEntity> children;
    @TableField(value = "parent_id")
    @DefaultValue("")
    private String parentID;

    @TableField(exist = false)
    @DefaultValue("")
    private String tableName;

    @TableField(exist = false)
    @DefaultValue("")
    private String productName;
    /**
     * 写入di 文件名
     */
    @TableField(value = "di_file_name")
    private String diFileName;

    /**
     * 主键
     */
    @TableId(value = "id")
    @DefaultValue("")
    private String id;

    /**
     * abbr
     */
    @TableField(value = "abbr")
    @DefaultValue("")
    private String abbr;

    /**
     * 词条
     */
    @TableField(value = "entry")
    @DefaultValue("")
    private String entry;

    /**
     *
     */
    @TableField(value = "entry_length")
    @DefaultValue("")
    private Integer entryLength;

    /**
     * 中文释义
     */
    @TableField(value = "chinese_interpretation")
    @DefaultValue("")
    private String chineseInterpretation;

    /**
     * 英文释义
     */
    @TableField(value = "english_interpretation")
    @DefaultValue("")
    private String englishInterpretation;

    /**
     * 词条来源
     */
    @TableField(value = "entry_source")
    @DefaultValue("")
    private String entrySource;

    /**
     * 词条状态(0新建，
     * 1词条待审核，
     * 2词条审核不通过
     * 3词条审核通过4已归档)
     */
    @TableField(value = "entry_state")
    private Integer entryState;

    /**
     * 修改人
     */
    @TableField(value = "`update`")
    @DefaultValue("")
    private String update;

    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 产品版本
     */
    @TableField(value = "version_id")
    @DefaultValue("")
    private String versionID;

    /**
     * 词条标签
     */
    @TableField(value = "entry_label")
    @DefaultValue("")
    private String entryLabel;

    /**
     * 词性备注
     */
    @TableField(value = "part_of_speech")
    @DefaultValue("")
    private String partOfSpeech;

    /**
     * 词条所属分类
     */
    @TableField(value = "classify_id")
    @DefaultValue("")
    private String classifyId;

    /**
     * 备注
     */
    @TableField(value = "remark")
    @DefaultValue("")
    private String remark;

    /**
     * 任务id
     */
    @TableField(value = "task_id")
    @DefaultValue("")
    private String taskId;

    /**
     * 西文翻译长度
     */
    @TableField(value = "spa_char_length")
    @DefaultValue("")
    private Integer spaCharLength;

    /**
     * 最大译长度
     */
    @TableField(value = "max_length")
    private Integer maxLength;
    /**
     * 公共库（0否 1是）
     */
    @TableField(value = "is_public")
    private Integer isPublic;

    /**
     * 删除状态（0否 1是）
     */
    @TableField(value = "is_delete")
    private Integer isDelete;

    /**
     * 导入类型
     */
    @TableField(value = "import_type")
    @DefaultValue("")
    private String importType;

    /**
     * 词条版本
     */
    @TableField(value = "entry_version")
    private Integer entryVersion;

    /**
     * 产品ID
     */
    @TableField(value = "product_id")
    @DefaultValue("")
    private String productID;

    /**
     * 一级分类
     */
    @TableField(value = "classfy1")
    @DefaultValue("")
    private String classfy1;

    /**
     * 二级分类
     */
    @TableField(value = "classfy2")
    @DefaultValue("")
    private String classfy2;

    /**
     * tag
     * db ：        nodeName_appName_dbName+tbName
     */
    @TableField(value = "tag")
    private String tag;

    /**
     * 翻译
     */
    private String translate;

    /**
     * 翻译状态
     */
    private String translateState;

    /**
     * 存在情况（0否 1是） 导入时使用这个判断是否存在用
     */
    @TableField(value = "is_exist")
    private Integer isExist;

    /**
     * 审核意见
     */
    @TableField(value = "audit_suggess")
    @DefaultValue("")
    private String auditSuggess;

    /**
     * 词条版本类型区分
     */
    @TableField(value = "entry_version_type")
    @DefaultValue("")
    private String entryVersionID;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
