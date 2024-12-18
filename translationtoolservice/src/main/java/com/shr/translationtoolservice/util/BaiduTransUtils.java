package com.shr.translationtoolservice.util;

import lombok.SneakyThrows;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Component
public class BaiduTransUtils {
    public static final String API_KEY = "J5IWoHTX4LnEVxkLdiIwtOfQ";
    public static final String SECRET_KEY = "YQyq8WFTwJGEAoqmHmcet0hgzS8o46hi";

    static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().build();

    @SneakyThrows
    @GetMapping("/translate")
    public String translate(@RequestParam String textToTranslate, @RequestParam String fromLang, @RequestParam String toLang) throws IOException {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\"q\":\"" + textToTranslate + "\",\"from\":\"" + fromLang + "\",\"to\":\"" + toLang + "\"}");
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rpc/2.0/mt/texttrans/v1?access_token=" + getAccessToken())
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        String responseBody = response.body().string();
        JSONObject jsonObject = new JSONObject(responseBody);
        JSONArray transResult = jsonObject.getJSONObject("result").getJSONArray("trans_result");
        return transResult.getJSONObject(0).getString("dst");
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