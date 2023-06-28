package com.shr.translationtoolservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.PassToken;
import com.shr.translationtoolservice.common.Result;
import com.shr.translationtoolservice.dao.EntryCommonEntityMapper;
import com.shr.translationtoolservice.dao.EntryProductEntityMapper;
import com.shr.translationtoolservice.dao.EntryProjectEntityMapper;
import com.shr.translationtoolservice.entity.*;
import com.shr.translationtoolservice.service.EntryCommonEntityService;
import com.shr.translationtoolservice.service.EntryManagementService;

import com.shr.translationtoolservice.service.EntryProductEntityService;
import com.shr.translationtoolservice.service.EntryProjectEntityService;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/entry")
@Api(tags = "词条管理")
@Slf4j
public class EntryController extends BaseController{
    @Autowired
    EntryManagementService entryManagementService;

    @Autowired
    EntryProductEntityMapper entryProductEntityMapper;
    @Autowired
    EntryProjectEntityMapper entryProjectEntityMapper;
    @Autowired
    EntryCommonEntityMapper entryCommonEntityMapper;
    @Autowired
    EntryProjectEntityService entryProjectEntityService;
    @Autowired
    EntryCommonEntityService entryCommonEntityService;
    @Autowired
    EntryProductEntityService entryProductEntityService;

    @PostMapping("/insertEntry")
    @ResponseBody
    @ApiOperation("新增词条")
    public Result insertEntry(HttpServletRequest request){
        String token = request.getHeader(Constant.TOKEN);
        return Result.ok(JWTTokenUtils.getUserName(token));
    }


    //查询词条信息
    @PostMapping("/searchEntry")
    @ApiOperation("词条查询")
    @PassToken
    @CrossOrigin
    public HttpResponse<ResponseListModel> searchEntry(@RequestBody EntryReqEntry entryReqEntry,
                                                         @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                         @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize){

        ResponseListModel result = new ResponseListModel<>();

        QueryWrapper<EntryProjectEntity> projectEntityQueryWrapper = new QueryWrapper<EntryProjectEntity>();
        QueryWrapper<EntryProductEntity> productEntityQueryWrapper = new QueryWrapper<>();
        QueryWrapper<EntryCommonEntity> commonEntityQueryWrapper = new QueryWrapper<>();

        if (StringUtils.isBlank(entryReqEntry.getLexicon())) {
            result.setList(entryManagementService.getAllEntry(entryReqEntry, pageIndex, pageSize));
            int total = entryCommonEntityMapper.selectCount(commonEntityQueryWrapper)
                    + entryProductEntityMapper.selectCount(productEntityQueryWrapper) + entryProjectEntityMapper.selectCount(projectEntityQueryWrapper);
            result.setTotalNum(total);
            //产品表
        } else if ("project".equals(entryReqEntry.getLexicon())) {
            result.setList(entryProjectEntityService.searchEntry(entryReqEntry, pageIndex, pageSize));
            result.setTotalNum(entryProjectEntityMapper.selectCount(projectEntityQueryWrapper));
            //工程表
        } else if ("product".equals(entryReqEntry.getLexicon())) {
            result.setList(entryProductEntityService.searchEntry(entryReqEntry, pageIndex, pageSize));
            result.setTotalNum(entryProductEntityMapper.selectCount(productEntityQueryWrapper));
            //公共表
        } else if ("common".equals(entryReqEntry.getLexicon())) {
            result.setList(entryCommonEntityService.searchEntry(entryReqEntry, pageIndex, pageSize));
            result.setTotalNum(entryCommonEntityMapper.selectCount(commonEntityQueryWrapper));
        }

         return checkResult(result);
    }
    //批量审核
    @PostMapping("/bathAudit")
    @ApiOperation("批量审核")
    @PassToken
    @CrossOrigin
    public HttpResponse<String> bathAudit(  @RequestParam(value = "idList") List<String> idList){
        if (CollectionUtils.isEmpty(idList)){
            return checkResult(ErrorCodeList.INPUT_IS_NULL);
        }
        String result = entryManagementService.bathAudit(idList);

        return checkResult(result);
    }





}
