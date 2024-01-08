<template>
    <div class="productEntryBox" ref="productEntryBox">
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
                    label="词条"
                    name="entry"
                    >
                        <a-input v-model:value="search.entry" placeholder="请输入内容"></a-input>
                    </a-form-item>
                    
                    <a-form-item
                    label="Abbr"
                    name="abbr"
                    >
                        <a-input v-model:value="search.abbr" placeholder="请输入内容"></a-input>
                    </a-form-item>
                    <a-form-item
                    label="词性备注"
                    name="partSpeech"
                    >
                        <a-input v-model:value="search.partOfSpeech" placeholder="请输入内容"></a-input>
                    </a-form-item>
                    <!-- <a-form-item
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
                    </a-form-item> -->
                    <a-form-item
                    label="词条来源"
                    name="entrySource"
                    >
                        <a-input v-model:value="search.entrySource" placeholder="请输入内容"></a-input>
                    </a-form-item>
                </a-form>
            </template>
            <template v-slot:operate>
                <a-button type="primary" size="middle" class="resetBtn" @click="reset">重置</a-button>
                <a-button type="primary" size="middle" @click="getEntryByVersion">查询</a-button>
            </template>
        </SearchBox>
        <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
            <template v-slot:label>
                当前版本： <a-select
                            v-model:value="currentVersion"
                            style="width: 120px"
                            placeholder="请选择内容"
                            :options='productVersions'
                            :fieldNames="{label:'name',value:'id'}"
                            size="small"
                            @select="changeVersion"
                            >
                            </a-select>
            </template>
            <template v-slot:operate>
                <div ref="button" v-if="true" style="margin-bottom:8px;display:flex;gap:8px">
                    <!-- <a-button type="primary" size="small"><template #icon><PlusOutlined /></template>新增</a-button> -->
                    <a-button type="primary" size="small" @click="deleteEntry" v-if="edit"><template #icon><DeleteOutlined /></template>删除</a-button>
                    <a-button type="primary" size="small" @click="batchSave" v-if="edit"><template #icon><SaveOutlined /></template>保存</a-button>
                    <!-- <a-button type="primary" size="small" class="resetBtn" ><template #icon><UpSquareOutlined /></template>升级</a-button> -->
                    <a-popover
                        trigger="click"
                        placement="leftTop"
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
                        <a-button type="primary" size="small"><template #icon><SettingOutlined /></template>展示列</a-button>
                    </a-popover>
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
                        <template #bodyCell="{ column, record, text }">
                            <template v-if="inputColumn.includes(column.dataIndex)">
                                <div>
                                    <template v-if="editableData[record.id]">
                                        <a-input
                                            v-model:value="editableData[record.id][column.dataIndex]"
                                            style="margin: -5px 0"
                                            @click="clickInput"
                                        />
                                    </template>
                                    <template v-else>
                                        {{ text }}
                                    </template>
                                </div>
                            </template>
                            <template v-if="admin && translateColumn.includes(column.dataIndex)">
                                <div>
                                    <template v-if="editableData[record.id]">
                                        <a-input
                                            v-model:value="editableData[record.id][column.dataIndex]"
                                            style="margin: -5px 0"
                                            @click="clickInput"
                                        />
                                    </template>
                                    <template v-else>
                                        {{ text }}
                                    </template>
                                </div>
                            </template>
                            <template v-if="column.dataIndex === 'operation'">
                                <div class="editable-row-operations">
                                <span v-if="editableData[record.id]">
                                    <a-button type="primary" ghost size="small" @click.stop="save(record.id)">保存</a-button>
                                    <a-button type="primary" ghost size="small" danger @click.stop="cancel(record.id)">取消</a-button>
                                </span>
                                <span v-else>
                                    <a-button type="primary" ghost size="small" @click.stop="entryDetails(record)">详情</a-button>
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
                <div class="entryDetails">
                    <table>
                        <tr>
                            <td class="tableTitle">翻译语种</td>
                            <td>英文</td>
                            <td>俄文</td>
                            <td>西文</td>
                            <td>法文</td>
                        </tr>
                        <tr>
                            <td class="tableTitle">翻译结果</td>
                            <td>{{currentEntry.english}}</td>
                            <td>{{currentEntry.russian}}</td>
                            <td>{{currentEntry.spanish}}</td>
                            <td>{{currentEntry.french}}</td>
                        </tr>
                        <tr>
                            <td class="tableTitle">翻译状态</td>
                            <td>
                                <template v-if="currentEntry.englishTranslateState === '已审核'">
                                    <a-badge color="#36BF7D" /><span style="color:#36BF7D">{{currentEntry.englishTranslateState}}</span>
                                </template>
                                <template v-else-if="currentEntry.englishTranslateState != null">
                                    <a-badge color="#FBB31F" /><span style="color:#FBB31F">{{currentEntry.englishTranslateState}}</span>
                                </template>
                            </td>
                            <td>
                                <template v-if="currentEntry.russianTranslateState === '已审核'">
                                    <a-badge color="#36BF7D" /><span style="color:#36BF7D">{{currentEntry.russianTranslateState}}</span>
                                </template>
                                <template v-else-if="currentEntry.russianTranslateState != null">
                                    <a-badge color="#FBB31F" /><span style="color:#FBB31F">{{currentEntry.russianTranslateState}}</span>
                                </template>
                            </td>
                            <td>
                                <template v-if="currentEntry.spanishTranslateState === '已审核'">
                                    <a-badge color="#36BF7D" /><span style="color:#36BF7D">{{currentEntry.spanishTranslateState}}</span>
                                </template>
                                <template v-else-if="currentEntry.spanishTranslateState != null">
                                    <a-badge color="#FBB31F" /><span style="color:#FBB31F">{{currentEntry.spanishTranslateState}}</span>
                                </template>
                            </td>
                            <td>
                                <template v-if="currentEntry.frenchTranslateState === '已审核'">
                                    <a-badge color="#36BF7D" /><span style="color:#36BF7D">{{currentEntry.frenchTranslateState}}</span>
                                </template>
                                <template v-else-if="currentEntry.frenchTranslateState != null">
                                    <a-badge color="#FBB31F" /><span style="color:#FBB31F">{{currentEntry.frenchTranslateState}}</span>
                                </template>
                                
                            </td>
                        </tr>
                        <tr>
                            <td class="tableTitle">选择</td>
                            <td>
                                <a-checkbox 
                                :disabled="currentEntry.englishTranslateState != '已审核'"
                                v-model:checked="currentEntry.englishChecked"
                                >
                                </a-checkbox>
                            </td>
                            <td>
                                <a-checkbox 
                                :disabled="currentEntry.russianTranslateState != '已审核'"
                                v-model:checked="currentEntry.russianChecked"
                                >
                                </a-checkbox>
                            </td>
                            <td>
                                <a-checkbox 
                                :disabled="currentEntry.spanishTranslateState != '已审核'"
                                v-model:checked="currentEntry.spanishChecked"
                                >
                                </a-checkbox>
                            </td>
                            <td>
                                <a-checkbox 
                                :disabled="currentEntry.frenchTranslateState != '已审核'"
                                v-model:checked="currentEntry.frenchChecked"
                                >
                                </a-checkbox>
                            </td>
                        </tr>
                    </table>
                    <div class="details">
                        <div>中文释义：{{currentEntry.chineseInterpretation}}</div>
                        <div>英文释义：{{currentEntry.englishInterpretation}}</div>
                        <div class="btnBox" v-if="admin">
                            <a-button type="primary" size="small" @click="addPublic('1')"><template #icon><PlusOutlined /></template>添加到部门公共库</a-button>
                            <a-button type="primary" size="small" style="margin-left:16px" @click="addPublic('2')"><template #icon><PlusOutlined /></template>添加到公司公共库</a-button>
                        </div>
                    </div>
                </div>
            </template>
        </OperationArea>
        <EditReason :visible="editVisible" :entry="editEntry" @editClose="editClose" @editOk="editOk"/>
    </div>
</template>
<script>
import tableParam from "./tableParam.js";
import common from "./common.js";
import SearchBox from '@/components/search/searchBox.vue'
import DataBox from '@/components/dataBox/index.vue'
import OperationArea from '@/components/operationArea/index.vue'
import EditReason from '@/views/entry/editReason.vue'
import { message,Modal } from 'ant-design-vue';
import { defineComponent, ref, createVNode } from 'vue';
import { cloneDeep } from 'lodash-es';
import { 
    getLanguage
} from "@/http/api/translate";
import { 
    getProductVersion
} from "@/http/api/product";
import { 
    getEntryByVersion,
    deleteEntryInfo,
    updatePublicEntry,
} from "@/http/api/entryManage";
import {
  PlusOutlined,
  DeleteOutlined,
  CopyOutlined,
  SaveOutlined,
  UpSquareOutlined,
  PlusCircleOutlined,
  SettingOutlined,
  SwapOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue';
export default {
    components:{
        SearchBox,
        DataBox,
        OperationArea,
        EditReason,
        PlusOutlined,
        DeleteOutlined,
        CopyOutlined,
        SaveOutlined,
        UpSquareOutlined,
        PlusCircleOutlined,
        SettingOutlined,
        SwapOutlined,
        ExclamationCircleOutlined
    },
    emits:[],
    props: {
        boxHeight:0,
        currentProduct:{},
        productEdit:false
    },
    data() {
        return{
            box:0,
            user:{},
            admin:false,
            edit: false,// 用户对该产品是否有编辑权限
            product:{},
            labelCol: { style: { width: '84px' } },
            search:{
                entry:'',
                abbr:'',
                partOfSpeech:'',
                translateType:null,
                entrySource:''
            },
            translateTypes:[],
            tableTitle:'词条列表',
            copyVisible:false,
            copyNumber:1,
            dataHeight:200,
            tableHeight: { x:'100%',y: 0 },
            loading:false,
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:50,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left',index:0},
                {title: 'Abbr',dataIndex: 'abbr',align:'center',width:150,fixed: 'left',resizable: true,index:1},
                {title: '词条',dataIndex: 'entry',align:'center',width:150,resizable: true,ellipsis: true,index:2},
                {title: '词性备注',dataIndex: 'partOfSpeech',align:'center',width:180,resizable: true,index:4},
                {title: '词条状态',dataIndex: 'entryState',align:'center',width:180,resizable: true,index:5,},
                {title: '英文翻译',dataIndex: 'english',align:'center',width:150,resizable: true,index:9},
                {title: '俄文翻译',dataIndex: 'russian',align:'center',width:150,resizable: true,index:16},
                {title: '西文翻译',dataIndex: 'spanish',align:'center',width:150,resizable: true,index:19},
                {title: '法文翻译',dataIndex: 'french',align:'center',width:150,resizable: true,index:22},
                {title: '操作',dataIndex: 'operation',align:'center',width:150,fixed: 'right',index:100}
            ],
            dataSource:[],
            overlayStyle: tableParam.overlayStyle,
            checkboxList: tableParam.checkboxList,
            checkedColumn: tableParam.checkedColumn,
            inputColumn:tableParam.inputColumn,
            translateColumn: tableParam.translateColumn,
            editableData:{},
            selectedRowKeys:[],
            selectedRows:[],
            selectedRowIndex:null,
            currentEntry:{},
            showOperationArea:false,
            operationAreaTitle:'详情信息',
            operationAreaHeight:190,
            currentVersion:null,
            productVersions:[],
            editVisible:false,
            editEntry:[]
        }
    },
    
    created() {
        
    },
    mounted () {
        this.user = this.$store.state.user
        this.admin = this.$store.state.admin
        //保证初次传的值给到
        this.box = this.boxHeight
        this.edit = this.productEdit
        this.setTableHeight()
        this.product = this.currentProduct
        this.getLanguage()
        this.init()
    },
    watch: {
        boxHeight(newval,oldval){
            this.box = newval
            // console.log(newval)
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
    unmounted() {

    },
    methods: {
        init(){
           this.getProductVersion()
        },
        // 获取翻译语言
        getLanguage(){
            let data = {}
            getLanguage(data).then((res) => {
                this.translateTypes = res.data.list
            })
        },
        // 动态设置表格高度
        setTableHeight(){
            this.$nextTick(() => {
                // 设置列表父元素高度
                let searchHeight = this.$refs.search.$el.offsetHeight
                try {
                    let operationAreaHeight = this.$refs.operationArea.$el.offsetHeight
                    this.dataHeight = this.box - searchHeight - operationAreaHeight - 104
                } catch (error) {
                    this.dataHeight = this.box - searchHeight - 104
                }

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

        // 查询产品的所有版本
        getProductVersion(){
            let params = {
                productName:this.product.title,
                department:this.product.department
            }
            getProductVersion(params).then((res) => {
                this.productVersions = res.data.list
                if(this.productVersions.length > 0){
                    this.currentVersion = this.productVersions[0].id
                    // 获取版本下的词条
                    this.getEntryByVersion()
                }else{
                    this.currentVersion = null
                }
            })
        },
        // 获取版本词条
        getEntryByVersion(){
            if(this.currentVersion === null){
                return
            }
            let params = {
                pageIndex: -1,
                pageSize: -1,
            }
            let version = this.productVersions.find(item => item.id === this.currentVersion)
            let data = {
                versionID: this.currentVersion,
                tableName: version.tableName,
                abbr:this.search.abbr,
                entry:this.search.entry,
                partOfSpeech:this.search.partOfSpeech,
                entrySource:this.search.entrySource
            }

            getEntryByVersion(data,params).then((res) => {
                this.assemblyTableData(res.data.list)
            })
        },
        // 组装表格数据
        assemblyTableData(data){
            let dataSource = []
            let version = this.productVersions.find(item => item.id === this.currentVersion)
            data.forEach(element => {
                let item = element.entryInfoEntity
                // TODO  
                // item.tableName = element.tableName
                item.tableName = version.tableName
                if(element.translateEntity){
                    element.translateEntity.forEach(tran => {
                        if(tran.type === '英文'){
                            item.english = tran.translate
                            item.englishId = tran.id
                            item.englishTranslateState = tran.translateState
                            item.englishPublicState = tran.publicState
                            item.englishChecked = false
                        }else if(tran.type === '俄文'){
                            item.russian = tran.translate
                            item.russianId = tran.id
                            item.russianTranslateState = tran.translateState
                            item.russianPublicState = tran.publicState
                            item.russianChecked = false
                        }else if(tran.type === '西文'){
                            item.spanish = tran.translate
                            item.spanishId = tran.id
                            item.spanishTranslateState = tran.translateState
                            item.spanishPublicState = tran.publicState
                            item.spanishChecked = false
                        }else if(tran.type === '法文'){
                            item.french = tran.translate
                            item.frenchId = tran.id
                            item.frenchTranslateState = tran.translateState
                            item.frenchPublicState = tran.publicState
                            item.frenchChecked = false
                        }
                    })
                }
                dataSource.push(item)
            });
            this.dataSource = dataSource
        },
        changeVersion(version){
            // console.log(version)
            this.currentVersion = version
            // 查询版本词条
            this.getEntryByVersion()
        },

        // 添加表格行点击事件
        customRow(record, index){
            return {
                onClick: (event) => {
                    let _this = this
                    clearTimeout(this.timer)
                    this.timer = setTimeout(function () {
                        _this.selectedRowIndex = record.id
                        _this.currentEntry = record
                        _this.showOperationArea = true
                        _this.setTableHeight()
                    }, 300);
                },
                onDblclick: (event) => {
                    clearTimeout(this.timer)
                    if(this.edit){
                        this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
                    }
                }
            }
        },
        // 详情
        entryDetails(record){
            this.selectedRowIndex = record.id
            this.currentEntry = record
            this.showOperationArea = true
            this.setTableHeight()
        },
        deleteEntry(){
            if(this.selectedRowKeys.length === 0){
                return
            }
            Modal.confirm({
                title: '是否确定删除?',
                icon: createVNode(ExclamationCircleOutlined),
                okText: '确定',
                cancelText: '取消',
                onOk: () => {
                    let version = this.productVersions.find(item => item.id === this.currentVersion)
                    let params = {
                        tableName:version.tableName
                    }
                    deleteEntryInfo(this.selectedRowKeys,params).then((res) => {
                        message.success("删除成功！")
                        this.getEntryByVersion()
                        this.selectedRowKeys = []
                        this.selectedRows = []
                    })
                }
            });
        },
        // 取消
        cancel(id){
            delete this.editableData[id]
        },
        // 保存
        save(id){
            this.editEntry = [this.editableData[id]]
            this.editVisible = true
        },
        // 批量保存
        batchSave(){
            let edit = []
            for(let key in this.editableData){
                edit.push(this.editableData[key])
            }
            this.editEntry = edit
            this.editVisible = true
        },
        editOk(id){
            delete this.editableData[id]
            this.getEntryByVersion()
            this.editVisible = false
        },
        editClose(){
            this.editVisible = false
        },
        // 表格复选框选择事件
        onSelectChange(selectedRowKeys,selectedRows){
            this.selectedRowKeys = selectedRowKeys
            this.selectedRows = selectedRows
        },
        // 表格列可伸缩
        handleResizeColumn: (w, col) => {
            col.width = w;
        },
        // 关闭流程操作区
        closeOperationArea(){
            this.showOperationArea = false
            this.setTableHeight()
            this.selectedRowIndex = null
        },
        // 添加公共库
        addPublic(type){
            // type 1 部门公共库  2公司公共库
            let data = []
            if(this.currentEntry.englishChecked){
                let ele = {
                    id: this.currentEntry.englishId
                }
                data.push(ele)
            }
            if(this.currentEntry.russianChecked){
                let ele = {
                    id: this.currentEntry.russianId
                }
                data.push(ele)
            }
            if(this.currentEntry.spanishChecked){
                let ele = {
                    id: this.currentEntry.spanishId
                }
                data.push(ele)
            }
            if(this.currentEntry.frenchChecked){
                let ele = {
                    id: this.currentEntry.frenchId
                }
                data.push(ele)
            }
            data.forEach(element => {
                let data = {
                    id: element.id,
                    publicState: 1,
                    visualRange: type === '1' ? this.currentProduct.department : '公司'
                }
                updatePublicEntry(data).then((res) => {

                })

            })
            message.success("添加成功！")
        },
        reset(){
            this.search={
                entry:'',
                abbr:'',
                partOfSpeech:'',
                translateType:null,
                entrySource:''
            }
            this.getEntryByVersion()
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
        clickInput(event){
            event.stopPropagation();
        },
    }
}
</script>
<style lang="less">
@import url("@/assets/style/common.less");
</style>
<style scoped lang="less">
.productEntryBox{
    padding: 0px 16px 16px 16px;
    width: 100%;
    height: 100%;
    // border: 1px solid red;
}
.entryDetails{
    width: 100%;
    height: 100%;
    // border: 1px solid red;
    display: flex;
    flex-direction: row;

    table{
        width: 736px;
        height: 100%;
        border: 1px solid #E7E7E7;
        

        tr{
            border: 1px solid #E7E7E7;
        }
        td{
            border: 1px solid #E7E7E7;
            text-align: center;
            color: var(--text-icon-font-gy-340-placeholder, rgba(0, 0, 0, 0.40));
            /* 五级文字/常规 */
            font-family: Microsoft YaHei;
            font-size: 12px;
            font-style: normal;
            font-weight: 400;
        }

        .tableTitle{
            width: 96px;
            background-color: #F9F9F9;
        }
    }

    .details{
        height: 100%;
        width: calc(100% - 736px);
        // border: 1px solid red;
        padding-left: 16px;
        position: relative;

        div{
            color: var(--text-icon-font-gy-190-primary, rgba(0, 0, 0, 0.90));
            /* 五级文字/常规 */
            font-family: Microsoft YaHei;
            font-size: 12px;
            font-style: normal;
            font-weight: 400;
            line-height: 20px;
        }
        
        .btnBox{
            position: absolute;
            bottom: 0px;
        }
    }
}

</style>