package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.dao.TranslateMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.ImportExcleVO;
import com.shr.translationtoolservice.service.EntryTempService;
import com.shr.translationtoolservice.dao.EntryTempMapper;
import com.shr.translationtoolservice.util.ExcelUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

/**
 *
 */
@Service
public class EntryTempServiceImpl extends ServiceImpl<EntryTempMapper, EntryTempEntity>
        implements EntryTempService {

    @Autowired
    private EntryTempMapper entryTempMapper;

    @Autowired
    private EntryInfoMapper entryInfoMapper;

    @Autowired
    private TranslateMapper translateMapper;

    @Value("${ConfigFile.url}")
    private String configFileUrl;

    @Autowired
    private ExcelUtils excelUtils;

    @Override
    public String insertEntry(List<EntryTempEntity> tempEntities) {
        int insert = 0;
        for (EntryTempEntity entryTempEntity : tempEntities) {
            if (!CollectionUtils.isEmpty(entryTempEntity.getChildren())) {
                for (EntryTempEntity entryTempEntity1 : entryTempEntity.getChildren()) {
                    entryTempEntity1.setChildren(null);
                    insert += entryTempMapper.insert(entryTempEntity1);
                }
            }
            entryTempEntity.setChildren(null);
            insert += entryTempMapper.insert(entryTempEntity);
        }
        if (insert < tempEntities.size()) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;

    }

    @Override
    public String updateEntryTemp(List<EntryTempEntity> tempEntities) {
        int update = 0;
        for (EntryTempEntity entryTempEntity : tempEntities) {
            List<EntryTempEntity> childTempEntry = entryTempEntity.getChildren();
            if (!CollectionUtils.isEmpty(childTempEntry)){
                for (EntryTempEntity entryTempEntity1 : childTempEntry){
                    update += entryTempMapper.updateById(entryTempEntity1);
                }
            }
            update += entryTempMapper.updateById(entryTempEntity);
        }
        if (update < tempEntities.size()) {
            log.error(" entryInfoEntity update  error ! ");
            return ErrorCodeList.OPERATE_ERROR;
        }
        return ConstantInterface.OK_STR;
    }

    @Override
    public List<EntryTempEntity> getEntryTempByTaskID(String taskID) {
        List<EntryTempEntity> newEntryTemp = new ArrayList<>();
        List<EntryTempEntity> entryTempEntities = entryTempMapper.getEntryTempByTaskID(taskID);
        int sum = 0;
        //entryid -> tempEntry
        Map<String, EntryTempEntity> entryTempEntityMap = new HashMap<>();
        for (EntryTempEntity childEntryTemp : entryTempEntities) {
            String parentID = childEntryTemp.getParentID();
            //构建聚合结构
            if (StringUtils.isNotBlank(parentID)) {
                EntryTempEntity parentEntryTemp = entryTempEntityMap.get(parentID);
                //判断map 空 则找到父 放到map里 不是空则把子放到父里
                if (Objects.isNull(parentEntryTemp)) {
                    for (EntryTempEntity parentEntryTemp1 : entryTempEntities) {
                        if (parentID.equals(parentEntryTemp1.getId())) {
                            ArrayList<EntryTempEntity> entityArrayList = new ArrayList<>();
                            entityArrayList.add(childEntryTemp);
                            parentEntryTemp1.setChildren(entityArrayList);
                            entryTempEntityMap.put(parentEntryTemp1.getId(), parentEntryTemp1);
                            sum += 1;
                        }
                    }

                } else {
                    if (CollectionUtils.isEmpty(parentEntryTemp.getChildren())) {
                        ArrayList<EntryTempEntity> childList = new ArrayList<>();
                        childList.add(childEntryTemp);
                        parentEntryTemp.setChildren(childList);
                        sum += 1;
                    } else {
                        parentEntryTemp.getChildren().add(childEntryTemp);
                        sum += 1;
                    }
                }

            } else {
                entryTempEntityMap.put(childEntryTemp.getId(), childEntryTemp);
                sum += 1;
            }
        }
        for (EntryTempEntity entryTempEntity : entryTempEntityMap.values()) {
            newEntryTemp.add(entryTempEntity);
        }
        log.warn( " ==== sum is : " + sum + " ==== ");
        return newEntryTemp;
    }

    @Override
    public String deleteEntryTempByID(List<String> entryID) {
        int delete = entryTempMapper.deleteBatchIds(entryID);
        return ConstantInterface.OK_STR;
    }

    @Override
    public int getEntryTempByTaskIDTotal(String taskID) {

        return entryTempMapper.getEntryTempByTaskIDTotal(taskID);
    }

    @Override
    public List<EntryTempEntity> preTranslate(String taskID) {
        List<EntryTempEntity> entryTempByTaskID = entryTempMapper.getEntryTempByTaskID(taskID);
        List<EntryTempEntity> entryTempEntities = new ArrayList<>();
        for (EntryTempEntity entryTempEntity : entryTempByTaskID){
            //子不翻译
            if (StringUtils.isNotBlank(entryTempEntity.getParentID())){
                continue;
            }
            String entry = entryTempEntity.getEntry();
            if (StringUtils.isNotBlank(entryTempEntity.getTranslate())){
                entryTempEntities.add(entryTempEntity);
                continue;
            }
            List<TranslateEntity> versionSuggestTrans = translateMapper.getVersionSuggestTrans(entry, entryTempEntity.getTranslateType());
            String translate = "";
            if (!CollectionUtils.isEmpty(versionSuggestTrans)){
                 translate = versionSuggestTrans.get(0).getTranslate();
            }

            entryTempEntity.setTranslate(translate);
            entryTempEntities.add(entryTempEntity);
        }
        return entryTempEntities;
    }

    @Override
    public void getTemplateFile(HttpServletResponse response) {
        try {
            String fileName = "模板文件.xls";
            fileName = URLEncoder.encode(fileName, "UTF-8");
            log.warn( " **** fileName : " + fileName + " ***** ");
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.addHeader("Pargam", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            response.setStatus(200);

            FileInputStream fileInputStream = new FileInputStream(configFileUrl);
            ServletOutputStream outputStream = response.getOutputStream();
            HSSFWorkbook workbook = new HSSFWorkbook(fileInputStream);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();

        } catch (Exception e) {
            log.error("代码生成出错", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.sendError(500, "代码生成出错，无法下载");
            } catch (IOException ex) {
                log.error("响应报错信息出错", e);
            }
        }

    }


}




