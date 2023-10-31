<template>
    <div style="100%;height:100%;background-color:#F3F3F3">
        <a-row type="flex" :gutter="8">
            <a-col :flex="classifyWidth">
                <div class="classify">
                    <div class="classifyTitle">
                        <span v-if="showClassify">分类列表</span>
                        <!-- <img src="../../assets/icon/closeClassify.png" title="关闭" v-if="showClassify" @click="closeOrOpenClassify('close')"/>
                        <img src="../../assets/icon/openClassify.png" title="展开" v-if="!showClassify" @click="closeOrOpenClassify('open')"/> -->
                    </div>
                    <div class="treeBox">
                        <a-tree
                        show-icon
                        v-if="showClassify" 
                        v-model:expandedKeys="expandedKeys"
                        :defaultExpandAll="true"
                        :selectedKeys="selectedTreeKeys"
                        :tree-data="treeData"
                        @select="clickTree"
                        draggable
                        block-node
                        @dragenter="onDragEnter"
                        @drop="onDrop"
                        >
                            <template #title="{ key: treeKey, title,maxByte }">
                                <a-dropdown :trigger="['contextmenu']">
                                    <span>{{ title }}</span>
                                    <template #overlay>
                                        <a-menu>
                                            <a-menu-item @click="onContextMenuClick(treeKey, 'addChild',title)">添加子类</a-menu-item>
                                            <a-menu-item @click="onContextMenuClick(treeKey, 'edit',title,maxByte)">编辑</a-menu-item>
                                            <a-menu-item>
                                                <a-popconfirm
                                                    title="确定要删除该分类及其子类吗?"
                                                    ok-text="是"
                                                    cancel-text="否"
                                                    @confirm="onContextMenuClick(treeKey, 'delete',title)"
                                                >删除
                                                </a-popconfirm>
                                            </a-menu-item>
                                        </a-menu>
                                    </template>
                                </a-dropdown>
                            </template>
                            <!-- <template #icon>
                                <img src="../../assets/icon/checkbox.png" />
                            </template> -->
                        </a-tree>
                    </div>
                    <div class="addBox" v-if="showClassify">
                        <a-button type="primary" ghost style="width:100%;height:100%" @click="addRootClassify">新增根节点</a-button>
                    </div>
                </div>
            </a-col>
            <a-col flex="auto">
                <div class="content" ref="box">
                    <SearchForm ref="searchForm"  @change="setTableHeight">
                        <template v-slot:form>
                            <a-form
                            :model="search"
                            layout="inline"
                            autocomplete="off"
                            :label-col="labelCol"
                            >
                                <a-form-item
                                label="中文术语"
                                name="chinese"
                                >
                                    <a-input v-model:value="search.chinese" placeholder="请输入中文术语" size="small"></a-input>
                                </a-form-item>
                                <a-form-item
                                label="Abbr"
                                name="abbr"
                                >
                                    <a-input v-model:value="search.abbr" placeholder="请输入Abbr" size="small"></a-input>
                                </a-form-item>
                                <a-form-item
                                label="词性"
                                name="partOfSpeech"
                                >
                                    <!-- <a-input v-model:value="search.partOfSpeech" placeholder="请输入词性备注" size="small"></a-input> -->
                                    <a-select
                                        v-model:value="search.partOfSpeech"
                                        placeholder="请选择词性备注"
                                        :options='partSpeechs'
                                        size="small"
                                        >
                                    </a-select>
                                </a-form-item>
                                <a-form-item
                                label="版本"
                                name="version"
                                >
                                    <a-select
                                    v-model:value="search.version"
                                    placeholder="请选择版本"
                                    size="small"
                                    >
                                        <template v-for="(item,index) in versions" :key="index">
                                            <a-select-option :value="item.name">{{item.name}}</a-select-option>
                                        </template>
                                    </a-select>
                                </a-form-item>
                                <a-form-item
                                label="创建人"
                                name="creator"
                                >
                                    <a-input v-model:value="search.creator" placeholder="请输入创建人" size="small"></a-input>
                                </a-form-item>
                                <a-form-item
                                label="创建日期"
                                name="createTime"
                                >
                                    <a-range-picker v-model:value="createTime" 
                                    size="small" 
                                    :locale="locale"
                                    @change="changePicker"
                                    />
                                </a-form-item>
                            </a-form>
                        </template>
                        <template v-slot:operate>
                            <div style="margin-top:5px;text-align: right;">
                                <a-button type="primary" size="middle" @click="searchBtn">查询</a-button>
                                <a-button type="primary" size="middle" style="margin-left:10px;background-color:#36BF7D;border:#36BF7D" @click="reset">重置</a-button>
                            </div>
                        </template>
                    </SearchForm>
                    <div class="operateBtn">
                        <a-button type="primary" size="small" style="margin-left:10px" @click="addEntry">
                            <template #icon><PlusOutlined /></template>
                            新增
                        </a-button>
                        <a-button type="primary" size="small" style="margin-left:10px" @click="deleteEntry">
                            <template #icon><DeleteOutlined /></template>
                            删除
                        </a-button>
                        <a-button type="primary" size="small" style="margin-left:10px" @click="mergeEntry">
                            <template #icon><LinkOutlined /></template>
                            合并
                        </a-button>
                        <!-- <a-button type="primary" size="small" style="margin-left:10px">
                            <template #icon><DownloadOutlined /></template>
                            词条导入
                        </a-button> -->
                        <ImportDataBtn />
                        <a-button type="primary" size="small" style="margin-left:10px" @click="exportData">
                            <template #icon><UploadOutlined /></template>
                            导出
                        </a-button>
                        <!-- <a-button type="primary" size="small" style="margin-left:10px">
                            <template #icon><HistoryOutlined /></template>
                            版本库
                        </a-button> -->
                        <!-- <AddRepository :size="repositoryBtnSize" :title="repositoryBtnTitle" :icon="repositoryBtnIcon"/> -->
                        <a-popover
                            trigger="click"
                            placement="leftBottom"
                            :overlayStyle="overlayStyle"
                        >
                            <template #content>
                                <a-checkbox-group
                                    v-model:value="checkedColumn"
                                    @change="changeColumn"
                                >
                                    <a-row
                                        v-for="item in checkboxList"
                                        :key="item.value"
                                    >
                                        <a-col :span="24">
                                            <a-checkbox :value="item.value">
                                                {{ item.label }}
                                            </a-checkbox>
                                        </a-col>
                                    </a-row>
                                </a-checkbox-group>
                            </template>
                            <a-button type="primary" size="small" style="margin-left:10px">
                                <template #icon><SettingOutlined /></template>
                                显示列设置
                            </a-button>
                        </a-popover>
                    </div>
                    <div class="table">
                        <a-form ref="formRef" :model="editableData[checkRecordId]" >
                            <a-table 
                            class="ant-table-striped"
                            :customRow="doubleClick"
                            :columns="columns" 
                            :data-source="dataSource" 
                            :loading="loading"
                            :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
                            :row-key="record => record.id"
                            :scroll="tableHeight"
                            :pagination='false'
                            :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
                            ref="entryTable"
                            @resizeColumn="handleResizeColumn"
                            bordered>
                                <template #bodyCell="{ column, record, text }">
                                    <template v-if="inputColumn.includes(column.dataIndex)">
                                        <div>
                                            <template v-if="editableData[record.id]">
                                                <a-input
                                                    v-model:value="editableData[record.id][column.dataIndex]"
                                                    style="margin: -5px 0"
                                                />
                                            </template>
                                            <template v-else>
                                                {{ text }}
                                            </template>
                                        </div>
                                    </template>
                                    <template v-if='translateColumn.includes(column.dataIndex)'>
                                        <div>
                                            <template v-if="editableData[record.id]">
                                                <a-form-item :name="column.dataIndex" :rules="[{validator: this.checkField}]">
                                                    <a-input
                                                        v-model:value="editableData[record.id][column.dataIndex]"
                                                        @focus="tranlateInputChange(record)"
                                                        style="margin: -5px 0"
                                                    />
                                                </a-form-item>
                                            </template>
                                            <template v-else>
                                                {{ text }}
                                            </template>
                                        </div>
                                    </template>
                                    <template v-if="column.dataIndex === 'entryLabel'">
                                        <div>
                                            <template v-if="editableData[record.id]">
                                                <!-- <a-select
                                                    v-model:value="editableData[record.id][column.dataIndex]"
                                                    :options='entryLabelOptions'
                                                    style="width:100%"
                                                    mode="SECRET_COMBOBOX_MODE_DO_NOT_USE"
                                                >
                                                </a-select> -->
                                                <a-select
                                                v-model:value="editableData[record.id]['labelList']"
                                                mode="multiple"
                                                :max-tag-count="maxTagCount"
                                                style="width: 100%"
                                                placeholder="请选择标签"
                                                :options='entryLabelOptions'
                                                >
                                                </a-select>
                                            </template>
                                            <template v-else>
                                                {{ text }}
                                            </template>
                                        </div>
                                    </template>
                                    <template v-if="column.dataIndex === 'partOfSpeech'">
                                        <div>
                                            <template v-if="editableData[record.id]">
                                                <a-select
                                                v-model:value="editableData[record.id][column.dataIndex]"
                                                placeholder="请选择词性备注"
                                                :options='partSpeechs'
                                                style="width:100%"
                                                >
                                                </a-select>
                                            </template>
                                            <template v-else>
                                                {{ text }}
                                            </template>
                                        </div>
                                    </template>
                                    <!-- <template v-if="column.dataIndex === 'version'">
                                        <div>
                                            <template v-if="editableData[record.id]">
                                                <a-select
                                                v-model:value="editableData[record.id][column.dataIndex]"
                                                style="width:100%"
                                                >
                                                    <template v-for="(item,index) in versions" :key="index">
                                                        <a-select-option :value="item.name">{{item.name}}</a-select-option>
                                                    </template>
                                                </a-select>
                                            </template>
                                            <template v-else>
                                                {{ text }}
                                            </template>
                                        </div>
                                    </template> -->
                                    <template v-if="column.dataIndex === 'entryState'">
                                        <template v-if="record.entryState === -1">
                                            <a-badge color="red" /><span style="color:red">已删除</span>
                                        </template>
                                        <template v-if="record.entryState === 0">
                                            <a-badge color="#dae3e6" /><span style="color:#dae3e6">已废弃</span>
                                        </template>
                                        <template v-if="record.entryState === 1">
                                            <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
                                        </template>
                                        <template v-if="record.entryState === 2">
                                            <a-badge color="#FBB31F" /><span style="color:#FBB31F">待审核</span>
                                        </template>
                                        <template v-if="record.entryState === 3">
                                            <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
                                        </template>
                                    </template>
                                    <template v-if="column.dataIndex === 'operation'">
                                        <div class="editable-row-operations">
                                        <span v-if="editableData[record.id]">
                                            <a-button type="primary" ghost size="small" @click="save(record.id)">保存</a-button>
                                            <a-popconfirm title="是否取消?" ok-text='是' cancel-text='否' @confirm="cancel(record.id)">
                                            <a-button type="primary" ghost size="small" danger>取消</a-button>
                                            </a-popconfirm>
                                        </span>
                                        <span v-else>
                                            <a-button type="primary" ghost size="small" @click="entryDetails(record)">详情</a-button>
                                            <a-button type="primary" ghost size="small" @click="entryHistory(record)">历史</a-button>
                                            <a-button type="primary" ghost size="small" @click="upgrade(record)">升级</a-button>
                                        </span>
                                        </div>
                                    </template>
                                </template>
                            </a-table>
                        </a-form>
                        
                    </div>
                    <Pagination ref="pagination" :total="pagination.total" @pageChange="pageChange" style="padding:0 20px 10px 0px"/>
                </div>
            </a-col>
        </a-row>
    </div>
    
    <ClassifyModal 
    ref="classifyModal"
    :visible="classifyVisible" 
    :modalTitle="classifyModalTitle" 
    @classifyHandleClose="classifyHandleClose"
    @classifyHandleOK="classifyHandleOK"
    />

    <EntryHistory 
    ref="entryHistory"
    :visible="entryHistoryVisible"
    :modalTitle="entryHistoryTitle"
    @historyClose="historyClose"
    />

    <EntryDetails 
    ref="entryDetails"
    :visible="entryDetailsVisible"
    :classifyData="treeData"
    :versions="versions"
    :entryLabels="entryLabelOptions"
    :dataSource="allDataSource"
    :byteLimit="byteLimit"
    :partSpeechs="partSpeechs"
    @detailsClose="detailsClose"
    />

    <MergeEntry 
    ref="mergeEntry"
    :visible="mergeVisible"
    @mergeClose="mergeClose"
    />
    <Export 
    ref="export"
    :visible="exportVisible"
    @exportClose="exportClose"
    />
    <EditReason :visible="editVisible" @editClose="editClose" @editOK="editOk"/>
</template>
<script>
import AddRepository from '@/views/repository/addRepository.vue'
import ImportDataBtn from '@/views/entry/importDataBtn.vue'
import { cloneDeep } from 'lodash-es';
import SearchForm from '@/components/search/searchForm.vue'
import ClassifyModal from '@/views/entry/classifyModal.vue'
import EntryHistory from '@/views/entry/entryHistory.vue'
import EntryDetails from '@/views/entry/entryDetails.vue'
import MergeEntry from '@/views/entry/mergeEntry.vue'
import Export from '@/views/entry/export.vue'
import EditReason from '@/views/entry/editReason.vue'
import locale from 'ant-design-vue/es/date-picker/locale/zh_CN';
import Pagination from "@/components/page/pagination.vue"
import tableParam from "./tableParam.js";
import common from "./common.js";
import { 
    searchEntry,
    getThesaurus,
    getEntryClassfy,
    addEntryClassfy,
    updateEntryClassfy,
    deleteEntryClassfy,
    deleteEntry,
    insertEntry,
    updateEntry,
    getEntryProperty,
    upgradeEntry
} from "@/http/api/entry";
import { 
    queryLabel
} from "@/http/api/label";
import { 
    queryVersionInfo
} from "@/http/api/version";
import { message,Modal} from 'ant-design-vue';
import { defineComponent, ref, createVNode } from 'vue';
import {
  PlusOutlined,
  SettingOutlined,
  LinkOutlined,
  DeleteOutlined,
  DownloadOutlined,
  UploadOutlined,
  ExclamationCircleOutlined,
  HistoryOutlined,
  DownOutlined, 
  SmileOutlined
} from '@ant-design/icons-vue';
export default {
    components:{
        SearchForm,
        ClassifyModal,
        Pagination,
        EntryHistory,
        EntryDetails,
        MergeEntry,
        Export,
        ImportDataBtn,
        AddRepository,
        EditReason,
        SettingOutlined,
        PlusOutlined,
        LinkOutlined,
        DeleteOutlined,
        DownloadOutlined,
        UploadOutlined,
        HistoryOutlined,
        DownOutlined, 
        SmileOutlined
    },
    data(){
        return{
            user:{},
            repositoryBtnSize:'small',
            repositoryBtnTitle:'生成版本库',
            repositoryBtnIcon:'BookOutlined',
            locale: locale,
            loading:false,
            classifyWidth:"200px",
            showClassify: true,
            treeData: [],
            selectedTreeKeys:[],
            expandedKeys:[],
            classifyVisible:false,
            classifyModalTitle:"",
            classifyKey:"",
            search:{
                chinese: '',
                creator: '',
                abbr: '',
                version: null,
                partOfSpeech: null,
                createTime:'',
                createEndRTime:''
            },
            labelCol: { style: { width: '60px' } },
            versions:[],
            createTime:[],
            pagination:{
                current: 1,
                pageSize: 20,
                total: 0
            },
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:70,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left',index:0},
                {title: 'Abbr',dataIndex: 'abbr',align:'center',width:150,fixed: 'left',resizable: true,index:1},
                {title: '状态',dataIndex: 'entryState',align:'center',width:150,resizable: true,index:2},
                {title: '功能/环境',dataIndex: 'environmentRemark',align:'center',width:200,ellipsis: true,resizable: true,index: 3},
                {title: '英文释义',dataIndex: 'englishInterpretation',align:'center',width:200,ellipsis: true,resizable: true,index: 4},
                {title: '词性备注',dataIndex: 'partOfSpeech',align:'center',width:200,resizable: true,index:5},
                {title: '中文术语',dataIndex: 'chinese',align:'center',width:200,ellipsis: true,resizable: true,index:6},
                {title: '英文术语',dataIndex: 'english',align:'center',width:200,ellipsis: true,resizable: true,index:9},
                
                {title: '操作',dataIndex: 'operation',align:'center',width:200,fixed: 'right',index:100}
            ],
            dataSource:[],
            checkboxList: tableParam.checkboxList,
            checkedColumn: tableParam.checkedColumn,
            inputColumn: tableParam.inputColumn,
            translateColumn: tableParam.translateColumn,
            overlayStyle: tableParam.overlayStyle,
            tableHeight: { x:'100%',y: 0 },
            selectedRowKeys:[],
            editableData:{},
            entryLabelOptions:[],
            entryHistoryVisible: false,
            entryHistoryTitle:"",
            entryDetailsVisible: false,
            allDataSource:[],
            mergeVisible: false,
            maxTagCount: 1,
            byteLimit:{},
            rules:{
                english:[{validator: this.checkField,trigger: 'change'} ]
            },
            checkRecordId:'',
            partSpeechs:[],
            exportVisible: false,
            editVisible: false,
            editEntryId: ""
        }
    },
    mounted () {
        let _this = this
        this.$nextTick(() => {
            this.user = this.$store.state.user
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
        // 词条翻译输入框变化触发的事件
        tranlateInputChange(record){
            // 当前需要校验的表格数据
            this.checkRecordId = record.id
        },
        // 翻译字节数校验
        checkField(rule,value){
            if(value === undefined || value === null){
                return Promise.resolve();
            }
            let byteLimit = this.byteLimit[this.editableData[this.checkRecordId].classifyId]
            if(byteLimit === undefined || byteLimit === null){
                return Promise.resolve();
            }
            let length = common.byteLength(value)
            
            if(length > byteLimit){
                return Promise.reject('最大字节数为'+byteLimit);
            }
           
           return Promise.resolve();
        },
        // 初始化
        init(){
            this.pagination.current = this.$refs.pagination.current
            this.pagination.pageSize = this.$refs.pagination.pageSizeOptions[0]
            //设置表格高度
            this.setTableHeight();
            // 查询词条分类
            this.getClassifyData();
            // 查询词条
            // this.getEntryDataSource();
            // this.getAllEntry()
            // 查询词条版本
            this.getVersions();
            // 查询词条标签
            this.getEntryLabel()
            // 查询词性
            this.getPartOfSpeech()
        },
        setTableHeight(){
            this.$nextTick(() => {
                let box = this.$refs.box.offsetHeight
                let searchHeight = this.$refs.searchForm.$el.offsetHeight
                let paginationHeight = this.$refs.pagination.$el.offsetHeight
                // let tabHeight = this.$refs.tab.$el.offsetHeight
                this.tableHeight.y = box - searchHeight - paginationHeight - 95
            })
        },
        // 查询词条版本
        getVersions(){
            let params = {
                pageIndex: -1,
                pageSize: -1
            }
            queryVersionInfo(params).then((res) => {
                this.versions = res.data.list
            })
        },
        // 查询词条标签
        getEntryLabel(){
            queryLabel({},-1,-1).then((res) => {
                this.entryLabelOptions = []
                res.data.list.forEach(item =>{
                    let label = {
                        value: item.labelName,
                        label: item.labelName
                    }
                    this.entryLabelOptions.push(label)
                })
            })
        },
        // 查询词性
        getPartOfSpeech(){
            let data = {}
            getEntryProperty(data).then((res) => {
                this.partSpeechs = []
                res.data.list.forEach(item => {
                    let part = {
                        value: item.propertyName,
                        label: item.propertyName,
                    }
                    this.partSpeechs.push(part)
                })
            })
        },
        // 获取词条分类数据
        getClassifyData(){
            // TODO 管理员角色可以看到所有 非管理员只能看到本部门的
            let params = {
                department: this.user.department
            }
            getEntryClassfy(params).then((res) => {
                this.treeData = res.data.list
                if(this.treeData.length > 0){
                    this.selectedTreeKeys.push(this.treeData[0].key)
                }
                this.expandedData(this.treeData);
                this.byteLimit = this.headerClassifyData(this.treeData)

                // 查询词条
                this.getEntryDataSource();
                this.getAllEntry()
            })
        },
        // 获取分类允许最大翻译字节数
        headerClassifyData(treeData,arr = {}){
            for (let item of treeData) {
                if(item.maxByte != null && item.maxByte != ''){
                    arr[item.key] = item.maxByte
                }
                if (item.children && item.children.length) this.headerClassifyData(item.children, arr)
            }
            return arr
        },
        // 设置树全部展开
        expandedData(data){
            data.forEach(item => {
                if(item.children.length > 0){
                    this.expandedKeys.push(item.key)
                    this.expandedData(item.children)
                }
            })
        },
        // 显示与隐藏分类列表
        closeOrOpenClassify(type){
            this.showClassify = !this.showClassify
            if(type === 'close'){
                this.classifyWidth = '10px'
            }else{
                this.classifyWidth = '200px'
            }
        },
        // 词条分类点击事件
        clickTree(selectedKeys,e){
            if(e.selected){
                this.selectedTreeKeys = selectedKeys
            }else{
                this.selectedTreeKeys = [e.node.key]
            }
            this.getEntryDataSource()
            this.getAllEntry()
        },
        // 词条分类右键菜单点击事件
        onContextMenuClick(treeKey,menuKey,title,maxByte){
            this.$refs.classifyModal.reset()
            this.classifyKey = treeKey
            if(menuKey === 'addChild'){
                this.classifyVisible = true
                this.classifyModalTitle = title+"-添加子类"
            }else if(menuKey === 'edit'){
                let classify = {
                    name:title,
                    maxByte:maxByte
                }
                this.$refs.classifyModal.init(classify)
                this.classifyModalTitle = title+"-编辑"
                this.classifyVisible = true
            }else if(menuKey === 'delete'){
                this.deleteClassify(treeKey)
            }
        },
        addRootClassify(){
            this.classifyVisible = true
            this.classifyModalTitle = "新增根节点"
            this.$refs.classifyModal.reset()
        },
        //关闭分类弹窗
        classifyHandleClose(){
            this.classifyVisible = false
        },
        // 分类弹窗确认事件
        classifyHandleOK(classify){
            let params = {
                title:classify.name,
                maxByte:classify.maxByte
            }

            if(this.classifyModalTitle === '新增根节点'){
                params.parentId = '0';
                this.addClassify(params)
            }else if(this.classifyModalTitle.includes('添加子类')){
                params.parentId = this.classifyKey;
                this.addClassify(params)
            }else if(this.classifyModalTitle.includes('编辑')){
                params.key = this.classifyKey
                this.editClassify(params)
            }
            
        },
        // 新增分类
        addClassify(params){
            addEntryClassfy(params).then((res) => {
                message.success("添加成功！")
                this.classifyVisible = false
                this.getClassifyData()
            })
        },
        // 编辑分类
        editClassify(params){
            updateEntryClassfy(params).then((res) => {
                message.success("编辑成功！")
                this.classifyVisible = false
                this.classifyKey = ""
                this.getClassifyData()
            })
        },
        // 分类拖拽
        onDragEnter(info){
            // console.log("onDragEnter:",info)
        },
        onDrop(info){
            // console.log("onDrop",info)
            // 拖拽的节点
            let dragNode = info.dragNode.dataRef
            // 目标节点
            let node = info.node.dataRef
            if(info.dropToGap){
                // 和目标分类同级
                dragNode.parentId = node.parentId
            }else{
                // 是目标分类的子集
                dragNode.parentId = node.key
            }
            updateEntryClassfy(dragNode).then((res) => {
                this.getClassifyData()
            })
        },
        // 删除分类
        deleteClassify(key){
            let data = [key]
            deleteEntryClassfy(data).then((res) => {
                message.success("删除成功！")
                this.getClassifyData()
            })
        },
        // 日期范围选择器改变时触发
        changePicker(value, dateString){
            if(dateString.length > 1){
                this.search.createTime = dateString[0]
                this.search.createEndRTime = dateString[1]
            }
        },
        onSelectChange(selectedRowKeys,selectedRows){
            this.selectedRowKeys = selectedRowKeys
            this.selectedRows = selectedRows
        },
         //分页
        pageChange(current,pageSize){
            this.pagination.current = current
            this.pagination.pageSize = pageSize
            this.getEntryDataSource()
        },
        changeColumn(checkedValue) {
            this.checkedColumn = checkedValue;
            this.checkboxList.forEach((value) => {
                let checkedIndex = this.checkedColumn.findIndex(
                    (item) => item === value.value
                );
                let nowColumnIndex = this.columns.findIndex(
                    (item) => item.dataIndex === value.value
                );
                if (
                    (nowColumnIndex !== -1 && checkedIndex !== -1) ||
                    (nowColumnIndex === -1 && checkedIndex === -1)
                ) {
                    return;
                }
                if (nowColumnIndex === -1 && checkedIndex !== -1) {
                    let newCol = {
                        title: value.label,
                        dataIndex: value.value,
                        align: "center",
                        width: 200,
                        ellipsis: true,
                        resizable: true,
                        index: value.index
                    };
                    this.columns.splice(-1, 0, newCol);
                }
                if (nowColumnIndex !== -1 && checkedIndex === -1) {
                    this.columns.splice(nowColumnIndex, 1);
                }
            });
            this.columns.sort(function(a, b){
                return a.index - b.index
            })
        },
        searchBtn(){
            this.getEntryDataSource()
            this.getAllEntry()
        },
        //词条查询
        getEntryDataSource(){
            this.loading = true
            if(this.selectedTreeKeys.length > 0){
                this.search.classifyId = this.selectedTreeKeys[0]
            }else{
                this.search.classifyId = ''
            }
            let params = {
                entryState:'1,2,3',
                pageIndex:this.pagination.current,
                pageSize:this.pagination.pageSize
            }
            searchEntry(this.search,params).then((res) => {
                this.dataSource = res.data.list
                this.dataSource.forEach(item => {
                    if(item.entryLabel === '' || item.entryLabel === null){
                        item.labelList = undefined
                    }else{
                        item.labelList = item.entryLabel.split(',')
                    }
                })
                this.pagination.total = res.data.totalNum
                this.editableData = {}
                this.loading = false
            })
        },
        // 查询所有符合条件的词条
        getAllEntry(){
            if(this.selectedTreeKeys.length > 0){
                this.search.classifyId = this.selectedTreeKeys[0]
            }
            let params ={
                entryState:'1,2,3',
                pageIndex:-1,
                pageSize:-1
            }
            searchEntry(this.search,params).then((res) => {
                this.allDataSource = res.data.list
            })
        },
        //新增词条
        addEntry(){
            let classify = this.selectedTreeKeys.length > 0 ? this.selectedTreeKeys[0] : ""
            let newData = {
                id: `new${this.dataSource.length + 1}`,
                entryState:1,
                classifyId:classify,
                version:'V1.0'
            }
            this.dataSource.push(newData)
            this.editableData[newData.id] = newData;

            // 滚动到最底部
            this.$nextTick(()=>{
                let container = this.$refs.entryTable.$el.querySelector('.ant-table-body')
                container.scrollTop = container.scrollHeight
            })
        },
        checkTranlateLength(data){
            let flag = true

            let byteLimit = this.byteLimit[this.editableData[data.id].classifyId]
            if(byteLimit === undefined || byteLimit === null){
                return flag
            }
            this.translateColumn.forEach(item => {
                if(data[item] != null && data[item] != undefined){
                    let length = common.byteLength(data[item])
                    if(length > byteLimit){
                        flag = false
                    }
                }
            })
            return flag
        },
        // 词条升级版本
        upgrade(record){
            let index = this.dataSource.findIndex(item =>{
                if(item.id === record.id){
                    return true
                }
            })
            let newRecord = cloneDeep(record)
            let id = 'upgrade'+record.id
            newRecord.id = id
            this.editableData[id] = newRecord
            // console.log(newRecord)
            // console.log(this.editableData)
            this.dataSource.splice(index+1,0,newRecord)

        },
        // 保存
        save(id){
            // 判断翻译字节数是否合法
            let flag = this.checkTranlateLength(this.editableData[id]);
            // console.log(flag)
            if(!flag){
                return
            }

            if(id.startsWith('new')){
                // 新增词条
                let data = this.editableData[id]
                
                if(data.labelList != undefined){
                    data.entryLabel = ""
                    data.labelList.forEach(item => {
                        data.entryLabel += item +','
                    })
                    data.entryLabel = data.entryLabel.substring(0, data.entryLabel.lastIndexOf(','))
                }
                data.entryState = 2
                insertEntry(data).then((res) => {
                    let index = this.dataSource.findIndex(item =>{
                        if(item.id === id){
                            return true
                        }
                    })
                    this.dataSource.splice(index,1)
                    this.dataSource.push(res.data)
                    message.success("新增成功！")
                    delete this.editableData[id];
                })
            }else if(id.startsWith('upgrade')){
                // message.info("升级版本")
                this.editableData[id].id = this.editableData[id].id.replace('upgrade','')
                upgradeEntry(this.editableData[id]).then((res) => {
                    message.success("升级成功！")
                    delete this.editableData[id];
                    // Object.assign(this.dataSource.filter(item => id === item.id)[0], res.data.data);
                    this.getEntryDataSource()
                })
            }else{
                // 编辑词条
                this.editVisible = true
                this.editEntryId = id
                
            }
        },
        cancel(id){
            delete this.editableData[id];
            if(id.startsWith("new") || id.startsWith("upgrade")){
                //从dataSource中删除
                this.dataSource.some((item,i) => {
                    if(item.id === id){
                        this.dataSource.splice(i,1)
                        return true
                    }
                })
            }
        },
        // 编辑原因确认
        editOk(reason){
            if(this.editableData[this.editEntryId].labelList != undefined){
                this.editableData[this.editEntryId].entryLabel = ""
                this.editableData[this.editEntryId].labelList.forEach(item => {
                    this.editableData[this.editEntryId].entryLabel += item +','
                })
                this.editableData[this.editEntryId].entryLabel = this.editableData[this.editEntryId].entryLabel.substring(0, this.editableData[this.editEntryId].entryLabel.lastIndexOf(','))
            }
            
            let params = {
                notes: reason
            }
            updateEntry(params,this.editableData[this.editEntryId]).then((res) => {
                message.success("编辑成功！")
                Object.assign(this.dataSource.filter(item => this.editEntryId === item.id)[0], res.data);
                delete this.editableData[this.editEntryId];
                this.editVisible = false
                this.editEntryId = ""
            })
        },
        // 编辑原因取消
        editClose(){
            this.editVisible = false
            this.editEntryId = ""
        },
        //双击表格行 可编辑
        doubleClick(record, index){ 
            return {
                onDblclick: (event) => {
                    // 已审核的词条不可编辑
                    // if(record.entryState != 3){
                        this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
                    // }else{
                    //     message.warn("已审核词条不可编辑！")
                    // }
                }
            }
        },
        // 删除词条
        deleteEntry(){
            if(this.selectedRowKeys.length === 0){
                message.warn('请选择需要删除的词条！')
                return
            }
            Modal.confirm({
                title: '是否确认删除?',
                icon: createVNode(ExclamationCircleOutlined),
                okText: '确认',
                cancelText: '取消',
                onOk: () => {
                    deleteEntry(this.selectedRowKeys).then((res) => {
                        message.success('删除成功！')
                        this.getEntryDataSource()
                        this.selectedRowKeys = []
                    })
                }
            });
            
        },
        // 词条操作历史
        entryHistory(record){
            this.entryHistoryVisible = true
            this.entryHistoryTitle = "操作历史"
            this.$refs.entryHistory.clearForm()
            this.$refs.entryHistory.init(record.id)
        },
        historyClose(){
            this.entryHistoryVisible = false
        },
        // 词条详情
        entryDetails(record){
            this.entryDetailsVisible = true
            this.$refs.entryDetails.init(cloneDeep(record))
        },
        detailsClose(){
            this.entryDetailsVisible = false
            this.getEntryDataSource()
        },
        //重置
        reset(){
            this.search = {
                chinese: '',
                creator: '',
                abbr: '',
                version: null,
                partOfSpeech: '',
                createTime:'',
                createEndRTime:'',
                tableName:""
            }
            this.pagination.current = 1
            this.$refs.pagination.current = 1
            // this.selectedTreeKeys = []
            this.getEntryDataSource()
        },
        mergeEntry(){
            this.mergeVisible = true
            this.$refs.mergeEntry.init()
        },
        mergeClose(){
            this.mergeVisible = false
        },
        // 导出
        exportData(){
            this.exportVisible = true
        },
        exportClose(){
            this.exportVisible = false
        },
        handleResizeColumn: (w, col) => {
            col.width = w;
        },
    }
}
</script>
<style lang="less">
@import url("@/assets/style/common.less");
</style>
<style scoped lang="less">
.ant-row{
    height: 100%;
    .ant-col{

        .classify{
            width: 100%;
            height: 100%;
            background-color: white;
            position: relative;

            .treeBox{
                position: absolute;
                width: 100%;
                height: calc(100% - 60px);
                overflow: auto;
                padding: 10px;
            }

            .addBox{
                position: absolute;
                bottom: 0%;
                height: 30px;
                width: 100%;
            }
        }

        .content{
            width: calc(100% - 10px);
            height: 100%;
            background-color: white;  
            padding: 10px;  
            position: absolute;
        }
    }
}
.classifyTitle{
    width: 100%;
    height: 30px;
    border-bottom: 1px solid #E7E7E7;
    padding: 5px;
    display: flex;
    align-items: center;
    span{
        font-family: Microsoft YaHei;
        font-size: 12px;
        font-style: normal;
        font-weight: 700;
        color: black;
    }

    img{
        width: 15px;
        height: 15px;
        margin-left: auto;
    }
}
.search{
    :deep(.ant-input),:deep(.ant-select-selector),.ant-picker-range{
        width: 230px;
    }
    :deep(label){
        font-size: 12px;
    }
}
:deep(.ant-tabs){
    padding: 0 10px;
}
:deep(.ant-tabs-nav) {
    margin: 0 0 5px 0;
}
:deep(.ant-tabs-tab-btn){
  font-size: 12px;
}
.operateBtn{
    width: 100%;
    margin-bottom: 5px;
    text-align: right;

    .ant-btn{
        display: inline-block;
    }
}
.table{
    width: 100%;
    position: relative;
}
:deep(.ant-table-cell){

    .ant-form-item{
        margin-bottom: 0px;
    }
    
}
</style>