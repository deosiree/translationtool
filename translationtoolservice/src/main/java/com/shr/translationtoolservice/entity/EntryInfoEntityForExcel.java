package com.shr.translationtoolservice.entity;

import lombok.Data;

@Data
public class EntryInfoEntityForExcel extends EntryInfoEntity {

    public String maxChineseLength;

    public String foreignMaxLength;

    
    @Override
    public boolean equals(Object o) {
        // TODO Auto-generated method stub
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }

    public static EntryInfoEntityForExcel convertFromEntryInfoEntity(EntryInfoEntity entity){
        EntryInfoEntityForExcel convert = new EntryInfoEntityForExcel();

        convert.setUpdate(entity.getUpdate());
        convert.setChildren(entity.getChildren());
        convert.setParentID(entity.getParentID());
        convert.setTableName(entity.getTableName());
        convert.setProductName(entity.getProductName());
        convert.setVersionName(entity.getVersionName());
        convert.setDiFileName(entity.getDiFileName());
        convert.setId(entity.getId());
        convert.setAbbr(entity.getAbbr());
        convert.setEntry(entity.getEntry());
        convert.setEntryLength(entity.getEntryLength());
        convert.setChineseInterpretation(entity.getChineseInterpretation());
        convert.setEnglishInterpretation(entity.getEnglishInterpretation());
        convert.setEntrySource(entity.getEntrySource());
        convert.setEntryState(entity.getEntryState());
        convert.setUpdate(entity.getUpdate());
        convert.setUpdateTime(entity.getUpdateTime());
        convert.setVersionID(entity.getVersionID());
        convert.setEntryLabel(entity.getEntryLabel());
        convert.setPartOfSpeech(entity.getPartOfSpeech());
        convert.setClassifyId(entity.getClassifyId());
        convert.setRemark(entity.getRemark());
        convert.setTaskId(entity.getTaskId());
        convert.setEnTransId(entity.getEnTransId());
        convert.setEnglish(entity.getEnglish());
        convert.setEnglishTranslateState(entity.getEnglishTranslateState());
        convert.setEnglishPublicState(entity.getEnglishPublicState());
        convert.setEnCharLength(entity.getEnCharLength());
        convert.setRuTransId(entity.getRuTransId());
        convert.setRussian(entity.getRussian());
        convert.setRussianTranslateState(entity.getRussianTranslateState());
        convert.setRussianPublicState(entity.getRussianPublicState());

        convert.setEnglishAuditSuggest(entity.getEnglishAuditSuggest());
        convert.setRussianAuditSuggest(entity.getRussianAuditSuggest());
        convert.setSpanishAuditSuggest(entity.getSpanishAuditSuggest());
        convert.setFrenchAuditSuggest(entity.getFrenchAuditSuggest());

        convert.setRuCharLength(entity.getRuCharLength());
        convert.setFraTransId(entity.getFraTransId());
        convert.setFrench(entity.getFrench());
        convert.setFrenchTranslateState(entity.getFrenchTranslateState());
        convert.setFrenchPublicState(entity.getFrenchPublicState());
        convert.setFraCharLength(entity.getFraCharLength());

        convert.setSpaTransId(entity.getSpaTransId());
        convert.setSpanish(entity.getSpanish());
        convert.setSpanishTranslateState(entity.getSpanishTranslateState());
        convert.setSpanishPublicState(entity.getSpanishPublicState());

        convert.setSpaCharLength(entity.getSpaCharLength());
        convert.setMaxLength(entity.getMaxLength());
        convert.setIsPublic(entity.getIsPublic());
        convert.setIsDelete(entity.getIsDelete());
        convert.setImportType(entity.getImportType());
        convert.setWriteType(entity.getWriteType());
        convert.setEntryVersion(entity.getEntryVersion());

        convert.setProductID(entity.getProductID());
        convert.setClassfy1(entity.getClassfy1());
        convert.setClassfy2(entity.getClassfy2());
        convert.setTag(entity.getTag());
        convert.setComment(entity.getComment());
        convert.setIsExist(entity.getIsExist());
        convert.setAuditSuggess(entity.getAuditSuggess());
        convert.setEntryVersion(entity.getEntryVersion());
        return convert;
    }
}
