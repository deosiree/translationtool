package com.shr.translationtoolservice.util;

/**
 * @ClassName ExcelUtils
 * @Description TODO
 * @USER: Cola
 * @Date 2023/8/30 0030 9:52
 **/

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import com.shr.translationtoolservice.dao.EntryClassifyMapper;
import com.shr.translationtoolservice.dao.TranslateMapper;
import com.shr.translationtoolservice.dao.VersionMapper;
import com.shr.translationtoolservice.entity.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.*;
import java.util.function.Function;

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

    public static Set<Character> specialCharacterForCSV = new HashSet<>();

    static{

        specialCharacterForCSV.add('\n');
        specialCharacterForCSV.add('\t');
        specialCharacterForCSV.add('\b');
        specialCharacterForCSV.add('\r');
        specialCharacterForCSV.add('\f');
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
    public <T> ParseFileInfo<T> readExcelToEntity(String translateType ,Class<T> classzz, InputStream in, String fileName, List<ExcelHead> excelHeads) throws Exception {
        checkFile(fileName);    //是否EXCEL文件
        Workbook workbook = getWorkBoot(in, fileName); //兼容新老版本
        return readNewExcel(classzz, workbook, excelHeads);  //解析Excel
    }

    private static String charsetDetect(String filePath){


        return null;
    }

    private static String postProcessSubText(String subText){
        if(subText == null){
            return null;
        }
        if(subText.equals("")){
            return subText;
        }else if(subText.equals("\"")){
            return null;
        }
        if(subText.startsWith("\"")){
            if(subText.endsWith("\"")){
                String processedSubText = subText.substring(1,subText.length() - 1);
                // 如果字符串里面"不成对出现就有问题
                boolean skip = false;
                StringBuilder builder = new StringBuilder();
                for(int idx = 0 ; idx < processedSubText.length() ; idx ++ ){
                    char next = processedSubText.charAt(idx);
                    builder.append(next);
                    if(next == '"'){
                        if(idx + 1 < processedSubText.length() && processedSubText.charAt(idx + 1) == '"'){
                            idx ++;
                        }else{
                            skip = true;    
                            break;   
                        }
                    }
                }
                if(skip) return null;
                return builder.toString();
            }else{
                return null;
            }
        }else{
            return subText;
        }
    }

    /**
    *   先找到最近的两个逗号
        如果第一个后面紧跟"，则看第二个逗号前面是不是"
            如果是，则取这两个逗号之间的内容，
            否则,找下一个符合前面是"的逗号，如果到最后一个字符，如果前面是"，则取这两个"之间的内容，如果不是，则报错
        如果第一个后面不是"，则取下一个逗号前面的所有
        如果取的内容前后有",则判断剩余的内容，如果剩余的内容只有一个'"',则取下一个','
     * @param lineText
     * @return
     */
    private static String[] CSVParse(String lineText){
        if(lineText == null){
            return new String[0];
        }
        int textLength = lineText.length();
        if(textLength == 0){
            return new String[0];
        }
        List<String> textList = new ArrayList<>();
        int leftCommaIndex = -1;
        for(int i = 0 ; i < textLength ; i ++ ){
            char nextChar = lineText.charAt(i);
            if(nextChar == ','){
                String subText = lineText.substring(leftCommaIndex + 1, i);
                String processedText = postProcessSubText(subText);
                if(processedText == null){
                    // 不符合要求
                    continue;
                }
                textList.add(processedText);
                leftCommaIndex = i;
            }else{
                // if(i + 1 == textLength){
                //     // 最后一个了
                //     String processedText = postProcessSubText(lineText.substring(leftCommaIndex + 1, textLength));
                //     if(processedText == null){
                //         throw new RuntimeException("提供的csv文件存在异常");
                //     }
                //     textList.add(processedText);
                // }
            }
        }
        String processedText = postProcessSubText(lineText.substring(leftCommaIndex + 1 , textLength));
        if(processedText == null){
            throw new RuntimeException("解析的这一行数据存在异常: 该行数据为: " + lineText);
        }
        textList.add(processedText);
        return textList.toArray(new String[textList.size()]);
    }

    private static <T> void buildColumnNameProjection(List<Map<String,String>> entryNameProjectionList,Class<T> classzz){
        if(classzz == ImportExcleEntry.class){
            entryNameProjectionList.add(ImportExcleEntry.aliasMap);
        }else{
            entryNameProjectionList.add(ConstantInterface.constructEntryName());
        }
    }

    private static boolean isColumnForField(List<Map<String,String>> entryNameProjectionList,String fieldName,String headerName){

        for(Map<String,String> entryNameProjection : entryNameProjectionList){
            if(entryNameProjection.get(fieldName) != null && entryNameProjection.get(fieldName).equals(headerName)){
                return true;
            }
        }
        return false;
    }

    public static <T> Method[] methodListFromCSV(String lineText,Class<T> classzz){
        return methodListFromCSV(lineText, classzz, null);
    }

    public static <T> Method[] methodListFromCSV(String lineText,Class<T> classzz,Map<String,String> methodRedirect){
        if(methodRedirect == null){
            methodRedirect = new HashMap<>();
        }
        if(classzz == null){
            throw new RuntimeException("没有提供classzz");
        }
        // List<Method> methodList = new ArrayList<>();
        String[] elementList = ExcelUtils.CSVParse(lineText);
        Method[] methodList = new Method[elementList.length];
        /* 获得属性名-(表头名1，表头名2,……)映射表 */
        List<Map<String,String>> entryNameProjectionList = new ArrayList<>();
        buildColumnNameProjection(entryNameProjectionList,classzz);

        for(int i = 0 ; i < elementList.length ; i ++ ){
            String nextHeader = elementList[i];
            if(nextHeader == null || nextHeader.equals("")){
                methodList[i] = null;
                continue;
            }
            boolean isFindMethod = false;
            Class<? super T> currentClass = classzz;
            while (currentClass != null) {
                for(Field field : currentClass.getDeclaredFields()){
                    String fieldName = field.getName();
                    String headerName = nextHeader;
                    if(methodRedirect != null && methodRedirect.containsKey(headerName)){
                        headerName = methodRedirect.get(headerName);
                    }
                    if(isColumnForField(entryNameProjectionList, fieldName, headerName)){
                    // if(entryNameProjection.get(fieldName) != null && entryNameProjection.get(fieldName).equals(headerName)){
                        String methodName =  MethodUtils.setMethodName(fieldName);
                        Method method = null;
                        try {
                            method = classzz.getMethod(methodName, field.getType());
                        } catch (NoSuchMethodException e) {
                            // TODO Auto-generated catch block
                            throw new RuntimeException(e);
                        } catch (SecurityException e) {
                            // TODO Auto-generated catch block
                            throw new RuntimeException(e);
                        }
                        if(method == null){
                            throw new RuntimeException("没有获取到methodName: \"" + methodName + "\"方法");
                        }
                        isFindMethod = true;
                        // methodList.add(method);
                        methodList[i] = method;
                    }
                }
                currentClass = currentClass.getSuperclass();
            }
            if(!isFindMethod){
                /* 没有找到说明该单元格的内容无效 */
                // throw new RuntimeException("当前单元格内容为: " + nextHeader + "没有找到对应的方法");
                methodList[i] = null;
            }
        }

        return methodList;
    }
    /**
     * 是否有成对的"
     * @param textLine
     * @return
     */
    public boolean checkCSVLine(String textLine){
        int length = textLine.length();
        int quotaCount = 0;
        for(int i = 0; i < length ; i ++ ){
            if(textLine.charAt(i) == '\"'){
                quotaCount ++;
            }
        }
        return quotaCount % 2 == 0;
    }

    public List<String> parseCSVFile(InputStream in,String charset) throws Exception{
        InputStreamReader inputStreamReader = charset == null || charset.trim().isEmpty() ? new InputStreamReader(in) : new InputStreamReader(in,charset);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        List<String> lineTextList = new ArrayList<>();
        String nextLine = null;
        String beforeLine = "";
        while ((nextLine = reader.readLine()) != null) {    
            if(nextLine.equals("")){
                /* 过滤掉空行 */
                continue;   
            }
            if((int)nextLine.charAt(0) == 65279){
                // UTF-8 +BOM 格式
                nextLine = nextLine.substring(1,nextLine.length());
                if(nextLine.equals("")) continue;
            }
            if(beforeLine.equals("")){
                try {
                    CSVParse(nextLine);
                    lineTextList.add(nextLine);
                    beforeLine = "";
                } catch (Exception e) {
                    /* 说明当前行解析不正常，说明可能有单元格的文本有换行符 */
                    beforeLine += nextLine;
                }
            }else{
                try {
                    CSVParse(beforeLine + '\n' + nextLine);
                    lineTextList.add(beforeLine + '\n' + nextLine);
                    beforeLine = "";
                } catch (Exception e) {
                    /* 说明当前行解析不正常，说明可能有单元格的文本有换行符 */
                    beforeLine += ('\n' + nextLine);
                }
            }            
        }
        return lineTextList;
    }

    private void checkMethodList(Method[] methods){
        Set<String> methodName = new HashSet<>();
        boolean isFindEntry = false;
        for(Method method : methods){
            if(method == null){
                continue;
            }
            if(method.getName().equals("setEntry")){
                isFindEntry = true;
            }
            if(methodName.contains(method.getName())){
                throw new RuntimeException("当前csv文件有多行被解析为向同一个属性写入数据, 该方法名为: " + method.getName());
            }
            methodName.add(method.getName());
        }
        if(!isFindEntry)
            throw new RuntimeException("当前解析的csv文件没有词条对应的列");
    }

    public <T> ParseFileInfo<T> readCSVToEntity(Class<T> classzz,InputStream in,String fileName,Map<String,String> kwargs) throws Exception{
        checkFile(fileName);
        List<T> entities = new  ArrayList<>();
        /* 获取编码模式  */
        String encoding = StringUtil.checkEncoding(kwargs.get("encoding"));
        if(encoding == null) encoding = "GBK";
        DateFormat electedFormat = null;
        try {
            ParseFileInfo<T> parseFileInfo = new ParseFileInfo<>();
            Method[] methodList = null;
            boolean isFindHeader = false;
            List<String> lineTextList = parseCSVFile(in,encoding);
            if(lineTextList.isEmpty()){
                throw new RuntimeException("文件内部没有任何内容");
            }
            for(String lineText : lineTextList){
                String[] elementList = ExcelUtils.CSVParse(lineText);
                if(!isFindHeader){
                    // 第一个非空行,代表表头    
                    parseFileInfo.setColumnName(elementList);
                    methodList = methodListFromCSV(lineText, classzz);
                    isFindHeader = true;
                    checkMethodList(methodList);
                }else{
                    if(methodList == null){
                        throw new RuntimeException("methodList为null");
                    }
                    int dataLength = elementList.length;
                    T instance= null; 
                    for(int i = 0 ; i < dataLength ; i ++ ){
                        String nextValue = elementList[i];
                        // 以后考虑一下如果有这个字段, 但是单元格为空，将这个字段的值填充为"", 如果没有这个字段的列，那么这个字段的值为null
                        if(nextValue == null || (nextValue.equals("") &&  (kwargs != null && !Boolean.parseBoolean(kwargs.get("emptyAsValue")))) ){
                            /* 单元格内容为空时跳过，该字段默认值 */
                            continue;
                        }
                        /* 校验get方法的参数个数以及类型 */
                        if(i >= methodList.length){
                            /* 当该单元格超过了方法对应行的所在列，此时退出，后续单元格的内容不进行处理了 */
                            break;  
                        }
                        Method method = methodList[i];
                        if(method == null){
                            /* method为null，反应该单元格的内容无法映射到entryinfo的属性上,所以跳过这一列 */
                            continue;
                        }
                        if(instance == null){
                            instance = classzz.newInstance();
                        }
                        Class<?> paramTypes[] = method.getParameterTypes();
                        try {
                            if(paramTypes.length != 1){
                                throw new RuntimeException("取得的方法的参数个数不等于1");
                            }
                            Class<?> paramType = paramTypes[0];
                            /* 执行get方法 */
                            if(paramType != String.class){
                                if(nextValue.equals("")){
                                    continue;   // 跳过,非字符串类型的数据就跳过
                                }
                                Object convertedValue = convertType(paramType, nextValue);
                                if(convertedValue.getClass() != paramType){
                                    // convertType失效
                                    if(paramType == Date.class){
                                        if(electedFormat != null){
                                            convertedValue = electedFormat.parse(nextValue);
                                        }else{
                                            try {
                                                convertedValue = LocalTimeUtils.formatForFile.parse(nextValue);
                                                electedFormat = LocalTimeUtils.formatForFile;
                                            } catch (Exception e) {
                                                try {
                                                    convertedValue = LocalTimeUtils.format2ForFile.parse(nextValue);
                                                    electedFormat = LocalTimeUtils.format2ForFile;
                                                    
                                                } catch (Exception firstException) {
                                                    // TODO: handle exception
                                                    try {
                                                        convertedValue = LocalTimeUtils.format3ForFile.parse(nextValue);
                                                        electedFormat = LocalTimeUtils.format3ForFile;

                                                    } catch (Exception secondException) {
                                                        // TODO: handle exception
                                                        throw new RuntimeException(secondException);
                                                    }
                                                }
                                            }
                                        }

                                    }
                                }
                                method.invoke(instance, convertedValue);
                            }else{
                                if(method.getName().equals("setEntry")){
                                    if(nextValue == null || nextValue.equals("")){
                                        throw new RuntimeException("当前解析csv文件发现存在词条为null或空字符串");
                                    }
                                }
                                method.invoke(instance, nextValue);
                            }
                        } catch (IllegalArgumentException e) {
                            // TODO: handle exception
                            throw new RuntimeException("调用方法method: " + method.getName() + "; 获取其参数值: " + nextValue + "时显示参数类型不符合要求", e);
                        } catch (InvocationTargetException e){
                            throw new RuntimeException("调用方法method: " + method.getName() + "; 获取其参数值: " + nextValue + "执行时报错", e);
                        }
                    }
                    if(instance == null){
                        /* 表头的内容无法映射到对象的属性上，在return前会打印相关信息 */
                        continue;
                    }
                    entities.add(instance);
                }
            }
            if(entities.isEmpty()){
                log.debug("当前解析的文件没有获取到任何词条,header信息为: " + lineTextList.get(0));
            }
            parseFileInfo.setParsedObjects(entities);
            return parseFileInfo;
        } catch (Exception e) {
            throw e;
        } finally{

        }
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
    public <T> List<T> readZZExcelToEntity(String translateType,Class<T> classzz, InputStream in, String fileName, List<ExcelHead> excelHeads) throws Exception {
        checkFile(fileName);    //是否EXCEL文件
        Workbook workbook = getWorkBoot(in, fileName); //兼容新老版本
        List<T> excelForBeans = readExcel(translateType,classzz, workbook, excelHeads);  //解析Excel
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
    public <T> ParseFileInfo<T> readExcelToEntity(String translateType ,Class<T> classzz, InputStream in, String fileName) throws Exception {
        return readExcelToEntity(translateType,classzz, in, fileName, null);
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
    public <T> List<T> readZZExcelToEntity(String translateType,Class<T> classzz, InputStream in, String fileName) throws Exception {
        return readZZExcelToEntity(translateType,classzz, in, fileName, null);
    }

    /**
     * 校验是否是Excel文件
     *
     * @param fileName
     * @throws Exception
     */
    public void checkFile(String fileName) throws Exception {
        if (!StringUtils.isEmpty(fileName) && !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls") || fileName.endsWith(".csv"))) {
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
    private <T> List<T> readExcel(String translateType,Class<T> classzz, Workbook workbook, List<ExcelHead> excelHeads) throws Exception {
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
                /*    if ((headName.equals("英文术语") ||
                            headName.equals("西文术语") ||
                            headName.equals("俄文术语") ||
                            headName.equals("法文术语") ||
                            headName.equals("术语英文释义")||
                            headName.equals("术语法文释义")||
                            headName.equals("术语俄文释义")||
                            headName.equals("术语西文释义")) && !headName.contains(translateType)){
                        break;
                    }*/
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
                            headName.equals("法文术语") ||
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
                        System.out.println(field.getName());
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
    private <T> ParseFileInfo<T> readNewExcel(Class<T> classzz, Workbook workbook, List<ExcelHead> excelHeads) throws Exception {
        List<T> beans = new ArrayList<T>();
        int sheetNum = workbook.getNumberOfSheets();
        ParseFileInfo<T> parseFileInfo = new ParseFileInfo<>();
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
            String[] columnNames = this.getFileColumns(head);
            parseFileInfo.setColumnName(columnNames);
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
        parseFileInfo.setParsedObjects(beans);
        return parseFileInfo;
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

        public static Collection<Method> methods(Class<?> clazz,Collection<String> fieldNames,Function<String,String> methodNameGetFunction){
            Collection<Method> methods = new ArrayList<>();
            fieldNames.stream().forEach((fieldName)->{
                try {
                    String methodName = methodNameGetFunction.apply(fieldName);
                    Method method = clazz.getMethod(methodName);
                    methods.add(method);
                } catch(Exception e){
                    e.printStackTrace();;
                }

            });
            return methods;
        }

        public static final Function<String,String> DEFAULT_GET_METHOD_NAME_GENERATOR = (t)->{return t != null ? MethodUtils.getMethodName(t) : "";};

        public static final Function<String,String> DEFAULT_SET_METHOD_NAME_GENERATOR = (t)->{return t != null ? MethodUtils.setMethodName(t) : "";};

        /**
         * 获取指定属性名的方法
         * @param <T>
         * @param clazz
         * @param fieldName
         * @param methodNameGenerator
         * @param parameterTypes
         * @return
         */
        public static <T> Method acquireMethod(Class<T> clazz,String fieldName,Function<String,String> methodNameGenerator,Class<?>... parameterTypes){
            if(clazz == null){
                throw new NullPointerException("clazz为null");
            }
            if(fieldName.isEmpty() || fieldName.trim().isEmpty()){

            }
            String methodName = "";
            try {
                methodName = methodNameGenerator.apply(fieldName);
                Method method = clazz.getMethod(methodName, parameterTypes);
                return method;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(String.format("没有找到对应的方法, 方法名为: %s",methodName));
            } catch (SecurityException e) {
                throw new RuntimeException(String.format("类与方法不匹配, 类名为 :%s, 方法名为: %s", clazz.getName(),methodName));
            } catch (Exception e){
                throw new RuntimeException(String.format("出现其他异常, 异常信息为: %s", e.getMessage()));
            }
        }


        public static class MethodDefinition{

            public String methodName;

            public Class<?>[] parameterTypes;

            public MethodDefinition(String methodName) {
                this.methodName = methodName;
            }

            public MethodDefinition(String methodName, Class<?>... parameterTypes) {
                this.methodName = methodName;
                this.parameterTypes = parameterTypes;
            }

            public String getMethodName() {
                return methodName;
            }

            public Class<?>[] getParameterTypes() {
                return parameterTypes;
            }
        }

        public static class MethodEntity{

            Method method;
            /* 方法实际调用时的传参 */
            Object[] params;

            public MethodEntity(Method method) {
                this.method = method;
            }

            public MethodEntity(Method method, Object... params) {
                this.method = method;
                this.params = params;
            }

            public Method getMethod() {
                return method;
            }

            public void setParams(Object... params) {
                this.params = params;
            }

            public Object[] getParams() {
                return params;
            }

            public Object invoke(Object instance){
                try {
                    Object objectValue = this.method.invoke(instance,this.getParams());
                    return objectValue;
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    throw new RuntimeException(String.format("该对象在更新方法中无法调用方法: %s", this.method.getName()));
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    throw new RuntimeException(String.format("方法: %s对应的参数不符合要求, 参数列表: %s, 请检查", this.method.getName(),this.params.toString()));
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    throw new RuntimeException(e);
                }
            }
            
        }

        public static class PropertyMethods<T>{

            public Class<T> clazz = null;

            public Method getMethod = null;

            public Method setMethod = null;

            public PropertyMethods(Class<T> clazz,MethodDefinition getMethodDefinition,MethodDefinition setMethodDefinition){
 
                this.clazz = clazz;
                String getMethodName = MethodUtils.getMethodName(getMethodDefinition.getMethodName());
                String setMethodName = MethodUtils.setMethodName(setMethodDefinition.getMethodName());

                try {
                    Method getMethod = clazz.getMethod(getMethodName,getMethodDefinition.getParameterTypes());
                    this.getMethod = getMethod;

                    Method setMethod = clazz.getMethod(setMethodName,setMethodDefinition.getParameterTypes());
                    this.setMethod = setMethod;
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(String.format("没有找到对应的方法, get方法名为: %s, set方法名为: %s", getMethodName,setMethodName));
                } catch (SecurityException e) {
                    throw new RuntimeException(String.format("类与方法不匹配, 类名为 :%s, get方法名为: %s, set方法名为: %s", clazz.getName(),getMethodName,setMethodName));
                } catch (Exception e){
                    throw new RuntimeException(String.format("出现其他异常, 异常信息为: %s", e.getMessage()));
                }
            }


            public Class<T> getClazz() {
                return clazz;
            }


            public Method getGetMethod() {
                return getMethod;
            }


            public Method getSetMethod() {
                return setMethod;
            }


            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
                result = prime * result + ((getMethod == null) ? 0 : getMethod.hashCode());
                result = prime * result + ((setMethod == null) ? 0 : setMethod.hashCode());
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                PropertyMethods other = (PropertyMethods) obj;
                if (clazz == null) {
                    if (other.clazz != null)
                        return false;
                } else if (!clazz.equals(other.clazz))
                    return false;
                if (getMethod == null) {
                    if (other.getMethod != null)
                        return false;
                } else if (!getMethod.equals(other.getMethod))
                    return false;
                if (setMethod == null) {
                    if (other.setMethod != null)
                        return false;
                } else if (!setMethod.equals(other.setMethod))
                    return false;
                return true;
            }

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
                case ConstantInterface.CHINESE:
                    translateEntity = translateMapper.selectById(entryInfoEntity.getZhTransId());
                    if (Objects.nonNull(translateEntity)) {
                        outputExcel.setTranslate(translateEntity.getTranslate());
                    }
                    break;
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
    public void getWorkBook(ByteArrayOutputStream byteArrayOutputStream,List<?> dataList, List<String> exportFields, List<String> heards, String excelName) {
        if(byteArrayOutputStream == null){
            throw new NullPointerException("byteArrayOutputStream == null");
        }
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
            CellStyle cellStyle = workbook.createCellStyle();
            for (int colIndex = 0; colIndex < exportFields.size(); colIndex++) {
                String fieldName = exportFields.get(colIndex);


                try {
                    // 根据字段名获取对应的 getter 方法
                    Method method = dataObj.getClass().getMethod("get" + capitalize(fieldName));
                    // 调用 getter 方法获取字段值
                    Object fieldValue = method.invoke(dataObj);
                    // 创建单元格并设置字段值
                    Cell dataCell = dataRow.createCell(colIndex);

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
                        String dateString = LocalTimeUtils.formatForFile.format(fieldValue);
                        dataCell.setCellValue(dateString);
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


        try {
            // if(byteArrayOutputStream == null)
            workbook.write(byteArrayOutputStream);
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.error("向byte缓冲输出流写入数据时报错", e);

        } finally{
            try {
                workbook.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                log.error("关闭workbook的流时报错", e);
            }
        }


        // // 设置响应头信息
        // try {
            // excelName = URLEncoder.encode(excelName, "UTF-8");
        //     response.setContentType("application/octet-stream;charset=UTF-8");
        //     response.setHeader("Content-disposition", "attachment;filename=" + excelName + ".xlsx");
        //     response.addHeader("Pargam", "no-cache");
        //     response.addHeader("Cache-Control", "no-cache");
        //     response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        //     // 将工作簿写入响应输出流中
        //     workbook.write(response.getOutputStream());
        //     workbook.close();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
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
    /**
     * CSV每个单元格文本的内容修正，如果text中存在'"'或','或特殊字符例如'\n'，则首尾添加'"',
     * 如果text中出现一个'"',则再添加一个'""
     * 
     * @param text
     * @return  如果{@code text}是null,则返回null；如果{@code text}是空字符串，则返回""；
     */
    public static String processStringToSaveForCSV(String text){
        if(text == null){
            return null;
        }
        int length = text.length();
        if(length == 0){
            return text;
        }
        StringBuilder builder = new StringBuilder();
        boolean containsSpecialChar = false;
        for(int i = 0; i < length ; i ++ ){
            char nextChar = text.charAt(i);
            if(nextChar == '"'){
                builder.append("\"\"");
                containsSpecialChar = true;
            }else if(nextChar == ','){
                containsSpecialChar = true;
                builder.append(nextChar);
            }else{
                if(specialCharacterForCSV.contains(nextChar)){
                    containsSpecialChar = true;
                }
                builder.append(nextChar);
            }
        }
        if(containsSpecialChar){
            return "\"" + builder.toString() + "\"";
        }else{
            return builder.toString();
        }
    }
    /**
     * 如果{@code textList}是null，则返回null；如果{@code textList}是空的，则返回"\n"；
     * @param textList
     * @return
     */
    public static String csvBuildLine(List<String> textList){
        if(textList == null){
            return null;
        }
        int size = textList.size();
        StringBuilder builder = new StringBuilder();
        for(int i = 0 ; i < size ; i ++ ){
            builder.append(processStringToSaveForCSV(textList.get(i)));
            if(i + 1 != size){
                builder.append(",");
            }
        }
        builder.append("\n");
        return builder.toString();
    }

    public static <T extends EntryInfoEntityForExcel> void csvBuildLines(List<String> entityLines,List<T> entities,List<Method> methodList){
        if(entityLines == null){
            throw new NullPointerException("entityLines == null");
        }
        for(EntryInfoEntityForExcel entity : entities){
            if(entity == null){
                continue;
            }
            List<String> entityContents = new ArrayList<>();
            for(Method method : methodList){
                try {
                    method.invoke(entity);
                    Object fieldValue = method.invoke(entity);
                    if(fieldValue == null){
                        entityContents.add("");
                    }else{
                        if(method.getReturnType() == Date.class){
                            fieldValue = LocalTimeUtils.formatForFile.format(fieldValue);
                        }
                        entityContents.add(String.valueOf(fieldValue));
                    }
                }catch (IllegalArgumentException e){
                    throw new RuntimeException("函数名: " + method.getName() + "在默认无参调用时报IllegalArgumentException异常");
                }catch (SecurityException e) {
                // TODO Auto-generated catch block
                    throw new RuntimeException(e);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }

            }
            String entityLine = csvBuildLine(entityContents);
            if(entityLine == null){
                throw new NullPointerException("entityLine为null");
            }
            entityLines.add(entityLine);
        }
        return ;
    }

    public <T extends EntryInfoEntityForExcel> List<String> exportEntitiesToCSV(List<T> entities,Class<T> classzz,List<String> exportFields,List<String> columnNames){
        
        // 设置响应头信息
        try {
            if(entities == null || exportFields == null || columnNames == null){
                throw new RuntimeException("entities == null || exportFields == null || columnNames == null");
            }
            if(exportFields.size() != columnNames.size()){
                throw new RuntimeException("exportFields.size() != columnNames.size()");
            }
            List<Method> methodList = new ArrayList<>(exportFields.size());
            // 写header
            String columnLine = csvBuildLine(columnNames);
            if(columnLine == null || !columnLine.endsWith("\n")){
                throw new NullPointerException("columnLine == null || !columnLine.endsWith(\"\n\")");
            }
            for(String field: exportFields){
                String methodName = MethodUtils.getMethodName(field);
                Method method = null;
                try {
                    method = classzz.getMethod(methodName);
                    if(method == null){
                        throw new NullPointerException("MethodUtils.getMethodName(field)返回结果为null, field: " + field);
                    }
                }catch (NoSuchMethodException e) {
                        // TODO Auto-generated catch block
                    throw new RuntimeException("没有名称为: " + methodName + "的方法");
                } catch (Exception e) {
                   throw new RuntimeException("获取methodName: " + methodName + "的方法时异常", e);
                }
                methodList.add(method);
            }
            // 写entities
            List<String> contentLines = new ArrayList<>(entities.size() + 1);
            contentLines.add(columnLine);
            csvBuildLines(contentLines,entities, methodList);
            return contentLines;
        } catch (Exception e){
            throw new RuntimeException(e);
        } finally{
            // response.setContentType("application/octet-stream;charset=" + charsetName);
            // response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".csv");
            // response.addHeader("Pargam", "no-cache");
            // response.addHeader("Cache-Control", "no-cache");
            // response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            // /* outputstream流必须在header之后写 */
            // try {
            //     outputStream = response.getOutputStream();
            //     if(columnLine != null) outputStream.write(columnLine.getBytes(charsetName));
            //     if(entityLines != null){
            //         for(String entityLine : entityLines){
            //             outputStream.write(entityLine.getBytes(charsetName));
            //         }
            //     }
            // }catch (Exception e){
            //     log.error(null, e);
            // } 
        }
    }


    public <T> ParseFileInfo<T> parseFileToEntity(Class<T> clazz,InputStream in,String originalFilename,KeyValueArguments<String> kwargs){
        try {
            if(originalFilename.endsWith("csv")){
                Map<String,String> keyValueMap = new HashMap<>();
                if(kwargs == null){
                    keyValueMap.put("emptyAsValue", String.valueOf(false));
                    keyValueMap.put("encoding", "GBK");
                }else{
                    Boolean emptyAsValue = kwargs.get("emptyAsValue", Boolean.class);
                    keyValueMap.put("emptyAsValue", emptyAsValue == null ? String.valueOf(false) : String.valueOf(emptyAsValue));
                    String encoding = kwargs.get("encoding", String.class);
                    keyValueMap.put("encoding", encoding == null || encoding.isEmpty() ? "GBK" : encoding);
                }
                return this.readCSVToEntity(clazz, in, originalFilename, keyValueMap);
            }else if(originalFilename.endsWith(".xlsx")){
                return this.readExcelToEntity(null, clazz, in, originalFilename);
            }else{
                throw new RuntimeException("不支持当前的文件格式, 文件不是csv和xlsx类型");
            }                 
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
   
    }

    public String[] getFileColumns(Row head){
        short firstCellNum = head.getFirstCellNum();
        short lastCellNum = head.getLastCellNum();
        String[] columns = new String[lastCellNum - firstCellNum];
        for(int cellIndex = firstCellNum; cellIndex < lastCellNum; cellIndex++){
            Cell headCell = head.getCell(cellIndex);
            String columnName = headCell.getStringCellValue();
            columns[cellIndex - firstCellNum] = columnName;
        }
        return columns;
    }

    public <T> Collection<String> getAttributesByFileColumns(Map<String,String> columnNameAttributeMap,String[] columnLine){
        Collection<String> attributes = new HashSet<>();
        for(String columnName : columnLine){
            columnNameAttributeMap.forEach((field,targetColumnName)->{
                if(targetColumnName.equals(columnName)){
                    attributes.add(field);
                }
            });
        }   
        return attributes;
    }



    public class ParseFileInfo<T>{

        Collection<T> parsedObjects;

        String[] columnName;

        public ParseFileInfo() {
        }

        public ParseFileInfo(Collection<T> parsedObjects, String[] columnName) {
            this.parsedObjects = parsedObjects;
            this.columnName = columnName;
        }

        public Collection<T> getParsedObjects() {
            return parsedObjects;
        }

        public String[] getColumnName() {
            return columnName;
        }

        public void setParsedObjects(Collection<T> parsedObjects) {
            this.parsedObjects = parsedObjects;
        }

        public void setColumnName(String[] columnName) {
            this.columnName = columnName;
        }

    }
}
