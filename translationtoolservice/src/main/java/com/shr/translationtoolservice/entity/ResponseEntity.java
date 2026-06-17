package com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.TableField;

import lombok.Data;

@Data
public class ResponseEntity {
    
    @TableField
    private String taskId;

    @TableField
    private String task;

    @TableField
    private String content;

    @TableField
    private String dataType;

    @TableField
    private String updateTime;

    @TableField
    private String createTime;
}
