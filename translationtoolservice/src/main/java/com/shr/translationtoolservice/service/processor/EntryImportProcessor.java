package com.shr.translationtoolservice.service.processor;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.ImportExcleEntry;
import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.shr.translationtoolservice.util.CommonUtils;
import org.junit.platform.commons.util.StringUtils;
import com.alibaba.fastjson.JSONObject;


@Component
public class EntryImportProcessor {

    @Autowired
    protected CommonUtils commonUtils;
    
    @Autowired
    protected EntryInfoMapper entryInfoMapper;

    protected Map<String,String> translateMap = ConstantInterface.translateMap();

    public void ptEntryHandle(Collection<EntryInfoEntity> entryEntitys, String userName, TaskInfoEntity taskInfoEntity, String fileName) {
        Date date = new Date(System.currentTimeMillis());
        for (EntryInfoEntity entryInfoEntity : entryEntitys) {

            entryInfoEntity.setEntryLength(entryInfoEntity.getEntry().length());
            entryInfoEntity.setUpdate(userName);
            entryInfoEntity.setUpdateTime(date);

            entryInfoEntity.setIsDelete(0);
            entryInfoEntity.setIsPublic(0);
            entryInfoEntity.setEntryState(1);

            entryInfoEntity.setId(commonUtils.getUUID());
            if (Objects.nonNull(taskInfoEntity)) {
                entryInfoEntity.setTaskId(taskInfoEntity.getId());
                entryInfoEntity.setProductID(taskInfoEntity.getProductId());
                entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
            }
            if (fileName.contains("_db_meta_common.xlsx")) {
                entryInfoEntity.setWriteType(ConstantInterface.DI);
                String tag = entryInfoEntity.getTag();
                //tag 转成json
                entryInfoEntity.setImportType(ConstantInterface.DB_META);
                JSONObject jsonObject = JSONObject.parseObject(tag);
                String appName = jsonObject.get("appName").toString();
                String dbName = jsonObject.get("dbName").toString().replace(".","_");
                String tableName = jsonObject.get("tableName").toString();
                String fieldName = jsonObject.get("fieldName").toString();
                entryInfoEntity.setTag( tableName + "/" + fieldName);
                entryInfoEntity.setDiFileName("db/meta/"  +  dbName );
            }


            if (StringUtils.isBlank(entryInfoEntity.getImportType())){
                entryInfoEntity.setImportType(ConstantInterface.EXCEL);
            }
            if (StringUtils.isBlank(entryInfoEntity.getWriteType())){
                entryInfoEntity.setWriteType(ConstantInterface.DI);
            }

            if (StringUtils.isBlank(entryInfoEntity.getEntrySource())){
                entryInfoEntity.setEntrySource("import : " + ConstantInterface.EXCEL + " ; fileName" + fileName);
            }

            String productTableName = "t_entry_info";
            // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());
            List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryList(productTableName, entryInfoEntity, taskInfoEntity.getProductId());
            if (CollectionUtils.isEmpty(entryEntities)) {
                //创建新翻译
                entryInfoEntity.setIsExist(0);
                entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
                entryInfoEntity.setEntryVersion(1);
                setPTTranslateState(taskInfoEntity, entryInfoEntity);

            } else {
                entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
                entryInfoEntity.setIsExist(1);
            }
        }

    }

    public void ptTSEntryHandle(Collection<EntryInfoEntity> entryEntitys, String userName, TaskInfoEntity taskInfoEntity, String fileName) {
        this.ptEntryHandle(entryEntitys, userName, taskInfoEntity, fileName);

        for(EntryInfoEntity entity : entryEntitys){
            for(Map.Entry<String,String> translateInfo : translateMap.entrySet()){
                this.createNewTrans(entity, translateInfo.getValue());
            }
        }

        return;
    }


    private void setPTTranslateState(TaskInfoEntity taskInfoEntity, EntryInfoEntity entryInfoEntity) {
        //写入翻译字段
        switch (taskInfoEntity.getTranslateType()) {
            case ConstantInterface.CHINESE:
                if (StringUtils.isNotBlank(entryInfoEntity.getChinese())) {
                    entryInfoEntity.setChineseTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setChineseTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(entryInfoEntity.getZhCharLength())) {
                    entryInfoEntity.setEntryLength(entryInfoEntity.getZhCharLength());
                }
                break;
            case ConstantInterface.ENGLISH:
                if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(entryInfoEntity.getEnCharLength())) {
                    entryInfoEntity.setEntryLength(entryInfoEntity.getEnCharLength());
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(entryInfoEntity.getSpaCharLength())) {
                    entryInfoEntity.setEntryLength(entryInfoEntity.getSpaCharLength());
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(entryInfoEntity.getRuCharLength())) {
                    entryInfoEntity.setEntryLength(entryInfoEntity.getRuCharLength());
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(entryInfoEntity.getFraCharLength())) {
                    entryInfoEntity.setEntryLength(entryInfoEntity.getFraCharLength());
                }
                break;
        }
    }

    public void zzEntryHandle(List<ImportExcleEntry> importExcleEntries, List<EntryInfoEntity> entryEntitys, String userName, TaskInfoEntity taskInfoEntity, String fileName) {
        Date date = new Date(System.currentTimeMillis());
        for (ImportExcleEntry importExcleEntry : importExcleEntries) {

            EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
            BeanUtils.copyProperties(importExcleEntry, entryInfoEntity);
            entryInfoEntity.setAbbr(importExcleEntry.getAbbr());
            entryInfoEntity.setEntry(importExcleEntry.getAbbr());
            entryInfoEntity.setEntryLength(importExcleEntry.getAbbr().length());
            entryInfoEntity.setUpdate(userName);
            entryInfoEntity.setUpdateTime(date);
            entryInfoEntity.setChineseInterpretation(importExcleEntry.getChineseInterpretation());
            entryInfoEntity.setEnglishInterpretation(importExcleEntry.getEnglishInterpretation());
            entryInfoEntity.setClassfy1(importExcleEntry.getClassfy1());
            entryInfoEntity.setClassfy2(importExcleEntry.getClassfy2());
            entryInfoEntity.setProductID(taskInfoEntity.getProductId());
            entryInfoEntity.setIsDelete(0);
            entryInfoEntity.setIsPublic(0);
            entryInfoEntity.setEntryState(1);
            entryInfoEntity.setTaskId(taskInfoEntity.getProductId());
            entryInfoEntity.setId(commonUtils.getUUID());
            if (Objects.nonNull(taskInfoEntity)) {
                entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
            }
            entryInfoEntity.setImportType(ConstantInterface.EXCEL);
            entryInfoEntity.setWriteType(ConstantInterface.EXCEL);
            // entryInfoEntity.setEntrySource("import : " + ConstantInterface.EXCEL + " ; fileName" + fileName);
            entryInfoEntity.setRemark(importExcleEntry.getCreator());
            String productTableName = "t_entry_info";
            caseExisttry(entryInfoEntity, taskInfoEntity, importExcleEntry, productTableName);
            entryEntitys.add(entryInfoEntity);
        }
    
    }


    public void zzEntryHandle(List<EntryInfoEntity> entryEntitys, String userName, TaskInfoEntity taskInfoEntity, String fileName) {
        Date date = new Date(System.currentTimeMillis());
        for(EntryInfoEntity entryInfoEntity : entryEntitys){
            entryInfoEntity.setEntryLength(entryInfoEntity.getEntry().length());
            entryInfoEntity.setIsDelete(0);
            if(entryInfoEntity.getUpdate() == null || entryInfoEntity.getUpdate().equals("")){
                entryInfoEntity.setUpdate(userName);
            }
            entryInfoEntity.setUpdateTime(date);
            entryInfoEntity.setIsPublic(0);
            entryInfoEntity.setEntryState(1);
            entryInfoEntity.setId(commonUtils.getUUID());
            if (Objects.nonNull(taskInfoEntity)) {
                entryInfoEntity.setTaskId(taskInfoEntity.getId());
                entryInfoEntity.setProductID(taskInfoEntity.getProductId());
                entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
            }
            if (StringUtils.isBlank(entryInfoEntity.getImportType())){
                entryInfoEntity.setImportType(ConstantInterface.EXCEL);
            }
            String productTableName = "t_entry_info";
            caseExisttry(entryInfoEntity, taskInfoEntity,  productTableName);
        }
    }


    private void caseExisttry(EntryInfoEntity entryInfoEntity, TaskInfoEntity taskInfoEntity, String productTableName) {
        // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());
        List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryListForEquipment(productTableName, entryInfoEntity, taskInfoEntity.getProductId());
        if (CollectionUtils.isEmpty(entryEntities)) {
            //创建新翻译
            entryInfoEntity.setIsExist(0);
            entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
            entryInfoEntity.setEntryVersion(1);
            createNewTrans(entryInfoEntity, taskInfoEntity);
        } else {
            entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
            int maxVersion = entryEntities.stream().max(Comparator.comparing(EntryInfoEntity::getEntryVersion)).get().getEntryVersion();
            entryInfoEntity.setEntryVersion(maxVersion);
            entryInfoEntity.setIsExist(1);
        }
    }

    private void caseExisttry(EntryInfoEntity entryInfoEntity, TaskInfoEntity taskInfoEntity, ImportExcleEntry importExcleEntry, String productTableName) {
        // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());
        List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryListForEquipment(productTableName, entryInfoEntity, taskInfoEntity.getProductId());
        if (CollectionUtils.isEmpty(entryEntities)) {
            //创建新翻译
            entryInfoEntity.setIsExist(0);
            entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
            entryInfoEntity.setEntryVersion(1);
            createNewTrans(entryInfoEntity, taskInfoEntity, importExcleEntry);
        } else {
            entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
            int maxVersion = entryEntities.stream().max(Comparator.comparing(EntryInfoEntity::getEntryVersion)).get().getEntryVersion();
            entryInfoEntity.setEntryVersion(maxVersion);
            entryInfoEntity.setIsExist(1);
        }
    }

    private void createNewTrans(EntryInfoEntity entryInfoEntity, TaskInfoEntity taskInfoEntity) {
        this.createNewTrans(entryInfoEntity, taskInfoEntity.getTranslateType());
    }

    private void createNewTrans(EntryInfoEntity entryInfoEntity, String translateType) {
        switch (translateType) {
            case ConstantInterface.CHINESE:
                if (StringUtils.isNotBlank(entryInfoEntity.getChinese())) {
                    entryInfoEntity.setChineseTranslateState(ConstantInterface.TRANSLATED);
                    entryInfoEntity.setZhCharLength(entryInfoEntity.getChinese().length());
                } else {
                    entryInfoEntity.setChineseTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.ENGLISH:
                if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
                    entryInfoEntity.setEnCharLength(entryInfoEntity.getEnglish().length());
                } else {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
                    entryInfoEntity.setSpaCharLength(entryInfoEntity.getSpanish().length());
                } else {
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
                    entryInfoEntity.setRuCharLength(entryInfoEntity.getRussian().length());
                } else {
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
                    entryInfoEntity.setFraCharLength(entryInfoEntity.getFrench().length());
                } else {
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
        }
    }


    private void createNewTrans(EntryInfoEntity entryInfoEntity, TaskInfoEntity taskInfoEntity, ImportExcleEntry importExcleEntry) {
        //写入翻译字段
        switch (taskInfoEntity.getTranslateType()) {
            case ConstantInterface.CHINESE:
                if (StringUtils.isNotBlank(importExcleEntry.getChinese())) {
                    entryInfoEntity.setChinese(importExcleEntry.getChinese());
                    entryInfoEntity.setChineseTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setChineseTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(importExcleEntry.getZhCharLength())) {
                    entryInfoEntity.setEntryLength(importExcleEntry.getZhCharLength());
                }
                break;
            case ConstantInterface.ENGLISH:
                if (StringUtils.isNotBlank(importExcleEntry.getEnglish())) {
                    entryInfoEntity.setEnglish(importExcleEntry.getEnglish());
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(importExcleEntry.getEnCharLength())) {
                    entryInfoEntity.setEntryLength(importExcleEntry.getEnCharLength());
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isNotBlank(importExcleEntry.getSpanish())) {
                    entryInfoEntity.setSpanish(importExcleEntry.getSpanish());
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(importExcleEntry.getSpaCharLength())) {
                    entryInfoEntity.setEntryLength(importExcleEntry.getSpaCharLength());
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isNotBlank(importExcleEntry.getRussian())) {
                    entryInfoEntity.setRussian(importExcleEntry.getRussian());
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(importExcleEntry.getRuCharLength())) {
                    entryInfoEntity.setEntryLength(importExcleEntry.getRuCharLength());
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isNotBlank(importExcleEntry.getFrench())) {
                    entryInfoEntity.setFrench(importExcleEntry.getFrench());
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(importExcleEntry.getFraCharLength())) {
                    entryInfoEntity.setEntryLength(importExcleEntry.getFraCharLength());
                }
                break;
        }
    }

    public void zzEntryHandleForXML(Collection<EntryInfoEntity> entryEntitys, String userName, TaskInfoEntity taskInfoEntity, String fileName) {
        Date date = new Date(System.currentTimeMillis());
        for(EntryInfoEntity entryInfoEntity : entryEntitys){
            entryInfoEntity.setEntryLength(entryInfoEntity.getEntry().length());
            entryInfoEntity.setIsDelete(0);
            if(entryInfoEntity.getUpdate() == null || entryInfoEntity.getUpdate().equals("")){
                entryInfoEntity.setUpdate(userName);
            }
            entryInfoEntity.setUpdateTime(date);
            entryInfoEntity.setIsPublic(0);
            entryInfoEntity.setEntryState(1);
            entryInfoEntity.setId(commonUtils.getUUID());
            if (Objects.nonNull(taskInfoEntity)) {
                entryInfoEntity.setTaskId(taskInfoEntity.getId());
                entryInfoEntity.setProductID(taskInfoEntity.getProductId());
                entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
            }
            if (StringUtils.isBlank(entryInfoEntity.getImportType())){
                entryInfoEntity.setImportType(ConstantInterface.XML);
            }
            String productTableName = "t_entry_info";
            caseExisttryForXML(entryInfoEntity, taskInfoEntity,  productTableName);
        }
    }

    public void caseExisttryForXML(EntryInfoEntity entryInfoEntity, TaskInfoEntity taskInfoEntity, String productTableName) {
        // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());
        List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryListForXML(productTableName, entryInfoEntity, taskInfoEntity.getProductId());
        if (CollectionUtils.isEmpty(entryEntities)) {
            //创建新翻译
            entryInfoEntity.setIsExist(0);
            entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
            entryInfoEntity.setEntryVersion(1);
            createNewTrans(entryInfoEntity, taskInfoEntity);
        } else {
            entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
            int maxVersion = entryEntities.stream().max(Comparator.comparing(EntryInfoEntity::getEntryVersion)).get().getEntryVersion();
            entryInfoEntity.setEntryVersion(maxVersion);
            entryInfoEntity.setIsExist(1);
        }
    }

    public <E> Collection<EntryInfoEntity> convertToEntryInfos(Function<E,EntryInfoEntity> converter,Collection<E> infos){

        if(converter == null){
            return null;
        }
        return infos.stream().map(converter).collect(Collectors.toList());

    }
    
}
