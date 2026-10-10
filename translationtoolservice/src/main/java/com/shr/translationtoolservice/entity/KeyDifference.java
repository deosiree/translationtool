package com.shr.translationtoolservice.entity;

import java.util.HashSet;
import java.util.Set;

public class KeyDifference<K,T>{
    
    // 只在第一个Map中存在的键
    private Set<K> onlyInFirstMapKeys = new HashSet<>();
    // 只在第二个Map中存在的键
    private Set<K> onlyInSecondMapKeys = new HashSet<>();;
    // 两个map中都存在的键, 但是两个键的value的equals方法不同
    private Set<K> allExistButValueDifferentKeys = new HashSet<>();;

    public Set<K> getOnlyInFirstMapKeys() { 
        return onlyInFirstMapKeys; 
    }

    public void setOnlyInFirstMapKeys(Set<K> onlyInFirstMapKeys) { 
        this.onlyInFirstMapKeys = onlyInFirstMapKeys; 
    }

    public void addKeyOnlyInFirst(K key){
        this.onlyInFirstMapKeys.add(key);
        return;
    }

    public Set<K> getOnlyInSecondMapKeys() { 
        return onlyInSecondMapKeys; 
    }

    public void setOnlyInSecondMapKeys(Set<K> onlyInSecondMapKeys) { 
        this.onlyInSecondMapKeys = onlyInSecondMapKeys; 
    }

    public void addKeyOnlyInSecond(K key){
        this.onlyInSecondMapKeys.add(key);
        return;
    }

    public Set<K> getAllExistButValueDifferentKeys() {
        return allExistButValueDifferentKeys;
    }

    public void setAllExistButValueDifferentKeys(Set<K> allExistButValueDifferentKeys) {
        this.allExistButValueDifferentKeys = allExistButValueDifferentKeys;
    }


    @Override
    public String toString() {
        return "MapDiffResult{" +
                "onlyInFirstMapKeys=" + onlyInFirstMapKeys +
                ", onlyInSecondMapKeys=" + onlyInSecondMapKeys +
                ", allExistButValueDifferentKeys=" + allExistButValueDifferentKeys +
                '}';
    }
    

}