package com.shr.translationtoolservice.controller;

import com.hankcs.hanlp.dependency.nnparser.parser_dll;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.entity.vo.SykEntryVO;
import com.shr.translationtoolservice.service.SykService;
import com.shr.translationtoolservice.service.impl.SykServiceImpl;
import com.shr.translationtoolservice.util.StringUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


/**
 * @author ：210093
 * @date ：Created in 2025/1/3 15:22
 * @description：SykController
 */
@RestController
@RequestMapping("/Syk")
@Api(tags = "术语库管理")
@Slf4j
public class SykController extends BaseController{


    @Autowired
    private SykService sykService;

    @PostMapping("/getSykEntry")
    @ApiOperation("获取术语库")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getSykEntry(@RequestBody TranslateEntity translate , @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                      @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        try {
            ResponseListModel responseListModel = new ResponseListModel();
            // entry/translate 仅做 LIKE 转义；entryRegex/translateRegex 禁止转义
            translate.setEntry(StringUtil.addEscapeCharacter(translate.getEntry()));
            translate.setTranslate(StringUtil.addEscapeCharacter(translate.getTranslate()));
            responseListModel.setList(sykService.getSykEntry(translate,null,pageIndex,pageSize));
            responseListModel.setTotalNum(sykService.getSykEntry(translate,null,-1,-1).size());
            return checkResult(responseListModel);
        } catch (Exception e) {
            return handleRegexpOrRethrow(e);
        }
    }

    @PostMapping("/getSykEntryRelation")
    @ApiOperation("获取术语库词条关联信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getSykEntryRelation(@RequestBody List<TranslateEntity> translates) {
        try {
            ResponseListModel<SykEntryVO> responseListModel = new ResponseListModel();
            List<SykEntryVO> translateEntityList = sykService.getSykEntryRelation(translates);
            responseListModel.setList(translateEntityList);
            responseListModel.setTotalNum(translateEntityList.size());
            return checkResult(responseListModel);
        } catch (Exception e) {
            log.error("getSykEntryRelation 异常", e);
            return error(null, e.getMessage());
        }
    }

    @PostMapping("/getSykNotUsed")
    @ApiOperation("获取术语库未使用的翻译")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel<TranslateEntity>> getSykNotUsed(@RequestBody TranslateEntity translateTemplate,HttpServletRequest request) {
        try {
            ResponseListModel<TranslateEntity> responseListModel = new ResponseListModel<>();
            translateTemplate.setEntry(StringUtil.addEscapeCharacter(translateTemplate.getEntry()));
            translateTemplate.setTranslate(StringUtil.addEscapeCharacter(translateTemplate.getTranslate()));
            List<TranslateEntity> translateEntityList = sykService.getSykNotUsed(translateTemplate,request.getHeader("token"));
            responseListModel.setList(translateEntityList);
            responseListModel.setTotalNum(translateEntityList.size());
            return checkResult(responseListModel);
        } catch (Exception e) {
            return handleRegexpOrRethrow(e);
        }
    }

    @PostMapping("/updateSykEntry")
    @ApiOperation("更新术语库")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel<String>> updateSykEntry(@RequestBody List<TranslateEntity> translates) {
        try {
            ResponseListModel<String> responseListModel = new ResponseListModel<>();
            List<TranslateEntity> failedTranslates = sykService.updateSykEntry(translates);
            responseListModel.setList(failedTranslates.stream().map(TranslateEntity::getId).collect(Collectors.toList()));
            responseListModel.setTotalNum(failedTranslates.size());
            return ok(responseListModel);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return error(null, e.getMessage());
        }
    }

    @PostMapping("/deleteSykEntry")
    @ApiOperation("删除术语库")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel<String>> deleteSykEntry(@RequestBody List<TranslateEntity> translates){

        try {
            ResponseListModel<String> model = new ResponseListModel<>();
            List<TranslateEntity> failedTranslates = sykService.deleteSykEntry(translates);
            model.setList(failedTranslates.stream().map(TranslateEntity::getId).collect(Collectors.toList()));
            model.setTotalNum(failedTranslates.size());
            return ok(model);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return error(null, e.getMessage());
        }
    }


    @PostMapping("/checkSykEntry")
    @ApiOperation("检查术语库翻译")
    @CrossOrigin
    @Transactional
    public HttpResponse<List<TranslateEntity>> checkSykEntry(@RequestBody TranslateEntity translatesTemplate) {
        translatesTemplate.setEntry(StringUtil.addEscapeCharacter(translatesTemplate.getEntry()));
        translatesTemplate.setTranslate(StringUtil.addEscapeCharacter(translatesTemplate.getTranslate()));
        List<TranslateEntity> problemEntities = sykService.checkSykEntryByTemplate(translatesTemplate);
        return checkResult(problemEntities);

    }

    @PostMapping("/checkSykEntryBeforeSave")
    @ApiOperation("检查预翻译")
    @CrossOrigin
    @Transactional
    public HttpResponse<List<TranslateEntity>> checkSykEntryBeforeSave(@RequestBody List<TranslateEntity> translateEntities){

        List<TranslateEntity> problemEntities = sykService.checkSykEntryOnList(translateEntities);
        return checkResult(problemEntities);

    }


    @PostMapping("/checkSykSameEntry")
    @ApiOperation("获取一个词条多种翻译的术语")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> checkSykSameEntry(@RequestBody TranslateEntity translate , @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                                                      @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        try {
            ResponseListModel<TranslateEntity> responseListModel = new ResponseListModel<>();
            translate.setEntry(StringUtil.addEscapeCharacter(translate.getEntry()));
            translate.setTranslate(StringUtil.addEscapeCharacter(translate.getTranslate()));
            /* 获取术语库中entry,type,department相同,但翻译不同的术语(translate为null，entry,type,department不为null) */
            List<TranslateEntity> acquireSykSameEntry = sykService.acquireSykSameEntry(translate,pageIndex,pageSize);
            responseListModel.setList(acquireSykSameEntry); 
            responseListModel.setTotalNum(sykService.acquireSykSameEntry(translate,-1,-1).size());
            return checkResult(responseListModel);
        } catch (Exception e) {
            return handleRegexpOrRethrow(e);
        }
    }

    @PostMapping("/getSameEntryRelation")
    @ApiOperation("获取术语库词条关联信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getSameEntryRelation(@RequestBody TranslateEntity template) {
        /* 获取符合条件的所有术语 */
        ResponseListModel<SykEntryVO> responseListModel = new ResponseListModel();
        Set<String> matchList = new HashSet<>();
        matchList.add("translate");
        matchList.add("entry");
        List<TranslateEntity> translateEntities = sykService.getSykEntry(template,matchList,-1,-1);
        List<SykEntryVO> translateEntityList = sykService.getSykEntryRelation(translateEntities);
        responseListModel.setList(translateEntityList);
        responseListModel.setTotalNum(translateEntityList.size());
        return checkResult(responseListModel);
    }

    /**
     * 非法 REGEXP 返回可读错误；其它异常原样抛出。
     */
    @SuppressWarnings("rawtypes")
    private HttpResponse handleRegexpOrRethrow(Exception e) {
        if (isRegexpRelated(e)) {
            String detail = rootMessage(e);
            log.warn("Syk list REGEXP failed: {}", detail);
            return error(null, "正则无效或 SQL 错误: " + detail);
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        throw new RuntimeException(e);
    }

    private static boolean isRegexpRelated(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String msg = t.getMessage();
            if (msg == null) {
                continue;
            }
            String lower = msg.toLowerCase();
            if (lower.contains("regexp")
                    || (lower.contains("got error") && lower.contains("from regexp"))) {
                return true;
            }
        }
        return false;
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getMessage() != null ? cur.getMessage() : e.getClass().getSimpleName();
    }
}
