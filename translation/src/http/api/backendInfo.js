import request from "../request";

// 返回任务状态（分支新建-基于lang文档新增任务/taskManage/createTaskByLang）
export function getLangDirImportTaskState(params) {
  return request({
    url: "/backendInfo/getLangDirImportTaskState",
    method: "POST",
    params,
  });
}