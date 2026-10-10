package com.shr.translationtoolservice.entity.vo;

import com.shr.translationtoolservice.entity.EntryInfoEntity;
import com.shr.translationtoolservice.entity.EntryInfoEntityForExcel;

import lombok.Data;

import java.util.List;
import java.util.function.Predicate;

/**
 * @ClassName ExcelExportVO
 * @Description excel 导出文件入参
 * @USER: Cola
 * @Date 2024/2/27 0027 11:09
 **/
@Data
public class ExcelExportVO<T extends EntryInfoEntity> {
    //筛选条件
    private T entryInfoEntity;
    //自定义列名
    private List<String> columnNames;
    //文件名字
    private String excelName;
    //导出数据
    private List<T> entryInfoEntities;
    /* 导出的文件类型: xlsx,csv,xml */
    private String exportFileType;
    /* 判断词条是否不应当被导出,test方法为true，代表不应该被导出 */
    private Predicate<EntryInfoEntityForExcel> predicate;

}
