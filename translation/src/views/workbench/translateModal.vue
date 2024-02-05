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
            <div class="table">
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
                    <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="preTranslation">预翻译</a-button>
                    <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="save">保存</a-button>
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
                ref="taskTable"
                @resizeColumn="handleResizeColumn"
                :customRow="customRow"
                >
                    <template #bodyCell="{ column, text, record }">
                        <template v-if="column.dataIndex === 'translate'">
                            <div>
                                <template v-if="editableData[record.id]">
                                    <a-input
                                        v-model:value="editableData[record.id][column.dataIndex]"
                                        style="margin: -5px 0"
                                        @pressEnter="inputPressEnter(record)"
                                        @click="clickInput"
                                        @change="changeInput(record)"
                                    />
                                </template>
                                <template v-else>
                                    {{ text }}
                                </template>
                            </div>
                        </template>
                        
                    </template>
                </a-table>
            </div>
            <div class="suggest">
                <div style="margin-bottom: 6px;">翻译建议：</div>
                <div class="suggentContent">
                    <span class="title">本地翻译：</span>
                    <template v-for="(item,index) in suggest.local" :key="index">
                        <div class="suggentItem" @click="suggestClick(item.title,item.id)">
                            <div class="tran">
                                <img src="../../assets/icon/local.png" style="width:24px;height:24px;margin-right:8px"/>
                                <span>{{item.title}}</span>
                            </div>
                            <div class="tips">{{item.tips}}</div>
                        </div>
                    </template>
                    <span class="title">外网翻译：</span>
                    <template v-for="(item,index) in suggest.web" :key="index">
                        <div class="suggentItem" @click="suggestClick(item.title,item.id)">
                            <div class="tran">
                                <img :src="require('../../assets/icon/'+item.type+'.png')" style="width:24px;height:24px;margin-right:8px"/>
                                <span>{{item.title}}</span>
                            </div>
                            <div class="tips">{{item.tips}}</div>
                        </div>
                    </template>
                </div>
                <a-spin :spinning="spinning" tip="翻译中...."/>
            </div>
        </div>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import { cloneDeep } from 'lodash-es';
import {
    getEntryTempByTaskID,
    updateEntryTemp,
    preTranslate
} from '@/http/api/workbench'
import {
    translate
} from '@/http/api/entryManage'
import { message } from 'ant-design-vue';
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
            default:'词条翻译'
        },
        currentTask:{
            type:Object
        }
    },
    
    data() {
        return{
            modalWidth:"60%",
            task:{},
            keyWords:"",
            tableHeight: { x:'100%',y: '315px' },
            loading:false,
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:40,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left'},
                {title: '词条',dataIndex: 'entry',align:'center',width:100,fixed: 'left',resizable: true},
                // {title: 'Abbr',dataIndex: 'abbr',align:'center',width:100,resizable: true,index:2},
                {title: '来源',dataIndex: 'source',align:'center',width:100,resizable: true,ellipsis:true},
                {title: '翻译',dataIndex: 'translate',align:'center',width:100,ellipsis: true,resizable: true},
                {title: '说明',dataIndex: 'auditTransFeedback',align:'center',width:100,ellipsis: true,resizable: true},
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
        // 获取待翻译词条
        getTranslateEntry(){
            this.loading = true
            let params = {
                taskID: this.task.id,
                pageIndex: -1,
                pageSize: -1
            }
            getEntryTempByTaskID(params).then((res) => {
                this.dataSource = res.data.list
                this.allData = this.dataSource
                this.loading = false
            })
        },
        handleOK(){
            for (let key in this.editableData) {
				let entry = this.dataSource.find(item => item.id === key)
                entry.translate = this.editableData[key].translate
			}
            this.editableData = []
            updateEntryTemp(this.allData).then((res) => {
                message.success('翻译已保存！')
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
            this.editableData = []
            updateEntryTemp(this.allData).then((res) => {
                message.success('已保存！')
            })
        },
        // 添加表格行点击事件
        customRow(record, index){
            return {
                onClick: (event) => {
                    let _this = this
                    clearTimeout(this.timer)
                    this.timer = setTimeout(function () {
                        _this.selectedRowIndex = record.id
                        _this.assistedTranslation(record.entry)
                    }, 300);
                },
                onDblclick: (event) => {
                    clearTimeout(this.timer)
                    this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
                }
            }
        },
        handleResizeColumn: (w, col) => {
            col.width = w;
        },
        
        select(){
            this.dataSource = this.allData.filter(item => item.entry.includes(this.keyWords))
        },
        // 辅助翻译
        assistedTranslation(entry){
            this.spinning = true
            let params = {
                name: entry,
                type: this.task.translateType,
                department: this.task.department
            }
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
            })
        },
        suggestClick(title,id){
            if(this.selectedRowIndex === null){
                return
            }
            let record = this.dataSource.find(item => item.id === this.selectedRowIndex)
            record.translate = title
            record.translateID = id

            // 如果有子词条  则写入子词条
            if(record.children && record.children.length > 0){
                record.children.forEach(item => {
                    item.translate = title
                    item.translateID = id
                })
            }

            if(this.editableData[this.selectedRowIndex] != undefined){
                this.editableData[this.selectedRowIndex].translate = title
                this.editableData[this.selectedRowIndex].translateID = id

                // 如果有子词条  则写入子词条
                if(this.editableData[this.selectedRowIndex].children && this.editableData[this.selectedRowIndex].children.length > 0){
                    this.editableData[this.selectedRowIndex].children.forEach(item => {
                        item.translate = title
                        item.translateID = id
                    })
                }
            }
            
        },
        // 输入框 回车事件
        inputPressEnter(record){
            record.translate = this.editableData[record.id].translate
            // 如果有子词条  则写入子词条
            if(record.children && record.children.length > 0){
                record.children.forEach(item => {
                    item.translate = record.translate
                    item.translateID = record.translateID
                })
            }
            delete this.editableData[record.id]
        },
        // 预翻译
        preTranslation(){
            // message.info("预翻译")
            let params = {
                taskID: this.task.id
            }
            this.loading = true
            preTranslate(params).then((res) => {
                this.dataSource = res.data.list
                this.allData = this.dataSource
                this.loading = false
            })
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
            this.suggest = {
                local:[],
                web:[]
            }
            this.editableData = []
        }
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
        padding-top: 30px;
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
            max-height: 360px;

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

</style>