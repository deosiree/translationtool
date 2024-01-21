package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName t_entry_info
 */
@TableName(value ="t_entry_info")
@Data
public class EntryInfoEntity implements Serializable {

    @TableField(exist = false)
    private String tableName;

    /**
     * 主键
     */
    @TableId(value = "id")
    private String id;

    /**
     * abbr
     */
    @TableField(value = "abbr")
    private String abbr;

    /**
     * 词条
     */
    @TableField(value = "entry")
    private String entry;

    /**
     * 
     */
    @TableField(value = "entry_length")
    private Integer entryLength;

    /**
     * 中文释义
     */
    @TableField(value = "chinese_interpretation")
    private String chineseInterpretation;

    /**
     * 英文释义
     */
    @TableField(value = "english_interpretation")
    private String englishInterpretation;

    /**
     * 词条来源
     */
    @TableField(value = "entry_source")
    private String entrySource;

    /**
     * 词条状态
     */
    @TableField(value = "entry_state")
    private String entryState;

    /**
     * 修改人
     */
    @TableField(value = "`update`")
    private String update;

    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 版本
     */
    @TableField(value = "version_id")
    private String versionID;

    /**
     * 词条标签
     */
    @TableField(value = "entry_label")
    private String entryLabel;

    /**
     * 词性备注
     */
    @TableField(value = "part_of_speech")
    private String partOfSpeech;

    /**
     * 词条所属分类
     */
    @TableField(value = "classify_id")
    private String classifyId;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 任务id
     */
    @TableField(value = "task_id")
    private String taskId;

    /**
     * 英文翻译id
     */
    @TableField(value = "en_trans_id")
    private String enTransId;
    @TableField(exist = false)
    private String english;
    @TableField(exist = false)
    private String englishTranslateState;
    @TableField(exist = false)
    private String englishPublicState;
    /**
     * 俄文翻译id
     */
    @TableField(value = "ru_trans_id")
    private String ruTransId;
    @TableField(exist = false)
    private String russian;
    @TableField(exist = false)
    private String russianTranslateState;
    @TableField(exist = false)
    private String russianPublicState;
    /**
     * 法文翻译id
     */
    @TableField(value = "fra_trans_id")
    private String fraTransId;
    @TableField(exist = false)
    private String french;
    @TableField(exist = false)
    private String frenchTranslateState;
    @TableField(exist = false)
    private String frenchPublicState;
    /**
     * 西文翻译id
     */
    @TableField(value = "spa_trans_id")
    private String spaTransId;
    @TableField(exist = false)
    private String spanish;
    @TableField(exist = false)
    private String spanishTranslateState;
    @TableField(exist = false)
    private String spanishPublicState;
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
    private String importType;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}