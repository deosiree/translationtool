package com.shr.translationtoolservice.service.workflow.node;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue;
import com.shr.translationtoolservice.entity.vo.exception.ExceptionVO;

public abstract class CheckWorkNode<T> extends WorkNode<T>{

    protected static CheckType checkType;

    protected boolean isFinished = false;

    protected CheckResult<T> checkResult;

    public abstract void checkInternal();

    public void check(){
        try {
            checkInternal();    
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally{
            this.isFinished = true;
        }
    }

    protected void setCheckResult(CheckResult<T> checkResult) {
        this.checkResult = checkResult;
    }

    public boolean isFinished(){
        return isFinished;
    }

    public boolean isPassed(){
        return this.checkResult.getCheckResultCode().equals(CheckResultCode.CHECK_PASSED);
    }

    public String getMessage(){
        return this.checkResult.getMessage();
    }
    
    @Override
    public String convertResultToJSONString() {
        // TODO Auto-generated method stub
        Gson gson = new GsonBuilder()
            .setPrettyPrinting() // 开启格式化（换行+缩进）
            .disableHtmlEscaping() // 禁用HTML转义（避免中文/特殊字符被转义）
            .create();
        return gson.toJson(this.checkResult);
    }

    public abstract Issue getIssue();

    public static enum CheckResultCode{

        UNFINISHED,
        CHECK_FAILED,
        CHECK_PASSED,
        CHECK_UNPASSED,
        CHECK_PARTILLY_PASSED

    }

    public static enum CheckType{
        CHECK_MISSING_ENTRY_ID,
        CHECK_MISSING_PARENT_ID
    }

    public static class CheckResult<T>{

        public CheckResultCode checkResultCode;

        public String message;

        public T data;

        public ExceptionVO exceptionVO;

        public CheckResult(CheckResultCode checkResultCode, String message, T data) {
            this.checkResultCode = checkResultCode;
            this.message = message;
            this.data = data;
        }

        public CheckResultCode getCheckResultCode() {
            return checkResultCode;
        }

        public ExceptionVO getExceptionVO() {
            return exceptionVO;
        }

        public void setExceptionVO(ExceptionVO exceptionVO) {
            this.exceptionVO = exceptionVO;
        }
        
        public String getMessage() {
            return message;
        }

    }

}
