package com.shr.translationtoolservice.util;


import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName Translate
 * @Description 翻译工具
 * @USER: Cola
 * @Date 2023/7/5 0005 16:13
 **/

@Component
@Slf4j
@PropertySource("classpath:application.yml")
public class TranslateUtils {


    // 对接的api为百度翻译
    @Value("${translate.baidu.url}")
    private String BAIDU_TRANS_API_HOST;

    @Value("${translate.baidu.appid}")
    private String appid;

    @Value("${translate.baidu.securityKey}")
    private String securityKey;

    @Autowired
    private HTTPUtils httpUtils;

    public TranslateUtils instance;

    @Autowired
    private TLanguageMapper languageMapper;

    @Autowired
    private DeepLTranslateUtils deepLTranslateUtils;

    @Autowired
    private YoudaoTrans youdaoTrans;

    @Autowired
    private BaiduTransUtils baiduTransUtils;


    public TranslateUtils getInstance() {

        if (instance == null) {
            instance = new TranslateUtils();
        }
        return instance;
    }

    private ConstantInterface constantInterface = ConstantInterface.getInstance();

    // 发送查询
    //query  要查询的词    from 默认auto   to => 语种
    public LanguageEntity getTranslateResult(String query, String from, TLanguage tLanguages) {

        String to = tLanguages.getBdCode();
        log.info(" **** to : " + to + " **** ");
        LanguageEntity languageEntity = new LanguageEntity();

        ConstantInterface.getInstance();
        Map<String, String> languageMap = ConstantInterface.LANGUAGE_MAP;

        languageEntity.setLanguage(languageMap.get(to).toLowerCase());
        languageEntity.setState(true);

        Map<String, String> params = new HashMap();

        String result = "";
        try {
            params.put("q", query);
            params.put("from", from);
            params.put("to", to);
            params.put("appid", appid);
            // 随机数
            String salt = String.valueOf(System.currentTimeMillis());
            params.put("salt", salt);
            // 签名
            String src = appid + query + salt + securityKey; // 加密前的原文
            params.put("sign", SecureUtil.md5(src));
            log.info(" **** params : " + params.toString() + " **** ");

            String transText = "";

            transText = httpUtils.get(BAIDU_TRANS_API_HOST, params);
            Thread.sleep(1000);

            // String transText = HttpUtil.get(TRANS_API_HOST, params);

            JSONObject jsonObject = JSONObject.parseObject(transText);
            String trans_result = jsonObject.getString("trans_result");
            if (StringUtils.isBlank(trans_result)) {
                languageEntity.setValue("");
                return languageEntity;
            }

            String str = JSONObject.parseArray(trans_result).getString(0);
            result = JSONObject.parseObject(str).getString("dst");
            log.info(" **** trans : " + result + " **** ");
        } catch (Exception e) {
            e.printStackTrace();
        }
        languageEntity.setValue(result);
        return languageEntity;

    }


    public boolean containsChinese(String text) {
        Pattern pattern = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    public boolean containsEnglish(String text) {
        Pattern pattern = Pattern.compile("[a-zA-Z]");
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }


    //百度翻译  type 是当前语言
    public Translate baiduTranslate(String entry, String type, List<TLanguage> tLanguages) {
        Translate entryEntity = new Translate();
        entryEntity.setSource(ConstantInterface.translateMachine().get(ConstantInterface.BD));
        entryEntity.setEntry(entry);
        //ArrayList<TranslateEntity> list = new ArrayList<>();
        ArrayList<LanguageEntity> languageEntities = new ArrayList<>();


            for (TLanguage tLanguage : tLanguages) {
             /*   if (tLanguage.getName().equals(type)) {
                    continue;
                }*/
                LanguageEntity languageEntity = new LanguageEntity();

                ConstantInterface.getInstance();
                Map<String, String> languageMap = ConstantInterface.LANGUAGE_MAP;

                languageEntity.setLanguage(languageMap.get(tLanguage.getBdCode()).toLowerCase());
                languageEntity.setState(true);
                String trans = "";
                try {
                //默认主语言都是中文
                 trans = baiduTransUtils.translate(entry, "zh", tLanguage.getBdCode());
                 Thread.sleep(1000);
                } catch (Exception e) {
                    log.error(" ==== baidu翻译失败 ！ ====");
                    //languageEntities.add(languageEntity);
                    e.printStackTrace();
                }
                languageEntity.setValue(trans);
                languageEntity.setSource("百度翻译");


                languageEntities.add(languageEntity);

            }
            entryEntity.setLanguageEntities(languageEntities);



        return entryEntity;

    }

    public String getTransStr(String type) {
        List<TLanguage> languageList = languageMapper.selectLaguageByName(type);
        if (!CollectionUtils.isEmpty(languageList)) {
            return languageList.get(0).getCode();
        }
        return "";
    }

    public Translate youdaoTranslate(String name, String type, List<TLanguage> tLanguages) {
        // YoudaoTrans.readJsonFromUrl(name,ConstantInterface.ENGLISH);]
        Translate entryEntity = new Translate();
        //QueryWrapper queryWrapper = new QueryWrapper();
        //List<TLanguage> tLanguages = tLanguageMapper.selectList(new QueryWrapper<>());

        entryEntity.setSource(ConstantInterface.translateMachine().get(ConstantInterface.YD));
        entryEntity.setEntry(name);
        ArrayList<LanguageEntity> languageEntities = new ArrayList<>();


        for (TLanguage tLanguage : tLanguages) {
          /*  if (tLanguage.getName().equals(type)) {
                continue;
            }*/
            languageEntities.add(youdaoTrans.youdaoTranslate(name, "zh-CHS", tLanguage));
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }


        entryEntity.setLanguageEntities(languageEntities);

        return entryEntity;

    }


    public Translate modelTranslate(String name, String type, List<TLanguage> tLanguages) {
        // YoudaoTrans.readJsonFromUrl(name,ConstantInterface.ENGLISH);]
        Translate entryEntity = new Translate();
        //QueryWrapper queryWrapper = new QueryWrapper();
        //List<TLanguage> tLanguages = tLanguageMapper.selectList(new QueryWrapper<>());

        entryEntity.setSource(ConstantInterface.translateMachine().get(ConstantInterface.MD));
        entryEntity.setEntry(name);
        ArrayList<LanguageEntity> languageEntities = new ArrayList<>();


        for (TLanguage tLanguage : tLanguages) {
          /*  if (tLanguage.getName().equals(type)) {
                continue;
            }*/
            languageEntities.add(youdaoTrans.youdaoTranslate(name, "zh-CHS", tLanguage));
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }


        entryEntity.setLanguageEntities(languageEntities);

        return entryEntity;

    }

    public Translate localTranslate(String name, String type, List<TranslateEntity> translates) {
        Translate entryEntity = new Translate();
        entryEntity.setSource("本地翻译");
        entryEntity.setEntry(name);
        ArrayList<LanguageEntity> languageEntities = new ArrayList<>();
        for (TranslateEntity translate : translates) {
            LanguageEntity languageEntity = new LanguageEntity();
            languageEntity.setLanguage(translate.getType());
            languageEntity.setValue(translate.getTranslate());
            languageEntity.setId(translate.getId());
            languageEntity.setCreateTime(translate.getLastUseTime());
            languageEntity.setSource("本地翻译-" + translate.getVisualRange());
            languageEntities.add(languageEntity);
        }
        entryEntity.setLanguageEntities(languageEntities);
        return entryEntity;
    }

    /**
     * deepl翻译
     *
     * @param text       待翻译文本
     * @param tLanguages 目标语言code
     * @return
     */
    public Translate deeplTranslate(String text, List<TLanguage> tLanguages) {
        Translate entryEntity = new Translate();
        entryEntity.setSource(ConstantInterface.translateMachine().get(ConstantInterface.DEEPL));
        entryEntity.setEntry(text);
        ArrayList<LanguageEntity> languageEntities = new ArrayList<>();
        for (TLanguage tLanguage : tLanguages) {
            String code = tLanguage.getDeeplCode();
            // 翻译
            String translate = deepLTranslateUtils.translate(text, null, code);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }
            // 设置返回值
            LanguageEntity languageEntity = new LanguageEntity();
            languageEntity.setValue(translate);
            languageEntity.setLanguage(tLanguage.getCode());
            languageEntity.setSource("deepl翻译");
            languageEntities.add(languageEntity);
        }
        entryEntity.setLanguageEntities(languageEntities);

        return entryEntity;
    }
}
