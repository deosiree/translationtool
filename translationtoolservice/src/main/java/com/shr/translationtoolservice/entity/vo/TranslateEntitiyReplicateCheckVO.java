package com.shr.translationtoolservice.entity.vo;

import java.util.Date;


public class TranslateEntitiyReplicateCheckVO {
    
    public String translate;

    public String entry;

    public String translateType;

    public String visualRange;

    public Integer translateState;

    public Date latestUseTime;

    public String id;


    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((translate == null) ? 0 : translate.hashCode());
        result = prime * result + ((entry == null) ? 0 : entry.hashCode());
        result = prime * result + ((translateType == null) ? 0 : translateType.hashCode());
        result = prime * result + ((visualRange == null) ? 0 : visualRange.hashCode());
        result = prime * result + ((translateState == null) ? 0 : translateState.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TranslateEntitiyReplicateCheckVO other = (TranslateEntitiyReplicateCheckVO) obj;
        if (translate == null) {
            if (other.translate != null)
                return false;
        } else if (!translate.equals(other.translate))
            return false;
        if (entry == null) {
            if (other.entry != null)
                return false;
        } else if (!entry.equals(other.entry))
            return false;
        if (translateType == null) {
            if (other.translateType != null)
                return false;
        } else if (!translateType.equals(other.translateType))
            return false;
        if (visualRange == null) {
            if (other.visualRange != null)
                return false;
        } else if (!visualRange.equals(other.visualRange))
            return false;
        if (translateState == null) {
            if (other.translateState != null)
                return false;
        } else if (!translateState.equals(other.translateState))
            return false;
        return true;
    }

    public String getTranslate() {
        return translate;
    }

    public void setTranslate(String translate) {
        this.translate = translate;
    }

    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    public String getTranslateType() {
        return translateType;
    }

    public void setTranslateType(String translateType) {
        this.translateType = translateType;
    }

    public String getVisualRange() {
        return visualRange;
    }

    public void setVisualRange(String visualRange) {
        this.visualRange = visualRange;
    }

    public Integer getTranslateState() {
        return translateState;
    }

    public void setTranslateState(Integer translateState) {
        this.translateState = translateState;
    }

    public Date getLatestUseTime() {
        return latestUseTime;
    }

    public void setLatestUseTime(Date latestUseTime) {
        this.latestUseTime = latestUseTime;
    }

}
