package com.shr.translationtoolservice.service.workflow.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue.IssueType;
import com.shr.translationtoolservice.entity.vo.exception.ExceptionVO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckMissingEntryRelationWorkNode extends CheckWorkNode<Collection<ExceptionVO>>{

    public Collection<String> entryIDs = null;

    public Map<String, Set<String>> idRelationMap = null;

    public CheckMissingEntryRelationWorkNode() {
    }

    static{
        checkType = CheckType.CHECK_MISSING_PARENT_ID;
    }


    public void addCheckData(Collection<String> entryIDs,Map<String, Set<String>> idRelationMap){
        this.entryIDs = entryIDs;
        this.idRelationMap = idRelationMap;
    }

    /**
     * 检查id关联表中父节点是否在翻译的文件上, 如果不在检查是否有子节点在翻译文件上
     * @param translatedFile
     * @param idRelationMap
     * @return
     */
    protected Collection<ExceptionVO> compareTranslatedFileWithIDRelation(Collection<String> entryIDs,Map<String, Set<String>> idRelationMap) {
        Collection<ExceptionVO> exceptionVOs = new ArrayList<>();
        if(idRelationMap == null || idRelationMap.isEmpty()){
            ExceptionVO exceptionVO = new ExceptionVO("没有解析获取到id关联表的信息, 请检查提供的json文件是否正确");
            exceptionVOs.add(exceptionVO);
            return exceptionVOs;
        }
        /* 检查如果父节点不存在, 是否有一个或多个子节点, 如果没有子节点, 则为异常信息(没法更新翻译(回填)) */
        for(Map.Entry<String,Set<String>> idRelation : idRelationMap.entrySet()){
            String parentID = idRelation.getKey();
            Set<String> childIDs = idRelation.getValue();
            if(parentID == null){
                throw new RuntimeException("id映射表存在异常, 父节点的ID为null");
            }
            if(entryIDs.contains(parentID)){
                /* 父节点存在 */
                continue;
            }else{
                int childIDExistInEntryIDs = 0; // entryIDs中子节点的个数
                for(String childID : childIDs){
                    if(!entryIDs.contains(childID)){
                        continue;   // entryIDs中不存在这个子节点ID
                    }
                    childIDExistInEntryIDs ++ ;
                }
                if(childIDExistInEntryIDs == 0){
                    /* 父节点没有, 子节点一个也没有, 这种情况是丢了 */
                    exceptionVOs.add(new ExceptionVO(
                        String.format("父节点id: '%s'的词条送翻记录丢失, 并且也没有子节点的送翻信息, 与之关联的子节点的id信息为: %s", parentID,childIDs.toString()),
                        "检查该id的词条是否送翻, 检查送翻的文件是否正确, 是否送翻文件内容被修改过, 检查id关联文件是否对应, 检查该文件是否被篡改"
                    )); // id关联表存在的id信息, 但是翻译文件里面没有
                    continue;
                }else{
                    /* 父节点没有, 但有一个或多个子节点,(父节点的也要更新),满足要求  */
                    continue;
                }
            }
        }

        return exceptionVOs;
    }

    @Override
    public void checkInternal() {
        // TODO Auto-generated method stub
        try {
            Collection<ExceptionVO> exceptionVos = this.compareTranslatedFileWithIDRelation(this.entryIDs,this.idRelationMap);
            CheckResultCode resultCode = null;
            String message = "";
            if(exceptionVos.isEmpty()){
                resultCode = CheckResultCode.CHECK_PASSED;
                message = "校验通过";
                this.setCheckResult(new CheckResult<>(resultCode, message, null));
            }else{
                resultCode = CheckResultCode.CHECK_UNPASSED;
                message = "存在丢失的词条, 无法更新翻译, 请查看详细信息, 其中记录了丢失的词条的ID";
                this.setCheckResult(new CheckResult<>(resultCode, message, exceptionVos));
            }
            return;

        } catch (Exception e) {
            String errorMessage = String.format("异常信息: %s", e.getMessage());
            log.error(errorMessage,e);
            ExceptionVO exceptionVO = new ExceptionVO(errorMessage);
            CheckResult<Collection<ExceptionVO>> checkResult = new CheckResult<>(CheckResultCode.CHECK_FAILED, errorMessage, null);
            checkResult.setExceptionVO(exceptionVO);
            this.setCheckResult(checkResult);
            return;
        }

    }
    

    @Override
    public IssueType getIssueType() {
        // TODO Auto-generated method stub
        return IssueType.PARENT_ID_NOT_FOUND;
    }

    @Override
    public Issue getIssue() {
        Issue issue = new Issue();
        issue.setLevel(Issue.Level.FATAL);
        issue.setType(this.getIssueType());
        issue.setMessage(this.getMessage());
        return issue;
    }

    
    
}
