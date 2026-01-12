package com.shr.translationtoolservice.service;

import java.util.Collection;

import org.springframework.web.multipart.MultipartFile;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.vo.EntryInfoGroupVO;
import com.shr.translationtoolservice.entity.vo.ValueDifferenceVO;

public interface EntryFileService{

    /**
     * 功能: 将文件中的词条进行分组，key代表这一组词条中的其中一个词条, value是这组词条中的其他词条
     * 使用点：为导出的文件进行去重, 返回去重后的所有的词条, 以及去重后保留的词条的ID与未保留的其他词条的ID的关联关系
     * @param multipartFile
     * @return
     */
    EntryInfoGroupVO makeGroupForEntryInfosOnExportedFile(MultipartFile multipartFile,Collection<String> replicatedTargetAttributes);


    /**
     * 通过指定要用于比对的属性, 比较两个文件中词条的不同（通用接口）
     * @param firstFile
     * @param secondFile
     * @param targetAttributes  用于比对使用的属性名的集合
     */
    ValueDifferenceVO<EntryInfoEntity> compareEntryInfosBetweenFiles(MultipartFile file1,MultipartFile file2,Collection<String> targetAttributes);

    /**
     * 比对送翻前的文件和送翻后的文件, 检查id列是否存在丢失
     * @param translatedFile
     * @param beforeTranslatedFile
     */
    ValueDifferenceVO<EntryInfoEntity> compareTranslatedFileBeforeAfter(MultipartFile translatedFile,MultipartFile beforeTranslatedFile);


    // void compareTranslatedFileBefore

}
