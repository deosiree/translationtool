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
                ref="dictRef"
                name="custom-validation"
                autocomplete='off'
                :model="dict"
                :label-col="labelCol"
            >
                <a-form-item label="辞典名称" name="name"
                    :rules="[{ required: true, message: '请输入名称!' }]"
                >
                    <a-input v-model:value="dict.name" placeholder="请输入内容"></a-input>
                </a-form-item>
            </a-form>
        </div>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import { message } from 'ant-design-vue';
import { v4 as uuidv4 } from 'uuid';
import {
    createDic
} from '@/http/api/i18Server';
export default {
    components:{
        Modal
    },
    emits:['modalClose'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:"新增辞典"
        },
        currentDict:{}
    },
    data() {
        return{
            labelCol: { style: { width: '80px' } },
            modalWidth:"400px",
            dict:{
                name:""
            }
        }
    },
    
    created() {
        
    },
    mounted () {
        // this.dict = this.currentDict
    },
    watch: {
        // currentClass(newval,oldval){
        //     this.classify = newval
        // }
    },
    methods: {
        handleClose(){
            this.$emit("modalClose",false)
        },
        handleOK(){
            this.$refs.dictRef.validate().then(() => {
                let params = {
                    dicName: this.dict.name
                }
                createDic(params).then((res) => {
                    message.success("创建成功！")
                    this.$emit("modalClose",true)
                }).catch((err) => {
                    message.error("创建失败！")
                })
            })
            
        },
        afterClose(){
            this.dict.name = ""
            this.$refs.dictRef.clearValidate()
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