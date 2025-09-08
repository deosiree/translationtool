<template>
    <Modal 
    :modalWidth="modalWidth" 
    :visible="visible" 
    :modalTitle="modalTitle"
    @handleClose="handleClose"
    @handleOK="handleOK"
    >
        <a-tabs v-model:activeKey="activeTab" ref="tab" @change="changeTab" >
            <a-tab-pane key="noMerge" tab="未合并"></a-tab-pane>
            <a-tab-pane key="merge" tab="已合并"></a-tab-pane>
        </a-tabs>
        <div style="overflow:hidden">
            <a-form
            layout="inline"
            autocomplete="off"
            style="float:left"
            >
                <a-form-item
                name="operator"
                >
                    <a-input v-model:value="chinese" placeholder="请输入词条名称" style="width:300px">
                        <template #prefix>
                            <SearchOutlined />
                        </template>
                    </a-input>
                </a-form-item>
                <a-form-item>
                    <a-button type="primary" size="middle" style="margin-left:10px" @click="search">查询</a-button>
                </a-form-item>
            </a-form>
            <div style="float:right">
                <a-button type="primary" size="middle" style="margin-left:10px" v-if="activeTab === 'noMerge'" @click="merge">
                    <template #icon><LinkOutlined /></template>
                    合并
                </a-button>
                <a-button type="primary" size="middle" style="margin-left:10px" v-else @click="split">
                    <template #icon><ScissorOutlined /></template>
                    拆分
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
                    <a-button type="primary" style="margin-left:10px">
                        <template #icon><SettingOutlined /></template>
                        显示列设置
                    </a-button>
                </a-popover>
            </div>
        </div>
        <div class="table">
            <a-table 
            v-if="activeTab === 'noMerge'"
            class="ant-table-striped"
            :loading="loading"
            :columns="columns" 
            :data-source="dataSource"
            :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            :row-key="record => record.id"
            :scroll="{x:'100%' , y: '245px'}"
            :pagination='false'
            :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
            ref="mergeTable"
            bordered>
                <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'entryState'">
                        <template v-if="record.entryState === 0">
                            <a-badge color="red" /><span style="color:red">未通过</span>
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
                </template>
            </a-table>

            <a-table 
            v-else
            class="ant-table-striped"
            :loading="loading"
            :columns="columns" 
            :data-source="dataSource"
            :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            :row-key="record => record.repeatEntryId"
            :scroll="{x:'100%' , y: '245px'}"
            :pagination='false'
            :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
            ref="mergeTable"
            bordered>
                <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'entryState'">
                        <template v-if="record.entryState === 0">
                            <a-badge color="red" /><span style="color:red">未通过</span>
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
                </template>
            </a-table>
        </div>
    </Modal>
</template>
<script>
import { entryParams as tableParam } from "@/utils/commonParam.js";
import Modal from '@/components/modal/index.vue';
import {
  SearchOutlined,
  LinkOutlined,
  SettingOutlined,
  ScissorOutlined
} from '@ant-design/icons-vue';
import { 
    getEntryNoMerge,
    getEntryMerge,
    entryMerge,
    mergerSplit
} from "@/http/api/entry";
import { message } from 'ant-design-vue';
export default {
    components:{
        Modal,
        SearchOutlined,
        LinkOutlined,
        SettingOutlined,
        ScissorOutlined
    },
    emits:['mergeClose'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:'词条合并'
        }
    },
    watch: {
        
    },
    data() {
        return{
            loading:false,
            modalWidth:"60%",
            dataSource:[],
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:70,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left'},
                {title: 'Abbr',dataIndex: 'abbr',align:'center',width:150,fixed: 'left'},
                {title: '状态',dataIndex: 'entryState',align:'center',width:150},
                {title: '中文术语',dataIndex: 'chinese',align:'center',width:200,ellipsis: true,},
                {title: '中文释义',dataIndex: 'chineseInterpretation',align:'center',width:200,ellipsis: true,},
                {title: '词性备注',dataIndex: 'partOfSpeech',align:'center',width:200},
            ],
            chinese:"",
            activeTab:'noMerge',
            overlayStyle: tableParam.overlayStyle,
            checkboxList: tableParam.checkboxList,
            checkedColumn: tableParam.checkedColumn,
            selectedRowKeys:[],
            selectedRows:[]
        }
    },
    
    created() {
        
    },
    mounted () {
        
    },
    methods: {
        init(){
            this.activeTab = 'noMerge'
            // 查询需要合并的数据
            this.getDataSource()
        },
        search(){
            if(this.activeTab === 'noMerge'){
                this.getDataSource()
            }else{
                this.getMergedData()
            }
        },
        // 查询需要合并的数据
        getDataSource(){
            this.loading = true
            this.selectedRowKeys = []
            this.selectedRows = []
            let params = {
                chinese:this.chinese
            }
            getEntryNoMerge(params).then((res) => {
                this.dataSource = res.data.list
                this.loading = false
            })
        },
        // 查询已合并数据
        getMergedData(){
            this.loading = true
            this.selectedRowKeys = []
            this.selectedRows = []
            let params = {
                chinese:this.chinese
            }
            getEntryMerge(params).then((res) => {
                this.dataSource = res.data.list
                this.loading = false
            })
        },
        handleClose(){
            this.$emit("mergeClose")
        },
        handleOK(){
             this.$emit("mergeClose")
        },
        // tab页切换
        changeTab(activeKey){
            this.activeTab = activeKey
            if(this.activeTab === 'noMerge'){
                this.getDataSource()
            }else{
                this.getMergedData()
            }
        },
        // 词条合并
        merge(){
            if(this.selectedRows.length < 2){
                message.warn("单个词条无法合并！")
                return
            }
            entryMerge(this.selectedRows).then((res) => {
                message.success('合并成功！')
                this.search()
            })
        },
        // 词条拆分
        split(){

            let data = []
            this.selectedRowKeys.forEach(key => {
                this.dataSource.forEach(item => {
                    if(item.repeatEntryId === key){
                        data.push(item.id)
                    }
                })
            })
            mergerSplit(data).then((res) => {
                message.success('拆分成功！')
                this.search()
            })
        },
        onSelectChange(selectedRowKeys,selectedRows){
            this.selectedRowKeys = selectedRowKeys
            this.selectedRows = selectedRows
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
                    };
                    // this.columns.splice(-1, 0, newCol);
                    this.columns.push(newCol)
                }
                if (nowColumnIndex !== -1 && checkedIndex === -1) {
                    this.columns.splice(nowColumnIndex, 1);
                }
            });
        },
    }
}
</script>
<style lang="less" scoped>
.table{
    width: 100%;
    margin-top: 5px;
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
</style>