package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.ImportExcleVO;
import com.shr.translationtoolservice.entity.vo.ImportResultEntryVO;
import com.shr.translationtoolservice.service.EntryTempService;
import com.shr.translationtoolservice.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;

/**
 *
 */
@Service
@Slf4j
public class EntryTempServiceImpl extends ServiceImpl<EntryTempMapper, EntryTempEntity>
        implements EntryTempService {

    @Autowired
    private EntryTempMapper entryTempMapper;

    @Autowired
    private EntryInfoMapper entryInfoMapper;

    @Autowired
    private TranslateMapper translateMapper;

    @Autowired
    private ProductTableMapper productTableMapper;

    @Autowired
    private TaskInfoMapper taskInfoMapper;

    @Value("${ConfigFile.zzUrl}")
    private String configFileZZUrl;

    @Value("${ConfigFile.commonUrl}")
    private String configFileCommonUrl;

    @Autowired
    private ExcelUtils excelUtils;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private TranslateUtils translateUtils;

    @Autowired
    private ProductRelationMapper productRelationMapper;

    @Autowired
    private TLanguageMapper languageMapper;

    @Autowired
    private YoudaoTrans youdaoTrans;

    @Autowired
    private DeepLTranslateUtils deepLTranslateUtils;

    @Override
    public String insertEntry(List<EntryInfoEntity> entities) {
        int insert = 0;
        for (EntryInfoEntity entryInfoEntity : entities) {
            if (!CollectionUtils.isEmpty(entryInfoEntity.getChildren())) {
                for (EntryInfoEntity entryInfoEntity1 : entryInfoEntity.getChildren()) {
                    entryInfoEntity.setChildren(null);
                    insert += entryInfoMapper.insert(entryInfoEntity1);
                }
            }
            entryInfoEntity.setChildren(null);
            insert += entryInfoMapper.insert(entryInfoEntity);
        }
        if (insert < entities.size()) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;

    }

    @Override
    public String updateEntryTemp(List<EntryTempEntity> tempEntities) {
        int update = 0;
        for (EntryTempEntity entryTempEntity : tempEntities) {
            List<EntryTempEntity> childTempEntry = entryTempEntity.getChildren();
            if (!CollectionUtils.isEmpty(childTempEntry)) {
                for (EntryTempEntity entryTempEntity1 : childTempEntry) {
                    update += entryTempMapper.updateById(entryTempEntity1);
                }
            }
            update += entryTempMapper.updateById(entryTempEntity);
        }
        if (update < tempEntities.size()) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public List<EntryTempEntity> getEntryTempByTaskID(String taskID) {
        List<EntryTempEntity> newEntryTemp = new ArrayList<>();
        List<EntryTempEntity> entryTempEntities = entryTempMapper.getEntryTempByTaskID(taskID);
        int sum = 0;
        //entryid -> tempEntry
        Map<String, EntryTempEntity> entryTempEntityMap = new HashMap<>();
        for (EntryTempEntity childEntryTemp : entryTempEntities) {
            String parentID = childEntryTemp.getParentID();
            //构建聚合结构
            if (StringUtils.isNotBlank(parentID)) {
                EntryTempEntity parentEntryTemp = entryTempEntityMap.get(parentID);
                //判断map 空 则找到父 放到map里 不是空则把子放到父里
                if (Objects.isNull(parentEntryTemp)) {
                    for (EntryTempEntity parentEntryTemp1 : entryTempEntities) {
                        if (parentID.equals(parentEntryTemp1.getId())) {
                            ArrayList<EntryTempEntity> entityArrayList = new ArrayList<>();
                            entityArrayList.add(childEntryTemp);
                            parentEntryTemp1.setChildren(entityArrayList);
                            entryTempEntityMap.put(parentEntryTemp1.getId(), parentEntryTemp1);
                            sum += 1;
                        }
                    }

                } else {
                    if (CollectionUtils.isEmpty(parentEntryTemp.getChildren())) {
                        ArrayList<EntryTempEntity> childList = new ArrayList<>();
                        childList.add(childEntryTemp);
                        parentEntryTemp.setChildren(childList);
                        sum += 1;
                    } else {
                        parentEntryTemp.getChildren().add(childEntryTemp);
                        sum += 1;
                    }
                }

            } else {
                entryTempEntityMap.put(childEntryTemp.getId(), childEntryTemp);
                sum += 1;
            }
        }
        for (EntryTempEntity entryTempEntity : entryTempEntityMap.values()) {
            newEntryTemp.add(entryTempEntity);
        }
        log.warn(" ==== sum is : " + sum + " ==== ");
        return newEntryTemp;
    }

    @Override
    public String deleteEntryInfoByID(List<String> entryIDs) {

        HashMap<String, Object> map = new HashMap();
        for (String entryID : entryIDs) {
            map.put("entry_id", entryID);
        }

        productRelationMapper.deleteByMap(map);
        int delete = entryInfoMapper.deleteBatchIds(entryIDs);
        return ConstantInterface.OK_STR;
    }

    @Override
    public int getEntryTempByTaskIDTotal(String taskID) {

        return entryTempMapper.getEntryTempByTaskIDTotal(taskID);
    }

    @Override
    public List<EntryInfoEntity> preTranslate(List<EntryInfoEntity> entryInfoList, String taskID, String priority) {

        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        for (EntryInfoEntity entryInfoEntity : entryInfoList) {
            //子不翻译
            if (StringUtils.isNotBlank(entryInfoEntity.getParentID())) {
                continue;
            }
            String entry = entryInfoEntity.getEntry();
            String translateType = taskInfoMapper.selectById(taskID).getTranslateType();
            String translate = "";

            switch (translateType) {
                case ConstantInterface.ENGLISH:
                    if (StringUtils.isBlank(entryInfoEntity.getEnglish())) {
                        translate = addSuggessTransByPriority(entryInfoEntity, translateType, priority);
                        if (StringUtils.isNotBlank(translate)) {
                            entryInfoEntity.setEnglish(translate);
                        }
                    }
                    entryInfoEntities.add(entryInfoEntity);
                    break;
                case ConstantInterface.SPANISH:
                    if (StringUtils.isBlank(entryInfoEntity.getSpanish())) {
                        translate = addSuggessTransByPriority(entryInfoEntity, translateType, priority);
                        if (StringUtils.isNotBlank(translate)) {
                            entryInfoEntity.setSpanish(translate);
                        }
                    }
                    entryInfoEntities.add(entryInfoEntity);
                    break;
                case ConstantInterface.FRENCH:
                    if (StringUtils.isBlank(entryInfoEntity.getFrench())) {
                        translate = addSuggessTransByPriority(entryInfoEntity, translateType, priority);
                        if (StringUtils.isNotBlank(translate)) {
                            entryInfoEntity.setFrench(translate);
                        }
                    }
                    entryInfoEntities.add(entryInfoEntity);
                    break;
                case ConstantInterface.RUSSIAN:
                    if (StringUtils.isBlank(entryInfoEntity.getRussian())) {
                        translate = addSuggessTransByPriority(entryInfoEntity, translateType, priority);
                        if (StringUtils.isNotBlank(translate)) {
                            entryInfoEntity.setRussian(translate);
                        }
                    }
                    entryInfoEntities.add(entryInfoEntity);
                    break;
            }


        }
        return entryInfoEntities;
    }

    //优先级  术语库 外网
    private String addSuggessTrans(EntryInfoEntity entryInfoEntity, String translateType, String priority) {
        //priority :
        String translateRes = "";
        TLanguage language = languageMapper.selectLaguageByName(translateType).get(0);
        switch (priority) {
            case ConstantInterface.SYK:
                translateRes = getSYKTranslate(entryInfoEntity.getEntry(), translateType);
            case ConstantInterface.BD:
                if (StringUtils.isBlank(translateRes)) {
                    LanguageEntity translateResult = translateUtils.getTranslateResult(entryInfoEntity.getEntry(), ConstantInterface.AUTO, language);
                    if (!Objects.isNull(translateResult)){
                        translateRes = translateResult.getValue();
                    }else {
                            LanguageEntity languageEntity = youdaoTrans.youdaoTranslate(entryInfoEntity.getEntry(), ConstantInterface.AUTO, language);
                            if (!Objects.isNull(languageEntity)) {
                                translateRes = languageEntity.getValue();
                            }else {
                                translateRes = getSYKTranslate(entryInfoEntity.getEntry(), translateType);
                            }

                        }
                    }


                break;
            case ConstantInterface.YD:
                if (StringUtils.isBlank(translateRes)) {
                    LanguageEntity languageEntity = youdaoTrans.youdaoTranslate(entryInfoEntity.getEntry(), ConstantInterface.AUTO, language);
                    if (!Objects.isNull(languageEntity)){
                        translateRes = languageEntity.getValue();
                    }else {
                        LanguageEntity translateResult = translateUtils.getTranslateResult(entryInfoEntity.getEntry(), ConstantInterface.AUTO, language);
                        if (!Objects.isNull(translateResult)){
                            translateRes = translateResult.getValue();
                        }else {
                            translateRes = getSYKTranslate(entryInfoEntity.getEntry(), translateType);
                        }

                    }

                }
                break;
            case ConstantInterface.GG:
                //TODO
                break;
            case ConstantInterface.MD:
                //TODO
                break;
        }
        return translateRes;
    }

    //优先级  术语库 外网
    private String addSuggessTransByPriority(EntryInfoEntity entryInfoEntity, String translateType, String priority) {
        Queue<String> queue = new LinkedList<>();
        queue.add(priority);
        for (String key : ConstantInterface.translateMachine().keySet()) {
            if (!priority.equals(key)){
                queue.add(key);
            }
        }

        TLanguage language = languageMapper.selectLaguageByName(translateType).get(0);
        String translateRes = "";
        while (!queue.isEmpty()){
            if (translateRes != null && !"".equals(translateRes)){
                break;
            }
            String type = queue.remove();
            if (type.equals(ConstantInterface.DEEPL)){
                // deepl翻译
                translateRes = deepLTranslateUtils.translate(entryInfoEntity.getEntry(), null, language.getDeeplCode());
            }else if (type.equals(ConstantInterface.BD)){
                // 百度翻译
                LanguageEntity translateResult = translateUtils.getTranslateResult(entryInfoEntity.getEntry(), ConstantInterface.AUTO, language);
                if (!Objects.isNull(translateResult)){
                    translateRes = translateResult.getValue();
                }
            }else if (type.equals(ConstantInterface.SYK)){
                // 术语库翻译
                translateRes = getSYKTranslate(entryInfoEntity.getEntry(), translateType);
            }else if (type.equals(ConstantInterface.YD)){
                // 有道翻译
                LanguageEntity languageEntity = youdaoTrans.youdaoTranslate(entryInfoEntity.getEntry(), ConstantInterface.AUTO, language);
                if (!Objects.isNull(languageEntity)) {
                    translateRes = languageEntity.getValue();
                }
            }else if (type.equals(ConstantInterface.GG)){
                // TODO google翻译
            }else if (type.equals(ConstantInterface.MD)){
                // TODO 本地模型翻译
            }
        }
        return translateRes;
    }

    private String getSYKTranslate(String entry, String translateType) {
        List<TranslateEntity> versionSuggestTrans = translateMapper.getVersionSuggestTrans(entry, translateType);
        String translate = "";
        if (!CollectionUtils.isEmpty(versionSuggestTrans)) {
            translate = versionSuggestTrans.get(0).getTranslate();
        }
        return translate;
    }

    @Override
    public void getTemplateFile(HttpServletResponse response,String fileType) {
        try {
            String fileUrl = "";
            Workbook workbook ;
            String fileName = "";
            if (fileType.equals("common")){
                fileUrl = configFileCommonUrl;
                fileName = "通用词条翻译模板_common.xlsx";
            }else if (fileType.equals("zz")){
                fileUrl = configFileZZUrl;
                fileName = "装置词条翻译模板_zz.xlsx";
            }
            FileInputStream fileInputStream = new FileInputStream(fileUrl);
            if (fileUrl.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fileInputStream);
            } else {
                workbook = new HSSFWorkbook(fileInputStream);
            }

            fileName = URLEncoder.encode(fileName, "UTF-8");
            log.warn(" **** fileName : " + fileName + " ***** ");
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.addHeader("Pargam", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            response.setStatus(200);


            ServletOutputStream outputStream = response.getOutputStream();

            workbook.write(outputStream);
            workbook.close();
            outputStream.close();

        } catch (Exception e) {
            log.error("代码生成出错", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.sendError(500, "代码生成出错，无法下载");
            } catch (IOException ex) {
                log.error("响应报错信息出错", e);
            }
        }

    }


    @Override
    //state 第一位 词条状态 第二位 翻译状态
    public List<EntryInfoEntity> getEntryInfoList(String taskID, String entryState, List<String> transStates ,String entry) {
        List<EntryInfoEntity> newEntry = new ArrayList<>();
       // List<EntryInfoEntity> entryInfoEntities;

        //没给翻译状态直接查词条状态
        List<EntryInfoEntity> entryInfoEntities  = getEntryInfo(taskID,entryState,transStates,entry);


        int sum = 0;
        //entryid -> tempEntry
        Map<String, EntryInfoEntity> entryInfoEntityMap = new HashMap<>();
        for (EntryInfoEntity childEntryInfo : entryInfoEntities) {
            String parentID = childEntryInfo.getParentID();
            //构建聚合结构
            if (StringUtils.isNotBlank(parentID)) {
                EntryInfoEntity parentEntryInfo = entryInfoEntityMap.get(parentID);
                //判断map 空 则找到父 放到map里 不是空则把子放到父里
                if (Objects.isNull(parentEntryInfo)) {
                    for (EntryInfoEntity parentEntryInfo1 : entryInfoEntities) {
                        if (parentID.equals(parentEntryInfo1.getId())) {
                            ArrayList<EntryInfoEntity> entityArrayList = new ArrayList<>();
                            entityArrayList.add(childEntryInfo);
                            parentEntryInfo1.setChildren(entityArrayList);
                            entryInfoEntityMap.put(parentEntryInfo1.getId(), parentEntryInfo1);
                            sum += 1;
                        }
                    }

                } else {
                    if (CollectionUtils.isEmpty(parentEntryInfo.getChildren())) {
                        ArrayList<EntryInfoEntity> childList = new ArrayList<>();
                        childList.add(childEntryInfo);
                        parentEntryInfo.setChildren(childList);
                        sum += 1;
                    } else {
                        parentEntryInfo.getChildren().add(childEntryInfo);
                        sum += 1;
                    }
                }

            } else {
                entryInfoEntityMap.put(childEntryInfo.getId(), childEntryInfo);
                sum += 1;
            }
        }
        for (EntryInfoEntity entryInfoEntity : entryInfoEntityMap.values()) {
            newEntry.add(entryInfoEntity);
        }
        log.warn(" ==== sum is : " + sum + " ==== ");
        return newEntry;
    }
    //没给翻译状态直接查词条状态
    private List<EntryInfoEntity> getEntryInfo(String taskID, String entryState, List<String> transStates,String entry) {
        List<EntryInfoEntity> entryInfoEntities ;
        TLanguage tLanguage = languageMapper.getLanguageByTask(taskID);

        if (CollectionUtils.isEmpty(transStates)) {
            //  entryInfoEntities = entryInfoMapper.selectList(queryWrapper);
            entryInfoEntities = entryInfoMapper.getEntryByTaskIDAndEntry(taskID, entryState,entry);
        } else {
            String transState = "";
            String s = "";
            String entrySql = "";
            String entryStateSql = "";
            // 翻译状态处理 结果 ： 1,2,3
            for (String transState1 : transStates) {
                if (StringUtils.isBlank(transState)) {
                    transState =  " '" + transState1 + "' ";
                }else {
                    transState += ",'" + transState1 + "' ";
                }
                if (Integer.parseInt(transState1) == 0) {
                    s = " or t2." + tLanguage.getYdCode() + "_trans_id  is null or t2.en_trans_id = ''";
                }
            }
            if (StringUtils.isNotBlank(entry)){
                 entrySql =" and t2.entry = '"+entry + "'";
            }
            if (StringUtils.isNotBlank(entryState)){
                 entryStateSql =" and t2.entry_state = "  +entryState;
            }


            String sql = "select t2.* ,t3.audit_suggest as "   + tLanguage.getEnglish() +
                    "AuditSuggest  , t3.translate_state as " + tLanguage.getEnglish() +
                    "TranslateState ,t3.translate as "+ tLanguage.getEnglish() +
                    " from  t_product_relation t1  join  t_entry_info t2 on t1.entry_id = t2.id left JOIN t_translate t3 ON  t2." +
                    tLanguage.getYdCode() + "_trans_id = t3.id   where ( t3.translate_state in ( " + transState + ")  "  +s + ") and  t1.task_id = '" + taskID + "' " +  entryStateSql  + entrySql;



            entryInfoEntities = entryInfoMapper.getTransStateEntry(sql);
        }
        return  entryInfoEntities;
    }

    @Override
    public String updateEntryList(List<EntryInfoEntity> entryInfoEntities, String taskID, HttpServletRequest request) {
        int update = 0;
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        String translateType = taskInfoMapper.selectById(taskID).getTranslateType();
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            List<EntryInfoEntity> childrenInfoEntry = entryInfoEntity.getChildren();
            updateEntryInfoTranslate(translateType, entryInfoEntity, department);
            if (!CollectionUtils.isEmpty(childrenInfoEntry)) {
                for (EntryInfoEntity entryInfoEntity1 : childrenInfoEntry) {
                    entryInfoEntity1.setEntryState(entryInfoEntity.getEntryState());
                    if (StringUtils.isNotBlank(entryInfoEntity.getEnTransId())){
                        entryInfoEntity1.setEnTransId(entryInfoEntity.getEnTransId());
                    }else if (StringUtils.isNotBlank(entryInfoEntity.getRuTransId())){
                        entryInfoEntity1.setRuTransId(entryInfoEntity.getRuTransId());
                    }else if (StringUtils.isNotBlank(entryInfoEntity.getFraTransId())){
                        entryInfoEntity1.setFraTransId(entryInfoEntity.getFraTransId());
                    }else if (StringUtils.isNotBlank(entryInfoEntity.getSpaTransId())){
                        entryInfoEntity1.setSpaTransId(entryInfoEntity.getSpaTransId());
                    }
                    update += entryInfoMapper.updateById(entryInfoEntity1);
                }
            }


            update += entryInfoMapper.updateById(entryInfoEntity);
        }
        if (update < entryInfoEntities.size()) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }


    //写入翻译
    private void updateEntryInfoTranslate(String translateType, EntryInfoEntity entryInfoEntity, String department) {
        String transId = "";
        //如果有翻译 就更新翻译
        switch (translateType) {
            case ConstantInterface.ENGLISH:
                transId = updateTrans(entryInfoEntity.getEnglishAuditSuggest(),entryInfoEntity.getEnglish(), entryInfoEntity.getEnTransId(), ConstantInterface.ENGLISH, entryInfoEntity.getEnglishTranslateState(), entryInfoEntity, department);
                entryInfoEntity.setEnTransId(transId);
                writeI18Entry(entryInfoEntity, entryInfoEntity.getEnglish());
                break;
            case ConstantInterface.RUSSIAN:
                transId = updateTrans(entryInfoEntity.getRussianAuditSuggest(),entryInfoEntity.getRussian(), entryInfoEntity.getRuTransId(), ConstantInterface.RUSSIAN, entryInfoEntity.getRussianTranslateState(), entryInfoEntity, department);
                entryInfoEntity.setRuTransId(transId);
                break;
            case ConstantInterface.FRENCH:
                transId = updateTrans(entryInfoEntity.getFrenchAuditSuggest(),entryInfoEntity.getFrench(), entryInfoEntity.getFraTransId(), ConstantInterface.FRENCH, entryInfoEntity.getFrenchTranslateState(), entryInfoEntity, department);
                entryInfoEntity.setFraTransId(transId);
                break;
            case ConstantInterface.SPANISH:
                transId = updateTrans(entryInfoEntity.getSpanishAuditSuggest(),entryInfoEntity.getSpanish(), entryInfoEntity.getSpaTransId(), ConstantInterface.SPANISH, entryInfoEntity.getSpanishTranslateState(), entryInfoEntity, department);
                entryInfoEntity.setSpaTransId(transId);
                break;
        }
    }

    private void writeI18Entry(EntryInfoEntity entryInfoEntity, String translate) {
        if (StringUtils.isBlank(entryInfoEntity.getImportType())) {
            return;
        }
        switch (entryInfoEntity.getImportType()) {
            case ConstantInterface.DB:
                break;
            case ConstantInterface.DICTIONARY:
                break;
            default:
                return;
        }
    }

    private String updateTrans(String auditSuggest, String trans, String transId, String type, String transState, EntryInfoEntity entryInfoEntity, String department) {
        String newTransID = "";
        /*if (StringUtils.isBlank(transState)) {
            transState = "1";
        }
        //如果是翻译审核 校验重复翻译 ID挂载重复的

        if (3 == Integer.parseInt(transState) && StringUtils.isNotBlank(transId)) {
            //无翻译直接删掉
            if (StringUtils.isBlank(trans)) {
                translateMapper.deleteById(transId);
                return newTransID;
            }
            QueryWrapper<TranslateEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("entry", entryInfoEntity.getEntry());
            queryWrapper.eq("type", type);
            queryWrapper.eq("translate", trans);
            queryWrapper.eq("delete_state", 0);
            queryWrapper.eq("translate_state", 3);
            List<TranslateEntity> translateEntityList = translateMapper.selectList(queryWrapper);
            if (translateEntityList.size() > 1) {
                log.error(" ===== 词条更新翻译查重多于1条，transid : " + transId + "  ,entry : " + entryInfoEntity.getEntry() + " , trans : " + trans + " ===== ");
                newTransID = translateEntityList.get(0).getId();
                int delete = translateMapper.deleteById(transId);
            } else if (translateEntityList.size() == 1) {
                newTransID = translateEntityList.get(0).getId();

                int delete = translateMapper.deleteById(transId);
            }else if (translateEntityList.size() <1){
                newTransID = transId;
            }
            entryInfoEntity.setVersionID("");
            entryInfoEntity.setTaskId("");
        }*/
        //翻译id 不是空 trans 是空 删除
        //翻译id 不是空  trans 不是空  update
        //翻译id 是空 trans 不是空 insert
        // 翻译id trans 都是空 跳过
        if (StringUtils.isNotBlank(transId) && StringUtils.isNotBlank(transState)){
            if (StringUtils.isBlank(trans)){
                int delete = translateMapper.deleteById(transId);
                log.info("删除 （"+ delete + " ）条 翻译 到翻译表中, transID ( " + transId + ") 更新内容 ： trans ( " + trans + ") ");
                return newTransID;
            }else {
                //如果是翻译审核 校验重复翻译 ID挂载重复的
                if (3 == Integer.parseInt(transState)){
                     newTransID = updateAuditTrans(entryInfoEntity,transId,type,newTransID,trans,auditSuggest,department);
                } else{
                    newTransID = updateTransEntity(transState,transId,auditSuggest,trans);
                }

            }
        }else {
            if (StringUtils.isBlank(trans)){
                return newTransID;
            }else {
                String entry= entryInfoEntity.getEntry();
                newTransID = insertTransEntity(trans,transState,department,type,entry);

            }
        }
        return  newTransID;
        //翻译id 是空 trans 不是空 insert
        // 翻译id trans 都是空 跳过


/*
        if (StringUtils.isNotBlank(trans)) {
            TranslateEntity translateEntity = new TranslateEntity();
            translateEntity.setTranslate(trans);
            translateEntity.setTranslateState(transState);
            if (StringUtils.isBlank(transId)) {
                newTransID = commonUtils.getUUID();
                translateEntity.setId(transId);
                translateEntity.setPublicState(0);
                translateEntity.setVisualRange(department);
                translateEntity.setType(type);
                translateEntity.setDeleteState(0);
                translateEntity.setType(type);
                int insert = translateMapper.insert(translateEntity);
                return newTransID;
            }
            translateEntity.setId(newTransID);
            int update = translateMapper.updateById(translateEntity);
        }
        return newTransID;*/
    }



    private String insertTransEntity(String trans, String transState, String department, String type, String entry) {
        String newTransID = commonUtils.getUUID();
        TranslateEntity translateEntity = new TranslateEntity();
        translateEntity.setTranslate(trans);
        translateEntity.setTranslateState(transState);
        translateEntity.setId(newTransID);
        translateEntity.setPublicState(0);
        translateEntity.setVisualRange(department);
        translateEntity.setDeleteState(0);
        translateEntity.setType(type);
        translateEntity.setEntry(entry);
        int insert = translateMapper.insert(translateEntity);
        log.info("新增 （"+ insert + " ）条 翻译 到翻译表中, transID ( " + newTransID + ") 更新内容 ： trans ( " + trans + "),  transState ( " + transState + ")  ");
        return newTransID;
    }

    private String updateTransEntity(String transState, String transId, String auditSuggest, String trans) {
        TranslateEntity translateEntity = new TranslateEntity();
        translateEntity.setTranslate(trans);
        translateEntity.setTranslateState(transState);
        translateEntity.setId(transId);
        translateEntity.setAuditSuggest(auditSuggest);
        int update = translateMapper.updateById(translateEntity);
        log.info("更新 （"+ update + " ）条 翻译 到翻译表中, transID ( " + transId + ") 更新内容 ： trans ( " + trans + "),  transState ( " + transState + ")  ");
        return transId;
    }

    private String updateAuditTrans(EntryInfoEntity entryInfoEntity, String transId, String type, String newTransID, String trans,String auditSuggest,String department) {

        QueryWrapper<TranslateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("entry", entryInfoEntity.getEntry());
        queryWrapper.eq("type", type);
        queryWrapper.eq("translate", trans);
        queryWrapper.eq("delete_state", 0);
        queryWrapper.eq("translate_state", 3);
       // queryWrapper.eq("audit_suggest", auditSuggest);
        List<TranslateEntity> translateEntityList = translateMapper.selectList(queryWrapper);
        if (translateEntityList.size() > 1) {
            log.error(" ===== 词条更新翻译查重多于1条，transid : " + transId + "  ,entry : " + entryInfoEntity.getEntry() + " , trans : " + trans + " ===== ");
            newTransID = translateEntityList.get(0).getId();
            int delete = translateMapper.deleteById(transId);
            log.info("删除 （"+ delete + " ）条 翻译 到翻译表中, transID ( " + transId + ") 更新内容 ： trans ( " + trans + ") ");
        } else if (translateEntityList.size() == 1) {
            newTransID = translateEntityList.get(0).getId();
            int delete = translateMapper.deleteById(transId);
            log.info("删除 （"+ delete + " ）条 翻译 到翻译表中, transID ( " + transId + ") 更新内容 ： trans ( " + trans + ") ");
        }else if (translateEntityList.size() <1){

            TranslateEntity translateEntity = new TranslateEntity();
            translateEntity.setId(transId);
            translateEntity.setTranslate(trans);
            translateEntity.setTranslateState("3");
            translateEntity.setPublicState(0);
            translateEntity.setVisualRange(department);
            translateEntity.setDeleteState(0);
            translateEntity.setType(type);
            translateEntity.setEntry(entryInfoEntity.getEntry());
            translateEntity.setAuditSuggest(auditSuggest);
            int update = translateMapper.updateById(translateEntity);
            log.info("更新 （"+ update + " ）条 翻译 到翻译表中, transID ( " + transId + ") 更新内容 ： trans ( " + trans + ") ");
            newTransID = transId;
        }
        entryInfoEntity.setVersionID("");
        entryInfoEntity.setTaskId("");
        return newTransID;
    }

  /*  @Override
    public ImportResultEntryVO checkExistEntry(List<EntryTempEntity> entryTempEntities) {
        ImportResultEntryVO importResultEntryVO = new ImportResultEntryVO();
        List<EntryTempEntity> existTempEntryList = new ArrayList<>();
        List<EntryTempEntity> importTempEntryList = new ArrayList<>();
        for (EntryTempEntity entryTempEntity : entryTempEntities){
            if (StringUtils.isNotBlank(entryTempEntity.getAbbr())){
                ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(entryTempEntity.getProductID());
                List<EntryInfoEntity> entryInfoEntities  = productTableMapper.getEntryInfoByAbbr(productTableEntity.getProductTableName(),entryTempEntity);

                //存在重复的abbr
                if (CollectionUtils.isEmpty(entryInfoEntities)){
                    existTempEntryList.add(entryTempEntity);
                }else {
                    importTempEntryList.add(entryTempEntity);
                }
            }
        }
        importResultEntryVO.setImportEntryList(importTempEntryList);
        importResultEntryVO.setExistEntryList(existTempEntryList);
        return importResultEntryVO;
    }*/


}




