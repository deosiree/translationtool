package com.shr.translationtoolservice.service.workflow.node;
import java.util.ArrayList;
import java.util.Collection;

import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue.IssueType;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue.Level;
import com.shr.translationtoolservice.entity.vo.exception.ExceptionVO;
import com.shr.translationtoolservice.service.analyze.AnalyzeSample;
import com.shr.translationtoolservice.service.analyze.DefaultTranslateAnalyzer;
import com.shr.translationtoolservice.service.analyze.DefaultTranslateAnalyzer.DefaultTranslateAnalyzeSample;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils.MethodEntity;
import lombok.extern.slf4j.Slf4j;
/**
 * 校验指定字段的值是否存在不允许出现的特殊字符
 */
@Slf4j
public class CheckSpecialCharacterWorkNode<T> extends CheckWorkNode<Collection<T>>{

    public Collection<T> entities;

    public DefaultTranslateAnalyzer defaultTranslateAnalyzer;

    public MethodEntity getEntryMethodEntity;

    public Collection<MethodEntity> getTranslationMethods;


    public CheckSpecialCharacterWorkNode(DefaultTranslateAnalyzer defaultTranslateAnalyzer) {
        this.defaultTranslateAnalyzer = defaultTranslateAnalyzer;
    }

    public void setEntryInfoEntities(Collection<T> entities) {
        this.entities = entities;
    }

    public void setGetEntryMethodEntity(MethodEntity getEntryMethodEntity) {
        this.getEntryMethodEntity = getEntryMethodEntity;
    }

    public void setGetTranslationMethods(Collection<MethodEntity> getTranslationMethods) {
        this.getTranslationMethods = getTranslationMethods;
    }

    @Override
    public void checkInternal() {
        try {
            CheckResultCode resultCode = CheckResultCode.UNFINISHED;
            Collection<T> notCorrectEntryInfos = new ArrayList<>();
            if(getEntryMethodEntity != null && getTranslationMethods != null && !getTranslationMethods.isEmpty() && entities != null && !entities.isEmpty()){
                for(T entity : entities){
                    String entry = getEntryMethodEntity.getMethod().invoke(entity, getEntryMethodEntity.getParams()) != null ? 
                        String.valueOf(getEntryMethodEntity.getMethod().invoke(entity, getEntryMethodEntity.getParams())) : null;
                    for(MethodEntity getMethodEntity : getTranslationMethods){
                        try {
                            String translate = getMethodEntity.getMethod().invoke(entity) != null ? String.valueOf(getMethodEntity.getMethod().invoke(entity)) : null;
                            DefaultTranslateAnalyzeSample defaultTranslateAnalyzeSample = defaultTranslateAnalyzer.prepare(entry, translate);
                            AnalyzeSample analyze = defaultTranslateAnalyzer.analyze(defaultTranslateAnalyzeSample);
                            if(analyze.isBad()){
                                notCorrectEntryInfos.add(entity);
                            }
                        } catch(Exception e){
                            throw new RuntimeException(String.format("检查词条的翻译的占位符或特殊字符等是否格式一致时出现异常"),e);
                        }
                    }
                }
            }
            resultCode = notCorrectEntryInfos.isEmpty() ? CheckResultCode.CHECK_PASSED : CheckResultCode.CHECK_UNPASSED;
            String message = resultCode == CheckResultCode.CHECK_UNPASSED ?  "校验不通过, 请检查对应ID的词条与其翻译对应的占位符的个数和顺序是否匹配" : "校验通过";
            CheckResult<Collection<T>> notMatchCheckResult = new CheckResult<>(resultCode,message,notCorrectEntryInfos);
            this.setCheckResult(notMatchCheckResult);
            return;  
        } catch (Exception e) {
            String errorMessage = String.format("异常信息: %s", e.getMessage());
            log.error(errorMessage,e);
            ExceptionVO exceptionVO = new ExceptionVO(errorMessage);
            CheckResult<Collection<T>> checkResult = new CheckResult<>(CheckResultCode.CHECK_FAILED, errorMessage, null);
            checkResult.setExceptionVO(exceptionVO);
            this.setCheckResult(checkResult);
            return;
        }
    }

    @Override
    public Issue getIssue() {
        // TODO Auto-generated method stub
        Issue issue = new Issue();
        issue.setLevel(Level.WARN);
        issue.setMessage(this.checkResult.getMessage());
        issue.setType(this.getIssueType());
        return issue;
    }

    @Override
    public IssueType getIssueType() {
        // TODO Auto-generated method stub
        return IssueType.TRANSLATE_NOT_CORRECT;
    }
    
}
