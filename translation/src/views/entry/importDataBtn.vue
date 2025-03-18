<template>
    <a-upload
        name="file"
        :beforeUpload="beforeUpload"
        :accept="accept"
        :showUploadList="false"
        @change="handleChange"
    >
        <a-button type="primary" size="small" style="margin-left:10px">
            <template #icon><DownloadOutlined /></template>
            词条导入
        </a-button>
    </a-upload>
    <Modal 
    :modalWidth="modalWidth" 
    :visible="visible" 
    :modalTitle="modalTitle"
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
import { message } from 'ant-design-vue';
import Modal from '@/components/modal/index.vue';
import { DownloadOutlined } from '@ant-design/icons-vue';
import { 
    importExcle,
    bachAddEntry
} from "@/http/api/entry";
export default {
    components:{
        DownloadOutlined,
        Modal
    },
    data(){
        return{
            accept:".xls,.xlsx",
            visible:false,
            modalWidth:"60%",
            modalTitle:"导入数据预览",
            loading:false,
            dataSource:[],
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:70,customRender: (text, record, index, column) => {
                    return text.index + 1 + this.pagination.pageSize*(this.pagination.current-1);
                },fixed: 'left'},
                {title: '版本',dataIndex: 'version',align:'center',width:150,fixed: 'left'},
                {title: 'Abbr',dataIndex: 'abbr',align:'center',width:150,fixed: 'left'},
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
            ],
            pagination:{
                total: 0,
                current: 1,
                pageSize: 20,
                showTotal: (total) => `共 ${total} 条数据`
            }
        }
    },
    mounted(){

    },
    methods:{
        beforeUpload(){
            // console.log("before");
            return false
        },
        handleChange(info){
            let formData = new FormData()
            formData.append('multipartFile',info.file)
            this.visible = true
            this.loading = true
            importExcle(formData).then((res) => {
                this.dataSource = res.data.list
                this.pagination.total = this.dataSource.length
                this.loading = false
            })
        },
        handleTableChange(pagination){
            this.pagination.current = pagination.current
            this.pagination.total = pagination.total
        },
        close(){
            this.visible = false
        },
        ok(){
            // 插入数据
            this.dataSource.forEach(item => {
                item.state = 1
            })
            bachAddEntry(this.dataSource).then((res) => {
                message.success(res.data)
                this.visible = false
            })
        },
        afterClose(){
            this.dataSource = []
            this.pagination.total = 0
            this.pagination.current = 1
        }
    }
}
</script>
<style lang="less" scoped>
    :deep(.ant-pagination){
        margin-bottom: 0px;
    }
</style>>
