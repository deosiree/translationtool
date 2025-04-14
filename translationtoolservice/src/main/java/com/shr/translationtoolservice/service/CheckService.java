package com.shr.translationtoolservice.service;

import java.util.List;

import com.google.gson.JsonObject;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.vo.TsVo;

public interface CheckService {
    JsonObject getQuestionTypes(String url);

    JsonObject getModuleNames(String url);

    JsonObject searchCheckInfo(String url,String checkUrl,String params);
    /**
     * 检验词条中是否有entry，tag,comment相同，但翻译不相同的情况
     * @param entryInfoEntityTemplate
     * @return
     */
    List<TsVo> getTsEntryWithProblem(EntryInfoEntity entryInfoEntityTemplate,String department);

}
