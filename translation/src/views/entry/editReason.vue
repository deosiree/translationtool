<template>
    <Modal
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
                :model="edit"
            >
                <a-form-item has-feedback label="编辑原因" name="reason"
                    :rules="[{ required: true, message: '请输入编辑原因!' }]"
                >
                    <a-textarea v-model:value="edit.reason" placeholder="请输入编辑原因" />
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
    emits:['editClose','editOK'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:"编辑原因"
        }
    },
    watch: {
        
    },
    data() {
        return{
            edit:{
                reason:""
            }
        }
    },
    
    created() {
    },
    mounted () {
        this.$nextTick(() => {
        })
    },
    methods: {
       
        handleOK(){
            this.$refs.formRef.validate().then(() => {
                this.$emit("editOK",this.edit.reason)
            }).catch(err => {
                // console.log('error', err);
            });
            
        },
        handleClose(){
            this.$emit("editClose")
        },
        afterClose(){
            this.edit.reason = ""
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