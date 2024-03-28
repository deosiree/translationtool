package com.shr.translationtoolservice.util;

import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import lombok.extern.slf4j.Slf4j;
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


    public List<EntryInfoEntity> buildRepeEntry(List<EntryInfoEntity> entryInfoEntities,String translateType) {
        List<EntryInfoEntity> newEntry = new ArrayList<>();
        //entry_translate,entryTempEntity
        Map<String, EntryInfoEntity> entryEntityMap = new HashMap<>();
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {

            String entry = entryInfoEntity.getEntry();
            String translate = "";
            //有翻译字段 直接放到map里
            switch (translateType){
                case ConstantInterface.ENGLISH:
                    translate = entryInfoEntity.getEnglish();
                    break;
                case ConstantInterface.SPANISH:
                    translate = entryInfoEntity.getSpanish();
                    break;
                case ConstantInterface.RUSSIAN:
                    translate = entryInfoEntity.getRussian();
                    break;
                case ConstantInterface.FRENCH:
                    translate = entryInfoEntity.getFrench();
                    break;
            }
            EntryInfoEntity mapValueEntry = entryEntityMap.get(entry + ConstantInterface.UNDERLINE + translate);
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
                entryEntityMap.put(entry + ConstantInterface.UNDERLINE + translate, entryInfoEntity);
            }
        }


        Collection<EntryInfoEntity> values = entryEntityMap.values();
        Iterator<EntryInfoEntity> iterator = values.iterator();
        while (iterator.hasNext()) {
            newEntry.add(iterator.next());
        }

        return newEntry;
    }
}