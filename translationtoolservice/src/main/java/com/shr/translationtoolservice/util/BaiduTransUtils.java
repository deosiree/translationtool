package com.shr.translationtoolservice.util;

import lombok.SneakyThrows;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class BaiduTransUtils {
    public static final String API_KEY = "J5IWoHTX4LnEVxkLdiIwtOfQ";
    public static final String SECRET_KEY = "YQyq8WFTwJGEAoqmHmcet0hgzS8o46hi";

    static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().build();

    private String constructRequestBody(String textToTranslate,String fromLang,String toLang){
        Gson gson = new Gson();
        Map<String,String> requestMap = new HashMap<>();
        requestMap.put("q", textToTranslate);
        requestMap.put("from",fromLang);
        requestMap.put("to", toLang);
        return gson.toJson(requestMap);
    }

    @SneakyThrows
    @GetMapping("/translate")
    public String translate(@RequestParam String textToTranslate, @RequestParam String fromLang, @RequestParam String toLang) throws IOException {
        MediaType mediaType = MediaType.parse("application/json");
        // RequestBody body = RequestBody.create(mediaType, "{\"q\":\"" + textToTranslate + "\",\"from\":\"" + fromLang + "\",\"to\":\"" + toLang + "\"}");
        String requestMessage = constructRequestBody(textToTranslate, fromLang, toLang);
        RequestBody body = RequestBody.create(mediaType,requestMessage);
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rpc/2.0/mt/texttrans/v1?access_token=" + getAccessToken())
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        String responseBody = response.body().string();
        JSONObject jsonObject = new JSONObject(responseBody);
        try {
            JSONArray transResult = jsonObject.getJSONObject("result").getJSONArray("trans_result");   
            return transResult.getJSONObject(0).getString("dst");
        } catch (Exception e) {
            // TODO: handle exception
            throw new RuntimeException("当前获取baidu翻译异常,获取到的响应体的内容为: " + responseBody + ", 请求体内容为: " + requestMessage);
        }
    }

    @SneakyThrows
    static String getAccessToken() throws IOException {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "grant_type=client_credentials&client_id=" + API_KEY + "&client_secret=" + SECRET_KEY);
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/oauth/2.0/token")
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        return new JSONObject(response.body().string()).getString("access_token");
    }
}