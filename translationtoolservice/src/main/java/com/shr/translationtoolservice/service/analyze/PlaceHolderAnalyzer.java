package com.shr.translationtoolservice.service.analyze;

import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PlaceHolderAnalyzer implements TranslateAnalyzer<AnalyzeSample> {

    // @Resource(name = "DefaultTransProcessor")
    // TransProcessor transProcessor;

    private static String[] placeHolders = {"{:d}","{:s}"}; // 列表中的占位符必须保证每一个占位符字符串不是另一个占位符字符串的子串


    @Override
    public boolean analyze(AnalyzeSample analyzeSample) {
        // TODO Auto-generated method stub
       
        String entry = analyzeSample.getEntry();
        String translate = analyzeSample.getTranslate();
        if(entry == null || translate == null){
            return false;
        }
        List<String> entryPlaceHolder = countPlaceHolder(entry,placeHolders);
        List<String> translatePlaceHolder = countPlaceHolder(translate,placeHolders);

        return !entryPlaceHolder.equals(translatePlaceHolder);
        
        
    }

    /**
     * 统计字符串里面目标占位符的个数，并按顺序将占位符放入列表内
     * @param text
     * @return
     */
    private List<String> countPlaceHolder(String text,String[] placeHolders){
        List<String> list = new LinkedList<>();
        if(placeHolders.length == 0){
            return list;
        }
        int length = text.length();
        if(length == 0){
            // text是""
            for(String placeHolder : placeHolders){
                if(placeHolder.equals(text)){
                    list.add(placeHolder);
                    break;
                }
            }
        }else{
            for(int i = 0 ; i < length ; i ++ ){
                for(String placeHolder : placeHolders){
                    if(isContains(text, placeHolder, i)){
                        int placeHolderLength = placeHolder.length();
                        list.add(text.substring(i, i + placeHolderLength));
                        i += placeHolderLength - 1;
                        break;
                    }else{
                        continue;
                    }
                }

            }
        }
        return list;
    }
    
    /**
     * {@code string1}从{@code startIndex}开始，长度为{@code target}的子字符串是否与{@code target}相同 
     * @param string1
     * @param target
     * @param startIndex
     * @return  当{@code startIndex}大于string1的长度时，为false
     */
    private boolean isContains(String string1,String target,int startIndex){
        // "","",0 --> true  ---> true
        // "","",1 --> false  ---> false
        // "244dvds","",任意 --> false  --->false
        // "","23232",0  ---> false ---> false
        // "sdw","sdw",1 ---> false ---> false
        // "sdw","sdw",0 ---> true ---> true
        // "sdw","sdw",3 ---> false ---> false
        // "sdwsdw","sdw",0 --> true --> true
        // "sdwsdw","sdw",1 --> false --> false
        // "sdwsdw","sdw",3 --> true -->  true
        // "sdwsdw","sdw",4 --> false --> false
        // "sdwsdw","sdw",6 --> false --> false

        int length1 = string1.length();
        int length2 = target.length();
        if(length1 == 0){
            return startIndex == 0 && length2 == length1;
        }
        if(startIndex >= length1 || startIndex + length2 > length1 || startIndex < 0){
            return false;
        }else if(length2 == 0 && length1 > 0){
            return false; //  target = ""
        }
        for(int i = 0 ; i < length2 ; i ++){
            if(string1.charAt(i + startIndex) != target.charAt(i)){
                return false;
            }
        }
        return true;
    }

    // @Override
    // public void trigger(AnalyzeSample analyzeSample) {
    //     // TODO Auto-generated method stub
    //     String translateRevised = transProcessor.correctPlaceHolder(analyzeSample.getTranslate());
    //     analyzeSample.setTranslateRevised(translateRevised);
    //     return;
    // }



   
}
