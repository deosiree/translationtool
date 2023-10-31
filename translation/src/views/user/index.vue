<template>
<div style="width:100%;height:100%;" ref="box">
    <SearchForm ref="searchForm" @change="setTableHeight">
        <template v-slot:form>
            <a-row>
                <a-col :span="16">
                    <a-form
                        :model="search"
                        name="horizontal_login"
                        layout="inline"
                        autocomplete="off"
                    >
                        <a-form-item
                        label="用户名"
                        name="userName"
                        >
                            <a-input v-model:value="search.userName" placeholder="请输入用户名"></a-input>
                        </a-form-item>
                        <a-form-item
                        label="部门"
                        name="department"
                        >
                            <a-input v-model:value="search.department" placeholder="请输入部门"></a-input>
                        </a-form-item>
                        <a-form-item
                        label="角色"
                        name="roleId"
                        >
                            <a-select
                            v-model:value="search.roleId"
                            style="width: 180px"
                            placeholder="请选择角色"
                            >
                                <template v-for="(item,index) in roles" :key="index">
                                    <a-select-option :value="item.id">{{item.roleName}}</a-select-option>
                                </template>
                            </a-select>
                        </a-form-item>
                    </a-form>
                </a-col>
                <a-col :span="8">
                    <div class="operation">
                        <a-button type="primary" size="middle" style="margin-left:10px" @click="getUserList">查询</a-button>
                        <a-button type="primary" size="middle" @click="reset" style="margin-left:10px;background-color:#36BF7D;border:#36BF7D">重置</a-button>
                        <a-button type="primary" size="middle" style="margin-left:10px" @click="handleAdd" v-if="authority.includes('addUser')">
                            <template #icon><PlusOutlined /></template>
                            新增
                        </a-button>
                        <a-button type="primary" size="middle" style="margin-left:10px" @click="deleteBatch" v-if="authority.includes('deleteUser')">
                            <template #icon><DeleteOutlined /></template>
                            批量删除
                        </a-button>
                    </div>
                </a-col>
            </a-row>
        </template>
        
    </SearchForm>
    <a-table 
    class="ant-table-striped"
    :columns="columns" 
    :data-source="dataSource" 
    :customRow="doubleClick"
    :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
    :row-key="record => record.id"
    :scroll="tableHeight"
    :pagination='false'
    :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
    ref="userTable"
    bordered
    >
        <!-- <template v-slot:num="slotProps">
            {{(pagination.current - 1) * pagination.pageSize + slotProps.index + 1}}
            {{slotProps.index + 1}}
        </template> -->
        <template #bodyCell="{ column, text, record }">
            <template v-if="['userName', 'jobNumber', 'department','roleName','roleId'].includes(column.dataIndex)">
                <div>
                    <template v-if="editableData[record.id]">
                        <a-select
                        v-if="column.dataIndex === 'roleName'"
                        ref="select"
                        v-model:value="editableData[record.id]['roleId']"
                        style="width: 120px"
                        @change="handleChangeRole(record.id)"
                        >
                            <template v-for="(item,index) in roles" :key="index">
                                <a-select-option :value="item.id">{{item.roleName}}</a-select-option>
                            </template>
                        </a-select>
                        <a-input
                            v-else
                            v-model:value="editableData[record.id][column.dataIndex]"
                            style="margin: -5px 0"
                        />

                    </template>
                    <template v-else>
                        {{ text }}
                    </template>
                </div>
            </template>
            <template v-else-if="column.dataIndex === 'operation'">
                <div class="editable-row-operations">
                <span v-if="editableData[record.id]">
                    <a-button type="primary" ghost size="small" @click="save(record.id)">保存</a-button>
                    <a-popconfirm title="是否取消?" ok-text='是' cancel-text='否' @confirm="cancel(record.id)">
                    <a-button type="primary" ghost size="small">取消</a-button>
                    </a-popconfirm>
                </span>
                <span v-else>
                    <a-button type="primary" ghost size="small" 
                    @click="edit(record.id)" v-if="authority.includes('updateUserInfo')">编辑</a-button>
                    <a-popconfirm title="确认删除?" ok-text='是' cancel-text='否' @confirm="deleteUser(record.id)" v-if="authority.includes('deleteUser')">
                        <a-button type="primary" ghost size="small" >删除</a-button>
                    </a-popconfirm>
                </span>
                </div>
            </template>
        </template>
    </a-table>
    <Pagination ref="pagination" :total="pagination.total" @pageChange="pageChange"/>
</div>
</template>
<script>
import {
  PlusOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue';
import { cloneDeep } from 'lodash-es';
import { message,Modal } from 'ant-design-vue';
import { defineComponent, ref, createVNode } from 'vue';
import { 
    queryUser,
    addUser,
    updateUserInfo,
    deleteUser
} from "@/http/api/user";
import { queryRoleInfo } from "@/http/api/role";
import Pagination from "@/components/page/pagination.vue"
import SearchForm from '@/components/search/searchForm.vue'
export default {
    components:{
        PlusOutlined,
        DeleteOutlined,
        Pagination,
        SearchForm
    },
    data() {
        return{
            name:"user",
            selectedRowKeys:[],
            tableHeight:{ x:'100%',y: 0 },
            roles:[],
            columns:[
                {title: "序号",dataIndex: 'index',align:'center',width:70,customRender: (text, record, index, column) => {
                    return text.index + 1
                }},
                {title: '用户名',dataIndex: 'userName',align:'center',width: '15%'},
                {title: '工号', dataIndex: 'jobNumber',align:'center',width: '15%'},
                {title: '部门',dataIndex: 'department',align:'center',},
                {title: '角色',dataIndex: 'roleName',align:'center',width: '20%'},
                // {title: '操作',dataIndex: 'operation',align:'center',width:150},
            ],
            dataSource: [],
            editableData:{},
            rowSelection:[],
            pagination:{
                current: 1,
                pageSize: 20,
                total: 0
            },
            authority:[],
            search:{
                userName: '',
                department: '',
                roleId:undefined
            },
            operationFlag: false
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
    methods: {
        // 搜索框收起 展开时  设置表格高度
        setTableHeight(){
            this.$nextTick(() => {
                let box = this.$refs.box.offsetHeight
                let searchHeight = this.$refs.searchForm.$el.offsetHeight
                let paginationHeight = this.$refs.pagination.$el.offsetHeight
                this.tableHeight.y = box - searchHeight - paginationHeight - 60
            })
        },
        //初始化
        init(){
            this.setTableHeight()

            this.pagination.current = this.$refs.pagination.current
            this.pagination.pageSize = this.$refs.pagination.pageSizeOptions[0]
            //获取用户权限
            this.getAuthority()
            this.getUserList()
            this.getRoles()
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
            // 权限中含有 编辑  删除  绑定权限时 表格展示操作栏
            if(this.authority.includes('updateUserInfo') || this.authority.includes('deleteUser')){
                this.addOperationColumn()
                this.operationFlag = true
            }
        },
        // 添加操作栏
        addOperationColumn(){
            let operation = {title: '操作',dataIndex: 'operation',align:'center',width:150}
            this.columns.push(operation)
        },
        // 删除操作栏
        deleteOperationColumn(){
            if(!this.operationFlag){
                this.columns.some((item,i) => {
                    if(item.dataIndex === 'operation'){
                        this.columns.splice(i,1)
                        return true
                    }
                })
            }
        },
        //获取用户列表
        getUserList(){
            queryUser(this.search,this.pagination.current,this.pagination.pageSize).then((res) => {
                this.pagination.total = res.data.totalNum
                this.dataSource = res.data.list
            })
        },
        //分页
        pageChange(current,pageSize){
            this.pagination.current = current
            this.pagination.pageSize = pageSize
            this.getUserList();
        },
        //获取角色列表
        getRoles(){
            queryRoleInfo().then((res) => {
                this.roles = res.data.list
            })
        },
        //编辑
        edit(id){
            // if(JSON.stringify(this.editableData) !== '{}'){
            //     let tempKey
            //     for(let key in this.editableData){
            //         tempKey = key
            //     }
            //     Modal.confirm({
            //         title: '数据未保存，是否保存?',
            //         icon: createVNode(ExclamationCircleOutlined),
            //         // content: 'Bla bla ...',
            //         okText: '保存',
            //         cancelText: '取消',
            //         onOk: () => {
            //             this.save(tempKey)
            //             this.edit(id)
            //         }
            //     });
            // }else{
            //     this.editableData[id] = cloneDeep(this.dataSource.filter(item => id === item.id)[0])
            // }
            this.editableData[id] = cloneDeep(this.dataSource.filter(item => id === item.id)[0])
        },
        //取消
        cancel(id){
            delete this.editableData[id];
            if(id.startsWith("new")){
                //从dataSource中删除
                this.dataSource.some((item,i) => {
                    if(item.id === id){
                        this.dataSource.splice(i,1)
                        return true
                    }
                })
            }
            this.deleteOperationColumn()
        },
        //保存
        save(id){
            // Object.assign(this.dataSource.filter(item => id === item.id)[0], this.editableData[id]);
            if(this.editableData[id].userName === '' || this.editableData[id].userName === null){
                message.warning("请输入用户名！")
                return
            }
            if(id.startsWith('new')){
                //调用新增接口
                addUser(this.editableData[id]).then((res) => {
                    this.dataSource.filter(item => {
                        if(item.id === id){
                            item.id = res.data
                            return true
                        }
                    })
                    message.success("新增成功！")
                    delete this.editableData[id];
                    this.deleteOperationColumn()
                })
            }else{
                //调用修改接口
                updateUserInfo(this.editableData[id]).then((res) => {
                    message.success("编辑成功！")
                    Object.assign(this.dataSource.filter(item => id === item.id)[0], this.editableData[id]);
                    delete this.editableData[id];
                    this.deleteOperationColumn()
                })
            }
        },
        //双击表格行 可编辑
        doubleClick(record, index){
            return {
                onDblclick: (event) => {
                    if(this.authority.includes('updateUserInfo')){
                        this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
                    }
                }
            }
        },
        //新增
        handleAdd(){
            // 无操作栏  则添加操作栏
            if(!this.operationFlag){
                this.addOperationColumn()
            }

            const newData = {
                id: `new${this.dataSource.length + 1}`,
                userName: '',
                jobNumber: '',
                roleName: '',
                roleId:''
            };
            this.dataSource.push(newData);
            this.editableData[newData.id] = newData;
            // 滚动到最底部
            this.$nextTick(()=>{
                let container = this.$refs.userTable.$el.querySelector('.ant-table-body')
                container.scrollTop = container.scrollHeight
            })
        },
        onSelectChange(selectedRowKeys){
            this.selectedRowKeys = selectedRowKeys
        },
        // 批量删除
        deleteBatch(){
            if(this.selectedRowKeys.length === 0){
                message.warn('请选择需要删除的用户！')
                return
            }
            Modal.confirm({
                title: '是否确认删除?',
                icon: createVNode(ExclamationCircleOutlined),
                // content: 'Bla bla ...',
                okText: '确认',
                cancelText: '取消',
                onOk: () => {
                    console.log(this.selectedRowKeys)
                    deleteUser(this.selectedRowKeys).then((res) => {
                        message.success("删除成功！")
                        this.getUserList()
                        this.selectedRowKeys = []
                    })
                }
            });
        },
        // 删除
        deleteUser(id){
            console.log("删除：",id)
            let data = [id]
            deleteUser(data).then((res) => {
                message.success("删除成功！")
                 this.getUserList()
            })
        },
        handleChangeRole(id){
            let roleId = this.editableData[id].roleId
            let roleName
            this.roles.filter(item =>{
                if(item.id === roleId){
                    roleName = item.roleName
                }
            })
            this.editableData[id].roleName = roleName
        },
        // 重置
        reset(){
            this.search.userName = ''
            this.search.department = ''
            this.search.roleId = undefined

            this.pagination.current = 1
            this.$refs.pagination.current = 1

            this.getUserList()
        }
    },
}
</script>
<style lang="less">
@import url("@/assets/style/common.less");
</style>
<style scoped lang="less">
.editable-row-operations button {
  margin-right: 8px;
}
// .search{
//     height: 60px;
//     margin-bottom: 10px;
//     border-radius: 4px;
//     border: 1px solid #f0f0f0;
//     padding: 5px;

//     .searchItem{
//         width: 100%;
//         height: 100%;
//         border-radius: 4px;
//         background-color: #F3F3F3;
//         position: relative;
//         line-height: 50px;
//         padding: 0 10px;
//     }
// }
// .ant-form{
//     width: 100%;
//     position: absolute;
//     left: 50%;
//     top: 50%;
//     transform: translate(-50%, -50%);
//     margin: 0 10px;
// }
.ant-form input{
    width: 180px;
}
// .search div{
//    float: right;
// }
.operation{

    text-align: right;

    .ant-btn{
        margin-left: 10px;
    }
}
</style>