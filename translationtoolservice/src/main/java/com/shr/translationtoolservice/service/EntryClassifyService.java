package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.EntryClassify;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 *
 */
public interface EntryClassifyService extends IService<EntryClassify> {


    List<EntryClassify> getEntryClassfy(String department,String className,HttpServletRequest request);

    String updateEntryClassfy(EntryClassify entryClassify);

    String deleteEntryClassfy(List<String> idList);

    String addEntryClassfy(EntryClassify entryClassify, HttpServletRequest request);
}
