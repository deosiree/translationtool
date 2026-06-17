package com.shr.translationtoolservice.service.analyze;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.shr.translationtoolservice.service.analyze.BaiduTranslateAnalyzer.BaiduSampleBuilder;
import com.shr.translationtoolservice.service.analyze.BaiduTranslateAnalyzer.BaiduTranslateAnalyzeSample;
import com.shr.translationtoolservice.service.analyze.DefaultTranslateAnalyzer.DefaultTranslateAnalyzeSample;

@Component
public class BaiduTranslateAnalyzer extends AbstractTranslateAnaylzer<BaiduSampleBuilder,BaiduTranslateAnalyzeSample>{

    @Autowired
    private DefaultTranslateAnalyzer defaultTranslateAnalyzer;

    public static class BaiduTranslateAnalyzeSample extends DefaultTranslateAnalyzeSample{
    
        public BaiduTranslateAnalyzeSample(String entry, String translate) {
            super(entry, translate);
            //TODO Auto-generated constructor stub
        }

    }

    public static class BaiduSampleBuilder extends AbstractTranslateAnaylzer.AnalyzeSampleBuilder<BaiduTranslateAnalyzeSample>{

        @Override
        public BaiduTranslateAnalyzeSample prepare(String entry, String translate) {
            // TODO Auto-generated method stub
            return new BaiduTranslateAnalyzeSample(entry, translate);
        }

    }


    public BaiduTranslateAnalyzeSample prepare(String entry,String translate){
        return builder.prepare(entry, translate);
    }


    @Override
    protected BaiduSampleBuilder builder() {
        // TODO Auto-generated method stub
        return new BaiduSampleBuilder();
    }



    @Override
    public AnalyzeSample analyze(BaiduTranslateAnalyzeSample analyzeSample) {
        // TODO Auto-generated method stub
        boolean isPlaceHolderHasProblems = this.defaultTranslateAnalyzer.placeHolderAnalyzer.analyze(analyzeSample);
        analyzeSample.setPlaceHolderHasProblems(isPlaceHolderHasProblems);

        analyzeSample.setBad(isPlaceHolderHasProblems && true);
        return analyzeSample;
        // throw new UnsupportedOperationException("Unimplemented method 'analyze'");
    }


    // @Override
    // public void trigger(BaiduTranslateAnalyzeSample analyzeSample) {
    //     // TODO Auto-generated method stub
    //     defaultTranslateAnalyzer.trigger(analyzeSample);
    // }




    // @Override
    // public BaiduTranslateAnalyzeSample afterTranslate(BaiduTranslateAnalyzeSample analyzeSample) {
    //     /*
    //      * analyzer1.analyze -----> analyzer2.analyze ----> analyzern.analyze ----> return
    //      */
    //     boolean isPlaceHolderHasProblems = placeHolderAnalyzer.analyze(analyzeSample);
    //     analyzeSample.setPlaceHolderHasProblems(isPlaceHolderHasProblems);

    //     analyzeSample.setBad(isPlaceHolderHasProblems && true);
    //     return analyzeSample;
        
    // }


    // @Override
    // public BaiduTranslateAnalyzeSample beforeTranslate(BaiduTranslateAnalyzeSample analyzeSample) {
    //     // TODO Auto-generated method stub
    //      throw new UnsupportedOperationException("Unimplemented method 'beforeTranslate'");
    // }
    


    // public void triggerPlaceHolder(BaiduTranslateAnalyzeSample analyzeSample){
        
    //     placeHolderAnalyzer.trigger(analyzeSample);
        
    //     return;
    // }

    
}
