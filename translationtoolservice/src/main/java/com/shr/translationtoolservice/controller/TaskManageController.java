package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
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

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

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
    public HttpResponse<ResponseListModel> getToDoTaskInfo(HttpServletRequest request,
                                                           @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                           @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<TaskInfoEntity> result = new ResponseListModel<>();
        List<TaskInfoEntity> taskInfoEntities = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            taskInfoEntities = taskInfoService.getToDoTaskInfo(offset, pageSize, request);
        }
        result.setList(taskInfoEntities);
        result.setTotalNum(taskInfoService.getToDoTaskInfoTotal(request));
        return checkResult(result);

    }

    //查询任务信息
    @PostMapping("/getFinishTaskInfo")
    @ApiOperation("任务已办查询")
    @CrossOrigin
    public HttpResponse<ResponseListModel> getFinishTaskInfo(HttpServletRequest request,
                                                             @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                             @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<TaskInfoEntity> result = new ResponseListModel<>();
        List<TaskInfoEntity> taskInfoEntities = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            taskInfoEntities = taskInfoService.getFinishTaskInfo(offset, pageSize, request);
        }
        result.setList(taskInfoEntities);
        result.setTotalNum(taskInfoService.getFinishTaskInfoTotal(request));
        return checkResult(result);

    }

    //查询任务信息
    @PostMapping("/taskEntryExport")
    @ApiOperation("任务词条导出")
    @CrossOrigin
    public HttpResponse<String> taskEntryExport(@RequestParam String taskID) {

        String url = taskInfoService.taskEntryExport(taskID);
        return checkResult(url);
    }

}
