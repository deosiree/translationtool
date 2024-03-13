<template>
    <Modal
    :visible="visible" 
    :modalTitle="modalTitle"
    :modalWidth="modalWidth"
    okText="保存"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    >
        <div class="content">
            <div class="table">
                <div class="taskInfo">
                    <div class="taskItem">任务名称：{{task.name}}</div>
                    <div class="taskItem">产品名称：{{task.productName}}</div>
                    <div class="taskItem">翻译语种：{{task.translateType}}</div>
                </div>
                <div class="form">
                    词条：
                    <a-input
                        v-model:value="keyWords"
                        style="width:250px"
                        size="small"
                        placeholder='请输入词条搜索'
                    />
                    <span style="margin-left:10px">翻译状态：</span>
                    <a-select
                    v-model:value="translateState"
                    allowClear
                    size="small"
                    style="width: 250px"
                    placeholder='请选择'
                    >
                        <a-select-option value="0">待翻译</a-select-option>
                        <a-select-option value="1">待审核</a-select-option>
                        <a-select-option value="2">审核不通过</a-select-option>
                        <!-- <a-select-option value="33">审核通过</a-select-option> -->
                    </a-select>
                    <a-button type="primary" size="small" style="margin-left:8px" @click="getTranslateEntry">查询</a-button>
                    <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="preTranslation">预翻译</a-button>
                    <!-- <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="save">保存</a-button> -->
                    <a-button type="primary" size="small" style="margin-left:8px" @click="exportExcel">导出Excel</a-button>
                    <a-upload
                        name="file"
                        :beforeUpload="beforeUpload"
                        :accept="accept"
                        :showUploadList="false"
                        @change="handleChange"
                    >
                        <a-button type="primary" size="small" style="margin-left:8px">翻译导入</a-button>
                    </a-upload>
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
                childrenColumnName="child"
                ref="tableContainer"
                @resizeColumn="handleResizeColumn"
                :customRow="customRow"
                >
                    <template #bodyCell="{ column, text, record }">
                        <template v-if="['english','russian','spanish','french'].includes(column.dataIndex)">
                            <div>
                                <template v-if="editableData[record.id]">
                                    <a-form :model="editableData[record.id]" :rules="rules[record.id]" :ref="'form'+record.id.replaceAll('-','')+column.dataIndex" autocomplete="off">
                                        <a-form-item :name="column.dataIndex"
                                        >
                                            <a-input
                                                :ref="'ref'+record.id.replaceAll('-','')"
                                                v-model:value="editableData[record.id][column.dataIndex]"
                                                style="margin: -5px 0"
                                                @pressEnter="inputPressEnter(record)"
                                                @click="clickInput"
                                                @change="changeInput(record)"
                                            />
                                        </a-form-item>
                                    </a-form>
                                </template>
                                <template v-else>
                                    {{ text }}
                                </template>
                            </div>
                        </template>
                        <template v-if="['englishTranslateState','russianTranslateState','spanishTranslateState','frenchTranslateState'].includes(column.dataIndex)">
                            <template v-if="record[column.dataIndex] === '0'">
                                <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">未翻译</span>
                            </template>
                            <template v-if="record[column.dataIndex] === '1'">
                                <a-badge color="#FBB31F" /><span style="color:#FBB31F">待审核</span>
                            </template>
                            <template v-if="record[column.dataIndex] === '2'">
                                <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
                            </template>
                            <template v-if="record[column.dataIndex] === '3'">
                                <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
                            </template>
                        </template>
                    </template>
                </a-table>
            </div>
            <div class="suggest">
                <div style="height:30px">
                    <span style="float:right;font-size:12px">
                        <a-tooltip placement="left">
                            <template #title>
                                <table>
                                    <tr><td style="width:100px">上一个</td><td>Ctrl + ↑</td></tr>
                                    <tr><td style="width:100px">下一个</td><td>Ctrl + ↓</td></tr>
                                    <tr><td style="width:100px">上一个未翻译</td><td>Ctrl + Shift + ↑</td></tr>
                                    <tr><td style="width:100px">下一个未翻译</td><td>Ctrl + Shift + ↓</td></tr>
                                    <tr><td style="width:100px">编辑 </td><td>Ctrl + e</td></tr>
                                    <tr><td style="width:100px">保存 </td><td>Ctrl + Enter</td></tr>
                                </table>
                            </template>
                            快捷键
                            <QuestionCircleOutlined />
                        </a-tooltip>
                    </span>
                </div>
                <div style="margin-bottom: 6px;">翻译建议：</div>
                <div class="suggentContent">
                    <div>
                        <span class="title">中文释义：</span><span>{{chineseInterpretation}}</span>
                    </div>
                    <div>
                        <span class="title">英文释义：</span><span>{{englishInterpretation}}</span>
                    </div>
                    <span>翻译：</span>
                    <span class="title">本地翻译：</span>
                    <template v-for="(item,index) in suggest.local" :key="index">
                        <div class="suggentItem" @click="suggestClick(item.title,item.id)">
                            <div class="tran">
                                <img src="../../assets/icon/local.png" style="width:24px;height:24px;margin-right:8px"/>
                                <span>{{item.title}}</span>
                            </div>
                            <div class="tips">{{item.tips}}  Ctrl+{{index + 1}}</div>
                        </div>
                    </template>
                    <span class="title">外网翻译：</span>
                    <template v-for="(item,index) in suggest.web" :key="index">
                        <div class="suggentItem" @click="suggestClick(item.title,item.id)">
                            <div class="tran">
                                <img :src="require('../../assets/icon/'+item.type+'.png')" style="width:24px;height:24px;margin-right:8px"/>
                                <span>{{item.title}}</span>
                            </div>
                            <div class="tips">{{item.tips}}  Ctrl+{{index + this.suggest.local.length + 1}}</div>
                        </div>
                    </template>
                </div>
                <a-spin :spinning="spinning" tip="翻译中...."/>
            </div>
        </div>
    </Modal>
    <Modal  
    :visible="selectVisible" 
    modalTitle="预翻译"
    style="top: 30%"
    :okLoading="preTranslateOkLoading"
    @handleClose="selectHandleClose"
    @handleOK="selectHandleOK"
    @afterClose="selectAfterClose"
    >
        <div style="width:100%;height:100%">
            <a-form
                ref="formRef"
                name="custom-validation"
                autocomplete='off'
                :label-col="labelCol"
                :model="preTran"
            >
                <a-form-item label="优先级" name="priority"
                    :rules="[{ required: true, message: '请选择优先级!' }]"
                >
                    <a-select
                    v-model:value="preTran.priority"
                    placeholder="请选择"
                    >
                        <a-select-option value="shuyuku">术语库</a-select-option>
                        <a-select-option value="youdao">有道翻译</a-select-option>
                        <a-select-option value="baidu">百度翻译</a-select-option>
                        <a-select-option value="google">Google翻译</a-select-option>
                        <a-select-option value="module">本地模型</a-select-option>
                    </a-select>
                </a-form-item>
            </a-form>
        </div>
    </Modal>
    <Modal
    :visible="exportVisible" 
    modalTitle="导出"
    style="top:30%"
    @handleClose="exportClose"
    @handleOK="exportOK"
    @afterClose="exportAfterClose"
    >
        <div style="width:100%;height:100%">
            <a-form
                ref="exportForm"
                name="custom-validation"
                :model="exportModal"
            >
                <a-form-item
                label="导出字段"
                name="field"
                :rules="[{ required: true, message: '请选择导出字段!' }]"
                >
                    <a-select
                    mode="multiple"
                    v-model:value="exportModal.field"
                    :options="fieldOptions"
                    :fieldNames="{label:'label',value:'label'}"
                    placeholder="请选择"
                    ></a-select>
                </a-form-item>
            </a-form>
        </div>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import { cloneDeep } from 'lodash-es';
import {
    getEntryTempByTaskID,
    updateEntryTemp,
    preTranslate,
    getEntryInfoList,
    updateEntryList,
} from '@/http/api/workbench'
import {
    translate
} from '@/http/api/entryManage'
import {
    entryExportByCondition
} from "@/http/api/download"
import {
    importExcle
} from '@/http/api/entry'
import {
  QuestionCircleOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import workbenchCommon from '@/views/workbench/common.js';
import common from '../entry/common';
import tableParam from '../entry/tableParam';
import key from 'keymaster'
export default {
    components:{
        Modal,
        QuestionCircleOutlined
    },
    emits:['handleClose','handleOK'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:'词条翻译'
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
            modalWidth:"75%",
            task:{},
            keyWords:"",
            tableHeight: { x:'100%',y: '415px' },
            loading:false,
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:50,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left'},
                {title: '翻译状态',dataIndex: 'state',align:'center',width:90,fixed: 'left'},
                {title: '词条',dataIndex: 'entry',align:'center',width:200,fixed: 'left',resizable: true},
                {title: 'Abbr',dataIndex: 'abbr',align:'center',width:150,resizable: true,index:2},
                {title: '翻译',dataIndex: 'translate',align:'center',width:300,ellipsis: true,resizable: true},
                {title: 'TAG',dataIndex: 'entryLabel',align:'center',width:150,ellipsis: true},
                // {title: '来源',dataIndex: 'entrySource',align:'center',width:150,resizable: true,ellipsis:true},
                {title: '审核意见',dataIndex: 'auditSuggess',align:'center',width:100,ellipsis: true,resizable: true},
            ],
            dataSource:[],
            allData:[],
            editableData:{},
            selectedRowIndex:null,
            suggest:{
                local:[],
                web:[]
            },
            spinning: false,
            timer:null,
            selectVisible:false,
            selectTitle:"",
            preTran:{
                priority:null
            },
            labelCol: { style: { width: '80px' } },
            chineseInterpretation:"",
            englishInterpretation:"",
            translateState:null,
            rules:{},
            exportVisible: false,
            exportModal:{
                field:[]
            },
            fieldOptions: tableParam.exportFields,
            accept:".xls,.xlsx",
            preTranslateOkLoading: false
        }
    },
    
    created() {
    },
    mounted () {
        this.task = this.currentTask
    },
    unmounted(){
        
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
                if(item.title === '翻译状态'){
                    item.dataIndex = workbenchCommon.languageMap[this.task.translateType].code + "TranslateState"
                }
                if(item.title === '审核意见'){
                    item.dataIndex = workbenchCommon.languageMap[this.task.translateType].code + "AuditSuggest"
                }
            })
        },
        init(){
            this.getTranslateEntry()
            let _this = this
            // 绑定快捷键
            key('ctrl+down',function(){ _this.nextEntry(); return false })
            key('ctrl+up',function(){ _this.prevEntry(); return false })
            key('ctrl+shift+down',function(){ _this.nextNotTransEntry(); return false })
            key('ctrl+shift+up',function(){ _this.prevNotTransEntry(); return false })
            key('ctrl+e',function(){ _this.editSelectRow(); return false })
            key('ctrl+enter',function(){ _this.enterEditEntry(); return false })
        },
        // 获取待翻译词条
        getTranslateEntry(){
            let params = {
                taskID: this.task.id,
                entryState: '3',
                entry: this.keyWords
            }
            this.loading = true
            let languageCode = workbenchCommon.languageMap[this.task.translateType].code
            let data = (this.translateState === null || this.translateState === undefined) ? ['0','2'] : [this.translateState]
            getEntryInfoList(params,data).then((res) => {
                this.dataSource = res.data.list
                if(this.dataSource.length > 0){
                    this.selectedRowIndex = this.dataSource[0].id
                    this.assistedTranslation(this.dataSource[0])
                }
                this.dataSource.forEach(item => {
                    if(item[languageCode+'TranslateState'] === null ||
                    item[languageCode+'TranslateState'] === ''){
                        item[languageCode+'TranslateState'] = '0'
                    }
                })

                // this.allData = this.dataSource
                this.loading = false
                // this.select()
            }).catch((err) => {
                this.loading = false
            })
        },
        handleOK(){
            let languageCode = workbenchCommon.languageMap[this.task.translateType].code
            let transIdName = workbenchCommon.languageMap[this.task.translateType].transIdName
            for (let key in this.editableData) {
				let entry = this.dataSource.find(item => item.id === key)
                // entry.translate = this.editableData[key].translate
                entry[languageCode] = this.editableData[key][languageCode]
                entry[transIdName] = this.editableData[key][transIdName]
			}
            this.editableData = {}
            // 设置翻译状态
            this.dataSource.forEach(item => {
                if(item[languageCode] === null || item[languageCode] === ""){
                    item[languageCode+"TranslateState"] = '0'// 待翻译
                }else if(item[languageCode+"TranslateState"] === '0' || item[languageCode+"TranslateState"] === '2'){
                    item[languageCode+"TranslateState"] = '1'// 已翻译待审核
                }
                
            })

            let params = {
                taskID: this.task.id
            }
            updateEntryList(params,this.dataSource).then((res) => {
                message.success('已保存！')
                this.getTranslateEntry()
            }).catch((err) => {
                message.error('保存失败！')
            })
        },
        handleClose(){
            this.$emit('handleClose')
        },
        getRowClassName(record, index){
            let className = null
            if(index % 2 === 1){
                className = 'table-striped'
                if(this.selectedRowIndex === record.id){
                    className = className + " highlighted-row"
                }
            }else{
                if(this.selectedRowIndex === record.id){
                    className = "highlighted-row"
                }
            }
            return className
        },
        // 保存
        save(){
            for (let key in this.editableData) {
				let entry = this.dataSource.find(item => item.id === key)
                entry.translate = this.editableData[key].translate

                // 如果有子词条  则写入子词条
                if(record.children && record.children.length > 0){
                    record.children.forEach(item => {
                        item.translate = entry.translate
                        item.translateID = entry.id
                    })
                }
			}
            this.editableData = {}
            updateEntryTemp(this.dataSource).then((res) => {
                message.success('已保存！')
            })
        },
        // 添加表格行点击事件
        customRow(record, index){
            return {
                onClick: (event) => {
                    let _this = this
                    this.selectedRowIndex = record.id
                    this.assistedTranslation(record)
                    // clearTimeout(this.timer)
                    
                    // this.timer = setTimeout(function () {
                    //     _this.selectedRowIndex = record.id
                    //     _this.assistedTranslation(record)
                    // }, 500);
                },
                onDblclick: (event) => {
                    // clearTimeout(this.timer)
                    if(this.editableData.hasOwnProperty(record.id)){
                        // 当前行在编辑状态
                        return
                    }
                    this.edit(record)
                }
            }
        },
        edit(record){
            this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
            // 设置校验规则
            this.rules[record.id] = {
                entry:[{ validator: this.vilidFildLength(record) },
                { required: true, message: '请输入!' }]
            }
            let languageCode = workbenchCommon.languageMap[this.task.translateType].code
            this.rules[record.id][languageCode] = [{ validator: this.vilidFildLength(record) }]
            return Promise.resolve()
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
        handleResizeColumn: (w, col) => {
            col.width = w;
        },
        
        select(){
            this.dataSource = this.allData.filter(item => item.entry.includes(this.keyWords))
        },
        // 辅助翻译
        assistedTranslation(record){
            // 设置翻译建议中的中文释义  英文释义
            this.chineseInterpretation = record.chineseInterpretation
            this.englishInterpretation = record.englishInterpretation

            this.spinning = true
            let params = {
                name: record.entry,
                type: this.task.translateType,
                department: this.task.department
            }
            // 清空 快捷键
            this.deleteShortcutKeys()
            
            translate(params).then((res) => {
                this.suggest = {
                    local:[],
                    web:[]
                }
                res.data.translateEntities.forEach(element => {
                    if(element.source.includes('本地')){
                        element.languageEntities.forEach(item => {
                            let suggent = {
                                title: item.value,
                                tips: element.source,
                                type:'local',
                                id: item.id
                            }
                            this.suggest.local.push(suggent)
                        })
                    }else{
                        let type = ''
                        if(element.source.includes('百度')){
                            type = 'baidu'
                        }else if(element.source.includes('有道')){
                            type = 'youdao'
                        }else if(element.source.includes('谷歌')){
                            type = 'google'
                        }else if(element.source.includes('模型')){
                            type = 'ai'
                        }else{
                            type = 'local'
                        }
                        element.languageEntities.forEach(item => {
                            let suggent = {
                                title: item.value,
                                tips: element.source,
                                type:type,
                                id: item.id
                            }
                            this.suggest.web.push(suggent)
                        })
                    }
                });
                this.spinning = false
                this.setShortcutKeys()
            }).catch((err) => {
                this.suggest = {
                    local:[],
                    web:[]
                }
                this.spinning = false
            })
        },
        // 设置翻译建议快捷键
        setShortcutKeys(){
            let _this = this
            for(let i = 1 ; i<= this.suggest.local.length; i++){
                key('ctrl+'+i,function(){_this.clickSug(i); return false })
            }
            for(let i = 1 ; i<= this.suggest.web.length; i++){
                let j = this.suggest.local + i
                key('ctrl+'+j,function(){_this.clickSug(i); return false })
            }
        },
        // 删除辅助翻译快捷键
        deleteShortcutKeys(){
            let list = this.suggest.local.concat(this.suggest.web)
            for(let i = 1; i<= list.length;i++){
                key.unbind('ctrl+'+i)
            }
        },
        // 辅助翻译快捷键点击事件
        clickSug(i){
            let list = this.suggest.local.concat(this.suggest.web)
            // console.log(i)
            this.suggestClick(list[i-1].title,list[i-1].id)
        },
        suggestClick(title,id){
            if(this.selectedRowIndex === null){
                return
            }
            let record = this.dataSource.find(item => item.id === this.selectedRowIndex)

            let languageCode = workbenchCommon.languageMap[this.task.translateType].code
            let transIdName = workbenchCommon.languageMap[this.task.translateType].transIdName

            record[languageCode] = title
            record[transIdName] = id
            // 如果有子词条  则写入子词条
            if(record.children && record.children.length > 0){
                record.children.forEach(item => {
                    item[languageCode] = title
                    item[transIdName] = id
                })
            }

            if(this.editableData[this.selectedRowIndex] != undefined){
                this.editableData[this.selectedRowIndex][languageCode] = title
                this.editableData[this.selectedRowIndex][transIdName] = id

                // 如果有子词条  则写入子词条
                if(this.editableData[this.selectedRowIndex].children && this.editableData[this.selectedRowIndex].children.length > 0){
                    this.editableData[this.selectedRowIndex].children.forEach(item => {
                        item[languageCode] = title
                        item[transIdName] = id
                    })
                }
            }
            
            // 校验字符串长度
            let maxLength = this.classifyLimit[record.classfy1]
            if(common.byteLength(title) > maxLength){
                if(this.editableData[this.selectedRowIndex] != undefined){
                    let list = [eval("this.$refs.form"+ record.id.replaceAll('-','') + languageCode).validate()]
                    Promise.all(list).then(() => {

                    })
                }else{
                    this.edit(record).then(() => {
                        eval("this.$refs.form"+ record.id.replaceAll('-','') + languageCode).validate().then(() => {

                        }).catch((err) => {
                            
                        })
                    })
                }
            }
        },
        // 输入框 回车事件
        inputPressEnter(record){

            let languageCode = workbenchCommon.languageMap[this.task.translateType].code
            let transIdName = workbenchCommon.languageMap[this.task.translateType].transIdName

            // 长度校验
            let list = [eval("this.$refs.form"+ record.id.replaceAll('-','') + languageCode).validate()]
            Promise.all(list).then(() => {
                record[languageCode] = this.editableData[record.id][languageCode]
                record[transIdName] = this.editableData[record.id][transIdName]
                
                // 如果有子词条  则写入子词条
                if(record.children && record.children.length > 0){
                    record.children.forEach(item => {
                        item[languageCode] = record[languageCode]
                        item[transIdName] = record[transIdName]
                    })
                }
                delete this.editableData[record.id]
            }).catch((err) => {
                
            })

            
        },
        // 预翻译
        preTranslation(){
            if(this.dataSource.length === 0){
                return
            }
            this.selectVisible = true
        },
        selectHandleOK(){
            this.$refs.formRef.validate().then(() => {
                this.preTranslateOkLoading = true
                let params = {
                    taskID: this.task.id,
                    priority: this.preTran.priority
                }
                this.loading = true
                preTranslate(params,this.dataSource).then((res) => {
                    this.dataSource = res.data.list
                    // this.allData = this.dataSource
                    // 修改编辑中数据值
                    for (let key in this.editableData) {
                        this.editableData[key] = this.dataSource.find(item => item.id === key)
                    }
                    this.loading = false
                    this.selectVisible = false
                    this.preTranslateOkLoading = false
                }).catch((err) => {
                    message.error("预翻译失败！")
                    this.loading = false
                    this.selectVisible = false
                    this.preTranslateOkLoading = false
                })
            })
        },
        selectHandleClose(){
            this.selectVisible = false
        },
        selectAfterClose(){
            this.preTran.priority = null
        },
        clickInput(event){
            event.stopPropagation();
        },
        changeInput(record){
            record.translateID = ""
        },
        afterClose(){
            this.selectedRowIndex = null
            this.keyWords = ""
            this.translateState = null
            // 清空 辅助翻译快捷键
            this.deleteShortcutKeys()
            this.suggest = {
                local:[],
                web:[]
            }
            this.editableData = {}
            // 解绑快捷键
            key.unbind('ctrl+down,ctrl+up,ctrl+shift+down,ctrl+shift+up,ctrl+e,ctrl+enter')
        },

        // 导出
        exportExcel(){
            if(this.dataSource.length === 0){
                return
            }
            this.exportVisible = true
        },
        exportClose(){
            this.exportVisible = false
        },
        exportOK(){
            this.$refs.exportForm.validate().then(() => {
                // 导出接口
                let fields = ['id'].concat(this.exportModal.field)
                let data = {
                    columnNames: fields,
                    entryInfoEntities: this.dataSource,
                    excelName:'词条导出'
                }
                entryExportByCondition(data).then((res) => {
                    let fileName = res.headers["content-disposition"].split(";")[1].split("filename=")[1]
                    let contentType = res.headers['content-type']
                    const blob = new Blob([res.data], {type: contentType})
                    const a = document.createElement('a') // 转换完成，创建一个a标签用于下载
                    a.download = decodeURI(fileName)
                    a.href = window.URL.createObjectURL(blob)
                    a.click()
                    a.remove()
                    window.URL.revokeObjectURL(a.href);
                    this.exportVisible = false
                })
            }).catch((err) => {

            })
        },
        exportAfterClose(){
            this.exportModal.field = []
        },
        beforeUpload(file, fileList){
            // console.log("before");
            return false
        },
        handleChange(info){
            let file = info.file
            let formData = new FormData()
            formData.append('file',file)
            formData.append('transType',this.task.translateType)
            this.loading = true
            importExcle(formData).then((res) => {
                this.dataSource = res.data.list
                // this.allData = this.dataSource
                this.loading = false
            }).catch((err) => {
                message.error('导入失败！')
                this.loading = false
            })
        },
        // 下一个词条 快捷键
        nextEntry(){
            if(this.selectedRowIndex === null){
                return
            }
            let index = this.dataSource.findIndex(item => item.id === this.selectedRowIndex)
            if(index === this.dataSource.length - 1){
                return   
            }
            index++ 
            this.selectedRowIndex = this.dataSource[index].id
            this.scrollTableToRow(index)
            // this.assistedTranslation(this.dataSource[index])
        },
        // 上一个词条
        prevEntry(){
            if(this.selectedRowIndex === null){
                return
            }
            let index = this.dataSource.findIndex(item => item.id === this.selectedRowIndex)
            if(index === 0){
                return   
            }
            index--
            this.selectedRowIndex = this.dataSource[index].id
            this.scrollTableToRow(index)
            // this.assistedTranslation(this.dataSource[index])
        },
        // 下一个未翻译词条
        nextNotTransEntry(){
            if(this.selectedRowIndex === null){
                return
            }
            let index = this.dataSource.findIndex(item => item.id === this.selectedRowIndex)
            if(index === this.dataSource.length - 1){
                return   
            }
            let notTransIndex = index
            index++
            let language = workbenchCommon.languageMap[this.task.translateType].code
            for(index; index < this.dataSource.length; index++){
                if(this.dataSource[index][language] === null || this.dataSource[index][language] === ""){
                    notTransIndex = index
                    break
                }
            }
            this.selectedRowIndex = this.dataSource[notTransIndex].id
            this.scrollTableToRow(notTransIndex)
            // this.assistedTranslation(this.dataSource[notTransIndex])
        },
        // 上一个未翻译词条
        prevNotTransEntry(){
            if(this.selectedRowIndex === null){
                return
            }
            let index = this.dataSource.findIndex(item => item.id === this.selectedRowIndex)
            if(index === 0){
                return   
            }
            let preNotTransIndex = index
            index--
            for(index;index >= 0;index--){
                let language = workbenchCommon.languageMap[this.task.translateType].code
                if(this.dataSource[index][language] === null || this.dataSource[index][language] === ""){
                    preNotTransIndex = index
                    break
                }
            }
            this.selectedRowIndex = this.dataSource[preNotTransIndex].id
            this.scrollTableToRow(preNotTransIndex)
            // this.assistedTranslation(this.dataSource[preNotTransIndex])
        },
        // 编辑选中行
        editSelectRow(){
            if(this.selectedRowIndex === null){
                return
            }
            if(this.editableData.hasOwnProperty(this.selectedRowIndex)){
                // 编辑数据中包含该数据
                eval("this.$refs.ref"+ this.selectedRowIndex.replaceAll('-','')).focus()
            }else{
                // 编辑数据中不包含该数据
                this.editableData[this.selectedRowIndex] = this.dataSource.find(item => item.id === this.selectedRowIndex)
                this.$nextTick(() => {
                    let input = eval("this.$refs.ref"+ this.selectedRowIndex.replaceAll('-',''))
                    input.focus()
                })
                
            }
        },
        // 确定编辑
        enterEditEntry(){
            let entry = this.dataSource.find(item => item.id === this.selectedRowIndex)
            entry = this.editableData[this.selectedRowIndex]
            delete this.editableData[this.selectedRowIndex]
        },
        // 滚动表格
        scrollTableToRow(rowIndex) {
            const table = this.$refs.tableContainer; // 获取表格容器元素
            if (table && rowIndex >= 0) {
                // 根据索引查找目标行元素
                const targetElement = table.$el.querySelectorAll('tr')[rowIndex]
                // console.log(targetElement.offsetHeight)
                let container = this.$refs.tableContainer.$el.querySelector('.ant-table-body')
                if (targetElement) {
                    // container.scrollTop = targetElement.offsetTop - container.scrollHeight / 2 + 40 // 设置滚动条位置
                    container.scrollTop = rowIndex * targetElement.offsetHeight - 370 // 当前行 * 行高 - 表格展示高度
                }
            }
        },
    }
}
</script>
<style lang="less">
@import url("@/assets/style/common.less");
</style>
<style scoped lang="less">
.content{
    width: 100%;
    height: 100%;
    min-height: 400px;
    padding: 10px;
    background-color: #F3F3F3;
    display: flex;
    // align-items: center;
    gap: 16px;
    align-self: stretch;

    .table{
        width:70%;
        height: 100%;
        
    }
    .suggest{
        width:30%;
        // padding-top: 30px;
        // flex:1;
        position: relative;

        .suggentContent{
            width:100%;
            height: calc(100% - 30px);
            background: #FFF;
            display: flex;
            padding: 10px;
            flex-direction: column;
            align-items: flex-start;
            gap: 8px;
            flex: 1 0 0;
            align-self: stretch;
            overflow: auto;
            // max-height: 360px;

            .title{
                color: var(--text-icon-font-gy-340-placeholder, rgba(0, 0, 0, 0.40));

                /* 五级文字/常规 */
                font-family: Microsoft YaHei;
                font-size: 12px;
                font-style: normal;
                font-weight: 400;
                line-height: 20px; /* 166.667% */
            }

            .suggentItem{
                width: 100%;
                .tran{
                    display: flex;
                    align-items: center;

                    span{
                        color: var(--text-icon-font-gy-190-primary, rgba(0, 0, 0, 0.90));
                        /* 四级文字/常规 */
                        font-family: Microsoft YaHei;
                        font-size: 14px;
                        font-style: normal;
                        font-weight: 400;
                        line-height: 22px;
                    }
                }
                .tips{
                    color: var(--text-icon-font-gy-340-placeholder, rgba(0, 0, 0, 0.40));
                    /* 五级文字/常规 */
                    font-family: Microsoft YaHei;
                    font-size: 10px;
                    font-style: normal;
                    font-weight: 400;
                    line-height: 20px; /* 166.667% */
                }
            }
            .suggentItem:hover{
                background-color: #f1f5f6;
            }
        }

        :deep(.ant-spin){
            position: absolute;
            left: 50%;
            top: 50%;
            transform: translate(-50%, -50%);
        }
    }
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
    .form{
        display: flex;
        align-items: center;
        align-self: stretch;
        width: 100%;
        margin-bottom: 6px;
    }
}
.ant-table-cell .ant-form-item{
    margin-bottom: 0%;
}
</style>