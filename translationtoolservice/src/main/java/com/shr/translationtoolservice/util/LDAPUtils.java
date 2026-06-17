package com.shr.translationtoolservice.util;

import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.entity.LDAPUser;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.*;
import java.io.IOException;
import java.util.*;

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

    @Value("${ldap.user}")
    private String userName;

    @Value("${ldap.password}")
    private String password;

    private NamingEnumeration<SearchResult> results = null;

    private List<String> departmentList = null;

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
        // LDAP连接超时2秒（本地Docker无内网LDAP，快速落到兜底）
        env.put("com.sun.jndi.ldap.connect.timeout", "2000");
        env.put("com.sun.jndi.ldap.read.timeout", "2000");

//        LdapContext ctx = null;
        try {
            ctx = new InitialLdapContext(env, null);
            ctx.close();
            return true;
        } catch (Exception e) {
            log.info("LDAP认证失败，尝试本地账号...");
            // 本地兜底账号（公司外网时使用）
            if ("admin".equals(account) && "admin123".equals(password)) {
                log.info("本地账号登录成功");
                return true;
            }
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

    public List getUserKey(String name, String password) {

        log.info("需要查询的ad信息：{}", name);
        List<JSONObject> resultList = new ArrayList<>();
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapURL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, name + accountSuffix);
        env.put(Context.SECURITY_CREDENTIALS, password);


        String company = "";
        try {
            ctx = new InitialLdapContext(env, null);
            // 域节点
            String searchBase = base;
            // LDAP搜索过滤器类
            //cn=*name*模糊查询
            //cn=name 精确查询
            // String searchFilter = "(objectClass="+type+")";
            String searchFilter = "(sAMAccountName=" + name + ")";  //查询域帐号

            // 创建搜索控制器
            SearchControls searchCtls = new SearchControls();
            String returnedAtts[] = {Constant.MEMBEROF};
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
            log.info("登录人：{}", name);
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


        return resultList;
    }

    /**
     * ssl方式免证书登录
     * System.setProperty("com.sun.jndi.ldap.object.disableEndpointIdentification","true");这句很关键，将他放在启动类的main方法李
     *
     * @return
     */
    private LdapContext adLogin() {

        LdapContext ldapContext = null;
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        //验证类型
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        //用户名称，cn,ou,dc 分别：用户，组，域
        env.put(Context.SECURITY_PRINCIPAL, "username");
        //用户密码 cn 的密码
        env.put(Context.SECURITY_CREDENTIALS, "password");
        //url 格式：协议://ip:端口/组,域   ,直接连接到域或者组上面
        env.put(Context.PROVIDER_URL, "ldapurl");
        //协议
        env.put(Context.SECURITY_PROTOCOL, "ssl");
        env.put("java.naming.ldap.factory.socket", "DummySSLSocketFactory类全路径");
        //objectGUID 转换，很关键
        env.put("java.naming.ldap.attributes.binary", "objectGUID");
        try {
            Control[] sortConnCtls = new SortControl[1];
            sortConnCtls[0] = new SortControl("sAMAccountName", Control.CRITICAL);
            ldapContext = new InitialLdapContext(env, sortConnCtls);
        } catch (IOException | NamingException e) {
            log.info("登录验证失败");
            e.printStackTrace();
        }
        return ldapContext;
    }

    /**
     * 查询组织架构
     *
     * @return
     * @throws Exception
     */
    private List<JSONObject> getOU() throws Exception {

        //域部门节点
        String searchBase = "OU=,DC=test,DC=com";
        //搜索条件
        String searchFilter = "objectclass=organizationalUnit";
//        String searchFilter = "(&(objectclass=organizationalUnit)(|(name=名称1)(name=名称2)))"; //查询部门，并且部门名称等于名称1或者名称2
        // 创建搜索控制器
        SearchControls searchCtls = new SearchControls();
        // 查询组织架构的其余逻辑
        return null;
    }

    /**
     * 获取部门列表
     */
    public List<String> getDepartments() {
        log.info("getDepartments: LDAP不可用，返回空列表");
        return new ArrayList<>();
    }

    /**
     * 获取所有用户
     */
    public Map<String, List<LDAPUser>> getAllUser() {
        log.info("getAllUser: LDAP不可用，返回空Map");
        return new HashMap<>();
    }

}

