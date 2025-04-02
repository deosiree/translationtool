package com.shr.translationtoolservice.controller;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.vo.EntryVO;
import com.shr.translationtoolservice.entity.vo.TaskEntryVO;
import com.shr.translationtoolservice.entity.vo.TsVo;
import com.shr.translationtoolservice.entity.vo.TsVo.TsEntryInfoVo;
import com.shr.translationtoolservice.service.CheckService;
import com.shr.translationtoolservice.service.EntryInfoService;
import com.shr.translationtoolservice.service.SykService;
import com.shr.translationtoolservice.util.JWTTokenUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/checkManage")
@Api(tags = "校验管理")
@Slf4j
public class CheckController  extends BaseController{

    @Autowired
    private CheckService checkService;

    @Autowired
    private EntryInfoService entryInfoService;

    @Autowired
    private SykService sykService;

    @GetMapping("/getModuleNames")
    @ApiOperation("查询数据库中所有的模块名称")
    @CrossOrigin
    @Transactional
    public HttpResponse<Map<String, Object>> getModuleNames( @RequestParam String url ) {
        JsonObject res = checkService.getModuleNames(url);
        if (!Objects.isNull(res)) {
            Gson gson = new Gson();
            Map<String, Object> resMap = gson.fromJson(res, Map.class);
            return checkResult(resMap);
        } else {
            return checkResult(null,"服务异常！");
        }
    }

    @GetMapping("/getQuestionTypes")
    @ApiOperation("查询数据库中所有的问题类型")
    @CrossOrigin
    @Transactional
    public HttpResponse< Map<String, Object>> getQuestionTypes( @RequestParam String url ) {
        JsonObject res = checkService.getQuestionTypes(url);
        if (!Objects.isNull(res)) {
            Gson gson = new Gson();
            Map<String, Object> resMap = gson.fromJson(res, Map.class);
            return checkResult(resMap);
        } else {
            return checkResult(null,"服务异常！");
        }
    }

    @PostMapping("/searchCheckInfo")
    @ApiOperation("根据模块名称和问题名称进⾏校验")
    @CrossOrigin
    @Transactional
    public HttpResponse<Map<String, Object>> searchCheckInfo( @RequestParam String i18n, @RequestParam String questionType,@RequestBody String params) {
        JsonObject res = checkService.searchCheckInfo(i18n,questionType,params);
        if (!Objects.isNull(res)) {
            Gson gson = new Gson();
            Map<String, Object> resMap = gson.fromJson(res, Map.class);
            return checkResult(resMap);
        } else {
            return checkResult(null,"服务异常！");
        }
    }

    @PostMapping("/tsProblems")
    @ApiOperation("检查所有ts文件是否有类名、tag、词条相同，翻译不同的情况")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel<TsVo>> searchTsProblems(@RequestBody EntryInfoEntity entryInfoEntityTemplate,HttpServletRequest request) {
        ResponseListModel<TsVo> model = new ResponseListModel<>();
        try {
            String department = JWTTokenUtils.getDepartment(request.getHeader("token"));
            List<TsVo> tsEntryWithProblem = checkService.getTsEntryWithProblem(entryInfoEntityTemplate,department);            
            
            model.setList(tsEntryWithProblem);
            model.setTotalNum(tsEntryWithProblem.size());
            return ok(model);

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return error(null, e.getMessage());
        }
    }


    @PostMapping("/getEntryByTsVo")
    @ApiOperation("展示 词条,所属类,tag,点击详情展示 对应ts文件,翻译")
    @CrossOrigin
    @Transactional
    public HttpResponse<ResponseListModel> getEntryByTsVo(@RequestBody List<TsVo> tsVoList) {
        ResponseListModel model = new ResponseListModel<>();
        try {
            // 去重
            List<EntryInfoEntity> tsEntryWithProblem = entryInfoService.getEntryInfoByTsVo(tsVoList);
            model.setList(tsEntryWithProblem);
            model.setTotalNum(tsEntryWithProblem.size());
            return ok(model);

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return error(null, e.getMessage());
        }
    }


}
