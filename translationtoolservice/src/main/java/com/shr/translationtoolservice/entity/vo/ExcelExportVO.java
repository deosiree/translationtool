package com.shr.translationtoolservice.entity.vo;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import lombok.Data;

import java.util.List;

/**
 * @ClassName ExcelExportVO
 * @Description excel 导出文件入参
 * @USER: Cola
 * @Date 2024/2/27 0027 11:09
 **/
@Data
public class ExcelExportVO {
    //筛选条件
    private EntryInfoEntity entryInfoEntity;
    //自定义列名
    private List<String> columnNames;
    //文件名字
    private String excelName;
    //导出数据
    private List<EntryInfoEntity> entryInfoEntities;
}
