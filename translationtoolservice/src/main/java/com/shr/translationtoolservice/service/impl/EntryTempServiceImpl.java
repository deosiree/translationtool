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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private BaiduTransUtils baiduTransUtils;

    @Autowired
    private TranslateMapper translateMapper;

    @Autowired
    private ProductTableMapper productTableMapper;

    @Autowired
    private TaskInfoMapper taskInfoMapper;

    @Value("${ConfigFile.zzUrl}")
    private String configFileZZUrl;

    @Value("${ConfigFile.jkUrl}")
    private String configFilejkUrl;

    @Value("${ConfigFile.commonUrl}")
    private String configFileCommonUrl;

    @Value("${ConfigFile.zh_CN.commonUrl}")
    private String config_File_Common_ZH_Url;
    @Value("${ConfigFile.zh_CN.jkUrl}")
    private String config_File_JK_ZH_Url;
    @Value("${ConfigFile.zh_CN.zzUrl}")
    private String config_File_ZZ_ZH_Url;

    @Value("${ConfigFile.en_US.commonUrl}")
    private String config_File_Common_EN_Url;
    @Value("${ConfigFile.en_US.jkUrl}")
    private String config_File_JK_EN_Url;
    @Value("${ConfigFile.en_US.zzUrl}")
    private String config_File_ZZ_EN_Url;

    @Value("${ConfigFile.es_ES.commonUrl}")
    private String config_File_Common_ES_Url;
    @Value("${ConfigFile.es_ES.jkUrl}")
    private String config_File_JK_ES_Url;
    @Value("${ConfigFile.es_ES.zzUrl}")
    private String config_File_ZZ_ES_Url;

    @Value("${ConfigFile.fr_FR.commonUrl}")
    private String config_File_Common_FR_Url;
    @Value("${ConfigFile.fr_FR.jkUrl}")
    private String config_File_JK_FR_Url;
    @Value("${ConfigFile.fr_FR.zzUrl}")
    private String config_File_ZZ_FR_Url;

    @Value("${ConfigFile.ru_RU.commonUrl}")
    private String config_File_Common_RU_Url;
    @Value("${ConfigFile.ru_RU.jkUrl}")
    private String config_File_JK_RU_Url;
    @Value("${ConfigFile.ru_RU.zzUrl}")
    private String config_File_ZZ_RU_Url;


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
    private EntryProcessUtils entryProcessUtils;

    @Autowired
    private DeepLTranslateUtils deepLTranslateUtils;

    @Autowired
    private LocalTimeUtils localTimeUtils;

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
    public String deleteEntryInfoByID(List<String> entryIDs,String productID,String versionID) {

        HashMap<String, Object> map = new HashMap();
        for (String entryID : entryIDs) {
            map.put("entry_id", entryID);
            if (StringUtils.isNotBlank(productID)) {
                map.put("product_id", productID);
            }
            if (StringUtils.isNotBlank(versionID)) {
                map.put("version_id", versionID);
            }
        }

        // 1.删除当前产品版本下的关联信息
        productRelationMapper.deleteByMap(map);
        List<ProductRelationEntity> productRelationEntities = productRelationMapper.selectList(new QueryWrapper<ProductRelationEntity>().eq("entry_id", entryIDs));
        // 2.删除完后用entryID查询关联表是否有被关联的词条，如果没有则词条表删除
        if (CollectionUtils.isEmpty(productRelationEntities)) {
            entryInfoMapper.deleteBatchIds(entryIDs);
        }
        return ConstantInterface.OK_STR;
    }
    @Override
    public String deleteEntryInfoByTaskID(List<String> entryIDs,String taskID) {

        HashMap<String, Object> map = new HashMap();
        for (String entryID : entryIDs) {
            map.put("entry_id", entryID);
            map.put("task_id", taskID);
        }

        // 1.删除当前产品版本下的关联信息
        productRelationMapper.deleteByMap(map);
        List<ProductRelationEntity> productRelationEntities = productRelationMapper.selectList(new QueryWrapper<ProductRelationEntity>().eq("entry_id", entryIDs));
        // 2.删除完后用entryID查询关联表是否有被关联的词条，如果没有则词条表删除
        if (CollectionUtils.isEmpty(productRelationEntities)) {
            entryInfoMapper.deleteBatchIds(entryIDs);
        }
        return ConstantInterface.OK_STR;
    }
    @Override
    public int getEntryTempByTaskIDTotal(String taskID) {

        return entryTempMapper.getEntryTempByTaskIDTotal(taskID);
    }

    @Override
    public List<EntryInfoEntity> preTranslate(HttpServletRequest request,List<EntryInfoEntity> entryInfoList, String taskID, String priority) {
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
            for (EntryInfoEntity entryInfoEntity : entryInfoList) {
            //子不翻译
            if (StringUtils.isNotBlank(entryInfoEntity.getParentID())) {
                continue;
            }
            String translateType = taskInfoMapper.selectById(taskID).getTranslateType();
            String translate = "";

            switch (translateType) {
                case ConstantInterface.CHINESE:
                    if (StringUtils.isBlank(entryInfoEntity.getChinese())) {
                        translate = addSuggessTransByPriority(entryInfoEntity, translateType, priority,department);
                        if (StringUtils.isNotBlank(translate)) {
                            entryInfoEntity.setChinese(translate);
                        }
                    }
                    entryInfoEntities.add(entryInfoEntity);
                    break;
                case ConstantInterface.ENGLISH:
                    if (StringUtils.isBlank(entryInfoEntity.getEnglish())) {
                        translate = addSuggessTransByPriority(entryInfoEntity, translateType, priority,department);
                        if (StringUtils.isNotBlank(translate)) {
                            entryInfoEntity.setEnglish(translate);
                        }
                    }
                    entryInfoEntities.add(entryInfoEntity);
                    break;
                case ConstantInterface.SPANISH:
                    if (StringUtils.isBlank(entryInfoEntity.getSpanish())) {
                        translate = addSuggessTransByPriority(entryInfoEntity, translateType, priority,department);
                        if (StringUtils.isNotBlank(translate)) {
                            entryInfoEntity.setSpanish(translate);
                        }
                    }
                    entryInfoEntities.add(entryInfoEntity);
                    break;
                case ConstantInterface.FRENCH:
                    if (StringUtils.isBlank(entryInfoEntity.getFrench())) {
                        translate = addSuggessTransByPriority(entryInfoEntity, translateType, priority,department);
                        if (StringUtils.isNotBlank(translate)) {
                            entryInfoEntity.setFrench(translate);
                        }
                    }
                    entryInfoEntities.add(entryInfoEntity);
                    break;
                case ConstantInterface.RUSSIAN:
                    if (StringUtils.isBlank(entryInfoEntity.getRussian())) {
                        translate = addSuggessTransByPriority(entryInfoEntity, translateType, priority,department);
                        if (StringUtils.isNotBlank(translate)) {
                            entryInfoEntity.setRussian(translate);
                        }
                    }
                    entryInfoEntities.add(entryInfoEntity);
                    break;
            }
            //entryInfoEntities 中的元素没有翻译的排序在前面
            sortEmptyTrans(entryInfoEntities,translateType);


        }
        return entryInfoEntities;
    }


    //优先级  术语库 外网
    private String addSuggessTransByPriority(EntryInfoEntity entryInfoEntity, String translateType, String priority,String department) {
        if (ConstantInterface.SYNTHESIS.equals(priority)) {
            // 综合优先级
            String trans = synthesisPriority(entryInfoEntity, translateType,department);
            return trans;
        }
        // 非综合优先级
        Queue<String> queue = new LinkedList<>();
        for (String key : ConstantInterface.translateMachine().keySet()) {
            if (priority.equals(key)) {
                queue.add(key);
            }
        }
        //术语库只走术语库
        if (priority.equals(ConstantInterface.SYK)) {
            queue.clear();
            queue.add(priority);
        }




        TLanguage language = languageMapper.selectLaguageByName(translateType).get(0);
        String translateRes = "";
        while (!queue.isEmpty()) {
            if (StringUtils.isNotBlank(translateRes)) {
                break;
            }
            String type = queue.remove();
            if (type.equals(ConstantInterface.DEEPL)) {
                // deepl翻译
                translateRes = deepLTranslateUtils.translate(entryInfoEntity.getEntry(), null, language.getDeeplCode());
            } else if (type.equals(ConstantInterface.BD)) {
                // 百度翻译
               // LanguageEntity translateResult = translateUtils.getTranslateResult(entryInfoEntity.getEntry(), ConstantInterface.AUTO, language);
                //默认主语言都是中文
                try {
                    translateRes = baiduTransUtils.translate(entryInfoEntity.getEntry(), "zh", language.getBdCode());
                } catch (IOException e) {
                    translateRes = "";
                    e.printStackTrace();
                }


            } else if (type.equals(ConstantInterface.SYK)) {
                // 术语库翻译
                translateRes = getSYKTranslate(entryInfoEntity.getEntry(), translateType,department);
            } else if (type.equals(ConstantInterface.YD)) {
                // 有道翻译
                LanguageEntity languageEntity = youdaoTrans.youdaoTranslate(entryInfoEntity.getEntry(), ConstantInterface.AUTO, language);
                if (!Objects.isNull(languageEntity)) {
                    translateRes = languageEntity.getValue();
                }
            } else if (type.equals(ConstantInterface.GG)) {
                // TODO google翻译
            } else if (type.equals(ConstantInterface.MD)) {
                // TODO 本地模型翻译
            }
        }
        return translateRes;
    }

    /**
     * 综合优先级 （使用所有的翻译引擎进行翻译，取出现次数最多的翻译为当前词条的翻译！）
     *
     * @param entryInfoEntity 词条实体
     * @param translateType   翻译语言
     * @return 翻译结果
     */
    public String synthesisPriority(EntryInfoEntity entryInfoEntity, String translateType,String department) {
        // 获取翻译语言代码
        TLanguage language = languageMapper.selectLaguageByName(translateType).get(0);
        List<String> translates = new ArrayList<>();
        // 术语库翻译
        String sykTranslate = getSYKTranslate(entryInfoEntity.getEntry(), translateType,department);
        if (null != sykTranslate && !"".equals(sykTranslate)) {
            translates.add(sykTranslate);
        }
        // deepl翻译
        String deelp = deepLTranslateUtils.translate(entryInfoEntity.getEntry(), null, language.getDeeplCode());
        if (null != deelp && !"".equals(deelp)) {
            translates.add(deelp);
        }
        // 百度翻译
        /*LanguageEntity translateResult = translateUtils.getTranslateResult(entryInfoEntity.getEntry(), ConstantInterface.AUTO, language);
        if (!Objects.isNull(translateResult)) {
            translates.add(translateResult.getValue());
        }*/
        //默认主语言都是中文
        String trans = null;
        try {
            trans = baiduTransUtils.translate(entryInfoEntity.getEntry(), "zh", language.getBdCode());
        } catch (Exception e) {
            trans = "";
            log.error(e.getMessage());
        }
        translates.add(trans);

        // 有道翻译
        LanguageEntity languageEntity = youdaoTrans.youdaoTranslate(entryInfoEntity.getEntry(), ConstantInterface.AUTO, language);
        if (!Objects.isNull(languageEntity)) {
            translates.add(languageEntity.getValue());
        }
        if (translates.isEmpty()) {
            return null;
        }
        // 计算出现次数最多的翻译
        Map<String, Integer> countMap = new HashMap<>();
        for (String translate : translates) {
            String str = translate.toLowerCase();
            countMap.put(str, countMap.getOrDefault(str, 0) + 1);
        }
        // 找出出现次数最多的元素
        Optional<Map.Entry<String, Integer>> max = countMap.entrySet().stream().max(Map.Entry.comparingByValue());

        //获取key
        String maxTranslate = max.get().getKey();
        return maxTranslate;
    }

    private String getSYKTranslate(String entry, String translateType,String department) {
        List<TranslateEntity> versionSuggestTrans = translateMapper.getVersionSuggestTrans(entry, translateType,department);

        String translate = "";
        if (!CollectionUtils.isEmpty(versionSuggestTrans)) {

             TranslateEntity translateEntity = versionSuggestTrans
                    .stream()
                    .max(Comparator.comparing(TranslateEntity::getLastUseTime)).orElse(null);
            translate = translateEntity.getTranslate();
        }
        return translate;
    }

    @Override
    public void getTemplateFile(HttpServletResponse response, String fileType,String translateType) {
        try {
            String fileUrl = "";
            Workbook workbook;
            String fileName = "";
            if (StringUtils.isNotBlank(translateType)){
                switch (translateType) {
                    case ConstantInterface.CHINESE:
                        if (fileType.equals("common")) {
                            //src/main/resources/config/en_common.xlsx
                            fileUrl = config_File_Common_ZH_Url;
                            fileName = "通用词条中文翻译模板_common.xlsx";
                        } else if (fileType.equals("zz")) {
                            fileUrl = config_File_ZZ_ZH_Url;
                            fileName = "装置词条中文翻译模板_zz.xlsx";
                        } else if (fileType.equals("jk")) {
                            fileUrl = config_File_JK_ZH_Url;
                            fileName = "监控词条中文翻译模板_zz.xlsx";
                        }
                        break;
                    case ConstantInterface.ENGLISH:
                        if (fileType.equals("common")) {
                            //src/main/resources/config/en_common.xlsx
                            fileUrl = config_File_Common_EN_Url;
                            fileName = "通用词条英文翻译模板_common.xlsx";
                        } else if (fileType.equals("zz")) {
                            fileUrl = config_File_ZZ_EN_Url;
                            fileName = "装置词条英文翻译模板_zz.xlsx";
                        } else if (fileType.equals("jk")) {
                            fileUrl = config_File_JK_EN_Url;
                            fileName = "监控词条英文翻译模板_zz.xlsx";
                        }
                        break;
                    case ConstantInterface.RUSSIAN:
                        if (fileType.equals("common")) {
                            //src/main/resources/config/en_common.xlsx
                            fileUrl = config_File_Common_RU_Url;
                            fileName = "通用词条俄文翻译模板_common.xlsx";
                        } else if (fileType.equals("zz")) {
                            fileUrl = config_File_ZZ_RU_Url;
                            fileName = "装置词条俄文翻译模板_zz.xlsx";
                        } else if (fileType.equals("jk")) {
                            fileUrl = config_File_JK_RU_Url;
                            fileName = "监控词条俄文翻译模板_zz.xlsx";
                        }
                        break;
                    case ConstantInterface.FRENCH:
                        if (fileType.equals("common")) {
                            //src/main/resources/config/en_common.xlsx
                            fileUrl = config_File_Common_FR_Url;
                            fileName = "通用词条法文翻译模板_common.xlsx";
                        } else if (fileType.equals("zz")) {
                            fileUrl = config_File_ZZ_FR_Url;
                            fileName = "装置词条法文翻译模板_zz.xlsx";
                        } else if (fileType.equals("jk")) {
                            fileUrl = config_File_JK_FR_Url;
                            fileName = "监控词条法文翻译模板_zz.xlsx";
                        }
                        break;
                    case ConstantInterface.SPANISH:
                        if (fileType.equals("common")) {
                            //src/main/resources/config/en_common.xlsx
                            fileUrl = config_File_Common_ES_Url;
                            fileName = "通用词条西文翻译模板_common.xlsx";
                        } else if (fileType.equals("zz")) {
                            fileUrl = config_File_ZZ_ES_Url;
                            fileName = "装置词条西文翻译模板_zz.xlsx";
                        } else if (fileType.equals("jk")) {
                            fileUrl = config_File_JK_ES_Url;
                            fileName = "监控词条西文翻译模板_zz.xlsx";
                        }
                }
            }

          /*  if (fileType.equals("common")) {
                //src/main/resources/config/en_common.xlsx
                fileUrl = configFileCommonUrl;

                fileName = "通用词条翻译模板_common.xlsx";
            } else if (fileType.equals("zz")) {
                fileUrl = configFileZZUrl;
                fileName = "装置词条翻译模板_zz.xlsx";
            } else if (fileType.equals("jk")) {
                fileUrl = configFilejkUrl;
                fileName = "监控词条翻译模板_zz.xlsx";
            }*/
            //ClassLoader classLoader = EntryTempServiceImpl.class.getClassLoader();
           // File configFile = new File(classLoader.getResource(fileUrl).getFile());
           /* if (!configFile.exists()) {
                log.error(" 模板文件不存在 ！");
            }*/

            FileInputStream fileInputStream = new FileInputStream(fileUrl);
            if (fileUrl.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fileInputStream);
            } else {
                workbook = new HSSFWorkbook(fileInputStream);
            }
            //src\main\java\com\shr\translationtoolservice\service\impl\EntryTempServiceImpl.java
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
    public List<EntryInfoEntity> getEntryInfoList(String taskID, String entryState, List<String> transStates, String entry) {
        List<EntryInfoEntity> newEntry = new ArrayList<>();
        // List<EntryInfoEntity> entryInfoEntities;

        //没给翻译状态直接查词条状态
        List<EntryInfoEntity> entryInfoEntities = getEntryInfo(taskID, entryState, transStates, entry);
        TaskInfoEntity taskEntityByTaskID = taskInfoMapper.getTaskEntityByTaskID(taskID);
        List<EntryInfoEntity> entryInfoEntityList = new ArrayList<>();
        // if ( StringUtils.isBlank(entryState) || Integer.parseInt(entryState) > 1 ) {
        //     entryInfoEntityList = entryProcessUtils.findChildEntry(entryInfoEntities, taskEntityByTaskID.getTranslateType());
        // }else {
        entryInfoEntityList = entryProcessUtils.buildRepeEntry(entryInfoEntities, taskEntityByTaskID.getTranslateType());

        // }
        //entryInfoEntityList 的元素中没有翻译的排序在前面
        sortEmptyTrans(entryInfoEntityList,taskEntityByTaskID.getTranslateType());


        return entryInfoEntityList;
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

    //没给翻译状态直接查词条状态
    private List<EntryInfoEntity> getEntryInfo(String taskID, String entryState, List<String> transStates, String entry) {
        List<EntryInfoEntity> entryInfoEntities;
        TLanguage tLanguage = languageMapper.getLanguageByTask(taskID);

        if (CollectionUtils.isEmpty(transStates)) {
            //  entryInfoEntities = entryInfoMapper.selectList(queryWrapper);
            entryInfoEntities = entryInfoMapper.getEntryByTaskIDAndEntry(taskID, entryState, entry);
        } else {
            String transState = "";
            String s = "";
            String entrySql = "";
            String entryStateSql = "";
            String tbCode = tLanguage.getTbCode();
            // 翻译状态处理 结果 ： 1,2,3
            for (String transState1 : transStates) {
                if (StringUtils.isBlank(transState)) {
                    transState = " '" + transState1 + "' ";
                } else {
                    transState += ",'" + transState1 + "' ";
                }
                if (Integer.parseInt(transState1) == 0) {
                    s = " or t2." + tLanguage.getTbCode() + "_trans_id  is null or t2." + tbCode +"_trans_id = ''";
                }
            }
            if (StringUtils.isNotBlank(entry)) {
                entrySql = " and t2.entry like '%" + entry + "%'";
            }
            if (StringUtils.isNotBlank(entryState)) {
                entryStateSql = " and t2.entry_state = " + entryState;
            }


            String sql = "select t2.* ,t3.audit_suggest as " + tLanguage.getEnglish() +
                    "AuditSuggest  , t3.translate_state as " + tLanguage.getEnglish() +
                    "TranslateState ,t3.translate as " + tLanguage.getEnglish() +
                    " from  t_product_relation t1  join  t_entry_info t2 on t1.entry_id = t2.id left JOIN t_translate t3 ON  t2." +
                    tLanguage.getTbCode() + "_trans_id = t3.id   where ( t3.translate_state in ( " + transState + ")  " + s + ") and  t1.task_id = '" + taskID + "' " + entryStateSql + entrySql
                    + " and t2.is_delete = 0";

            entryInfoEntities = entryInfoMapper.getTransStateEntry(sql);
        }
        return entryInfoEntities;
    }

    @Override
    public List<EntryInfoEntity> updateEntryList(List<EntryInfoEntity> entryInfoEntities, String taskID, HttpServletRequest request) {
        int update = 0;
        String token = request.getHeader("token");
        String userName = JWTTokenUtils.getUserName(token);
        String department = JWTTokenUtils.getDepartment(token);
        String translateType = taskInfoMapper.selectById(taskID).getTranslateType();
        // clearParentID(entryInfoEntities, taskID);
        return processEntryInfo(entryInfoEntities, translateType, department,update);
   /*     for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            List<EntryInfoEntity> childrenInfoEntry = entryInfoEntity.getChildren();
            updateEntryInfoTranslate(translateType, entryInfoEntity, department);

            QueryWrapper<EntryInfoEntity> queryWrapper = new QueryWrapper();
            queryWrapper.eq("parent_id", entryInfoEntity.getId());
            queryWrapper.eq("is_delete", 0);
            List<EntryInfoEntity> childList = entryInfoMapper.selectList(queryWrapper);


            if (!CollectionUtils.isEmpty(childrenInfoEntry)) {
                update += syncChildrenTrans(entryInfoEntity, childrenInfoEntry,translateType);
            }else if (!CollectionUtils.isEmpty(childList)){
                update += syncChildrenTrans(entryInfoEntity, childList,translateType);
            }


            update += entryInfoMapper.updateById(entryInfoEntity);
        }*/
       /* if (update < entryInfoEntities.size()) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }*/
        // return ConstantInterface.OK_STR;
    }

    // private void clearParentID(List<EntryInfoEntity> entryInfoEntities,String taskID){
    //     // 获取任务对应的类型，然后查看翻译状态，如果
    //     TaskInfoEntity taskEntityByTaskID = taskInfoMapper.getTaskEntityByTaskID(taskID);
    //     String translateType = taskEntityByTaskID.getTranslateType();
    //     Iterator<EntryInfoEntity> iterator = entryInfoEntities.iterator();

    //     while(iterator.hasNext()){
    //         EntryInfoEntity entryInfoEntity = iterator.next();
    //         if(translateType.equals(ConstantInterface.ENGLISH)){
    //             if(entryInfoEntity.getEnglishTranslateState().equals("3"))
    //                 entryInfoEntity.setParentID("");
    //         }else if(translateType.equals(ConstantInterface.SPANISH)){
    //             if(entryInfoEntity.getSpanishTranslateState().equals("3"))
    //                 entryInfoEntity.setParentID("");
    //         }else if(translateType.equals(ConstantInterface.RUSSIAN)){
    //             if(entryInfoEntity.getRussianTranslateState().equals("3"))
    //                 entryInfoEntity.setParentID("");
    //         }else if(translateType.equals(ConstantInterface.FRENCH)){
    //             if(entryInfoEntity.getFrenchTranslateState().equals("3"))
    //                 entryInfoEntity.setParentID("");
    //         }else{
    //             throw new RuntimeException("翻译类型不属于可选择范围");
    //         }
    //     }

    //     return;

    // }

    private List<EntryInfoEntity> processEntryInfo(List<EntryInfoEntity> entryInfoEntities, String translateType, String department,int update) {
        int numberOfThreads = 32; // Number of threads to use
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        int chunkSize = entryInfoEntities.size() / numberOfThreads;
        ConcurrentLinkedQueue<EntryInfoEntity> failedEntryInfoEntities = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < numberOfThreads; i++) {
            int start = i * chunkSize;
            int end = (i == numberOfThreads - 1) ? entryInfoEntities.size() : (i + 1) * chunkSize;
            List<EntryInfoEntity> sublist = entryInfoEntities.subList(start, end);

            new Thread(() -> {
                int update1 = 0;
                try {

                    // Process the sublist
                    for (EntryInfoEntity entryInfoEntity : sublist) {
                        // Your processing logic here
                        try {
                            if(entryInfoEntity.getEntry().length() > 512){
                                // 更新的词条的长度不能大于512
                                failedEntryInfoEntities.add(entryInfoEntity);
                                continue;
                            }
                            List<EntryInfoEntity> childrenInfoEntry = entryInfoEntity.getChildren();
                            updateEntryInfoTranslate(translateType, entryInfoEntity, department);
    
                            QueryWrapper<EntryInfoEntity> queryWrapper = new QueryWrapper();
                            queryWrapper.eq("parent_id", entryInfoEntity.getId());
                            queryWrapper.eq("is_delete", 0);
                            List<EntryInfoEntity> childList = entryInfoMapper.selectList(queryWrapper);
    
    
                            if (!CollectionUtils.isEmpty(childrenInfoEntry)) {
                               syncChildrenTrans(entryInfoEntity, childrenInfoEntry,translateType);
                            }else if (!CollectionUtils.isEmpty(childList)){
                                 syncChildrenTrans(entryInfoEntity, childList,translateType);
                            }
    
    
                           entryInfoMapper.updateById(entryInfoEntity);
                        } catch (Exception e) {
                            // TODO: handle exception
                            log.error(e.getMessage());
                            failedEntryInfoEntities.add(entryInfoEntity);
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
        return Arrays.asList(failedEntryInfoEntities.toArray(new EntryInfoEntity[failedEntryInfoEntities.size()]));
    }

    private int syncChildrenTrans(EntryInfoEntity entryInfoEntity, List<EntryInfoEntity> childrenInfoEntry,String translateType) {
       int update = 0;
        for (EntryInfoEntity entryInfoEntity1 : childrenInfoEntry) {
            entryInfoEntity1.setEntryState(entryInfoEntity.getEntryState());
            switch (translateType){
                case ConstantInterface.CHINESE:
                    if (StringUtils.isNotBlank(entryInfoEntity.getChinese())) {
                        entryInfoEntity1.setZhTransId(entryInfoEntity.getZhTransId());
                    }
                    break;
                case ConstantInterface.ENGLISH:
                    if (StringUtils.isNotBlank(entryInfoEntity.getEnglish())) {
                        entryInfoEntity1.setEnTransId(entryInfoEntity.getEnTransId());
                    }
                    break;
                case ConstantInterface.RUSSIAN:
                    if (StringUtils.isNotBlank(entryInfoEntity.getRussian())) {
                        entryInfoEntity1.setRuTransId(entryInfoEntity.getRuTransId());
                    }
                    break;
                case ConstantInterface.FRENCH:
                    if (StringUtils.isNotBlank(entryInfoEntity.getFrench())) {
                        entryInfoEntity1.setFraTransId(entryInfoEntity.getFraTransId());
                    }
                    break;
                case ConstantInterface.SPANISH:
                    if (StringUtils.isNotBlank(entryInfoEntity.getSpanish())) {
                        entryInfoEntity1.setSpaTransId(entryInfoEntity.getSpaTransId());
                    }
                    break;
            }
            update +=  entryInfoMapper.updateById(entryInfoEntity1);
        }
        return update;
    }


    //写入翻译
    private String updateEntryInfoTranslate(String translateType, EntryInfoEntity entryInfoEntity, String department) {
        String transId = "";
        String finalTransId ;
        //如果有翻译 就更新翻译
        switch (translateType) {
            case ConstantInterface.CHINESE:
                transId = updateTrans(entryInfoEntity.getChineseAuditSuggest(), entryInfoEntity.getChinese(), entryInfoEntity.getZhTransId(), ConstantInterface.CHINESE, entryInfoEntity.getChineseTranslateState(), entryInfoEntity, department);
                if (StringUtils.isBlank(transId)){
                    return "";
                }
                entryInfoEntity.setZhTransId(transId);


                finalTransId = transId;
                if (CollectionUtils.isEmpty(entryInfoEntity.getChildren())){
                    break;
                }
                entryInfoEntity.getChildren().stream().forEach(entryInfoEntity1 -> {
                    entryInfoEntity1.setZhTransId(finalTransId);
                });
                // writeI18Entry(entryInfoEntity, entryInfoEntity.getEnglish());
                break;
            case ConstantInterface.ENGLISH:
                transId = updateTrans(entryInfoEntity.getEnglishAuditSuggest(), entryInfoEntity.getEnglish(), entryInfoEntity.getEnTransId(), ConstantInterface.ENGLISH, entryInfoEntity.getEnglishTranslateState(), entryInfoEntity, department);
                if (StringUtils.isBlank(transId)){
                    return "";
                }
                entryInfoEntity.setEnTransId(transId);


                finalTransId = transId;
                if (CollectionUtils.isEmpty(entryInfoEntity.getChildren())){
                    break;
                }
                    entryInfoEntity.getChildren().stream().forEach(entryInfoEntity1 -> {
                    entryInfoEntity1.setEnTransId(finalTransId);
                });
               // writeI18Entry(entryInfoEntity, entryInfoEntity.getEnglish());
                break;
            case ConstantInterface.RUSSIAN:
                transId = updateTrans(entryInfoEntity.getRussianAuditSuggest(), entryInfoEntity.getRussian(), entryInfoEntity.getRuTransId(), ConstantInterface.RUSSIAN, entryInfoEntity.getRussianTranslateState(), entryInfoEntity, department);
                if (StringUtils.isBlank(transId)){
                    return "";
                }
                entryInfoEntity.setRuTransId(transId);
                finalTransId = transId;
                if (CollectionUtils.isEmpty(entryInfoEntity.getChildren())){
                    break;
                }
                entryInfoEntity.getChildren().stream().forEach(entryInfoEntity1 -> {
                    entryInfoEntity1.setRuTransId(finalTransId);
                });
                break;
            case ConstantInterface.FRENCH:
                transId = updateTrans(entryInfoEntity.getFrenchAuditSuggest(), entryInfoEntity.getFrench(), entryInfoEntity.getFraTransId(), ConstantInterface.FRENCH, entryInfoEntity.getFrenchTranslateState(), entryInfoEntity, department);
                if (StringUtils.isBlank(transId)){
                    return "";
                }
                entryInfoEntity.setFraTransId(transId);
                finalTransId = transId;
                if (CollectionUtils.isEmpty(entryInfoEntity.getChildren())){
                    break;
                }
                entryInfoEntity.getChildren().stream().forEach(entryInfoEntity1 -> {
                    entryInfoEntity1.setFraTransId(finalTransId);
                });
                break;
            case ConstantInterface.SPANISH:
                transId = updateTrans(entryInfoEntity.getSpanishAuditSuggest(), entryInfoEntity.getSpanish(), entryInfoEntity.getSpaTransId(), ConstantInterface.SPANISH, entryInfoEntity.getSpanishTranslateState(), entryInfoEntity, department);
                if (StringUtils.isBlank(transId)){
                    return "";
                }
                entryInfoEntity.setSpaTransId(transId);
                finalTransId = transId;
                if (CollectionUtils.isEmpty(entryInfoEntity.getChildren())){
                    break;
                }
                entryInfoEntity.getChildren().stream().forEach(entryInfoEntity1 -> {
                    entryInfoEntity1.setSpaTransId(finalTransId);
                });
                break;
        }
        return transId;
    }

    private void writeI18Entry(EntryInfoEntity entryInfoEntity, String translate) {
        if (StringUtils.isBlank(entryInfoEntity.getWriteType())) {
            return;
        }
        switch (entryInfoEntity.getWriteType()) {
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
        //翻译id 不是空 trans 是空 删除
        //翻译id 不是空  trans 不是空  update
        //翻译id 是空 trans 不是空 insert
        // 翻译id trans 都是空 跳过
        if (StringUtils.isNotBlank(transId) ) {
            TranslateEntity translateEntity = translateMapper.selectById(transId);
            if (Objects.isNull(translateEntity)) {
                log.error("transId is null , transId : " + transId + " , entry : " + entryInfoEntity.getEntry());
                return "";
            }
            if (StringUtils.isBlank(trans) ) {
               // int delete = translateMapper.deleteById(transId);
                entryInfoEntity.setEnTransId("");
             //  log.info("删除 （" + delete + " ）条 翻译 到翻译表中, transID ( " + transId + ") 更新内容 ： trans ( " + trans + ") ");
                return newTransID;
            } else {
                //如果是翻译审核 校验重复翻译 ID挂载重复的
                if (3 == Integer.parseInt(transState)) {
                    newTransID = updateAuditTrans(entryInfoEntity, transId, type, newTransID, trans, auditSuggest, department);
                    return newTransID;
                } else {
                    newTransID = updateTransEntity(entryInfoEntity,transState, transId, auditSuggest, trans,department);
                    return newTransID;
                }

            }
        } else {
            if (StringUtils.isBlank(trans)) {
                return newTransID;
            } else {
                String entry = entryInfoEntity.getEntry();
                newTransID = insertTransEntity(trans, transState, department, type, entry);

            }
        }
        return newTransID;
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
        log.info("**** 待翻译词条 entry : " + entry + "更新内容 ： trans ( " + trans + ") **** ");
        TranslateEntity translateEntity = new TranslateEntity();
        translateEntity.setTranslate(trans);
        translateEntity.setTranslateState(transState);
        translateEntity.setId(newTransID);
        translateEntity.setPublicState(0);
        translateEntity.setVisualRange(department);
        translateEntity.setDeleteState(0);
        translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
        translateEntity.setType(type);
        translateEntity.setEntry(entry);
        // int insert = translateMapper.insert(translateEntity);
        int insert = translateMapper.insertTranslate(translateEntity);
        log.info("新增 （" + insert + " ）条 翻译 到翻译表中, transID ( " + newTransID + ") 更新内容 ： trans ( " + trans + "),  transState ( " + transState + ")  ");
        return newTransID;
    }

    private String updateTransEntity(EntryInfoEntity entryInfoEntity,String transState, String transId, String auditSuggest, String trans,String department) {
        TranslateEntity translateEntity = new TranslateEntity();
        translateEntity.setTranslate(trans);
        translateEntity.setEntry(entryInfoEntity.getEntry());
        translateEntity.setTranslateState(transState);
        translateEntity.setId(transId);
        translateEntity.setVisualRange(department);
        translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
        translateEntity.setAuditSuggest(auditSuggest);
        int update = translateMapper.updateById(translateEntity);
        log.info("更新 （" + update + " ）条 翻译 到翻译表中, transID ( " + transId + ") 更新内容 ： trans ( " + trans + "),  transState ( " + transState + ")  ");
        return transId;
    }

    private String updateAuditTrans(EntryInfoEntity entryInfoEntity, String transId, String type, String newTransID, String trans, String auditSuggest, String department) {

        QueryWrapper<TranslateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("entry", entryInfoEntity.getEntry());
        queryWrapper.eq("type", type);
        queryWrapper.eq("translate", trans);
        queryWrapper.eq("delete_state", 0);
        queryWrapper.eq("visual_range", department);
        queryWrapper.eq("translate_state", 3);
        queryWrapper.notIn("id",transId);
        // queryWrapper.eq("audit_suggest", auditSuggest);
        List<TranslateEntity> translateEntityList = translateMapper.selectList(queryWrapper);
        if (translateEntityList.size() > 0) {
            log.error(" ===== 词条更新翻译查重多于1条，size: " + translateEntityList.size() +" ，transid : " + transId + "  ,entry : " + entryInfoEntity.getEntry() + " , trans : " + trans + " ===== ");
            //更新使用时间
            //取最后的使用时间
             TranslateEntity translateEntity = translateEntityList.stream().max(Comparator.comparing(TranslateEntity::getLastUseTime)).get();
             translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
             newTransID = translateEntity.getId();
            translateMapper.updateById(translateEntity);
            int delete = translateMapper.deleteById(transId);

            log.info("删除 （" + delete + " ）条 翻译 到翻译表中, transID ( " + transId + ") 更新内容 ： trans ( " + trans + ") ");
        }  else if (translateEntityList.size() < 1) {

            TranslateEntity translateEntity = new TranslateEntity();
            translateEntity.setId(transId);
            translateEntity.setTranslate(trans);
            translateEntity.setTranslateState("3");
            translateEntity.setPublicState(0);
            translateEntity.setVisualRange(department);
            translateEntity.setLastUseTime(localTimeUtils.getBeijingTime());
            translateEntity.setDeleteState(0);
            translateEntity.setType(type);
            translateEntity.setEntry(entryInfoEntity.getEntry());
            translateEntity.setAuditSuggest(auditSuggest);
            int update = translateMapper.updateById(translateEntity);
            log.info("更新 （" + update + " ）条 翻译 到翻译表中, transID ( " + transId + ") 更新内容 ： trans ( " + trans + ") ");
            newTransID = transId;
        }
        return newTransID;
    }


}




