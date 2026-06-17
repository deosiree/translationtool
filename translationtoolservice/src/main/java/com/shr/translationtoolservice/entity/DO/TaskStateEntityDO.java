package com.shr.translationtoolservice.entity.DO;

import java.util.Collection;

public class TaskStateEntityDO {

    public String taskID;

    public Collection<EntryStateDO> entryStateDOs;


    public String getTaskID() {
        return taskID;
    }

    public Collection<EntryStateDO> getEntryStateDOs() {
        return entryStateDOs;
    }


    public static class EntryStateDO {


        public String entryState;

        public String translateState;
        
        /**
         * 处于该词条审核状态的词条的个数
         */
        public Integer counts;


        public String getEntryState() {
            return entryState;
        }

        public Integer getCounts() {
            return counts;
        }
        
        public String getTranslateState() {
            return translateState;
        }
        
    }


}
