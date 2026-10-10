package com.shr.translationtoolservice.service.analyze;

import com.shr.translationtoolservice.service.analyze.AbstractTranslateAnaylzer.AnalyzeSampleBuilder;

public abstract class AbstractTranslateAnaylzer<B extends AnalyzeSampleBuilder<T>,T extends AnalyzeSample>{


    protected B builder;

    public AbstractTranslateAnaylzer(){
        this.builder = builder();
    }

    public static abstract class AnalyzeSampleBuilder<T>{

        public abstract T prepare(String entry,String translate);
    }


    protected abstract B builder();

    public abstract AnalyzeSample analyze(T analyzeSample);

    public T prepare(String entry,String translate){
        return builder.prepare(entry, translate);
    }

    // public abstract void trigger(T analyzeSample);

    
    
}
