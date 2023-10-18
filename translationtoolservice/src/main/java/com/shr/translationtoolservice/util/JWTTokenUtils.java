package com.shr.translationtoolservice.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.entity.User;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class JWTTokenUtils {
    private static String SECRET = "token!Q@W#E$R";
    //token到期时间24小时
    private static final long EXPIRE_TIME= 24*60*60*1000;//1天

    /**
     * 生产token
     */
    public static String createToken(Map<String,String> map) {
        JWTCreator.Builder builder = JWT.create();

        //payload   将用户信息放到令牌里面
        map.forEach((k, v) -> {
            builder.withClaim(k, v);
        });

        Date expireAt=new Date(System.currentTimeMillis()+EXPIRE_TIME);
        builder.withExpiresAt(expireAt);//指定令牌的过期时间
        String token = builder.sign(Algorithm.HMAC256(SECRET));//签名
        return token;
    }

    public static String createToken(Map<String,String> map,List<String> list) {
        JWTCreator.Builder builder = JWT.create();

        //payload   将用户信息放到令牌里面
        map.forEach((k, v) -> {
            builder.withClaim(k, v);
        });
        builder.withClaim(Constant.AUTHORITY,list);
        Date expireAt=new Date(System.currentTimeMillis()+EXPIRE_TIME);
        builder.withExpiresAt(expireAt);//指定令牌的过期时间
        String token = builder.sign(Algorithm.HMAC256(SECRET));//签名
        return token;
    }

    /**
     * 验证token
     */
    public static Boolean verify(String token) {
        try {
            //如果有任何验证异常，此处都会抛出异常
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(SECRET)).build().verify(token);
            return true;
        }catch (Exception e){
            log.info("token verify error : {}",e.getMessage());
            return false;
        }
    }

    /**
     * 获取token中的 payload
     * @param token
     * @return
     */
    public static DecodedJWT getToken(String token) {
        DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(SECRET)).build().verify(token);
        return decodedJWT;
    }

    /**
     * 获取用户名
     * @param token
     * @return
     */
    public static String getUserName(String token) {
        String userName = getToken(token).getClaim(Constant.USER_NAME).asString();
        return userName;
    }

    /**
     * 获取用户所属部门
     * @param token
     * @return
     */
    public static String getDepartment(String token) {
        String department = getToken(token).getClaim(Constant.DEPARTMENT).asString();
        return department;
    }

    /**
     * 获取用户操作权限列表
     * @param token
     * @return
     */
    public static List<String> getAuthority(String token) {
        List<String> list = getToken(token).getClaim(Constant.AUTHORITY).asList(String.class);
        return list;
    }





}
