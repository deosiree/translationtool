package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shr.translationtoolservice.util.DefaultValue;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 
 * @TableName t_entry_info
 */
@TableName(value ="t_entry_info")
@Data
public class EntryInfoEntity implements Serializable {


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

    @TableField(exist = false)
    @DefaultValue("")
    private String versionName;
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

    @TableField(value = "russian_interpretation")
    @DefaultValue("")
    private String russianInterpretation;

    @TableField(value = "french_interpretation")
    @DefaultValue("")
    private String frenchInterpretation;

    @TableField(value = "spanish_interpretation")
    @DefaultValue("")
    private String spanishInterpretation;
    /**
     * 词条来源
     */
    @TableField(value = "entry_source")
    @DefaultValue("")
    private String entrySource;

    /**
     * 词条状态(0新建，
     * 1词条待审核，entryInfo
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
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
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
     * 英文翻译id
     */
    @TableField(value = "en_trans_id")
    @DefaultValue("")
        private String enTransId;
    @TableField(exist = false)
    @DefaultValue("")
    private String english;


    @TableField(exist = false)
    @DefaultValue("")
    private String englishTranslateState;
    @TableField(exist = false)
    @DefaultValue("")
    private String englishPublicState;

    /**
     * 英文翻译长度
     */
    @TableField(value = "en_char_length")
    @DefaultValue("")
    private Integer enCharLength;


    /**
     * 俄文翻译id
     */
    @TableField(value = "zh_trans_id")
    @DefaultValue("")
    private String zhTransId;
    @TableField(exist = false)
    @DefaultValue("")
    private String chinese;
    @TableField(exist = false)
    @DefaultValue("")
    private String chineseTranslateState;
    @TableField(exist = false)
    @DefaultValue("")
    private String chinesePublicState;

    /**
     * 俄文翻译id
     */
    @TableField(value = "ru_trans_id")
    @DefaultValue("")
    private String ruTransId;
    @TableField(exist = false)
    @DefaultValue("")
    private String russian;
    @TableField(exist = false)
    @DefaultValue("")
    private String russianTranslateState;
    @TableField(exist = false)
    @DefaultValue("")
    private String russianPublicState;

    @TableField(exist = false)
    private String chineseAuditSuggest;

    @TableField(exist = false)
    private String englishAuditSuggest;

    @TableField(exist = false)
    private String russianAuditSuggest;

    @TableField(exist = false)
    private String spanishAuditSuggest;

    @TableField(exist = false)
    private String frenchAuditSuggest;

    /**
     * 俄文翻译长度
     */
    @TableField(value = "ru_char_length")
    @DefaultValue("")
    private Integer ruCharLength;
    /**
     * 俄文翻译长度
     */
    @TableField(value = "zh_char_length")
    @DefaultValue("")
    private Integer zhCharLength;


    /**
     * 法文翻译id
     */
    @TableField(value = "fra_trans_id")
    @DefaultValue("")
    private String fraTransId;
    @TableField(exist = false)
    @DefaultValue("")
    private String french;
    @TableField(exist = false)
    @DefaultValue("")
    private String frenchTranslateState;
    @TableField(exist = false)
    @DefaultValue("")
    private String frenchPublicState;
    /**
     * 法文翻译长度
     */
    @TableField(value = "fra_char_length")
    @DefaultValue("")
    private Integer fraCharLength;
    /**
     * 西文翻译id
     */
    @TableField(value = "spa_trans_id")
    @DefaultValue("")
    private String spaTransId;
    @TableField(exist = false)
    @DefaultValue("")
    private String spanish;
    @TableField(exist = false)
    @DefaultValue("")
    private String spanishTranslateState;
    @TableField(exist = false)
    @DefaultValue("")
    private String spanishPublicState;
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
     * 回写类型
     */
    @TableField(value = "write_type")
    @DefaultValue("")
    private String writeType;

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
     * comment
     */
    @TableField(value = "comment")
    private String comment;

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


    /*来源表名*/
    @TableField(value = "source_table")
    private String srcTabName;
    /*数据库记录ID */
    @TableField(value = "db_record_id")
    private String dbRID;

}