package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.service.TranslateService;
import com.shr.translationtoolservice.dao.TranslateMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 */
@Service
public class TranslateServiceImpl extends ServiceImpl<TranslateMapper, TranslateEntity>
    implements TranslateService{

    @Autowired
    private TranslateMapper translateMapper;

    @Autowired
    private CommonUtils commonUtils;

    @Override
    public String updateTranslation(List<TranslateEntity> translateEntityList) {
        int updata = translateMapper.updateTranslation(translateEntityList);
        if (updata != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public String addTranslate(TranslateEntity translateEntity) {
        if (StringUtils.isBlank(translateEntity.getId())){
            translateEntity.setId(commonUtils.getUUID());
        }

        int insert = translateMapper.insert(translateEntity);
        if (insert != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }
}




