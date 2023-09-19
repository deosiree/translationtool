package com.shr.translationtoolservice.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;

import java.beans.Encoder;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.LanguageEntity;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @ClassName 有道翻译
 * @USER: Cola
 * @Date 2023/9/15 0015 9:12
 **/
public class YoudaoTrans {

    private static String appKeyID = "12539761968f9072";
    private static String appKey = "aCwCTdgEH1lVY1DEfTXjnIV8dJbgD8sZ";
    private static Logger logger = LoggerFactory.getLogger(YoudaoTrans.class);

    private static final String YOUDAO_URL = "https://openapi.youdao.com/api";


    public static void main(String[] args) {
        String str1 = "wo+ni";
        //考虑到翻译词语可能会有特殊符号  必须经过处理 不然会错误
        String regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str1);

        String str = "中国";
        String[] split2 = str.split("\n");
        System.out.println(split2.length);
        try {
            String result = readJsonFromUrl(str, "zh-CHS", "en").getValue();
            System.out.println(result);
            String[] split = result.split("\n");
            List<String> list = Arrays.asList(split);
            for (String string : list) {
                System.out.println(string);
            }
            System.out.println(list.size());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


   /*     String value = "";

        Map<String, String> params = new HashMap<String, String>();
        String q = "你好";
        String salt = String.valueOf(System.currentTimeMillis());
        params.put("from", "auto");
        params.put("to", "auto");
        params.put("signType", "v3");
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        params.put("curtime", curtime);
        String signStr = appKeyID + truncate(q) + salt + curtime + appKey;
        String sign = getDigest(signStr);
        params.put("appKey", appKeyID);
        params.put("q", q);
        params.put("salt", salt);
        params.put("sign", sign);

        *//** 处理结果 *//*
        try {
            value = requestForHttp(YOUDAO_URL, params);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public static LanguageEntity youdaoTranslate(String entry, String from, String to) {
        if ("ch".equals(to)) {
            to = "zh-CHS";
        }

        LanguageEntity languageEntity = new LanguageEntity();
        String value = "";
        Map<String, String> params = new HashMap<String, String>();
        //String q = "苹果";
        String salt = String.valueOf(System.currentTimeMillis());
        params.put("from", from);
        params.put("to", to);
        params.put("signType", "v3");
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        params.put("curtime", curtime);
        String signStr = appKeyID + truncate(entry) + salt + curtime + appKey;
        String sign = getDigest(signStr);
        params.put("appKey", appKeyID);
        params.put("q", entry);
        params.put("salt", salt);
        params.put("sign", sign);
        /** 处理结果 */
        try {
            JSONObject json = new JSONObject( requestForHttp(YOUDAO_URL, params).toString());
            JSONArray array = (JSONArray) json.get("translation");
            StringBuffer text = new StringBuffer();
            int i = 0;
            //考虑到批量查询   查询的词中间用\n分隔  为了返回的是个字符串  此处是拼起来了 用的时候分割也可以  你也可以直接用集合接收  随你高兴
            for (; i < array.size(); i++) {

                text.append(array.get(i) );
            }
            languageEntity.setValue(text.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, String> languageMap = ConstantInterface.LANGUAGE_MAP;
        languageEntity.setLanguage(languageMap.get(to).toLowerCase());
        return languageEntity;
    }

    /**
     * 生成32位md5摘要
     *
     * @return
     */
    public static String md5(String string) {
        if (string == null) {
            return null;
        }
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        byte[] btInput = string.getBytes();
        try {
            /** 获得MD5摘要算法的 MessageDigest对象 **/
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            /** 使用指定的字节更新摘要 **/
            mdInst.update(btInput);
            /** 获得密文 **/
            byte[] md = mdInst.digest();
            /** 把密文转换成十六进制的字符串形式 **/
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成有道URL
     *
     * @return
     */
    public static String creatUrl(String query, String from, String to) {

        String salt = String.valueOf(System.currentTimeMillis());
        String sign = md5(appKeyID + query + salt + appKey);
        Map<String, String> params = new HashMap<String, String>();
        params.put("q", query);
        params.put("from", from);
        params.put("to", to);
        params.put("sign", sign);
        params.put("salt", salt);
        params.put("appKey", appKeyID);
        params.put("signType", "v3");
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        params.put("curtime", curtime);

        String urlStr = "https://openapi.youdao.com/api";
        return getUrlWithQueryString(urlStr, params);

    }

    public static String createParam(String query, String from, String to) {
        String salt = String.valueOf(System.currentTimeMillis());
        String sign = md5(appKeyID + query + salt + appKey);
        Map<String, String> params = new HashMap<String, String>();
        params.put("q", query);
        params.put("from", from);
        params.put("to", to);
        params.put("sign", sign);
        params.put("salt", salt);
        params.put("appKey", appKeyID);

        String urlStr = "";
        return getUrlWithQueryString(urlStr, params);
    }


    /**
     * 根据api地址和参数生成请求URL
     *
     * @param url
     * @param params
     * @return
     */
    public static String getUrlWithQueryString(String url, Map<String, String> params) {
        if (params == null) {
            return url;
        }

        StringBuilder builder = new StringBuilder(url);
        if (url.contains("?")) {
            builder.append("&");
        } else {
            builder.append("?");
        }

        int i = 0;
        for (String key : params.keySet()) {
            String value = params.get(key);
            if (value == null) { // 过滤空的key
                continue;
            }

            if (i != 0) {
                builder.append('&');
            }

            builder.append(key);
            builder.append('=');
            builder.append(encode(value));

            i++;
        }

        return builder.toString();
    }

    /**
     * 进行URL编码
     *
     * @param input
     * @return
     */
    public static String encode(String input) {
        if (input == null) {
            return "";
        }

        try {
            return URLEncoder.encode(input, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return input;
    }


    /**
     * @param query 查询的词  多词用\n
     * @param from  原文语种可以为auto
     * @param to    必填
     * @author S. Yichen
     */
    public static LanguageEntity readJsonFromUrl(String query, String from, String to) throws IOException, JSONException {
        LanguageEntity languageEntity = new LanguageEntity();
        languageEntity.setLanguage(to);
        //生成查询地址
        String url = creatUrl(query, from, to);
        System.out.println(url);
        InputStream is = new URL(url).openStream();
        String str = null;
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            while ((str = reader.readLine()) != null) {
                result.append(str).append("\n");
            }

            JSONObject json = new JSONObject(result.toString());

            //开发者自行处理错误
            //停用返回401
            String error_code = json.getStr("errorCode");
            if (!"0".equals(error_code)) {
                System.out.println("出错代码:" + error_code);
                System.out.println("出错信息:" + json.getStr("errorCode"));
                return null;
            }
            //获取返回翻译结果
            JSONArray array = (JSONArray) json.get("translation");
            StringBuffer text = new StringBuffer();
            int i = 0;
            //考虑到批量查询   查询的词中间用\n分隔  为了返回的是个字符串  此处是拼起来了 用的时候分割也可以  你也可以直接用集合接收  随你高兴
            for (; i < array.size(); i++) {

                text.append(array.get(i) + "\n");
            }
            languageEntity.setValue(text.toString());
            return languageEntity;
        } finally {
            is.close();
        }
    }


    public static String requestForHttp(String url, Map<String, String> params) throws IOException {

        /** 创建HttpClient */
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String json = "";
        /** httpPost */
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> paramsList = new ArrayList<NameValuePair>();
        Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> en = it.next();
            String key = en.getKey();
            String value = en.getValue();
            paramsList.add(new BasicNameValuePair(key, value));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(paramsList, "UTF-8"));
        CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
        try {
            Header[] contentType = httpResponse.getHeaders("Content-Type");
            logger.info("Content-Type:" + contentType[0].getValue());
            if ("audio/mp3".equals(contentType[0].getValue())) {
                //如果响应是wav
                HttpEntity httpEntity = httpResponse.getEntity();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                httpResponse.getEntity().writeTo(baos);
                byte[] result = baos.toByteArray();
                EntityUtils.consume(httpEntity);
                if (result != null) {//合成成功
                    String file = "合成的音频存储路径" + System.currentTimeMillis() + ".mp3";
                    byte2File(result, file);
                }
            } else {
                /** 响应不是音频流，直接显示结果 */
                HttpEntity httpEntity = httpResponse.getEntity();
                json = EntityUtils.toString(httpEntity, "UTF-8");
                EntityUtils.consume(httpEntity);
                logger.info(json);

                System.out.println(json);

            }
        } finally {
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            } catch (IOException e) {
                logger.info("## release resouce error ##" + e);
            }
        }
        return json;
    }

    /**
     * 生成加密字段
     */
    public static String getDigest(String string) {
        if (string == null) {
            return null;
        }
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        byte[] btInput = string.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest mdInst = MessageDigest.getInstance("SHA-256");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * @param result 音频字节流
     * @param file   存储路径
     */
    private static void byte2File(byte[] result, String file) {
        File audioFile = new File(file);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(audioFile);
            fos.write(result);

        } catch (Exception e) {
            logger.info(e.toString());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static String truncate(String q) {
        if (q == null) {
            return null;
        }
        int len = q.length();
        String result;
        return len <= 20 ? q : (q.substring(0, 10) + len + q.substring(len - 10, len));
    }
}
