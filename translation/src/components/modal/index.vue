<template>
    <a-modal
    :visible="modalVisible"
    :title='null'
    :closable='false'
    :bodyStyle="{padding: '0px',height: '70%'}"
    :width="width"
    :footer="!footer ? null : undefined"
    :afterClose="afterClose"
    :maskClosable='false'
    :wrap-class-name=" fullScreen ? 'full-modal' : null"
    ok-text="确定"
    cancel-text="取消"
    @ok="ok"
    @cancel="cancel"
    id="modalId"
    ref="modalRef2"
    >
        <div class="modalHeader">
            <div class="title">
                <img src="../../assets/icon/modal.png" style="width:18px;height:18px"/>
                <span>{{modalTitle}}</span>
            </div>
            <img src="../../assets/icon/full.png" class='full' v-if="fullFlag && !fullScreen" @click="full" title="全屏"/>
            <img src="../../assets/icon/unfull.png" class='full' v-if="fullFlag && fullScreen" @click="reduce" title="还原"/>
            <img src="../../assets/icon/closeModel.png" class='close' @click="cancel" title="关闭"/>
        </div>
        <div class="modalContent" ref="contentRef">
            <a-spin :spinning="spinning" :tip="loadingTip">
                <slot v-if="collapsed" />
            </a-spin>
        </div>
        <template #footer>
            <slot name="leftBottomBtn"/>
            <a-button key="back" @click="cancel" v-if="showCancel">{{cancelText}}</a-button>
            <a-button key="submit" type="primary" @click="ok" v-if="showOk" :loading="okLoading">{{okText}}</a-button>
        </template>
    </a-modal>
</template>
<script>
import {
    ExpandOutlined,
    ExpandAltOutlined
} from '@ant-design/icons-vue';
export default {
    components:{
        ExpandOutlined,
        ExpandAltOutlined
    },
    emits:['handleClose','handleOK','afterClose','setTableHeight'],
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
        },
        fullFlag:{
            type: Boolean,
            default: false
        }
    },
    data() {
        return{
            collapsed: true,
            fullScreen: false,
            oldWidth:"",
            width:"",
            spinning: false,
            loadingTip:""
        }
    },
    
    created() {
    },
    mounted () {
        this.width = this.modalWidth
    },
    watch: {
        modalWidth(newval,oldval){
            this.width = newval
        }
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
        },
        full(){
            this.oldWidth = this.width
            this.width = "100%"
            this.fullScreen = true
            this.$nextTick(() => {
                let height = this.$refs.contentRef.offsetHeight
                this.$emit('setTableHeight',height,'full')
            })
        },
        reduce(){
            this.width = this.oldWidth
            this.fullScreen = false
            this.$nextTick(() => {
                let height = this.$refs.contentRef.offsetHeight
                this.$emit('setTableHeight',height,'reduce')
            })
        }
    }
}
</script>
<style scoped lang="less">
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
.full{
    position: absolute;
    right: 30px;
    top: 50%;
    transform: translateY(-50%);
    margin-right: 12px;
    width: 20px;
    height: 20px;
    color: #FFF;
}
.modalContent{
    width: 100%;
    height: calc(100% - 30px);
    padding: 12px 16px;
    // max-height: 700px;
    overflow-y: auto;
}

</style>
<style lang="less">
.full-modal {
  .ant-modal {
    max-width: 100%;
    top: 0;
    padding-bottom: 0;
    margin: 0;
  }
  .ant-modal-content {
    display: flex;
    flex-direction: column;
    height: calc(100vh);
  }
  .ant-modal-body {
    flex: 1;
  }
}
</style>