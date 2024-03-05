package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.entity.EntryCommonEntity;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.EntryTempEntity;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.vo.ImportResultEntryVO;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    private EntryTempService entryTempService;
    @Autowired
    private CommonUtils commonUtils;

    @PostMapping("/importExcle")
    @ApiOperation("导入excle(待完善)")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> importExcle(@RequestParam("file") MultipartFile multipartFile,
                                                       @RequestParam("taskID") String taskID
    ) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryInfoEntity> entryEntities = entryInfoService.importExcle(multipartFile, taskID);
        responseListModel.setList(entryEntities);
        responseListModel.setTotalNum(entryEntities.size());

        return checkResult(responseListModel);
    }


    @PostMapping("/readZZExcle")
    @ApiOperation("读取装置excle")
    @CrossOrigin
    @Transactional
    //new
    public HttpResponse<ResponseListModel> importZZExcle(@RequestParam("file") MultipartFile multipartFile,
                                                         @RequestParam("taskID") String taskID,HttpServletRequest httpServletRequest
    ) {
        ResponseListModel responseListModel = new ResponseListModel();
         List<EntryInfoEntity> entryInfoEntities = entryInfoService.importZZExcle(multipartFile, taskID, httpServletRequest);
        responseListModel.setList(entryInfoEntities);
        responseListModel.setTotalNum(entryInfoEntities.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/insertEntry")
    @ApiOperation("新增词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> insertEntry(@RequestBody List<EntryInfoEntity> entryInfoEntities,
                                            @RequestParam("taskID") String taskID,HttpServletRequest request) {

        String result = entryInfoService.insertEntry(entryInfoEntities,taskID,request);
        return checkResult(result);
    }

    @PostMapping("/updateEntryList")
    @ApiOperation("更新词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> updateEntryList(@RequestBody List<EntryInfoEntity> entryInfoEntities,@RequestParam String taskID,HttpServletRequest request) {

        String result = entryTempService.updateEntryList(entryInfoEntities,taskID,request);
        return checkResult(result);
    }

    @PostMapping("/getEntryInfoList")
    @ApiOperation("查询词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getEntryInfoList(@RequestParam String taskID,@RequestParam String entryState ,@RequestBody List<String> transStates) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        entryInfoEntities = entryTempService.getEntryInfoList(taskID,entryState,transStates);
        responseListModel.setList(entryInfoEntities);
        responseListModel.setTotalNum(entryInfoEntities.size());

        return checkResult(responseListModel);
    }




    @PostMapping("/deleteEntryInfoByID")
    @ApiOperation("删除词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> deleteEntryInfoByID(@RequestBody List<String> entryID) {

        String result = entryTempService.deleteEntryInfoByID(entryID);

        return checkResult(result);
    }

    @PostMapping("/preTranslate")
    @ApiOperation("预翻译")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> preTranslate(@RequestBody List<EntryInfoEntity> entryInfoEntities,@RequestParam String taskID,@RequestParam String priority) {
        ResponseListModel responseListModel = new ResponseListModel();

        List<EntryInfoEntity> entryInfoEntities1 = entryTempService.preTranslate(entryInfoEntities,taskID,priority);
        responseListModel.setList(entryInfoEntities1);
        responseListModel.setTotalNum(entryInfoEntities1.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/getTemplateFile")
    @ApiOperation("模板下载")
    @CrossOrigin
    @Transactional
    public void getTemplateFile(HttpServletResponse response) {
        entryTempService.getTemplateFile(response);
    }




}
