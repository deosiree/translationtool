package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
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
    public HttpResponse<ResponseListModel> importExcle(@RequestBody MultipartFile multipartFile) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryCommonEntity> entryEntities = entryInfoService.importExcle(multipartFile);
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
    public HttpResponse<ResponseListModel> getEntryTempByTaskID(@RequestParam String taskID,
                                                     @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                     @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryTempEntity> entryTempEntities = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
             entryTempEntities = entryTempService.getEntryTempByTaskID(taskID,offset,pageSize);

        }

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


}
