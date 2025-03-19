package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.Translate;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.entity.vo.SykEntryVO;

import java.util.List;

public interface SykService {

    List<TranslateEntity> getSykEntry(TranslateEntity translate, Integer pageIndex, Integer pageSize);

    /**
     * 
     * @param translates
     * @return  更新失败的翻译记录
     * @throws Exception
     */
    List<TranslateEntity> updateSykEntry(List<TranslateEntity> translates) throws Exception;

    List<SykEntryVO> getSykEntryRelation(List<TranslateEntity> translates);

    List<TranslateEntity> getSykNotUsed();
}
