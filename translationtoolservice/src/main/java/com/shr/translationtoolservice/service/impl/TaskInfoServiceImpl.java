package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.TaskImportEntryEntity.TaskImportEntryVO;
import com.shr.translationtoolservice.entity.TaskInfoEntity.TaskInfoEntityVO;
import com.shr.translationtoolservice.entity.DO.TaskStateEntityDO;
import com.shr.translationtoolservice.entity.vo.TaskInfoVo;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.service.EntryTempService;
import com.shr.translationtoolservice.service.TLanguageService;
import com.shr.translationtoolservice.service.TaskInfoService;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler.BatchCreateEntryResult;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.ExcelUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import com.shr.translationtoolservice.util.task.BackendTaskInfoHandler;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    private TLanguageService languageService;

    @Autowired
    private EntryClassifyMapper entryClassifyMapper;

    @Autowired
    private ProductRelationMapper productRelationMapper;

    @Autowired
    private BatchInsertEntryHandler batchInsertEntryHandler;

    @Autowired
    private BackendTaskInfoHandler backendTaskInfoHandler;

    private Map<String,String> fileNamePrefixMap = new HashMap<>();
    {
        fileNamePrefixMap.put("db", "db/");
        fileNamePrefixMap.put("meta", "db/meta/");
        fileNamePrefixMap.put("config", "config/");
        fileNamePrefixMap.put("enum", "enum/");
        fileNamePrefixMap.put("tr", "tr/");
        fileNamePrefixMap.put("pt", "pt/");
        fileNamePrefixMap.put("jk", "jk/");
    }

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

    @Transactional
    @Override
    public List<TaskInfoEntity> addTaskInfoListAndSubmit(List<TaskInfoVo> taskInfoVoList,String token){

        List<String> taskIDs = new ArrayList<>();
        List<TaskInfoEntity> taskInfoEntities = new ArrayList<>();
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
                return taskInfoEntities;
            }
            taskInfoEntities.add(taskInfoEntity);
            taskIDs.add(id);
            
        }
        this.taskSubmission(taskIDs, "0","1");

        return taskInfoEntities;
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
    public List<TaskInfoEntityVO> getToDoTaskInfo(int offset, Integer pageSize, HttpServletRequest request, TaskInfoEntity taskInfoEntity) {

        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        List<TaskInfoEntity> taskInfos = taskInfoMapper.getTaskInfoByUserName(userName, offset, pageSize, taskInfoEntity);
        List<TaskInfoEntityVO> taskInfoVOs = new ArrayList<>();
        for (TaskInfoEntity taskInfo : taskInfos){
            TaskInfoEntityVO taskInfoEntityVO = new TaskInfoEntityVO();
            BeanUtils.copyProperties(taskInfo,taskInfoEntityVO);
            EntryClassify entryClassify = entryClassifyMapper.getEntryClassfyById(taskInfo.getProductId());
            if (Objects.nonNull(entryClassify)){
                EntryClassify parentClassfy = entryClassifyMapper.getEntryClassfyById(entryClassify.getParentId());
                if (Objects.nonNull(parentClassfy)){
                    taskInfo.setClassifyName(parentClassfy.getTitle());
                }
        
                taskInfoEntityVO.setCodeBranch(entryClassify.getCodeBranch());
            }
            taskInfoVOs.add(taskInfoEntityVO);
        }

        return taskInfoVOs;
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
                    // translateMapper.insert(translateEntity);
                    translateMapper.insertTranslate(translateEntity);
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
        String tableName = "t_entry_info";
        QueryWrapper<ProductRelationEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id",CopiedTaks.getId());
        //获取被复制的任务的词条关系
        List<ProductRelationEntity> productRelationEntities = productRelationMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(productRelationEntities)){
            return ErrorCodeList.UPDATE_ERROR;
        }
        //插入新任务的词条关系
        for (ProductRelationEntity productRelationEntity : productRelationEntities){
            String entryId = productRelationEntity.getEntryId();
            ProductRelationEntity productRelationEntity1 = new ProductRelationEntity();
            productRelationEntity1.setId(commonUtils.getUUID());
            productRelationEntity1.setEntryId(entryId);
            productRelationEntity1.setTaskId(newId);
            productRelationEntity1.setVersionId(taskInfoEntity.getVersionId());
            productRelationEntity1.setProductId(taskInfoEntity.getProductId());
            productRelationMapper.insert(productRelationEntity1);
        }
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

    /**
     * 提供的tr文件列表是[tr/xxx,enum/xxx,db/xxx,....]
     * @param taskLangDirMap key: 任务,value: config,db,enum,meta,ts,dic
     * @param trFileList
     */
    private List<String> dispathTRFilesForTask(TaskInfoEntity taskInfoEntity,Map<String,String> taskLangDirMap,List<String> trFileList){
        String taskName = taskInfoEntity.getName();
        String searchFileType = taskLangDirMap.get(taskName);   // config,db,enum,meta,ts,dic
        if(searchFileType == null){
            log.warn("警告，无法为任务指派要导入的文件的词条类型");
            return null;
        }
        String targetFileNamePrefix = this.fileNamePrefixMap.get(searchFileType);
        if(targetFileNamePrefix == null){
            log.warn(String.format("警告: 没有找到对应词条类型: %s 的文件名前缀",searchFileType));
            return null;
        }
        log.debug(String.format("任务: %s将要获取文件类型为: %s的词条",taskName,searchFileType));
        return trFileList.stream().filter(new Predicate<String>() {

            @Override
            public boolean test(String t) {
                if(targetFileNamePrefix.equals("db/")){
                    return t.startsWith(targetFileNamePrefix) && !t.startsWith("db/meta");
                }else{
                    return t.startsWith(targetFileNamePrefix);
                }
                
            }
            
        }).collect(Collectors.toList());
    }

    /**
     * 根据预计提供给任务要导入的词条文件来源，结合获取到的ts文件列表，确定任务最终要导入的词条的文件来源
     * @param taskInfoEntity
     * @param taskFileNamePrefixMap
     * @param tsFileList
     * @return
     */
    private List<String> dispatchTSFilesForTask(TaskInfoEntity taskInfoEntity,Map<String,Set<String>> taskFileNamePrefixMap,Set<String> tsFileList){

        String taskName = taskInfoEntity.getName();
        Set<String> fileNamePrefix = taskFileNamePrefixMap.get(taskName);   // gui_i18n_tool,gui_manager
        if(fileNamePrefix == null){
            return null;
        }
        List<TLanguage> tLanguages = languageService.getLanguages(new TLanguage());

        return tsFileList.stream().filter(new Predicate<String>() {

            @Override
            public boolean test(String fileName) {
                // TODO Auto-generated method stub
                for (TLanguage tLanguage : tLanguages) {
                    if (fileName.contains(tLanguage.getCode())) {
                        return fileNamePrefix.contains(fileName.substring(0, fileName.indexOf("_" + tLanguage.getCode())));
                    }
                }
                return false;
                
            }
            
        }).collect(Collectors.toList());

    }

    @Transactional
    @Override
    public String createTaskAndCreateEntryByLangDir(
        String i18nAddress,
        List<TaskInfoVo> taskInfoVos,
        Map<String,String> taskDirMap,
        String token,
        List<String> targetLanguageTypes,
        String backendTaskID,
        Map<String,Object> otherArgs
    ){
        /* 创建任务成功，到词条导入失败，任务不能提交 */
        List<TaskInfoEntity> taskInfoEntities = this.addTaskInfoListAndSubmit(taskInfoVos, token);
        if(taskInfoEntities.size() != taskInfoVos.size()){
            throw new RuntimeException("创建任务时存在异常");   // 抛异常直接回滚
        }
        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("任务创建成功, 共%s个任务", taskInfoEntities.size()));
        return this.createEntryByLangDirForTaskInfos(i18nAddress, taskInfoEntities, taskDirMap, token, targetLanguageTypes,backendTaskID,otherArgs);   // 抛异常直接回滚
    }

    @Transactional
    @Override
    public String createEntryByLangDirForTaskInfos(String i18nAddress,List<TaskInfoEntity> taskInfoEntities,Map<String,String> taskDirMap,String token,List<String> targetLanguageTypes,String backendTaskID,Map<String,Object> otherArgs) {
        // TODO Auto-generated method stub
        // backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "");
        /* 分类出TS和TR类任务 */
        List<TaskInfoEntity> dicTaskInfoList = taskInfoEntities.stream().filter(new Predicate<TaskInfoEntity>() {

            @Override
            public boolean test(TaskInfoEntity t) {
                // TODO Auto-generated method stub
                String taskName = t.getName();
                if(taskName == null || !taskDirMap.containsKey(taskName)){
                    return false;
                }
                return !taskDirMap.get(taskName).equals("ts");
            }
            
        }).collect(Collectors.toList());

        List<TaskInfoEntity> tsTaskInfoList = taskInfoEntities.stream().filter(new Predicate<TaskInfoEntity>() {

            @Override
            public boolean test(TaskInfoEntity t) {
                // TODO Auto-generated method stub
                String taskName = t.getName();
                if(taskName == null || !taskDirMap.containsKey(taskName)){
                    return false;
                }
                return taskDirMap.get(taskName).equals("ts");
            }
            
        }).collect(Collectors.toList());

        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        /* dic的词条 */
        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "准备获取dic文件列表");
        List<String> trFileListUsingI18nServer = batchInsertEntryHandler.getTRFileListUsingI18nServer(i18nAddress, department);
        if(trFileListUsingI18nServer == null){
            throw new RuntimeException("获取dic文件列表时出现异常,无法导入dic的词条");
        }
        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("dic文件列表获取成功, 共%s个", String.valueOf(trFileListUsingI18nServer.size())));
        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "准备获取ts文件列表");
        Set<String> tsFileListUsingI18nServer = batchInsertEntryHandler.getTSFileListUsingI18nServer(i18nAddress, targetLanguageTypes);
        if(tsFileListUsingI18nServer == null){
            throw new RuntimeException("获取ts文件列表时出现异常,无法导入ts的词条");
        }
        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, String.format("ts文件列表获取成功, 共%s个", String.valueOf(trFileListUsingI18nServer.size())));

        Map<TaskInfoEntity,List<String>> taskTRFilesMap = new HashMap<>();  // {{"taskName" : ["tr/xxx","db/xxxx"]},{"taskName2" : ["tr/xxx","db/xxxx",'config/xxx']}}
        Map<TaskInfoEntity,List<String>> taskTSFilesMap = new HashMap<>();  // {{"taskName": ["gui_i18n_tool","fcwwewe"]},{"taskName2": ["gui_manager"]}}
        String externalMessage = "";    // 额外的提供给用户的信息
        Set ignoredFileList = (Set)(otherArgs.get("ignore_files"));     
        if(!trFileListUsingI18nServer.isEmpty()){
            for(TaskInfoEntity taskInfoEntity : dicTaskInfoList){
                List<String> dispathTRFilesForTask = this.dispathTRFilesForTask(taskInfoEntity, taskDirMap, trFileListUsingI18nServer);
                if(dispathTRFilesForTask == null){
                    continue;
                }
                dispathTRFilesForTask = dispathTRFilesForTask.stream()
                    .filter(new Predicate<String>() {

                        @Override
                        public boolean test(String t) {
                            // TODO Auto-generated method stub
                            return !ignoredFileList.contains(t);
                        }
                        
                    })
                    .collect(Collectors.toList());
                taskTRFilesMap.put(taskInfoEntity, dispathTRFilesForTask);
            }
            Set<String> noEntryInsertedFileNames = new HashSet<>();
            for(Map.Entry<TaskInfoEntity,List<String>> taskTRFileEntry : taskTRFilesMap.entrySet()){
                /* 每一个任务获取对应的词条 */
                BatchCreateEntryResult createEntryResult = batchInsertEntryHandler.createEntryFromTRFiles(i18nAddress, taskTRFileEntry.getValue(),taskTRFileEntry.getKey(),targetLanguageTypes,token,backendTaskID);
                noEntryInsertedFileNames.addAll(createEntryResult.getFileNameForNoEntryInserted());
                String failedEntryInfoMessages = "";
                if(!createEntryResult.getFailedEntryInfoEntities().isEmpty()){
                    /* 处理导入失败的词条 */
                    List<EntryInfoEntity> failedEntryInfoEntities = createEntryResult.getFailedEntryInfoEntities();
                    for(EntryInfoEntity entryInfoEntity : failedEntryInfoEntities){
                        String _message = String.format("词条: entry: %s, tag: %s,comment: %s ,文件名: %s导入失败;", entryInfoEntity.getEntry(),entryInfoEntity.getTag(),entryInfoEntity.getComment(),entryInfoEntity.getEntrySource());
                        backendTaskInfoHandler.addMessageForTaskID(
                            backendTaskID,
                            _message
                        );
                        failedEntryInfoMessages += _message;
                    }
                }
                externalMessage += failedEntryInfoMessages;
            }
            if(!noEntryInsertedFileNames.isEmpty()){
                String _message = String.format("没有导入任何词条的dic文件(dic系列)有: %s;", noEntryInsertedFileNames.toString());
                backendTaskInfoHandler.addMessageForTaskID(backendTaskID, _message);
                externalMessage += _message;
            }
            
        }else{
            backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "没有找到可以获取到的dic文件,没有导入dic的词条");
            log.info("没有找到可以获取到的dic文件,没有导入dic的词条");
            externalMessage += "没有找到可以获取到的dic文件,没有导入dic的词条;";
        }
        if(!tsFileListUsingI18nServer.isEmpty()){
            List<TLanguage> tLanguages = languageService.getLanguages(new TLanguage());

            Map<String,Set<String>> taskFileNamePrefixMap = new HashMap<>();
            if(!tsTaskInfoList.isEmpty()){
                taskFileNamePrefixMap.put(tsTaskInfoList.get(0).getName(),tsFileListUsingI18nServer.stream().map(new Function<String,String>() {

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
                    
                }).collect(Collectors.toSet()));
            }
            

            for(TaskInfoEntity taskInfoEntity : tsTaskInfoList){
                List<String> dispathTSFilesForTask = this.dispatchTSFilesForTask(taskInfoEntity, taskFileNamePrefixMap, tsFileListUsingI18nServer);
                if(dispathTSFilesForTask == null){
                    continue;
                }
                taskTSFilesMap.put(taskInfoEntity, dispathTSFilesForTask);
            }
            
            Set<String> noEntryInsertedFileNames = new HashSet<>();
            for(Map.Entry<TaskInfoEntity,List<String>> taskTSFileEntry : taskTSFilesMap.entrySet()){
                /* 每一个任务获取对应的词条 */
                Set<String> entrySources = taskTSFileEntry.getValue().stream().map(new Function<String,String>() {

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
                    
                }).collect(Collectors.toSet()); //['gui_i18n_tool','gui_manager']
                Set<String> filteredEntrySources = entrySources.stream().filter(new Predicate<String>() {

                    @Override
                    public boolean test(String t) {
                        // TODO Auto-generated method stub
                        return !ignoredFileList.contains(t);
                    }
                    
                }).collect(Collectors.toSet());

                BatchCreateEntryResult createEntryResult = batchInsertEntryHandler.createEntryFromTSFiles(i18nAddress, new ArrayList<>(filteredEntrySources), taskTSFileEntry.getKey(), targetLanguageTypes, token,backendTaskID); 
                noEntryInsertedFileNames.addAll(createEntryResult.getFileNameForNoEntryInserted());
                String failedEntryInfoMessages = "";
                if(!createEntryResult.getFailedEntryInfoEntities().isEmpty()){
                    List<EntryInfoEntity> failedEntryInfoEntities = createEntryResult.getFailedEntryInfoEntities();
                    for(EntryInfoEntity entryInfoEntity : failedEntryInfoEntities){
                        String _message = String.format("词条: entry: %s, tag: %s,comment: %s ,文件名: %s导入失败", entryInfoEntity.getEntry(),entryInfoEntity.getTag(),entryInfoEntity.getComment(),entryInfoEntity.getEntrySource());
                        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, _message);
                        failedEntryInfoMessages += _message;
                    }
                }
                externalMessage += failedEntryInfoMessages;
            }
            if(!noEntryInsertedFileNames.isEmpty()){
                String _message = String.format("没有导入任何词条的dic文件(dic系列)有: %s", noEntryInsertedFileNames.toString());
                backendTaskInfoHandler.addMessageForTaskID(backendTaskID, _message);
                externalMessage += _message;
            }
        }else{
            backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "没有找到可以获取的ts文件,没有导入ts词条");
            log.info("没有找到可以获取的ts文件,没有导入ts词条");
            externalMessage += "没有找到可以获取的ts文件,没有导入ts词条;";
        }

        backendTaskInfoHandler.addMessageForTaskID(backendTaskID, "词条导入并存库成功, 并以提交到对应的任务中");
        return "创建任务成功,词条创建成功, 并以提交到对应任务中" + externalMessage;
    }




    @Transactional
    @Override
    public String createEntryByLangDirForTaskInfos(
        String i18nAddress,
        Collection<TaskImportEntryVO>  taskImportEntryVOs,
        String token,
        List<String> targetLanguageTypes,
        String backendTaskID
    ){

        // Queue<String> responseMessage -
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 2, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        // CountDownLatch latch = new CountDownLatch(taskImportEntryVOs.size());
        Set<String> errorMessages = new HashSet<>();
        for(TaskImportEntryVO taskImportEntryVO : taskImportEntryVOs){
            List<TaskInfoVo> taskInfoVos = new ArrayList<>();
            taskInfoVos.add(taskImportEntryVO.getTaskInfoVO());
            /* 创建任务并提交 */
            List<TaskInfoEntity> taskInfoEntities = this.addTaskInfoListAndSubmit(taskInfoVos, token);
            if(taskInfoEntities.isEmpty()){
                throw new RuntimeException("任务创建失败: 任务名为: " + taskImportEntryVO.getTaskInfoVO().getName());
            }
            TaskInfoEntity taskInfoEntity = taskInfoEntities.get(0);
            String taskName = taskInfoEntity.getName();
            List<String> fileNames = taskImportEntryVO.getEntrySources();
            if(fileNames == null){
                log.info("任务: " + taskName + "没有要导入的词条文件,仅创建任务");
                continue;   // 只创建任务, 不导入词条
            }
            String fileType = taskImportEntryVO.getFileType();
            executor.execute(new Runnable(){

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        List<String> _targetLanguageTypes = targetLanguageTypes;
                        /* 目标语言: 如果没传参, 用taskVO的，如果有，就用传参的 */
                        if(_targetLanguageTypes == null){
                            _targetLanguageTypes = taskImportEntryVO.getTargetLanguageTypes();
                        }
                        if(fileType.equals("dic")){
                            if(_targetLanguageTypes == null){
                                _targetLanguageTypes = new ArrayList<>();
                            }
                            BatchCreateEntryResult createResult = batchInsertEntryHandler.createEntryFromTRFiles(i18nAddress, fileNames, taskInfoEntity, _targetLanguageTypes, token, backendTaskID);
                            log.info(createResult.getMessage());
                        }else if(fileType.equals("ts")) {
                            if(_targetLanguageTypes == null){
                                log.warn("警告,没有获取到导入的词条对应的文件的语言信息, 无法导入ts文件, 任务名: " + taskName + ",词条来源: " + fileNames.toString());
                                return;
                            }
                            BatchCreateEntryResult entryFromTSFiles = batchInsertEntryHandler.createEntryFromTSFiles(i18nAddress, fileNames, taskInfoEntity, _targetLanguageTypes, token, backendTaskID);
                            log.info(entryFromTSFiles.getMessage());
                        }
                        return;
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        errorMessages.add(String.format("任务名: %s,词条来源: %s ,出现异常,信息为: %s", taskName,fileNames.toString(),e.getMessage()));
                    } finally{
                        // latch.countDown();;
                    }
                }
                
            });
        }
        // try {
            // if(!latch.await(1, TimeUnit.HOURS)){
            //     throw new RuntimeException("指定时间内没有成功导入所有词条, 导入失败");
            // }
        if(!errorMessages.isEmpty()){
            throw new RuntimeException("执行失败, 异常信息为: "+ errorMessages.toString());
        }
        // } catch (InterruptedException e) {
        //     // TODO Auto-generated catch block
        //     log.warn("警告,系统服务异常,线程被interrupted");
        //     throw new RuntimeException("警告,系统服务异常,线程被interrupted");
        // }

        return "任务全部完成";
    }

    @Override
    public List<TaskStateEntity> countEntryTranslateStateForTasks(Set<String> taskIDs) {
        // TODO Auto-generated method stub
        List<TaskStateEntityDO> countEntryTranslateStateForTasks = entryInfoMapper.countEntryTranslateStateForTasks(taskIDs);

        return countEntryTranslateStateForTasks.stream().map(new Function<TaskStateEntityDO,TaskStateEntity>() {

            @Override
            public TaskStateEntity apply(TaskStateEntityDO taskStateEntityDO) {
                // TODO Auto-generated method stub
                TaskStateEntity taskStateEntity = TaskStateEntity.convertFrom(taskStateEntityDO);
    
                return taskStateEntity;
            }
            
        }).collect(Collectors.toList());
    }


}




