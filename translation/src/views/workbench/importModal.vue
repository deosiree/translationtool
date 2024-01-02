<template>
    <CustomModal
    :visible="visible" 
    :modalTitle="modalTitle"
    :modalWidth="modalWidth"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    >
        <div class="content">
            <div class="taskInfo">
                <div class="taskItem">任务名称：{{task.name}}</div>
                <div class="taskItem">产品名称：{{task.productName}}</div>
                <div class="taskItem">翻译语种：{{task.translateType}}</div>
            </div>
            <div style="width:100%">
                数据类型：
                <a-radio-group v-model:value="dataType" @change="dataTypeChange">
                    <a-radio :value="'file'">文件</a-radio>
                    <a-radio v-if="task.department === '通用平台部'" :value="'ts'">TS</a-radio>
                    <a-radio v-if="task.department === '通用平台部'" :value="'database'">数据库</a-radio>
                    <a-radio v-if="task.department === '通用平台部'" :value="'dictionary'">字典</a-radio>
                </a-radio-group>
                <a-button type="primary" size="small" class="resetBtn" style="float:right" @click="importEntryData">导入</a-button>
            </div>
            <div class="dataTypeBox" v-if="dataType === 'file'">
                词条文件：
                <a-input
                    v-model:value="filePath"
                    style="width:70%"
                    size="small"
                />
                
                <a-upload
                    name="file"
                    :beforeUpload="beforeUpload"
                    :accept="accept"
                    :showUploadList="false"
                    @change="handleChange"
                >
                    <a-button type="primary" size="small" style="margin-left:8px">选择文件</a-button>
                </a-upload>
            </div>
            <div class="dataTypeBox" v-if="dataType === 'ts'">
                {{selectTitle}}:
                <a-select
                v-model:value="tsFileValue"
                mode="multiple"
                style="width: 70%;margin-left:10px"
                placeholder="请选择"
                :options="tsOptions"
                size="small"
                >
                </a-select>
            </div>
            <div class="dataTypeBox" v-if="dataType === 'dictionary'">
                <a-radio-group v-model:value="dictionaryType" :options="dictionaryOptions">
                </a-radio-group>
            </div>
            <div class="dataTypeBox" v-if="dataType === 'database'">
                <a-form
                ref="formRef"
                name="advanced_search"
                class="ant-advanced-search-form"
                :model="dataLibrary"
                style="width:100%"
                >
                    <a-row :gutter="24">
                        <a-col :span="12">
                            <a-form-item>
                                <a-select
                                ref="select"
                                v-model:value="dataLibrary.node"
                                :options="nodeOptions"
                                placeholder="请选择节点"
                                size="small"
                                @select="nodeSelect"
                                ></a-select>
                            </a-form-item>
                        </a-col>
                        <a-col :span="12">
                            <a-form-item>
                                <a-select
                                ref="select"
                                v-model:value="dataLibrary.server"
                                :options="serverOptions"
                                placeholder="请选择应用"
                                size="small"
                                @select="appSelect"
                                ></a-select>
                            </a-form-item>
                        </a-col>
                    </a-row>
                    <a-row :gutter="24">
                        <a-col :span="12">
                            <a-form-item>
                                <a-select
                                ref="select"
                                v-model:value="dataLibrary.library"
                                :options="libraryOptions"
                                placeholder="请选择库"
                                size="small"
                                @select="librarySelect"
                                ></a-select>
                            </a-form-item>
                        </a-col>
                        <a-col :span="12">
                            <a-form-item>
                                <a-select
                                v-model:value="dataLibrary.table"
                                mode="multiple"
                                placeholder="请选择表"
                                :options="tableOptions"
                                :max-tag-count="3"
                                size="small"
                                >
                                    <template #dropdownRender="{ menuNode: menu }">
                                        <v-nodes :vnodes="menu" />
                                        <a-divider style="margin: 4px 0" />
                                        <div style="padding: 4px 8px; cursor: pointer;" @mousedown="e => e.preventDefault()">
                                            <a-button type="link" @click="selectAllTable">全选</a-button>
                                            <a-button type="link" @click="clearAllTable">清空</a-button>
                                        </div>
                                    </template>
                                </a-select>
                            </a-form-item>
                        </a-col>
                    </a-row>
                    字段/别名：
                             <a-radio-group v-model:value="dataLibrary.type" @change="dataTypeChange">
                                <a-radio :value="'field'">字段</a-radio>
                                <a-radio :value="'alias'">别名</a-radio>
                            </a-radio-group>
                </a-form>
            </div>

            <div class="form">
                词条展示：
                <a-input
                    v-model:value="keyWords"
                    style="width:70%"
                    size="small"
                    placeholder='请输入关键词搜索'
                />
                <a-button type="primary" size="small" style="margin-left:8px" @click="select">查询</a-button>
                <!-- <a-button type="primary" size="small" style="margin-left:8px" @click="deleteEntry">删除</a-button> -->
            </div>
            <a-table 
            bordered
            class="ant-table-striped"
            :columns="columns" 
            :data-source="dataSource" 
            :row-key="record => record.id"
            :scroll="tableHeight"
            :pagination='false'
            :loading="loading"
            :rowClassName="getRowClassName"
            :customRow="customRow"
            ref="workTable"
            @resizeColumn="handleResizeColumn"
            >
                <template #bodyCell="{ column, text, record }">
                    <template v-if="column.dataIndex === 'translate'">
                        <div>
                            <template v-if="editableData[record.id]">
                                <a-input
                                    v-model:value="editableData[record.id][column.dataIndex]"
                                    style="margin: -5px 0"
                                    @pressEnter="inputPressEnter(record)"
                                />
                            </template>
                            <template v-else>
                                {{ text }}
                            </template>
                        </div>
                    </template>
                    <template v-else-if="column.dataIndex === 'label'">
                        <div class="editable-row-operations">
                            <span>
                                <a-checkable-tag :checked="record.auditState === 1" :class="record.auditState === 1 ? 'passTagChecked' : 'passTag' " >通过</a-checkable-tag>
                                <a-checkable-tag :checked="record.auditState === 0" :class="record.auditState === 0 ? 'rejectTagChecked' : 'rejectTag'" >驳回</a-checkable-tag>
                            </span>
                        </div>
                    </template>
                    <template v-else-if="column.dataIndex === 'operation'">
                        <div class="editable-row-operations">
                            <span v-if="editableData[record.id]">
                                <a-tooltip placement="top">
                                    <template #title>
                                    <span>保存</span>
                                    </template>
                                    <CheckOutlined style="color:#369FFF;margin-left:8px" @click="edit(record)"/>
                                </a-tooltip>
                                <a-tooltip placement="top">
                                    <template #title>
                                    <span>取消</span>
                                    </template>
                                    <CloseOutlined style="color:red;margin-left:8px" @click="cancel(record)"/>
                                </a-tooltip>
                            </span>
                        </div>
                    </template>
                </template>
            </a-table>
        </div>
    </CustomModal>
</template>
<script>
import CustomModal from '@/components/modal/index.vue';
import { cloneDeep, iteratee } from 'lodash-es';
import { message ,Modal} from 'ant-design-vue';
import { defineComponent, ref, createVNode } from 'vue';
import {
    CheckOutlined,
    CloseOutlined,
    ExclamationCircleOutlined
} from '@ant-design/icons-vue';
import {
    getFileListByLang,
    getTsWords,
    getDictionary,
    getDictionaryEntry,
    getAllNode,
    getAppByNode,
    getdbByApp,
    getTableByApp,
    getFieldByTable
} from '@/http/api/i18Server';
import {
    insertEntry,
    getEntryTempByTaskID
} from '@/http/api/workbench'
export default {
    components:{
        CheckOutlined,
        CloseOutlined,
        CustomModal,
        ExclamationCircleOutlined,
        VNodes: (_, { attrs }) => {
            return attrs.vnodes;
        },
    },
    emits:['handleClose','handleOK'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:'词条导入'
        },
        currentTask:{
            type:Object
        }
    },
    
    data() {
        return{
            modalWidth:"40%",
            task:{},
            filePath:"",
            keyWords:"",
            dataType:'file',
            tableHeight: { x:'100%',y: '250px' },
            loading:false,
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:40,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left'},
                {title: '词条',dataIndex: 'entry',align:'center',width:100,fixed: 'left',resizable: true},
                {title: 'Abbr',dataIndex: 'abbr',align:'center',width:100,resizable: true,index:2},
                {title: '翻译',dataIndex: 'translate',align:'center',width:100,ellipsis: true,resizable: true},
                // {title: '审核',dataIndex: 'label',align:'center',width:100,ellipsis: true,},
                // {title: '说明',dataIndex: 'auditEntryFeedback',align:'center',width:100,ellipsis: true,resizable: true},
                // {title: '操作',dataIndex: 'operation',align:'center',width:50,fixed: 'right'}
            ],
            dataSource:[],
            editableData:{},
            allData:[],
            selectedRowKeys:[],
            selectedRows:[],
            accept:".xls,.xlsx",
            tsOptions:[],
            selectTitle:'',
            tsFileValue:[],
            dataLibrary:{
                node:null,
                server:null,
                library:null,
                table:[],
                type:"field"
            },
            nodeOptions:[],
            serverOptions:[],
            libraryOptions:[],
            tableOptions:[],
            dictionaryType:'',
            dictionaryOptions:[],
            file:{}

        }
    },
    
    created() {
    },
    mounted () {
        this.task = this.currentTask
    },
    watch:{
        currentTask(newval,oldval){
            this.task = newval
        }
    },
    methods: {
       
        handleOK(){
            // if(Object.keys(this.editableData).length != 0){
            //     Modal.confirm({
            //         title: '有编辑数据未保存，是否保存?',
            //         icon: createVNode(ExclamationCircleOutlined),
            //         okText: '保存',
            //         cancelText: '取消',
            //         onOk: () => {
            //             console.log("保存")
            //         },
            //         onCancel: () => {
            //             console.log("取消")
            //         }
            //     });
            // }
            this.allData.forEach(item => {
                // 默认全部通过
                item.auditState = 1
            })
            insertEntry(this.allData).then((res) => {
                message.success('数据已保存！')
                this.$emit('handleClose')
            })
        },
        handleClose(){
            this.$emit('handleClose')
        },
        getRowClassName(record, index){
            let className = null
            if(index % 2 === 1){
                className = 'table-striped'
                if(this.selectedRowIndex === index){
                    className = className + " highlighted-row"
                }
            }else{
                if(this.selectedRowIndex === index){
                    className = "highlighted-row"
                }
            }
            return className
        },
        handleResizeColumn: (w, col) => {
            col.width = w;
        },
        // 获取该任务有无已导入词条
        initTaskEntry(){
            let params = {
                taskID: this.task.id,
                pageIndex: -1,
                pageSize: -1
            }
            getEntryTempByTaskID(params).then((res) => {
                if(res.data.list.length > 0){
                    this.dataSource = res.data.list
                    this.allData = this.dataSource

                    let index = this.columns.findIndex(item => item.dataIndex === 'label')
                    if(index < 0){
                        let addColumn = [
                            {title: '审核',dataIndex: 'label',align:'center',width:100,ellipsis: true,},
                            {title: '说明',dataIndex: 'auditEntryFeedback',align:'center',width:100,ellipsis: true,resizable: true},
                        ]
                        this.columns = this.columns.concat(addColumn)
                        this.modalWidth = "50%"
                    }
                    
                }else{
                    this.modalWidth = "40%"
                    this.columns = this.columns.filter(function(item){
                        return item.dataIndex !== 'operation' && item.dataIndex !== 'auditEntryFeedback';
                    });
                }
            })
        },

        beforeUpload(file, fileList){
            // console.log("before");
            if(file.path != undefined){
                this.filePath = file.path
            }else{
                this.filePath = file.name
            }
            return false
        },
        handleChange(info){
            console.log(info)
            this.file = info.file
            // let formData = new FormData()
            // formData.append('multipartFile',info.file)
            // this.visible = true
            // this.loading = true
            // importExcle(formData).then((res) => {
            //     this.dataSource = res.data.list
            //     this.pagination.total = this.dataSource.length
            //     this.loading = false
            // })
        },
        // 模糊查询
        select(){
            this.dataSource = this.allData.filter(item => item.entry.includes(this.keyWords))
        },
        onSelectChange(selectedRowKeys,selectedRows){
            this.selectedRowKeys = selectedRowKeys
            this.selectedRows = selectedRows
        },
        // 删除
        deleteEntry(){
            if(this.selectedRowKeys.length === 0){
                return
            }
            Modal.confirm({
                title: '是否确定删除?',
                icon: createVNode(ExclamationCircleOutlined),
                okText: '确定',
                cancelText: '取消',
                onOk: () => {
                    this.selectedRowKeys.forEach(id => {
                        this.dataSource = this.dataSource.filter(item => {
                            return item.id != id
                        })
                    })
                    this.selectedRowKeys = []
                    this.selectedRows = []
                }
            });
            
        },
        // 数据类型选择事件
        dataTypeChange(){
            // this.dataSource = []
            // this.allData = []
            if(this.dataType === 'database'){
                // 数据库
                this.getAllNode()
            }else if(this.dataType === 'dictionary'){
                // 字典
                this.getDictionary()

            }else if(this.dataType === 'ts'){
                // TS
                this.selectTitle = "选择文件"
                this.getTsFiles()
            }
        },
        // 获取ts文件
        getTsFiles(){
            let params = {
                language: this.task.translateType
            }
            getFileListByLang(params).then((res) => {
                this.tsOptions = []
                res.data.list.forEach(item => {
                    let option = {
                        label: item,
                        value: item
                    }
                    this.tsOptions.push(option)
                })
            })
        },
        // 获取字典文件
        getDictionary(){
            getDictionary().then((res) => {
                this.dictionaryOptions = []
                res.data.list.forEach(item => {
                    let option = {
                        label: item,
                        value: item
                    }
                    this.dictionaryOptions.push(option)
                })
                if(this.dictionaryOptions.length > 0){
                    this.dictionaryType = this.dictionaryOptions[0].value
                }
            })
        },
        // 获取数据库节点信息
        getAllNode(){
            getAllNode().then((res) => {
                this.nodeOptions = []
                res.data.list.forEach(item => {
                    let node = {
                        label: item,
                        value: item
                    }
                    this.nodeOptions.push(node)
                })
            })
        },
        // 节点选择事件
        nodeSelect(value){
            this.dataLibrary.server = null
            this.dataLibrary.library = null
            this.dataLibrary.table = []
            // 获取应用数据
            let params = {
                nodeName: value
            }
            this.serverOptions = []
            getAppByNode(params).then((res) => {
                res.data.list.forEach(item => {
                    let app = {
                        label: item.name,
                        value: item.name,
                        type: item.type
                    }
                    this.serverOptions.push(app)
                })
            })
        },
        // 应用选择事件
        appSelect(value,option){
            this.dataLibrary.library = null
            this.dataLibrary.table = []
            // 获取库
            let params = {
                nodeName: this.dataLibrary.node,
                appName: value,
                modeType: option.type
            }
            this.libraryOptions = []
            getdbByApp(params).then((res) => {
                res.data.list.forEach(item => {
                    let library = {
                        label: item,
                        value: item
                    }
                    this.libraryOptions.push(library)
                })
            })
        },
        // 库选择事件
        librarySelect(value){
            this.dataLibrary.table = []
            // 获取表
            let params = {
                appName: this.dataLibrary.server,
                dbName: value
            }
            this.tableOptions = []
            getTableByApp(params).then((res) => {
                res.data.list.forEach(item => {
                    let table = {
                        label: item.tableName,
                        value: item.tableId
                    }
                    this.tableOptions.push(table)
                })
                
            })

        },
        // 全选表
        selectAllTable(){
            this.dataLibrary.table = []
            this.dataLibrary.table = this.tableOptions.map((item,index)=>{
                return item.value
            })
        },
        // 清空选中的表
        clearAllTable(){
            this.dataLibrary.table = []
        },
        // 导入词条数据
        importEntryData(){
            this.loading = true
            if(this.dataType === 'ts'){
                // ts文件导入
                let params = {
                    taskID: this.task.id,
                    translateType: this.task.translateType
                }
                getTsWords(params,this.tsFileValue).then((res) => {
                    this.dataSource = res.data.list
                    this.dataSource.forEach(item => {
                        item.auditState = 1
                    })
                    this.allData = this.dataSource
                    this.loading = false
                })
            }else if(this.dataType === 'dictionary'){
                // 字典文件导入
                let params = {
                    type: this.dictionaryType,
                    taskID: this.task.id,
                    versionID: this.task.versionId,
                    transType : this.task.translateType, 
                }
                getDictionaryEntry(params).then((res) => {
                    this.dataSource = res.data.list
                    this.allData = this.dataSource
                    this.loading = false
                })
            }else if(this.dataType === 'database'){
                // 数据库导入
                let params = {
                    dbName: this.dataLibrary.library,
                    appName: this.dataLibrary.server,
                    taskID: this.task.id,
                    versionID: this.task.versionId,
                    translateType: this.task.translateType, 
                    type: this.dataLibrary.type
                }
                let data = []
                this.dataLibrary.table.forEach(tableId => {
                    let tableObj = this.tableOptions.find(item => item.value === tableId)
                    let tableItem = {
                        tableName: tableObj.label,
                        tableId: tableObj.value
                    }
                    data.push(tableItem)
                })
                getFieldByTable(params,data).then((res) => {
                    this.dataSource = res.data.list
                    this.allData = this.dataSource
                    this.loading = false
                })

            }else if(this.dataType === 'file'){
                console.log(this.file)
            }
        },
         // 添加表格行点击事件
        customRow(record, index){
            return {
                onDblclick: (event) => {
                    this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
                    if(this.columns.findIndex(item => item.dataIndex === 'operation') === -1){
                        // 添加操作列
                        let operation = {title: '操作',dataIndex: 'operation',align:'center',width:50,fixed: 'right'}
                        this.columns.push(operation)
                    }
                }
            }
        },
        // 输入框 回车事件
        inputPressEnter(record){
            record.translate = this.editableData[record.id].translate
            delete this.editableData[record.id]
            this.deleteOperationColumns()
        },
        // 编辑
        edit(record){
            record.translate = this.editableData[record.id].translate
            delete this.editableData[record.id]
            this.deleteOperationColumns()
        },
        // 取消编辑
        cancel(record){
            delete this.editableData[record.id]
            this.deleteOperationColumns()
        },
        // 删除操作列
        deleteOperationColumns(){
            if(Object.keys(this.editableData).length === 0){
                this.columns = this.columns.filter(item => {
                    return item.dataIndex != 'operation'
                })
            }
        },
        afterClose(){
            this.editableData = {}
            this.keyWords = ""
            this.dataType = 'file'
            this.dataSource = []
            this.allData = []
            this.tsFileValue = []
            this.file = {}
            this.dataLibrary = {
                node:null,
                server:null,
                library:null,
                table:[],
                type:"field"
            }
        }
    }
}
</script>
<style scoped lang="less">
.ant-divider{
    margin: 15px 0;
}
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

    .taskInfo{
        display: flex;
        padding: 4px 0px;
        align-items: center;
        gap: 32px;
        align-self: stretch;

        .taskItem{
            display: flex;
            align-items: center;
            flex: 1 0 0;
        }
    }
    .dataTypeBox{
        display: flex;
        align-items: center;
        align-self: stretch;
        width: 100%;
        border-radius: 4px;
        background-color: white;
        padding: 16px;
    }
    .form{
        display: flex;
        align-items: center;
        align-self: stretch;
        width: 100%;
    }
    .ant-row{
        height: 38px;
    }

    .rejectBtn{
        background:#FBB31F;
        border-color:#FBB31F
    }
    .rejectBtn:hover{
        background:#FBB31F;
        border-color:#FBB31F
    }
    .rejectBtn:focus{
        background:#FBB31F;
        border-color:#FBB31F
    }
    .passTag{
        border: 1px solid #36BF7D;
        color: #36BF7D;
    }
    .passTagChecked{
        background-color: #36BF7D;
        color: white;
    }
    .rejectTag{
        border: 1px solid #FBB31F;
        color: #FBB31F;
    }
    .rejectTagChecked{
        background-color: #FBB31F;
        color: white;
    }
}
</style>