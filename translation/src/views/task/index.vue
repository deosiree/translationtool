<template>
    <div class="box" ref="box">
        <SearchBox ref="search">
            <template v-slot:form>
                <a-form
                    :model="search"
                    name="horizontal_login"
                    layout="inline"
                    autocomplete="off"
                    :label-col="labelCol"
                >
                    <a-form-item
                    label="任务名称"
                    name="name"
                    >
                        <a-input v-model:value="search.name" placeholder="请输入任务名称" size="small"></a-input>
                    </a-form-item>
                    <a-form-item
                    label="执行部门"
                    name="department"
                    >
                        <!-- <a-input v-model:value="search.department" placeholder="请输入执行部门" size="small"></a-input> -->
                        <a-select
                        v-model:value="search.department"
                        style="width: 186px"
                        placeholder="请选择执行部门"
                        :options='departments'
                        size="small"
                        >
                        </a-select>
                    </a-form-item>
                    <a-form-item
                    label="产品名称"
                    name="productName"
                    >
                        <a-input v-model:value="search.productId" placeholder="请输入产品名称" size="small"></a-input>
                    </a-form-item>
                    <a-form-item
                    label="开发员"
                    name="developer"
                    >
                        <a-input v-model:value="search.developer" placeholder="请输入开发员" size="small"></a-input>
                    </a-form-item>
                    <a-form-item
                    label="词条审核员"
                    name="auditor"
                    >
                        <a-input v-model:value="search.auditor" placeholder="请输入词条审核员" size="small"></a-input>
                    </a-form-item>
                    <a-form-item
                    label="翻译员"
                    name="translator"
                    >
                        <a-input v-model:value="search.translator" placeholder="请输入翻译员" size="small"></a-input>
                    </a-form-item>
                    <a-form-item
                    label="翻译审核员"
                    name="translationAuditor"
                    >
                        <a-input v-model:value="search.translationAuditor" placeholder="请输入翻译审核员" size="small"></a-input>
                    </a-form-item>
                    <a-form-item
                    label="翻译语种"
                    name="language"
                    >
                        <a-select
                        v-model:value="search.translateType"
                        style="width: 186px"
                        placeholder="请选择翻译语种"
                        :options='translateTypes'
                        :fieldNames="{label:'name',value:'code'}"
                        size="small"
                        >
                        </a-select>
                    </a-form-item>
                    <a-form-item
                    label="任务状态"
                    name="state"
                    >
                        <a-select
                        v-model:value="search.state"
                        style="width: 186px"
                        placeholder="请选择任务状态"
                        :options='states'
                        size="small"
                        >
                        </a-select>
                    </a-form-item>
                </a-form>
            </template>
            <template v-slot:operate>
                <a-button type="primary" size="middle" class="resetBtn" @click="reset">重置</a-button>
                <a-button type="primary" size="middle" @click="searchTaskInfo">查询</a-button>
            </template>
        </SearchBox>
        <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
            <template v-slot:operate>
                <div ref="button" v-if="true" style="margin-bottom:8px;display:flex;gap:8px">
                    <a-button type="primary" size="small" @click="handleAdd"><template #icon><PlusOutlined /></template>新增</a-button>
                    <a-button type="primary" size="small" @click="deleteTask"><template #icon><DeleteOutlined /></template>删除</a-button>
                    <a-popover v-model:visible="copyVisible" trigger="click" placement="bottom">
                        <template #content>
                            <a-input v-model:value="copyNumber" addon-before="复制" addon-after="条" type="number" style="width:180px" />
                            <div style="width:100%;margin-top:5px;display: flex;justify-content: center;">
                                <a @click="copy">确定</a>
                            </div>
                        </template>
                        <a-button type="primary" size="small"><template #icon><CopyOutlined /></template>复制</a-button>
                    </a-popover>
                    <a-button type="primary" size="small" @click="batchSave"><template #icon><SaveOutlined /></template>保存</a-button>
                    <a-button type="primary" size="small" class="resetBtn" @click="submitTask"><template #icon><SendOutlined /></template>下发任务</a-button>
                </div>
            </template>
            <template v-slot:data>
                <div style="width:100%;position: absolute;">
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
                    ref="taskTable"
                    @resizeColumn="handleResizeColumn"
                    :customRow="customRow"
                    >
                        <template #bodyCell="{ column, text, record }">
                            <template v-if="['name', 'description'].includes(column.dataIndex)">
                                <template v-if="editableData[record.id]">
                                    <a-input
                                        @click="clickInput"
                                        v-model:value="editableData[record.id][column.dataIndex]"
                                        style="margin: -5px 0"
                                    />
                                </template>
                                <template v-else>
                                    {{ text }}
                                </template>
                            </template>
                            <!-- <template v-if="'department' === column.dataIndex">
                                <template v-if="editableData[record.id]">
                                    <a-select
                                    v-model:value="editableData[record.id][column.dataIndex]"
                                    style="width: 100%"
                                    placeholder="请选择"
                                    :options='departments'
                                    @click="clickInput"
                                    @change="changeDepartment(record)"
                                    >
                                    </a-select>
                                </template>
                                <template v-else>
                                    {{ text }}
                                </template>
                            </template> -->
                            <template v-if="'productName' === column.dataIndex">
                                <template v-if="editableData[record.id]">
                                    <a-select
                                    v-model:value="editableData[record.id]['productId']"
                                    style="width: 85%"
                                    placeholder="请选择"
                                    :options='options[record.id]["products"]'
                                    :fieldNames='{label:"name",value:"id"}'
                                    @click="clickInput"
                                    @change="changeProduct(record)"
                                    >
                                    </a-select>
                                    <PlusCircleOutlined class="editable-cell-icon" style="color:#369FFF;margin-left:5px" @click.stop="addProduct(record)"/>
                                </template>
                                <template v-else>
                                    {{ text }}
                                </template>
                            </template>
                            <template v-if="'versionName' === column.dataIndex">
                                <template v-if="editableData[record.id]">
                                    <a-select
                                    v-model:value="editableData[record.id]['versionId']"
                                    style="width: 85%"
                                    placeholder="请选择"
                                    :options='options[record.id]["versions"]'
                                    @click="clickInput"
                                    >
                                    </a-select>
                                    <PlusCircleOutlined class="editable-cell-icon" style="color:#369FFF;margin-left:5px" @click.stop="addVersion(record)"/>
                                </template>
                                <template v-else>
                                    {{ text }}
                                </template>
                            </template>
                            <template v-if="'developer' === column.dataIndex">
                                <template v-if="editableData[record.id]">
                                    <a-select
                                    v-model:value="editableData[record.id][column.dataIndex]"
                                    style="width: 100%"
                                    placeholder="请选择"
                                    :options='options[record.id]["developers"]'
                                    :fieldNames="{label:'userName',value:'userName'}"
                                    @click="clickInput"
                                    >
                                    </a-select>
                                </template>
                                <template v-else>
                                    {{ text }}
                                </template>
                            </template>
                            <template v-if="'entryAuditor' === column.dataIndex">
                                <template v-if="editableData[record.id]">
                                    <a-select
                                    v-model:value="editableData[record.id][column.dataIndex]"
                                    style="width: 100%"
                                    placeholder="请选择"
                                    :options='options[record.id]["entryAuditors"]'
                                    :fieldNames="{label:'userName',value:'userName'}"
                                    @click="clickInput"
                                    >
                                    </a-select>
                                </template>
                                <template v-else>
                                    {{ text }}
                                </template>
                            </template>
                            <template v-if="'translator' === column.dataIndex">
                                <template v-if="editableData[record.id]">
                                    <a-select
                                    v-model:value="editableData[record.id][column.dataIndex]"
                                    style="width: 100%"
                                    placeholder="请选择"
                                    :options='options[record.id]["translators"]'
                                    :fieldNames="{label:'userName',value:'userName'}"
                                    @click="clickInput"
                                    >
                                    </a-select>
                                </template>
                                <template v-else>
                                    {{ text }}
                                </template>
                            </template>
                            <template v-if="'translationAuditor' === column.dataIndex">
                                <template v-if="editableData[record.id]">
                                    <a-select
                                    v-model:value="editableData[record.id][column.dataIndex]"
                                    style="width: 100%"
                                    placeholder="请选择"
                                    :options='options[record.id]["translatorAuditors"]'
                                    :fieldNames="{label:'userName',value:'userName'}"
                                    @click="clickInput"
                                    >
                                    </a-select>
                                </template>
                                <template v-else>
                                    {{ text }}
                                </template>
                            </template>
                            <template v-if="'translateType' === column.dataIndex">
                                <template v-if="editableData[record.id]">
                                    <a-select
                                    v-model:value="editableData[record.id][column.dataIndex]"
                                    style="width: 100%"
                                    placeholder="请选择"
                                    :options='translateTypes'
                                    :fieldNames="{label:'name',value:'name'}"
                                    @click="clickInput"
                                    >
                                    </a-select>
                                </template>
                                <template v-else>
                                    {{ text }}
                                </template>
                            </template>
                            <template v-if="column.dataIndex === 'state'">
                                <template v-if="record.state === '0'">
                                    <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
                                </template>
                                <template v-if="record.state > '0' && record.state < '6'">
                                    <a-badge color="#FBB31F" /><span style="color:#FBB31F">流程中</span>
                                </template>
                                <template v-if="record.state === '6'">
                                    <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
                                </template>
                            </template>
                            <template v-else-if="column.dataIndex === 'operation'">
                                <div class="editable-row-operations">
                                    <span v-if="editableData[record.id]">
                                        <a-button type="primary" ghost size="small" @click.stop="save(record.id)">保存</a-button>
                                        <!-- <a-popconfirm title="是否取消?" ok-text='是' cancel-text='否' @confirm="cancel">
                                            <a-button type="primary" ghost size="small" danger>取消</a-button>
                                        </a-popconfirm> -->
                                        <a-button type="primary" ghost size="small" danger @click.stop="cancel(record.id)">取消</a-button>
                                    </span>
                                    <span v-else>
                                        <a-button type="primary" ghost size="small" @click.stop="edit(record)">编辑</a-button>
                                    </span>
                                </div>
                            </template>
                        </template>
                    </a-table>
                </div>
            </template>
        </DataBox>
        <OperationArea 
        ref="operationArea" 
        :title="operationAreaTitle" 
        :height="operationAreaHeight"
        v-if="showOperationArea"
        @close="closeOperationArea"
        >
            <template v-slot:content>
                <TimeLine :showButton="false" :currentTask="currentTask" ref="timeLine"></TimeLine>
            </template>
        </OperationArea>
    </div>
    <ProductModal 
    :visible="addProductVisible" 
    :currentTask="addProductTask"
    @productClose="addProductClose"
    @productOk="addProductOk"
    />
    <VersionModal 
    :visible="addVersionVisible" 
    :currentVersion="addProductTask"
    @versionClose="addVersionClose"
    @versionOk="addVersionOk"
    />
</template>
<script>
import { message,Modal } from 'ant-design-vue';
import locale from 'ant-design-vue/es/date-picker/locale/zh_CN';
import SearchBox from '@/components/search/searchBox.vue'
import DataBox from '@/components/dataBox/index.vue'
import OperationArea from '@/components/operationArea/index.vue'
import TimeLine from '@/components/timeLine/index.vue'
import ProductModal from '@/views/task/productModal.vue'
import VersionModal from '@/views/task/versionModal.vue'
import { cloneDeep } from 'lodash-es';
import {
  PlusOutlined,
  DeleteOutlined,
  CopyOutlined,
  SaveOutlined,
  SendOutlined,
  PlusCircleOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue';
import { 
    searchTaskInfo,
    addTaskInfos,
    deleteTaskInfo,
    updateTaskInfo,
    taskSubmission
} from "@/http/api/task";
import { 
    getProduct
} from "@/http/api/product";
import { 
    getVersion
} from "@/http/api/productVersion";
import { 
    getRoleUserByDepartment,
    getDepartments
} from "@/http/api/user";
import { 
    getLanguage
} from "@/http/api/translate";
import { defineComponent, ref, createVNode } from 'vue';
export default {
    components:{
        SearchBox,
        DataBox,
        OperationArea,
        TimeLine,
        ProductModal,
        VersionModal,
        PlusOutlined,
        DeleteOutlined,
        CopyOutlined,
        SaveOutlined,
        SendOutlined,
        PlusCircleOutlined
    },
    data(){
        return{
            user:{
                userName:"",
                department:""
            },
            locale: locale,
            labelCol: { style: { width: '84px' } },
            search:{
                name: '',
                productId: null,
                translateType: null,
                department: null,
                state:null,
                developer:'',
                auditor: '',
                translator:'',
                translationAuditor:''
            },
            tableTitle:"任务列表",
            dataHeight:400,
            tableHeight: { x:'100%',y: 0 },
            loading:false,
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:50,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left'},
                {title: '任务名称',dataIndex: 'name',align:'center',width:150,fixed: 'left',resizable: true},
                {title: '执行部门',dataIndex: 'department',align:'center',width:150,resizable: true},
                {title: '产品名称',dataIndex: 'productName',align:'center',width:180,resizable: true},
                {title: '版本名称',dataIndex: 'versionName',align:'center',width:180,resizable: true},
                {title: '开发员',dataIndex: 'developer',align:'center',width:150,resizable: true},
                {title: '词条审核员',dataIndex: 'entryAuditor',align:'center',width:150,resizable: true},
                {title: '翻译员',dataIndex: 'translator',align:'center',width:150,resizable: true},
                {title: '翻译审核员',dataIndex: 'translationAuditor',align:'center',width:150,resizable: true},
                {title: '翻译语种',dataIndex: 'translateType',align:'center',width:150,resizable: true},
                {title: '任务描述',dataIndex: 'description',align:'center',width:150,ellipsis: true,resizable: true},
                {title: '任务状态',dataIndex: 'state',align:'center',width:100,fixed: 'right'},
                {title: '操作',dataIndex: 'operation',align:'center',width:150,fixed: 'right',index:100}
            ],
            dataSource:[],
            editableData:{},
            selectedRowKeys:[],
            selectedRows:[],
            selectedRowIndex:null,
            currentTask:{},
            options:{},
            operationAreaTitle:"流程显示区",
            operationAreaHeight:190,
            showOperationArea: false,
            timer:null,
            departments:[],
            copyVisible: false,
            copyNumber:1,
            states:[
                {label:"新建",value:'0'},
                {label:"流程中",value:'1,2,3,4,5'},
                {label:"已完成",value:'6'},
            ],
            translateTypes:[],
            addProductVisible: false,
            addProductTask:"",
            addVersionVisible: false
        }
    },
    mounted () {
        let _this = this
        this.$nextTick(() => {
            this.init()
            /** 控制table的高度 */
            window.onresize = function () {
                _this.setTableHeight()
            }
        })
    },
    unmounted() {
        //注销window.onresize事件
        window.onresize = null;
    },
    methods:{
        // 初始化
        init(){
            this.user = this.$store.state.user
            this.setTableHeight()
            this.searchTaskInfo()
            this.getDepartments()
            this.getLanguage()
        },
        // 动态设置表格高度
        setTableHeight(){
            this.$nextTick(() => {
                // 设置列表父元素高度
                let box = this.$refs.box.offsetHeight
                let searchHeight = this.$refs.search.$el.offsetHeight
                try {
                    let operationAreaHeight = this.$refs.operationArea.$el.offsetHeight
                    this.dataHeight = box - searchHeight - operationAreaHeight
                } catch (error) {
                    this.dataHeight = box - searchHeight
                }

                // 设置表格高度
                let buttonHeight = 0
                try {
                    buttonHeight = this.$refs.button.offsetHeight + 8
                } catch (error) {
                    
                }
                this.tableHeight.y = this.dataHeight - buttonHeight - 110

                // console.log(this.tableHeight.y)
            })
        },
        // 获取执行部门
        getDepartments(){
            getDepartments().then((res) => {
                this.departments = []
                res.data.list.forEach(item => {
                    let d = {
                        label: item,
                        value: item
                    }
                    this.departments.push(d)
                })
            })
        },
        // 获取翻译语言
        getLanguage(){
            let data = {}
            getLanguage(data).then((res) => {
                this.translateTypes = res.data.list
            })
        },
        // 获取任务列表
        searchTaskInfo(){
            this.loading = true
            let params = {
                pageIndex:-1,
                pageSize:-1
            }
            searchTaskInfo(this.search,params).then((res) => {
                this.dataSource = res.data.list
                this.loading = false
            })
        },
        clickInput(event){
            event.stopPropagation();
        },
        // 关闭流程操作区
        closeOperationArea(){
            this.showOperationArea = false
            this.setTableHeight()
            this.selectedRowIndex = null
        },
        // 设置表格每一行的class
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
        // 添加表格行点击事件
        customRow(record, index){
            return {
                onClick: (event) => {
                    let _this = this
                    clearTimeout(this.timer)
                    this.timer = setTimeout(function () {
                        _this.selectedRowIndex = record.id
                        _this.currentTask = record
                        // _this.$refs.timeLine.init()
                        _this.showOperationArea = true
                        _this.setTableHeight()
                    }, 300);
                },
                onDblclick: (event) => {
                    clearTimeout(this.timer)
                    // this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
                    this.edit(record)
                }
            }
        },
        //新增
        handleAdd(){
            const newData = {
                id: `new${this.dataSource.length + 1}`,
                name: '',
                state:'0',
                department:this.user.department
            };
            this.options[newData.id] = {
                products: [],
                version:[],
                developer:[]
            }
            this.dataSource.push(newData);
            this.editableData[newData.id] = newData;
            this.getOptions(newData)
            // 滚动到最底部
            this.$nextTick(()=>{
                let container = this.$refs.taskTable.$el.querySelector('.ant-table-body')
                container.scrollTop = container.scrollHeight
            })
        },
        // 查看流程
        edit(record){
            // 获取选择菜单数据
            this.getOptions(record)
            this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
        },
        // 保存
        save(id){
            // console.log(this.editableData[id])
            if(id.startsWith('new') || id.startsWith('copy')){
                // 新增
                let data = [this.editableData[id]]
                addTaskInfos(data).then((res) => {
                    message.success("新增成功！")
                    this.searchTaskInfo()
                    delete this.editableData[id];
                })
            }else{
                // 编辑
                updateTaskInfo(this.editableData[id]).then((res) => {
                    message.success("编辑成功！")
                    this.searchTaskInfo()
                    delete this.editableData[id];
                })
            }
        },
        // 批量保存
        batchSave(){
            let add = []
            let edit = []
            for(let key in this.editableData){
                // console.log(this.editableData[key])
                let id = this.editableData[key].id
                if(id.startsWith('new') || id.startsWith('copy')){
                    add.push(this.editableData[key])
                }else{
                    edit.push(this.editableData[key])
                }
            }
            if(add.length === 0 && edit.length === 0){
                return
            }
            Modal.confirm({
                title: '是否全部保存?',
                icon: createVNode(ExclamationCircleOutlined),
                okText: '确定',
                cancelText: '取消',
                onOk: () => {
                    if(add.length > 0){
                        // 新增接口
                        addTaskInfos(add).then((res) => {
                            // this.searchTaskInfo()
                        })
                    }
                    if(edit.length > 0){
                        // 修改接口
                        edit.forEach(item => {
                            updateTaskInfo(item).then((res) => {
                                
                            })
                        })
                    }
                    this.searchTaskInfo()
                    message.success("保存成功！")
                    this.editableData = {}
                }
            });
        },
        // 取消
        cancel(id){
            delete this.editableData[id];
            if(id.startsWith("new") || id.startsWith("copy")){
                //从dataSource中删除
                this.dataSource.some((item,i) => {
                    if(item.id === id){
                        this.dataSource.splice(i,1)
                        return true
                    }
                })
            }
        },
        deleteTask(){
            if(this.selectedRowKeys.length === 0){
                message.info('请选择需要删除的任务！')
                return
            }
            Modal.confirm({
                title: '是否确定删除?',
                icon: createVNode(ExclamationCircleOutlined),
                okText: '确定',
                cancelText: '取消',
                onOk: () => {
                    deleteTaskInfo(this.selectedRowKeys).then((res) => {
                        message.success("删除成功！")
                        this.searchTaskInfo()
                        this.selectedRowKeys = []
                        this.selectedRows = []
                    })
                }
            });
        },
        // 任务下发
        submitTask(){
            if(this.selectedRows.length === 0){
                message.info("请选择需要下发的任务！")
                return
            }
            Modal.confirm({
                title: '是否确定下发?',
                icon: createVNode(ExclamationCircleOutlined),
                okText: '确定',
                cancelText: '取消',
                onOk: () => {
                    let ids = []
                    this.selectedRows.forEach(item => {
                        if(item.state === '0'){
                            // 新建状态
                            ids.push(item.id)
                        }else{

                        }
                    })
                    if(ids.length === 0){
                        return
                    }
                    taskSubmission(ids).then((res) => {
                        this.searchTaskInfo()
                        message.success("下发成功！")
                        this.selectedRowKeys = []
                        this.selectedRows = []
                    })
                }
            });
            
        },
        // 获取可编辑行下拉菜单的选项
        getOptions(record){
            let products = []
            let versions = []
            let op = {
                products: products,
                versions: versions
            }
            this.options[record.id] = op
            // console.log(this.options[record.id])
            // 获取部门产品列表
            let product = {
                // department: record.department
                department: this.user.department
            }
            getProduct(product).then((res) => {
                
                this.options[record.id].products = res.data.list
            })
            // 获取产品版本列表
            if(record.productId != null){
                let version = {
                    productId:record.productId
                }
                getVersion(version).then((res) => {
                    res.data.list.forEach(item => {
                        let v = {
                            label: item.name,
                            value: item.id
                        }
                        this.options[record.id].versions.push(v)
                    });
                })
            }
            // 获取部门下的 开发员、词条审核员、翻译员、翻译审核员
            let params = {
                department:record.department
            }
            getRoleUserByDepartment(params).then((res) => {
                let data = res.data
                if(data.DEVELOPER){
                    this.options[record.id].developers = data.DEVELOPER
                }
                if(data.ENTRY_AUDITOR){
                    this.options[record.id].entryAuditors = data.ENTRY_AUDITOR
                }
                if(data.TRANSLATOR){
                    this.options[record.id].translators = data.TRANSLATOR
                }
                if(data.TRANSLATE_AUDITOR){
                    this.options[record.id].translatorAuditors = data.TRANSLATE_AUDITOR
                }
            })
            // console.log(this.options)
        },
        // 部门选择触发事件
        changeDepartment(record){
            // console.log(this.editableData[record.id])
            // 部门选择时  将产品和版本清空
            this.editableData[record.id].productId = null
            this.editableData[record.id].productName = null
            this.editableData[record.id].versionId = null
            this.editableData[record.id].versionName = null
            this.getOptions(this.editableData[record.id])
        },
        // 产品选择触发事件
        changeProduct(record){
            // 将版本清空
            this.editableData[record.id].versionId = null
            this.editableData[record.id].versionName = null
            this.getOptions(this.editableData[record.id])
        },
        // 复制
        copy(){
            this.copyVisible = false
            if(this.copyNumber < 1){
                this.copyNumber = 1
                return
            }
            if(this.selectedRows.length != 1){
                message.info("请选择一条需要复制的任务！")
                return
            }
            let task = this.selectedRows[0]
            // TODO 查询当前任务执行部门下的产品
            for(let i = 1; i <= this.copyNumber; i++){
                let copyTask = cloneDeep(task)
                let id = "copy_" + copyTask.id + "_" + i
                copyTask.id = id
                this.dataSource.push(copyTask)
                this.options[id] = {
                    products:[],
                    version:[],
                    developer:[],
                }
                this.editableData[id] = copyTask
                this.getOptions(this.editableData[id])
            }
            this.copyNumber = 1
            // 滚动到最底部
            this.$nextTick(()=>{
                let container = this.$refs.taskTable.$el.querySelector('.ant-table-body')
                container.scrollTop = container.scrollHeight
            })
        },
        // 表格列可伸缩
        handleResizeColumn: (w, col) => {
            col.width = w;
        },
        // 表格复选框选择事件
        onSelectChange(selectedRowKeys,selectedRows){
            this.selectedRowKeys = selectedRowKeys
            this.selectedRows = selectedRows
        },
        // 添加产品
        addProduct(record){
            // message.info("添加产品！")
            this.addProductTask = this.editableData[record.id]
            this.addProductVisible = true
        },
        addProductOk(record){
            this.addProductVisible = false
            this.getOptions(record)
        },
        addProductClose(){
            this.addProductVisible = false
        },
        // 添加版本
        addVersion(record) {
            let productId = this.editableData[record.id].productId
            if(productId === null || productId === '' || productId === undefined){
                message.info("请先选择产品！")
                return
            }
            this.addProductTask = this.editableData[record.id]
            this.addVersionVisible = true
        },
        addVersionOk(record){
            this.addVersionVisible = false
            this.getOptions(record)
        },
        addVersionClose(){
            this.addVersionVisible = false
        },
        // 重置
        reset(){
            this.search={
                name: '',
                productId: null,
                translateType: null,
                department: null,
                state:null,
                developer:'',
                auditor: '',
                translator:'',
                translationAuditor:''
            }
            this.searchTaskInfo()
        }
    }
}
</script>
<style scoped lang="less">
.box{
    width: 100%;
    height: 100%;
    // border: 1px solid red;
}
</style>
<style lang="less">
.editable-cell {
  position: relative;
  .editable-cell-input-wrapper,
  .editable-cell-text-wrapper {
    padding-right: 24px;
  }

  .editable-cell-text-wrapper {
    padding: 5px 24px 5px 5px;
  }

  .editable-cell-icon,
  .editable-cell-icon-check {
    position: absolute;
    right: 0;
    width: 20px;
    cursor: pointer;
  }

  .editable-cell-icon {
    margin-top: 4px;
    display: none;
  }

  .editable-cell-icon-check {
    line-height: 28px;
  }

  .editable-cell-icon:hover,
  .editable-cell-icon-check:hover {
    color: #108ee9;
  }

  .editable-add-btn {
    margin-bottom: 8px;
  }
}
.editable-cell:hover .editable-cell-icon {
  display: inline-block;
}
</style>