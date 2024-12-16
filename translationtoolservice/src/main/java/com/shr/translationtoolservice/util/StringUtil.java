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
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            } else {
                result.append(" "); // 保留多余的空格
            }
        }
        return result.toString().trim(); // 去掉最后的多余空格
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
        String[] words = str.split(" ");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toLowerCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            } else {
                result.append(" "); // 保留多余的空格
            }
        }
        return result.toString().trim(); // 去掉最后的多余空格
    }
}
