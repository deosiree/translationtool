package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.SykEntryVO;
import com.shr.translationtoolservice.service.SykService;
import com.shr.translationtoolservice.service.analyze.AnalyzeSample;
import com.shr.translationtoolservice.service.analyze.DefaultTranslateAnalyzer;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import com.shr.translationtoolservice.util.AsyncUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;


@Service
public class SykServiceImpl implements SykService {
    private static final Logger log = LoggerFactory.getLogger(SykServiceImpl.class);
    @Autowired
    private TranslateMapper translateMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private TaskInfoMapper taskInfoMapper;
    @Autowired
    private VersionMapper versionMapper;
    @Autowired
    private ProductRelationMapper productRelationMapper;
    @Autowired
    private EntryInfoMapper entryInfoMapper;
    @Autowired
    private EntryClassifyMapper entryClassifyMapper;

    @Autowired
    private DefaultTranslateAnalyzer translateAnalyzer;
    
    @Autowired
    private AsyncUtils asyncUtils;


    @Override
    public List<TranslateEntity> getSykEntry(TranslateEntity translate,Set<String> matchList, Integer pageIndex, Integer pageSize) {

        return translateMapper.getSykTrans(translate,matchList, (pageIndex - 1) * pageSize, pageSize);
    }

    @Override
    public List<TranslateEntity> acquireSykSameEntry(TranslateEntity translate,Integer pageIndex, Integer pageSize) {
        // TODO Auto-generated method stub
        List<TranslateEntity> translateEntities = translateMapper.checkSykSameEntry(translate, (pageIndex - 1) * pageSize, pageSize);
        return translateEntities;
        
    }

    @Override
    public List<TranslateEntity> updateSykEntry(List<TranslateEntity> translates) throws Exception {
        /*根据translates的个数来确定是单线程执行 */
        // int taskStrategyLimit = 1;
        int translatesTotalCount = translates.size();
        List<TranslateEntity> failedTranslateEntities = batchUpdate(translates,translatesTotalCount > AsyncUtils.UPDATE_TRANSLATE_LIMIT);
        return failedTranslateEntities;

    }

    public List<TranslateEntity> batchUpdate(List<TranslateEntity> translates,boolean isAsync) throws Exception{

        List<TranslateEntity> failedTranslates = new LinkedList<>();    // 没有找到对应id的翻译记录
        if(!isAsync){
            for(TranslateEntity translateEntity : translates){  
                boolean isSuccess = false;
                try {
                    isSuccess = translateMapper.updateEntity(translateEntity) > 0; // 只要有对应id的记录，就是true
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    isSuccess = false;
                }
                if(!isSuccess){
                    failedTranslates.add(translateEntity);
                }  
            }
        }else{

            Map<Future<Integer>,TranslateEntity> futureMap = new HashMap<>();
            for(TranslateEntity translateEntity : translates){
                Future<Integer> future = asyncUtils.asyncCompute(translateMapper, "updateEntity", TranslateEntity.class, translateEntity);
                // futureList.add(future);
                futureMap.put(future, translateEntity);
            }
            Map<Future<Integer>,Exception> exceptionsMap = new HashMap<>();
            List<Future<Integer>> resultList = asyncUtils.waitForCompleted(new LinkedList<>(futureMap.keySet()),exceptionsMap);

            for(Future<Integer> future : resultList){
                int updateCount = future.get();
                if(updateCount <= 0){
                    failedTranslates.add(futureMap.get(future));
                }
            }
            if(!exceptionsMap.isEmpty()){
                
                for(Entry<Future<Integer>,Exception> entry : exceptionsMap.entrySet()){
                    Exception error = entry.getValue() ;
                    error.printStackTrace();
                    failedTranslates.add(futureMap.get(entry.getKey()));
                }

            }
        }

        return failedTranslates;
    }

    @Override
    public List<SykEntryVO> getSykEntryRelation(List<TranslateEntity> translates) {
        if (CollectionUtils.isEmpty(translates)) {
            return Collections.emptyList();
        }
        List<SykEntryVO> sykEntryVOS = new ArrayList<>();
        for (TranslateEntity translate : translates) {
            String id = translate.getId();
            QueryWrapper<EntryInfoEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_delete", 0);
            switch (translate.getType()){
                case ConstantInterface.CHINESE:
                    queryWrapper.eq("zh_trans_id", id);
                    break;
                case ConstantInterface.ENGLISH:
                    queryWrapper.eq("en_trans_id", id);
                    break;
                case ConstantInterface.FRENCH:
                    queryWrapper.eq("fr_trans_id", id);
                    break;
                case ConstantInterface.SPANISH:
                    queryWrapper.eq("spa_trans_id", id);
                    break;
                case ConstantInterface.RUSSIAN:
                    queryWrapper.eq("ru_trans_id", id);
                    break;
            }
            List<EntryInfoEntity> entities = entryInfoMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(entities)) {
                continue;
            }
            for (EntryInfoEntity entity : entities) {
                QueryWrapper<ProductRelationEntity> queryWrapper1 = new QueryWrapper<>();
                queryWrapper1.eq("entry_id", entity.getId());
                List<ProductRelationEntity> productRelationEntitys = productRelationMapper.selectList(queryWrapper1);

                /* 过滤掉没有关联产品的词条 */
                if (!CollectionUtils.isEmpty(productRelationEntitys)) {
                    ProductRelationEntity productRelationEntity ;
                    if (productRelationEntitys.size() > 1) {
                        log.error("词条关联多个产品 词条id:{}", entity.getId());
                        continue;
                    }else  {
                        productRelationEntity = productRelationEntitys.get(0);
                    }

                    SykEntryVO sykEntryVO = new SykEntryVO();
                    ProductEntity productEntity = productMapper.selectById(productRelationEntity.getProductId());
                    sykEntryVO.setProductName(productEntity.getName());
                    if (StringUtils.isNotBlank(productRelationEntity.getVersionId())) {
                        sykEntryVO.setVersionName(versionMapper.selectById(productRelationEntity.getVersionId()).getName());
                    }
                    TaskInfoEntity taskInfo = taskInfoMapper.selectById(productRelationEntity.getTaskId());
                    if(taskInfo != null)
                        sykEntryVO.setTaskName(taskInfo.getName());
                    sykEntryVO.setId(entity.getId());
                    sykEntryVO.setEntry(translate.getEntry());
                    sykEntryVO.setTranslate(translate.getTranslate());
                    EntryClassify entryClassfyById = entryClassifyMapper.getEntryClassfyById(productEntity.getId());
                    /* 过滤掉没有纳入分类的产品的词条 */
                    if(entryClassfyById == null){
                        continue;
                    }
                    /* 过滤掉术语顶层分类的词条 */
                    EntryClassify entryClassfyByParentId = entryClassifyMapper.getEntryClassfyById(entryClassfyById.getParentId());
                    if(entryClassfyByParentId == null){
                        continue;
                    }
                    sykEntryVO.setClassify(entryClassfyByParentId.getTitle());
                    sykEntryVO.setComment(entity.getComment());
                    sykEntryVO.setAbbr(entity.getAbbr());
                    sykEntryVO.setEntrySource(entity.getEntrySource());
                    sykEntryVO.setDiName(entity.getDiFileName());
                    sykEntryVO.setUserName(entity.getUpdate());
                    sykEntryVO.setCreateTime(entity.getUpdateTime());
                    sykEntryVO.setTag(entity.getTag());
                    sykEntryVO.setEntryState(entity.getEntryState());
                    sykEntryVO.setTranslateState(translate.getTranslateState());
                    sykEntryVOS.add(sykEntryVO);
                }
            }

        }
        return sykEntryVOS;
    }

    @Override
    public List<TranslateEntity> getSykNotUsed(TranslateEntity translateTemplate,String token) {
        // String token = request.getHeader("token");
        // String department = JWTTokenUtils.getDepartment(token);
        String userName = JWTTokenUtils.getUserName(token);
        String department = translateTemplate.getVisualRange();
        // if(department == null || department.equals("")){
        //     // 没有提供查询的部门则查本部门的
        //     translateTemplate.setVisualRange(JWTTokenUtils.getDepartment(token));
        // }
        List<TranslateEntity> translateEntityList = translateMapper.getTransForSykNotUsed(translateTemplate, -1, -1);
        // List<TranslateEntity> translateEntityList = translateMapper.selectList(new QueryWrapper<TranslateEntity>().eq("delete_state", 0).eq("visual_range", department));
        List<EntryInfoEntity> entities = entryInfoMapper.selectList(new QueryWrapper<EntryInfoEntity>().eq("is_delete", 0));
        processTranslateEntity(translateEntityList,entities);

        return translateEntityList.stream().filter(translateEntity -> translateEntity.getDeleteState() == 1).collect(Collectors.toList());
    }

    private void processTranslateEntity(List<TranslateEntity> translateEntityList,List<EntryInfoEntity> entities) {
        int numberOfThreads = 32; // Number of threads to use
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        int chunkSize = translateEntityList.size() / numberOfThreads;
        for (int i = 0; i < numberOfThreads; i++) {
            int start = i * chunkSize;
            int end = (i == numberOfThreads - 1) ? translateEntityList.size() : (i + 1) * chunkSize;
            List<TranslateEntity> sublist = translateEntityList.subList(start, end);

            new Thread(() -> {
                try {
                    for (TranslateEntity translateEntity : sublist) {
                        //开始全删
                        translateEntity.setDeleteState(1);

                        if (CollectionUtils.isEmpty(entities)) {
                            continue;
                        }

                        for (EntryInfoEntity entity : entities) {
                            //如果有一个词条使用了翻译 则不删除
                            if (translateEntity.getId().equals(entity.getEnTransId())
                                    || translateEntity.getId().equals(entity.getFraTransId())
                                    || translateEntity.getId().equals(entity.getSpaTransId())
                                    || translateEntity.getId().equals(entity.getRuTransId())) {
                                translateEntity.setDeleteState(0);
                                break;
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await(); // Wait for all threads to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


    }


    @Override
    public List<TranslateEntity> checkSykEntryByTemplate(TranslateEntity translateEntityTemplate){
        List<TranslateEntity> trans = translateMapper.getTransForCheckSykEntry(translateEntityTemplate,-1,-1);
        List<TranslateEntity> problemEntities = checkSykEntryOnList(trans);
        for(TranslateEntity translateEntity : problemEntities){
            List<EntryInfoEntity> entryByTranslateIDs = entryInfoMapper.getEntryInfoUsingTranslate(translateEntity);
            translateEntity.setNotUsedByEntryInfo(entryByTranslateIDs.isEmpty());
        }
        return problemEntities;
    }

    /**
     * 占位符，{:d}, %1--->% 1校验检测
     * 
     * @return
     */
    public List<TranslateEntity> checkSykEntryOnList(List<TranslateEntity> translateEntities){

        List<TranslateEntity> problemEntities = new LinkedList<>();

        for(TranslateEntity translateEntity: translateEntities){
            String textToTranslate = translateEntity.getEntry();
            String translate = translateEntity.getTranslate();
            AnalyzeSample sample = translateAnalyzer.analyze(translateAnalyzer.prepare(textToTranslate, translate));
            if(!sample.isBad()){
                continue;
            }else{
                problemEntities.add(translateEntity);
            }
        }
        
        return problemEntities;

    }

    @Override
    public List<TranslateEntity> deleteSykEntry(List<TranslateEntity> translateEntities) throws Exception {
        // TODO Auto-generated method stub
        int deleteCounts = translateEntities.size();
        List<TranslateEntity> failedTranslateEntity = new LinkedList<>();
        if(deleteCounts < AsyncUtils.UPDATE_TRANSLATE_LIMIT){
            for(TranslateEntity translateEntity : translateEntities){
                boolean isSuccess = false;
                try {
                    int updateCount = translateMapper.deleteById(translateEntity);   
                    isSuccess = updateCount > 0;
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    isSuccess = false;
                }
                if(!isSuccess){
                    failedTranslateEntity.add(translateEntity);
                }
            }

        }else{
            Map<Future<Integer>,TranslateEntity> futureMap = new HashMap<>();
            for(TranslateEntity translateEntity : translateEntities){
                Future<Integer> future = asyncUtils.asyncCompute(translateMapper, "deleteById", TranslateEntity.class, translateEntity);
                futureMap.put(future, translateEntity);
            }
            Map<Future<Integer>,Exception> exceptionsMap = new HashMap<>();
            List<Future<Integer>> resultList = asyncUtils.waitForCompleted(new LinkedList<>(futureMap.keySet()),exceptionsMap);

            for(Future<Integer> future : resultList){
                int deleteCount = future.get();
                if(deleteCount <= 0){
                    failedTranslateEntity.add(futureMap.get(future));
                }
            }
            if(!exceptionsMap.isEmpty()){
                
                for(Entry<Future<Integer>,Exception> entry : exceptionsMap.entrySet()){
                    Exception error = entry.getValue() ;
                    error.printStackTrace();
                    failedTranslateEntity.add(futureMap.get(entry.getKey()));
                }

            }
        }
        return failedTranslateEntity;
    }

}


