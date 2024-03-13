<template>
    <CustomModal
    :visible="visible" 
    :modalTitle="modalTitle"
    :modalWidth="modalWidth"
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
            <div class="form">
                词条：
                <a-input
                    v-model:value="keyWords"
                    style="width:300px"
                    size="small"
                    placeholder='请输入词条搜索'
                />
                <span style="margin-left:10px">词条状态：</span>
                <a-select
                v-model:value="entryState"
                size="small"
                style="width: 300px"
                >
                    <a-select-option value="1">待审核</a-select-option>
                    <a-select-option value="2">审核不通过</a-select-option>
                    <a-select-option value="3">审核通过</a-select-option>
                </a-select>
                <a-button type="primary" size="small" style="margin-left:8px" @click="getTaskEntry">查询</a-button>
                <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="pass">通过</a-button>
                <a-button type="primary" size="small" style="margin-left:8px" class="rejectBtn" @click="reject">驳回</a-button>
                <a-button type="primary" size="small" danger style="margin-left:8px" @click="deleteTaskEntry">删除</a-button>
                <!-- <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="aggregation">聚合</a-button>
                <a-button type="primary" size="small" style="margin-left:8px" class="yellowBtn" @click="cancelAggregation">取消聚合</a-button> -->
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
            :expandIconColumnIndex="2"
            ref="workTable"
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
                    <template v-if="['chineseInterpretation','englishInterpretation','auditSuggess','entryLabel'].includes(column.dataIndex)">
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
                    <template v-else-if="column.dataIndex === 'audit'">
                        <div class="editable-row-operations">
                            <span>
                                <a-checkable-tag :checked="record.auditState === 1" :class="record.auditState === 1 ? 'passTagChecked' : 'passTag' " @change="passTagChange(record)">通过</a-checkable-tag>
                                <a-checkable-tag :checked="record.auditState === 0" :class="record.auditState === 0 ? 'rejectTagChecked' : 'rejectTag'" @change="rejectTagChange(record)">驳回</a-checkable-tag>
                            </span>
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
</template>
<script>
import CustomModal from '@/components/modal/index.vue';
import { cloneDeep, iteratee } from 'lodash-es';
import {
    getEntryTempByTaskID,
    updateEntryTemp,
    getEntryInfoList,
    updateEntryList,
    deleteEntryInfoByID
} from '@/http/api/workbench'
import {
    CheckOutlined,
    CloseOutlined,
    ExclamationCircleOutlined,
    CaretDownOutlined,
    CaretRightOutlined
} from '@ant-design/icons-vue';
import { message ,Modal} from 'ant-design-vue';
import workbenchCommon from '@/views/workbench/common.js';
import common from '../entry/common';
import { defineComponent, ref, createVNode } from 'vue';
export default {
    components:{
        CheckOutlined,
        CloseOutlined,
        CaretDownOutlined,
        CaretRightOutlined,
        CustomModal
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
            keyWords:"",
            tableHeight: { x:'100%',y: '415px' },
            loading:false,
            columns: [
                {title: "序号",dataIndex: 'index',width:70,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left',align:'center'},
                {title: '存在状态',dataIndex: 'isExist',align:'center',width:100,fixed: 'left'},
                {title: '词条',dataIndex: 'entry',width:150,fixed: 'left',resizable: true},
                {title: 'Abbr',dataIndex: 'abbr',align:'center',width:150,resizable: true,index:2},
                {title: '翻译',dataIndex: 'translate',align:'center',width:200,ellipsis: true,resizable: true},
                {title: '中文释义',dataIndex: 'chineseInterpretation',align:'center',width:200,resizable: true},
                {title: '英文释义',dataIndex: 'englishInterpretation',align:'center',width:200,resizable: true},
                {title: 'TAG',dataIndex: 'entryLabel',align:'center',width:200},
                {title: '审核意见',dataIndex: 'auditSuggess',align:'center',width:200,resizable: true},
                {title: '词条状态',dataIndex: 'entryState',align:'center',width:100,fixed: 'right'},
                {title: '审核',dataIndex: 'audit',align:'center',width:100,ellipsis: true,fixed: 'right'},
            ],
            dataSource:[],
            allData:[],
            selectedRowKeys:[],
            selectedRows:[],
            editableData:{},
            rules:{},
            entryState:'1',
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
            })
        },
        // 获取待审核词条
        getTaskEntry(){
            let params = {
                taskID: this.task.id,
                entryState: this.entryState,
                entry: this.keyWords
            }
            this.loading = true
            getEntryInfoList(params,[]).then((res) => {
                this.dataSource = res.data.list
                // 排序  将已存在的词条放到前面
                this.dataSource.sort(function(a,b){
                    return b.isExist - a.isExist
                })
                this.dataSource.forEach(item => {
                    item.auditState = -1
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
            for (let key in this.editableData) {
				let entry = this.dataSource.find(item => item.id === key)
                entry.auditSuggess = this.editableData[key].auditSuggess
                entry[languageCode] = this.editableData[key][languageCode]

                entry.chineseInterpretation =  this.editableData[key].chineseInterpretation
                entry.englishInterpretation =  this.editableData[key].englishInterpretation
                entry.entryLabel = this.editableData[key].entryLabel

                if(entry[languageCode] != null && entry[languageCode] != null){
                    // 翻译存在  则状态为待审核状态
                    entry[languageCode+"TranslateState"] = '1'
                }
			}
            this.editableData = {}
            let params = {
                taskID: this.task.id
            }
            let updateArr = []
            this.dataSource.forEach(item => {
                if(item.auditState === 1){
                    // 词条审核通过
                    item.entryState = 3
                    updateArr.push(item)
                }else if(item.auditState === 0){
                    // 词条审核不通过
                    item.entryState = 2
                    updateArr.push(item)
                }
            })
            if(updateArr.length > 0){
                updateEntryList(params,updateArr).then((res) => {
                    message.success('已保存！')
                    this.getTaskEntry()
                }).catch((err) => {
                    message.error('保存失败！')
                })
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
                // onClick: (event) => {
                //     let _this = this
                //     clearTimeout(this.timer)
                    
                //     this.timer = setTimeout(function () {
                //         _this.selectedRowIndex = record.id
                //     }, 500);
                // },
                onDblclick: (event) => {
                    // clearTimeout(this.timer)
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

            record.chineseInterpretation =  this.editableData[record.id].chineseInterpretation
            record.englishInterpretation =  this.editableData[record.id].englishInterpretation
            record.entryLabel = this.editableData[record.id].entryLabel

            let languageCode = workbenchCommon.languageMap[this.task.translateType].code
            // 长度校验
            let list = [eval("this.$refs.form"+ record.id.replaceAll('-','') + languageCode).validate()]
            Promise.all(list).then(() => {
                record.auditSuggess = this.editableData[record.id].auditSuggess
                record[languageCode] = this.editableData[record.id][languageCode]

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
            record.auditEntryFeedback = this.editableData[record.id].auditEntryFeedback
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
                message.warn("请选择两条以上词条聚合！")
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
            
            // this.allData = this.dataSource
            this.selectedRowKeys = []
            this.selectedRows = []
        },
        // 取消聚合
        cancelAggregation(){
            // console.log(this.selectedRows)
            this.selectedRows.forEach(item => {
                if((item.parentID === '' || item.parentID ===null ) && item.children){
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
            // this.allData = this.dataSource
            this.selectedRowKeys = []
            this.selectedRows = []
        },
        afterClose(){
            this.editableData = {}
            this.selectedRows = []
            this.selectedRowKeys = []
            this.keyWords = ""
        },
        // 删除词条
        deleteTaskEntry(){
            if(this.selectedRows.length === 0){
                return
            }
            Modal.confirm({
                title: '是否确定删除?',
                icon: createVNode(ExclamationCircleOutlined),
                okText: '是',
                cancelText: '否',
                style:{top:'30%'},
                onOk: () => {
                    deleteEntryInfoByID(this.selectedRowKeys).then((res) => {
                        message.success('删除成功！')
                        this.getTaskEntry()
                    }).catch((err) => {
                        message.error('删除失败！')
                    })
                },
                onCancel: () => {
                    
                }
            });
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