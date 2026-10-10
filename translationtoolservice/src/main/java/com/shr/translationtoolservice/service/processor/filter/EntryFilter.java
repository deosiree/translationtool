package com.shr.translationtoolservice.service.processor.filter;

import java.util.function.Predicate;

import com.shr.translationtoolservice.entity.EntryInfoEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EntryFilter implements Predicate<EntryInfoEntity>{

    @Override
    public boolean test(EntryInfoEntity t) {
        // TODO Auto-generated method stub
        if(t.getEntry().length() > 512){
            log.info(String.format("词条长度超过限制, 词条为: %s", t.getEntry()));
            return false;
        }else{
            return true;
        }
    }
    
}
