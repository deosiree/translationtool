<template>
    <Modal 
    :visible="visible" 
    :modalTitle="modalTitle"
    @handleClose="handleClose"
    @handleOK="handleOK"
    >
        <div class="content">
            <a-form
                ref="formRef"
                name="custom-validation"
                :model="exportData"
            >
                <a-form-item label="导出字段" name="field">
                <a-tree-select
                        v-model:value="exportData.field"
                        style="width: 100%"
                        :tree-data="treeData"
                        tree-checkable
                        allow-clear
                        :show-checked-strategy="SHOW_PARENT"
                        placeholder="请选择"
                        tree-node-filter-prop="label"
                        :maxTagCount="maxTagCount"
                    />
                </a-form-item>
                <a-form-item label="导出格式" name="format">
                <a-select
                    v-model:value="exportData.type"
                    placeholder="请选择"
                    :options='typeList'
                    style="width:100%"
                    allowClear
                    >
                    </a-select>
                </a-form-item>
                <a-form-item label="导出数据" name="data">
                    <a-upload
                        v-model:file-list="fileList"
                        name="file"
                        :beforeUpload="beforeUpload"
                        :accept="accept"
                        @change="handleChange"
                    >
                        <a-button>
                        <UploadOutlined></UploadOutlined>
                        点击上传
                        </a-button>
                    </a-upload>
                </a-form-item>
            </a-form>
        </div>
    </Modal>
</template>
<script>
import tableParam from "./tableParam.js";
import Modal from '@/components/modal/index.vue';
import { message,TreeSelect } from 'ant-design-vue';
import {
  UploadOutlined,
} from '@ant-design/icons-vue';
import { 
    exportEntry
} from "@/http/api/entry";
import axios from "axios"; 
export default {
    components:{
        Modal,
        UploadOutlined
    },
    emits:['exportClose','exportOK'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:'导出词条'
        }
    },
    watch: {
        
    },
    data() {
        return{
            modalWidth:"450px",
            SHOW_PARENT:TreeSelect.SHOW_PARENT,
            exportData:{
                field:[],
                type:null
            },
            treeData:tableParam.checkboxList,
            typeList:[
                {value:'.xlsx',label:'.xlsx'},
                {value:'.xdt',label:'.xdt'},
                {value:'.tbx',label:'.tbx'}
            ],
            maxTagCount:2,
            fileList:[],
            accept:".xlsx"
        }
    },
    
    created() {
        
    },
    mounted () {
        
    },
    methods: {
        handleClose(){
            this.$emit("exportClose")
        },
        handleOK(){
            let formData = new FormData()
            formData.append('fields',this.exportData.field)
            formData.append('type',this.exportData.type)
            if(this.fileList.length > 0){
                formData.append('file',this.fileList[0].originFileObj)
            }
            
            exportEntry(formData).then((res) => {
                
                let contentType = res.headers['content-type']
                let fileName = res.headers["content-disposition"].split(";")[1].split("filename=")[1]
                
                const blob = new Blob([res.data], {type: contentType})
                const a = document.createElement('a') // 转换完成，创建一个a标签用于下载
                a.download = decodeURI(fileName)
                a.href = window.URL.createObjectURL(blob)
                a.click()
                a.remove()
            })

        },

        // 文件状态改变时触发的事件
        handleChange(info) {
        //   console.log(info)
          const status = info.file.status;
        //   console.log(status)
          if (status !== 'uploading') {
            // console.log(info.file, info.fileList);
          }
          if (status === 'done') {
            message.success(`${info.file.name} 文件上传成功！`);
          } else if (status === 'error') {
            message.error(`${info.file.name} 文件上传失败！`);
          }
        },
        // 上传前触发的事件
        beforeUpload(file, fileList) {
          
            if(this.fileList.length > 0){
                this.fileList[0] = file
            }else{
                this.fileList.push(file)
            }

          return false  //暂时不保存  必须写上，不然会请求地址栏的地址
        },
    }
}
</script>
<style scoped>
:deep(.ant-form-item-label){
    width: 85px;
}
.content{
    width: 100%;
    height: 100%;
    padding: 10px;
    background-color: #F3F3F3;
}
</style>