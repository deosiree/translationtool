package com.shr.translationtoolservice.entity.vo;

import java.util.List;

import com.shr.translationtoolservice.entity.EntryInfoEntity;

public class EntryInfoVO {
    

    List<EntryInfoEntity> entryInfoEntities;

    int totalSize;

    public EntryInfoVO() {
    }

    public EntryInfoVO(List<EntryInfoEntity> entryInfoEntities) {
        this.entryInfoEntities = entryInfoEntities;
        this.totalSize = entryInfoEntities.size();
    }

    public List<EntryInfoEntity> getEntryInfoEntities() {
        return entryInfoEntities;
    }

    public void setEntryInfoEntities(List<EntryInfoEntity> entryInfoEntities) {
        this.entryInfoEntities = entryInfoEntities;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }
}
