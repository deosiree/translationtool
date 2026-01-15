package com.shr.translationtoolservice.service.processor.groupby;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * 
 */
public abstract class GeneralGroupbyStrategy<T> implements GroupbyStrategy<T,GeneralReplicatedVOType> {

    /**
     * 里面每一个代表需要用哪些key的值进行去重操作
     */
    public Collection<Integer> targetColumnNumbers = new ArrayList<>();

    public Function<List<Object>,GeneralReplicatedVOType> replicatedVOTypeBuilder;

    public GeneralGroupbyStrategy() {
        this.replicatedVOTypeBuilder = createReplicatedVOTypeBuilder();
    }

    public GeneralGroupbyStrategy(Function<List<Object>, GeneralReplicatedVOType> replicatedVOTypeBuilder) {
        if(Objects.isNull(replicatedVOTypeBuilder)){
            this.replicatedVOTypeBuilder = this.createReplicatedVOTypeBuilder();
        }else{
            this.replicatedVOTypeBuilder = replicatedVOTypeBuilder;
        }
    }

    protected Function<List<Object>,GeneralReplicatedVOType> createReplicatedVOTypeBuilder(){
        return new Function<List<Object>,GeneralReplicatedVOType>() {

            @Override
            public GeneralReplicatedVOType apply(List<Object> fieldValueList) {
                // TODO Auto-generated method stub
                GeneralReplicatedVOType generalReplicatedVOType = new GeneralReplicatedVOType();
                if(targetColumnNumbers == null || targetColumnNumbers.isEmpty()){
                    /* 没有提供用于分组区分的条件, 则不进行分组, 返回随机不重复的数字 */
                    generalReplicatedVOType.addAttribute(String.valueOf(0),UUID.randomUUID().toString());
                }
                for(Integer targetColumnNumber : targetColumnNumbers){
                    if(targetColumnNumber >= fieldValueList.size()){
                        throw new IndexOutOfBoundsException(String.format("该对象属性共%s个, 但需要取第%s列的属性值", fieldValueList.size(),targetColumnNumber));
                    }
                    generalReplicatedVOType.addAttribute(String.valueOf(targetColumnNumber),fieldValueList.get(targetColumnNumber));
                }
                return generalReplicatedVOType;
            }
        };
    }

    protected void addTargetColumnNumber(Integer columnNumber){
        this.targetColumnNumbers.add(columnNumber);
    }
    
    protected abstract List<Object> asList(T targetObject);

    @Override
    public Function<T, GeneralReplicatedVOType> newInstance() {
        // TODO Auto-generated method stub
        return new Function<T,GeneralReplicatedVOType>() {

            @Override
            public GeneralReplicatedVOType apply(T t) {
                // TODO Auto-generated method stub
                return replicatedVOTypeBuilder.apply(asList(t));
            }
            
        };
    }
 
}
