package com.shr.translationtoolservice.util.task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 与后台任务执行相关, 现阶段的任务是接收任务传递过来的信息
 */
@Component
public class BackendTaskInfoHandler {


    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss::sss");

    /**
     * 后台任务详细的日志信息, 可用于给用户展示
     */
    protected Map<String,List<String>> taskMessageQueues = new ConcurrentHashMap<>();   // key : backendID , task

    protected Map<String,TASK_STATE> taskExecuteStates = new ConcurrentHashMap<>();

    protected Map<String,List<String>> entryImportFromLangDirTaskProductIDsMap = new HashMap<>();   // key: backendID, value: [产品1的ID，产品2的ID]



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


    public enum TASK_STATE{
        EXECUTING,
        FINISHED,
        FAILED
    }
    
}
