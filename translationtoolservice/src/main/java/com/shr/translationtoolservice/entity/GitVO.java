package com.shr.translationtoolservice.entity;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Arrays;

public class GitVO {

    public String i18nUrl = "";

    public final String RELATIVE_PATH = "/git/execute";

    public UserInformation userInformation;

    public Commit commit = new Commit();

    public Push push = new Push();

    public Checkout checkout = new Checkout();
    
    public CreateBranch createBranch = new CreateBranch();

    public Remote remote = new Remote();

    public GitVO(String i18nUrl){
        this.i18nUrl = i18nUrl;
    }


    public static class UserInformation{

        public String userName = "shr";

        public String email = "shr@5000";
    }

    public static class Commit{
        public String commitMessage = "";
    }

    public static class Push{

        public String repository;

        public String branch;
    }

    public static class Checkout{

        public String branch;
    }

    public static class CreateBranch{

        public String newBranchName;

        public String basedBranchName;

        public String commitHash;
    }

    public static class Remote{

        public String repository;

        public String url;

    }
    
    public static class GitI18nResult{

        String resultCode;

        private String message;

        private String terminalMessage;

        public String getTerminalMessage() {
            return terminalMessage;
        }

        public static boolean isSuccess(GitI18nResult gitI18nResult){
            if(gitI18nResult.resultCode == null){
                return false;
            }
            return gitI18nResult.resultCode.equals("200");
        }

        public String getMessage(){
            return message;
        }

        

        
        public static String parseForShowCurrentBranch(GitI18nResult i18nResult){
            /*
             * 返回的信息为:   branch2\n  master\n  newBranch\n* newBranchCreate\n
             * 每一行前会有几个空格，需要先去掉空格，根据git规范，分支名不能以空格开头
             */
            String targetMessage = i18nResult.terminalMessage;
            if(targetMessage == null){
                return null;
            }
            return targetMessage.split("\n")[0];

        }

        public static String[] parseForShowBranches(GitI18nResult i18nResult){
            /*
             * 返回的信息为:   branch2\n  master\n  newBranch\n* newBranchCreate\n
             * 每一行前会有几个空格，需要先去掉空格，根据git规范，分支名不能以空格开头
             */
            String targetMessage = i18nResult.terminalMessage;
            if(targetMessage == null){
                return null;
            }
            String[] branches = targetMessage.split("\n");
            for(int i =0 ; i < branches.length ; i ++ ){
                String currentBranch = branches[i];
                if(currentBranch.length() <= 2){
                    return null;    // 不可能小于2，因为前两个字符是空格
                }
                branches[i] = currentBranch.substring(2,currentBranch.length());
            }
            return branches;

        }
    }
}
