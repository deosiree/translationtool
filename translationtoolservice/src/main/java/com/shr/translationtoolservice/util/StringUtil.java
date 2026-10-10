package com.shr.translationtoolservice.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StringUtil {
    /**
     * 将字符串中每个单词的首字母转换为大写
     * @param str 输入的字符串
     * @return 每个单词首字母大写的字符串
     */
    public static String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        String[] words = str.split(" ");
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                if (i == 0) {
                    result.append(Character.toUpperCase(words[i].charAt(0)))
                            .append(words[i].substring(1).toLowerCase());
                } else {
                    result.append(" ")
                            .append(Character.toLowerCase(words[i].charAt(0)))
                            .append(words[i].substring(1).toLowerCase());
                }
            } else {
                result.append(" "); // Preserve multiple spaces
            }
        }
        return result.toString().trim(); // Remove trailing spaces
    }

    /**
     * 将字符串中每个单词的首字母转换为小写
     * @param str 输入的字符串
     * @return 每个单词首字母小写的字符串
     */
    public static String uncapitalizeWords(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder result = new StringBuilder();
      /*  String[] words = str.split(" ");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toLowerCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            } else {
                result.append(" "); // 保留多余的空格
            }
        }*/
        //str首字母转换为大写
        result.append(Character.toLowerCase(str.charAt(0)));
        if (str.length() > 1) {
            result.append(str.substring(1));
        }
        return result.toString().trim(); // 去掉最后的多余空格
    }

    public static String addEscapeCharacter(String text){
        if(text == null){
            return null;
        }
        int length = text.length();
        if(length == 0){
            return text;
        }
        StringBuilder builder = new StringBuilder();
        for(int i = 0 ; i < length; i ++ ){
            char nextChar = text.charAt(i);
            if(nextChar == '%'){
                builder.append('\\');
            }else if(nextChar == '\\'){
                builder.append('\\');
            }else if(nextChar == '_'){
                builder.append('\\');
            }
            builder.append(nextChar);
        }
        return builder.toString();
    }

    public static String checkEncoding(String encoding){
        if(encoding == null){
            return null;
        }
        if(encoding.equals("UTF-8") || encoding.equals("GBK")){
            return encoding;
        }
        return null;
    }
}
