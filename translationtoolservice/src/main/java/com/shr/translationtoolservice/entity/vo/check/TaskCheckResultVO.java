package com.shr.translationtoolservice.entity.vo.check;

import java.util.ArrayList;
import java.util.Collection;

import lombok.Data;

@Data
public class TaskCheckResultVO {
    

    boolean success;

    boolean canBackFill;

    Summary summary;

    Collection<Issue> issues = new ArrayList<>();

    Collection<String> previews = new ArrayList<>();

    Attachments attachments;

    @Data
    public static class Summary{


    };

    @Data
    public static class Issue{

        public Level level;

        public IssueType type;

        public String message;

        public static enum Level{
            WARN,
            ERROR,
            FATAL
        }

        public static enum IssueType{
            ENTRY_MISSING,
            PARENT_ID_NOT_FOUND,
            INFO_NOT_MATCH_WITH_DB,
            TRANSLATE_NOT_CORRECT,
            TRANSLATE_LENGTH,
            COLUMN_NOT_FOUND
        }
    }


    @Data
    public static class Attachment{

        public String fileName;

        public String downloadUrl;
    }
    @Data
    public static class Attachments{

        Attachment issueLog;
    }

}
