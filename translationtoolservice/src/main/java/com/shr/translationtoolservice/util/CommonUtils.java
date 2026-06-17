package com.shr.translationtoolservice.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.stereotype.Component;
import com.shr.translationtoolservice.entity.KeyDifference;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils.MethodEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CommonUtils {
    /**
     * 获取uuid
     * @return
     */
    public String getUUID(){
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }
    public boolean checkPage(int pageIndex, int pageSize)
    {
        return StringUtils.isNotBlank(String.valueOf(pageIndex)) && StringUtils.isNotBlank(String.valueOf(pageSize));
    }


    /**
     * 比对两个List是否相等（忽略顺序，元素完全一致）
     * @param list1 第一个List
     * @param list2 第二个List
     * @return true=相等，false=不相等
     */
    private <E> boolean areListsEqual(List<E> list1, List<E> list2) {
        // 长度不同直接返回false
        if (list1.size() != list2.size()) {
            return false;
        }
        // 转为Set比对（注意：如果List中有重复元素，需用频率统计）
        Map<E, Long> frequency1 = list1.stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        Map<E, Long> frequency2 = list2.stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        return frequency1.equals(frequency2);
    }

    public <E,T> KeyDifference<T,E> compareMap(Map<T, List<E>> entryInfosGroupMap1, Map<T, List<E>> entryInfosGroupMap2){

        if(entryInfosGroupMap1 == null || entryInfosGroupMap2 == null){
            return null;
        }

        // 1. 获取两个Map的键集合
        Set<T> keys1 = entryInfosGroupMap1.keySet();
        Set<T> keys2 = entryInfosGroupMap2.keySet();

        // 2. 找出只在第一个Map存在的键
        Set<T> onlyInFirst = keys1.stream()
                .filter(key -> !keys2.contains(key))
                .collect(Collectors.toSet());

        // 3. 找出只在第二个Map存在的键
        Set<T> onlyInSecond = keys2.stream()
                .filter(key -> !keys1.contains(key))
                .collect(Collectors.toSet());

        // 4. 找出共同存在的键，并比对对应List的差异
        Set<T> commonKeys = keys1.stream()
                .filter(keys2::contains)
                .collect(Collectors.toSet());
        Set<T> allExistButValueDifferentKeys = new HashSet<>();
        for (T key : commonKeys) {
            List<E> list1 = entryInfosGroupMap1.get(key);
            List<E> list2 = entryInfosGroupMap2.get(key);

            // 处理List为空的情况(命名前面加safe前缀)
            List<E> safeList1 = list1 == null ? new ArrayList<>() : new ArrayList<>(list1);
            List<E> safeList2 = list2 == null ? new ArrayList<>() : new ArrayList<>(list2);

            // 比对两个List是否相等（忽略顺序，只看元素是否完全一致）
            if (!areListsEqual(safeList1, safeList2)) {
                allExistButValueDifferentKeys.add(key);
            }
        }

        KeyDifference<T,E> keyDifference = new KeyDifference<>();
        keyDifference.setOnlyInFirstMapKeys(onlyInFirst);
        keyDifference.setOnlyInSecondMapKeys(onlyInSecond);
        keyDifference.setAllExistButValueDifferentKeys(allExistButValueDifferentKeys);
        return keyDifference;
    }

    /**
     * 查询T对象与其数据库中存储的T对象的相关字段的值是否一致
     * 例如查看文件解析获得的{@link EntryInfoEntity}对象对应字段的值与库中的值是否一致
     * (首先获取文件中词条对应的ID，然后根据该ID查看该词条在库中各属性的值，然后与文件中对应属性的值比较，查看是否有所不同)
     * 例如查看词条ID为"xfed",文件中tag字段的值是否与库中tag字段的值一致
     */
    public static abstract class EntityMatchAnalyzer<T>{


        protected boolean nullAsEmptyString;


        public EntityMatchAnalyzer() {
        }
        
        public EntityMatchAnalyzer(boolean nullAsEmptyString) {
            this.nullAsEmptyString = nullAsEmptyString;
        }

        public void setNullAsEmptyString(boolean nullAsEmptyString) {
            this.nullAsEmptyString = nullAsEmptyString;
        }

        /**
         * 比对两个对象的指定方法获取的值是否一样
         * @param <T>
         * @param object1
         * @param object2
         * @param getMethods
         * @return
         */
        public boolean isFieldEqual(T object1,T object2,Collection<MethodEntity> getMethods){
            if(getMethods == null){
                throw new NullPointerException("getMethods is Null");
            }
            for(MethodEntity methodEntity : getMethods){
                Object value1,value2;
                try {
                    value1 = methodEntity.getMethod().invoke(object1,methodEntity.getParams());
                    value2 = methodEntity.getMethod().invoke(object2,methodEntity.getParams());
                    if(value1 == null && value2 == null){
                        continue;
                    }else if((value1 != null && value2 == null)){
                        if(!nullAsEmptyString){
                            return false;
                        }
                        return value1.equals("");
                    }else if((value1 == null && value2 != null)){
                        if(!nullAsEmptyString){
                            return false;
                        }
                        return value2.equals("");
                    }
                    if(!value1.equals(value2)){
                        return false;
                    }
                } catch(Exception e){
                    throw new RuntimeException(e);
                }
            }
            return true;
        }

        /**
         * 利用{@link #isFieldEqual(Object, Object, Collection)}获取其中不匹配的entity对
         * @param <T>
         * @param entityPairForComparsions
         * @param methods
         * @return
         */
        public List<EntityPairForComparsion<T>> getNotMatchEntities(Collection<EntityPairForComparsion<T>> entityPairForComparsions,Collection<MethodEntity> methods){
            List<EntityPairForComparsion<T>> notMatchEntryInfosCollection = new ArrayList<>();
            for(EntityPairForComparsion<T> entityPairForComparsion : entityPairForComparsions){
                if(isFieldEqual(entityPairForComparsion.getEntity1(), entityPairForComparsion.getEntity2(), methods)){
                    continue;
                }
                notMatchEntryInfosCollection.add(entityPairForComparsion);
            }
            return notMatchEntryInfosCollection;
        }


        protected abstract Collection<EntityPairForComparsion<T>> getEntityForComparsion(List<T> entities);

        public List<EntityPairForComparsion<T>> getNotMatchEntity(List<T> entities,Collection<MethodEntity> methods){

            int size = entities.size();
            int batchSize = 100;
            List<EntityPairForComparsion<T>> notMatchEntryInfosCollection = new ArrayList<>();
            for(int leftIdx = 0 ; leftIdx < size ; leftIdx += batchSize){
                int rightIdx = leftIdx + batchSize > size ? size : leftIdx + batchSize;
                List<T> subList = entities.subList(leftIdx, rightIdx).stream().collect(Collectors.toList());

                Collection<EntityPairForComparsion<T>> entityForComparsions = this.getEntityForComparsion(subList);
                if(entityForComparsions == null){
                    continue;
                }
                List<EntityPairForComparsion<T>> notMatchEntities = getNotMatchEntities(entityForComparsions, methods);
                notMatchEntryInfosCollection.addAll(notMatchEntities);
            }
            return notMatchEntryInfosCollection;
        }


        public static class EntityPairForComparsion<T>{

            T entity1;

            T entity2;

            public EntityPairForComparsion(T entity1,T entity2){
                this.entity1 = entity1;
                this.entity2 = entity2;
            }

            public T getEntity1() {
                return entity1;
            }

            public T getEntity2() {
                return entity2;
            }

            
        }
    }

    

}
