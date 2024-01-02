<template>
    <div class="tableBox" :style="{ height: height + 'px' }">
        <div class="title">
            <span>{{title}}：</span>
        </div>
        <div class="content">
            <div class="operateBox" v-if="showOperate">
                <div class="label">
                    <slot name="label"/>
                </div>
                <div class="operate">
                    <slot name="operate"/>
                </div>
            </div>
            <div class="data">
                <slot name="data" v-if="collapsed" />
            </div>
            
        </div>
    </div>
</template>
<script>
export default {
    props:{
        height:0,
        title:{
            type:String
        },
        showOperate:{
            type:Boolean,
            default: false
        }
    },
    data(){
        return{
            collapsed: true,
            width:1200
        }
    },
    mounted () {
        
    },
    methods:{
        
    }
}
</script>
<style scoped lang="less">
.tableBox{
    border: 1px solid #DCDCDC;
    border-top: none;
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
        // gap: 8px;
        position: relative;

        .operateBox{
            display: flex;
            align-items: center;

            .label{
                display: flex;
                color: black;
                gap: 8px;
                align-items: center;
            }

            .operate{
                display: flex;
                margin-left: auto;
                gap: 8px;
            }
        }

        .data{
            // display: flex;
            width: 100%;
            position: relative;
        }
        
    }

}
</style>