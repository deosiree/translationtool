<template>
    <Modal 
    :modalWidth="modalWidth" 
    :visible="visible" 
    :modalTitle="modalTitle"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    >
        <div class="content">
            <a-form
                ref="formRef"
                name="custom-validation"
                autocomplete='off'
                :model="product"
                :label-col="labelCol"
            >
                <a-form-item label="名称" name="name"
                :rules="[{ required: true, message: '请输入产品名称!' }]"
                >
                    <a-input v-model:value="product.name" placeholder="请输入内容"></a-input>
                </a-form-item>
                <a-form-item label="所属分类" name="classify"
                :rules="[{ required: true, message: '请选择分类!' }]"
                >
                    <a-tree-select
                        v-model:value="product.classify"
                        style="width: 100%"
                        :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
                        placeholder="请选择"
                        allow-clear
                        tree-default-expand-all
                        :tree-data="treeData"
                        tree-node-filter-prop="label"
                        :fieldNames="{label:'title',value:'key'}"
                    >
                    </a-tree-select>
                </a-form-item>
            </a-form>
        </div>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import { 
    addEntryClassfy,
    getClassTree
} from "@/http/api/entryManage";
import { 
    addProduct
} from "@/http/api/product";
import { message } from 'ant-design-vue';
import { v4 as uuidv4 } from 'uuid';
export default {
    components:{
        Modal
    },
    emits:['productClose','productOk'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:'新增产品'
        },
        currentTask:{}
    },
    data() {
        return{
            labelCol: { style: { width: '74px' } },
            modalWidth:"400px",
            product:{
                name:"",
                classify: null
            },
            task:{},
            treeData:[]
        }
    },
    
    created() {
        
    },
    mounted () {
        this.task = this.currentTask
        this.getClassify()
    },
    watch: {
        currentTask(newval,oldval){
            this.task = newval
            this.getClassify()
        }
    },
    methods: {
        getClassify(){
            let params = {
                department:this.task.department
            }
            getClassTree(params).then((res) => {
                this.treeData = res.data.list
                this.handleTreeData(this.treeData)
            })
        },
        handleTreeData(data){
            data.forEach(element => {
                if(element.type === 'product' || element.type === 'common' ){
                    const index = data.indexOf(element);
                    if(index !== -1){
                        data.splice(index, 1);
                    }
                }else if(element.children && element.children.length > 0){
                    this.handleTreeData(element.children)
                }
            });
            return data
        },
        handleClose(){
            this.$emit("productClose")
        },
        handleOK(){
            this.$refs.formRef.validate().then(() => {
                let classify = {
                    key: uuidv4(),
                    title: this.product.name,
                    parentId: this.product.classify,
                    type: 'product'
                }
                addEntryClassfy(classify).then((res) => {
                })

                let data = {
                    id:classify.key,
                    name:classify.title,
                    parentId: classify.parentId
                }
                addProduct(data).then((res) => {
                    message.success('添加成功！')
                    this.$emit("productOk",this.task)
                })
            }).catch(err => {
                // console.log('error', err);
            });
        },
        afterClose(){
            this.product={
                name:"",
                classify: null
            }
            this.$refs.formRef.clearValidate()
        }
    }
}
</script>
<style scoped>
:deep(.ant-form-item-label){
    width: 85px;
}
.content{
    width: 100%;
    height: 100%;
    padding: 10px;
    background-color: #F3F3F3;
}
</style>