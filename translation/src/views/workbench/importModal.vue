<template>
    <CustomModal
    :visible="visible" 
    :modalTitle="modalTitle"
    :modalWidth="modalWidth"
    :showCancel="false"
    :okLoading="saveLoading"
    okText="保存"
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
                    <a-radio v-if="task.department === '通用平台部' || task.department === '监控系统部'" :value="'ts'">TS</a-radio>
                    <a-radio v-if="task.department === '通用平台部' || task.department === '监控系统部'" :value="'database'">实时库</a-radio>
                    <a-radio v-if="task.department === '通用平台部' || task.department === '监控系统部'" :value="'dictionary'">字典</a-radio>
                </a-radio-group>
                <!-- <a-button type="primary" size="small" class="resetBtn" style="float:right" @click="importEntryData">导入</a-button> -->
            </div>
            <div class="dataTypeBox" v-if="dataType === 'file'">
                词条文件：
                <a-input
                    v-model:value="filePath"
                    style="width:65%"
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
                <a style="font-size:12px;margin-left:10px" @click="templateFileDownload">下载模板</a>
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
                <span v-if="dictionaryOptions.length === 0" style="font-size:12px;color:rgba(0, 0, 0, 0.40);margin-left:45%">暂无数据</span>
            </div>
            <div class="dataTypeBox" v-if="dataType === 'database'" style="padding-top:0px">
                <a-tabs v-model:activeKey="dataLibrary.type" size="small" style="width:100%" @change="changeDataLibraryType">
                    <a-tab-pane key="field" tab="字段"></a-tab-pane>
                    <a-tab-pane key="alias" tab="元数据"></a-tab-pane>
                </a-tabs>
                <a-form
                ref="formRef"
                name="advanced_search"
                class="ant-advanced-search-form"
                :model="dataLibrary"
                style="width:100%"
                 v-if="dataLibrary.type === 'field'"
                >
                    <a-row :gutter="24">
                        <a-col :span="12">
                            <a-form-item>
                                <a-tree-select
                                    v-model:value="dataLibrary.table"
                                    tree-data-simple-mode
                                    style="width: 100%"
                                    :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
                                    :tree-data="treeData"
                                    placeholder="请选择表"
                                    :load-data="onLoadData"
                                    size="small"
                                    @select="treeSelect"
                                />
                            </a-form-item>
                        </a-col>
                        <a-col :span="12">
                            <a-form-item>
                                <a-select
                                v-model:value="dataLibrary.field"
                                mode="multiple"
                                placeholder="请选择字段"
                                :options="fieldOptions"
                                :max-tag-count="3"
                                size="small"
                                >
                                    <template #dropdownRender="{ menuNode: menu }">
                                        <v-nodes :vnodes="menu" />
                                        <a-divider style="margin: 4px 0" />
                                        <div style="padding: 4px 8px; cursor: pointer;" @mousedown="e => e.preventDefault()">
                                            <a-button type="link" @click="selectAllField">全选</a-button>
                                            <a-button type="link" @click="clearAllField">清空</a-button>
                                        </div>
                                    </template>
                                </a-select>
                            </a-form-item>
                        </a-col>
                    </a-row>
                </a-form>
                <a-form
                ref="formRef"
                name="advanced_search"
                class="ant-advanced-search-form"
                :model="dataLibrary"
                style="width:100%"
                v-else
                >
                    <a-row :gutter="24">
                        <a-col :span="12">
                            <a-form-item>
                                <a-tree-select
                                    v-model:value="dataLibrary.table"
                                    tree-data-simple-mode
                                    style="width: 100%"
                                    :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
                                    :tree-data="treeData"
                                    placeholder="请选择库"
                                    :load-data="onLoadData"
                                    size="small"
                                    @select="treeSelect"
                                />
                            </a-form-item>
                        </a-col>
                    </a-row>
                </a-form>
            </div>

            <div class="form">
                词条展示：
                <a-input
                    v-model:value="keyWords"
                    style="width:40%"
                    size="small"
                    placeholder='请输入关键词搜索'
                />
                <a-button type="primary" size="small" style="margin-left:8px" @click="importEntryData">查询</a-button>
                <!-- <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="aggregation">聚合</a-button>
                <a-button type="primary" size="small" style="margin-left:8px" class="yellowBtn" @click="cancelAggregation">取消聚合</a-button> -->
                <a-button type="primary" danger size="small" style="margin-left:8px" @click="deleteEntry">删除</a-button>
                <!-- <a-button type="primary" size="small" class="resetBtn" style="margin-left:8px" @click="insertEntry" :loading="saveLoading">保存</a-button> -->
                
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
            :expandIconColumnIndex="2"
            :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
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
                <template #expandIcon="props">
                    <span v-if="props.record.children != null && props.record.children.length > 0">
                        <div
                            v-if="props.expanded"
                            style="display: inline-block; margin-right: 10px"
                            @click="(e) => {props.onExpand(props.record, e);}"
                        >
                            <CaretDownOutlined />
                        </div>
                        <div
                            v-else
                            style="display: inline-block; margin-right: 10px"
                            @click="(e) => {props.onExpand(props.record, e);}"
                        >
                            <CaretRightOutlined />
                        </div>
                    </span>
                    <span v-else style="margin-right:23px"></span>
                </template>
            </a-table>
        </div>
        <template v-slot:leftBottomBtn>
            <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="aggregation">聚合</a-button>
            <a-button type="primary" size="small" style="margin-left:8px" class="yellowBtn" @click="cancelAggregation">取消聚合</a-button>
        </template>
    </CustomModal>
</template>
<script>
import CustomModal from '@/components/modal/index.vue';
import { cloneDeep, iteratee } from 'lodash-es';
import { message ,Modal} from 'ant-design-vue';
import { defineComponent, ref, createVNode } from 'vue';
import { v4 as uuidv4 } from 'uuid';
import {
    CheckOutlined,
    CloseOutlined,
    ExclamationCircleOutlined,
    CaretDownOutlined,
    CaretRightOutlined
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
    getFieldByTable,
    getFieldData,
    getAlias
} from '@/http/api/i18Server';
import {
    insertEntry,
    getEntryTempByTaskID,
    importExcle,
    deleteEntryTempByID
} from '@/http/api/workbench'
import{
    templateFileDownload
} from '@/http/api/download'
export default {
    components:{
        CheckOutlined,
        CloseOutlined,
        CustomModal,
        ExclamationCircleOutlined,
        CaretDownOutlined,
        CaretRightOutlined,
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
                {title: "序号",dataIndex: 'index',width:70,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left'},
                {title: '词条',dataIndex: 'entry',width:150,fixed: 'left',resizable: true},
                // {title: 'Abbr',dataIndex: 'abbr',align:'center',width:100,resizable: true,index:2},
                {title: '来源',dataIndex: 'source',align:'center',width:150,resizable: true,ellipsis:true},
                {title: '翻译',dataIndex: 'translate',align:'center',width:150,ellipsis: true,resizable: true},
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
                type:"field",
                table:null,
                field:[]
            },
            nodeOptions:[],
            serverOptions:[],
            libraryOptions:[],
            tableOptions:[],
            fieldOptions:[],
            dictionaryType:'',
            dictionaryOptions:[],
            file:{},
            treeData:[],
            saveLoading: false
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
            // this.allData.forEach(item => {
            //     // 默认全部通过
            //     item.auditState = 1
            // })
            // insertEntry(this.allData).then((res) => {
            //     message.success('数据已保存！')
            //     this.$emit('handleClose')
            // })
            this.insertEntry()
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
        insertEntry(){
            for (let key in this.editableData) {
				let entry = this.dataSource.find(item => item.id === key)
                entry.translate = this.editableData[key].translate
			}
            this.editableData = []

            this.allData.forEach(item => {
                // 默认全部通过
                item.auditState = 1
            })
            if(this.allData.length === 0){
                return
            }
            this.saveLoading = true
            insertEntry(this.allData).then((res) => {
                message.success('数据已保存！')
                this.saveLoading = false
            }).catch((err) => {
                message.success('数据已存在！')
                this.saveLoading = false
            })
        },
        // 获取该任务有无已导入词条
        initTaskEntry(){
            let params = {
                taskID: this.task.id,
                pageIndex: -1,
                pageSize: -1
            }
            this.loading = true
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
                        return item.dataIndex !== 'operation' && item.dataIndex !== 'auditEntryFeedback' && item.dataIndex !== 'label';
                    });
                }
                this.loading = false
            }).catch((err) => {
                this.loading = false
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
            // console.log(info)
            this.file = info.file
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
                    this.allData = this.dataSource
                    deleteEntryTempByID(this.selectedRowKeys).then((res) => {

                    })
                    message.success('已删除！')
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
                this.dataLibrary.table = null
                this.dataLibrary.field = []
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
                this.treeData = []
                res.data.list.forEach(item => {
                    let node = {
                        id: uuidv4(),
                        pId: 0,
                        value: item,
                        title: item,
                        isLeaf: false,
                        type: 'node',
                        key: item
                    }
                    this.treeData.push(node)
                })
            })
        },
        // treeData加载子数据
        onLoadData(node){
            const id = node.dataRef.id;
            const type = node.dataRef.type;
            return new Promise(resolve => {
                if(type === 'node'){
                    // 获取应用
                    let params = {
                        nodeName: node.dataRef.value
                    }
                    getAppByNode(params).then((res) => {
                        res.data.list.forEach(item => {
                            const newId = uuidv4();
                            let app = {
                                id: newId,
                                pId: id,
                                value: newId,
                                key: newId,
                                title: item.name,
                                type: 'app',
                                appId: item.type,
                                node: node.value,
                                isLeaf: false
                            }
                            this.treeData.push(app)
                        })
                        
                    })
                }else if(type === 'app'){
                    // 获取库
                    // console.log(node.dataRef)
                    let params = {
                        nodeName: node.dataRef.node,
                        appName: node.dataRef.title,
                        modeType: node.dataRef.appId
                    }
                    getdbByApp(params).then((res) => {
                        res.data.list.forEach(item => {
                            const newId = uuidv4();
                            let temp = {
                                id: newId,
                                pId: id,
                                value: newId,
                                key: newId,
                                title: item,
                                type: 'db',
                                node: node.dataRef.node,
                                app: node.dataRef.title,
                                isLeaf: this.dataLibrary.type === 'field' ? false : true 
                            }
                            this.treeData.push(temp)
                        })
                    })
                }else if(type === 'db'){
                    // 获取表
                    let params = {
                        nodeName: node.dataRef.node,
                        appName: node.dataRef.app,
                        dbName: node.dataRef.title
                    }
                    getTableByApp(params).then((res) => {
                        res.data.list.forEach(item => {
                            const newId = uuidv4();
                            let table = {
                                id: newId,
                                pId: id,
                                value: newId,
                                key: newId,
                                tableId: item.tableId,
                                title: item.tableName,
                                type: 'table',
                                node: node.dataRef.node,
                                app: node.dataRef.app,
                                db: node.dataRef.title,
                                isLeaf: true
                            }
                            this.treeData.push(table)
                        })
                        
                    })
                }
               resolve(true);
            })
            
        },
        // 树选择事件
        treeSelect(value, node, extra){
            // console.log(node)
            if(node.isLeaf){
                this.dataLibrary.table = value

                if(this.dataLibrary.type === 'field'){
                    let params = {
                        dbName: node.db,
                        nodeName: node.node,
                        appName: node.app,
                        tbName: node.title
                    }
                    this.fieldOptions = []
                    this.dataLibrary.field = []
                    getFieldByTable(params).then((res) => {
                        res.data.list.forEach(item => {
                            let table = {
                                label: item.fieldName,
                                value: item.fieldID,
                                size: item.size
                            }
                            this.fieldOptions.push(table)
                        })
                    })
                }
            }else{
                this.dataLibrary.table = null
            }
        },
        // tabs切换
        changeDataLibraryType(activeKey){
            this.treeData = []
            this.dataLibrary.table = null
            this.dataLibrary.field = []
            this.getAllNode()
        },
        // 全选字段
        selectAllField(){
            this.dataLibrary.field = []
            this.dataLibrary.field = this.fieldOptions.map((item,index)=>{
                return item.value
            })
        },
        // 清空选中的表
        clearAllField(){
            this.dataLibrary.field = []
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
                    this.select()
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
                    this.select()
                })
            }else if(this.dataType === 'database'){
                // 数据库导入
                if(this.dataLibrary.table === null){
                    return
                }
                if(this.dataLibrary.type === 'field'){
                    this.getFieldData()
                }else if(this.dataLibrary.type === 'alias'){
                    this.getAlias()
                }
                
            }else if(this.dataType === 'file'){
                // console.log(this.file)
                // 文件导入
                let formData = new FormData()
                formData.append('file',this.file)
                formData.append('taskID',this.task.id)
                this.loading = true
                importExcle(formData).then((res) => {
                    this.dataSource = res.data.list
                    this.allData = this.dataSource
                    this.loading = false
                    this.select()
                })
            }
            
        },
        // 获取字段内容
        getFieldData(){
            let table = this.treeData.find(item => item.id === this.dataLibrary.table)
            let params = {
                dbName: table.db,
                appName: table.app,
                nodeName: table.node,
                tbID: table.tableId,
                tbName: table.title,
                taskID: this.task.id,
                versionID: this.task.versionId,
                translateType: this.task.translateType
            }
            let data = []
            this.dataLibrary.field.forEach(fieldId => {
                let fieldObj = this.fieldOptions.find(item => item.value === fieldId)
                let field = {
                    fieldName: fieldObj.label,
                    fieldID: fieldObj.value,
                    size: fieldObj.size
                }
                data.push(field)
            })
            getFieldData(params,data).then((res) => {
                this.dataSource = res.data.list
                this.allData = this.dataSource
                this.loading = false
                this.select()
            })
        },
        // 获取别名
        getAlias(){
            let table = this.treeData.find(item => item.id === this.dataLibrary.table)
            let params = {
                dbName: table.title,
                appName: table.app,
                nodeName: table.node,
                taskID: this.task.id,
                versionID: this.task.versionId,
                translateType: this.task.translateType
            }
            getAlias(params).then((res) => {
                this.dataSource = res.data.list
                this.allData = this.dataSource
                this.loading = false
                this.select()
            })
        },
         // 添加表格行点击事件
        customRow(record, index){
            return {
                onDblclick: (event) => {
                    this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
                    // if(this.columns.findIndex(item => item.dataIndex === 'operation') === -1){
                    //     // 添加操作列
                    //     let operation = {title: '操作',dataIndex: 'operation',align:'center',width:50,fixed: 'right'}
                    //     this.columns.push(operation)
                    // }
                }
            }
        },
        // 输入框 回车事件
        inputPressEnter(record){
            record.translate = this.editableData[record.id].translate
            delete this.editableData[record.id]
            // this.deleteOperationColumns()
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
        // 聚合
        aggregation(){
            if(this.selectedRows.length < 2){
                message.warn("请选择两条及以上词条聚合！")
            }
            let children = []
            for(let i = 1; i < this.selectedRows.length; i++){
                let child = this.selectedRows[i]
                if(child.children && child.children.length > 0){
                    child.children.forEach(item => {
                        children.push(item)
                    })
                }
                child.children = []
                children.push(child)
            }
            children.forEach(item => {
                item.parentID = this.selectedRows[0].id

                this.dataSource = this.dataSource.filter(data => data.id != item.id)
            })
            if(this.selectedRows[0].children){
                this.selectedRows[0].children = this.selectedRows[0].children.concat(children)
            }else{
                this.selectedRows[0].children = children
            }
            
            this.allData = this.dataSource
            this.selectedRowKeys = []
            this.selectedRows = []
        },
        // 取消聚合
        cancelAggregation(){
            // console.log(this.selectedRows)
            this.selectedRows.forEach(item => {
                if((item.parentID === '' || item.parentID === null) && item.children){
                    let index = this.dataSource.findIndex(entry => entry.id === item.id)
                    for(let i = 0; i < item.children.length; i++){
                        let child = item.children[i]
                        child.parentID = ""
                        this.dataSource.splice(index + i + 1,0,child)
                    }
                    item.children = []
                }else{
                    let parent = this.dataSource.find(data => data.id === item.parentID)
                    parent.children = parent.children.filter(child => child.id != item.id)
                    let index = this.dataSource.findIndex(data => data.id === item.parentID)
                    item.parentID = ""
                    this.dataSource.splice(index + 1, 0 , item)
                }
            })
            this.allData = this.dataSource
            this.selectedRowKeys = []
            this.selectedRows = []
        },
        afterClose(){
            this.editableData = {}
            this.keyWords = ""
            this.dataType = 'file'
            this.dataSource = []
            this.allData = []
            this.tsFileValue = []
            this.file = {}
            this.filePath = ""
            this.dataLibrary = {
                node:null,
                server:null,
                library:null,
                table:[],
                type:"field"
            }
        },
        // 模板下载
        templateFileDownload(){
            templateFileDownload().then((res) => {
                let fileName = res.headers["content-disposition"].split(";")[1].split("filename=")[1]
                let contentType = res.headers['content-type']
                const blob = new Blob([res.data], {type: contentType})
                const a = document.createElement('a')
                a.download = decodeURI(fileName)
                a.href = window.URL.createObjectURL(blob)
                a.click()
                a.remove()
            })
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
        // display: flex;
        // align-items: center;
        // align-self: stretch;
        width: 100%;
        border-radius: 4px;
        background-color: white;
        padding: 16px;

        :deep(.ant-tabs-nav){
            margin-bottom: 10px;
        }
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