<template>
    <a-modal
    :visible="modalVisible"
    :title='null'
    :closable='false'
    :bodyStyle="{padding: '0px',height: '70%'}"
    :width="modalWidth"
    :footer="!footer ? null : undefined"
    :afterClose="afterClose"
    :maskClosable='false'
    ok-text="确定"
    cancel-text="取消"
    @ok="ok"
    @cancel="cancel"
    >
        <div class="modalHeader">
            <div class="title">
                <img src="../../assets/icon/modal.png" style="width:18px;height:18px"/>
                <span>{{modalTitle}}</span>
            </div>
            <img src="../../assets/icon/closeModel.png" class='close' @click="cancel"/>
        </div>
        <div class="modalContent">
            <slot v-if="collapsed" />
        </div>
        <template #footer>
            <slot name="leftBottomBtn"/>
            <a-button key="back" @click="cancel" v-if="showCancel">{{cancelText}}</a-button>
            <a-button key="submit" type="primary" @click="ok" v-if="showOk" :loading="okLoading">{{okText}}</a-button>
        </template>
    </a-modal>
</template>
<script>
export default {
    components:{
        
    },
    emits:['handleClose','handleOK','afterClose'],
    props: {
        modalTitle:{
            type: String
        },
        modalVisible:{
            type:Boolean,
            default: false
        },
        modalWidth:{
            type: String,
            default:'500px'
        },
        footer:{
            type:Boolean,
            default: true
        },
        showCancel:{
            type: Boolean,
            default: true
        },
        cancelText: {
            type: String,
            default: '取消'
        },
        showOk:{
            type: Boolean,
            default: true
        },
        okText:{
            type: String,
            default: '确定'
        },
        okLoading:{
            type: Boolean,
            default: false
        }
    },
    watch: {
        
    },
    data() {
        return{
            collapsed: true
        }
    },
    
    created() {
    },
    mounted () {
        
    },
    methods: {
        cancel(){
            this.$emit("handleClose")
        },
        ok(){
            this.$emit("handleOK")
        },
        afterClose(){
            this.$emit("afterClose")
        }
    }
}
</script>
<style scoped>
.ant-divider{
    margin: 15px 0;
}
.modalHeader{
    position: relative;
    width:100%;
    height:35px;
    padding:4px 12px;
    background-color: #369FFF;
    line-height: 35px;
    border-radius: 8px 8px 0 0;
}
.modalHeader .title{
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    color: var(--grey-white-100, #FFF);
    font-size: 14px;
    font-weight: 700;
    line-height: 35px;
    /* width: 100%; */
    display: flex;
    align-items: center;
}
.title span{
    margin-left: 5px;
}
.modalHeader .close{
    position: absolute;
    right: 0%;
    top: 50%;
    transform: translateY(-50%);
    margin-right: 12px;
    width: 14px;
    height: 14px;
}
.modalContent{
    width: 100%;
    height: calc(100% - 30px);
    padding: 12px 16px;
    max-height: 630px;
    overflow-y: auto;
}
</style>