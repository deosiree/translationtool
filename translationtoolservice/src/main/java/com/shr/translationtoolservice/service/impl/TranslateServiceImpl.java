package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.service.TranslateService;
import com.shr.translationtoolservice.dao.TranslateMapper;
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

    @Override
    public String updateTranslation(List<TranslateEntity> translateEntityList) {
        int updata = translateMapper.updateTranslation(translateEntityList);
        if (updata != ConstantInterface.DB_SUCCESS_RESULT) {
            return ErrorCodeList.UPDATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }
}




