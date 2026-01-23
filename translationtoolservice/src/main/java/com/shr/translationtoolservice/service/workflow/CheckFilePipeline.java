package com.shr.translationtoolservice.service.workflow;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.shr.translationtoolservice.dao.EntryClassifyMapper;
import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.dao.ProductRelationMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.DO.EntryInfoEntityDO;
import com.shr.translationtoolservice.service.analyze.BatchMaxLengthTranslateAnalyzer;
import com.shr.translationtoolservice.service.analyze.DefaultTranslateAnalyzer;
import com.shr.translationtoolservice.service.workflow.node.CheckColumnExistWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CheckEntryNotMatchWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CheckMissingEntryRelationWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CheckMissingEntryWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CheckSpecialCharacterWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CheckTranslationMaxLengthWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CheckWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CompareEntityWorkNode;
import com.shr.translationtoolservice.util.CommonUtils.EntityMatchAnalyzer;
import com.shr.translationtoolservice.util.EntryUtils;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils.MethodEntity;

@Component
public class CheckFilePipeline implements Pipeline {
    
    @Autowired
    protected EntryUtils entryUtils;

    @Autowired
    protected EntryInfoMapper entryInfoMapper;

    @Autowired
    protected DefaultTranslateAnalyzer translateAnalyzer;

    @Autowired
    protected ProductRelationMapper productRelationMapper;

    @Autowired
    protected EntryClassifyMapper entryClassifyMapper;

    protected Gson gson = new Gson();

    public CompareEntityWorkNode<EntryInfoEntity> buildCheckMissingEntryWorkNode(Collection<EntryInfoEntity> entryCollection1,Collection<EntryInfoEntity> entryCollection2){
        CheckMissingEntryWorkNode checkMissingEntryWorkNode = new CheckMissingEntryWorkNode(entryUtils);
        checkMissingEntryWorkNode.addCompareData(entryCollection1, entryCollection2);
        return checkMissingEntryWorkNode;
    }

    public CheckMissingEntryRelationWorkNode buildCheckMissingEntryRelationWorkNode(Collection<EntryInfoEntity> entryInfoEntities, Map<String,Set<String>> idRelationMap){
        CheckMissingEntryRelationWorkNode checkMissingEntryRelationWorkNode = new CheckMissingEntryRelationWorkNode();
        checkMissingEntryRelationWorkNode.addCheckData(entryInfoEntities.stream().map(EntryInfoEntity::getId).collect(Collectors.toSet()), idRelationMap);
        return checkMissingEntryRelationWorkNode;
    }

    public CheckEntryNotMatchWorkNode<EntryInfoEntity> buildCheckEntryNotMatchWorkNode(Collection<EntryInfoEntity> fileEntryInfoEntities,Collection<String> attributes,BuildOption buildOption){
        EntryInfoEntityMatchAnalyzer entryInfoEntityMatchAnalyzer = new EntryInfoEntityMatchAnalyzer(true,entryInfoMapper);
        CheckEntryNotMatchWorkNode<EntryInfoEntity> checkEntryNotMatchWorkNode = new CheckEntryNotMatchWorkNode<>(entryInfoEntityMatchAnalyzer);
        checkEntryNotMatchWorkNode.setFileEntryInfoEntities(fileEntryInfoEntities.stream().collect(Collectors.toList()));
        checkEntryNotMatchWorkNode.setMethods(this.getMethodByAttributes(EntryInfoEntity.class, attributes,buildOption));   // ['english','chinese','russian']
        return checkEntryNotMatchWorkNode;
    }

    public CheckSpecialCharacterWorkNode<EntryInfoEntity> buildCheckSpecialCharacterWorkNode(Collection<EntryInfoEntity> entryInfoEntities,Collection<String> attributes,BuildOption buildOption){
        CheckSpecialCharacterWorkNode<EntryInfoEntity> checkSpecialCharacterWorkNode = new CheckSpecialCharacterWorkNode<>(translateAnalyzer);
        checkSpecialCharacterWorkNode.setEntryInfoEntities(entryInfoEntities);
        try {
            checkSpecialCharacterWorkNode.setGetEntryMethodEntity(new MethodEntity(EntryInfoEntity.class.getMethod(MethodUtils.getMethodName("entry"))));
            checkSpecialCharacterWorkNode.setGetTranslationMethods(this.getMethodByAttributes(EntryInfoEntity.class, attributes, buildOption));
            return checkSpecialCharacterWorkNode;
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public CheckTranslationMaxLengthWorkNode buildTranslationMaxLengthWorkNode(Collection<EntryInfoEntity> entryInfoEntities,Collection<String> translateAttributes,BuildOption buildOption){
        BatchMaxLengthTranslateAnalyzer batchMaxLengthTranslateAnalyzer = new BatchMaxLengthTranslateAnalyzer(entryInfoMapper, productRelationMapper,entryClassifyMapper);
        CheckTranslationMaxLengthWorkNode checkTranslationMaxLengthWorkNode = new CheckTranslationMaxLengthWorkNode(batchMaxLengthTranslateAnalyzer);
        checkTranslationMaxLengthWorkNode.setEntryInfoEntities(entryInfoEntities);
        Set<String> getTransMethodNames = translateAttributes.stream().map((transAttr)->{return MethodUtils.getMethodName(transAttr);}).collect(Collectors.toSet());

        Collection<String> translateTypes = new ArrayList<>();
        Map<String, String> entryInfoEntityGetterTranslateMap = ConstantInterface.entryInfoEntityGetterTranslateMap();
        entryInfoEntityGetterTranslateMap.forEach((transType,getMethodName)->{
            if(getTransMethodNames.contains(getMethodName)){
                translateTypes.add(transType);
            }
        });

        checkTranslationMaxLengthWorkNode.addTranslateType(translateTypes); // ['英文','中文','俄文']
        return checkTranslationMaxLengthWorkNode;
    }

    public CheckColumnExistWorkNode buildCheckColumnExistWorkNode(Collection<String> fileColumnNames,Collection<String> setTargetAttributes){
        CheckColumnExistWorkNode checkColumnExistWorkNode=  new CheckColumnExistWorkNode();
        checkColumnExistWorkNode.setAttributeColumnMap(ConstantInterface.constructEntryName());
        checkColumnExistWorkNode.setFileColumnNames(fileColumnNames);
        checkColumnExistWorkNode.setTargetAttributes(setTargetAttributes);
        return checkColumnExistWorkNode;
    }


    /**
     * 执行校验, 并利用checkWorkNode的转换方法转换为pipeline可以处理的格式,然后pipeline统一处理
     */
    public void execute(Collection<CheckWorkNode<?>> checkWorkNodes,ExecuteOption executeOption){
        // fytest_hisdata.run.0_hismdl_alias_HisStaTask
        for(CheckWorkNode<?> checkWorkNode : checkWorkNodes){
            if(checkWorkNode == null){
                throw new RuntimeException("checkWorkNode is null");
            }
            checkWorkNode.check();
            if(checkWorkNode.isPassed()){
                continue;   // 如果校验通过, 就不打印信息
            }else{
                if(executeOption.isReturnAfterOnceFailed()){
                    return;
                }
            }
            
        }

        return;

    }

    /**
     * 根据属性名，获取对应类的方法
     * @param <T>
     * @param clazz
     * @param attributes    clazz属性名的集合
     * @return
     */
    protected <T> Collection<MethodEntity> getMethodByAttributes(Class<T> clazz, Collection<String> attributes,BuildOption buildOption){
        if(attributes == null){
            return null;
        }
        Set<String> avaliableAttributes = buildOption != null ? buildOption.getAvaliableAttributes() : null;
        Collection<MethodEntity> methods = new ArrayList<>();
        attributes.stream().forEach((attribute)->{
            if(avaliableAttributes != null){
                if(!avaliableAttributes.contains(attribute)){
                    return; // 该字段不允许用来进行校验, (程序内部拦截)
                }
            }
            try {
                String methodName = MethodUtils.getMethodName(attribute);
                Method method = clazz.getMethod(methodName);
                methods.add(new MethodEntity(method,null));
            } catch (Exception e){
                throw new RuntimeException(e);
            }
        });
        return methods;
    }

    public static class BuildOption{


        public Set<String> avaliableAttributes = new HashSet<>();

        public Set<String> getAvaliableAttributes() {
            return avaliableAttributes;
        }

        public void setAvaliableAttributes(Set<String> avaliableAttributes) {
            this.avaliableAttributes = avaliableAttributes;
        }

    }

    public static class ExecuteOption{


        boolean returnAfterOnceFailed;

        public boolean isReturnAfterOnceFailed() {
            return returnAfterOnceFailed;
        }

        public void setReturnAfterOnceFailed(boolean returnAfterOnceFailed) {
            this.returnAfterOnceFailed = returnAfterOnceFailed;
        }
    }

    public static class EntryInfoEntityMatchAnalyzer extends EntityMatchAnalyzer<EntryInfoEntity>{

        protected EntryInfoMapper entryInfoMapper;

        public EntryInfoEntityMatchAnalyzer(boolean nullAsEmptyString, EntryInfoMapper entryInfoMapper) {
            super(nullAsEmptyString);
            this.entryInfoMapper = entryInfoMapper;
        }

        public EntryInfoEntityMatchAnalyzer(EntryInfoMapper entryInfoMapper) {
            this.entryInfoMapper = entryInfoMapper;
        }

        @Override
        protected Collection<EntityPairForComparsion<EntryInfoEntity>> getEntityForComparsion(List<EntryInfoEntity> entities) {
            Collection<EntityPairForComparsion<EntryInfoEntity>> entityPairForComparsions = new ArrayList<>();
            if(entities == null){
                return null;
            }
            if(entities.isEmpty()){
                return entityPairForComparsions;
            }
            List<EntryInfoEntityDO> entryInfoDOs = entryInfoMapper.selectEntryInfosByIDs(entities.stream().map(EntryInfoEntity::getId).collect(Collectors.toList()));
            Function<EntryInfoEntity,String> func = (entry)->{return entry.getId();};
            Map<String, List<EntryInfoEntity>> entryInfoIDMap = entities.stream().collect(Collectors.groupingBy(func));
                
            List<EntryInfoEntity> entryInfosInDB = EntryInfoEntityDO.convertFromEntities(entryInfoDOs, (entryDO)->{return EntryInfoEntityDO.convertFromEntity(entryDO);}).collect(Collectors.toList());


            entryInfosInDB.stream().forEach((entryInfoInDB)->{
                String id = entryInfoInDB.getId();
                List<EntryInfoEntity> entryInfos = entryInfoIDMap.get(id);
                if(entryInfos == null || entryInfos.isEmpty()){
                    return; // 词条被删除了, 就没有了
                }
                if(entryInfos.size() > 1){
                    throw new RuntimeException(String.format("同一个词条ID获取到多个结果, 联系研发检查, entryInfoIDMap: %s, ", entryInfoIDMap.toString()));
                }
                EntityPairForComparsion<EntryInfoEntity> entityPairForComparsion = new EntityPairForComparsion<EntryInfoEntity>(entryInfos.get(0),entryInfoInDB);
                entityPairForComparsions.add(entityPairForComparsion);
            });
            return entityPairForComparsions;
        }


    }


}
