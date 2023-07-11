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

import com.shr.translationtoolservice.util.Translate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    Translate translate;
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
    EntryVersionMapper entryVersionMapper;
    @Autowired
    IndexMapper indexMapper;
    @Autowired
    EntryClassifyMapper entryClassifyMapper;
    @Autowired
    EntryLabelMapper entryLabelMapper;

    @Autowired
    CommonUtils commonUtils;
    @Autowired
    EntryProjectEntityService entryProjectEntityService;

    @Override
    public ResponseListModel searchEntry(EntryEntity entryEntity, Integer pageIndex, Integer pageSize) {
        //校验前端日期格式

        if (Objects.nonNull(entryEntity.getCreateTime()) && entryEntity.getCreateTime().toString().length() < 10) {
            String time = entryEntity.getCreateTime().toString() + ConstantInterface.TIME_ZERO;
            entryEntity.setCreateTime(new Date(time));
        }
        if (Objects.nonNull(entryEntity.getCreateEndRTime()) && entryEntity.getCreateEndRTime().toString().length() < 10) {
            String time = entryEntity.getCreateEndRTime().toString() + ConstantInterface.TIME_ZERO;
            entryEntity.setCreateEndRTime(new Date(time));
        }
        ResponseListModel result = new ResponseListModel<>();
        List<EntryEntity> entryEntities = getAllEntry(entryEntity, pageIndex, pageSize);
        result.setList(entryEntities);
        result.setTotalNum(entryEntities.size());



       /* QueryWrapper<EntryProjectEntity> projectEntityQueryWrapper = new QueryWrapper<EntryProjectEntity>();
        QueryWrapper<EntryProductEntity> productEntityQueryWrapper = new QueryWrapper<>();
        QueryWrapper<EntryCommonEntity> commonEntityQueryWrapper = new QueryWrapper<>();
            result.setList(getAllEntry(entryEntity, pageIndex, pageSize));
        if (StringUtils.isBlank(entryEntity.getTableName())) {
            result.setList(getAllEntry(entryEntity, pageIndex, pageSize));
            int total = entryCommonEntityMapper.selectCount(commonEntityQueryWrapper)
                    + entryProductEntityMapper.selectCount(productEntityQueryWrapper) + entryProjectEntityMapper.selectCount(projectEntityQueryWrapper);
            result.setTotalNum(total);
            //产品表

            } else if (ConstantInterface.PROJECT_TABLE.equals(entryEntity.getTableName())) {
                List<EntryEntity> entryEntities = entryMapper.selectListByEntry(entryEntity, pageSize, offset);
                result.setList(entryEntities);
                result.setTotalNum(entryEntities.size());
                //工程表
            } else if (ConstantInterface.PRODUCT_TABLE.equals(entryReqEntity.getLexicon())) {
                result.setList(entryProductEntityService.searchEntry(entryReqEntity, pageIndex, pageSize));
                result.setTotalNum(entryProductEntityMapper.selectCount(productEntityQueryWrapper));
                //公共表
            } else if (ConstantInterface.COMMON_TABLE.equals(entryReqEntity.getLexicon())) {
                result.setList(entryCommonEntityService.searchEntry(entryReqEntity, pageIndex, pageSize));
                result.setTotalNum(entryCommonEntityMapper.selectCount(commonEntityQueryWrapper));
            }*/

        return result;
    }

    //先查project表，不够再查 product ，最后再查comm
    @Override
    public List<EntryEntity> getAllEntry(EntryEntity entryEntity, Integer pageIndex, Integer pageSize) {
        List<EntryEntity> entry = new ArrayList();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            if (entryEntity.getTableName().contains(",")) {
                String[] tableNames = entryEntity.getTableName().split(",");


                for (String tableName : tableNames) {
                    entryEntity.setTableName(tableName);
                    List<EntryEntity> entryEntity1 = entryMapper.selectListByEntry(entryEntity, pageSize, offset);
                    entry.addAll(entryEntity1);
                    if (checkPage(entryEntity1, pageIndex, pageSize)) {
                        return entry;
                    }
                    pageSize = pageIndex - entryEntity1.size() / pageSize;

                }
            } else {
                entryEntity.setTableName(entryEntity.getTableName());
                List<EntryEntity> entryEntity1 = entryMapper.selectListByEntry(entryEntity, pageSize, offset);
                entry.addAll(entryEntity1);

            }


        }
     /*
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            entryEntity.setTableName(ConstantInterface.PROJECT_TABLE_Name);
            List<EntryEntity> entryEntity_project =  entryMapper.selectListByEntry(entryEntity, pageSize, offset);
            entry.addAll(entryEntity_project);
            if (checkPage(entryEntity_project, pageIndex, pageSize)) {
                return entry;
            }

            int pageLastIndex = pageIndex - entryEntity_project.size() / pageSize;
            entryEntity.setTableName(ConstantInterface.PROJECT_TABLE_Name);
            List<EntryEntity> entryEntity_product = entryMapper.selectListByEntry(entryEntity, pageLastIndex, pageSize);


            entry.addAll(entryEntity_product);

            if (checkPage(entryEntity_product, pageIndex, pageSize)) {
                return entry;
            }


            //剩余页码
            int pageLastIndex1 = pageLastIndex - entryEntity_product.size() / pageSize;
            List<EntryEntity> entryEntity_common = entryMapper.selectListByEntry(entryEntity, pageLastIndex1, pageSize);

            entry.addAll(entryEntity_common);
        }
*/
        return entry;
    }

    @Override
    public EntryOperate queryOperate(String entryId) {

        return entryOperateMapper.selectByEntryId(entryId);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    public List<EntryEntity> selectRepeEntry(String repeatEntryId) {
        List<EntryEntity> entryEntities = new ArrayList<>();
        //合并和未合并sql 增加过滤
        entryEntities = entryMapper.selectRepeEntry(repeatEntryId).stream().distinct().collect(Collectors.toList());

        return entryEntities;
    }

    @Override
    public String entryMerge(List<EntryEntity> entryEntities) {
        Index index = new Index();
        index.setRepeatEntryId(commonUtils.getUUID());
        int i = 0;
        if (entryEntities.size() > 0) {

            for (EntryEntity entryEntity : entryEntities) {
                i += 1;
                Index index1 = indexMapper.getIndexByEntry(entryEntity.getEntry());
                //如果存在 返回异常
                if (!Objects.isNull(index1)) {
                    return ErrorCodeList.OBJECT_HAS_EXIST;
                }


                //更新词条
                entryEntity.setRepeatEntryId(index.getRepeatEntryId());
                int update = entryMapper.updateById(entryEntity);
                if (update != ConstantInterface.DB_SUCCESS_RESULT) {
                    return ErrorCodeList.UPDATE_ERROR;
                }
                //写入index
                index.setEntry(entryEntity.getEntry());
                switch (i) {
                    case 1:
                        index.setTable1(entryEntity.getTableName());

                    case 2:
                        index.setTable2(entryEntity.getTableName());
                        break;
                    case 3:
                        index.setTable3(entryEntity.getTableName());
                        break;
                }

            }
        }
        int insert = indexMapper.insert(index);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" t_entry_operate update insert error ! ");
            return ErrorCodeList.INSERT_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public List<EntryClassify> getEntryClassfy(Integer pageIndex,
                                               Integer pageSize) {
        List<EntryClassify> entryClassifies = new ArrayList<>();

        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            entryClassifies = entryClassifyMapper.getEntryClassfyByIds(pageSize, offset);
        }
        return entryClassifies;

    }


    @Override
    public String insertEntry(EntryEntity entryEntity, HttpServletRequest request) {

        List<EntryEntity> entryEntities = entryMapper.selectByAbbr(entryEntity);
        List<EntryEntity> entryEntities1 = entryMapper.selectByName(entryEntity);
        if (entryEntities.size() + entryEntities1.size() > 0) {
            return ErrorCodeList.ABBR_HAS_EXIST;
        }
        String uuid = commonUtils.getUUID();
        entryEntity.setId(uuid);
        //构建字符长度
        constructEntry(entryEntity);
        if (Objects.isNull(entryEntity.getEntryState() ) ){
            entryEntity.setEntryState(1);
        }

        //创建人
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        if (StringUtils.isBlank(entryEntity.getCreator())) {
            entryEntity.setCreator(userName);
        }
        if (Objects.isNull(entryEntity.getCreateTime())) {
            Date date = new Date(System.currentTimeMillis());
            entryEntity.setCreateTime(date);
        }

        String lable = entryEntity.getEntryLabel();
        QueryWrapper<EntryLabel> queryWrapper = new QueryWrapper();
        queryWrapper.eq("name",lable);
        EntryLabel entryLabel = entryLabelMapper.selectOne(queryWrapper);
        if (Objects.isNull(entryLabel)){
            EntryLabel entryLabel1 = new EntryLabel();
            entryLabel1.setId(commonUtils.getUUID());
            entryLabel1.setLabelName(lable);
            entryLabelMapper.insert(entryLabel1);
        }

        int insert = entryMapper.insert(entryEntity);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.INSERT_ERROR;
        }
        EntryOperate entryOperate = new EntryOperate();

        entryOperate.setOperateContent(" 新增词条 ");
        int insert1 = constructOperate(entryOperate, entryEntity.getTableName(), entryEntity.getId(), request);


        if (insert1 != ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" t_entry_operate update insert error ! ");
            return ErrorCodeList.INSERT_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    private void constructEntry(EntryEntity entryEntity) {
        if (StringUtils.isNotBlank(entryEntity.getEntry())) {
            entryEntity.setEntryLength(entryEntity.getEntry().length());
        }
        if (StringUtils.isNotBlank(entryEntity.getEnglish())) {
            entryEntity.setEnglishLength(entryEntity.getEnglish().length());
            entryEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
        }
        if (StringUtils.isNotBlank(entryEntity.getEnglishDisable())) {
            entryEntity.setEnglishDisableLength(entryEntity.getEnglishDisable().length());
        }
        if (StringUtils.isNotBlank(entryEntity.getRussian())) {
            entryEntity.setRussianLength(entryEntity.getRussian().length());
            entryEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
        }
        if (StringUtils.isNotBlank(entryEntity.getSpanish())) {
            entryEntity.setSpanishLength(entryEntity.getSpanish().length());
            entryEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
        }
        if (StringUtils.isNotBlank(entryEntity.getFrench())) {
            entryEntity.setFrenchLength(entryEntity.getFrench().length());
            entryEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
        }
        //是否最新版本
        if (StringUtils.isNotBlank(entryEntity.getVersion())) {
            EntryVersion newVersion = entryVersionMapper.getNewVersion();
            if (entryEntity.getVersion().equals(newVersion.getName())) {
                entryEntity.setIsLatestVersion(1);
            } else {
                entryEntity.setIsLatestVersion(0);
            }
        }
        Index index = indexMapper.getIndexByEntry(entryEntity.getEntry());
        if (!Objects.isNull(index)) {
            entryEntity.setRepeatEntryId(index.getRepeatEntryId());
        }


    }


    @Override
    public String updateEntry(EntryEntity entryEntity, HttpServletRequest request) {
        EntryEntity beforEntry = entryMapper.selectById(entryEntity.getId(), entryEntity.getTableName());
        constructEntry(entryEntity);
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        entryEntity.setUpdate(userName);
        entryEntity.setUpdateTime(new Date(System.currentTimeMillis()));
        int update = entryMapper.updateById(entryEntity);
        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        if (Objects.isNull(entryEntity.getEntryState())) {
            entryEntity.setEntryState(beforEntry.getEntryState());
        }
        //更新操作记录表
        EntryOperate entryOperate = new EntryOperate();
        List<ComparisonResult> results = new ArrayList<>();
        OperateContentEntity operateContentEntity = new OperateContentEntity();
        try {
            EntryEntity afterEntry = entryMapper.selectById(entryEntity.getId(), entryEntity.getTableName());
            results = CompareUtils.compareFields(beforEntry, afterEntry, EntryEntity.class);
            if (results.size() == 0) {
                log.error(" t_entry_operate compare result is null ! ");
                return ErrorCodeList.INSERT_ERROR;
            }
            operateContentEntity.setResults(results);
            operateContentEntity.setEntryID(entryEntity.getId());
            String res = " 词条ID : " + operateContentEntity.getEntryID() + ", 修改内容 : ";
            for (ComparisonResult comparisonResult : operateContentEntity.getResults()) {
                res += comparisonResult.getStr() + " ; ";
            }
            entryOperate.setOperateContent(res);
            int insert = constructOperate(entryOperate, entryEntity.getTableName(), entryEntity.getId(), request);
            if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
                log.error(" t_entry_operate update insert error ! ");
                return ErrorCodeList.INSERT_ERROR;
            }
        } catch (Exception e) {
            log.error(" ComparisonResult 类型对比异常 ！ ");
            log.error(e.getMessage());
        }
        return ConstantInterface.OK_STR;


    }

    @Override
    public String deleteEntry(List<EntryEntity> entryEntities, String tableName) {
        int delete = entryMapper.deleteEntries(entryEntities, tableName);
        if (delete < ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;

    }

    @Override
    public TranslateEntity translate(EntryEntity entryEntity) {
        String english = translate.getTranslateResult(entryEntity.getEntry(), ConstantInterface.AUTO, ConstantInterface.ENGLISH);
        String russia = translate.getTranslateResult(entryEntity.getEntry(), ConstantInterface.AUTO, ConstantInterface.RUSSIAN);
        String spanish = translate.getTranslateResult(entryEntity.getEntry(), ConstantInterface.AUTO, ConstantInterface.SPANISH);
        String french = translate.getTranslateResult(entryEntity.getEntry(), ConstantInterface.AUTO, ConstantInterface.FRENCH);
        entryEntity.setEnglish(english);
        entryEntity.setRussian(russia);
        entryEntity.setSpanish(spanish);
        entryEntity.setFrench(french);
        entryMapper.updateById(entryEntity);
        TranslateEntity translateEntity = new TranslateEntity();
        translateEntity.setEnglish(english);
        translateEntity.setFrench(french);
        translateEntity.setRussian(russia);
        translateEntity.setSpanish(spanish);
        return translateEntity;
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
                EntryOperate entryOperate = new EntryOperate();

                //更新操作记录表
                EntryEntity entryEntity1 = entryMapper.selectById(entryID, tableName);

                List<ComparisonResult> results = new ArrayList<>();
                OperateContentEntity operateContentEntity = new OperateContentEntity();
                try {
                    results = CompareUtils.compareFields(entryEntity, entryEntity1, EntryEntity.class);
                    if (results.size() == 0) {
                        log.error(" t_entry_operate compare result is null ! ");
                        return ErrorCodeList.INSERT_ERROR;
                    }
                    operateContentEntity.setResults(results);
                    operateContentEntity.setEntryID(entryID);
                    String res = " 词条ID : " + operateContentEntity.getEntryID() + ", 修改内容 : ";
                    for (ComparisonResult comparisonResult : operateContentEntity.getResults()) {
                        res += comparisonResult.getStr() + " ; ";
                    }
                    entryOperate.setOperateContent(res);
                    int insert = constructOperate(entryOperate, entryGroupEntity.getTableName(), entryID, request);
                    if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
                        log.error(" t_entry_operate update insert error ! ");
                        return ErrorCodeList.INSERT_ERROR;
                    }
                } catch (Exception e) {
                    log.error(" ComparisonResult 类型对比异常 ！ ");
                    log.error(e.getMessage());
                }


            }

        }


        return ConstantInterface.OK_STR;
    }

    private int constructOperate(EntryOperate entryOperate, String tableName, String entryId, HttpServletRequest request) {
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        Date date = new Date();
        entryOperate.setOperateTime(date);
        entryOperate.setOperator(userName);
        entryOperate.setId(commonUtils.getUUID());
        entryOperate.setEntryId(entryId);
        entryOperate.setType(tableName);

        int insert = entryOperateMapper.insert(entryOperate);
        return insert;
    }


    private EntryEntity auditByID(String tableName, String entryID, int state) {

        EntryEntity entryEntity = entryMapper.selectById(entryID, tableName);

        int update = entryMapper.auditById(tableName, entryID, state);

        if (update != ConstantInterface.DB_SUCCESS_RESULT) {
            return entryEntity;
        }
        return entryEntity;
        //工程表


    }
}
