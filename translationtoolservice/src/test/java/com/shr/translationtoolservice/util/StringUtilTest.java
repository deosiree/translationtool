package com.shr.translationtoolservice.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;


public class StringUtilTest {


    public static void testAddEscapeCharacter(){
        String[] testCaseGroup1 = {"%","\\","\\%","%%","%%%","%%\\","%\\%"};
        String[] testCaseGroup2 = {"a%","%a","a%b","a%%","%%a","a%%%","%%%a","a%%%b"};
        String[] testCaseGroup3 = {"\\a%","\\%a","\\a%b","\\a%%","\\%%a","\\a%%%","\\%%%a","\\a%%%b"};
        String[] testCaseGroup4 = {"a%\\","%a\\","a%b\\","a%%\\","%%a\\","a%%%\\","%%%a\\","a%%%b\\"};
        String[] testCaseGroup5 = {"a\\%","%\\a","a\\%b","a%\\b","a\\%%","a%\\%","%\\%a","%%\\a","a\\%%%","a%\\%%","a%%\\%",
        "%\\%%a","%%\\%a","%%%\\a","a\\%%%b","a%\\%%b","a%%\\%b","a%%%\\b"};
        String[] testCaseGroup6 = {"sdv","sda","dv",""};
    
        // 末尾是\需要注意,%xxx\%，意思不对，本来是找****xxx\****，但现在找的是******xxx*******
        String[] groupResult1 = {"\\%","\\\\","\\\\\\%","\\%\\%","\\%\\%\\%","\\%\\%\\\\","\\%\\\\\\%"};
        String[] groupResult2 = {"a\\%","\\%a","a\\%b","a\\%\\%","\\%\\%a","a\\%\\%\\%","\\%\\%\\%a","a\\%\\%\\%b"};
        String[] groupResult3 = {"\\\\a\\%","\\\\\\%a","\\\\a\\%b","\\\\a\\%\\%","\\\\\\%\\%a","\\\\a\\%\\%\\%","\\\\\\%\\%\\%a","\\\\a\\%\\%\\%b"};
        String[] groupResult4 = {"a\\%\\\\","\\%a\\\\","a\\%b\\\\","a\\%\\%\\\\","\\%\\%a\\\\","a\\%\\%\\%\\\\","\\%\\%\\%a\\\\","a\\%\\%\\%b\\\\"};


        String[] groupResult5 = {"a\\\\\\%","\\%\\\\a","a\\\\\\%b","a\\%\\\\b","a\\\\\\%\\%","a\\%\\\\\\%","\\%\\\\\\%a",
        "\\%\\%\\\\a","a\\\\\\%\\%\\%","a\\%\\\\\\%\\%","a\\%\\%\\\\\\%",
        "\\%\\\\\\%\\%a","\\%\\%\\\\\\%a","\\%\\%\\%\\\\a","a\\\\\\%\\%\\%b","a\\%\\\\\\%\\%b","a\\%\\%\\\\\\%b","a\\%\\%\\%\\\\b"};
        String[] groupResult6 = {"sdv","sda","dv",""};
        // %\\\\% -> 相当于四个\才等于实际的一个\ 
        testGroup(testCaseGroup1, groupResult1);
        testGroup(testCaseGroup2, groupResult2);
        testGroup(testCaseGroup3, groupResult3);
        testGroup(testCaseGroup4, groupResult4);
        testGroup(testCaseGroup5, groupResult5);
        testGroup(testCaseGroup6, groupResult6);


    }

    public static void testGroup(String[] cases,String[] results){
        if(cases.length != results.length){
            throw new RuntimeException("cases和results的列表长度不相同");
        }
        int length = cases.length;
        for(int i = 0 ; i < length ; i ++ ){
            assertEquals(StringUtil.addEscapeCharacter(cases[i]),results[i]);
        }
        return;

    }

    public static void main(String[] args) {
        StringUtilTest.testAddEscapeCharacter();
    }
    
}
