package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.*;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.service.I18nService;
import com.shr.translationtoolservice.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

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
    private EntryVersionMapper entryVersionMapper;

    @Override
    public List<EntryInfoEntity> getEntryByVersion(EntryInfoEntity entryInfoEntity1, Integer pageIndex, Integer pageSize) {
        int offset = 0;
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            offset = (pageIndex - 1) * pageSize;

        }
        processTranslationState(entryInfoEntity1);



        List<EntryInfoEntity> entryByVersion = entryInfoMapper.getEntryByVersion(entryInfoEntity1, offset, pageSize);


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
        return entryByVersion;
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
                    int transInsert = translateMapper.insert(translateEntities);

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

        int update = entryInfoMapper.updateEntryInfo(entryInfoEntity);
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
            TranslateEntity translateEntity;
            translateEntity = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getEnglish(), department);
            if (Objects.isNull(translateEntity)) {
                translateEntity = new TranslateEntity();
                id = commonUtils.getUUID();
                translateEntity.setId(id);
                translateEntity.setEntry(entryInfoEntity.getEntry());
                translateEntity.setVersionID(entryInfoEntity.getVersionID());
                translateEntity.setTranslate(entryInfoEntity.getEnglish());
                translateEntity.setPublicState(0);
                translateEntity.setTranslateState("3");
                translateEntity.setDeleteState(0);
                translateEntity.setVisualRange(department);
                translateEntity.setLastUseTime(new Date(System.currentTimeMillis()));
                translateEntity.setType(ConstantInterface.ENGLISH);
                translateMapper.insert(translateEntity);

            } else {
                translateEntity.setLastUseTime(new Date(System.currentTimeMillis()));
                id = translateEntity.getId();
                translateMapper.updateById(translateEntity);
            }
            entryInfoEntity.setEnTransId(id);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
            transType = ConstantInterface.FRENCH;
            TranslateEntity translateEntity;
            translateEntity = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getFrench(), department);
            if (Objects.isNull(translateEntity)) {
                translateEntity = new TranslateEntity();
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
                translateEntity.setLastUseTime(new Date(System.currentTimeMillis()));
                translateMapper.insert(translateEntity);
            } else {
                translateEntity.setLastUseTime(new Date(System.currentTimeMillis()));
                id = translateEntity.getId();
                translateMapper.updateById(translateEntity);
            }
            entryInfoEntity.setFraTransId(id);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
            transType = ConstantInterface.SPANISH;
            TranslateEntity translateEntity;
            translateEntity = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getSpanish(), department);
            if (Objects.isNull(translateEntity)) {
                translateEntity = new TranslateEntity();
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
                translateEntity.setLastUseTime(new Date(System.currentTimeMillis()));
                translateMapper.insert(translateEntity);
            } else {
                translateEntity.setLastUseTime(new Date(System.currentTimeMillis()));
                id = translateEntity.getId();
                translateMapper.updateById(translateEntity);
            }
            entryInfoEntity.setSpaTransId(id);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
            transType = ConstantInterface.RUSSIAN;
            TranslateEntity translateEntity;
            translateEntity = translateMapper.getRepTrans(entryInfoEntity.getEntry(), transType, entryInfoEntity.getRussian(), department);
            if (Objects.isNull(translateEntity)) {
                translateEntity = new TranslateEntity();
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
                translateEntity.setLastUseTime(new Date(System.currentTimeMillis()));
                translateMapper.insert(translateEntity);
            } else {
                translateEntity.setLastUseTime(new Date(System.currentTimeMillis()));
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
            insert += translateMapper.insert(translateEntity);
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
                importExcleEntries = excelUtils.readZZExcelToEntity(ImportExcleEntry.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
                zzEntryHandle(importExcleEntries, entryEntitys, userName, taskInfoEntity, fileName);
            } else if (fileName.contains("_common.xlsx")) {
                entryEntitys = excelUtils.readExcelToEntity(EntryInfoEntity.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
                ptEntryHandle(entryEntitys, userName, taskInfoEntity, fileName);
            } else if (fileName.contains("_jk.xlsx")) {
                entryEntitys = excelUtils.readExcelToEntity(EntryInfoEntity.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
                ptEntryHandle(entryEntitys, userName, taskInfoEntity, fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return entryProcessUtils.buildRepeEntry(entryEntitys, taskInfoEntity.getTranslateType());
    }

    private void ptEntryHandle(List<EntryInfoEntity> entryEntitys, String userName, TaskInfoEntity taskInfoEntity, String fileName) {
        Date date = new Date(System.currentTimeMillis());
        for (EntryInfoEntity entryInfoEntity : entryEntitys) {

            entryInfoEntity.setEntryLength(entryInfoEntity.getEntry().length());
            entryInfoEntity.setUpdate(userName);
            entryInfoEntity.setUpdateTime(date);
            entryInfoEntity.setProductID(taskInfoEntity.getProductId());
            entryInfoEntity.setIsDelete(0);
            entryInfoEntity.setIsPublic(0);
            entryInfoEntity.setEntryState(1);
            entryInfoEntity.setTaskId(taskInfoEntity.getId());
            entryInfoEntity.setId(commonUtils.getUUID());
            if (Objects.nonNull(taskInfoEntity)) {
                entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
            }
            entryInfoEntity.setImportType(ConstantInterface.EXCEL);
            entryInfoEntity.setWriteType(ConstantInterface.EXCEL);
            entryInfoEntity.setEntrySource("import : " + ConstantInterface.EXCEL + " ; fileName" + fileName);
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
            String productTableName = "t_entry_info";
            caseExisttry(entryInfoEntity, taskInfoEntity, importExcleEntry, productTableName);
            entryEntitys.add(entryInfoEntity);
        }
    }

    @Override
    @Transactional
    //用is_exist 区分 已存在和新词条，已存在升级词条版本在插入
    public String insertEntry(List<EntryInfoEntity> entryInfoEntities, String taskID, HttpServletRequest request) {
        long start = System.currentTimeMillis();
        int insert = 0;
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);

        TaskInfoEntity taskInfoEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

        //ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(taskInfoEntity.getProductId());
        // String entryRelationTableName = productTableEntity.getEntryRelationTableName();

        insert += insertRelation(entryInfoEntities, taskInfoEntity, department);
        //insert child
     /*   for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            if (!CollectionUtils.isEmpty(entryInfoEntity.getChildren())){
                insert +=insertRelation(entryInfoEntity.getChildren(),taskInfoEntity,department);
            }


        }*/
        if (0 == insert) {
            return ErrorCodeList.INSERT_ERROR;
        }
        long end = System.currentTimeMillis();
        log.info(" ===== add entry number  :" + insert + " == and time : {}=== ", end - start);
        return ConstantInterface.OK_STR;
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
                    if (StringUtils.isNotBlank(entryInfoEntity.getEnTransId())) {
                        entryInfoEntity1.setEnTransId(entryInfoEntity.getEnTransId());
                    } else if (StringUtils.isNotBlank(entryInfoEntity.getRuTransId())) {
                        entryInfoEntity1.setRuTransId(entryInfoEntity.getRuTransId());
                    } else if (StringUtils.isNotBlank(entryInfoEntity.getFraTransId())) {
                        entryInfoEntity1.setFraTransId(entryInfoEntity.getFraTransId());
                    } else if (StringUtils.isNotBlank(entryInfoEntity.getSpaTransId())) {
                        entryInfoEntity1.setSpaTransId(entryInfoEntity.getSpaTransId());
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
    public void entryExportByCondition(ExcelExportVO excelExportVO, HttpServletResponse response, String taskID) {
        EntryInfoEntity entryInfoEntity = excelExportVO.getEntryInfoEntity();
        List<String> columnNames = excelExportVO.getColumnNames();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmss");
        Date date = new Date();
        String da = format.format(date);
        String excelName = excelExportVO.getExcelName() + "_" + da;
        TaskInfoEntity taskEntity = null;
        if (StringUtils.isNotBlank(taskID)) {
            taskEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

        }

        List<EntryInfoEntity> entryInfoEntities;
        if (!CollectionUtils.isEmpty(excelExportVO.getEntryInfoEntities())) {
            entryInfoEntities = excelExportVO.getEntryInfoEntities();
        } else {
            entryInfoEntities = entryInfoMapper.getEntryInfo(entryInfoEntity);
        }
        for (EntryInfoEntity entryInfoEntity1 : entryInfoEntities) {
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
        List<String> exportFields = new ArrayList<>();
        for (String column : columnNames) {
            exportFields.add(ConstantInterface.EXCEL_LIST_NAME_MAP().get(column));

        }
        excelUtils.getWorkBook(entryInfoEntities, response, exportFields, columnNames, excelName);

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
    public String addProductRelation(List<ProductRelationEntity> relationEntity) {
        for (ProductRelationEntity relationEntity1 : relationEntity) {
            relationEntity1.setId(commonUtils.getUUID());
            productRelationMapper.insert(relationEntity1);
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

            entryInfoEntities = excelUtils.readExcelToEntity(EntryInfoEntity.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
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
    public String setInfoByEntryList(List<EntryInfoEntity> entryInfoEntities, String translateType, String writeType, boolean tag, boolean comment, String fileName) {

        switch (writeType) {
            case ConstantInterface.DI:
                diUtils.writeDiEntry(entryInfoEntities, fileName, translateType, tag, comment);
                break;
            case ConstantInterface.TS:
                tsUtils.writeTSEntry(entryInfoEntities, fileName, tag);
                break;
            case ConstantInterface.DEFAUT:
                i18nService.setInfoByEntryList(entryInfoEntities, translateType, tag, comment);
                break;

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
            entryEntitys = excelUtils.readExcelToEntity(EntryInfoEntity.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
            if (CollectionUtils.isEmpty(entryEntitys)) {
                return null;
            }
            for (EntryInfoEntity entryInfoEntity1 : entryEntitys) {
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
                TranslateEntity repTrans = translateMapper.getRepTrans(entryInfoEntity1.getEntry(), transType, trans, department);
                TranslateEntity translateEntity;
                if (Objects.nonNull(repTrans)) {
                    translateEntity = repTrans;
                } else {
                    translateEntity = new TranslateEntity();
                    translateEntity.setId(commonUtils.getUUID());
                    translateEntity.setEntry(entryInfoEntity1.getEntry());
                    translateEntity.setTranslate(trans);
                    translateEntity.setLastUseTime(new Date(System.currentTimeMillis()));
                    translateEntity.setTranslateState("3");
                    translateEntity.setDeleteState(0);
                    translateEntity.setVisualRange(department);
                    translateEntity.setPublicState(0);
                }

                switch (transType) {
                    case ConstantInterface.ENGLISH:
                        if (StringUtils.isBlank(entryInfoEntity1.getEnglish())){
                            continue;
                        }
                        if (StringUtils.isBlank(originalEntry.getEnTransId())) {
                            entryInfoEntity1.setEnTransId(translateEntity.getId());
                            translateMapper.insert(translateEntity);
                            entryInfoMapper.updateById(entryInfoEntity1);
                            continue;
                        }
                        TranslateEntity originEnTrans = translateMapper.selectById(originalEntry.getEnTransId());
                        if (Objects.isNull(originEnTrans) || !entryInfoEntity1.getEnglish().equals(originEnTrans.getTranslate())) {
                            entryInfoEntity1.setEnTransId(translateEntity.getId());
                            translateMapper.insert(translateEntity);
                            entryInfoMapper.updateById(entryInfoEntity1);
                            continue;
                        }
                        break;
                    case ConstantInterface.SPANISH:
                        if (StringUtils.isBlank(entryInfoEntity1.getSpanish())){
                            continue;
                        }
                        if (StringUtils.isBlank(originalEntry.getSpaTransId())) {
                            entryInfoEntity1.setSpaTransId(translateEntity.getId());
                            translateMapper.insert(translateEntity);
                            entryInfoMapper.updateById(entryInfoEntity1);
                            continue;
                        }
                        TranslateEntity originSpaTrans = translateMapper.selectById(originalEntry.getSpaTransId());
                        if (Objects.isNull(originSpaTrans) || !entryInfoEntity1.getSpanish().equals(originSpaTrans.getTranslate())) {
                            entryInfoEntity1.setSpaTransId(translateEntity.getId());
                            translateMapper.insert(translateEntity);
                            entryInfoMapper.updateById(entryInfoEntity1);
                            continue;
                        }
                        break;
                    case ConstantInterface.RUSSIAN:
                        if (StringUtils.isBlank(entryInfoEntity1.getRussian())){
                            continue;
                        }
                        if (StringUtils.isBlank(originalEntry.getRuTransId())) {
                            entryInfoEntity1.setRuTransId(translateEntity.getId());
                            translateMapper.insert(translateEntity);
                            entryInfoMapper.updateById(entryInfoEntity1);
                            continue;
                        }
                        TranslateEntity originRuTrans = translateMapper.selectById(originalEntry.getRuTransId());
                        if (Objects.isNull(originRuTrans) ||!entryInfoEntity1.getRussian().equals(originRuTrans.getTranslate())) {
                            entryInfoEntity1.setRuTransId(translateEntity.getId());
                            translateMapper.insert(translateEntity);
                            entryInfoMapper.updateById(entryInfoEntity1);
                            continue;
                        }
                        break;
                    case ConstantInterface.FRENCH:
                        if (StringUtils.isBlank(entryInfoEntity1.getFrench())){
                            continue;
                        }
                        if (StringUtils.isBlank(originalEntry.getFraTransId())) {
                            entryInfoEntity1.setFraTransId(translateEntity.getId());
                            translateMapper.insert(translateEntity);
                            entryInfoMapper.updateById(entryInfoEntity1);
                            continue;
                        }
                        TranslateEntity originFraTrans = translateMapper.selectById(originalEntry.getFraTransId());
                        if (Objects.isNull(originFraTrans) || !entryInfoEntity1.getFrench().equals(originFraTrans.getTranslate())) {
                            entryInfoEntity1.setFraTransId(translateEntity.getId());
                            translateMapper.insert(translateEntity);
                            entryInfoMapper.updateById(entryInfoEntity1);
                            continue;
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
        List<EntryInfoEntity> entryEntitys = new ArrayList<>();
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
        return entryEntitys;

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

        translateEntity.setDeleteState(0);
        translateEntity.setPublicState(0);
        translateEntity.setVisualRange(department);
        translateEntity.setId(translateID);
        translateEntity.setTranslate(translate);
        if (Objects.isNull(translateLength)) {
            translateEntity.setCharLength(translate.length());
        }

        translateMapper.insert(translateEntity);

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
                if (StringUtils.isNotBlank(importExcleEntry.getRussia())) {
                    entryInfoEntity.setRussian(importExcleEntry.getRussia());
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

        //读取excle转换的实体
        List<ImportExcleVO> importExcleEntries = new ArrayList<>();
        try {

            importExcleEntries = excelUtils.readExcelToEntity(ImportExcleVO.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());
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
        localTranslate.setSource("本地翻译-部门");
        translateEntityList.add(localTranslate);

        //本地翻译 publicState：公司
        List<TranslateEntity> companyTranslates = translateMapper.getSuggestTrans(name, type, "公司");
        Translate comLocalTranslate = translateUtils.localTranslate(name, type, companyTranslates);
        comLocalTranslate.setSource("本地翻译-公司");
        translateEntityList.add(comLocalTranslate);

        //本地翻译 publicState：公司
        List<TranslateEntity> qtTranslates = translateMapper.getSuggestTrans(name, type, "");
        Translate qtLocalTranslate = translateUtils.localTranslate(name, type, qtTranslates);
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


}




