package com.shr.translationtoolservice.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.*;
import com.shr.translationtoolservice.entity.vo.TsVo.TsEntryInfoVo;
import com.shr.translationtoolservice.exception.i18nServerConnectException;
import com.shr.translationtoolservice.exception.i18nServerParseException;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.service.I18nService;
import com.shr.translationtoolservice.util.*;
import com.shr.translationtoolservice.util.DiUtils.DiEntryResult;
import com.shr.translationtoolservice.util.TsUtils.TsEntryResult;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.checkerframework.checker.units.qual.A;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collector;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 */
@Service
@Slf4j
public class EntryInfoServiceImpl extends ServiceImpl<EntryInfoMapper, EntryInfoEntity>
        implements EntryInfoService {

    @Autowired
    private EntryInfoMapper entryInfoMapper;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private EntryTempMapper entryTempMapper;

    @Autowired
    private TranslateMapper translateMapper;

    @Autowired
    private EntryOperateMapper entryOperateMapper;

    @Autowired
    private EntryClassifyMapper entryClassifyMapper;

    @Autowired
    private TaskInfoMapper taskInfoMapper;

    @Autowired
    private TLanguageMapper languageMapper;

    @Autowired
    private ProductRelationMapper productRelationMapper;

    @Autowired
    private TranslateUtils translateUtils;
    @Autowired
    private ExcelUtils excelUtils;

    @Autowired
    private VersionMapper versionMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductTableMapper productTableMapper;

    @Autowired
    private I18nService i18nService;

    @Autowired
    private EntryProcessUtils entryProcessUtils;

    @Autowired
    private DiUtils diUtils;
    @Autowired
    private TsUtils tsUtils;
    @Autowired
    private HTTPUtils httpUtils;

    @Value("${I18server.url}")
    private String I18URL;

    @Autowired
    private LocalTimeUtils localTimeUtils;

    @Autowired
    private EntryUtils entryUtils;

    @Autowired
    private EntryVersionMapper entryVersionMapper;

    @Override
    public List<EntryInfoEntity> getEntryByVersion(EntryInfoEntity entryInfoEntity1, Integer pageIndex, Integer pageSize) {
        int offset = 0;
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            offset = (pageIndex - 1) * pageSize;

        }
        processTranslationState(entryInfoEntity1);
        List<EntryInfoEntity> res = new ArrayList<>();
        List<EntryInfoEntity> entryByVersion = entryInfoMapper.getEntryByVersion(entryInfoEntity1, offset, pageSize);
        res.addAll(entryByVersion);
        for (EntryInfoEntity entryInfoEntity : entryByVersion) {
            TranslateEntity translateEntity = translateMapper.selectById(entryInfoEntity.getEnTransId());

            if (Objects.nonNull(translateEntity)) {
                entryInfoEntity.setEnglishTranslateState(translateEntity.getTranslateState());
            }else {
                entryInfoEntity.setEnglishTranslateState("0");
            }
            if (StringUtils.isNotBlank(entryInfoEntity1.getEnglishTranslateState()) && !entryInfoEntity1.getEnglishTranslateState().equals(entryInfoEntity.getEnglishTranslateState())){
                res.remove(entryInfoEntity);
            }
        }

      /*  List<EntryVO> entryVOS = new ArrayList<>();
        for (EntryInfoEntity entryInfoEntity : entryByVersion) {
            EntryVO entryVO = new EntryVO();
            List<TranslateEntity> translateEntityList = new ArrayList<>();
            getTransEntity(entryInfoEntity.getEnTransId(), translateEntityList);
            getTransEntity(entryInfoEntity.getRuTransId(), translateEntityList);
            getTransEntity(entryInfoEntity.getSpaTransId(), translateEntityList);
            getTransEntity(entryInfoEntity.getFraTransId(), translateEntityList);
            entryVO.setTranslateEntity(translateEntityList);
            entryVO.setEntryInfoEntity(entryInfoEntity);
            entryVOS.add(entryVO);
        }*/
        return res;
    }

    private void processTranslationState(EntryInfoEntity entryInfoEntity) {
        if (StringUtils.isNotBlank(entryInfoEntity.getEnglishTranslateState())){
            switch (entryInfoEntity.getEnglishTranslateState()){
                case ConstantInterface.TRANSLAT_UNAUDIT:
                    entryInfoEntity.setEnglishTranslateState("1");
                    break;
                case ConstantInterface.TRANSLAT_AUDIT:
                    entryInfoEntity.setEnglishTranslateState("3");
                    break;
                case ConstantInterface.UNTRANSLATED:
                    entryInfoEntity.setEnglishTranslateState("0");
                    break;
                case ConstantInterface.TRANSLAT_FAIL_AUDIT:
                    entryInfoEntity.setEnglishTranslateState("2");
                    break;
            }
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getRussianTranslateState())){
            switch (entryInfoEntity.getRussianTranslateState()){
                case ConstantInterface.TRANSLAT_UNAUDIT:
                    entryInfoEntity.setRussianTranslateState("1");
                    break;
                case ConstantInterface.TRANSLAT_AUDIT:
                    entryInfoEntity.setRussianTranslateState("3");
                    break;
                case ConstantInterface.UNTRANSLATED:
                    entryInfoEntity.setRussianTranslateState("0");
                    break;
                case ConstantInterface.TRANSLAT_FAIL_AUDIT:
                    entryInfoEntity.setRussianTranslateState("2");
                    break;
            }
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getSpanishTranslateState())){
            switch (entryInfoEntity.getSpanishTranslateState()){
                case ConstantInterface.TRANSLAT_UNAUDIT:
                    entryInfoEntity.setSpanishTranslateState("1");
                    break;
                case ConstantInterface.TRANSLAT_AUDIT:
                    entryInfoEntity.setSpanishTranslateState("3");
                    break;
                case ConstantInterface.UNTRANSLATED:
                    entryInfoEntity.setSpanishTranslateState("0");
                    break;
                case ConstantInterface.TRANSLAT_FAIL_AUDIT:
                    entryInfoEntity.setSpanishTranslateState("2");
                    break;
            }
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getSpanishTranslateState())){
            switch (entryInfoEntity.getSpanishTranslateState()){
                case ConstantInterface.TRANSLAT_UNAUDIT:
                    entryInfoEntity.setSpanishTranslateState("1");
                    break;
                case ConstantInterface.TRANSLAT_AUDIT:
                    entryInfoEntity.setSpanishTranslateState("3");
                    break;
                case ConstantInterface.UNTRANSLATED:
                    entryInfoEntity.setSpanishTranslateState("0");
                    break;
                case ConstantInterface.TRANSLAT_FAIL_AUDIT:
                    entryInfoEntity.setSpanishTranslateState("2");
                    break;
            }
        }

    }

    private void getTransEntity(String transId, List<TranslateEntity> translateEntityList) {
        if (StringUtils.isNotBlank(transId)) {
            TranslateEntity translateEntity = translateMapper.selectById(transId);
            translateEntityList.add(translateEntity);
        }
    }

    @Override
    public int getEntryByVersionTotal(EntryInfoEntity entryInfoEntity1) {
        return entryInfoMapper.getEntryByVersionTotal(entryInfoEntity1);
    }

    @Override
    public HttpResponse<String> addEntryByVersion(List<EntryVO> entryVOS, HttpServletRequest request) {
        HttpResponse<String> response = new HttpResponse<>();
        for (EntryVO entryVO : entryVOS) {
            EntryInfoEntity entryInfoEntity = entryVO.getEntryInfoEntity();
            //一次新增只有一种翻译，取第一个元素
            TranslateEntity translateEntities = entryVO.getTranslateEntity().get(0);
            //存在翻译
            if (!Objects.isNull(translateEntities)) {
                //查询是否有存在已有的翻译 需传入版本
                if (StringUtils.isBlank(translateEntities.getVersionID())) {
                    response.setMessage("TranslateEntity version is null ！ ");
                    response.setCode(HttpResponse.Type.ERROR.getVal());
                    response.setType(HttpResponse.Type.ERROR);
                    return response;
                }
                List<TranslateEntity> translateEntity = translateMapper.getTrans(translateEntities, 0, 20);

                //不存在翻译则创建
                if (CollectionUtils.isEmpty(translateEntity)) {
                    translateEntities.setDeleteState(0);
                    translateEntities.setPublicState(0);
                    //添加transid
                    addTransID(translateEntities, entryInfoEntity);
                    // int transInsert = translateMapper.insert(translateEntities);
                    int transInsert = translateMapper.insertTranslate(translateEntities);

                    if (transInsert != ConstantInterface.DB_SUCCESS_RESULT) {
                        log.error("  insert error ! ");
                        response.setMessage(ErrorCodeList.INSERT_ERROR);
                        response.setCode(HttpResponse.Type.ERROR.getVal());
                        response.setType(HttpResponse.Type.ERROR);
                        return response;
                    }
                } else {
                    //添加transid
                    addTransID(translateEntity.get(0), entryInfoEntity);
                }
            }
            //校验ABBR
            if (checkAbbrRepe(entryInfoEntity, entryVO.getTableName())) {
                response.setMessage(ErrorCodeList.ABBR_HAS_EXIST);
                response.setCode(HttpResponse.Type.ERROR.getVal());
                response.setType(HttpResponse.Type.ERROR);
                return response;
            }
            if (StringUtils.isBlank(entryInfoEntity.getId())) {
                entryInfoEntity.setId(commonUtils.getUUID());
            }
            entryInfoEntity.setIsDelete(0);
            entryInfoEntity.setEntryState(0);
            int insert = entryInfoMapper.insertEntry(entryInfoEntity, entryVO.getTableName());


            if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
                log.error("  insert error ! ");
                response.setMessage(ErrorCodeList.INSERT_ERROR);
                response.setCode(HttpResponse.Type.ERROR.getVal());
                response.setType(HttpResponse.Type.ERROR);
                return response;
            }


            EntryOperate entryOperate = new EntryOperate();
            //添加操作类型
            entryOperate.setType(ConstantInterface.OPERATION_TYPE_INSERT);
            entryOperate.setOperateContent(ConstantInterface.OPERATION_TYPE_INSERT);
            int insert1 = constructOperate(entryOperate, entryInfoEntity.getId(), request);
            if (insert1 != ConstantInterface.DB_SUCCESS_RESULT) {
                log.error(" t_entry_operate update insert error ! ");
                response.setMessage(ErrorCodeList.INSERT_ERROR);
                response.setCode(HttpResponse.Type.ERROR.getVal());
                response.setType(HttpResponse.Type.ERROR);
                return response;
            }
        }

        response.setMessage(ErrorCodeList.SUCCESS);
        response.setCode(HttpResponse.Type.OK.getVal());
        response.setType(HttpResponse.Type.OK);
        return response;
    }

    @Override
    public String addEntryInfo(EntryInfoEntity entryInfoEntity, HttpServletRequest request, String tableName) {
        if (StringUtils.isBlank(entryInfoEntity.getId())) {
            entryInfoEntity.setId(commonUtils.getUUID());
        }
        int insert = entryInfoMapper.insertEntry(entryInfoEntity, tableName);
        //版本和任务不为空 才往关系表中写入
        ProductRelationEntity productRelationEntity = new ProductRelationEntity();
        productRelationEntity.setId(commonUtils.getUUID());
        productRelationEntity.setEntryId(entryInfoEntity.getId());
        productRelationEntity.setProductId(entryInfoEntity.getProductID());
        productRelationEntity.setVersionId(entryInfoEntity.getVersionID());
        productRelationMapper.insert(productRelationEntity);


        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" t_entry_operate update insert error ! ");
            return ErrorCodeList.INSERT_ERROR;
        }
        EntryOperate entryOperate = new EntryOperate();
        //添加操作类型
        entryOperate.setType(ConstantInterface.OPERATION_TYPE_INSERT);
        entryOperate.setOperateContent(ConstantInterface.OPERATION_TYPE_INSERT);
        int insert1 = constructOperate(entryOperate, entryInfoEntity.getId(), request);
        if (insert1 != ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" t_entry_operate update insert error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String updateEntryInfo(EntryInfoEntity entryInfoEntity, HttpServletRequest request, String notes) {
        EntryInfoEntity beforEntry = entryInfoMapper.selectEntryById(entryInfoEntity);
        //beforEntry.setTableName(entryInfoEntity.getTableName());
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        Date date = new Date(System.currentTimeMillis());
        entryInfoEntity.setUpdate(userName);
        entryInfoEntity.setUpdateTime(date);

        //翻译如果更新的情况下 查找翻译表存在相同的 挂在已存在的额id 没有新增一个翻译
        updateTrans(entryInfoEntity, department);

        int update = entryInfoMapper.updateEntryInfoForManager(entryInfoEntity);
        if (update < ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        //操作记录写入
        EntryOperate entryOperate = null;
        try {
            entryOperate = insertUpdateOperate(beforEntry, notes);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (Objects.isNull(entryOperate)) {
            return " 词条信息无变化 ！ ";
        }
        int insert = constructOperate(entryOperate, beforEntry.getId(), request);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" t_entry_operate update insert error ! ");
            return ErrorCodeList.INSERT_ERROR;
        }

        return ConstantInterface.OK_STR;
    }

    //翻译如果更新的情况下 查找翻译表存在相同的 挂在已存在的额id 没有新增一个翻译
    //此方法只适用已审核的翻译
    private void updateTrans(EntryInfoEntity entryInfoEntity, String department) {
        String id = "";
        String transType = "";
        if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
            transType = ConstantInterface.ENGLISH;
            List<TranslateEntity> translateEntitys = new ArrayList<>();
            translateEntitys = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getEnglish(), department);
            if (CollectionUtils.isEmpty(translateEntitys)) {
                TranslateEntity translateEntity = new TranslateEntity();
                id = commonUtils.getUUID();
                translateEntity.setId(id);
                translateEntity.setEntry(entryInfoEntity.getEntry());
                translateEntity.setVersionID(entryInfoEntity.getVersionID());
                translateEntity.setTranslate(entryInfoEntity.getEnglish());
                translateEntity.setPublicState(0);
                translateEntity.setTranslateState("3");
                translateEntity.setDeleteState(0);
                translateEntity.setVisualRange(department);
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                translateEntity.setType(ConstantInterface.ENGLISH);
                // translateMapper.insert(translateEntity);
                translateMapper.insertTranslate(translateEntity);

            } else {
                TranslateEntity translateEntity = translateEntitys.stream().max(Comparator.comparing(TranslateEntity::getLastUseTime)).get();
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                id = translateEntity.getId();
                translateMapper.updateById(translateEntity);
            }
            entryInfoEntity.setEnTransId(id);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
            transType = ConstantInterface.FRENCH;
            List<TranslateEntity> translateEntitys = new ArrayList<>();
            translateEntitys = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getFrench(), department);
            if (CollectionUtils.isEmpty(translateEntitys)) {
                TranslateEntity translateEntity = new TranslateEntity();
                id = commonUtils.getUUID();
                translateEntity.setId(id);
                translateEntity.setEntry(entryInfoEntity.getEntry());
                translateEntity.setTranslate(entryInfoEntity.getFrench());
                translateEntity.setVersionID(entryInfoEntity.getVersionID());
                translateEntity.setPublicState(0);
                translateEntity.setTranslateState("3");
                translateEntity.setDeleteState(0);
                translateEntity.setType(transType);
                translateEntity.setVisualRange(department);
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                // translateMapper.insert(translateEntity);
                translateMapper.insertTranslate(translateEntity);
            } else {
                TranslateEntity translateEntity = translateEntitys.stream().max(Comparator.comparing(TranslateEntity::getLastUseTime)).get();
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                id = translateEntity.getId();
                translateMapper.updateById(translateEntity);
            }
            entryInfoEntity.setFraTransId(id);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
            transType = ConstantInterface.SPANISH;
            List<TranslateEntity> translateEntitys = new ArrayList<>();
            translateEntitys = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getSpanish(), department);
            if (CollectionUtils.isEmpty(translateEntitys)) {
                TranslateEntity translateEntity = new TranslateEntity();
                id = commonUtils.getUUID();
                translateEntity.setId(id);
                translateEntity.setEntry(entryInfoEntity.getEntry());
                translateEntity.setTranslate(entryInfoEntity.getSpanish());
                translateEntity.setVersionID(entryInfoEntity.getVersionID());
                translateEntity.setPublicState(0);
                translateEntity.setTranslateState("3");
                translateEntity.setDeleteState(0);
                translateEntity.setType(transType);
                translateEntity.setVisualRange(department);
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                // translateMapper.insert(translateEntity);
                translateMapper.insertTranslate(translateEntity);
            } else {
                TranslateEntity translateEntity = translateEntitys.stream().max(Comparator.comparing(TranslateEntity::getLastUseTime)).get();
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                id = translateEntity.getId();
                translateMapper.updateById(translateEntity);
            }
            entryInfoEntity.setSpaTransId(id);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
            transType = ConstantInterface.RUSSIAN;
            List<TranslateEntity> translateEntitys = new ArrayList<>();
            translateEntitys = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getRussian(), department);
            if (CollectionUtils.isEmpty(translateEntitys)) {
                TranslateEntity translateEntity = new TranslateEntity();
                id = commonUtils.getUUID();
                translateEntity.setId(id);
                translateEntity.setEntry(entryInfoEntity.getEntry());
                translateEntity.setTranslate(entryInfoEntity.getRussian());
                translateEntity.setVersionID(entryInfoEntity.getVersionID());
                translateEntity.setPublicState(0);
                translateEntity.setTranslateState("3");
                translateEntity.setDeleteState(0);
                translateEntity.setType(transType);
                translateEntity.setVisualRange(department);
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                // translateMapper.insert(translateEntity);
                translateMapper.insertTranslate(translateEntity);
            } else {
                TranslateEntity translateEntity = translateEntitys.stream().max(Comparator.comparing(TranslateEntity::getLastUseTime)).get();
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                id = translateEntity.getId();
                translateMapper.updateById(translateEntity);
            }
            entryInfoEntity.setRuTransId(id);
        }


    }


    //新增一个翻译
    //此方法只适用未审核的翻译
    private void updateWorkTrans(EntryInfoEntity entryInfoEntity, String department) {
        String id = "";
        String transType = "";
        if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
            transType = ConstantInterface.ENGLISH;
            List<TranslateEntity> translateEntitys = new ArrayList<>();
            translateEntitys = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getEnglish(), department);
            if (CollectionUtils.isEmpty(translateEntitys)) {
                TranslateEntity translateEntity = new TranslateEntity();
                id = commonUtils.getUUID();
                translateEntity.setId(id);
                translateEntity.setEntry(entryInfoEntity.getEntry());
                translateEntity.setVersionID(entryInfoEntity.getVersionID());
                translateEntity.setTranslate(entryInfoEntity.getEnglish());
                translateEntity.setPublicState(0);
                translateEntity.setTranslateState("1");
                translateEntity.setDeleteState(0);
                translateEntity.setVisualRange(department);
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                translateEntity.setType(ConstantInterface.ENGLISH);
                // translateMapper.insert(translateEntity);
                translateMapper.insertTranslate(translateEntity);

            }
            entryInfoEntity.setEnTransId(id);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
            transType = ConstantInterface.FRENCH;
            List<TranslateEntity> translateEntitys = new ArrayList<>();
            translateEntitys = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getFrench(), department);
            if (CollectionUtils.isEmpty(translateEntitys)) {
                TranslateEntity translateEntity = new TranslateEntity();
                id = commonUtils.getUUID();
                translateEntity.setId(id);
                translateEntity.setEntry(entryInfoEntity.getEntry());
                translateEntity.setTranslate(entryInfoEntity.getFrench());
                translateEntity.setVersionID(entryInfoEntity.getVersionID());
                translateEntity.setPublicState(0);
                translateEntity.setTranslateState("1");
                translateEntity.setDeleteState(0);
                translateEntity.setType(transType);
                translateEntity.setVisualRange(department);
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                // translateMapper.insert(translateEntity);
                translateMapper.insertTranslate(translateEntity);
            }
            entryInfoEntity.setFraTransId(id);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
            transType = ConstantInterface.SPANISH;
            List<TranslateEntity> translateEntitys = new ArrayList<>();
            translateEntitys = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getSpanish(), department);
            if (CollectionUtils.isEmpty(translateEntitys)) {
                TranslateEntity translateEntity = new TranslateEntity();
                id = commonUtils.getUUID();
                translateEntity.setId(id);
                translateEntity.setEntry(entryInfoEntity.getEntry());
                translateEntity.setTranslate(entryInfoEntity.getSpanish());
                translateEntity.setVersionID(entryInfoEntity.getVersionID());
                translateEntity.setPublicState(0);
                translateEntity.setTranslateState("1");
                translateEntity.setDeleteState(0);
                translateEntity.setType(transType);
                translateEntity.setVisualRange(department);
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                // translateMapper.insert(translateEntity);
                translateMapper.insertTranslate(translateEntity);
            }
            entryInfoEntity.setSpaTransId(id);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
            transType = ConstantInterface.RUSSIAN;
            List<TranslateEntity> translateEntitys = new ArrayList<>();
            translateEntitys = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getRussian(), department);
            if (CollectionUtils.isEmpty(translateEntitys)) {
                TranslateEntity translateEntity = new TranslateEntity();
                id = commonUtils.getUUID();
                translateEntity.setId(id);
                translateEntity.setEntry(entryInfoEntity.getEntry());
                translateEntity.setTranslate(entryInfoEntity.getRussian());
                translateEntity.setVersionID(entryInfoEntity.getVersionID());
                translateEntity.setPublicState(0);
                translateEntity.setTranslateState("1");
                translateEntity.setDeleteState(0);
                translateEntity.setType(transType);
                translateEntity.setVisualRange(department);
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                // translateMapper.insert(translateEntity);
                translateMapper.insertTranslate(translateEntity);
            }
            entryInfoEntity.setRuTransId(id);
        }


    }

    //更新操作记录表
    private EntryOperate insertUpdateOperate(EntryInfoEntity beforEntry, String notes) throws Exception {
        EntryOperate entryOperate = new EntryOperate();
        entryOperate.setType(ConstantInterface.OPERATION_TYPE_UPDATE);
        entryOperate.setNotes(notes);
        List<ComparisonResult> results = new ArrayList<>();
        OperateContentEntity operateContentEntity = new OperateContentEntity();
        EntryInfoEntity afterEntry = entryInfoMapper.selectEntryById(beforEntry);


        results = CompareUtils.compareFields(beforEntry, afterEntry, EntryInfoEntity.class);
        if (results.size() == 0) {
            log.error(" t_entry_operate no change ! ");
//                return new ResultObject(ErrorCodeList.INSERT_ERROR);
            return null;
        }
        operateContentEntity.setResults(results);
        operateContentEntity.setEntryID(beforEntry.getId());
        String res = "";
        //操作记录写入
        for (ComparisonResult comparisonResult : operateContentEntity.getResults()) {
            String name = comparisonResult.getKey();
            if ("enTransId".equals(name) || "ruTransId".equals(name) || "fraTransId".equals(name) || "spaTransId".equals(name)) {
                continue;
            }
            //不写入操作内容的字段
            if ("update".equals(name) || "updateTime".equals(name) || "entryLength".equals(name) || "tableName".equals(name)) {
                continue;
            }
            HashMap<String, String> entryName = ConstantInterface.constructEntryName();
            String str = "";
            String r1 = comparisonResult.getPrevious();
            String r2 = comparisonResult.getLater();
            if (StringUtils.isBlank(r1)) {

                if ("classifyId".equals(name)) {
                    str = entryName.get(comparisonResult.getKey()) + "新增值为 ( " + entryClassifyMapper.selectClassfyById(r2).getTitle() + " )  ";
                    //翻译字段处理
                } else if (ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.EN_TRANS) ||
                        ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.RU_TRANS) ||
                        ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.SPA_TRANS) ||
                        ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.FRA_TRANS)) {
                    // String transStr = translateMapper.selectById(r2).getTranslate();
                    str = entryName.get(name) + " 新增值为： " + r2;

                } else {
                    str = entryName.get(name) + " 新增值为： " + r2;
                }
            } else {

                if ("classifyId".equals(name)) {
                    str = entryName.get(name) + " 值由 ( " + entryClassifyMapper.selectClassfyById(r1).getTitle() + " ) 改为 ( " + entryClassifyMapper.selectById(r2).getTitle() + " )  ";
                } else if (ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.EN_TRANS) ||
                        ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.RU_TRANS) ||
                        ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.SPA_TRANS) ||
                        ConstantInterface.constructEntryName().get(name).equals(ConstantInterface.FRA_TRANS)) {
                    // String r1TransStr = translateMapper.selectById(r1).getTranslate();
                    //  String r2TransStr = translateMapper.selectById(r2).getTranslate();
                    str = entryName.get(name) + " 值由 ( " + r1 + " ) 改为 ( " + r2 + " )  ";

                } else {
                    str = entryName.get(name) + " 值由 ( " + r1 + " ) 改为 ( " + r2 + " )  ";
                }
            }

            res += str + " ; ";
        }
        if (StringUtils.isBlank(res)) {
            log.error(" t_entry_operate no change ! ");
//                return new ResultObject(ErrorCodeList.INSERT_ERROR);
            return null;
        }

        entryOperate.setOperateContent(res);


        return entryOperate;
    }

    @Override
    public String deleteEntryInfo(List<String> idList, String tableName) {
        int delete = entryInfoMapper.deleteByIdList(idList, tableName);
        if (delete != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        productRelationMapper.deleteByEntryID(idList);
        return ConstantInterface.OK_STR;
    }

    @Override
    public List<TranslateEntity> getPublicEntry(TranslateEntity translateEntity, int offset, Integer pageSize) {
        List<TranslateEntity> entryPublicEntities = translateMapper.getPublicEntry(translateEntity, offset, pageSize);
        return entryPublicEntities;
    }

    @Override
    public int getPublicEntryTotal(TranslateEntity translateEntity) {
        return translateMapper.getPublicEntryTotal(translateEntity);
    }

    @Override
    public String updatePublicEntry(TranslateEntity translateEntity) {
        int update = translateMapper.updateById(translateEntity);
        if (update < ConstantInterface.DB_SUCCESS_RESULT) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String addPublicEntry(List<TranslateEntity> translateEntities) {
        int insert = 0;
        for (TranslateEntity translateEntity : translateEntities) {
            //判断同版本下是否存在此翻译
            TranslateEntity translateEntity1 = translateMapper.selectPublicByEntry(translateEntity);
            if (Objects.nonNull(translateEntity1)) {
                continue;
            }
            if (StringUtils.isBlank(translateEntity.getId())) {
                translateEntity.setId(commonUtils.getUUID());
            }
            translateEntity.setDeleteState(0);
            // insert += translateMapper.insert(translateEntity);
            insert += translateMapper.insertTranslate(translateEntity1);
        }
        if (insert < translateEntities.size()) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String deletePublicEntry(List<String> idlist) {
        int delete = translateMapper.deleteByIds(idlist);
        if (delete != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    //TODO  废弃
    public String upgrade(UpgradeVO upgradeVO, HttpServletRequest request) {
        //创建任务 流程到翻译审核员
        TaskInfoEntity taskInfoEntity = upgradeVO.getTaskInfoEntities();
        List<EntryVO> entryVOList = upgradeVO.getEntryVOList();
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        Date date = new Date();
        taskInfoEntity.setDepartment(userName);
        taskInfoEntity.setImportTime(date);
        taskInfoEntity.setEntryAuditor(userName);
        taskInfoEntity.setEntryAutiorStartTime(date);
        taskInfoEntity.setTranslator(userName);
        taskInfoEntity.setTranslateStartTime(date);
        taskInfoEntity.setState("4");
        taskInfoEntity.setDepartment(department);
        int insert = taskInfoMapper.insert(taskInfoEntity);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.INSERT_TASK_ERROR;
        }

        // 更新词条信息

        for (EntryVO entryVO : entryVOList) {
            String tableName = entryVO.getTableName();
            EntryInfoEntity entryInfoEntity = entryVO.getEntryInfoEntity();
            TranslateEntity translateEntity = entryVO.getTranslateEntity().get(0);
            //查找同版本下相同词条是否有同翻译
        }

        // 记录操作记录
        return null;
    }

    @Override
    public String updateEntryInfoList(List<EntryInfoEntity> entryInfoEntities, HttpServletRequest request, String notes) {
        String result = "";
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            if (StringUtils.isBlank(entryInfoEntity.getTableName())) {
                return ErrorCodeList.TBALE_IS_NULL;
            }
            result = updateEntryInfo(entryInfoEntity, request, notes);

        }

        return result;
    }

    private int constructOperate(EntryOperate entryOperate, String entryId, HttpServletRequest request) {
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        Date date = new Date();
        entryOperate.setOperateTime(date);
        entryOperate.setOperator(userName);
        entryOperate.setId(commonUtils.getUUID());
        entryOperate.setEntryId(entryId);

        int insert = entryOperateMapper.insert(entryOperate);
        return insert;
    }

    @Override
    public void addTransID(TranslateEntity translateEntities, EntryInfoEntity entryInfoEntity) {
        switch (translateEntities.getType()) {
            case ConstantInterface.ENGLISH:
                if (StringUtils.isBlank(translateEntities.getId())) {
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setEnTransId(transID);
                } else {
                    entryInfoEntity.setEnTransId(translateEntities.getId());
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isBlank(translateEntities.getId())) {
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setFraTransId(transID);
                } else {
                    entryInfoEntity.setFraTransId(translateEntities.getId());
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isBlank(translateEntities.getId())) {
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setRuTransId(transID);
                } else {
                    entryInfoEntity.setRuTransId(translateEntities.getId());
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isBlank(translateEntities.getId())) {
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setSpaTransId(transID);
                } else {
                    entryInfoEntity.setSpaTransId(translateEntities.getId());
                }
                break;

        }
    }

    @Override
    public void versionExport(String versionID, HttpServletResponse response, String translateType) {

        VersionEntity versionEntity = versionMapper.selectById(versionID);
        String tableName = versionEntity.getTableName();
        String productID = versionEntity.getProductId();

        ProductEntity productEntity = productMapper.selectById(productID);

        List<EntryInfoEntity> entryInfoEntities = entryInfoMapper.getEntryByVersionID(tableName, versionID);


        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        String da = format.format(date);
//        List<TLanguage> languageList = languageMapper.selectLaguageByName(translateType);
        String excelName = translateType +
                ConstantInterface.UNDERLINE + productEntity.getName() +
                ConstantInterface.UNDERLINE + versionEntity.getName() + ConstantInterface.UNDERLINE + da;

        String fileName = excelName + ".xls";

        try {
            fileName = URLEncoder.encode(fileName, "UTF-8");
            log.warn(" **** fileName : " + fileName + " ***** ");
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
        ServletOutputStream outputStream = null;
        Workbook workbook = null;
        try {

            workbook = excelUtils.outPutExcel(entryInfoEntities, translateType, excelName);
            outputStream = response.getOutputStream();
            workbook.write(outputStream);

            workbook.close();
            outputStream.close();

        } catch (Exception e) {
            log.error(" ===== excel write error : " + e.getMessage() + " ===== ");
        } finally {
       /*     try {
                outputStream.close();
            } catch (IOException e) {
                log.error("最终关闭流失败!", e);
            }*/
        }
    }

    @Override
    public List<EntryInfoEntity> importZZExcle(MultipartFile multipartFile, String taskID, HttpServletRequest request) {
        String fileName = multipartFile.getOriginalFilename();
        List<ImportExcleEntry> importExcleEntries = new ArrayList<>();
        List<EntryInfoEntity> entryEntitys = new ArrayList<>();
        String token = request.getHeader("token");

        String userName = JWTTokenUtils.getUserName(token);
        //读取excle转换的实体
        TaskInfoEntity taskInfoEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

        //  ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(taskInfoEntity.getProductId());

        try {
            //装饰
            if (fileName.contains("_zz.xlsx")) {
                //拼成词条实体
                //2区分出已存在词条 和新词条 is_exist 区分
                //3.新词条翻译预写在词条中
                importExcleEntries = excelUtils.readZZExcelToEntity(taskInfoEntity.getTranslateType(),ImportExcleEntry.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
                zzEntryHandle(importExcleEntries, entryEntitys, userName, taskInfoEntity, fileName);
            } else if (fileName.contains("_common.xlsx")) {
                entryEntitys = excelUtils.readExcelToEntity(taskInfoEntity.getTranslateType(),EntryInfoEntity.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
                ptEntryHandle(entryEntitys, userName, taskInfoEntity, fileName);
            } else if (fileName.contains("_jk.xlsx")) {
                entryEntitys = excelUtils.readExcelToEntity(taskInfoEntity.getTranslateType(),EntryInfoEntity.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
                ptEntryHandle(entryEntitys, userName, taskInfoEntity, fileName);
            }else if (fileName.contains("_db.xlsx")) {
                entryEntitys = excelUtils.readExcelToEntity(taskInfoEntity.getTranslateType(),EntryInfoEntity.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
                ptEntryHandle(entryEntitys, userName, taskInfoEntity, fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String diFileName = request.getParameter("diFileName");
        if(diFileName != null){
            for(EntryInfoEntity entry : entryEntitys){
                entry.setDiFileName(diFileName);
            }
        }
        return entryProcessUtils.buildRepeEntry(entryEntitys, taskInfoEntity.getTranslateType());
    }

    private void ptEntryHandle(List<EntryInfoEntity> entryEntitys, String userName, TaskInfoEntity taskInfoEntity, String fileName) {
        Date date = new Date(System.currentTimeMillis());
        for (EntryInfoEntity entryInfoEntity : entryEntitys) {

            entryInfoEntity.setEntryLength(entryInfoEntity.getEntry().length());
            entryInfoEntity.setUpdate(userName);
            entryInfoEntity.setUpdateTime(date);

            entryInfoEntity.setIsDelete(0);
            entryInfoEntity.setIsPublic(0);
            entryInfoEntity.setEntryState(1);

            entryInfoEntity.setId(commonUtils.getUUID());
            if (Objects.nonNull(taskInfoEntity)) {
                entryInfoEntity.setTaskId(taskInfoEntity.getId());
                entryInfoEntity.setProductID(taskInfoEntity.getProductId());
                entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
            }
            if (fileName.contains("_db_meta_common.xlsx")) {
                entryInfoEntity.setWriteType(ConstantInterface.DI);
                String tag = entryInfoEntity.getTag();
                //tag 转成json
                entryInfoEntity.setImportType(ConstantInterface.DB_META);
                JSONObject jsonObject = JSONObject.parseObject(tag);
                String appName = jsonObject.get("appName").toString();
                String dbName = jsonObject.get("dbName").toString().replace(".","_");
                String tableName = jsonObject.get("tableName").toString();
                String fieldName = jsonObject.get("fieldName").toString();
                entryInfoEntity.setTag( tableName + "/" + fieldName);
                entryInfoEntity.setDiFileName("db/meta/"  +  dbName );
            }


            if (StringUtils.isBlank(entryInfoEntity.getImportType())){
                entryInfoEntity.setImportType(ConstantInterface.EXCEL);
            }
            if (StringUtils.isBlank(entryInfoEntity.getWriteType())){
                entryInfoEntity.setWriteType(ConstantInterface.DI);
            }

            if (StringUtils.isBlank(entryInfoEntity.getEntrySource())){
                entryInfoEntity.setEntrySource("import : " + ConstantInterface.EXCEL + " ; fileName" + fileName);
            }

            String productTableName = "t_entry_info";
            // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());
            List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryList(productTableName, entryInfoEntity, taskInfoEntity.getProductId());
            if (CollectionUtils.isEmpty(entryEntities)) {
                //创建新翻译
                entryInfoEntity.setIsExist(0);
                entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
                entryInfoEntity.setEntryVersion(1);
                setPTTranslateState(taskInfoEntity, entryInfoEntity);

            } else {
                entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
                entryInfoEntity.setIsExist(1);
            }
        }

    }

    private void setPTTranslateState(TaskInfoEntity taskInfoEntity, EntryInfoEntity entryInfoEntity) {
        //写入翻译字段
        switch (taskInfoEntity.getTranslateType()) {
            case ConstantInterface.ENGLISH:
                if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(entryInfoEntity.getEnCharLength())) {
                    entryInfoEntity.setEntryLength(entryInfoEntity.getEnCharLength());
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(entryInfoEntity.getSpaCharLength())) {
                    entryInfoEntity.setEntryLength(entryInfoEntity.getSpaCharLength());
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(entryInfoEntity.getRuCharLength())) {
                    entryInfoEntity.setEntryLength(entryInfoEntity.getRuCharLength());
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(entryInfoEntity.getFraCharLength())) {
                    entryInfoEntity.setEntryLength(entryInfoEntity.getFraCharLength());
                }
                break;
        }
    }

    private void zzEntryHandle(List<ImportExcleEntry> importExcleEntries, List<EntryInfoEntity> entryEntitys, String userName, TaskInfoEntity taskInfoEntity, String fileName) {
        Date date = new Date(System.currentTimeMillis());
        for (ImportExcleEntry importExcleEntry : importExcleEntries) {

            EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
            BeanUtils.copyProperties(importExcleEntry, entryInfoEntity);
            entryInfoEntity.setAbbr(importExcleEntry.getAbbr());
            entryInfoEntity.setEntry(importExcleEntry.getEntry());
            entryInfoEntity.setEntryLength(importExcleEntry.getEntryLength());
            entryInfoEntity.setUpdate(userName);
            entryInfoEntity.setUpdateTime(date);
            entryInfoEntity.setChineseInterpretation(importExcleEntry.getChineseInterpretation());
            entryInfoEntity.setEnglishInterpretation(importExcleEntry.getEnglishInterpretation());
            entryInfoEntity.setClassfy1(importExcleEntry.getClassfy1());
            entryInfoEntity.setClassfy2(importExcleEntry.getClassfy2());
            entryInfoEntity.setProductID(taskInfoEntity.getProductId());
            entryInfoEntity.setIsDelete(0);
            entryInfoEntity.setIsPublic(0);
            entryInfoEntity.setEntryState(1);
            entryInfoEntity.setTaskId(taskInfoEntity.getProductId());
            entryInfoEntity.setId(commonUtils.getUUID());
            if (Objects.nonNull(taskInfoEntity)) {
                entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
            }
            entryInfoEntity.setImportType(ConstantInterface.EXCEL);
            entryInfoEntity.setWriteType(ConstantInterface.EXCEL);
            entryInfoEntity.setEntrySource("import : " + ConstantInterface.EXCEL + " ; fileName" + fileName);
            entryInfoEntity.setRemark(importExcleEntry.getCreator());
            String productTableName = "t_entry_info";
            caseExisttry(entryInfoEntity, taskInfoEntity, importExcleEntry, productTableName);
            entryEntitys.add(entryInfoEntity);
        }
    }

    @Override
    @Transactional
    //用is_exist 区分 已存在和新词条，已存在升级词条版本在插入
    public List<EntryInfoEntity> insertEntry(List<EntryInfoEntity> entryInfoEntities, String taskID, HttpServletRequest request) {
        long start = System.currentTimeMillis();
        int insert = 0;
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);

        TaskInfoEntity taskInfoEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

        // 过滤掉不符合条件的词条
        List<EntryInfoEntity> matchList = new ArrayList<>();
        List<EntryInfoEntity> nonMatchList = new ArrayList<>();
        List<EntryInfoEntity> updateList = new ArrayList<>();
        for(EntryInfoEntity entryInfoEntity : entryInfoEntities){
            String entry = entryInfoEntity.getEntry();
                if (entryInfoEntity.getEntryState() == 2){
                updateList.add(entryInfoEntity);
                continue;
            }
            if(entry.toCharArray().length <= 512){
                matchList.add(entryInfoEntity);
            }else{
                nonMatchList.add(entryInfoEntity);
            }
        }
        if (!updateList.isEmpty()){
           for (EntryInfoEntity entryInfoEntity : updateList) {
               entryInfoMapper.updateById(entryInfoEntity);
           }
        }
        if(!matchList.isEmpty()){
            insert += insertRelation(matchList, taskInfoEntity, department);
            if (0 == insert) {
                return entryInfoEntities;
                // return ErrorCodeList.INSERT_ERROR;
            }
            if(!nonMatchList.isEmpty()){
                return nonMatchList;
            }
            long end = System.currentTimeMillis();
            log.info(" ===== add entry number  :" + insert + " == and time : {}=== ", end - start);
            return new ArrayList<>();
        }else{
            return nonMatchList;
        }


    }

    private int insertRelation(List<EntryInfoEntity> entryInfoEntities, TaskInfoEntity taskInfoEntity, String department) {
        int insert = 0;
        List<EntryInfoEntity> entryInfoEntityList = new ArrayList<>();
        List<ProductRelationEntity> productRelationEntities = new ArrayList<>();
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            // updateWorkTrans(entryInfoEntity, department);
            if (entryInfoEntity.getEntry().length() > 512) {
                log.info(" ==== insert error : entry lenth too long : {} ======", entryInfoEntity.getEntry().length());
                continue;
            }
            int exist = 1;
            //空值代表解聚合词条，默认是新增词条
            if (Objects.isNull(entryInfoEntity.getIsExist())) {
                exist = 0;
                entryInfoEntity.setIsExist(exist);
                log.warn(" ========== id : " + entryInfoEntity.getId() + " , entry  : " + entryInfoEntity.getEntry());
            }
            //存在升级版本
            if (1 == entryInfoEntity.getIsExist()) {
                int versionNum = entryInfoMapper.getLastVersionNum(entryInfoEntity);
                entryInfoEntity.setEntryVersion(versionNum + 1);
                entryInfoEntity.setProductID(taskInfoEntity.getProductId());
            }

            String transID = setTranslate(entryInfoEntity, taskInfoEntity.getTranslateType(), department);

            //版本和任务不为空 才往关系表中写入

            ProductRelationEntity productRelationEntity = new ProductRelationEntity();
            productRelationEntity.setId(commonUtils.getUUID());
            productRelationEntity.setEntryId(entryInfoEntity.getId());
            productRelationEntity.setTaskId(taskInfoEntity.getId());
            productRelationEntity.setProductId(taskInfoEntity.getProductId());
            productRelationEntity.setVersionId(taskInfoEntity.getVersionId());
            // productRelationMapper.insert(productRelationEntity);
            // System.out.println(" =============" + entryInfoEntity.getEntry() + " =============" + entryInfoEntity.getEntry().length());
            entryInfoEntityList.add(entryInfoEntity);
            productRelationEntities.add(productRelationEntity);
            // insert += entryInfoMapper.insert(entryInfoEntity);

            if (!CollectionUtils.isEmpty(entryInfoEntity.getChildren())) {
                for (EntryInfoEntity entryInfoEntity1 : entryInfoEntity.getChildren()) {
                    entryInfoEntity1.setIsExist(0);
                    entryInfoEntity1.setEntryVersion(1);
                    entryInfoEntity1.setProductID(taskInfoEntity.getProductId());
                    if (StringUtils.isNotBlank(entryInfoEntity.getEnTransId())) {
                        entryInfoEntity1.setEnglish(entryInfoEntity.getEnglish());
                        entryInfoEntity1.setEnTransId(entryInfoEntity.getEnTransId());
                    } else if (StringUtils.isNotBlank(entryInfoEntity.getRuTransId())) {
                        entryInfoEntity1.setRuTransId(entryInfoEntity.getRuTransId());
                        entryInfoEntity1.setRussian(entryInfoEntity.getRussian());
                    } else if (StringUtils.isNotBlank(entryInfoEntity.getFraTransId())) {
                        entryInfoEntity1.setFraTransId(entryInfoEntity.getFraTransId());
                        entryInfoEntity1.setFrench(entryInfoEntity.getFrench());
                    } else if (StringUtils.isNotBlank(entryInfoEntity.getSpaTransId())) {
                        entryInfoEntity1.setSpaTransId(entryInfoEntity.getSpaTransId());
                        entryInfoEntity1.setSpanish(entryInfoEntity.getSpanish());
                    }


                    ProductRelationEntity productRelationEntity1 = new ProductRelationEntity();
                    productRelationEntity1.setId(commonUtils.getUUID());
                    productRelationEntity1.setEntryId(entryInfoEntity1.getId());
                    productRelationEntity1.setTaskId(taskInfoEntity.getId());
                    productRelationEntity1.setProductId(taskInfoEntity.getProductId());
                    productRelationEntity1.setVersionId(taskInfoEntity.getVersionId());
                    // productRelationMapper.insert(productRelationEntity1);
                    productRelationEntities.add(productRelationEntity1);
                    entryInfoEntityList.add(entryInfoEntity1);

                }
            }


        }
        productRelationMapper.insertList(productRelationEntities);

        insert += entryInfoMapper.insertEntryList(entryInfoEntityList);
        return insert;
    }

    @Override
    public String addEntryAudit(List<EntryInfoEntity> entryInfoEntities, String taskID, HttpServletRequest request) {
        return null;
    }

    @Override
    public String createVersionByEntry(List<EntryInfoEntity> entryInfoEntities, String productID, String common, String versionName, HttpServletRequest request) {
        //创建版本  更新关联表
        String id = commonUtils.getUUID();

        VersionEntity versionEntity = new VersionEntity();
        versionEntity.setId(id);
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        versionEntity.setCreator(userName);
        versionEntity.setCreateTime(new Date(System.currentTimeMillis()));
        versionEntity.setIsDelete(0);
        versionEntity.setProductId(productID);
        versionEntity.setName(versionName);
        versionEntity.setDetails(common);
        versionEntity.setTableName("t_entry_info");
        int insert = versionMapper.insert(versionEntity);

        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            ProductRelationEntity productRelationEntity = new ProductRelationEntity();
            productRelationEntity.setVersionId(id);
            productRelationEntity.setEntryId(entryInfoEntity.getId());
            productRelationEntity.setProductId(entryInfoEntity.getProductID());

            productRelationMapper.insert(productRelationEntity);

        }

        return id;
    }
    
    private List<EntryInfoEntityForExcel> postProcessEntryInfoForExport(ExcelExportVO<EntryInfoEntityForExcel> excelExportVO){
        List<EntryInfoEntityForExcel> entryInfoEntities = excelExportVO.getEntryInfoEntities();
        if(entryInfoEntities == null){
            EntryInfoEntity template = excelExportVO.getEntryInfoEntity();
            if(template != null){
                List<EntryInfoEntity> entities = entryInfoMapper.getEntryInfo(template);   
                entryInfoEntities = new ArrayList<>(entities.stream().map(EntryInfoEntityForExcel::convertFromEntryInfoEntity).collect(Collectors.toList()));
            }else{
                throw new RuntimeException("在没有提供要导出的词条的情况下没有提供要查询的词条需要符合的条件,无法获取要导出的词条");
            }
        }
        if(entryInfoEntities.isEmpty()){
            return entryInfoEntities;
        }
        Map<String,EntryClassify> entryClassifyMap = new HashMap<>();

        // 获取每一个词条对应的中文字符数和外文字符数
        List<EntryClassify> entryClassifies = entryClassifyMapper.getEntryClassfyByNames(entryInfoEntities.stream().map(EntryInfoEntityForExcel::getClassfy1).collect(Collectors.toList()));
        for(EntryClassify item : entryClassifies){
            entryClassifyMap.put(item.getTitle(), item);
        }
        for(EntryInfoEntityForExcel item : entryInfoEntities){
            String classifyName = item.getClassfy1();
            if(classifyName == null){
                continue;   // 没有一级分类，不属于模块
            }
            EntryClassify entryClassify = entryClassifyMap.get(classifyName);
            if(entryClassify != null){
                // 找到了对应模块
                Integer maxByte = entryClassify.getMaxByte();
                Integer foreignMaxByte = entryClassify.getForeignMaxByte();
                item.setMaxChineseLength(maxByte != null ? String.valueOf(maxByte) : null);
                item.setForeignMaxLength(foreignMaxByte != null ? String.valueOf(foreignMaxByte) : null);
            }
        }
        return entryInfoEntities;
    }

    @Override
    public void entryExportByCondition(ExcelExportVO<EntryInfoEntityForExcel> excelExportVO, HttpServletResponse response, String taskID) {

        List<String> columnNames = excelExportVO.getColumnNames();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmss");
        Date date = new Date();
        String da = format.format(date);
        String excelName = excelExportVO.getExcelName() + "_" + da;
        TaskInfoEntity taskEntity = null;
        if (StringUtils.isNotBlank(taskID)) {
            taskEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

        }
        List<EntryInfoEntityForExcel> entryInfoEntities = null;
        try {
            entryInfoEntities = postProcessEntryInfoForExport(excelExportVO);            
        } catch (Exception e) {
            String message = e.getMessage();
            log.error(message);
            // 不导出excel
            entryInfoEntities = new ArrayList<>();
        }

        processEntryInfo(entryInfoEntities, columnNames, response, excelName, taskEntity);


       /* for (EntryInfoEntity entryInfoEntity1 : entryInfoEntities) {
            if (Objects.isNull(taskEntity)) {
                QueryWrapper<ProductRelationEntity> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("entry_id", entryInfoEntity1.getId());
                List<ProductRelationEntity> productRelationEntities = productRelationMapper.selectList(queryWrapper);
                String versionID = productRelationEntities.get(0).getVersionId();
                String productID = productRelationEntities.get(0).getProductId();
                if (StringUtils.isNotBlank(versionID)) {
                    entryInfoEntity1.setVersionID(versionID);
                    VersionEntity versionByID = versionMapper.getVersionByID(versionID);
                    entryInfoEntity1.setVersionName(versionByID.getName());
                }
                if (StringUtils.isNotBlank(productID)) {
                    entryInfoEntity1.setProductID(productID);
                    ProductEntity productEntity = productMapper.selectById(productID);
                    entryInfoEntity1.setProductName(productEntity.getName());
                }
            } else {
                String versionID = taskEntity.getVersionId();
                if (StringUtils.isNotBlank(versionID)) {
                    entryInfoEntity1.setVersionID(versionID);
                    VersionEntity versionByID = versionMapper.getVersionByID(versionID);
                    entryInfoEntity1.setVersionName(versionByID.getName());
                }
                String productId = taskEntity.getProductId();
                if (StringUtils.isNotBlank(productId)) {
                    entryInfoEntity1.setProductID(productId);
                    ProductEntity productEntity = productMapper.selectById(productId);
                    entryInfoEntity1.setProductName(productEntity.getName());
                }
            }


        }*/
        List<String> exportFields = new ArrayList<>();
        for (String column : columnNames) {
            exportFields.add(ConstantInterface.EXCEL_LIST_NAME_MAP().get(column));

        }
        excelUtils.getWorkBook(entryInfoEntities, response, exportFields, columnNames, excelName);

    }

    private <T extends EntryInfoEntity> void processEntryInfo(List<T> entryInfoEntities, List<String> columnNames, HttpServletResponse response, String excelName, TaskInfoEntity taskEntity) {
        int numberOfThreads = 32; // Number of threads to use
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        int chunkSize = entryInfoEntities.size() / numberOfThreads;
        for (int i = 0; i < numberOfThreads; i++) {
            int start = i * chunkSize;
            int end = (i == numberOfThreads - 1) ? entryInfoEntities.size() : (i + 1) * chunkSize;
            List<T> sublist = entryInfoEntities.subList(start, end);

            new Thread(() -> {
                try {
                    // Process the sublist
                    for (EntryInfoEntity entryInfoEntity1 : sublist) {
                        // Your processing logic here
                        if (Objects.isNull(taskEntity)) {
                            QueryWrapper<ProductRelationEntity> queryWrapper = new QueryWrapper<>();
                            queryWrapper.eq("entry_id", entryInfoEntity1.getId());
                            List<ProductRelationEntity> productRelationEntities = productRelationMapper.selectList(queryWrapper);
                            String versionID = productRelationEntities.get(0).getVersionId();
                            String productID = productRelationEntities.get(0).getProductId();
                            if (StringUtils.isNotBlank(versionID)) {
                                entryInfoEntity1.setVersionID(versionID);
                                VersionEntity versionByID = versionMapper.getVersionByID(versionID);
                                entryInfoEntity1.setVersionName(versionByID.getName());
                            }
                            if (StringUtils.isNotBlank(productID)) {
                                entryInfoEntity1.setProductID(productID);
                                ProductEntity productEntity = productMapper.selectById(productID);
                                entryInfoEntity1.setProductName(productEntity.getName());
                            }
                        } else {
                            String versionID = taskEntity.getVersionId();
                            if (StringUtils.isNotBlank(versionID)) {
                                entryInfoEntity1.setVersionID(versionID);
                                VersionEntity versionByID = versionMapper.getVersionByID(versionID);
                                entryInfoEntity1.setVersionName(versionByID.getName());
                            }
                            String productId = taskEntity.getProductId();
                            if (StringUtils.isNotBlank(productId)) {
                                entryInfoEntity1.setProductID(productId);
                                ProductEntity productEntity = productMapper.selectById(productId);
                                entryInfoEntity1.setProductName(productEntity.getName());
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await(); // Wait for all threads to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }









    }

    @Override
    public List<EntryClassify> getClassfy(String parentId, String type) {
        EntryClassify entryClassify = new EntryClassify();
        entryClassify.setParentId(parentId);
        entryClassify.setType(type);
        List<EntryClassify> entryClassifies = entryClassifyMapper.getClassfy(entryClassify);
        return entryClassifies;
    }

    @Override
    public String addProductRelation(List<EntryInfoEntity> entryInfoEntities,HttpServletRequest request) {
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);

        for (EntryInfoEntity relationEntity : entryInfoEntities) {
            relationEntity.setEntryState(1);
            //插入关联表
            ProductRelationEntity productRelationEntity = new ProductRelationEntity();
            productRelationEntity.setProductId(relationEntity.getProductID());
            productRelationEntity.setEntryId(relationEntity.getId());
            productRelationEntity.setVersionId(relationEntity.getVersionID());
            productRelationEntity.setTaskId(relationEntity.getTaskId());
            int sum = productRelationMapper.checkExist(productRelationEntity);
            if (sum < 1) {
                productRelationEntity.setId(commonUtils.getUUID());
                int insert= productRelationMapper.insert(productRelationEntity);
                if (insert < ConstantInterface.DB_SUCCESS_RESULT) {
                    return ErrorCodeList.INSERT_ERROR;
                }
            }
            TranslateEntity translateEntity = new TranslateEntity();
            String transID = commonUtils.getUUID();
            translateEntity.setEntry(relationEntity.getEntry());

            //通过任务拿到语言
            TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(relationEntity.getTaskId());
            String lang = taskInfoEntity.getTranslateType();
           switch (lang){
               case ConstantInterface.ENGLISH:
                   if (StringUtils.isBlank(relationEntity.getEnglish())){
                          break;
                   }

                   translateEntity.setTranslate(relationEntity.getEnglish());

                   break;
                case ConstantInterface.RUSSIAN:
                    if (StringUtils.isBlank(relationEntity.getRussian())){
                        break;
                    }
                    translateEntity.setTranslate(relationEntity.getRussian());
                    break;
                case ConstantInterface.SPANISH:
                    if (StringUtils.isBlank(relationEntity.getSpanish())){
                        break;
                    }
                    translateEntity.setTranslate(relationEntity.getSpanish());
                    break;
                case ConstantInterface.FRENCH:
                    if (StringUtils.isBlank(relationEntity.getFrench())){
                        break;
                    }
                    translateEntity.setTranslate(relationEntity.getFrench());
                    break;
           }
            translateEntity.setTranslateState("1");
            translateEntity.setId(transID);
            translateEntity.setDeleteState(0);
            translateEntity.setVisualRange(department);
            translateEntity.setType(lang);

            translateEntity.setTranslateState("1");
            translateEntity.setId(transID);
            relationEntity.setEnTransId(transID);
            // int insert = translateMapper.insert(translateEntity);
            int insert = translateMapper.insertTranslate(translateEntity);
            if (insert < ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.INSERT_ERROR;
            }
            entryInfoMapper.updateEntryInfo(relationEntity);
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public List<EntryInfoEntity> importCommonExcle(MultipartFile multipartFile, String taskID) {
        String name = multipartFile.getOriginalFilename();
        TaskInfoEntity taskEntityByTaskID = taskInfoMapper.getTaskEntityByTaskID(taskID);
        //读取excle转换的实体
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        try {

            entryInfoEntities = excelUtils.readExcelToEntity(taskEntityByTaskID.getTranslateType(),EntryInfoEntity.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntitys = new ArrayList<>();

        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            if (StringUtils.isBlank(entryInfoEntity.getId())) {
                log.warn(" importCommonExcle **** 导入翻译词条异常, 词条 (" + entryInfoEntity.getEntry() + ")ID 为空 !!");
                continue;
            }
            if (entryInfoEntity.getEntryState() == null) {
                entryInfoEntity.setEntryState(0);
            }
            entryInfoEntity.setTaskId(taskID);

            TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(taskID);
            if (Objects.nonNull(taskInfoEntity)) {
                entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
            }
            entryInfoEntity.setEntrySource("import : " + ConstantInterface.EXCEL + "fileName" + name);
            switch (taskEntityByTaskID.getTranslateType()) {
                case ConstantInterface.ENGLISH:
                    if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
                        entryInfoEntity.setEnglishTranslateState("1");
                    }
                    break;
                case ConstantInterface.RUSSIAN:
                    if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
                        entryInfoEntity.setRussianTranslateState("1");
                    }
                    break;
                case ConstantInterface.SPANISH:
                    if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
                        entryInfoEntity.setSpanishTranslateState("1");
                    }
                    break;
                case ConstantInterface.FRENCH:
                    if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
                        entryInfoEntity.setFrenchTranslateState("1");
                    }
                    break;
            }
            entryEntitys.add(entryInfoEntity);
        }

        return entryProcessUtils.buildRepeEntry(entryEntitys, taskEntityByTaskID.getTranslateType());
    }

    @Override
    public List<EntryInfoEntity> filterSourceLanguage(List<EntryInfoEntity> entryInfoEntities, String languageType) {
        List<EntryInfoEntity> entryInfoEntityList = new ArrayList<>();
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            switch (languageType) {
                case ConstantInterface.CHINESE:
                    if (translateUtils.containsChinese(entryInfoEntity.getEntry())) {
                        entryInfoEntityList.add(entryInfoEntity);
                    }
                    break;
                case ConstantInterface.ENGLISH:
                    if (translateUtils.containsEnglish(entryInfoEntity.getEntry())) {
                        entryInfoEntityList.add(entryInfoEntity);
                    }
                    break;
            }
        }
        return entryInfoEntityList;
    }


    @Override
    public String setInfoByEntryList(List<EntryInfoEntity> entryInfoEntities,
                                     String translateType, String writeType, boolean tag, boolean comment, String fileName, String i18nUrl) {

        switch (writeType) {
            case ConstantInterface.DI:
                return diUtils.writeDiEntry(entryInfoEntities, fileName, translateType, tag, comment,i18nUrl);
            case ConstantInterface.TS:
                return tsUtils.writeTSEntry(entryInfoEntities, fileName, tag,i18nUrl,translateType);
            case ConstantInterface.DEFAUT:
                return i18nService.setInfoByEntryList(entryInfoEntities, translateType, tag, comment,i18nUrl);
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String importTransExcle(MultipartFile multipartFile, HttpServletRequest request, String transType) {

        List<EntryInfoEntity> entryEntitys = new ArrayList<>();
        String token = request.getHeader("token");
        String department = JWTTokenUtils.getDepartment(token);

        //  ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(taskInfoEntity.getProductId());

        try {
            entryEntitys = excelUtils.readExcelToEntity(transType,EntryInfoEntity.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
            if (CollectionUtils.isEmpty(entryEntitys)) {
                return null;
            }
            for (EntryInfoEntity entryInfoEntity1 : entryEntitys) {
                if (entryInfoEntity1.getEntry().equals("字节顺序")){
                    System.out.println("字节顺序");
                }
                String trans = "";
                switch (transType) {
                    case ConstantInterface.ENGLISH:
                        trans = entryInfoEntity1.getEnglish();
                        break;
                    case ConstantInterface.SPANISH:
                        trans = entryInfoEntity1.getSpanish();
                        break;
                    case ConstantInterface.RUSSIAN:
                        trans = entryInfoEntity1.getRussian();
                        break;
                    case ConstantInterface.FRENCH:
                        trans = entryInfoEntity1.getFrench();
                        break;

                }
                //获取原词条翻译id 对比翻译结果是否有变化 如果没变化 则不改变
                //有变化 在翻译新增翻译 实体
                EntryInfoEntity originalEntry = entryInfoMapper.selectById(entryInfoEntity1.getId());
                if (Objects.isNull(originalEntry)){
                    continue;
                }
                // List<TranslateEntity> repTrans = translateMapper.getRepTrans(entryInfoEntity1.getEntry(), transType, trans, department);
                List<TranslateEntity> repTrans = new LinkedList<>();
                TranslateEntity translateEntity;
                boolean isExist = false;
                if (!CollectionUtils.isEmpty(repTrans)){
                    //translateEntity使用repTrans中lastUseTime最近的
                    translateEntity = repTrans.stream().max(Comparator.comparing(TranslateEntity::getLastUseTime)).get();
                    isExist = true;
                } else {
                    translateEntity = new TranslateEntity();
                    translateEntity.setId(commonUtils.getUUID());
                    translateEntity.setEntry(entryInfoEntity1.getEntry());
                    translateEntity.setType(transType);
                    translateEntity.setTranslate(trans);
                    translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                    translateEntity.setTranslateState("1");
                    translateEntity.setDeleteState(0);
                    translateEntity.setVisualRange(department);
                    translateEntity.setPublicState(0);
                    isExist = false;
                }

                switch (transType) {
                    case ConstantInterface.ENGLISH:
                        if (StringUtils.isBlank(entryInfoEntity1.getEnglish())){
                            break;
                        }
                        if (StringUtils.isBlank(originalEntry.getEnTransId())) {
                            entryInfoEntity1.setEnTransId(translateEntity.getId());
                            entryInfoMapper.updateById(entryInfoEntity1);
                            if (isExist){
                                break;
                            }
                            // translateMapper.insert(translateEntity);
                            translateMapper.insertTranslate(translateEntity);
                            break;
                        }
                        TranslateEntity originEnTrans = translateMapper.selectById(originalEntry.getEnTransId());
                        if (Objects.isNull(originEnTrans) || !entryInfoEntity1.getEnglish().equals(originEnTrans.getTranslate())) {
                            entryInfoEntity1.setEnTransId(translateEntity.getId());
                            entryInfoMapper.updateById(entryInfoEntity1);
                            if (isExist){
                                break;
                            }
                            translateMapper.insertTranslate(translateEntity);
                            break;
                        }
                        break;
                    case ConstantInterface.SPANISH:
                        if (StringUtils.isBlank(entryInfoEntity1.getSpanish()) ){
                            break;
                        }
                        if (StringUtils.isBlank(originalEntry.getSpaTransId())) {
                            entryInfoEntity1.setSpaTransId(translateEntity.getId());
                            entryInfoMapper.updateById(entryInfoEntity1);
                            if (isExist){
                                break;
                            }
                            translateMapper.insertTranslate(translateEntity);
                            continue;
                        }
                        TranslateEntity originSpaTrans = translateMapper.selectById(originalEntry.getSpaTransId());
                        if (Objects.isNull(originSpaTrans) || !entryInfoEntity1.getSpanish().equals(originSpaTrans.getTranslate())) {
                            entryInfoEntity1.setSpaTransId(translateEntity.getId());
                            entryInfoMapper.updateById(entryInfoEntity1);
                            if (isExist){
                                break;
                            }
                            translateMapper.insertTranslate(translateEntity);
                            break;
                        }
                        break;
                    case ConstantInterface.RUSSIAN:
                        if (StringUtils.isBlank(entryInfoEntity1.getRussian())){
                            break;
                        }
                        if (StringUtils.isBlank(originalEntry.getRuTransId())) {
                            entryInfoEntity1.setRuTransId(translateEntity.getId());
                            if (isExist){
                                break;
                            }
                            translateMapper.insertTranslate(translateEntity);
                            break;
                        }
                        TranslateEntity originRuTrans = translateMapper.selectById(originalEntry.getRuTransId());
                        if (Objects.isNull(originRuTrans) ||!entryInfoEntity1.getRussian().equals(originRuTrans.getTranslate())) {
                            entryInfoEntity1.setRuTransId(translateEntity.getId());
                            if (isExist){
                                break;
                            }
                            translateMapper.insertTranslate(translateEntity);
                            break;
                        }
                        break;
                    case ConstantInterface.FRENCH:
                        if (StringUtils.isBlank(entryInfoEntity1.getFrench())){
                            break;
                        }
                        if (StringUtils.isBlank(originalEntry.getFraTransId())) {
                            entryInfoEntity1.setFraTransId(translateEntity.getId());
                            if (isExist){
                                break;
                            }
                            translateMapper.insertTranslate(translateEntity);

                            break;
                        }
                        TranslateEntity originFraTrans = translateMapper.selectById(originalEntry.getFraTransId());
                        if (Objects.isNull(originFraTrans) || !entryInfoEntity1.getFrench().equals(originFraTrans.getTranslate())) {
                            entryInfoEntity1.setFraTransId(translateEntity.getId());
                            if (isExist){
                                break;
                            }
                            translateMapper.insertTranslate(translateEntity);

                            break;
                        }
                        break;

                }
        /*

                TranslateEntity translateEntity = new TranslateEntity();
                translateEntity.setId(commonUtils.getUUID());
                translateEntity.setEntry(entryInfoEntity1.getEntry());
                translateEntity.setLastUseTime(new Date(System.currentTimeMillis()));
                translateEntity.setTranslateState("3");
                translateEntity.setDeleteState(0);
                translateEntity.setVisualRange(department);
                translateEntity.setPublicState(0);

                switch (transType) {
                    case ConstantInterface.ENGLISH:
                        if (StringUtils.isNotBlank(entryInfoEntity1.getEnglish())) {
                            String trans = entryInfoMapper.getTransByID(entryInfoEntity1.getId(), "en_trans_id");
                            //如果是空则返回
                            if (trans.equals(entryInfoEntity1.getEnglish())) {
                                continue;
                            }
                            translateEntity.setType(ConstantInterface.ENGLISH);

                            entryInfoEntity1.setEnTransId(translateEntity.getId());

                        }
                    case ConstantInterface.SPANISH:
                        if (StringUtils.isNotBlank(entryInfoEntity1.getSpanish())) {
                            String trans = entryInfoMapper.getTransByID(entryInfoEntity1.getId(), "spa_trans_id");
                            //如果是空则返回
                            if (trans.equals(entryInfoEntity1.getSpanish())) {
                                continue;
                            }
                            translateEntity.setType(ConstantInterface.SPANISH);

                            entryInfoEntity1.setSpaTransId(translateEntity.getId());

                        }
                    case ConstantInterface.FRENCH:
                        if (StringUtils.isNotBlank(entryInfoEntity1.getFrench())) {
                            String trans = entryInfoMapper.getTransByID(entryInfoEntity1.getId(), "fra_trans_id");
                            //如果是空则返回
                            if (trans.equals(entryInfoEntity1.getFrench())) {
                                continue;
                            }
                            translateEntity.setType(ConstantInterface.FRENCH);

                            entryInfoEntity1.setFraTransId(translateEntity.getId());

                        }
                    case ConstantInterface.RUSSIAN:
                        if (StringUtils.isNotBlank(entryInfoEntity1.getRussian())) {
                            String trans = entryInfoMapper.getTransByID(entryInfoEntity1.getId(), "ru_trans_id");
                            //如果是空则返回
                            if (trans.equals(entryInfoEntity1.getRussian())) {
                                continue;
                            }
                            translateEntity.setType(ConstantInterface.RUSSIAN);

                            entryInfoEntity1.setRussian(translateEntity.getId());

                        }
                }

                entryInfoMapper.updateById(entryInfoEntity1);
                translateMapper.updateById(translateEntity);*/
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return ConstantInterface.OK_STR;

    }

    @Override
    public List<EntryInfoEntity> workImportExcleTrans(MultipartFile multipartFile, String taskID) {
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(taskID);
        try {
            entryInfoEntities = excelUtils.readExcelToEntity(taskInfoEntity.getTranslateType(),EntryInfoEntity.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
            sortEmptyTrans(entryInfoEntities,taskInfoEntity.getTranslateType());

       /* TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(taskID);
        Iterator<EntryInfoEntity> iterator = entryInfoEntities.iterator();
        while (iterator.hasNext()) {
            EntryInfoEntity entryEntity = iterator.next();
            for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
                if (entryEntity.getId().equals(entryInfoEntity.getId())) {
                    switch (taskInfoEntity.getTranslateType()) {
                        case ConstantInterface.ENGLISH:
                            if (StringUtils.isNotBlank(entryEntity.getEnglish())) {
                                entryInfoEntity.setEnglish(entryEntity.getEnglish());
                            }
                            break;
                        case ConstantInterface.SPANISH:
                            if (StringUtils.isNotBlank(entryEntity.getSpanish())) {
                                entryInfoEntity.setSpanish(entryEntity.getSpanish());
                            }
                            break;
                        case ConstantInterface.RUSSIAN:
                            if (StringUtils.isNotBlank(entryEntity.getRussian())) {
                                entryInfoEntity.setRussian(entryEntity.getRussian());
                            }
                            break;
                        case ConstantInterface.FRENCH:
                            if (StringUtils.isNotBlank(entryEntity.getFrench())) {
                                entryInfoEntity.setFrench(entryEntity.getFrench());
                            }
                            break;
                    }
                    break;
                }
            }
        }*/
        } catch (Exception e) {
            log.error("导入异常 ！");
            throw new RuntimeException(e);
        }
        return entryInfoEntities;



       /*
        List<EntryInfoEntity> reTransEntries = entryInfoMapper.getReTransEntry(taskID);
        //  ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(taskInfoEntity.getProductId());
        try {
            entryEntitys = excelUtils.readExcelToEntity(EntryInfoEntity.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());



            if (CollectionUtils.isEmpty(entryEntitys)) {
                return null;
            }
            for (EntryInfoEntity entryInfoEntity1 : entryEntitys) {
                List<EntryInfoEntity> newChildren = new ArrayList<>();
                for (EntryInfoEntity childEntry : reTransEntries) {
                    //父亲设置
                    if (entryInfoEntity1.getId().equals(childEntry.getId())) {
                        if (StringUtils.isNotBlank(entryInfoEntity1.getEnglish())) {
                            childEntry.setEnglish(entryInfoEntity1.getEnglish());
                            childEntry.setEnglishTranslateState("1");
                        }
                        if (StringUtils.isNotBlank(entryInfoEntity1.getSpanish())) {
                            childEntry.setSpanish(entryInfoEntity1.getSpanish());
                            childEntry.setSpanishTranslateState("1");
                        }
                        if (StringUtils.isNotBlank(entryInfoEntity1.getRussian())) {
                            childEntry.setRussian(entryInfoEntity1.getRussian());
                            childEntry.setRussianTranslateState("1");
                        }
                        if (StringUtils.isNotBlank(entryInfoEntity1.getFrench())) {
                            childEntry.setFrench(entryInfoEntity1.getFrench());
                            childEntry.setFrenchTranslateState("1");
                        }
                        BeanUtils.copyProperties(childEntry, entryInfoEntity1);
                    }
                    //父子相认
                    if (StringUtils.isNotBlank(childEntry.getParentID()) && entryInfoEntity1.getId().equals(childEntry.getParentID())) {
                        if (StringUtils.isNotBlank(entryInfoEntity1.getEnglish())) {
                            childEntry.setEnglish(entryInfoEntity1.getEnglish());
                            childEntry.setEnglishTranslateState("1");
                        }
                        if (StringUtils.isNotBlank(entryInfoEntity1.getSpanish())) {
                            childEntry.setSpanish(entryInfoEntity1.getSpanish());
                            childEntry.setSpanishTranslateState("1");
                        }
                        if (StringUtils.isNotBlank(entryInfoEntity1.getRussian())) {
                            childEntry.setRussian(entryInfoEntity1.getRussian());
                            childEntry.setRussianTranslateState("1");
                        }
                        if (StringUtils.isNotBlank(entryInfoEntity1.getFrench())) {
                            childEntry.setFrench(entryInfoEntity1.getFrench());
                            childEntry.setFrenchTranslateState("1");
                        }
                        newChildren.add(childEntry);
                    }
                }
                entryInfoEntity1.setChildren(newChildren);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return entryEntitys;*/

    }

    @Override
    public List<EntryInfoEntity> capitalizeWords(List<EntryInfoEntity> entryInfoEntities, String changeType,String translateType) {

        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            if (StringUtils.isNotBlank(changeType) && changeType.equals(ConstantInterface.CAPITAL)) {
                switch (translateType) {
                    case ConstantInterface.ENGLISH:
                        entryInfoEntity.setEnglish(StringUtil.capitalizeWords(entryInfoEntity.getEnglish()));
                        break;
                    case ConstantInterface.RUSSIAN:
                        entryInfoEntity.setRussian(StringUtil.capitalizeWords(entryInfoEntity.getRussian()));
                        break;
                    case ConstantInterface.SPANISH:
                        entryInfoEntity.setSpanish(StringUtil.capitalizeWords(entryInfoEntity.getSpanish()));
                        break;
                    case ConstantInterface.FRENCH:
                        entryInfoEntity.setFrench(StringUtil.capitalizeWords(entryInfoEntity.getFrench()));
                        break;
                }
            }else if (StringUtils.isNotBlank(changeType) && changeType.equals(ConstantInterface.UNCAPITAL)){
                switch (translateType) {
                    case ConstantInterface.ENGLISH:
                        entryInfoEntity.setEnglish(StringUtil.uncapitalizeWords(entryInfoEntity.getEnglish()));
                        break;
                    case ConstantInterface.RUSSIAN:
                        entryInfoEntity.setRussian(StringUtil.uncapitalizeWords(entryInfoEntity.getRussian()));
                        break;
                    case ConstantInterface.SPANISH:
                        entryInfoEntity.setSpanish(StringUtil.uncapitalizeWords(entryInfoEntity.getSpanish()));
                        break;
                    case ConstantInterface.FRENCH:
                        entryInfoEntity.setFrench(StringUtil.uncapitalizeWords(entryInfoEntity.getFrench()));
                        break;
                }
            }
        }
        return entryInfoEntities;
    }

    @Override
    public List<EntryInfoEntity> replaceWords(List<EntryInfoEntity> entryInfoEntities, String sourceStr, String replaceStr, String translateType) {

        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            switch (translateType) {
                case ConstantInterface.ENGLISH:
                    entryInfoEntity.setEnglish(entryInfoEntity.getEnglish().replace(sourceStr, replaceStr));
                    break;
                case ConstantInterface.RUSSIAN:
                    entryInfoEntity.setRussian(entryInfoEntity.getRussian().replace(sourceStr, replaceStr));
                    break;
                case ConstantInterface.SPANISH:
                    entryInfoEntity.setSpanish(entryInfoEntity.getSpanish().replace(sourceStr, replaceStr));
                    break;
                case ConstantInterface.FRENCH:
                    entryInfoEntity.setFrench(entryInfoEntity.getFrench().replace(sourceStr, replaceStr));
                    break;
            }
        }

        return entryInfoEntities;
    }

    public List<EntryInfoEntity> getEntryByClassfy(EntryInfoEntity entryInfoEntityTemplate,String classfyID){
        List<String> productidList = new ArrayList<>();
        //一级分类查询
        if (StringUtils.isBlank(entryInfoEntityTemplate.getClassfy1())){
            getProductClassfyList(classfyID, productidList);
        }

      /*  if (CollectionUtils.isEmpty(productidList)){
            log.error("当前分类不存在或者没有子分类");
          return null;
        }*/
        List<EntryInfoEntity> entryInfoEntities = entryInfoMapper.getEntryByClassfy(entryInfoEntityTemplate, productidList, -1,-1);
        return entryInfoEntities;
    }

    public List<EntryInfoEntity> onPage(List<EntryInfoEntity> entryInfoEntities,Integer pageIndex,Integer pageSize){
        int offset = 0; 
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            offset = (pageIndex - 1) * pageSize; // 如果pageIndex = 1,则offset会是0
        }
        if(offset < 0){
            String errorMessage = "offset不能小于0,当前offset: " + offset;
            throw new RuntimeException(errorMessage);
        }  
        int toIndex = offset + pageSize;
        if(toIndex < offset){
            String errorMessage = "pageSize和pageIndex设定有误,当前offset: " + offset + ",toIndex: " + toIndex;
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        return entryInfoEntities.subList(offset, toIndex >= entryInfoEntities.size() ? entryInfoEntities.size() : toIndex);
    }

    public List<EntryInfoEntity> filterEntryInfoByTransInfo(List<EntryInfoEntity> entryInfoEntities,EntryInfoEntity entryInfoEntityTemplate,String translateType){

        if (CollectionUtils.isEmpty(entryInfoEntities)){
            return entryInfoEntities;
        }
        List<EntryInfoEntity> res = new LinkedList<>(); 
        // 根据翻译状态，确定是否返回改词条，如果template不提供翻译状态，则返回该词条
        if (StringUtils.isNotBlank(translateType)) {

            putEntryTransInfo(entryInfoEntities,entryInfoEntityTemplate,res,translateType);
            return res;
            // if (
            //     "0".equals(entryInfoEntityTemplate.getEnglishTranslateState()) || 
            //     "0".equals(entryInfoEntityTemplate.getRussianTranslateState()) || 
            //     "0".equals(entryInfoEntityTemplate.getSpanishTranslateState()) || 
            //     "0".equals(entryInfoEntityTemplate.getFrenchTranslateState()))
            // {
            //     return res;
            // }
        }else {
            String[] languages = {ConstantInterface.ENGLISH,ConstantInterface.FRENCH,ConstantInterface.RUSSIAN,ConstantInterface.SPANISH};
            for(String lang : languages){
                res = new LinkedList<>();
                putEntryTransInfo(entryInfoEntities, entryInfoEntityTemplate, res, lang);
                entryInfoEntities = res;
                
            }
            return res;
        }
    }


    @Deprecated
    @Override
    public List<EntryInfoEntity> getEntryByClassfy(EntryInfoEntity entryInfoEntity, String classfyID, Integer pageIndex, Integer pageSize,String translateType) {
        List<String> productidList = new ArrayList<>();
        getProductClassfyList(classfyID,productidList);
        //查询当前分类及其所有子分类,如果没有则说明当前分类是最底层分类（产品）
        if (CollectionUtils.isEmpty(productidList)){
            log.error("当前分类不存在或者没有子分类");
          return null;
        }
        int offset = 0;
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            offset = (pageIndex - 1) * pageSize;

        }
        List<EntryInfoEntity> res = new ArrayList<>();
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        if (StringUtils.isNotBlank(entryInfoEntity.getEnglishTranslateState())
                || StringUtils.isNotBlank(entryInfoEntity.getRussianTranslateState())
                || StringUtils.isNotBlank(entryInfoEntity.getSpanishTranslateState())
                || StringUtils.isNotBlank(entryInfoEntity.getFrenchTranslateState())){
             entryInfoEntities =  entryInfoMapper.getEntryByClassfy(entryInfoEntity,productidList,-1,-1);
        }else {
           entryInfoEntities =  entryInfoMapper.getEntryByClassfy(entryInfoEntity,productidList,-1,-1);
           if(pageSize >= 0){
                int lastIndex = offset + pageSize;
                entryInfoEntities = entryInfoEntities.subList(offset, lastIndex >= entryInfoEntities.size() ? entryInfoEntities.size() : lastIndex);
           }
        }

        if (CollectionUtils.isEmpty(entryInfoEntities)){
            return null;
        }

        //使用countdownlunch线程处理entryInfoEntities
        //翻译状态处理
        if (StringUtils.isNotBlank(translateType)) {


            putEntryTransInfo(entryInfoEntities,entryInfoEntity,res,translateType);
            //res 分页
             if ("0".equals(entryInfoEntity.getEnglishTranslateState())
                    || "0".equals(entryInfoEntity.getRussianTranslateState()) || "0".equals(entryInfoEntity.getSpanishTranslateState()) || "0".equals(entryInfoEntity.getFrenchTranslateState())){

                if (pageSize != -1 && res.size() > pageSize){
                    return res.subList(offset,offset+pageSize >= res.size() ? res.size() : offset + pageSize);
                }else {
                    return res;
                }
            }


        }else {
            return entryInfoEntities;
        }


    /*    for (EntryInfoEntity entryEntity : entryInfoEntities) {
            TranslateEntity translateEntity = translateMapper.selectById(entryEntity.getEnTransId());

            if (Objects.nonNull(translateEntity)) {
                entryEntity.setEnglishTranslateState(translateEntity.getTranslateState());
            }else {
                entryEntity.setEnglishTranslateState("0");
            }
            if (StringUtils.isNotBlank(entryInfoEntity.getEnglishTranslateState()) && !entryInfoEntity.getEnglishTranslateState().equals(entryEntity.getEnglishTranslateState())){
                res.remove(entryEntity);
            }
        }*/

        return res;
    }

    @Override
    public int getEntryByClassfTotal(EntryInfoEntity entryInfoEntity ,String classfyID ,String translateType) {
        List<String> productidList = new ArrayList<>();
        getProductClassfyList(classfyID,productidList);
        //查询当前分类及其所有子分类,如果没有则说明当前分类是最底层分类（产品）
        if (CollectionUtils.isEmpty(productidList)){
            log.error("当前分类不存在或者没有子分类");
            return 0;
        }
        List<EntryInfoEntity> res = new ArrayList<>();
        List<EntryInfoEntity> entryInfoEntities =  entryInfoMapper.getEntryByClassfyTotal(entryInfoEntity,productidList);

        if (CollectionUtils.isEmpty(entryInfoEntities)){
            return 0;
        }
        //使用countdownlunch线程处理entryInfoEntities
        if (StringUtils.isNotBlank(translateType)) {
            putEntryTransInfo(entryInfoEntities,entryInfoEntity,res,translateType);
        }else {
            return entryInfoEntities.size();
        }

     /*   for (EntryInfoEntity entryEntity : entryInfoEntities) {
            TranslateEntity translateEntity = translateMapper.selectById(entryEntity.getEnTransId());

            if (Objects.nonNull(translateEntity)) {
                entryEntity.setEnglishTranslateState(translateEntity.getTranslateState());
            }else {
                entryEntity.setEnglishTranslateState("0");
            }
            if (StringUtils.isNotBlank(entryInfoEntity.getEnglishTranslateState()) && !entryInfoEntity.getEnglishTranslateState().equals(entryEntity.getEnglishTranslateState())){
                res.remove(entryEntity);
            }
        }*/

        return res.size();
    }


    @Override
    public List<EntryInfoEntity> checkNotUseEntry(String i18nUrl,String classfyID, Map<String,List<String>> soucreMap) throws Exception{
    
        // List<EntryInfoEntity> entities = checkNotUseEntry(i18nUrl, classfyID, request, soucreMap,from,offset);  // 分页查询是classfyID中对应的所有词条中分页
        // return filterCheckNotUseEntryResult(entities, condition);
        return checkNotUseEntry(i18nUrl, classfyID, soucreMap,new HashMap<>());
    }   

    @Override
    public List<EntryInfoEntity> checkNotUseEntry(String i18nUrl,String classfyID, Map<String,List<String>> soucreMap,
        Map<String,String> condition) throws Exception {
        //todo:
        //1.调用i18n接口 获取ts 所有词条
        //2.调用di接口 获取di 所有词条
        //3.调用数据库 获取所有词条
        //4.对比ts di 数据库 词条
        //5.返回未使用词条
        List<String> productidList = new ArrayList<>();
        getProductClassfyList(classfyID,productidList);
        //通过关联表获取所有任务id
        List<ProductRelationEntity> productRelationEntities = productRelationMapper.selectList(new QueryWrapper<ProductRelationEntity>().in("product_id", productidList));
        Set<String> entryIdList = new HashSet<>();

        for (ProductRelationEntity productRelationEntity : productRelationEntities) {
            if (StringUtils.isBlank(productRelationEntity.getTaskId())){
                //打印详细信息
                log.warn("当前产品没有关联任务,任务id为空,产品id为:{},词条id为：{}",productRelationEntity.getProductId(),productRelationEntity.getEntryId());
                continue;
            }
            entryIdList.add(productRelationEntity.getEntryId());
        }
        if(entryIdList.isEmpty()){
            return new LinkedList<>();
        }
        
        String entry = condition.get("entry");
        String entryState = condition.get("entryState");
        String tag = condition.get("tag");
        String classfy2 = condition.get("classfy2");
        String entrySource = condition.get("entrySource");
        String translateType = condition.get("translateType");
        String translateState = condition.get("translateState");
        String translate = condition.get("translate");
        TranslateEntity translateEntityTemplate = new TranslateEntity();
        EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
        entryInfoEntity.setEntry(entry);
        if(entryState != null)
            entryInfoEntity.setEntryState(Integer.parseInt(entryState));
        entryInfoEntity.setTag(tag);
        entryInfoEntity.setClassfy2(classfy2);
        entryInfoEntity.setEntrySource(entrySource);
        translateEntityTemplate.setType(translateType);
        translateEntityTemplate.setTranslateState(translateState);
        translateEntityTemplate.setTranslate(translate);
        translateEntityTemplate.setVisualRange(condition.get("department"));
        List<EntryInfoEntity> entities = entryInfoMapper.getEntryForTaskNotUseEntry(entryIdList, condition,translateEntityTemplate,entryInfoEntity);
        return checkNotUseEntry(i18nUrl, entities, soucreMap);
    }

    @Override
    public List<EntryInfoEntity> checkNotUseEntry(String i18nUrl,List<EntryInfoEntity> entities,Map<String,List<String>> sourceMap){
        //按照导入类型分类
        if(sourceMap == null){
            sourceMap = new HashMap<>();
        }
        addDefaultSource(sourceMap);
        Map<String, List<EntryInfoEntity>> entryMap = new HashMap<>();
        for (EntryInfoEntity entity : entities) {
            if (StringUtils.isNotBlank(entity.getImportType())){
                if (entryMap.containsKey(entity.getImportType())){
                    entryMap.get(entity.getImportType()).add(entity);
                }else {
                    List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
                    entryInfoEntities.add(entity);
                    entryMap.put(entity.getImportType(),entryInfoEntities);
                }
            }
        }
        if (CollectionUtils.isEmpty(entryMap)){
            return new ArrayList<>();
        }
        List<String> exceptionMessages = new ArrayList<>();
        List<EntryInfoEntity> allEntrys = new ArrayList<>();
        try {
            List<EntryInfoEntity> tsnoUsedEntrys =  getTSNoUsedEntry(i18nUrl,entryMap.get(ConstantInterface.TS),sourceMap.get(ConstantInterface.TS),exceptionMessages);
            allEntrys.addAll(tsnoUsedEntrys);   
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        try {
            //查询di 冗余词条
            List<EntryInfoEntity> dinoUsedEntrys =  getDINoUsedEntry(i18nUrl,entryMap.get(ConstantInterface.DI),sourceMap.get(ConstantInterface.DI),exceptionMessages);   
            allEntrys.addAll(dinoUsedEntrys);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();

        }
        //查询 元数据 冗余词条 暂不开放
        // List<EntryInfoEntity> dbnoUsedEntrys = getDBNoUsedEntry(i18nUrl,entryMap.get(ConstantInterface.DB),soucreMap.get(ConstantInterface.DB));
        // try {
        //     //查询 枚举 冗余词条
        //     List<EntryInfoEntity> enumnoUsedEntrys =  getENUMNoUsedEntry(i18nUrl,entryMap.get(ConstantInterface.ENUM),sourceMap.get(ConstantInterface.ENUM));  
        //     allEntrys.addAll(enumnoUsedEntrys);
        // } catch (Exception e) {
        //     // TODO: handle exception
        //     e.printStackTrace();

        // }

        // try {
        //     //查询 配置文件 冗余词条
        //     List<EntryInfoEntity> confignoUsedEntrys =  getCONFIGNoUsedEntry(i18nUrl,entryMap.get(ConstantInterface.CONFIG),sourceMap.get(ConstantInterface.CONFIG));   
        //     allEntrys.addAll(confignoUsedEntrys);
        // } catch (Exception e) {
        //     // TODO: handle exception
        //     e.printStackTrace();
        // }

        List<EntryInfoEntity> fliteredEntryInfoEntities = allEntrys.stream().filter(new Predicate<EntryInfoEntity>() {

            Integer entryStateLow = 3;
            Integer entryStateHigh = 4;
            @Override
            public boolean test(EntryInfoEntity t) {
                // TODO Auto-generated method stub
                Integer entryState = t.getEntryState();
                return entryState >= entryStateLow && entryState <= entryStateHigh;
            }
            
        }).collect(Collectors.toList());
        return fliteredEntryInfoEntities;
    }

    private void addDefaultSource(Map<String,List<String>> sourceMap){
        List<String> tsSources = new ArrayList<>();
        tsSources.add("qt_help");
        tsSources.add("designer");
        tsSources.add("assistant");
        tsSources.add("qtquickcontrols");
        tsSources.add("qtquickcontrols2");
        tsSources.add("qtxmlpatterns");
        tsSources.add("qtbase");
        sourceMap.put(ConstantInterface.TS, tsSources);
        return;
    }

    private List<EntryInfoEntity> getCONFIGNoUsedEntry(String i18nUrl, List<EntryInfoEntity> entryInfoEntities, List<String> configList) {
        List<EntryInfoEntity> dbSourceEntrys = new ArrayList<>();
        Set<String> fileSet = new HashSet<>();
        if ( CollectionUtils.isEmpty(entryInfoEntities)){
            return new ArrayList<>();
        }
        if (CollectionUtils.isEmpty(configList)){
            configList = new ArrayList<>();
        }
        //去除白名单中的词条
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            if (StringUtils.isNotBlank(entryInfoEntity.getEntrySource())){
                if (entryInfoEntity.getImportType().equals(ConstantInterface.CONFIG) && !configList.contains(entryInfoEntity.getEntrySource())){
                    dbSourceEntrys.add(entryInfoEntity);
                    fileSet.add(entryInfoEntity.getEntrySource());
                }
            }
        }
        if(CollectionUtils.isEmpty(fileSet)){
            return new ArrayList<>();
        }
        //查找i18n 词条
        List<EntryInfoEntity> configEntry = new ArrayList<>();
        try {
            configEntry = i18nService.getCONFIGEntryBySource(i18nUrl,fileSet, "", "", "", "", "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //对比数据库词条，冗余的词条存起来
        List<EntryInfoEntity> noUsedEntrys = new ArrayList<>();
        for (EntryInfoEntity dbEntryEntity : dbSourceEntrys) {
            boolean isExist = false;
            if (StringUtils.isBlank(dbEntryEntity.getComment())){
                dbEntryEntity.setComment("");
            }
            if (StringUtils.isBlank(dbEntryEntity.getTag())){
                dbEntryEntity.setTag("");
            }
            for (EntryInfoEntity entryInfoEntity : configEntry) {
                if (StringUtils.isBlank(entryInfoEntity.getComment())){
                    entryInfoEntity.setComment("");
                }
                if (StringUtils.isBlank(entryInfoEntity.getTag())){
                    entryInfoEntity.setTag("");
                }
                if (entryInfoEntity.getEntry().equals(dbEntryEntity.getEntry())
                        && entryInfoEntity.getTag().equals(dbEntryEntity.getTag())
                        && entryInfoEntity.getComment().equals(dbEntryEntity.getComment())
                        && entryInfoEntity.getEntrySource().equals(dbEntryEntity.getEntrySource())){
                    // continue;
                    isExist = true;
                    break;
                }
                // noUsedEntrys.add(dbEntryEntity);
            }
            if(!isExist){
                noUsedEntrys.add(dbEntryEntity);
            }
        }
        return noUsedEntrys;

    }

    private List<EntryInfoEntity> getENUMNoUsedEntry(String i18nUrl, List<EntryInfoEntity> entryInfoEntities, List<String> enumList) {
        List<EntryInfoEntity> dbSourceEntrys = new ArrayList<>();
        Set<String> fileSet = new HashSet<>();
        if ( CollectionUtils.isEmpty(entryInfoEntities)){
            return new ArrayList<>();
        }
        if (CollectionUtils.isEmpty(enumList)){
            enumList = new ArrayList<>();
        }
        //去除白名单中的词条
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            if (StringUtils.isNotBlank(entryInfoEntity.getEntrySource())){
                if (entryInfoEntity.getImportType().equals(ConstantInterface.ENUM) && !enumList.contains(entryInfoEntity.getEntrySource())){
                    dbSourceEntrys.add(entryInfoEntity);
                    fileSet.add(entryInfoEntity.getEntrySource());
                }
            }
        }
        if(CollectionUtils.isEmpty(fileSet)){
            return new ArrayList<>();
        }
        //查找i18n 词条
        List<EntryInfoEntity> enumEntry = new ArrayList<>();
        try {
            enumEntry = i18nService.getENUMEntryBySource(i18nUrl,fileSet, "", "", "", "", "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //对比数据库词条，冗余的词条存起来
        List<EntryInfoEntity> noUsedEntrys = new ArrayList<>();
        for (EntryInfoEntity dbEntryEntity : dbSourceEntrys) {
            boolean isExist = false;
            if (StringUtils.isBlank(dbEntryEntity.getTag())){
                dbEntryEntity.setTag("");
            }
            if (StringUtils.isBlank(dbEntryEntity.getComment())){
                dbEntryEntity.setComment("");
            }
            for (EntryInfoEntity entryInfoEntity : enumEntry) {
                if (StringUtils.isBlank(entryInfoEntity.getComment())){
                    entryInfoEntity.setComment("");
                }
                if (StringUtils.isBlank(entryInfoEntity.getTag())){
                    entryInfoEntity.setTag("");
                }
                if (entryInfoEntity.getEntry().equals(dbEntryEntity.getEntry())
                        && entryInfoEntity.getTag().equals(dbEntryEntity.getTag())
                        && entryInfoEntity.getComment().equals(dbEntryEntity.getComment())
                        && entryInfoEntity.getEntrySource().equals(dbEntryEntity.getEntrySource())){
                    isExist = true;
                    break;
                    // continue;
                }
                // noUsedEntrys.add(dbEntryEntity);
            }
            if(!isExist){
                noUsedEntrys.add(dbEntryEntity);
            }
        }
        return noUsedEntrys;


    }

    private List<EntryInfoEntity> getDBNoUsedEntry(String i18nUrl, List<EntryInfoEntity> entryInfoEntities, List<String> dbList) {
        List<EntryInfoEntity> dbSourceEntrys = new ArrayList<>();
        Set<String> fileSet = new HashSet<>();
        //去除白名单中的词条
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            if (StringUtils.isNotBlank(entryInfoEntity.getEntrySource())){
                if (entryInfoEntity.getImportType().equals(ConstantInterface.DB) && !dbList.contains(entryInfoEntity.getEntrySource())){
                    dbSourceEntrys.add(entryInfoEntity);
                    fileSet.add(entryInfoEntity.getEntrySource());
                }
            }
        }
        if (CollectionUtils.isEmpty(fileSet)){
            return new ArrayList<>();
        }

        //查找i18n 词条
        List<EntryInfoEntity> dbEntry = new ArrayList<>();
        try {
            dbEntry = i18nService.getDBEntryBySource(i18nUrl,fileSet, "", "", "", "", "");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        //对比数据库词条，冗余的词条存起来
        List<EntryInfoEntity> noUsedEntrys = new ArrayList<>();
        for (EntryInfoEntity dbEntryEntity : dbSourceEntrys) {
            boolean isExist = false;
            for (EntryInfoEntity entryInfoEntity : dbEntry) {
                if (entryInfoEntity.getEntry().equals(dbEntryEntity.getEntry())
                        && entryInfoEntity.getTag().equals(dbEntryEntity.getTag())
                        && entryInfoEntity.getComment().equals(dbEntryEntity.getComment())
                        && entryInfoEntity.getEntrySource().equals(dbEntryEntity.getEntrySource())){
                    // continue;
                    isExist = true;
                    break;
                }
                // noUsedEntrys.add(dbEntryEntity);
            }
            if(!isExist){
                noUsedEntrys.add(dbEntryEntity);
            }
        }
        return noUsedEntrys;
    }

    private List<EntryInfoEntity> getDINoUsedEntry(String i18nUrl, List<EntryInfoEntity> entryInfoEntities, List<String> diList,List<String> exceptionMessages) {
        if(exceptionMessages == null){
            throw new NullPointerException("exceptionMessages是null");
        }
        List<EntryInfoEntity> diSourceEntrys = new ArrayList<>();
        Set<String> fileSet = new HashSet<>();
        if ( CollectionUtils.isEmpty(entryInfoEntities)){
           return new ArrayList<>();
        }
        if (CollectionUtils.isEmpty(diList)){
            diList = new ArrayList<>();
        }
        //去除白名单中的词条
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            String entrySource = entryInfoEntity.getEntrySource();
            if (StringUtils.isNotBlank(entrySource)){
                if (entryInfoEntity.getImportType().equals(ConstantInterface.DI) && !entrySource.endsWith(".xlsx") && !diList.contains(entrySource)){
                    diSourceEntrys.add(entryInfoEntity);
                    fileSet.add(entryInfoEntity.getEntrySource());
                }
            }
        }
        if (CollectionUtils.isEmpty(fileSet)){
            return new ArrayList<>();
        }

        //查找i18n 词条
        List<EntryInfoEntity> diEntry = new ArrayList<>();
        try {
            DiEntryResult diEntryResult = diUtils.getDiEntry(i18nUrl,fileSet, "", "", "", "", "");
            diEntry.addAll(diEntryResult.getResult());
            exceptionMessages.addAll(diEntryResult.getExceptionMessage());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //对比数据库词条，冗余的词条存起来
        List<EntryInfoEntity> noUsedEntrys = new ArrayList<>();

        for (EntryInfoEntity diEntryEntity : diSourceEntrys) {
            boolean isExist = false;
            for (EntryInfoEntity entryInfoEntity : diEntry) {
                if (StringUtils.isBlank(entryInfoEntity.getComment())){
                    entryInfoEntity.setComment("");
                }
                if (StringUtils.isBlank(entryInfoEntity.getTag())){
                    entryInfoEntity.setTag("");
                }
                if (StringUtils.isBlank(diEntryEntity.getTag())){
                    diEntryEntity.setTag("");
                }
                if (StringUtils.isBlank(diEntryEntity.getComment())){
                    diEntryEntity.setComment("");
                }
                if (diEntryEntity.getEntry().equals(entryInfoEntity.getEntry())
                        && diEntryEntity.getTag().equals(entryInfoEntity.getTag())
                        && diEntryEntity.getComment().equals(entryInfoEntity.getComment())
                        && diEntryEntity.getEntrySource().equals(entryInfoEntity.getEntrySource())){
                    // continue;
                    isExist = true;
                    break;
                }
                // noUsedEntrys.add(diEntryEntity);
            }
            if(!isExist){
                noUsedEntrys.add(diEntryEntity);
            }
        }
        return noUsedEntrys;
    }

    private List<EntryInfoEntity> getTSNoUsedEntry(String i18nUrl, List<EntryInfoEntity> entryInfoEntities,  List<String> tsList,List<String> exceptionMessages) {
        List<EntryInfoEntity> tsSourceEntrys = new ArrayList<>();
        Set<String> fileSet = new HashSet<>();
        if ( CollectionUtils.isEmpty(entryInfoEntities)){
            return new ArrayList<>();
        }
        if (CollectionUtils.isEmpty(tsList)){
            tsList = new ArrayList<>();
        }
        //去除白名单中的词条
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            if (StringUtils.isNotBlank(entryInfoEntity.getEntrySource())){
                if (entryInfoEntity.getImportType().equals(ConstantInterface.TS) && !tsList.contains(entryInfoEntity.getEntrySource())){
                    tsSourceEntrys.add(entryInfoEntity);
                    fileSet.add(entryInfoEntity.getEntrySource());
                }
            }
        }
        if (CollectionUtils.isEmpty(fileSet)){
            return new ArrayList<>();
        }
        // 获取数据库有,ts里面没有
        //查找i18n 词条
        List<EntryInfoEntity> i18ntsEntry = new ArrayList<>();
        try {
            // i18ntsEntry = tsUtils.getTsEntry(i18nUrl,fileSet, "", "", "", "", "英文");
            TsEntryResult tsEntryResult = tsUtils.getTsEntry(i18nUrl,fileSet, "", "", "", "", "英文");
            i18ntsEntry.addAll(tsEntryResult.getResult());
            exceptionMessages.addAll(tsEntryResult.getExceptionMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //对比数据库词条，冗余的词条存起来
        List<EntryInfoEntity> noUsedEntrys = new ArrayList<>();
        for (EntryInfoEntity tsEntryEntity : tsSourceEntrys) {
            boolean isExist = false;
            for (EntryInfoEntity entryInfoEntity : i18ntsEntry) {
                if (StringUtils.isBlank(entryInfoEntity.getComment())){
                    entryInfoEntity.setComment("");
                }
                if (StringUtils.isBlank(entryInfoEntity.getTag())){
                    entryInfoEntity.setTag("");
                }
                if (StringUtils.isBlank(tsEntryEntity.getTag())){
                    tsEntryEntity.setTag("");
                }
                if (StringUtils.isBlank(tsEntryEntity.getComment())){
                    tsEntryEntity.setComment("");
                }
                if (entryInfoEntity.getEntry().equals(tsEntryEntity.getEntry())
                        && entryInfoEntity.getTag().equals(tsEntryEntity.getTag())
                        && entryInfoEntity.getComment().equals(tsEntryEntity.getComment())
                        && entryInfoEntity.getEntrySource().equals(tsEntryEntity.getEntrySource())){
                    isExist = true;
                    break;
                    // continue;
                }
                // noUsedEntrys.add(tsEntryEntity);
            }
            if(!isExist){
                noUsedEntrys.add(tsEntryEntity);
            }
        }


        return noUsedEntrys;
    }

    @Override
    public List<TaskEntryVO> checkNewEntryByClassfy(String i18nUrl, String classfyID, HttpServletRequest request) {
        //TODO:
        //1.获取当前分类下所有任务
        //2.获取每个任务下所有词条
        //2.收集当前分类下都有哪些ts
        //3.收集当前分类下都有哪些di
        //4.调用i18n 获取所有ts词条
        //5.调用i18n 获取所有di词条
        //6.合并ts 和di的词条
        //7.对比数据库看哪些是新增的词条
        //8.返回新增词条

        String token = request.getHeader("token");

        String userName = JWTTokenUtils.getUserName(token);
        //获取当前分类下所有产品
        List<String> productidList = new ArrayList<>();
        getProductClassfyList(classfyID,productidList);
        //通过关联表获取所有任务id
        List<ProductRelationEntity> productRelationEntities = productRelationMapper.selectList(new QueryWrapper<ProductRelationEntity>().in("product_id", productidList));
        Set<String> taskIDList = new HashSet<>();
        for (ProductRelationEntity productRelationEntity : productRelationEntities) {
            if (StringUtils.isBlank(productRelationEntity.getTaskId())){
                //打印详细信息
               log.warn("当前产品没有关联任务,任务id为空,产品id为:{},词条id为：{}",productRelationEntity.getProductId(),productRelationEntity.getEntryId());
               continue;
            }
            taskIDList.add(productRelationEntity.getTaskId());
        }
        List<TaskEntryVO> taskEntryVOS = new ArrayList<>();
        //遍历任务id 获取每个任务的词条
        for (String taskID : taskIDList) {
            TaskEntryVO taskEntryVO = new TaskEntryVO();

            TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(taskID);
            List<ProductRelationEntity> productRelationEntities1 = productRelationMapper.selectList(new QueryWrapper<ProductRelationEntity>().eq("task_id", taskID));
            log.info("当前任务下的任务id为:{}",taskID);
            String productId = productRelationEntities1.get(0).getProductId();
            String versionId = productRelationEntities1.get(0).getVersionId();
            //任务只对应一个产品
            ProductEntity productEntity = productMapper.selectById(productId);
            taskEntryVO.setTaskName(taskInfoEntity.getName());
            taskEntryVO.setProductName(productEntity.getName());
            taskEntryVO.setTaskID(taskID);
            taskEntryVO.setProductID(productId);
            taskEntryVO.setTranslateType(taskInfoEntity.getTranslateType());

            List<EntryInfoEntity> entryByTaskID = entryInfoMapper.getEntryByTaskID(taskID,"t_entry_info");
            List<String> tsEntryList = new ArrayList<>();
            List<String> diEntryList = new ArrayList<>();
            Set<String> tsfileSet = new HashSet<>();
            Set<String> difileSet = new HashSet<>();
            for (EntryInfoEntity entryInfoEntity : entryByTaskID) {
                if (StringUtils.isNotBlank(entryInfoEntity.getEntrySource())){
                    if (entryInfoEntity.getEntrySource().equals(ConstantInterface.TS)){
                        tsEntryList.add(entryInfoEntity.getEntry());
                        tsfileSet.add(entryInfoEntity.getEntrySource());
                    }else if (entryInfoEntity.getEntrySource().equals(ConstantInterface.DI)){
                        diEntryList.add(entryInfoEntity.getEntry());
                        difileSet.add(entryInfoEntity.getDiFileName());
                    }
                }
            }
            //调用i18n接口获取ts词条

            List<EntryInfoEntity> tsEntry = new ArrayList<>();
            List<EntryInfoEntity> diEntry = new ArrayList<>();
            try {
                // tsEntry = tsUtils.getTsEntry(i18nUrl,tsfileSet, taskID, userName, productEntity.getId(), versionId, taskInfoEntity.getTranslateType());
                // diEntry = diUtils.getDiEntry(i18nUrl,difileSet, taskID, userName, productEntity.getId(), versionId, taskInfoEntity.getTranslateType());
                TsEntryResult tsEntryResult = tsUtils.getTsEntry(i18nUrl,tsfileSet, taskID, userName, productEntity.getId(), versionId, taskInfoEntity.getTranslateType());
                DiEntryResult diEntryResult = diUtils.getDiEntry(i18nUrl,difileSet, taskID, userName, productEntity.getId(), versionId, taskInfoEntity.getTranslateType());
                tsEntry.addAll(tsEntryResult.getResult());
                diEntry.addAll(diEntryResult.getResult());
            } catch (Exception e) {
                return null;
            }
          //合并ts和di词条
            List<EntryInfoEntity> allEntry = new ArrayList<>();
            allEntry.addAll(tsEntry);
            allEntry.addAll(diEntry);
            //对比数据库词条
            ArrayList<EntryInfoEntity> newEntry = new ArrayList<>();
            processNewEntry(allEntry, taskID, newEntry);

            List<EntryInfoEntity> entryInfoEntities = entryUtils.buildRepeTempEntry(allEntry, taskInfoEntity.getTranslateType());
            if (CollectionUtils.isEmpty(entryInfoEntities)){
                continue;
            }
            taskEntryVO.setEntities(entryInfoEntities);
            taskEntryVOS.add(taskEntryVO);

        }
        return taskEntryVOS;
    }

    private void processNewEntry(List<EntryInfoEntity> allEntry, String taskID, ArrayList<EntryInfoEntity> newEntry) {
        entryUtils.caseNewEntry(allEntry, taskID,newEntry);
    }

    private <E> void constructExceptionMessage(List<String> exceptionMessage,Map<Exception, Set<E>> exceptionCollection){
        constructExceptionMessage(exceptionMessage, exceptionCollection,null,null);
    }

    private <E> void constructExceptionMessage(List<String> exceptionMessage,Map<Exception, Set<E>> exceptionCollection,String prefix,String suffix){
        for(Map.Entry<Exception,Set<E>> entry : exceptionCollection.entrySet()){
            if(entry.getKey() instanceof i18nServerConnectException){
                exceptionMessage.add("i18n服务连接异常, 检测i18n服务是否开启");
            }else{
                exceptionMessage.add((prefix != null ? prefix : "") + entry.getKey().toString() + ": " + entry.getValue().toString() + (suffix != null ? suffix : ""));
            }
        }
        return;
    }

    @Override
    public  List<SourceEntryVO> getEntrysourceListByClassfy(String classfyID,String i18nUrl, HttpServletRequest request,List<String> exceptionMessage) {
        //TODO:
        //1.获取当前分类下所有任务
        //2.获取每个任务下所有词条
        //2.收集当前分类下都有哪些ts
        //3.收集当前分类下都有哪些di
        //4.调用i18n 获取所有ts词条
        //5.调用i18n 获取所有di词条
        //6.合并ts 和di的词条
        //7.对比数据库看哪些是新增的词条
        //8.返回新增词条
        if(exceptionMessage == null){
            throw new NullPointerException("exceptionMessage为null");
        }
        String token = request.getHeader("token");

        String userName = JWTTokenUtils.getUserName(token);
        //获取当前分类下所有产品
        List<String> productidList = new ArrayList<>();
        getProductClassfyList(classfyID,productidList);
        //通过关联表获取所有任务id
        List<ProductRelationEntity> productRelationEntities = productRelationMapper.selectList(new QueryWrapper<ProductRelationEntity>().in("product_id", productidList));
        Set<String> taskIDList = new HashSet<>();
        for (ProductRelationEntity productRelationEntity : productRelationEntities) {
            if (StringUtils.isBlank(productRelationEntity.getTaskId())){
                //打印详细信息
                log.warn("当前产品没有关联任务,任务id为空,产品id为:{},词条id为：{}",productRelationEntity.getProductId(),productRelationEntity.getEntryId());
                continue;
            }
            taskIDList.add(productRelationEntity.getTaskId());
        }
        List<TaskEntryVO> taskEntryVOS = new ArrayList<>();
        Set<String> allTSfileSet = new HashSet<>();
        Set<String> allDIfileSet = new HashSet<>();
        Set<String> allENUMfileSet = new HashSet<>();
        Set<String> allCONFIGfileSet = new HashSet<>();
        Set<String> allDBfileSet = new HashSet<>();

        //遍历任务id 获取每个任务的词条
        for (String taskID : taskIDList) {
            TaskEntryVO taskEntryVO = new TaskEntryVO();

            TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(taskID);
            if ("6".equals(taskInfoEntity.getState())){
                continue;
            }
            List<ProductRelationEntity> productRelationEntities1 = productRelationMapper.selectList(new QueryWrapper<ProductRelationEntity>().eq("task_id", taskID));
            log.info("当前任务下的任务id为:{}",taskID);
            String productId = productRelationEntities1.get(0).getProductId();
            String versionId = productRelationEntities1.get(0).getVersionId();
            //任务只对应一个产品
            ProductEntity productEntity = productMapper.selectById(productId);
            taskEntryVO.setTaskName(taskInfoEntity.getName());
            taskEntryVO.setProductName(productEntity.getName());
            taskEntryVO.setTaskID(taskID);
            taskEntryVO.setProductID(productId);
            taskEntryVO.setTranslateType(taskInfoEntity.getTranslateType());

            List<EntryInfoEntity> entryByTaskID = entryInfoMapper.getEntryByTaskID(taskID,"t_entry_info");
            List<String> tsEntryList = new ArrayList<>();
            List<String> diEntryList = new ArrayList<>();
            List<String> enumEntryList = new ArrayList<>();
            List<String> configEntryList = new ArrayList<>();
            List<String> dbEntryList = new ArrayList<>();
            Set<String> tsfileSet = new HashSet<>();
            Set<String> difileSet = new HashSet<>();
            Set<String> enumfileSet = new HashSet<>();
            Set<String> configfileSet = new HashSet<>();
            Set<String> dbfileSet = new HashSet<>();
            for (EntryInfoEntity entryInfoEntity : entryByTaskID) {
                if (StringUtils.isNotBlank(entryInfoEntity.getEntrySource())) {
                    if (entryInfoEntity.getImportType().equals(ConstantInterface.TS)) {
                        tsEntryList.add(entryInfoEntity.getEntry());
                        tsfileSet.add(entryInfoEntity.getEntrySource());
                    } else if (entryInfoEntity.getImportType().equals(ConstantInterface.DI)) {
                        diEntryList.add(entryInfoEntity.getEntry());
                        difileSet.add(entryInfoEntity.getEntrySource());
                    } else if (entryInfoEntity.getImportType().equals(ConstantInterface.ENUM)) {
                        enumEntryList.add(entryInfoEntity.getEntry());
                        enumfileSet.add(entryInfoEntity.getEntrySource());
                    } else if (entryInfoEntity.getImportType().equals(ConstantInterface.CONFIG)) {
                        configEntryList.add(entryInfoEntity.getEntry());
                        configfileSet.add(entryInfoEntity.getEntrySource());
                    } else if (entryInfoEntity.getImportType().equals(ConstantInterface.DB)) {
                        dbEntryList.add(entryInfoEntity.getEntry());
                        dbfileSet.add(entryInfoEntity.getEntrySource());
                    }
                }
            }
            //调用i18n接口获取ts词条
            allTSfileSet.addAll(tsfileSet);
            allDIfileSet.addAll(difileSet);
            allENUMfileSet.addAll(enumfileSet);
            allCONFIGfileSet.addAll(configfileSet);
            allDBfileSet.addAll(dbfileSet);

            List<EntryInfoEntity> tsEntry = new ArrayList<>();
            List<EntryInfoEntity> diEntry = new ArrayList<>();
            List<EntryInfoEntity> enumEntry = new ArrayList<>();
            List<EntryInfoEntity> configEntry = new ArrayList<>();
            List<EntryInfoEntity> dbEntry = new ArrayList<>();
            try {
                TsEntryResult<String> tsEntryResult = tsUtils.getTsEntry(i18nUrl,tsfileSet, taskID, userName, productEntity.getId(), versionId, taskInfoEntity.getTranslateType());  
                DiEntryResult<String> diEntryResult = diUtils.getDiEntry(i18nUrl,difileSet, taskID, userName, productEntity.getId(), versionId, taskInfoEntity.getTranslateType());
                DiEntryResult<String> enumEntryResult = diUtils.getEnumEntry(i18nUrl,enumfileSet, taskID, userName, productEntity.getId(), versionId, taskInfoEntity.getTranslateType());
                DiEntryResult<String> configEntryResult = diUtils.getConfigEntry(i18nUrl,configfileSet, taskID, userName, productEntity.getId(), versionId, taskInfoEntity.getTranslateType());
                // 存放正常获取的信息
                tsEntry.addAll(tsEntryResult.getResult());
                diEntry.addAll(diEntryResult.getResult());
                enumEntry.addAll(enumEntryResult.getResult());
                configEntry.addAll(configEntryResult.getResult());
                // 存放异常信息
                constructExceptionMessage(exceptionMessage, tsEntryResult.getExceptionCollection());
                constructExceptionMessage(exceptionMessage, diEntryResult.getExceptionCollection());
                constructExceptionMessage(exceptionMessage, enumEntryResult.getExceptionCollection());
                constructExceptionMessage(exceptionMessage, configEntryResult.getExceptionCollection());
                // tsEntry = tsUtils.getTsEntry(i18nUrl,tsfileSet, taskID, userName, productEntity.getId(), versionId, taskInfoEntity.getTranslateType());                
                // diEntry = diUtils.getDiEntry(i18nUrl,difileSet, taskID, userName, productEntity.getId(), versionId, taskInfoEntity.getTranslateType());
                // enumEntry = diUtils.getEnumEntry(i18nUrl,enumfileSet, taskID, userName, productEntity.getId(), versionId, taskInfoEntity.getTranslateType());
                // configEntry = diUtils.getConfigEntry(i18nUrl,configfileSet, taskID, userName, productEntity.getId(), versionId, taskInfoEntity.getTranslateType());

            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                // return null;
                throw new RuntimeException(e);
            }
            //合并ts和di词条
            List<EntryInfoEntity> allEntry = new ArrayList<>();
            allEntry.addAll(enumEntry);
            allEntry.addAll(tsEntry);
            allEntry.addAll(diEntry);
            allEntry.addAll(configEntry);
            //对比数据库词条
            ArrayList<EntryInfoEntity> newEntry = new ArrayList<>();
            entryUtils.caseNewEntry(allEntry, taskID,newEntry);
            List<EntryInfoEntity> entryInfoEntities = entryUtils.buildRepeTempEntry(newEntry, taskInfoEntity.getTranslateType());
            if (CollectionUtils.isEmpty(entryInfoEntities)){
                continue;
            }
            taskEntryVO.setEntities(entryInfoEntities);
            taskEntryVOS.add(taskEntryVO);
        }
        if (CollectionUtils.isEmpty(taskEntryVOS) ){
            return new ArrayList<>();
        }
        List<SourceEntryVO> sourceList = new ArrayList<>();
        List<SourceFileAndEntryVO> sourceTSFileAndEntryVOS = reduceSourceFileAndEntryVO(allTSfileSet, taskEntryVOS);
        List<SourceFileAndEntryVO> sourceDIFileAndEntryVOS = reduceSourceFileAndEntryVO(allDIfileSet, taskEntryVOS);
        List<SourceFileAndEntryVO> sourceENUMFileAndEntryVOS = reduceSourceFileAndEntryVO(allENUMfileSet, taskEntryVOS);
        List<SourceFileAndEntryVO> sourceCONFIGFileAndEntryVOS = reduceSourceFileAndEntryVO(allCONFIGfileSet, taskEntryVOS);
        List<SourceFileAndEntryVO> sourceDBFileAndEntryVOS = reduceSourceFileAndEntryVO(allDBfileSet, taskEntryVOS);

        if (!CollectionUtils.isEmpty(sourceTSFileAndEntryVOS)){
            SourceEntryVO sourceEntryVO = new SourceEntryVO();
            sourceEntryVO.setType(ConstantInterface.TS);
            sourceEntryVO.setSourceFileAndEntryVO(sourceTSFileAndEntryVOS);
            sourceList.add(sourceEntryVO);
        }
        if (!CollectionUtils.isEmpty(sourceDIFileAndEntryVOS)){
            SourceEntryVO sourceEntryVO = new SourceEntryVO();
            sourceEntryVO.setType(ConstantInterface.DI);
            sourceEntryVO.setSourceFileAndEntryVO(sourceDIFileAndEntryVOS);
            sourceList.add(sourceEntryVO);
        }
        if (!CollectionUtils.isEmpty(sourceENUMFileAndEntryVOS)){
            SourceEntryVO sourceEntryVO = new SourceEntryVO();
            sourceEntryVO.setType(ConstantInterface.ENUM);
            sourceEntryVO.setSourceFileAndEntryVO(sourceENUMFileAndEntryVOS);
            sourceList.add(sourceEntryVO);
        }
        if (!CollectionUtils.isEmpty(sourceCONFIGFileAndEntryVOS)){
            SourceEntryVO sourceEntryVO = new SourceEntryVO();
            sourceEntryVO.setType(ConstantInterface.CONFIG);
            sourceEntryVO.setSourceFileAndEntryVO(sourceCONFIGFileAndEntryVOS);
            sourceList.add(sourceEntryVO);
        }


        return sourceList;
    }

    private   List<SourceFileAndEntryVO> reduceSourceFileAndEntryVO(Set<String> fileSet, List<TaskEntryVO> taskEntryVOS ) {
        List<SourceFileAndEntryVO> sourceFileAndEntryVOS = new ArrayList<>();
        for (String tsFileName : fileSet) {
            if (StringUtils.isBlank(tsFileName)){
                continue;
            }
            SourceFileAndEntryVO sourceFileAndEntryVO = new SourceFileAndEntryVO();
            sourceFileAndEntryVO.setSourceFile(tsFileName);
            for (TaskEntryVO taskEntryVO : taskEntryVOS) {
                TaskEntryVO taskEntryVO1 = new TaskEntryVO();
                taskEntryVO1.setProductID(taskEntryVO.getProductID());
                taskEntryVO1.setTaskID(taskEntryVO.getTaskID());
                taskEntryVO1.setTaskName(taskEntryVO.getTaskName());
                taskEntryVO1.setTranslateType(taskEntryVO.getTranslateType());
                taskEntryVO1.setProductName(taskEntryVO.getProductName());
                for (EntryInfoEntity entity : taskEntryVO.getEntities()) {
                    if (tsFileName.equals(entity.getEntrySource())){

                        if (CollectionUtils.isEmpty(taskEntryVO1.getEntities())){
                            List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
                            entryInfoEntities.add(entity);
                            taskEntryVO1.setEntities(entryInfoEntities);
                        }else {
                            taskEntryVO1.getEntities().add(entity);
                        }

                    }
                }
                if (CollectionUtils.isEmpty(taskEntryVO1.getEntities())) {
                    continue;
                }

                if (CollectionUtils.isEmpty(sourceFileAndEntryVO.getTaskEntryVOList())){
                    List<TaskEntryVO> taskEntryVOS1 = new ArrayList<>();
                    taskEntryVOS1.add(taskEntryVO1);
                    sourceFileAndEntryVO.setTaskEntryVOList(taskEntryVOS1);
                }else{
                    sourceFileAndEntryVO.getTaskEntryVOList().add(taskEntryVO1);
                }
            }
            if (CollectionUtils.isEmpty(sourceFileAndEntryVO.getTaskEntryVOList())){
                continue;
            }
            sourceFileAndEntryVOS.add(sourceFileAndEntryVO);
        }
        return sourceFileAndEntryVOS;
    }

    @Override
    public void doWeight() {
        //获取所有entry 查询 entry,tag,entryt_source,en_trans_id，write_type ,di_fileName 相同的词条
        List<EntryInfoEntity> entryInfoEntities = entryInfoMapper.selectList(new QueryWrapper<EntryInfoEntity>().eq("is_delete", 0).eq("write_type", "DI"));
        Map<String,List<EntryInfoEntity>> entryMap = new HashMap<>();
        ArrayList<EntryInfoEntity> repeEntrys = new ArrayList<>();
        ArrayList<String> deleteID = new ArrayList<>();
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            String key = entryInfoEntity.getUpdate() + "/" + entryInfoEntity.getEntry() + "/"
                    + entryInfoEntity.getEntrySource() + "/" + entryInfoEntity.getEnTransId() + "/" + entryInfoEntity.getDiFileName() + "/" + entryInfoEntity.getTag();
            //如果遇到重复的词条
            if (!CollectionUtils.isEmpty(entryMap.get(key))) {
                deleteID.add(entryInfoEntity.getId());
             /*   entryInfoMapper.deleteById(entryInfoEntity.getId());

                productRelationMapper.delete(new QueryWrapper<ProductRelationEntity>().eq("entry_id",entryInfoEntity.getId()));*/
                log.warn("当前词条重复,已删除词条,词条id为:{},key为：{}",entryInfoEntity.getId(),key);
                entryMap.get(key).add(entryInfoEntity);
                repeEntrys.add(entryInfoEntity);
            }else {
                ArrayList<EntryInfoEntity> entities = new ArrayList<>();
                entities.add(entryInfoEntity);
                entryMap.put(key,entities);
            }
        }
        productRelationMapper.deleteByEntryID(deleteID);
        entryInfoMapper.deleteByIdList(deleteID,"t_entry_info");
        int a = 0;

    }

    @Override
    public  List<String> updateEntryByEntrySource(List<SourceEntryVO> sourceEntryVOS,HttpServletRequest request) {
        //处理任务  关闭的任务开启，删除的任务跳过
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);

        Set<String> taskSet = new HashSet<>();
        for (SourceEntryVO sourceEntryVO : sourceEntryVOS){
            sourceEntryVO.getSourceFileAndEntryVO().forEach(sourceFileAndEntryVO -> {
                sourceFileAndEntryVO.getTaskEntryVOList().forEach(taskEntryVO -> {
                    taskSet.add(taskEntryVO.getTaskID());
                });
            });
        }
        List<String> taskIdList = taskSet.stream().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(taskIdList)){
            return null;
        }
        List<String> taskNames = new ArrayList<>();
        List<TaskInfoEntity> taskInfoEntities = taskInfoMapper.getTaskByIDList(taskIdList);
        for (TaskInfoEntity taskInfoEntity : taskInfoEntities) {
            //已归档的任务跳过
            if (taskInfoEntity.getState().equals("6")){
               continue;
            }
            if (0 == taskInfoEntity.getIsDelete()){
                taskInfoMapper.updateById(taskInfoEntity);
                taskNames.add(taskInfoEntity.getName());
            }else {
                taskSet.remove(taskInfoEntity.getId());
            }
        }

        for (SourceEntryVO sourceEntryVO : sourceEntryVOS){
            List<SourceFileAndEntryVO> sourceFileAndEntryVO = sourceEntryVO.getSourceFileAndEntryVO();
            for (SourceFileAndEntryVO fileAndEntryVO : sourceFileAndEntryVO) {
                List<TaskEntryVO> taskEntryVOList = fileAndEntryVO.getTaskEntryVOList();
                if (CollectionUtils.isEmpty(taskEntryVOList)){
                    continue;
                }
                for (TaskEntryVO taskEntryVO : taskEntryVOList) {
                    //如果taskEntryVO.getTaskID() 不在taskSet 内则跳过
                    if (!taskSet.contains(taskEntryVO.getTaskID())){
                        continue;
                    }
                    List<ProductRelationEntity> productRelationEntities = new ArrayList<>();
                    List<EntryInfoEntity> entities = taskEntryVO.getEntities();
                    if (CollectionUtils.isEmpty(entities)){
                        continue;
                    }
                    TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(taskEntryVO.getTaskID());
                    insertRelation(entities,taskInfoEntity ,department);
               /*     for (EntryInfoEntity entity : entities) {
                        //entity.setEntrySource(sourceEntryVO.getType());
                        ProductRelationEntity productRelationEntity = new ProductRelationEntity();
                        productRelationEntity.setId(commonUtils.getUUID());
                        productRelationEntity.setEntryId(entity.getId());
                        productRelationEntity.setProductId(taskEntryVO.getProductID());
                        productRelationEntity.setTaskId(taskEntryVO.getTaskID());
                        productRelationEntity.setVersionId(taskEntryVO.getTranslateType());
                        productRelationEntities.add(productRelationEntity);
                    }
                    productRelationMapper.insertList(productRelationEntities);
                    entryInfoMapper.insertEntryList(entities);*/
                }

            }

        }

        //taskNames 去重
        taskNames = taskNames.stream().distinct().collect(Collectors.toList());
        return taskNames;
    }

    /**
     * 1、如果template中翻译状态是null,则返回entryInfoEntities ---->end(添加)
     * 2、如果list中的entitiy的TransID是null，设定entryInfo的翻译状态为0
     * 3、如果list中的entitiy的TransID不是null,根据entity的transID在t_translate表中找到对应的记录
     *      a、如果没找到，则设定entryInfo的翻译状态为0
     *      b、如果找到了，则设定entryInfo对应语言的翻译状态为t_translate表中对应记录的状态
     * 4、如果template中的翻译状态和词条对应的语言的翻译状态不同，则不添加到res里面
     *                          
     * @param entryInfoEntities
     * @param entryInfoEntity
     * @param res   存放满足条件的词条
     * @param translateType
     */
    private void putEntryTransInfo(List<EntryInfoEntity> entryInfoEntities, EntryInfoEntity entryInfoEntity, List<EntryInfoEntity> res,String translateType) {
            /*
             * 添加一个锁，保证线程安全
             */
            ReentrantLock lock = new ReentrantLock();
            int numberOfThreads = 64; // Number of threads to use
            CountDownLatch latch = new CountDownLatch(numberOfThreads);

            int chunkSize = entryInfoEntities.size() / numberOfThreads;
            for (int i = 0; i < numberOfThreads; i++) {
                int start = i * chunkSize;
                int end = (i == numberOfThreads - 1) ? entryInfoEntities.size() : (i + 1) * chunkSize;
                List<EntryInfoEntity> sublist = entryInfoEntities.subList(start, end);

                new Thread(() -> {
                    List<EntryInfoEntity> collection = new LinkedList<>();
                    try {
                        // Process the sublist
                        for (EntryInfoEntity entryEntity : sublist) {
                            TranslateEntity translateEntity;
                            // Your processing logic here
                            switch (translateType) {
                                case ConstantInterface.ENGLISH:
                                    if (StringUtils.isBlank(entryInfoEntity.getEnglishTranslateState())){
                                        // res.add(entryEntity);
                                        collection.add(entryEntity);
                                        continue;
                                    }
                                    if (StringUtils.isBlank(entryEntity.getEnTransId())) {
                                        entryEntity.setEnglishTranslateState("0");
                                    } else {
                                        translateEntity = translateMapper.selectById(entryEntity.getEnTransId());
                                        if (Objects.nonNull(translateEntity)) {
                                            entryEntity.setEnglishTranslateState(translateEntity.getTranslateState());
                                        } else {
                                            entryEntity.setEnglishTranslateState("0");
                                        }
                                    }
                                    if ( entryInfoEntity.getEnglishTranslateState().equals(entryEntity.getEnglishTranslateState())) {
                                        // res.add(entryEntity);
                                        collection.add(entryEntity);
                                    }
                                    break;
                                case ConstantInterface.RUSSIAN:
                                    if (StringUtils.isBlank(entryInfoEntity.getRussianTranslateState())){
                                        // res.add(entryEntity);
                                        collection.add(entryEntity);
                                        continue;
                                    }
                                    if (StringUtils.isBlank(entryEntity.getRuTransId())) {
                                        entryEntity.setRussianTranslateState("0");
                                    } else {
                                        translateEntity = translateMapper.selectById(entryEntity.getRuTransId());
                                        if (Objects.nonNull(translateEntity)) {
                                            entryEntity.setRussianTranslateState(translateEntity.getTranslateState());
                                        } else {
                                            entryEntity.setRussianTranslateState("0");
                                        }
                                    }
                                    if (entryInfoEntity.getRussianTranslateState().equals(entryEntity.getRussianTranslateState())) {
                                        // res.add(entryEntity);
                                        collection.add(entryEntity);
                                    }
                                    break;
                                case ConstantInterface.SPANISH:
                                    if (StringUtils.isBlank(entryInfoEntity.getSpanishTranslateState())){
                                        // res.add(entryEntity);
                                        collection.add(entryEntity);
                                        continue;
                                    }
                                    if (StringUtils.isBlank(entryEntity.getSpaTransId())) {
                                        entryEntity.setSpanishTranslateState("0");
                                    } else {
                                        translateEntity = translateMapper.selectById(entryEntity.getSpaTransId());
                                        if (Objects.nonNull(translateEntity)) {
                                            entryEntity.setSpanishTranslateState(translateEntity.getTranslateState());
                                        } else {
                                            entryEntity.setSpanishTranslateState("0");
                                        }
                                    }
                                    if (entryInfoEntity.getSpanishTranslateState().equals(entryEntity.getSpanishTranslateState())) {
                                        // res.add(entryEntity);
                                        collection.add(entryEntity);
                                    }
                                    break;
                                case ConstantInterface.FRENCH:
                                    if (StringUtils.isBlank(entryInfoEntity.getFrenchTranslateState())){
                                        // res.add(entryEntity);
                                        collection.add(entryEntity);
                                        continue;
                                    }
                                    if (StringUtils.isBlank(entryEntity.getFraTransId())) {
                                        entryEntity.setFrenchTranslateState("0");
                                    } else {
                                        translateEntity = translateMapper.selectById(entryEntity.getFraTransId());
                                        if (Objects.nonNull(translateEntity)) {
                                            entryEntity.setFrenchTranslateState(translateEntity.getTranslateState());
                                        } else {
                                            entryEntity.setFrenchTranslateState("0");
                                        }
                                    }
                                    if (entryInfoEntity.getFrenchTranslateState().equals(entryEntity.getFrenchTranslateState())) {
                                        // res.add(entryEntity);
                                        collection.add(entryEntity);
                                    }
                                    break;
                            }
                        }
                        try {
                            lock.lock();
                            res.addAll(collection);
                        } catch (Exception e) {
                            // TODO: handle exception
                            e.printStackTrace();
                        } finally{
                            lock.unlock();
                        }
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            try {
                latch.await(); // Wait for all threads to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

    }

    private List<String>    getProductClassfyList(String classfyID, List<String> productidList) {

        ArrayList<String> classfyList = new ArrayList<>();
        classfyList.add(classfyID);
        List<EntryClassify> entryClassfyByParentId = entryClassifyMapper.getEntryClassfyByParentId(classfyList);
        if (CollectionUtils.isEmpty(entryClassfyByParentId)) {
            productidList.add(classfyID);
        } else {
            for (EntryClassify entryClassify : entryClassfyByParentId) {
                if (StringUtils.isBlank(entryClassify.getType()) && entryClassify.getType().equals("module")) {
                    continue;
                }
                getProductClassfyList(entryClassify.getKey(), productidList);
            }
        }
        return productidList;
    }

    private void sortEmptyTrans(List<EntryInfoEntity> entryInfoEntityList, String translateType) {
        switch (translateType){
            case ConstantInterface.ENGLISH:
                entryInfoEntityList.sort(Comparator.comparing(EntryInfoEntity::getEnglish, Comparator.nullsFirst(Comparator.naturalOrder())));
                break;
            case ConstantInterface.RUSSIAN:
                entryInfoEntityList.sort(Comparator.comparing(EntryInfoEntity::getRussian, Comparator.nullsFirst(Comparator.naturalOrder())));
                break;
            case ConstantInterface.FRENCH:
                entryInfoEntityList.sort(Comparator.comparing(EntryInfoEntity::getFrench, Comparator.nullsFirst(Comparator.naturalOrder())));
                break;
            case ConstantInterface.SPANISH:
                entryInfoEntityList.sort(Comparator.comparing(EntryInfoEntity::getSpanish, Comparator.nullsFirst(Comparator.naturalOrder())));
                break;
        }
    }

    private String getTransByType(EntryInfoEntity entryInfoEntity, String translateType) {
        String transState = "";
        String trans = "";
        switch (translateType) {
            case ConstantInterface.ENGLISH:
                transState = entryInfoEntity.getEnglishTranslateState();
                trans = entryInfoEntity.getEnglish();
                break;
            case ConstantInterface.RUSSIAN:
                transState = entryInfoEntity.getRussianTranslateState();
                trans = entryInfoEntity.getRussian();
                break;
            case ConstantInterface.SPANISH:
                transState = entryInfoEntity.getSpanishTranslateState();
                trans = entryInfoEntity.getSpanish();
                break;
            case ConstantInterface.FRENCH:
                transState = entryInfoEntity.getFrenchTranslateState();
                trans = entryInfoEntity.getFrench();
                break;
        }
        if (!"3".equals(transState)) {
            return "";
        }
        return trans;
    }


    private String setTranslate(EntryInfoEntity entryInfoEntity, String translateType, String department) {
        String translate = "";
        Integer translateLength = null;
        String translateID = commonUtils.getUUID();
        switch (translateType) {
            case ConstantInterface.ENGLISH:
                translate = entryInfoEntity.getEnglish();
                if (StringUtils.isBlank(translate)) {
                    return "";
                }
                translateLength = entryInfoEntity.getEnCharLength();
                entryInfoEntity.setEnTransId(translateID);

                break;
            case ConstantInterface.RUSSIAN:
                translate = entryInfoEntity.getRussian();
                if (StringUtils.isBlank(translate)) {
                    return "";
                }
                translateLength = entryInfoEntity.getRuCharLength();
                entryInfoEntity.setRuTransId(translateID);
                break;
            case ConstantInterface.SPANISH:
                translate = entryInfoEntity.getSpanish();
                if (StringUtils.isBlank(translate)) {
                    return "";
                }
                translateLength = entryInfoEntity.getSpaCharLength();
                entryInfoEntity.setSpaTransId(translateID);
                break;
            case ConstantInterface.FRENCH:
                translate = entryInfoEntity.getSpanish();
                if (StringUtils.isBlank(translate)) {
                    return "";
                }
                translateLength = entryInfoEntity.getSpaCharLength();
                entryInfoEntity.setFraTransId(translateID);
                break;

        }

        addTranslateEntity(entryInfoEntity, translateType, translate, translateLength, translateID, department);
        return translateID;

    }

    //构建写入翻译表
    private void addTranslateEntity(EntryInfoEntity entryInfoEntity,
                                    String translateType, String translate, Integer translateLength, String translateID, String department) {
        TranslateEntity translateEntity = new TranslateEntity();
        translateEntity.setEntry(entryInfoEntity.getEntry());
        translateEntity.setType(translateType);
        if (StringUtils.isNotBlank(translate)) {
            translateEntity.setTranslateState("1");
        } else {
            translateEntity.setTranslateState("0");
            return;

        }
        if (Objects.isNull(entryInfoEntity.getMaxLength())) {
            entryInfoEntity.setMaxLength(0);
        } else {
            translateEntity.setMaxLength(entryInfoEntity.getMaxLength());
        }
        translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
        translateEntity.setDeleteState(0);
        translateEntity.setPublicState(0);
        translateEntity.setVisualRange(department);
        translateEntity.setId(translateID);
        translateEntity.setTranslate(translate);
        if (Objects.isNull(translateLength)) {
            translateEntity.setCharLength(translate.length());
        }

        // translateMapper.insert(translateEntity);
        translateMapper.insertTranslate(translateEntity);

    }


    private void caseExisttry(EntryInfoEntity entryInfoEntity, TaskInfoEntity taskInfoEntity, ImportExcleEntry importExcleEntry, String productTableName) {
        // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());
        List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryList(productTableName, entryInfoEntity, taskInfoEntity.getProductId());
        if (CollectionUtils.isEmpty(entryEntities)) {
            //创建新翻译
            entryInfoEntity.setIsExist(0);
            entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
            entryInfoEntity.setEntryVersion(1);
            createNewTrans(entryInfoEntity, taskInfoEntity, importExcleEntry);
        } else {
            entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
            int maxVersion = entryEntities.stream().max(Comparator.comparing(EntryInfoEntity::getEntryVersion)).get().getEntryVersion();
            entryInfoEntity.setEntryVersion(maxVersion);
            entryInfoEntity.setIsExist(1);
        }
    }

    private void createNewTrans(EntryInfoEntity entryInfoEntity, TaskInfoEntity taskInfoEntity, ImportExcleEntry importExcleEntry) {
        //写入翻译字段
        switch (taskInfoEntity.getTranslateType()) {
            case ConstantInterface.ENGLISH:
                if (StringUtils.isNotBlank(importExcleEntry.getEnglish())) {
                    entryInfoEntity.setEnglish(importExcleEntry.getEnglish());
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(importExcleEntry.getEnCharLength())) {
                    entryInfoEntity.setEntryLength(importExcleEntry.getEnCharLength());
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isNotBlank(importExcleEntry.getSpanish())) {
                    entryInfoEntity.setSpanish(importExcleEntry.getSpanish());
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(importExcleEntry.getSpaCharLength())) {
                    entryInfoEntity.setEntryLength(importExcleEntry.getSpaCharLength());
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isNotBlank(importExcleEntry.getRussian())) {
                    entryInfoEntity.setRussian(importExcleEntry.getRussian());
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(importExcleEntry.getRuCharLength())) {
                    entryInfoEntity.setEntryLength(importExcleEntry.getRuCharLength());
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isNotBlank(importExcleEntry.getFrench())) {
                    entryInfoEntity.setFrench(importExcleEntry.getFrench());
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
                }
                if (!Objects.isNull(importExcleEntry.getFraCharLength())) {
                    entryInfoEntity.setEntryLength(importExcleEntry.getFraCharLength());
                }
                break;
        }
    }


    //检查abbr 是否重复 重复返回true
    private boolean checkAbbrRepe(EntryInfoEntity entryInfoEntity, String tableName) {
        List<EntryInfoEntity> abbrEntryInfo = new ArrayList<>();
        if (StringUtils.isNotBlank(entryInfoEntity.getAbbr())) {
            abbrEntryInfo = entryInfoMapper.getEntryByAbbr(entryInfoEntity.getAbbr(), entryInfoEntity.getVersionID(), tableName);
            //校验ABBR 重复
            if (!CollectionUtils.isEmpty(abbrEntryInfo)) {
                return true;
            }

        }
        return false;
    }

    @Override
    //读取带序号的表格
    public List<EntryInfoEntity> importExcle(MultipartFile multipartFile, String taskID) {
        String name = multipartFile.getOriginalFilename();
        TaskInfoEntity taskInfoEntity1 = taskInfoMapper.selectById(taskID);
        //读取excle转换的实体
        List<ImportExcleVO> importExcleEntries = new ArrayList<>();
        try {

            importExcleEntries = excelUtils.readExcelToEntity(taskInfoEntity1.getTranslateType(),ImportExcleVO.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntitys = new ArrayList<>();

        for (ImportExcleVO importExcleEntry : importExcleEntries) {
            EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
            BeanUtils.copyProperties(importExcleEntry, entryInfoEntity);
            entryInfoEntity.setEntryState(0);
            entryInfoEntity.setTaskId(taskID);
            entryInfoEntity.setId(commonUtils.getUUID());
            entryInfoEntity.setIsDelete(0);
            TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(taskID);
            if (Objects.nonNull(taskInfoEntity)) {
                entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
            }
            entryInfoEntity.setEntrySource("import : " + ConstantInterface.EXCEL + "fileName" + name);

            entryEntitys.add(entryInfoEntity);
        }
        return entryEntitys;
    }


    @Override
    public EntryInfoEntity addSingleEntry(EntryInfoEntity entryInfoEntity, HttpServletRequest request) {
        if (entryInfoEntity.isUpgrade()) {
            return upgradeEnrty(entryInfoEntity, request);
        } else {
            //存在词条
            List<EntryInfoEntity> entryInfos = entryInfoMapper.getExistEntryList("t_entry_info", entryInfoEntity, entryInfoEntity.getProductID());
            if (!CollectionUtils.isEmpty(entryInfos)) {
                entryInfoEntity.setEntryVersionID(entryInfos.get(0).getEntryVersionID());
                int maxVersion = entryInfos.stream().max(Comparator.comparing(EntryInfoEntity::getEntryVersion)).get().getEntryVersion();
                entryInfoEntity.setEntryVersion(maxVersion);
                entryInfoEntity.setIsExist(1);
            } else {
                entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
                entryInfoEntity.setEntryVersion(1);
                entryInfoEntity.setIsExist(0);
            }
        }


        String id = commonUtils.getUUID();
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        Date date = new Date(System.currentTimeMillis());
        entryInfoEntity.setIsDelete(0);
        entryInfoEntity.setId(id);
        entryInfoEntity.setEntrySource("ADD");
        entryInfoEntity.setIsPublic(0);
        entryInfoEntity.setEntryState(0);

        entryInfoEntity.setUpdate(userName);
        entryInfoEntity.setUpdateTime(date);


        if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
            setTranslate(entryInfoEntity, ConstantInterface.ENGLISH, department);
        } else if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
            setTranslate(entryInfoEntity, ConstantInterface.SPANISH, department);
        } else if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
            setTranslate(entryInfoEntity, ConstantInterface.FRENCH, department);
        } else if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
            setTranslate(entryInfoEntity, ConstantInterface.RUSSIAN, department);
        }


        ProductRelationEntity productRelationEntity = new ProductRelationEntity();
        productRelationEntity.setId(commonUtils.getUUID());
        productRelationEntity.setTaskId(entryInfoEntity.getTaskId());
        productRelationEntity.setVersionId(entryInfoEntity.getVersionID());
        productRelationEntity.setProductId(entryInfoEntity.getProductID());
        productRelationEntity.setEntryId(entryInfoEntity.getId());
        productRelationMapper.insert(productRelationEntity);

        int insert = entryInfoMapper.insert(entryInfoEntity);
        if (insert != 1) {
            return null;
        }
        return entryInfoEntity;
    }

    private EntryInfoEntity upgradeEnrty(EntryInfoEntity entryInfoEntity, HttpServletRequest request) {
        int lastVersionNum = entryInfoMapper.getLastVersionNum(entryInfoEntity);
        entryInfoEntity.setEntryVersion(lastVersionNum + 1);
        entryInfoEntity.setIsExist(0);

        String id = commonUtils.getUUID();
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        Date date = new Date(System.currentTimeMillis());
        entryInfoEntity.setIsDelete(0);
        entryInfoEntity.setId(id);
        entryInfoEntity.setEntrySource("UPGRADE");
        entryInfoEntity.setIsPublic(0);
        entryInfoEntity.setEntryState(0);

        entryInfoEntity.setUpdate(userName);
        entryInfoEntity.setUpdateTime(date);
        if (StringUtils.isNotBlank(entryInfoEntity.getTaskId()) || StringUtils.isNotBlank(entryInfoEntity.getVersionID())) {
            ProductRelationEntity productRelationEntity = new ProductRelationEntity();
            productRelationEntity.setId(commonUtils.getUUID());
            productRelationEntity.setTaskId(entryInfoEntity.getTaskId());
            productRelationEntity.setVersionId(entryInfoEntity.getVersionID());
            productRelationEntity.setProductId(entryInfoEntity.getProductID());
            productRelationEntity.setEntryId(entryInfoEntity.getId());
            productRelationMapper.insert(productRelationEntity);
        }
        int insert = entryInfoMapper.insert(entryInfoEntity);
        if (insert != 1) {
            return null;
        }
        return entryInfoEntity;
    }


    @Override
    public TranslateEntities translate(String name, String type, String visualRange) {

        TranslateEntities translateEntities = new TranslateEntities();
        List<Translate> translateEntityList = new ArrayList<>();
        QueryWrapper<TLanguage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", type);
        List<TLanguage> tLanguages = languageMapper.selectList(queryWrapper);
        // 百度翻译
        Translate baiduEntities = translateUtils.baiduTranslate(name, type, tLanguages);
        translateEntityList.add(baiduEntities);

        //有道翻译
        Translate youdao_Entities = translateUtils.youdaoTranslate(name, type, tLanguages);
        translateEntityList.add(youdao_Entities);

        //DeepL翻译
        Translate deepl_translate = translateUtils.deeplTranslate(name, tLanguages);
        translateEntityList.add(deepl_translate);

        //有道翻译
        Translate model_Entities = translateUtils.modelTranslate(name, type, tLanguages);
        translateEntityList.add(model_Entities);

        //本地翻译 ：部门
        List<TranslateEntity> departTranslates = translateMapper.getSuggestTrans(name, type, visualRange);
        Translate localTranslate = translateUtils.localTranslate(name, type, departTranslates);
        localTranslate.setSource("本地翻译-" + visualRange);
        translateEntityList.add(localTranslate);

        //本地翻译 publicState：公司
        List<TranslateEntity> companyTranslates = translateMapper.getSuggestTrans(name, type, "公司");
        Translate comLocalTranslate = translateUtils.localTranslate(name, type, companyTranslates);
        comLocalTranslate.setSource("本地翻译-公司");
        translateEntityList.add(comLocalTranslate);

        //本地翻译 publicState：公司
        List<TranslateEntity> qtTranslates = translateMapper.getSuggestTrans(name, type, "");
        List<TranslateEntity> qtTranslates1 = new ArrayList<>();
        for (TranslateEntity qtTranslate : qtTranslates) {
            if (!CollectionUtils.isEmpty(qtTranslates) && !qtTranslate.getVisualRange().equals(visualRange)) {
                qtTranslates1.add(qtTranslate);
            }
        }
        Translate qtLocalTranslate = translateUtils.localTranslate(name, type, qtTranslates1);
        qtLocalTranslate.setSource("本地翻译-其他");
        translateEntityList.add(qtLocalTranslate);
        translateEntities.setTranslateEntities(translateEntityList);
        return translateEntities;
    }

    @Override
    public String updateEntryTemp(List<EntryTempEntity> entryTempEntities, HttpServletRequest request) {
        String taskID = entryTempEntities.get(0).getTaskId();
        List<EntryTempEntity> entryTempByTaskID = entryTempMapper.getEntryTempByTaskID(taskID);
        //新导入词条
        for (EntryTempEntity entryTempEntity : entryTempEntities) {
            String entry = entryTempEntity.getEntry();
            //旧词条
            for (EntryTempEntity entryTempEntity1 : entryTempByTaskID) {
                if (entryTempEntity1.getEntry().equals(entry)) {
                    entryTempEntity = entryTempEntity1;

                }
            }

        }
        int delete = entryTempMapper.deleteByTaskID(taskID);
        for (EntryTempEntity entryTempEntity : entryTempEntities) {
            int insert = entryTempMapper.insert(entryTempEntity);
        }


        return ConstantInterface.OK_STR;
    }

    @Override
    public List<EntryInfoEntity> getEntryInfoByTsVo(List<TsVo> tsVoList) {
        // TODO Auto-generated method stub
        List<EntryInfoEntity> entities = new LinkedList<>();
        for(TsVo tsVo : tsVoList){
            entities.addAll(entryInfoMapper.getEntryByTsVo(tsVo));
        }
        // entityTemplate.
        return entities;
    }




}




