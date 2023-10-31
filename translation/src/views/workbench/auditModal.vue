<template>
    <Modal
    :visible="visible" 
    :modalTitle="modalTitle"
    @handleClose="handleClose"
    @handleOK="handleOK"
    >
        <div class="content">
            <a-form
                ref="formRef"
                name="custom-validation"
                :model="audit"
            >
                <a-form-item has-feedback label="审核状态" name="entryState">
                    <a-radio-group v-model:value="audit.entryState" :options="options" />
                </a-form-item>
                <a-form-item has-feedback label="审核意见" name="notes">
                    <a-textarea v-model:value="audit.notes" placeholder="请输入意见" allow-clear />
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
    emits:['handleClose','handleOK'],
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
            options: [
                { label: '审核通过', value: '3' },
                { label: '审核不通过', value: '0' },
            ],
            audit:{
                entryState:"3",
                notes:""
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
            this.$emit("handleOK",this.audit)
        },
        handleClose(){
            this.$emit("handleClose",'false')
        },
        reset(){
            this.audit.entryState = "3"
            this.audit.notes = ""
        }
    }
}
</script>
<style scoped>
.ant-divider{
    margin: 15px 0;
}
.content{
    width: 100%;
    height: 100%;
    padding: 10px;
    background-color: #F3F3F3;
}
</style>