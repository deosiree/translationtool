package com.shr.translationtoolservice.service.impl;

import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.TaskInfoVo;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.service.EntryTempService;
import com.shr.translationtoolservice.service.TaskInfoService;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.ExcelUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 */
@Service
@Slf4j
public class TaskInfoServiceImpl extends ServiceImpl<TaskInfoMapper, TaskInfoEntity>
        implements TaskInfoService {


    @Autowired
    private TaskInfoMapper taskInfoMapper;
    @Autowired
    private CommonUtils commonUtils;
    @Autowired
    private VersionMapper versionMapper;
    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private EntryInfoMapper entryInfoMapper;

    @Autowired
    private EntryTempMapper entryTempMapper;

    @Autowired
    private ExcelUtils excelUtil;
    @Autowired
    private VersionTableMapper versionTableMapper ;

    @Autowired
    private EntryTempService entryTempService;

    @Autowired
    private TranslateMapper translateMapper;

    @Autowired
    private EntryInfoService entryInfoService;

    @Autowired
    private EntryClassifyMapper entryClassifyMapper;

    @Override
    //获取任务信息
    //入参 taskInfoEntity 任务实体 ， offset 页码，pageSize 页内行数
    public List<TaskInfoEntity> getTaskInfo(TaskInfoEntity taskInfoEntity, Integer offset, Integer pageSize, HttpServletRequest request) {

        List<TaskInfoEntity> taskInfoEntities = taskInfoMapper.getTaskInfo(taskInfoEntity, offset, pageSize);
       /* VersionEntity versionEntity = versionMapper.selectById(taskInfoEntity.getVersionId());
        if (Objects.nonNull(versionEntity)) {
            for (TaskInfoEntity taskInfoEntity1 : taskInfoEntities) {
                    // 当前任务已完成时 从t_version_xxxxxx表中查询词条数量
                    List<EntryInfoEntity> entryInfoEntities = entryInfoMapper.getEntryByTaskID(taskInfoEntity1.getId(), versionEntity.getTableName());
                    taskInfoEntity1.setEntryNum(entryInfoEntities.size());

            }
        }
*/
        return taskInfoEntities;
    }

    @Override
    public int getTotalNum(TaskInfoEntity taskInfoEntity) {
        int total = taskInfoMapper.getTaskInfoTotal(taskInfoEntity);
        return total;
    }

    @Override
        public String addTaskInfoList(List<TaskInfoVo> taskInfoVoList, HttpServletRequest request) {

        String token = request.getHeader("token");

        for (TaskInfoVo taskInfoVo : taskInfoVoList) {
            TaskInfoEntity taskInfoEntity = new TaskInfoEntity();
            BeanUtils.copyProperties(taskInfoVo, taskInfoEntity);
            String id = commonUtils.getUUID();
            taskInfoEntity.setId(id);
            //创建人
            String userName = JWTTokenUtils.getUserName(token);
            if (StringUtils.isBlank(taskInfoEntity.getCreator())) {
                taskInfoEntity.setCreator(userName);
            }
            //创建部门
            String department = JWTTokenUtils.getDepartment(token);
            if (StringUtils.isBlank(taskInfoEntity.getDepartment())) {
                taskInfoEntity.setDepartment(department);
            }

            if (Objects.isNull(taskInfoEntity.getUpgrade())) {
                taskInfoEntity.setUpgrade(0);
            }

            //创建时间
            if (Objects.isNull(taskInfoEntity.getCreateTime())) {
                Date date = new Date(System.currentTimeMillis());
                taskInfoEntity.setCreateTime(date);
            }
            //状态更新导入状态
            taskInfoEntity.setState("0");

            taskInfoEntity.setIsDelete(0);
            int insert = taskInfoMapper.insert(taskInfoEntity);
            if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.INSERT_ERROR;
            }
        }


        return ConstantInterface.OK_STR;
    }


    @Override
    public String updateTaskInfo(TaskInfoVo taskInfoVo) {

        TaskInfoEntity taskInfoEntity = new TaskInfoEntity();
        BeanUtils.copyProperties(taskInfoVo, taskInfoEntity);
        int update = taskInfoMapper.updateById(taskInfoEntity);
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }

        return ConstantInterface.OK_STR;
    }

    @Override
    public String deleteTaskInfo(List<String> taskIds) {
        int delete = taskInfoMapper.deleteByIds(taskIds);
        if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String taskSubmission(List<String> taskIDs, String oldState, String nextState) {

        for (String taskId : taskIDs) {
            TaskInfoEntity taskInfoEntity = new TaskInfoEntity();
            taskInfoEntity.setId(taskId);
            taskInfoEntity.setState(nextState);
            //根据状态更新操作时间
            if (ConstantInterface.DELIVERY_STATE.equals(oldState)) {
                taskInfoEntity.setDeliveryTime(new Date(System.currentTimeMillis()));
            } else if (ConstantInterface.IMPORT_STATE.equals(oldState)) {
                taskInfoEntity.setImportTime(new Date(System.currentTimeMillis()));
            } else if (ConstantInterface.ENTRY_AUDIT_STATE.equals(oldState)) {
                taskInfoEntity.setEntryAutiorStartTime(new Date(System.currentTimeMillis()));
            } else if (ConstantInterface.TRANSLATE_STATE.equals(oldState)) {
                taskInfoEntity.setTranslateStartTime(new Date(System.currentTimeMillis()));
            } else if (ConstantInterface.TRANSLATE_AUDIT_STATE.equals(oldState)) {
                taskInfoEntity.setTranslationAuditorStartTime(new Date(System.currentTimeMillis()));
            } else if (ConstantInterface.EXPORT_STATE.equals(oldState)) {
                taskInfoEntity.setEndTime(new Date(System.currentTimeMillis()));
            }
            int update = taskInfoMapper.updateById(taskInfoEntity);
            if (update != ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.UPDATE_ERROR;
            }
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public List<TaskInfoEntity> getTaskInfoByVersion(TaskInfoEntity taskInfoEntity, int offset, Integer pageSize) {

        //List<TaskInfoEntity>  taskInfoEntities = taskInfoMapper.getTaskInfoByVersion(taskInfoEntity,offset,pageSize);
        return null;
    }

    @Override
    public List<TaskInfoEntity> getToDoTaskInfo(int offset, Integer pageSize, HttpServletRequest request, TaskInfoEntity taskInfoEntity) {

        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        List<TaskInfoEntity> taskInfos = taskInfoMapper.getTaskInfoByUserName(userName, offset, pageSize, taskInfoEntity);
        for (TaskInfoEntity taskInfo : taskInfos){
             EntryClassify entryClassify = entryClassifyMapper.getEntryClassfyById(taskInfo.getProductId());
             if (Objects.nonNull(entryClassify)){
                 EntryClassify parentClassfy = entryClassifyMapper.getEntryClassfyById(entryClassify.getParentId());
                 if (Objects.nonNull(parentClassfy)){
                     taskInfo.setClassifyName(parentClassfy.getTitle());
                 }
             }

        }

        return taskInfos;
    }

    @Override
    public int getToDoTaskInfoTotal(HttpServletRequest request, TaskInfoEntity taskInfoEntity) {
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        int taskInfoSum = taskInfoMapper.getToDoTaskInfoTotal(userName, taskInfoEntity);

        return taskInfoSum;
    }

    @Override
    public List<TaskInfoEntity> getFinishTaskInfo(int offset, Integer pageSize, HttpServletRequest request, TaskInfoEntity taskInfoEntity) {
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
            List<TaskInfoEntity> taskInfos = taskInfoMapper.getFinishTaskInfo(userName, offset, pageSize, taskInfoEntity);
        for (TaskInfoEntity taskInfo : taskInfos){
            EntryClassify entryClassify = entryClassifyMapper.getEntryClassfyById(taskInfo.getProductId());
            if (Objects.nonNull(entryClassify)){
                EntryClassify parentClassfy = entryClassifyMapper.getEntryClassfyById(entryClassify.getParentId());
                if (Objects.nonNull(parentClassfy)){
                    taskInfo.setClassifyName(parentClassfy.getTitle());
                }
            }

        }
        return taskInfos;
    }

    @Override
    public int getFinishTaskInfoTotal(HttpServletRequest request, TaskInfoEntity taskInfoEntity) {
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        int taskInfoSum = taskInfoMapper.getFinishTaskInfoTotal(userName, taskInfoEntity);

        return taskInfoSum;
    }


    @Override
    public void taskEntryExport(String taskID, HttpServletResponse response,String importType) {




        TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(taskID);
         VersionEntity versionEntity = versionMapper.getVersionByID(taskInfoEntity.getVersionId());


        String tableName = versionEntity.getTableName();


        List<EntryInfoEntity> entryInfoEntities = entryInfoMapper.getEntryByTaskID(taskID,tableName);
        List<EntryInfoEntity> entryInfoEntities1 = new ArrayList<>();
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities){
            if ("".equals(importType)){
                // 当传入的importType为空时  导出全部词条
                entryInfoEntities1.add(entryInfoEntity);
            }else {
                // 否则 根据传入的importType筛选词条
                if (importType.equals(entryInfoEntity.getImportType())){
                    entryInfoEntities1.add(entryInfoEntity);
                }
            }


        }

            Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        String da = format.format(date);
        String excelName = taskInfoEntity.getTranslateType() + ConstantInterface.UNDERLINE +
                versionEntity.getProductName() + ConstantInterface.UNDERLINE + versionEntity.getName() + ConstantInterface.UNDERLINE + taskInfoEntity.getName()+ ConstantInterface.UNDERLINE + da;
        log.info(" **** excelName is : " + excelName + " **** ");

        String fileName = excelName + ".xls";

        try {
            fileName = URLEncoder.encode(fileName, "UTF-8");
            log.warn( " **** fileName : " + fileName + " ***** ");
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.addHeader("Pargam", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            response.setStatus(200);


        } catch (Exception e) {
            log.error("代码生成出错", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.sendError(500, "代码生成出错，无法下载");
            } catch (IOException ex) {
                log.error("响应报错信息出错", e);
            }
        }
        try {
            Workbook workbook = excelUtil.outPutExcel(entryInfoEntities1, taskInfoEntity.getTranslateType(), fileName);
            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            outputStream.close();
            workbook.close();
        } catch (Exception e) {
            log.error(" ==== excel write error : {} ! ===", e.getMessage());
        }


    }

    @Override
    public void exportEntryByTaskId(String taskId, HttpServletResponse response) {
        // 获取任务
        TaskInfoEntity task = taskInfoMapper.selectById(taskId);
        // 获取该任务的产品版本
        VersionEntity versionEntity = versionMapper.getVersionByID(task.getVersionId());

        List<EntryInfoEntity> entryInfoEntityList = new ArrayList<>();
        List<EntryTempEntity> entryTempEntityList = new ArrayList<>();
        // 判断任务是否已经结束
        if (ConstantInterface.END_STATE.equals(task.getState())){
            // 任务已结束  从 t_version_xxxxxx 表中查询词条数据
            String tableName = versionEntity.getTableName();
            entryInfoEntityList = entryInfoMapper.getEntryByTaskID(taskId,tableName);
        }else {
            // 任务未结束 从 t_entry_temp 表中查询词条数据
            entryTempEntityList = entryTempService.getEntryTempByTaskID(taskId);
        }

        // 获取当前日期
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        String currentDate = format.format(date);

        // 生成导出文件名称
        String excelName = task.getTranslateType() + ConstantInterface.UNDERLINE + versionEntity.getProductName() + ConstantInterface.UNDERLINE +
                            versionEntity.getName() + ConstantInterface.UNDERLINE + task.getName()+ ConstantInterface.UNDERLINE + currentDate;
        log.info(" **** excelName is : " + excelName + " **** ");
        String fileName = excelName + ".xls";

        try {
            fileName = URLEncoder.encode(fileName, "UTF-8");
            log.warn( " **** fileName : " + fileName + " ***** ");
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.addHeader("Pargam", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            response.setStatus(200);

        } catch (Exception e) {
            log.error(" exportEntryByTaskId 代码生成出错", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.sendError(500, "代码生成出错，无法下载");
            } catch (IOException ex) {
                log.error("响应报错信息出错", e);
            }
        }

        try {
            // 生成excel文件
            Workbook workbook = null;
            if (ConstantInterface.END_STATE.equals(task.getState())){
                workbook = excelUtil.outPutExcel(entryInfoEntityList, task.getTranslateType(), fileName);
            }else {
                workbook = excelUtil.exportTempEntryUtil(entryTempEntityList,fileName,versionEntity.getName());
            }
            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            outputStream.close();
            workbook.close();
        } catch (Exception e) {
            log.error(" ==== exportEntryByTaskId excel write error : {} ! ===", e.getMessage());
        }


    }

    @Override
    public String putTempToProductTable(List<EntryTempEntity> entryTempEntities) {

        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();


        //写入翻译表，写入版本表 删除临时表
        int insert = 0;

        
        insert += buildTranslateEntity(entryTempEntities, entryInfoEntities);

        int delete = deleteTempEntry(entryTempEntities);
        if (insert == delete) {
            return ConstantInterface.OK_STR;
        }
        return ErrorCodeList.UPDATE_ERROR;

    }

    private int deleteTempEntry(List<EntryTempEntity> tempEntities) {
        int delete = entryTempMapper.deleteByTaskID(tempEntities.get(0).getTaskId());
        return delete;
    }
    //1.遍历 entryTempEntities
    //2.如果词条有翻译id 则代表是公共词条 ，没有则创建翻译，插入翻译表
    //3.检查词条实体是否存在儿子，如果存在则让父子翻译id相同
    //4.插入词条
    public int buildTranslateEntity(List<EntryTempEntity> entryTempEntities, List<EntryInfoEntity> entryInfoEntities) {
        int insert = 0;
        for (EntryTempEntity entryTempEntity : entryTempEntities) {
            String transID = commonUtils.getUUID();
            EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
            entryInfoEntity.setId(entryTempEntity.getId());
            TranslateEntity translateEntity = new TranslateEntity();

            //翻译id是空 则不是公共词条库的词条，需新增翻译
            if (StringUtils.isBlank(entryTempEntity.getTranslateID())){
                translateEntity.setId(transID);
                translateEntity.setTranslate(entryTempEntity.getTranslate());
                translateEntity.setTranslateState("3");
                translateEntity.setEntry(entryTempEntity.getEntry());
                translateEntity.setType(entryTempEntity.getTranslateType());
                translateEntity.setVisualRange("部门");
                translateEntity.setDeleteState(0);
                translateEntity.setVersionID(entryTempEntity.getVersionID());

                if (StringUtils.isNotBlank(entryTempEntity.getTranslate())){
                    translateMapper.insert(translateEntity);
                }

                entryInfoService.addTransID(translateEntity, entryInfoEntity);
            }else {
                translateEntity.setId(entryTempEntity.getTranslateID());
                translateEntity.setType(entryTempEntity.getTranslateType());
            }

            //如果孩子不为空，则新建词条插入
            if (!CollectionUtils.isEmpty(entryTempEntity.getChildren())){
                for (EntryTempEntity childTempEntry : entryTempEntities){
                    EntryInfoEntity entryInfoEntity1 = new EntryInfoEntity();
                    childTempEntry.setId(childTempEntry.getId());
                    entryInfoService.addTransID(translateEntity, entryInfoEntity1);

                    insertTempToVersionEntry(childTempEntry,entryInfoEntity1);
                    entryInfoEntities.add(entryInfoEntity1);
                }
            }
            insert += insertTempToVersionEntry(entryTempEntity,entryInfoEntity);
            entryInfoEntities.add(entryInfoEntity);

        }
        return insert;
    }

    private int insertTempToVersionEntry(EntryTempEntity entryTempEntity, EntryInfoEntity entryInfoEntity) {
        int insert = 0;
        int isUpdate = entryTempEntity.getIsUpdate();
        if (1 == isUpdate){
            insert = entryInfoMapper.updateEntryInfo(entryInfoEntity);
        }else if  (0 == isUpdate){
            //写版本表
            entryInfoEntity.setEntry(entryTempEntity.getEntry());
            entryInfoEntity.setVersionID(entryTempEntity.getVersionID());
            entryInfoEntity.setIsPublic(0);
            entryInfoEntity.setTaskId(entryTempEntity.getTaskId());
            entryInfoEntity.setEntrySource(entryTempEntity.getSource());
            entryInfoEntity.setAbbr(entryTempEntity.getAbbr());
            entryInfoEntity.setIsDelete(0);
            entryInfoEntity.setImportType(entryTempEntity.getImportype());
            entryInfoEntity.setWriteType(entryTempEntity.getWriteype());
            entryInfoEntity.setEntryState(2);
            String versionID = entryTempEntity.getVersionID();
            VersionEntity versionEntity = versionMapper.selectById(versionID);
            String tableName = versionEntity.getTableName();
            insert += entryInfoMapper.insertEntry(entryInfoEntity,tableName);
            //entryInfoMapper.insert(entryInfoEntity);

        }
        return insert;
    }

    @Override
    public String taskCreateNewLanguageTask(TaskInfoEntity taskInfoEntity, String taskID) {
        String newId = commonUtils.getUUID();
        taskInfoEntity.setId(newId);
        // 新增复制的任务
        int insert = taskInfoMapper.insert(taskInfoEntity);
        // 获取被复制的任务
        TaskInfoEntity CopiedTaks = taskInfoMapper.selectById(taskID);
        List<EntryInfoEntity> entryInfoEntities ;
        List<VersionTableEntity> versionInfoByVersion = versionTableMapper.getVersionInfoByVersionID(CopiedTaks.getVersionId());
        String tableName = "t_entry_info";
        entryInfoEntities = entryInfoMapper.getEntryByTaskID(taskID,tableName);
        entryTempService.insertEntry(entryInfoEntities);
        return taskInfoEntity.getId();
    }

    @Override
    public Map<String, String> getImportType(String taskID) {
        List<EntryTempEntity> entryTempEntities = entryTempMapper.getEntryTempByTaskID(taskID);
        //key:type , value :source
        Map<String, String> typeMap = new HashMap<>();
        for (EntryTempEntity entryTempEntity : entryTempEntities) {
            typeMap.put(entryTempEntity.getImportype(), entryTempEntity.getSource());
        }

        return typeMap;
    }



    public List<EntryTempEntity> buildRepeTempEntry(List<EntryTempEntity> entryTempEntities) {
        List<EntryTempEntity> newTempEntry = new ArrayList<>();
        //entry_translate,entryTempEntity
        Map<String, EntryTempEntity> entryTempEntityMap = new HashMap<>();
        for (EntryTempEntity entryTempEntity : entryTempEntities) {
            String entry = entryTempEntity.getEntry();
            String translate = "";
            //有翻译字段 直接放到map里

            translate = entryTempEntity.getTranslate();
            EntryTempEntity mapValueEntry = entryTempEntityMap.get(entry + ConstantInterface.UNDERLINE + translate);
            //判断map 是否有这个key
            if (Objects.nonNull(mapValueEntry)) {
                entryTempEntity.setParentID(mapValueEntry.getId());

                if (CollectionUtils.isEmpty(mapValueEntry.getChildren())) {
                    List<EntryTempEntity> entryTempEntities1 = new ArrayList<>();
                    entryTempEntities1.add(entryTempEntity);
                    mapValueEntry.setChildren(entryTempEntities1);
                } else {
                    mapValueEntry.getChildren().add(entryTempEntity);
                }

            } else {
                entryTempEntity.setParentID("");
                entryTempEntityMap.put(entry + ConstantInterface.UNDERLINE + translate, entryTempEntity);
            }
        }


        Collection<EntryTempEntity> values = entryTempEntityMap.values();
        Iterator<EntryTempEntity> iterator = values.iterator();
        while (iterator.hasNext()) {
            newTempEntry.add(iterator.next());
        }
        return newTempEntry;
    }
}




