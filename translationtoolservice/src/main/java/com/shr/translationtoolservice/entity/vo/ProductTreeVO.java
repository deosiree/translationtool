package com.shr.translationtoolservice.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * @ClassName ProductTreeVO
 * @USER: Cola
 * @Date 2023/11/20 0020 15:27
 **/
@Data
public class ProductTreeVO {
    private String name;
    private String type;
    private String creator;
    private String department;
    private String remark;
    private List<ProductTreeVO> childrenList;
}
