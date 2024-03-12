package com.shr.translationtoolservice.controller;

import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.google.gson.JsonArray;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.dao.*;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.*;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.util.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.ParameterResolutionDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sun.reflect.FieldInfo;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @ClassName I18SeverController
 * @Description i18Server
 * @USER: Cola
 * @Date 2023/12/13 0013 18:51
 **/


@RestController
@RequestMapping("/I18Sever")
@Api(tags = "i18Sever")
@Slf4j
@PropertySource("classpath:application.yml")
public class I18SeverController extends BaseController {


    @Value("${I18server.url}")
    private String I18URL;

    @Autowired
    private HTTPUtils httpUtils;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private TLanguageMapper languageMapper;

    @Autowired
    private TaskInfoMapper taskInfoMapper;

    @Autowired
    private EntryTempMapper entryTempMapper;

    @Autowired
    private TranslateMapper translateMapper;

    @Autowired
    private VersionTableMapper versionTableMapper;

    @Autowired
    private VersionMapper versionMapper;

    @Autowired
    private EntryInfoService entryInfoService;

    @Autowired
    private EntryInfoMapper entryInfoMapper;
    @Autowired
    private ProductTableMapper productTableMapper;

    @Autowired
    private EntryProcessUtils entryProcessUtils;


    @GetMapping("/language")
    @ApiOperation("获取语言缩写")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> language() {
        ResponseListModel<String> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            s = httpUtils.get(I18URL + ConstantInterface.LANGUAGE);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                list.add(jsonArray.get(i).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }


    @GetMapping("/getFileListByLang")
    @ApiOperation("获取文件列表")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getFileListByLang(@RequestParam String language) {
        //获取语言code
        List<TLanguage> languageList = languageMapper.selectLaguageByName(language);
        ResponseListModel<String> responseListModel = new ResponseListModel<>();
        Map<String, String> headerParameters = new HashMap<>();
        headerParameters.put("laguage", languageList.get(0).getCode());
        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            s = httpUtils.get(I18URL + ConstantInterface.GET_FILE_LIST, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                list.add(jsonArray.get(i).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }


    @PostMapping("/getWords")
    @ApiOperation("获取TS文件词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getWords(@RequestBody List<String> fileNames,
                                                    @RequestParam String taskID,
                                                    @RequestParam String translateType, HttpServletRequest request) {

        //List<TLanguage> languageList = languageMapper.selectLaguageByName(translateType);
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);
        List<String> versionIDs = taskInfoMapper.getVersionIDByTaskID(taskID);
        String versionID = "";
        if (!CollectionUtils.isEmpty(versionIDs)) {
            versionID = versionIDs.get(0);
        }
        String productId = versionMapper.getVersionByID(versionID).getProductId();
        // String language = languageList.get(0).getName();
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();

        ArrayList<EntryInfoEntity> list = new ArrayList<>();
        for (String fileName : fileNames) {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("fileName", fileName);

            JSONArray jsonArray = new JSONArray();
            String s = "";

            try {
                s = httpUtils.get(I18URL + ConstantInterface.GET_WORDS, headerParameters);

                jsonArray = JSONArray.parseArray(s);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONArray jsonArray1 = JSONArray.parseArray(jsonArray.get(i).toString());
                    String entry = jsonArray1.get(0).toString();
                    String translate = jsonArray1.getString(1);
                    String tag = jsonArray1.getString(2);
                    EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                    entryInfoEntity.setId(commonUtils.getUUID());
                    entryInfoEntity.setEntrySource(fileName);
                    entryInfoEntity.setEntryState(1);
                    entryInfoEntity.setTaskId(taskID);
                    entryInfoEntity.setEntry(entry);
                    entryInfoEntity.setUpdate(userName);
                    entryInfoEntity.setUpdateTime(date);
                    entryInfoEntity.setProductID(productId);
                    entryInfoEntity.setTag(tag);
                    entryInfoEntity.setVersionID(versionID);
                    entryInfoEntity.setImportType(ConstantInterface.TS);
                    entryInfoEntity.setIsDelete(0);
                    createNewTrans(entryInfoEntity, translateType, translate);
                    list.add(entryInfoEntity);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        List<EntryInfoEntity> entryInfoEntities = buildRepeTempEntry(list, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryInfoEntities, taskID);
        responseListModel.setList(entryInfoEntities);
        responseListModel.setTotalNum(entryInfoEntities.size());
        return checkResult(responseListModel);
    }

    private void createNewTrans(EntryInfoEntity entryInfoEntity, String translateType, String translate) {
        //写入翻译字段
        switch (translateType) {
            case ConstantInterface.ENGLISH:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setEnglish(translate);
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setSpanish(translate);
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setSpanishTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setRussian(translate);
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setRussianTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isNotBlank(translate)) {
                    entryInfoEntity.setFrench(translate);
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.TRANSLATED);
                } else {
                    entryInfoEntity.setFrenchTranslateState(ConstantInterface.UNTRANSLATED);
                }
                break;
        }
    }

    @PostMapping("/saveWords")
    @ApiOperation("回写ts文件")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> saveWords(@RequestBody List<TSWordVO> tsWordVOS) {
        String s = "";
        try {
            //遍历文件
            for (TSWordVO tsWordVO : tsWordVOS) {
                String ffileName = tsWordVO.getFileName();
                ArrayList<Map<String, String>> requestList = new ArrayList<>();
                JSONObject jsonObject = new JSONObject();
                //遍历单词
                for (EntryInfoEntity entryInfoEntity : tsWordVO.getEntryInfoEntities()) {
                    Map<String, String> requestMap = new HashMap<>();
                    requestMap.put("source", entryInfoEntity.getEntry());
                    switch (tsWordVO.getTranslateType()) {
                        case ConstantInterface.ENGLISH:
                            requestMap.put("translate", entryInfoEntity.getEnglish());
                            break;
                        case ConstantInterface.RUSSIAN:
                            requestMap.put("translate", entryInfoEntity.getRussian());
                            break;
                        case ConstantInterface.SPANISH:
                            requestMap.put("translate", entryInfoEntity.getSpanish());
                            break;
                        case ConstantInterface.FRENCH:
                            requestMap.put("translate", entryInfoEntity.getFrench());
                            break;
                    }

                    requestList.add(requestMap);
                }
                jsonObject.put("entry", requestList);
                jsonObject.put("test", "test6");
                s = httpUtils.post(I18URL + ConstantInterface.SAVE_WORDS + "?fileName=" + ffileName, jsonObject);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return checkResult(s);
    }

    @GetMapping("/getDictionary")
    @ApiOperation("获取字典列表")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getDictionary() {

        ResponseListModel<String> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            s = httpUtils.get(I18URL + ConstantInterface.DICTIONARY);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {

                list.add(jsonArray.get(i).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }


    @PostMapping("/setDictionaryInfo")
    @ApiOperation("回写字典详情")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> setDictionaryInfo(@RequestBody List<String> types) {

        ResponseListModel<String> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            for (String type : types) {
                s = httpUtils.post(I18URL + ConstantInterface.DICTIONARY + "/" + type, new JSONObject());
            }

            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                list.add(jsonArray.get(i).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }

    @GetMapping("/createDic")
    @ApiOperation("创建辞典")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> createDic(@RequestParam String dicName) {

        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("dictionary", dicName);
            s = httpUtils.get(I18URL + ConstantInterface.Create_DI  , headerParameters);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return checkResult(ConstantInterface.OK_STR);
    }



    @Autowired
    private ExcelUtils excelUtil;

  /*  @PostMapping("/setInfo")
    @ApiOperation("回写")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> setInfo(@RequestBody List<EntryInfoEntity> entryInfoEntities, String translateType) {


        //List<EntryInfoEntity> tempEntities = entryTempMapper.getEntryTempByTaskID(taskID);
        ResponseListModel<String> responseListModel = new ResponseListModel<>();
        List<EntryInfoEntity> dbEntrInfo = new ArrayList<>();
        List<EntryInfoEntity> diEntryInfo = new ArrayList<>();
        List<EntryInfoEntity> tsEntryInfo = new ArrayList<>();
        String fileName = "";
        ArrayList<Map<String, String>> tsEntryInfoMap = new ArrayList<>();
        JSONObject jsonObject = new JSONObject();
        //分组

        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            if ("TS".equals(entryInfoEntity.getImportType())) {
                fileName = entryInfoEntity.getEntrySource();
                //遍历单词
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("source", entryInfoEntity.getEntry());
                requestMap.put("tag", entryInfoEntity.getTag());
                String trans = getTransByType(entryInfoEntity, translateType);
                if (StringUtils.isBlank(trans)) {
                    continue;
                }

                requestMap.put("translate", trans);
                tsEntryInfoMap.add(requestMap);
            } else if ("DI".equals(entryInfoEntity.getImportType())) {
                diEntryInfo.add(entryInfoEntity);
                return checkResult(ConstantInterface.OK_STR);
            } else if ("DB".equals(diEntryInfo)) {
                dbEntrInfo.add(entryInfoEntity);
                return checkResult(ConstantInterface.OK_STR);
            }
        }
        //写入i18
        if (!CollectionUtils.isEmpty(tsEntryInfoMap)) {
            jsonObject.put("entry", tsEntryInfoMap);
            String s = httpUtils.post(I18URL + ConstantInterface.SAVE_WORDS + "?fileName=" + fileName, jsonObject);
        }
        if (!CollectionUtils.isEmpty(diEntryInfo)) {
            //TODO 等待字典写入接口结束
            log.warn("  DI导出暂未开放 ！");
        }
        if (!CollectionUtils.isEmpty(dbEntrInfo)) {
            //TODO 等待字典写入接口结束
            log.warn("  DB导出暂未开放 ！");
        }

        return checkResult(ConstantInterface.OK_STR);
    }
*/

    @PostMapping("/setInfoByTask")
    @ApiOperation("回写")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> setInfo(@RequestParam String taskID) {


        List<EntryInfoEntity> entryInfoEntities = entryInfoMapper.getEntryByTaskID(taskID, "");
        TaskInfoEntity taskInfoEntity = taskInfoMapper.selectById(taskID);
        String translateType = taskInfoEntity.getTranslateType();
        ResponseListModel<String> responseListModel = new ResponseListModel<>();
        List<EntryInfoEntity> dbEntrInfo = new ArrayList<>();
        List<EntryInfoEntity> diEntryInfo = new ArrayList<>();
        List<EntryInfoEntity> tsEntryInfo = new ArrayList<>();
        List<EntryInfoEntity> configEntryInfo = new ArrayList<>();
        String fileName = "";
        ArrayList<Map<String, String>> tsEntryInfoMap = new ArrayList<>();
        JSONObject jsonObject = new JSONObject();
        //di分组   fileName -> list
        Map<String, List<EntryInfoEntity>> diTypeMap = new HashMap<>();
        Map<String, List<EntryInfoEntity>> dbTypeMap = new HashMap<>();
        Map<String, List<EntryInfoEntity>> configTypeMap = new HashMap<>();
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            if ("TS".equals(entryInfoEntity.getImportType())) {
                fileName = entryInfoEntity.getEntrySource();
                //遍历单词
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("source", entryInfoEntity.getEntry());
                requestMap.put("tag", entryInfoEntity.getTag());
                String trans = getTransByType(entryInfoEntity, translateType);
                if (StringUtils.isBlank(trans)) {
                    continue;
                }

                requestMap.put("translate", trans);
                tsEntryInfoMap.add(requestMap);
            } else if ("DI".equals(entryInfoEntity.getImportType())) {
                //di 来源处理
                List<EntryInfoEntity> entities;
                if (CollectionUtils.isEmpty(diTypeMap.get(entryInfoEntity.getEntrySource()))) {
                    entities = new ArrayList<>();
                    entities.add(entryInfoEntity);
                    diTypeMap.put(entryInfoEntity.getEntrySource(), entities);
                } else {
                    entities = diTypeMap.get(entryInfoEntity.getEntrySource());
                    entities.add(entryInfoEntity);
                }

                diEntryInfo.add(entryInfoEntity);

            } else if ("DB".equals(entryInfoEntity.getImportType())) {
                //dB 来源处理
                //最后一位是写入DI 的文件名
               // String[] s = entryInfoEntity.getDiFileName().split("_");
                String diFileName = entryInfoEntity.getDiFileName();
                List<EntryInfoEntity> entities;

                if (CollectionUtils.isEmpty( dbTypeMap.get(diFileName))) {
                    entities = new ArrayList<>();
                    entities.add(entryInfoEntity);
                    dbTypeMap.put(diFileName, entities);
                } else {
                    entities = dbTypeMap.get(diFileName);
                    entities.add(entryInfoEntity);
                }

                dbEntrInfo.add(entryInfoEntity);

            } else if ("CONFIG".equals(entryInfoEntity.getImportType())) {
                //di 来源处理
                String diFileName = entryInfoEntity.getDiFileName();
                List<EntryInfoEntity> entities;

                if (CollectionUtils.isEmpty( dbTypeMap.get(diFileName))) {
                    entities = new ArrayList<>();
                    entities.add(entryInfoEntity);
                    configTypeMap.put(diFileName, entities);
                } else {
                    entities = configTypeMap.get(diFileName);
                    entities.add(entryInfoEntity);
                }
                configEntryInfo.add(entryInfoEntity);

            }
        }
        //写入i18
        if (!CollectionUtils.isEmpty(tsEntryInfoMap)) {
            jsonObject.put("entry", tsEntryInfoMap);
            String s = httpUtils.post(I18URL + ConstantInterface.SAVE_WORDS + "?fileName=" + fileName, jsonObject);
        }
        if (!CollectionUtils.isEmpty(diEntryInfo)) {
            //按照分裂写入di
            for (String di_fileName : diTypeMap.keySet()) {
                writeDiWords(di_fileName, translateType, dbEntrInfo);
            }

        }
        if (!CollectionUtils.isEmpty(dbEntrInfo)) {
            //按照分裂写入di
            for (String dbFileName : dbTypeMap.keySet()) {
                //没有的词条新增 已有的更新翻译
                writeDbWords(dbFileName, translateType, dbEntrInfo);
            }

        }
        if (!CollectionUtils.isEmpty(configEntryInfo)) {

            //按照分裂写入di
            for (String dbFileName : configTypeMap.keySet()) {
                //没有的词条新增 已有的更新翻译
                writeDbWords(dbFileName, translateType, configEntryInfo);
            }
        }
        return checkResult(ConstantInterface.OK_STR);
    }

    private void writeDbWords(String dbFileName, String translateType, List<EntryInfoEntity> dbEntrInfo) {
        //获取字典所有词条，将对应词条的对应翻译插入其中 ，再全量写入
        String s = "";
        String s2 = "";
        JSONArray jsonArray;
        try {
            s = httpUtils.get(I18URL + ConstantInterface.DICTIONARY + ConstantInterface.SPRIT + dbFileName);
            jsonArray = JSONArray.parseArray(s);
            List<DictionaryVo> dictionaryVos = new ArrayList<>();
            String langCode = languageMapper.selectLaguageByName(translateType).get(0).getCode();
            for (EntryInfoEntity entryInfoEntity : dbEntrInfo) {
                boolean isExist = updateTransToDiVo(langCode, jsonArray, entryInfoEntity, translateType, dictionaryVos);
                //如果不存在 则新增词条
                if (!isExist) {
                    addEntryToDIVo(langCode, entryInfoEntity, translateType, dictionaryVos);
                }
            }
            String dictionaryVosStr = JSONObject.toJSONString(dictionaryVos);

            //10.16.193.63:18099/dictionary/user
            s2 = httpUtils.post(I18URL + ConstantInterface.DICTIONARY + ConstantInterface.SPRIT + dbFileName, dictionaryVosStr);


        } catch (Exception e) {
            log.error(" 请求失败 URL ： " + I18URL + ConstantInterface.DICTIONARY + ConstantInterface.SPRIT  );
            e.printStackTrace();
        }
    }

    private void addEntryToDIVo(String langCode, EntryInfoEntity entryInfoEntity, String transType, List<DictionaryVo> dictionaryVos) {
        DictionaryVo dictionaryVo = new DictionaryVo();
        dictionaryVo.setSource(entryInfoEntity.getEntry());
        dictionaryVo.setComments(entryInfoEntity.getDiFileName());
        dictionaryVo.setTag(entryInfoEntity.getTag());
        Map<String, String> transMap = new HashMap<>();
        switch (transType) {
            case ConstantInterface.ENGLISH:
                if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
                    transMap.put(langCode, entryInfoEntity.getEnglish());
                    dictionaryVo.setTranslation(transMap);
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
                    transMap.put(langCode, entryInfoEntity.getRussian());
                    dictionaryVo.setTranslation(transMap);
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
                    transMap.put(langCode, entryInfoEntity.getSpanish());
                    dictionaryVo.setTranslation(transMap);
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
                    transMap.put(langCode, entryInfoEntity.getFrench());
                    dictionaryVo.setTranslation(transMap);
                }
                break;
        }
        dictionaryVos.add(dictionaryVo);


    }


    private boolean updateTransToDiVo(String langCode, JSONArray jsonArray, EntryInfoEntity entryInfoEntity, String transType, List<DictionaryVo> dictionaryVos) {
        boolean isExist = false;
        for (int i = 0; i < jsonArray.size(); i++) {
            DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
            if (dictionaryVo.getSource().equals(entryInfoEntity.getEntry())) {
                if (!dictionaryVo.getTag().equals(entryInfoEntity.getTag())) {
                    continue;
                }
                isExist = true;
                Map<String, String> transMap = dictionaryVo.getTranslation();
                if (CollectionUtils.isEmpty(transMap)) {
                    transMap = new HashMap<>();
                }
                ArrayList<Map<String, String>> transList = new ArrayList<>();
                switch (transType) {
                    case ConstantInterface.ENGLISH:
                        if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
                            transMap.put(langCode, entryInfoEntity.getEnglish());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                    case ConstantInterface.RUSSIAN:
                        if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
                            transMap.put(langCode, entryInfoEntity.getRussian());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                    case ConstantInterface.SPANISH:
                        if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
                            transMap.put(langCode, entryInfoEntity.getSpanish());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                    case ConstantInterface.FRENCH:
                        if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
                            transMap.put(langCode, entryInfoEntity.getFrench());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                }

            }


            dictionaryVos.add(dictionaryVo);

        }
        return isExist;
    }


    private boolean updateTransToDbcVo(DictionaryVo dictionaryVo, List<EntryInfoEntity> entryInfoEntities, String transType, List<DictionaryVo> dictionaryVos) {
        String langCode = languageMapper.selectLaguageByName(transType).get(0).getCode();
        boolean is_exist = false;
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            if (dictionaryVo.getSource().equals(entryInfoEntity.getEntry())) {
                if (!dictionaryVo.getTag().equals(entryInfoEntity.getTag())) {
                    continue;
                }
                is_exist = true;
                Map<String, String> transMap = dictionaryVo.getTranslation();
                if (CollectionUtils.isEmpty(transMap)) {
                    transMap = new HashMap<>();
                }
                ArrayList<Map<String, String>> transList = new ArrayList<>();
                switch (transType) {
                    case ConstantInterface.ENGLISH:
                        if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
                            transMap.put(langCode, entryInfoEntity.getEnglish());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                    case ConstantInterface.RUSSIAN:
                        if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
                            transMap.put(langCode, entryInfoEntity.getRussian());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                    case ConstantInterface.SPANISH:
                        if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
                            transMap.put(langCode, entryInfoEntity.getSpanish());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                    case ConstantInterface.FRENCH:
                        if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
                            transMap.put(langCode, entryInfoEntity.getFrench());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                }
            }
        }
        dictionaryVos.add(dictionaryVo);
        return is_exist;
    }


    private String getTransByType(EntryInfoEntity entryInfoEntity, String translateType) {
        String trans = "";
        switch (translateType) {
            case ConstantInterface.ENGLISH:
                trans = entryInfoEntity.getEnglish();
                break;
            case ConstantInterface.RUSSIAN:
                trans = entryInfoEntity.getRussian();
                break;
            case ConstantInterface.SPANISH:
                trans = entryInfoEntity.getSpanish();
                break;
            case ConstantInterface.FRENCH:
                trans = entryInfoEntity.getFrench();
                break;
        }
        return trans;
    }



    //1.遍历 entryTempEntities
    //2.如果词条有翻译id 则代表是公共词条 ，没有则创建翻译，插入翻译表
    //3.检查词条实体是否存在儿子，如果存在则让父子翻译id相同
    //4.插入词条
    public int buildTranslateEntity(List<EntryTempEntity> entryTempEntities, List<EntryInfoEntity> entryInfoEntities, int isUpdate) {
        int insert = 0;
        for (EntryTempEntity entryTempEntity : entryTempEntities) {
            String transID = commonUtils.getUUID();
            EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
            entryInfoEntity.setId(entryTempEntity.getId());
            TranslateEntity translateEntity = new TranslateEntity();

            //翻译id是空 则不是公共词条库的词条，需新增翻译
            if (StringUtils.isBlank(entryTempEntity.getTranslateID())) {
                translateEntity.setId(transID);
                translateEntity.setTranslate(entryTempEntity.getTranslate());
                translateEntity.setTranslateState("3");
                translateEntity.setEntry(entryTempEntity.getEntry());
                translateEntity.setType(entryTempEntity.getTranslateType());
                translateEntity.setVisualRange("部门");
                translateEntity.setDeleteState(0);
                translateEntity.setVersionID(entryTempEntity.getVersionID());

                if (StringUtils.isNotBlank(entryTempEntity.getTranslate())) {
                    translateMapper.insert(translateEntity);
                }

                entryInfoService.addTransID(translateEntity, entryInfoEntity);
            } else {
                translateEntity.setId(entryTempEntity.getTranslateID());
                translateEntity.setType(entryTempEntity.getTranslateType());
            }

            //如果孩子不为空，则新建词条插入
            if (!CollectionUtils.isEmpty(entryTempEntity.getChildren())) {
                for (EntryTempEntity childTempEntry : entryTempEntities) {
                    EntryInfoEntity entryInfoEntity1 = new EntryInfoEntity();
                    childTempEntry.setId(childTempEntry.getId());
                    entryInfoService.addTransID(translateEntity, entryInfoEntity1);

                    insertTempToVersionEntry(childTempEntry, entryInfoEntity1, isUpdate);
                    entryInfoEntities.add(entryInfoEntity1);
                }
            }
            insert += insertTempToVersionEntry(entryTempEntity, entryInfoEntity, isUpdate);
            entryInfoEntities.add(entryInfoEntity);

        }
        return insert;
    }

    private int insertTempToVersionEntry(EntryTempEntity entryTempEntity, EntryInfoEntity entryInfoEntity, int isUpdate) {
        int insert = 0;
        if (1 == isUpdate) {
            insert = entryInfoMapper.updateEntryInfo(entryInfoEntity);
        } else if (0 == isUpdate) {
            //写版本表
            entryInfoEntity.setEntry(entryTempEntity.getEntry());
            entryInfoEntity.setVersionID(entryTempEntity.getVersionID());
            entryInfoEntity.setIsPublic(0);
            entryInfoEntity.setTaskId(entryTempEntity.getTaskId());
            entryInfoEntity.setEntrySource(entryTempEntity.getSource());
            entryInfoEntity.setAbbr(entryTempEntity.getAbbr());
            entryInfoEntity.setIsDelete(0);
            entryInfoEntity.setImportType(entryTempEntity.getImportype());
            entryInfoEntity.setEntryState(0);
            String versionID = entryTempEntity.getVersionID();
            VersionEntity versionEntity = versionMapper.selectById(versionID);
            String tableName = versionEntity.getTableName();
            insert += entryInfoMapper.insertEntry(entryInfoEntity, tableName);


            //entryInfoMapper.insert(entryInfoEntity);

        }
        return insert;
    }


    @GetMapping("/getDictionaryInfo")
    @ApiOperation("获取字典详情")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getDictionaryInfo(@RequestParam String type,
                                                             @RequestParam String transType,
                                                             @RequestParam String versionID,
                                                             @RequestParam String taskID, HttpServletRequest request) {

        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);
        List<TLanguage> languageList = languageMapper.selectLaguageByName(transType);
        String code = "";
        if (CollectionUtils.isEmpty(languageList)) {
            code = languageList.get(0).getName();
        }
        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<EntryInfoEntity> list = new ArrayList<>();
        String productId = versionMapper.getVersionByID(versionID).getProductId();
        try {
            s = httpUtils.get(I18URL + ConstantInterface.DICTIONARY + ConstantInterface.SPRIT + type);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
                EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                entryInfoEntity.setId(commonUtils.getUUID());
                entryInfoEntity.setEntry(dictionaryVo.getSource());
                entryInfoEntity.setVersionID(versionID);
                entryInfoEntity.setTaskId(taskID);
                entryInfoEntity.setImportType(ConstantInterface.DI);
                entryInfoEntity.setEntryState(1);
                entryInfoEntity.setEntrySource(type);
                entryInfoEntity.setDiFileName(type);
                entryInfoEntity.setProductID(productId);
                entryInfoEntity.setIsDelete(0);
                entryInfoEntity.setUpdateTime(date);
                entryInfoEntity.setUpdate(userName);
                Map<String, String> map = dictionaryVo.getTranslation();
                if (StringUtils.isBlank(map.get(code))) {
                    switch (code) {
                        case ConstantInterface.ENGLISH:
                            entryInfoEntity.setEnglish(map.get(code));
                            break;
                        case ConstantInterface.RUSSIAN:
                            entryInfoEntity.setRussian(map.get(code));
                            break;
                        case ConstantInterface.FRENCH:
                            entryInfoEntity.setFrench(map.get(code));
                            break;
                        case ConstantInterface.SPANISH:
                            entryInfoEntity.setSpanish(map.get(code));
                            break;
                    }
                }
                list.add(entryInfoEntity);


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryInfoEntities = buildRepeTempEntry(list, transType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryInfoEntities, taskID);
        responseListModel.setList(entryInfoEntities);
        responseListModel.setTotalNum(entryInfoEntities.size());
        return checkResult(responseListModel);
    }


    @GetMapping("/getAllNode")
    @ApiOperation("获取节点信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getAllNode() {

        ResponseListModel<String> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            s = httpUtils.get(I18URL + ConstantInterface.GET_ALL_NODE);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                list.add(jsonArray.get(i).toString());
            }
            log.info(" start send http request : " + I18URL + ConstantInterface.GET_ALL_NODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }

    @GetMapping("/getAppByNode")
    @ApiOperation("获取应用信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getAppByNode(@RequestParam String nodeName) {
        ResponseListModel<TDBappVo> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<TDBappVo> list = new ArrayList<>();
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("nodeName", nodeName);
            s = httpUtils.get(I18URL + ConstantInterface.GET_APP_BYNODE, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBappVo tdBappVo = new TDBappVo();
                JSONArray jsonArray1 = JSONArray.parseArray(jsonArray.getString(i));
                String name = jsonArray1.getString(0);
                int type = jsonArray1.getInteger(1);
                tdBappVo.setName(name);
                tdBappVo.setType(type);
                list.add(tdBappVo);
            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }


    @GetMapping("/getdbByApp")
    @ApiOperation("获取数据库信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getdbByApp(@RequestParam String nodeName,
                                                      @RequestParam String appName,
                                                      @RequestParam String modeType) {
        ResponseListModel<String> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<String> list = new ArrayList<>();
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("nodeName", nodeName);
            headerParameters.put("appName", appName);
            headerParameters.put("modeType", modeType);
            s = httpUtils.get(I18URL + ConstantInterface.GET_DB_BYAPP, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {

                list.add(jsonArray.getString(i));
            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }

    @GetMapping("/getTableByApp")
    @ApiOperation("获取表信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getTableByApp(@RequestParam String dbName,
                                                         @RequestParam String nodeName,
                                                         @RequestParam String appName) {
        ResponseListModel<TDBTableInfo> responseListModel = new ResponseListModel<>();

        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<TDBTableInfo> list = new ArrayList<>();
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("dbName", dbName);
            headerParameters.put("appName", appName);
            headerParameters.put("nodeName", nodeName);
            s = httpUtils.get(I18URL + ConstantInterface.GET_TB_APP, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBTableInfo tdbTableInfo = JSONArray.parseObject(jsonArray.getString(i), TDBTableInfo.class);

                list.add(tdbTableInfo);
            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseListModel.setList(list);
        responseListModel.setTotalNum(list.size());
        return checkResult(responseListModel);
    }


    @GetMapping("/getFieldByTable")
    @ApiOperation("获取字段集合")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getFieldByTable(@RequestParam String dbName,
                                                           @RequestParam String nodeName,
                                                           @RequestParam String appName,

                                                           @RequestParam String tbName
    ) {
        ResponseListModel<TDBFieldInfo> responseListModel = new ResponseListModel<>();
        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<TDBFieldInfo> fieldList = new ArrayList<>();

        try {

            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("dbName", dbName);
            headerParameters.put("appName", appName);
            headerParameters.put("tbName", tbName);
            headerParameters.put("nodeName", nodeName);
            s = httpUtils.get(I18URL + ConstantInterface.GET_FIELD_TABLE, headerParameters);
            if ("0".equals(s)) {
                return checkResult(null, " request error ! ");
            }
            jsonArray = JSONArray.parseArray(s);


            for (int i = 0; i < jsonArray.size(); i++) {
                TDBFieldInfo tdbFieldInfo = JSONArray.parseObject(jsonArray.getString(i), TDBFieldInfo.class);
                fieldList.add(tdbFieldInfo);
            }
            //  tdbTableInfo.setFields(fieldList);

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }


        responseListModel.setList(fieldList);
        responseListModel.setTotalNum(fieldList.size());
        return checkResult(responseListModel);
    }


    @PostMapping("/getFieldData")
    @ApiOperation("获取字段内容")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getFieldData(@RequestParam String dbName,
                                                        @RequestParam String nodeName,
                                                        @RequestParam String appName,
                                                        @RequestParam int tbID,
                                                        @RequestParam String tbName,
                                                        @RequestParam String versionID,
                                                        @RequestParam String taskID,
                                                        @RequestParam String translateType,
                                                        @RequestParam String diFileName,
                                                        @RequestBody List<TDBFieldInfo> fieldInfos, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);
        String productId = versionMapper.getVersionByID(versionID).getProductId();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();
        String s = "";
        ArrayList<EntryInfoEntity> fieldList = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("dbName", dbName);
            jsonObject.put("appName", appName);
            jsonObject.put("tbName", tbName);
            jsonObject.put("nodeName", nodeName);
            jsonObject.put("tbID", tbID);
            jsonObject.put("fieldInfos", fieldInfos);
            s = httpUtils.post(I18URL + ConstantInterface.GET_FIELD_DATA, jsonObject);
            if ("0".equals(s)) {
                return checkResult(null, " request error ! ");
            }
            jsonArray = JSONArray.parseArray(s);

            String source = nodeName + "_" + appName + "_" + dbName + "_" + tbName;
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBFieldInfo tdbFieldInfo = JSONArray.parseObject(jsonArray.getString(i), TDBFieldInfo.class);
                for (String entry : tdbFieldInfo.getFieldDatas()) {
                    EntryInfoEntity fieldEntry = new EntryInfoEntity();
                    fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbFieldInfo.getFieldID());
                    fieldEntry.setEntry(entry);

                    // fieldEntry.setEntrySource(  nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tbName + ConstantInterface.UNDERLINE + tdbFieldInfo.getFieldName());
                    fieldEntry.setEntryState(1);
                    fieldEntry.setDiFileName(diFileName);
                    fieldEntry.setEntrySource(source);
                    fieldEntry.setVersionID(versionID);
                    fieldEntry.setTaskId(taskID);
                    fieldEntry.setUpdate(userName);
                    fieldEntry.setUpdateTime(date);
                    fieldEntry.setProductID(productId);
                    fieldEntry.setIsDelete(0);
                    fieldEntry.setImportType(ConstantInterface.DB);
                    entryInfoEntities.add(fieldEntry);
                }


                //   fieldList.add(fieldEntry);
            }
            //  tdbTableInfo.setFields(fieldList);

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<EntryInfoEntity> entryInfoEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryInfoEntities, taskID);
        responseListModel.setList(entryInfoEntities1);
        responseListModel.setTotalNum(entryInfoEntities1.size());
        return checkResult(responseListModel);
    }


    @GetMapping("/getAlias")
    @ApiOperation("获取别名")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getAlias(@RequestParam String dbName,
                                                    @RequestParam String nodeName,
                                                    @RequestParam String appName,
                                                    @RequestParam String versionID,
                                                    @RequestParam String taskID,
                                                    @RequestParam String diFileName,
                                                    @RequestParam String translateType, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        String productId = versionMapper.selectById(versionID).getProductId();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);

        JSONArray jsonArray = new JSONArray();
        String s = "";
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("dbName", dbName);
            headerParameters.put("appName", appName);
            headerParameters.put("nodeName", nodeName);
            s = httpUtils.get(I18URL + ConstantInterface.GET_ALIAS, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            String source = nodeName + "_" + appName + "_" + dbName;
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBTableInfo tdbTableInfo = JSONArray.parseObject(jsonArray.getString(i), TDBTableInfo.class);

                //将表的别名写入词条
                EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                entryInfoEntity.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbTableInfo.getTableId());
                entryInfoEntity.setEntry(tdbTableInfo.getAlias());
                entryInfoEntity.setDiFileName(diFileName);
                entryInfoEntity.setEntrySource(source);
                //  entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                entryInfoEntity.setEntryState(1);
                entryInfoEntity.setVersionID(versionID);
                entryInfoEntity.setTaskId(taskID);
                createNewTrans(entryInfoEntity, translateType, "");
                entryInfoEntity.setImportType(ConstantInterface.DB);
                entryInfoEntity.setProductID(productId);
                entryInfoEntities.add(entryInfoEntity);

                //写表下的别名
                List<TDBFieldInfo> fields = tdbTableInfo.getFields();
                for (TDBFieldInfo fieldInfo : fields) {
                    EntryInfoEntity fieldEntry = new EntryInfoEntity();
                    fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                    fieldEntry.setEntry(fieldInfo.getAliasName());
                    fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                    fieldEntry.setEntryState(1);
                    fieldEntry.setEntrySource(source + "_" + tdbTableInfo.getTableName());
                    fieldEntry.setIsDelete(0);
                    fieldEntry.setUpdateTime(date);
                    fieldEntry.setUpdate(userName);
                    fieldEntry.setImportType(ConstantInterface.DB);
                    entryInfoEntities.add(fieldEntry);
                }


            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryEntities1, taskID);
        responseListModel.setList(entryEntities1);
        responseListModel.setTotalNum(entryEntities1.size());
        return checkResult(responseListModel);
    }


    @GetMapping("/getDBALLAliasEntryByNode")
    @ApiOperation("通过节点名取别名")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getDBALLAliasEntryByNode(@RequestParam String nodeName,
                                                                    @RequestParam String versionID,
                                                                    @RequestParam String taskID,
                                                                    @RequestParam String diFileName,
                                                                    @RequestParam String translateType, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        String productId = taskInfoMapper.selectById(taskID).getProductId();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);

        JSONArray jsonArray = new JSONArray();
        String s = "";
        try {
            Map<String, String> headerParameters = new HashMap<>();

            headerParameters.put("nodeName", nodeName);
            s = httpUtils.get(I18URL + ConstantInterface.GET_DBALLENTRYBYNODE, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBTableInfo tdbTableInfo = JSONArray.parseObject(jsonArray.getString(i), TDBTableInfo.class);

                //将表的别名写入词条
                EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                entryInfoEntity.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbTableInfo.getTableId());
                entryInfoEntity.setEntry(tdbTableInfo.getAlias());
                entryInfoEntity.setDiFileName(diFileName);
                entryInfoEntity.setEntrySource(tdbTableInfo.getCommon());
                // entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                entryInfoEntity.setEntryState(1);
                entryInfoEntity.setVersionID(versionID);
                entryInfoEntity.setTaskId(taskID);
                createNewTrans(entryInfoEntity, translateType, "");
                entryInfoEntity.setImportType(ConstantInterface.DB);
                entryInfoEntity.setProductID(productId);
                entryInfoEntities.add(entryInfoEntity);

                //写表下的别名
                List<TDBFieldInfo> fields = tdbTableInfo.getFields();
                for (TDBFieldInfo fieldInfo : fields) {
                    EntryInfoEntity fieldEntry = new EntryInfoEntity();
                    fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                    fieldEntry.setEntry(fieldInfo.getAliasName());
                    fieldEntry.setDiFileName(diFileName);
                    // fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                    fieldEntry.setEntryState(1);
                    fieldEntry.setEntrySource(fieldInfo.getCommon());
                    fieldEntry.setIsDelete(0);
                    fieldEntry.setUpdateTime(date);
                    fieldEntry.setUpdate(userName);
                    fieldEntry.setImportType(ConstantInterface.DB);
                    entryInfoEntities.add(fieldEntry);
                }


            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryEntities1, taskID);
        responseListModel.setList(entryEntities1);
        responseListModel.setTotalNum(entryEntities1.size());
        return checkResult(responseListModel);
    }


    @GetMapping("/getDBALLFileEntryByNode")
    @ApiOperation("通过节点名取字段")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getDBALLFileEntryByNode(@RequestParam String nodeName,
                                                                   @RequestParam String versionID,
                                                                   @RequestParam String taskID,
                                                                   @RequestParam String diFileName,
                                                                   @RequestParam String translateType, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        String productId = taskInfoMapper.selectById(taskID).getProductId();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);

        JSONArray jsonArray = new JSONArray();
        String s = "";
        try {
            Map<String, String> headerParameters = new HashMap<>();

            headerParameters.put("nodeName", nodeName);
            System.out.println("get request :" + I18URL + ConstantInterface.GET_DBALLENTRYBYNODE);
            s = httpUtils.get(I18URL + ConstantInterface.GET_DBALLENTRYBYNODE, headerParameters);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBTableInfo tdbTableInfo = JSONArray.parseObject(jsonArray.getString(i), TDBTableInfo.class);

                //将表的别名写入词条
                EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                entryInfoEntity.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbTableInfo.getTableId());
                entryInfoEntity.setEntry(tdbTableInfo.getTableName());
                entryInfoEntity.setDiFileName(diFileName);
                entryInfoEntity.setEntrySource(tdbTableInfo.getCommon());
                // entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                entryInfoEntity.setEntryState(1);
                entryInfoEntity.setVersionID(versionID);
                entryInfoEntity.setTaskId(taskID);
                createNewTrans(entryInfoEntity, translateType, "");
                entryInfoEntity.setImportType(ConstantInterface.DB);
                entryInfoEntity.setProductID(productId);
                entryInfoEntities.add(entryInfoEntity);

                //写表下的别名
                List<TDBFieldInfo> fields = tdbTableInfo.getFields();
                for (TDBFieldInfo fieldInfo : fields) {
                    EntryInfoEntity fieldEntry = new EntryInfoEntity();
                    fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + fieldInfo.getFieldID());
                    fieldEntry.setEntry(fieldInfo.getFieldName());
                    // fieldEntry.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tdbTableInfo.getTableName());
                    fieldEntry.setEntryState(1);
                    fieldEntry.setDiFileName(diFileName);
                    fieldEntry.setEntrySource(fieldInfo.getCommon());
                    fieldEntry.setIsDelete(0);
                    fieldEntry.setUpdateTime(date);
                    fieldEntry.setUpdate(userName);
                    fieldEntry.setImportType(ConstantInterface.DB);
                    entryInfoEntities.add(fieldEntry);
                }


            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryEntities1, taskID);
        responseListModel.setList(entryEntities1);
        responseListModel.setTotalNum(entryEntities1.size());
        return checkResult(responseListModel);
    }


    public List<EntryInfoEntity> buildRepeTempEntry(List<EntryInfoEntity> entryInfoEntities, String translateType) {
        return entryProcessUtils.buildRepeEntry(entryInfoEntities, translateType);
    }

    //校验是否有重复词条
    private void caseExistEntry(List<EntryInfoEntity> newEntry, String taskID) {
        TaskInfoEntity taskInfoEntity = taskInfoMapper.getTaskEntityByTaskID(taskID);

        //  ProductTableEntity productTableEntity = productTableMapper.getTableInfoByProductId(taskInfoEntity.getProductId());
        String productTableName = "t_entry_info";

        for (EntryInfoEntity entryInfoEntity : newEntry) {
            // entryTempEntityQueryWrapper.eq("entry_version",entryTempEntity.getEntryVersion());
            List<EntryInfoEntity> entryEntities = entryInfoMapper.getExistEntryList(productTableName, entryInfoEntity, taskInfoEntity.getProductId());
            if (CollectionUtils.isEmpty(entryEntities)) {
                //创建新翻译
                entryInfoEntity.setIsExist(0);
                entryInfoEntity.setEntryVersion(0);
                entryInfoEntity.setEntryVersionID(commonUtils.getUUID());
            } else {
                entryInfoEntity.setIsExist(1);
                entryInfoEntity.setEntryVersionID(entryEntities.get(0).getEntryVersionID());
            }
        }

    }


    @PostMapping("/saveDIWords")
    @ApiOperation("回写di")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> saveDIWords(@RequestParam String diFileName,
                                            @RequestParam String transType,
                                            @RequestBody List<EntryInfoEntity> entryInfoEntities) {
        writeDiWords(diFileName, transType, entryInfoEntities);

        return checkResult(ConstantInterface.OK_STR);

    }

    private void writeDiWords(String diFileName, String transType, List<EntryInfoEntity> diEntrInfo) {
        //获取字典所有词条，将对应词条的对应翻译插入其中 ，再全量写入
        String s = "";
        String s2 = "";
        JSONArray jsonArray;
        String langCode = languageMapper.selectLaguageByName(transType).get(0).getCode();
        try {
            s = httpUtils.get(I18URL + ConstantInterface.DICTIONARY + ConstantInterface.SPRIT + diFileName);
            jsonArray = JSONArray.parseArray(s);
            List<DictionaryVo> dictionaryVos = new ArrayList<>();

            for (EntryInfoEntity entryInfoEntity : diEntrInfo) {
                boolean isExist = updateTransToDiVo(langCode, jsonArray, entryInfoEntity, transType, dictionaryVos);
                if (!isExist){
                    log.warn(" entry : ("  + entryInfoEntity.getEntry()  + ") tag : (" + entryInfoEntity.getTag() + ") 在辞典 " + diFileName + " 中没找到 ！ " );
                }
            }

        /*    for (int i = 0; i < jsonArray.size(); i++) {
                DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
                addTransToDicVo(dictionaryVo, entryInfoEntities, transType, dictionaryVos);
            }*/
            String dictionaryVosStr = JSONObject.toJSONString(dictionaryVos);

            //10.16.193.63:18099/dictionary/user
            s2 = httpUtils.post(I18URL + ConstantInterface.DICTIONARY + ConstantInterface.SPRIT + diFileName, dictionaryVosStr);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //写入翻译
    private void addTransToDicVo(DictionaryVo dictionaryVo, List<EntryInfoEntity> entryInfoEntities, String transType, List<DictionaryVo> dictionaryVos) {
        String langCode = languageMapper.selectLaguageByName(transType).get(0).getCode();
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            if (dictionaryVo.getSource().equals(entryInfoEntity.getEntry())) {
                if (!dictionaryVo.getTag().equals(entryInfoEntity.getTag())) {
                    continue;
                }
                Map<String, String> transMap = dictionaryVo.getTranslation();
                if (CollectionUtils.isEmpty(transMap)) {
                    transMap = new HashMap<>();
                }
                ArrayList<Map<String, String>> transList = new ArrayList<>();
                switch (transType) {
                    case ConstantInterface.ENGLISH:
                        if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
                            transMap.put(langCode, entryInfoEntity.getEnglish());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                    case ConstantInterface.RUSSIAN:
                        if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
                            transMap.put(langCode, entryInfoEntity.getRussian());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                    case ConstantInterface.SPANISH:
                        if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
                            transMap.put(langCode, entryInfoEntity.getSpanish());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                    case ConstantInterface.FRENCH:
                        if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
                            transMap.put(langCode, entryInfoEntity.getFrench());
                            dictionaryVo.setTranslation(transMap);
                        }
                        break;
                }
            }
        }
        dictionaryVos.add(dictionaryVo);
    }


    @GetMapping("/getConfigEntry")
    @ApiOperation("读取配置文件词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getConfigEntry(@RequestParam String versionID,
                                                          @RequestParam String taskID,
                                                          @RequestParam String diFileName,
                                                          @RequestParam String translateType, HttpServletRequest request
    ) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        String productId = taskInfoMapper.selectById(taskID).getProductId();
        String token = request.getHeader("token");
        Date date = new Date(System.currentTimeMillis());
        String userName = JWTTokenUtils.getUserName(token);

        JSONArray jsonArray = new JSONArray();
        String s = "";
        try {

            s = httpUtils.get(I18URL + ConstantInterface.GET_CONGIF_ENTRY);
            jsonArray = JSONArray.parseArray(s);
            for (int i = 0; i < jsonArray.size(); i++) {
                TDBFieldInfo fieldInfo = JSONArray.parseObject(jsonArray.getString(i), TDBFieldInfo.class);

                //将表的别名写入词条
                EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                entryInfoEntity.setId(commonUtils.getUUID());
                entryInfoEntity.setEntry(fieldInfo.getFieldName());
                entryInfoEntity.setDiFileName(diFileName);
                entryInfoEntity.setEntrySource(fieldInfo.getCommon());
                // entryInfoEntity.setEntrySource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName);
                entryInfoEntity.setEntryState(1);
                entryInfoEntity.setVersionID(versionID);
                entryInfoEntity.setTaskId(taskID);
                createNewTrans(entryInfoEntity, translateType, "");
                entryInfoEntity.setImportType(ConstantInterface.CONFIG);
                entryInfoEntity.setProductID(productId);
                entryInfoEntities.add(entryInfoEntity);


            }

            log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<EntryInfoEntity> entryEntities1 = buildRepeTempEntry(entryInfoEntities, translateType);
        //查询产品表里的词条是否有重复的
        caseExistEntry(entryEntities1, taskID);
        responseListModel.setList(entryEntities1);
        responseListModel.setTotalNum(entryEntities1.size());
        return checkResult(responseListModel);
    }


}


   /* @PostMapping("/getFieldData")
    @ApiOperation("获取字段内容")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getFieldData(@RequestParam String dbName,
                                                        @RequestParam String nodeName,
                                                        @RequestParam String appName,
                                                        @RequestParam String versionID,
                                                        @RequestParam String taskID,
                                                        @RequestParam String translateType,
                                                        @RequestParam String type,
                                                        @RequestParam String tbName,
                                                        @RequestBody List<TDBTableInfo> tdbTableInfos
    ) {
        ResponseListModel<EntryTempEntity> responseListModel = new ResponseListModel<>();
        ArrayList<EntryTempEntity> entryTempEntities = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();
        String s = "";
        for (TDBTableInfo tdbTableInfo : tdbTableInfos) {
            String tbName = tdbTableInfo.getTableName();
            try {
                ArrayList<TDBFieldInfo> fieldList = new ArrayList<>();
                Map<String, String> headerParameters = new HashMap<>();
                headerParameters.put("dbName", dbName);
                headerParameters.put("appName", appName);
                headerParameters.put("tbName", tbName);
                s = httpUtils.get(I18URL + ConstantInterface.GET_FIELD_TABLE, headerParameters);
                if ("0".equals(s)) {
                    return checkResult(null, " request error ! ");
                }
                jsonArray = JSONArray.parseArray(s);


                for (int i = 0; i < jsonArray.size(); i++) {
                    TDBFieldInfo tdbFieldInfo = JSONArray.parseObject(jsonArray.getString(i), TDBFieldInfo.class);
                    EntryTempEntity fieldEntry = new EntryTempEntity();
                    fieldEntry.setId(commonUtils.getUUID() + ConstantInterface.UNDERLINE + tdbFieldInfo.getId());
                    if (type.equals("field")) {
                        fieldEntry.setEntry(tdbFieldInfo.getFieldName());

                    } else if (type.equals("alias")) {
                        fieldEntry.setEntry(tdbFieldInfo.getAliasName());
                    }
                    fieldEntry.setSource(nodeName + ConstantInterface.UNDERLINE + appName + ConstantInterface.UNDERLINE + dbName + ConstantInterface.UNDERLINE + tbName);
                    fieldEntry.setAuditState(0);
                    fieldEntry.setVersionID(versionID);
                    fieldEntry.setTaskId(taskID);
                    fieldEntry.setTranslateType(translateType);
                    fieldEntry.setImportype(ConstantInterface.DB);
                    entryTempEntities.add(fieldEntry);
                    fieldList.add(tdbFieldInfo);
                }
                tdbTableInfo.setFields(fieldList);

                log.info(" start send http request : " + I18URL + ConstantInterface.GET_APP_BYNODE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        responseListModel.setList(entryTempEntities);
        responseListModel.setTotalNum(entryTempEntities.size());
        return checkResult(responseListModel);
    }*/
