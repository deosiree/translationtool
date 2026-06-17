package com.shr.translationtoolservice.entity.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * @ClassName TaskInfoVo  web 交互实体
 * @USER: Cola
 * @Date 2023/11/9 0009 11:28
 **/
@Data
public class TaskInfoVo {


    private String versionName;
    private String productName;
    private String productId;



    /**
     * 主键
     */
    private String id;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 任务名字
     */
    private String name;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;

    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date endTime;

    /**
     * 部门
     */
    private String department;

    /**
     * 开发员
     */
    private String developer;

    /**
     * 词条审核员
     */
    private String entryAuditor;

    /**
     * 翻译员
     */
    private String translator;

    /**
     * 翻译审核员
     */
    private String translationAuditor;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务状态
     */
    private String state;

    /**
     * 版本ID
     */
    private String versionId;

    /**
     * 开发员操作时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date importTime;

    /**
     * 词条审核操作时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date entryAutiorStartTime;

    /**
     * 翻译审核操作时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date translationAuditorStartTime;

    /**
     * 翻译员操作时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date translateStartTime;

    /**
     * 任务下达时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date deliveryTime;

    /**
     * 翻译类型
     */
    private String translateType;

    public List<String> ignore;         // 分支新建功能, 设定忽略的ts和dic文件的名称



}
