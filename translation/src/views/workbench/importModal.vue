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
                    <a-radio :value="'ts'">TS</a-radio>
                    <a-radio :value="'database'">实时库</a-radio>
                    <a-radio :value="'dictionary'">字典</a-radio>
                    <a-radio :value="'config'">配置文件</a-radio>
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
                :max-tag-count="4"
                style="width: 70%;margin-left:10px"
                placeholder="请选择"
                size="small"
                :options="tsOptions"
                >
                    <template #dropdownRender="{ menuNode: menu }">
                        <v-nodes :vnodes="menu" />
                        <a-divider style="margin: 4px 0" />
                        <div style="padding: 4px 8px; cursor: pointer;" @mousedown="e => e.preventDefault()">
                            <a-button type="link" @click="selectAllTs">全选</a-button>
                            <a-button type="link" @click="clearAllTs">清空</a-button>
                        </div>
                    </template>
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
                    <a-tab-pane key="allData" tab="全量"></a-tab-pane>
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
                        <a-col :span="8">
                            <a-form-item>
                                <a-tree-select
                                    v-model:value="dataLibrary.table"
                                    tree-data-simple-mode
                                    style="width: 100%"
                                    :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
                                    :tree-data="treeData"
                                    placeholder="请选择表"
                                    :load-data="onLoadData"
                                    :show-checked-strategy="SHOW_PARENT"
                                    @select="treeSelect"
                                    size="small"
                                />
                            </a-form-item>
                        </a-col>
                        <a-col :span="8">
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
                        <a-col :span="8">
                            <a-form-item>
                                <a-select
                                v-model:value="dataLibrary.diFileName"
                                placeholder="请选择翻译数据回写字典目录"
                                :options="dictionaryOptions"
                                style="width:90%"
                                size="small"
                                >
                                </a-select>
                                <a-tooltip placement="top">
                                    <template #title>
                                    <span>添加字典</span>
                                    </template>
                                    <PlusSquareOutlined @click="createDictionary" style="color:#369FFF;margin-left:8px"/>
                                </a-tooltip>
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
                v-if="dataLibrary.type === 'alias'"
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
                                    @select="treeSelect"
                                    size="small"
                                />
                            </a-form-item>
                        </a-col>
                        <a-col :span="12">
                            <a-form-item>
                                <a-select
                                v-model:value="dataLibrary.diFileName"
                                placeholder="请选择翻译数据回写字典目录"
                                :options="dictionaryOptions"
                                style="width:90%"
                                size="small"
                                >
                                </a-select>
                                <a-tooltip placement="top">
                                    <template #title>
                                    <span>添加字典</span>
                                    </template>
                                    <PlusSquareOutlined @click="createDictionary" style="color:#369FFF;margin-left:8px"/>
                                </a-tooltip>
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
                v-if="dataLibrary.type === 'allData'"
                >
                    <a-row :gutter="24">
                        <a-col :span="12">
                            <a-form-item>
                                <a-tree-select
                                    v-model:value="dataLibrary.tables"
                                    tree-data-simple-mode
                                    style="width: 100%"
                                    :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
                                    :tree-data="treeData"
                                    placeholder="请选择"
                                    :load-data="onLoadData"
                                    :maxTagCount="3"
                                    tree-checkable
                                    :show-checked-strategy="SHOW_PARENT"
                                    @select="treeBatchSelect"
                                    size="small"
                                />
                            </a-form-item>
                        </a-col>
                        <a-col :span="12">
                            <a-form-item>
                                <a-select
                                v-model:value="dataLibrary.diFileName"
                                placeholder="请选择翻译数据回写字典目录"
                                :options="dictionaryOptions"
                                style="width:90%"
                                size="small"
                                >
                                </a-select>
                                <a-tooltip placement="top">
                                    <template #title>
                                    <span>添加字典</span>
                                    </template>
                                    <PlusSquareOutlined @click="createDictionary" style="color:#369FFF;margin-left:8px"/>
                                </a-tooltip>
                            </a-form-item>
                        </a-col>
                    </a-row>
                </a-form>
            </div>
            <div class="dataTypeBox" v-if="dataType === 'config'">
                <a-form
                ref="formRef"
                name="advanced_search"
                class="ant-advanced-search-form"
                :model="configFile"
                style="width:100%"
                >
                    <a-form-item
                    label="回写字典目录"
                    name="dict"
                    >
                        <a-select
                        v-model:value="configFile.dict"
                        placeholder="请选择翻译数据回写字典目录"
                        :options="dictionaryOptions"
                        style="width:50%"
                        size="small"
                        >
                        </a-select>
                        <a-tooltip placement="top">
                            <template #title>
                            <span>添加字典</span>
                            </template>
                            <PlusSquareOutlined @click="createDictionary" style="color:#369FFF;margin-left:8px"/>
                        </a-tooltip>
                    </a-form-item>
                </a-form>
            </div>
            <div class="form">
                词条：
                <a-input
                    v-model:value="keyWords"
                    style="width:40%"
                    size="small"
                    placeholder='请输入词条搜索'
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
                    <template v-if="['entry','english','russian','spanish','french'].includes(column.dataIndex)">
                        <div>
                            <template v-if="editableData[record.id]">
                                <a-form :model="editableData[record.id]" :rules="rules[record.id]" :ref="'form'+record.id.replaceAll('-','')+column.dataIndex" autocomplete="off">
                                    <a-form-item :name="column.dataIndex"
                                    >
                                        <a-input
                                            v-model:value="editableData[record.id][column.dataIndex]"
                                            style="margin: -5px 0"
                                            @pressEnter="inputPressEnter(record)"
                                        />
                                    </a-form-item>
                                </a-form>
                            </template>
                            <template v-else>
                                {{ text }}
                            </template>
                        </div>
                    </template>
                    <template v-if="['chineseInterpretation','englishInterpretation','entryLabel'].includes(column.dataIndex)">
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
                    <template v-if="column.dataIndex === 'entryState'">
                        <template v-if="record.entryState === 0">
                            <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
                        </template>
                        <template v-if="record.entryState === 1">
                            <a-badge color="#FBB31F" /><span style="color:#FBB31F">待审核</span>
                        </template>
                        <template v-if="record.entryState === 2">
                            <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
                        </template>
                        <template v-if="record.entryState === 3">
                            <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
                        </template>
                    </template>
                    <template v-if="column.dataIndex === 'isExist'">
                        <template v-if="record.isExist === 0">
                            <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
                        </template>
                        <template v-if="record.isExist === 1">
                            <a-badge color="#FBB31F" /><span style="color:#FBB31F">已存在</span>
                        </template>
                    </template>
                    <!-- <template v-else-if="column.dataIndex === 'label'">
                        <div class="editable-row-operations">
                            <span>
                                <a-checkable-tag :checked="record.auditState === 1" :class="record.auditState === 1 ? 'passTagChecked' : 'passTag' " >通过</a-checkable-tag>
                                <a-checkable-tag :checked="record.auditState === 0" :class="record.auditState === 0 ? 'rejectTagChecked' : 'rejectTag'" >驳回</a-checkable-tag>
                            </span>
                        </div>
                    </template> -->
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
            <a-button type="primary" size="small" style="margin-left:8px;float:left" class="resetBtn" @click="aggregation">聚合</a-button>
            <a-button type="primary" size="small" style="margin-left:8px;float:left" class="yellowBtn" @click="cancelAggregation">取消聚合</a-button>
        </template>
    </CustomModal>
    <CustomModal
    :visible="createDictVisible" 
    modalTitle="新增字典"
    @handleOK="createDictOk"
    @handleClose="createDictClose"
    style="top:30%"
    >
        <div class="condent">
            <a-form
            ref="dictRef"
            name="advanced_search"
            class="ant-advanced-search-form"
            :model="createDict"
            style="width:100%"
            >
                <a-form-item
                label="字典名称"
                name="name"
                :rules="[{ required: true, message: '请输入字典名称!' }]"
                >
                    <a-input
                        v-model:value="createDict.name"
                        placeholder='请输入字典名称'
                    />
                </a-form-item>
            </a-form>
        </div>
    </CustomModal>
</template>
<script>
import CustomModal from '@/components/modal/index.vue';
import { add, cloneDeep, iteratee } from 'lodash-es';
import { message ,Modal} from 'ant-design-vue';
import { defineComponent, ref, createVNode } from 'vue';
import { v4 as uuidv4 } from 'uuid';
import { TreeSelect } from 'ant-design-vue';
import {
    CheckOutlined,
    CloseOutlined,
    ExclamationCircleOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    PlusSquareOutlined
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
    getAlias,
    getConfigEntry,
    createDic,
    getDBALLEntryByApp,
    getDBALLEntryByNode,
    getDBALLEntryByDB
} from '@/http/api/i18Server';
import {
    insertEntry,
    getEntryTempByTaskID,
    importExcle,
    readZZExcle,
    deleteEntryTempByID,
    getEntryInfoList,
    updateEntryList,
    deleteEntryInfoByID
} from '@/http/api/workbench'
import{
    templateFileDownload
} from '@/http/api/download'
import workbenchCommon from '@/views/workbench/common.js';
import common from '../entry/common';
export default {
    components:{
        CheckOutlined,
        CloseOutlined,
        CustomModal,
        ExclamationCircleOutlined,
        CaretDownOutlined,
        CaretRightOutlined,
        PlusSquareOutlined,
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
        },
        classifyLimit:{
            type: Object
        }
    },
    
    data() {
        return{
            modalWidth:"70%",
            task:{},
            filePath:"",
            keyWords:"",
            dataType:'file',
            tableHeight: { x:'100%',y: '350px' },
            loading:false,
            columns: [
                {title: "序号",dataIndex: 'index',width:90,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left'},
                {title: '存在状态',dataIndex: 'isExist',align:'center',width:100,fixed: 'left'},
                {title: 'Abbr',dataIndex: 'abbr',align:'center',fixed: 'left',width:150,resizable: true},
                {title: '词条',dataIndex: 'entry',width:200,resizable: true},
                {title: '翻译',dataIndex: 'translate',align:'center',width:200,resizable: true},
                {title: '中文释义',dataIndex: 'chineseInterpretation',align:'center',width:200,resizable: true},
                {title: '英文释义',dataIndex: 'englishInterpretation',align:'center',width:200,resizable: true},
                {title: 'TAG',dataIndex: 'entryLabel',align:'center',width:200},
                {title: '审核意见',dataIndex: 'auditSuggess',align:'center',width:150,resizable: true},
                // {title: '来源',dataIndex: 'entrySource',align:'center',width:150,resizable: true},
                {title: '词条状态',dataIndex: 'entryState',align:'center',width:100,fixed: 'right'},
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
                tables:[],
                field:[],
                diFileName: null,
                selectNode:[]
            },
            configFile:{
                dict:null,
                dictOptions:[]
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
            saveLoading: false,
            rules:{},
            // classifyLimit:{
            //     间隔: 10
            // },
            SHOW_PARENT: TreeSelect.SHOW_PARENT,
            createDictVisible: false,
            createDict:{
                name:""
            }
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
            this.setTranslateColumn()
        }
    },
    methods: {
        // 设置翻译列展示的语言
        setTranslateColumn(){
            this.columns.forEach(item => {
                if(item.title === '翻译'){
                    item.dataIndex = workbenchCommon.languageMap[this.task.translateType].code
                }
            })
        },
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
            // 校验字段
            let languageCode = workbenchCommon.languageMap[this.task.translateType].code

            let checkList = []
            for (let key in this.editableData) {
                let list = [eval("this.$refs.form"+ this.editableData[key].id.replaceAll('-','') + 'entry').validate(),
                        eval("this.$refs.form"+ this.editableData[key].id.replaceAll('-','') + languageCode).validate()]
                checkList = checkList.concat(list)
            }
            Promise.all(checkList).then(() => {
                // 校验成功 保存
                this.saveEntrys()
            }).catch((err) => {
                message.error('词条校验失败！')
            })
            
        },
        // 保存词条
        saveEntrys(){
            let languageCode = workbenchCommon.languageMap[this.task.translateType].code
            for (let key in this.editableData) {
				let entry = this.dataSource.find(item => item.id === key)
                // entry = this.editableData[key]
                entry.entry = this.editableData[key].entry
                entry[languageCode] = this.editableData[key][languageCode]

                entry.chineseInterpretation = this.editableData[key].chineseInterpretation
                entry.englishInterpretation = this.editableData[key].englishInterpretation
                entry.entryLabel = this.editableData[key].entryLabel

                if(entry[languageCode] != null && entry[languageCode] != null){
                    // 翻译存在  则状态为待审核状态
                    entry[languageCode+"TranslateState"] = '1'
                }
			}
            this.editableData = {}

            if(this.allData.length === 0){
                return
            }
            this.saveLoading = true

            let addArr = []
            let updateArr = []

            let notInterpretation = []
            this.allData.forEach(item => {
                
                if((item.englishInterpretation === null || item.englishInterpretation === '')
                && (item.chineseInterpretation === null || item.chineseInterpretation === '')){
                    notInterpretation.push(item)
                }

                if(item.entryState === 2){
                    item.entryState = 1
                    updateArr.push(item)
                }else if(item.entryState === 1){
                    addArr.push(item)
                }
            })

            if(notInterpretation.length > 0){
                Modal.confirm({
                    title: '含有中文释义和中文释义都不存在的词条，是否继续保存?',
                    icon: createVNode(ExclamationCircleOutlined),
                    content: '',
                    okText: '是',
                    cancelText: '否',
                    style:{top:'30%'},
                    onOk: () => {
                        this.insertOrUpdateEntrys(addArr,updateArr)
                    },
                    onCancel: () => {
                        this.saveLoading = false
                    }
                });
            }else{
                this.insertOrUpdateEntrys(addArr,updateArr)
            }

            // let params = {
            //     taskID: this.task.id
            // }
            // if(addArr.length > 0){
            //     // 新增
            //     insertEntry(params,addArr).then((res) => {
            //         message.success('数据已保存！')
            //         this.saveLoading = false
            //         this.afterClose()
            //     }).catch((err) => {
            //         this.saveLoading = false
            //     })
            // }
            // if(updateArr.length > 0){
            //     // 编辑
            //     updateEntryList(params,updateArr).then((res) => {
            //         message.success('数据已保存！')
            //         this.saveLoading = false
            //         this.afterClose()
            //     }).catch((err) => {
            //         this.saveLoading = false
            //     })
            // }
        },
        // 
        insertOrUpdateEntrys(addArr,updateArr){
            this.loading = true
            let params = {
                taskID: this.task.id
            }
            if(addArr.length > 0){
                // 新增
                insertEntry(params,addArr).then((res) => {
                    message.success('数据已保存！')
                    this.saveLoading = false
                    this.afterClose()
                    this.loading = false
                }).catch((err) => {
                    this.saveLoading = false
                    this.loading = false
                })
            }
            if(updateArr.length > 0){
                // 编辑
                updateEntryList(params,updateArr).then((res) => {
                    message.success('数据已保存！')
                    this.saveLoading = false
                    this.afterClose()
                }).catch((err) => {
                    this.saveLoading = false
                })
            }
        },

        // 获取该任务有无审核未通过的词条
        initTaskEntry(){
            let params = {
                taskID: this.task.id,
                entryState: '2',
                entry: this.keyWords
            }
            this.loading = true
            getEntryInfoList(params,[]).then((res) => {
                if(res.data.list.length > 0){
                    this.dataSource = res.data.list
                    this.allData = this.dataSource
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
                style:{top:'30%'},
                onOk: () => {
                    this.selectedRowKeys.forEach(id => {
                        this.dataSource = this.dataSource.filter(item => {
                            return item.id != id
                        })
                    })
                    this.allData = this.dataSource
                    let deleteID = []
                    this.selectedRows.forEach(item => {
                        if(item.entryState === 2){// 词条审核未通过
                            deleteID.push(item.id)
                        }
                    })
                    if(deleteID.length > 0){
                        deleteEntryInfoByID(deleteID).then((res) => {

                        })
                    }
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
                this.dataLibrary.tables = []
                this.dataLibrary.diFileName = null
                this.getAllNode()
                this.getDictionary()
            }else if(this.dataType === 'dictionary'){
                // 字典
                this.getDictionary()

            }else if(this.dataType === 'ts'){
                // TS
                this.selectTitle = "选择文件"
                this.getTsFiles()
            }else if(this.dataType === 'config'){
                this.configFile.dict = null
                this.getDictionary()
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
            // console.log("onLoad",node)
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
                                appId: node.dataRef.appId,
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
            // console.log("treeSelect",node)
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
        // 树批量选择事件
        treeBatchSelect(value, node, extra){
            // console.log(extra)
            // console.log(node)
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
         // 全选ts文件
        selectAllTs(){
            this.tsFileValue = []
            this.tsFileValue = this.tsOptions.map((item,index)=>{
                return item.value
            })
        },
        // 清空选中的ts文件
        clearAllTs(){
            this.tsFileValue = []
        },
        // 导入词条数据
        importEntryData(){
            this.loading = true
            if(this.dataType === 'ts'){
                // ts文件导入
                if(this.tsFileValue.length === 0){
                    this.loading = false
                    return
                }
                let params = {
                    taskID: this.task.id,
                    translateType: this.task.translateType
                }
                getTsWords(params,this.tsFileValue).then((res) => {
                    this.dataSource = res.data.list
                    this.sortArray(this.dataSource,'isExist')
                    this.dataSource.forEach(item => {
                        item.auditState = 1
                    })
                    this.allData = this.dataSource
                    this.loading = false
                    this.select()
                }).catch((err) => {
                    this.loading = false
                })
            }else if(this.dataType === 'dictionary'){
                if(this.dictionaryType === '' || this.dictionaryType === null){
                    this.loading = false
                    return
                }
                // 字典文件导入
                let params = {
                    type: this.dictionaryType,
                    taskID: this.task.id,
                    versionID: this.task.versionId,
                    transType : this.task.translateType, 
                }
                getDictionaryEntry(params).then((res) => {
                    this.dataSource = res.data.list
                    this.sortArray(this.dataSource,'isExist')
                    this.allData = this.dataSource
                    this.loading = false
                    this.select()
                }).catch((err) => {
                    this.loading = false
                })
            }else if(this.dataType === 'database'){
                // 数据库导入
                if(this.dataLibrary.diFileName === null){
                    this.loading = false
                    message.warn("请选择回写字典目录！")
                    return
                }
                if(this.dataLibrary.type === 'field'){
                    if(this.dataLibrary.table === null){
                        this.loading = false
                        return
                    }
                    this.getFieldData()
                }else if(this.dataLibrary.type === 'alias'){
                    if(this.dataLibrary.table === null){
                        this.loading = false
                        return
                    }
                    this.getAlias()
                }else if(this.dataLibrary.type === 'allData'){
                    if(this.dataLibrary.tables.length === 0){
                        this.loading = false
                        return
                    }
                    this.batchImportDatabase()
                }
                
            }else if(this.dataType === 'file'){
                // console.log(this.file)
                if(Object.keys(this.file).length === 0){
                    this.loading = false
                    return
                }
                // 文件导入
                let formData = new FormData()
                formData.append('file',this.file)
                formData.append('taskID',this.task.id)
                this.loading = true
                // importExcle(formData).then((res) => {
                //     this.dataSource = res.data.list
                //     this.allData = this.dataSource
                //     this.loading = false
                //     this.select()
                // })
                readZZExcle(formData).then((res) => {
                    this.dataSource = res.data.list
                    this.sortArray(this.dataSource,'isExist')
                    this.allData = this.dataSource
                    this.loading = false
                    this.select()
                }).catch((err) => {
                    message.error("导入失败！")
                    this.loading = false
                })
            }else if(this.dataType === 'config'){
                // 配置文件数据导入
                if(this.configFile.dict === null){
                    message.warn("请选择回写字典目录！")
                    return
                }
                let params = {
                    diFileName: this.configFile.dict,
                    taskID: this.task.id,
                    versionID: this.task.versionId ? this.task.versionId : "",
                    translateType : this.task.translateType, 
                }
                getConfigEntry(params).then((res) => {
                    this.dataSource = res.data.list
                    this.sortArray(this.dataSource,'isExist')
                    this.allData = this.dataSource
                    this.loading = false
                    this.select()
                }).catch((err) => {
                    this.loading = false
                })
                
            }
            
        },
        sortArray(arr,key){
            return arr.sort((a,b) => {
                let x = a[key]
                let y = b[key]
                return ((x>y) ? -1 : (x<y) ? 1 : 0)
            })
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
                versionID: this.task.versionId ? this.task.versionId : "",
                translateType: this.task.translateType,
                diFileName: this.dataLibrary.diFileName
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
                this.sortArray(this.dataSource,'isExist')
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
                versionID: this.task.versionId ? this.task.versionId : "",
                translateType: this.task.translateType,
                diFileName: this.dataLibrary.diFileName
            }
            getAlias(params).then((res) => {
                this.dataSource = res.data.list
                this.sortArray(this.dataSource,'isExist')
                this.allData = this.dataSource
                this.loading = false
                this.select()
            })
        },
        // 批量导入数据库
        batchImportDatabase(){
            let selectNode = []
            this.dataLibrary.tables.forEach(item => {
                let node = this.treeData.find(d => d.value === item)
                selectNode.push(node)
            })
            if(selectNode.length === 0){
                return
            }
            let nodes = []
            let apps = []
            let dbs = []
            let tables = []

            selectNode.forEach(item => {
                if(item.type === 'node'){
                    nodes.push(item)
                }else if(item.type === 'app'){
                    apps.push(item)
                }else if(item.type === 'db'){
                    dbs.push(item)
                }else if(item.type === 'table'){
                    tables.push(item)
                }
            })
            // console.log("nodes:",nodes)
            // console.log("apps:",apps)
            // console.log("dbs:",dbs)
            // console.log("tables:",tables)
            this.dataSource = []
            let params = {
                taskID: this.task.id,
                versionID: this.task.versionId ? this.task.versionId : "",
                translateType: this.task.translateType,
                diFileName: this.dataLibrary.diFileName
            }
            if(nodes.length > 0){
                nodes.forEach(item => {
                    item.node = item.value
                })
                getDBALLEntryByNode(params,nodes).then((res) => {
                    this.dataSource = this.dataSource.concat(res.data.list)
                    this.sortArray(this.dataSource,'isExist')
                    this.allData = this.dataSource
                    this.loading = false
                    this.select()
                })
            }
            if(apps.length > 0){
                apps.forEach(item => {
                    item.modeType = item.appId
                    item.app = item.title
                })
                getDBALLEntryByApp(params,apps).then((res) => {
                    this.dataSource = this.dataSource.concat(res.data.list)
                    this.sortArray(this.dataSource,'isExist')
                    this.allData = this.dataSource
                    this.loading = false
                    this.select()
                })
            }
            if(dbs.length > 0){
                dbs.forEach(item => {
                    item.db = item.title
                    item.modeType = item.appId
                })
                getDBALLEntryByDB(params,dbs).then((res) => {
                    this.dataSource = this.dataSource.concat(res.data.list)
                    this.sortArray(this.dataSource,'isExist')
                    this.allData = this.dataSource
                    this.loading = false
                    this.select()
                })
            }
            
        },
        // 添加表格行点击事件
        customRow(record, index){
            return {
                onDblclick: (event) => {
                    if(this.editableData.hasOwnProperty(record.id)){
                        // 当前行在编辑状态
                        return
                    }
                    this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
                    // 设置校验规则
                    this.rules[record.id] = {
                        entry:[{ validator: this.vilidFildLength(record) },
                        { required: true, message: '请输入!' }]
                    }
                    let languageCode = workbenchCommon.languageMap[this.task.translateType].code
                    this.rules[record.id][languageCode] = [{ validator: this.vilidFildLength(record) }]
                }
            }
        },
        // 校验输入数据的长度
        vilidFildLength(record){
            return (rule,value) =>{
                let maxLength = this.classifyLimit[record.classfy1]
                if(maxLength === undefined || maxLength === null || maxLength === 0){
                    return Promise.resolve();
                }
                // 获取输入数据的长度
                let length = common.byteLength(value)
                if(length > maxLength){
                    return Promise.reject('允许最大字符数为'+maxLength+'！');
                }
                return Promise.resolve();
            }
        },
        // 输入框 回车事件
        inputPressEnter(record){
            record.chineseInterpretation =  this.editableData[record.id].chineseInterpretation
            record.englishInterpretation =  this.editableData[record.id].englishInterpretation
            record.entryLabel = this.editableData[record.id].entryLabel

            let languageCode = workbenchCommon.languageMap[this.task.translateType].code
            // 长度校验
            let list = [eval("this.$refs.form"+ record.id.replaceAll('-','') + 'entry').validate(),
                        eval("this.$refs.form"+ record.id.replaceAll('-','') + languageCode).validate()]
            Promise.all(list).then(() => {
                record[languageCode] = this.editableData[record.id][languageCode]
                record.entry = this.editableData[record.id].entry
                if(record[languageCode] != null && record[languageCode] != null){
                    // 翻译存在  则状态为待审核状态
                    record[languageCode+"TranslateState"] = '1'
                }
                delete this.editableData[record.id]
            }).catch((err) => {
                
            })
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
        },
        // 创建字典
        createDictionary(){
            this.createDictVisible = true
            this.createDict.name = ""
        },
        createDictOk(){
            this.$refs.dictRef.validate().then(() => {
                let params = {
                    dicName: this.createDict.name
                }
                createDic(params).then((res) => {
                    message.success("创建成功！")
                    this.getDictionary()
                })
            })
        },
        createDictClose(){
            this.createDictVisible = false
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
.ant-form-item{
    margin-bottom: 0%;
}
</style>