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

//        LdapContext ctx = null;
        try {
            ctx = new InitialLdapContext(env, null);
            ctx.close();
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
        String[] returnedAttrs = {"ou", "name", "canonicalName", "distinguishedName", "objectGUID", "objectCategory"};
        searchCtls.setReturningAttributes(returnedAttrs); //设置指定返回的字段，不设置则返回全部
        //  设置搜索范围 深度
        searchCtls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        //查询结果
        NamingEnumeration answer = ctx.search(searchBase, searchFilter, searchCtls);

        List<JSONObject> jsonObjectList = new ArrayList<>();

        while (answer.hasMoreElements()) {
            SearchResult searchResult = (SearchResult) answer.next();
            Attributes attributes = searchResult.getAttributes();

            if (attributes != null) {
                JSONObject jsonObject = new JSONObject();
                for (NamingEnumeration ne = attributes.getAll(); ne.hasMore(); ) {
                    Attribute attribute = (Attribute) ne.next();
                    for (NamingEnumeration e = attribute.getAll(); e.hasMore(); ) {
                        if ("objectGUID".equals(attribute.getID())) {
                            String guid = this.getObjectGUID((byte[]) e.next());
                            jsonObject.put(attribute.getID(), guid);
                        } else {
                            jsonObject.put(attribute.getID(), e.next().toString());
                        }
                        jsonObjectList.add(jsonObject);
                    }
                }
            }

        }
        return jsonObjectList;
    }

    public List<String> getDepartments() {
        List<String> departmentList = new ArrayList<>();
        try {

            Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapURL);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, userName + accountSuffix);
            env.put(Context.SECURITY_CREDENTIALS, password);

//        LdapContext ctx = null;
            ctx = new InitialLdapContext(env, null);
            // 搜索所有的OU
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);

            NamingEnumeration<SearchResult> results = ctx.search("OU=研发中心," + base, "(objectClass=organizationalUnit)", searchControls);


            // 遍历OU结构树
            while (results.hasMore()) {
                SearchResult result = results.next();
                Attributes attrs = result.getAttributes();
                String department = attrs.get("ou").get().toString();
                departmentList.add(department);
            }


        } catch (NamingException e) {
            e.printStackTrace();
        } finally {
            try {
                ctx.close();
            } catch (NamingException e) {

            }

        }
        return departmentList;
    }

    /**
     * guid转换（网上找了很多办法，这种最靠谱）
     *
     * @param GUID
     * @return
     */
    private String getObjectGUID(byte[] GUID) {
        String strGUID = "";
        String byteGUID = "";
        for (int c = 0; c < GUID.length; c++) {
            byteGUID = byteGUID + "\\" + AddLeadingZero((int) GUID[c] & 0xFF);
        }
        strGUID = strGUID + AddLeadingZero((int) GUID[3] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[2] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[1] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[0] & 0xFF);
        strGUID = strGUID + "-";
        strGUID = strGUID + AddLeadingZero((int) GUID[5] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[4] & 0xFF);
        strGUID = strGUID + "-";
        strGUID = strGUID + AddLeadingZero((int) GUID[7] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[6] & 0xFF);
        strGUID = strGUID + "-";
        strGUID = strGUID + AddLeadingZero((int) GUID[8] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[9] & 0xFF);
        strGUID = strGUID + "-";
        strGUID = strGUID + AddLeadingZero((int) GUID[10] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[11] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[12] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[13] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[14] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[15] & 0xFF);
        return strGUID;
    }

    private static String AddLeadingZero(int k) {
        return (k <= 0xF) ? "0" + Integer.toHexString(k) : Integer
                .toHexString(k);
    }

    /**
     * 组织架构新增
     *
     * @throws Exception
     */
    private void addOU() throws Exception {

        LdapContext ldapContext = this.adLogin();

        String distinguishedName = "ou=新增部门名称,OU=顶级跟目录,DC=test,DC=com";

        //新增部门
        Attributes attributes = new BasicAttributes();
        //类名
        attributes.put("objectClass", "organizationalUnit");
        //显示名称
        attributes.put("distinguishedName", distinguishedName);
        attributes.put("ou", "新增部门名称");

        //添加
        ldapContext.createSubcontext(distinguishedName, attributes);
    }

    /**
     * 组织架构修改
     *
     * @throws Exception
     */
    private void updateOU() throws Exception {

        LdapContext ldapContext = this.adLogin();
        String oldName = "ou=旧组织架构名称,OU=顶级跟目录,DC=test,DC=com";
        String newName = "ou=新组织架构名称,OU=顶级跟目录,DC=test,DC=com";

        try {
            ldapContext.rename(oldName, newName);
            log.info("修改成功！");
        } catch (Exception e) {
            log.info("修改失败！");
            e.printStackTrace();
        } finally {
            try {
                ldapContext.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 组织架构删除
     *
     * @throws Exception
     */
    private void deleteOU() throws Exception {

        LdapContext ldapContext = this.adLogin();
        String name = "ou=新组织架构名称,OU=顶级跟目录,DC=test,DC=com";

        try {
            ldapContext.destroySubcontext(name);
            log.info("删除成功！");
        } catch (Exception e) {
            log.info("删除失败！");
            e.printStackTrace();
        } finally {
            try {
                ldapContext.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    //获取ldap 所有用户信息
    //返回 key -> department ， value -> ldapuser
    public Map<String, List<LDAPUser>> getAllUser() {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapURL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, userName + accountSuffix);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put("java.naming.referral", "follow");
        env.put(Context.REFERRAL, "ignore");

        //反参
        List<LDAPUser> ldapUsers = new ArrayList<>();
        //key -> department , value -> map<department,ldapUser>

        Map<String, List<LDAPUser>> ldapUserMap = new HashMap<>();
        try {
            // 创建LDAP连接
            LdapContext ctx = new InitialLdapContext(env, null);
            int pageSize = 1000;//程序单次查询最大条数
            ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, Control.CRITICAL)});

            byte[] cookie = null;//用于判断是否还有剩余数据（进行分页）

            // 搜索所有的OU
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"displayName", "sAMAccountName", "cn", "ou", "distinguishedName", "mail", "employeeID", "userAccountControl"});
            do {
                NamingEnumeration<SearchResult> results = ctx.search("DC=sp5000,DC=com", "(&(objectClass=top)(objectClass=user)(objectClass=person)(objectClass=organizationalPerson))", searchControls);


                // 遍历OU结构树
                int index = 0;

                while (results != null && results.hasMoreElements()) {

                    LDAPUser ldapUser = new LDAPUser();
                    SearchResult result = results.nextElement();

                    Attributes attrs = result.getAttributes();
                    // String ou = attrs.get("ou").get().toString();
                    String name = attrs.get("cn").get().toString();
                    ldapUser.setName(name);
                    Attribute email = attrs.get("mail");
                    String emailStr = "";
                    if (!Objects.isNull(email)) {
                        emailStr = attrs.get("mail").get().toString();
                        ldapUser.setEmail(emailStr);
                    }
                    String distinguishedname = attrs.get("distinguishedname").get().toString();
                    if (!distinguishedname.contains("OU=")) {
                        continue;
                    }
                    distinguishedname = distinguishedname.replace(",", "");

                    String[] split = distinguishedname.split("OU=");
                    String department = "";
                    String center = "";
                    String group = "";
                    //CN=郑运召OU= 监控系统部OU= 研发中心DC=sp5000DC=com
                    if (split.length == 3) {

                        department = split[1];


                        center = split[2].split("DC=")[0];
                        ldapUser.setDepartment(department);
                        ldapUser.setCenter(center);

                        //CN=郑运召OU= 前置通讯组OU= 监控系统部OU= 研发中心DC=sp5000DC=com
                    } else if (split.length > 3) {
                        department = split[2];
                        center = split[3].split("DC=")[0];
                        group = split[1];
                        ldapUser.setDepartment(department);
                        ldapUser.setCenter(center);
                        ldapUser.setGroup(group);
                    } else {
                        continue;
                    }

                    // ldapUsers.add(ldapUser);

                    //map 插入元素
                    if (Objects.isNull(ldapUserMap.get(department))) {
                        List<LDAPUser> ldapUserList = new ArrayList<>();
                        ldapUserList.add(ldapUser);

                        ldapUserMap.put(department, ldapUserList);
                    } else {
                        ldapUserMap.get(department).add(ldapUser);
                    }


            /*    index += 1;
                if (StringUtils.isBlank(group)){
                    System.out.println(index + ", 中心 ： " + center + ", 部门： " + department + ", 姓名:" + cn);
                }else {
                    System.out.println(index + ", 中心 ： " + center + ", 部门： " + department + ", 组： " + group + ", 姓名:" + cn);
                }*/

                }
                //读取cookie，判断是否有未读取完
                Control[] controls = ctx.getResponseControls();
                if (controls != null) {
                    for (int i = 0; i < controls.length; i++) {
                        if (controls[i] instanceof PagedResultsResponseControl) {
                            PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
                            cookie = prrc.getCookie();
                        }
                    }
                }

                // 将cookie提供给LdapContext，让它在接下来的查询中进行换页
                ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, cookie, Control.CRITICAL)});

            } while (cookie != null);


            // 关闭LDAP连接
            ctx.close();
        } catch (NamingException | IOException e) {
            e.printStackTrace();
        }
        return ldapUserMap;
    }


}
