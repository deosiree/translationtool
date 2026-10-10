package com.shr.translationtoolservice.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.common.Token;
import com.shr.translationtoolservice.dao.EntryInfoMapper;
import com.shr.translationtoolservice.dao.TranslateMapper;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.TranslateEntity;
import com.shr.translationtoolservice.entity.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @ClassName ConfigManageController
 * @Description
 * @USER: Cola
 * @Date 2024/12/20 0020 14:07
 **/
@RestController
@RequestMapping("/test")
@Api(tags = "测试")
@Slf4j
public class TestController extends BaseController {

    @Autowired
    private TranslateMapper translateMapper;
    @GetMapping("/beijing-time")
    public String getBeijingTime() {
        // 获取当前时间
        Date date = new Date(System.currentTimeMillis());

        // 创建一个 Calendar 实例并设置为当前时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        // 设置时区为北京时间
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Shanghai");
        calendar.setTimeZone(timeZone);

        // 获取北京时间
        Date beijingTime = calendar.getTime();
        System.out.println(java.util.TimeZone.getDefault());
        // 格式化输出
        return "北京时间: " + beijingTime.toString() + " system: " + java.util.TimeZone.getDefault();
    }

    //编辑词条
    @PostMapping("/checkSYKEntry")
    @ApiOperation("查询术语库中相同词条不同翻译的情况")
    @CrossOrigin
    @Transactional(propagation = Propagation.NESTED)
    @Token
    public HttpResponse<Map> checkSYKEntry() {
        List<TranslateEntity> translateEntityList = translateMapper.selectList(new QueryWrapper<TranslateEntity>());
        Map<String, List<TranslateEntity>> map = new HashMap<>();
        //查询translateEntityList中 相同entry 不同 translate的情况
        for (int i = 0; i < translateEntityList.size(); i++) {
            TranslateEntity translateEntity = translateEntityList.get(i);
            for (int j = i + 1; j < translateEntityList.size(); j++) {
                TranslateEntity translateEntity1 = translateEntityList.get(j);
                if (StringUtils.isBlank(translateEntity.getEntry()) || StringUtils.isBlank(translateEntity1.getEntry())
                        || StringUtils.isBlank(translateEntity.getTranslate()) || StringUtils.isBlank(translateEntity1.getTranslate())) {
                    continue;
                }

                if (translateEntity.getEntry().equals(translateEntity1.getEntry()) && !translateEntity.getTranslate().equals(translateEntity1.getTranslate())) {
                    log.info("entry:{} 有不同的翻译", translateEntity.getEntry());
                    log.info("id:{}  translate:{}", translateEntity.getId(), translateEntity.getTranslate());
                    log.info("id:{}  translate:{}", translateEntity1.getId(), translateEntity1.getTranslate());
                    map.put(translateEntity.getEntry(), Arrays.asList(translateEntity, translateEntity1));
                }
            }
        }
        return checkResult(map);
    }

    @Autowired
    private EntryInfoMapper entryInfoMapper;
//    @GetMapping("/updateWarnEnTransID")
//    public String updateWarnEnTransID() {
//        List<EntryInfoEntity> parentEntry = entryInfoMapper.getParentEntry();
//        List<EntryInfoEntity> childEntry = entryInfoMapper.getChildEntry();
//        //遍历childEntry 通过parentEntry的id找到对应的childEntry parentID
//        int total = 0;
//        for (EntryInfoEntity entryInfoEntity : childEntry) {
//            for (EntryInfoEntity parent : parentEntry) {
//                if (entryInfoEntity.getParentID().equals(parent.getId())) {
//                    entryInfoEntity.setEnTransId(parent.getEnTransId());
//                    total += entryInfoMapper.updateById(entryInfoEntity);
//                }
//            }
//        }
//        log.info("更新了{}条数据", total);
//        return ConstantInterface.OK_STR;
//    }
}
