<template>
    <CustomModal
    :visible="visible" 
    :modalTitle="modalTitle"
    :modalWidth="modalWidth"
    :fullFlag="true"
    :okLoading="saveLoading"
    :showCancel="false"
    okText="归档并结束任务"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    @setTableHeight="setTableHeight"
    >
        <div class="content">
            <div class="taskInfo">
                <div class="taskItem">任务名称：{{task.name}}</div>
                <div class="taskItem">产品名称：{{task.productName}}</div>
                <div class="taskItem">上级分类名称：{{task.classifyName}}</div>
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
                placeholder="请选择"
                allowClear
                >
                    <a-select-option value="1">待审核</a-select-option>
                    <a-select-option value="2">审核不通过</a-select-option>
                    <a-select-option value="3">已审核</a-select-option>
                </a-select>
                <span style="margin-left:10px">翻译状态：</span>
                <a-select
                v-model:value="translateState"
                size="small"
                style="width: 300px"
                placeholder="请选择"
                allowClear
                >
                    <a-select-option value="0">未翻译</a-select-option>
                    <a-select-option value="1">未审核</a-select-option>
                    <a-select-option value="2">审核不通过</a-select-option>
                    <a-select-option value="3">已审核</a-select-option>
                </a-select>
                <a-button type="primary" size="small" style="margin-left:8px" @click="getTaskEntry">查询</a-button>
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
                    <a-button type="primary" size="small" style="margin-left:auto"><template #icon><SettingOutlined /></template>展示列</a-button>
                </a-popover>
            </div>
            <a-table 
            bordered
            class="ant-table-striped"
            :columns="columns" 
            :data-source="dataSource" 
            :row-key="record => record.id"
            :scroll="tableHeight"
            :pagination='pagination'
            :loading="loading"
            :rowClassName="getRowClassName"
            :expandIconColumnIndex="2"
            :customRow="customRow"
            :row-selection="{ 
                selectedRowKeys: selectedRowKeys, 
                selectedRows: selectedRows,
                onChange: onSelectChange,
                selections:[
                    {key:'selectAll',text:'全部选择',onSelect:selectAllEntry},
                    {key:'clearAll',text:'取消选择',onSelect:clearAllEntry}
                ]
            }"
            ref="archiveTable"
            @resizeColumn="handleResizeColumn"
            @change="handleTableChange"
            >
                <template #bodyCell="{ column, record,text }">
                    <template v-if="column.dataIndex === 'entry'">
                        <span v-text="text.replace(/\n/g, '\\n')"></span>
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
                    <template v-if="['englishTranslateState','frenchTranslateState','russianTranslateState','spanishTranslateState'].includes(column.dataIndex)">
                        <template v-if="record[column.dataIndex] === '0' || record[column.dataIndex] === null">
                            <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">未翻译</span>
                        </template>
                        <template v-if="record[column.dataIndex] === '1'">
                            <a-badge color="#FBB31F" /><span style="color:#FBB31F">未审核</span>
                        </template>
                        <template v-if="record[column.dataIndex] === '2'">
                            <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
                        </template>
                        <template v-if="record[column.dataIndex] === '3'">
                            <a-badge color="#36BF7D" /><span style="color:#36BF7D">审核通过</span>
                        </template>
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
                <!-- 设置筛选菜单 -->
                <template
                #customFilterDropdown="{ setSelectedKeys, selectedKeys, confirm, clearFilters, column }"
                >
                    <div style="padding: 8px">
                        <a-input
                        ref="searchInput"
                        :placeholder="`搜索 ${column.title}`"
                        :value="selectedKeys[0]"
                        style="width: 188px; margin-bottom: 8px; display: block"
                        @change="e => setSelectedKeys(e.target.value ? [e.target.value] : [])"
                        @pressEnter="handleSearch(selectedKeys, confirm, column.dataIndex)"
                        />
                        <a-button
                        type="primary"
                        size="small"
                        style="width: 90px; margin-right: 8px"
                        @click="handleSearch(selectedKeys, confirm, column.dataIndex)"
                        >
                        <template #icon><SearchOutlined /></template>搜索</a-button>
                        <a-button size="small" style="width: 90px" @click="handleReset(clearFilters)">重置</a-button>
                    </div>
                </template>
                <!-- 设置筛选图标 -->
                <template #customFilterIcon="{ filtered }">
                    <SearchOutlined :style="{ color: filtered ? '#108ee9' : undefined }" />
                </template>
            </a-table>
        </div>
        <template v-slot:leftBottomBtn>
            <a-button @click="handleClose">取消</a-button>
            <a-button type="primary" ghost @click="placeOnFile">归档</a-button>
        </template>
    </CustomModal>
    <CustomModal
    :visible="ipSelectModal" 
    modalTitle="回写服务器"
    @handleClose="ipSelectClose"
    @handleOK="ipSelectOK"
    @afterClose="ipSelectAfterClose"
    >
        <div style="width:100%;height:100%">
            <a-form
                ref="ipModal"
                name="custom-validation"
                :model="ipModal"
            >
                <a-form-item
                label="IP"
                name="ip"
                :rules="[{ required: true, message: '请选择IP!' }]"
                >
                    <a-select
                    v-model:value="ipModal.ip"
                    :options="ipOptions"
                    placeholder="请选择"
                    ></a-select>
                </a-form-item>
            </a-form>
        </div>
    </CustomModal>
</template>
<script>
import CustomModal from '@/components/modal/index.vue';
import { cloneDeep, iteratee } from 'lodash-es';
import {
    getEntryInfoList,
    getI18nAdress
} from '@/http/api/workbench'
import {
    updateTaskInfo
} from '@/http/api/task'
import {
    setInfo
}from '@/http/api/i18Server'
import {
    CheckOutlined,
    CloseOutlined,
    ExclamationCircleOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    SettingOutlined,
    SearchOutlined
} from '@ant-design/icons-vue';
import { message ,Modal} from 'ant-design-vue';
import workbenchCommon from '@/views/workbench/common.js';
import common from '../entry/common.js';
import { defineComponent, ref, createVNode } from 'vue';
export default {
    components:{
        CheckOutlined,
        CloseOutlined,
        CaretDownOutlined,
        CaretRightOutlined,
        SettingOutlined,
        SearchOutlined,
        ExclamationCircleOutlined,
        CustomModal
    },
    emits:['handleClose','handleOK','refresh'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:'归档预览'
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
                },fixed: 'left',index:0},
                {title: 'Abbr',dataIndex: 'abbr',align:'center',fixed: 'left',width:150,resizable: true,index:2},
                {title: '词条',dataIndex: 'entry',width:200,resizable: true,index:3,align:'center',},
                {title: '翻译',dataIndex: 'translate',align:'center',width:200,resizable: true,index:4},
                {title: '翻译状态',dataIndex: 'translateState',align:'center',width:130,resizable: true,index:4.5},
                {title: '中文释义',dataIndex: 'chineseInterpretation',align:'center',width:200,resizable: true,index:5},
                {title: '英文释义',dataIndex: 'englishInterpretation',align:'center',width:200,resizable: true,index:6},
                {title: '审核意见',dataIndex: 'auditSuggess',align:'center',width:200,resizable: true,index:98},
                {title: '词条状态',dataIndex: 'entryState',align:'center',width:130,fixed: 'right',index:99},
            ],
            dataSource:[],
            pagination:{
                pageSizeOptions:['20','50','100'],
                showSizeChanger: true,
                defaultPageSize:20,
                total:0,
                current:1,
                pageSize:20,
                showTotal:total => `共 ${total} 条`,
                onChange: this.pageChange
            },
            entryState:null,
            translateState:null,
            selectedRowIndex: null,
            overlayStyle: workbenchCommon.overlayStyle,
            checkedColumn: workbenchCommon.checkedColumn,
            checkboxList: workbenchCommon.checkboxList,
            state:{
                searchText: '',
                searchedColumn: '',
            },
            filters:null,
            filteredData:[],
            saveLoading: false,
            selectedRowKeys: [],
            selectedRows: [],
            ipSelectModal: false,
            ipModal:{
                ip: null
            },
            ipOptions: [],
            optionFlag: 0,
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
                if(item.title === '翻译状态'){
                    item.dataIndex = workbenchCommon.languageMap[this.task.translateType].code + "TranslateState"
                }
            })
        },
        // 获取词条
        getTaskEntry(){
            let params = {
                taskID: this.task.id,
                entry: this.keyWords,
                entryState: this.entryState
            }
            let data = []
            if(this.translateState){
                data.push(this.translateState)
            }
            this.loading = true
            
            getEntryInfoList(params,data).then((res) => {
                this.dataSource = res.data.list
                this.loading = false
            }).catch((err) => {
                this.loading = false
            })
        },
        handleOK(){
            // 归档
            // 获取全部词条
            let params = {
                taskID: this.task.id,
                entry: '',
            }
            let data = []
            this.saveLoading = true
            getEntryInfoList(params,data).then((res) => {
                this.checkedEntry(res.data.list)
            })
        },
        checkedEntry(data){
            if(!data){
                return
            }
            let code = workbenchCommon.languageMap[this.task.translateType].code + "TranslateState"
            let flag = false
            data.forEach(item => {
                if(item.entryState != 3 || item[code] != 3){
                    // 含有未处理完的词条
                    flag = true
                }
            })
            
            if(flag){
                this.saveLoading = false
                Modal.error({
                    title: '当前任务存在未处理完的词条，不可归档！',
                    style: {top:'30%'}
                });
            }else{
                this.optionFlag = 1
                this.ipSelectModal = true
                this.getIPs()
                // Modal.confirm({
                //     title: '是否确定归档并结束任务?',
                //     icon: createVNode(ExclamationCircleOutlined),
                //     okText: '是',
                //     cancelText: '否',
                //     style: {top:'30%'},
                //     onOk: () => {
                //         this.saveLoading = false
                //         this.task.state = '6'
                //         this.task.endTime = new Date().toLocaleString().replaceAll('/','-')
                //         updateTaskInfo(this.task).then((res) => {
                //             message.success("已归档！")
                //             this.$emit('refresh')
                //         })
                //         // 回写数据
                //         let params = {
                //             taskID: this.task.id,
                //             translateType: this.task.translateType,
                //             isTag:0,
                //             isComment:0
                //         }
                //         setInfo(params,[]).then((res) => {

                //         }).catch((err) => {
                            
                //         })
                //     },
                //     onCancel: () => {
                //         this.saveLoading = false
                //     }
                // });
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
        // 添加表格行点击事件
        customRow(record, index){
            return {
                onClick: (event) => {
                    // this.selectedRowIndex = record.id
                },
            }
        },
        afterClose(){
            this.keyWords = ""
            this.pagination.current = 1
            this.pagination.pageSize = 20
            this.clearFilters()
        },
        // 展示列设置
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
                    if(newCol.dataIndex === 'abbr'){
                        newCol.fixed = 'left'
                    }
                    if(newCol.dataIndex === 'entrySource'){
                        // 添加词条来源可筛选
                        newCol.width = 250,
                        newCol.customFilterDropdown = true
                        newCol.filteredValue = null,
                        newCol.onFilter = eval('(value, record) => record.entrySource.toString().toLowerCase().includes(value.toLowerCase())')
                    }
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
        // 列筛选
        handleSearch(selectedKeys, confirm, dataIndex){
            confirm();
            this.state.searchText = selectedKeys[0];
            this.state.searchedColumn = dataIndex;
        },
        handleReset(clearFilters){
            clearFilters({ confirm: true });
            this.state.searchText = '';
        },
        // 动态设置表格高度
        setTableHeight(height,type){
            if(type === 'full'){
                this.tableHeight.y = height - 230
            }else if(type === 'reduce'){
                this.tableHeight.y = 415
            }
        },
        // 分页切换
        pageChange(page,pageSize){
            this.pagination.current = page
            this.pagination.pageSize = pageSize
        },
        // 表格change事件
        handleTableChange(pagination, filters) {
            this.filters = filters
            for (let key in filters) {
                this.columns.forEach(col => {
                    if(col.dataIndex === key){
                        col.filteredValue = filters[key]
                    }
                })
			}
            // 获取筛选后的数据
            let isExistData = this.dataSource.filter(item => {
                return filters.isExist && filters.isExist.includes(item.isExist);
            });
            let sourceData = this.dataSource.filter(item => {
                return filters.entrySource && item.entrySource.includes(filters.entrySource);
            });
            this.filteredData = this.intersection(isExistData,sourceData)
        },
        // 两个数组取并集
        intersection(nums1, nums2) {
            if(nums1.length === 0){
                return nums2
            }
            if(nums2.length === 0){
                return nums1
            }
            let a=new Set(nums1);
            let b=new Set(nums2);
            let arr = Array.from(new Set([...b].filter(x => a.has(x))));
            return arr;
        },
        // 清空表格筛选条件
        clearFilters(){
            if(this.filters){
                for (let key in this.filters) {
                    this.columns.forEach(col => {
                        if(col.dataIndex === key){
                            col.filteredValue = null
                        }
                    })
                }
            }
        },
        onSelectChange(selectedRowKeys,selectedRows){
            this.selectedRowKeys = selectedRowKeys
            this.selectedRows = selectedRows
        },
        selectAllEntry(){
            this.selectedRowKeys = []
            this.selectedRows = []
            this.dataSource.forEach(item => {
                this.selectedRowKeys.push(item.id)
                this.selectedRows.push(item)
            })
        },
        clearAllEntry(){
            this.selectedRowKeys = []
            this.selectedRows = []
        },
        // 归档  回写数据
        writeBackFun(){
            if(this.selectedRows.length === 0){
                message.info("请选择词条")
                return
            }
            // 回写数据
            let params = {
                taskID: this.task.id,
                translateType: this.task.translateType,
                isTag:0,
                isComment:0,
                i18nUrl: this.ipModal.ip
            }
            setInfo(params,this.selectedRows).then((res) => {
                message.success("归档成功！")
                this.selectedRowKeys = []
                this.selectedRows = []
                this.ipSelectModal = false
            }).catch((err) => {
                message.error("归档失败！")
            })
        },
        // 归档、归档并结束任务按钮点击事件
        placeOnFile(){
            if(this.selectedRows.length === 0){
                message.info("请选择词条")
                return
            }
            this.ipSelectModal = true
            this.optionFlag = 0
            this.getIPs()
        },
        ipSelectAfterClose(){
            this.ipModal.ip === null
        },
        ipSelectOK(){
            this.$refs.ipModal.validate().then(() => {
                if(this.optionFlag === 0){
                    // 归档
                    this.writeBackFun()
                }else if(this.optionFlag === 1){
                    // 归档并结束任务
                    this.saveLoading = false
                    this.task.state = '6'
                    this.task.endTime = common.getCurrentFormattedTime()
                    updateTaskInfo(this.task).then((res) => {
                        message.success("已归档！")
                        this.$emit('refresh')
                    })
                    // 回写数据
                    let params = {
                        taskID: this.task.id,
                        translateType: this.task.translateType,
                        isTag:0,
                        isComment:0,
                        i18nUrl: this.ipModal.ip
                    }
                    setInfo(params,[]).then((res) => {
                        this.ipSelectModal = false
                    }).catch((err) => {
                        
                    })
                }
            }).catch(err => {

            })
            
        },
        ipSelectClose(){
            this.ipSelectModal = false
            this.saveLoading = false
            this.ipModal.ip = null
        },
        // 获取i18服务器ip
        getIPs(){
            this.ipOptions = []
            getI18nAdress().then((res) => {
                res.data.list.forEach(item => {
                    let ip = {
                        label: item.ip,
                        value: item.ip
                    }
                    if(item.state === '1'){
                        this.ipModal.ip = item.ip
                    }
                    this.ipOptions.push(ip)
                })
            })
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
}
.ant-table-cell .ant-form-item{
    margin-bottom: 0%;
}
:deep(.ant-pagination) {
    margin: 8px 0;
}
</style>