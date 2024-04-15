<template>
    <CustomModal 
    :modalWidth="modalWidth" 
    :visible="visible" 
    :modalTitle="modalTitle"
    :showOk="false"
    cancelText="关闭"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    >
        <div style="width:100%;height:350px">
            <a-form
            :model="search"
            layout="inline"
            autocomplete="off"
            >
                <a-form-item
                label="一级分类"
                name="operator"
                >
                    <a-select
                    v-model:value="search.classify1"
                    style="width: 200px"
                    placeholder="请选择"
                    :fieldNames="{label:'title',value:'key'}"
                    :options='classfy1Option'
                    @select="classify1Select"
                    >
                    </a-select>
                </a-form-item>
                <a-form-item>
                    <a-button type="primary" @click="add">新增</a-button>
                    <a-button type="primary" danger @click="deleteClassify2" style="margin-left:8px">删除</a-button>
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
                :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
                :row-key="record => record.id"
                ref="secondClassifyTable"
                bordered>
                    <template #bodyCell="{ column, record, text }">
                        <template v-if="column.dataIndex === 'name'">
                            <div class="editable-cell">
                                <template v-if="editableData[record.id]">
                                    <a-input
                                        v-model:value="editableData[record.id][column.dataIndex]"
                                        style="margin: -5px 0;width:80%"
                                    />
                                    <CheckOutlined style="color:#369FFF;margin-left:8px" @click="save(record.id)" />
                                    <CloseOutlined style="color:red;margin-left:8px" @click="cancel(record.id)"/>
                                </template>
                                <template v-else>
                                    {{ text }}
                                    <EditOutlined class="editable-cell-icon" @click="edit(record.id)" />
                                </template>
                            </div>
                        </template>
                    </template>
                </a-table>
            </div>
        </div>
    </CustomModal>
</template>
<script>
import CustomModal from '@/components/modal/index.vue';
import locale from 'ant-design-vue/es/date-picker/locale/zh_CN';
import { CheckOutlined, EditOutlined,CloseOutlined,ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { message,Modal } from 'ant-design-vue';
import { defineComponent, ref, createVNode } from 'vue';
import { cloneDeep, iteratee } from 'lodash-es';
import {
    getClassfy
} from '@/http/api/entryManage'
import {
    getSecondClassify,
    addSecondClassify,
    updateSecondClassify,
    deleteSecondClassify
} from '@/http/api/secondClassify'
export default {
    components:{
        CustomModal,
        CheckOutlined,
        EditOutlined,
        CloseOutlined,
        ExclamationCircleOutlined
    },
    emits:['secondClassifyClose'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:"二级分类设置"
        },
        currentProduct:{
            type: Object
        }
    },
    
    data() {
        return{
            locale: locale,
            modalWidth:"60%",
            dataSource:[],
            columns: [
                {title: "序号",dataIndex: 'index',align:'center',width:60,customRender: (text, record, index, column) => {
                    return text.index + 1
                }},
                {title: '二级分类名称',dataIndex: 'name',align:'center',width:200,},
                {title: '一级分类名称',dataIndex: 'parentName',align:'center',width:200,},
                {title: '创建人',dataIndex: 'creator',align:'center',width:150,},
                {title: '创建时间',dataIndex: 'createTime',align:'center',width:200}
            ],
            search:{
                classify1:null
            },
            classfy1Option:[],
            product:{},
            editableData:{},
            selectedRowKeys:[]
        }
    },
    
    created() {
        
    },
    mounted () {
        this.product = this.currentProduct
    },
    watch: {
        currentProduct(newval,oldval){
            this.product = newval
        }
    },
    methods: {
        handleClose(){
            this.$emit("secondClassifyClose")
        },
        handleOK(){
             this.$emit("secondClassifyClose")
        },
        // 初始化数据
        init(){
            // console.log(this.product)
            if(this.product.type === 'module'){
                this.search.classify1 = this.product.key
                // 获取二级分类
                this.getSecondClassify()
            }else{
                this.search.classify1 = null
            }
            // 获取一级分类
            let params = {
                parentId:this.product.type === 'module' ? this.product.parentId : this.product.key,
                type:'module'
            }
            getClassfy(params).then((res) => {
                this.classfy1Option = res.data.list
            })

        },
        // 获取二级分类
        getSecondClassify(){
            let data = {
                parentId: this.search.classify1
            }
            getSecondClassify(data).then((res) => {
                this.dataSource = res.data.list
            })
        },
        classify1Select(value){
            // console.log(value)
            this.getSecondClassify()
        },
        add(){
            if(this.search.classify1 === null || this.search.classify1 === ""){
                message.warn("请选择一级分类！")
                return
            }
            let newData = {
                id:"new_"+ this.dataSource.length + 1,
                creator:this.$store.state.user.userName,
                parentId: this.search.classify1,
                parentName: this.classfy1Option.find(item => item.key === this.search.classify1).title,
                createTime:new Date().toLocaleString().replaceAll('/','-')
            }
            this.dataSource.push(newData)
            this.editableData[newData.id] = newData
            // 滚动到最底部
            this.$nextTick(()=>{
                let container = this.$refs.secondClassifyTable.$el.querySelector('.ant-table-body')
                container.scrollTop = container.scrollHeight
            })
        },
        edit(id){
            this.editableData[id] = cloneDeep(this.dataSource.find(item => item.id === id))
        },
        save(id){
            if(id.startsWith('new')){
                // 新增
                addSecondClassify(this.editableData[id]).then((res) => {
                    message.success("新增成功！")
                    this.getSecondClassify()
                    delete this.editableData[id]
                })
            }else{
                // 编辑
                updateSecondClassify(this.editableData[id]).then((res) => {
                    message.success("编辑成功！")
                    this.getSecondClassify()
                    delete this.editableData[id]
                })
            }
        },
        cancel(id){
            delete this.editableData[id]
            if(id.startsWith('new')){
                this.dataSource.some((item,i) => {
                    if(item.id === id){
                        this.dataSource.splice(i,1)
                        return true
                    }
                })
            }
        },
        // 删除二级分类
        deleteClassify2(){
            if(this.selectedRowKeys.length === 0){
                return
            }
            Modal.confirm({
                title: '是否确定删除?',
                icon: createVNode(ExclamationCircleOutlined),
                content: "",
                okText: '是',
                cancelText: '否',
                style:"top:30%",
                onOk: () => {
                    deleteSecondClassify(this.selectedRowKeys).then((res) => {
                        message.success("删除成功！")
                        this.getSecondClassify()
                    })
                }
            });
        },
        onSelectChange(selectedRowKeys){
            this.selectedRowKeys = selectedRowKeys
        },
        afterClose(){
            this.dataSource = []
            this.classfy1Option = []
            this.search.classify1 = null
            this.selectedRowKeys = []
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
</style>