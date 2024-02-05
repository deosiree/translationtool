<template>
    <Modal
    :visible="visible" 
    :modalTitle="modalTitle"
    :modalWidth="modalWidth"
    :okLoading="exportLoading"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    >
        <div class="content">
            导出文件：
            <a-checkbox-group v-model:value="type" style="width: 100%">
                <template v-for="(value, key) in typeSelect"  :key="key">
                    <a-row>
                        <a-col :span="4">
                            <a-checkbox :value="value.type" :disabled="value.disabled">{{ value.type }}:  </a-checkbox>
                        </a-col>
                        <a-col :span="20" style="color:rgba(0, 0, 0, 0.25)">
                            <span>{{ value.file }}</span>
                        </a-col>
                    </a-row>
                </template>
                <!-- <a-row>
                    <a-col>
                        <a-checkbox v-model:checked="checkedExcel" @change="changeExcel">excel文件</a-checkbox>
                    </a-col>
                </a-row> -->
            </a-checkbox-group>
        </div>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import { cloneDeep, iteratee } from 'lodash-es';
import { message } from 'ant-design-vue';
import {
    getImportType
} from '@/http/api/workbench'
import {
    setInfo
} from '@/http/api/i18Server'
import {
    taskDownload
} from '@/http/api/download'
import {
    updateTaskInfo
} from '@/http/api/task'
export default {
    components:{
        Modal
    },
    emits:['handleClose','handleOK'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:'文件导出'
        },
        currentTask:{
            type:Object
        }
    },
    
    data() {
        return{
            modalWidth:"430px",
            task:{},
            type:[],
            typeSelect:[],
            checkedExcel: false,
            exportLoading: false
        }
    },
    
    created() {
    },
    mounted () {
        this.task = this.currentTask
        // this.getImportType()
    },
    watch:{
        currentTask(newval,oldval){
            this.task = newval
            // this.getImportType()
        },
    },
    methods: {
        getImportType(){
            if(Object.keys(this.task).length === 0){
                return
            }
            let params = {
                taskID: this.task.id
            }
            getImportType(params).then((res) => {
                this.typeSelect = []
                Object.keys(res.data).forEach(key => {
                    let select = {
                        type: key,
                        file: res.data[key],
                        disabled: true
                    }
                    this.type.push(key)
                    this.typeSelect.push(select)
                })
            })
        },
        changeExcel(){
            this.checkedExcel = !this.checkedExcel
        },
        handleOK(){
            // console.log(this.checkedExcel)
            let params = {
                taskID: this.task.id
            }
            this.exportLoading = true
            setInfo(params).then((res) => {
                
                if(this.type.includes('excel')){
                    this.exportExcel()
                }else{
                    message.success('导出完成！')
                    this.$emit('handleClose')
                }
                this.exportLoading = false
                this.taskEnd()
            }).catch((err) => {
                this.exportLoading = false
            })
        },
        // 导出excel
        exportExcel(){
            let params = {
                taskID: this.task.id,
                importType: 'excel'
            }
            taskDownload(params).then((res) => {
                let fileName = res.headers["content-disposition"].split(";")[1].split("filename=")[1]
                let contentType = res.headers['content-type']
                const blob = new Blob([res.data], {type: contentType})
                const a = document.createElement('a')
                a.download = decodeURI(fileName)
                a.href = window.URL.createObjectURL(blob)
                a.click()
                a.remove()
                window.URL.revokeObjectURL(a.href);
                this.$emit('handleClose')
            })
        },
        // 任务结束
        taskEnd(){
            this.task.state = '6'
            this.task.endTime = new Date().toLocaleString().replaceAll('/','-')
            updateTaskInfo(this.task).then((res) => {

            })
        },
        handleClose(){
            this.$emit('handleClose')
        },
        afterClose(){
            this.checkedExcel = false
        }
    }
}
</script>
<style scoped lang="less">
.content{
    width: 100%;
    height: 100%;
    padding: 10px;
    background-color: #F3F3F3;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    align-self: stretch;
    
    :deep(.ant-row){
        background-color: white;
        border: 1px solid #F3F3F3;
        padding: 5px;
    }
}
</style>