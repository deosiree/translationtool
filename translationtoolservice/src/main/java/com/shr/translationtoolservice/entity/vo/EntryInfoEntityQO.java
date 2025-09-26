package com.shr.translationtoolservice.entity.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.util.DefaultValue;

public class EntryInfoEntityQO implements Serializable{
    
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
    private List<Integer> entryState;

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
    private List<String> classfy1 = null;

    /**
     * 二级分类
     */
    @TableField(value = "classfy2")
    @DefaultValue("")
    private List<String> classfy2 = null;

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
    public boolean isUpgrade() {
        return upgrade;
    }
    public void setUpgrade(boolean upgrade) {
        this.upgrade = upgrade;
    }
    public List<EntryInfoEntity> getChildren() {
        return children;
    }
    public void setChildren(List<EntryInfoEntity> children) {
        this.children = children;
    }
    public String getParentID() {
        return parentID;
    }
    public void setParentID(String parentID) {
        this.parentID = parentID;
    }
    public String getTableName() {
        return tableName;
    }
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    public String getProductName() {
        return productName;
    }
    public void setProductName(String productName) {
        this.productName = productName;
    }
    public String getVersionName() {
        return versionName;
    }
    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }
    public String getDiFileName() {
        return diFileName;
    }
    public void setDiFileName(String diFileName) {
        this.diFileName = diFileName;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getAbbr() {
        return abbr;
    }
    public void setAbbr(String abbr) {
        this.abbr = abbr;
    }
    public String getEntry() {
        return entry;
    }
    public void setEntry(String entry) {
        this.entry = entry;
    }
    public Integer getEntryLength() {
        return entryLength;
    }
    public void setEntryLength(Integer entryLength) {
        this.entryLength = entryLength;
    }
    public String getChineseInterpretation() {
        return chineseInterpretation;
    }
    public void setChineseInterpretation(String chineseInterpretation) {
        this.chineseInterpretation = chineseInterpretation;
    }
    public String getEnglishInterpretation() {
        return englishInterpretation;
    }
    public void setEnglishInterpretation(String englishInterpretation) {
        this.englishInterpretation = englishInterpretation;
    }
    public String getRussianInterpretation() {
        return russianInterpretation;
    }
    public void setRussianInterpretation(String russianInterpretation) {
        this.russianInterpretation = russianInterpretation;
    }
    public String getFrenchInterpretation() {
        return frenchInterpretation;
    }
    public void setFrenchInterpretation(String frenchInterpretation) {
        this.frenchInterpretation = frenchInterpretation;
    }
    public String getSpanishInterpretation() {
        return spanishInterpretation;
    }
    public void setSpanishInterpretation(String spanishInterpretation) {
        this.spanishInterpretation = spanishInterpretation;
    }
    public String getEntrySource() {
        return entrySource;
    }
    public void setEntrySource(String entrySource) {
        this.entrySource = entrySource;
    }
    public List<Integer> getEntryState() {
        return entryState;
    }
    public void setEntryState(List<Integer> entryState) {
        this.entryState = entryState;
    }
    public String getUpdate() {
        return update;
    }
    public void setUpdate(String update) {
        this.update = update;
    }
    public Date getUpdateTime() {
        return updateTime;
    }
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
    public String getVersionID() {
        return versionID;
    }
    public void setVersionID(String versionID) {
        this.versionID = versionID;
    }
    public String getEntryLabel() {
        return entryLabel;
    }
    public void setEntryLabel(String entryLabel) {
        this.entryLabel = entryLabel;
    }
    public String getPartOfSpeech() {
        return partOfSpeech;
    }
    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }
    public String getClassifyId() {
        return classifyId;
    }
    public void setClassifyId(String classifyId) {
        this.classifyId = classifyId;
    }
    public String getRemark() {
        return remark;
    }
    public void setRemark(String remark) {
        this.remark = remark;
    }
    public String getTaskId() {
        return taskId;
    }
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    public String getEnTransId() {
        return enTransId;
    }
    public void setEnTransId(String enTransId) {
        this.enTransId = enTransId;
    }
    public String getEnglish() {
        return english;
    }
    public void setEnglish(String english) {
        this.english = english;
    }
    public String getEnglishTranslateState() {
        return englishTranslateState;
    }
    public void setEnglishTranslateState(String englishTranslateState) {
        this.englishTranslateState = englishTranslateState;
    }
    public String getEnglishPublicState() {
        return englishPublicState;
    }
    public void setEnglishPublicState(String englishPublicState) {
        this.englishPublicState = englishPublicState;
    }
    public Integer getEnCharLength() {
        return enCharLength;
    }
    public void setEnCharLength(Integer enCharLength) {
        this.enCharLength = enCharLength;
    }
    public String getZhTransId() {
        return zhTransId;
    }
    public void setZhTransId(String zhTransId) {
        this.zhTransId = zhTransId;
    }
    public String getChinese() {
        return chinese;
    }
    public void setChinese(String chinese) {
        this.chinese = chinese;
    }
    public String getChineseTranslateState() {
        return chineseTranslateState;
    }
    public void setChineseTranslateState(String chineseTranslateState) {
        this.chineseTranslateState = chineseTranslateState;
    }
    public String getChinesePublicState() {
        return chinesePublicState;
    }
    public void setChinesePublicState(String chinesePublicState) {
        this.chinesePublicState = chinesePublicState;
    }
    public String getRuTransId() {
        return ruTransId;
    }
    public void setRuTransId(String ruTransId) {
        this.ruTransId = ruTransId;
    }
    public String getRussian() {
        return russian;
    }
    public void setRussian(String russian) {
        this.russian = russian;
    }
    public String getRussianTranslateState() {
        return russianTranslateState;
    }
    public void setRussianTranslateState(String russianTranslateState) {
        this.russianTranslateState = russianTranslateState;
    }
    public String getRussianPublicState() {
        return russianPublicState;
    }
    public void setRussianPublicState(String russianPublicState) {
        this.russianPublicState = russianPublicState;
    }
    public String getChineseAuditSuggest() {
        return chineseAuditSuggest;
    }
    public void setChineseAuditSuggest(String chineseAuditSuggest) {
        this.chineseAuditSuggest = chineseAuditSuggest;
    }
    public String getEnglishAuditSuggest() {
        return englishAuditSuggest;
    }
    public void setEnglishAuditSuggest(String englishAuditSuggest) {
        this.englishAuditSuggest = englishAuditSuggest;
    }
    public String getRussianAuditSuggest() {
        return russianAuditSuggest;
    }
    public void setRussianAuditSuggest(String russianAuditSuggest) {
        this.russianAuditSuggest = russianAuditSuggest;
    }
    public String getSpanishAuditSuggest() {
        return spanishAuditSuggest;
    }
    public void setSpanishAuditSuggest(String spanishAuditSuggest) {
        this.spanishAuditSuggest = spanishAuditSuggest;
    }
    public String getFrenchAuditSuggest() {
        return frenchAuditSuggest;
    }
    public void setFrenchAuditSuggest(String frenchAuditSuggest) {
        this.frenchAuditSuggest = frenchAuditSuggest;
    }
    public Integer getRuCharLength() {
        return ruCharLength;
    }
    public void setRuCharLength(Integer ruCharLength) {
        this.ruCharLength = ruCharLength;
    }
    public Integer getZhCharLength() {
        return zhCharLength;
    }
    public void setZhCharLength(Integer zhCharLength) {
        this.zhCharLength = zhCharLength;
    }
    public String getFraTransId() {
        return fraTransId;
    }
    public void setFraTransId(String fraTransId) {
        this.fraTransId = fraTransId;
    }
    public String getFrench() {
        return french;
    }
    public void setFrench(String french) {
        this.french = french;
    }
    public String getFrenchTranslateState() {
        return frenchTranslateState;
    }
    public void setFrenchTranslateState(String frenchTranslateState) {
        this.frenchTranslateState = frenchTranslateState;
    }
    public String getFrenchPublicState() {
        return frenchPublicState;
    }
    public void setFrenchPublicState(String frenchPublicState) {
        this.frenchPublicState = frenchPublicState;
    }
    public Integer getFraCharLength() {
        return fraCharLength;
    }
    public void setFraCharLength(Integer fraCharLength) {
        this.fraCharLength = fraCharLength;
    }
    public String getSpaTransId() {
        return spaTransId;
    }
    public void setSpaTransId(String spaTransId) {
        this.spaTransId = spaTransId;
    }
    public String getSpanish() {
        return spanish;
    }
    public void setSpanish(String spanish) {
        this.spanish = spanish;
    }
    public String getSpanishTranslateState() {
        return spanishTranslateState;
    }
    public void setSpanishTranslateState(String spanishTranslateState) {
        this.spanishTranslateState = spanishTranslateState;
    }
    public String getSpanishPublicState() {
        return spanishPublicState;
    }
    public void setSpanishPublicState(String spanishPublicState) {
        this.spanishPublicState = spanishPublicState;
    }
    public Integer getSpaCharLength() {
        return spaCharLength;
    }
    public void setSpaCharLength(Integer spaCharLength) {
        this.spaCharLength = spaCharLength;
    }
    public Integer getMaxLength() {
        return maxLength;
    }
    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }
    public Integer getIsPublic() {
        return isPublic;
    }
    public void setIsPublic(Integer isPublic) {
        this.isPublic = isPublic;
    }
    public Integer getIsDelete() {
        return isDelete;
    }
    public void setIsDelete(Integer isDelete) {
        this.isDelete = isDelete;
    }
    public String getImportType() {
        return importType;
    }
    public void setImportType(String importType) {
        this.importType = importType;
    }
    public String getWriteType() {
        return writeType;
    }
    public void setWriteType(String writeType) {
        this.writeType = writeType;
    }
    public Integer getEntryVersion() {
        return entryVersion;
    }
    public void setEntryVersion(Integer entryVersion) {
        this.entryVersion = entryVersion;
    }
    public String getProductID() {
        return productID;
    }
    public void setProductID(String productID) {
        this.productID = productID;
    }
    public List<String> getClassfy1() {
        return classfy1;
    }
    public void setClassfy1(List<String> classfy1) {
        this.classfy1 = classfy1;
    }
    public List<String> getClassfy2() {
        return classfy2;
    }
    public void setClassfy2(List<String> classfy2) {
        this.classfy2 = classfy2;
    }
    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public Integer getIsExist() {
        return isExist;
    }
    public void setIsExist(Integer isExist) {
        this.isExist = isExist;
    }
    public String getAuditSuggess() {
        return auditSuggess;
    }
    public void setAuditSuggess(String auditSuggess) {
        this.auditSuggess = auditSuggess;
    }
    public String getEntryVersionID() {
        return entryVersionID;
    }
    public void setEntryVersionID(String entryVersionID) {
        this.entryVersionID = entryVersionID;
    }
    public static long getSerialversionuid() {
        return serialVersionUID;
    }
    public String getSrcTabName() {
        return srcTabName;
    }
    public void setSrcTabName(String srcTabName) {
        this.srcTabName = srcTabName;
    }
    public String getDbRID() {
        return dbRID;
    }
    public void setDbRID(String dbRID) {
        this.dbRID = dbRID;
    }
  

}
