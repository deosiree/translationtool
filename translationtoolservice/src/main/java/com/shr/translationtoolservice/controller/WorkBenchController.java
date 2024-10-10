package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import com.shr.translationtoolservice.entity.vo.ImportResultEntryVO;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.service.EntryTempService;
import com.shr.translationtoolservice.service.I18nService;
import com.shr.translationtoolservice.service.TI8nAddressService;
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

    @Autowired
    private TI8nAddressService ti8nAddressService;
    @Autowired
    private I18nService i18nService;

    @PostMapping("/importExcle")
    @ApiOperation("导入excle(预留)")
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


    @GetMapping("/getI18nAdress")
    @ApiOperation("获取i8n地址")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getI18nAdress(
    ) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<TI8nAddress> i18nAddress = ti8nAddressService.getI18nAdress();
        responseListModel.setList(i18nAddress);
        responseListModel.setTotalNum(i18nAddress.size());
        return checkResult(responseListModel);
    }
    @GetMapping("/addI18nAdress")
    @ApiOperation("新增i8n地址")
    @CrossOrigin
    @Transactional
    public HttpResponse<Integer> addI18nAdress(@RequestParam("ip") String ip
    ) {
        int res = ti8nAddressService.addI18nAdress(ip);
        return checkResult(res);
    }
    @GetMapping("/changeI18nAdress")
    @ApiOperation("修改i8n地址")
    @CrossOrigin
    @Transactional
    public HttpResponse<Integer> changeI18nAdress(@RequestParam("id") String id,
                                                            @RequestParam("ip") String ip
    ) {
        int res = ti8nAddressService.changeI18nAdress(id,ip);
        return checkResult(res);
    }
    @GetMapping("/deleteI18nAdress")
    @ApiOperation("删除i8n地址")
    @CrossOrigin
    @Transactional
    public HttpResponse<Integer> deleteI18nAdress(@RequestParam("id") String id
    ) {
        int res = ti8nAddressService.deleteI18nAdress(id);
        return checkResult(res);
    }

    @PostMapping("/importCommonExcle")
    @ApiOperation("导入excle(翻译)")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> importCommonExcle(@RequestParam("file") MultipartFile multipartFile,
                                                             @RequestParam("taskID") String taskID
    ) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryInfoEntity> entryEntities = entryInfoService.importCommonExcle(multipartFile, taskID);
        responseListModel.setList(entryEntities);
        responseListModel.setTotalNum(entryEntities.size());

        return checkResult(responseListModel);
    }


    @PostMapping("/entryImportExcle")
    @ApiOperation("词条excle导入（通用和装置）")
    @CrossOrigin
    @Transactional
    //new
    public HttpResponse<ResponseListModel> entryImportExcle(@RequestParam("file") MultipartFile multipartFile,
                                                            @RequestParam("taskID") String taskID, HttpServletRequest httpServletRequest
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
                                            @RequestParam("taskID") String taskID, HttpServletRequest request) {

        String result = entryInfoService.insertEntry(entryInfoEntities, taskID, request);
        return checkResult(result);
    }

    @PostMapping("/getDictory")
    @ApiOperation("查询辞典")
    @CrossOrigin
    @Transactional
    public HttpResponse<  ResponseListModel<DictionaryVo>> getDictory(String entry, String tag,
                                                                      String common, @RequestParam String fileName,@RequestParam String i18nUrl) {
        ResponseListModel<DictionaryVo> responseListModel = new ResponseListModel<>();
        List<DictionaryVo> dictionaryVos = i18nService.getDictory(entry,tag,common,fileName,i18nUrl);
        responseListModel.setList(dictionaryVos);
        responseListModel.setTotalNum(dictionaryVos.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/updateEntryList")
    @ApiOperation("更新词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> updateEntryList(@RequestBody List<EntryInfoEntity> entryInfoEntities, @RequestParam String taskID, HttpServletRequest request) {

        String result = entryTempService.updateEntryList(entryInfoEntities, taskID, request);
        return checkResult(result);
    }

    @PostMapping("/getEntryInfoList")
    @ApiOperation("查询词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getEntryInfoList(@RequestParam String taskID, @RequestParam String entry,  String entryState, @RequestBody List<String> transStates) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        entryInfoEntities = entryTempService.getEntryInfoList(taskID, entryState, transStates, entry);
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
    public HttpResponse<ResponseListModel> preTranslate( HttpServletRequest request,@RequestBody List<EntryInfoEntity> entryInfoEntities, @RequestParam String taskID, @RequestParam String priority) {
        ResponseListModel responseListModel = new ResponseListModel();

        List<EntryInfoEntity> entryInfoEntities1 = entryTempService.preTranslate(request,entryInfoEntities, taskID, priority);
        responseListModel.setList(entryInfoEntities1);
        responseListModel.setTotalNum(entryInfoEntities1.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/getTemplateFile")
    @ApiOperation("模板下载")
    @CrossOrigin
    @Transactional
    public void getTemplateFile(HttpServletResponse response, String fileType) {
        entryTempService.getTemplateFile(response, fileType);
    }

    @PostMapping("/filterSourceLanguage")
    @ApiOperation("过滤语言")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> filterSourceLanguage(@RequestBody List<EntryInfoEntity> entryInfoEntities,@RequestParam  String languageType) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryInfoEntity> entryInfoEntities1 = entryInfoService.filterSourceLanguage(entryInfoEntities, languageType);
        responseListModel.setList(entryInfoEntities1);
        responseListModel.setTotalNum(entryInfoEntities1.size());
        return checkResult(responseListModel);
    }


}
