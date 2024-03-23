import axios from "axios";  //引入axios
import env from "../env"; 
import store from '@/store'

// 版本词条导出
export function versionDownload(params) {
    return axios(
        {
            url: env.dev.baseUrl+'/entryInfo/versionExport',
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
            url: env.dev.baseUrl+'/taskManage/taskEntryExport',
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
            url: env.dev.baseUrl+'/taskManage/exportEntryByTaskId',
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
            url: env.dev.baseUrl+'/workbench/getTemplateFile',
            method: 'POST',
            headers: {
                token: store.state.token
            },
            responseType: 'blob',
            params
        }
    )
}

// 导入模板下载
export function entryExportByCondition(data) {
    return axios(
        {
            url: env.dev.baseUrl+'/entryInfo/entryExportByCondition',
            method: 'POST',
            data:data,
            headers: {
                token: store.state.token
            },
            responseType: 'blob'
        }
    )
}
