<template>
    <div class="productVersionBox">
        <a-row type="flex">
            <a-col flex="240px" class="vesionBox">
                <div class="versionSearch">
                    <a-input v-model:value="keyWords" placeholder="关键字搜索" style="width:90%">
                        <template #suffix>
                            <SearchOutlined style="color: #DCDCDC;"/>
                        </template>
                    </a-input>
                    <a-tooltip placement="top">
                        <template #title>
                        <span>添加版本</span>
                        </template>
                        <PlusSquareOutlined style="color:#369fff;margin-left: auto;font-size:16px;" @click="addVersion"/>
                    </a-tooltip>
                </div>
                <div class="version">
                    <a-directory-tree
                    v-model:expandedKeys="expandedKeys"
                    :defaultExpandAll="true"
                    :selectedKeys="selectedTreeKeys"
                    :tree-data="versions"
                    :replaceFileds="{title:'name',key:'id'}"
                    @select="clickTree"
                    >
                        <template #title="{ key: treeKey, title,details}">
                            <template v-if="editVersion[treeKey]">
                                <a-input 
                                v-model:value="editVersion[treeKey]" 
                                size="small" 
                                style="width:80%"
                                @click="clickInput"
                                @pressEnter="editVersionOk(treeKey)"
                                >
                                </a-input>
                            </template>
                            <template v-else>
                                <a-dropdown :trigger="['contextmenu']">
                                    <span>{{ title }}</span>
                                    <template #overlay>
                                        <a-menu>
                                            <a-menu-item>导出</a-menu-item>
                                            <a-menu-item v-if="edit" @click="editVersionName(treeKey,title)">重命名</a-menu-item>
                                            <a-menu-item v-if="edit" @click="editVersionDetails(treeKey)">修改详情</a-menu-item>
                                            <a-menu-item v-if="edit">
                                                <a-popconfirm
                                                    title="确定要删除吗?"
                                                    ok-text="是"
                                                    cancel-text="否"
                                                    @confirm="deleteVersion(treeKey)"
                                                >删除
                                                </a-popconfirm>
                                            </a-menu-item>
                                        </a-menu>
                                    </template>
                                </a-dropdown>
                            </template>
                            <a-tooltip placement="top">
                                <template #title>
                                <span>备注：{{details}}</span>
                                </template>
                                <InfoCircleOutlined class="treeIcon" style="margin-left:5px;font-size:14px;margin-top:5px"/>
                            </a-tooltip>
                            <a-tooltip placement="top">
                                <template #title>
                                <span>查看</span>
                                </template>
                                <EyeOutlined class="treeIcon" @click.stop="viewEntrys(treeKey)"/>
                            </a-tooltip>
                        </template>
                    </a-directory-tree>
                </div>
            </a-col>
            <a-col flex="auto" class="taskBox">
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
                                <a-input v-model:value="search.name" placeholder="请输入内容"></a-input>
                            </a-form-item>
                            
                            <a-form-item
                            label="开发员"
                            name="developer"
                            >
                                <a-input v-model:value="search.developer" placeholder="请输入内容"></a-input>
                            </a-form-item>
                            <a-form-item
                            label="翻译员"
                            name="translator"
                            >
                                <a-input v-model:value="search.translator" placeholder="请输入内容"></a-input>
                            </a-form-item>
                            <a-form-item
                            label="翻译语种"
                            name="language"
                            >
                                <a-select
                                v-model:value="search.translateType"
                                style="width: 186px"
                                placeholder="请选择内容"
                                :options='translateTypes'
                                :fieldNames="{label:'name',value:'name'}"
                                >
                                </a-select>
                            </a-form-item>
                        </a-form>
                    </template>
                    <template v-slot:operate>
                        <a-button type="primary" size="middle" class="resetBtn" @click="reset">重置</a-button>
                        <a-button type="primary" size="middle" @click="getTaskList">查询</a-button>
                    </template>
                </SearchBox>
                <DataBox :title="tableTitle" :height="dataHeight" :showOperate="false">
                    <template v-slot:data>
                        <div style="width:100%;position: absolute;">
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
                            ref="taskTable"
                            @resizeColumn="handleResizeColumn"
                            :customRow="customRow"
                            >
                                <template #bodyCell="{ column, record }">
                                    <template v-if="column.dataIndex === 'operation'">
                                        <div class="editable-row-operations">
                                        <span>
                                            <a-button type="primary" ghost size="small" @click="exportTaskEntry(record)">导出</a-button>
                                        </span>
                                        </div>
                                    </template>
                                </template>
                            </a-table>
                        </div>
                    </template>
                </DataBox>
            </a-col>
        </a-row>
    </div>
    <VersionModal 
    :visible="versionVisible" 
    :modalTitle="versionModalTitle"
    :currentVersion="currentVersion"
    @versionClose="versionClose"
    ></VersionModal>
</template>
<script>
import SearchBox from '@/components/search/searchBox.vue'
import DataBox from '@/components/dataBox/index.vue'
import VersionModal from '@/views/entry/versionModal.vue'
import {
  SearchOutlined,
  EyeOutlined,
  PlusSquareOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue';
import { 
    getProductVersion
} from "@/http/api/product";
import { 
    updateVersion,
    deleteVersion
} from "@/http/api/productVersion";
import { 
    searchTaskInfo
} from "@/http/api/task";
import { 
    getLanguage
} from "@/http/api/translate";
import { message } from 'ant-design-vue';
export default {
    components:{
        SearchBox,
        DataBox,
        VersionModal,
        SearchOutlined,
        EyeOutlined,
        PlusSquareOutlined,
        InfoCircleOutlined
    },
    emits:['viewEntry'],
    props: {
        boxHeight:0,
        currentProduct:{},
        productEdit:false
    },
    data() {
        return{
            box:0,
            product:{},
            edit:false,// 用户对该产品是否有编辑权限
            keyWords:'',
            expandedKeys:[],
            selectedTreeKeys:[],
            versions:[],
            editVersion:{},
            search:{
                name:'',
                developer:'',
                translator:'',
                translateType:null
            },
            labelCol: { style: { width: '84px' } },
            tableTitle:'任务列表',
            dataHeight:200,
            tableHeight: { x:'100%',y: 0 },
            loading:false,
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:50,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left'},
                {title: '任务名称',dataIndex: 'name',align:'center',width:150,fixed: 'left',resizable: true},
                {title: '执行部门',dataIndex: 'department',align:'center',width:150,resizable: true},
                {title: '产品名称',dataIndex: 'productName',align:'center',width:180,resizable: true},
                {title: '版本',dataIndex: 'versionName',align:'center',width:180,resizable: true},
                {title: '词条数量',dataIndex: 'entryNum',align:'center',width:150,resizable: true},
                {title: '翻译语种',dataIndex: 'translateType',align:'center',width:150,resizable: true},
                {title: '开发员',dataIndex: 'developer',align:'center',width:150,resizable: true},
                {title: '词条审核员',dataIndex: 'entryAuditor',align:'center',width:150,resizable: true},
                {title: '翻译员',dataIndex: 'translator',align:'center',width:150,resizable: true},
                {title: '翻译审核员',dataIndex: 'translationAuditor',align:'center',width:150,resizable: true},
                {title: '任务描述',dataIndex: 'description',align:'center',width:150,resizable: true},
                {title: '下发时间',dataIndex: 'deliveryTime',align:'center',width:150,resizable: true},
                {title: '操作',dataIndex: 'operation',align:'center',fixed: 'right',width:80},
            ],
            dataSource:[],
            selectedRowIndex:null,
            translateTypes:[],
            versionVisible: false,
            versionModalTitle:'添加版本',
            currentVersion:{},
        }
    },
    
    created() {
        
    },
    mounted () {
        this.box = this.boxHeight
        this.product = this.currentProduct
        this.edit = this.productEdit
        this.setTableHeight()
        this.init()
    },
    watch: {
        boxHeight(newval,oldval){
            this.box = newval
            this.setTableHeight()
        },
        currentProduct(newval,oldval){
            this.product = newval
            this.init()
            // console.log(newval)
        },
        productEdit(newval,oldval){
            this.edit = newval
        }
    },
    methods: {
        init(){
            this.getProductVersion()
            this.getLanguage()
        },
        // 获取翻译语言
        getLanguage(){
            let data = {}
            getLanguage(data).then((res) => {
                this.translateTypes = res.data.list
            })
        },
        clickInput(event){
            event.stopPropagation();
        },
        // 动态设置表格高度
        setTableHeight(){
            this.$nextTick(() => {
                // 设置列表父元素高度
                let searchHeight = this.$refs.search.$el.offsetHeight
                this.dataHeight = this.box - searchHeight - 104

                // 设置表格高度
                let buttonHeight = 0
                try {
                    buttonHeight = this.$refs.button.offsetHeight + 8
                } catch (error) {
                    
                }
                this.tableHeight.y = this.dataHeight - buttonHeight - 110
            })
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
        // 历史版本点击事件
        clickTree(selectedKeys,e){
            // console.log(e)
            // console.log(selectedKeys)
            if(e.selected){
                this.selectedTreeKeys = selectedKeys
            }else{
                this.selectedTreeKeys = [e.node.key]
            }
            this.getTaskList()
        },
        // 查询任务
        getTaskList(){
            // console.log(this.selectedTreeKeys)
            if(this.selectedTreeKeys.length === 0){
                message.info("请选择版本！")
                return
            }
            this.loading = true
            // let version = this.versions.find(item => item.key === this.selectedTreeKeys[0])
            let data = {
                versionId: this.selectedTreeKeys[0],
                // tableName:version.tableName,
                name: this.search.name,
                developer: this.search.developer,
                translator: this.search.translator,
                translateType: this.search.translateType
            }
            let params = {
                pageIndex: -1,
                pageSize: -1
            }
            searchTaskInfo(data,params).then((res) => {
                // console.log(res)
                this.loading = false
                this.dataSource = res.data.list
            })
        },
        // 查看版本词条
        viewEntrys(treeKey){
            // message.info('查看版本词条')
            this.$emit('viewEntry',treeKey)
        },
        // 表格列可伸缩
        handleResizeColumn: (w, col) => {
            col.width = w;
        },
         // 添加表格行点击事件
        customRow(record, index){
            return {
                onClick: (event) => {
                    this.selectedRowIndex = record.id
                },
                onDblclick: (event) => {
                    
                }
            }
        },
        // 查询产品的所有版本
        getProductVersion(){
            let params = {
                productName:this.product.title,
                department:this.product.department
            }
            getProductVersion(params).then((res) => {
                let data = []
                res.data.list.forEach(element => {
                    let res = JSON.parse(
                        JSON.stringify(element)
                        .replace(/name/g, 'title')
                        .replace(/id/g, 'key')
                    );
                    data.push(res)
                });
                this.versions = data

                if(this.versions.length > 0){
                    let select = [this.versions[0].key]
                    this.selectedTreeKeys = select
                    this.getTaskList()
                }else{
                    this.dataSource = []
                }
            })
        },
        // 版本重命名
        editVersionName(key,title){
            this.editVersion[key] = title
        },
        editVersionOk(key){
            // 编辑版本
            let data = {
                id: key,
                name: this.editVersion[key]
            }
            updateVersion(data).then((res) => {
                message.success('重命名成功！')
                delete this.editVersion[key]
                this.getProductVersion()
            })
        },
        // 删除版本
        deleteVersion(key){
            let data = [key]
            deleteVersion(data).then((res) => {
                message.success('删除成功！')
                this.getProductVersion()
            })
        },
        // 添加版本
        addVersion(){
            this.currentVersion = {
                id:"",
                name:"",
                details:"",
                productId: this.product.key
            }

            this.versionVisible = true
        },
        // 修改详情
        editVersionDetails(key){
            let version = this.versions.find(item => item.key === key)
            
            this.currentVersion = JSON.parse(JSON.stringify(version)
                        .replace(/title/g, 'name')
                        .replace(/key/g, 'id'))
            this.versionModalTitle = '修改详情'
            this.versionVisible = true
        },
        // 版本弹窗关闭
        versionClose(){
            this.versionVisible = false
            this.getProductVersion()
        },
        reset(){
            this.search={
                name:'',
                translator:'',
                developer:'',
                translateType:null
            }
            this.getTaskList()
        },
        // 导出
        exportTaskEntry(record){
            message.info("导出！")
        }
    }
}
</script>
<style scoped lang="less">
.productVersionBox{
    padding: 0px 16px 16px 16px;
    width: 100%;
    height: 100%;
    // border: 1px solid red;

    .ant-row{
        height: 100%;
    }

    .vesionBox{
        display: flex;
        padding: 16px;
        flex-direction: column;
        align-items: flex-start;
        gap: 8px;
        align-self: stretch;
        border: 1px solid#DCDCDC;
        height: 100%;
        position: relative;

        .version{
            width: 100%;
            height: calc(100vh - 220px);
            overflow-y: auto;
        }

        .versionSearch{
            width: 100%;
            display:flex;
            align-items:center;
            justify-content:center;
        }
    }

    .taskBox{
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        // gap: 16px;
        flex: 1 0 0;
        align-self: stretch;
        // border: 1px solid#DCDCDC;
        border-left: none;
    }
}
.treeIcon{
    float:right;
    font-size:16px;
    margin-top:4px
}
</style>
<style scoped lang="less">
    :deep(.ant-tree-switcher){
        display: none;
    }
    :deep(.ant-tree){
        width: 100%;
    }
    :deep(.ant-tree .ant-tree-node-content-wrapper .ant-tree-iconEle){
        display: none;
    }
    :deep(.ant-tree.ant-tree-directory .ant-tree-treenode-selected::before){
        background-color: #EEF7FF;
    }
    :deep(.ant-tree.ant-tree-directory .ant-tree-treenode .ant-tree-node-content-wrapper.ant-tree-node-selected){
        color: #369FFF;
    }
</style>
