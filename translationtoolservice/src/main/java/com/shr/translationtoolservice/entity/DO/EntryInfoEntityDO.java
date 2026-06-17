package com.shr.translationtoolservice.entity.DO;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;

import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.TranslateEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EntryInfoEntityDO {

    public EntryInfoEntity entity;

    public List<TranslateEntity> translateEntities;

    protected static Map<String,String> setTranslateMap = ConstantInterface.entryInfoEntitySetterTranslateMap();

    protected static Map<String,String> setTranslateCharLengthMap = new HashMap<>();

    protected static Map<String,String> setTranslateStateMap = new HashMap<>();

    protected static Function<EntryInfoEntityDO,EntryInfoEntity> entryInfoEntitiyConverter = (entryInfoEntitiyDO) -> {return convertFromEntity(entryInfoEntitiyDO);};

    /* 设定翻译字符长度 */
    static{
        setTranslateCharLengthMap.put(ConstantInterface.CHINESE, "setZhCharLength");
        setTranslateCharLengthMap.put(ConstantInterface.ENGLISH, "setEnCharLength");
        setTranslateCharLengthMap.put(ConstantInterface.FRENCH, "setFraCharLength");
        setTranslateCharLengthMap.put(ConstantInterface.SPANISH, "setSpaCharLength");
        setTranslateCharLengthMap.put(ConstantInterface.RUSSIAN, "setRuCharLength");
    }
    /* 设定翻译状态 */
    static{
        setTranslateStateMap.put(ConstantInterface.CHINESE, "setChineseTranslateState");
        setTranslateStateMap.put(ConstantInterface.ENGLISH, "setEnglishTranslateState");
        setTranslateStateMap.put(ConstantInterface.RUSSIAN, "setRussianTranslateState");
        setTranslateStateMap.put(ConstantInterface.FRENCH, "setFrenchTranslateState");
        setTranslateStateMap.put(ConstantInterface.SPANISH, "setSpanishTranslateState");
    }


    public EntryInfoEntity getEntity() {
        return entity;
    }

    public List<TranslateEntity> getTranslateEntities() {
        return translateEntities;
    }

    public static Function<EntryInfoEntityDO,EntryInfoEntity> newConverterForEntryInfoEntitiy(){
        return entryInfoEntitiyConverter;
    }


    public static EntryInfoEntity convertFromEntity(EntryInfoEntityDO entityDO){
        
        EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
        BeanUtils.copyProperties(entityDO.getEntity(),entryInfoEntity);
        Class<? extends EntryInfoEntity> clazz = EntryInfoEntity.class;
        List<TranslateEntity> translateEntities = entityDO.getTranslateEntities();
        if(translateEntities == null){
            return entryInfoEntity;
        }
        /* 设定翻译 */
        for(TranslateEntity translateEntity : translateEntities){
            String transType = translateEntity.getType();
            String translate = translateEntity.getTranslate();
            String setTranslateMethodName = setTranslateMap.get(transType);
            String setTranslateCharLength = setTranslateCharLengthMap.get(transType);
            String setTranslateState = setTranslateStateMap.get(transType);
            if(setTranslateMethodName == null || setTranslateCharLength == null || setTranslateState == null){
                throw new RuntimeException("警告, 没有找到对应语种翻译的相关方法: " + transType);
            }
            try {
                Method setTranslateMethod = clazz.getMethod(setTranslateMethodName,String.class);
                setTranslateMethod.invoke(entryInfoEntity, translate == null ? "" : translate);
                Method setTranslateCharLengthMethod = clazz.getMethod(setTranslateCharLength, Integer.class);
                setTranslateCharLengthMethod.invoke(entryInfoEntity, translate != null ? translate.length() : -1);
                Method setTranslateStateMethod = clazz.getMethod(setTranslateState, String.class);
                setTranslateStateMethod.invoke(entryInfoEntity, translate == null ? 0 : translateEntity.getTranslateState());
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                throw new RuntimeException(e);
            }
            
        }
        return entryInfoEntity;

    }


    public static <T> Stream<T> convertFromEntities(Collection<EntryInfoEntityDO> entityDOs,Function<EntryInfoEntityDO,T> function){
        return entityDOs.stream().map(function);
    }

}
