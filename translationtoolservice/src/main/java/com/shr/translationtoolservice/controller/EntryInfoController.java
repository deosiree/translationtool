package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.dao.EntryClassifyMapper;
import com.shr.translationtoolservice.dao.EntryMapper;
import com.shr.translationtoolservice.dao.EntryVersionMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.EntryVO;
import com.shr.translationtoolservice.entity.vo.ProductTreeVO;
import com.shr.translationtoolservice.service.EntryClassifyService;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.service.EntryPublicService;
import com.shr.translationtoolservice.service.ProductService;
import com.shr.translationtoolservice.util.CommonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName EntryInfoController
 * @USER: Cola
 * @Date 2023/11/20 0020 11:26
 **/
@RestController
@RequestMapping("/entryInfo")
@Api(tags = "词条管理（新）")
@Slf4j
public class EntryInfoController extends BaseController {


    @Autowired
    private EntryPublicService entryPublicService;
    @Autowired
    private CommonUtils commonUtils;
    @Autowired
    private ProductService productService;
    @Autowired
    private EntryClassifyService entryClassifyService;
    @Autowired
    private EntryInfoService entryInfoService;

    //查询词条信息
    @PostMapping("/getPublicEntryByDepartment")
    @ApiOperation("查询公共库")
    @Token
    @CrossOrigin
    public HttpResponse<ResponseListModel> getPublicEntryByDepartment(@RequestBody EntryPublicEntity entryPublicEntity,
                                                                      @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                                      @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel result = new ResponseListModel<>();
        List<EntryPublicEntity> entryPublicEntities = new ArrayList<>();

        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            entryPublicEntities = entryPublicService.getPublicEntryByDepartment(entryPublicEntity, offset, pageSize);
        }
        result.setList(entryPublicEntities);

        return checkResult(result);
    }

    @PostMapping("/getClassTree")
    @ApiOperation("查询分类树")
    @Token
    @CrossOrigin
    public HttpResponse<ResponseListModel> getClassTree(String department) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryClassify> entryClassifies = new ArrayList<>();

        //department 空 为管理员，可查看所有分类
        entryClassifies = entryClassifyService.getEntryClassfy(department);
        responseListModel.setList(entryClassifies);
        responseListModel.setTotalNum(entryClassifies.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/updateEntryClassfy")
    @ApiOperation("词条分类修改")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> updateEntryClassfy(@RequestBody EntryClassify entryClassify) {


        return checkResult(entryClassifyService.updateEntryClassfy(entryClassify));

    }

    @PostMapping("/deleteEntryClassfy")
    @ApiOperation("词条分类删除")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> deleteEntryClassfy(@RequestBody List<String> idList) {

        return checkResult(entryClassifyService.deleteEntryClassfy(idList));

    }


    @PostMapping("/addEntryClassfy")
    @ApiOperation("词条分类新增")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> addEntryClassfy(@RequestBody EntryClassify entryClassify, HttpServletRequest request) {


        return checkResult(entryClassifyService.addEntryClassfy(entryClassify, request));

    }


    @PostMapping("/getEntryByVersion")
    @ApiOperation("获取版本词条")
    @CrossOrigin
    @Token
    public HttpResponse<ResponseListModel<EntryVO>> getEntryByVersion(String versionID,
                                                                      @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                                      @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<EntryVO> responseListModel = new ResponseListModel<EntryVO>();
        List<EntryVO> entryVOS = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            entryVOS = entryInfoService.getEntryByVersion(versionID, offset, pageSize);
        }
        responseListModel.setList(entryVOS);
        responseListModel.setTotalNum(entryInfoService.getEntryByVersionTotal(versionID));
        return checkResult(responseListModel);

    }

    @PostMapping("/addEntryByVersion")
    @ApiOperation("新增版本词条(导入)")
    @CrossOrigin
    @Token
    @Transactional
    public HttpResponse<HttpResponse<String>> addEntryByVersion(@RequestBody List<EntryVO> entryVOS,
                                                                HttpServletRequest request) {

        return checkResult(entryInfoService.addEntryByVersion(entryVOS, request));
    }

    @PostMapping("/addEntryInfo")
    @ApiOperation("新增词条(单条无翻译)")
    @CrossOrigin
    @Token
    @Transactional
    public HttpResponse<String> addEntryInfo(@RequestBody EntryInfoEntity entryInfoEntity,
                                             String tableName,
                                             HttpServletRequest request) {
        //tableName = "t_version_202311";
        return checkResult(entryInfoService.addEntryInfo(entryInfoEntity, request, tableName));
    }

/*    //编辑词条
    @PostMapping("/updateEntry")
    @ApiOperation("编辑词条")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<EntryCommonEntity> updateEntry(@RequestBody EntryInfoEntity entryInfoEntity,HttpServletRequest request,String notes) {
    *//*    if (StringUtils.isBlank(entryEntity.getTableName())) {
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }*//*
        ResultObject resultObject = entryManagementService.updateEntry(entryEntity, request,notes);
        EntryCommonEntity entryEntity1 = (EntryCommonEntity)resultObject.getData();


        return checkResult(entryEntity1,resultObject.getMsg());
    }*/
}
