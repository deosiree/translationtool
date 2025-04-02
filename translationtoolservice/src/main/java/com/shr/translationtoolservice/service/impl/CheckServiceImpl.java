package com.shr.translationtoolservice.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.vo.TsVo;
import com.shr.translationtoolservice.service.CheckService;
import com.shr.translationtoolservice.util.HTTPUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CheckServiceImpl implements CheckService {

    @Autowired
    private HTTPUtils httpUtils;

    @Autowired
    private EntryInfoMapper entryInfoMapper;
    
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
    public JsonObject searchCheckInfo(String i18nUrl, String checkUrl,String params) {
        //通过调用10.17.14.115:18001/checkManage/getQuestionTypes接口获取数据
        String s = "";
        JsonObject jsonObject;
        try {
           
            String address = i18nUrl + "/checkManage/searchCheckInfo/" + checkUrl;
 
            s = httpUtils.proxyHttpRequest(address,"POST",null,params);
            //JSONObject jsonObject = new JSONObject();

            jsonObject = JsonParser.parseString(s).getAsJsonObject();
            // 提取 "data" 部分
            // dataObject = jsonObject.getAsJsonObject("data");



        } catch (Exception e) {
            return null;
        }

        return jsonObject;
    }
    
    @Override
    public List<TsVo> getTsEntryWithProblem(EntryInfoEntity entryInfoEntityTemplate,String department) {
        
        entryInfoEntityTemplate.setImportType("TS");
        List<TsVo> entryUsingTranslate = entryInfoMapper.getEntryByImportTypeUsingTranslate(entryInfoEntityTemplate,department);
        int totalCount = entryUsingTranslate.size();
        List<TsVo> entryFiltered = new LinkedList<>();
        if(totalCount <= 0){
            return entryFiltered;
        }else{
            int rightIndex = 0;
            // 翻译不同，需要把翻译不同，但source、comment、tag相同的都收集起来
            /**
             * 如果栈中没有元素，则放进去一个
             * if: peek对象的前三个值与next对象的相同
             *      if translate相同,放进去 isHasProblems = false
             *      else 放进去,isHasProblems = true
             * else: 
             *     全部出栈，放入next对象
             *          如果出栈，isHasProblems = true，则将出栈的元素都放入带搜索列表
             *          
             */
            Stack<TsVo> stack = new Stack<>();
            boolean isHasProblems = false;
            while(rightIndex < totalCount){
                if(stack.isEmpty()){
                    stack.push(entryUsingTranslate.get(rightIndex++));
                }else{
                    TsVo refTsVo = stack.firstElement();
                    TsVo rightTsVo = entryUsingTranslate.get(rightIndex++);
                    if(refTsVo.getType().equals(rightTsVo.getType()) && refTsVo.isEqualsNotConsiderTrans(rightTsVo)){
                        String refTranslate = refTsVo.getTranslate();
                        String rightTranslate = rightTsVo.getTranslate();
                        isHasProblems = !TsVo.isEquals(refTranslate, rightTranslate);
                        // 去重
                        if(isHasProblems && !rightTranslate.equals(stack.lastElement().getTranslate())){
                            stack.push(rightTsVo);
                        }
                    }else{
                        if(isHasProblems){
                            // entryFiltered.addAll(stack.subList(0, stack.size()));
                            entryFiltered.add(TsVo.copy(stack.peek()));
                            isHasProblems = false;
                        }
                        stack.clear();
                        stack.push(rightTsVo);
                    }
                }
            }
            if(isHasProblems){
                // entryFiltered.addAll(stack.subList(0, stack.size()));
                entryFiltered.add(TsVo.copy(stack.peek()));
            }
            return entryFiltered;
        }


    }
}
