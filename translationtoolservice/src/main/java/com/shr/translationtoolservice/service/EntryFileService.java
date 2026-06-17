package com.shr.translationtoolservice.service;

import java.util.Collection;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.FileInputStreamEntity;
import com.shr.translationtoolservice.entity.KeyValueArguments;
import com.shr.translationtoolservice.entity.vo.EntryInfoGroupVO;
import com.shr.translationtoolservice.entity.vo.ValueDifferenceVO;

public interface EntryFileService{

    /**
     * 功能: 将文件中的词条进行分组，key代表这一组词条中的其中一个词条, value是这组词条中的其他词条
     * 使用点：为导出的文件进行去重, 返回去重后的所有的词条, 以及去重后保留的词条的ID与未保留的其他词条的ID的关联关系
     * @param multipartFile
     * @return
     */
    EntryInfoGroupVO makeGroupForEntryInfosOnFile(FileInputStreamEntity multipartFile,Collection<String> replicatedTargetAttributes,String encoding);

    /**
     * 将文件的内容解析成词条对象, 没有后处理操作
     * @param multipartFile
     * @param request
     * @return
     */
    Collection<EntryInfoEntity> parseFileToEntryInfos(FileInputStreamEntity multipartFile,KeyValueArguments<String> keyValueArguments);


    /**
     * 通过指定要用于比对的属性, 比较两个文件中词条的不同（通用接口）
     * @param firstFile
     * @param secondFile
     * @param targetAttributes  用于比对使用的属性名的集合
     */
    ValueDifferenceVO<EntryInfoEntity> compareEntryInfosBetweenFiles(FileInputStreamEntity file1,FileInputStreamEntity file2,Collection<String> targetAttributes,KeyValueArguments<String> kwargs);



}
