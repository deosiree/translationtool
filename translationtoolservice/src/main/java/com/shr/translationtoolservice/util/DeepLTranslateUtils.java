package com.shr.translationtoolservice.util;

import com.deepl.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @title DeepLTranslateUtils
 * @create 2024/4/1 15:21
 * @description <TODO description class purpose>
 **/
@Service
@Slf4j
public class DeepLTranslateUtils {

    @Value("${translate.deepl.url}")
    private String deeplUrl;

    @Value("${translate.deepl.key}")
    private String key;

    /**
     * DeepL翻译接口
     * @param text 待翻译文本
     * @param sourceLang 源语言 可为null
     * @param targetLang 目标语言
     * @return 翻译结果
     */
    public String translate(String text, String sourceLang, String targetLang){
        // 创建链接
        TranslatorOptions translatorOptions = new TranslatorOptions();
        translatorOptions.setServerUrl(deeplUrl);
        Translator translator = new Translator(key,translatorOptions);
        try {
            // 翻译
            TextResult textResult = translator.translateText(text, sourceLang, targetLang);
            log.info("****deepl翻译****text:{};***to:{};*****翻译结果：{}",text,targetLang,textResult.getText());
            return textResult.getText();
        } catch (DeepLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
/*    Translator translator;

    public DeepLTranslateUtils() throws Exception {
        String authKey = "4e97acfd-4c5f-4382-9197-3bcf58b3fdee:fx";  // Replace with your key
        translator = new Translator(authKey);
        TextResult result =
                translator.translateText("Hello, world!", null, "fr");
        System.out.println(result.getText()); // "Bonjour, le monde !"
    }*/
}
