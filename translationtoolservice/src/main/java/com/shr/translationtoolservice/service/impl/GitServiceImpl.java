package com.shr.translationtoolservice.service.impl;

import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.shr.translationtoolservice.entity.GitVO;
import com.shr.translationtoolservice.entity.GitVO.GitI18nResult;
import com.shr.translationtoolservice.service.GitService;
import com.shr.translationtoolservice.util.HTTPUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GitServiceImpl implements GitService {


    @Autowired
    protected HTTPUtils httpUtils;

    protected Gson gson = new Gson();

    protected JSONObject requestBodyForInitRespository(GitVO gitVO){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("task", "0") ;
        jsonObject.put("username", gitVO.userInformation.userName);
        jsonObject.put("email", gitVO.userInformation.email);
        return jsonObject;
    }

    protected JSONObject requestBodyForCreateBranch(GitVO gitVO){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("task", "1");
        jsonObject.put("new_branch_name", gitVO.createBranch.newBranchName);
        jsonObject.put("based_branch_name", gitVO.createBranch.basedBranchName);
        return jsonObject;
    }


    protected JSONObject requestBodyForCreateBranchFrom(GitVO gitVO){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("task", "2");
        jsonObject.put("new_branch_name", gitVO.createBranch.newBranchName);
        jsonObject.put("based_branch_name", gitVO.createBranch.basedBranchName);
        jsonObject.put("commit_hash", gitVO.createBranch.commitHash);
        return jsonObject;
    }

    protected JSONObject requestBodyForCheckout(GitVO gitVO){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("task", "3");
        jsonObject.put("branch", gitVO.checkout.branch);
        return jsonObject;
    }

    protected JSONObject requestBodyForCommit(GitVO gitVO){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("task", "4");
        jsonObject.put("commit_message", gitVO.commit.commitMessage);
        return jsonObject;
    }

    protected JSONObject requestBodyForPush(GitVO gitVO){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("task", "5");
        jsonObject.put("branch", gitVO.push.branch);
        jsonObject.put("repository", gitVO.push.repository);

        return jsonObject;
    }

    protected JSONObject requestBodyForShowCurrentBranch(GitVO gitVO){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("task", "6");
        return jsonObject;
    }

    protected JSONObject requestBodyForShowBranches(GitVO gitVO){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("task", "7");
        return jsonObject;
    }

    protected JSONObject requestBodyForAddRemote(GitVO gitVO){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("task", "8");
        jsonObject.put("repository", gitVO.remote.repository);
        jsonObject.put("url", gitVO.remote.url);
        return jsonObject;
    }

    protected String assembleURL(String address,String relativePath){
        if(address.endsWith("/")){
            if(!relativePath.startsWith("/")){
                return address + relativePath;
            }else{
                return address + relativePath.substring(1,relativePath.length());
            }
        }else{
            if(!relativePath.startsWith("/")){
                return address + relativePath;
            }else{
                return address + "/" + relativePath;
            }
        }

    }


    protected GitI18nResult requestInternal(String requestPath,JSONObject jsonObject){
   
        String s = httpUtils.post(requestPath, jsonObject);   
        if(s == null || s.equals("")){
            throw new RuntimeException("i18n服务出现异常,请检查网络连接");
        }
        try {
            GitI18nResult result = this.gson.fromJson(s, new TypeToken<GitI18nResult>() {}.getType());   
            return result;  // 不可能为null
        } catch (Exception e) {
            // TODO: handle exception
            log.error("解析结果时出现异常", e);
            throw new RuntimeException("解析结果时出现异常,异常信息为: " + e.getMessage());
        }
    }


    @Override
    public GitI18nResult init(GitVO gitVO) {
        JSONObject jsonObject = requestBodyForInitRespository(gitVO);
        return requestInternal(assembleURL(gitVO.i18nUrl,gitVO.RELATIVE_PATH), jsonObject);
    }


    @Override
    public GitI18nResult commit(GitVO gitVO) {
        // TODO Auto-generated method stub
        JSONObject jsonObject = requestBodyForCommit(gitVO);
        return requestInternal(assembleURL(gitVO.i18nUrl,gitVO.RELATIVE_PATH), jsonObject);

    }

    @Override
    public GitI18nResult push(GitVO gitVO){
        JSONObject jsonObject = requestBodyForPush(gitVO);
        return requestInternal(assembleURL(gitVO.i18nUrl,gitVO.RELATIVE_PATH), jsonObject);

    }

    @Override
    public GitI18nResult checkout(GitVO gitVO) {
        // TODO Auto-generated method stub
        JSONObject jsonObject = requestBodyForCheckout(gitVO);
        return requestInternal(assembleURL(gitVO.i18nUrl,gitVO.RELATIVE_PATH), jsonObject);
    }

    @Override
    public GitI18nResult createBranch(GitVO gitVO) {
        // TODO Auto-generated method stub
        JSONObject jsonObject = requestBodyForCreateBranch(gitVO);
        return requestInternal(assembleURL(gitVO.i18nUrl,gitVO.RELATIVE_PATH), jsonObject);

    }

    @Override
    public GitI18nResult createBranchFrom(GitVO gitVO) {
        // TODO Auto-generated method stub
        JSONObject jsonObject = requestBodyForCreateBranchFrom(gitVO);
        return requestInternal(assembleURL(gitVO.i18nUrl,gitVO.RELATIVE_PATH), jsonObject);

    }

    @Override
    public GitI18nResult showCurrentBranch(GitVO gitVO) {
        // TODO Auto-generated method stub
        JSONObject jsonObject = requestBodyForShowCurrentBranch(gitVO);
        return requestInternal(assembleURL(gitVO.i18nUrl,gitVO.RELATIVE_PATH), jsonObject);
    }

    @Override
    public GitI18nResult showBranches(GitVO gitVO) {
        // TODO Auto-generated method stub
        JSONObject jsonObject = requestBodyForShowBranches(gitVO);
        return requestInternal(assembleURL(gitVO.i18nUrl,gitVO.RELATIVE_PATH), jsonObject);
    }

    @Override
    public GitI18nResult addRemote(GitVO gitVO) {
        // TODO Auto-generated method stub
        JSONObject jsonObject = requestBodyForAddRemote(gitVO);
        return requestInternal(assembleURL(gitVO.i18nUrl,gitVO.RELATIVE_PATH), jsonObject);
        
    }

    @Override
    public String processCommitMessage(String message) {
        /* 防止传递的提交信息中包含"--"," "这类字符导致git的commit命令执行异常 */
        // int length = message.length();
        // for(int i = 0 ; i < length ; i ++ ){
        //     if(message.charAt(i) == ' '){
        //         if(!message.startsWith("\"") && !message.endsWith("\"")){
        //             return '\"' + message + '\"';
        //         }
        //     }
        // }
        // return message;
        return decorate(message);
    }

    @Override
    public String decorate(String string) {
        // TODO Auto-generated method stub
        return '\"' + string + '\"';
    }



}
