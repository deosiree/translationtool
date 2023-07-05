package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.shr.translationtoolservice.common.AnjiDescription;

import lombok.Data;

import java.util.Date;

/**
 * @ClassName EntryEntity
 * @Description TODO
 * @USER: Cola
 * @Date 2023/7/3 0003 14:40
 **/
@Data
public class EntryEntity {


    private String type;

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

    private String entry;

    /**
     * 词条字符数
     */

    private Double entryLength;

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
    @AnjiDescription("词条状态")
    private Integer entryState;

    /**
     * 创建人
     */

    private String creator;

    /**
     * 创建时间
     */

    private Date createTime;

    /**
     * 修改人
     */

    @TableField("`update`")
    private String update;

    /**
     * 修改时间
     */

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
    private Double englishLength;

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
    private Double englishDisableLength;

    /**
     * 俄文翻译
     */
    private String russian;

    /**
     * 俄文翻译字符数
     */
    private Double russianLength;

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
    private Double spanishLength;

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
    private Double frenchLength;

    /**
     * 法文翻译状态
     */
    private String frenchTranslateState;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
