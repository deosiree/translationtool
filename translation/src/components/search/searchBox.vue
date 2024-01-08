<template>
    <div class="search">
        <div class="title">
            <span>查询条件：</span>
        </div>
        <div class="content">
            <div class="form">
                <slot name="form" v-if="collapsed" />
            </div>
            <div class="operate" v-if="operate">
                <slot name="operate" v-if="collapsed"/>
            </div>
        </div>
    </div>
</template>
<script>
export default {
    props:{
        operate:{
            type:Boolean,
            default:true
        }
    },
    data(){
        return{
            collapsed: true
        }
    },
    methods:{
        trigger() {
            this.collapsed = !this.collapsed;
            this.$emit("change")
        }
    }
}
</script>
<style scoped lang="less">
.search{
    border: 1px solid #DCDCDC;
    width: 100%;
    // position: relative;

    .title{
        width: 100%;
        display: flex;
        height: 32px;
        padding: 8px 16px;
        align-items: center;
        gap: 8px;
        align-self: stretch;
        background: var(--grey-grey-02, #F3F3F3);

        span{
            color: var(--text-icon-font-gy-190-primary, rgba(0, 0, 0, 0.90));
            /* 四级文字/加粗 */
            font-family: Microsoft YaHei;
            font-size: 14px;
            font-style: normal;
            font-weight: 700;
            line-height: 22px;
        }
    }

    .content{
        display: flex;
        padding: 16px;
        flex-direction: column;
        align-items: flex-start;
        gap: 16px;
        align-self: stretch;
        position: relative;

        .form{
            width: 100%;
            height: 100%;
        }

        .operate{
            display: flex;
            height: 32px;
            justify-content: flex-end;
            align-items: flex-start;
            gap: 8px;
            align-self: stretch;
        }
    }

}
</style>