package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.dao.ProductRelationMapper;
import com.shr.translationtoolservice.dao.ProductTableMapper;
import com.shr.translationtoolservice.dao.VersionTableMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.VersionService;
import com.shr.translationtoolservice.dao.VersionMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.Action;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Service
@Slf4j
public class VersionServiceImpl extends ServiceImpl<VersionMapper, VersionEntity>
    implements VersionService{

    @Autowired
    private VersionMapper versionMapper;
    @Autowired
    private CommonUtils commonUtils;
    @Autowired
    private VersionTableMapper versionTableMapper;

    @Autowired
    private ProductTableMapper productTableMapper;

    @Autowired
    private ProductRelationMapper productRelationMapper;


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
        versionEntity.setIsDelete(0);
       // ProductTableEntity tableInfoByProductId = productTableMapper.getTableInfoByProductId(versionEntity.getProductId());
       // versionEntity.setTableName(tableInfoByProductId.getEntryInfoTableName());
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
        //查询productRelation 中versionID在idList中的数据
        productRelationMapper.deleteByVersionID(idList);

        return ConstantInterface.OK_STR;
    }

    @Override
    public List<VersionEntity> getVersionByName(String versionName, String productID) {
        QueryWrapper<VersionEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(versionName)){
            queryWrapper.eq("name",versionName);
        }
        queryWrapper.eq("product_id",productID);

        queryWrapper.eq("is_delete",0);
        queryWrapper.orderByAsc("create_time");
         List<VersionEntity> versionEntities = versionMapper.selectList(queryWrapper);

        return versionEntities;
    }
}




