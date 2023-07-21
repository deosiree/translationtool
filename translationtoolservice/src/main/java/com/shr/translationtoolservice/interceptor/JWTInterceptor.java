package com.shr.translationtoolservice.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.common.PassToken;
import com.shr.translationtoolservice.common.Result;
import com.shr.translationtoolservice.common.ResultCode;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Slf4j
public class JWTInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        //获取请求头中的令牌
        String token = request.getHeader(Constant.TOKEN);
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        //检查是否有通过PassToken注解
        if (method.isAnnotationPresent(PassToken.class)) {
            //如果有则跳过认证检查
            PassToken passToken = method.getAnnotation(PassToken.class);
            if (passToken.required()) {
                return true;
            }
        }
        //否则验证token
        if (null != token){
            Boolean verify = JWTTokenUtils.verify(token);
            if (verify){
                //获取访问的接口名称
                String requestURI = request.getRequestURI();
                //判断当前用户是否有该接口的访问权限 TODO ： 查权限表URL
                if (JWTTokenUtils.getAuthority(token).contains(requestURI)){
                    return true;
                }
                //响应到前台
                String json = new ObjectMapper().writeValueAsString(Result.fail(ResultCode.PERMISSION.getCode(),ResultCode.PERMISSION.getMessage()));
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().println(json);
                return false;
            }
        }
        //响应到前台
        String json = new ObjectMapper().writeValueAsString(Result.fail(ResultCode.LOGIN_EXPIRED.getCode(),ResultCode.LOGIN_EXPIRED.getMessage()));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().println(json);
        return false;
    }
}
