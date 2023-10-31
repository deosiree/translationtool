<template>
    <Modal 
    :modalWidth="modalWidth" 
    :visible="visible" 
    :modalTitle="modalTitle"
    :showOk="false"
    cancelText="关闭"
    @handleClose="handleClose"
    @handleOK="handleOK"
    >
        <div style="width:100%;height:350px">
            <a-form
            :model="search"
            layout="inline"
            autocomplete="off"
            >
                <a-form-item
                label="操作人"
                name="operator"
                >
                    <a-input v-model:value="search.operator" placeholder="请输入操作人" size="small"></a-input>
                </a-form-item>
                <a-form-item
                label="操作日期"
                name="operateTime"
                >
                    <a-range-picker v-model:value="operateTime" 
                    size="small" 
                    :locale="locale"
                    @change="changePicker"
                    style="width:230px"
                    />
                </a-form-item>
                <a-form-item>
                    <a-button type="primary" size="middle" style="margin-left:10px" @click="getDataSource">查询</a-button>
                    <a-button type="primary" size="middle" style="margin-left:10px;background-color:#36BF7D;border:#36BF7D" @click="reset">重置</a-button>
                </a-form-item>
            </a-form>
            <div class="table">
                <a-table 
                class="ant-table-striped"
                :columns="columns" 
                :data-source="dataSource"
                :scroll="{x:'100%' , y: '280px'}"
                :pagination='false'
                :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
                ref="historyTable"
                bordered>
                </a-table>
            </div>
        </div>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import locale from 'ant-design-vue/es/date-picker/locale/zh_CN';
import { 
    getOperateByEntryId
} from "@/http/api/entry";
export default {
    components:{
        Modal
    },
    emits:['historyClose'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String
        }
    },
    watch: {
        
    },
    data() {
        return{
            locale: locale,
            modalWidth:"60%",
            entryId:'',
            dataSource:[],
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:60,customRender: (text, record, index, column) => {
                    return text.index + 1
                }},
                {title: '操作人',dataIndex: 'operator',align:'center',width:80,},
                {title: '操作时间',dataIndex: 'operateTime',align:'center',width:180,},
                {title: '操作类型',dataIndex: 'type',align:'center',width:80,},
                {title: '操作内容',dataIndex: 'operateContent',align:'center',},
                {title: '备注',dataIndex: 'notes',align:'center',width:180,},
            ],
            search:{
                operator:'',
                startOperateTime:'',
                endOperateTime:'',
                entryId:''
            },
            operateTime:[],
        }
    },
    
    created() {
        
    },
    mounted () {
        
    },
    methods: {
        handleClose(){
            this.$emit("historyClose")
        },
        handleOK(){
             this.$emit("historyClose")
        },
        // 初始化
        init(entryId){
            // console.log("init:",entryId)
            this.entryId = entryId
            this.getDataSource()
        },
        // 查询词条操作历史
        getDataSource(){
            this.search.entryId = this.entryId
            getOperateByEntryId(this.search).then((res) => {
                this.dataSource = res.data.list
            })
        },
        reset(){
            this.search = {
                operator:'',
                startOperateTime:'',
                endOperateTime:'',
                entryId:''
            }
            this.operateTime = []
            this.getDataSource()
        },
        // 日期范围选择器改变时触发
        changePicker(value, dateString){
            if(dateString.length > 1){
                this.search.startOperateTime = dateString[0]
                this.search.endOperateTime = dateString[1]
            }
        },
        clearForm(){
            this.search.operator = ""
            this.operateTime = []
        }
    }
}
</script>
<style lang="less" scoped>
.table{
    width: 100%;
    margin-top: 5px;
    position: relative;
}
</style>