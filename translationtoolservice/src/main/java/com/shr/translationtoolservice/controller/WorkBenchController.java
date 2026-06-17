package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.DictionaryVo;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.service.EntryTempService;
import com.shr.translationtoolservice.service.I18nService;
import com.shr.translationtoolservice.service.TI8nAddressService;
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
import java.util.Collection;
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
    private TI8nAddressService ti8nAddressService;
    @Autowired
    private I18nService i18nService;



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
        FileInputStreamEntity fileInputStreamEntity = FileInputStreamEntity.convertFrom(multipartFile);
        try {
            ResponseListModel responseListModel = new ResponseListModel();
            List<EntryInfoEntity> entryEntities = entryInfoService.importCommonExcle(FileInputStreamEntity.convertFrom(multipartFile), taskID);
            responseListModel.setList(entryEntities);
            responseListModel.setTotalNum(entryEntities.size());

            return checkResult(responseListModel);            
        } catch (Exception e) {
            log.error("导入翻译文件时出现异常", e);
            return error(null, String.format("导入文件时出现异常, 异常信息为: %s", e.getMessage()));
        } finally{
            FileInputStreamEntity.close(fileInputStreamEntity);
        }

    }


    @PostMapping("/entryImportExcle")
    @ApiOperation("词条excle导入（通用和装置）")
    @CrossOrigin
    @Transactional
    //new
    public HttpResponse<ResponseListModel> entryImportExcle(@RequestParam("file") MultipartFile multipartFile,
        @RequestParam(value = "departmentType",defaultValue = "通用平台部") String departmentType, 
        @RequestParam("taskID") String taskID,
        @RequestParam(name = "encoding",defaultValue = "GBK",required = false) String encoding, 
        HttpServletRequest httpServletRequest
    ) {
        ResponseListModel responseListModel = new ResponseListModel();
        Collection<EntryInfoEntity> entryInfoEntities = null;
        FileInputStreamEntity fileInputStreamEntity = FileInputStreamEntity.convertFrom(multipartFile);
        try {
            entryInfoEntities = entryInfoService.importEntitiesFromFile(fileInputStreamEntity, taskID,departmentType, encoding,httpServletRequest);

            if(entryInfoEntities == null){
                throw new NullPointerException("entryInfoEntities == null");
            }
            responseListModel.setList(entryInfoEntities);
            responseListModel.setTotalNum(entryInfoEntities.size());
            return checkResult(responseListModel);
        } catch (Exception e) {
            // TODO: handle exception
            log.error("导入excel文件的词条时报错", e);
            return error(null, e.getMessage());
        } finally{
            FileInputStreamEntity.close(fileInputStreamEntity);
        }

    }

    @PostMapping("/insertEntry")
    @ApiOperation("新增词条")
    @CrossOrigin
    @Transactional
    public HttpResponse insertEntry(@RequestBody List<EntryInfoEntity> entryInfoEntities,
                                            @RequestParam("taskID") String taskID, HttpServletRequest request) {

        List<EntryInfoEntity> result = entryInfoService.insertEntry(entryInfoEntities, taskID, request);
        ResponseListModel model = new ResponseListModel<>();
        model.setList(result);
        model.setTotalNum(result.size());

        return checkResult(model);
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
    public HttpResponse<ResponseListModel> updateEntryList(@RequestBody List<EntryInfoEntity> entryInfoEntities, @RequestParam String taskID,HttpServletRequest request) {

        List<EntryInfoEntity> result = entryTempService.updateEntryList(entryInfoEntities, taskID, request);
        ResponseListModel model = new ResponseListModel<>();
        model.setList(result);
        model.setTotalNum(result.size());
        return checkResult(model);
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

//
    @PostMapping("/deleteEntryInfoByID")
    @ApiOperation("删除词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> deleteEntryInfoByID(@RequestBody List<String> entryID,
                                                    @RequestParam("productID") String productID,
                                                    String versionID) {

        String result = entryTempService.deleteEntryInfoByID(entryID,productID,versionID);

        return checkResult(result);
    }

    @PostMapping("/deleteEntryInfoByTaskID")
    @ApiOperation("删除任务中的词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> deleteEntryInfoByTaskID(@RequestBody List<String> entryID,
                                                    @RequestParam("taskID") String taskID) {

        String result = entryTempService.deleteEntryInfoByTaskID(entryID,taskID);

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
    public void getTemplateFile(HttpServletResponse response, String fileType,String translateType) {
        entryTempService.getTemplateFile(response, fileType,translateType);
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

    @PostMapping("/capitalizeWords")
    @ApiOperation("句首字母转换为大写")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> capitalizeWords(@RequestBody List<EntryInfoEntity> entryInfoEntities,
                                                           @RequestParam("changeType")  String changeType,@RequestParam("translateType")  String translateType) {
        ResponseListModel responseListModel = new ResponseListModel();

        List<EntryInfoEntity> entryInfoEntities1 = entryInfoService.capitalizeWords(entryInfoEntities, changeType,translateType);
        responseListModel.setList(entryInfoEntities1);
        responseListModel.setTotalNum(entryInfoEntities1.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/replaceWords")
    @ApiOperation("批量替换")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> replaceWords(@RequestBody List<EntryInfoEntity> entryInfoEntities,
                                                        @RequestParam("sourceStr")  String sourceStr,@RequestParam("replaceStr")  String replaceStr,
                                                        @RequestParam("translateType")  String translateType) {
        ResponseListModel responseListModel = new ResponseListModel();

        List<EntryInfoEntity> entryInfoEntities1 = entryInfoService.replaceWords(entryInfoEntities, sourceStr,replaceStr,translateType);
        responseListModel.setList(entryInfoEntities1);
        responseListModel.setTotalNum(entryInfoEntities1.size());
        return checkResult(responseListModel);
    }




}
