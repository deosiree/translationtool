package com.shr.translationtoolservice.controller;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.EntryLabel;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.Translate;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.entity.vo.SykEntryVO;
import com.shr.translationtoolservice.service.SykService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;


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
        ResponseListModel responseListModel = new ResponseListModel();
        responseListModel.setList(sykService.getSykEntry(translate,pageIndex,pageSize));
        responseListModel.setTotalNum(sykService.getSykEntry(translate,-1,-1).size());
        return checkResult(responseListModel);
    }

    @PostMapping("/getSykEntryRelation")
    @ApiOperation("获取术语库词条关联信息")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getSykEntryRelation(@RequestBody List<TranslateEntity> translates) {
        ResponseListModel<SykEntryVO> responseListModel = new ResponseListModel();
        List<SykEntryVO> translateEntityList = sykService.getSykEntryRelation(translates);
        responseListModel.setList(translateEntityList);
        responseListModel.setTotalNum(translateEntityList.size());
        return checkResult(responseListModel);
    }

    @GetMapping("/getSykNotUsed")
    @ApiOperation("获取术语库未使用的翻译")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel<TranslateEntity>> getSykNotUsed(@RequestBody TranslateEntity translateTemplate,@RequestHeader Map<String,String> header) {
        ResponseListModel<TranslateEntity> responseListModel = new ResponseListModel<>();

        List<TranslateEntity> translateEntityList = sykService.getSykNotUsed(translateTemplate,header.get("token"));
        responseListModel.setList(translateEntityList);
        responseListModel.setTotalNum(translateEntityList.size());
        return checkResult(responseListModel);
    }

    @PostMapping("/updateSykEntry")
    @ApiOperation("更新术语库")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> updateSykEntry(@RequestBody List<TranslateEntity> translates) {

        return checkResult(sykService.updateSykEntry(translates));
    }

    @PostMapping("/checkSykEntry")
    @ApiOperation("检查翻译")
    @CrossOrigin
    @Transactional
    public HttpResponse<String> checkSykEntry(@RequestBody List<TranslateEntity> translates) {

        return checkResult(sykService.updateSykEntry(translates));
    }
}
