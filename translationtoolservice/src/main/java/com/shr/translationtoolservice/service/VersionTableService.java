package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.VersionTable;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 *
 */
public interface VersionTableService extends IService<VersionTable> {

    ResponseListModel<VersionTable> getVersionTableByCondition(VersionTable versionTable, Integer pageIndex, Integer pageSize);

    String batchDeleteVersionTable(List<String> ids);
}
