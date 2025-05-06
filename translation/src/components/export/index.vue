<template>
    <Modal
    :visible="visible" 
    :okLoading="loading"
    :modalTitle="modalTitle"
    @handleClose="close"
    @handleOK="ok"
    @afterClose="afterClose"
    style="top:30%"
    >
        <div class="content">
            <a-form
                ref="formRef"
                name="custom-validation"
                :model="exportModal"
            >
                <a-form-item
                label="导出字段"
                name="field"
                :rules="[{ required: true, message: '请选择导出字段!' }]"
                >
                    <a-select
                    mode="multiple"
                    v-model:value="exportModal.field"
                    :options="fieldOptions"
                    :fieldNames="{label:'label',value:'label'}"
                    placeholder="请选择"
                    allowClear
                    ></a-select>
                </a-form-item>
            </a-form>
        </div>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import tableParam from '@/views/entry/tableParam';
import {
    queryUserPartiality,
    updateUserPartiality
} from '@/http/api/userPartiality'
export default {
    components:{
        Modal
    },
    emits:['exportClose','exportOk'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:"导出"
        },
        loading:{
            type:Boolean,
            default: false
        }
    },
    
    data() {
        return{
            exportModal:{
                field:undefined
            },
            fieldOptions: tableParam.exportFields,
        }
    },
    
    created() {
    },
    mounted () {
        
        
    },
    watch: {
        
    },
    methods: {
        // 加载用户上次使用选择的字段
        init(){
            queryUserPartiality().then((res) => {
                if(res.data.list && res.data.list.length > 0){
                    let exportColumn = res.data.list[0].exportColumn
                    if(exportColumn != null && exportColumn != ''){
                        this.exportModal.field = exportColumn.split(",")
                    }
                }
            })
        },

        close(){
            this.$emit('exportClose')
        },
       
        ok(){

        },

        afterClose(){

        },
        // 设置偏好
        updatePartiality(data){
            updateUserPartiality(data).then((res) => {

            })
        },
    }
}
</script>

<style scoped>

</style>
