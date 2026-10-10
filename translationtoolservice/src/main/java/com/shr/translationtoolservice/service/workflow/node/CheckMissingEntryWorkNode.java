package com.shr.translationtoolservice.service.workflow.node;

import java.util.Collection;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.vo.ValueDifferenceVO;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue.IssueType;
import com.shr.translationtoolservice.entity.vo.exception.ExceptionVO;
import com.shr.translationtoolservice.service.processor.groupby.DefaultEntryGroupbyStrategy;
import com.shr.translationtoolservice.util.EntryUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckMissingEntryWorkNode extends CompareEntityWorkNode<EntryInfoEntity>{

    protected EntryUtils entryUtils;

    static{
        checkType = CheckType.CHECK_MISSING_ENTRY_ID;
    }

    public CheckMissingEntryWorkNode(EntryUtils entryUtils) {
        super();
        this.entryUtils = entryUtils;
        this.attributes.add("id");
    }

    /**
     * 比对送翻前的文件和送翻后的文件, 检查id列是否存在丢失
     * @param translatedFile
     * @param beforeTranslatedFile
     */
    @Override
    public void checkInternal() {
        // 以第一个词条集合为基准, 查找第二个词条集合中没有的词条
        ValueDifferenceVO<EntryInfoEntity> valueDifference = this.getDifference();
        Collection<EntryInfoEntity> missingEntryInfo = valueDifference.getValueOnlyInFirst();
        try {
            CheckResultCode resultCode = null;
            String message = "";
            if(missingEntryInfo.isEmpty()){
                resultCode = CheckResultCode.CHECK_PASSED;
                message = "校验通过, 没有丢失的词条";
            }else{
                resultCode = CheckResultCode.CHECK_UNPASSED;
                message = "校验未通过, 存在丢失的词条, 请查看详细信息";
            }
            this.setCheckResult(new CheckResult<>(resultCode, message, missingEntryInfo));      
            return;
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
    public IssueType getIssueType() {
        // TODO Auto-generated method stub
        return IssueType.ENTRY_MISSING;
    }

    @Override
    public Issue getIssue() {
        Issue issue = new Issue();
        issue.setLevel(Issue.Level.WARN);
        issue.setType(this.getIssueType());
        issue.setMessage(this.getMessage());
        return issue;
    }

    @Override
    protected ValueDifferenceVO<EntryInfoEntity> getDifference() {
        DefaultEntryGroupbyStrategy entryGroupbyStrategy = new DefaultEntryGroupbyStrategy();
        entryGroupbyStrategy.addTargetAttributes(this.attributes);
        ValueDifferenceVO<EntryInfoEntity> valueDifferenceVO = entryUtils.compareEntryInfos(entity1, entity2,entryGroupbyStrategy);  
        return valueDifferenceVO; 
    }

}
