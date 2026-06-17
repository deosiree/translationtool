package com.shr.translationtoolservice.entity.vo.check;

import java.util.List;

public class TaskRequest {

    public List<TaskRule> rules;

    public TaskOptions options;

    public List<TaskRule> getTaskRules() {
        return rules;
    }

    public TaskOptions getOptions() {
        return options;
    }
    
}
