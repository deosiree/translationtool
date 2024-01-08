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
import { 
    updateEntryInfo,
    updateTranslation
} from "@/http/api/entryManage";
import { message } from 'ant-design-vue';
export default {
    components:{
        Modal
    },
    emits:['editClose','editOk'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:"编辑原因"
        },
        entry:{
            type: Array,
            default: []
        }
    },
    
    data() {
        return{
            edit:{
                reason:""
            },
            editEntry:[]
        }
    },
    
    created() {
    },
    mounted () {
        this.editEntry = this.entry
        this.$nextTick(() => {
        })
    },
    watch: {
        entry(newval,oldval){
            this.editEntry = newval
        }
    },
    methods: {
       
        handleOK(){
            this.$refs.formRef.validate().then(() => {
                // console.log(this.editEntry)
                let params = {
                    notes:this.edit.reason
                }
                this.editEntry.forEach(entry => {
                    updateEntryInfo(entry,params).then((res) => {
                        
                        this.$emit("editOk",entry.id)
                    })
                    if(this.$store.state.admin){
                        let tran = []
                        let english = {
                            id: entry.englishId,
                            translate: entry.english
                        }
                        let russian = {
                            id: entry.russianId,
                            translate: entry.russian
                        }
                        let spanish = {
                            id: entry.spanishId,
                            translate: entry.spanish
                        }
                        let french = {
                            id: entry.frenchId,
                            translate: entry.french
                        }
                        tran.push(english)
                        tran.push(russian)
                        tran.push(spanish)
                        tran.push(french)
                        let data = []
                        tran.forEach(item => {
                            if(item.id != undefined && item.id != '' && item.id != null){
                                data.push(item)
                            }
                        })
                        updateTranslation(data).then((res) => {
                            
                        })
            
                    }
                })
                message.success("编辑成功！")
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