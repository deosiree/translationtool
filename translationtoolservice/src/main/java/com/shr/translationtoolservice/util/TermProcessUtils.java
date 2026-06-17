package com.shr.translationtoolservice.util;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;


public class TermProcessUtils {
    // 计算余弦相似度
    public static double cosineSimilarity(Map<String, Integer> vec1, Map<String, Integer> vec2) {
        double dotProduct = 0;
        double magnitude1 = 0;
        double magnitude2 = 0;

        // 计算点积和模
        for (String term : vec1.keySet()) {
            if (vec2.containsKey(term)) {
                dotProduct += vec1.get(term) * vec2.get(term);
            }
        }

        for (int count : vec1.values()) {
            magnitude1 += Math.pow(count, 2);
        }
        for (int count : vec2.values()) {
            magnitude2 += Math.pow(count, 2);
        }

        // 计算余弦相似度
        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    // 将文本分词并计算词频
    public static Map<String, Integer> getTermFrequency(List<String> words) {
        Map<String, Integer> termFrequency = new HashMap<>();
        for (String word : words) {
            termFrequency.put(word, termFrequency.getOrDefault(word, 0) + 1);
        }
        return termFrequency;
    }

    public static void main(String[] args) {
        String text1 = "我喜欢吃苹果";
        String text2 = "我喜欢吃 苹果";

        // 使用 HanLP 进行分词
        List<String> words1 = HanLP.segment(text1).stream()
                .map(term -> term.word)
                .collect(Collectors.toList());
        List<String> words2 = HanLP.segment(text2).stream()
                .map(term -> term.word)
                .collect(Collectors.toList());

        // 获取词频
        Map<String, Integer> tf1 = getTermFrequency(words1);
        Map<String, Integer> tf2 = getTermFrequency(words2);

        // 计算余弦相似度
        double similarity = cosineSimilarity(tf1, tf2);
        System.out.println("Cosine Similarity: " + similarity);
    }
}
