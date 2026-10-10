package com.shr.translationtoolservice.util;

import com.shr.translationtoolservice.dao.TranslateMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.TranslateEntity;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @ClassName EntryProcessUtils
 * @Description 词条公共处理器
 * @USER: Cola
 * @Date 2024/2/29 0029 10:17
 **/
@Slf4j
@Component
public class EntryProcessUtils {

    private final TranslateMapper translateMapper;

    public EntryProcessUtils(TranslateMapper translateMapper) {
        this.translateMapper = translateMapper;
    }

    private String getMapKey(EntryInfoEntity entryInfoEntity,String translateType,String forDepartment){
        if(forDepartment == null || !forDepartment.equals("装置开发部")){
            String entry = entryInfoEntity.getEntry();
            String translate = "";
            //有翻译字段 直接放到map里
            switch (translateType){
                case ConstantInterface.CHINESE:
                    if (StringUtils.isNotBlank(entryInfoEntity.getZhTransId())){
                        TranslateEntity translateEntitiy = translateMapper.selectById(entryInfoEntity.getZhTransId());
                        if(translateEntitiy != null)
                            translate = translateEntitiy.getTranslate();
                    }else {
                        translate = entryInfoEntity.getChinese();
                    }
                    break;
                case ConstantInterface.ENGLISH:
                    if (StringUtils.isNotBlank(entryInfoEntity.getEnTransId())){
                        TranslateEntity translateEntitiy = translateMapper.selectById(entryInfoEntity.getEnTransId());
                        if(translateEntitiy != null)
                            translate = translateEntitiy.getTranslate();
                    }else {
                        translate = entryInfoEntity.getEnglish();
                    }
                    break;
                case ConstantInterface.SPANISH:
                    if (StringUtils.isNotBlank(entryInfoEntity.getSpaTransId())){
                        TranslateEntity translateEntitiy = translateMapper.selectById(entryInfoEntity.getSpaTransId());
                        if(translateEntitiy != null)
                            translate = translateEntitiy.getTranslate();
                    }else {
                        translate = entryInfoEntity.getSpanish();
                    }
                    break;
                case ConstantInterface.RUSSIAN:
                    if (StringUtils.isNotBlank(entryInfoEntity.getRuTransId())){
                        TranslateEntity translateEntitiy = translateMapper.selectById(entryInfoEntity.getRuTransId());
                        if(translateEntitiy != null)
                            translate = translateEntitiy.getTranslate();
                    }else {
                        translate = entryInfoEntity.getRussian();
                    }
                    break;
                case ConstantInterface.FRENCH:
                    if (StringUtils.isNotBlank(entryInfoEntity.getFraTransId())){
                        TranslateEntity translateEntitiy = translateMapper.selectById(entryInfoEntity.getFraTransId());
                        if(translateEntitiy != null)
                            translate = translateEntitiy.getTranslate();
                    }else {
                        translate = entryInfoEntity.getFrench();
                    }
                    break;
            }
            String mapKey = entry + ConstantInterface.UNDERLINE + translate + ConstantInterface.UNDERLINE + entryInfoEntity.getIsExist() + 
                ConstantInterface.UNDERLINE + entryInfoEntity.getComment() + ConstantInterface.UNDERLINE + entryInfoEntity.getAbbr();
            return mapKey;
        }else{
            return entryInfoEntity.getEntry();
        }
    }

    public List<EntryInfoEntity> buildRepeEntry(Collection<EntryInfoEntity> entryInfoEntities, String translateType) {
        return buildRepeEntry(entryInfoEntities, translateType,null);
    }

    public List<EntryInfoEntity> buildRepeEntry(Collection<EntryInfoEntity> entryInfoEntities, String translateType,String forDepartment) {
        List<EntryInfoEntity> newEntry = new ArrayList<>();
        //entry_translate,entryTempEntity
        Map<String, EntryInfoEntity> entryEntityMap = new HashMap<>();
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {

            // String entry = entryInfoEntity.getEntry();
            // String translate = "";
            // //有翻译字段 直接放到map里
            // switch (translateType){
            //     case ConstantInterface.CHINESE:
            //         if (StringUtils.isNotBlank(entryInfoEntity.getZhTransId())){
            //             translate = translateMapper.selectById(entryInfoEntity.getZhTransId()).getTranslate();
            //         }else {
            //             translate = entryInfoEntity.getChinese();
            //         }
            //         break;
            //     case ConstantInterface.ENGLISH:
            //         if (StringUtils.isNotBlank(entryInfoEntity.getEnTransId())){
            //             translate = translateMapper.selectById(entryInfoEntity.getEnTransId()).getTranslate();
            //         }else {
            //             translate = entryInfoEntity.getEnglish();
            //         }
            //         break;
            //     case ConstantInterface.SPANISH:
            //         if (StringUtils.isNotBlank(entryInfoEntity.getSpaTransId())){
            //             translate = translateMapper.selectById(entryInfoEntity.getSpaTransId()).getTranslate();
            //         }else {
            //             translate = entryInfoEntity.getSpanish();
            //         }
            //         break;
            //     case ConstantInterface.RUSSIAN:
            //         if (StringUtils.isNotBlank(entryInfoEntity.getRuTransId())){
            //             translate = translateMapper.selectById(entryInfoEntity.getRuTransId()).getTranslate();
            //         }else {
            //             translate = entryInfoEntity.getRussian();
            //         }
            //         break;
            //     case ConstantInterface.FRENCH:
            //         if (StringUtils.isNotBlank(entryInfoEntity.getFraTransId())){
            //             translate = translateMapper.selectById(entryInfoEntity.getFraTransId()).getTranslate();
            //         }else {
            //             translate = entryInfoEntity.getFrench();
            //         }
            //         break;
            // }
            // String mapKey = entry + ConstantInterface.UNDERLINE + translate + ConstantInterface.UNDERLINE + entryInfoEntity.getIsExist() + 
            //     ConstantInterface.UNDERLINE + entryInfoEntity.getComment() + ConstantInterface.UNDERLINE + entryInfoEntity.getAbbr();
            String mapKey = getMapKey(entryInfoEntity, translateType, forDepartment);
            EntryInfoEntity mapValueEntry = entryEntityMap.get(mapKey);
            //判断map 是否有这个key
            if (Objects.nonNull(mapValueEntry)) {
                entryInfoEntity.setParentID(mapValueEntry.getId());

                if (CollectionUtils.isEmpty(mapValueEntry.getChildren())) {
                    List<EntryInfoEntity> entryInfoEntities1 = new ArrayList<>();
                    entryInfoEntities1.add(entryInfoEntity);
                    mapValueEntry.setChildren(entryInfoEntities1);
                } else {
                    mapValueEntry.getChildren().add(entryInfoEntity);
                }

            } else {
                entryInfoEntity.setParentID("");
                entryEntityMap.put(mapKey, entryInfoEntity);
            }
        }


        Collection<EntryInfoEntity> values = entryEntityMap.values();
        Iterator<EntryInfoEntity> iterator = values.iterator();
        while (iterator.hasNext()) {
            newEntry.add(iterator.next());
        }

        return newEntry;
    }

    public List<EntryInfoEntity> findChildEntry(List<EntryInfoEntity> entryInfoEntities, String translateType) {
        List<EntryInfoEntity> parentEntrys = new ArrayList<>();
        Map<String, EntryInfoEntity> entryMap = new HashMap<>();
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            entryMap.put(entryInfoEntity.getId(), entryInfoEntity);
        }

        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            String parentId = entryInfoEntity.getParentID();
            if (StringUtils.isNotBlank(parentId)) {
                EntryInfoEntity parentEntry = entryMap.get(parentId);
                if (parentEntry != null) {
                    if (parentEntry.getChildren() == null) {
                        parentEntry.setChildren(new ArrayList<>());
                    }
                    parentEntry.getChildren().add(entryInfoEntity);
                    continue;
                }else {
                    parentEntrys.add(entryInfoEntity);
                }

            }else {
                parentEntrys.add(entryInfoEntity);
            }
        }
        return parentEntrys;
    }
}