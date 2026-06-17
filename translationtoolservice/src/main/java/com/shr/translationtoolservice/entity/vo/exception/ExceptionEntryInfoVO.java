package com.shr.translationtoolservice.entity.vo.exception;

import com.shr.translationtoolservice.entity.vo.EntryInfoVO;

public class ExceptionEntryInfoVO extends ExceptionVO{

    EntryInfoVO entryInfoVO;

    public EntryInfoVO getEntryInfoVO() {
        return entryInfoVO;
    }

    public void setEntryInfoVO(EntryInfoVO entryInfoVO) {
        this.entryInfoVO = entryInfoVO;
    }

    public ExceptionEntryInfoVO() {
    }
    
}
