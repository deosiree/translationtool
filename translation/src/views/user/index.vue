<template>
<div class="box" ref="box">
    <div class="left">
        <div class="title"><span>用户权限查询：</span></div>
        <div class="content" ref="content">
            <div class="searchBox" ref="search">
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
                        <a-input v-model:value="search.userName" allowClear placeholder="请输入用户名" style="width:200px"></a-input>
                    </a-form-item>
                    <a-form-item
                    label="角色"
                    name="roleName"
                    >
                        <a-input v-model:value="search.roleName" allowClear placeholder="请输入角色" style="width:200px"></a-input>
                    </a-form-item>
                    <a-form-item
                    label="部门"
                    name="department"
                    >
                        <a-input v-model:value="search.department" allowClear placeholder="请输入部门" style="width:200px"></a-input>
                    </a-form-item>
                    <a-form-item style="margin-right:0px">
                        <a-button type="primary" size="middle" @click="queryUser">查询</a-button>
                    </a-form-item>
                </a-form>
            </div>
            <span class="userList">用户列表</span>
            <a-table 
            class="ant-table-striped"
            :columns="userColumns" 
            :data-source="userDataSource" 
            :scroll="tableHeight"
            :pagination='false'
            :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
            ref="userTable"
            bordered>
            </a-table>
        </div>
    </div>
    <div class="right">
        <div class="title"><span>用户权限配置：</span></div>
        <div class="content">
            <div class="searchBox" ref="search">
                <a-form
                    :model="search"
                    name="horizontal_login"
                    layout="inline"
                    autocomplete="off"
                >
                    <a-form-item>
                        <a-input-search
                        v-model:value="keyWords"
                        placeholder="关键字搜索"
                        style="width: 300px"
                        allowClear
                        @search="onSearch"
                        @pressEnter="onSearch"
                        />
                    </a-form-item>
                </a-form>
            </div>
            <a-table 
            class="ant-table-striped"
            :columns="authorColumns" 
            :data-source="authorDataSource" 
            :scroll="tableHeight"
            :pagination='false'
            :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
            :row-key="record => record.id"
            :defaultExpandAllRows="true"
            ref="authorTable"
            :loading="loading"
            bordered>
                <template #bodyCell="{ column, record }">
                    <template v-if="['admin', 'developer', 'entryReviewer', 'translator', 'translateReviewer'].includes(column.dataIndex)">
                        <a-checkbox 
                        v-model:checked="record[column.dataIndex]"
                        @change="clickCheckBox(record,column)"
                        ></a-checkbox>
                    </template>
                    <!-- <template v-if="column.dataIndex === 'userName'">
                        <a-checkbox 
                        >{{text}}</a-checkbox>
                    </template> -->
                </template>
                <template #expandIcon="props">
                    <span v-if="props.record.children != null">
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
            </a-table>
            <div class="authorBtn">
                <!-- <a-button type="primary" size="small" class="resetBtn">更新用户列表</a-button> -->
                <a-button type="primary"  @click="save">保存</a-button>
            </div>
        </div>
    </div>
</div>
</template>
<script>
import {
  CaretDownOutlined,
  CaretRightOutlined
} from '@ant-design/icons-vue';
import { 
    getUserPermission,
    getUserInfo,
    addUserPermission
} from "@/http/api/user";
import { v4 as uuidv4 } from 'uuid';
import { cloneDeep } from 'lodash-es';
import { message } from 'ant-design-vue';
export default {
    components:{
        CaretDownOutlined,
        CaretRightOutlined
    },
    data() {
        return{
            name:"user",
            search:{
                userName:"",
                roleName:"",
                department:"",
            },
            tableHeight: { x:'100%',y: 0 },
            userColumns:[
                {title: "序号",dataIndex: 'index',align:'center',width:70,customRender: (text, record, index, column) => {
                    return text.index + 1
                }},
                {title: '用户名',dataIndex: 'userName',align:'center',width: '20%'},
                {title: '部门', dataIndex: 'department',align:'center',width: '20%'},
                {title: '角色',dataIndex: 'roleNames',align:'center'},
            ],
            userDataSource: [],
            keyWords:"",
            authorColumns:[
                {title: '用户名',dataIndex: 'name',width: '20%'},
                {title: '管理员', dataIndex: 'admin',align:'center',width: '20%'},
                {title: '开发员',dataIndex: 'developer',align:'center'},
                {title: '词条审核员',dataIndex: 'entryReviewer',align:'center'},
                {title: '翻译员',dataIndex: 'translator',align:'center'},
                {title: '翻译审核员',dataIndex: 'translateReviewer',align:'center'},
            ],
            authorDataSource:[],
            changeAuthor:{},
            loading:false
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
        init(){
            this.setTableHeight()
            this.queryUser()
            this.getUserPermission()
        },

        setTableHeight(){
            this.$nextTick(() => {
                let box = this.$refs.box.offsetHeight
                let search = this.$refs.search.offsetHeight
                this.tableHeight.y = box - search - 170
            })
        },
        // 查询用户
        queryUser(){
            let params = {
                pageIndex: -1,
                pageSize: -1
            }
            let data = {
                userName:this.search.userName,
                department: this.search.department
            }
            if(this.search.roleName !=""){
                data.roleName = [this.search.roleName]
            }
            getUserInfo(data,params).then((res) => {
                this.userDataSource = res.data.list
                this.userDataSource.forEach(item => {
                    let roleNames = ""
                    if(item.roleName && item.roleName.length > 0){
                        roleNames = item.roleName.join(", ")
                    }
                    item.roleNames = roleNames
                })
            })
        },
        // 查询用户权限
        getUserPermission(){
            this.loading = true
            let params = {
                name:this.keyWords
            }
            this.changeAuthor = {}
            getUserPermission(params).then((res) => {
                this.loading = false
                this.authorDataSource = res.data.list
                this.ergodicTree(this.authorDataSource)
            })
        },
        // 遍历树结构生成id
        ergodicTree(data){
            data.forEach(item => {
                item.id = uuidv4()
                if(item.children != null){
                    this.ergodicTree(item.children)
                }
            })
        },
        // 用户权限搜索框点击事件
        onSearch(){
            this.getUserPermission()
        },
        // 权限列表复选框点击事件
        clickCheckBox(record,column){
            this.changeAuthor[record.id] = record
            // 设置子节点选中状态
            this.setChildren(record,column.dataIndex)
        },
        // 设置子的选中状态
        setChildren(record,field){
            if(record.children != null ){
                record.children.forEach(item => {
                    item[field] = record[field]
                    this.setChildren(item,field)
                })
            }else{
                return
            }
        },
        save(){
            let data = []
            for(let key in this.changeAuthor){
                if(this.changeAuthor[key].type === "department"){
                    let arr = this.getChangeUser(this.changeAuthor[key])
                    data = data.concat(arr)
                }else{
                    data.push(this.changeAuthor[key])
                }
            }
            if(data.length > 0){
                // 保存
                addUserPermission(data).then((res) => {
                    message.success("保存成功！")
                    this.getUserPermission()
                    this.queryUser()
                })
            }
        },
        getChangeUser(data,arr=[]){
            if(data.type === "department"){
                if(data.children != null){
                    data.children.forEach(item => {
                        this.getChangeUser(item,arr)
                    })
                }
            }else{
                arr.push(data)
            }
            return arr
        }
    },
}
</script>
<style lang="less">
@import url("@/assets/style/common.less");
</style>
<style scoped lang="less">
.box{
    width:100%;
    height:100%;
    // border:1px solid #DCDCDC;
    display: flex;
    flex-direction: row;

    .left{
        width:50%;
        height:100%;
        border:1px solid #DCDCDC;
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        align-self: stretch;

        .userList{
            color: #000;

            /* 四级文字/常规 */
            font-family: Microsoft YaHei;
            font-size: 14px;
            font-style: normal;
            font-weight: 400;
            line-height: 22px;
        }
    }
    .right{
        width:50%;
        height:100%;
        border:1px solid #DCDCDC;
        display: flex;
        padding-bottom: 0px;
        flex-direction: column;
        align-items: flex-start;
        align-self: stretch;
    }
    .title{
        display: flex;
        height: 32px;
        padding: 8px 16px;
        align-items: center;
        gap: 8px;
        align-self: stretch;
        border: 0px solid var(--grey-grey-07, #DCDCDC);
        background: var(--grey-grey-02, #F3F3F3);

        span{
            color: var(--text-icon-font-gy-190-primary, rgba(0, 0, 0, 0.90));
            /* 四级文字/加粗 */
            font-family: Microsoft YaHei;
            font-size: 14px;
            font-style: normal;
            font-weight: 700;
            line-height: 22px;
        }
    }
    .content{
        display: flex;
        padding: 16px;
        flex-direction: column;
        align-items: flex-start;
        gap: 16px;
        flex: 1 0 0;
        align-self: stretch;
        // border: 1px solid red;
        width: 100%;
        height: 100%;

        .searchBox{
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            gap: 16px;
            align-self: stretch;
            // border: 1px solid red;
        }
    }
}
.authorBtn{
    display: flex;
    justify-content: flex-end;
    align-items: flex-start;
    gap: 16px;
    align-self: stretch;
    // border: 1px solid red;
}

</style>