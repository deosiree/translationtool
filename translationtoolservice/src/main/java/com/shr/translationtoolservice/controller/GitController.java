package com.shr.translationtoolservice.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shr.translationtoolservice.common.HttpResponse;
import com.shr.translationtoolservice.entity.GitVO;
import com.shr.translationtoolservice.entity.GitVO.GitI18nResult;
import com.shr.translationtoolservice.service.GitService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/workbench")
@Api(tags = "Git管理")
@Slf4j
public class GitController extends BaseController {

    public final String RESPOSITORY = "origin";

    @Autowired
    private GitService gitService;
    

    protected void checkURL(String url){
        if(url == null ||  url.equals("") ){
            throw new RuntimeException("提供的url为空字符串或为null,无法执行后续操作");
        }
        /* 检查是否符合规范(是否是正确的IP，并正确的结合端口信息) */

    }

    /**
     * 展示当前所有的分支
     * @param ip
     * @return
     */
    @PostMapping("/getBranches")
    @CrossOrigin
    public HttpResponse<Map<String,String[]>> showAllBranches(@RequestParam(required = true) String ip){
        try {
            checkURL(ip);
            GitVO gitVo = new GitVO(ip);
            GitI18nResult result = gitService.showBranches(gitVo);
            if(!GitI18nResult.isSuccess(result)){
                /* 没有成功 */
                return error(null,result.getMessage() + ": " + result.getTerminalMessage());
            }
            String[] branches = GitI18nResult.parseForShowBranches(result);
            if(branches == null){
                return error(null, "git运行异常: " + result.getMessage() + "," + result.getTerminalMessage());
            }
            Map<String,String[]> responseMap = new HashMap<>();
            responseMap.put("list", branches);
            return ok(responseMap);
        } catch (Exception e) {
            log.error("获取所有分支时发生异常", e);
            return error(null,"获取所有分支时发生异常,异常信息为: " +  e.getMessage());
        }
    }


    /**
     * 先切换到对应分支,然后在提交
     * 
     * @param i18nUrl
     * @param requestBody
     * @return
     */
    @PostMapping("/gitCommit")
    @CrossOrigin
    public HttpResponse<String> commit(
        @RequestParam(required = true) String ip,
        @RequestParam(required = true) String branch,
        @RequestParam(required = true) String versionName){
        
        
        try {
            checkURL(ip);
            GitVO gitVo = new GitVO(ip);
            GitI18nResult currentBranchResult = gitService.showCurrentBranch(gitVo);
            if(!GitI18nResult.isSuccess(currentBranchResult)){
                return error(null, "查询当前分支名失败," + currentBranchResult.getMessage() + ", " + currentBranchResult.getTerminalMessage());
            }
            String currentBranch = GitI18nResult.parseForShowCurrentBranch(currentBranchResult);
            if(!currentBranch.equals(branch)){
                gitVo.checkout.branch = branch;
                GitI18nResult checkoutResult = gitService.checkout(gitVo);
                boolean isCheckoutSuccess = GitI18nResult.isSuccess(checkoutResult);
                if(!isCheckoutSuccess){
                    /* checkout错误,大概率分支不存在 */
                    return error(null, "切换到分支: " + branch + "失败, " + checkoutResult.getMessage() + ", " + checkoutResult.getTerminalMessage());
                }
            }
            gitVo.commit.commitMessage = gitService.processCommitMessage(versionName);
            GitI18nResult commitResult = gitService.commit(gitVo);
            boolean isCommitSuccess = GitI18nResult.isSuccess(commitResult);
            if(!isCommitSuccess){
                return error(null, "提交失败, " + commitResult.getMessage() + ", " + commitResult.getTerminalMessage());
            }
            return ok(commitResult.getMessage() + "," + commitResult.getTerminalMessage());

        } catch (Exception e) {
            log.error("提交git时系统异常", e);
            return error(null, e.getMessage());
        }

    }

    /**
     * 先获取当前的分支名，然后将该分支提交到远程仓库
     * @param ip
     * @param respository
     * @return
     */
    @PostMapping("/gitPush")
    @CrossOrigin
    public HttpResponse<String> push(
        @RequestParam(required = true) String ip,
        @RequestParam(required = false) String respository
    ){
        try {
            checkURL(ip);
            GitVO gitVO = new GitVO(ip);
            /* 先获取当前的分支 */
            GitI18nResult currentBranchResult = gitService.showCurrentBranch(gitVO);
            if(!GitI18nResult.isSuccess(currentBranchResult)){
                return error(null, currentBranchResult.getMessage() + ", " + currentBranchResult.getTerminalMessage());
            }
            String currentBranch = GitI18nResult.parseForShowCurrentBranch(currentBranchResult);
            if(currentBranch == null || currentBranch.equals("")){
                return error(null, "没有正常获取到当前分支");
            }
            gitVO.push.repository = respository == null ? this.RESPOSITORY : respository;
            gitVO.push.branch = currentBranch;
            GitI18nResult pushResult = gitService.push(gitVO);
            if(!GitI18nResult.isSuccess(pushResult)){
                return error(null, "推送到远程仓库失败, " + pushResult.getMessage() + ", " + pushResult.getTerminalMessage());
            }
            return ok(pushResult.getMessage() + "," + pushResult.getTerminalMessage());

        } catch (Exception e) {
            log.error("提交到远程仓库失败,系统服务异常", e);
            return error(null, e.getMessage());
        }
    }


    // @PostMapping("/gitCreateBranch")
    // @CrossOrigin
    // public HttpResponse<String> createBranch(
    //     @RequestParam(required = true) String ip,
    //     @RequestParam(required = true) String branchName
    // ){
    //     try {
    //         GitVO gitVO = new GitVO(ip);
    //         gitVO.createBranch.newBranchName = branchName;
    //         GitI18nResult createBranchResult = gitService.createBranch(gitVO);

    //     } catch (Exception e) {
    //         // TODO: handle exception
    //     }

    // }

}
