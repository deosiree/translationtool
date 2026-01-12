package com.shr.translationtoolservice.service;

import java.io.ByteArrayOutputStream;
import java.util.List;

import com.shr.translationtoolservice.entity.EntryInfoEntityForExcel;
import com.shr.translationtoolservice.entity.vo.ExcelExportVO;

public interface EntryFileExportService {

    /***********************************************词条导出相关功能*******************************************************/

    void getEntryInfoEntityForExport(ExcelExportVO<EntryInfoEntityForExcel> excelExportVO);
    /**
     * 过滤出不符合要求的词条
     * @param excelExportVO 
     * @return  返回不符合要求的词条,null代表均符合要求
     */
    List<EntryInfoEntityForExcel> filterEntryInfoBeforeExport(ExcelExportVO<EntryInfoEntityForExcel> excelExportVO);

    /**
     * 预处理提供的词条，主要查找词条要导出的产品和版本信息,没有过滤不符合规定的词条的功能
     * @param excelExportVO
     */
    void postProcessEntryInfoForExport(ExcelExportVO<EntryInfoEntityForExcel> excelExportVO);
    /**
     * 导出提供的词条到流中，可用于写入socket流和文件流
     * @param buffer
     * @param excelExportVO
     * @param taskID
     * @return excelExportVO 和 buffer 为null时为false，如果成功完成所有接口的调用，则返回true，导出存在异常场景时会抛出异常
     */
    boolean entryExportByCondition(ByteArrayOutputStream buffer,ExcelExportVO<EntryInfoEntityForExcel> excelExportVO,String taskID);



}
