package com.shr.translationtoolservice.service.impl;

import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.dao.ProductMapper;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.dao.VersionMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.service.PublicService;
import com.shr.translationtoolservice.util.DeepLTranslateUtils;
import com.shr.translationtoolservice.util.TranslateUtils;
import com.shr.translationtoolservice.util.YoudaoTrans;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @title PublicServiceImpl
 * @create 2024/4/8 14:46
 * @description <TODO description class purpose>
 **/
@Service
public class PublicServiceImpl implements PublicService {

    @Autowired
    private EntryInfoMapper entryInfoMapper;

    @Autowired
    private TLanguageMapper languageMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private VersionMapper versionMapper;

    @Autowired
    private DeepLTranslateUtils deepLTranslateUtils;

    @Autowired
    private TranslateUtils translateUtils;

    @Autowired
    private YoudaoTrans youdaoTrans;

    @Override
    public List<PublicEntryEntity> queryTranslate(EntryInfoEntity entity, String targetLang) {
        List<PublicEntryEntity> res = new ArrayList<>();
        if(StringUtils.isNotBlank(entity.getProductName()) && StringUtils.isBlank(entity.getProductID()) ){
            List<ProductEntity> productEntities = productMapper.selectByName(entity.getProductName());
            if (CollectionUtils.isEmpty(productEntities)){
                return null;
            }
            entity.setProductID(productEntities.get(0).getId());
        }
        if (StringUtils.isNotBlank(entity.getVersionName()) && StringUtils.isBlank(entity.getVersionID())){
             List<VersionEntity> versionEntities = versionMapper.selectByName(entity.getVersionName());
             if (CollectionUtils.isEmpty(versionEntities)){
                 entity.setVersionID(versionEntities.get(0).getId());
             }
        }
        entity.setIsDelete(0);
        entity.setEntryState(3);
        entity.setEnglishTranslateState("3");
        entity.setRussianTranslateState("3");
        entity.setFrenchTranslateState("3");
        entity.setSpanishTranslateState("3");
        List<EntryInfoEntity> entryList = entryInfoMapper.getEntryByVersion(entity, ConstantInterface.MINUS_ONE, ConstantInterface.MINUS_ONE);
        for (EntryInfoEntity entryInfoEntity : entryList) {
            PublicEntryEntity publicEntry = new PublicEntryEntity();
            // 赋值相同属性
            BeanUtils.copyProperties(entryInfoEntity,publicEntry);
            // 设置翻译
            if (ConstantInterface.EN_ENGLISH.equals(targetLang)){
                // 英文
                publicEntry.setTranslate(entryInfoEntity.getEnglish());
                publicEntry.setTranslateState(entryInfoEntity.getEnglishTranslateState());
            }else if(ConstantInterface.EN_RUSSIAN.equals(targetLang)){
                // 俄文
                publicEntry.setTranslate(entryInfoEntity.getRussian());
                publicEntry.setTranslateState(entryInfoEntity.getRussianTranslateState());
            }else if(ConstantInterface.EN_FRENCH.equals(targetLang)){
                // 法文
                publicEntry.setTranslate(entryInfoEntity.getFrench());
                publicEntry.setTranslateState(entryInfoEntity.getFrenchTranslateState());
            }else if(ConstantInterface.EN_SPANISH.equals(targetLang)){
                // 西文
                publicEntry.setTranslate(entryInfoEntity.getSpanish());
                publicEntry.setTranslateState(entryInfoEntity.getSpanishTranslateState());
            }
            res.add(publicEntry);
        }
        return res;
    }

    @Override
    public List<PublicEntryEntity> realTimeTranslate(List<String> entityList, String targetLang) {
        // 查询目标语言
        TLanguage tLanguage = new TLanguage();
        tLanguage.setEnglish(targetLang);
        List<TLanguage> languages = languageMapper.getLanguages(tLanguage);
        if (languages.isEmpty()){
            return null;
        }
        // 目标语言
        TLanguage target = languages.get(0);
        //
        Queue<String> queue = new LinkedList<>();
        for (String key : ConstantInterface.translateMachine().keySet()) {
            if (!ConstantInterface.SYK.equals(key)){
                queue.add(key);
            }
        }
        List<PublicEntryEntity> res = new ArrayList<>();
        for (String text : entityList) {
            String translateRes = "";
            while (!queue.isEmpty()){
                if (translateRes != null && !"".equals(translateRes)){
                    break;
                }
                String type = queue.remove();
                if (type.equals(ConstantInterface.DEEPL)){
                    // deepl翻译
                    translateRes = deepLTranslateUtils.translate(text, null, target.getDeeplCode());
                }else if (type.equals(ConstantInterface.BD)){
                    // 百度翻译
                    LanguageEntity translateResult = translateUtils.getTranslateResult(text, ConstantInterface.AUTO, target);
                    if (!Objects.isNull(translateResult)){
                        translateRes = translateResult.getValue();
                    }
                }else if (type.equals(ConstantInterface.YD)){
                    // 有道翻译
                    LanguageEntity languageEntity = youdaoTrans.youdaoTranslate(text, ConstantInterface.AUTO, target);
                    if (!Objects.isNull(languageEntity)) {
                        translateRes = languageEntity.getValue();
                    }
                }else if (type.equals(ConstantInterface.GG)){
                    // TODO google翻译
                }else if (type.equals(ConstantInterface.MD)){
                    // TODO 本地模型翻译
                }
            }
            PublicEntryEntity publicEntry = new PublicEntryEntity();
            publicEntry.setEntry(text);
            publicEntry.setTranslate(translateRes);
            res.add(publicEntry);
        }
        return res;
    }
}
