package com.shr.translationtoolservice.controller;

/**
 * @author ：210093
 * @date ：Created in 2023/6/19 15:22
 * @description：BaseController
 */


import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.util.task.BackendTaskInfoHandler;
import com.shr.translationtoolservice.util.task.BackendTaskInfoHandler.TASK_STATE;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;


@Slf4j
@RestController
public class BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public <T> HttpResponse<T> ok(T entity)
    {
        HttpResponse<T> httpResponse = new HttpResponse<>();
        httpResponse.setCode(HttpResponse.Type.OK.getVal());
        httpResponse.setType(HttpResponse.Type.OK);
        httpResponse.setData(entity);
        return httpResponse;
    }

    public <T> HttpResponse<T> ok(T entity, String operationObject)
    {
        HttpResponse<T> httpResponse = new HttpResponse<>();
        httpResponse.setCode(HttpResponse.Type.OK.getVal());
        httpResponse.setType(HttpResponse.Type.OK);
        httpResponse.setData(entity);
        httpResponse.setOperationObject(operationObject);
        return httpResponse;
    }

    public <T> HttpResponse<T> error(T entity, String msg)
    {
        HttpResponse<T> httpResponse = new HttpResponse<>();
        httpResponse.setCode(HttpResponse.Type.ERROR.getVal());
        httpResponse.setType(HttpResponse.Type.ERROR);
        httpResponse.setData(entity);
        httpResponse.setMessage(msg);
        return httpResponse;
    }

    public <T> HttpResponse<T> error(T entity, String msg, String operationObject)
    {
        HttpResponse<T> httpResponse = new HttpResponse<>();
        httpResponse.setCode(HttpResponse.Type.ERROR.getVal());
        httpResponse.setType(HttpResponse.Type.ERROR);
        httpResponse.setData(entity);
        httpResponse.setMessage(msg);
        httpResponse.setOperationObject(operationObject);
        return httpResponse;
    }

    //    @ExceptionHandler(Exception.class)
    public HttpResponse<String> exceptionHandler(Exception e)
    {
        log.error("---BaseController ExceptionHandler ---");
//        if (e instanceof ConstraintViolationException) {
//            for (ConstraintViolation cv : ((ConstraintViolationException) e).getConstraintViolations()) {
//                return error("", cv.getMessage());
//            }
//        }
//        if(e instanceof MethodArgumentNotValidException) {
//            List<ObjectError> oes = ((MethodArgumentNotValidException)e).getBindingResult().getAllErrors();
//            return error("", oes.get(0).getDefaultMessage());
//        }
        return error("", "请求超时");
    }

    public <T> HttpResponse<T> checkResult(T result, String operationObject)
    {
        if (Objects.isNull(result) || result.equals(0)) {
            return error(result, "", operationObject);
        }
        if (result instanceof String && ErrorCodeList.getErrorCodeList().contains(result)) {
            return error(result, (String) result, operationObject);
        }
        return ok(result, operationObject);
    }

    public <T> HttpResponse<T> checkResult(T result)
    {
        if (Objects.isNull(result) || result.equals(0)) {
            return error(result, "");
        }
        if (result instanceof String && ErrorCodeList.getErrorCodeList().contains(result)) {
            return error(result, (String) result);
        }
        return ok(result);
    }

    protected <T> HttpResponse<T> submitAsyncTask(String taskID,AsyncTaskThread<?> runnable,BackendTaskInfoHandler backendTaskInfoHandler,ThreadPoolExecutor threadPoolExecutor){
        Future<?> future = null;
        try {
            backendTaskInfoHandler.setTaskExecuteState(taskID, TASK_STATE.EXECUTING);   // 不能runnable内部才设定正在执行, 因为该请求返回后, 线程池可能没法立即执行该任务
            future = threadPoolExecutor.submit(runnable);    
        } catch(RejectedExecutionException rejectException){
            log.error(String.format("线程池拒绝执行词条更新任务, taskID : %s", taskID), rejectException);
            backendTaskInfoHandler.setTaskExecuteState(taskID, TASK_STATE.FAILED);    
            return error(null, "线程池拒绝执行词条更新任务, 请稍后重试");   // 任务没有提交执行, 缓存没有信息
        } catch(NullPointerException nullException){
            log.error(String.format("系统服务异常, 出现空指针, taskID : %s", taskID), nullException);
            backendTaskInfoHandler.setTaskExecuteState(taskID, TASK_STATE.FAILED);    
            return error(null, "系统服务异常, runnable = null");    // 任务没有提交执行, 缓存没有信息
        } catch (Exception e) {
            log.error(String.format("提交更新词条任务到线程池时出现异常, taskID : %s", taskID), e);
            // runnable可能已经执行，需要中断该任务
            boolean isStopSuccessfully = backendTaskInfoHandler.stopTask(taskID, future);
            backendTaskInfoHandler.setTaskExecuteState(taskID, TASK_STATE.FAILED);
            
            return error(null, String.format("创建更新词条任务时出现异常, 异常信息为: %s", e.getMessage()));
        }
        try {
            HttpResponse<T> resposne = new HttpResponse<>();
            resposne.setCode(HttpResponse.Type.OK.getVal());
            resposne.setType(HttpResponse.Type.OK);     
            resposne.setData(null);
            resposne.setMessage("创建词条更新任务成功");
            return resposne;
        } catch (Exception e) {
            log.error("创建更新词条任务时出现异常", e);
            // runnable可能已经执行， 需要中断该任务
            backendTaskInfoHandler.stopTask(taskID, future);
            backendTaskInfoHandler.setTaskExecuteState(taskID, TASK_STATE.FAILED);
            return error(null, String.format("创建更新词条任务时出现异常, 异常信息为: %s", e.getMessage()));
        }
    }

    protected abstract class AsyncTaskThread<T> implements Runnable{

        protected String taskID;

        protected BackendTaskInfoHandler backendTaskInfoHandler;

        public AsyncTaskThread(String taskID,BackendTaskInfoHandler backendTaskInfoHandler){
            this.taskID = taskID;
            this.backendTaskInfoHandler = backendTaskInfoHandler;
        }

        protected void afterTaskFinished(String taskID,T resultVO){
            TASK_STATE beforeExecuteState = backendTaskInfoHandler.getTaskExecuteState(taskID);
            if(beforeExecuteState != null && (beforeExecuteState == TASK_STATE.CANCELED || beforeExecuteState == TASK_STATE.CANCEL_FAILED)){
                backendTaskInfoHandler.setTaskResultVOs(taskID, checkResult(null, "任务执行失败, 请稍后重试"));
            }else{
                backendTaskInfoHandler.setTaskResultVOs(taskID, resultVO);
                backendTaskInfoHandler.setTaskExecuteState(taskID, TASK_STATE.FINISHED);    
            }
        }

        protected void afterThrowException(String taskID,Exception e){
            backendTaskInfoHandler.setTaskResultVOs(taskID, checkResult("内部子线程执行更新任务时出现异常",e.getMessage()));
            backendTaskInfoHandler.setTaskExecuteState(taskID, TASK_STATE.FAILED);    
        }

        protected abstract T runInternal();

        @Override
        public void run() {
            try {
                T resultVO = runInternal();
                this.afterTaskFinished(taskID, resultVO);
            } catch (Exception e) {
                this.afterThrowException(this.taskID,e);
            }
        }
        
    }

}
