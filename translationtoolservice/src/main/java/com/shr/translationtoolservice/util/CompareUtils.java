package com.shr.translationtoolservice.util;

import com.shr.translationtoolservice.common.AnjiDescription;
import com.shr.translationtoolservice.dao.EntryClassifyMapper;
import com.shr.translationtoolservice.entity.ComparisonResult;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @ClassName CompareUtils
 * @Description 类比较器
 * @USER: Cola
 * @Date 2023/7/4 0004 16:51
 **/

@Component
public class CompareUtils {

    @Autowired
    EntryClassifyMapper entryClassifyMapper;
    private final static String DATE_FORMATTR_SECOND = "yyyy-MM-dd HH:mm:ss";

    /**
     * 比较两个实体属性值，返回一有差异的属性
     *
     * @param obj1 进行属性比较的对象1
     * @param obj2 进行属性比较的对象2
     * @return
     */
    public static List<ComparisonResult> compareFields(Object obj1, Object obj2, Class clazz) throws Exception {
        try {
            List<String> ignoreList = getCompareFields(clazz);
            Map<String, String> descsMap = getFieldSwaggerValue(clazz);
            List<ComparisonResult> list = new ArrayList<>();

            // 只有两个对象都是同一类型的才有可比性
            if (obj1.getClass() == obj2.getClass()) {
                Class claz = obj1.getClass();
                // 获取object的属性描述
                PropertyDescriptor[] pds = Introspector.getBeanInfo(claz, Object.class).getPropertyDescriptors();
                for (PropertyDescriptor pd : pds) {
                    String name = pd.getName();
                    //String label = descsMap.get(name);
                    // TODO !ignoreList.contains(name)
                    if (ignoreList != null && ignoreList.contains(name)) {
                        continue;
                    }
                    Method readMethod = pd.getReadMethod();
                    Object o1 = readMethod.invoke(obj1);
                    Object o2 = readMethod.invoke(obj2);
                    String r1 = null, r2 = null;
                    if (Objects.isNull(o1) && Objects.isNull(o2)) {
                        continue;
                    }
                    if (null == o1 || null == o2) {
                        setResult(o1, o2, name, list);
                        continue;
                    }
                    if (compareField(o1, o2)) {
                        setResult(o1, o2, name, list);
                    }
                }
            } else {
                throw new Exception("对象类型不一致");
            }
            return list;
        } catch (Exception e) {
            throw new Exception("对象比较失败", e);
        }
    }

    /**
     * 获取比较的类中字段
     *
     * @param objectClass
     * @return
     */
    private static List<String> getCompareFields(Class objectClass) {
        Field[] fields = objectClass.getDeclaredFields();
        List<String> map = new ArrayList<>();
        for (Field f : fields) {
            boolean annotationPresent2 = f.isAnnotationPresent(AnjiDescription.class);
            if (annotationPresent2) {
                map.add(f.getName());
            }
        }
        return map;
    }

    /**
     * 获取实体类字段的description
     *
     * @param clazz
     * @return
     */
    private static Map<String, String> getFieldSwaggerValue(Class clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Map<String, String> map = new HashMap<>();
        for (Field f : fields) {
            boolean annotationPresent2 = f.isAnnotationPresent(AnjiDescription.class);
            if (annotationPresent2) {
                AnjiDescription name = f.getAnnotation(AnjiDescription.class);
                String nameStr = name.value();
                map.put(f.getName(), nameStr);
            }
        }
        return map;
    }

    private static void setResult(Object o1, Object o2, String name, List<ComparisonResult> list) {
        Object ot = null;
        String r1 = null, r2 = null;
        if (!Objects.isNull(o1)) {
            ot = o1;
        } else if (!Objects.isNull(o2)) {
            ot = o2;
        }
        if (ot instanceof String) {
            r1 = objectToString(o1);
            r2 = objectToString(o2);
        } else if (ot instanceof BigDecimal) {
            System.out.println("BigDecimal");
            r1 = objectToString(o1);
            r2 = objectToString(o2);
        } else if (ot instanceof Integer) {
            System.out.println("Integer");
            r1 = objectToString(o1);
            r2 = objectToString(o2);
        } else if (ot instanceof Boolean) {
            System.out.println("Boolean");
            r1 = booleanToString(o1);
            r2 = booleanToString(o2);
        } else if (ot instanceof Long) {
            System.out.println("Long");
            r1 = objectToString(o1);
            r2 = objectToString(o2);
        } else if (ot instanceof LocalDateTime) {
            System.out.println("LocalDateTime");
            r1 = localDateTimeToString(o1);
            r2 = localDateTimeToString(o2);
        }else if (ot instanceof Date) {
            System.out.println("LocalDateTime");
            r1 = localDateTimeToString(o1);
            r2 = localDateTimeToString(o2);
        }
        ComparisonResult result = new ComparisonResult();
        //更新时间跳过
      /*  if (name.equals("update") || name.equals("updateTime")){
            return;
        }*/
    /*    if ("update".equals(name) || "updateTime".equals(name) ||  "entryLength".equals(name)  ||  "classifyId".equals(name)
                || "englishLength".equals(name) || "englishTranslateState".equals(name) || "englishDisable".equals(name) || "englishDisableLength".equals(name)
                || "russianLength".equals(name) || "russianhTranslateState".equals(name) || "russianDisable".equals(name) || "russianDisableLength".equals(name)
                || "spanishLength".equals(name) || "spanishhTranslateState".equals(name) || "spanishDisable".equals(name) || "spanishDisableLength".equals(name)
                || "frenchLength".equals(name) || "frenchhTranslateState".equals(name) || "frenchDisable".equals(name) || "frenchDisableLength".equals(name)

        ){
            return;
        }
        HashMap<String, String> entryName = constructEntryName();*/

        result.setKey(name);
        result.setPrevious(r1);
        result.setLater(r2);
        String str = "";

     /*   if (StringUtils.isBlank(r1)){
            str = entryName.get(name) + " 新增值为： " + r2 ;
        }else {
            str = entryName.get(name) + " 值由 ( " + r1 + " ) 改为 ( " + r2 + " )  " ;
        }*/

        result.setStr(str);
        list.add(result);

    }
    private static HashMap<String, String> constructEntryName(){
        HashMap<String, String> entryName = new HashMap<>();
        entryName.put("entry","词条");
        entryName.put("abbr","abbr");
        entryName.put("chineseInterpretation","中文释义");
        entryName.put("englishInterpretation","英文释义");
        entryName.put("entrySource","词条来源");
        entryName.put("entryState","词条状态");
        entryName.put("creator","创建人");
        entryName.put("createTime","创建时间");
        entryName.put("update","修改人");
        entryName.put("updateTime","修改时间");
        entryName.put("version","版本");
        entryName.put("isLatestVersion","是否最新版本");
        entryName.put("entryLabel","词条标签");
        entryName.put("partOfSpeech","词性备注");
        entryName.put("classifyId","词条所属分类");
        entryName.put("repeatEntryId","重复词条id");
        entryName.put("english","英文翻译");
        entryName.put("russian","俄文翻译");
        entryName.put("spanish","西文翻译");
        entryName.put("french","法文翻译");

        return entryName;
    }

    private static boolean compareField(Object o1, Object o2) {
        Boolean sign = false;
        Object ot = null;
        if (!Objects.isNull(o1)) {
            ot = o1;
        } else if (!Objects.isNull(o2)) {
            ot = o2;
        }
        if (ot instanceof String) {
            sign = !o1.equals(o2);
        } else if (ot instanceof BigDecimal) {
            System.out.println("BigDecimal");
            sign = ((BigDecimal) o1).compareTo((BigDecimal) o2) != 0 ? true : false;
        } else if (ot instanceof Integer) {
            System.out.println("Integer");
            sign = ((Integer) o1).compareTo((Integer) o2) != 0 ? true : false;
        } else if (ot instanceof Boolean) {
            System.out.println("Boolean");
            sign = ((Boolean) o1).compareTo((Boolean) o2) != 0 ? true : false;
        } else if (ot instanceof Long) {
            System.out.println("Long");
            sign = ((Long) o1).compareTo((Long) o2) != 0 ? true : false;
        } else if (ot instanceof LocalDateTime) {
            System.out.println("LocalDateTime");
            sign = ((LocalDateTime) o1).compareTo((LocalDateTime) o2) != 0 ? true : false;
        }
        return sign;
    }

    private static String objectToString(Object obj) {
        if (null == obj) {
            return "";
        } else {
            return obj.toString();
        }
    }

    private static String booleanToString(Object obj) {
        if (null == obj) {
            return null;
        } else {
            return (Boolean) obj ? "是" : "否";
        }
    }

    private static String localDateTimeToString(Object obj) {
        if (null == obj) {
            return null;
        } else {
            DateTimeFormatter df = DateTimeFormatter.ofPattern(DATE_FORMATTR_SECOND);
            LocalDateTime ld = LocalDateTime.now();
            return df.format(ld);
        }
    }
}
