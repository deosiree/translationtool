package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shr.translationtoolservice.entity.vo.EntryVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 *
 */
public interface EntryInfoService extends IService<EntryInfoEntity> {

    List<EntryVO> getEntryByVersion(String vsersionID,Integer offset,Integer pageSize);

    int getEntryByVersionTotal(String vsersionID);

    HttpResponse<String> addEntryByVersion(List<EntryVO> entryVOS, HttpServletRequest request);

    String addEntryInfo(EntryInfoEntity entryInfoEntity, HttpServletRequest request,String tableName);
}
