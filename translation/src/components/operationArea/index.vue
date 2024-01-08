<template>
    <div class="area" :style="{ height: height + 'px' }">
        <div class="title">
            <span>{{title}}：</span>
            <div class="closeIcon" @click="close">
                <img src="../../assets/icon/close.png"/>
            </div>
        </div>
        <div class="content">
            <slot name="content" v-if="collapsed" />
        </div>
    </div>
</template>
<script>
export default {
    data(){
        return{
            collapsed: true
        }
    },
    props:{
        height:0,
        title:{
            type:String
        }
    },
    methods:{
        close(){
            console.log("close...........")
            this.$emit("close")
        }
    }
}
</script>
<style scoped lang="less">
.area{
    border: 1px solid #DCDCDC;
    border-top: none;
    width: 100%;
    position: relative;

    .title{
        width: 100%;
        display: flex;
        padding: 5px 16px;
        justify-content: space-between;
        align-items: center;
        align-self: stretch;
        background: var(--primary-blue-light, #EEF7FF);

        span{
            color: var(--text-icon-font-gy-190-primary, rgba(0, 0, 0, 0.90));
            /* 四级文字/加粗 */
            font-family: Microsoft YaHei;
            font-size: 14px;
            font-style: normal;
            font-weight: 700;
            line-height: 22px;
        }

        .closeIcon{
            display: flex;
            width: 20px;
            height: 20px;
            margin-left: auto;
        }
        .closeIcon:hover{
            cursor: pointer;
        }
    }

    .content{
        display: flex;
        padding: 16px;
        flex-direction: column;
        align-items: flex-start;
        gap: 16px;
        align-self: stretch;
        height: calc(100% - 32px);
        width: 100%;
        overflow: auto;
        position: absolute;
    }

}
</style>