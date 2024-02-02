package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.dao.EntryTempMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.TaskInfoVo;
import com.shr.translationtoolservice.service.TaskInfoService;
import com.shr.translationtoolservice.util.CommonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName TaskManageController
 * @USER: Cola
 * @Date 2023/11/6 0006 9:37
 **/

@RestController
@RequestMapping("/taskManage")
@Api(tags = "任务管理")
@Slf4j
public class TaskManageController extends BaseController {

    @Autowired
    private TaskInfoService taskInfoService;
    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private EntryTempMapper entryTempMapper;

    //查询任务信息
    @PostMapping("/searchTaskInfo")
    @ApiOperation("任务查询")
    @CrossOrigin
    public HttpResponse<ResponseListModel> getTaskInfo(@RequestBody TaskInfoEntity taskInfoEntity, HttpServletRequest request,
                                                       @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                       @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<TaskInfoEntity> result = new ResponseListModel<>();
        List<TaskInfoEntity> taskInfoEntities = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            taskInfoEntities = taskInfoService.getTaskInfo(taskInfoEntity, offset, pageSize, request);
        }
        result.setList(taskInfoEntities);
        result.setTotalNum(taskInfoService.getTotalNum(taskInfoEntity));
        return checkResult(result);

    }

    @PostMapping("/addTaskInfos")
    @ApiOperation("任务新增")
    @CrossOrigin
    @Transactional
    //返回id
    public HttpResponse<String> addTaskInfos(@RequestBody List<TaskInfoVo> taskInfoVoList, HttpServletRequest request) {


        String result = taskInfoService.addTaskInfoList(taskInfoVoList, request);


        return checkResult(result);

    }


    @PostMapping("/updateTaskInfo")
    @ApiOperation("任务更新")
    @CrossOrigin
    //返回id
    public HttpResponse<String> updateTaskInfo(@RequestBody TaskInfoVo taskInfoVo) {


        String result = taskInfoService.updateTaskInfo(taskInfoVo);


        return checkResult(result);

    }

    @PostMapping("/deleteTaskInfo")
    @ApiOperation("任务删除")
    @CrossOrigin
    //返回id
    public HttpResponse<String> deleteTaskInfo(@RequestBody List<String> taskIds) {


        String result = taskInfoService.deleteTaskInfo(taskIds);


        return checkResult(result);

    }


    @PostMapping("/taskSubmission")
    @ApiOperation("任务递交")
    @CrossOrigin
    @Transactional
    //返回id
    public HttpResponse<String> taskSubmission(@RequestBody List<String> taskIDs) {

        String oldState = "0";
        String nextState = "1";
        String result = taskInfoService.taskSubmission(taskIDs, oldState, nextState);


        return checkResult(result);

    }

    //任务待办查询
    @PostMapping("/getToDoTaskInfo")
    @ApiOperation("任务待办查询")
    @CrossOrigin
    public HttpResponse<ResponseListModel> getToDoTaskInfo(HttpServletRequest request ,@RequestBody TaskInfoEntity taskInfoEntity,
                                                           @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                           @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<TaskInfoEntity> result = new ResponseListModel<>();
        List<TaskInfoEntity> taskInfoEntities = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            taskInfoEntities = taskInfoService.getToDoTaskInfo(offset, pageSize, request,taskInfoEntity);
        }
        result.setList(taskInfoEntities);
        result.setTotalNum(taskInfoService.getToDoTaskInfoTotal(request,taskInfoEntity));
        return checkResult(result);

    }

    //查询任务信息
    @PostMapping("/getFinishTaskInfo")
    @ApiOperation("任务已办查询")
    @CrossOrigin
    public HttpResponse<ResponseListModel> getFinishTaskInfo(HttpServletRequest request,@RequestBody TaskInfoEntity taskInfoEntity,
                                                             @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                             @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<TaskInfoEntity> result = new ResponseListModel<>();
        List<TaskInfoEntity> taskInfoEntities = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            taskInfoEntities = taskInfoService.getFinishTaskInfo(offset, pageSize, request,taskInfoEntity);
        }
        result.setList(taskInfoEntities);
        result.setTotalNum(taskInfoService.getFinishTaskInfoTotal(request,taskInfoEntity));
        return checkResult(result);

    }

    //查询任务信息
    @PostMapping("/taskEntryExport")
    @ApiOperation("任务词条导出")
    @CrossOrigin
    public void taskEntryExport(@RequestParam String taskID, HttpServletResponse response,@RequestParam String importType) {


       taskInfoService.taskEntryExport(taskID,response,importType);

    }

    // 根据任务id  导出任务所有词条
    @PostMapping("/exportEntryByTaskId")
    @ApiOperation("根据任务id导出词条")
    @CrossOrigin
    public void exportEntryByTaskId(@RequestParam String taskId, HttpServletResponse response){
        taskInfoService.exportEntryByTaskId(taskId,response);
    }

    //以任务生成任务
    @PostMapping("/taskCreateNewLanguageTask")
    @ApiOperation("任务生成任务")
    @CrossOrigin
    public HttpResponse<String> taskCreateNewLanguageTask(@RequestBody TaskInfoEntity taskInfoEntity,
                                                          @RequestParam String taskID) {


        String  id =  taskInfoService.taskCreateNewLanguageTask(taskInfoEntity,taskID);
        return checkResult(id);
    }


    //以任务生成任务
    @PostMapping("/getImportType")
    @ApiOperation("获取导入类型")
    @CrossOrigin
    public HttpResponse<Map<String, String>> getImportType( @RequestParam String taskID) {

        ResponseListModel<String> result = new ResponseListModel<>();
        Map<String, String> typeMap =  taskInfoService.getImportType(taskID);

        return checkResult(typeMap);
    }
}
