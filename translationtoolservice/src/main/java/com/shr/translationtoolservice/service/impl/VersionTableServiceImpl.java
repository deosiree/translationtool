package com.shr.translationtoolservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shr.translationtoolservice.entity.ConstantInterface;
import com.shr.translationtoolservice.entity.ErrorCodeList;
import com.shr.translationtoolservice.entity.ResponseListModel;
import com.shr.translationtoolservice.entity.VersionTable;
import com.shr.translationtoolservice.service.VersionTableService;
import com.shr.translationtoolservice.dao.VersionTableMapper;
import com.shr.translationtoolservice.util.CommonUtils;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 *
 */
@Service
public class VersionTableServiceImpl extends ServiceImpl<VersionTableMapper, VersionTable>
    implements VersionTableService{

    @Autowired
    private CommonUtils commonUtils;

    @Autowired
    private VersionTableMapper versionTableMapper;

    @Override
    public ResponseListModel<VersionTable> getVersionTableByCondition(String version, Integer pageIndex, Integer pageSize) {
        ResponseListModel responseListModel = new ResponseListModel();
        if (commonUtils.checkPage(pageIndex,pageSize)) {
            int offset = (pageIndex - 1) * pageSize;
            // 分页查询数据
            List<VersionTable> versionInfoByVersion = versionTableMapper.getVersionTableByCondition(version,offset,pageSize);
            responseListModel.setList(versionInfoByVersion);
            // 查询符合条件的数据总数
            Integer total = versionTableMapper.getTotalByCondition(version);
            responseListModel.setTotalNum(total);
        }
        return responseListModel;
    }

    @Override
    @Transactional
    public String batchDeleteVersionTable(List<String> ids) {
        // 根据ids查询版本库信息
        List<VersionTable> versionTables = versionTableMapper.selectBatchIds(ids);
        // 删除版本库信息
        int delete = versionTableMapper.deleteBatchIds(ids);
        if (delete < ConstantInterface.DB_SUCCESS_RESULT){
            return ErrorCodeList.DELETE_ERROR;
        }
        // 删除版本库中的词条
        for (VersionTable versionTable : versionTables) {
            int i = versionTableMapper.deleteEntryByVersion(versionTable.getVersionTableName(),versionTable.getVersion());
            if (i < ConstantInterface.DB_SUCCESS_RESULT){
                return ErrorCodeList.DELETE_ERROR;
            }
        }
        return ConstantInterface.OK_STR;
    }
}




