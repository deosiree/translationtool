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
                    <a-input v-model:value="dictTerm.entry" placeholder="请输入内容" :disabled="modalTitle.includes('编辑') ? true : false"></a-input>
                </a-form-item>
                <a-form-item label="Tag" name="Tag">
                    <a-input v-model:value="dictTerm.tag" placeholder="请输入内容" :disabled="modalTitle.includes('编辑') ? true : false"></a-input>
                </a-form-item>
                <a-form-item label="翻译语言" name="lang"
                    :rules="[{ required: true, message: '请选择!' }]"
                >
                    <a-select
                    v-model:value="dictTerm.lang"
                    placeholder="请选择"
                    :options='translateTypes'
                    :fieldNames="{label:'name',value:'code'}"
                    @change="tranlateChange"
                    >
                    </a-select>
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
    addDicTerm,
    updateDicTrans
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
            type:String
        },
        currentDict:{},
        currentData:{},
        currentIP:null
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
            translateTypes: [],
            editData:{},
        }
    },
    
    created() {
        
    },
    mounted () {
        this.dict = this.currentDict
        this.editData = this.currentData
        if(this.modalTitle.includes("编辑")){
            this.initEdit()
        }
    },
    watch: {
        currentDict(newval,oldval){
            this.dict = newval
        },
        currentData(newval,oldval){
            this.editData = newval
            if(this.modalTitle.includes("编辑")){
                this.initEdit()
            }
        }
    },
    methods: {
        
        init(){
            // 获取翻译语言
            let data = {}
            getLanguage(data).then((res) => {
                if(this.modalTitle.includes("编辑")){
                    let keys = []
                    for (var key in this.editData.translation) {
                        keys.push(key)
                    }
                    res.data.list.forEach(element => {
                        if(!keys.includes(element.code)){
                            element.disabled = true
                        }
                    });
                    this.translateTypes = res.data.list
                }else{
                    this.translateTypes = res.data.list
                }
            })
        },
        handleClose(){
            this.$emit("modalClose",false)
        },
        handleOK(){
            this.$refs.dictTermRef.validate().then(() => {
                this.dictTerm.dicName = this.dict
                this.dictTerm.i18nUrl = this.currentIP
                let params = {
                    dicName: this.dictTerm.dicName,
                    lang: this.dictTerm.lang,
                    i18nUrl: this.dictTerm.i18nUrl
                }
                let map = {}
                map[this.dictTerm.lang] = this.dictTerm.translation
                
                let data = {
                    translation: map,
                    source: this.dictTerm.entry,
                    tag : this.dictTerm.tag
                }
                if(this.modalTitle.includes("新增")){
                    addDicTerm(params,data).then((res) => {
                        message.success("新增成功！")
                        this.$emit("modalClose",true)
                    }).catch((err) => {
                        message.error("新增失败！")
                    })
                }else if(this.modalTitle.includes("编辑")){
                    updateDicTrans(params,data).then((res) => {
                        message.success("编辑成功！")
                        this.$emit("modalClose",true)
                    }).catch((err) => {
                        message.error("编辑失败！")
                    })
                }
                
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
        },
        initEdit(){
            this.dictTerm.dicName = this.dict
            this.dictTerm.entry = this.editData.source
            this.dictTerm.tag = this.editData.tag
        },
        // 翻译语言选择事件
        tranlateChange(value){
            if(this.modalTitle.includes("编辑")){
                this.dictTerm.translation = this.editData.translation[value]
            }
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