<template>
    <CustomModal 
    :modalWidth="modalWidth" 
    modalTitle="批量选择"
    :visible="visible"
    :showCancel="false"
    :fullFlag="true"
    cancelText="取消"
    okText="创建版本"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    @setTableHeight="setTableHeight"
    >
        <div style="width:100%;height:515px">
            <!-- <a-form
            :model="search"
            layout="inline"
            autocomplete="off"
            ref="formRef"
            >
                <a-form-item
                label="版本名称"
                name="versionName"
                :rules="[{ required: true, message: '请输入版本名称!' }]"
                >
                    <a-input v-model:value="search.versionName" placeholder="请输入版本名称"></a-input>
                </a-form-item>
            </a-form> -->
            <div class="table">
                <div>已选词条：</div>
                <a-config-provider :locale="locale">
                    <a-table 
                    class="ant-table-striped"
                    :columns="columns" 
                    :data-source="dataSource"
                    :scroll="tableHeight"
                    :pagination="pagination"
                    :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
                    ref="historyTable"
                    bordered>
                        <template #bodyCell="{ column, record }">
                            <template v-if="column.dataIndex === 'operation'">
                                <div class="editable-row-operations">
                                    <!-- <MinusSquareOutlined style="color:#369FFF;font-size:16px" @click="remove(record)"/> -->
                                    <DeleteOutlined style="color:#369FFF;font-size:16px" @click="remove(record)"/>
                                </div>
                            </template>
                            <template v-if="column.dataIndex === 'entryState'">
                                <template v-if="record.entryState === 0">
                                    <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
                                </template>
                                <template v-if="record.entryState === 1">
                                    <a-badge color="#FBB31F" /><span style="color:#FBB31F">审核中</span>
                                </template>
                                <template v-if="record.entryState === 2">
                                    <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
                                </template>
                                <template v-if="record.entryState === 3">
                                    <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
                                </template>
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
                </a-config-provider>
            </div>
        </div>
        <template v-slot:leftBottomBtn>
            <a-button @click="cancelCreate">取消选择</a-button>
            <a-button type="primary" @click="writeBackFun">回写</a-button>
            <a-button type="primary" danger @click="deleteEntrys">删除</a-button>
            <a-button type="primary" @click="exportExcel">导出Excel</a-button>
            <a-button type="primary" @click="examine">提交审核/翻译</a-button>
        </template>
    </CustomModal>
    <CustomModal
    :modalTitle="title"
    :modalWidth="operateWidth"
    :modalVisible="operateVisible"
    style="top: 30%"
    @handleClose="operateClose"
    @handleOK="operateOk"
    @afterClose="afterOperateClose"
    >
        <div style="width:100%;height:100%">
            <a-form
            v-if="title === '创建版本'"
            :model="version"
            autocomplete="off"
            ref="versionForm"
            :label-col="{ span: 6 }"
            >
                <a-form-item
                label="产品版本名称"
                name="versionName"
                :rules="[{ required: true, message: '请输入版本名称!' }]"
                >
                    <a-input v-model:value="version.versionName" placeholder="请输入版本名称"></a-input>
                </a-form-item>
                <a-form-item
                label="备注"
                name="remarks"
                >
                    <a-textarea v-model:value="version.remarks" placeholder="请输入备注" :rows="4" />
                </a-form-item>
            </a-form>
            <a-form
            v-if="title === '导出'"
            :model="exportClass"
            autocomplete="off"
            ref="exportForm"
            :label-col="{ span: 4 }"
            >
                <a-form-item
                label="导出字段"
                name="field"
                :rules="[{ required: true, message: '请选择导出字段!' }]"
                >
                    <a-select
                    mode="multiple"
                    v-model:value="exportClass.field"
                    :options="fieldOptions"
                    :fieldNames="{label:'label',value:'label'}"
                    placeholder="请选择"
                    ></a-select>
                </a-form-item>
            </a-form>
            <div class="table"  v-if="title === '选择任务'">
                <a-table 
                class="ant-table-striped"
                :columns="taskColumns" 
                :data-source="taskDataSource"
                :row-selection = 'taskRowSelection'
                :row-key="record => record.id"
                :scroll="{x:'100%' , y: '195px'}"
                :pagination="false"
                :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
                ref="taskTable"
                bordered>
                </a-table>
        
            </div>
            <a-form
            v-if="title === '回写'"
            :model="writeBack"
            autocomplete="off"
            ref="writeBack"
            :label-col="{ span: 4 }"
            >
                <a-form-item
                label="回写语言"
                name="language"
                :rules="[{ required: true, message: '请选择导出字段!' }]"
                >
                    <a-select
                    v-model:value="writeBack.language"
                    placeholder="请选择"
                    >
                        <a-select-option value="英文">英文</a-select-option>
                        <a-select-option value="俄文">俄文</a-select-option>
                        <a-select-option value="西文">西文</a-select-option>
                        <a-select-option value="法文">法文</a-select-option>
                    </a-select>
                </a-form-item>
                <a-form-item
                label="回写TAG"
                name="isTag"
                >
                    <a-switch v-model:checked="writeBack.isTag" checked-children="是" un-checked-children="否" />
                </a-form-item>
                <a-form-item
                label="回写来源"
                name="isComment"
                >
                    <a-switch v-model:checked="writeBack.isComment" checked-children="是" un-checked-children="否" />
                </a-form-item>
                
            </a-form>
        </div>
        
    </CustomModal>
</template>
<script>
import CustomModal from '@/components/modal/index.vue';
import zh_CN from 'ant-design-vue/es/locale/zh_CN';
import tableParam from "./tableParam.js";
import {
  MinusSquareOutlined,
  ExclamationCircleOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue';
import { message, Modal } from 'ant-design-vue';
import { defineComponent, ref, createVNode } from 'vue';
import {
    deleteEntryInfoByID
} from '@/http/api/workbench.js'
import {
    createVersionByEntry,
    addProductRelation,
    updateEntryInfo
} from '@/http/api/entryManage'
import {
    entryExportByCondition
} from '@/http/api/download'
import {
    searchTaskInfo
} from '@/http/api/task'
import {
    setInfo
} from '@/http/api/i18Server'
export default {
    components:{
        CustomModal,
        MinusSquareOutlined,
        ExclamationCircleOutlined,
        DeleteOutlined
    },
    emits:['createClose','removeEntry','cancelCreate'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        dataSource:{
            type: Array
        },
        currentProduct:{
            type: Object
        }
    },
    
    data() {
        return{
            locale: zh_CN,
            modalWidth:"60%",
            tableHeight:{x:'100%' , y: 395},
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:60,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left'},
                {title: '词条状态',dataIndex: 'entryState',align:'center',width:130,fixed:'left'},
                {title: 'Abbr',dataIndex: 'abbr',align:'center',width:180,fixed: 'left'},
                {title: '词条',dataIndex: 'entry',align:'center',width:180,},
                {title: '中文释义',dataIndex: 'chineseInterpretation',align:'center',width:180,},
                {title: '英文释义',dataIndex: 'englishInterpretation',align:'center',width:180,},
                {title: '英文翻译',dataIndex: 'english',align:'center',width:180,},
                {title: '英文翻译状态',dataIndex: 'englishTranslateState',align:'center',width:180,},
                {title: '西文翻译',dataIndex: 'spanish',align:'center',width:180,},
                {title: '西文翻译状态',dataIndex: 'spanishTranslateState',align:'center',width:180,},
                {title: '俄文翻译',dataIndex: 'russian',align:'center',width:180,},
                {title: '俄文翻译状态',dataIndex: 'russianTranslateState',align:'center',width:180,},
                {title: '法文翻译',dataIndex: 'french',align:'center',width:180,},
                {title: '法文翻译状态',dataIndex: 'frenchTranslateState',align:'center',width:180,},
                {title: '删除',dataIndex: 'operation',align:'center',width:50,fixed: 'right'}
            ],
            version:{
                language:null,
                versionName:'',
                remarks:""
            },
            pagination:{
                pageSizeOptions:['20','50','100'],
                defaultPageSize:20,
            },
            title:"",
            operateVisible: false,
            operateWidth:'500px',
            exportClass:{
                field:["abbr","词条"]
            },
            fieldOptions:tableParam.exportFields,
            product:{},
            taskColumns:[
                {title: "序号",dataIndex: 'index',align:'center',width:70,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left'},
                {title: '任务名称',dataIndex: 'name',align:'center',width:150,fixed: 'left',resizable: true},
                {title: '产品名称',dataIndex: 'productName',align:'center',width:230,resizable: true},
                {title: '版本名称',dataIndex: 'versionName',align:'center',width:180,resizable: true},
                {title: '翻译语种',dataIndex: 'translateType',align:'center',width:150},
                {title: '开发员',dataIndex: 'developer',align:'center',width:150},
                {title: '词条审核员',dataIndex: 'entryAuditor',align:'center',width:150},
                {title: '翻译员',dataIndex: 'translator',align:'center',width:150},
                {title: '翻译审核员',dataIndex: 'translationAuditor',align:'center',width:150},
                {title: '任务描述',dataIndex: 'description',align:'center',width:230,ellipsis: true,resizable: true},
                {title: '下发时间',dataIndex: 'deliveryTime',align:'center',width:200},
            ],
            taskDataSource:[],
            selectedTaskRows:[],
            writeBack:{
                language:null,
                isTag:null,
                isComment: null
            }
        }
    },
    
    created() {
        this.product = this.currentProduct
    },
    mounted () {

    },
    computed:{
        taskRowSelection(){
            return {
                type:'radio',
                onChange:(selectedRowKeys,selectedRows) => {
                    // console.log(selectedRowKeys)
                    // console.log(selectedRows)
                    this.selectedTaskRows = selectedRows
                }
            }
        },
    },
    watch: {
        currentProduct(newval,oldval){
            this.product = newval
        }
    },
    methods: {
        
        handleClose(){
            this.$emit("createClose")
        },
        // 创建版本
        handleOK(){
            
            this.operateVisible = true
            this.operateWidth = '500px'
            this.title = "创建版本"
        },
        remove(record){
            this.$emit("removeEntry",record)
        },
        cancelCreate(){
            Modal.confirm({
                title: '是否确认取消选择?',
                icon: createVNode(ExclamationCircleOutlined),
                content: '确认取消后，已选择的词条将被清空',
                okText: '是',
                cancelText: '否',
                style:{top:'30%'},
                onOk: () => {
                    this.$emit("cancelCreate")
                }
            });
        },
        afterClose(){
            this.search = {
                versionName: "",
                language: null
            }
            // this.$refs.formRef.clearValidate()
        },
        // 导出Excel
        exportExcel(){
            this.operateVisible = true
            this.operateWidth = '500px'
            this.title = "导出"
        },
        // 提交审核/翻译
        examine(){
            this.operateVisible = true
            this.operateWidth = '50%'
            this.title = "选择任务"
            this.getTaskList()
        },
        // 回写
        writeBackFun(){
            this.operateVisible = true
            this.operateWidth = '500px'
            this.title = "回写"
        },
        // 获取该产品下的任务
        getTaskList(){
            let params = {
                pageIndex: -1,
                pageSize: -1
            }
            let data = {
                productId: this.product.key,
                state:'1,2,3,4,5'
            }
            searchTaskInfo(data,params).then((res) => {
                this.taskDataSource = res.data.list
            })
        },
        operateClose(){
            this.operateVisible = false
        },
        operateOk(){
            if(this.title === '创建版本'){
                this.$refs.versionForm.validate().then(() => {
                    // TODO 创建版本接口
                    let params = {
                        productID:this.product.key,
                        versionName:this.version.versionName,
                        common:this.version.remarks
                    }
                    createVersionByEntry(params,this.dataSource).then((res) => {
                        message.success('创建版本完成！')
                        this.operateVisible = false
                        this.$emit("createClose")
                        this.$emit("cancelCreate")
                    }).catch((err) => {
                        message.error("创建失败！")
                    })
                    
                })
            }else if(this.title === '导出'){
                this.$refs.exportForm.validate().then(() => {
                    // 导出接口
                    let fields = ['id'].concat(this.exportClass.field)
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
                        this.operateVisible = false
                        this.$emit("createClose")
                        this.$emit("cancelCreate")
                    })
                })
            }else if(this.title === '选择任务'){
                //提交审核/翻译
                if(this.selectedTaskRows.length === 0){
                    message.warn('请选择任务！')
                    return
                }
                let params = {
                    notes:""
                }
                // 修改词条状态
                this.dataSource.forEach(item => {
                    if(item.entryState === 0){
                        item.entryState = 1
                        updateEntryInfo(item,params).then((res) => {

                        })
                    }
                })
                // 将词条提交到任务
                let data = []
                this.dataSource.forEach(item => {
                    let info = {
                        entryId: item.id,
                        productId: this.product.key,
                        taskId: this.selectedTaskRows[0].id,
                        versionId: this.selectedTaskRows[0].versionId
                    }
                    data.push(info)
                })
                addProductRelation(data).then((res) => {
                    message.success('已提交！')
                    this.operateVisible = false
                    this.$emit("createClose")
                    this.$emit("cancelCreate")
                }).catch((err) => {
                    message.error('提交失败！')
                })
            }else if(this.title === '回写'){
                let params = {
                    translateType: this.writeBack.language,
                    isTag: this.writeBack.isTag ? 1 : 0,
                    isComment: this.writeBack.isComment ? 1 : 0
                }
                setInfo(params,this.dataSource).then((res) => {
                    message.success('回写成功！')
                    this.operateVisible = false
                    this.$emit("createClose")
                    this.$emit("cancelCreate")
                }).catch((err) => {
                    message.error('回写失败！')
                })
            }
            
        },
        // 删除词条
        deleteEntrys(){
            Modal.confirm({
                title: '是否确定删除?',
                icon: createVNode(ExclamationCircleOutlined),
                okText: '是',
                cancelText: '否',
                style:{top:'30%'},
                onOk: () => {
                    let ids = []
                    this.dataSource.forEach(item => {
                        ids.push(item.id)
                    })
                    if(ids.length > 0){
                        deleteEntryInfoByID(ids).then((res) => {
                            message.success('已删除！')
                            this.$emit("createClose")
                            this.$emit("cancelCreate")
                        })
                    }
                }
            });
        },
        
        afterOperateClose(){
            this.version = {versionName:"",remarks:""}
            this.exportClass = {field:["abbr","词条"]}
            this.writeBack = {
                language:null,
                isTag:null,
                isComment: null
            }
        },
        // 动态设置表格高度
        setTableHeight(height,type){
            if(type === 'full'){
                this.tableHeight.y = height - 150
            }else if(type === 'reduce'){
                this.tableHeight.y = 395
            }
        },
    }
}
</script>
<style lang="less" scoped>
.table{
    width: 100%;
    margin-top: 5px;
    position: relative;
}
.ant-form-inline .ant-form-item-with-help {
    margin-bottom: 0px;
}
</style>