package com.shr.translationtoolservice.entity.vo;

public class WorkBenchVO {

    /**
     * 工作台导入界面的文件版本传参(针对装置开发部)
     */
    public static enum EntryImportFileTypeVO{

        NEW_FILE_VERSION,
        OLD_FILE_VERSION,
        COMMON_VERSION;


        public static EntryImportFileTypeVO parse(String param){
            if(param == null){
                return null;
            }

            if(param.equals("新模板")){
                return EntryImportFileTypeVO.NEW_FILE_VERSION;
            }else if(param.equals("旧模板")){
                return EntryImportFileTypeVO.OLD_FILE_VERSION;
            }else if(param.equals("通用模板")){
                return EntryImportFileTypeVO.COMMON_VERSION;
            }else{
                return null;
            }
        }

        public static String convertTOString(EntryImportFileTypeVO entryImportFileTypeVO){
            if(entryImportFileTypeVO == EntryImportFileTypeVO.COMMON_VERSION){
                return "通用模板";
            }
            if(entryImportFileTypeVO == EntryImportFileTypeVO.NEW_FILE_VERSION){
                return "新模板";
            }
            if(entryImportFileTypeVO == EntryImportFileTypeVO.OLD_FILE_VERSION){
                return "旧模板";
            }
            return "";
        }
    }
    
}
