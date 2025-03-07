package com.shr.translationtoolservice.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @ClassName DiUtils
 * @Description 辞典工具
 * @USER: Cola
 * @Date 2024/3/20 0020 14:49
 **/
@Component
@Slf4j
public class DiUtils {
    @Autowired
    private HTTPUtils httpUtils;

    @Value("${I18server.url}")
    private String I18URL;

    @Autowired
    private TLanguageMapper languageMapper;

    @Autowired
    private CommonUtils commonUtils;

    //辞典更新 不需要tag 和comment 的更新和查询传入前先清空字段内容
    public String writeDiEntry(List<EntryInfoEntity> entryInfoEntities, String fileName, String translateType,boolean tag,boolean common,String i18nUrl) {
        //获取字典所有词条，将对应词条的对应翻译插入其中 ，再全量写入
        String s = "";
        String s2 = "";
        JSONArray jsonArray;
        if (StringUtils.isBlank(i18nUrl)) {
            i18nUrl = I18URL;
        }
        try {
            Map<String, String> headerParameters = new HashMap<>();
            headerParameters.put("name", fileName);
            s = httpUtils.get(i18nUrl + ConstantInterface.GET_INIT_DICTIONARY ,headerParameters);

            if (s.equals("501") || StringUtils.isBlank(s) || s.equals("null")){
                ErrorCodeList.setErrorCodeList(ErrorCodeList.GET_DIC_ERROR +fileName);
                return ErrorCodeList.GET_DIC_ERROR + fileName;
            }
            jsonArray = JSONArray.parseArray(s);
            List<DictionaryVo> dictionaryVos = new ArrayList<>();
            constructDIVO(jsonArray, dictionaryVos);
            String langCode = languageMapper.selectLaguageByName(translateType).get(0).getCode();
            int add = 0;
            int update = 0 ;
            int sum =0;
            for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
                //枚举tag 不需要回写！！！ tr的tag 需要回写
                if (!tag) {
                    entryInfoEntity.setTag("");
                }
                if (!common) {
                    entryInfoEntity.setComment("");
                }
                update++;
                boolean isExist = updateTransToDiVo(tag,common,langCode, entryInfoEntity, translateType, dictionaryVos);
                //如果不存在 则新增词条
                if (!isExist) {
                    add ++;
                    addEntryToDIVo(langCode, entryInfoEntity, translateType, dictionaryVos);
                }
            }
            String dictionaryVosStr = JSONObject.toJSONString(dictionaryVos);
            headerParameters.put("dictionaryInfo", dictionaryVosStr);
            headerParameters.put("dictionary", fileName);
            String req =  JSONObject.toJSONString(headerParameters);
             sum = add+update;
            //10.16.193.63:18099/dictionary/user
            s2 = httpUtils.post(i18nUrl + ConstantInterface.UPDTAE_DICTIONARY , req);
            if (StringUtils.isBlank(s2) ){
                ErrorCodeList.setErrorCodeList(ErrorCodeList.UPDATE_DIC_ERROR +fileName);
                return ErrorCodeList.UPDATE_DIC_ERROR + fileName;
            }
            log.info(" ==== " + i18nUrl +" 辞典 " + fileName + " 新增词条 ： " + add + " **** 更新词条 ：" + update + " ****  sum is :" + sum + " ==== ");

        } catch (Exception e) {
            log.error(" 请求失败 URL ： " + i18nUrl + ConstantInterface.UPDTAE_DICTIONARY + ConstantInterface.SPRIT);
            e.printStackTrace();
            return ErrorCodeList.I18N_SERVER_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    private void addEntryToDIVo(String langCode, EntryInfoEntity entryInfoEntity, String translateType, List<DictionaryVo> dictionaryVos) {
        DictionaryVo dictionaryVo = new DictionaryVo();
        dictionaryVo.setSource(entryInfoEntity.getEntry());
        dictionaryVo.setComments(entryInfoEntity.getComment());

        dictionaryVo.setTag(entryInfoEntity.getTag());

        // dictionaryVo.setComments(entryInfoEntity.getEntrySource().split("_")[2]);
        // dictionaryVo.setTag(entryInfoEntity.getEntryLabel());
        Map<String, String> transMap = new HashMap<>();
        switch (translateType) {
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

    private boolean updateTransToDiVo(boolean tag,boolean common,String langCode, EntryInfoEntity entryInfoEntity, String translateType, List<DictionaryVo> dictionaryVos) {
        boolean isExist = false;

        for (DictionaryVo dictionaryVo : dictionaryVos) {
            if (dictionaryVo.getSource().equals(entryInfoEntity.getEntry())) {
                //是否需要tag 查询
                if (tag){
                    if ((StringUtils.isNotBlank(entryInfoEntity.getTag()) && !entryInfoEntity.getTag().equals(dictionaryVo.getTag())) ||
                            (StringUtils.isBlank(entryInfoEntity.getTag()) && StringUtils.isNotBlank(dictionaryVo.getTag()))) {
                        continue;
                    }
                }else {
                    //不需要tag 的时候 如果辞典里存在tag 但是不存在空tag的词条，则是不匹配的
                    if (StringUtils.isBlank(entryInfoEntity.getTag() )  && StringUtils.isNotBlank(dictionaryVo.getTag())) {
                        continue;
                    }
                }


                //是否需要common 查询
                if (common){
                    if (StringUtils.isNotBlank(entryInfoEntity.getComment() ) && !entryInfoEntity.getTag().equals(dictionaryVo.getComments())) {
                        continue;
                    }else if (StringUtils.isBlank(entryInfoEntity.getComment()) && StringUtils.isBlank(dictionaryVo.getComments())){
                        continue;
                    }
                }else {
                    //不需要tag 的时候 如果辞典里存在tag 但是不存在空tag的词条，则是不匹配的
                    if (StringUtils.isBlank(entryInfoEntity.getComment() )  && StringUtils.isNotBlank(dictionaryVo.getComments())) {
                        continue;
                    }
                }
                isExist = true;

                Map<String, String> transMap = dictionaryVo.getTranslation();
                if (CollectionUtils.isEmpty(transMap)) {
                    transMap = new HashMap<>();
                }
                switch (translateType) {
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
        return isExist;
    }

    private void constructDIVO(JSONArray jsonArray, List<DictionaryVo> dictionaryVos) {
        for (int i = 0; i < jsonArray.size(); i++) {
            DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
            dictionaryVos.add(dictionaryVo);
        }
    }


    public List<EntryInfoEntity> getDiEntry(String i18nUrl, Set<String> difileSet, String taskID, String userName, String productId, String versionID, String translateType) throws Exception {

        ArrayList<EntryInfoEntity> list = new ArrayList<>();


                for (String dic : difileSet) {
                JSONArray jsonArray = new JSONArray();
                Date date = new Date(System.currentTimeMillis());
                String dicName = "";
                Map<String, String> headerParameters = new HashMap<>();
                if (dic.contains("平台")){
                    dicName = dic.replaceAll("平台","pt");
                }else if (dic.contains("监控")){
                    dicName = dic.replaceAll("监控","jk");
                }else {
                    dicName = dic;
                }
                headerParameters.put("dictionary", dicName);
                String dicName1 =dicName;
                log.info("getDiEntry: " + i18nUrl + ConstantInterface.IMPORT_DICTIONARY + " fileName : " + dicName);
                if (dicName.contains("/")){
                    dicName1 = dicName.split("/")[1];
                }
                String s = httpUtils.get(i18nUrl + ConstantInterface.IMPORT_DICTIONARY, headerParameters);
                jsonArray = JSONArray.parseArray(s);
                for (int i = 0; i < jsonArray.size(); i++) {
                    DictionaryVo dictionaryVo = JSONObject.parseObject(jsonArray.getString(i), DictionaryVo.class);
                    if (Objects.isNull(dictionaryVo.getTranslation())) {
                        continue;
                    }
                    EntryInfoEntity entryInfoEntity = new EntryInfoEntity();
                    entryInfoEntity.setId(commonUtils.getUUID());
                    entryInfoEntity.setEntry(dictionaryVo.getSource());
                    entryInfoEntity.setVersionID(versionID);
                    entryInfoEntity.setTaskId(taskID);
                    entryInfoEntity.setTag(dictionaryVo.getTag());
                    entryInfoEntity.setEntryLabel(dictionaryVo.getComments());
                    entryInfoEntity.setImportType(ConstantInterface.DI);
                    entryInfoEntity.setWriteType(ConstantInterface.DI);
                    entryInfoEntity.setEntryState(1);
                    entryInfoEntity.setEntrySource(dicName);
                    entryInfoEntity.setDiFileName("tr/" + dicName1);
                    entryInfoEntity.setProductID(productId);
                    entryInfoEntity.setIsDelete(0);
                    entryInfoEntity.setUpdateTime(date);
                    entryInfoEntity.setUpdate(userName);
                    Map<String, String> map = dictionaryVo.getTranslation();
                    if (StringUtils.isBlank(map.get(translateType))) {
                        switch (translateType) {
                            case ConstantInterface.ENGLISH:
                                entryInfoEntity.setEnglish(map.get(translateType));
                                break;
                            case ConstantInterface.RUSSIAN:
                                entryInfoEntity.setRussian(map.get(translateType));
                                break;
                            case ConstantInterface.FRENCH:
                                entryInfoEntity.setFrench(map.get(translateType));
                                break;
                            case ConstantInterface.SPANISH:
                                entryInfoEntity.setSpanish(map.get(translateType));
                                break;
                        }
                    }
                    list.add(entryInfoEntity);
                }
            }


        return  list;
    }
}
