package com.shr.translationtoolservice.service.workflow.node;

import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue.IssueType;

public abstract class WorkNode<T> {
    
    public abstract String convertResultToJSONString();

    public abstract IssueType getIssueType();
    
}
