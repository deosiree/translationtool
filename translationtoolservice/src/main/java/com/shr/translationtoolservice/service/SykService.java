package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.Translate;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.entity.vo.SykEntryVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public interface SykService {

    List<TranslateEntity> getSykEntry(TranslateEntity translate, Integer pageIndex, Integer pageSize);

    String updateSykEntry(List<TranslateEntity> translates);

    List<SykEntryVO> getSykEntryRelation(List<TranslateEntity> translates);

    List<TranslateEntity> getSykNotUsed(TranslateEntity translateEntity,String token);
}
