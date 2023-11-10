package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.VersionEntity;
import com.shr.translationtoolservice.service.VersionService;
import com.shr.translationtoolservice.dao.VersionMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.Action;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Service
public class VersionServiceImpl extends ServiceImpl<VersionMapper, VersionEntity>
    implements VersionService{

    @Autowired
    private VersionMapper versionMapper;
    @Autowired
    private CommonUtils commonUtils;



    @Override
    public List<VersionEntity> getVersion(VersionEntity versionEntity) {
        List<VersionEntity> versionEntities = versionMapper.getVersion(versionEntity);
        return versionEntities;
    }

    @Override
    public int getVersionTotal(VersionEntity versionEntity) {
        return  versionMapper.getVersionTotal(versionEntity);
    }

    @Override
    public String createVersion(VersionEntity versionEntity, HttpServletRequest request) {
        String id = commonUtils.getUUID();
        versionEntity.setId(id);
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        versionEntity.setCreator(userName);
        versionEntity.setCreateTime(new Date(System.currentTimeMillis()));
        versionEntity.setIsDelete(versionEntity.getIsDelete());
        int insert = versionMapper.insert(versionEntity);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.INSERT_ERROR;
        }

        return id;
    }

    @Override
    public String updateVersion(VersionEntity versionEntity) {
        int update = versionMapper.updateById(versionEntity);
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String deleteVersion(List<String> idList) {
        int delete = versionMapper.deleteByIds(idList);
        if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }
}




