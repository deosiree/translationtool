package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.DO.EntryInfoEntityDO;
import com.shr.translationtoolservice.entity.vo.*;
import com.shr.translationtoolservice.entity.vo.WorkBenchVO.EntryImportFileTypeVO;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Attachments;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Issue;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO.Attachment;
import com.shr.translationtoolservice.entity.vo.check.TaskRequest;
import com.shr.translationtoolservice.entity.vo.check.TaskRule;
import com.shr.translationtoolservice.entity.vo.exception.ExceptionEntryInfoVO;
import com.shr.translationtoolservice.entity.vo.exception.ExceptionVO;
import com.shr.translationtoolservice.exception.i18nServerConnectException;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.service.EntryStorageService;
import com.shr.translationtoolservice.service.I18nService;
import com.shr.translationtoolservice.service.TranslationStorageService;
import com.shr.translationtoolservice.service.analyze.BatchMaxLengthTranslateAnalyzer;
import com.shr.translationtoolservice.service.entry.EntryInfoEntityImportHandler;
import com.shr.translationtoolservice.service.exporter.TSExporter;
import com.shr.translationtoolservice.service.processor.groupby.DefaultEntryGroupbyStrategy;
import com.shr.translationtoolservice.service.workflow.CheckFilePipeline;
import com.shr.translationtoolservice.service.workflow.CheckFilePipeline.BuildOption;
import com.shr.translationtoolservice.service.workflow.CheckFilePipeline.ExecuteOption;
import com.shr.translationtoolservice.service.workflow.node.CheckColumnExistWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CheckEntryNotMatchWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CheckMissingEntryRelationWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CheckSpecialCharacterWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CheckTranslationMaxLengthWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CheckWorkNode;
import com.shr.translationtoolservice.service.workflow.node.CompareEntityWorkNode;
import com.shr.translationtoolservice.util.*;
import com.shr.translationtoolservice.util.DiUtils.DiEntryResult;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils;
import com.shr.translationtoolservice.util.ExcelUtils.ParseFileInfo;
import com.shr.translationtoolservice.util.ExcelUtils.MethodUtils.MethodEntity;
import com.shr.translationtoolservice.util.TsUtils.TsEntryResult;

import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
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
    private I18nService i18nService;

    @Autowired
    private EntryProcessUtils entryProcessUtils;

    @Autowired
    private DiUtils diUtils;
    @Autowired
    private TsUtils tsUtils;

    @Value("${I18server.url}")
    private String I18URL;

    @Autowired
    private LocalTimeUtils localTimeUtils;

    @Autowired
    private EntryUtils entryUtils;

    @Autowired
    private EntryStorageService entryStorageService;

    @Autowired
    private TranslationStorageService translationStorageService;

    @Autowired
    private CheckFilePipeline checkFilePipeline;

    @Autowired
    private EntryInfoEntityImportHandler entryInfoEntityImportHandler;


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
        if(department.equals("装置开发部") && StringUtils.isBlank(entryInfoEntity.getClassfy2())){
            return "不允许二级分类更新为空字符串";
        }
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
        if (StringUtils.isNotBlank(entryInfoEntity.getChinese())) {
            transType = ConstantInterface.CHINESE;
            List<TranslateEntity> translateEntitys = new ArrayList<>();
            translateEntitys = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getChinese(), department);
            if (CollectionUtils.isEmpty(translateEntitys)) {
                TranslateEntity translateEntity = new TranslateEntity();
                id = commonUtils.getUUID();
                translateEntity.setId(id);
                translateEntity.setEntry(entryInfoEntity.getEntry());
                translateEntity.setVersionID(entryInfoEntity.getVersionID());
                translateEntity.setTranslate(entryInfoEntity.getChinese());
                translateEntity.setPublicState(0);
                translateEntity.setTranslateState("3");
                translateEntity.setDeleteState(0);
                translateEntity.setVisualRange(department);
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                translateEntity.setType(transType);
                // translateMapper.insert(translateEntity);
                translateMapper.insertTranslate(translateEntity);

            } else {
                TranslateEntity translateEntity = translateEntitys.stream().max(Comparator.comparing(TranslateEntity::getLastUseTime)).get();
                translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
                id = translateEntity.getId();
                translateMapper.updateById(translateEntity);
            }
            entryInfoEntity.setZhTransId(id);
        }
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
                translateEntity.setType(transType);
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
            case ConstantInterface.CHINESE:
                if (StringUtils.isBlank(translateEntities.getId())) {
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setZhTransId(transID);
                } else {
                    entryInfoEntity.setZhTransId(translateEntities.getId());
                }
                break;
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
    public Collection<EntryInfoEntity> importEntitiesFromFile(FileInputStreamEntity fileInputStreamEntitiy,String taskID,String departmentType,String encoding,HttpServletRequest request){
        String fileName = fileInputStreamEntitiy.getFileName();
        Collection<EntryInfoEntity> entryInfoEntities = null;
        try {
            String token = request.getHeader("token");
            String userName = JWTTokenUtils.getUserName(token);
            User user = new User();
            user.setDepartment(departmentType);
            user.setUserName(userName);

            TaskInfoEntity taskInfoEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);
            /* 加版本号 */
            if(fileName.endsWith(".xlsx")){
                EntryImportFileTypeVO modeType = EntryImportFileTypeVO.parse(request.getParameter("templateType"));
                // if(departmentType.equals("装置开发部")){
                //     EntryImportFileTypeVO modeType = EntryImportFileTypeVO.parse(request.getParameter("templateType"));
                //     if(modeType == null){
                //         throw new NullPointerException("传递的modeType参数不正确或为null");
                //     }
                //     if(modeType == EntryImportFileTypeVO.NEW_FILE_VERSION){
                //         throw new Exception("暂不支持,请使用其他模板");
                //         // entryInfoEntities = excelUtils.readZZExcelToEntity(taskInfoEntity.getTranslateType(),EntryInfoEntity.class, multipartFile.getInputStream(), fileName);
                //         // zzEntryHandle(entryInfoEntities, userName, taskInfoEntity, fileName);
                //     }else if(modeType == EntryImportFileTypeVO.OLD_FILE_VERSION){
                //         List<ImportExcleEntry> excelToEntity = excelUtils.readZZExcelToEntity(taskInfoEntity.getTranslateType(),ImportExcleEntry.class, fileInputStreamEntitiy.getInputStream(), fileName);
                //         entryInfoEntities = new ArrayList<>();
                //         zzEntryHandle(excelToEntity, entryInfoEntities, userName, taskInfoEntity, fileName);
                //     }else if(modeType == EntryImportFileTypeVO.COMMON_VERSION){
                //         entryInfoEntities = excelUtils.readExcelToEntity(taskInfoEntity.getTranslateType(),EntryInfoEntity.class, fileInputStreamEntitiy.getInputStream(), fileName).getParsedObjects().stream().collect(Collectors.toList());
                //         zzEntryHandle(entryInfoEntities, userName, taskInfoEntity, fileName);
                //     }else{
                //         throw new Exception("当前不支持该文件类型: " + request.getParameter("modeType"));
                //     }
                // }else{
                //     entryInfoEntities = excelUtils.readExcelToEntity(taskInfoEntity.getTranslateType(),EntryInfoEntity.class, fileInputStreamEntitiy.getInputStream(), fileName).getParsedObjects().stream().collect(Collectors.toList());
                //     ptEntryHandle(entryInfoEntities, userName, taskInfoEntity, fileName);
                // }
                entryInfoEntities = entryInfoEntityImportHandler.importExcel(fileInputStreamEntitiy, user, modeType, taskInfoEntity, encoding);
            }else if(fileName.endsWith(".csv")){
                // Map<String,String> kwargs = new HashMap<>();
                // kwargs.put("encoding", encoding);
                // entryInfoEntities = excelUtils.readCSVToEntity(EntryInfoEntity.class, fileInputStreamEntitiy.getInputStream(), fileName, kwargs).getParsedObjects().stream().collect(Collectors.toList());
                // if(departmentType.equals("装置开发部")){
                //     zzEntryHandle(entryInfoEntities, userName, taskInfoEntity, fileName);
                // }else{
                //     ptEntryHandle(entryInfoEntities, userName, taskInfoEntity, fileName);
                // }
                entryInfoEntities = entryInfoEntityImportHandler.importCSV(fileInputStreamEntitiy, user, taskInfoEntity, encoding);
            }else if(fileName.endsWith(".xml")){
                String template = request.getParameter("templateType");
                // if(departmentType.equals("装置开发部")){
                //     String template = request.getParameter("templateType");
                //     if(template == null){
                //         throw new Exception("请确定xml采用何种模板");
                //     }
                //     if(template.equals("可视化词条")){
                //         entryInfoEntities = XMLUtils.parseXML(fileInputStreamEntitiy.getInputStream(),new XMLUtils.XMLHandlerForEquipment());
                //     }else if(template.equals("装置辞典")){
                //         entryInfoEntities = XMLUtils.parseXML(fileInputStreamEntitiy.getInputStream(),new XMLUtils.XMLHandlerForEquipment2());
                //     }else{
                //         throw new Exception("当前不支持该版本的xml导入: " + template);
                //     }
                //     zzEntryHandleForXML(entryInfoEntities, userName, taskInfoEntity, fileName);
                // }else{
                //     throw new Exception("暂不支持XML导入");
                // }
                entryInfoEntities = entryInfoEntityImportHandler.importXML(fileInputStreamEntitiy, user, taskInfoEntity, template);
            }else if(fileName.endsWith(".ts")){
                entryInfoEntities = entryInfoEntityImportHandler.importTS(fileInputStreamEntitiy, user, taskInfoEntity);
            }else if(fileName.endsWith(".dic")){
                entryInfoEntities = entryInfoEntityImportHandler.importDIC(fileInputStreamEntitiy, user, taskInfoEntity, encoding);


            }else{
                throw new RuntimeException("暂时不支持文件: " + fileName + "的词条导入功能");
            }
            String diFileName = request.getParameter("diFileName");
            if(diFileName != null){
                for(EntryInfoEntity entry : entryInfoEntities){
                    entry.setDiFileName(diFileName);
                }
            }
            return entryProcessUtils.buildRepeEntry(entryInfoEntities, taskInfoEntity.getTranslateType(),departmentType);
        } catch (Exception e) {
            // TODO: handle exception
            throw new RuntimeException(e);
        } 

    }


    // private void ptEntryHandle(List<EntryInfoEntity> entryEntitys, String userName, TaskInfoEntity taskInfoEntity, String fileName) {
    //     Date date = new Date(System.currentTimeMillis());
    //     for (EntryInfoEntity entryInfoEntity : entryEntitys) {

    //         entryInfoEntity.setEntryLength(entryInfoEntity.getEntry().length());
    //         entryInfoEntity.setUpdate(userName);
    //         entryInfoEntity.setUpdateTime(date);

    //         entryInfoEntity.setIsDelete(0);
    //         entryInfoEntity.setIsPublic(0);
    //         entryInfoEntity.setEntryState(1);

    //         entryInfoEntity.setId(commonUtils.getUUID());
    //         if (Objects.nonNull(taskInfoEntity)) {
    //             entryInfoEntity.setTaskId(taskInfoEntity.getId());
    //             entryInfoEntity.setProductID(taskInfoEntity.getProductId());
    //             entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
    //         }
    //         if (fileName.contains("_db_meta_common.xlsx")) {
    //             entryInfoEntity.setWriteType(ConstantInterface.DI);
    //             String tag = entryInfoEntity.getTag();
    //             //tag 转成json
    //             entryInfoEntity.setImportType(ConstantInterface.DB_META);
    //             JSONObject jsonObject = JSONObject.parseObject(tag);
    //             String appName = jsonObject.get("appName").toString();
    //             String dbName = jsonObject.get("dbName").toString().replace(".","_");
    //             String tableName = jsonObject.get("tableName").toString();
    //             String fieldName = jsonObject.get("fieldName").toString();
    //             entryInfoEntity.setTag( tableName + "/" + fieldName);
    //             entryInfoEntity.setDiFileName("db/meta/"  +  dbName );
    //         }


    //         if (StringUtils.isBlank(entryInfoEntity.getImportType())){
    //             entryInfoEntity.setImportType(ConstantInterface.EXCEL);
    //         }
    //         if (StringUtils.isBlank(entryInfoEntity.getWriteType())){
    //             entryInfoEntity.setWriteType(ConstantInterface.DI);
    //         }

    //         if (StringUtils.isBlank(entryInfoEntity.getEntrySource())){
    //             entryInfoEntity.setEntrySource("import : " + ConstantInterface.EXCEL + " ; fileName" + fileName);
    //         }

    //         String productTableName = "t_entry_info";
    //         // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());
    //         List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryList(productTableName, entryInfoEntity, taskInfoEntity.getProductId());
    //         if (CollectionUtils.isEmpty(entryEntities)) {
    //             //创建新翻译
    //             entryInfoEntity.setIsExist(0);
    //             entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
    //             entryInfoEntity.setEntryVersion(1);
    //             setPTTranslateState(taskInfoEntity, entryInfoEntity);

    //         } else {
    //             entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
    //             entryInfoEntity.setIsExist(1);
    //         }
    //     }

    // }

    // private void setPTTranslateState(TaskInfoEntity taskInfoEntity, EntryInfoEntity entryInfoEntity) {
    //     //写入翻译字段
    //     switch (taskInfoEntity.getTranslateType()) {
    //         case ConstantInterface.CHINESE:
    //             if (StringUtils.isNotBlank(entryInfoEntity.getChinese())) {
    //                 entryInfoEntity.setChineseTranslateState(ConstantInterface.TRANSLATED);
    //             } else {
    //                 entryInfoEntity.setChineseTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             if (!Objects.isNull(entryInfoEntity.getZhCharLength())) {
    //                 entryInfoEntity.setEntryLength(entryInfoEntity.getZhCharLength());
    //             }
    //             break;
    //         case ConstantInterface.ENGLISH:
    //             if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
    //                 entryInfoEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
    //             } else {
    //                 entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             if (!Objects.isNull(entryInfoEntity.getEnCharLength())) {
    //                 entryInfoEntity.setEntryLength(entryInfoEntity.getEnCharLength());
    //             }
    //             break;
    //         case ConstantInterface.SPANISH:
    //             if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
    //                 entryInfoEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
    //             } else {
    //                 entryInfoEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             if (!Objects.isNull(entryInfoEntity.getSpaCharLength())) {
    //                 entryInfoEntity.setEntryLength(entryInfoEntity.getSpaCharLength());
    //             }
    //             break;
    //         case ConstantInterface.RUSSIAN:
    //             if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
    //                 entryInfoEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
    //             } else {
    //                 entryInfoEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             if (!Objects.isNull(entryInfoEntity.getRuCharLength())) {
    //                 entryInfoEntity.setEntryLength(entryInfoEntity.getRuCharLength());
    //             }
    //             break;
    //         case ConstantInterface.FRENCH:
    //             if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
    //                 entryInfoEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
    //             } else {
    //                 entryInfoEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             if (!Objects.isNull(entryInfoEntity.getFraCharLength())) {
    //                 entryInfoEntity.setEntryLength(entryInfoEntity.getFraCharLength());
    //             }
    //             break;
    //     }
    // }

    // private void zzEntryHandleForXML(List<EntryInfoEntity> entryEntitys, String userName, TaskInfoEntity taskInfoEntity, String fileName) {
    //     Date date = new Date(System.currentTimeMillis());
    //     for(EntryInfoEntity entryInfoEntity : entryEntitys){
    //         entryInfoEntity.setEntryLength(entryInfoEntity.getEntry().length());
    //         entryInfoEntity.setIsDelete(0);
    //         if(entryInfoEntity.getUpdate() == null || entryInfoEntity.getUpdate().equals("")){
    //             entryInfoEntity.setUpdate(userName);
    //         }
    //         entryInfoEntity.setUpdateTime(date);
    //         entryInfoEntity.setIsPublic(0);
    //         entryInfoEntity.setEntryState(1);
    //         entryInfoEntity.setId(commonUtils.getUUID());
    //         if (Objects.nonNull(taskInfoEntity)) {
    //             entryInfoEntity.setTaskId(taskInfoEntity.getId());
    //             entryInfoEntity.setProductID(taskInfoEntity.getProductId());
    //             entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
    //         }
    //         if (StringUtils.isBlank(entryInfoEntity.getImportType())){
    //             entryInfoEntity.setImportType(ConstantInterface.EXCEL);
    //         }
    //         String productTableName = "t_entry_info";
    //         caseExisttryForXML(entryInfoEntity, taskInfoEntity,  productTableName);
    //     }
    // }

    // private void zzEntryHandle(List<EntryInfoEntity> entryEntitys, String userName, TaskInfoEntity taskInfoEntity, String fileName) {
    //     Date date = new Date(System.currentTimeMillis());
    //     for(EntryInfoEntity entryInfoEntity : entryEntitys){
    //         entryInfoEntity.setEntryLength(entryInfoEntity.getEntry().length());
    //         entryInfoEntity.setIsDelete(0);
    //         if(entryInfoEntity.getUpdate() == null || entryInfoEntity.getUpdate().equals("")){
    //             entryInfoEntity.setUpdate(userName);
    //         }
    //         entryInfoEntity.setUpdateTime(date);
    //         entryInfoEntity.setIsPublic(0);
    //         entryInfoEntity.setEntryState(1);
    //         entryInfoEntity.setId(commonUtils.getUUID());
    //         if (Objects.nonNull(taskInfoEntity)) {
    //             entryInfoEntity.setTaskId(taskInfoEntity.getId());
    //             entryInfoEntity.setProductID(taskInfoEntity.getProductId());
    //             entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
    //         }
    //         if (StringUtils.isBlank(entryInfoEntity.getImportType())){
    //             entryInfoEntity.setImportType(ConstantInterface.EXCEL);
    //         }
    //         String productTableName = "t_entry_info";
    //         caseExisttry(entryInfoEntity, taskInfoEntity,  productTableName);
    //     }
    // }

    // private void zzEntryHandle(List<ImportExcleEntry> importExcleEntries, List<EntryInfoEntity> entryEntitys, String userName, TaskInfoEntity taskInfoEntity, String fileName) {
    //     Date date = new Date(System.currentTimeMillis());
    //     for (ImportExcleEntry importExcleEntry : importExcleEntries) {

    //         EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
    //         BeanUtils.copyProperties(importExcleEntry, entryInfoEntity);
    //         entryInfoEntity.setAbbr(importExcleEntry.getAbbr());
    //         entryInfoEntity.setEntry(importExcleEntry.getAbbr());
    //         entryInfoEntity.setEntryLength(importExcleEntry.getAbbr().length());
    //         entryInfoEntity.setUpdate(userName);
    //         entryInfoEntity.setUpdateTime(date);
    //         entryInfoEntity.setChineseInterpretation(importExcleEntry.getChineseInterpretation());
    //         entryInfoEntity.setEnglishInterpretation(importExcleEntry.getEnglishInterpretation());
    //         entryInfoEntity.setClassfy1(importExcleEntry.getClassfy1());
    //         entryInfoEntity.setClassfy2(importExcleEntry.getClassfy2());
    //         entryInfoEntity.setProductID(taskInfoEntity.getProductId());
    //         entryInfoEntity.setIsDelete(0);
    //         entryInfoEntity.setIsPublic(0);
    //         entryInfoEntity.setEntryState(1);
    //         entryInfoEntity.setTaskId(taskInfoEntity.getProductId());
    //         entryInfoEntity.setId(commonUtils.getUUID());
    //         if (Objects.nonNull(taskInfoEntity)) {
    //             entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
    //         }
    //         entryInfoEntity.setImportType(ConstantInterface.EXCEL);
    //         entryInfoEntity.setWriteType(ConstantInterface.EXCEL);
    //         // entryInfoEntity.setEntrySource("import : " + ConstantInterface.EXCEL + " ; fileName" + fileName);
    //         entryInfoEntity.setRemark(importExcleEntry.getCreator());
    //         String productTableName = "t_entry_info";
    //         caseExisttry(entryInfoEntity, taskInfoEntity, importExcleEntry, productTableName);
    //         entryEntitys.add(entryInfoEntity);
    //     }
    // }

    @Override
    @Transactional
    public List<EntryInfoEntity> insertEntry(List<EntryInfoEntity> entryInfoEntities, String taskID,HttpServletRequest request) {
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        return this.insertEntry(entryInfoEntities, taskID, userName, department);
    }

    @Override
    @Transactional
    //用is_exist 区分 已存在和新词条，已存在升级词条版本在插入
    public List<EntryInfoEntity> insertEntry(List<EntryInfoEntity> entryInfoEntities, String taskID, String userName,String department) {
        long start = System.currentTimeMillis();
        int insert = 0;

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
                    if (StringUtils.isNotBlank(entryInfoEntity.getZhTransId())) {
                        entryInfoEntity1.setChinese(entryInfoEntity.getChinese());
                        entryInfoEntity1.setZhTransId(entryInfoEntity.getZhTransId());
                    }
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

    @Override
    public void getEntryInfoEntityForExport(ExcelExportVO<EntryInfoEntityForExcel> excelExportVO){
        List<EntryInfoEntityForExcel> entityForExcels = excelExportVO.getEntryInfoEntities();
        if(entityForExcels != null){
            /* 如果已经提供要导出的词条, 那么就不用从库里查找要导出的词条 */
            return;
        }
        EntryInfoEntity template = excelExportVO.getEntryInfoEntity();
        if(template != null){
            List<EntryInfoEntity> entities = entryInfoMapper.getEntryInfo(template);   
            List<EntryInfoEntityForExcel> entryInfoEntities = new ArrayList<>(entities.stream().map(EntryInfoEntityForExcel::convertFromEntryInfoEntity).collect(Collectors.toList()));
            excelExportVO.setEntryInfoEntities(entryInfoEntities);
        }else{
            throw new RuntimeException("在没有提供要导出的词条的情况下没有提供要查询的词条需要符合的条件,无法获取要导出的词条");
        }

    }

    @Override
    public List<EntryInfoEntityForExcel> filterEntryInfoBeforeExport(ExcelExportVO<EntryInfoEntityForExcel> excelExportVO){
        Predicate<EntryInfoEntityForExcel> predicate = excelExportVO.getPredicate();
        if(predicate == null){
            return null;
        }
        List<EntryInfoEntityForExcel> entryInfoEntityForExcels = excelExportVO.getEntryInfoEntities();
        if(entryInfoEntityForExcels == null){
            return null;
        }
        return entryInfoEntityForExcels.stream().filter(predicate).collect(Collectors.toList());

    }

    
    @Override
    public void postProcessEntryInfoForExport(ExcelExportVO<EntryInfoEntityForExcel> excelExportVO){
        List<EntryInfoEntityForExcel> entryInfoEntities = excelExportVO.getEntryInfoEntities();

        if(entryInfoEntities == null || entryInfoEntities.isEmpty()){
            return;
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
        return;
    }

    @Override
    public boolean entryExportByCondition(ByteArrayOutputStream buffer,ExcelExportVO<EntryInfoEntityForExcel> excelExportVO, String taskID) {

        if(excelExportVO == null || buffer == null){
            return false;
        }
        List<String> columnNames = excelExportVO.getColumnNames();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmss");
        Date date = new Date();
        String da = format.format(date);
        String excelName = excelExportVO.getExcelName() + "_" + da;
        TaskInfoEntity taskEntity = null;
        if (StringUtils.isNotBlank(taskID)) {
            taskEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

        }
        List<EntryInfoEntityForExcel> entryInfoEntities = excelExportVO.getEntryInfoEntities();
        // processEntryInfo(entryInfoEntities, columnNames, excelName, taskEntity);
        processEntryInfo(entryInfoEntities, columnNames, taskEntity);

        buildExportedFiles(buffer,entryInfoEntities,excelExportVO.getExportFileType(), columnNames, excelName);
        return true;
    }

    private void buildExportedFiles(ByteArrayOutputStream buffer,List<EntryInfoEntityForExcel> entities,String exportFileType,List<String> columnNames,String excelName){
        if(buffer == null){
            throw new NullPointerException("buffer == null");
        }
        List<String> exportFields = new ArrayList<>();
        for (String column : columnNames) {
            String fieldName = ConstantInterface.EXCEL_LIST_NAME_MAP().get(column);
            if(fieldName == null){
                throw new NullPointerException("没有找到: " + column + "对应的方法");
            }
            exportFields.add(fieldName);

        }
        if(exportFileType == null){
            exportFileType = "excel";
        }
        if(exportFileType.equals("excel")){
            excelUtils.getWorkBook(buffer,entities,exportFields, columnNames, excelName);
        }else if(exportFileType.equals("csv")){
            String charsetName = "GBK";
            try {
                if(!columnNames.contains("id")){
                    columnNames.add("id");
                    exportFields.add(ConstantInterface.EXCEL_LIST_NAME_MAP().get("id"));
                }
                List<String> contentLines = excelUtils.exportEntitiesToCSV(entities,EntryInfoEntityForExcel.class, exportFields, columnNames);
                /* outputstream流必须在header之后写 */
                for(String entityLine : contentLines){
                    buffer.write(entityLine.getBytes(charsetName));
                }
                
            } catch (Exception e) {
                // TODO Auto-generated catch block
                throw new RuntimeException("解析CSV文件时报错",e);
            } 
        }else if(exportFileType.equals("ts")){
            List<TLanguage> languages = this.languageMapper.selectLaguageByName("英文");


            TSExporter tsExporter = new TSExporter(entryUtils,languages.get(0));

            tsExporter.export(entities.stream().map((entryExcel)->{
                EntryInfoEntity entity = new EntryInfoEntity();
                entity.setEntry(entryExcel.getEntry());
                entity.setTag(entryExcel.getTag());
                entity.setComment(entryExcel.getComment());
                entity.setEnglish(entryExcel.getEnglish());
                entity.setChinese(entryExcel.getChinese());
                entity.setRussian(entryExcel.getRussian());
                entity.setChinese(entryExcel.getChinese());
                entity.setSpanish(entryExcel.getSpanish());
                return entity;
            }).collect(Collectors.toList()), buffer);

        }else{
            throw new RuntimeException("当前不支持exportFileType为: " + exportFileType);
        }
    }

    /**
     * 根据t_product和t_version填写词条的产品ID，产品名，版本ID和版本名，其中产品ID和版本ID根据t_product_relation获取
     * @param <T>
     * @param entryInfoEntities
     * @param columnNames
     * @param taskEntity
     */
    private <T extends EntryInfoEntity> void processEntryInfo(List<T> entryInfoEntities,List<String> columnNames,TaskInfoEntity taskEntity){
        if(entryInfoEntities == null){
            return;
        }
        Map<String,EntryInfoEntity> entryInfoEntitiesIDMap = entryInfoEntities.stream().collect(Collectors.toMap(EntryInfoEntity::getId, EntryInfoEntity -> EntryInfoEntity));
        /*
         * 可能出现的情况
         * 1. 词条在t_product_relation表中没有找到对应词条的记录信息，则productID依然为NULL，并且productName为NULL
         * 2. 词条在t_product_relation表中找到对应词条的记录信息，但productID项为NULL，则productID依然为NULL，并且productName为NULL
         * 3. 词条在t_product_relation表中找到对应词条的记录信息，productID不为NULL
         *  但t_product表中没有找到对应productID的行记录，则productID依然为NULL，并且productName为NULL
         *  如果找到对应的行记录，则productID不为NULL，并且productName为对应行记录的结果
         */
        if(taskEntity == null){
            List<String> ids = new ArrayList<>(entryInfoEntitiesIDMap.keySet());
            if(ids.isEmpty()){
                return;
            }
            Map<String,ProductRelationEntity> productRelationIDMap = productRelationMapper.getProductionRelationsByIDs(ids);

            Set<String> productIDs = productRelationIDMap.values().stream().map(ProductRelationEntity::getProductId).collect(Collectors.toSet());
            Set<String> versionIDs = productRelationIDMap.values().stream().map(ProductRelationEntity::getVersionId).collect(Collectors.toSet());
            
            Map<String,ProductEntity> productIDMap = null;
            if(!productIDs.isEmpty()){
                productIDMap = productMapper.getProductByIDs(productIDs);
            }else{
                productIDMap = new HashMap<>();
            }
            Map<String,VersionEntity> versionIDMap = null;
            if(!versionIDs.isEmpty()){
                versionIDMap = versionMapper.getVersionByIDs(versionIDs);
            }else{
                versionIDMap = new HashMap<>();
            }
            for(Map.Entry<String,EntryInfoEntity> entry : entryInfoEntitiesIDMap.entrySet()){
                EntryInfoEntity entity = entry.getValue();
                String id = entity.getId();
                if(id == null){
                    throw new NullPointerException("存在词条的ID为NULL");
                }
                ProductRelationEntity productRelationEntity = productRelationIDMap.get(id);
                if(productRelationEntity == null){
                    log.warn("警告: t_production_relation表中不存在id为: " + id + "的词条的相关信息");
                    continue;
                }
                String productID = productRelationEntity.getProductId();
                String versionID = productRelationEntity.getVersionId();
                ProductEntity productEntity = productIDMap.get(productID);
                VersionEntity versionEntity = versionIDMap.get(versionID);
                if(StringUtils.isBlank(productID)){
                    log.warn("警告,词条id: " + id + "没有关联到任何产品上");
                }else if(productID == null){
                    log.warn("警告: 没有在t_product表中查询到productID: " + productID + "的信息");
                }else{
                    entity.setProductID(productID);
                    entity.setProductName(productEntity.getName()); 
                }
                if(StringUtils.isBlank(versionID)){
                    log.warn("警告,词条id: " + id + "没有关联到任何版本上");
                }else if(versionID == null){
                    log.warn("警告: 没有在t_product表中查询到versionID: " + versionID + "的信息");
                }else{
                    entity.setVersionID(versionID);
                    entity.setVersionName(versionEntity.getName());
                }
            }
        }else{
            String productID = taskEntity.getProductId();
            if(productID == null){
                log.warn("警告,任务id: " + taskEntity.getId() + "没有关联到任何产品上");
            }else{
                Map<String,ProductEntity> productIDMap = productMapper.getProductByIDs(new HashSet<>(Arrays.asList(new String[]{productID})));
                ProductEntity productEntity = productIDMap.get(productID);
                if(productEntity == null){
                    log.warn("警告: 没有在t_product表中查询到productID: " + productID + "的信息");
                }else{
                    for(Map.Entry<String,EntryInfoEntity> entry : entryInfoEntitiesIDMap.entrySet()){
                        EntryInfoEntity entity = entry.getValue();
                        entity.setProductID(productID);
                        entity.setProductName(productEntity.getName());
                    }
                }

                
            }
            String versionID = taskEntity.getVersionId();
            if(versionID == null){
                log.warn("警告,任务id: " + taskEntity.getId() + "没有关联到任何版本上");
            }else{
                Map<String,VersionEntity> versionIDMap = versionMapper.getVersionByIDs(new HashSet<>(Arrays.asList(new String[]{versionID})));
                VersionEntity versionEntity = versionIDMap.get(versionID);
                if(versionEntity == null){
                    log.warn("警告,任务id: " + taskEntity.getVersionId() + "没有关联到任何版本上");
                }else{
                    for(Map.Entry<String,EntryInfoEntity> entry : entryInfoEntitiesIDMap.entrySet()){
                        EntryInfoEntity entity = entry.getValue();
                        entity.setProductID(versionID);
                        entity.setProductName(versionEntity.getName());
                    }    
                }
            }
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
    public String addProductRelation(List<EntryInfoEntity> entryInfoEntities,HttpServletRequest request) throws Exception {
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        ArrayList<EntryInfoEntity> update_entities = new ArrayList<>();
        for (EntryInfoEntity relationEntity : entryInfoEntities) {
            ProductRelationEntity productRelationEntity = new ProductRelationEntity();
            productRelationEntity.setProductId(relationEntity.getProductID());
            productRelationEntity.setEntryId(relationEntity.getId());
            //通过任务拿到语言
            TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(relationEntity.getTaskId());
            String lang = taskInfoEntity.getTranslateType();
            if (relationEntity.getEntryState() == 0){
                relationEntity.setEntryState(1);
            }else {
                throw new Exception(ErrorCodeList.ENTRY_STATE_ERROR);
            }
            //插入关联表
            productRelationEntity.setVersionId(relationEntity.getVersionID());
            productRelationEntity.setTaskId(relationEntity.getTaskId());
            //查询是否存在此关联信息，没有则新建
            int sum = productRelationMapper.checkExist(productRelationEntity);
            if (sum < 1) {
                productRelationEntity.setId(commonUtils.getUUID());
                int insert= productRelationMapper.insert(productRelationEntity);
                if (insert < ConstantInterface.DB_SUCCESS_RESULT) {
                    return ErrorCodeList.INSERT_ERROR;
                }
                ProductRelationEntity delete_relation = new ProductRelationEntity();
                delete_relation.setProductId(relationEntity.getProductID());
                delete_relation.setEntryId(relationEntity.getId());
                delete_relation.setVersionId(relationEntity.getVersionID());
                List<ProductRelationEntity> delete_list = productRelationMapper.selectList(
                        new QueryWrapper<ProductRelationEntity>().eq("entry_id", relationEntity.getId())
                                .eq("product_id", relationEntity.getProductID()).isNull("version_id").isNull("task_id"));


                if (delete_list.size() > 0) {
                    //如果存在多条关联信息，删除旧的

                    int delete = productRelationMapper.delete(
                            new QueryWrapper<ProductRelationEntity>().eq("entry_id", relationEntity.getId())
                            .eq("product_id", relationEntity.getProductID()).isNull("version_id").isNull("task_id"));

                }
            }
            TranslateEntity translateEntity = new TranslateEntity();
            String transID = null;
            translateEntity.setEntry(relationEntity.getEntry());
            List<TranslateEntity> translateEntityList = new ArrayList<>();

           switch (lang){
               case ConstantInterface.CHINESE:
                   if (StringUtils.isBlank(relationEntity.getChinese())){
                       break;
                   }
                   translateEntityList = translateMapper.selectList(
                           new QueryWrapper<TranslateEntity>().eq("entry", relationEntity.getEntry())
                                   .eq("type", lang).eq("translate", relationEntity.getChinese())
                                   .eq("delete_state", 0).eq("visual_range", department).eq("translate_state", "3")
                   );
                   if (translateEntityList != null && !translateEntityList.isEmpty()) {
                       // 如果已经存在相同的翻译，则换成这个id
                       transID = translateEntityList.get(0).getId();
                       relationEntity.setZhTransId(transID);
                   }else {
                       transID = commonUtils.getUUID();
                       relationEntity.setZhTransId(transID);
                       translateEntity.setTranslateState("1");
                       translateEntity.setId(transID);
                       translateEntity.setDeleteState(0);
                       translateEntity.setVisualRange(department);
                       translateEntity.setType(lang);
                       translateEntity.setTranslate(relationEntity.getChinese());
                       int insert = translateMapper.insertTranslate(translateEntity);
                       if (insert < ConstantInterface.DB_SUCCESS_RESULT) {
                           return ErrorCodeList.INSERT_ERROR;
                       }
                   }
               case ConstantInterface.ENGLISH:
                   if (StringUtils.isBlank(relationEntity.getEnglish())){
                       break;
                   }
                  translateEntityList = translateMapper.selectList(
                           new QueryWrapper<TranslateEntity>().eq("entry", relationEntity.getEntry())
                                   .eq("type", lang).eq("translate", relationEntity.getEnglish())
                                   .eq("delete_state", 0).eq("visual_range", department).eq("translate_state", "3")
                   );
                   if (translateEntityList != null && !translateEntityList.isEmpty()) {
                       // 如果已经存在相同的翻译，则换成这个id
                       transID = translateEntityList.get(0).getId();
                       relationEntity.setEnTransId(transID);
                   }else {
                       transID = commonUtils.getUUID();
                       relationEntity.setEnTransId(transID);
                       translateEntity.setTranslateState("1");
                       translateEntity.setId(transID);
                       translateEntity.setDeleteState(0);
                       translateEntity.setVisualRange(department);
                       translateEntity.setType(lang);
                       translateEntity.setTranslate(relationEntity.getEnglish());
                       int insert = translateMapper.insertTranslate(translateEntity);
                       if (insert < ConstantInterface.DB_SUCCESS_RESULT) {
                           return ErrorCodeList.INSERT_ERROR;
                       }
                   }

                case ConstantInterface.RUSSIAN:
                    if (StringUtils.isBlank(relationEntity.getRussian())){
                        break;
                    }
                    translateEntityList = translateMapper.selectList(
                            new QueryWrapper<TranslateEntity>().eq("entry", relationEntity.getEntry())
                                    .eq("type", lang).eq("translate", relationEntity.getRussian())
                                    .eq("delete_state", 0).eq("visual_range", department).eq("translate_state", "3")
                    );
                    if (translateEntityList != null && !translateEntityList.isEmpty()) {
                        // 如果已经存在相同的翻译，则换成这个id
                        transID = translateEntityList.get(0).getId();
                        relationEntity.setRuTransId(transID);
                    }else {
                        transID = commonUtils.getUUID();
                        relationEntity.setRuTransId(transID);
                        translateEntity.setTranslateState("1");
                        translateEntity.setId(transID);
                        translateEntity.setDeleteState(0);
                        translateEntity.setVisualRange(department);
                        translateEntity.setType(lang);
                        translateEntity.setTranslate(relationEntity.getRussian());
                        int insert = translateMapper.insertTranslate(translateEntity);
                        if (insert < ConstantInterface.DB_SUCCESS_RESULT) {
                            return ErrorCodeList.INSERT_ERROR;
                        }
                    }

                case ConstantInterface.SPANISH:
                    if (StringUtils.isBlank(relationEntity.getSpanish())){
                        break;
                    }
                    translateEntityList = translateMapper.selectList(
                            new QueryWrapper<TranslateEntity>().eq("entry", relationEntity.getEntry())
                                    .eq("type", lang).eq("translate", relationEntity.getSpanish())
                                    .eq("delete_state", 0).eq("visual_range", department).eq("translate_state", "3")
                    );
                    if (translateEntityList != null && !translateEntityList.isEmpty()) {
                        // 如果已经存在相同的翻译，则换成这个id
                        transID = translateEntityList.get(0).getId();
                        relationEntity.setSpaTransId(transID);
                    }else {
                        transID = commonUtils.getUUID();
                        relationEntity.setSpaTransId(transID);
                        translateEntity.setTranslateState("1");
                        translateEntity.setId(transID);
                        translateEntity.setDeleteState(0);
                        translateEntity.setVisualRange(department);
                        translateEntity.setType(lang);
                        translateEntity.setTranslate(relationEntity.getSpanish());
                        int insert = translateMapper.insertTranslate(translateEntity);
                        if (insert < ConstantInterface.DB_SUCCESS_RESULT) {
                            return ErrorCodeList.INSERT_ERROR;
                        }
                    }

                case ConstantInterface.FRENCH:
                    if (StringUtils.isBlank(relationEntity.getFrench())){
                        break;
                    }
                    translateEntityList = translateMapper.selectList(
                            new QueryWrapper<TranslateEntity>().eq("entry", relationEntity.getEntry())
                                    .eq("type", lang).eq("translate", relationEntity.getFrench())
                                    .eq("delete_state", 0).eq("visual_range", department).eq("translate_state", "3")
                    );
                    if (translateEntityList != null && !translateEntityList.isEmpty()) {
                        // 如果已经存在相同的翻译，则换成这个id
                        transID = translateEntityList.get(0).getId();
                        relationEntity.setFraTransId(transID);
                    }else {
                        transID = commonUtils.getUUID();
                        relationEntity.setFraTransId(transID);
                        translateEntity.setTranslateState("1");
                        translateEntity.setId(transID);
                        translateEntity.setDeleteState(0);
                        translateEntity.setVisualRange(department);
                        translateEntity.setType(lang);
                        translateEntity.setTranslate(relationEntity.getFrench());
                        int insert = translateMapper.insertTranslate(translateEntity);
                        if (insert < ConstantInterface.DB_SUCCESS_RESULT) {
                            return ErrorCodeList.INSERT_ERROR;
                        }
                    }

           }
            int update = entryInfoMapper.updateEntryInfo(relationEntity);
            if (update < ConstantInterface.DB_SUCCESS_RESULT) {
                return ErrorCodeList.UPDATE_ERROR;
            }
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public List<EntryInfoEntity> importCommonExcle(FileInputStreamEntity fileInputStreamEntitiy, String taskID) {
        String name = fileInputStreamEntitiy.getFileName();
        TaskInfoEntity taskEntityByTaskID = taskInfoMapper.getTaskEntityByTaskID(taskID);
        //读取excle转换的实体
        Collection<EntryInfoEntity> entryInfoEntities = null;
        try {

            entryInfoEntities = excelUtils.readExcelToEntity(taskEntityByTaskID.getTranslateType(),EntryInfoEntity.class, fileInputStreamEntitiy.getInputStream(),name).getParsedObjects();
        } catch (Exception e) {
            throw new RuntimeException(e);
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
                case ConstantInterface.CHINESE:
                    if (StringUtils.isNotBlank(entryInfoEntity.getChinese())) {
                        entryInfoEntity.setChineseTranslateState("1");
                    }
                    break;
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


    @Transactional
    @Override
    public UpdateEntryInfoByFileVO importTransExcle(FileInputStreamEntity fileInputStreamEntitiy,  String token, String transType,String encoding,Map<String, Set<String>> idRelationMap){
    
        try {
            KeyValueArguments<String> kwargs = new KeyValueArguments<>();
            kwargs.set("encoding", encoding);
            Collection<EntryInfoEntity> entryInfosParsedOnFile = excelUtils.parseFileToEntity(EntryInfoEntity.class,fileInputStreamEntitiy.getInputStream(), fileInputStreamEntitiy.getFileName(), kwargs).getParsedObjects();
            return this.importTransExcle(entryInfosParsedOnFile, token, transType, idRelationMap);
        } catch (Exception e) {
            log.error("更新词条翻译时出现异常", e); 
            throw new RuntimeException(e);
        }
    }


    @Override
    public UpdateEntryInfoByFileVO importTransExcle(Collection<EntryInfoEntity> entryInfosParsedOnFile, String token,String transType, Map<String, Set<String>> idRelationMap) {
        
        UpdateEntryInfoByFileVO updateTranslationByFileVO = new UpdateEntryInfoByFileVO();

        try {
            
            String department = JWTTokenUtils.getDepartment(token);
            Map<String,Object> kwargs = new HashMap<>();
            kwargs.put("department", department);
            Consumer<TranslateEntity> translateEntiityProcessor = new Consumer<TranslateEntity>() {
                @Override
                public void accept(TranslateEntity translateEntity) {
                    // TODO Auto-generated method stub
                    translateEntity.setTranslateState("3");
                }
            };
            kwargs.put("translateProcessor", translateEntiityProcessor);    // 利用excel文件更新翻译，默认更新为已审核
            /* 解析文件, 生成词条 */
            // Collection<EntryInfoEntity> entryInfosParsedOnFile = excelUtils.parseFileToEntity(EntryInfoEntity.class,fileInputStreamEntitiy.getInputStream(), fileInputStreamEntitiy.getFileName(), null).getParsedObjects();
            
            /* 获取其他没送翻, 但是也应当根据送翻后的词条更新翻译的词条 */
            String getMethodName = MethodUtils.getMethodName(transType);    // 获取getTranslate方法
            String setMethodName = MethodUtils.setMethodName(transType);    // 获取setTranslate方法
            Method getMethod;
            Method setMethod;
            /* 获取getTranslate方法 */
            try {
                if(StringUtils.isBlank(getMethodName)){
                    throw new NullPointerException("transType对应的getMethodName为null");
                }                
                getMethod = EntryInfoEntity.class.getMethod(getMethodName);
            } catch (Exception e) {
                throw new RuntimeException(String.format("没有找到语言类型: %s对应的getMethod方法, 详细信息为: %s", transType,e.getMessage()));
            }

            /* 获取setTranslate方法 */
            try {
                if(StringUtils.isBlank(setMethodName)){
                    throw new NullPointerException("transType对应的setMethodName为null");
                }
                setMethod = EntryInfoEntity.class.getMethod(setMethodName, String.class);
            } catch (Exception e) {
                throw new RuntimeException(String.format("没有找到类型: %s对应的setMethod方法, 详细信息为: %s", setMethodName,e.getMessage()));
            }
            if(setMethod == null || getMethod == null){
                throw new RuntimeException(String.format("没有找到获取翻译和设定翻译的方法, 无法更新翻译, transType: %s",transType));
            }
            List<EntryInfoEntity> entryInfosAllowdParsedOnFile = entryInfosParsedOnFile.stream().filter((entry)->{
                try {
                    if(StringUtils.isNotBlank(getMethod.invoke(entry) == null ? null : String.valueOf(getMethod.invoke(entry)))){
                        return true;
                    }  // 如果该行记录的翻译是空的，是不进行更新的
                    ExceptionVO exceptionVO = new ExceptionVO(String.format("送翻后的翻译文件的词条没有对应语言的翻译, 请检查翻译文件, id: %s", entry.getId()),"请在翻译文件上填写翻译结果, 或删除该词条不进行更新");
                    updateTranslationByFileVO.addException(exceptionVO);
                    updateTranslationByFileVO.addFailedEntryInfo(entry);
                    return false;
                } catch(Exception e){
                    log.error(e.getMessage(), e);
                    ExceptionVO exceptionVO = new ExceptionVO(String.format("判断词条对应语种: %s是否有翻译时出现异常: %s,异常信息为: %s, id: %s", transType,e.getMessage(),entry.getId()),"联系研发");
                    updateTranslationByFileVO.addException(exceptionVO);
                    updateTranslationByFileVO.addFailedEntryInfo(entry);
                    return false;
                }
            }).collect(Collectors.toList());
            /* 先把文件中对应的词条的翻译更新掉 */
            String transTypeChinese =  ConstantInterface.translateMap().get(transType);   // english->英文,russian->俄文
            if(StringUtils.isBlank(transTypeChinese)){
                throw new RuntimeException(String.format("当前不支持更新语种: %s为的翻译", transType));
            }
            Collection<ExceptionEntryInfoVO> updateExceptionVOs = this.updateTranslations(entryInfosAllowdParsedOnFile, transTypeChinese, kwargs);
            
            updateExceptionVOs.forEach((vo) -> {updateTranslationByFileVO.addException(vo);updateTranslationByFileVO.addFailedEntryInfos(vo.getEntryInfoVO().getEntryInfoEntities());});

            if(idRelationMap == null || idRelationMap.isEmpty()){
                return updateTranslationByFileVO;
            }
            Map<String, List<EntryInfoEntity>> entryInfosOnFileIDRelation = entryInfosParsedOnFile.stream().collect(Collectors.groupingBy((t) -> t.getId())); // key是id, value是entryInfo
            /*
                测试场景
                √：代表更新正常；×：代表更新失败
                注意事项:
                    - 只能更新库里未删除的词条
                    - id关联表只能确定每个节点对应的用来更新的翻译是什么，不能代表是否能正常将翻译存库（例如词条被删除）
                    
                1.没有id关联表, 则仅更新翻译文件中对应ID的词条的翻译
                2. id关联表中的父ID在送翻的文件上，并且子节点都不在送翻的翻译文件上
                    - 结果： 子节点使用父节点的翻译结果，父节点使用自己的翻译结果（√）
                3. id关联表中的父ID在送翻的文件上，子节点也在送翻的翻译文件上
                    - 结果：以ID关联表的翻译获取逻辑实现
                4. id关联表中的父id，不在送翻的翻译文件上
                    - 情况1: 子节点都不在送翻的翻译文件上
                        - 结果： 报异常信息（×）
                    - 情况2: 子节点在送翻的翻译文件上, 但只有一个子节点
                        - 结果：父节点和所有子节点都采用该子节点对应的翻译（√）
                    - 情况3: 子节点在送翻的翻译文件上, 但有多个子节点
                        - 情况1：假设所有子节点的翻译结果相同
                            - 结果：父节点和所有子节点都采用子节点对应的翻译（√）
                        - 情况2：不同子节点的翻译不同
                            - 结果： 报异常信息（×）
            */
            for(Map.Entry<String,Set<String>> idRelation : idRelationMap.entrySet()){
                String parentID = idRelation.getKey();
                Set<String> childIDs = idRelation.getValue();
                if(parentID == null){
                    throw new RuntimeException("id映射表存在异常, 父节点的ID为null");
                }
                List<EntryInfoEntity> entryInfosOnFile = entryInfosOnFileIDRelation.get(parentID);
                String translate = "";
                List<String> searchIDs = new ArrayList<>();
                if(entryInfosOnFile == null || entryInfosOnFile.isEmpty()){
                    /* 文件里面丢失了要翻译的词条 */
                    // 父节点丢失, 查看是否子节点是否被当作送翻的词条
                    searchIDs.add(parentID);

                    List<EntryInfoEntity> childEntryInfos = new ArrayList<>();
                    if(childIDs != null){
                        for(String childID : childIDs){
                            List<EntryInfoEntity> childEntryInfosOnFile = entryInfosOnFileIDRelation.get(childID);
                            if(childEntryInfosOnFile == null || childEntryInfosOnFile.isEmpty()){
                                searchIDs.add(childID);
                                continue;   // 送翻的词条没有这个子节点
                            }
                            childEntryInfos.addAll(childEntryInfosOnFile);
                        }
                    }
                    List<String> childTranslates = new ArrayList<>();
                    for(EntryInfoEntity childEntryInfo : childEntryInfos){
                        childTranslates.add(getMethod.invoke(childEntryInfo) == null ? null : String.valueOf(getMethod.invoke(childEntryInfo)));
                    }
                    /* 确定更新的翻译的翻译内容是什么 */
                    int countChildIDOnParsedFile = childTranslates.size();
                    if(countChildIDOnParsedFile == 0){
                        /* 父节点没有, 子节点一个也没有, 这种情况是丢了 */
                        updateTranslationByFileVO.addException(new ExceptionVO(
                            String.format("父节点id: \"%s\"的词条送翻记录丢失, 并且也没有子节点的送翻信息, 与之关联的子节点的id信息为: %s", parentID,childIDs.toString()),
                            "检查该id的词条是否送翻, 检查送翻的文件是否正确, 是否送翻文件内容被修改过, 检查id关联文件是否对应, 检查该文件是否被篡改"
                        ));
                        continue;
                        // throw new RuntimeException(String.format("父节点id: \"%s\"的词条送翻记录丢失, 请检查送翻后的翻译文件是否有该词条", parentID));
                    }else if(countChildIDOnParsedFile > 1){
                        /* 父节点没有，但是有多个子节点,需要判断这多个子节点的翻译是不是一样的,不一样说明存在异常， 一样就正常翻译就行, 利用子节点的翻译结果更新翻译(父节点的也要更新) */
                        Set<String> childTranslateSet = childTranslates.stream().collect(Collectors.toSet());
                        if(childTranslateSet.size() > 1){
                            updateTranslationByFileVO.addException(new ExceptionVO(
                                String.format("父节点id: \"%s\",子节点id信息为 \"%s\", 送翻前去重属于同一组的多个词条在该翻译文件中, 并且这多个词条的翻译有所不同, 相关的翻译结果分别为: \"%s\"",parentID, childIDs.toString(),childTranslateSet.toString()), 
                                "送翻后的文件中该组词条只保留一个，删除掉该组其他的词条, 然后重新更新翻译"));
                            updateTranslationByFileVO.addFailedEntryInfos(childEntryInfos);
                            // throw new RuntimeException(String.format("送翻前去重属于同一组的多个词条在该翻译文件中, 并且这多个词条的翻译有所不同, 相关的翻译结果分别为: \"%s\"", childTranslateSet.toString()));
                            continue;
                        }
                        translate = childTranslates.get(0);
                    }else{
                        /* 父节点没有, 但只有一个子节点,(父节点的也要更新)  */
                        translate = childTranslates.get(0);
                    }
                    if(StringUtils.isBlank(translate)){
                        /* 父节点没有，送翻后的子节点的翻译还是空的，需要进行检查 */
                        updateTranslationByFileVO.addException(new ExceptionVO(
                            String.format("送翻后的翻译文件的词条没有对应语言的翻译, 请检查翻译文件", parentID), 
                            String.format("检查父节点id为: %s, 子节点id为: %s的翻译结果是否为空的,如果是, 请在翻译文件上填写翻译结果", parentID,childIDs.toString()))
                        );
                        updateTranslationByFileVO.addFailedEntryInfos(childEntryInfos);
                        continue;   // 1. 只送翻了一个子节点, 但翻译为""或null; 2. 送翻了多个子节点, 但所有子节点的翻译都为"" 或null
                    }
                }else{
                    /* 父节点存在， 并且之前已经进行了更新翻译操作(无论成功与否(例如父节点已经在库里被删除了)), 之后为所有子节点添加对应的翻译 */
                    translate = getMethod.invoke((entryInfosOnFile.get(0))) == null ? null : String.valueOf(getMethod.invoke((entryInfosOnFile.get(0))));
                    searchIDs.addAll(childIDs);
                    if(StringUtils.isBlank(translate)){
                        /* 送翻后的翻译还是空的，需要进行检查 */
                        updateTranslationByFileVO.addException(new ExceptionVO(
                            String.format("送翻后的翻译文件的词条没有对应语言的翻译, 请检查翻译文件", parentID), 
                            String.format("检查父节点id为: %s, 子节点id为: %s的翻译结果是否为空的,如果是, 请在翻译文件上填写翻译结果", parentID,childIDs.toString()))
                        );
                        // updateTranslationByFileVO.addFailedEntryInfos(entryInfosOnFile);
                        continue;
                    }
                }

                List<EntryInfoEntityDO> otherEntryInfoDOs = entryInfoMapper.selectEntryInfosByIDs(searchIDs.stream().collect(Collectors.toList()));
                List<EntryInfoEntity> otherEntryInfos = EntryInfoEntityDO.convertFromEntities(otherEntryInfoDOs, EntryInfoEntityDO.newConverterForEntryInfoEntitiy()).collect(Collectors.toList());
                for(EntryInfoEntity otherEntryInfo : otherEntryInfos){
                    setMethod.invoke(otherEntryInfo, translate);
                }
                /* 
                    将更新的结果存库
                    如果翻译文件只有父节点，那么父节点和子节点的翻译都相同
                    如果翻译文件存在父节点和子节点，那么子节点的翻译也是父节点的翻译（当子节点的翻译结果和父节点不同时）
                    如果翻译文件只有子节点，那么父节点和子节点的翻译都相同
                */
                Collection<ExceptionEntryInfoVO> updateOtherEntryInfoExceptions = this.updateTranslations(otherEntryInfos, transTypeChinese, kwargs);
                updateOtherEntryInfoExceptions.forEach((vo) -> {updateTranslationByFileVO.addException(vo);});  // 因为要处理的是文件中的词条，此处更新的不是文件中的词条，所以不需要返回给用户,不需要执行addFailedEntryInfos
            }

            return updateTranslationByFileVO;
        } catch (Exception e) {
            log.error("导入文件更新库里词条的翻译时出现异常",e);
            throw new RuntimeException(e);
        }
    }

    protected Collection<ExceptionEntryInfoVO> updateTranslations(Collection<EntryInfoEntity> entryEntitys,String transType,Map<String,Object> kwargs){

        try {
            String department = String.valueOf(kwargs.get("department"));
            if(department.isEmpty()){
                throw new RuntimeException("部门参数为空, 联系研发");
            }
            List<ExceptionEntryInfoVO> problematicEntryInfoEntities = new ArrayList<>();

            KeyValueArguments<String> keyValueArguments = new KeyValueArguments<>();
            keyValueArguments.set("department", department);
            keyValueArguments.set("translateProcessor", kwargs.get("translateProcessor"));
            translationStorageService.updateEntryInfoTranslations(entryEntitys, Arrays.asList(transType), keyValueArguments);
            /* 校验导入的翻译是否超过了最大字符长度,仅警告用户存在问题 */
            BatchMaxLengthTranslateAnalyzer batchMaxLengthTranslateAnalyzer = new BatchMaxLengthTranslateAnalyzer(entryInfoMapper,productRelationMapper,entryClassifyMapper);
            batchMaxLengthTranslateAnalyzer.addLanguageType(transType);
            boolean isNotOK = batchMaxLengthTranslateAnalyzer.analyze(entryEntitys);
            if(isNotOK){
                /* 存在翻译超过了对应的字符限制 */
                ExceptionEntryInfoVO exceptionEntryInfoVO = new ExceptionEntryInfoVO();
                EntryInfoVO entryInfoVO = new EntryInfoVO(batchMaxLengthTranslateAnalyzer.getProblematicEntryInfoEntities().stream().collect(Collectors.toList()));
                exceptionEntryInfoVO.setEntryInfoVO(entryInfoVO);
                exceptionEntryInfoVO.setMessage("翻译超过了对应词条的字符长度限制");
                exceptionEntryInfoVO.setResolvedMethodMessage("请修改对应词条的翻译的字符长度,然后再次更新翻译");
                problematicEntryInfoEntities.add(exceptionEntryInfoVO);
            }
            return problematicEntryInfoEntities;
        } catch (Exception e) {
            throw new RuntimeException(String.format("利用excel翻译后的文件更新词条翻译时出现异常, 异常信息为: %s", e.getMessage()));
        }
    }

    @Override
    public List<EntryInfoEntity> workImportExcleTrans(FileInputStreamEntity fileInputStreamEntitiy, String taskID) {
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(taskID);
        try {
            entryInfoEntities = excelUtils.readExcelToEntity(taskInfoEntity.getTranslateType(),EntryInfoEntity.class, fileInputStreamEntitiy.getInputStream(), fileInputStreamEntitiy.getFileName()).getParsedObjects().stream().collect(Collectors.toList());
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
                    case ConstantInterface.CHINESE:
                        entryInfoEntity.setChinese(StringUtil.capitalizeWords(entryInfoEntity.getChinese()));
                        break;
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
                    case ConstantInterface.CHINESE:
                        entryInfoEntity.setChinese(StringUtil.uncapitalizeWords(entryInfoEntity.getChinese()));
                        break;
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
                case ConstantInterface.CHINESE:
                    entryInfoEntity.setChinese(entryInfoEntity.getChinese().replace(sourceStr, replaceStr));
                    break;
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

    private void preprocessEntryInfoEntityTemplate(EntryInfoEntityQO entryInfoEntityTemplate,Set<String> clearMatchSet){
        if(clearMatchSet == null || !clearMatchSet.contains("词条")){
            entryInfoEntityTemplate.setEntry(StringUtil.addEscapeCharacter(entryInfoEntityTemplate.getEntry()));
        }
        if(clearMatchSet == null || !clearMatchSet.contains("tag")){
            entryInfoEntityTemplate.setTag(StringUtil.addEscapeCharacter(entryInfoEntityTemplate.getTag()));
        }
        if(clearMatchSet == null || !clearMatchSet.contains("Comment")){
            entryInfoEntityTemplate.setComment(StringUtil.addEscapeCharacter(entryInfoEntityTemplate.getComment()));
        }
        if(clearMatchSet == null || !clearMatchSet.contains("词条来源")){
            entryInfoEntityTemplate.setEntrySource(StringUtil.addEscapeCharacter(entryInfoEntityTemplate.getEntrySource()));
        }
        if(clearMatchSet == null || !clearMatchSet.contains("辞典名称")){
            entryInfoEntityTemplate.setDiFileName(StringUtil.addEscapeCharacter(entryInfoEntityTemplate.getDiFileName()));
        }
        if(clearMatchSet == null || !clearMatchSet.contains("翻译结果")){
            entryInfoEntityTemplate.setChinese(StringUtil.addEscapeCharacter(entryInfoEntityTemplate.getChinese()));
            entryInfoEntityTemplate.setEnglish(StringUtil.addEscapeCharacter(entryInfoEntityTemplate.getEnglish()));
            entryInfoEntityTemplate.setSpanish(StringUtil.addEscapeCharacter(entryInfoEntityTemplate.getSpanish()));
            entryInfoEntityTemplate.setFrench(StringUtil.addEscapeCharacter(entryInfoEntityTemplate.getFrench()));
            entryInfoEntityTemplate.setRussian(StringUtil.addEscapeCharacter(entryInfoEntityTemplate.getRussian()));
        }
        /* 修改人采用模糊匹配的方式  */
        entryInfoEntityTemplate.setUpdate(StringUtil.addEscapeCharacter(entryInfoEntityTemplate.getUpdate()));
    }

    public EntryInfoVO getEntryByClassfy(EntryInfoEntityQO entryInfoEntityTemplate,Set<String> clearMatchSet,String classfyID,String startTime,String endTime,Integer pageIndex,Integer pageSize){
        List<String> productidList = new ArrayList<>();
        //一级分类查询
        // if (StringUtils.isBlank(entryInfoEntityTemplate.getClassfy1())){
        getProductClassfyList(classfyID, productidList);
        // }

      /*  if (CollectionUtils.isEmpty(productidList)){
            log.error("当前分类不存在或者没有子分类");
          return null;
        }*/
        /* 无法查到禁用的词条，在where里面写的 */
        this.preprocessEntryInfoEntityTemplate(entryInfoEntityTemplate, clearMatchSet);
        /*  */
        Set<String> productIDs = new HashSet<>(); 
        Set<String> taskIDs = new HashSet<>();
        Set<String> versionIDs = new HashSet<>();

        productIDs.addAll(productidList);
        int offset = (pageIndex - 1) * pageSize;
        /* 处理时差 */
        String targetStartTimeStr =  null;  // 数据库是按照GMT-6存的，所以转换时区
        String targetEndTimeStr = null;

        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
        parser.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai")); // 指定解析时的时区
        // 2. 格式化：转为西六区（America/Chicago）时间
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("America/Chicago")); // 目标时区

        if(StringUtils.isNotBlank(startTime)){
            // Date startDate = null;
            try {
                Date localStartDate = parser.parse(startTime); // 解析为东八区的2025-12-01 00:00:00
                targetStartTimeStr = formatter.format(localStartDate);
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                log.error("解析起始查询时间异常", e);
            }
            
        }
        if(StringUtils.isNotBlank(endTime)){
            // Date endDate = null;
            try {
                Date localEndDate = parser.parse(endTime); // 解析为东八区的2025-12-01 00:00:00
                targetEndTimeStr = formatter.format(localEndDate);
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                log.error("解析末尾查询时间异常", e);
            }
        }        
        Set<String> entryIDs = entryInfoMapper.selectEntryInfoIDsByEntryTransConditions(entryInfoEntityTemplate, null,productIDs, clearMatchSet,targetStartTimeStr,targetEndTimeStr,offset,pageSize);

        List<EntryInfoEntityDO> entryInfoEntities = entryInfoMapper.selectEntryInfosByIDs(entryIDs.stream().collect(Collectors.toList()));
        EntryInfoVO entryInfoVO = new EntryInfoVO();
        entryInfoVO.setEntryInfoEntities(EntryInfoEntityDO.convertFromEntities(entryInfoEntities, EntryInfoEntityDO.newConverterForEntryInfoEntitiy()).collect(Collectors.toList()));

        String totalNumber = entryInfoMapper.countEntryIDsByConditions(entryInfoEntityTemplate,null,productIDs, clearMatchSet,targetStartTimeStr,targetEndTimeStr);

        entryInfoVO.setTotalSize(Integer.parseInt(totalNumber));    // 暂不考虑int溢出的问题
        return entryInfoVO;
    }



    @Override
    public List<EntryInfoEntity> checkNotUseEntry(String i18nUrl,String classfyID, Map<String,List<String>> soucreMap) throws Exception{
    
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
    public  List<SourceEntryVO> getEntrysourceListByClassfy(String classfyID,String i18nUrl, String token,List<String> exceptionMessage) {
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
                    String importType = entryInfoEntity.getImportType();
                    if(importType == null){
                        continue;
                    }
                    if (importType.equals(ConstantInterface.TS)) {
                        tsEntryList.add(entryInfoEntity.getEntry());
                        tsfileSet.add(entryInfoEntity.getEntrySource());
                    } else if (importType.equals(ConstantInterface.DI)) {
                        diEntryList.add(entryInfoEntity.getEntry());
                        difileSet.add(entryInfoEntity.getEntrySource());
                    } else if (importType.equals(ConstantInterface.ENUM)) {
                        enumEntryList.add(entryInfoEntity.getEntry());
                        enumfileSet.add(entryInfoEntity.getEntrySource());
                    } else if (importType.equals(ConstantInterface.CONFIG)) {
                        configEntryList.add(entryInfoEntity.getEntry());
                        configfileSet.add(entryInfoEntity.getEntrySource());
                    } else if (importType.equals(ConstantInterface.DB)) {
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
            case ConstantInterface.CHINESE:
                entryInfoEntityList.sort(Comparator.comparing(EntryInfoEntity::getChinese, Comparator.nullsFirst(Comparator.naturalOrder())));
                break;
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



    private String setTranslate(EntryInfoEntity entryInfoEntity, String translateType, String department) {
        String translate = "";
        Integer translateLength = null;
        String translateID = commonUtils.getUUID();
        switch (translateType) {
            case ConstantInterface.CHINESE:
                translate = entryInfoEntity.getChinese();
                if (StringUtils.isBlank(translate)) {
                    return "";
                }
                translateLength = entryInfoEntity.getZhCharLength();
                entryInfoEntity.setZhTransId(translateID);

                break;
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

    // private void caseExisttryForXML(EntryInfoEntity entryInfoEntity, TaskInfoEntity taskInfoEntity, String productTableName) {
    //     // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());
    //     List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryListForXML(productTableName, entryInfoEntity, taskInfoEntity.getProductId());
    //     if (CollectionUtils.isEmpty(entryEntities)) {
    //         //创建新翻译
    //         entryInfoEntity.setIsExist(0);
    //         entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
    //         entryInfoEntity.setEntryVersion(1);
    //         createNewTrans(entryInfoEntity, taskInfoEntity);
    //     } else {
    //         entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
    //         int maxVersion = entryEntities.stream().max(Comparator.comparing(EntryInfoEntity::getEntryVersion)).get().getEntryVersion();
    //         entryInfoEntity.setEntryVersion(maxVersion);
    //         entryInfoEntity.setIsExist(1);
    //     }
    // }

    // private void caseExisttry(EntryInfoEntity entryInfoEntity, TaskInfoEntity taskInfoEntity, String productTableName) {
    //     // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());
    //     List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryListForEquipment(productTableName, entryInfoEntity, taskInfoEntity.getProductId());
    //     if (CollectionUtils.isEmpty(entryEntities)) {
    //         //创建新翻译
    //         entryInfoEntity.setIsExist(0);
    //         entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
    //         entryInfoEntity.setEntryVersion(1);
    //         createNewTrans(entryInfoEntity, taskInfoEntity);
    //     } else {
    //         entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
    //         int maxVersion = entryEntities.stream().max(Comparator.comparing(EntryInfoEntity::getEntryVersion)).get().getEntryVersion();
    //         entryInfoEntity.setEntryVersion(maxVersion);
    //         entryInfoEntity.setIsExist(1);
    //     }
    // }

    // private void caseExisttry(EntryInfoEntity entryInfoEntity, TaskInfoEntity taskInfoEntity, ImportExcleEntry importExcleEntry, String productTableName) {
    //     // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());
    //     List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryListForEquipment(productTableName, entryInfoEntity, taskInfoEntity.getProductId());
    //     if (CollectionUtils.isEmpty(entryEntities)) {
    //         //创建新翻译
    //         entryInfoEntity.setIsExist(0);
    //         entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
    //         entryInfoEntity.setEntryVersion(1);
    //         createNewTrans(entryInfoEntity, taskInfoEntity, importExcleEntry);
    //     } else {
    //         entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
    //         int maxVersion = entryEntities.stream().max(Comparator.comparing(EntryInfoEntity::getEntryVersion)).get().getEntryVersion();
    //         entryInfoEntity.setEntryVersion(maxVersion);
    //         entryInfoEntity.setIsExist(1);
    //     }
    // }
    // private void createNewTrans(EntryInfoEntity entryInfoEntity, TaskInfoEntity taskInfoEntity) {
    //     switch (taskInfoEntity.getTranslateType()) {
    //         case ConstantInterface.CHINESE:
    //             if (StringUtils.isNotBlank(entryInfoEntity.getChinese())) {
    //                 entryInfoEntity.setChineseTranslateState(ConstantInterface.TRANSLATED);
    //                 entryInfoEntity.setZhCharLength(entryInfoEntity.getChinese().length());
    //             } else {
    //                 entryInfoEntity.setChineseTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             break;
    //         case ConstantInterface.ENGLISH:
    //             if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
    //                 entryInfoEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
    //                 entryInfoEntity.setEnCharLength(entryInfoEntity.getEnglish().length());
    //             } else {
    //                 entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             break;
    //         case ConstantInterface.SPANISH:
    //             if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
    //                 entryInfoEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
    //                 entryInfoEntity.setSpaCharLength(entryInfoEntity.getSpanish().length());
    //             } else {
    //                 entryInfoEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             break;
    //         case ConstantInterface.RUSSIAN:
    //             if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
    //                 entryInfoEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
    //                 entryInfoEntity.setRuCharLength(entryInfoEntity.getRussian().length());
    //             } else {
    //                 entryInfoEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             break;
    //         case ConstantInterface.FRENCH:
    //             if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
    //                 entryInfoEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
    //                 entryInfoEntity.setFraCharLength(entryInfoEntity.getFrench().length());
    //             } else {
    //                 entryInfoEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             break;
    //     }
    // }

    // private void createNewTrans(EntryInfoEntity entryInfoEntity, TaskInfoEntity taskInfoEntity, ImportExcleEntry importExcleEntry) {
    //     //写入翻译字段
    //     switch (taskInfoEntity.getTranslateType()) {
    //         case ConstantInterface.CHINESE:
    //             if (StringUtils.isNotBlank(importExcleEntry.getChinese())) {
    //                 entryInfoEntity.setChinese(importExcleEntry.getChinese());
    //                 entryInfoEntity.setChineseTranslateState(ConstantInterface.TRANSLATED);
    //             } else {
    //                 entryInfoEntity.setChineseTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             if (!Objects.isNull(importExcleEntry.getZhCharLength())) {
    //                 entryInfoEntity.setEntryLength(importExcleEntry.getZhCharLength());
    //             }
    //             break;
    //         case ConstantInterface.ENGLISH:
    //             if (StringUtils.isNotBlank(importExcleEntry.getEnglish())) {
    //                 entryInfoEntity.setEnglish(importExcleEntry.getEnglish());
    //                 entryInfoEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
    //             } else {
    //                 entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             if (!Objects.isNull(importExcleEntry.getEnCharLength())) {
    //                 entryInfoEntity.setEntryLength(importExcleEntry.getEnCharLength());
    //             }
    //             break;
    //         case ConstantInterface.SPANISH:
    //             if (StringUtils.isNotBlank(importExcleEntry.getSpanish())) {
    //                 entryInfoEntity.setSpanish(importExcleEntry.getSpanish());
    //                 entryInfoEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
    //             } else {
    //                 entryInfoEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             if (!Objects.isNull(importExcleEntry.getSpaCharLength())) {
    //                 entryInfoEntity.setEntryLength(importExcleEntry.getSpaCharLength());
    //             }
    //             break;
    //         case ConstantInterface.RUSSIAN:
    //             if (StringUtils.isNotBlank(importExcleEntry.getRussian())) {
    //                 entryInfoEntity.setRussian(importExcleEntry.getRussian());
    //                 entryInfoEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
    //             } else {
    //                 entryInfoEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             if (!Objects.isNull(importExcleEntry.getRuCharLength())) {
    //                 entryInfoEntity.setEntryLength(importExcleEntry.getRuCharLength());
    //             }
    //             break;
    //         case ConstantInterface.FRENCH:
    //             if (StringUtils.isNotBlank(importExcleEntry.getFrench())) {
    //                 entryInfoEntity.setFrench(importExcleEntry.getFrench());
    //                 entryInfoEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
    //             } else {
    //                 entryInfoEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
    //             }
    //             if (!Objects.isNull(importExcleEntry.getFraCharLength())) {
    //                 entryInfoEntity.setEntryLength(importExcleEntry.getFraCharLength());
    //             }
    //             break;
    //     }
    // }


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

        if (StringUtils.isNotBlank(entryInfoEntity.getChinese())) {
            setTranslate(entryInfoEntity, ConstantInterface.CHINESE, department);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
            setTranslate(entryInfoEntity, ConstantInterface.ENGLISH, department);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
            setTranslate(entryInfoEntity, ConstantInterface.SPANISH, department);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
            setTranslate(entryInfoEntity, ConstantInterface.FRENCH, department);
        } 
        if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
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

    @Override
    public boolean forrbiddenEntry(List<EntryInfoEntity> entryInfoEntities,HttpServletRequest request) {
        // TODO Auto-generated method stub
        Set<String> idList = entryInfoEntities.stream().map(EntryInfoEntity::getId).collect(Collectors.toSet());
        int updateCount = entryInfoMapper.forrbiddenEntry(idList);
        if(updateCount != entryInfoEntities.size()){
            return false;
        }
        List<EntryInfoEntity> entities = entryInfoMapper.getEntryByIDs(idList);
        String token = request.getHeader("token");
        String user = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        Date date = new Date(System.currentTimeMillis());
        String info = "用户: \"" + user + "\"(" + department +")在" + LocalTimeUtils.formatForFile.format(date) + "(UTC-8)禁用该词条";
        for(EntryInfoEntity entity : entities){
            String remark = entity.getRemark();
            EntryInfoEntity newEntity = new EntryInfoEntity();
            newEntity.setRemark(remark != null ? remark + "," + info : info);
            newEntity.setId(entity.getId());
            String result =  this.updateEntryInfo(newEntity,request,info);
            if(!result.equals(ConstantInterface.OK_STR)){
                log.warn("禁用词条时更新备注信息存在异常,异常信息为: " + result);
            }
        }
        
        return true;
    }

    @Override
    public boolean checkBeforeAddSingleEntry(EntryInfoEntity entryInfoEntity,String department) {
        // TODO Auto-generated method stub
        if(department.equals("装置开发部")){
            String classfy2 = entryInfoEntity.getClassfy2();
            return StringUtils.isNotBlank(classfy2);
        }else{
            return true;
        }
        
    }

    @Override
    public Set<String> getEntrySourcesByClassify(String classifyID,String writeType) {
        // TODO Auto-generated method stub
        if(classifyID == null){
            return new HashSet<>();
        }
        List<String> productIDs = new ArrayList<>();
        getProductClassfyList(classifyID, productIDs);
        if(productIDs.isEmpty()){
            return new HashSet<>();
        }
        return entryInfoMapper.selectEntrySourcesByIDs(productIDs.stream().collect(Collectors.toSet()),null,writeType);
    }

    @Override
    public Set<String> getWriteFileNamesByClassify(String classifyID,String writeType) {
        // TODO Auto-generated method stub
        if(classifyID == null){
            return new HashSet<>();
        }
        List<String> productIDs = new ArrayList<>();
        getProductClassfyList(classifyID, productIDs);
        if(productIDs.isEmpty()){
            return new HashSet<>();
        }
        return entryInfoMapper.selectWriteTypesByIDs("di_file_name", productIDs.stream().collect(Collectors.toSet()), null,writeType);
    }

    @Override
    public EntryInfoVO getEntryByClassfyOnPage(String classfyID, Integer pageIndex, Integer pageSize) {
        // TODO Auto-generated method stub
        List<String> productidList = new ArrayList<>();
        //一级分类查询
        // if (StringUtils.isBlank(entryInfoEntityTemplate.getClassfy1())){
        getProductClassfyList(classfyID, productidList);
        Set<String> productIDSet = productidList.stream().collect(Collectors.toSet());

        int offset = (pageIndex - 1) * pageSize;

        List<ProductRelationEntity> selectInfosByProductRelationEntity = productRelationMapper.selectInfosByProductRelationEntity(
            null, 
            productIDSet, 
            null,
            null,pageSize,offset);
        List<String> idList = selectInfosByProductRelationEntity.stream().map(ProductRelationEntity::getEntryId).collect(Collectors.toList());
        List<EntryInfoEntityDO> entryInfoEntities = entryInfoMapper.selectEntryInfosByIDs(idList);
        EntryInfoVO entryInfoVO = new EntryInfoVO();
        entryInfoVO.setEntryInfoEntities(EntryInfoEntityDO.convertFromEntities(entryInfoEntities, EntryInfoEntityDO.newConverterForEntryInfoEntitiy()).collect(Collectors.toList()));

        String totalNumber = entryInfoMapper.countEntryIDsByConditions(null,null,productIDSet,null,null,null);

        entryInfoVO.setTotalSize(Integer.parseInt(totalNumber));    // 暂不考虑int溢出的问题
        return entryInfoVO;
    }

    @Override
    public EntryInfoGroupVO makeGroupForEntryInfosOnFile(FileInputStreamEntity multipartFile,Collection<String> replicatedTargetAttributes,String encoding) {

        String originalFilename = multipartFile.getFileName();
        Collection<EntryInfoEntity> entryInfoEntities = null;
        try {
            KeyValueArguments<String> keyValueArguments = new KeyValueArguments<>();
            keyValueArguments.set("encoding", encoding);
            entryInfoEntities = excelUtils.parseFileToEntity(EntryInfoEntity.class, multipartFile.getInputStream(), originalFilename, keyValueArguments).getParsedObjects();
        } catch (Exception e) {
            throw new RuntimeException(String.format("解析文件词条时出现异常, 无法进行词条去重分组, 异常信息为: %s", e.getMessage()),e);
        }
        if(entryInfoEntities == null || entryInfoEntities.isEmpty()){
            throw new RuntimeException(String.format("没有从文件中解析到任何词条, 请检查文件"));
        }
        return this.makeGroupForEntryInfos(entryInfoEntities,replicatedTargetAttributes);
    }

    @Override
    public EntryInfoGroupVO makeGroupForEntryInfos(Collection<EntryInfoEntity> entryInfoEntities,Collection<String> replicatedTargetAttributes) {
        if(entryInfoEntities == null){
            throw new RuntimeException("没有提供用于分组的词条");
        }
        if(replicatedTargetAttributes == null){
            throw new RuntimeException("没有提供用于分组的条件");
        }
        DefaultEntryGroupbyStrategy entryGroupbyStrategy = new DefaultEntryGroupbyStrategy();
        entryGroupbyStrategy.addTargetAttributes(replicatedTargetAttributes);
        Collection<List<EntryInfoEntity>> entryInfoGroups = entryUtils.makeGroupForEntryInfoEntities(entryInfoEntities, entryGroupbyStrategy);
        if(entryInfoGroups == null){
            throw new RuntimeException("词条分组失败, entryInfoGroups为null, 联系研发");
        }
        /** 建立parentID和childIDs之间的关联 */
        Map<String,List<String>> idRelationMap = new HashMap<>();
        Collection<EntryInfoEntity> notReplicatedEntryInfos = new ArrayList<>(entryInfoGroups.size());
        for(List<EntryInfoEntity> entities : entryInfoGroups){
            if(entities.isEmpty()){
                continue;
            }
            EntryInfoEntity parentEntryInfo = entities.get(0);
            String parentID = parentEntryInfo.getId();
            try {
                EntryUtils.checkEntryID(parentEntryInfo, true);
            } catch (Exception e) {
                throw new RuntimeException(String.format("该组词条信息中存在词条的id列的内容为空字符串, 请检查输入", entities.toString()),e);
            }
            /* 父词条为去重后保留的词条, child为其他和它重复的词条 */
            List<EntryInfoEntity> childList = entities.subList(1, entities.size());
            for(EntryInfoEntity child : childList){
                try {
                    EntryUtils.checkEntryID(child, true);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("该组词条信息中存在词条的id列的内容为空字符串, 请检查输入", entities.toString()),e);
                }
            }
            parentEntryInfo.setChildren(childList);
            notReplicatedEntryInfos.add(parentEntryInfo);   
            idRelationMap.put(parentID,childList.stream().map(EntryInfoEntity::getId).collect(Collectors.toList()));
        }

        EntryInfoGroupVO entryInfoGroupVO = new EntryInfoGroupVO(notReplicatedEntryInfos, idRelationMap);
        /* 需要做一个一致性校验, 去重后的excel文件和对应的ID关联表是否一致 */
        return entryInfoGroupVO;
    }

    @Override
    public Collection<EntryInfoEntity> parseFileToEntryInfos(FileInputStreamEntity multipartFile, KeyValueArguments<String> kwargs) {
        // TODO Auto-generated method stub
        try {
            return excelUtils.parseFileToEntity(EntryInfoEntity.class,multipartFile.getInputStream(),multipartFile.getFileName(),kwargs).getParsedObjects();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e) ;
        }
    }

    @Override
    public ValueDifferenceVO<EntryInfoEntity> compareEntryInfosBetweenFiles(FileInputStreamEntity file1, FileInputStreamEntity file2,Collection<String> targetAttributes,KeyValueArguments<String> kwargs) {
        // TODO Auto-generated method stub
        Collection<EntryInfoEntity> entryCollection1 = this.parseFileToEntryInfos(file1, kwargs);
        Collection<EntryInfoEntity> entryCollection2 = this.parseFileToEntryInfos(file2, kwargs);
        DefaultEntryGroupbyStrategy entryGroupbyStrategy = new DefaultEntryGroupbyStrategy();
        entryGroupbyStrategy.addTargetAttributes(targetAttributes);
        return entryUtils.compareEntryInfos(entryCollection1, entryCollection2, entryGroupbyStrategy);
    }

    private boolean containsRestrictedFieldNames(Collection<String> fieldNames){
        for(String fieldName : fieldNames){
            if(ConstantInterface.constructEntryName().containsKey(fieldName)){
                continue;
            }
            return true;
        }
        return false;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public UpdateEntryInfoByFileVO updateEntryInfosByFile(FileInputStreamEntity fileInputStreamEntitiy, Collection<String> fieldNames,KeyValueArguments<String> keyValueArguments) {

        try {
            if(fileInputStreamEntitiy == null){
                return UpdateEntryInfoByFileVO.newInstance("更新词条信息失败", ExceptionVO.newInstance("没有提供更新词条的文件", "请检查是否提供词条文件"));
            }
            Collection<EntryInfoEntity> entryInfos = this.parseFileToEntryInfos(fileInputStreamEntitiy, keyValueArguments);  // 属性为null: 没有对应的列，单元格没有值   
            if(entryInfos == null){
                return UpdateEntryInfoByFileVO.newInstance("更新词条失败", ExceptionVO.newInstance("解析文件失败, 未获取词条信息","检查提供的文件格式是否存在异常"));
            }
            if(entryInfos.isEmpty()){
                return UpdateEntryInfoByFileVO.newInstance("更新词条失败", ExceptionVO.newInstance("解析文件成功, 但未获取任何词条","请提供存在词条的文件"));
            }
            if(this.containsRestrictedFieldNames(fieldNames)){
                return UpdateEntryInfoByFileVO.newInstance("词条更新失败", ExceptionVO.newInstance("禁止选择更新词条ID列的内容"));
            }
            Map<String, String> fieldColumnNameMap = ConstantInterface.constructEntryName();    // EntryInfoEntity的属性名与表格的列名的映射关系

            if(fieldColumnNameMap == null || fieldColumnNameMap.isEmpty()){
                return UpdateEntryInfoByFileVO.newInstance("更新词条失败", ExceptionVO.newInstance("系统服务异常, 没有对应文件的列名的映射关系","联系研发"));
            }
            // 获取要更新的属性名
            Collection<String> targetFieldNames = fieldNames;
            Class<EntryInfoEntity> clazz = EntryInfoEntity.class;
            // get方法和set方法映射关系
            Map<Method,Method> methodMap = new HashMap<>();

            targetFieldNames.forEach((fieldName)->{

                Method getMethod = MethodUtils.acquireMethod(clazz, fieldName, MethodUtils.DEFAULT_GET_METHOD_NAME_GENERATOR);
                Method setMethod = MethodUtils.acquireMethod(clazz, fieldName, MethodUtils.DEFAULT_SET_METHOD_NAME_GENERATOR,getMethod.getReturnType());
                methodMap.put(getMethod, setMethod);

            });
            if(methodMap == null || methodMap.isEmpty()){
                return UpdateEntryInfoByFileVO.newInstance("更新词条失败", ExceptionVO.newInstance("没有找到对应的更新方法, 无法更新词条","请检查提供的文件的列名是否正确"));
            }
            // 遍历更新词条信息
            UpdateEntryInfoByFileVO updateTranslationByFileVO = UpdateEntryInfoByFileVO.newInstance("词条更新成功");

            Collection<EntryInfoEntity> updateTemplates = new ArrayList<>();
            entryInfos.stream().forEach((entry)->{
                Set<MethodEntity> setMethodValues = new HashSet<>();
                methodMap.forEach((getMethod,setMethod)->{
                    MethodEntity getMethodEntity = new MethodEntity(getMethod);
                    Object fileValue = getMethodEntity.invoke(entry);
                    setMethodValues.add(new MethodEntity(setMethod, fileValue));
                });
                EntryInfoEntity updateEntryInfoTemplate = entryStorageService.buildUpdateEntryInfoTemplate(entry.getId(), setMethodValues);
                updateTemplates.add(updateEntryInfoTemplate);
            });
            // sql操作
            boolean updateEntryInfosResult = entryStorageService.updateEntryInfos(updateTemplates);
            if(!updateEntryInfosResult){
                /* 更新失败 */
                updateTranslationByFileVO.addException(ExceptionVO.newInstance(String.format("词条更新失败"),"存库异常,检查词条是否存在"));
                updateTranslationByFileVO.setGlobalMessage("词条更新存库失败");
            }
            // 更新t_translate表信息
            Consumer<TranslateEntity> translateEntiityProcessor = new Consumer<TranslateEntity>() {
                @Override
                public void accept(TranslateEntity translateEntity) {
                    // TODO Auto-generated method stub
                    translateEntity.setTranslateState("3");
                }
            };
            Collection<String> updateTranslationTypes = new HashSet<>();
            Map<String, String> translateMap = ConstantInterface.translateMap();
            fieldNames.forEach((fieldName)->{if(translateMap.containsKey(fieldName)){updateTranslationTypes.add(translateMap.get(fieldName));}});
            
            if(updateTemplates != null && !updateTranslationTypes.isEmpty() && keyValueArguments != null){
                keyValueArguments.set("translateProcessor", translateEntiityProcessor);
                boolean updateTransResult = translationStorageService.updateEntryInfoTranslations(updateTemplates,updateTranslationTypes,keyValueArguments);
                if(!updateTransResult){
                    /* 更新失败 */
                    updateTranslationByFileVO.addException(ExceptionVO.newInstance(String.format("词条的翻译更新失败"),"存库异常, 检查提供的excel文件的数据, 或稍后再试"));
                    updateTranslationByFileVO.setGlobalMessage("词条翻译更新存库失败");
                }
            }

            return updateTranslationByFileVO;
        } catch (Exception e) {
            String errorMessage = String.format("更新词条时发生异常, 异常信息为: %s", e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage,e);
        }
    }
 
    @Override
    public TaskCheckResultVO checkBeforeUpdateTranslationByFile(
        FileInputStreamEntity unTranslateFile,
        FileInputStreamEntity translatedFile,
        FileInputStreamEntity idRelationFile,
        TaskRequest taskRequest,
        String encodingForUnTranslatedFile,
        String encodingForTranslatedFile
    ) {
        // TODO Auto-generated method stub
        try {
            TaskCheckResultVO taskCheckResultVO = new TaskCheckResultVO();
            if(taskRequest == null){
                return taskCheckResultVO;
            }
            KeyValueArguments<String> keyValueArguments1 = new KeyValueArguments<>();
            keyValueArguments1.set("emptyAsValue", taskRequest.getOptions().isEmptyStringAsValue());
            keyValueArguments1.set("encoding", encodingForUnTranslatedFile);

            KeyValueArguments<String> keyValueArguments2 = new KeyValueArguments<>();
            keyValueArguments2.set("emptyAsValue", taskRequest.getOptions().isEmptyStringAsValue());
            keyValueArguments2.set("encoding", encodingForTranslatedFile);

            ParseFileInfo<EntryInfoEntity> unTranslatedFileInfo = excelUtils.parseFileToEntity(EntryInfoEntity.class, unTranslateFile.getInputStream(), unTranslateFile.getFileName(), keyValueArguments1);    
            ParseFileInfo<EntryInfoEntity> translatedFileInfo = excelUtils.parseFileToEntity(EntryInfoEntity.class, translatedFile.getInputStream(), translatedFile.getFileName(), keyValueArguments2);    

            Collection<EntryInfoEntity> entryInfoOnUnTranslatedFile = unTranslatedFileInfo.getParsedObjects();
            Collection<EntryInfoEntity> entryInfoOnTranslatedFile = translatedFileInfo.getParsedObjects();

            Collection<CheckWorkNode<?>> checkWorkNodes = new ArrayList<>();
            List<TaskRule> taskRules = taskRequest.getTaskRules();
            for(TaskRule taskRule : taskRules){
                String taskType = taskRule.getTaskType();
                if(taskType.equals("IdMatch")){
                    CompareEntityWorkNode<EntryInfoEntity> checkWorkNode = checkFilePipeline.buildCheckMissingEntryWorkNode(entryInfoOnUnTranslatedFile, entryInfoOnTranslatedFile);
                    checkWorkNodes.add(checkWorkNode);
                }else if(idRelationFile != null && taskType.equals("filterIdMap")){
                    Map<String,Set<String>> idRelationMap = JSONUtils.parseToJson(idRelationFile.getInputStream(), StandardCharsets.UTF_8.name(), new TypeToken<Map<String,Set<String>>>() {}.getType());
                    CheckMissingEntryRelationWorkNode checkMissingEntryRelationWorkNode = checkFilePipeline.buildCheckMissingEntryRelationWorkNode(entryInfoOnTranslatedFile, idRelationMap);
                    checkWorkNodes.add(checkMissingEntryRelationWorkNode);
                }else if(taskType.equals("checkFields")){
                    Map<String, Object> params = taskRule.getParams();
                    Object checkFields = params.get("checkFields");
                    List checkFieldList = (List)(checkFields);  // type safety不管
                    BuildOption buildOption = new BuildOption();
                    String[] fileColumns = translatedFileInfo.getColumnName();
                    Collection<String> attributes = excelUtils.getAttributesByFileColumns(ConstantInterface.constructEntryName(), fileColumns);
                    buildOption.setAvaliableAttributes(attributes.stream().collect(Collectors.toSet()));
                    CheckEntryNotMatchWorkNode<EntryInfoEntity> checkEntryNotMatchWorkNode = checkFilePipeline.buildCheckEntryNotMatchWorkNode(entryInfoOnTranslatedFile, checkFieldList,buildOption);
                    checkWorkNodes.add(checkEntryNotMatchWorkNode);
                }else if(taskType.equals("checkSpecialChar")){
                    Map<String, Object> params = taskRule.getParams();
                    List translateAttributes =(List) params.get("translateAttributes");
                    BuildOption buildOption = new BuildOption();

                    String[] fileColumns = translatedFileInfo.getColumnName();
                    Collection<String> attributes = excelUtils.getAttributesByFileColumns(ConstantInterface.constructEntryName(), fileColumns);
                    buildOption.setAvaliableAttributes(attributes.stream().collect(Collectors.toSet()));
                    CheckSpecialCharacterWorkNode<EntryInfoEntity> checkSpecialCharacterWorkNode = checkFilePipeline.buildCheckSpecialCharacterWorkNode(entryInfoOnTranslatedFile, translateAttributes,buildOption);
                    checkWorkNodes.add(checkSpecialCharacterWorkNode);
                }else if(taskType.equals("checkMaxLength")){
                    Map<String, Object> params = taskRule.getParams();
                    List translateAttributes =(List) params.get("translateAttributes");
                    BuildOption buildOption = new BuildOption();
                    String[] fileColumns = translatedFileInfo.getColumnName();
                    Collection<String> attributes = excelUtils.getAttributesByFileColumns(ConstantInterface.constructEntryName(), fileColumns);
                    buildOption.setAvaliableAttributes(attributes.stream().collect(Collectors.toSet()));
                    CheckTranslationMaxLengthWorkNode translationMaxLengthWorkNode = checkFilePipeline.buildTranslationMaxLengthWorkNode(entryInfoOnTranslatedFile,translateAttributes,buildOption);
                    
                    checkWorkNodes.add(translationMaxLengthWorkNode);
                }else if(taskType.equals("backfillFields")){
                    Map<String, Object> params = taskRule.getParams();
                    List checkFields =(List) params.get("backfillFields");
                    
                    CheckColumnExistWorkNode checkColumnExistWorkNode = checkFilePipeline.buildCheckColumnExistWorkNode(Arrays.asList(translatedFileInfo.getColumnName()), checkFields);
                    checkWorkNodes.add(checkColumnExistWorkNode);
                }
            }
            ExecuteOption executeOption = new ExecuteOption();
            executeOption.setReturnAfterOnceFailed(taskRequest.getOptions().isFailFast());
            checkFilePipeline.execute(checkWorkNodes,executeOption);
            Collection<String> notPassedMessages = new ArrayList<>();
            Collection<Issue> issues = new ArrayList<>();
            Attachments attachments = new Attachments();
            boolean canBackFill = true;
            for(CheckWorkNode<?> checkNode : checkWorkNodes){
                if(checkNode.isFinished() && !checkNode.isPassed()){
                    String convertResultToJSONString = checkNode.convertResultToJSONString();
                    notPassedMessages.add(convertResultToJSONString);
                    issues.add(checkNode.getIssue());
                    Issue.Level level = checkNode.getIssue().getLevel();
                    if(level == Issue.Level.ERROR || level == Issue.Level.FATAL){
                        canBackFill = false;
                    }
                }

            }
            if(!notPassedMessages.isEmpty()){
                Collection<Object> wholeMessage = new ArrayList<>();
                notPassedMessages.stream().forEach((message)->{
                    Gson gson = new Gson();
                    Object object =  gson.fromJson(message, Object.class);
                    wholeMessage.add(object);
                });
                String logDir=  "/app/cache/log/";
                // String logDir = "E:\\work\\translation\\translationTool\\translationtool\\cache\\log\\";
                String logID = commonUtils.getUUID();
                // String logID = "f5698f09-99d3-462e-b835-cce5447d1c67";
                String filePath = logDir + logID + ".log";
                JSONUtils.exportJson(wholeMessage, filePath);
                Attachment attachment = new Attachment();
                attachment.setFileName(logID + ".log");
                attachment.setDownloadUrl(filePath);
                attachments.setIssueLog(attachment);
            }
            taskCheckResultVO.setSuccess(true);
            taskCheckResultVO.setCanBackFill(canBackFill);
            taskCheckResultVO.setIssues(issues);
            taskCheckResultVO.setAttachments(attachments);
            // 日志量不可控，但是日志量不会特别大，但是校验选项增加的概率较高，并且需要保证低运维成本，选择mongodb
            return taskCheckResultVO;

        } catch (Exception e) {
            throw new RuntimeException(e) ;
        }
        

    }

    @Override
    public void getLog(ByteArrayOutputStream outputStream, String logPath) {
        // 使用 try-with-resources 自动关闭流，避免资源泄漏
        try (FileInputStream fileInputStream = new FileInputStream(new File(logPath))) {
            // 定义缓冲区（8KB 是常用的缓冲区大小，平衡性能和内存占用）
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            // 循环读取文件内容到缓冲区，直到文件末尾（返回 -1）
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                // 将缓冲区中读取到的字节写入输出流
                outputStream.write(buffer, 0, bytesRead);
            }
            
            // 刷新输出流，确保所有数据都被写入
            outputStream.flush();
            return;
        } catch (FileNotFoundException e) {
            // 更友好的异常提示，而非仅打印堆栈
            throw new RuntimeException("日志文件未找到：" + logPath);
        } catch (IOException e) {
            // 捕获所有 IO 相关异常
            throw new RuntimeException("读取/写入日志文件时发生 IO 错误：" + logPath);
        }
    }
    

}




