package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.SecondClassify;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.service.SecondClassifyInterface;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @title SecondClassifyController
 * @create 2024/3/8 14:14
 * @description <TODO description class purpose>
 **/
@RestController
@RequestMapping("/secondClassify")
@Api(tags = "二级分类管理")
@Slf4j
public class SecondClassifyController extends BaseController {

    @Autowired
    private SecondClassifyInterface secondClassifyInterface;

    @PostMapping("/getSecondClassify")
    @ApiOperation("查询二级分类")
    @CrossOrigin
    public HttpResponse<ResponseListModel<SecondClassify>> getSecondClassify(@RequestBody SecondClassify secondClassify){
        List<SecondClassify> classifyList = secondClassifyInterface.getSecondClassify(secondClassify);
        ResponseListModel<SecondClassify> result = new ResponseListModel<>();
        result.setList(classifyList);
        result.setTotalNum(classifyList.size());
        return checkResult(result);
    }

    @PostMapping("/addSecondClassify")
    @ApiOperation("新增二级分类")
    @CrossOrigin
    public HttpResponse<String> addSecondClassify(@RequestBody SecondClassify secondClassify, HttpServletRequest request){
        return checkResult(secondClassifyInterface.addSecondClassify(secondClassify,request));
    }

    @PostMapping("/updateSecondClassify")
    @ApiOperation("编辑二级分类")
    @CrossOrigin
    public HttpResponse<Integer> updateSecondClassify(@RequestBody SecondClassify secondClassify){
        return checkResult(secondClassifyInterface.updateSecondClassify(secondClassify));
    }

    @PostMapping("/deleteSecondClassify")
    @ApiOperation("删除二级分类")
    @CrossOrigin
    public HttpResponse<Integer> deleteSecondClassify(@RequestBody List<String> ids){
        return checkResult(secondClassifyInterface.deleteSecondClassify(ids));
    }

}
