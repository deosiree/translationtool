package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.entity.EntryCommonEntity;
import com.shr.translationtoolservice.entity.EntryTempEntity;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.service.EntryTempService;
import com.shr.translationtoolservice.util.CommonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName WorkBenchController
 * @Description 工作台
 * @USER: Cola
 * @Date 2023/12/13 0013 9:08
 **/


@RestController
@RequestMapping("/workbench")
@Api(tags = "工作台")
@Slf4j
public class WorkBenchController extends BaseController {

    @Autowired
    private EntryInfoService entryInfoService;
    @Autowired
    private EntryTempService entryTempService ;
    @Autowired
    private CommonUtils commonUtils;

    @PostMapping("/importExcle")
    @ApiOperation("导入excle")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> importExcle(@RequestParam("file") MultipartFile multipartFile,
                                                       @RequestParam("taskID") String taskID
                                                       ) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryTempEntity> entryEntities = entryInfoService.importExcle(multipartFile,taskID);
        responseListModel.setList(entryEntities);
        responseListModel.setTotalNum(entryEntities.size());

        return checkResult(responseListModel);
    }

    @PostMapping("/insertEntry")
    @ApiOperation("保存临时词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> insertEntry(@RequestBody List<EntryTempEntity> tempEntities) {

        String result = entryTempService.insertEntry(tempEntities);


        return checkResult(result);
    }

    @PostMapping("/updateEntryTemp")
    @ApiOperation("更新临时词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> updateEntryTemp(@RequestBody List<EntryTempEntity> tempEntities) {

        String result = entryTempService.updateEntryTemp(tempEntities);


        return checkResult(result);
    }

    @PostMapping("/getEntryTempByTaskID")
    @ApiOperation("查询临时词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getEntryTempByTaskID(@RequestParam String taskID) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryTempEntity> entryTempEntities = new ArrayList<>();

             entryTempEntities = entryTempService.getEntryTempByTaskID(taskID);



        responseListModel.setList(entryTempEntities);
        responseListModel.setTotalNum(entryTempService.getEntryTempByTaskIDTotal(taskID));

        return checkResult(responseListModel);
    }


    @PostMapping("/deleteEntryTempByID")
    @ApiOperation("删除临时词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> deleteEntryTempByID(@RequestBody List<String> entryID) {

        String result = entryTempService.deleteEntryTempByID(entryID);

        return checkResult(result);
    }

    @PostMapping("/preTranslate")
    @ApiOperation("预翻译")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> preTranslate(@RequestParam String taskID) {
        ResponseListModel responseListModel = new ResponseListModel();

        List<EntryTempEntity> entryTempEntities = entryTempService.preTranslate(taskID);
        responseListModel.setList(entryTempEntities);
        responseListModel.setTotalNum(entryTempEntities.size());
        return checkResult(responseListModel);
    }




}
