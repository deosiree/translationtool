package com.shr.translationtoolservice.service.analyze;


/**
 * 方法都是具体的{@code TranslateAnalyzer}的实现内部执行的方法一般不对外开放
 * 对翻译结果进行分析
 */
public interface TranslateAnalyzer<T> {

    /**
     * 有问题是true,该方法仅分析翻译后的结果是否存在问题，不执行{@code trigger}方法
     * @param analyzeSample
     * @return
     */
    boolean analyze(T analyzeSample);

    // /**
    //  * 会对翻译的结果进行修正
    //  * @param analyzeSample
    //  */
    // void trigger(T analyzeSample);

    

}
