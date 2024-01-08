package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryCommonEntity;
import com.shr.translationtoolservice.entity.EntryTempEntity;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.service.EntryTempService;
import com.shr.translationtoolservice.dao.EntryTempMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.ws.Action;
import java.util.List;

/**
 *
 */
@Service
public class EntryTempServiceImpl extends ServiceImpl<EntryTempMapper, EntryTempEntity>
    implements EntryTempService{

    @Autowired
    private EntryTempMapper entryTempMapper;

    @Autowired
    private EntryInfoMapper entryInfoMapper;

    @Override
    public String insertEntry(List<EntryTempEntity> tempEntities) {
        int insert = 0;
        for (EntryTempEntity entryTempEntity : tempEntities){
            insert +=  entryTempMapper.insert(entryTempEntity);
        }
        if (insert < tempEntities.size()) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;

    }

    @Override
    public String updateEntryTemp(List<EntryTempEntity> tempEntities) {
        int update = 0 ;
        for (EntryTempEntity entryTempEntity : tempEntities){
            update += entryTempMapper.updateById(entryTempEntity);
        }
        if (update < tempEntities.size()) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public List<EntryTempEntity> getEntryTempByTaskID(String taskID, int offset, Integer pageSize) {
        List<EntryTempEntity> entryTempEntities = entryTempMapper.getEntryTempByTaskID(taskID,offset,pageSize);
        return entryTempEntities;
    }

    @Override
    public String deleteEntryTempByID(List<String> entryID) {
        int delete = entryTempMapper.deleteBatchIds(entryID);
        return ConstantInterface.OK_STR;
    }

    @Override
    public int getEntryTempByTaskIDTotal(String taskID) {

        return entryTempMapper.getEntryTempByTaskIDTotal(taskID);
    }


}




