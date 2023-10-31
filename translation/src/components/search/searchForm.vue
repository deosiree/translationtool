<template>
    <div class="search">
            <div class="searchItem">
                <slot name="form" v-if="collapsed" />
            </div>
            <img src="../../assets/icon/expand.png" title="展开" v-if="!collapsed" @click="trigger"/>
            <img src="../../assets/icon/retract.png" title="收起" v-else @click="trigger"/>
            <slot name="operate" v-if="collapsed"/>
        </div>
</template>
<script>
export default {
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
    margin-bottom: 5px;
    border-radius: 4px;
    border: 1px solid #f0f0f0;
    padding: 5px 15px 5px 5px;
    width: 100%;
    position: relative;

    .searchItem{
        width: 100%;
        height: 100%;
        border-radius: 4px;
        background-color: #F3F3F3;
        padding: 10px;
    }
    img{
        width: 15px;
        height: 15px;
        position: absolute;
        top: 50%;
        right: 0%;
        transform: translateY(-50%);
    }
}
</style>