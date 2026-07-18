<template>
    <div class="search" :style="controlWidthStyle">
        <div class="title">
            <span>{{titleName}}：</span>
            <up-outlined class="icon" v-if="showContent" title="收起" @click="trigger"/>
            <down-outlined class="icon" v-if="!showContent" title="展开" @click="trigger"/>
        </div>
        <div class="content" v-if="showContent">
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
import {
    UpOutlined,
    DownOutlined
} from '@ant-design/icons-vue';
import { SEARCH_CONTROL_WIDTH_CSS } from './searchControlWidth';
export default {
    components:{
        UpOutlined,
        DownOutlined
    },
    props:{
        operate:{
            type:Boolean,
            default:true
        },
        defaultTitleName:{
            type:String,
            default:"查询条件"
        },
    },
    data(){
        return{
            collapsed: true,
            showContent: true,
            titleName:this.defaultTitleName,
            controlWidthStyle: {
                '--search-control-width': SEARCH_CONTROL_WIDTH_CSS,
            },
        }
    },
    methods:{
        trigger() {
            this.showContent = !this.showContent;
            this.$emit("change")
        }
    }
}
</script>
<style scoped lang="less">
.search{
    border: 1px solid #DCDCDC;
    width: 100%;

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

        .icon{
            margin-left:auto;
            color:rgba(0, 0, 0, 0.4);
        }
    }

    .content{
        display: flex;
        padding: 16px;
        flex-direction: column;
        align-items: flex-start;
        gap: 16px;
        align-self: stretch;
        width: 100%;

        .form{
            width: 100%;
            height: 100%;

            /* 查询控件宽度唯一源：页面勿再写 style="width: …" */
            :deep(.ant-form-item-control){
                width: var(--search-control-width);
            }
            :deep(.ant-form-item-control-input-content > *){
                width: 100%;
            }
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