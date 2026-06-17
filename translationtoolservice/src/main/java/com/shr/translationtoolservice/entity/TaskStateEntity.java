package com.shr.translationtoolservice.entity;

import java.util.Collection;

import com.shr.translationtoolservice.entity.DO.TaskStateEntityDO;
import com.shr.translationtoolservice.entity.DO.TaskStateEntityDO.EntryStateDO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskStateEntity {

    String taskID;

    int totalNum;

    int importNum;

    int examineNum;

    int translateNum;

    int examineTranslateNum;

    private TaskStateEntity(){}


    public String getTaskID() {
        return taskID;
    }

    public void setTaskID(String taskID) {
        this.taskID = taskID;
    }

    public int getTotalCounts() {
        return totalNum;
    }

    public void setTotalCounts(int totalNum) {
        this.totalNum = totalNum;
    }

    public int getimportNum() {
        return importNum;
    }

    public void setimportNum(int importNum) {
        this.importNum = importNum;
    }

    public int getEntryExamineCounts() {
        return examineNum;
    }

    public void setEntryExamineCounts(int examineNum) {
        this.examineNum = examineNum;
    }

    public int getTranslateCounts() {
        return translateNum;
    }

    public void setTranslateCounts(int translateNum) {
        this.translateNum = translateNum;
    }

    public int getTranslateExamineCounts() {
        return examineTranslateNum;
    }

    public void setTranslateExamineCounts(int examineTranslateNum) {
        this.examineTranslateNum = examineTranslateNum;
    }

    public static TaskStateEntity convertFrom(TaskStateEntityDO taskStateEntityDO){
        TaskStateEntity taskStateEntity = new TaskStateEntity();
        taskStateEntity.setTaskID(taskStateEntityDO.getTaskID());
        Collection<EntryStateDO> entryStateDOs = taskStateEntityDO.getEntryStateDOs();
        int importNum = 0;
        int examineNum = 0;
        int translateNum = 0;
        int examineTranslateNum = 0;
        for(EntryStateDO entryStateDO : entryStateDOs){
            String entryState = entryStateDO.getEntryState();
            String translateState = entryStateDO.getTranslateState();
            int counts = entryStateDO.getCounts();
            if(entryState == null || translateState == null){
                log.warn("警告, 获取到的词条和翻译状态存在null");
                continue;
            }
            if(entryState.equals("0")){
                throw new RuntimeException("警告,获取到了任务中的词条状态为新建的词条");    // 新建的都是没有提交任务的(目前是这样的)
            }else if(entryState.equals("1")){
                /* 词条待审核, 词条审核阶段 */
                examineNum +=counts ;
            }else if(entryState.equals("2")){
                /* 词条审核不通过, 导入阶段 */
                importNum += counts ;
            }else if(entryState.equals("3")){
                /* 词条审核通过, 翻译或翻译审核阶段 */
                if(translateState.equals("0")){
                    translateNum += counts ;        // 没有翻译
                }else if(translateState.equals("1")){
                    examineTranslateNum += counts ; // 翻译未审核
                }else if(translateState.equals("2")){
                    translateNum += counts ;        // 翻译不通过重新翻译的
                }else{

                }
            }else if(entryState.equals("4")){
                /* 词条审核通过, 归档阶段 */
            }else if(entryState.equals("-1")){
                /* 禁用的，跳过 */
            }else{
                throw new RuntimeException(String.format("警告: 获取到的任务的词条状态无法被处理: %s", entryState));
            }
    
        }
        int totalNum = importNum + examineNum + translateNum + examineTranslateNum;
        taskStateEntity.setTotalCounts(totalNum);
        taskStateEntity.setimportNum(importNum);
        taskStateEntity.setEntryExamineCounts(examineNum);
        taskStateEntity.setTranslateCounts(translateNum);
        taskStateEntity.setTranslateExamineCounts(examineTranslateNum);

        return taskStateEntity;
    }
}
