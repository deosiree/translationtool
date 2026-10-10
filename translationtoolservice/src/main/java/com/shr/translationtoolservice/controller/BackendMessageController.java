package com.shr.translationtoolservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.vo.SourceEntryVO;
import com.shr.translationtoolservice.entity.vo.UpdateEntryInfoByFileVO;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import com.shr.translationtoolservice.util.task.BackendTaskInfoHandler;
import com.shr.translationtoolservice.util.task.BackendTaskInfoHandler.TASK_STATE;
import com.shr.translationtoolservice.util.task.BackendTaskInfoHandler.TaskIDGenerator.TaskType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/backendInfo")
@Api(tags = "后台任务信息管理")
@Slf4j
public class BackendMessageController extends BaseController {

    @Autowired
    private BackendTaskInfoHandler backendTaskInfoHandler;

    @PostMapping("/getLangDirImportTaskState")
    @ApiOperation("获取从lang文件夹导入词条的任务的目前的信息")
    @CrossOrigin
    public HttpResponse<Map<String,Object>> getLangDirImportTaskState(@RequestParam("id") String backendTaskID){
        final String STATE = "state";
        TASK_STATE taskExecuteState = backendTaskInfoHandler.getTaskExecuteState(backendTaskID);
        Map<String,Object> responseMessage = new HashMap<>();
        responseMessage.put("productIDs", null);
        HttpResponse<Map<String,Object>> response = new HttpResponse<>();
        response.setCode(HttpResponse.Type.OK.getVal());
        response.setType(HttpResponse.Type.OK);
        if(taskExecuteState == null){
            /* 没有查到任务的信息 */
            // return ok("0");
            responseMessage.put(STATE, "0");
            response.setMessage("没有查到正在执行的任务信息");
        }else if(taskExecuteState == TASK_STATE.FINISHED){
            /* 任务执行完了，返回信息后删除掉之前这个任务的所有信息 */ 
            backendTaskInfoHandler.deleteEntryImportFromLangDirTaskMessage(backendTaskID);
            responseMessage.put(STATE, "0");
            response.setMessage("任务执行成功");    
        }else if(taskExecuteState == TASK_STATE.EXECUTING){
            // return ok("1");
            responseMessage.put(STATE, "1");
            response.setMessage("任务正在执行中");
        }else if(taskExecuteState == TASK_STATE.FAILED){
    
            responseMessage.put(STATE, "2");
            List<String> failedProductIDs = backendTaskInfoHandler.getEntryImportFromLangDirTaskProductIDs(backendTaskID);
            responseMessage.put("productIDs", failedProductIDs);
            response.setMessage("任务执行失败");
            backendTaskInfoHandler.deleteEntryImportFromLangDirTaskMessage(backendTaskID);

        }else{
            responseMessage.put(STATE, "0");
            response.setMessage("系统服务存在异常, 联系研发");
            log.warn("警告, 未对任务执行状态: 的返回相应进行处理" + taskExecuteState.toString());
        }
        response.setData(responseMessage);
        return response;

    }

    @PostMapping("/getLangDirImportTaskLogMessages")
    @ApiOperation("获取从lang文件夹导入词条的任务的目前的信息")
    @CrossOrigin
    public HttpResponse<List<String>> getLangDirImportTaskLogMessages(@RequestParam("id") String backendTaskID,@RequestParam(name = "getAndDelete",required = false) boolean deleteMessage){

        // String   backendTaskInfoHandler.getTas
        List<String> taskMessaage = backendTaskInfoHandler.getTaskMessaage(backendTaskID);
        if(deleteMessage){
            backendTaskInfoHandler.deleteEntryImportFromLangDirTaskMessage(backendTaskID);
        }
        return ok(taskMessaage);

    }

    protected HttpResponse<Map<String,Object>> getTaskState(String taskID){
        final String STATE = "state";
        try {
            TASK_STATE taskExecuteState = backendTaskInfoHandler.getTaskExecuteState(taskID);
            Map<String,Object> responseMessage = new HashMap<>();
            HttpResponse<Map<String,Object>> response = new HttpResponse<>();
            response.setCode(HttpResponse.Type.OK.getVal());
            response.setType(HttpResponse.Type.OK);
            if(taskExecuteState == null){
                /* 没有查到任务的信息 */
                responseMessage.put(STATE, "0");
                response.setMessage("没有查到正在执行的任务信息");
            }else if(taskExecuteState == TASK_STATE.FINISHED){
                responseMessage.put(STATE, "2");
                response.setMessage("任务执行成功");    
                backendTaskInfoHandler.deleteTaskExecuteState(taskID);
            }else if(taskExecuteState == TASK_STATE.EXECUTING){
                responseMessage.put(STATE, "1");
                response.setMessage("任务正在执行中");
            }else if(taskExecuteState == TASK_STATE.FAILED){
                responseMessage.put(STATE, "3");
                response.setMessage("任务执行失败");
                backendTaskInfoHandler.deleteTaskExecuteState(taskID);
            }else if(taskExecuteState == TASK_STATE.CANCELED){
                /* 这种状态下, 可能会查到任务的执行结果, 因为stop失败 */
                responseMessage.put(STATE, "4");
                response.setMessage("任务终止执行");
                backendTaskInfoHandler.deleteTaskExecuteState(taskID);
            }else if(taskExecuteState == TASK_STATE.CANCEL_FAILED){
                responseMessage.put(STATE, "5");
                response.setMessage("任务终止执行失败");
                backendTaskInfoHandler.deleteTaskExecuteState(taskID);
            }else{
                responseMessage.put(STATE, "6");
                response.setMessage("系统服务存在异常, 联系研发, 存在未知的任务状态");
                log.warn("警告, 未对任务执行状态: 的返回相应进行处理" + taskExecuteState.toString());
                backendTaskInfoHandler.deleteTaskExecuteState(taskID);
            }
            response.setData(responseMessage);
            return response;            
        } catch (Exception e) {
            return error(null, e.getMessage());
        } finally{
        }
    }

    @PostMapping("/getEntrysourceListByClassfyTaskState")
    @ApiOperation("获取更新任务的执行状态(成功,失败,执行中)")
    @CrossOrigin
    public HttpResponse<Map<String,Object>> getEntrysourceListByClassfyTaskState(@RequestParam String classifyID,@RequestParam String i18nUrl,HttpServletRequest request){
        final TaskType taskType = TaskType.UPDATE_ENTRY;
        final String taskID = BackendTaskInfoHandler.TaskIDGenerator.getTaskID(taskType,classifyID,i18nUrl); // 参数顺序必须一致
        return this.getTaskState(taskID);
    }

    @PostMapping("/getEntrysourceListByClassfyResult")
    @ApiOperation("获取更新任务的结果")
    @CrossOrigin
    @SuppressWarnings("unchecked")
    public HttpResponse<ResponseListModel<SourceEntryVO>> getEntrysourceListByClassfyResult(@RequestParam String classifyID,@RequestParam String i18nUrl,HttpServletRequest request){

        try {
            final TaskType taskType = TaskType.UPDATE_ENTRY;
            String token = request.getHeader("token");
            String department = JWTTokenUtils.getDepartment(token);
            String username = JWTTokenUtils.getUserName(token);
            final String taskID = BackendTaskInfoHandler.TaskIDGenerator.getTaskID(taskType,classifyID,i18nUrl);
            HttpResponse<ResponseListModel<SourceEntryVO>> taskResultVOs = (HttpResponse<ResponseListModel<SourceEntryVO>>) backendTaskInfoHandler.getTaskResultVOs(taskID,true);
            return taskResultVOs == null ? ok(null) : taskResultVOs;         
        } catch (Exception e) {
            return error(null, e.getMessage());
        }
    }

    @PostMapping("/getEntryImportExcleTaskState")
    @ApiOperation("获取更新翻译任务的执行状态(成功,失败,执行中)")
    @CrossOrigin
    public HttpResponse<Map<String,Object>> getEntryImportExcleTaskState(@RequestParam("transType") String transType,HttpServletRequest request){
        final TaskType taskType = TaskType.UPDATE_TRANSLATION;
        String token = request.getHeader("token");
        String department = JWTTokenUtils.getDepartment(token);
        String username = JWTTokenUtils.getUserName(token);
        final String taskID = BackendTaskInfoHandler.TaskIDGenerator.getTaskID(taskType,transType,department,username); // 参数顺序必须一致
        return this.getTaskState(taskID);

    }

    @PostMapping("/getEntryImportExcleTaskStateResult")
    @ApiOperation("获取更新翻译任务的结果")
    @CrossOrigin
    @SuppressWarnings("unchecked")
    public HttpResponse<UpdateEntryInfoByFileVO> getEntryImportExcleTaskStateResult(@RequestParam("transType") String transType,HttpServletRequest request){

        try {
            final TaskType taskType = TaskType.UPDATE_TRANSLATION;
            String token = request.getHeader("token");
            String department = JWTTokenUtils.getDepartment(token);
            String username = JWTTokenUtils.getUserName(token);
            final String taskID = BackendTaskInfoHandler.TaskIDGenerator.getTaskID(taskType,transType,department,username); // 参数顺序必须一致
            HttpResponse<UpdateEntryInfoByFileVO> taskResultVOs = (HttpResponse<UpdateEntryInfoByFileVO>) backendTaskInfoHandler.getTaskResultVOs(taskID,true);
            return taskResultVOs == null ? ok(null) : taskResultVOs;         
        } catch (Exception e) {
            return error(null, e.getMessage());
        }
    }

}
