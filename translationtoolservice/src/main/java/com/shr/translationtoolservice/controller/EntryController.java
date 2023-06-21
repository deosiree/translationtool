package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.common.PassToken;
import com.shr.translationtoolservice.common.Result;
import com.shr.translationtoolservice.entity.SearchCondition;
import com.shr.translationtoolservice.entity.Term;
import com.shr.translationtoolservice.service.TermManagementService;
import com.shr.translationtoolservice.util.HttpResponse;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/entry")
@Api(tags = "词条管理")
@Slf4j
public class EntryController extends BaseController{
    @Autowired
    TermManagementService termManagementService;

    @PostMapping("/insertEntry")
    @ResponseBody
    @ApiOperation("新增词条")
    public Result insertEntry(HttpServletRequest request){
        String token = request.getHeader(Constant.TOKEN);
        return Result.ok(JWTTokenUtils.getUserName(token));
    }


    //查询词条信息
    @PostMapping("/searchTerm")
    @ApiOperation("词条查询")
    @PassToken
    @CrossOrigin
    public HttpResponse<List<Term>> search_term(@RequestBody SearchCondition searchCondition,
                                                @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize){

        List<Term> terms = termManagementService.search_term(searchCondition, pageIndex, pageSize);
        return checkResult(terms);
    }
    //批量审核
    @PostMapping("/bathAudit")
    @ApiOperation("批量审核")
    @PassToken
    @CrossOrigin
    public HttpResponse<String> bathAudit(  @RequestParam(value = "idList") List<String> idList){
        String result = termManagementService.bathAudit(idList);

        return checkResult(result);
    }
}
