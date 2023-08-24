package com.shr.translationtoolservice.util;


import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.LanguageEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName Translate
 * @Description 翻译工具
 * @USER: Cola
 * @Date 2023/7/5 0005 16:13
 **/

@Component
@Slf4j
@PropertySource("classpath:application.yml")
public class Translate {


    // 对接的api为百度翻译
    private static final String TRANS_API_HOST = "http://api.fanyi.baidu.com/api/trans/vip/translate";

    @Value("${baidu.translate.appid}")
    private String appid = ConstantInterface.BAIDU_TRANSLATE_APPID;

    @Value("${baidu.translate.securityKey}")
    private String securityKey = ConstantInterface.BAIDU_TRANSLATE_KEY;
    @Autowired
    HTTPUtils httpUtils;

    private static ConstantInterface constantInterface = ConstantInterface.getInstance();

    // 发送查询
    //query  要查询的词    from 默认auto   to => 语种
    public LanguageEntity getTranslateResult(String query, String from, String to) {
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

            transText = httpUtils.get(TRANS_API_HOST, params);


            // String transText = HttpUtil.get(TRANS_API_HOST, params);

            JSONObject jsonObject = JSONObject.parseObject(transText);
            String trans_result = jsonObject.getString("trans_result");
            if (StringUtils.isBlank(trans_result)) {
                languageEntity.setValue("");
                return languageEntity ;
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


}
