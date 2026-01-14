package com.shr.translationtoolservice.entity;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public class FileInputStreamEntity extends InputStreamEntity {
    

    private FileInputStreamEntity(InputStream ins,String fileName) {
        super(ins);
        this.fileName = fileName;
    }

    public String fileName;

    public String getFileName() {
        return fileName;
    }

    public static FileInputStreamEntity convertFrom(MultipartFile file){
        try {
            if(file == null){
                return null;
            }
            FileInputStreamEntity fileInputStreamEntitiy = new FileInputStreamEntity(file.getInputStream(), file.getOriginalFilename());
            return fileInputStreamEntitiy;
        } catch (Exception e) {
            throw new RuntimeException(String.format("创建FileInputStreamEntitiy对象时出现异常, 异常信息为 :%s", e.getMessage()));
        }
    }




}
