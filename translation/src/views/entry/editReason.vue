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
import {
    addTranslate
} from "@/http/api/translate"
import { message } from 'ant-design-vue';
import { v4 as uuidv4 } from 'uuid';
import { cloneDeep } from 'lodash-es';
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
            editEntry:[],
            languageMap:{
                english: {idName:'enTransId',chinese:'英文'},
                french: {idName:'fraTransId',chinese:'法文'},
                spanish: {idName:'spaTransId',chinese:'西文'},
                russian: {idName:'ruTransId',chinese:'俄文'},
            }
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
                        message.success('已保存！')
                    })
                })
                
                
                // this.editEntry.forEach(entry => {
                //     // if(this.$store.state.admin){
                //         let tran = []
                //         let english = {
                //             id: entry.enTransId,
                //             translate: entry.english,
                //             language: 'english'
                //         }
                //         let russian = {
                //             id: entry.ruTransId,
                //             translate: entry.russian,
                //             language: 'russian'
                //         }
                //         let spanish = {
                //             id: entry.spaTransId,
                //             translate: entry.spanish,
                //             language: 'spanish'
                //         }
                //         let french = {
                //             id: entry.fraTransId,
                //             translate: entry.french,
                //             language: 'french'
                //         }
                //         tran.push(english)
                //         tran.push(russian)
                //         tran.push(spanish)
                //         tran.push(french)
                //         let updateData = []
                //         let addData = []
                //         let tempEntry = cloneDeep(entry)
                //         tran.forEach(item => {
                //             if(item.translate === '' || item.translate === null){
                //                 tempEntry[this.languageMap[item.language].idName] = ""
                //             }
                //             if(item.id != undefined && item.id != '' && item.id != null 
                //             && item.translate != null && item.translate != '' ){
                //                 updateData.push(item)
                //             }else{
                //                 if(item.translate != '' && item.translate != null){
                //                     addData.push(item)
                //                 }
                //             }
                //         })
                //         // 修改翻译
                //         if(updateData.length > 0){
                //             updateTranslation(updateData).then((res) => {
                            
                //             })
                //         }
                        

                //         addData.forEach(item => {
                //             let id = uuidv4()
                //             item.id = id
                //             tempEntry[this.languageMap[item.language].idName] = id
                //             // 新增翻译
                //             item.type = this.languageMap[item.language].chinese
                //             item.entry = tempEntry.entry
                //             item.versionID = tempEntry.versionID
                //             item.translateState = '1'
                //             addTranslate(item).then((res) => {

                //             })
                //         })
                //         entry = tempEntry
                //     // }
                //     updateEntryInfo(entry,params).then((res) => {
                        
                //         this.$emit("editOk",entry.id)
                //     })
                // })
                // message.success("编辑成功！")
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