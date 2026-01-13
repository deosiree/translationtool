package com.shr.translationtoolservice.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.stereotype.Component;
import com.shr.translationtoolservice.entity.KeyDifference;

import java.util.ArrayList;
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

}
