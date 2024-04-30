package com.shr.translationtoolservice;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.dao.EntryMapper;
import com.shr.translationtoolservice.dao.IndexMapper;
import com.shr.translationtoolservice.entity.*;

import com.shr.translationtoolservice.entity.vo.ImportExcleVO;
import com.shr.translationtoolservice.util.ExcelUtils;
import com.shr.translationtoolservice.util.HTTPUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.platform.commons.util.StringUtils;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.NodeList;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.poi.sl.usermodel.PresetColor.Control;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:application.yml"})
class TranslationtoolserviceApplicationTests {
    private static final String DEEPL_TRANSLATE_URL = "https://api.deepl.com/v2/translate";
    private static final String AUTH_HEADER_NAME = "Authorization";
    private static final String AUTH_HEADER_VALUE_TEMPLATE = "DeepL-Auth-Key %s";
    private static final String TARGET_LANG_QUERY_PARAM = "target_lang";
    private static final String TEXT_QUERY_PARAM = "text";

    public static String translate(String apiKey, String text, String targetLang) throws Exception {
        String url = buildUrl(DEEPL_TRANSLATE_URL, apiKey, text, targetLang);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty(AUTH_HEADER_NAME, String.format(AUTH_HEADER_VALUE_TEMPLATE, apiKey));

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) { // 200
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString();
        } else {
            return "Error: " + responseCode;
        }
    }

    private static String buildUrl(String baseUrl, String apiKey, String text, String targetLang) throws Exception {
        return baseUrl + "?" + TARGET_LANG_QUERY_PARAM + "=" + targetLang + "&" + TEXT_QUERY_PARAM + "=" + URLEncoder.encode(text, "UTF-8");
    }

    public static void main(String[] args) {
        try {
            String apiKey = "your_api_key_here";
            String textToTranslate = "Hallo Welt!";
            String targetLanguage = "EN";

            String translatedText = translate(apiKey, textToTranslate, targetLanguage);
            System.out.println(translatedText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
