package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.dao.VersionMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.shr.translationtoolservice.entity.VersionEntity;
import com.shr.translationtoolservice.service.TaskInfoService;
import com.shr.translationtoolservice.dao.TaskInfoMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
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

    @Override
    //获取任务信息
    //入参 taskInfoEntity 任务实体 ， offset 页码，pageSize 页内行数
    public List<TaskInfoEntity> getTaskInfo(TaskInfoEntity taskInfoEntity, Integer offset, Integer pageSize,HttpServletRequest request) {

        List<TaskInfoEntity> taskInfoEntities = taskInfoMapper.getTaskInfo(taskInfoEntity,offset,pageSize);

  /*
        List<VersionEntity> versionEntities = versionMapper.selectByName( taskInfoEntity.getVersion());
     //没有版本 新建版本
        if (CollectionUtils.isEmpty(versionEntities)){
            String token = request.getHeader("token");
            String userName = JWTTokenUtils.getUserName(token);

            VersionEntity versionEntity = new VersionEntity();
            versionEntity.setName(taskInfoEntity.getVersion());
            versionEntity.setCreator(userName);
            versionEntity.setCreateTime(new Date(System.currentTimeMillis()));
            versionMapper.insert()
        }*/
        return taskInfoEntities;
    }

    @Override
    public int getTotalNum() {
        int total = taskInfoMapper.selectCount(new QueryWrapper<>());
        return total;
    }

    @Override
    public String addTaskInfoList(List<TaskInfoEntity> taskInfoEntities, HttpServletRequest request) {

        String token = request.getHeader("token");

        for (TaskInfoEntity taskInfoEntity : taskInfoEntities){
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
                taskInfoEntity.setCreator(department);
            }
            //创建时间
            if (Objects.isNull(taskInfoEntity.getCreateTime())){
                Date date = new Date(System.currentTimeMillis());
                taskInfoEntity.setCreateTime(date);
            }
            //状态更新导入状态
            taskInfoEntity.setState(0);
            int insert = taskInfoMapper.insert(taskInfoEntity);
            if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.INSERT_ERROR;
            }
        }


        return ConstantInterface.OK_STR;
    }




    @Override
    public String updateTaskInfo(TaskInfoEntity taskInfoEntity) {


        int update = taskInfoMapper.updateById(taskInfoEntity);
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }

        return ConstantInterface.OK_STR;
    }

    @Override
    public String deleteTaskInfo(List<String> taskIds) {
        int delete = taskInfoMapper.deleteBatchIds(taskIds);
        if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String taskSubmission(List<String> taskIDs, int oldState, int nextState) {

        for (String taskId : taskIDs){
            TaskInfoEntity taskInfoEntity = new TaskInfoEntity();
            taskInfoEntity.setId(taskId);
            taskInfoEntity.setState(nextState);
            //根据状态更新操作时间
            if (ConstantInterface.DELIVERY_STATE == oldState){
                taskInfoEntity.setDeliveryTime(new Date(System.currentTimeMillis()));
            }else  if (ConstantInterface.IMPORT_STATE == oldState){
                taskInfoEntity.setImportTime(new Date(System.currentTimeMillis()));
            }else  if (ConstantInterface.ENTRY_AUDIT_STATE == oldState){
                taskInfoEntity.setEntryAutiorStartTime(new Date(System.currentTimeMillis()));
            }else  if (ConstantInterface.TRANSLATE_STATE == oldState){
                taskInfoEntity.setTranslateStartTime(new Date(System.currentTimeMillis()));
            }else  if (ConstantInterface.TRANSLATE_AUDIT_STATE == oldState){
                taskInfoEntity.setTranslationAuditorStartTime(new Date(System.currentTimeMillis()));
            }else  if (ConstantInterface.EXPORT_STATE == oldState){
                taskInfoEntity.setEndTime(new Date(System.currentTimeMillis()));
            }
            int update = taskInfoMapper.insert(taskInfoEntity);
            if (update != ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.UPDATE_ERROR;
            }

        }
        return ConstantInterface.OK_STR;
    }
}




