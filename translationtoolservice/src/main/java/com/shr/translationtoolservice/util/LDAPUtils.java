package com.shr.translationtoolservice.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.common.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * ldap 登录鉴权
 */
@Component
@Slf4j
public class LDAPUtils {

    @Value("${ldap.ldapURL}")
    private String ldapURL;

    @Value("${ldap.accountSuffix}")
    private String accountSuffix;

    @Value("${ldap.base}")
    private String base;

    private static LdapContext ctx = null;

    public Boolean LDAP_AUTH_AD(String account, String password) {
        if (account.isEmpty() || password.isEmpty()) {
            log.info("用户名或密码为空，登录失败");
            return false;
        }
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapURL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, account + accountSuffix);
        env.put(Context.SECURITY_CREDENTIALS, password);

//        LdapContext ctx = null;
        try {
            ctx = new InitialLdapContext(env, null);
            return true;
        } catch (Exception e) {
            log.info("认证失败！");
            return false;
        } finally {
            /*if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                }
            }*/
        }
    }

    public List getUserKey(String name) {

        log.info("需要查询的ad信息：{}", name);
        List<JSONObject> resultList = new ArrayList<>();
        if (ctx != null) {
            String company = "";
            try {
                // 域节点
                String searchBase = base;
                // LDAP搜索过滤器类
                //cn=*name*模糊查询
                //cn=name 精确查询
                // String searchFilter = "(objectClass="+type+")";
                String searchFilter = "(sAMAccountName=" + name + ")";  //查询域帐号

                // 创建搜索控制器
                SearchControls searchCtls = new SearchControls();
                String returnedAtts[]={Constant.MEMBEROF};
                searchCtls.setReturningAttributes(returnedAtts); //设置指定返回的字段，不设置则返回全部
                // 设置搜索范围 深度
                searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                // 根据设置的域节点、过滤器类和搜索控制器搜索LDAP得到结果
                NamingEnumeration answer = ctx.search(searchBase, searchFilter, searchCtls);
                // 初始化搜索结果数为0
                int totalResults = 0;
                int rows = 0;
                while (answer.hasMoreElements()) {// 遍历结果集
                    SearchResult sr = (SearchResult) answer.next();// 得到符合搜索条件的DN
                    ++rows;
                    String dn = sr.getName();
                    log.info(dn);
                    Attributes Attrs = sr.getAttributes();// 得到符合条件的属性集
                    if (Attrs != null) {
                        try {
                            for (NamingEnumeration ne = Attrs.getAll(); ne.hasMore(); ) {
                                Attribute Attr = (Attribute) ne.next();// 得到下一个属性
                                // 读取属性值
                                for (NamingEnumeration e = Attr.getAll(); e.hasMore(); totalResults++) {
                                    company = e.next().toString();
                                    JSONObject tempJson = new JSONObject();
                                    tempJson.put(Attr.getID(), company.toString());
                                    resultList.add(tempJson);
                                }
                            }
                        } catch (NamingException e) {
                            log.info("Throw Exception : " + e.getMessage());
                        }
                    }
                }
                log.info("登录人：{}",name);
            } catch (NamingException e) {
                log.info("Throw Exception : " + e.getMessage());
            } finally {
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (NamingException e) {
                    }
                }
            }
        }

        return resultList;
    }

}
