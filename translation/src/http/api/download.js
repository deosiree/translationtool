import axios from "axios";  //引入axios
import env from "../env";
import store from '@/store'
import request from "../request";//引入request.js文件

// 版本词条导出
export function versionDownload(params) {
  return axios(
    {
      url: env.dev.baseUrl + '/entryInfo/versionExport',
      method: 'POST',
      params: params,
      headers: {
        token: store.state.token
      },
      responseType: 'blob'
    }
  )
}

// 任务词条导出
export function taskDownload(params) {
  return axios(
    {
      url: env.dev.baseUrl + '/taskManage/taskEntryExport',
      method: 'POST',
      params: params,
      headers: {
        token: store.state.token
      },
      responseType: 'blob'
    }
  )
}

// 根据任务id 导出词条
export function exportEntryBytaskId(params) {
  return axios(
    {
      url: env.dev.baseUrl + '/taskManage/exportEntryByTaskId',
      method: 'POST',
      params: params,
      headers: {
        token: store.state.token
      },
      responseType: 'blob'
    }
  )
}

// 导入模板下载
export function templateFileDownload(params) {
  return axios(
    {
      url: env.dev.baseUrl + '/workbench/getTemplateFile',
      method: 'POST',
      headers: {
        token: store.state.token
      },
      responseType: 'blob',
      params
    }
  )
}

// 已选词条导出前的校验
export function checkBeforeExportEntry(data, params) {
  return request({
    url: "/entryInfo/checkBeforeExportEntry",
    method: "POST",
    data,
    params
  });
}

// 已选词条导出
export function entryExportByCondition(data, params) {
  return axios(
    {
      url: env.dev.baseUrl + '/entryInfo/entryExportByCondition',
      method: 'POST',
      data: data,
      params: params,
      headers: {
        token: store.state.token
      },
      responseType: 'blob'
    }
  )
}

// 通用文件下载（从URL下载文件）
export function downloadFileFromUrl(downloadUrl) {
  return axios({
    url: downloadUrl.startsWith('http') ? downloadUrl : env.dev.baseUrl + downloadUrl,
    method: 'GET',
    responseType: 'blob',
    headers: {
      token: store.state.token
    }
  });
}
