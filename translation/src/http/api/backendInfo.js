import request from "../request";
import {
  getEntrysourceListByClassfyTaskStateMock,
  getEntrysourceListByClassfyResultMock,
} from "./mock/backendInfo";

// 返回任务状态（分支新建-基于lang文档新增任务/taskManage/createTaskByLang）
export function getLangDirImportTaskState(params) {
  return request({
    url: "/backendInfo/getLangDirImportTaskState",
    method: "POST",
    params,
  });
}

// 查询更新词条任务的状态
export function getEntrysourceListByClassfyTaskState(params) {
  // console.log("查询更新词条任务的状态params", params);
  // return getEntrysourceListByClassfyTaskStateMock(params);// mock

  return request({
    url: "/backendInfo/getEntrysourceListByClassfyTaskState",
    method: "POST",
    params,
  });
}

// 获取更新词条任务的结果
export function getEntrysourceListByClassfyResult(params) {
  // console.log("获取更新词条任务的结果params", params);
  // return getEntrysourceListByClassfyResultMock(params);// mock

  return request({
    url: "/backendInfo/getEntrysourceListByClassfyResult",
    method: "POST",
    params,
  });
}
