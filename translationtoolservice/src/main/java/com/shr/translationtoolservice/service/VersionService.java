package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.VersionEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 *
 */
public interface VersionService extends IService<VersionEntity> {

    List<VersionEntity> getVersion(@Param("versionEntity") VersionEntity versionEntity);

    int getVersionTotal(VersionEntity versionEntity);

    String createVersion(VersionEntity versionEntity, HttpServletRequest request);

    String updateVersion(VersionEntity versionEntity);

    String deleteVersion(List<String> idList);
}
