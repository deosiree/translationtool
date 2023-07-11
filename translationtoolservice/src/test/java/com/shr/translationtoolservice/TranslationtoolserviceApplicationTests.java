package com.shr.translationtoolservice;

import com.shr.translationtoolservice.util.GoogleTranslate;
import com.shr.translationtoolservice.util.Translate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@SpringBootTest
class TranslationtoolserviceApplicationTests {

    @Test
    void contextLoads() {

    }

    public static void main(String[] args) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        try {
            Date date1 =  formatter.parse(formatter.format(new Date(System.currentTimeMillis())));

            Date date = new Date(System.currentTimeMillis());
            int a =0;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Translate translate = new Translate();
        GoogleTranslate googleTranslate = new GoogleTranslate();
       // googleTranslate.translateText("苹果"，)
        translate.getTranslateResult("苹果","auto","ru");
    }
}
