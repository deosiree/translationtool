package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shr.translationtoolservice.common.AnjiDescription;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 公共词条表
 * @TableName t_entry_common
 */
@TableName(value ="t_entry_common")
@Data
public class EntryCommonEntity implements Serializable {


    private String tableName;

    private String key;
    private Date createEndRTime;

    /**
     * 主键
     */
    @TableId
    private String id;

    /**
     * abbr
     */
    private String abbr;

    /**
     * 词条
     */
    private String chinese;

    /**
     * 词条字符数
     */
    private Integer chineseLength;

    /**
     * 词条字符数
     */
    private String chineseTranslateState;
    /**
     * 中文释义
     */
    private String chineseInterpretation;

    /**
     * 英文释义
     */
    private String englishInterpretation;

    /**
     * 词条来源
     */
    private String entrySource;

    /**
     * 词条状态
     */
    private Integer entryState;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;

    /**
     * 修改人
     */
    @TableField("'update'")
    private String update;

    /**
     * 修改时间
     */
    @AnjiDescription("修改时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date updateTime;

    /**
     * 版本
     */
    private String version;

    /**
     * 是否最新版本   1 是  0 否
     */
    private Integer isLatestVersion;

    /**
     * 词条标签
     */
    private String entryLabel;

    /**
     * 词性备注
     */
    private String partOfSpeech;

    /**
     * 词条所属分类
     */
    private String classifyId;

    /**
     * 重复词条id
     */
    private String repeatEntryId;

    /**
     * 英文翻译
     */
    private String english;

    /**
     * 英文字符数
     */
    private int englishLength;

    /**
     * 英文翻译状态
     */
    private String englishTranslateState;

    /**
     * 英文禁用术语
     */
    private String englishDisable;

    /**
     * 英文禁用数据字符数
     */
    private Integer englishDisableLength;

    /**
     * 俄文翻译
     */
    private String russian;

    /**
     * 俄文翻译字符数
     */
    private Integer russianLength;

    /**
     * 俄文翻译状态
     */
    private String russianTranslateState;

    /**
     * 西文翻译
     */
    private String spanish;

    /**
     * 西文翻译字符数
     */
    private Integer spanishLength;

    /**
     * 西文翻译状态
     */
    private String spanishTranslateState;

    /**
     * 法文翻译
     */
    private String french;

    /**
     * 法文字符数
     */
    private Integer frenchLength;

    /**
     * 法文翻译状态
     */
    private String frenchTranslateState;

    private String remark;

    @AnjiDescription("词条种类")
    private String typeId;


    @AnjiDescription("环境/功能 备注")
    private String environmentRemark;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}