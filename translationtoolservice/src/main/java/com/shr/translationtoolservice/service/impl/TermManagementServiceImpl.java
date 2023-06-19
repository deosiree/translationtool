package com.shr.translationtoolservice.service.impl;

import com.shr.translationtoolservice.entity.SearchCondition;
import com.shr.translationtoolservice.entity.Term;
import com.shr.translationtoolservice.service.TermManagementService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName TermManagementServiceImpl
 * @Description TODO
 * @USER: Cola
 * @Date 2023/6/19 0019 16:58
 **/
@Service
public class TermManagementServiceImpl implements TermManagementService {
    @Override
    //TODO
    public List<Term> search_term(SearchCondition searchCondition, Integer pageIndex, Integer pageSize) {
        return null;
    }

    @Override
    //TODO
    public String bathAudit(List<String> id) {
        return null;
    }
}
