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
                ref="dictTermRef"
                name="custom-validation"
                autocomplete='off'
                :model="dictTerm"
                :label-col="labelCol"
            >
                <a-form-item label="词条" name="entry"
                    :rules="[{ required: true, message: '请输入词条!' }]"
                >
                    <a-input v-model:value="dictTerm.entry" placeholder="请输入内容"></a-input>
                </a-form-item>
                <a-form-item label="翻译语言" name="lang"
                    :rules="[{ required: true, message: '请选择!' }]"
                >
                    <a-select
                    v-model:value="dictTerm.lang"
                    placeholder="请选择"
                    :options='translateTypes'
                    :fieldNames="{label:'name',value:'name'}"
                    >
                    </a-select>
                </a-form-item>
                <a-form-item label="Tag" name="Tag">
                    <a-input v-model:value="dictTerm.tag" placeholder="请输入内容"></a-input>
                </a-form-item>
                <a-form-item label="翻译" name="translation"
                    :rules="[{ required: true, message: '请输入内容!' }]"
                >
                    <a-textarea v-model:value="dictTerm.translation" placeholder="请输入内容"></a-textarea>
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
    addDicTerm
} from '@/http/api/i18Server';
import {
    getLanguage
} from "@/http/api/entry";
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
            modalWidth:"500px",
            dict:"",
            dictTerm:{
                dicName:"",
                entry:"",
                translation:"",
                lang:null,
                tag:""
            },
            translateTypes: []
        }
    },
    
    created() {
        
    },
    mounted () {
        this.dict = this.currentDict
        this.getLanguage()
    },
    watch: {
        currentDict(newval,oldval){
            this.dict = newval
        }
    },
    methods: {
        // 获取翻译语言
        getLanguage(){
            let data = {}
            getLanguage(data).then((res) => {
                this.translateTypes = res.data.list
            })
        },
        handleClose(){
            this.$emit("modalClose",false)
        },
        handleOK(){
            this.$refs.dictTermRef.validate().then(() => {
                this.dictTerm.dicName = this.dict
                addDicTerm(this.dictTerm).then((res) => {
                    message.success("新增成功！")
                    this.$emit("modalClose",true)
                }).catch((err) => {
                    message.error("新增失败！")
                })
                
            })
        },
        afterClose(){
            this.dictTerm = {
                dicName: "",
                entry:"",
                translation:"",
                lang:null,
                tag:""
            }
            this.$refs.dictTermRef.clearValidate()
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