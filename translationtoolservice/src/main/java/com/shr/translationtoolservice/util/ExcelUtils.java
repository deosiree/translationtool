package com.shr.translationtoolservice.util;

/**
 * @ClassName ExcelUtils
 * @Description TODO
 * @USER: Cola
 * @Date 2023/8/30 0030 9:52
 **/

import cn.afterturn.easypoi.cache.manager.IFileLoader;
import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import com.shr.translationtoolservice.dao.EntryClassifyMapper;
import com.shr.translationtoolservice.dao.TranslateMapper;
import com.shr.translationtoolservice.dao.VersionMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.ImportExcleVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.*;

import static org.springframework.util.StringUtils.capitalize;

/**
 * @Author: ***
 * @DateTime: 2020/10/30 15:51
 */

@Slf4j
@Component
public class ExcelUtils {
    private final String FULL_DATA_FORMAT = "yyyy/MM/dd  HH:mm:ss";
    private final String SHORT_DATA_FORMAT = "yyyy/MM/dd";
    @Autowired
    private TranslateMapper translateMapper;
    @Autowired
    private VersionMapper versionMapper;
    @Autowired
    private EntryClassifyMapper entryClassifyMapper;

    /**
     * Excel表头对应Entity属性 解析封装javabean
     *
     * @param classzz    类
     * @param in         excel流
     * @param fileName   文件名
     * @param excelHeads excel表头与entity属性对应关系
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> List<T> readExcelToEntity(Class<T> classzz, InputStream in, String fileName, List<ExcelHead> excelHeads) throws Exception {
        checkFile(fileName);    //是否EXCEL文件
        Workbook workbook = getWorkBoot(in, fileName); //兼容新老版本
        List<T> excelForBeans = readNewExcel(classzz, workbook, excelHeads);  //解析Excel
        return excelForBeans;
    }


    /**
     * Excel表头对应Entity属性 解析封装javabean
     *
     * @param classzz    类
     * @param in         excel流
     * @param fileName   文件名
     * @param excelHeads excel表头与entity属性对应关系
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> List<T> readZZExcelToEntity(Class<T> classzz, InputStream in, String fileName, List<ExcelHead> excelHeads) throws Exception {
        checkFile(fileName);    //是否EXCEL文件
        Workbook workbook = getWorkBoot(in, fileName); //兼容新老版本
        List<T> excelForBeans = readExcel(classzz, workbook, excelHeads);  //解析Excel
        return excelForBeans;
    }

    /**
     * 解析Excel转换为Entity
     *
     * @param classzz  类
     * @param in       excel流
     * @param fileName 文件名
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> List<T> readExcelToEntity(Class<T> classzz, InputStream in, String fileName) throws Exception {
        return readExcelToEntity(classzz, in, fileName, null);
    }


    /**
     * 解析Excel转换为Entity
     *
     * @param classzz  类
     * @param in       excel流
     * @param fileName 文件名
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> List<T> readZZExcelToEntity(Class<T> classzz, InputStream in, String fileName) throws Exception {
        return readZZExcelToEntity(classzz, in, fileName, null);
    }

    /**
     * 校验是否是Excel文件
     *
     * @param fileName
     * @throws Exception
     */
    public void checkFile(String fileName) throws Exception {
        if (!StringUtils.isEmpty(fileName) && !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
            throw new Exception("不是Excel文件！");
        }
    }

    /**
     * 兼容新老版Excel
     *
     * @param in
     * @param fileName
     * @return
     * @throws IOException
     */
    private Workbook getWorkBoot(InputStream in, String fileName) throws IOException, InvalidFormatException {
        if (fileName.endsWith(".xlsx")) {
            Workbook workbook=WorkbookFactory.create(in);
            return workbook;
        } else {
            return new HSSFWorkbook(in);
        }
    }

    /**
     * 解析Excel
     *
     * @param classzz    类
     * @param workbook   工作簿对象
     * @param excelHeads excel与entity对应关系实体
     * @param <T>
     * @return
     * @throws Exception
     */
    private <T> List<T> readExcel(Class<T> classzz, Workbook workbook, List<ExcelHead> excelHeads) throws Exception {
        List<T> beans = new ArrayList<T>();
        int sheetNum = workbook.getNumberOfSheets();
        for (int sheetIndex = 0; sheetIndex < sheetNum; sheetIndex++) {
            //判断分类
            boolean isWrite = false;
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            Field[] fields = classzz.getDeclaredFields();
            String sheetName = sheet.getSheetName();


            log.info(" **** 当前sheet为 " + sheetName + " **** ");
            int firstRowNum = sheet.getFirstRowNum();
            int lastRowNum = sheet.getLastRowNum();
            //首行子段名
            Row head = sheet.getRow(firstRowNum);
            if (head == null)
                continue;
            short firstCellNum = head.getFirstCellNum();
            short lastCellNum = head.getLastCellNum();



          /*  for (int i =0 ; i <lastCellNum;i++){
                if (org.junit.platform.commons.util.StringUtils.isBlank(head.getCell(i).getStringCellValue())){
                    head.sheet.getRow(firstRowNum+1).getCell(i)
                }
            }*/


            String classfy = "";
            for (int rowIndex = firstRowNum + 2; rowIndex <= lastRowNum; rowIndex++) {
                isWrite = false;
                log.info(" **** 正在读Excel 第 " + rowIndex + "行 **** ");
               // isWrite = true;
                Row dataRow = sheet.getRow(rowIndex);
                if (dataRow == null)
                    continue;
                T instance = classzz.newInstance();
                if (CollectionUtils.isEmpty(excelHeads)) {  //非头部映射方式，默认不校验是否为空，提高效率
                    firstCellNum = dataRow.getFirstCellNum();
                    lastCellNum = dataRow.getLastCellNum();
                   /* if (0 != firstCellNum) {
                        break;
                    }*/
                }
                for (int cellIndex = firstCellNum; cellIndex < lastCellNum; cellIndex++) {
                    Cell headCell = head.getCell(cellIndex);
                    /*//第一行
                    if (org.junit.platform.commons.util.StringUtils.isBlank(headCell.getStringCellValue())){
                        headCell =  sheet.getRow(firstRowNum+1).getCell(cellIndex);
                    }*/

                    if (headCell == null)
                        continue;
                    Cell cell = dataRow.getCell(cellIndex);
                    //空行校验(Abbr校验)
                    if ("Abbr".equals(headCell.getStringCellValue()) &&
                            (Objects.isNull(cell) || org.junit.platform.commons.util.StringUtils.isBlank(cell.getStringCellValue()))) {
                        isWrite = false;
                        break;
                    }
                    //获取颜色
                    if (null == cell) {
                        continue;
                    }
                    CellStyle cellStyle = cell.getCellStyle();
                    XSSFColor xssfColor = (XSSFColor) cellStyle.getFillForegroundColorColor();
                    byte[] bytes;
                    if (xssfColor != null) {
                        bytes = xssfColor.getRGB();
                        String color = String.format("#%02X%02X%02X", bytes[0], bytes[1], bytes[2]);
                        //识别绿色 写入分类
                        if ("#C5E0B4".equals(color)) {
                            classfy = cell.getStringCellValue();
                            isWrite = false;
                            break;
                        }
                    }


                    headCell.setCellType(Cell.CELL_TYPE_STRING);
                    String headName = headCell.getStringCellValue().trim();
                    log.info(" **** 当前列头为 " + headName + " **** ");
                    if (headName.equals("展开（缩写采用单驼峰,首字母大写）")) {
                        int a = 0;
                    }
                    //列头如果是空读取第二行，如果还是空跳过
                    if (StringUtils.isEmpty(headName) ||
                            headName.equals("英文术语") ||
                            headName.equals("西文术语") ||
                            headName.equals("俄文术语") ||
                            headName.equals("中文术语") ||
                            headName.equals("ABB类全驼峰") ||
                            headName.equals("展开（缩写采用单驼峰,首字母大写）") ||
                            headName.equals("单多驼峰搭配下划线（继保类似）") ||
                            headName.equals("展开（缩写可采用单/多驼峰）")) {
                        Row head1 = sheet.getRow(firstRowNum + 1);
                        Cell headCell1 = head1.getCell(cellIndex);
                        headCell1.setCellType(Cell.CELL_TYPE_STRING);
                        if (StringUtils.isEmpty(headName)) {
                            Cell headCell2 = head.getCell(cellIndex - 1);
                            headCell2.setCellType(Cell.CELL_TYPE_STRING);

                            headName = headCell2.getStringCellValue().trim() + headCell1.getStringCellValue().trim();
                        } else {
                            headName = headName.trim() + headCell1.getStringCellValue().trim();
                        }


                    }

                    ExcelHead eHead = null;
                    if (!CollectionUtils.isEmpty(excelHeads)) {
                        for (ExcelHead excelHead : excelHeads) {
                            if (headName.equals(excelHead.getExcelName())) {
                                eHead = excelHead;
                                headName = eHead.getEntityName();
                                break;
                            }
                        }
                    }
                    boolean isClassfy2 = true;
                    boolean isClassfy1 = true;
                    //遍历哦实体属性
                    for (Field field : fields) {

                        if (isClassfy2) {

                            Method classfyMethod = classzz.getMethod("setClassfy2", field.getType());
                            classfyMethod.invoke(instance, convertType(field.getType(), classfy.trim()));
                            isClassfy2 = false;
                        }

                        if (isClassfy1) {
                            Method classfyMethod = classzz.getMethod("setClassfy1", field.getType());
                            classfyMethod.invoke(instance, convertType(field.getType(), sheetName.trim()));
                            isClassfy1 = false;
                        }

                        if (headName.equalsIgnoreCase(ImportExcleEntry.aliasMap.get(field.getName()))) {
                            String methodName = MethodUtils.setMethodName(field.getName());
                            System.out.println(methodName);
                            Method method = classzz.getMethod(methodName, field.getType());
                            if (isDateFied(field)) {
                                Date date = null;
                                //过滤空数据
                                if (cell != null && 1 != cell.getCellType()) {

                                    date = cell.getDateCellValue();
                                    log.info(" **** 当前读取的值为 " + date + " **** ");
                                }
                                if (date == null) {
                                    volidateValueRequired(eHead, sheetName, rowIndex);
                                    break;
                                }


                                method.invoke(instance, cell.getDateCellValue());
                            } else if (isTimeStamp(field)) {
                                Date date = null;
                                if (cell != null) {
                                    date = cell.getDateCellValue();
                                }
                                if (date == null) {
                                    volidateValueRequired(eHead, sheetName, rowIndex);
                                    break;
                                }
                                log.info(" **** 当前读取的值为 " + new Timestamp(cell.getDateCellValue().getTime()) + " **** ");
                                method.invoke(instance, new Timestamp(cell.getDateCellValue().getTime()));
                            } else if (isEnum(field)) {
                                String value = null;
                                if (cell != null) {
                                    value = cell.getStringCellValue();
                                }
                                if (StringUtils.isEmpty(value)) {
                                    volidateValueRequired(eHead, sheetName, rowIndex);
                                    break;
                                }
                                log.info(" **** 当前读取的值为 " + Enum.valueOf((Class) field.getType(), value) + " **** ");
                                method.invoke(instance, Enum.valueOf((Class) field.getType(), value));

                            } else {
                                String value = null;
                                if (cell != null) {
                                    cell.setCellType(Cell.CELL_TYPE_STRING);
                                    value = cell.getStringCellValue();
                                }
                                if (StringUtils.isEmpty(value)) {
                                    volidateValueRequired(eHead, sheetName, rowIndex);
                                    break;
                                }
                                if (headName.equals("中文术语术语") &&  !StringUtils.isEmpty(value)){
                                    isWrite = true;
                                }
                                if (headName.equals("中文术语术语") &&  StringUtils.isEmpty(value)){
                                    break;
                                }
                           /*     if (headName.contains("日期")){
                                    int intDay = Integer.parseInt( convertType(field.getType(), value.trim()));
                                    Date dd = DateUtils.addDays(calendar.getTime(),intDay);
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
                                    String format = simpleDateFormat.format(dd);
                                }*/
                                log.info(" **** 当前读取的值为 " + convertType(field.getType(), value.trim()) + " **** ");
                                method.invoke(instance, convertType(field.getType(), value.trim()));
                            }
                            log.info(" ======= headName is : " + headName + " ======== ");
                            //break;
                        }
                    }


                }
                //是否要写
                if (isWrite) {
                    beans.add(instance);
                }

            }
        }
        return beans;
    }


    /**
     * 解析Excel
     *
     * @param classzz    类
     * @param workbook   工作簿对象
     * @param excelHeads excel与entity对应关系实体
     * @param <T>
     * @return
     * @throws Exception
     */
    private <T> List<T> readNewExcel(Class<T> classzz, Workbook workbook, List<ExcelHead> excelHeads) throws Exception {
        List<T> beans = new ArrayList<T>();
        int sheetNum = workbook.getNumberOfSheets();
        for (int sheetIndex = 0; sheetIndex < sheetNum; sheetIndex++) {
            //判断分类
            boolean isWrite = false;
            Sheet sheet = workbook.getSheetAt(sheetIndex);

            String sheetName = sheet.getSheetName();
            log.info(" **** 当前sheet为 " + sheetName + " **** ");
            int firstRowNum = sheet.getFirstRowNum() + 1;
            int lastRowNum = sheet.getLastRowNum();
            //首行子段名
            Row head = sheet.getRow(firstRowNum);
            if (head == null)
                continue;
            short firstCellNum = head.getFirstCellNum();
            short lastCellNum = head.getLastCellNum();



          /*  for (int i =0 ; i <lastCellNum;i++){
                if (org.junit.platform.commons.util.StringUtils.isBlank(head.getCell(i).getStringCellValue())){
                    head.sheet.getRow(firstRowNum+1).getCell(i)
                }
            }*/


            Field[] fields = classzz.getDeclaredFields();
            String classfy = "";
            for (int rowIndex = firstRowNum + 1; rowIndex <= lastRowNum; rowIndex++) {
                isWrite = false;
                log.info(" **** 正在读Excel 第 " + rowIndex + "行 **** ");

                Row dataRow = sheet.getRow(rowIndex);
                if (dataRow == null)
                    continue;
                T instance = classzz.newInstance();
                if (CollectionUtils.isEmpty(excelHeads)) {  //非头部映射方式，默认不校验是否为空，提高效率
                    firstCellNum = dataRow.getFirstCellNum();
                    lastCellNum = dataRow.getLastCellNum();
                   /* if (0 != firstCellNum) {
                        break;
                    }*/
                }
                for (int cellIndex = firstCellNum; cellIndex < lastCellNum; cellIndex++) {
                    Cell headCell = head.getCell(cellIndex);
                    /*//第一行
                    if (org.junit.platform.commons.util.StringUtils.isBlank(headCell.getStringCellValue())){
                        headCell =  sheet.getRow(firstRowNum+1).getCell(cellIndex);
                    }*/
                    if (headCell == null)
                        continue;
                    Cell cell = dataRow.getCell(cellIndex);
                    //空行校验(Abbr校验)
                  /*  if (Objects.isNull(cell) || org.junit.platform.commons.util.StringUtils.isBlank(cell.getStringCellValue())) {
                        isWrite = false;
                        break;
                    }*/
                    //获取颜色
                    if (null == cell) {
                        continue;
                    }

                    //判断颜色
                  /*  CellStyle cellStyle = cell.getCellStyle();
                    XSSFColor xssfColor = (XSSFColor) cellStyle.getFillForegroundColorColor();
                    byte[] bytes;
                    if (xssfColor != null) {
                        bytes = xssfColor.getRGB();
                        String color = String.format("#%02X%02X%02X", bytes[0], bytes[1], bytes[2]);
                        //识别绿色 写入分类
                        if ("#C5E0B4".equals(color)) {
                            classfy = cell.getStringCellValue();
                            isWrite = false;
                            break;
                        }
                    }*/


                    headCell.setCellType(Cell.CELL_TYPE_STRING);
                    String headName = headCell.getStringCellValue().trim();
                    if (headName.equals("展开（缩写采用单驼峰,首字母大写）")) {
                        int a = 0;
                    }
                /*    //列头如果是空读取第二行，如果还是空跳过
                    if (StringUtils.isEmpty(headName) ||
                            headName.equals("英文术语") ||
                            headName.equals("西文术语") ||
                            headName.equals("俄文术语") ||
                            headName.equals("中文术语") ||
                            headName.equals("ABB类全驼峰") ||
                            headName.equals("展开（缩写采用单驼峰,首字母大写）") ||
                            headName.equals("单多驼峰搭配下划线（继保类似）") ||
                            headName.equals("展开（缩写可采用单/多驼峰）")) {
                        Row head1 = sheet.getRow(firstRowNum + 1);
                        Cell headCell1 = head1.getCell(cellIndex);
                        headCell1.setCellType(Cell.CELL_TYPE_STRING);
                        if (StringUtils.isEmpty(headName)) {
                            Cell headCell2 = head.getCell(cellIndex - 1);
                            headCell2.setCellType(Cell.CELL_TYPE_STRING);

                            headName = headCell2.getStringCellValue().trim() + headCell1.getStringCellValue().trim();
                        } else {
                            headName = headName.trim() + headCell1.getStringCellValue().trim();
                        }


                    }*/
                    log.info(" **** 当前列头为 " + headName + " **** ");
                    ExcelHead eHead = null;
                    if (!CollectionUtils.isEmpty(excelHeads)) {
                        for (ExcelHead excelHead : excelHeads) {
                            if (headName.equals(excelHead.getExcelName())) {
                                eHead = excelHead;
                                headName = eHead.getEntityName();
                                break;
                            }
                        }
                    }
                    boolean isClassfy = true;
                    //遍历哦实体属性
                    for (Field field : fields) {

                 /*       if (isClassfy) {

                            Method classfyMethod = classzz.getMethod("setClassfy", field.getType());
                            classfyMethod.invoke(instance, convertType(field.getType(), classfy.trim()));
                            isClassfy = false;
                        }
*/

                        if (headName.equalsIgnoreCase(ConstantInterface.constructEntryName().get(field.getName()))) {
                            String methodName = MethodUtils.setMethodName(field.getName());
                            System.out.println(methodName);
                            Method method = classzz.getMethod(methodName, field.getType());
                            if (isDateFied(field)) {
                            /*    Date date = null;
                                if (cell != null) {
                                    date = cell.getDateCellValue();
                                    log.info(" **** 当前读取的值为 " + date + " **** ");
                                }
                                if (date == null) {
                                    volidateValueRequired(eHead, sheetName, rowIndex);
                                    break;
                                }


                                method.invoke(instance, cell.getDateCellValue());*/
                            } else if (isTimeStamp(field)) {
                                Date date = null;
                                if (cell != null) {
                                    date = cell.getDateCellValue();
                                }
                                if (date == null) {
                                    volidateValueRequired(eHead, sheetName, rowIndex);
                                    break;
                                }
                                log.info(" **** 当前读取的值为 " + new Timestamp(cell.getDateCellValue().getTime()) + " **** ");
                                method.invoke(instance, new Timestamp(cell.getDateCellValue().getTime()));
                            } else if (isEnum(field)) {
                                String value = null;
                                if (cell != null) {
                                    value = cell.getStringCellValue();
                                }
                                if (StringUtils.isEmpty(value)) {
                                    volidateValueRequired(eHead, sheetName, rowIndex);
                                    break;
                                }
                                log.info(" **** 当前读取的值为 " + Enum.valueOf((Class) field.getType(), value) + " **** ");
                                method.invoke(instance, Enum.valueOf((Class) field.getType(), value));

                            } else {
                                String value = null;
                                if (cell != null) {
                                    cell.setCellType(Cell.CELL_TYPE_STRING);
                                    value = cell.getStringCellValue();
                                }
                                //如果词条字段不是空 写入
                                if (headName.equals("词条") &&  !StringUtils.isEmpty(value)){
                                    isWrite = true;
                                }
                                if (headName.equals("词条") &&  StringUtils.isEmpty(value)){
                                    break;
                                }
                                if (StringUtils.isEmpty(value)) {
                                    volidateValueRequired(eHead, sheetName, rowIndex);
                                    break;
                                }
                           /*     if (headName.contains("日期")){
                                    int intDay = Integer.parseInt( convertType(field.getType(), value.trim()));
                                    Date dd = DateUtils.addDays(calendar.getTime(),intDay);
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
                                    String format = simpleDateFormat.format(dd);
                                }*/
                                log.info(" **** 当前读取的值为 " + convertType(field.getType(), value) + " **** ");
                                method.invoke(instance, convertType(field.getType(), value));
                            }
                            log.info(" ======= headName is : " + headName + " ======== ");
                            break;
                        }
                    }
                }
                //是否要写
                if (isWrite) {
                    beans.add(instance);
                }

            }
        }
        return beans;
    }

    /**
     * 是否日期字段
     *
     * @param field
     * @return
     */
    private boolean isDateFied(Field field) {
        return (Date.class == field.getType());
    }

    /**
     * 是否时间戳类型
     *
     * @param field
     * @return
     */
    private static boolean isTimeStamp(Field field) {
        return (Timestamp.class == field.getType());
    }

    /**
     * 是否枚举类型
     *
     * @param field
     * @return
     */
    private static boolean isEnum(Field field) {
        return field.getType().isEnum();
    }

    /**
     * 空值校验
     *
     * @param excelHead
     * @throws Exception
     */
    private void volidateValueRequired(ExcelHead excelHead, String sheetName, int rowIndex) throws Exception {
        if (excelHead != null && excelHead.isRequired()) {
            throw new Exception("《" + sheetName + "》第" + (rowIndex + 1) + "行:\"" + excelHead.getExcelName() + "\"不能为空！");
        }
    }

    /**
     * 类型转换
     *
     * @param classzz
     * @param value
     * @return
     */
    private Object convertType(Class classzz, String value) {
        if (Integer.class == classzz || int.class == classzz) {
            return Integer.valueOf(value);
        }
        if (Short.class == classzz || short.class == classzz) {
            return Short.valueOf(value);
        }
        if (Byte.class == classzz || byte.class == classzz) {
            return Byte.valueOf(value);
        }
        if (Character.class == classzz || char.class == classzz) {
            return value.charAt(0);
        }
        if (Long.class == classzz || long.class == classzz) {
            return Long.valueOf(value);
        }
        if (Float.class == classzz || float.class == classzz) {
            return Float.valueOf(value);
        }
        if (Double.class == classzz || double.class == classzz) {
            return Double.valueOf(value);
        }
        if (Boolean.class == classzz || boolean.class == classzz) {
            return Boolean.valueOf(value.toLowerCase());
        }
        if (BigDecimal.class == classzz) {
            return new BigDecimal(value);
        }

       /* if (Date.class == classzz) {
            SimpleDateFormat formatter = new SimpleDateFormat(FULL_DATA_FORMAT);
            ParsePosition pos = new ParsePosition(0);
            Date date = formatter.parse(value, pos);
            return date;
        }*/
        return value;
    }

    /**
     * 获取properties的set和get方法
     */
    @Data
    public static class MethodUtils {
        private static final String SET_PREFIX = "set";
        private static final String GET_PREFIX = "get";

        private static String capitalize(String name) {
            if (name == null || name.length() == 0) {
                return name;
            }
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }

        public static String setMethodName(String propertyName) {
            return SET_PREFIX + capitalize(propertyName);
        }

        public static String getMethodName(String propertyName) {
            return GET_PREFIX + capitalize(propertyName);
        }
    }


    public Workbook outPutExcel(List<EntryInfoEntity> entryInfoEntities, String transType, String excelName) throws IOException {

        List<OutputExcel> dataList = new ArrayList<>();
        int i = 0;
        for (EntryInfoEntity entryInfoEntity : entryInfoEntities) {
            i += 1;
            OutputExcel outputExcel = new OutputExcel();
            outputExcel.setEntry(entryInfoEntity.getEntry());
            Map<String, String> map = new HashMap<>();

            //查找翻译
            TranslateEntity translateEntity = null;
            switch (transType) {
                case ConstantInterface.ENGLISH:
                    translateEntity = translateMapper.selectById(entryInfoEntity.getEnTransId());
                    if (Objects.nonNull(translateEntity)) {
                        outputExcel.setTranslate(translateEntity.getTranslate());
                    }
                    break;
                case ConstantInterface.RUSSIAN:
                    translateEntity = translateMapper.selectById(entryInfoEntity.getRuTransId());
                    if (Objects.nonNull(translateEntity)) {
                        outputExcel.setTranslate(translateEntity.getTranslate());
                    }

                    break;
                case ConstantInterface.FRENCH:
                    translateEntity = translateMapper.selectById(entryInfoEntity.getFraTransId());
                    if (Objects.nonNull(translateEntity)) {
                        outputExcel.setTranslate(translateEntity.getTranslate());
                    }
                    break;
                case ConstantInterface.SPANISH:
                    translateEntity = translateMapper.selectById(entryInfoEntity.getSpaTransId());
                    if (Objects.nonNull(translateEntity)) {
                        outputExcel.setTranslate(translateEntity.getTranslate());
                    }
                    break;
            }

            outputExcel.setNum(i);

            outputExcel.setVersion(versionMapper.selectById(entryInfoEntity.getVersionID()).getName());

            EntryClassify entryClassify = entryClassifyMapper.getEntryClassfyById(entryInfoEntity.getClassifyId());
            if (Objects.nonNull(entryClassify)) {
                outputExcel.setClassify(entryClassify.getKey());
            }
            dataList.add(outputExcel);
        }
        //生成excel文档
//        FileOutputStream fos =  new FileOutputStream(excelName);

        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams("词条翻译工具导出", "词条数据"),
                OutputExcel.class, dataList);


        // workbook.write(fos);
        return workbook;


    }

    /**
     * 选择字段导出excel
     * dataList：数据源
     * exportFields：列名 data属性名
     * heards：字段名也就是excel首行
     * excelName：sheet名
     */
    public void getWorkBook(List<?> dataList, HttpServletResponse response, List<String> exportFields, List<String> heards, String excelName) {
        // 创建工作簿对象
        Workbook workbook = new XSSFWorkbook();
        // 创建工作表对象
        Sheet sheet = workbook.createSheet(excelName);
        //开启锁定
       // sheet.protectSheet("11");
        //第一行 标题行
        Row oneHeaderRow = sheet.createRow(0);
        Cell oneHeaderCell = oneHeaderRow.createCell(0);
        oneHeaderCell.setCellValue(excelName);
        // 合并单元格，参数依次为起始行，结束行，起始列，结束列 (索引0开始)
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, exportFields.size() - 1));//标题合并单元格操作，6为总列数
        CellStyle oneCellStyle = workbook.createCellStyle();
        oneCellStyle.setAlignment(HorizontalAlignment.CENTER);//设置水平居中
        oneCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);//设置垂直居中
        oneHeaderCell.setCellStyle(oneCellStyle);

        CellStyle lockStyle = workbook.createCellStyle();
        lockStyle.setLocked(true);
        // 单元格，格式化样式
        DataFormat dataFormat = workbook.createDataFormat();
        lockStyle.setDataFormat(dataFormat.getFormat("@"));
        lockStyle.setAlignment(HorizontalAlignment.CENTER);//设置水平居中
        lockStyle.setVerticalAlignment(VerticalAlignment.CENTER);//设置垂直居中

        // 不锁定样式
        CellStyle unlockStyle = workbook.createCellStyle();
        unlockStyle.setLocked(false);
        unlockStyle.setDataFormat(dataFormat.getFormat("@")); // 设置单元格文本样式为文本
        unlockStyle.setAlignment(HorizontalAlignment.CENTER);//设置水平居中
        unlockStyle.setVerticalAlignment(VerticalAlignment.CENTER);//设置垂直居中



        // 创建字体对象
        Font oneHeaderFont = workbook.createFont();
        oneHeaderFont.setFontName("黑体");
        oneHeaderFont.setFontHeightInPoints((short) 16);
        // 将字体应用于单元格样式
        oneCellStyle.setFont(oneHeaderFont);
        // 应用样式到单元格
        oneHeaderCell.setCellStyle(oneCellStyle);
        int freezeColumn = 3;
        // 第二行 小标题行
        Row headerRow = sheet.createRow(1);
        headerRow.setRowStyle(oneCellStyle);


        // 根据导出字段列表创建表头单元格
        for (int i = 0; i < exportFields.size(); i++) {
          /*  //锁定前两列
            if (i < 2) {
                sheet.setDefaultColumnStyle(i, lockStyle);
            }else {
                sheet.setDefaultColumnStyle(i, unlockStyle);
            }*/


            Cell headerCell = headerRow.createCell(i);
            headerCell.setCellValue(heards.get(i));
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.CENTER);//设置水平居中
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);//设置垂直居中
            sheet.setColumnWidth(i, 256 * 25 + 184);//设置宽度 这里的25对应excel中的列宽
            cellStyle.setWrapText(true);// 设置自动换行
            cellStyle.setLocked(true);
            headerCell.setCellStyle(cellStyle);
            // 创建字体对象
            Font headerFont = workbook.createFont();
            headerFont.setFontName("黑体");
            headerFont.setFontHeightInPoints((short) 14);
            // 将字体应用于单元格样式
            cellStyle.setFont(headerFont);
            // 应用样式到单元格
            headerCell.setCellStyle(cellStyle);
        }


        // 创建数据行
        for (int rowIndex = 0; rowIndex < dataList.size(); rowIndex++) {
            Row dataRow = sheet.createRow(rowIndex + 2);//这里+2因为我已经给第一行设置了值
            // 获取数据对象
            Object dataObj = dataList.get(rowIndex);
            // 遍历导出字段列表
            for (int colIndex = 0; colIndex < exportFields.size(); colIndex++) {
                String fieldName = exportFields.get(colIndex);


                try {
                    // 根据字段名获取对应的 getter 方法
                    Method method = dataObj.getClass().getMethod("get" + capitalize(fieldName));
                    // 调用 getter 方法获取字段值
                    Object fieldValue = method.invoke(dataObj);
                    // 创建单元格并设置字段值
                    Cell dataCell = dataRow.createCell(colIndex);
                    CellStyle cellStyle = workbook.createCellStyle();
                    cellStyle.setAlignment(HorizontalAlignment.CENTER);//设置水平居中
                    cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);//设置垂直居中
                    sheet.setColumnWidth(colIndex, 256 * 25 + 184);//设置宽度 这里的25对应excel中的列宽
                    dataCell.setCellStyle(cellStyle);
                    //隐藏列
                    if ("id".equals(fieldName)){
                        sheet.setColumnHidden(0,true);
                    }

                    //锁定列 暂不生效
                    if ("id".equals(fieldName) || "entry".equals(fieldName) || "abbr".equals(fieldName)){
                        // 单元格，锁定样式
                        dataCell.setCellStyle(lockStyle);

                    }else {
                        dataCell.setCellStyle(unlockStyle);
                    }

                    if (fieldValue instanceof String) {
                        dataCell.setCellValue((String) fieldValue);
                    } else if (fieldValue instanceof Number) {
                        dataCell.setCellValue(((Number) fieldValue).doubleValue());
                    } else if (fieldValue instanceof Date) {
                        dataCell.setCellValue((Date) fieldValue);
                    } else if (fieldValue instanceof Boolean) {
                        dataCell.setCellValue((Boolean) fieldValue);
                    } else {
                        if (Objects.isNull(fieldValue)) {
                            dataCell.setCellValue("");
                        } else {
                            dataCell.setCellValue(String.valueOf(fieldValue));
                        }
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

        }


        // 设置响应头信息
        try {
            excelName = URLEncoder.encode(excelName, "UTF-8");
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + excelName + ".xlsx");
            response.addHeader("Pargam", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            // 将工作簿写入响应输出流中
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 临时表 t_entry_temp 词条导出
    public Workbook exportTempEntryUtil(List<EntryTempEntity> EntryTempList, String fileName, String versionName) throws IOException {

        List<OutputExcel> dataList = new ArrayList<>();
        int i = 0;
        for (EntryTempEntity entryTemp : EntryTempList) {
            i += 1;
            OutputExcel outputExcel = new OutputExcel();
            outputExcel.setEntry(entryTemp.getEntry());
            outputExcel.setTranslate(entryTemp.getTranslate());
            outputExcel.setNum(i);
            outputExcel.setVersion(versionName);
            dataList.add(outputExcel);
        }
        //生成excel文档
//        FileOutputStream fos =  new FileOutputStream(fileName);

        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams("词条翻译工具导出", "词条数据"),
                OutputExcel.class, dataList);


        // workbook.write(fos);
        return workbook;


    }
}
