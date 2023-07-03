<template>
    <div class="modalHeader">
        <div class="title">
            <img src="../../assets/icon/modelIcon.png" style="width:24px;height:24px"/>
            <span>{{title}}</span>
        </div>
        <img src="../../assets/icon/closeModel.png" class='close' @click="close"/>
    </div>
    <div class="modalContent">
        <slot v-if="collapsed" />
    </div>
</template>
<script>
export default {
    props: {
        title:{
            type: String
        }
    },
    emits:['close'],
    data(){
        return{
            collapsed: true
        }
    },
    methods:{
        close(){
            this.$emit("close")
        }
    }
}
</script>
<style scoped>
.modalHeader{
    position: relative;
    width:100%;
    height:30px;
    padding:4px 12px;
    background-color: #369FFF;
    line-height: 40px;
}
.modalHeader .title{
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    color: var(--grey-white-100, #FFF);
    font-size: 12px;
    font-weight: 700;
    /* width: 100%; */
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
    max-height: 400px;
    overflow-y: auto;
}
</style>