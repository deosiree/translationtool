<template>
    <div class="page">
        <span class="total">共 {{total}} 项数据</span>
        <a-config-provider :locale="locale">
            <a-pagination
                v-model:current="current"
                :page-size-options="pageSizeOptions"
                :total="total"
                show-size-changer
                show-quick-jumper
                @change="pageChange"
            >
                <!-- <template #buildOptionText="props">
                    <span v-if="props.value !== 'all'">{{ props.value }}条/页</span>
                    <span v-else>全部</span>
                </template> -->
            </a-pagination>
        </a-config-provider>
    </div>
</template>
<script>
import zhCN from 'ant-design-vue/lib/locale-provider/zh_CN'
export default {
    props:{
        total:{
            type:Number,
            default:0
        }
    },
    data(){
        return{
            // 汉化包
      		locale: zhCN,
            pageSizeOptions:['10', '20', '50', '100'],
            current: 1,
        }
    },
    methods:{
        pageChange(current,pageSize){
            this.$emit("pageChange",current,pageSize)
        }
    }
}
</script>
<style scoped lang="less">
.page{
    position: absolute;
    bottom: 0%;
    width: 100%;
    
    :deep(.ant-pagination){
        float: right;
    }
    .total{
        position: absolute;
        top: 50%;
        transform: translateY(-50%);
        /* 五级文字/常规 */
        font-size: 12px;
        font-family: Microsoft YaHei;
        font-style: normal;
        font-weight: 400;
        color: black;
    }
}
</style>