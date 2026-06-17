package com.shr.translationtoolservice.entity.vo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.shr.translationtoolservice.entity.EntryInfoEntity;

/**
 * 词条分组信息, 用于对导出的文件进行分组去重功能使用
 */
public class EntryInfoGroupVO {
    /**
     * 去重后的词条信息, 与其重复的词条在EntryInfoEntity的children列表中
     */
    Collection<EntryInfoEntity> notReplicatedEntryInfos = null; 

    /**
     * 去重后的词条与其他重复的词条的ID的关联关系
     */
    Map<String, List<String>> idRelationMap = null;

    public EntryInfoGroupVO(Collection<EntryInfoEntity> notReplicatedEntryInfos,
            Map<String, List<String>> idRelationMap) {
        this.notReplicatedEntryInfos = notReplicatedEntryInfos;
        this.idRelationMap = idRelationMap;
    }

    public Collection<EntryInfoEntity> getNotReplicatedEntryInfos() {
        return notReplicatedEntryInfos;
    }

    public void setNotReplicatedEntryInfos(Collection<EntryInfoEntity> notReplicatedEntryInfos) {
        this.notReplicatedEntryInfos = notReplicatedEntryInfos;
    }

    public Map<String, List<String>> getIdRelationMap() {
        return idRelationMap;
    }

    public void setIdRelationMap(Map<String, List<String>> idRelationMap) {
        this.idRelationMap = idRelationMap;
    }

    
}
