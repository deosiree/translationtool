package com.shr.translationtoolservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.assertj.core.util.Arrays;
import org.junit.Test;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.vo.TsVo;

/**
 *  1、需要提取的片段在列表中包含1、2组
 *  2、需要提取的片段在列表中是否在列表两头
 *  3、两段需要提取的列表是否相邻
 *  
 */
public class EntryInfoServiceImplTest {


    /*        
                 [1,2,3,4] [1,2,4,5] [1,2,4,6] [4,2,3,1] [4,2,3,5] [5,2,3,7] 
                 [1,2,3,4] [1,2,4,5] [1,2,4,6] [4,2,3,1] [4,2,3,5] √
                 [1,2,3,4] [1,2,4,5] [1,2,4,6] [3,2,12,3] [4,2,3,1] [4,2,3,5] [5,2,3,7]  √
                 [1,2,3,4] [1,2,4,5] [1,2,4,6] [3,2,12,3] [4,2,3,1] [4,2,3,5] √
                 [1,2,3,4] [1,2,4,5] [1,2,4,6] [1,2,4,7] [3,4,2,4] [5,2,3,7] √
                 [1,2,3,4] [1,2,4,5] [1,2,4,6] [1,2,4,7] [3,4,2,4] √
                 [1,2,3,4] [1,2,4,5] [1,2,4,6] [1,2,4,7]   √
        *        [1,1,1,1] [1,1,1,2] [2,3,4,1] [4,4,4,2] [4,4,4,4] √
        *        [1,1,1,1] [1,1,1,2] [2,3,2,1] [2,3,2,3] [1,2,1,3]  √
        *        [1,1,1,1] [1,1,1,1] [2,21,3,1] √
        *        [1,1,1,1] [1,2,3,1] [2,3,4,1] √
        *        [1,1,1,1] [1,1,1,1] [1,1,1,2] [1,2,1,3]  √
        *        [1,1,1,1] [1,1,1,1] [1,1,1,2] [1,1,1,3] [1,2,1,3] √
        *        [1,1,1,1] [1,1,1,1] [1,1,1,2] [1,1,1,3]  √
        *        [1,1,1,1] [1,1,1,1] [1,1,1,2] √
        *        [1,1,1,1] [1,1,1,1] [1,1,1,1] √
                 [1,1,1,1] [1,1,1,1] [1,1,1,2] [1,1,1,2] √
                [1,1,1,1] [1,1,1,1] [1,1,1,2] [1,1,1,2] [1,1,1,3] √
        *        [1,1,1,1]  [1,1,1,1]  √
        *        [1,1,1,1]   √
        */
    

    
    private List<TsVoTest> testDetectReplicate(List<TsVoTest> entryUsingTranslate) {

        int totalCount = entryUsingTranslate.size();
        List<TsVoTest> entryFiltered = new LinkedList<>();
        if(totalCount <= 0){
            return entryFiltered;
        }else{
            // int leftIndex = 0;
            int rightIndex = 0;
            // TsVo leftRefTsVo = entryUsingTranslate.get(leftIndex);
            // 翻译不同，需要把翻译不同，但source、comment、tag相同的都收集起来
            /**
             * 如果栈中没有元素，则放进去一个
             * if: peek对象的前三个值与next对象的相同
             *      if translate相同,放进去 isHasProblems = false
             *      else 放进去,isHasProblems = true
             * else: 
             *     全部出栈，放入next对象
             *          如果出栈，isHasProblems = true，则将出栈的元素都放入带搜索列表
             *          
             */
            Stack<TsVoTest> stack = new Stack<>();
            boolean isHasProblems = false;
            while(rightIndex < totalCount){
                if(stack.isEmpty()){
                    stack.push(entryUsingTranslate.get(rightIndex++));
                }else{
                    TsVoTest refTsVo = stack.firstElement();
                    TsVoTest rightTsVo = entryUsingTranslate.get(rightIndex++);
                    if(refTsVo.isEqualsNotConsiderTrans(rightTsVo)){
                        String refTranslate = refTsVo.getTranslate();
                        String rightTranslate = rightTsVo.getTranslate();
                        isHasProblems = !TsVo.isEquals(refTranslate, rightTranslate);
                        if(isHasProblems && !rightTranslate.equals(stack.lastElement().getTranslate())){
                            stack.push(rightTsVo);
                        }
                    }else{
                        if(isHasProblems){
                            entryFiltered.addAll(stack.subList(0, stack.size()));
                            isHasProblems = false;
                        }
                        stack.clear();
                        stack.push(rightTsVo);
                    }
                }
            }
            if(isHasProblems){
                entryFiltered.addAll(stack.subList(0, stack.size()));
            }
            
            // for(TsVo tsVo : entryFiltered){
            //     List<EntryInfoEntity> entry = entryInfoMapper.getEntryByTsVo(tsVo);
            //     entryInfoEntities.addAll(entry);
            // }
            return entryFiltered;
        }
    
    }

    private TsVoTest buildTsVo(String source,String tag,String comment,String translate){
        TsVoTest tsVo = new TsVoTest();
        tsVo.setComment(comment);
        tsVo.setEntry(source);
        tsVo.setTag(tag);
        tsVo.setTranslate(translate);
        return  tsVo;
    }

    public void reset(List<TsVoTest> testCases,List<TsVoTest> correctList){
        testCases.clear();
        correctList.clear();
    }

    public void executeCase(List<TsVoTest> testCases,List<TsVoTest> correctList){
        List<TsVoTest> testDetectReplicate = testDetectReplicate(testCases);
        assertIterableEquals(correctList,testDetectReplicate);
        reset(testCases, correctList);
    }

    @Test
    public void testCase1(){
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        executeCase(testCases, correctList);
    }

    @Test
    public void testCase2(){
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "1"));
        executeCase(testCases, correctList);
    }

    @Test
    public void testCase3(){
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "2"));
        
        correctList.add(buildTsVo("1", "1", "1", "1"));
        correctList.add(buildTsVo("1", "1", "1", "2"));
        
        executeCase(testCases, correctList);
    }
    
    @Test
    public void testCase4(){
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "2"));
        testCases.add(buildTsVo("1", "1", "1", "3"));

        correctList.add(buildTsVo("1", "1", "1", "1"));
        correctList.add(buildTsVo("1", "1", "1", "2"));
        correctList.add(buildTsVo("1", "1", "1", "3"));

        executeCase(testCases, correctList);
    }

    @Test
    public void testCase5(){
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "2"));
        testCases.add(buildTsVo("1", "1", "1", "3"));
        testCases.add(buildTsVo("1", "2", "1", "3"));

        correctList.add(buildTsVo("1", "1", "1", "1"));
        correctList.add(buildTsVo("1", "1", "1", "2"));
        correctList.add(buildTsVo("1", "1", "1", "3"));

        executeCase(testCases, correctList);
    }

    @Test
    public void testCase6(){
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "2"));
        testCases.add(buildTsVo("1", "2", "1", "3"));

        correctList.add(buildTsVo("1", "1", "1", "1"));
        correctList.add(buildTsVo("1", "1", "1", "2"));

        executeCase(testCases, correctList);
    }


    @Test
    public void testCase7(){
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "2", "3", "1"));
        testCases.add(buildTsVo("2", "3", "4", "1"));

        executeCase(testCases, correctList);
    }

    @Test
    public void testCase8(){
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("2", "21", "3", "1"));

        executeCase(testCases, correctList);
    }

    @Test
    public void testCase9(){
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "2"));
        testCases.add(buildTsVo("2", "3", "2", "1"));
        testCases.add(buildTsVo("2", "3", "2", "3"));
        testCases.add(buildTsVo("1", "2", "1", "3"));

        correctList.add(buildTsVo("1", "1", "1", "1"));
        correctList.add(buildTsVo("1", "1", "1", "2"));
        correctList.add(buildTsVo("2", "3", "2", "1"));
        correctList.add(buildTsVo("2", "3", "2", "3"));

        executeCase(testCases, correctList);
    }

    @Test
    public void testCase10(){
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "2"));
        testCases.add(buildTsVo("2", "3", "4", "1"));
        testCases.add(buildTsVo("4", "4", "4", "2"));
        testCases.add(buildTsVo("4", "4", "4", "4"));

        correctList.add(buildTsVo("1", "1", "1", "1"));
        correctList.add(buildTsVo("1", "1", "1", "2"));
        correctList.add(buildTsVo("4", "4", "4", "2"));
        correctList.add(buildTsVo("4", "4", "4", "4"));

        executeCase(testCases, correctList);
    }

    @Test
    public void testCase11(){
        // [1,2,3,4] [1,2,4,5] [1,2,4,6] [1,2,4,7]
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "2", "3", "4"));
        testCases.add(buildTsVo("1", "2", "4", "5"));
        testCases.add(buildTsVo("1", "2", "4", "6"));
        testCases.add(buildTsVo("1", "2", "4", "7"));

        correctList.add(buildTsVo("1", "2", "4", "5"));
        correctList.add(buildTsVo("1", "2", "4", "6"));
        correctList.add(buildTsVo("1", "2", "4", "7"));

        executeCase(testCases, correctList);
    }

    @Test
    public void testCase12(){
        // [1,2,3,4] [1,2,4,5] [1,2,4,6] [1,2,4,7] [3,4,2,4]
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "2", "3", "4"));
        testCases.add(buildTsVo("1", "2", "4", "5"));
        testCases.add(buildTsVo("1", "2", "4", "6"));
        testCases.add(buildTsVo("1", "2", "4", "7"));
        testCases.add(buildTsVo("3", "4", "2", "4"));


        correctList.add(buildTsVo("1", "2", "4", "5"));
        correctList.add(buildTsVo("1", "2", "4", "6"));
        correctList.add(buildTsVo("1", "2", "4", "7"));

        executeCase(testCases, correctList);
    }

    @Test
    public void testCase13(){
        // [1,2,3,4] [1,2,4,5] [1,2,4,6] [1,2,4,7] [3,4,2,4] [5,2,3,7]
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "2", "3", "4"));
        testCases.add(buildTsVo("1", "2", "4", "5"));
        testCases.add(buildTsVo("1", "2", "4", "6"));
        testCases.add(buildTsVo("1", "2", "4", "7"));
        testCases.add(buildTsVo("3", "4", "2", "4"));
        testCases.add(buildTsVo("5", "2", "3", "7"));

        correctList.add(buildTsVo("1", "2", "4", "5"));
        correctList.add(buildTsVo("1", "2", "4", "6"));
        correctList.add(buildTsVo("1", "2", "4", "7"));
        
        executeCase(testCases, correctList);

    }

    @Test
    public void testCase14(){
        // [1,2,3,4] [1,2,4,5] [1,2,4,6] [3,2,12,3] [4,2,3,1] [4,2,3,5]
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "2", "3", "4"));
        testCases.add(buildTsVo("1", "2", "4", "5"));
        testCases.add(buildTsVo("1", "2", "4", "6"));
        testCases.add(buildTsVo("3", "2", "12", "3"));
        testCases.add(buildTsVo("4", "2", "3", "1"));
        testCases.add(buildTsVo("4", "2", "3", "5"));

        correctList.add(buildTsVo("1", "2", "4", "5"));
        correctList.add(buildTsVo("1", "2", "4", "6"));

        correctList.add(buildTsVo("4", "2", "3", "1"));
        correctList.add(buildTsVo("4", "2", "3", "5"));

        executeCase(testCases, correctList);
    }

    @Test
    public void testCase15(){
        // [1,2,3,4] [1,2,4,5] [1,2,4,6] [3,2,12,3] [4,2,3,1] [4,2,3,5] [5,2,3,7]
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "2", "3", "4"));
        testCases.add(buildTsVo("1", "2", "4", "5"));
        testCases.add(buildTsVo("1", "2", "4", "6"));
        testCases.add(buildTsVo("3", "2", "12", "3"));
        testCases.add(buildTsVo("4", "2", "3", "1"));
        testCases.add(buildTsVo("4", "2", "3", "5"));
        testCases.add(buildTsVo("5", "2", "3", "7"));

        correctList.add(buildTsVo("1", "2", "4", "5"));
        correctList.add(buildTsVo("1", "2", "4", "6"));

        correctList.add(buildTsVo("4", "2", "3", "1"));
        correctList.add(buildTsVo("4", "2", "3", "5"));

        executeCase(testCases, correctList);

    }

    @Test
    public void testCase16(){
        // [1,2,3,4] [1,2,4,5] [1,2,4,6] [4,2,3,1] [4,2,3,5] 
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "2", "3", "4"));
        testCases.add(buildTsVo("1", "2", "4", "5"));
        testCases.add(buildTsVo("1", "2", "4", "6"));
        testCases.add(buildTsVo("4", "2", "3", "1"));
        testCases.add(buildTsVo("4", "2", "3", "5"));

        correctList.add(buildTsVo("1", "2", "4", "5"));
        correctList.add(buildTsVo("1", "2", "4", "6"));

        correctList.add(buildTsVo("4", "2", "3", "1"));
        correctList.add(buildTsVo("4", "2", "3", "5"));

        executeCase(testCases, correctList);

    }

    @Test
    public void testCase17(){
        // [1,2,3,4] [1,2,4,5] [1,2,4,6] [4,2,3,1] [4,2,3,5] [5,2,3,7] 
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "2", "3", "4"));
        testCases.add(buildTsVo("1", "2", "4", "5"));
        testCases.add(buildTsVo("1", "2", "4", "6"));
        testCases.add(buildTsVo("4", "2", "3", "1"));
        testCases.add(buildTsVo("4", "2", "3", "5"));
        testCases.add(buildTsVo("5", "2", "3", "7"));


        correctList.add(buildTsVo("1", "2", "4", "5"));
        correctList.add(buildTsVo("1", "2", "4", "6"));

        correctList.add(buildTsVo("4", "2", "3", "1"));
        correctList.add(buildTsVo("4", "2", "3", "5"));

        executeCase(testCases, correctList);

    }

    @Test
    public void testCase18(){
        // [1,1,1,1] [1,1,1,1] [1,1,1,1]
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "1"));


        executeCase(testCases, correctList);

    }

    @Test
    public void testCase19(){
        // [1,1,1,1] [1,1,1,1] [1,1,1,2] [1,1,1,2]
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "2"));
        testCases.add(buildTsVo("1", "1", "1", "2"));

        correctList.add(buildTsVo("1", "1", "1", "1"));
        correctList.add(buildTsVo("1", "1", "1", "2"));

        executeCase(testCases, correctList);

    }

    @Test
    public void testCase20(){
        // [1,1,1,1] [1,1,1,1] [1,1,1,2] [1,1,1,2] [1,1,1,3]
        List<TsVoTest> testCases = new LinkedList<>();
        List<TsVoTest> correctList = new LinkedList<>();
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "1"));
        testCases.add(buildTsVo("1", "1", "1", "2"));
        testCases.add(buildTsVo("1", "1", "1", "2"));
        testCases.add(buildTsVo("1", "1", "1", "3"));

        correctList.add(buildTsVo("1", "1", "1", "1"));
        correctList.add(buildTsVo("1", "1", "1", "2"));
        correctList.add(buildTsVo("1", "1", "1", "3"));


        executeCase(testCases, correctList);

    }

    public static class TsVoTest extends TsVo{
        @Override
        public boolean equals(Object o) {
        // TODO Auto-generated method stub
            if(!super.equals(o)){
                return false;
            }
            TsVo tsVo1 = (TsVo) o;
            if(tsVo1.isEqualsNotConsiderTrans(this)){
                return tsVo1.getTranslate().equals(this.getTranslate());
            }
            return false;
        }

    }

    // public static void main(String[] args) {

    //     EntryInfoServiceImplTest test = new EntryInfoServiceImplTest();
    //     // test.testGroupCase1();

    // }
}