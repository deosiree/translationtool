package com.shr.translationtoolservice.service.impl;

import cn.afterturn.easypoi.cache.manager.IFileLoader;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.dao.TranslateMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.EntryVO;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Service
public class EntryInfoServiceImpl extends ServiceImpl<EntryInfoMapper, EntryInfoEntity>
        implements EntryInfoService {

    @Autowired
    private EntryInfoMapper entryInfoMapper;

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private TranslateMapper translateMapper;

    @Override
    public  List<EntryVO> getEntryByVersion(String versionID, Integer offset, Integer pagesize) {
        List<EntryInfoEntity> entryByVersion = entryInfoMapper.getEntryByVersion(versionID, offset, pagesize);

        List<EntryVO> entryVOS = new ArrayList<>();
        for (EntryInfoEntity entryInfoEntity : entryByVersion){
            EntryVO entryVO = new EntryVO();
            List<TranslateEntity> translateEntityList = new ArrayList<>();
            getTransEntity(entryInfoEntity.getEnTransId(),translateEntityList);
            getTransEntity(entryInfoEntity.getRuTransId(),translateEntityList);
            getTransEntity(entryInfoEntity.getSpaTransId(),translateEntityList);
            getTransEntity(entryInfoEntity.getFraTransId(),translateEntityList);
            entryVO.setTranslateEntity(translateEntityList);
            entryVO.setEntryInfoEntity(entryInfoEntity);
            entryVOS.add(entryVO);
        }
        return entryVOS;
    }

    private void getTransEntity(String transId, List<TranslateEntity> translateEntityList) {
        if (StringUtils.isNotBlank(transId)){
            TranslateEntity translateEntity = translateMapper.selectById(transId);
            translateEntityList.add(translateEntity);
        }
    }

    @Override
    public int getEntryByVersionTotal(String versionID) {
        return entryInfoMapper.getEntryByVersionTotal(versionID);
    }

    @Override
    public HttpResponse<String> addEntryByVersion(List<EntryVO> entryVOS, HttpServletRequest request) {
        HttpResponse<String> response = new HttpResponse<>();
        for (EntryVO entryVO : entryVOS) {
            EntryInfoEntity entryInfoEntity = entryVO.getEntryInfoEntity();
            //一次新增只有一种翻译，取第一个元素
            TranslateEntity translateEntities = entryVO.getTranslateEntity().get(0);

            translateEntities.setDeleteState(0);
            //添加transid
            addTransID(translateEntities,entryInfoEntity);

            //校验ABBR
            if (checkAbbrRepe(entryInfoEntity,entryVO.getTableName())) {
                response.setMessage(ErrorCodeList.ABBR_HAS_EXIST);
                response.setCode(HttpResponse.Type.ERROR.getVal());
                response.setType(HttpResponse.Type.ERROR);
                return response;
            }
            if (StringUtils.isBlank(entryInfoEntity.getId())) {
                entryInfoEntity.setId(commonUtils.getUUID());
            }
            entryInfoEntity.setIsDelete(0);
            entryInfoEntity.setEntryState(ConstantInterface.CREATE_STATE);
            int insert = entryInfoMapper.insertEntry(entryInfoEntity,entryVO.getTableName());
            int transInsert = translateMapper.insert(translateEntities);

        }

        response.setMessage(ErrorCodeList.SUCCESS);
        response.setCode(HttpResponse.Type.OK.getVal());
        response.setType(HttpResponse.Type.OK);
        return response;
}

    @Override
    public String addEntryInfo(EntryInfoEntity entryInfoEntity, HttpServletRequest request,String tableName) {
        if (StringUtils.isBlank(entryInfoEntity.getId())){
            entryInfoEntity.setId(commonUtils.getUUID());
        }
        int insert = entryInfoMapper.insertEntry(entryInfoEntity,tableName);
        return ConstantInterface.OK_STR;
    }

    private void addTransID(TranslateEntity translateEntities, EntryInfoEntity entryInfoEntity) {
        switch (translateEntities.getType()){
            case ConstantInterface.ENGLISH:
                if (StringUtils.isBlank(translateEntities.getId())){
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setEnTransId(transID);
                }else {
                    entryInfoEntity.setEnTransId(translateEntities.getId());
                }
                break;
            case ConstantInterface.FRENCH:
                if (StringUtils.isBlank(translateEntities.getId())){
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setFraTransId(transID);
                }else {
                    entryInfoEntity.setFraTransId(translateEntities.getId());
                }
                break;
            case ConstantInterface.RUSSIAN:
                if (StringUtils.isBlank(translateEntities.getId())){
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setRuTransId(transID);
                }else {
                    entryInfoEntity.setRuTransId(translateEntities.getId());
                }
                break;
            case ConstantInterface.SPANISH:
                if (StringUtils.isBlank(translateEntities.getId())){
                    String transID = commonUtils.getUUID();
                    translateEntities.setId(transID);
                    entryInfoEntity.setSpaTransId(transID);
                }else {
                    entryInfoEntity.setSpaTransId(translateEntities.getId());
                }
                break;

        }
    }

    //检查abbr 是否重复 重复返回true
    private boolean checkAbbrRepe(EntryInfoEntity entryInfoEntity,String tableName) {
        List<EntryInfoEntity> abbrEntryInfo = new ArrayList<>();
        if (StringUtils.isNotBlank(entryInfoEntity.getAbbr())) {
            abbrEntryInfo = entryInfoMapper.getEntryByAbbr(entryInfoEntity.getAbbr(), entryInfoEntity.getVersionID(),tableName);
            //校验ABBR 重复
            if (!CollectionUtils.isEmpty(abbrEntryInfo)) {
                return true;
            }

        }
        return false;
    }

    /**
     * 设置词条翻译状态和长度
     *
     * @param entryInfoEntity
     */
    private void constructEntry(EntryInfoEntity entryInfoEntity) {
     /*   if (StringUtils.isNotBlank(entryInfoEntity.getChinese())) {
            entryInfoEntity.setChineseLength(entryInfoEntity.getChinese().length());
            entryInfoEntity.setChineseTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setChineseLength(ConstantInterface.ZERO);
            entryEntity.setChineseTranslateState(ConstantInterface.UNTRANSLATED);
        }
        if (StringUtils.isNotBlank(entryEntity.getEnglish())) {
            entryEntity.setEnglishLength(entryEntity.getEnglish().length());
            entryEntity.setEnglishTranslateState(ConstantInterface.TRANSLATED);
        } else {
            entryEntity.setEnglishLength(ConstantInterface.ZERO);
            entryEntity.setEnglishTranslateState(ConstantInterface.UNTRANSLATED);
        }*/
        return;
    }
}




