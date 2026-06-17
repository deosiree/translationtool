package com.shr.translationtoolservice.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JSONUtils {

    public static Gson gson = new Gson();

    public static Gson getGson(){
        return gson;
    }

    public static <T> T parseToJson(InputStream ins, String charset, Class<T> clazz) {
        return parseToJson(ins, charset, (Type) clazz);
    }

    public static <T> T parseToJson(InputStream ins,String charset,Type type){
        if(ins == null){
            return null;
        }
        if(charset == null || charset.trim().isEmpty()){
            charset = StandardCharsets.UTF_8.name();    // 默认采用UTF-8
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(ins,charset));
            String jsonContent = reader.lines().collect(Collectors.joining());
            return gson.fromJson(jsonContent, type);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        } catch(Exception e){
            throw new RuntimeException(String.format("解析json文件出现异常, 异常信息为: %s", e.getMessage()));
        } finally{
            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    throw new RuntimeException(e);
                }
            }
        }


    }

    public static boolean exportJson(Object object,String filePath){
        Gson gson = new GsonBuilder()
            .setPrettyPrinting() // 开启格式化（换行+缩进）
            .disableHtmlEscaping() // 禁用HTML转义（避免中文/特殊字符被转义）
            .create();
        String jsonString =  gson.toJson(object);

        File file = new File(filePath);
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8.name());
            writer.write(jsonString);
            return true;
        } catch (Exception e) {
            log.error("导出json文件时出现异常", e);
            throw new RuntimeException(e);
        } finally{
            if(writer != null){
                try {
                    writer.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public static boolean exportJson(Object object,String charset,OutputStream outputStream){
        Gson gson = new GsonBuilder()
            .setPrettyPrinting() // 开启格式化（换行+缩进）
            .disableHtmlEscaping() // 禁用HTML转义（避免中文/特殊字符被转义）
            .create();
        String jsonString =  gson.toJson(object);

        try {
            outputStream.write(jsonString.getBytes(charset));
            return true;
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            log.error("");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.error("");
        } catch (Exception e){
            log.error("");
        }
        return false;
    }
    
}
