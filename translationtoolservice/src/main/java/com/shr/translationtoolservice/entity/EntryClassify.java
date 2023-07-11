package  com.shr.translationtoolservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 词条分类表
 * @TableName t_entry_classify
 */
@TableName(value ="t_entry_classify")
@Data
public class EntryClassify implements Serializable {
    /**
     * 
     */
    @TableId
    private String id;

    /**
     * 父id
     */
    private String parentId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 序号
     */
    private Integer index;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}