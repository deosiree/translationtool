package com.shr.translationtoolservice.service;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.multipart.MultipartFile;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.EntryInfoEntityForExcel;
import com.shr.translationtoolservice.entity.vo.EntryInfoGroupVO;
import com.shr.translationtoolservice.entity.vo.ExcelExportVO;
import com.shr.translationtoolservice.entity.vo.UpdateTranslationByFileVO;

public interface EntryFileService {
    
    /***********************************************词条导入相关功能*******************************************************/
    /**
     * 工作台利用文件导入词条
     * @param multipartFile
     * @param taskID
     * @param departmentType
     * @param request
     * @return
     */
    List<EntryInfoEntity> importEntitiesFromFile(MultipartFile multipartFile,String taskID,String departmentType,HttpServletRequest request);

    /**
     * 将文件的内容解析成词条对象, 没有后处理操作
     * @param multipartFile
     * @param request
     * @return
     */
    Collection<EntryInfoEntity> parseFileToEntryInfos(MultipartFile multipartFile,HttpServletRequest request);

    /**
     * 更新翻译接口(送翻文件解析, 更新库里词条的翻译)
     * @param multipartFile
     * @param token
     * @param transType
     * @return
     */
    UpdateTranslationByFileVO importTransExcle(MultipartFile multipartFile, String token,String transType);

    /**
     * 更新翻译接口(送翻文件解析, 更新库里词条的翻译)(利用去重的翻译文件,以及去重时使用的词条id关联表)
     * @param multipartFile
     * @param token
     * @param transType
     * @return
     */
    UpdateTranslationByFileVO importTransExcle(MultipartFile multipartFile, String token, String transType,Map<String, Set<String>> idRelationMap);


    List<EntryInfoEntity> importCommonExcle(MultipartFile multipartFile, String taskID);

    List<EntryInfoEntity> workImportExcleTrans(MultipartFile multipartFile,  String taskID);


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
    boolean entryExportByCondition( ByteArrayOutputStream buffer,ExcelExportVO<EntryInfoEntityForExcel> excelExportVO,String taskID);

    /**
     * 为导出的文件进行去重, 返回去重后的所有的词条, 以及去重后保留的词条的ID与未保留的其他词条的ID的关联关系
     * 将文件中的词条进行分组，key代表这一组词条中的其中一个词条, value是这组词条中的其他词条
     * @param multipartFile
     * @return
     */
    EntryInfoGroupVO makeGroupForEntryInfosOnExportedFile(MultipartFile multipartFile,Collection<String> replicatedTargetAttributes);
}
