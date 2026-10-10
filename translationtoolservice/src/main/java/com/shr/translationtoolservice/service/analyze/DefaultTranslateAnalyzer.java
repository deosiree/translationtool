package com.shr.translationtoolservice.service.analyze;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.shr.translationtoolservice.service.analyze.DefaultTranslateAnalyzer.DefaultSampleBuilder;
import com.shr.translationtoolservice.service.analyze.DefaultTranslateAnalyzer.DefaultTranslateAnalyzeSample;;

@Component
public class DefaultTranslateAnalyzer extends AbstractTranslateAnaylzer<DefaultSampleBuilder,DefaultTranslateAnalyzeSample> {
   
    @Autowired
    protected PlaceHolderAnalyzer placeHolderAnalyzer;

    @Autowired
    protected PercentageAnalyzer percentageAnalyzer;
    
    public static class DefaultTranslateAnalyzeSample extends AnalyzeSample{

        private boolean isPlaceHolderHasProblems;

        private boolean isPercentageHasProblems;

        public DefaultTranslateAnalyzeSample(String entry, String translate) {
            super(entry, translate);
            //TODO Auto-generated constructor stub
        }

        public boolean isPlaceHolderHasProblems() {
            return isPlaceHolderHasProblems;
        }

        public void setPlaceHolderHasProblems(boolean isPlaceHolderHasProblems) {
            this.isPlaceHolderHasProblems = isPlaceHolderHasProblems;
        }

        public boolean isPercentageHasProblems() {
            return isPercentageHasProblems;
        }

        public void setPercentageHasProblems(boolean isPercentageHasProblems) {
            this.isPercentageHasProblems = isPercentageHasProblems;
        }
    }

    public static class DefaultSampleBuilder extends AbstractTranslateAnaylzer.AnalyzeSampleBuilder<DefaultTranslateAnalyzeSample>{

        @Override
        public DefaultTranslateAnalyzeSample prepare(String entry, String translate) {
            // TODO Auto-generated method stub
            return new DefaultTranslateAnalyzeSample(entry, translate);
        }
    }

    @Override
    public DefaultSampleBuilder builder() {
        // TODO Auto-generated method stub
        return new DefaultSampleBuilder();
    }
    
    public PlaceHolderAnalyzer getPlaceHolderAnalyzer() {
        return placeHolderAnalyzer;
    }


    @Override
    public AnalyzeSample analyze(DefaultTranslateAnalyzeSample analyzeSample) {
        // TODO Auto-generated method stub
        boolean isPlaceHolderHasProblems = placeHolderAnalyzer.analyze(analyzeSample);
        analyzeSample.setPlaceHolderHasProblems(isPlaceHolderHasProblems);
        boolean isPercentageHasProblems = percentageAnalyzer.analyze(analyzeSample);
        analyzeSample.setPercentageHasProblems(isPercentageHasProblems);

        analyzeSample.setBad(isPlaceHolderHasProblems || isPercentageHasProblems);
        return analyzeSample;
    }

    // /**
    //  * 根据{@code analyzeSample}的结果，利用不同的{@link TranslateAnalyzer}的{@code trigger}方法对翻译结果进行校正
    //  * 校正后的结果放到{@code translateRevised}属性中
    //  * 
    //  * 由于多个分析器分析的结果可能都显示需要校正，不同{@link TranslateAnalyzer}执行trigger方法可能会存在冲突，所以需要
    //  * 考虑{@link TranslateAnalyzer}执行trigger方法的顺序
    //  * @param analyzeSample
    //  */
    // @Override
    // public void trigger(DefaultTranslateAnalyzeSample analyzeSample){
    //     if(!analyzeSample.isBad()){
    //         return;
    //     }
    //     if(analyzeSample.isPlaceHolderHasProblems()){
    //         placeHolderAnalyzer.trigger(analyzeSample);
    //     }

    //     return;
    // }




}
