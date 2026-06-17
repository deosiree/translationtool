package com.shr.translationtoolservice.entity;

import java.util.HashMap;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

public class KeyValueArguments<K> {


    Map<K,Object> keyValueMap = new HashMap<>();
    
    public <T> void set(K key,T value){
        keyValueMap.put(key, value);
        return;
    }

    public <T> T get(K key,Class<T> clazz){
        TypeToken<T> typeToken = TypeToken.of(clazz);
        return this.get(key, typeToken);
    }

    public <T> T get(K key,TypeToken<T> typeToken){
        Object value = keyValueMap.get(key);
        if(value == null){
            return null;
        }
        return (T)value;
    }
    
}
