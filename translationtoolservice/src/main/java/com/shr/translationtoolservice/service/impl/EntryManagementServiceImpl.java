package com.shr.translationtoolservice.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.EntryCommonEntityService;
import com.shr.translationtoolservice.service.EntryProductEntityService;
import com.shr.translationtoolservice.service.EntryProjectEntityService;
import com.shr.translationtoolservice.service.EntryManagementService;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.CompareUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @ClassName TermManagementServiceImpl
 * @Description TODO
 * @USER: Cola
 * @Date 2023/6/19 0019 16:58
 **/
@Service
@Slf4j
public class EntryManagementServiceImpl implements EntryManagementService {
    @Autowired
    EntryProductEntityMapper entryProductEntityMapper;
    @Autowired
    EntryProjectEntityMapper entryProjectEntityMapper;
    @Autowired
    EntryCommonEntityMapper entryCommonEntityMapper;
    @Autowired
    EntryProductEntityService entryProductEntityService;
    @Autowired
    EntryCommonEntityService entryCommonEntityService;
    @Autowired
    EntryMapper entryMapper;
    @Autowired
    EntryOperateMapper entryOperateMapper;

    @Autowired
    CommonUtils commonUtils;
    @Autowired
    EntryProjectEntityService entryProjectEntityService;

    @Override
    public ResponseListModel searchEntry(EntryReqEntity entryReqEntity, Integer pageIndex, Integer pageSize) {
        ResponseListModel result = new ResponseListModel<>();
        QueryWrapper<EntryProjectEntity> projectEntityQueryWrapper = new QueryWrapper<EntryProjectEntity>();
        QueryWrapper<EntryProductEntity> productEntityQueryWrapper = new QueryWrapper<>();
        QueryWrapper<EntryCommonEntity> commonEntityQueryWrapper = new QueryWrapper<>();

        if (StringUtils.isBlank(entryReqEntity.getLexicon())) {
            result.setList(getAllEntry(entryReqEntity, pageIndex, pageSize));
            int total = entryCommonEntityMapper.selectCount(commonEntityQueryWrapper)
                    + entryProductEntityMapper.selectCount(productEntityQueryWrapper) + entryProjectEntityMapper.selectCount(projectEntityQueryWrapper);
            result.setTotalNum(total);
            //产品表
        } else if (ConstantInterface.PROJECT_TABLE.equals(entryReqEntity.getLexicon())) {
            result.setList(entryProjectEntityService.searchEntry(entryReqEntity, pageIndex, pageSize));
            result.setTotalNum(entryProjectEntityMapper.selectCount(projectEntityQueryWrapper));
            //工程表
        } else if (ConstantInterface.PRODUCT_TABLE.equals(entryReqEntity.getLexicon())) {
            result.setList(entryProductEntityService.searchEntry(entryReqEntity, pageIndex, pageSize));
            result.setTotalNum(entryProductEntityMapper.selectCount(productEntityQueryWrapper));
            //公共表
        } else if (ConstantInterface.COMMON_TABLE.equals(entryReqEntity.getLexicon())) {
            result.setList(entryCommonEntityService.searchEntry(entryReqEntity, pageIndex, pageSize));
            result.setTotalNum(entryCommonEntityMapper.selectCount(commonEntityQueryWrapper));
        }
        return result;
    }

    //先查project表，不够再查 product ，最后再查comm
    @Override
    public List getAllEntry(EntryReqEntity entryReqEntity, Integer pageIndex, Integer pageSize) {
        List entry = new ArrayList();

        List projectEntities = entryProjectEntityService.searchEntry(entryReqEntity, pageIndex, pageSize);
        if (CollectionUtils.isEmpty(projectEntities)) {
            return entry;
        }
        entry.addAll(projectEntities);


        //先查project 查出页码和参数页码相同 即返回
        if (checkPage(projectEntities, pageIndex, pageSize)) {
            return entry;
        }

        //第一页没满的时候

        //剩余页码

        int pageLastIndex = pageIndex - projectEntities.size() / pageSize;


        List productEntities = entryProductEntityService.searchEntry(entryReqEntity, pageLastIndex, pageSize);
        if (CollectionUtils.isEmpty(productEntities)) {
            return entry;
        }
        entry.addAll(productEntities);

        if (checkPage(productEntities, pageIndex, pageSize)) {
            return entry;
        }


        //剩余页码
        int pageLastIndex1 = pageLastIndex - productEntities.size() / pageSize;
        List commonEntities = entryCommonEntityService.searchEntry(entryReqEntity, pageLastIndex1, pageSize);
        if (CollectionUtils.isEmpty(commonEntities)) {
            return entry;
        }
        entry.addAll(commonEntities);
        return entry;
    }

    @Override
    public String insertEntry(EntryEntity entryEntity) {
        //project
        if (ConstantInterface.PROJECT_TABLE.equals(entryEntity.getType())) {
            EntryProjectEntity entryProjectEntity = new EntryProjectEntity();
            BeanUtils.copyProperties(entryEntity, entryProjectEntity);
            String uuid = commonUtils.getUUID();
            entryProjectEntity.setId(uuid);
            int insert = entryProjectEntityMapper.insert(entryProjectEntity);
            if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.INSERT_ERROR;
            }

            return uuid;
            //工程表
        } else if (ConstantInterface.PRODUCT_TABLE.equals(entryEntity.getType())) {
            EntryProductEntity entryProductEntity = new EntryProductEntity();
            BeanUtils.copyProperties(entryEntity, entryProductEntity);
            String uuid = commonUtils.getUUID();
            entryProductEntity.setId(uuid);
            int insert = entryProductEntityMapper.insert(entryProductEntity);
            if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.INSERT_ERROR;
            }

            return uuid;
            //公共表
        } else if (ConstantInterface.COMMON_TABLE.equals(entryEntity.getType())) {
            EntryCommonEntity entryCommonEntity = new EntryCommonEntity();
            BeanUtils.copyProperties(entryEntity, entryCommonEntity);
            String uuid = commonUtils.getUUID();
            entryCommonEntity.setId(uuid);
            int insert = entryCommonEntityMapper.insert(entryCommonEntity);
            if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.INSERT_ERROR;
            }

            return uuid;
        }
        return ErrorCodeList.INSERT_ERROR;
    }

    @Override
    public String updateEntry(EntryEntity entryEntity) {
        if (ConstantInterface.PROJECT_TABLE.equals(entryEntity.getType())) {
            EntryProjectEntity entryProjectEntity = new EntryProjectEntity();
            BeanUtils.copyProperties(entryEntity, entryProjectEntity);
            QueryWrapper queryWrapper = new QueryWrapper();
            queryWrapper.eq("id", entryProjectEntity.getId());
            int update = entryProjectEntityMapper.update(entryProjectEntity, queryWrapper);
            if (update != ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.UPDATE_ERROR;
            }

            return ConstantInterface.OK_STR;
            //工程表
        } else if (ConstantInterface.PRODUCT_TABLE.equals(entryEntity.getType())) {

            EntryProductEntity entryProductEntity = new EntryProductEntity();
            BeanUtils.copyProperties(entryEntity, entryProductEntity);
            QueryWrapper queryWrapper = new QueryWrapper();
            queryWrapper.eq("id", entryProductEntity.getId());
            int update = entryProductEntityMapper.update(entryProductEntity, queryWrapper);
            if (update != ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.UPDATE_ERROR;
            }

            return ConstantInterface.OK_STR;
            //公共表
        } else if (ConstantInterface.COMMON_TABLE.equals(entryEntity.getType())) {
            EntryCommonEntity entryCommonEntity = new EntryCommonEntity();
            BeanUtils.copyProperties(entryEntity, entryCommonEntity);

            QueryWrapper queryWrapper = new QueryWrapper();
            queryWrapper.eq("id", entryCommonEntity.getId());
            int update = entryCommonEntityMapper.update(entryCommonEntity, queryWrapper);
            if (update != ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.UPDATE_ERROR;
            }

            return ConstantInterface.OK_STR;
        }
        return ErrorCodeList.UPDATE_ERROR;
    }

    @Override
    public String deleteEntry(EntryGroupEntity entryGroupEntity) {
        if (ConstantInterface.PROJECT_TABLE_Name.equals(entryGroupEntity.getTableName())) {
            int delete = entryProjectEntityMapper.deleteBatchIds(entryGroupEntity.getIds());
            if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.UPDATE_ERROR;
            }
            return ConstantInterface.OK_STR;
            //工程表
        } else if (ConstantInterface.PRODUCT_TABLE_Name.equals(entryGroupEntity.getTableName())) {
            int delete = entryProductEntityMapper.deleteBatchIds(entryGroupEntity.getIds());
            if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.UPDATE_ERROR;
            }
            return ConstantInterface.OK_STR;
            //公共表
        } else if (ConstantInterface.COMMON_TABLE_Name.equals(entryGroupEntity.getTableName())) {
            int delete = entryCommonEntityMapper.deleteBatchIds(entryGroupEntity.getIds());
            if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.UPDATE_ERROR;
            }
            return ConstantInterface.OK_STR;
        }
        return ErrorCodeList.DELETE_ERROR;
    }

    //判断list 是否满足 pageindex 的页码要求
    private boolean checkPage(List list, Integer pageIndex, Integer pageSize) {
        if (list.size() < pageSize) {
            return false;
        }
        int pageNowIndex = getPageIndex(list.size(), pageSize);
        //先查project 查出页码和参数页码相同 即返回
        if (pageNowIndex >= pageIndex) {
            return true;
        }
        return false;
    }

    private int getPageIndex(int size, Integer pageSize) {
        return size / pageSize + size % pageSize == 0 ? 0 : 1;
    }


    @Override
    //TODO
    public String bathAudit(List<EntryGroupEntity> entryGroupEntities, int state, HttpServletRequest request) {


        for (EntryGroupEntity entryGroupEntity : entryGroupEntities) {

            List<String> entryIDs = entryGroupEntity.getIds();
            String tableName = entryGroupEntity.getTableName();
            for (String entryID : entryIDs) {


                //批量审核
                EntryEntity entryEntity = auditByID(tableName, entryID, state);
                if (Objects.isNull(entryEntity)) {
                    return ErrorCodeList.UPDATE_ERROR;
                }
                //更新操作记录表
                EntryEntity entryEntity1 = entryMapper.selectById(entryID,tableName);


                String token = request.getHeader("token");
                String userName = JWTTokenUtils.getUserName(token);
                EntryOperate entryOperate = new EntryOperate();
                Date date = new Date();
                entryOperate.setOperateTime(date);
                entryOperate.setOperator(userName);
                entryOperate.setId(commonUtils.getUUID());
                entryOperate.setEntryId(entryID);
                entryOperate.setType(tableName);
                List<ComparisonResult> results = new ArrayList<>();
                OperateContentEntity operateContentEntity = new OperateContentEntity();
                try {
                    results = CompareUtils.compareFields(entryEntity, entryEntity1,EntryEntity.class);
                    if (results.size()==0){
                        log.error(" t_entry_operate compare result is null ! ");
                        return ErrorCodeList.INSERT_ERROR;
                    }
                    operateContentEntity.setResults(results);
                    operateContentEntity.setEntryID(entryID);
                    String res = " 词条ID : " + operateContentEntity.getEntryID() + ", 修改内容 : ";
                    for (ComparisonResult comparisonResult: operateContentEntity.getResults()){
                       res += comparisonResult.getStr() + " ; ";
                    }
                    entryOperate.setOperateContent(res);
                    int insert = entryOperateMapper.insert(entryOperate);
                    if (insert != ConstantInterface.DB_SUCCESS_RESULT){
                        log.error(" t_entry_operate update insert error ! ");
                        return ErrorCodeList.INSERT_ERROR;
                    }
                }catch (Exception e){
                    log.error(" ComparisonResult 类型对比异常 ！ ");
                    log.error(e.getMessage());
                }



                boolean operateResult = updateEntryOperate(entryEntity, entryEntity);


            }

        }


        return ConstantInterface.OK_STR;
    }

    private boolean updateEntryOperate(EntryEntity oldEntryEntity,EntryEntity newEntryEntity) {
        return true;


    }

    private EntryEntity auditByID(String tableName, String entryID, int state) {

        EntryEntity entryEntity = entryMapper.selectById(entryID,tableName);

        int update = entryMapper.auditById(tableName, entryID, state);

        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return entryEntity;
        }
        return entryEntity;
        //工程表


    }
}
