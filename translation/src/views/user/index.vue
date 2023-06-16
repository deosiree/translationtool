<template>
    <div class="search">
        <a-input v-model:value="userName" placeholder="请输入用户名称" />
        <a-button type="primary" size="middle" style="margin-left:10px">查询</a-button>
        <div>
            <a-button type="primary" size="middle" style="margin-left:10px">
                <template #icon><PlusOutlined /></template>
                新增
            </a-button>
            <a-button type="primary" size="middle" style="margin-left:10px">
                <template #icon><DeleteOutlined /></template>
                批量删除
            </a-button>
        </div>
    </div>
    <a-table 
    :columns="columns" 
    :data-source="dataSource" 
    :row-selection="rowSelection"
    :customRow="doubleClick"
    bordered>
        <template v-for="col in ['name', 'age', 'address']" #[col]="{ text, record }" :key="col">
        <div>
            <a-input
                v-if="editableData[record.key]"
                v-model:value="editableData[record.key][col]"
                style="margin: -5px 0"
            />
            <template v-else>
            {{ text }}
            </template>
        </div>
        </template>
        <template #operation="{ record }">
        <div class="editable-row-operations">
            <span v-if="editableData[record.key]">
            <a @click="save(record)">Save</a>
            <a-popconfirm title="Sure to cancel?" @confirm="cancel(record.key)">
                <a>Cancel</a>
            </a-popconfirm>
            </span>
            <span v-else>
            <a @click="edit(record.key)">Edit</a>
            </span>
        </div>
        </template>
    </a-table>
</template>
<script>
import {
  PlusOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue';
export default {
    components:{
        PlusOutlined,
        DeleteOutlined
    },
    data() {
        return{
            userName:"",
            columns:[
                {
                    title: 'name',
                    dataIndex: 'name',
                    width: '25%',
                    slots: { customRender: 'name' },
                },
                {
                    title: 'age',
                    dataIndex: 'age',
                    width: '15%',
                    slots: { customRender: 'age' },
                },
                {
                    title: 'address',
                    dataIndex: 'address',
                    width: '40%',
                    slots: { customRender: 'address' },
                },
                {
                    title: 'operation',
                    dataIndex: 'operation',
                    slots: { customRender: 'operation' },
                },
            ],
            dataSource: [
                {
                    key: '1',
                    name: 'John Brown',
                    age: 32,
                    address: 'New York No. 1 Lake Park',
                },
                {
                    key: '2',
                    name: 'Joe Black',
                    age: 42,
                    address: 'London No. 1 Lake Park',
                },
                {
                    key: '3',
                    name: 'Jim Green',
                    age: 32,
                    address: 'Sidney No. 1 Lake Park',
                },
                {
                    key: '4',
                    name: 'Jim Red',
                    age: 32,
                    address: 'London No. 2 Lake Park',
                },
            ],
            editableData:{},
            rowSelection:[]
        }
    },
    methods: {
        onSelectChange(){
        
        },
        edit(key){
            this.editableData[key] = this.dataSource.filter(item => key === item.key)[0]
        },
        cancel(key){
            delete this.editableData[key];
        },
        save(record){
            console.log(record)
        },
        //双击表格行 可编辑
        doubleClick(record, index){
            return {
                onDblclick: (event) => {
                    this.editableData[record.key] = this.dataSource.filter(item => record.key === item.key)[0]
                }
            }
        },
        
    },
}
</script>
<style scoped>
.editable-row-operations a {
  margin-right: 8px;
}
.search{
    height: 40px;
}
.search input{
    width: 30%;
}
.search div{
    float: right;
}
</style>