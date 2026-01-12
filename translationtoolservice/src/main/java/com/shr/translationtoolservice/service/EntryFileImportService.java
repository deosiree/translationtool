package com.shr.translationtoolservice.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.multipart.MultipartFile;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.vo.UpdateTranslationByFileVO;

public interface EntryFileImportService {

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
    Collection<EntryInfoEntity> parseFileToEntryInfos(MultipartFile multipartFile,Map<String,Object> kwargs);

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
}
