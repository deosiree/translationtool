/*
package com.shr.translationtoolservice.util;


import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.rules.RuleMatch;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import static com.deepl.api.LanguageCode.English;

public class GrammarChecker {
    public static void main(String[] args) throws Exception {
        // 要检查的文本
        toolCheckGrammar("This is an example sentence with a error.");
    }
    public static void apiCheckGrammar(String text) throws Exception {
        // 要检查的文本
         text = "This is an example sentence with a error.";

        // 请求 URL
        URL url = new URL("https://api.languagetool.org/v2/check");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);

        // 请求参数
        String params = "language=en-US&text=" + text;

        // 发送请求
        try (OutputStream os = connection.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
        }

        // 读取响应
        try (Scanner scanner = new Scanner(connection.getInputStream(), String.valueOf(StandardCharsets.UTF_8))) {
            String response = scanner.useDelimiter("\\A").next();
            System.out.println("Response: " + response);
        }
    }

    public static String toolCheckGrammar(String text) throws Exception {
        // 创建 LanguageTool 对象，指定语言为美式英语
        Language Language = new English();
        JLanguageTool langTool = new JLanguageTool(Language);
        JLanguageTool langTool1 = new JLanguageTool(new AmericanEnglish());
        // 要检查的文本
         text = "This is an example sentence with a error.";

        // 获取语法错误列表
        List<RuleMatch> matches = langTool.check(text);

        // 打印语法错误信息
        for (RuleMatch match : matches) {
            System.out.println("Potential error at characters " +
                    match.getFromPos() + "-" + match.getToPos() + ": " +
                    match.getMessage());
            System.out.println("Suggested correction(s): " +
                    match.getSuggestedReplacements());
        }
    }
}
*/
