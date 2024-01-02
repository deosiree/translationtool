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
                :model="version"
                :label-col="labelCol"
            >
                <a-form-item label="版本名称" name="name"
                    :rules="[{ required: true, message: '请输入版本名称!' }]"
                >
                    <a-input v-model:value="version.name" placeholder="请输入内容"></a-input>
                </a-form-item>
                <a-form-item label="备注" name="details">
                    <a-textarea v-model:value="version.details" placeholder="请输入内容" />
                </a-form-item>
            </a-form>
        </div>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import { message } from 'ant-design-vue';
import { 
    createVersion,
    updateVersion
} from "@/http/api/productVersion";
export default {
    components:{
        Modal
    },
    emits:['versionClose'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String
        },
        currentVersion:{}
    },
    data() {
        return{
            labelCol: { style: { width: '70px' } },
            modalWidth:"400px",
            version:{
                id:"",
                name:"",
                details:"",
                productId:""
            }
        }
    },
    
    created() {
        
    },
    mounted () {
        this.version = this.currentVersion
    },
    watch: {
        currentVersion(newval,oldval){
            this.version = newval
        }
    },
    methods: {
        handleClose(){
            this.$emit("versionClose")
        },
        handleOK(){
            this.$refs.formRef.validate().then(() => {
                if(this.modalTitle === '添加版本'){
                    createVersion(this.version).then((res) => {
                        message.success('添加成功！')
                        this.$emit("versionClose")
                    })
                }else if(this.modalTitle === '修改详情'){
                    updateVersion(this.version).then((res) => {
                        message.success('修改成功！')
                        this.$emit("versionClose")
                    })
                }
            }).catch(err => {
                // console.log('error', err);
            });
        },
        afterClose(){
            this.version = {
                name:"",
                details:"",
                productId:""
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