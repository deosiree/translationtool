package com.shr.translationtoolservice.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.shr.translationtoolservice.entity.TranslateEntity;

import java.util.List;
import java.util.Map;

@Component
public class AsyncUtils {

    @Resource(name = "TranslateMapperExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public static class AsyncEntity<T>{

        List<T> resultList;

        List<Exception> exceptions;

        public AsyncEntity(List<T> resultList, List<Exception> exceptions) {
            this.resultList = resultList;
            this.exceptions = exceptions;
        }

        public List<T> getResultList() {
            return resultList;
        }

        public List<Exception> getExceptions() {
            return exceptions;
        }
    } 

    public <T> Future<T> asyncCompute(Object obj,String methodName,Class<?> parameterTypes,Object... args){

         Callable<T> callable = new Callable<T>() {
                    
            @SuppressWarnings("unchecked")
            @Override
            public T call() throws Exception {
                // int updateCount = translateMapper.updateTranslation(subList);
                // return updateCount;
                Object results = obj.getClass().getMethod(methodName, parameterTypes).invoke(obj, args);
                return (T) results;
            }
        };
        return threadPoolTaskExecutor.submit(callable);

    }

    public <T> List<Future<T>> waitForCompleted(List<Future<T>> futureList){
        return waitForCompleted(futureList, null);
    }
    

    
    public <T> List<Future<T>> waitForCompleted(List<Future<T>> futureList,Map<Future<T>,Exception> exceptions){

        List<Future<T>> resultList = new LinkedList<>();
        do {
            Iterator<Future<T>> iterator = futureList.iterator();
            while (iterator.hasNext()) {
                Future<T> nextFuture = iterator.next();
                if(nextFuture.isDone()){
                    try {
                        nextFuture.get();   // 测试是否存在异常 
                        resultList.add(nextFuture);
                    } catch (Exception e) {
                        if(exceptions != null){
                            exceptions.put(nextFuture, e);
                        }else{
                            e.printStackTrace();
                        }
                            
                    }finally{
                        iterator.remove();
                    }
                }
            }    
                        
        } while (!futureList.isEmpty());
        return resultList;
    }

}
