package com.shr.translationtoolservice.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.service.CheckService;
import com.shr.translationtoolservice.util.HTTPUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CheckServiceImpl implements CheckService {

    @Autowired
    private HTTPUtils httpUtils;
    @Override
    public JsonObject getQuestionTypes(String i18nUrl) {
        //通过调用10.17.14.115:18001/checkManage/getQuestionTypes接口获取数据
        String s = "";
        JsonObject dataObject ;
        try {
            s = httpUtils.get(i18nUrl + "/checkManage/getQuestionTypes");
            //JSONObject jsonObject = new JSONObject();

            JsonObject jsonObject = JsonParser.parseString(s).getAsJsonObject();
            // 提取 "data" 部分
             dataObject = jsonObject.getAsJsonObject("data");



        } catch (Exception e) {
            return null;
        }

        return dataObject;
    }

    @Override
    public JsonObject getModuleNames(String url) {
        //通过调用10.17.14.115:18001/checkManage/getQuestionTypes接口获取数据
        String s = "";
        JsonObject dataObject ;
        try {
            s = httpUtils.get(url + "/checkManage/getModuleNames");
            //JSONObject jsonObject = new JSONObject();

            JsonObject jsonObject = JsonParser.parseString(s).getAsJsonObject();
            // 提取 "data" 部分
            dataObject = jsonObject.getAsJsonObject("data");



        } catch (Exception e) {
            return null;
        }

        return dataObject;
    }

    @Override
    public JsonObject searchCheckInfo(String i18nUrl, String checkUrl) {
        //通过调用10.17.14.115:18001/checkManage/getQuestionTypes接口获取数据
        String s = "";
        JsonObject dataObject ;
        try {
            s = httpUtils.get(i18nUrl + "checkManage/searchCheckInfo/" + checkUrl);
            //JSONObject jsonObject = new JSONObject();

            JsonObject jsonObject = JsonParser.parseString(s).getAsJsonObject();
            // 提取 "data" 部分
            dataObject = jsonObject.getAsJsonObject("data");



        } catch (Exception e) {
            return null;
        }

        return dataObject;
    }
}
