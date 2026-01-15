package com.shr.translationtoolservice.controller;

import com.google.gson.Gson;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.TaskImportEntryEntity.TaskImportEntryVO;
import com.shr.translationtoolservice.entity.TaskInfoEntity.TaskInfoEntityVO;
import com.shr.translationtoolservice.entity.vo.TaskInfoVo;
import com.shr.translationtoolservice.service.TLanguageService;
import com.shr.translationtoolservice.service.TaskInfoService;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import com.shr.translationtoolservice.util.task.BackendTaskInfoHandler;
import com.shr.translationtoolservice.util.task.BackendTaskInfoHandler.TASK_STATE;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private BackendTaskInfoHandler backendTaskInfoHandler;
    @Autowired
    private BatchInsertEntryHandler batchInsertEntryHandler;
    @Autowired
    private TLanguageService languageService;

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
        /* 添加codeBranch属性，把view对象修改一下 */
        ResponseListModel<TaskInfoEntityVO> result = new ResponseListModel<>();
        List<TaskInfoEntityVO> taskInfoEntities = new ArrayList<>();
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

    //写入版本表词条
    @PostMapping("/putTempToProductTable")
    @ApiOperation("写入版本表词条")
    @CrossOrigin
    public HttpResponse<String> putTempToProductTable( @RequestParam List<EntryTempEntity> entryTempEntities) {

        ResponseListModel<String> result = new ResponseListModel<>();
        String i = taskInfoService.putTempToProductTable(entryTempEntities);

        return checkResult(i);
    }


    @PostMapping("/createTaskByLang")
    @ApiOperation("批量创建任务")
    @CrossOrigin
    public HttpResponse<String> batchCreateTask(
        @RequestBody List<TaskInfoVo> taskInfoVos,
        @RequestParam("ip") String i18nAddress,
        @RequestParam(name = "link",required = false) String taskDirMapJsonString,
        @RequestParam("translateTypes[]") List<String> targetLanguageTypes,
        @RequestParam("parentId") String backendTaskID,
        HttpServletRequest request
    ){

        if(taskInfoVos.isEmpty()){
            return error(null, "没有提供任务, 无法执行");
        }
        /** 实际使用用的 */
        Gson gson = new Gson();
        Map<String,String> taskDirMap = gson.fromJson(taskDirMapJsonString, Map.class);
        /* 导出功能做去重校验 */
        String token = request.getHeader("token");
        String message = "后台将要执行的任务ID为: " + backendTaskID; 
        log.info(message);
        backendTaskInfoHandler.setTaskExecuteState(backendTaskID, TASK_STATE.EXECUTING);

        backendTaskInfoHandler.setEntryImportFromLangDirTaskProductIDs(backendTaskID, taskInfoVos.stream().map(TaskInfoVo::getProductId).collect(Collectors.toList()));
        log.info("设定后台任务执行状态成功,状态为正在执行,ID: " + backendTaskID);
        Map<String,Object> otherArgs = new HashMap<>();
        Collection<String> ignoredFiles = new HashSet<>();
        taskInfoVos.stream().forEach(new Consumer<TaskInfoVo>() {

            @Override
            public void accept(TaskInfoVo t) {
                // TODO Auto-generated method stub
                ignoredFiles.addAll(t.getIgnore());
            }
        });
        otherArgs.put("ignore_files", ignoredFiles);
        Thread thread = new Thread() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    String resultMessage = taskInfoService.createTaskAndCreateEntryByLangDir(i18nAddress, taskInfoVos, taskDirMap, token, targetLanguageTypes,backendTaskID,otherArgs);    
                    backendTaskInfoHandler.addMessageForTaskID(backendTaskID, resultMessage);   
                    backendTaskInfoHandler.addMessageForTaskID(backendTaskID,String.format("任务: %s 执行完成", backendTaskID));
                    log.info("后台任务执行完毕, 修改任务执行状态, ID: " + backendTaskID);
                    backendTaskInfoHandler.setTaskExecuteState(backendTaskID, TASK_STATE.FINISHED);   
                    log.info("修改后台任务执行状态成功,状态为执行完成,ID: " + backendTaskID);
                } catch (Exception e) {
                    // TODO: handle exception
                    log.error(e.getMessage(), e);
                    backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "执行失败,异常信息为: " +  e.getMessage());    
                    backendTaskInfoHandler.setTaskExecuteState(backendTaskID, TASK_STATE.FAILED);   
                } finally{

                }
            }
            
        };
        thread.start();
        return ok(backendTaskID);
    }


    @PostMapping("/getTRFileListUsingI18nServer")
    @ApiOperation("获取所有可导入词条的dic文件名")
    @CrossOrigin
    public HttpResponse<List<String>> getTRFileListUsingI18nServer(@RequestParam("ip") String  i18nAddress,HttpServletRequest request){
        try {
            List<String> trFileListUsingI18nServer = batchInsertEntryHandler.getTRFileListUsingI18nServer(i18nAddress,null);
            if(trFileListUsingI18nServer == null){
                return error(null, "获取dic文件列表时出现异常");
            }
            return ok(trFileListUsingI18nServer);   
        } catch (Exception e) {
            return error(null, e.getMessage());
        }
    }

    @PostMapping("/getTSFileListUsingI18nServer")
    @ApiOperation("获取所有可导入词条的ts文件名")
    @CrossOrigin
    public HttpResponse<Set<String>> getTSFileListUsingI18nServer(@RequestParam("ip") String  i18nAddress,@RequestParam("translateTypes[]") List<String> targetLanguageTypes,HttpServletRequest request){
   
        try {
            Set<String> tsFileListUsingI18nServer = batchInsertEntryHandler.getTSFileListUsingI18nServer(i18nAddress, targetLanguageTypes);
            if(tsFileListUsingI18nServer == null){
                return error(null, "获取ts文件列表时出现异常");
            }

            List<TLanguage> tLanguages = languageService.getLanguages(new TLanguage());

            Set<String> tsEntrySources = tsFileListUsingI18nServer.stream().map(new Function<String,String>() {

                @Override
                public String apply(String fileName) {
                    // TODO Auto-generated method stub
                    for (TLanguage tLanguage : tLanguages) {
                        if (fileName.contains(tLanguage.getCode())) {
                            return fileName.substring(0, fileName.indexOf("_" + tLanguage.getCode()));
                        }
                    }
                    return fileName;
                }
                
            }).collect(Collectors.toSet());
        
            return ok(tsEntrySources);
        } catch (Exception e) {
            return error(null, e.getMessage());
        }
 
    }

    @PostMapping("/getFileListUsingI18nServer")
    @ApiOperation("获取所有的dic和ts文件名")
    @CrossOrigin
    public HttpResponse<List<Map<String,String>>> getFileListUsingI18nServer(
        @RequestParam("ip") String i18nAddress,
        @RequestParam("translateTypes[]") List<String> targetLanguageTypes,
        HttpServletRequest request
    ){
        // Map<String,Set<String>> resultMap = new HashMap<>();
        List<Map<String,String>> resultMapList = new ArrayList<>();
        try {
            List<String> trFileListUsingI18nServer = batchInsertEntryHandler.getTRFileListUsingI18nServer(i18nAddress,null);
            if(trFileListUsingI18nServer == null){
                return error(null, "获取dic文件列表时出现异常");
            }  
            /* 将tr分类，根据db,config,enum,meta,tr */
            trFileListUsingI18nServer.stream().forEach(new Consumer<String>() {

                @Override
                public void accept(String t) {
                    // TODO Auto-generated method stub
                    Map<String,String> item = new HashMap<>();
                   if(t.startsWith("db/")){
                        item.put("link", "db");
                        item.put("title", t);
                   }else if(t.startsWith("db/meta/")){
                        item.put("link", "meta");
                        item.put("title", t);
                   }else if(t.startsWith("config/")){
                        item.put("link", "config");
                        item.put("title", t);
                   }else if(t.startsWith("enum/")){
                        item.put("link", "enum");
                        item.put("title", t);
                   }else if(t.startsWith("tr/")){
                        item.put("link", "tr");
                        item.put("title", t);
                   }else{
                        throw new RuntimeException(String.format("警告, 该dic文件类型无法识别, 其内容为: %s", t));
                   } 
                   resultMapList.add(item);
                }
                
            });

            Set<String> tsFileListUsingI18nServer = batchInsertEntryHandler.getTSFileListUsingI18nServer(i18nAddress, targetLanguageTypes);
            if(tsFileListUsingI18nServer == null){
                return error(null, "获取ts文件列表时出现异常");
            }

            List<TLanguage> tLanguages = languageService.getLanguages(new TLanguage());

            Set<String> tsEntrySources = tsFileListUsingI18nServer.stream().map(new Function<String,String>() {

                @Override
                public String apply(String fileName) {
                    // TODO Auto-generated method stub
                    for (TLanguage tLanguage : tLanguages) {
                        if (fileName.contains(tLanguage.getCode())) {
                            return fileName.substring(0, fileName.indexOf("_" + tLanguage.getCode()));
                        }
                    }
                    return fileName;
                }
                
            }).collect(Collectors.toSet());
            tsEntrySources.stream().forEach(new Consumer<String>() {

                @Override
                public void accept(String t) {
                    // TODO Auto-generated method stub
                    Map<String,String> item = new HashMap<>();
                    item.put("link", "ts");
                    item.put("title", t);
                    resultMapList.add(item);
                }

            });
            
            return ok(resultMapList);
        } catch (Exception e) {
            return error(null, e.getMessage());
        }
    }

    @PostMapping("/importEntryAndDispatchToTasks")
    @ApiOperation("将不同文件的词条导入给不同的任务")
    @CrossOrigin
    public HttpResponse<String> importEntryAndDispatchToTasks(
        @RequestParam("ip") String i18nAddress,
        @RequestBody List<TaskImportEntryVO> taskImportEntryVOs,
        @RequestParam("translateTypes[]") List<String> targetLanguageTypes,
        HttpServletRequest request
    ){
        String token = request.getHeader("token");
        String backendTaskID = commonUtils.getUUID();
        String message = "后台将要执行的任务ID为: " + commonUtils.getUUID(); 
        log.info(message);
        backendTaskInfoHandler.setTaskExecuteState(backendTaskID, TASK_STATE.EXECUTING);
        log.info("设定后台任务执行状态成功,状态为正在执行,ID: " + backendTaskID);
        Thread thread = new Thread() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    String resultMessage = taskInfoService.createEntryByLangDirForTaskInfos(i18nAddress, taskImportEntryVOs, token, targetLanguageTypes, backendTaskID);
                    backendTaskInfoHandler.addMessageForTaskID(backendTaskID, resultMessage);   
                    backendTaskInfoHandler.addMessageForTaskID(backendTaskID,String.format("任务: %s 执行完成", backendTaskID));
                } catch (Exception e) {
                    // TODO: handle exception
                    log.error(e.getMessage(), e);
                    backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "执行失败,异常信息为: " +  e.getMessage());    
                } finally{
                    log.info("后台任务执行完毕, 修改任务执行状态, ID: " + backendTaskID);
                    backendTaskInfoHandler.setTaskExecuteState(backendTaskID, TASK_STATE.FINISHED);   
                    log.info("修改后台任务执行状态成功,状态为执行完成,ID: " + backendTaskID);
                }
            }
            
        };
        thread.start();

        return ok(backendTaskID);
        
    }

    @PostMapping("/getTaskPending")
    @ApiOperation("统计每个任务中处于各阶段的词条个数")
    @CrossOrigin
    public HttpResponse<List<TaskStateEntity>> countEntryTranslateStateForTasks(@RequestBody Set<String> taskIDs){
        // Set<String> taskIDs = new HashSet<>();
        try {
            List<TaskStateEntity> taskStateEntities = taskInfoService.countEntryTranslateStateForTasks(taskIDs);
            return ok(taskStateEntities);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return error(null,"系统服务异常");
        }

    }

}
