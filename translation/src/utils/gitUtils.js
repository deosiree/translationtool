// git 提交 / 推送相关通用工具函数
import { message } from "ant-design-vue";
import { gitCommit, gitPush } from "@/http/api/workbench.js";

/**
 * 执行 git commit，统一处理成功/失败提示
 * @param {{ ip: string, branch: string, versionName: string }} params
 * @returns {Promise<boolean>} 是否提交成功
 */
export async function doGitCommit(params) {
  try {
    await gitCommit(params);
    message.success("commit提交成功");
    return true;
  } catch (error) {
    message.error(`commit提交失败: ${error?.message || error}`);
    // 控制台保留详细错误
    // eslint-disable-next-line no-console
    console.error("commit提交失败", error);
    return false;
  }
}

/**
 * 执行 git push，统一处理成功/失败提示
 * @param {{ ip: string }} params
 * @returns {Promise<boolean>} 是否推送成功
 */
export async function doGitPush(params) {
  try {
    await gitPush(params);
    message.success("push推送成功");
    return true;
  } catch (error) {
    message.error(`push推送失败: ${error?.message || error}`);
    // eslint-disable-next-line no-console
    console.error("push推送失败", error);
    return false;
  }
}

/**
 * 先 commit 再 push 的通用流程
 * @param {{ ip: string, branch: string, versionName: string }} commitParams
 * @param {{ ip: string }} pushParams
 * @returns {Promise<{commitSuccess: boolean, pushSuccess: boolean}>}
 */
export async function doCommitAndPush(commitParams, pushParams) {
  const commitSuccess = await doGitCommit(commitParams);
  let pushSuccess = false;
  if (commitSuccess) {
    pushSuccess = await doGitPush(pushParams);
  }
  return { commitSuccess, pushSuccess };
}

