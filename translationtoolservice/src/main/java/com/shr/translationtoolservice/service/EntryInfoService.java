package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.*;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.vo.*;
import com.shr.translationtoolservice.entity.vo.TsVo.TsEntryInfoVo;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public interface EntryInfoService extends IService<EntryInfoEntity> {

    List<EntryInfoEntity> getEntryByVersion(EntryInfoEntity entryInfoEntity,Integer offset,Integer pageSize);

    int getEntryByVersionTotal(EntryInfoEntity entryInfoEntity);

    HttpResponse<String> addEntryByVersion(List<EntryVO> entryVOS, HttpServletRequest request);

    String addEntryInfo(EntryInfoEntity entryInfoEntity, HttpServletRequest request,String tableName);

    String updateEntryInfo(EntryInfoEntity entryInfoEntity, HttpServletRequest request, String notes);

    String deleteEntryInfo(List<String> idList,String tableName);

    List<TranslateEntity> getPublicEntry(TranslateEntity translateEntity, int offset, Integer pageSize);

    int getPublicEntryTotal(TranslateEntity translateEntity);

    String updatePublicEntry(TranslateEntity translateEntity);

    String addPublicEntry(List<TranslateEntity> translateEntity);

    String deletePublicEntry(List<String> idlist);

    String upgrade(UpgradeVO upgradeVO, HttpServletRequest request);

    String updateEntryInfoList(List<EntryInfoEntity> entryInfoEntities, HttpServletRequest request, String notes);

    List<EntryInfoEntity>  importExcle(MultipartFile multipartFile,String taskID);

    boolean checkBeforeAddSingleEntry(EntryInfoEntity entryInfoEntity,String department);

    EntryInfoEntity addSingleEntry(EntryInfoEntity entryInfoEntity, HttpServletRequest request);

    EntryInfoEntity copyEntry(EntryInfoEntity entryInfoEntity,HttpServletRequest request);

    TranslateEntities translate(String name, String type,String visualRange);

    String updateEntryTemp(List<EntryTempEntity> entryTempEntities, HttpServletRequest request);

    void addTransID(TranslateEntity translateEntities, EntryInfoEntity entryInfoEntity);

    void versionExport(String versionID, HttpServletResponse response,String translateType);

    List<EntryInfoEntity> importEntitiesFromFile(MultipartFile multipartFile,String taskID,String departmentType,HttpServletRequest request);

    // List<EntryInfoEntity> importZZExcle(MultipartFile multipartFile, String taskID,HttpServletRequest httpServletRequest);

    List<EntryInfoEntity> insertEntry(List<EntryInfoEntity> entryInfoEntities,String taskID,HttpServletRequest httpServletRequest);

    List<EntryInfoEntity> insertEntry(List<EntryInfoEntity> entryInfoEntities,String taskID,String userName,String department);


    String addEntryAudit(List<EntryInfoEntity> entryInfoEntities, String taskID, HttpServletRequest request);

    String createVersionByEntry(List<EntryInfoEntity> entryInfoEntities, String productID, String common, String versionName, HttpServletRequest request);

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

    List<EntryClassify> getClassfy(String parentId, String type);

    String addProductRelation(List<EntryInfoEntity>  relationEntity,HttpServletRequest request) throws Exception;

    List<EntryInfoEntity> importCommonExcle(MultipartFile multipartFile, String taskID);

    List<EntryInfoEntity>  filterSourceLanguage(List<EntryInfoEntity> entryInfoEntities, String languageType);



    String setInfoByEntryList(List<EntryInfoEntity> entryInfoEntities, String translateType, String writeType, boolean tag, boolean comment, String fileName,String i18nUrl);

    Collection<EntryInfoEntity> importTransExcle(MultipartFile multipartFile, HttpServletRequest httpServletRequest,String transType);

    List<EntryInfoEntity> workImportExcleTrans(MultipartFile multipartFile,  String taskID);

    List<EntryInfoEntity> capitalizeWords(List<EntryInfoEntity> entryInfoEntities, String changeType,String translateType);

    List<EntryInfoEntity> replaceWords(List<EntryInfoEntity> entryInfoEntities, String sourceStr, String replaceStr,String translateType);

    // int getEntryByClassfTotal(EntryInfoEntity entryInfoEntity, String classfyID,String translateType);

    List<EntryInfoEntity> checkNotUseEntry(String i18nUrl,String classfyID, Map<String,List<String>> soucreMap)
     throws Exception;

    List<EntryInfoEntity> checkNotUseEntry(String i18nUrl,List<EntryInfoEntity> entities,Map<String,List<String>> sourceMap);


    List<EntryInfoEntity> checkNotUseEntry(String i18nUrl,String classfyID,  Map<String,List<String>> soucreMap,Map<String,String> condition) throws Exception;


    List<TaskEntryVO> checkNewEntryByClassfy(String i18nUrl, String classfyID, HttpServletRequest request);

    List<SourceEntryVO> getEntrysourceListByClassfy( String classfyID,String i18nUrl,HttpServletRequest request,List<String> exceptionMessage);

    void doWeight();

    List<String> updateEntryByEntrySource(List<SourceEntryVO> sourceEntryVOS,HttpServletRequest request);

    EntryInfoVO getEntryByClassfy(EntryInfoEntityQO entryInfoEntityTemplate,Set<String> clearMatchSet,String classfyID,String startTime,String endTime,Integer pageIndex,Integer pageSize);

    List<EntryInfoEntity> onPage(List<EntryInfoEntity> entryInfoEntities,Integer pageIndex,Integer pageSize);

    List<EntryInfoEntity> filterEntryInfoByTransInfo(List<EntryInfoEntity> entryInfoEntities,EntryInfoEntityQO entryInfoEntityTemplate,String translateType);


    List<EntryInfoEntity> getEntryInfoByTsVo(List<TsVo> tsVoList);
    /* 禁用词条 */
    boolean forrbiddenEntry(List<EntryInfoEntity> entryInfoEntities,HttpServletRequest request);

    Set<String> getEntrySourcesByClassify(String classifID);

}
