<template>
    <Modal
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
            <div class="form">
                词条展示：
                <a-input
                    v-model:value="keyWords"
                    style="width:50%"
                    size="small"
                    placeholder='请输入关键词搜索'
                />
                <a-button type="primary" size="small" style="margin-left:8px" @click="select">查询</a-button>
                <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="pass">通过</a-button>
                <a-button type="primary" size="small" style="margin-left:8px" class="rejectBtn" @click="reject">驳回</a-button>
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
                    <template v-if="column.dataIndex === 'auditEntryFeedback'">
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
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import { cloneDeep, iteratee } from 'lodash-es';
import {
    getEntryTempByTaskID,
    updateEntryTemp
} from '@/http/api/workbench'
import {
    CheckOutlined,
    CloseOutlined,
    ExclamationCircleOutlined,
    CaretDownOutlined,
    CaretRightOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
export default {
    components:{
        CheckOutlined,
        CloseOutlined,
        CaretDownOutlined,
        CaretRightOutlined,
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
            default:'词条导入'
        },
        currentTask:{
            type:Object
        }
    },
    
    data() {
        return{
            modalWidth:"50%",
            task:{},
            keyWords:"",
            tableHeight: { x:'100%',y: '315px' },
            loading:false,
            columns: [
                {title: "序号",dataIndex: 'index',width:70,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left'},
                {title: '词条',dataIndex: 'entry',width:150,fixed: 'left',resizable: true},
                // {title: 'Abbr',dataIndex: 'abbr',align:'center',width:100,resizable: true,index:2},
                {title: '来源',dataIndex: 'source',align:'center',width:150,resizable: true,ellipsis:true},
                {title: '翻译',dataIndex: 'translate',align:'center',width:150,ellipsis: true,resizable: true},
                {title: '审核',dataIndex: 'audit',align:'center',width:100,ellipsis: true,},
                {title: '说明',dataIndex: 'auditEntryFeedback',align:'center',width:100,ellipsis: true,resizable: true},
            ],
            dataSource:[],
            allData:[],
            selectedRowKeys:[],
            selectedRows:[],
            editableData:{}
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
        },
    },
    methods: {
        // 获取待审核词条
        getTaskEntry(){
            let params = {
                taskID: this.task.id,
                pageIndex: -1,
                pageSize: -1
            }
            this.loading = true
            getEntryTempByTaskID(params).then((res) => {
                this.dataSource = res.data.list
                this.allData = this.dataSource
                
                this.loading = false
            })
        },
        handleOK(){
            // console.log(this.allData)
            for (let key in this.editableData) {
				let entry = this.dataSource.find(item => item.id === key)
                entry.translate = this.editableData[key].translate
                entry.auditEntryFeedback = this.editableData[key].auditEntryFeedback
			}
            this.editableData = []
            updateEntryTemp(this.allData).then((res) => {
                message.success('审核完成！')
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
            if(record.auditState === 0){
                record.auditState = 1
            }
        },
        // 驳回标签点击事件
        rejectTagChange(record){
            if(record.auditState === 1){
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
        // 说明 输入框 回车事件
        inputPressEnter(record){
            record.auditEntryFeedback = this.editableData[record.id].auditEntryFeedback
            record.translate = this.editableData[record.id].translate
            delete this.editableData[record.id]
            // this.deleteOperationColumns()
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
            
            this.allData = this.dataSource
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
            this.allData = this.dataSource
            this.selectedRowKeys = []
            this.selectedRows = []
        },
        afterClose(){
            this.editableData = {}
            this.selectedRows = []
            this.selectedRowKeys = []
            this.keyWords = ""
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
</style>