package com.shr.translationtoolservice.service.impl;

import cn.afterturn.easypoi.cache.manager.IFileLoader;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.*;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.events.Event;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
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
    private EntryProcessUtils entryProcessUtils;

    @Override
    public List<EntryInfoEntity> getEntryByVersion(EntryInfoEntity entryInfoEntity1, Integer pageIndex, Integer pageSize) {
        int offset = 0;
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            offset = (pageIndex - 1) * pageSize;

        }
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
        Date date = new Date();
        entryInfoEntity.setUpdate(userName);
        entryInfoEntity.setUpdateTime(date);

        //翻译如果更新的情况下 查找翻译表存在相同的 挂在已存在的额id 没有新增一个翻译
        updateTrans(entryInfoEntity);

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
    private void updateTrans(EntryInfoEntity entryInfoEntity) {
        String id = "";
        String transType = "";
        if (StringUtils.isNotBlank(entryInfoEntity.getEnglish()) ){
            transType = ConstantInterface.ENGLISH;
            TranslateEntity translateEntity ;
            translateEntity = translateMapper.getRepTrans(entryInfoEntity.getEntry(),transType,entryInfoEntity.getEnglish());

            if (Objects.isNull(translateEntity)){
                translateEntity = new TranslateEntity();
                id = commonUtils.getUUID();
                translateEntity.setId(id);
                translateEntity.setEntry(entryInfoEntity.getEntry());
                translateEntity.setVersionID(entryInfoEntity.getVersionID());
                translateEntity.setTranslate(entryInfoEntity.getEnglish());
                translateEntity.setPublicState(0);
                translateEntity.setTranslateState("3");
                translateEntity.setDeleteState(0);
                translateEntity.setType(ConstantInterface.ENGLISH);
                translateMapper.insert(translateEntity);

            }else {
                id = translateEntity.getId();
            }
            entryInfoEntity.setEnTransId(id);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getFrench()) ){
            transType = ConstantInterface.FRENCH;
            TranslateEntity translateEntity ;
            translateEntity = translateMapper.getRepTrans(entryInfoEntity.getEntry(),transType,entryInfoEntity.getFrench());
            if (Objects.isNull(translateEntity)){
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
                translateMapper.insert(translateEntity);
            }else {
                id = translateEntity.getId();
            }
            entryInfoEntity.setFraTransId(id);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getSpanish()) ){
            transType = ConstantInterface.SPANISH;
            TranslateEntity translateEntity ;
            translateEntity = translateMapper.getRepTrans(entryInfoEntity.getEntry(),transType,entryInfoEntity.getSpanish());
            if (Objects.isNull(translateEntity)){
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
                translateMapper.insert(translateEntity);
            }else {
                id = translateEntity.getId();
            }
            entryInfoEntity.setSpaTransId(id);
        }
        if (StringUtils.isNotBlank(entryInfoEntity.getRussian()) ){
            transType = ConstantInterface.RUSSIAN;
            TranslateEntity translateEntity ;
            translateEntity = translateMapper.getRepTrans(entryInfoEntity.getEntry(),transType,entryInfoEntity.getRussian());
            if (Objects.isNull(translateEntity)){
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
                translateMapper.insert(translateEntity);
            }else {
                id = translateEntity.getId();
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
            if ("enTransId".equals(name) || "ruTransId".equals(name) || "fraTransId".equals(name) || "spaTransId".equals(name) ){
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
        String name = multipartFile.getOriginalFilename();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);
        //读取excle转换的实体
        List<ImportExcleEntry> importExcleEntries = new ArrayList<>();
        try {
            importExcleEntries = excelUtils.readZZExcelToEntity(ImportExcleEntry.class, multipartFile.getInputStream(), multipartFile.getOriginalFilename());

        } catch (Exception e) {
            e.printStackTrace();
        }
        //拼成词条实体
        //2区分出已存在词条 和新词条 is_exist 区分
        //3.新词条翻译预写在词条中
        List<EntryInfoEntity> entryEntitys = new ArrayList<>();
        TaskInfoEntity taskInfoEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

        //  ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(taskInfoEntity.getProductId());
        String productTableName = "t_entry_info";


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
            entryInfoEntity.setTaskId(taskID);
            entryInfoEntity.setId(commonUtils.getUUID());
            if (Objects.nonNull(taskInfoEntity)) {
                entryInfoEntity.setVersionID(taskInfoEntity.getVersionId());
            }
            entryInfoEntity.setImportType(ConstantInterface.EXCEL);
            entryInfoEntity.setEntrySource("import : " + ConstantInterface.EXCEL + " ; fileName" + name);
            caseExisttry(entryInfoEntity, taskInfoEntity, importExcleEntry, productTableName);
            entryEntitys.add(entryInfoEntity);
        }

        return  entryProcessUtils.buildRepeEntry(entryEntitys,taskInfoEntity.getTranslateType());
    }

    @Override
    //用is_exist 区分 已存在和新词条，已存在升级词条版本在插入
    public String insertEntry(List<EntryInfoEntity> entryInfoEntities, String taskID, HttpServletRequest request) {
        int insert = 0;
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        String translateType = taskInfoMapper.selectById(taskID).getTranslateType();
        TaskInfoEntity taskInfoEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

        ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(taskInfoEntity.getProductId());
        // String entryRelationTableName = productTableEntity.getEntryRelationTableName();
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            //存在升级版本
            if (1 == entryInfoEntity.getIsExist()) {
                int versionNum = entryInfoMapper.getLastVersionNum(entryInfoEntity);
                entryInfoEntity.setEntryVersion(versionNum + 1);
            }
            setTranslate(entryInfoEntity, translateType, department);

            //版本和任务不为空 才往关系表中写入
            if (StringUtils.isNotBlank(taskID) || StringUtils.isNotBlank(taskInfoEntity.getVersionId())) {
                ProductRelationEntity productRelationEntity = new ProductRelationEntity();
                productRelationEntity.setId(commonUtils.getUUID());
                productRelationEntity.setEntryId(entryInfoEntity.getId());
                productRelationEntity.setTaskId(taskID);
                productRelationEntity.setProductId(taskInfoEntity.getProductId());
                productRelationEntity.setVersionId(taskInfoEntity.getVersionId());
                productRelationMapper.insert(productRelationEntity);
            }

            insert += entryInfoMapper.insert(entryInfoEntity);

        }
        if (insert != entryInfoEntities.size()) {
            return ErrorCodeList.INSERT_ERROR;
        }
        return ConstantInterface.OK_STR;
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
    public void entryExportByCondition(ExcelExportVO excelExportVO, HttpServletResponse response) {
        EntryInfoEntity entryInfoEntity = excelExportVO.getEntryInfoEntity();
        List<String> columnNames = excelExportVO.getColumnNames();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmss");
        Date date = new Date();
        String da = format.format(date);
        String excelName = excelExportVO.getExcelName() +"_"+  da;;
        List<EntryInfoEntity> entryInfoEntities;
        if (!CollectionUtils.isEmpty(excelExportVO.getEntryInfoEntities())){
           entryInfoEntities = excelExportVO.getEntryInfoEntities();
        }else {
            entryInfoEntities = entryInfoMapper.getEntryInfo(entryInfoEntity);
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
    public String addProductRelation(List<ProductRelationEntity>  relationEntity) {
        for (ProductRelationEntity relationEntity1 : relationEntity){
            relationEntity1.setId(commonUtils.getUUID());
            productRelationMapper.insert(relationEntity1);
        }

        return ConstantInterface.OK_STR;
    }

    private void setTranslate(EntryInfoEntity entryInfoEntity, String translateType, String department) {
        String translate = "";
        Integer translateLength = null;
        String translateID = commonUtils.getUUID();
        switch (translateType) {
            case ConstantInterface.ENGLISH:
                translate = entryInfoEntity.getEnglish();
                translateLength = entryInfoEntity.getEnCharLength();
                entryInfoEntity.setEnTransId(translateID);
                break;
            case ConstantInterface.RUSSIAN:
                translate = entryInfoEntity.getRussian();
                translateLength = entryInfoEntity.getRuCharLength();
                entryInfoEntity.setRuTransId(translateID);
                break;
            case ConstantInterface.SPANISH:
                translate = entryInfoEntity.getSpanish();
                translateLength = entryInfoEntity.getSpaCharLength();
                entryInfoEntity.setSpaTransId(translateID);
                break;
            case ConstantInterface.FRENCH:
                translate = entryInfoEntity.getSpanish();
                translateLength = entryInfoEntity.getSpaCharLength();
                entryInfoEntity.setFraTransId(translateID);
                break;

        }

        addTranslateEntity(entryInfoEntity, translateType, translate, translateLength, translateID, department);


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
        List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryList(productTableName, entryInfoEntity,taskInfoEntity.getProductId());
        if (CollectionUtils.isEmpty(entryEntities)) {
            //创建新翻译
            entryInfoEntity.setIsExist(0);
            entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
            entryInfoEntity.setEntryVersion(0);
            createNewTrans(entryInfoEntity, taskInfoEntity, importExcleEntry);
        } else {
            entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
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
    public String addSingleEntry(EntryInfoEntity entryInfoEntity, HttpServletRequest request) {
        if (entryInfoEntity.isUpgrade()){
            return upgradeEnrty(entryInfoEntity,request);
        }else {
            //存在词条
            List<EntryInfoEntity> entryInfos = entryInfoMapper.getExistEntryList("t_entry_info", entryInfoEntity,entryInfoEntity.getProductID());
            if (!CollectionUtils.isEmpty(entryInfos)) {
                entryInfoEntity.setEntryVersionID(entryInfos.get(0).getEntryVersionID());
                entryInfoEntity.setEntryVersion(entryInfos.size() + 1);
                entryInfoEntity.setIsExist(1);
            }else {
                entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
                entryInfoEntity.setEntryVersion(0);
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
        entryInfoEntity.setEntrySource("UPGRADE");
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
            return ErrorCodeList.INSERT_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    private String upgradeEnrty(EntryInfoEntity entryInfoEntity, HttpServletRequest request) {
        int lastVersionNum = entryInfoMapper.getLastVersionNum(entryInfoEntity);
        entryInfoEntity.setEntryVersion(lastVersionNum +1);
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
            return ErrorCodeList.INSERT_ERROR;
        }
        return ConstantInterface.OK_STR;
    }


    @Override
    public TranslateEntities translate(String name, String type, String visualRange) {

        TranslateEntities translateEntities = new TranslateEntities();
        List<Translate> translateEntityList = new ArrayList<>();
        QueryWrapper<TLanguage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", type);
        List<TLanguage> tLanguages = languageMapper.selectList(queryWrapper);
        Translate baiduEntities = translateUtils.baiduTranslate(name, type, tLanguages);
        translateEntityList.add(baiduEntities);


        //有道翻译
        Translate youdao_Entities = translateUtils.youdaoTranslate(name, type, tLanguages);
        translateEntityList.add(youdao_Entities);

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
        Translate qtLocalTranslate = translateUtils.localTranslate(name, type, companyTranslates);
        comLocalTranslate.setSource("本地翻译-其他");
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




