package com.shr.translationtoolservice.service.analyze;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


import org.springframework.stereotype.Component;

@Component
public class PercentageAnalyzer implements TranslateAnalyzer<AnalyzeSample> {

    private Comparator<String> comparator = new Comparator<String>() {

        @Override
        public int compare(String o1, String o2) {
            // TODO Auto-generated method stub
            int length1 = o1.length();
            int length2 = o2.length();
            int idx1 = 0;
            int idx2 = 0;
            while (idx1 < length1 && idx2 < length2) {
                char nextChar1 = o1.charAt(idx1);
                char nextChar2 = o2.charAt(idx2);
                if(nextChar1 > nextChar2){
                    return 1;
                }else if(nextChar1 < nextChar2){
                    return -1;
                }
                idx1 ++ ;
                idx2 ++ ;
            }
            if(idx1 < length1){
                return 1;
            }else if(idx2 < length2){
                return -1;
            }else{
                return 0;
            }
        }
        
    };


    @Override
    public boolean analyze(AnalyzeSample analyzeSample) {
        // TODO Auto-generated method stu
        String entry = analyzeSample.getEntry();
        String translate = analyzeSample.getTranslate();
        if(entry == null || translate == null){
            return false;
        }
        List<String> entryPlaceHolders = acquirePlaceHolder(entry);
        entryPlaceHolders.sort(comparator);
        List<String> translatePlaceHolders = acquirePlaceHolder(translate);
        translatePlaceHolders.sort(comparator);
        return !entryPlaceHolders.equals(translatePlaceHolders);
       
    }

    private List<String> acquirePlaceHolder(String text){
    
        int length = text.length();
        List<String> placeholderByEntry = new ArrayList<>();
        for(int i = 0 ; i < length - 1; i ++){
            char nextChar = text.charAt(i);
            if(nextChar == '%'){
                // 获取%后面属于数字的部分
                int idx = i + 1;
                while (idx < length) {
                    char targetChar = text.charAt(idx);
                    if(targetChar >= '0' && targetChar <= '9'){
                        idx ++ ;
                    }else{
                        break;
                    }
                }
                if(idx == i + 1){
                    continue;
                }
                String placeholder = text.substring(i, idx);
                placeholderByEntry.add(placeholder);
                i = idx - 1;
            }
        }
        return placeholderByEntry;
    }
//     public static void main(String[] args) {
//         PercentageAnalyzer analyzer = new PercentageAnalyzer();
//         analyzer.acquirePlaceHolder("");
//         String[] testCases = {"","23fdvse","241242% 9","%% 8%#4","sdwrg%%",
//         "%1s3e","asfe%2b5d","%10vrf","das%1gvr","asg4%","%132","sdf%22",
//     "   23%1","v29%214",
//     "%2 fe23 %3 vd","f3: %2 fe23 %3 vd","cdfe: %2%3few",
//     "ceew: %123 and %31 is better than %1, is'it",
//     "name: %12%23 is better",
//     "your name is %1, but his name is %2",
//     "today is %1%2,but %1 today is %1%2",
//     "you'd better %12%23, because he is %1",
//     "%1243%12%1%2"


// };
//         for(String testCase : testCases){
//             List<String> acquirePlaceHolder = analyzer.acquirePlaceHolder(testCase);
//             if(!acquirePlaceHolder.isEmpty()){
//                 for(String placeHolder : acquirePlaceHolder){
//                     System.out.println("case: "+testCase + ",result: " + placeHolder);
//                 }
//             }
//         }
//     }
    
}
