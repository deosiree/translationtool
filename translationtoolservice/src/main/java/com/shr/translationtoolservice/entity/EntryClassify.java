package  com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Data;

import javax.persistence.Column;

/**
 * 词条分类表
 * @TableName t_entry_classify
 */
@TableName(value ="t_entry_classify")
@Data
public class EntryClassify implements Serializable {

    @TableField(exist = false)
    List<EntryClassify> children;
    /**
     * 
     */

    @TableId(value = "id")
    @Column(name = "`key`")
    private String key;

    /**
     *
     */

    @TableField("`type`")
    private String type;

    /**
     * 父id
     */
    private String parentId;

    /**
     * 分类名称
     */
    @TableField("`name`")
    private String title;

    /**
     * 序号
     */
    @TableField("`index`")
    private Integer index;

    /**
     * 中文限制字符数
     */
    @TableField("`max_byte`")
    private Integer maxByte;

    /**
     * 外文限制字符数
     */
    @TableField("`foreign_max_byte`")
    private Integer foreignMaxByte;

    /**
     * 部门
     */
    @TableField("`department`")
    private String department;

    /**
     * 创建人
     */
    @TableField("`creator`")
    private String creator;

    /**
     * 词条对应的代码版本
     */
    @TableField("`codeBranch`")
    private String codeBranch;

    /**
     * 部门
     */
    @TableField("`create_time`")
    private Date createTime;

    /**
     * 删除状态（0存在，1删除）
     */
    @TableField("`is_delete`")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}