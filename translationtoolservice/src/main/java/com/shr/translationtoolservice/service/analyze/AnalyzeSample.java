package com.shr.translationtoolservice.service.analyze;

public class AnalyzeSample {

    private String entry;

    private String translate;

    private boolean isBad;

    private String translateRevised;


    public AnalyzeSample(String entry, String translate) {
        this.entry = entry;
        this.translate = translate;
    }

    public String getEntry() {
        return entry;
    }

    public String getTranslate() {
        return translate;
    }

    public void setBad(boolean isBad) {
        this.isBad = isBad;
    }

    public boolean isBad() {
        return isBad;
    }

    public String getTranslateRevised() {
        return translateRevised;
    }

    public void setTranslateRevised(String translateRevised) {
        this.translateRevised = translateRevised;
    }
}
