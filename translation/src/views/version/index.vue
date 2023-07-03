<template>
    <a-button @click="alertModel">弹窗</a-button>
    <AuthorityModel :visible="modalVisible" :roleId="roleId" @changeShow="modelColse"/>
</template>
<script>
import AuthorityModel from '@/views/role/authorityModel';
export default{
    components:{
        AuthorityModel
    },
    data(){
        return{
            modalVisible: false,
            roleId:"1234"
        }
    },
    methods: {
        alertModel(){
            this.modalVisible = true
        },
        modelColse(data){
            console.log("params::",data)
            // this.modalVisible = false
            if(data === 'false'){
                this.modalVisible = false
            }else{
                this.modalVisible = true
            }
        }
    }
}
</script>
