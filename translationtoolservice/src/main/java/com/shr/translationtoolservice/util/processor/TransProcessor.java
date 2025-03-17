package com.shr.translationtoolservice.util.processor;

public interface TransProcessor {

    /**
     * 百度翻译等翻译软件在翻译有占位符的文字时存在问题，例如将{:d}翻译成{: d}
     * 该方法的目的在于校正翻译结果，将{: d}校正为{:d}
     * @param text
     * @return
     */
    String correctPlaceHolder(String text);
    

    
}
