package com.shr.translationtoolservice.service.processor.converter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.TLanguage;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler.DictionaryVO;
import com.shr.translationtoolservice.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class DictionaryVOConverter extends AbstractDictionaryVOConverter<DictionaryVO> {


    protected String entrySource;

    protected String taskID;

    protected String productID;

    protected String versionID;

    protected CommonUtils commonUtils;

    Map<String,String> languageSetTranslateMethodMap =  ConstantInterface.entryInfoEntitySetterTranslateMap();


    public DictionaryVOConverter(
        String entrySource, 
        String taskID, 
        String productID, 
        String versionID,
        CommonUtils commonUtils) {
        this.entrySource = entrySource;
        this.taskID = taskID;
        this.productID = productID;
        this.versionID = versionID;
        this.commonUtils = commonUtils;
    }

    /**
     * 针对由辞典导入的词条(由dic导入的)，设定通用基本属性，
     * 并会调用{@link constructCriticalAttributesForEntryInfoEntitiy}设定{@code source},{@code tag} {@code comment}和翻译信息(通用)
     * @param dictionaryVo
     * @param tLanguages
     * @return
     */
    protected EntryInfoEntity constructBasicAttributesForEntryInfoEntitiy(
        DictionaryVO dictionaryVo,
        List<TLanguage> tLanguages
    ){

        EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
        entryInfoEntity.setId(commonUtils.getUUID());
        this.constructCriticalAttributesForEntryInfoEntitiy(entryInfoEntity, dictionaryVo, tLanguages);        
        entryInfoEntity.setEntryState(0);
        entryInfoEntity.setIsDelete(0);

        return entryInfoEntity;
    }

    /**
     * 设定{@code source},{@code tag},{@code comment}以及{@code translation}（例如english,russian等） 
     * 针对由辞典导入的词条(由dic导入的), 根据导入的词条已有的翻译,设定{@code entryInfoEntity}对应语种的翻译信息，以及source,tag,commment(通用)
     * @param entryInfoEntity
     * @param dictionaryVo
     * @param tLanguages
     */
    protected void constructCriticalAttributesForEntryInfoEntitiy(EntryInfoEntity entryInfoEntity,DictionaryVO dictionaryVo,List<TLanguage> tLanguages){
        /* 添加source,tag,comment */
        entryInfoEntity.setEntry(dictionaryVo.getSource());
        entryInfoEntity.setTag(dictionaryVo.getTag());
        entryInfoEntity.setComment(dictionaryVo.getComments());
        /* 添加所有的语种的翻译 */
        Map<String, String> translationMap = dictionaryVo.getTranslation(); // {"en_US" : "xxx","zh_CN": "xxx", "ru_RU" : "xxxx"}

        for(TLanguage tLanguage : tLanguages){
            String langCode = tLanguage.getCode();
            String name = tLanguage.getName();
            String setMethodName = this.languageSetTranslateMethodMap.get(name);
            if(!translationMap.containsKey(langCode)){
                continue;
            }
            try {
                /* 设定语言属性 */
                Method method = entryInfoEntity.getClass().getMethod(setMethodName, String.class);
                String translate = translationMap.get(langCode);
                method.invoke(entryInfoEntity, translate == null ? "" : translate);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                log.error(e.getMessage(), e);
                continue;
            }
        }
    }


}
