package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.Constant;
import com.shr.translationtoolservice.common.Result;
import com.shr.translationtoolservice.util.JWTTokenUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/entry")
@Api(tags = "词条管理")
@Slf4j
public class EntryController {

    @PostMapping("/insertEntry")
    @ResponseBody
    @ApiOperation("新增词条")
    public Result insertEntry(HttpServletRequest request){
        String token = request.getHeader(Constant.TOKEN);
        return Result.ok(JWTTokenUtils.getUserName(token));
    }

}
