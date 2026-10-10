package com.shr.translationtoolservice.service.processor.groupby;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.shr.translationtoolservice.service.processor.groupby.GroupbyStrategy.ReplicatedVOType;

public final class GeneralReplicatedVOType extends ReplicatedVOType {

    public Map<String,Collection<Object>> targetAttributeMap = new HashMap<>(); // key为属性名, value为属性值

    public GeneralReplicatedVOType() {}

    public void addAttribute(String attributeName,Object targetValue){
        if(attributeName == null){
            return;
        }
        if(targetAttributeMap.containsKey(attributeName)){
            this.targetAttributeMap.get(attributeName).add(targetValue);
        }else{
            Collection<Object> attributes = new ArrayList<>();
            attributes.add(targetValue);
            this.targetAttributeMap.put(attributeName,attributes);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((targetAttributeMap == null) ? 0 : targetAttributeMap.hashCode());
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
        GeneralReplicatedVOType other = (GeneralReplicatedVOType) obj;
        if (targetAttributeMap == null) {
            if (other.targetAttributeMap != null)
                return false;
        } else if (!targetAttributeMap.equals(other.targetAttributeMap))
            return false;
        return true;
    }

}
