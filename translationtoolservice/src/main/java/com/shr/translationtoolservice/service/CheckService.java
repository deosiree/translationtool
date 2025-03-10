package com.shr.translationtoolservice.service;

import com.google.gson.JsonObject;

public interface CheckService {
    JsonObject getQuestionTypes(String url);

    JsonObject getModuleNames(String url);

    JsonObject searchCheckInfo(String url,String checkUrl,String params);
}
