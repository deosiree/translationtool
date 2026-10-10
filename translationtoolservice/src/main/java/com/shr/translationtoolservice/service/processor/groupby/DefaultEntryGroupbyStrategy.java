package com.shr.translationtoolservice.service.processor.groupby;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.shr.translationtoolservice.entity.EntryInfoEntity;

public class DefaultEntryGroupbyStrategy extends GeneralGroupbyStrategy<EntryInfoEntity>{

    protected Map<String,Integer> fieldColumnIndexMap = new HashMap<>();

    protected static Map<String,String> attributeFieldMap = new HashMap<>();

    static{
        /* key值前端传递过来的属性名 */
        Function<String,String> fieldAttributeBuilder = (t) -> t;
        attributeFieldMap.put("id", fieldAttributeBuilder.apply("id"));
        attributeFieldMap.put("isExist", fieldAttributeBuilder.apply("isExist"));
        attributeFieldMap.put("entryState", fieldAttributeBuilder.apply("entryState"));
        attributeFieldMap.put("entry", fieldAttributeBuilder.apply("entry"));
        attributeFieldMap.put("tag", fieldAttributeBuilder.apply("tag"));
        attributeFieldMap.put("comment", fieldAttributeBuilder.apply("comment"));
        attributeFieldMap.put("entryVersion", fieldAttributeBuilder.apply("entryVersion"));
        attributeFieldMap.put("entryLength", fieldAttributeBuilder.apply("entryLength"));
        attributeFieldMap.put("entrySource", fieldAttributeBuilder.apply("entrySource"));
        attributeFieldMap.put("chineseInterpretation", fieldAttributeBuilder.apply("chineseInterpretation"));
        attributeFieldMap.put("chinese", fieldAttributeBuilder.apply("chinese"));
        attributeFieldMap.put("chineseTranslateState", fieldAttributeBuilder.apply("chineseTranslateState"));
        attributeFieldMap.put("englishInterpretation", fieldAttributeBuilder.apply("englishInterpretation"));
        attributeFieldMap.put("english", fieldAttributeBuilder.apply("english"));
        attributeFieldMap.put("englishTranslateState", fieldAttributeBuilder.apply("englishTranslateState"));
        attributeFieldMap.put("russianInterpretation", fieldAttributeBuilder.apply("russianInterpretation"));
        attributeFieldMap.put("russian", fieldAttributeBuilder.apply("russian"));
        attributeFieldMap.put("russianTranslateState", fieldAttributeBuilder.apply("russianTranslateState"));
        attributeFieldMap.put("spanishInterpretation", fieldAttributeBuilder.apply("spanishInterpretation"));
        attributeFieldMap.put("spanish", fieldAttributeBuilder.apply("spanish"));
        attributeFieldMap.put("spanishTranslateState", fieldAttributeBuilder.apply("spanishTranslateState"));
        attributeFieldMap.put("frenchInterpretation", fieldAttributeBuilder.apply("frenchInterpretation"));
        attributeFieldMap.put("french", fieldAttributeBuilder.apply("french"));
        attributeFieldMap.put("frenchTranslateState", fieldAttributeBuilder.apply("frenchTranslateState"));
        attributeFieldMap.put("classfy1", fieldAttributeBuilder.apply("classfy1"));
        attributeFieldMap.put("classfy2", fieldAttributeBuilder.apply("classfy2"));
        attributeFieldMap.put("diFileName", fieldAttributeBuilder.apply("diFileName"));
        attributeFieldMap.put("remark", fieldAttributeBuilder.apply("remark"));
        attributeFieldMap.put("srcTabName", fieldAttributeBuilder.apply("srcTabName"));
        attributeFieldMap.put("dbRID", fieldAttributeBuilder.apply("dbRID"));
        attributeFieldMap.put("maxChineseLength", fieldAttributeBuilder.apply("maxChineseLength"));
        attributeFieldMap.put("foreignMaxLength", fieldAttributeBuilder.apply("foreignMaxLength"));
        attributeFieldMap.put("update", fieldAttributeBuilder.apply("update"));
        attributeFieldMap.put("updateTime", fieldAttributeBuilder.apply("updateTime"));
        attributeFieldMap.put("partOfSpeech", fieldAttributeBuilder.apply("partOfSpeech"));
        attributeFieldMap.put("classifyId", fieldAttributeBuilder.apply("classifyId"));
        attributeFieldMap.put("productName", fieldAttributeBuilder.apply("productName"));
        attributeFieldMap.put("versionName", fieldAttributeBuilder.apply("versionName"));
        attributeFieldMap.put("maxLength", fieldAttributeBuilder.apply("maxLength"));
        attributeFieldMap.put("enCharLength", fieldAttributeBuilder.apply("enCharLength"));
        attributeFieldMap.put("zhCharLength", fieldAttributeBuilder.apply("zhCharLength"));
        attributeFieldMap.put("ruCharLength", fieldAttributeBuilder.apply("ruCharLength"));
        attributeFieldMap.put("spaCharLength", fieldAttributeBuilder.apply("spaCharLength"));
        attributeFieldMap.put("fraCharLength", fieldAttributeBuilder.apply("fraCharLength"));
        attributeFieldMap.put("enTransId", fieldAttributeBuilder.apply("enTransId"));
        attributeFieldMap.put("ruTransId", fieldAttributeBuilder.apply("ruTransId"));
        attributeFieldMap.put("spaTransId", fieldAttributeBuilder.apply("spaTransId"));
        attributeFieldMap.put("fraTransId", fieldAttributeBuilder.apply("fraTransId"));
        attributeFieldMap.put("zhTransId", fieldAttributeBuilder.apply("zhTransId"));
        attributeFieldMap.put("importType", fieldAttributeBuilder.apply("importType"));
        attributeFieldMap.put("writeType", fieldAttributeBuilder.apply("writeType"));
        attributeFieldMap.put("entryLabel", fieldAttributeBuilder.apply("entryLabel"));
        attributeFieldMap.put("abbr", fieldAttributeBuilder.apply("abbr"));
    }

    {
        this.createFieldColumnIndexRelation();
    }

    public DefaultEntryGroupbyStrategy() {
    }

    public DefaultEntryGroupbyStrategy(Function<List<Object>, GeneralReplicatedVOType> replicatedVOTypeBuilder) {
        super(replicatedVOTypeBuilder);
    }

    protected void createFieldColumnIndexRelation(){
        Class<EntryInfoEntity> clazz = EntryInfoEntity.class;
        Field[] fields = clazz.getDeclaredFields(); // 每次返回的顺序可能不一样
        for(int idx = 0 ; idx < fields.length ; idx ++ ){
            Field field = fields[idx];
            fieldColumnIndexMap.put(field.getName(),idx);   // 属性名: 属性的值对应的第几列
        }
    }

    public Integer getFieldColumnNumber(String fieldName){
        return fieldColumnIndexMap.get(fieldName);
    }

    @Override
    protected List<Object> asList(EntryInfoEntity targetObject) {
        // TODO Auto-generated method stub
        List<Object> fieldValues = new ArrayList<>();
        // 获取对象的所有字段（包括私有字段）
        Class<?> clazz = targetObject.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            // 允许访问私有字段
            field.setAccessible(true);
            String fieldName = field.getName(); // 字段名作为Map的key
            Object fieldValue = null;
            try {
                fieldValue = field.get(targetObject);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } // 字段值作为Map的value
            Integer idx = fieldColumnIndexMap.get(fieldName);
            if(idx == null){
                throw new RuntimeException(String.format("将object对象转换为List出现异常, 未找到属性的值该存放的位置: %s", fieldName));
            }
            fieldValues.add(idx,fieldValue);
        }
        return fieldValues;
    }

    public void addTargetAttribute(String attribute){

        String fieldName = attributeFieldMap.get(attribute);
        if(fieldName == null){
            throw new RuntimeException(String.format("没有找到对应属性的字段名: %s", attribute));
        }
        Integer idx = this.getFieldColumnNumber(fieldName);
        if(idx == null){
            throw new RuntimeException(String.format("未找到该属性: %s, 对应的字段的值 : %s,存放的位置", attribute,fieldName));
        }
        this.addTargetColumnNumber(idx);
        return;
    }

    public void addTargetAttributes(Collection<String> attributes){
        if(attributes == null){
            return;
        }
        for(String attribute : attributes){
            this.addTargetAttribute(attribute);
        }
        return;
    }

}