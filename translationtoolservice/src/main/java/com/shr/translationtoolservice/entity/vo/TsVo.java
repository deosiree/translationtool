package com.shr.translationtoolservice.entity.vo;

import static org.junit.jupiter.api.Assertions.fail;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
public class TsVo {

    private String entry;

    private String comment;

    private String tag;

    private String translate;

    public static boolean isEquals(String text1,String text2){
        if(text1 == null && text2 == null){
            return true;
        }
        if(text1 == null || text2 == null){
            return false;
        }
        return text1.equals(text2);
    }


    public boolean isEqualsNotConsiderTrans(TsVo tsVo){
        return isEquals(entry,tsVo.getEntry()) && isEquals(tag, tsVo.getTag()) && isEquals(comment, tsVo.getComment());
    }
    
    

}
