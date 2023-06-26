package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.SearchCondition;
import com.shr.translationtoolservice.entity.Term;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Service
public interface TermManagementService {
    //查询词条信息
    List<Term> search_term(@RequestBody SearchCondition searchCondition,
                     @RequestParam(value = "pageIndex", defaultValue = "1") Integer pageIndex,
                     @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize);
    //批量审核
    String bathAudit(List<String> id);
}
