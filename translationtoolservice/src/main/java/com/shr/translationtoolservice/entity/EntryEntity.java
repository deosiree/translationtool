package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.shr.translationtoolservice.common.AnjiDescription;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * @ClassName EntryEntity
 * @USER: Cola
 * @Date 2023/7/3 0003 14:40
 **/
@Data
public class EntryEntity {



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
    @AnjiDescription("abbr")
    private String abbr;

    /**
     * 词条
     */
    @AnjiDescription("词条")
    private String chinese;

    /**
     * 词条字符数
     */
    @AnjiDescription("词条字符数")
    private Integer chineseLength;

    /**
     * 中文释义
     */
    @AnjiDescription("中文释义")
    private String chineseInterpretation;

    /**
     * 英文释义
     */
    @AnjiDescription("英文释义")
    private String englishInterpretation;

    /**
     * 词条来源
     */
    @AnjiDescription("词条来源")
    private String entrySource;

    /**
     * 词条状态
     */
    @AnjiDescription("词条状态")
    private Integer entryState;

    /**
     * 创建人
     */
    @AnjiDescription("创建人")
    private String creator;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @AnjiDescription("创建时间")
    private Date createTime;

    /**
     * 修改人
     */

    @TableField("`update`")
    @AnjiDescription("修改人")
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
    @AnjiDescription("版本")
    private String version;

    /**
     * 是否最新版本   1 是  0 否
     */
    @AnjiDescription("是否最新版本   1 是  0 否")
    private int isLatestVersion;

    /**
     * 词条标签
     */
    @AnjiDescription("词条标签")
    private String entryLabel;

    /**
     * 词性备注
     */
    @AnjiDescription("词性备注")
    private String partOfSpeech;

    /**
     * 词条所属分类ID
     */
    @AnjiDescription("词条所属分类ID")
    private String classifyId;

    /**
     * 词条所属分类
     */
    @AnjiDescription("词条所属分类")
    private String classify;


    /**
     * 重复词条id
     */
    @AnjiDescription("重复词条id")
    private String repeatEntryId;

    /**
     * 英文翻译
     */
    @AnjiDescription("英文翻译")
    private String english;

    /**
     * 英文字符数
     */
    @AnjiDescription("英文字符数")
    private int englishLength;

    /**
     * 英文翻译状态
     */
    @AnjiDescription("英文翻译状态")
    private String englishTranslateState;

    /**
     * 英文禁用术语
     */
    @AnjiDescription("英文禁用术语")
    private String englishDisable;

    /**
     * 英文禁用数据字符数
     */
    @AnjiDescription("英文禁用数据字符数")
    private int englishDisableLength;

    /**
     * 俄文翻译
     */
    @AnjiDescription("俄文翻译")
    private String russian;

    /**
     * 俄文翻译字符数
     */
    @AnjiDescription("俄文翻译字符数")
    private int russianLength;

    /**
     * 俄文翻译状态
     */
    @AnjiDescription("俄文翻译状态")
    private String russianTranslateState;

    /**
     * 西文翻译
     */
    @AnjiDescription("西文翻译")
    private String spanish;

    /**
     * 西文翻译字符数
     */
    @AnjiDescription("西文翻译字符数")
    private int spanishLength;

    /**
     * 西文翻译状态
     */
    @AnjiDescription("西文翻译状态")
    private String spanishTranslateState;

    /**
     * 法文翻译
     */
    @AnjiDescription("法文翻译")
    private String french;

    /**
     * 法文字符数
     */
    @AnjiDescription("法文字符数")
    private int frenchLength;

    /**
     * 法文翻译状态
     */
    @AnjiDescription("法文翻译状态")
    private String frenchTranslateState;

    /**
     * 备注
     */
    @AnjiDescription("备注")
    private String remark;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
