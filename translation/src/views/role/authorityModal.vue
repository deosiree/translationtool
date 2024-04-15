<template>
    <Modal
    :visible="visible" 
    :modalTitle="modalTitle"
    :modalWidth="modalWidth" 
    @handleClose="handleClose"
    @handleOK="handleOK"
    >
        <!-- 全选 -->
        <!-- <a-checkbox v-model:checked="CheckAll" @change="handleCheckAllChange" :indeterminate="allIndeterminate">全选</a-checkbox> -->
        <div v-for="(groupItem,groupIndex) in dataSource" :key="groupItem.id" style="margin:10px 0;">
            <a-checkbox
            v-model:checked="checkedMenu[groupIndex]"
            @change="groupAllChange(groupIndex)"
            :indeterminate="indeterminate[groupIndex]"
            >{{groupItem.menuName}}</a-checkbox>
            
            <div style="margin:10px 0 10px 25px;">
            <a-checkbox
                v-for="(item,index) in groupItem.authorities"
                :label="item.authorityName"
                :key="item.id"
                :value="item.id"
                v-model:checked="checkedAuthority[groupIndex][index]"
                @change="groupBtnChange(groupIndex)"
            >{{item.authorityName}}</a-checkbox>
            </div>
            <a-divider v-if="groupIndex !== dataSource.length -1"/>
        </div>
    </Modal>
</template>
<script>
import Modal from '@/components/modal/index.vue';
import { message } from 'ant-design-vue';
import { 
    getMenuInfoByRole,
    bindPermission
} from "@/http/api/role";
export default {
    components:{
        Modal
    },
    emits:['changeShow','queryDataSource'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        roleId:{
            type: String
        }
    },
    watch: {
        // roleId(newV) {
        //     this.queryDataSource(newV)
        // } 
    },
    data() {
        return{
            modalTitle:'权限绑定',
            modalWidth:'50%',
            dataSource: [],
            CheckAll: false,
            checkedMenu: [], // 分组全选选中情况
            checkedAuthority: [], // 所有小选的选中情况
            indeterminate: [],
            allIndeterminate: false
        }
    },
    
    created() {
        // this.handleDataSource()
        
    },
    mounted () {
        this.$nextTick(() => {
            
        })
    },
    methods: {
        // 获取权限数据
        queryDataSource(roleId){
            // console.log("roleID:",roleId)
            let params = {
                roleID: roleId
            }
            getMenuInfoByRole(params).then((res) => {
                this.dataSource = res.data.list
                this.handleDataSource()
            })
            
        },
        //处理权限数据
        handleDataSource(){
            this.checkedAuthority = []
            this.checkedMenu = []
            this.indeterminate = []
            // 处理分组全选数据
            this.dataSource.forEach(item => {
                this.checkedMenu.push(item.clecked)
                let childArr = []
                let trueIndex = 0
                item.authorities.forEach(it =>{
                    childArr.push(it.clecked)
                    if(it.clecked){
                        trueIndex ++
                    }
                })
                if(trueIndex === item.authorities.length || trueIndex === 0){
                    this.indeterminate.push(false)
                }else{
                    this.indeterminate.push(true)
                }
                this.checkedAuthority.push(childArr)
            })
        },
        handleOK(){
            let params = this.exportData()
            bindPermission(params).then((res) => {
                message.success("配置成功！")
                this.$emit("changeShow",'false')
            })
        },
        handleClose(){
            // this.modalVisible = false
            this.$emit("changeShow",'false')
        },
        handleCheckAllChange() {
            // 1. 全选控制分组全选
            for (let i = 0; i < this.checkedMenu.length; i++) {
                this.checkedMenu[i] = this.CheckAll
                // 2. 全选控制分组小选全选
                for (let j = 0; j < this.checkedAuthority[i].length; j++) {
                this.checkedAuthority[i][j] = this.CheckAll
                }
            }
        },
        // 输出选中的小选名
        exportData() {
            let authority = []
            let menu = []
            for (let i = 0; i < this.checkedAuthority.length; i++) {
                for (let j = 0; j < this.checkedAuthority[i].length; j++) {
                    if (this.checkedAuthority[i][j]) {
                        authority.push(this.dataSource[i].authorities[j].id)
                    }
                }
            }
            for (let i = 0; i < this.checkedMenu.length; i++){
                if(this.checkedMenu[i] || this.indeterminate[i]){
                    menu.push(this.dataSource[i].id)
                }
            }
            let params = {
                authorityIDList: authority,
                menuIDList: menu,
                roleID: this.roleId
            }
            return params
        },
        groupAllChange(index) {
            // 1. 分组全选控制小选
            let tag = this.checkedMenu[index]
            if(tag === false){
                //当前子节点全部选中时 反选  未全部选中时 则全部选中
                tag = this.checkedAuthority[index].every(item => item === true) ? false : true
            }
            this.checkedMenu[index] = tag
            for (let i = 0; i < this.checkedAuthority[index].length; i++) {
                this.checkedAuthority[index][i] = tag
            }
            // 2. 分组全选控制全选
            this.CheckAll = this.checkedMenu.every(item => item === true)

            // 3. 控制分组半选样式
            let flag =  this.checkedAuthority[index].every(item => item === true) || this.checkedAuthority[index].every(item => item === false)
            if(flag){
                this.indeterminate[index] = false
            }else{
                this.indeterminate[index] = true
            }
        },
        // 小选控制全选
        groupBtnChange(index) {
            // 1. 小选控制分组全选
            let tag = this.checkedAuthority[index].every(item => item === true)
            this.checkedMenu[index] = tag
            if(tag || this.checkedAuthority[index].every(item => item === false)){
                this.indeterminate[index] = false
            }else{
                this.indeterminate[index] = true
            }
            // 2. 小选控制全选
            this.CheckAll = this.judge()
        },
        // 判断所有小选的选中情况
        judge() {
            for (let i = 0; i < this.checkedAuthority.length; i++) {
                for (let j = 0; j < this.checkedAuthority[i].length; j++) {
                if (this.checkedAuthority[i][j] === false) {
                    return false
                }
                }
            }
            return true
        }
    }
}
</script>
<style scoped>
.ant-divider{
    margin: 15px 0;
}

</style>