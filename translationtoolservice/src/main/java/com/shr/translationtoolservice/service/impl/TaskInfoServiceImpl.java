package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.dao.ProductMapper;
import com.shr.translationtoolservice.dao.VersionMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.TaskInfoVo;
import com.shr.translationtoolservice.service.TaskInfoService;
import com.shr.translationtoolservice.dao.TaskInfoMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Service
public class TaskInfoServiceImpl extends ServiceImpl<TaskInfoMapper, TaskInfoEntity>
    implements TaskInfoService{


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

    @Override
    //获取任务信息
    //入参 taskInfoEntity 任务实体 ， offset 页码，pageSize 页内行数
    public List<TaskInfoEntity> getTaskInfo( TaskInfoEntity taskInfoEntity, Integer offset, Integer pageSize,HttpServletRequest request) {

        List<TaskInfoEntity> taskInfoEntities = taskInfoMapper.getTaskInfo(taskInfoEntity,offset,pageSize);
        if (StringUtils.isNotBlank(taskInfoEntity.getTableName())){
            for (TaskInfoEntity taskInfoEntity1 : taskInfoEntities){
                List<EntryInfoEntity> entryInfoEntities =  entryInfoMapper.getEntryByTaskID(taskInfoEntity1.getId(),taskInfoEntity.getTableName());
                taskInfoEntity1.setEntryNum(entryInfoEntities.size());
            }
        }

        return taskInfoEntities;
    }

    @Override
    public int getTotalNum( TaskInfoEntity taskInfoEntity) {
        int total = taskInfoMapper.getTaskInfoTotal(taskInfoEntity);
        return total;
    }

    @Override
    public String addTaskInfoList(List<TaskInfoVo> taskInfoVoList, HttpServletRequest request) {

        String token = request.getHeader("token");

        for (TaskInfoVo taskInfoVo : taskInfoVoList){
            TaskInfoEntity taskInfoEntity = new TaskInfoEntity();
            BeanUtils.copyProperties(taskInfoVo,taskInfoEntity);
            String id = commonUtils.getUUID();
            taskInfoEntity.setId(id);
            //创建人
            String userName = JWTTokenUtils.getUserName(token);
            if (StringUtils.isBlank(taskInfoEntity.getCreator())){
                taskInfoEntity.setCreator(userName);
            }
            //创建部门
            String department =JWTTokenUtils.getDepartment(token);
            if (StringUtils.isBlank(taskInfoEntity.getDepartment())){
                taskInfoEntity.setDepartment(department);
            }
            //创建时间
            if (Objects.isNull(taskInfoEntity.getCreateTime())){
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
        BeanUtils.copyProperties(taskInfoVo,taskInfoEntity);
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

        for (String taskId : taskIDs){
            TaskInfoEntity taskInfoEntity = new TaskInfoEntity();
            taskInfoEntity.setId(taskId);
            taskInfoEntity.setState(nextState);
            //根据状态更新操作时间
            if (ConstantInterface.DELIVERY_STATE.equals(oldState)){
                taskInfoEntity.setDeliveryTime(new Date(System.currentTimeMillis()));
            }else  if (ConstantInterface.IMPORT_STATE.equals(oldState)){
                taskInfoEntity.setImportTime(new Date(System.currentTimeMillis()));
            }else  if (ConstantInterface.ENTRY_AUDIT_STATE.equals(oldState)){
                taskInfoEntity.setEntryAutiorStartTime(new Date(System.currentTimeMillis()));
            }else  if (ConstantInterface.TRANSLATE_STATE.equals(oldState)){
                taskInfoEntity.setTranslateStartTime(new Date(System.currentTimeMillis()));
            }else  if (ConstantInterface.TRANSLATE_AUDIT_STATE.equals(oldState)){
                taskInfoEntity.setTranslationAuditorStartTime(new Date(System.currentTimeMillis()));
            }else  if (ConstantInterface.EXPORT_STATE.equals(oldState)){
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

        List<TaskInfoEntity>  taskInfoEntities = taskInfoMapper.getTaskInfoByVersion(taskInfoEntity,offset,pageSize);
        return null;
    }
}




