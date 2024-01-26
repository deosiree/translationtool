package com.shr.translationtoolservice;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.dao.EntryMapper;
import com.shr.translationtoolservice.dao.IndexMapper;
import com.shr.translationtoolservice.entity.*;

import com.shr.translationtoolservice.entity.vo.ImportExcleVO;
import com.shr.translationtoolservice.util.ExcelUtils;
import com.shr.translationtoolservice.util.HTTPUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.platform.commons.util.StringUtils;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.NodeList;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.poi.sl.usermodel.PresetColor.Control;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:application.yml"})
class TranslationtoolserviceApplicationTests {

    private static String ldapURL = "ldap://10.16.2.171:389";


    private static String accountSuffix = "@sp5000.com";

    @Value("${ldap.base}")
    private String base;

    public static void parseXML() {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapURL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "Administrator" + accountSuffix);
        env.put(Context.SECURITY_CREDENTIALS, "Admin@1234");


        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put("java.naming.referral", "follow");
        try {
            // 创建LDAP连接
            DirContext ctx = new InitialDirContext(env);

            // 搜索所有的OU
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"displayName", "sAMAccountName", "cn", "ou", "distinguishedName", "mail", "employeeID", "userAccountControl"});

            NamingEnumeration<SearchResult> results = ctx.search("DC=sp5000,DC=com", "(&(objectClass=top)(objectClass=user)(objectClass=person)(objectClass=organizationalPerson))", searchControls);

            // 遍历OU结构树
            int index = 0;

            while (results.hasMoreElements()) {
                SearchResult result = results.nextElement();
                Attributes attrs = result.getAttributes();
                // String ou = attrs.get("ou").get().toString();
                String name = attrs.get("cn").get().toString();
                Attribute email = attrs.get("mail");
                String emailStr = "";
                if (!Objects.isNull(email)) {
                    emailStr = attrs.get("email").get().toString();
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


                    //CN=郑运召OU= 前置通讯组OU= 监控系统部OU= 研发中心DC=sp5000DC=com
                } else if (split.length > 3) {
                    department = split[2];
                    center = split[3].split("DC=")[0];
                    group = split[1];
                } else {
                    continue;
                }


                if (name.equals("刘爱梅")) {
                    int a = 1;
                }
                index += 1;
                if (StringUtils.isBlank(group)) {
                    System.out.println(index + ", 中心 ： " + center + ", 部门： " + department + ", 姓名:" + name);
                } else {
                    System.out.println(index + ", 中心 ： " + center + ", 部门： " + department + ", 组： " + group + ", 姓名:" + name);
                }

            }

            // 关闭LDAP连接
            ctx.close();
        } catch (NamingException e) {
            e.printStackTrace();
        }
        System.out.println("end");
    }
    public static void read(String filePath) throws IOException {
        ExcelUtils excelUtils = new ExcelUtils();
        FileInputStream inputStream =null;
        inputStream=new FileInputStream(filePath);
        List<ImportExcleVO> importExcleVOS = null;
        try {
         importExcleVOS = excelUtils.readExcelToEntity(ImportExcleVO.class, inputStream, "20240117.xls");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(" *** excel size : " + importExcleVOS.size() + " **** ");
    }

    public static void outPutExcel(EntryInfoEntity entryInfoEntity) throws IOException {

            List<OutputExcel> dataList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                OutputExcel userEntity = new OutputExcel();
                userEntity.setEntry("苹果" + i);
                userEntity.setTranslate("apple" + i);
                userEntity.setNum(i);
                userEntity.setVersion("V1.0");
                userEntity.setClassify("class");
                dataList.add(userEntity);
            }
            //生成excel文档
            Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams("词条翻译工具导出","词条数据"),
                    OutputExcel.class, dataList);
            FileOutputStream fos = new FileOutputStream("easypoi-user1.xls");
            workbook.write(fos);
            fos.close();



    }

    @Autowired
    HTTPUtils httpUtils;

    public static void main(String[] args) throws Exception {

        read("20240117.xls");


    }
}
