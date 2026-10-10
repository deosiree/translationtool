package com.shr.translationtoolservice.service.entry;

import java.util.Collection;
import com.shr.translationtoolservice.entity.FileInputStreamEntity;
import com.shr.translationtoolservice.entity.TaskInfoEntity;
import com.shr.translationtoolservice.entity.User;
import com.shr.translationtoolservice.entity.vo.WorkBenchVO;

public abstract class AbstractEntryImportHandler<E> {
    
    public abstract Collection<E> importExcel(FileInputStreamEntity fileInputStreamEntitiy, User user, WorkBenchVO.EntryImportFileTypeVO entryImportFileTypeVO, TaskInfoEntity taskInfoEntity, String encoding) throws Exception;

    public abstract Collection<E> importCSV(FileInputStreamEntity fileInputStreamEntitiy, User user, TaskInfoEntity taskInfoEntity, String encoding) throws Exception;

    public abstract Collection<E> importXML(FileInputStreamEntity fileInputStreamEntitiy, User user, TaskInfoEntity taskInfoEntity, String templateType) throws Exception;

    public abstract Collection<E> importTS(FileInputStreamEntity fileInputStreamEntitiy, User user, TaskInfoEntity taskInfoEntity) throws Exception;

    public abstract Collection<E> importDIC(FileInputStreamEntity fileInputStreamEntitiy, User user, TaskInfoEntity taskInfoEntity,String encoding) throws Exception;

}
