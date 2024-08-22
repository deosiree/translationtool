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
    createVersion
} from "@/http/api/productVersion";
export default {
    components:{
        Modal
    },
    emits:['versionClose','versionOk'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:"添加版本"
        },
        currentVersion:{}
    },
    data() {
        return{
            labelCol: { style: { width: '70px' } },
            modalWidth:"400px",
            version:{
                name:"",
                details:"",
                productId:""
            },
            record:{}
        }
    },
    
    created() {
        
    },
    mounted () {
        this.record = this.currentVersion
    },
    watch: {
        currentVersion(newval,oldval){
            this.record = newval
            console.log(newval)
        }
    },
    methods: {
        handleClose(){
            this.$emit("versionClose")
        },
        handleOK(){
            this.$refs.formRef.validate().then(() => {
                // 判断版本名称是否已存在
                if(this.record.allVersions){
                    if(this.record.allVersions.find(item=>item.label===this.version.name)){
                        message.info("版本名称已存在，请重新输入！")
                        return
                    }
                }
                let data = {
                    name:this.version.name,
                    details:this.version.details,
                    productId:this.record.productId
                }
                createVersion(data).then((res) => {
                    message.success('添加成功！')
                    this.$emit("versionOk",this.record)
                })
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
.content{
    width: 100%;
    height: 100%;
    padding: 10px;
    background-color: #F3F3F3;
}
</style>