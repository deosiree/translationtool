package com.shr.translationtoolservice.util;

import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.dao.TaskInfoMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.KeyDifference;
import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.shr.translationtoolservice.entity.vo.EntryInfoReplicateCheckVO;
import com.shr.translationtoolservice.entity.vo.ValueDifferenceVO;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler;
import com.shr.translationtoolservice.service.processor.groupby.GeneralGroupbyStrategy;
import com.shr.translationtoolservice.service.processor.groupby.GeneralReplicatedVOType;
import com.shr.translationtoolservice.service.processor.groupby.GroupbyStrategy;
import com.shr.translationtoolservice.service.processor.groupby.GroupbyStrategy.ReplicatedVOType;

import edu.washington.cs.knowitall.logic.Expression.Paren.R;
import lombok.extern.slf4j.Slf4j;

import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EntryUtils {
    @Autowired
    private TaskInfoMapper taskInfoMapper;
    @Autowired
    private CommonUtils commonUtils;
    @Autowired
    private EntryInfoMapper entryInfoMapper;

    public static boolean checkEntryID(EntryInfoEntity entity,boolean throwExceptionIfNotExist){
        String id = entity.getId();
        if(id == null || id.trim().isEmpty()){
            if(throwExceptionIfNotExist){
                throw new RuntimeException(String.format("存在词条对象的id为null, 检查输入的内容"));
            }
            return false;
        }
        return true;
    }

    @Autowired
    private EntryProcessUtils entryProcessUtils;
    public void caseExistEntry(List<EntryInfoEntity> newEntry, String taskID) {


    TaskInfoEntity taskInfoEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

    //  ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(taskInfoEntity.getProductId());
    String productTableName = "t_entry_info";

    for (EntryInfoEntity entryInfoEntity : newEntry) {
        if (!CollectionUtils.isEmpty(entryInfoEntity.getChildren())) {
            caseExistEntry(entryInfoEntity.getChildren(), taskID);
        }
        // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());


        List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryList(productTableName, entryInfoEntity, taskInfoEntity.getProductId());



        if (CollectionUtils.isEmpty(entryEntities)) {
            //创建新翻译
            entryInfoEntity.setIsExist(0);
            entryInfoEntity.setEntryVersion(1);
            entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
        } else {
            entryInfoEntity.setIsExist(1);
            entryInfoEntity.setEntryVersion(entryEntities.stream().max(Comparator.comparing(EntryInfoEntity::getEntryVersion)).get().getEntryVersion());
            entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
        }
    }

}

    public void caseNewEntry(List<EntryInfoEntity> exportEntry, String taskID,ArrayList<EntryInfoEntity> newEntry ) {


        TaskInfoEntity taskInfoEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

        //  ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(taskInfoEntity.getProductId());
        String productTableName = "t_entry_info";

        for (EntryInfoEntity entryInfoEntity : exportEntry) {
            if (!CollectionUtils.isEmpty(entryInfoEntity.getChildren())) {
                caseNewEntry(entryInfoEntity.getChildren(), taskID,newEntry);
            }
            // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());


            List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryList(productTableName, entryInfoEntity, taskInfoEntity.getProductId());



            if (CollectionUtils.isEmpty(entryEntities)) {
                //创建新翻译
                entryInfoEntity.setIsExist(0);
                entryInfoEntity.setEntryVersion(1);
                entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
                newEntry.add(entryInfoEntity);
            }
        }

    }

    /**
     * 检查当前这批词条是否与库中某一个任务下的词条是否存在重复, 并利用{@link CheckExistEntryConsumer}的相关方法对重复的和不重复的词条进行处理
     * 检查当前这批词条是否与库中某一个产品下的词条是否存在重复, 并利用{@link CheckExistEntryConsumer}的相关方法对重复的和不重复的词条进行处理
     * 检查当前这批词条是否与库中的词条是否存在重复, 并利用{@link CheckExistEntryConsumer}的相关方法对重复的和不重复的词条进行处理
     * @param entryInfoEntities
     * @param productID 如果填写, 则检查当前这批词条是否与库中某一个产品下的词条是否存在重复
     * @param taskID 如果填写, 则检查当前这批词条是否与库中某一个任务下的词条是否存在重复
     * @param consumer 对重复的和不重复的词条进行后处理的类
     */
    @Transactional
    protected void caseExistEntry(List<EntryInfoEntity> entryInfoEntities, String productID,String taskID,CheckExistEntryConsumer consumer){
        if(consumer == null){
            return ;
        }
        Map<EntryInfoReplicateCheckVO,List<EntryInfoEntity>> entryInfosGroup = new HashMap<>();
        Set<EntryInfoReplicateCheckVO> entryInfoReplicateCheckVOs = new HashSet<>();
        for(EntryInfoEntity t : entryInfoEntities){
            EntryInfoReplicateCheckVO entryInfoReplicateCheckVO = EntryInfoReplicateCheckVO.convertFrom(t);
            /* 将词条分配到对应的组 */
            List<EntryInfoEntity> entryInfos = entryInfosGroup.get(entryInfoReplicateCheckVO);
            entryInfoReplicateCheckVOs.add(entryInfoReplicateCheckVO);
            if(entryInfos == null){
                entryInfos = new ArrayList<>();
                entryInfos.add(t);
                entryInfosGroup.put(entryInfoReplicateCheckVO, entryInfos); // 必须所有属性都设定好在存入map或set，否则hash，equals方法存在问题
            }else{
                entryInfos.add(t);
            }
        }

        Set<EntryInfoReplicateCheckVO> replicatedExists = entryInfoMapper.newGetEntryInfoExists(entryInfoReplicateCheckVOs,productID,taskID);   // 这个底层逻辑还有点不同，无法直接给workbench使用
        List<EntryInfoEntity> exitEntryInfos = new ArrayList<>();
        List<EntryInfoEntity> notExistEntryInfos = new ArrayList<>();

        for(Map.Entry<EntryInfoReplicateCheckVO,List<EntryInfoEntity>> entryInfo : entryInfosGroup.entrySet()){
            EntryInfoReplicateCheckVO checkVO = entryInfo.getKey();
            if(replicatedExists.contains(checkVO)){
                /* 存在 */
                exitEntryInfos.addAll(entryInfo.getValue());
            }else{
                notExistEntryInfos.addAll(entryInfo.getValue());
            }
        }
        consumer.processExistEntry(exitEntryInfos);
        consumer.processNotExistEntry(notExistEntryInfos);
        return;
    }

    /**
     * 判断当前的entryInfoEntitiy是否与库中对应任务中的词条重复,利用{@link EntryInfoMapper#newGetEntryInfoExists(Set, String, String)}实现;
     * 如果重复，给当前的entryInfoEntitiy添加相关默认属性{@link EntryInfoEntity#setIsExist(Integer)},{@link EntryInfoEntity#setEntryVersion(Integer)},{@link EntryInfoEntity#setEntryVersionID(String)}
     * 默认entryInfoEntities没有子节点,即{@link EntryInfoEntity#getChildren()}内部为空
     * @param entryInfoEntities
     * @param taskInfoEntity
     */
    @Transactional
    protected void caseExistEntry(List<EntryInfoEntity> entryInfoEntities, TaskInfoEntity taskInfoEntity) {
        
        Map<EntryInfoReplicateCheckVO,List<EntryInfoEntity>> entryInfosGroup = new HashMap<>();
        Set<EntryInfoReplicateCheckVO> entryInfoReplicateCheckVOs = new HashSet<>();
        for(EntryInfoEntity t : entryInfoEntities){
            EntryInfoReplicateCheckVO entryInfoReplicateCheckVO = EntryInfoReplicateCheckVO.convertFrom(t);
            /* 将词条分配到对应的组 */
            List<EntryInfoEntity> entryInfos = entryInfosGroup.get(entryInfoReplicateCheckVO);
            entryInfoReplicateCheckVOs.add(entryInfoReplicateCheckVO);
            if(entryInfos == null){
                entryInfos = new ArrayList<>();
                entryInfos.add(t);
                entryInfosGroup.put(entryInfoReplicateCheckVO, entryInfos); // 必须所有属性都设定好在存入map或set，否则hash，equals方法存在问题
            }else{
                entryInfos.add(t);
            }

            /* 设定默认值，如果发现存在，再后面修改 */
            t.setIsExist(0);
            t.setEntryVersion(1);
            t.setEntryVersionID(commonUtils.getUUID());
        }
        
        Set<EntryInfoReplicateCheckVO> replicatedExists = entryInfoMapper.newGetEntryInfoExists(entryInfoReplicateCheckVOs,null,taskInfoEntity.getId());
        for(EntryInfoReplicateCheckVO checkVO : replicatedExists){
            List<EntryInfoEntity> entryList = entryInfosGroup.get(checkVO);
            if(entryList == null){
                log.warn("警告,没有找到对应的词条, 重复校验时各字段信息为: " + checkVO.toString());
                continue;
            }
            String entryVersionID = checkVO.getEntryInfoIDsForMaxEntryVersion().isEmpty() ? "" : checkVO.getEntryInfoIDsForMaxEntryVersion().get(0);
            entryList.stream().forEach(new Consumer<EntryInfoEntity>() {

                @Override
                public void accept(EntryInfoEntity t) {
                    t.setEntryVersion(Integer.parseInt(checkVO.getMaxEntryVersion()) + 1);
                    t.setIsExist(1);
                    t.setEntryVersionID(entryVersionID);

                }
            });

        }
        return;

    }

    /**
     * 判断当前的entryInfoEntitiy是否与库中的词条重复, 如果重复，给当前的entryInfoEntitiy添加相关属性(通用的)
     * 默认entryInfoEntities没有子节点, childern里面是空的, 实际调用的是{@link #caseExistEntry(List, TaskInfoEntity)}, 该方法的目的在词条较多时, 采用多线程并发加快计算速度
     * 该方法被{@link BatchInsertEntryHandler#createEntryFromTSFiles(String, List, TaskInfoEntity, List, String, String)}和{@link BatchInsertEntryHandler#createEntryFromTSFiles(String, List, TaskInfoEntity, List, String, String)}使用
     * @param entryInfoEntities
     * @param taskInfoEntity
     * @param executor
     */
    @Transactional
    public void caseExistEntry(List<EntryInfoEntity> entryInfoEntities, TaskInfoEntity taskInfoEntity,ThreadPoolExecutor executor) {
        int batchSize = 10;

        if(executor == null){
            this.caseExistEntry(entryInfoEntities, taskInfoEntity);
            return;
        }
        int totalNum = entryInfoEntities.size();
        int taskNumber = (totalNum + batchSize - 1) / batchSize;
        log.info(String.format("任务分片共%s个", String.valueOf(taskNumber)));
        CountDownLatch countDownLatch = new CountDownLatch((totalNum + batchSize - 1) / batchSize); // 任务总数=ceil(totalNum/batchSize)
        Set<String> errorMessages = new HashSet<>();
        for(int idx = 0 ; idx < totalNum ; idx += batchSize){
            int currentIdx = idx;
            int endIdx = Math.min(currentIdx + batchSize, totalNum);
            List<EntryInfoEntity> batchList = entryInfoEntities.subList(currentIdx, endIdx);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        // 执行分片处理（传入提前截取的batchList，避免闭包引用问题）
                        caseExistEntry(batchList, taskInfoEntity);
                    } catch (Exception e) {
                        // 日志记录：建议用日志框架（SLF4J），这里简化打印
                        log.error("分片任务执行失败，当前分片索引：" + currentIdx + "-" + endIdx + "，异常信息：" + e.getMessage(),e);
                        errorMessages.add("分片任务执行失败，当前分片索引：" + currentIdx + "-" + endIdx + "，异常信息：" + e.getMessage());
                    } finally {
                        // 计数器减1（无论任务成功失败，都要计数）
                        countDownLatch.countDown();
                    }
                }
            });
            
        }
        try {
            // 4. 同步等待所有任务执行完成（根据业务需求选择：如果需要等待结果，就加这行；如果纯异步，可去掉）
            // 超时时间：避免无限等待（根据业务调整，比如1小时）
            boolean allDone = countDownLatch.await(3, TimeUnit.HOURS);
            if (!allDone) {

                throw new RuntimeException("部分分片任务执行超时，可能存在未完成的任务");
            } else {
                log.info(String.format("所有分片任务执行完成,成功的共%s,失败的共%s", String.valueOf(taskNumber - errorMessages.size()),String.valueOf(errorMessages.size())));
                if(!errorMessages.isEmpty()){
                    throw new RuntimeException("插入数据时出现异常, 异常信息为: " + errorMessages.toString());
                }
            }
        } catch (InterruptedException e) {
            log.warn("等待任务执行时被中断：" + e.getMessage());
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new RuntimeException(e);
        }
    
    }

    public void createNewTrans(EntryInfoEntity entryInfoEntity, String translateType, String translate) {
        //写入翻译字段
        switch (translateType) {
            case ConstantInterface.CHINESE:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setChinese(translate);
                    entryInfoEntity.setChineseTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.ENGLISH:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setEnglish(translate);
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setSpanish(translate);
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setRussian(translate);
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setFrench(translate);
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
        }
    }

    public List<EntryInfoEntity> buildRepeTempEntry(List<EntryInfoEntity> entryInfoEntities, String translateType) {
        return entryProcessUtils.buildRepeEntry(entryInfoEntities, translateType);
    }


    /**
     * 将词条分组, 返回的map中每一个key-value对应一组词条, value对应的是一组词条, key代表用于分类的对象
     * @param <E>
     * @param <T>
     * @param entryInfoEntities
     * @param groupbyStrategy
     * @return
     */
    public <E extends EntryInfoEntity,T extends ReplicatedVOType> Map<T, List<E>> makeGroupMapForEntryInfoEntities(Collection<E> entryInfoEntities,GroupbyStrategy<E,T> groupbyStrategy){
        if(groupbyStrategy == null){
            return null;
        }
        Map<T, List<E>> entryInfosGroupMap = entryInfoEntities.stream().collect(Collectors.groupingBy(groupbyStrategy.newInstance()));
        return entryInfosGroupMap;
    }


    /**
     * 将词条分组，返回的列表中的每一个元素代表一组词条
     * @param <E>
     * @param <R>
     * @param entryInfoEntities
     * @param groupbyStrategy   分组策略
     * @return
     */
    public <E extends EntryInfoEntity,T extends ReplicatedVOType> Collection<List<E>> makeGroupForEntryInfoEntities(Collection<E> entryInfoEntities,GroupbyStrategy<E,T> groupbyStrategy){
        
        Map<T, List<E>> entryInfosGroupMap = this.makeGroupMapForEntryInfoEntities(entryInfoEntities, groupbyStrategy);
        if(entryInfosGroupMap == null){
            return null;
        }
        Collection<List<E>> entryInfoGroups = new ArrayList<>(entryInfosGroupMap.size());
        for(Map.Entry<T,List<E>> entity : entryInfosGroupMap.entrySet()){
            List<E> replicatedEntryInfos = entity.getValue();
            if(replicatedEntryInfos.isEmpty()){
                log.warn("警告, 存在一组词条，但该组词条没有词条");
                continue;
            }
            entryInfoGroups.add(replicatedEntryInfos);
        }
        return entryInfoGroups;
    }

    public <E extends EntryInfoEntity,T extends ReplicatedVOType> KeyDifference<T,E> compareEntryInfos(Map<T, List<E>> entryInfosGroupMap1, Map<T, List<E>> entryInfosGroupMap2){
        return commonUtils.compareMap(entryInfosGroupMap1, entryInfosGroupMap2);
    }
    
    public ValueDifferenceVO<EntryInfoEntity> compareEntryInfos(
        Collection<EntryInfoEntity> entryCollection1,
        Collection<EntryInfoEntity> entryCollection2,
        GeneralGroupbyStrategy<EntryInfoEntity> entryGroupbyStrategy
    ){
        /* 
            根据属性，先将每个文件中的词条进行分组
        */
        Map<GeneralReplicatedVOType, List<EntryInfoEntity>> entryInfosGroupMap1 = this.makeGroupMapForEntryInfoEntities(entryCollection1, entryGroupbyStrategy);
        Map<GeneralReplicatedVOType, List<EntryInfoEntity>> entryInfosGroupMap2 = this.makeGroupMapForEntryInfoEntities(entryCollection2, entryGroupbyStrategy);
        // 获取两个文件的词条的差异信息
        KeyDifference<GeneralReplicatedVOType, EntryInfoEntity> compareResult = this.compareEntryInfos(entryInfosGroupMap1, entryInfosGroupMap2);
        // 根据差异信息封装成对象返回
        ValueDifferenceVO<EntryInfoEntity> valueDifferenceVO = new ValueDifferenceVO<>();
        // 1. 处理仅file1存在的各组词条
        Set<GeneralReplicatedVOType> refOnlyInFirstMapKeys = compareResult.getOnlyInFirstMapKeys();
        if(refOnlyInFirstMapKeys != null)
            refOnlyInFirstMapKeys.forEach((key)->{valueDifferenceVO.addValuesOnlyInFirst(entryInfosGroupMap1.get(key));});
        // 2. 处理仅file2存在的各组词条
        Set<GeneralReplicatedVOType> refOnlyInSecondMapKeys = compareResult.getOnlyInSecondMapKeys();
        if(refOnlyInSecondMapKeys != null)
            refOnlyInSecondMapKeys.forEach((key)->{valueDifferenceVO.addValueOnlyInSecond(entryInfosGroupMap2.get(key));});
        // 2. 处理file1,file2都存在的各组词条
        Set<GeneralReplicatedVOType> refAllExistButValueDifferentKeys = compareResult.getAllExistButValueDifferentKeys();
        if(refAllExistButValueDifferentKeys != null)
            refAllExistButValueDifferentKeys.forEach((key)->{
                ValueDifferenceVO.ListDiff<EntryInfoEntity> listDiff = new ValueDifferenceVO.ListDiff<>();
                listDiff.setFirstList(entryInfosGroupMap1.get(key));
                listDiff.setSecondList(entryInfosGroupMap2.get(key));
                valueDifferenceVO.addDifferentBetweenEachOther(listDiff);
            });

        return valueDifferenceVO;
    }


    public static abstract class CheckExistEntryConsumer{

        public abstract void processExistEntry(List<EntryInfoEntity> existEntryInfoEntities);

        public abstract void processNotExistEntry(List<EntryInfoEntity> notExistEntryInfoEntities);

    }

}
