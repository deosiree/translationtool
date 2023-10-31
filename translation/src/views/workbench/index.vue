<template>
    <div ref="box" style="width:100%;height:100%;padding:10px;">
        <SearchForm ref="searchForm" @change="setTableHeight">
            <template v-slot:form>
                <a-form
                    :model="search"
                    name="horizontal_login"
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
                        <a-select
                            v-model:value="search.partOfSpeech"
                            placeholder="请选择词性备注"
                            :options='partSpeechs'
                            size="small"
                            style="width: 200px"
                            >
                        </a-select>
                    </a-form-item>
                
                    <a-form-item
                    label="版本"
                    name="version"
                    >
                        <a-select
                        v-model:value="search.version"
                        style="width: 200px"
                        placeholder="请选择版本"
                        size="small"
                        >
                            <template v-for="(item,index) in versions" :key="index">
                                <a-select-option :value="item.name">{{item.name}}</a-select-option>
                            </template>
                        </a-select>
                    </a-form-item>
                    <a-form-item
                    label="词条标签"
                    name="label"
                    >
                        <a-select
                        v-model:value="searchLabel"
                        mode="multiple"
                        :max-tag-count="maxTagCount"
                        style="width: 200px"
                        placeholder="请选择标签"
                        size="small"
                        >
                            <template v-for="(item,index) in labels" :key="index">
                                <a-select-option :value="item.value">{{item.label}}</a-select-option>
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
                    <a-button type="primary" size="middle" style="margin-left:10px" @click="searchBtn">查询</a-button>
                    <a-button type="primary" size="middle" style="margin-left:10px;background-color:#36BF7D;border:#36BF7D" @click="reset">重置</a-button>
                </div>
            </template>
        </SearchForm>
        <a-tabs v-model:activeKey="entryState" @change="changeTab" ref="tab">
            <a-tab-pane key="2" tab="词条未审核"></a-tab-pane>
            <a-tab-pane key="1" tab="翻译未审核"></a-tab-pane>
        </a-tabs>
        <div class="operateBtn">
            <a-button type="primary" size="small" style="margin-left:10px" 
            @click="bathAudit"
            v-if="authority.includes('bathAudit')"
            >
                <template #icon><SnippetsOutlined /></template>
                批量审核
            </a-button>
            
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
            <a-table 
            class="ant-table-striped"
            :columns="columns" 
            :data-source="dataSource" 
            :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            :row-key="record => record.id"
            :scroll="tableHeight"
            :pagination='false'
            :loading="loading"
            :rowClassName="getRowClassName"
            ref="workTable"
            @resizeColumn="handleResizeColumn"
            :customRow="customRow"
            bordered>
                <template #bodyCell="{ column,text, record }">
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
                    <template v-if="showIcon(column,record)">
                        <div class="editable-cell">
                            <div class="editable-cell-text-wrapper">
                                {{ text }}
                                <CheckCircleOutlined class="editable-cell-icon" style="right:20px;color:#369FFF" @click="edit(record.key)" />
                                <CloseCircleOutlined class="editable-cell-icon" style="color:#ff4d4f"/>
                            </div>
                        </div>
                    </template>
                    <template v-if="column.dataIndex === 'operation'">
                        <a-button type="primary" ghost size="small" @click="audit(record)">审核</a-button>
                    </template>
                </template>
            </a-table>
        </div>
        <Pagination ref="pagination" :total="pagination.total" @pageChange="pageChange" style="padding:0 20px 10px 0px"/>
        <AuditModal ref="auditModal" :visible="modalVisible" :modalTitle="modalTitle" @handleClose="modelColse" @handleOK="modelOK"/>
    </div>
</template>
<script>
import { message,Modal } from 'ant-design-vue';
import locale from 'ant-design-vue/es/date-picker/locale/zh_CN';
import SearchForm from '@/components/search/searchForm.vue'
import Pagination from "@/components/page/pagination.vue"
import AuditModal from '@/views/workbench/auditModal';
import tableParam from "@/views/entry/tableParam.js";
import {
  SnippetsOutlined,
  SettingOutlined,
  DownloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue';
import { 
    searchEntry,
    bathAudit,
    getThesaurus,
    getEntryProperty,
    getTranslatedEntry
} from "@/http/api/entry";
import { 
    queryLabel
} from "@/http/api/label";
import { 
    queryVersionInfo
} from "@/http/api/version";
export default {
    components:{
        SearchForm,
        Pagination,
        AuditModal,
        SnippetsOutlined,
        SettingOutlined,
        DownloadOutlined,
        CheckCircleOutlined,
        CloseCircleOutlined
    },
    data(){
        return{
            loading:false,
            name:"workbench",
            // 汉化包
      		locale: locale,
            labelCol: { style: { width: '60px' } },
            maxTagCount: 1,
            entryState:"2",
            labels:[],
            searchLabel:undefined,
            search:{
                chinese: '',
                creator: '',
                abbr: '',
                version: null,
                partOfSpeech: null,
                createTime:'',
                createEndRTime:'',
                label:""
            },
            versions: [],
            createTime:[],
            tableHeight: { x:'100%',y: 0 },
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
                // {title: '操作',dataIndex: 'operation',align:'center',width:100,fixed: 'right',}
            ],
            dataSource:[],
            checkboxList: tableParam.checkboxList,
            visibleColumns: false,
            checkedColumn: tableParam.checkedColumn,
            selectedRowKeys:[],
            selectedRows:[],
            pagination:{
                current: 1,
                pageSize: 20,
                total: 0
            },
            overlayStyle: tableParam.overlayStyle,
            modalVisible: false,
            modalTitle:"",
            authority:[],
            clickRecord:{},
            partSpeechs:[],
            translateState:['englishTranslateState','russianTranslateState','spanishTranslateState','frenchTranslateState','chineseTranslateState'],
            translate:["chinese","english","french","russian","spanish"],
            selectedRowIndex:null
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
        customRow(record, index){
            return {
                onClick: (event) => {
                    // console.log("点击")
                    // this.selectedRowIndex = index
                }
            }
        },
        // 校验是否展示 审核图标
        showIcon(column,record){
            if(this.entryState === '2'){
                // 词条未审核列表不展示图标
                return false
            }
            let state = column.dataIndex+"TranslateState"
            if(this.translate.includes(column.dataIndex) && record[column.dataIndex] != null && record[state] === "已翻译"){
                return true
            }
            return false
        },
        init(){
            this.setTableHeight()
            this.pagination.current = this.$refs.pagination.current
            this.pagination.pageSize = this.$refs.pagination.pageSizeOptions[0]
            this.getVersion()
            this.getAuthority()
            this.getDataSource()
            this.getEntryLabel()
            this.getPartOfSpeech()
        },
        setTableHeight(){
            this.$nextTick(() => {
                let box = this.$refs.box.offsetHeight
                let searchHeight = this.$refs.searchForm.$el.offsetHeight
                let paginationHeight = this.$refs.pagination.$el.offsetHeight
                let tabHeight = this.$refs.tab.$el.offsetHeight
                this.tableHeight.y = box - searchHeight - paginationHeight - tabHeight - 90
            })
        },
        //获取用户权限
        getAuthority(){
            let authoritys = this.$store.state.authority;
            authoritys.filter(item => {
                if(item.name === this.name){
                    item.authorities.filter(temp =>{
                        this.authority.push(temp.authorityCode)
                    })
                }
            })
            // 表格展示操作栏
            if(this.authority.includes('bathAudit')){
                let operation = {title: '操作',dataIndex: 'operation',align:'center',width:100,fixed: 'right',index:100}
                this.columns.push(operation)
            }
        },
        // 查询版本
        getVersion(){
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
                this.labels = []
                res.data.list.forEach(item =>{
                    let label = {
                        value: item.labelName,
                        label: item.labelName
                    }
                    this.labels.push(label)
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
        // 查询按钮点击事件
        searchBtn(){
            if(this.entryState === '2'){
                this.getDataSource();
            }else{
                this.getTranslatedEntry()
            }
        },
        // 查询数据
        getDataSource(){
            this.loading = true
            if(this.searchLabel != undefined){
                let label = ""
                this.searchLabel.forEach(item => {
                    label += item + ","
                })
                label = label.substring(0, label.lastIndexOf(','))
                this.search.label = label
            }
            let params = {
                entryState:this.entryState,
                pageIndex:this.pagination.current,
                pageSize:this.pagination.pageSize
            }
            searchEntry(this.search,params).then((res) => {
                this.dataSource = res.data.list
                this.pagination.total = res.data.totalNum
                this.loading = false
            })
        },
        // 日期范围选择器改变时触发
        changePicker(value, dateString){
            if(dateString.length > 1){
                this.search.createTime = dateString[0]
                this.search.createEndRTime = dateString[1]
            }
        },
        // 待审核  已审核 切换时触发
        changeTab(activeKey){
            if(activeKey === "2"){
                // 查询词条未审核的列表
                this.getDataSource();
            }else{
                // 查询翻译未审核的数据列表
                this.getTranslatedEntry()
            }
            

            // //判断是否展示操作栏  
            // let index = this.columns.findIndex(
            //     (item) => item.dataIndex === "operation"
            // );
            // // 已审核表格去除操作栏
            // if(activeKey === "0,3" && index !== -1){
            //     this.columns.splice(index,1)
            // }
            // // 待审核表格展示操作栏
            // if(activeKey === "2" && index === -1 && this.authority.includes("bathAudit")){
            //     let operation = {title: '操作',dataIndex: 'operation',align:'center',width:100,fixed: 'right'}
            //     this.columns.push(operation)
            // }

        },
        // 查询翻译未审核的数据列表
        getTranslatedEntry(){
            let params = {
                pageIndex: this.pagination.current,
                pageSize: this.pagination.pageSize
            }
            getTranslatedEntry(params).then((res) => {
                this.dataSource = res.data.list
            })
        },
        //审核
        audit(record){
            this.clickRecord = record
            this.modalTitle = "审核"
            this.modalVisible = true
            this.$refs.auditModal.reset()
        },
        // 批量审核
        bathAudit(){
            if(this.selectedRows.length > 0){
                this.modalTitle = "批量审核"
                this.modalVisible = true
            }else{
                message.warning("请选择需要审核的词条！")
            }
            this.$refs.auditModal.reset()
        },
        modelColse(data){
            if(data === 'false'){
                this.modalVisible = false
            }else{
                this.modalVisible = true
            }
        },
        modelOK(audit){
            if(this.modalTitle.includes('批量')){
                let params = []
                this.selectedRows.forEach(item =>{
                    let flag = params.filter(i => i.tableName === item.tableName)
                    if(flag.length > 0){
                        flag[0].ids.push(item.id)
                    }else{
                        let temp = {
                            tableName: item.tableName,
                            ids: [item.id]
                        }
                        params.push(temp)
                    }
                })
                this.entryAudit(params,audit)
            }else{
                let data = [
                    {
                        ids: [this.clickRecord.id],
                        tableName:this.clickRecord.tableName
                    }
                ]
                this.entryAudit(data,audit)
            }
        },
        entryAudit(data,state){
            let params = {
                state:state.entryState,
                note:state.notes
            }
            bathAudit(data,params).then((res) => {
                this.modalVisible = false
                message.success("审核成功！")
                if(this.entryState === '2'){
                    this.getDataSource()
                }else{
                    this.getTranslatedEntry()
                }
                
                this.selectedRows = []
                this.selectedRowKeys = []
            })
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
        onSelectChange(selectedRowKeys,selectedRows){
            this.selectedRowKeys = selectedRowKeys
            this.selectedRows = selectedRows
        },
        //分页
        pageChange(current,pageSize){
            this.pagination.current = current
            this.pagination.pageSize = pageSize

            if(this.entryState === '2'){
                this.getDataSource();
            }else{
                this.getTranslatedEntry()
            }
            
        },
        
        // 重置
        reset(){
            this.searchLabel = undefined
            this.search = {
                label: '',
                chinese: '',
                creator: '',
                abbr: '',
                version: undefined,
                partOfSpeech: '',
                createStartDate:'',
                createEndDate:'',
            }
            this.pagination.current = 1
            this.$refs.pagination.current = 1
            this.entryState="2"
            this.getDataSource()
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
:deep(.ant-tabs){
    padding: 0 10px;
}
:deep(.ant-tabs-nav) {
    margin: 0 0 5px 0;
}
:deep(.ant-tabs-tab-btn){
  font-size: 12px;
}
.search{
    :deep(.ant-input){
        width: 200px;
    }
    :deep(label){
        font-size: 12px;
    }
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
.ant-btn-sm{
    font-size: 12px;
}
.ant-btn > .anticon + span, .ant-btn > span + .anticon {
    margin-left: 5px;
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

.highlighted-row>td {
  border-top: 1px solid #369FFF !important;
  border-bottom: 1px solid #369FFF !important;
}
 
.highlighted-row>td:first-child {
  border-left: 1px solid #369FFF !important;
}
.highlighted-row>td:first-child::before {
    content: url('/src/assets/icon/caret-right-small.png');
    position: absolute;
    left: 0%;
}
.highlighted-row>td:last-child {
  border-right: 1px solid #369FFF !important;
}
</style>