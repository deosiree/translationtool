package com.shr.translationtoolservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.service.impl.BackendTaskInfoHandler;
import com.shr.translationtoolservice.service.impl.BackendTaskInfoHandler.TASK_STATE;

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
}
