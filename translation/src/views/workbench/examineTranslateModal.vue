<template>
    <Modal
    :visible="visible" 
    :modalTitle="modalTitle"
    :modalWidth="modalWidth"
    okText="保存"
    :okLoading="saveLoading"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    >
        <div class="content">
            
            <div class="taskInfo">
                <div class="taskItem">任务名称：{{task.name}}</div>
                <div class="taskItem">产品名称：{{task.productName}}</div>
                <div class="taskItem">翻译语种：{{task.translateType}}</div>
                <span style="float:right;font-size:12px">
                    <a-tooltip placement="left">
                        <template #title>
                            <table>
                                <tr><td style="width:100px">上一个</td><td>Ctrl + ↑</td></tr>
                                <tr><td style="width:100px">下一个</td><td>Ctrl + ↓</td></tr>
                                <tr><td style="width:100px">上一个未审核</td><td>Ctrl + Shift + ↑</td></tr>
                                <tr><td style="width:100px">下一个未审核</td><td>Ctrl + Shift + ↓</td></tr>
                                <tr><td style="width:100px">编辑 </td><td>Ctrl + e</td></tr>
                                <tr><td style="width:100px">保存 </td><td>Ctrl + Enter</td></tr>
                                <tr><td style="width:100px">通过 </td><td>Ctrl + p</td></tr>
                                <tr><td style="width:100px">驳回 </td><td>Ctrl + r</td></tr>
                            </table>
                        </template>
                        快捷键
                        <QuestionCircleOutlined />
                    </a-tooltip>
                </span>
            </div>
            <div class="form">
                词条：
                <a-input
                    v-model:value="keyWords"
                    style="width:300px"
                    size="small"
                    placeholder='请输入词条搜索'
                />
                <span style="margin-left:10px">翻译状态：</span>
                <a-select
                v-model:value="translateState"
                allowClear
                size="small"
                style="width: 300px"
                placeholder='请选择'
                >
                    <a-select-option value="1">待审核</a-select-option>
                    <a-select-option value="2">审核不通过</a-select-option>
                    <a-select-option value="3">审核通过</a-select-option>
                </a-select>
                <a-button type="primary" size="small" style="margin-left:8px" @click="getTaskEntry">查询</a-button>
                <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="pass">通过</a-button>
                <a-button type="primary" size="small" style="margin-left:8px" class="rejectBtn" @click="reject">驳回</a-button>
            </div>
            <a-table 
            bordered
            class="ant-table-striped"
            :columns="columns" 
            :data-source="dataSource" 
            :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            :row-key="record => record.id"
            :scroll="tableHeight"
            :pagination='false'
            :loading="loading"
            :rowClassName="getRowClassName"
            :customRow="doubleClick"
            childrenColumnName="child"
            ref="tableContainer"
            @resizeColumn="handleResizeColumn"
            >
                <template #bodyCell="{ column, text, record }">
                    <template v-if="['english','russian','spanish','french'].includes(column.dataIndex)">
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
                    <template v-if="['englishAuditSuggest','russianAuditSuggest','spanishAuditSuggest','frenchAuditSuggest'].includes(column.dataIndex)">
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
                    <template v-else-if="column.dataIndex === 'operation'">
                        <div class="editable-row-operations">
                            <span>
                                <a-checkable-tag :checked="record.auditState === 1" :class="record.auditState === 1 ? 'passTagChecked' : 'passTag' " @change="passTagChange(record)">通过</a-checkable-tag>
                                <a-checkable-tag :checked="record.auditState === 0" :class="record.auditState === 0 ? 'rejectTagChecked' : 'rejectTag'" @change="rejectTagChange(record)">驳回</a-checkable-tag>
                            </span>
                        </div>
                    </template>
                </template>
            </a-table>
        </div>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import { cloneDeep, iteratee } from 'lodash-es';
import {
    getEntryTempByTaskID,
    updateEntryTemp,
    getEntryInfoList,
    updateEntryList
} from '@/http/api/workbench'
import { message } from 'ant-design-vue';
import workbenchCommon from '@/views/workbench/common.js';
import common from '../entry/common';
import {
    FileSearchOutlined,
    QuestionCircleOutlined
} from '@ant-design/icons-vue';
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
            default:'翻译审核'
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
            keyWords:"",
            tableHeight: { x:'100%',y: '415px' },
            loading:false,
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:40,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left'},
                {title: '审核状态',dataIndex: 'state',align:'center',width:100,fixed: 'left'},
                {title: '词条',dataIndex: 'entry',align:'center',width:200,fixed: 'left',resizable: true},
                {title: 'Abbr',dataIndex: 'abbr',align:'center',width:150,resizable: true,index:2},
                // {title: '来源',dataIndex: 'source',align:'center',width:100,resizable: true,ellipsis:true},
                {title: 'TAG',dataIndex: 'entryLabel',align:'center',width:150,ellipsis: true},
                {title: '翻译',dataIndex: 'translate',align:'center',width:200,ellipsis: true,resizable: true},
                {title: '审核意见',dataIndex: 'auditSuggess',align:'center',width:200,ellipsis: true,resizable: true},
                {title: '操作',dataIndex: 'operation',align:'center',width:100,ellipsis: true,},
            ],
            dataSource:[],
            allData:[],
            selectedRowKeys:[],
            selectedRows:[],
            editableData:{},
            rules:{},
            translateState:null,
            saveLoading: false,
            selectedRowIndex: null,
            timer: null
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
        },
    },
    methods: {
        // 设置翻译列展示的语言
        setTranslateColumn(){
            this.columns.forEach(item => {
                if(item.title === '翻译'){
                    item.dataIndex = workbenchCommon.languageMap[this.task.translateType].code
                }
                if(item.title === '审核状态'){
                    item.dataIndex = workbenchCommon.languageMap[this.task.translateType].code + "TranslateState"
                }
                if(item.title === '审核意见'){
                    item.dataIndex = workbenchCommon.languageMap[this.task.translateType].code + "AuditSuggest"
                }
            })
        },
        // 获取待审核词条
        getTaskEntry(){
            
            let params = {
                taskID: this.task.id,
                entryState: '3',
                entry: this.keyWords
            }
            this.loading = true
            let data = (this.translateState === null || this.translateState === undefined) ? ['1'] : [this.translateState]
            getEntryInfoList(params,data).then((res) => {
                this.dataSource = res.data.list

                this.dataSource.forEach(item => {
                    item.auditState = -1
                })
                // this.allData = this.dataSource
                this.loading = false
                // this.select()
            }).catch((err) => {
                this.loading = false
            })

            // 初始化快捷键
            this.initShortcutKeys()
        },
        handleOK(){
            this.saveLoading = true
            let languageCode = workbenchCommon.languageMap[this.task.translateType].code
            for (let key in this.editableData) {
				let entry = this.dataSource.find(item => item.id === key)
                entry[languageCode+"AuditSuggest"] = this.editableData[key][languageCode+"AuditSuggest"]
                entry[languageCode] = this.editableData[key][languageCode]
			}
            this.editableData = {}

            let params = {
                taskID: this.task.id
            }
            let updateArr = []
            this.dataSource.forEach(item => {
                if(item.auditState === 0){
                    // 审核不通过
                    item[languageCode+"TranslateState"] = '2'
                    updateArr.push(item)
                }else if(item.auditState === 1){
                    // 审核通过
                    item[languageCode+"TranslateState"] = '3'
                    updateArr.push(item)
                }
            })
            if(updateArr.length > 0){
                updateEntryList(params,updateArr).then((res) => {
                    message.success('已保存！')
                    this.getTaskEntry()
                    this.saveLoading = false
                }).catch((err) => {
                    message.error('保存失败！')
                    this.saveLoading = false
                })
            }else{
                this.saveLoading = false
            }
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
        handleResizeColumn: (w, col) => {
            col.width = w;
        },
        // 模糊查询
        select(){
            this.dataSource = this.allData.filter(item => item.entry.includes(this.keyWords))
        },
        onSelectChange(selectedRowKeys,selectedRows){
            this.selectedRowKeys = selectedRowKeys
            this.selectedRows = selectedRows
        },
        // 通过标签点击事件
        passTagChange(record){
            if(record.auditState === 1){
                // 取消选择
                record.auditState = -1
            }else{
                record.auditState = 1
            }
        },
        // 驳回标签点击事件
        rejectTagChange(record){
            if(record.auditState === 0){
                record.auditState = -1
            }else{
                record.auditState = 0
            }
        },
        // 通过按钮点击事件
        pass(){
            this.selectedRows.forEach(item => {
                item.auditState = 1
            })
            this.selectedRowKeys = []
            this.selectedRows = []
        },
        // 驳回按钮点击事件
        reject(){
            this.selectedRows.forEach(item => {
                item.auditState = 0
            })
            this.selectedRowKeys = []
            this.selectedRows = []
        },
        //双击表格行 可编辑
        doubleClick(record, index){
            return {
                onClick: (event) => {
                    this.selectedRowIndex = record.id
                },
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
        // 说明 输入框 回车事件
        inputPressEnter(record){

            let languageCode = workbenchCommon.languageMap[this.task.translateType].code
            // 长度校验
            let list = [eval("this.$refs.form"+ record.id.replaceAll('-','') + languageCode).validate()]
            Promise.all(list).then(() => {
                record[languageCode+"AuditSuggest"] = this.editableData[record.id][languageCode+"AuditSuggest"]
                record[languageCode] = this.editableData[record.id][languageCode]
                delete this.editableData[record.id]
            }).catch((err) => {
                
            })
        },
        afterClose(){
            this.editableData = {}
            this.selectedRows = []
            this.selectedRowKeys = []
            this.keyWords = ""
            // 解绑快捷键
            key.unbind('ctrl+down,ctrl+up,ctrl+shift+down,ctrl+shift+up,ctrl+e,ctrl+enter,ctrl+p,ctrl+r')
        },
        // 初始化快捷键
        initShortcutKeys(){
            let _this = this
            // 绑定快捷键
            key('ctrl+down',function(){ _this.nextEntry(); return false })
            key('ctrl+up',function(){ _this.prevEntry(); return false })
            key('ctrl+shift+down',function(){ _this.nextNotChecked(); return false })
            key('ctrl+shift+up',function(){ _this.prevNotChecked(); return false })
            key('ctrl+e',function(){ _this.editSelectRow(); return false })
            key('ctrl+enter',function(){ _this.enterEditEntry(); return false })
            key('ctrl+p',function(){ _this.translatePass(); return false })
            key('ctrl+r',function(){ _this.translateReject(); return false })
        },
        // 下一个词条 快捷键
        nextEntry(){
            let index = this.dataSource.findIndex(item => item.id === this.selectedRowIndex)
            if(index === this.dataSource.length - 1){
                return   
            }
            index++ 
            this.selectedRowIndex = this.dataSource[index].id
            this.scrollTableToRow(index)
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
        },
        // 下一个未审核
        nextNotChecked(){
            let index = this.dataSource.findIndex(item => item.id === this.selectedRowIndex)
            if(index === this.dataSource.length - 1){
                return   
            }
            let notTransIndex = index
            index++
            for(index; index < this.dataSource.length; index++){
                if(this.dataSource[index].auditState === -1){
                    notTransIndex = index
                    break
                }
            }
            this.selectedRowIndex = this.dataSource[notTransIndex].id
            this.scrollTableToRow(notTransIndex)
        },
        // 上一个未审核
        prevNotChecked(){
            let index = this.dataSource.findIndex(item => item.id === this.selectedRowIndex)
            if(index === 0){
                return   
            }
            let preNotTransIndex = index
            index--
            for(index;index >= 0;index--){
                if(this.dataSource[index].auditState === -1){
                    preNotTransIndex = index
                    break
                }
            }
            this.selectedRowIndex = this.dataSource[preNotTransIndex].id
            this.scrollTableToRow(preNotTransIndex)
        },
        // 编辑选中行
        editSelectRow(){
            if(this.selectedRowIndex === null){
                return
            }
            if(this.editableData.hasOwnProperty(this.selectedRowIndex)){
                // 编辑数据中包含该数据
                // eval("this.$refs.ref"+ this.selectedRowIndex.replaceAll('-','')).focus()
            }else{
                // 编辑数据中不包含该数据
                this.editableData[this.selectedRowIndex] = this.dataSource.find(item => item.id === this.selectedRowIndex)
                // this.$nextTick(() => {
                //     let input = eval("this.$refs.ref"+ this.selectedRowIndex.replaceAll('-',''))
                //     input.focus()
                // })
                
            }
        },
        // 确定编辑
        enterEditEntry(){
            let entry = this.dataSource.find(item => item.id === this.selectedRowIndex)
            entry = this.editableData[this.selectedRowIndex]
            delete this.editableData[this.selectedRowIndex]
        },
        // 审核通过快捷键
        translatePass(){
            if(this.selectedRowIndex === null){
                return
            }
            this.dataSource.find(item => item.id === this.selectedRowIndex).auditState = 1
        },
        // 审核拒绝快捷键
        translateReject(){
            if(this.selectedRowIndex === null){
                return
            }
            this.dataSource.find(item => item.id === this.selectedRowIndex).auditState = 0
        },
        // 滚动表格
        scrollTableToRow(rowIndex) {
            const table = this.$refs.tableContainer; // 获取表格容器元素
            if (table && rowIndex >= 0) {
                // 根据索引查找目标行元素
                const targetElement = table.$el.querySelectorAll('tr')[rowIndex]
                let container = this.$refs.tableContainer.$el.querySelector('.ant-table-body')
                if (targetElement) {
                    container.scrollTop = rowIndex * targetElement.offsetHeight - 370 // 当前行 * 行高 - 表格展示高度
                }
            }
        },
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
    .form{
        display: flex;
        align-items: center;
        align-self: stretch;
        width: 100%;
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
.ant-table-cell .ant-form-item{
    margin-bottom: 0%;
}
</style>