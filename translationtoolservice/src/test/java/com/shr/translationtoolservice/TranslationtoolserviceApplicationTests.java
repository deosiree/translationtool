package com.shr.translationtoolservice;

import com.shr.translationtoolservice.dao.EntryMapper;
import com.shr.translationtoolservice.dao.IndexMapper;
import com.shr.translationtoolservice.entity.EntryEntity;
import com.shr.translationtoolservice.entity.Index;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:application.yml"})

class TranslationtoolserviceApplicationTests {


    public static void main(String[] args) throws Exception {

        JedisPool jedisPool = new JedisPool();
        Jedis jedis = jedisPool.getResource();
        String ping = jedis.ping();
        System.out.println(ping);
        jedis.close();




    }
}
