package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.dao.EntryClassifyMapper;
import com.shr.translationtoolservice.dao.EntryMapper;
import com.shr.translationtoolservice.dao.EntryVersionMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.entity.vo.EntryTempCompareVO;
import com.shr.translationtoolservice.entity.vo.EntryVO;
import com.shr.translationtoolservice.entity.vo.ProductTreeVO;
import com.shr.translationtoolservice.entity.vo.UpgradeVO;
import com.shr.translationtoolservice.service.*;
import com.shr.translationtoolservice.util.CommonUtils;
import com.shr.translationtoolservice.util.ExcelUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
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

    @Autowired
    private TranslateService translateService;


    //查询词条信息
    @PostMapping("/getPublicEntry")
    @ApiOperation("查询公共库")
    @Token
    @CrossOrigin
    public HttpResponse<ResponseListModel<TranslateEntity>> getPublicEntry(@RequestBody TranslateEntity translateEntity,
                                                                      @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                                      @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<TranslateEntity> result = new ResponseListModel<>();
        List<TranslateEntity> entryPublicEntities = new ArrayList<>();

        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            entryPublicEntities = entryInfoService.getPublicEntry(translateEntity, offset, pageSize);
        }
        result.setList(entryPublicEntities);
        result.setTotalNum(entryInfoService.getPublicEntryTotal(translateEntity));
        return checkResult(result);
    }


    @PostMapping("/updatePublicEntry")
    @ApiOperation("修改公共库")
    @Token
    @CrossOrigin
    public HttpResponse<String> updatePublicEntry(@RequestBody TranslateEntity translateEntity) {


        String  result = entryInfoService.updatePublicEntry(translateEntity);

        return checkResult(result);
    }

    @PostMapping("/addPublicEntry")
    @ApiOperation("新增公共库")
    @Token
    @CrossOrigin
    public HttpResponse<String> addPublicEntry(@RequestBody List<TranslateEntity> translateEntity) {


        String  result = entryInfoService.addPublicEntry(translateEntity);

        return checkResult(result);
    }

    @PostMapping("/deletePublicEntry")
    @ApiOperation("删除公共库")
    @Token
    @CrossOrigin
    public HttpResponse<String> deletePublicEntry(@RequestBody List<String> idlist) {


        String  result = entryInfoService.deletePublicEntry(idlist);

        return checkResult(result);
    }

    @PostMapping("/getClassTree")
    @ApiOperation("查询分类树")
    @Token
    @CrossOrigin
    public HttpResponse<ResponseListModel> getClassTree(String department,HttpServletRequest request) {
        ResponseListModel responseListModel = new ResponseListModel();
        List<EntryClassify> entryClassifies = new ArrayList<>();

        //department 空 为管理员，可查看所有分类
        entryClassifies = entryClassifyService.getEntryClassfy(department,request);
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
    public HttpResponse<ResponseListModel<EntryInfoEntity>> getEntryByVersion(@RequestBody EntryInfoEntity entryInfoEntity,
                                                                      @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                                      @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        ResponseListModel<EntryInfoEntity> responseListModel = new ResponseListModel<EntryInfoEntity>();
        List<EntryInfoEntity> entryInfoEntities = new ArrayList<>();
        if (commonUtils.checkPage(pageIndex, pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            entryInfoEntities = entryInfoService.getEntryByVersion(entryInfoEntity, offset, pageSize);
        }
        responseListModel.setList(entryInfoEntities);
        responseListModel.setTotalNum(entryInfoEntities.size());
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

    @PostMapping("/addEntryByTemp")
    @ApiOperation("新增词条(临时表写入)")
    @CrossOrigin
    @Token
    @Transactional
    public HttpResponse<String> addEntryByTemp(@RequestBody List<EntryTempEntity> entryTempEntities,
                                             String tableName,
                                             HttpServletRequest request) {
        //tableName = "t_version_202311";
        return checkResult(entryInfoService.addEntryByTemp(entryTempEntities, request, tableName));
    }

    @PostMapping("/updateEntryTemp")
    @ApiOperation("临时表更新")
    @CrossOrigin
    @Token
    @Transactional
    public HttpResponse<String> updateEntryTemp(@RequestBody List<EntryTempEntity> entryTempEntities,
                                               HttpServletRequest request) {
        //tableName = "t_version_202311";
        return checkResult(entryInfoService.updateEntryTemp(entryTempEntities, request));
    }

    //编辑词条
    @PostMapping("/updateEntryInfo")
    @ApiOperation("编辑词条")
    @CrossOrigin
    @Transactional(propagation = Propagation.NESTED)
    @Token
    public HttpResponse<String> updateEntryInfo(@RequestBody EntryInfoEntity entryInfoEntity,HttpServletRequest request,String notes) {
            if (StringUtils.isBlank(entryInfoEntity.getTableName())) {
            return checkResult(ErrorCodeList.TBALE_IS_NULL);
        }
        String result = entryInfoService.updateEntryInfo(entryInfoEntity, request,notes);

        return checkResult(result);
    }


    //编辑词条
    @PostMapping("/updateEntryInfoList")
    @ApiOperation("批量更新词条")
    @CrossOrigin
    @Transactional(propagation = Propagation.NESTED)
    @Token
    public HttpResponse<String> updateEntryInfoList(@RequestBody List<EntryInfoEntity> entryInfoEntities,HttpServletRequest request,String notes) {

        String result = entryInfoService.updateEntryInfoList(entryInfoEntities, request,notes);

        return checkResult(result);
    }

    //编辑词条
    @PostMapping("/deleteEntryInfo")
    @ApiOperation("删除词条")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> deleteEntryInfo(@RequestBody List<String> idList,String tableName) {

        String result = entryInfoService.deleteEntryInfo(idList,tableName);

        return checkResult(result);
    }



    //编辑词条
    @PostMapping("/upgrade")
    @ApiOperation("升级（废弃）")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> upgrade(@RequestBody UpgradeVO upgradeVO,HttpServletRequest request) {

        String result = entryInfoService.upgrade(upgradeVO,request);

        return checkResult(result);
    }

    //编辑翻译
    @PostMapping("/updateTranslation")
    @ApiOperation("编辑翻译")
    @CrossOrigin
    @Transactional
    @Token
    public HttpResponse<String> updateTranslation(@RequestBody List<TranslateEntity> translateEntityList) {

        String result = translateService.updateTranslation(translateEntityList);

        return checkResult(result);
    }



    @PostMapping("/translate")
    @ApiOperation("翻译词条")
    @CrossOrigin
    @Transactional
    public HttpResponse<TranslateEntities> translate(@RequestParam String name,@RequestParam String type,@RequestParam String department) {
        if (StringUtils.isBlank(type)){
            checkResult(null," 入参 type 不能为空 ！");
        }

        TranslateEntities translateEntity = entryInfoService.translate(name,type,department);
        return checkResult(translateEntity);
    }

    @PostMapping("/versionExport")
    @ApiOperation("版本导出")
    @CrossOrigin
    @Transactional
    public void versionExport(@RequestParam String versionID,
                                              @RequestParam String translateType,
                                                         HttpServletResponse response) {


        entryInfoService.versionExport(versionID,response,translateType);


    }
}
