package com.shr.translationtoolservice.entity.vo;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shr.translationtoolservice.entity.EntryInfoEntity;

import lombok.Data;


@Data
public class TsVo {

    private String entry;

    private String comment;

    private String tag;

    private String translate;

    private String type;

    public static TsVo copy(TsVo tsVo){
        TsVo tsVoTarget = new TsVo();
        tsVoTarget.setComment(tsVo.getComment());
        tsVoTarget.setTag(tsVo.getTag());
        tsVoTarget.setEntry(tsVo.getEntry());
        tsVoTarget.setType(tsVo.getType());
        return tsVoTarget;
    }

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

    @Data
    public static class TsEntryInfoVo{

        private List<EntryInfoEntity> entryInfoEntities;

        private TsVo tsVo;

    }
}
