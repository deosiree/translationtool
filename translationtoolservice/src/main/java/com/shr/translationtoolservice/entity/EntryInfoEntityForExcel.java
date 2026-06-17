package com.shr.translationtoolservice.entity;

import org.springframework.beans.BeanUtils;

import lombok.Data;

@Data
public class EntryInfoEntityForExcel extends EntryInfoEntity {

    public String maxChineseLength;

    public String foreignMaxLength;

    
    @Override
    public boolean equals(Object o) {
        // TODO Auto-generated method stub
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }

    public static EntryInfoEntityForExcel convertFromEntryInfoEntity(EntryInfoEntity entity){
        EntryInfoEntityForExcel convert = new EntryInfoEntityForExcel();
        BeanUtils.copyProperties(entity, convert);
        return convert;
    }
}
