package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.TLanguage;
import com.shr.translationtoolservice.service.TLanguageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName TranslateController
 * @USER: Cola
 * @Date 2023/11/10 0010 16:40
 **/

@RestController
@RequestMapping("/translate")
@Api(tags = "翻译管理")
@Slf4j
public class TranslateController extends BaseController{

    @Autowired
    private TLanguageService tLanguageService;


    @PostMapping("/getLanguage")
    @ApiOperation("查询翻译语种")
    @CrossOrigin
    @Transactional
    //返回id
    public HttpResponse<ResponseListModel<TLanguage>> getLanguage(@RequestBody TLanguage language) {
        ResponseListModel<TLanguage> result = new ResponseListModel<>();
        List<TLanguage> languageList = tLanguageService.getLanguages(language);
        result.setList(languageList);
        result.setTotalNum(languageList.size());
        return checkResult(result);

    }
}
