<template>
    <a-modal
    :visible="modalVisible"
    :title='null'
    :closable='false'
    :bodyStyle="computedBodyStyle"
    :width="width"
    :footer="!footer ? null : undefined"
    :afterClose="afterClose"
    :maskClosable='false'
    :wrap-class-name=" fullScreen ? 'full-modal' : null"
    centered
    ok-text="确定"
    cancel-text="取消"
    @ok="ok"
    @cancel="cancel"
    id="modalId"
    ref="modalRef"
    >
        <div class="modalHeader" 
        @dblclick="doubleClickHandler" 
        ref="modalTitleRef"
        >
            <div class="title">
                <img src="../../assets/icon/modal.png" style="width:18px;height:18px"/>
                <span>{{modalTitle}}</span>
            </div>
            <img src="../../assets/icon/full.png" class='full' v-if="fullFlag && !fullScreen" @click="full" title="全屏"/>
            <img src="../../assets/icon/unfull.png" class='full' v-if="fullFlag && fullScreen" @click="reduce" title="还原"/>
            <img src="../../assets/icon/closeModel.png" class='close' @click="cancel" title="关闭"/>
        </div>
        <div class="modalContent" ref="contentRef" :style="computedContentStyle">
            <a-spin :spinning="spinning" :tip="loadingTip">
                <slot v-if="collapsed" />
            </a-spin>
        </div>
        <template #footer>
            <slot name="leftBottomBtn"/>
            <a-button key="back" @click="cancel" v-if="showCancel">{{cancelText}}</a-button>
            <a-button key="submit" type="primary" @click="ok" v-if="showOk" :loading="okLoading">{{okText}}</a-button>
        </template>
        <template #modalRender="{ originVNode }">
            <div :style="{transform: `translate(${transformX}px, ${transformY}px)`,}">
                <component :is="originVNode" />
            </div>
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
        // 自定义 modal body 高度：默认保持现有行为（70%）
        bodyHeight: {
            type: String,
            default: '70%'
        },
        // 自定义 modal body 最大高度：例如 '80vh'；默认不设置
        bodyMaxHeight: {
            type: String,
            default: null
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
            loadingTip:"",
            
            startX: 0,
            startY: 0,
            startedDrag: false,
            transformX: 0,
            transformY: 0,
            preTransformX: 0,
            preTransformY: 0,
            dragRect: { left: 0, right: 0, top: 0, bottom: 0 },
        }
    },
    computed: {
        computedBodyStyle() {
            // 默认保持旧行为：固定 height（百分比）
            // 当设置了 bodyMaxHeight 时，使用 maxHeight + auto 高度，避免内部内容“顶破” modal
            const style = { padding: '0px', height: this.bodyHeight };
            if (this.bodyMaxHeight) {
                style.maxHeight = this.bodyMaxHeight;
                style.height = 'auto';
                // 让滚动发生在 modalContent，而不是 body 本身
                style.overflow = 'hidden';
            }
            return style;
        },
        computedContentStyle() {
            if (!this.bodyMaxHeight) return null;
            // body 内部结构：header(35px) + content
            // 这里用 calc 约束内容区最大高度，确保超出时在内容区滚动，不会溢出 modal
            return {
                maxHeight: `calc(${this.bodyMaxHeight} - 35px)`,
                overflowY: 'auto',
            };
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
        // 关闭/隐藏 modal 前先移除焦点，避免 aria-hidden + focus 触发可访问性告警
        blurActiveElement() {
            try {
                const el = document && document.activeElement
                if (el && typeof el.blur === 'function') {
                    el.blur()
                }
            } catch (e) {
                // ignore
            }
        },
        cancel(){
            this.blurActiveElement()
            this.$emit("handleClose")
        },
        ok(){
            this.blurActiveElement()
            this.$emit("handleOK")
        },
        afterClose(){
            this.$emit("afterClose")
        },
        // 全屏展示
        full(){
            this.oldWidth = this.width
            this.width = "100%"
            this.fullScreen = true
            this.$nextTick(() => {
                let height = this.$refs.contentRef.offsetHeight
                this.$emit('setTableHeight',height,'full')
            })
        },
        // 还原
        reduce(){
            this.width = this.oldWidth
            this.fullScreen = false
            this.$nextTick(() => {
                let height = this.$refs.contentRef.offsetHeight
                this.$emit('setTableHeight',height,'reduce')
            })
        },
        // 双击全屏/还原
        doubleClickHandler(){
            if(!this.fullFlag){
                return
            }
            if(this.fullScreen){
                this.reduce()
            }else{
                this.full()
            }
        },
        
        mousedown(event){
            if (!this.startedDrag) {
                this.startX = event.pageX;
                this.startY = event.pageY;
                const bodyRect = document.body.getBoundingClientRect();
                const titleRect = this.$refs.modalTitleRef.getBoundingClientRect();
                this.dragRect.right = bodyRect.width - titleRect.width;
                this.dragRect.bottom = bodyRect.height - titleRect.height;
                this.preTransformX = this.transformX;
                this.preTransformY = this.transformY;
            }
            this.startedDrag = true;
        },
        mouseUp(event){
            this.startedDrag = false
        },
        mousemove(event){
            if (this.startedDrag) {
                this.transformX = this.preTransformX + Math.min(Math.max(this.dragRect.left, event.pageX), this.dragRect.right) - this.startX;
                this.transformY = this.preTransformY + Math.min(Math.max(this.dragRect.top, event.pageY), this.dragRect.bottom) - this.startY;
                // console.log(this.transformX)
                // console.log(this.transformY)
            }
        },
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