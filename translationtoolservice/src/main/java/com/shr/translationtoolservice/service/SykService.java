package com.shr.translationtoolservice.service;


import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.entity.vo.SykEntryVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SykService {

    List<TranslateEntity> getSykEntry(TranslateEntity translate,Set<String> matchList, Integer pageIndex, Integer pageSize);

    List<TranslateEntity> acquireSykSameEntry(TranslateEntity translate,Integer pageIndex, Integer pageSize);

    List<TranslateEntity> deleteSykEntry(List<TranslateEntity> translateEntity) throws Exception;

    /**
     * 
     * @param translates
     * @return  更新失败的翻译记录
     * @throws Exception
     */
    List<TranslateEntity> updateSykEntry(List<TranslateEntity> translates) throws Exception;

    List<SykEntryVO> getSykEntryRelation(List<TranslateEntity> translates);


    List<TranslateEntity> checkSykEntryByTemplate(TranslateEntity translateEntityTemplate);

    List<TranslateEntity> checkSykEntryOnList(List<TranslateEntity> translateEntities);
    
    List<TranslateEntity> getSykNotUsed(TranslateEntity translateEntity,String token);
}
