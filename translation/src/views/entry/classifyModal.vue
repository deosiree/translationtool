<template>
    <Modal 
    :modalWidth="modalWidth" 
    :visible="visible" 
    :modalTitle="modalTitle"
    @handleClose="handleClose"
    @handleOK="handleOK"
    >
        <div class="content">
            <a-form
                ref="formRef"
                name="custom-validation"
                :model="classify"
            >
                <a-form-item label="分类名称" name="name">
                    <a-input v-model:value="classify.name" placeholder="请输入分类名称"></a-input>
                </a-form-item>
                <a-form-item label="最大字节数" name="maxLength">
                    <a-input-number v-model:value="classify.maxByte" placeholder="请输入最大字节数" style="width:100%"></a-input-number>
                </a-form-item>
            </a-form>
        </div>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
export default {
    components:{
        Modal
    },
    emits:['classifyHandleClose','classifyHandleOK'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String
        }
    },
    watch: {
        
    },
    data() {
        return{
            modalWidth:"400px",
            classify:{
                name:"",
                maxByte:""
            }
        }
    },
    
    created() {
        
    },
    mounted () {
        
    },
    methods: {
        handleClose(){
            this.$emit("classifyHandleClose")
        },
        handleOK(){
            this.$emit("classifyHandleOK",this.classify)
        },
        init(classify){
            // console.log(classify)
            this.classify = classify
            // console.log(this.classify)
        },
        reset(){
            this.classify.name = ""
            this.classify.maxByte = ""
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