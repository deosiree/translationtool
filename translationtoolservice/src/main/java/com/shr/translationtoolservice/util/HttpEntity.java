package com.shr.translationtoolservice.util;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName HttpEntity
 * @Description TODO
 * @USER: Cola
 * @Date 2023/9/14 0014 10:52
 **/
public class HttpEntity {
    public static void main(String[] args) throws Exception {
        String from = "zh-CN";
        //翻译前语言
        String to = "en";
        // 翻译后语言
        String translateText = "中国人";
        // 翻译文本
        String baseUrl = "http://translate.google.cn/translate_a/single";
        String tk = token(translateText);
        String translateResult = getResponse(baseUrl, from, to, tk, translateText);
        System.out.println(translateResult);
    }

    public static String getResponse(String baseUrl, String from, String to, String tk, String translateText) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("client", "webapp");
        params.put("sl", from);
        params.put("tl", to);
        params.put("hl", "zh-CN");
        params.put("dt", "at");
        params.put("dt", "bd");
        params.put("dt", "ex");
        params.put("dt", "ld");
        params.put("dt", "md");
        params.put("dt", "qca");
        params.put("dt", "rw");
        params.put("dt", "rm");
        params.put("dt", "ss");
        params.put("dt", "t");
        params.put("source", "bh");
        params.put("ssel", "0");
        params.put("tsel", "0");
        params.put("kc", "1");
        params.put("tk", tk);
        params.put("q", translateText);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        URIBuilder uri = new URIBuilder(baseUrl);
        for (String key : params.keySet()) {
            String value = params.get(key);
            uri.addParameter(key, value);
        }
        HttpUriRequest request = new HttpGet(uri.toString());
        //设置user-agent
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0");
        CloseableHttpResponse response = httpClient.execute(request);
        org.apache.http.HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity, "UTF-8");
        System.out.println(result);
        result = result.substring(result.indexOf("\"") + 1, result.indexOf(",") - 1);
        System.out.println(result);
        EntityUtils.consume(entity);
        response.getEntity().getContent().close();
        response.close();
        return result;
    }

    //调用js返回tk参数
    private static String token(String value) {
        String result = "";
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
        try {
            FileReader reader = new FileReader("E:\\词条\\trans2.js");
            engine.eval(reader);
            if (engine instanceof Invocable) {
                Invocable invoke = (Invocable) engine;
                result = String.valueOf(invoke.invokeFunction("token", value));
                System.out.println(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}