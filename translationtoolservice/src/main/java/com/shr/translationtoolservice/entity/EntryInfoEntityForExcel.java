package com.shr.translationtoolservice.entity;

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
}
