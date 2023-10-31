<template>
    <Modal 
    :modalWidth="modalWidth" 
    :visible="visible" 
    :modalTitle="modalTitle"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    >
        <div class="optBtn">
            <!-- <a-tooltip placement="top">
                <template #title>
                <span>Ctrl + up</span>
                </template>
                <a-button type="primary" size="small" @click="previous">上一个</a-button>
            </a-tooltip>
            <a-tooltip placement="top">
                <template #title>
                <span>Ctrl + down</span>
                </template>
                <a-button type="primary" size="small" @click="next">下一个</a-button>
            </a-tooltip>
            <a-tooltip placement="top">
                <template #title>
                <span>Ctrl + Shift + up</span>
                </template>
                <a-button type="primary" size="small" @click="previousUntranslated">上一个未翻译</a-button>
            </a-tooltip>
            <a-tooltip placement="top">
                <template #title>
                <span>Ctrl + Shift + down</span>
                </template>
                <a-button type="primary" size="small" @click="nextUntranslated">下一个未翻译</a-button>
            </a-tooltip> -->
            <a-button type="primary" size="small" @click="previous">上一个</a-button>
            <a-button type="primary" size="small" @click="next">下一个</a-button>
            <a-button type="primary" size="small" @click="previousUntranslated">上一个未翻译</a-button>
            <a-button type="primary" size="small" @click="nextUntranslated">下一个未翻译</a-button>
        </div>
        <div class="form">
            <a-form
            :model="record"
            layout="inline"
            autocomplete="off"
            :label-col="labelCol"
            >
                <!-- <a-form-item
                label="词条"
                name="entry"
                >
                    <a-input v-model:value="record.entry" placeholder="请输入词条" size="small"></a-input>
                </a-form-item> -->
                <a-form-item
                label="Abbr"
                name="abbr"
                style="width:97%"
                >
                    <a-input v-model:value="record.abbr" placeholder="请输入Abbr" size="small" style="width:100%"></a-input>
                </a-form-item>
                <a-form-item
                label="中文释义"
                name="chineseInterpretation"
                style="width:97%"
                >
                    <a-input v-model:value="record.chineseInterpretation" placeholder="请输入中文释义" size="small" style="width:100%"></a-input>
                </a-form-item>
                <a-form-item
                label="英文释义"
                name="englishInterpretation"
                style="width:97%"
                >
                    <a-input v-model:value="record.englishInterpretation" placeholder="请输入英文释义" size="small" style="width:100%"></a-input>
                </a-form-item>
                <a-form-item
                label="词条来源"
                name="entrySource"
                >
                    <a-input v-model:value="record.entrySource" placeholder="请输入词条来源" size="small"></a-input>
                </a-form-item>
                <a-form-item
                label="词性备注"
                name="partOfSpeech"
                >
                    <a-select
                    v-model:value="record.partOfSpeech"
                    placeholder="请选择词性备注"
                    size="small"
                    :options="partSpeechs"
                    >
                    </a-select>
                </a-form-item>
                <a-form-item
                label="所属分类"
                name="classifyId"
                >
                    <a-tree-select
                        v-model:value="record.classifyId"
                        :dropdown-style="{ maxHeight: '250px', overflow: 'auto' }"
                        placeholder="请选择分类"
                        tree-default-expand-all
                        :tree-data="classifyData"
                        tree-node-filter-prop="label"
                        :fieldNames="{children:'children', label:'title', value: 'key' }"
                        size="small"
                    >
                    </a-tree-select>
                </a-form-item>
                <a-form-item
                label="词条版本"
                name="version"
                >
                    <a-select
                    v-model:value="record.version"
                    placeholder="请选择版本"
                    size="small"
                    >
                        <template v-for="(item,index) in versions" :key="index">
                            <a-select-option :value="item.name">{{item.name}}</a-select-option>
                        </template>
                    </a-select>
                </a-form-item>
                <a-form-item
                label="词条标签"
                name="entryLabel"
                >
                    <!-- <a-select
                    v-model:value="record.entryLabel"
                    :options='entryLabels'
                    mode="SECRET_COMBOBOX_MODE_DO_NOT_USE"
                    size="small"
                    placeholder="请选择或输入"
                    >
                    </a-select> -->
                    <a-select
                        v-model:value="record.labelList"
                        mode="multiple"
                        :max-tag-count="maxTagCount"
                        placeholder="请选择标签"
                        :options='entryLabels'
                        size="small"
                        >
                        </a-select>
                </a-form-item>
                <a-form-item
                label="词条状态"
                name="entryState"
                >
                    <a-select
                    v-model:value="record.entryState"
                    placeholder="请选择状态"
                    disabled
                    size="small"
                    :options="stateOption"
                    >
                    </a-select>
                </a-form-item>
                <a-form-item
                label="创建人"
                name="creator"
                >
                    <a-input v-model:value="record.creator" disabled size="small"></a-input>
                </a-form-item>
                <a-form-item
                label="创建日期"
                name="createTime"
                >
                    <a-input v-model:value="record.createTime" disabled size="small"></a-input>
                </a-form-item>
                
            </a-form>
        </div>
        <div class="tranlate">
            <a-form
            ref="tranlateForm"
            :model="record"
            layout="inline"
            autocomplete="off"
            :label-col="labelCol"
            :rules="rules"
            >
                <a-form-item
                label="中文术语"
                name="chinese"
                style="width:97%"
                >
                    <a-input v-model:value="record.chinese" placeholder="请输入中文术语" size="small"></a-input>
                </a-form-item>
                <a-form-item
                label="英文术语"
                name="english"
                style="width:97%"
                >
                    <a-input v-model:value="record.english" placeholder="请输入英文术语" size="small"></a-input>
                </a-form-item>
                <a-form-item
                label="俄文术语"
                name="russian"
                style="width:97%"
                >
                    <a-input v-model:value="record.russian" placeholder="请输入俄文术语" size="small"></a-input>
                </a-form-item>
                <a-form-item
                label="西文术语"
                name="spanish"
                style="width:97%"
                >
                    <a-input v-model:value="record.spanish" placeholder="请输入西文术语" size="small"></a-input>
                </a-form-item>
                <a-form-item
                label="法文术语"
                name="french"
                style="width:97%"
                >
                    <a-input v-model:value="record.french" placeholder="请输入法文翻译" size="small"></a-input>
                </a-form-item>
            </a-form>
            <div class="auxiliaryText" @click="changeAuxiliary">辅助翻译</div>
            <div class="auxiliary" v-if="showAuxiliary">
                <template v-for="(item,groupIndex) in transOptions" :key="groupIndex" >
                    <div style="width:100%;">
                        <div style="width:18%;float:left;">
                            <a-checkbox
                            v-model:checked="sourceChecked[groupIndex]"
                            @change="sourceClick(groupIndex)"
                            >{{item.source}} : </a-checkbox>
                        </div>
                        <a-row style="width:82%;float:left;">
                            <a-col v-for="(child,index) in item.languageEntities" :key="index">
                                <a-checkbox 
                                v-model:checked="checked[groupIndex][child.language]"
                                @change="checkedBox(groupIndex,child.language)"
                                >{{child.label}}</a-checkbox>
                            </a-col>
                        </a-row>
                    </div>
                    <canvas id="canvas" width="100%" height="0.1" style="border: 1px solid #ccc;"></canvas>
                </template>
                <a-button type="primary" ghost size="small" @click="getChecked" style="margin-left:90%">确定</a-button>
            </div>
            <div style="width:100%;text-align:center">
                <a-spin :spinning="showLoad"/>
            </div>
        </div>
    </Modal>
</template>
<script>
import { cloneDeep } from 'lodash-es';
import Modal from '@/components/modal/index.vue';
import locale from 'ant-design-vue/es/date-picker/locale/zh_CN';
import common from "./common.js";
import { message} from 'ant-design-vue';
import {
    updateEntry,
    translate,
    getLanguage
} from "@/http/api/entry";
import key from 'keymaster'
export default {
    components:{
        Modal
    },
    emits:['detailsClose'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type: String,
            default: '词条详情'
        },
        classifyData:{
            type: Array
        },
        versions:{
            type: Array
        },
        entryLabels:{
            type: Array
        },
        dataSource:{
            type: Array
        },
        byteLimit:{
            type: Object
        },
        partSpeechs:{
            type: Array
        }
    },
    watch: {
        
    },
    data() {
        return{
            showLoad: false,
            locale: locale,
            modalWidth:"760px",
            record:{
                entryState:'',
                classifyId:'',
                entryLabel:''
            },
            labelCol: { style: { width: '70px' } },
            showAuxiliary: false,
            stateOption:[
                {value:-1,label:'已删除'},
                {value:0,label:'已废弃'},
                {value:1,label:'新建'},
                {value:2,label:'待审核'},
                {value:3,label:'已审核'},
            ],
            index:0,
            transOptions:[],
            checked:{},
            sourceChecked:{},
            maxTagCount:2,
            language:{},
            rules:{
                english:[{validator: this.checkField} ],
                russian:[{validator: this.checkField} ],
                spanish:[{validator: this.checkField} ],
                french:[{validator: this.checkField} ]
            }
        }
    },
    
    created() {
        
    },
    mounted () {
        let _this = this
        // 绑定快捷键
        key('ctrl+down',function(){ _this.ctrlDown(); return false })
        key('ctrl+up',function(){ _this.ctrlUp(); return false })
        key('ctrl+shift+down',function(){ _this.ctrlShiftDown(); return false })
        key('ctrl+shift+up',function(){ _this.ctrlShiftUp(); return false })
        key('enter',function(){ _this.enter(); return false })
    },
    unmounted(){
        // 解绑快捷键
        key.unbind('ctrl+down,ctrl+up,ctrl+shift+down,ctrl+shift+up')
        key.unbind('enter')
    },
    methods: {
        // 翻译字节数校验
        checkField(rule, value,callback){
            let byteLimit = this.byteLimit[this.record.classifyId]
            // console.log((new TextEncoder).encode(value).length)
            if(value === null || value === '' || value === undefined){
                return Promise.resolve();
            }
            let length = common.byteLength(value)
            if ( length > byteLimit) {
                return Promise.reject('最大字节数为'+byteLimit);
            } 
            return Promise.resolve();
        },
        handleClose(){
            //重置表单
            this.$refs.tranlateForm.resetFields()
            this.$emit("detailsClose")
        },
        handleOK(){
            this.$refs.tranlateForm.validate().then(() => {
                // 翻译校验通过
                this.update()
            }).catch(err => {
                console.log('error', err);
            });
        },
        // 获取语言列表
        getLanguage(){
            getLanguage().then((res) => {
                let language = {}
                res.data.list.forEach(item => {
                    let lan = {
                        language: item.name,
                        code: item.bdCode,
                        english: item.code
                    }
                    language[item.code] = lan
                })
                console.log(language)
                this.language = language
            })
        },
        afterClose(){
            // console.log('afterClose')
        },
        // 修改词条
        update(){
            if(this.record.labelList != undefined){
                this.record.entryLabel = ""
                this.record.labelList.forEach(item => {
                    this.record.entryLabel += item +','
                })
                this.record.entryLabel = this.record.entryLabel.substring(0, this.record.entryLabel.lastIndexOf(','))
            }
            updateEntry(this.record).then((res) => {
                message.success("编辑成功！")
            })
        },
        // 辅助翻译点击事件
        changeAuxiliary(){
            if(this.showAuxiliary){
                this.showAuxiliary = false
                return
            }
            this.showLoad = true
            this.transOptions = []
            // 封装请求参数
            let params = {
                name: '',
                type: ''
            }
            for(var key in this.language){
                if(this.record[key] != null && this.record[key] != ''){
                    params.name = this.record[key]
                    params.type = this.language[key].english
                    break
                }
            }
            if(params.name === '' || params.type === ''){
                this.showLoad = false
                return
            }
            // 获取翻译结果
            translate(params).then((res) => {
                this.transOptions = res.data.translateEntities
                this.showAuxiliary = true
                this.handleTranData()
            }).catch(error =>{
                this.showLoad = false
            });
        },
        handleTranData(){

            this.transOptions.forEach(item => {
                item.languageEntities.forEach( tran => {
                    tran.label = this.language[tran.language].language +" / "+ tran.value
                })
            })

            // 处理数据
            this.sourceChecked = {}
            this.checked = {}
            this.transOptions.forEach((item,index) => {
                this.sourceChecked[index] = false
                let childflag = {}
                item.languageEntities.forEach(child => {
                    childflag[child.language] = false
                })
                this.checked[index] = childflag
            })

            this.showLoad = false
        },
        // 翻译复选框点击事件
        checkedBox(index,language){
            if(!this.checked[index][language]){
                // 取消选中
                this.checked[index][language] = false
            }else{
                // 选中时 将其他已选中的该语言翻译变为未选中  然后选中点击的翻译
                for (let i in this.checked) {
                    this.checked[i][language] = false
                }
                this.checked[index][language] = true
            }

            // 单独点击翻译是  将翻译来源设为未选中
            for (let i in this.sourceChecked){
                this.sourceChecked[i] = false
            }
            
        },
        // 翻译来源点击事件
        sourceClick(index){
            if(this.sourceChecked[index]){
                // 选中   先将已选中的翻译全部清空
                for (let i in this.checked){
                    for(let j in this.checked[i]){
                        this.checked[i][j] = false
                    }
                }
                // 将其他翻译来源设为未选中
                for (let i in this.sourceChecked){
                    if(i !== index.toString()){
                        this.sourceChecked[i] = false
                    }
                }
            }else{
                // 取消选中
            }
            // 修改翻译选中状态与翻译来源状态一致
            for (let i in this.checked[index]) {
                this.checked[index][i] = this.sourceChecked[index]
            }

        },
        // 获取用户辅助翻译选择结果 并填入词条对应翻译中
        getChecked(){
            // 获取选中的翻译
            let selectTran = []
            for (let i in this.checked) {
                let item = this.checked[i]
                for (let j in item) {
                    if(item[j]){
                        this.transOptions[i].languageEntities.forEach(tran => {
                            if(tran.language === j){
                                selectTran.push(tran)
                            }
                        })
                    }
                }
            }
            // 赋值
            selectTran.forEach(item => {
                this.record[item.language] = item.value
            })
        },
        init(record){
            this.record = record
            this.index = this.dataSource.findIndex(item => item.id == record.id)
            this.showAuxiliary = false

            this.getLanguage()
        },
        // 下一个
        next(){
            this.index ++
            if(this.index < this.dataSource.length){
                //重置表单
                this.$refs.tranlateForm.resetFields()
                this.record = cloneDeep(this.dataSource[this.index])
                // console.log(this.dataSource)
            }else{
                message.warn("当前已是最后一个！")
                this.index --
            }
        },
        // 上一个
        previous(){
            this.index --
            if(this.index >= 0 && this.index < this.dataSource.length){
                //重置表单
                this.$refs.tranlateForm.resetFields()
                this.record = cloneDeep(this.dataSource[this.index])
            }else{
                message.warn("当前已是第一个！")
                this.index ++
            }
        },
        // 下一个未翻译
        nextUntranslated(){
            this.index ++
            if(this.index < this.dataSource.length){
                let mark = false
                for(this.index;this.index < this.dataSource.length;this.index++){
                    let data = this.dataSource[this.index]
                    let flag = this.isUntranslated(data)
                    if(flag){
                        //重置表单
                        this.$refs.tranlateForm.resetFields()
                        this.record = cloneDeep(this.dataSource[this.index])
                        mark = true
                        break
                    }
                }
                if(!mark){
                    message.warn("当前已是最后一个未翻译！")
                    this.index --
                }
            }else{
                message.warn("当前已是最后一个未翻译！")
                this.index --
            }
            
        },
        isUntranslated(data){
            let flag = (data.english === null ||  data.english === '') ||
                        (data.russian === null ||  data.russian === '') ||
                        (data.spanish === null ||  data.spanish === '') ||
                        (data.french === null ||  data.french === '')
            return flag
        },
        // 上一个未翻译
        previousUntranslated(){
            this.index --
            if(this.index >= 0 && this.index < this.dataSource.length){
                let mark = false
                for(this.index;this.index >= 0;this.index--){
                    let data = this.dataSource[this.index]
                    let flag = this.isUntranslated(data)
                    if(flag){
                        //重置表单
                        this.$refs.tranlateForm.resetFields()
                        this.record = cloneDeep(this.dataSource[this.index])
                        mark = true
                        break
                    }
                }
                if(!mark){
                    message.warn("当前已是第一个未翻译！")
                    this.index ++
                }
            }else{
                message.warn("当前已是第一个未翻译！")
                this.index ++
            }
        },
        ctrlDown(){
            if(this.visible){
                this.next()
            }
        },
        ctrlUp(){
            if(this.visible){
                this.previous()
            }
        },
        ctrlShiftDown(){
            if(this.visible){
                this.nextUntranslated()
            }
        },
        ctrlShiftUp(){
            if(this.visible){
                this.previousUntranslated()
            }
        },
        enter(){
            if(this.visible){
                this.handleOK()
            }
        }
    }
}
</script>
<style lang="less" scoped>
.ant-form-item-has-error{
    margin-bottom: 0px;
}
.optBtn{
    width: 100%;
    text-align: right;
    margin-bottom: 10px;
    .ant-btn{
        margin-left: 10px;
    }
}
.form{
    background-color: #F3F3F3;
    padding: 10px;

    :deep(.ant-input),:deep(.ant-select-selector),.ant-picker-range{
        width: 265px;
    }
}
.tranlate{
    width: 100%;
    padding: 10px;
    margin-top: 10px;
    border: 1px solid #DCDCDC;
    background: #F9F9F9;

    .auxiliaryText{
        margin-left: 70px;
        font-size: 12px;
        color: #369FFF;
    }
    .auxiliary{
        width: 100%;
        // border: 1px solid #DCDCDC;
        padding: 5px 5px 5px 10px;
        position: relative;

        :deep(.ant-checkbox-wrapper){
            margin-left: 0px;
            margin-right: 8px;
        }
    }
}
</style>