package com.shr.translationtoolservice.service.entry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shr.translationtoolservice.dao.TLanguageMapper;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.FileInputStreamEntity;
import com.shr.translationtoolservice.entity.ImportExcleEntry;
import com.shr.translationtoolservice.entity.TLanguage;
import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.shr.translationtoolservice.entity.User;
import com.shr.translationtoolservice.entity.vo.WorkBenchVO.EntryImportFileTypeVO;
import com.shr.translationtoolservice.service.entry.BatchInsertEntryHandler.DictionaryVO;
import com.shr.translationtoolservice.service.parser.AbbrAppXMLParser;
import com.shr.translationtoolservice.service.parser.AbbrAppXMLParser.AbbrAppInfo;
import com.shr.translationtoolservice.service.parser.AbstractXMLParser.XMLInfoContainer;
import com.shr.translationtoolservice.service.parser.AppInfoDictXMLParser;
import com.shr.translationtoolservice.service.parser.AppInfoDictXMLParser.AppDictInfo;
import com.shr.translationtoolservice.service.parser.DICParser;
import com.shr.translationtoolservice.service.parser.TSParser;
import com.shr.translationtoolservice.service.processor.EntryImportProcessor;
import com.shr.translationtoolservice.service.processor.converter.AbbrAppInfoConverter;
import com.shr.translationtoolservice.service.processor.converter.AppDictInfoConverter;
import com.shr.translationtoolservice.service.processor.converter.DICDictionaryVOConverter;
import com.shr.translationtoolservice.service.processor.converter.TSDictionaryVOConverter;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.ExcelUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EntryInfoEntityImportHandler extends AbstractEntryImportHandler<EntryInfoEntity> {
    
    @Autowired
    protected ExcelUtils excelUtils;

    @Autowired
    protected CommonUtils commonUtils;

    @Autowired
    protected EntryImportProcessor entryImportProcessor;

    @Autowired
    protected TLanguageMapper languageMapper;

    @Autowired
    protected BatchInsertEntryHandler batchInsertEntryHandler;

    @Autowired
    protected TSParser tsParser;

    @Autowired
    protected DICParser dicParser;


    @Autowired
    protected AbbrAppXMLParser abbrAppXMLParser;

    @Autowired
    protected AbbrAppInfoConverter abbrAppInfoConverter;

    @Autowired
    protected AppInfoDictXMLParser appInfoDictXMLParser;
    
    @Autowired
    protected AppDictInfoConverter appDictInfoConverter;

    @Override
    public Collection<EntryInfoEntity> importExcel(
        FileInputStreamEntity fileInputStreamEntitiy, 
        User user,
        EntryImportFileTypeVO modeType, 
        TaskInfoEntity taskInfoEntity, 
        String encoding) throws Exception{

        String fileName = fileInputStreamEntitiy.getFileName();
        String userName = user.getUserName();
        String departmentType = user.getDepartment();
        List<EntryInfoEntity> entryInfoEntities = null;

        if(departmentType.equals("装置开发部")){
            if(modeType == null){
                throw new NullPointerException("传递的modeType参数不正确或为null");
            }
            if(modeType == EntryImportFileTypeVO.NEW_FILE_VERSION){
                throw new Exception("暂不支持,请使用其他模板");
            }else if(modeType == EntryImportFileTypeVO.OLD_FILE_VERSION){
                List<ImportExcleEntry> excelToEntity = excelUtils.readZZExcelToEntity(taskInfoEntity.getTranslateType(),ImportExcleEntry.class, fileInputStreamEntitiy.getInputStream(), fileName);
                entryInfoEntities = new ArrayList<>();
                entryImportProcessor.zzEntryHandle(excelToEntity, entryInfoEntities, userName, taskInfoEntity, fileName);
            }else if(modeType == EntryImportFileTypeVO.COMMON_VERSION){
                entryInfoEntities = excelUtils.readExcelToEntity(taskInfoEntity.getTranslateType(),EntryInfoEntity.class, fileInputStreamEntitiy.getInputStream(), fileName).getParsedObjects().stream().collect(Collectors.toList());
                entryImportProcessor.zzEntryHandle(entryInfoEntities, userName, taskInfoEntity, fileName);
            }else{
                throw new Exception("当前不支持该文件类型: " + EntryImportFileTypeVO.convertTOString(modeType));
            }
        }else{
            entryInfoEntities = excelUtils.readExcelToEntity(taskInfoEntity.getTranslateType(),EntryInfoEntity.class, fileInputStreamEntitiy.getInputStream(), fileName).getParsedObjects().stream().collect(Collectors.toList());
            entryImportProcessor.ptEntryHandle(entryInfoEntities, userName, taskInfoEntity, fileName);
        }
        return entryInfoEntities;
    }

    @Override
    public Collection<EntryInfoEntity> importCSV(
        FileInputStreamEntity fileInputStreamEntitiy, 
        User user,
        TaskInfoEntity taskInfoEntity, 
        String encoding) throws Exception {

        Map<String,String> kwargs = new HashMap<>();
        String fileName = fileInputStreamEntitiy.getFileName();
        String departmentType = user.getDepartment();
        String userName = user.getUserName();


        kwargs.put("encoding", encoding);
        List<EntryInfoEntity> entryInfoEntities = excelUtils.readCSVToEntity(EntryInfoEntity.class, fileInputStreamEntitiy.getInputStream(), fileName, kwargs).getParsedObjects().stream().collect(Collectors.toList());
        if(departmentType.equals("装置开发部")){
            entryImportProcessor.zzEntryHandle(entryInfoEntities, userName, taskInfoEntity, fileName);
        }else{
            entryImportProcessor.ptEntryHandle(entryInfoEntities, userName, taskInfoEntity, fileName);
        }
        return entryInfoEntities;
    }

    @Override
    public Collection<EntryInfoEntity> importXML(
        FileInputStreamEntity fileInputStreamEntitiy, 
        User user,    
        TaskInfoEntity taskInfoEntity,
        String template) throws Exception{

        String fileName = fileInputStreamEntitiy.getFileName();
        String departmentType = user.getDepartment();
        String userName = user.getUserName();
        Collection<EntryInfoEntity> entryInfoEntities = null;

        if(departmentType.equals("装置开发部")){
            if(template == null){
                throw new Exception("请确定xml采用何种模板");
            }
            if(template.equals("可视化词条")){
                // entryInfoEntities = 
                XMLInfoContainer<AbbrAppInfo> xmlInfoContainer = abbrAppXMLParser.parse(fileInputStreamEntitiy.getInputStream());
                Collection<AbbrAppInfo> abbrAppInfos = xmlInfoContainer.getCollection();
                entryInfoEntities = entryImportProcessor.convertToEntryInfos(abbrAppInfoConverter,abbrAppInfos);

            }else if(template.equals("装置辞典")){
                XMLInfoContainer<AppDictInfo> xmlInfoContainer = appInfoDictXMLParser.parse(fileInputStreamEntitiy.getInputStream());
                Collection<AppDictInfo> appDictInfos = xmlInfoContainer.getCollection();
                entryInfoEntities = entryImportProcessor.convertToEntryInfos(appDictInfoConverter,appDictInfos);

            }else{
                throw new Exception("当前不支持该版本的xml导入: " + template);
            }
            entryImportProcessor.zzEntryHandleForXML(entryInfoEntities, userName, taskInfoEntity, fileName);
        }else{
            throw new Exception("暂不支持XML导入");
        }
        return entryInfoEntities;
    }

    @Override
    public Collection<EntryInfoEntity> importTS(
        FileInputStreamEntity fileInputStreamEntitiy, 
        User user,
        TaskInfoEntity taskInfoEntity) throws Exception{

        String fileName = fileInputStreamEntitiy.getFileName();
        String userName = user.getUserName();

        List<TLanguage> tLanguages = languageMapper.selectList(new QueryWrapper<>());
        TLanguage targetLanguage = null;
        String entrySource = fileName.substring(0,fileName.indexOf("."));
        for (TLanguage tLanguage : tLanguages) {
            if (fileName.contains(tLanguage.getCode())) {
                entrySource = fileName.substring(0, fileName.indexOf("_" + tLanguage.getCode()));
                targetLanguage = tLanguage;
                break;
            }
        }
        if(targetLanguage == null){
            targetLanguage = languageMapper.getLanguageByTask(taskInfoEntity.getId());
        }

        /* 获取词条信息 */
        XMLInfoContainer<DictionaryVO> xmlInfoContainer = tsParser.parse(fileInputStreamEntitiy.getInputStream(), targetLanguage.getCode());
        Collection<DictionaryVO> dictionaryVOs = xmlInfoContainer.getCollection();

        TSDictionaryVOConverter tsDictionaryVOConverter = new TSDictionaryVOConverter(entrySource, taskInfoEntity.getId(), taskInfoEntity.getProductId(), taskInfoEntity.getVersionId(),commonUtils, userName, tLanguages);

        Collection<EntryInfoEntity> entryInfoEntities = entryImportProcessor.convertToEntryInfos(tsDictionaryVOConverter, dictionaryVOs);

        /* 词条后处理 */
        entryImportProcessor.ptTSEntryHandle(entryInfoEntities, userName, taskInfoEntity, fileName);
        return entryInfoEntities;

    }

    @Override
    public Collection<EntryInfoEntity> importDIC(
        FileInputStreamEntity fileInputStreamEntitiy, 
        User user,
        TaskInfoEntity taskInfoEntity, 
        String encoding) throws Exception {
        List<TLanguage> languages = languageMapper.selectLaguageByName(taskInfoEntity.getTranslateType());
        if(languages.isEmpty()){
            throw new Exception("");
        }
        String userName = user.getUserName();
        String fileName = fileInputStreamEntitiy.getFileName();

        /* 导入DIC文件词条 */
        Set<DictionaryVO> dictionaryVOs = dicParser.parse(fileInputStreamEntitiy.getInputStream(), encoding);

        int firstIndexOfDot = fileName.indexOf(".");
        String processedFileName = fileName.substring(0, firstIndexOfDot);
        String entrySource = "pt/" + processedFileName;    // 获取词条文件名
        String writeFileName = "tr/" + processedFileName;
        /* 获取词条信息 */
        DICDictionaryVOConverter dicDictionaryVOConverter = new DICDictionaryVOConverter(entrySource, taskInfoEntity.getId(), taskInfoEntity.getProductId(), taskInfoEntity.getVersionId(), commonUtils, languages, userName, writeFileName);

        Collection<EntryInfoEntity> entryInfoEntities = entryImportProcessor.convertToEntryInfos(dicDictionaryVOConverter, dictionaryVOs);

        entryImportProcessor.ptEntryHandle(entryInfoEntities, userName, taskInfoEntity, fileName);
        return entryInfoEntities;
    }


    
}
