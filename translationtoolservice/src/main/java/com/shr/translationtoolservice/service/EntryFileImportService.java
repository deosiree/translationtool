package com.shr.translationtoolservice.service;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.multipart.MultipartFile;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.FileInputStreamEntitiy;
import com.shr.translationtoolservice.entity.vo.UpdateTranslationByFileVO;
import com.shr.translationtoolservice.entity.vo.ValueDifferenceVO;
import com.shr.translationtoolservice.entity.vo.exception.ExceptionVO;

public interface EntryFileImportService {

    /***********************************************词条导入相关功能*******************************************************/
    /**
     * 工作台利用文件导入词条(相当于新添加)
     * @param multipartFile
     * @param taskID
     * @param departmentType
     * @param request
     * @return
     */
    List<EntryInfoEntity> importEntitiesFromFile(FileInputStreamEntitiy fileInputStreamEntitiy,String taskID,String departmentType,HttpServletRequest request);

    List<EntryInfoEntity> importCommonExcle(FileInputStreamEntitiy fileInputStreamEntitiy, String taskID);

    List<EntryInfoEntity> workImportExcleTrans(FileInputStreamEntitiy fileInputStreamEntitiy,  String taskID);

    /**
     * 更新翻译接口(送翻文件解析, 更新库里词条的翻译)
     * @param multipartFile
     * @param token
     * @param transType
     * @return
     */
    UpdateTranslationByFileVO importTransExcle(FileInputStreamEntitiy fileInputStreamEntitiy, String token,String transType);

    /**
     * 更新翻译接口(送翻文件解析, 更新库里词条的翻译)(利用去重的翻译文件,以及去重时使用的词条id关联表), idRelationMap传参为null,调用{@link #importTransExcle(MultipartFile, String, String)}进行更新翻译
     * @param multipartFile
     * @param token
     * @param transType
     * @return
     */
    UpdateTranslationByFileVO importTransExcle(FileInputStreamEntitiy fileInputStreamEntitiy, String token, String transType,Map<String, Set<String>> idRelationMap);

    /**
     * 比对送翻前的文件和送翻后的文件, 检查id列是否存在丢失
     * @param translatedFile
     * @param beforeTranslatedFile
     */
    ValueDifferenceVO<EntryInfoEntity> compareEntryIDsBetweenFiles(FileInputStreamEntitiy translatedFile,FileInputStreamEntitiy beforeTranslatedFile);

    /**
     * 比对送翻后的文件和这组id关联关系是否匹配
     */
    Collection<ExceptionVO> compareTranslatedFileWithIDRelation(FileInputStreamEntitiy fileInputStreamEntitiy,Map<String, Set<String>> idRelationMap);


    /**
     * 利用提供的文件更新词条的信息
     * @param ins
     */
    // void updateEntryInfosByFile(FileInputStreamEntitiy fileInputStreamEntitiy);

}
