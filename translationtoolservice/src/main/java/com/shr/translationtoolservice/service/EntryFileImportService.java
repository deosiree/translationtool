package com.shr.translationtoolservice.service;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.multipart.MultipartFile;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.FileInputStreamEntity;
import com.shr.translationtoolservice.entity.KeyValueArguments;
import com.shr.translationtoolservice.entity.vo.UpdateEntryInfoByFileVO;
import com.shr.translationtoolservice.entity.vo.check.TaskCheckResultVO;
import com.shr.translationtoolservice.entity.vo.check.TaskRequest;

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
    Collection<EntryInfoEntity> importEntitiesFromFile(FileInputStreamEntity fileInputStreamEntitiy,String taskID,String departmentType,String encoding,HttpServletRequest request);

    List<EntryInfoEntity> importCommonExcle(FileInputStreamEntity fileInputStreamEntitiy, String taskID);

    List<EntryInfoEntity> workImportExcleTrans(FileInputStreamEntity fileInputStreamEntitiy,  String taskID);

    /**
     * 利用提供的文件更新词条的信息(不包含翻译信息)
     * @param fileInputStreamEntitiy
     * @param updateColumnNames
     * @param kwargs
     * @return
     */
    UpdateEntryInfoByFileVO updateEntryInfosByFile(FileInputStreamEntity fileInputStreamEntitiy,Collection<String> updateFieldNames,KeyValueArguments<String> keyValueArguments);

    // UpdateEntryInfoByFileVO updateEntryInfosTranslationByFile(FileInputStreamEntity fileInputStreamEntitiy,Collection<String> translationColumnNames,KeyValueArguments<String> keyValueArguments);


    /**
     * 更新翻译接口(送翻文件解析, 更新库里词条的翻译)(利用去重的翻译文件,以及去重时使用的词条id关联表), idRelationMap传参为null,调用{@link #importTransExcle(MultipartFile, String, String)}进行更新翻译
     * @param multipartFile
     * @param token
     * @param transType
     * @return
     */
    UpdateEntryInfoByFileVO importTransExcle(FileInputStreamEntity fileInputStreamEntitiy, String token, String transType,String encoding ,Map<String, Set<String>> idRelationMap);

    UpdateEntryInfoByFileVO importTransExcle(Collection<EntryInfoEntity> entryInfoEntities, String token, String transType,Map<String, Set<String>> idRelationMap);


    TaskCheckResultVO checkBeforeUpdateTranslationByFile(
        FileInputStreamEntity unTranslateFile,
        FileInputStreamEntity translatedFile,
        FileInputStreamEntity idRelationFile,
        TaskRequest taskRequest,
        String encodingForUnTranslatedFile,
        String encodingForTranslatedFile
    );


}
