package com.shr.translationtoolservice.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * @ClassName UserUDetailsVo
 * @USER: Cola
 * @Date 2023/11/14 0014 11:04
 **/

@Data
public class UserDetailsVo {

    private String name;
    private Boolean entryReviewer ;
    private Boolean translateReviewer ;
    private Boolean developer ;
    private Boolean translator  ;
    private Boolean admin ;
    private String type;
    private String department;

    private Boolean readState;
    private Boolean writeState;

    private List<UserDetailsVo> children;
}
