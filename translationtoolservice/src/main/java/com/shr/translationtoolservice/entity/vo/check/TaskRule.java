package com.shr.translationtoolservice.entity.vo.check;

import java.util.Map;

public class TaskRule {

    public String taskType;

    public Map<String, Object> params;

    public String getTaskType() {
        return taskType;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((taskType == null) ? 0 : taskType.hashCode());
        result = prime * result + ((params == null) ? 0 : params.hashCode());
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
        TaskRule other = (TaskRule) obj;
        if (taskType == null) {
            if (other.taskType != null)
                return false;
        } else if (!taskType.equals(other.taskType))
            return false;
        if (params == null) {
            if (other.params != null)
                return false;
        } else if (!params.equals(other.params))
            return false;
        return true;
    }

}
