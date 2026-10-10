package com.shr.translationtoolservice.service.workflow.node;

import java.util.Collection;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue.IssueType;
import com.shr.translationtoolservice.entity.vo.exception.ExceptionVO;
import com.shr.translationtoolservice.service.analyze.BatchMaxLengthTranslateAnalyzer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckTranslationMaxLengthWorkNode extends CheckWorkNode<Collection<EntryInfoEntity>>{


    protected BatchMaxLengthTranslateAnalyzer batchMaxLengthTranslateAnalyzer;

    protected Collection<EntryInfoEntity> entryInfoEntities;

    public CheckTranslationMaxLengthWorkNode(BatchMaxLengthTranslateAnalyzer batchMaxLengthTranslateAnalyzer) {
        this.batchMaxLengthTranslateAnalyzer = batchMaxLengthTranslateAnalyzer;
    }

    public void setEntryInfoEntities(Collection<EntryInfoEntity> entryInfoEntities) {
        this.entryInfoEntities = entryInfoEntities;
    }

    public void addTranslateType(Collection<String> translateTypes){
        translateTypes.stream().forEach((type)->{batchMaxLengthTranslateAnalyzer.addLanguageType(type);});
        return;
    }

    @Override
    public void checkInternal() {
        try {
            boolean hasProblem = batchMaxLengthTranslateAnalyzer.analyze(entryInfoEntities);
            CheckResultCode resultCode = null;
            String message = "";
            if(!hasProblem){
                resultCode = CheckResultCode.CHECK_PASSED;
                message = "校验通过";
                this.setCheckResult(new CheckResult<>(resultCode, message, null));
            }else{
                resultCode = CheckResultCode.CHECK_UNPASSED;
                message = "翻译长度不符合要求";
                this.setCheckResult(new CheckResult<Collection<EntryInfoEntity>>(resultCode, message, batchMaxLengthTranslateAnalyzer.getProblematicEntryInfoEntities()));
            }
        } catch (Exception e) {
            String errorMessage = String.format("异常信息: %s", e.getMessage());
            log.error(errorMessage,e);
            ExceptionVO exceptionVO = new ExceptionVO(errorMessage);
            CheckResult<Collection<EntryInfoEntity>> checkResult = new CheckResult<>(CheckResultCode.CHECK_FAILED, errorMessage, null);
            checkResult.setExceptionVO(exceptionVO);
            this.setCheckResult(checkResult);
            return;
        }
    }

    @Override
    public Issue getIssue() {
        // TODO Auto-generated method stub
        Issue issue = new Issue();
        issue.setLevel(Issue.Level.WARN);
        issue.setType(this.getIssueType());
        issue.setMessage(this.getMessage());
        return issue;
    }

    @Override
    public IssueType getIssueType() {
        // TODO Auto-generated method stub
        return IssueType.TRANSLATE_LENGTH;
    }
    
}
