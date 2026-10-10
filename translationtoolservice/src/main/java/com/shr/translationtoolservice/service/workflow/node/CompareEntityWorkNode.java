package com.shr.translationtoolservice.service.workflow.node;

import java.util.Collection;
import java.util.HashSet;
import com.shr.translationtoolservice.entity.vo.ValueDifferenceVO;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class CompareEntityWorkNode<T> extends CheckWorkNode<Collection<T>> {



    protected Collection<T> entity1 = null;

    protected Collection<T> entity2 = null;

    protected Collection<String> attributes = new HashSet<>();




    public void addCompareData(Collection<T> entity1,Collection<T> entity2){
        this.entity1 = entity1;
        this.entity2 = entity2;
    }


    protected abstract ValueDifferenceVO<T> getDifference();

    
}
