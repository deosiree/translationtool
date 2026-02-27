package com.shr.translationtoolservice.util.task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * 与后台任务执行相关, 现阶段的任务是接收任务传递过来的信息
 */
@Slf4j
@Component
public class BackendTaskInfoHandler {


    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss::sss");

    /**
     * 后台任务详细的日志信息, 可用于给用户展示(log)
     */
    protected Map<String,List<String>> taskMessageQueues = new ConcurrentHashMap<>();   // key : backendID , task

    protected Map<String,TASK_STATE> taskExecuteStates = new ConcurrentHashMap<>();

    protected Map<String,List<String>> entryImportFromLangDirTaskProductIDsMap = new HashMap<>();   // key: backendID, value: [产品1的ID，产品2的ID]

    /* 添加一个类，可以返回后台任务的前端view对象 */
    protected Map<String,Object> taskResultVOs = new ConcurrentHashMap<>();

    public boolean addMessageForTaskID(String taskID,String message){
        if(taskID == null || taskID.equals("")){
            return false;
        }
        List<String> taskMessages = taskMessageQueues.get(taskID);

        if(taskMessages == null){
            taskMessages = new ArrayList<>();
            taskMessageQueues.put(taskID, taskMessages);
        }
        Date date = new Date(System.currentTimeMillis());
        taskMessages.add(String.format("%s: %s", this.format.format(date),message));
        return true;
    }

    public List<String> getTaskMessaage(String taskID){
        if(taskID == null || taskID.equals("")){
            return null;
        }
        return this.taskMessageQueues.get(taskID);
    }

    public TASK_STATE getTaskExecuteState(String taskID){
        if(taskID == null || taskID.equals("")){
            return null;
        }
        return this.taskExecuteStates.get(taskID);
    }

    public boolean setTaskExecuteState(String taskID,TASK_STATE taskState){

        if(taskID == null || taskID.equals("")){
            return false;
        }
        this.taskExecuteStates.put(taskID, taskState);
        return true;
    }

    public void deleteTaskExecuteState(String taskID){
        this.taskExecuteStates.remove(taskID);
        return;
    }

    public boolean deletetTaskMessage(String taskID){
        List<String> removedMessages = taskMessageQueues.remove(taskID);
        if(removedMessages == null){
            return false;
        }
        TASK_STATE removedState = taskExecuteStates.remove(taskID);
        if(removedState == null){
            return false;
        }
        return true;
    }

    public boolean setEntryImportFromLangDirTaskProductIDs(String taskID,List<String> productIDs){

        entryImportFromLangDirTaskProductIDsMap.put(taskID, productIDs);
        return true;
    }

    public List<String> getEntryImportFromLangDirTaskProductIDs(String taskID){
        return entryImportFromLangDirTaskProductIDsMap.get(taskID);
    }

    public boolean deleteEntryImportFromLangDirTaskMessage(String taskID){
        if(!this.deletetTaskMessage(taskID)){
            return false;
        }
        List<String> removedProductIDs = entryImportFromLangDirTaskProductIDsMap.remove(taskID);
        if(removedProductIDs == null){
            return false;
        }
        return true;
    }


    public Object getTaskResultVOs(String taskID,boolean onlyOnce){

        Object object = this.taskResultVOs.get(taskID);
        if(object == null){
            return null;
        }
        if(onlyOnce){
            this.deleteTaskResultVOs(taskID);
        }
        return object;
    }

    public boolean setTaskResultVOs(String taskID,Object resultVO){
        this.taskResultVOs.put(taskID, resultVO);
        return true;
    }

    public void deleteTaskResultVOs(String taskID){
        this.taskExecuteStates.remove(taskID);
        return;
    }

    public boolean stopTask(String taskID,Future<?> future){
        if(future == null){
            return true;
        }
        log.debug(String.format("准备阻止更新任务执行, 正在检查任务状态, taskID : %s", taskID));
        if(!future.isCancelled()){
            /* 之前已经被取消过 */
            log.warn(String.format("警告, 该更新任务之前被取消过, taskID: %s", taskID));
        }else if(future.isDone()){
            /* 执行完毕, 删除结果 */
            log.debug(String.format("更新任务已经执行完毕, 已无法停止, taskID: %s", taskID));
        }else{
            /* 正在执行中或还没有执行 */
            log.debug("任务可以被中断, 准备阻止更新任务执行");
            boolean isCancelSuccessfully = future.cancel(true);
            if(isCancelSuccessfully){
                log.debug(String.format("阻止任务执行成功, taskID: %s", taskID));
            }else{
                log.debug(String.format("阻止任务执行失败, taskID: %s", taskID));
                this.setTaskExecuteState(taskID, TASK_STATE.CANCEL_FAILED);
                return false;
            }
        }
        this.setTaskExecuteState(taskID, TASK_STATE.CANCELED);
        return true;

    }

    public enum TASK_STATE{
        EXECUTING,
        FINISHED,
        FAILED,
        CANCELED,
        CANCEL_FAILED
    }


    public static class TaskIDGenerator{

        public static String getTaskID(TaskType taskType,String... taskIDArguments){
            String taskIDPrefix = null;
            final String separator = "-";
            if(taskType == TaskType.UPDATE_ENTRY){
                taskIDPrefix = "update-entry";
            }else if(taskType == TaskType.UPDATE_TRANSLATION){
                taskIDPrefix = "update-translation";
            }else{
                return taskIDPrefix;
            }
            StringBuilder builder = new StringBuilder();
            builder.append(taskIDPrefix);
            builder.append(String.join(separator, taskIDArguments));
            return builder.toString();
        }

        public enum TaskType{
            UPDATE_ENTRY,   // 更新词条任务
            UPDATE_TRANSLATION
        }


    }
    
}
