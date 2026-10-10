package com.shr.translationtoolservice.service.workflow.node;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue.IssueType;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue.Level;
import com.shr.translationtoolservice.entity.vo.exception.ExceptionVO;
import lombok.extern.slf4j.Slf4j;
/**
 * 校验是否有指定的列名
 */
@Slf4j
public class CheckColumnExistWorkNode extends CheckWorkNode<Collection<String>>{


    public Collection<String> fileColumnNames;

    public Map<String,String> attributeColumnMap;

    public void setAttributeColumnMap(Map<String, String> attributeColumnMap) {
        this.attributeColumnMap = attributeColumnMap;
    }

    public Collection<String> targetAttributes;

    public CheckColumnExistWorkNode() {
    }

    public void setFileColumnNames(Collection<String> fileColumnNames) {
        this.fileColumnNames = fileColumnNames;
    }

    public void setTargetAttributes(Collection<String> targetAttributes) {
        this.targetAttributes = targetAttributes;
    }

    @Override
    public void checkInternal() {
        try {
            CheckResultCode resultCode = null;
            Collection<String> missingColumnNames=  new HashSet<>();
            if(targetAttributes != null && !targetAttributes.isEmpty() && fileColumnNames != null && !fileColumnNames.isEmpty()){
                for(String targetAttribute : targetAttributes){
                    String columnName = attributeColumnMap.get(targetAttribute);
                    if(fileColumnNames.contains(columnName)){
                        continue;
                    }
                    missingColumnNames.add(columnName);
                }           
            }
            if(missingColumnNames.isEmpty()){
                resultCode = CheckResultCode.CHECK_PASSED;
            }else{
                resultCode= CheckResultCode.CHECK_UNPASSED;
            }
            CheckResult<Collection<String>> checkResult = new CheckResult<>(resultCode,resultCode == CheckResultCode.CHECK_PASSED ? "校验通过" : String.format("校验不通过, 以下列名不存在: %s", missingColumnNames), missingColumnNames);
            this.setCheckResult(checkResult);
            return;
        } catch (Exception e) {
            String errorMessage = String.format("异常信息: %s", e.getMessage());
            log.error(errorMessage,e);
            ExceptionVO exceptionVO = new ExceptionVO(errorMessage);
            CheckResult<Collection<String>> checkResult = new CheckResult<>(CheckResultCode.CHECK_FAILED, errorMessage, null);
            checkResult.setExceptionVO(exceptionVO);
            this.setCheckResult(checkResult);
            return;
        }

        
    }

    @Override
    public Issue getIssue() {
        Issue issue = new Issue();
        issue.setLevel(Level.WARN);
        issue.setMessage(this.checkResult.getMessage());
        issue.setType(this.getIssueType());
        return issue;
        
    }

    @Override
    public IssueType getIssueType() {
        // TODO Auto-generated method stub
        return IssueType.COLUMN_NOT_FOUND;
    }

    
}
