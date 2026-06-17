package com.shr.translationtoolservice.entity;

import java.util.List;

import com.shr.translationtoolservice.entity.vo.TaskInfoVo;

/**
 * 从lang文件夹导入文件的词条，并将对应的词条提交到指定的任务中
 */
public class TaskImportEntryEntity {

    TaskInfoEntity taskInfoEntity;  // 任务

    List<String> entrySources;  // 任务对应的词条来源

    String fileType;    // 这一批词条来源对应的文件类型

    List<String> targetLanguageTypes;   // ['英文','俄文']

    public TaskInfoEntity getTaskInfoEntity() {
        return taskInfoEntity;
    }

    public void setTaskInfoEntity(TaskInfoEntity taskInfoEntity) {
        this.taskInfoEntity = taskInfoEntity;
    }

    public List<String> getEntrySources() {
        return entrySources;
    }

    public void setEntrySources(List<String> entrySources) {
        this.entrySources = entrySources;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public List<String> getTargetLanguageTypes() {
        return targetLanguageTypes;
    }

    public void setTargetLanguageTypes(List<String> targetLanguageTypes) {
        this.targetLanguageTypes = targetLanguageTypes;
    }

    public static class TaskImportEntryVO{

        TaskInfoVo taskInfoVO;  // 任务

        List<String> entrySources;  // 任务对应的词条来源

        String fileType;    // 这一批词条来源对应的文件类型

        List<String> targetLanguageTypes;   // ['英文','俄文']

        public TaskInfoVo getTaskInfoVO() {
            return taskInfoVO;
        }

        public void setTaskInfoVO(TaskInfoVo taskInfoVO) {
            this.taskInfoVO = taskInfoVO;
        }

        public List<String> getEntrySources() {
            return entrySources;
        }

        public void setEntrySources(List<String> entrySources) {
            this.entrySources = entrySources;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public List<String> getTargetLanguageTypes() {
            return targetLanguageTypes;
        }

        public void setTargetLanguageTypes(List<String> targetLanguageTypes) {
            this.targetLanguageTypes = targetLanguageTypes;
        }

    }


    public static TaskImportEntryEntity convertFromTaskImportEntryVO(TaskImportEntryVO taskImportEntryVO){
        TaskImportEntryEntity taskImportEntryEntity = new TaskImportEntryEntity();
        taskImportEntryEntity.setEntrySources(taskImportEntryVO.getEntrySources());
        taskImportEntryEntity.setFileType(taskImportEntryVO.getFileType());
        taskImportEntryEntity.setTargetLanguageTypes(taskImportEntryVO.getTargetLanguageTypes());
        return taskImportEntryEntity;
    }

}
