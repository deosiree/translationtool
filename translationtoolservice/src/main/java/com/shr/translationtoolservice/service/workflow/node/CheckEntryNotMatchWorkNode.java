package com.shr.translationtoolservice.service.workflow.node;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue.IssueType;
import com.shr.translationtoolservice.entity.vo.exception.ExceptionVO;
import com.shr.translationtoolservice.util.CommonUtils.EntityMatchAnalyzer;
import com.shr.translationtoolservice.util.CommonUtils.EntityMatchAnalyzer.EntityPairForComparsion;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils.MethodEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link #entitiyMatchAnalyzer}的封装类, 用于适配执行{@link #CheckWorkNode}的{@link #CheckWorkNode.check()}方法, 具体该WorkNode的功能需要查看使用的{@link #entitiyMatchAnalyzer}对象的具体功能
 */
@Slf4j
public class CheckEntryNotMatchWorkNode<T> extends CheckWorkNode<Collection<EntityPairForComparsion<T>>>{

    public List<T> entities;

    public Collection<MethodEntity> methods;

    protected EntityMatchAnalyzer<T> entitiyMatchAnalyzer;


    public CheckEntryNotMatchWorkNode(EntityMatchAnalyzer<T> entitiyMatchAnalyzer) {
        this.entitiyMatchAnalyzer = entitiyMatchAnalyzer;
    }

    public void setFileEntryInfoEntities(List<T> entities){
        this.entities = entities;
    }

    public void setMethods(Collection<MethodEntity> methods) {
        this.methods = methods;
    }


    @Override
    public void checkInternal() {
        // TODO Auto-generated method stub
        try {
            CheckResultCode resultCode = CheckResultCode.UNFINISHED;
            if(methods != null && !methods.isEmpty()){
                List<EntityPairForComparsion<T>> notMatchEntryInfos = entitiyMatchAnalyzer.getNotMatchEntity(entities, methods);
                resultCode = notMatchEntryInfos.isEmpty() ? CheckResultCode.CHECK_PASSED : CheckResultCode.CHECK_UNPASSED;
                String message = resultCode == CheckResultCode.CHECK_UNPASSED ?  
                    String.format("校验未通过, 词条ID的相关信息与库中对应信息不匹配, %s", methods.stream().map((methodEntity)->{return methodEntity.getMethod().getName();}).collect(Collectors.toList())) : "校验通过";
                CheckResult<Collection<EntityPairForComparsion<T>>> notMatchCheckResult = new CheckResult<>(resultCode,message,notMatchEntryInfos);
                this.setCheckResult(notMatchCheckResult);
            }else{
                this.setCheckResult(new CheckResult<Collection<EntityPairForComparsion<T>>>(CheckResultCode.CHECK_PASSED, "未选择要校验的字段, 校验通过", null));
            }

            return;  
        } catch (Exception e) {
            String errorMessage = String.format("异常信息: %s", e.getMessage());
            log.error(errorMessage,e);
            ExceptionVO exceptionVO = new ExceptionVO(errorMessage);
            CheckResult<Collection<EntityPairForComparsion<T>>> checkResult = new CheckResult<>(CheckResultCode.CHECK_FAILED, errorMessage, null);
            checkResult.setExceptionVO(exceptionVO);
            this.setCheckResult(checkResult);
            return;
        }


    }

    @Override
    public Issue getIssue() {
        Issue issue = new Issue();
        issue.setLevel(Issue.Level.FATAL);
        issue.setType(this.getIssueType());
        issue.setMessage(this.getMessage());
        return issue;
    }

    @Override
    public IssueType getIssueType() {
        // TODO Auto-generated method stub
        return IssueType.INFO_NOT_MATCH_WITH_DB;

    }


    
    
}
