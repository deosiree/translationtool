package com.shr.translationtoolservice.service;

import com.shr.translationtoolservice.entity.GitVO;
import com.shr.translationtoolservice.entity.GitVO.GitI18nResult;

public interface GitService {
    
    GitI18nResult init(GitVO gitVO);

    /* 修饰命令行中两个空格中间的字符串，在字符串两边添加'"',防止git解析错误 */
    String decorate(String string);

    String processCommitMessage(String message);

    GitI18nResult commit(GitVO gitVO);

    GitI18nResult push(GitVO gitVO);

    GitI18nResult checkout(GitVO gitVO);

    GitI18nResult createBranch(GitVO gitVO);

    GitI18nResult createBranchFrom(GitVO gitVO);

    GitI18nResult showCurrentBranch(GitVO gitVO);

    GitI18nResult showBranches(GitVO gitVO);

    GitI18nResult addRemote(GitVO gitVO);

}
