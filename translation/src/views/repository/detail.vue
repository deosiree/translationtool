<template>
    <Modal 
    :modalWidth="modalWidth" 
    :visible="visible" 
    :modalTitle="modalTitle"
    :showOk="false"
    cancelText="关闭"
    @handleClose="close"
    @handleOK="ok"
    @afterClose="afterClose"
    >
        <a-table 
        class="ant-table-striped"
        :loading="loading"
        :columns="columns" 
        :data-source="dataSource"
        :row-key="record => record.id"
        :scroll="{x:'100%' , y: '350px'}"
        :pagination='pagination'
        @change="handleTableChange"
        :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
        ref="table"
        bordered>
            
        </a-table>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import locale from 'ant-design-vue/es/date-picker/locale/zh_CN';
import { 
    getVersionTable
} from "@/http/api/versionTable";
export default {
    components:{
        Modal
    },
    emits:['detailClose'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default: '查看'
        },
    },
    watch: {
        
    },
    data() {
        return{
            locale: locale,
            modalWidth:"60%",
            loading:false,
            dataSource: [],
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:70,customRender: (text, record, index, column) => {
                    return text.index + 1 + this.pagination.pageSize*(this.pagination.current-1);
                },fixed: 'left'},
                {title: '版本',dataIndex: 'version',align:'center',width:150,fixed: 'left'},
                {title: '创建日期',dataIndex: 'createTime',align:'center',width:150,},
                {title: '创建人',dataIndex: 'creator',align:'center',width:150,},
                {title: '词性备注',dataIndex: 'partOfSpeech',align:'center',width:150,},
                {title: '中文释义',dataIndex: 'chineseInterpretation',align:'center',width:200,ellipsis: true,},
                {title: '英文释义',dataIndex: 'englishInterpretation',align:'center',width:200,ellipsis: true,},
                {title: '中文术语', children: [
                    {title: '术语',dataIndex: 'chinese',align:'center',width:200,ellipsis: true,},
                    {title: '字符数',dataIndex: 'chineseLength',align:'center',width:150,},
                ]},
                {title: '英文术语', children: [
                    {title: '术语',dataIndex: 'english',align:'center',width:200,ellipsis: true,},
                    {title: '字符数',dataIndex: 'englishLength',align:'center',width:150,},
                ]},
                {title: '西文术语', children: [
                    {title: '术语',dataIndex: 'spanish',align:'center',width:200,ellipsis: true,},
                    {title: '字符数',dataIndex: 'spanishLength',align:'center',width:150,},
                ]},
                {title: '俄文术语', children: [
                    {title: '俄文术语',dataIndex: 'russian',align:'center',width:200,ellipsis: true,},
                    {title: '俄文字符数',dataIndex: 'russianLength',align:'center',width:150,},
                ]},
                {title: '法文术语', children: [
                    {title: '法文术语',dataIndex: 'french',align:'center',width:200,ellipsis: true,},
                    {title: '法文字符数',dataIndex: 'frenchLength',align:'center',width:150,},
                ]},
                {title: 'Abbr',dataIndex: 'abbr',align:'center',width:150},// ,fixed: 'left'
            ],
            pagination:{
                total: 0,
                current: 1,
                pageSize: 20,
                showTotal: (total) => `共 ${total} 条数据`
            },
            version:""
        }
    },
    
    created() {
        
    },
    mounted () {
        
    },
    methods: {
        close(){
            this.$emit("detailClose")
        },
        ok(){
            // console.log(this.version)
             this.$emit("detailClose")
        },
        // 初始化
        init(version){
            this.version = version
            this.getDataSource()
        },
        // 查询词条操作历史
        getDataSource(){
            let params = {
                version: this.version,
                pageIndex: -1,
                pageSize: -1
            }
            getVersionTable(params).then((res) => {
                this.dataSource = res.data.list
            })
        },
        handleTableChange(pagination){
            this.pagination.current = pagination.current
            this.pagination.total = pagination.total
        },
        afterClose(){

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