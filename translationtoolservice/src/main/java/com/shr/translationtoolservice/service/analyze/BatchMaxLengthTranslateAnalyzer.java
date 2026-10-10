package com.shr.translationtoolservice.service.analyze;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.shr.translationtoolservice.dao.EntryClassifyMapper;
import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.dao.ProductRelationMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryClassify;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.ProductRelationEntity;
import com.shr.translationtoolservice.entity.DO.EntryInfoEntityDO;

public class BatchMaxLengthTranslateAnalyzer implements TranslateAnalyzer<Collection<EntryInfoEntity>> {

    public static Map<String,String> getterMap = ConstantInterface.entryInfoEntityGetterTranslateMap();

    protected final String nullKey = "NULL";

    protected List<String> targetLanguageType = new ArrayList<>();

    protected Collection<EntryInfoEntity> problematicEntryInfoEntities = new ArrayList<>();

    protected EntryInfoMapper entryInfoMapper;

    protected ProductRelationMapper productRelationMapper;

    protected EntryClassifyMapper entryClassifyMapper;
    

    public BatchMaxLengthTranslateAnalyzer(EntryInfoMapper entryInfoMapper,ProductRelationMapper productRelationMapper,EntryClassifyMapper entryClassifyMapper) {
        this.entryInfoMapper = entryInfoMapper;
        this.productRelationMapper=  productRelationMapper;
        this.entryClassifyMapper = entryClassifyMapper;
    }

    public boolean addLanguageType(String languageType){
        if(!getterMap.containsKey(languageType)){
            return false;
        }
        targetLanguageType.add(languageType);
        return true;
    }

    public Collection<EntryInfoEntity> getProblematicEntryInfoEntities() {
        return problematicEntryInfoEntities;
    }

    protected boolean analyzeByEntryMaxLength(Collection<String> translates,Integer maxLength){
        if(maxLength == null || translates == null){
            return true;
        }
        for(String translate : translates){
            if(translate.length() > maxLength){
                return false;
            }
        }

        return true;    // 没问题
    }

    @Override
    public boolean analyze(Collection<EntryInfoEntity> analyzeSample) {
        // TODO Auto-generated method stub
        /**
         * 1. 根据词条的max_length长度比对,返回结果
         * 2. 如果为null，则根据词条分类的字符长度进行判断
         * 3. 如果最大长度为null,小于等于0，就跳过不校验
         */
        boolean hasProblemEntry = false;
        Set<String> ids = analyzeSample.stream().map(EntryInfoEntity::getId).collect(Collectors.toSet());
        if(ids.isEmpty()){
            return false;
        }
        List<EntryInfoEntity> entryByIDs = EntryInfoEntityDO.convertFromEntities(
            entryInfoMapper.selectEntryInfosByIDs(ids.stream().collect(Collectors.toList())), 
            (entryDO)->{return EntryInfoEntityDO.convertFromEntity(entryDO);}).collect(Collectors.toList());
        if(entryByIDs == null){
            throw new NullPointerException("根据id查询不到对应的词条");
        }
        /* key: 词条ID ，value: 词条的最大长度 */
        Map<String,Integer> maxLengthMap = new HashMap<>();
        for(EntryInfoEntity entryInfoEntity : entryByIDs){
            Integer entryMaxLength = entryInfoEntity.getMaxLength();
            if(entryMaxLength == null || entryMaxLength <= 0){
                continue;
            }
            maxLengthMap.put(entryInfoEntity.getId(), entryInfoEntity.getMaxLength());
        }
        /* 禁用将禁用信息添加到备注里面 */
        /**
         * 先获取词条对应的产品，然后获取该产品下所有的模块，模块对应一级分类,匹配到就根据一级分类对应的最大字符长度进行匹配
         * key: 词条ID，values是产品
         */
        Map<String, ProductRelationEntity> productionRelationsByIDs = productRelationMapper.getProductionRelationsByIDs(new ArrayList<>(ids));
        Set<String> productIDs = productionRelationsByIDs.values().stream().map(ProductRelationEntity::getProductId).collect(Collectors.toSet());
        /* 获取每个产品下的所有的模块,key是分类(产品)的id,values是产品下所有的模块 */
        Map<String, List<EntryClassify>> entryClassfyDOByIds = entryClassifyMapper.getEntryClassfyDOByIds(new ArrayList<>(productIDs)).stream()
            // .collect(Collectors.groupingBy(EntryClassify::getParentId));
            .collect(Collectors.groupingBy(item -> item.getParentId() != null ? item.getParentId() : this.nullKey));
        for(EntryInfoEntity entity : entryByIDs){
            String entryID =  entity.getId();
            String classfyName = entity.getClassfy1();
            if(entryID == null || classfyName == null){
                continue;   
            }
            Integer currentMaxLength = maxLengthMap.get(entryID);
            ProductRelationEntity productRelationEntity = productionRelationsByIDs.get(entryID);
            if(productRelationEntity == null){
                continue;   // 这个词条没有关联产品
            }

            String productID = productRelationEntity.getProductId();
            if(productID == null){
                continue;   // 数据存在异常
            }
            List<EntryClassify> entryClassifyDOs = entryClassfyDOByIds.get(productID);
            if(entryClassifyDOs == null){
                continue;   // 这个词条对应的产品没有模块
            }
            Map<String,List<EntryClassify>> entryClassifyDOMap = entryClassifyDOs.stream()
                // .collect(Collectors.groupingBy(EntryClassify::getTitle));
                .collect(Collectors.groupingBy(item -> item.getTitle() != null ? item.getTitle() : this.nullKey)); 
            /* 寻找与词条一级分类名称相同的模块 */
            List<EntryClassify> entryClassifyDOList = entryClassifyDOMap.get(classfyName);
            if(entryClassifyDOList == null){
                continue;   // 没找到同名的一级分类
            }
            for(EntryClassify entryClassify : entryClassifyDOList){
                Integer foreignMaxByte = entryClassify.getForeignMaxByte();
                if(foreignMaxByte == null || foreignMaxByte <= 0){
                    continue;   // 值不规范
                }else{
                    if(currentMaxLength == null || (currentMaxLength != null && foreignMaxByte > currentMaxLength)){
                        maxLengthMap.put(entryID, foreignMaxByte);
                        currentMaxLength = foreignMaxByte;
                    }
                }
            }
        }


        for(EntryInfoEntity entity : analyzeSample){
            Integer maxLength = maxLengthMap.get(entity.getId());
            if(maxLength == null || maxLength <= 0){
                continue;
            }
            Collection<String> translates = new ArrayList<>(5);
            for(String lang : this.targetLanguageType){
                try {
                    translates.add(String.valueOf(entity.getClass().getMethod(getterMap.get(lang)).invoke(entity)));
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException e) {
                    // TODO Auto-generated catch block
                    throw new RuntimeException(e);
                }
            }

            if(!analyzeByEntryMaxLength(translates, maxLength)){
                this.problematicEntryInfoEntities.add(entity);
                hasProblemEntry = true;
                continue;
            }
            
        }
        

        return hasProblemEntry;    // 没有问题词条
    }


}
