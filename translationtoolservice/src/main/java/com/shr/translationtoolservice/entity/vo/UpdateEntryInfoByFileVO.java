package com.shr.translationtoolservice.entity.vo;

import java.util.ArrayList;
import java.util.Collection;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.vo.exception.ExceptionVO;

/**
 * 
 * importTransExcle接口返回的对象, 其中包含了多组异常信息, 以及失败的词条集合
 */
public class UpdateEntryInfoByFileVO{


    public String globalMessage;

    public Collection<EntryInfoEntity> failedEntryInfos = new ArrayList<>();    // 存在问题的词条, 用于前端导出一个文件, 这个文件内部是导入异常的词条信息

    public static UpdateEntryInfoByFileVO newInstance(String globalMessage,ExceptionVO... exceptionVOs){
        UpdateEntryInfoByFileVO updateTranslationByFileVO = new UpdateEntryInfoByFileVO();
        updateTranslationByFileVO.setGlobalMessage(globalMessage);
        for(ExceptionVO exceptionVO : exceptionVOs){
            updateTranslationByFileVO.addException(exceptionVO);
        }
        return updateTranslationByFileVO;
    }

    public static UpdateEntryInfoByFileVO newInstance(String globalMessage,Collection<ExceptionVO> exceptionVOs){
        UpdateEntryInfoByFileVO updateTranslationByFileVO = new UpdateEntryInfoByFileVO();
        updateTranslationByFileVO.setGlobalMessage(globalMessage);
        exceptionVOs.forEach((exceptionVO)->{updateTranslationByFileVO.addException(exceptionVO);});
        return updateTranslationByFileVO;
    }

    public static UpdateEntryInfoByFileVO newInstance(String globalMessage,Collection<ExceptionVO> exceptionVOs,Collection<EntryInfoEntity> failedEntryInfos){
        UpdateEntryInfoByFileVO updateTranslationByFileVO = new UpdateEntryInfoByFileVO();
        updateTranslationByFileVO.setGlobalMessage(globalMessage);
        exceptionVOs.forEach((exceptionVO)->{updateTranslationByFileVO.addException(exceptionVO);});
        updateTranslationByFileVO.addFailedEntryInfos(failedEntryInfos);
        return updateTranslationByFileVO;
    }

    public void addFailedEntryInfo(EntryInfoEntity entity){
        failedEntryInfos.add(entity);
    }
    
    public void addFailedEntryInfos(Collection<EntryInfoEntity> entities){
        failedEntryInfos.addAll(entities);
    }


    public void setGlobalMessage(String globalMessage) {
        this.globalMessage = globalMessage;
    }

    public Collection<ExceptionVO> exceptionVOs = new ArrayList<>();


    public void addException(ExceptionVO exceptionVO){
        exceptionVOs.add(exceptionVO);
    }

    public Integer exceptionNumber(){
        return exceptionVOs != null ? exceptionVOs.size() : 0;
    }

    public boolean hasError(){
        return exceptionVOs != null && !exceptionVOs.isEmpty();
    }

    // public 
    
}
