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
                :model="classify"
                :label-col="labelCol"
            >
                <a-form-item label="名称" name="title"
                    :rules="[{ required: true, message: '请输入名称!' }]"
                >
                    <a-input v-model:value="classify.title" placeholder="请输入内容"></a-input>
                </a-form-item>
                <!-- <a-form-item label="最大字节数" name="maxLength">
                    <a-input-number v-model:value="classify.maxByte" placeholder="请输入最大字节数" style="width:100%"></a-input-number>
                </a-form-item> -->
            </a-form>
        </div>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import { 
    addEntryClassfy,
    updateEntryClassfy
} from "@/http/api/entryManage";
import { 
    addProduct,
    updateProduct
} from "@/http/api/product";
import { message } from 'ant-design-vue';
import { v4 as uuidv4 } from 'uuid';
export default {
    components:{
        Modal
    },
    emits:['classifyClose'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String
        },
        currentClass:{}
    },
    data() {
        return{
            labelCol: { style: { width: '50px' } },
            modalWidth:"400px",
            classify:{
                title:""
            }
        }
    },
    
    created() {
        
    },
    mounted () {
        this.classify = this.currentClass
    },
    watch: {
        currentClass(newval,oldval){
            this.classify = newval
        }
    },
    methods: {
        handleClose(){
            this.$emit("classifyClose")
        },
        handleOK(){
            this.$refs.formRef.validate().then(() => {
                if(this.modalTitle === '添加分类'){
                    addEntryClassfy(this.classify).then((res) => {
                        message.success('新增成功！')
                        this.$emit("classifyClose")
                    })
                }else if(this.modalTitle === '编辑分类'){
                    updateEntryClassfy(this.classify).then((res) => {
                        message.success("编辑成功！")
                        this.$emit("classifyClose")
                    })
                }else if(this.modalTitle === '添加产品'){
                    this.classify.key = uuidv4()
                    addEntryClassfy(this.classify).then((res) => {
                    })

                    let data = {
                        id:this.classify.key,
                        name:this.classify.title,
                        parentId: this.classify.parentId
                    }
                    addProduct(data).then((res) => {
                        message.success('添加成功！')
                        this.$emit("classifyClose")
                    })

                }else if(this.modalTitle === '编辑产品'){
                    updateEntryClassfy(this.classify).then((res) => {
                        
                    })
                    let data = {
                        id:this.classify.key,
                        name:this.classify.title,
                    }
                    updateProduct(data).then((res) => {
                        message.success('编辑成功！')
                        this.$emit("classifyClose")
                    })

                }
            }).catch(err => {
                // console.log('error', err);
            });
            
        },
        afterClose(){
            this.classify.title = ""
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