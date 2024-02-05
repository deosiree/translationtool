<template>
    <div ref="box" class="box">
        <a-row type="flex">
            <a-col flex="240px" class="treeBox">
                <a-input v-model:value="keyWords" placeholder="关键字搜索" @pressEnter="getClassTree">
                    <template #suffix>
                        <SearchOutlined style="color: #DCDCDC;"/>
                    </template>
                </a-input>
                <a-tree
                show-icon
                v-model:expandedKeys="expandedKeys"
                :defaultExpandAll="true"
                :selectedKeys="selectedTreeKeys"
                :tree-data="treeData"
                @select="clickTree"
                draggable
                block-node
                @dragenter="onDragEnter"
                @drop="onDrop"
                >
                    <template #title="{ key: treeKey, title, type,maxByte }">
                        <a-dropdown :trigger="['contextmenu']">
                            <span>{{ title }}</span>
                            <template #overlay>
                                <a-menu  v-if="$store.state.admin">
                                    <a-menu-item v-if="type !='common' && type != 'product'  && type != 'module'" @click="addClassify(treeKey,'classify')">添加分类</a-menu-item>
                                    <a-menu-item v-if="type !='common' && type != 'product'  && type != 'module'" @click="addClassify(treeKey, 'product')">添加产品</a-menu-item>
                                    <a-menu-item v-if="type === 'product'" @click="productAuthority(treeKey)">权限设置</a-menu-item>
                                    <a-menu-item v-if="type === 'product'" @click="addClassify(treeKey,'module')">添加模块</a-menu-item>
                                    <a-menu-item v-if="type !='department' && type !='common'" @click="editClassify(treeKey, title, type,maxByte)">编辑</a-menu-item>
                                    <a-menu-item v-if="type !='department' && type !='common'">
                                        <a-popconfirm
                                            title="确定要删除吗?"
                                            ok-text="是"
                                            cancel-text="否"
                                            @confirm="deleteClassify(treeKey,type)"
                                        >删除
                                        </a-popconfirm>
                                    </a-menu-item>
                                </a-menu>
                            </template>
                        </a-dropdown>
                    </template>
                </a-tree>
                <span v-if="treeData.length === 0" style="color: rgba(0, 0, 0, 0.40);margin-left: 40%;">暂无数据</span>
            </a-col>
            <a-col flex="auto" class="dataBox">
                <div class="entryBox" v-if="isProduct">
                    <a-tabs v-model:activeKey="activeKey" type="card">
                        <a-tab-pane key="1" tab="词条详情">
                            <ProductEntry :boxHeight="boxHeight" :currentProduct="currentClickProduct" :productEdit="productEdit" ref="productEntry"/>
                        </a-tab-pane>
                        <a-tab-pane key="2" tab="产品版本">
                            <ProductVersion :boxHeight="boxHeight" :currentProduct="currentClickProduct" :productEdit="productEdit" @viewEntry="viewEntry"/>
                        </a-tab-pane>
                    </a-tabs>
                </div>
                <div class="entryBox" v-else>
                    <CommonEntry :boxHeight="boxHeight" :currentCommon="currentClickProduct"/>
                </div>
            </a-col>
        </a-row>
    </div>
    <ClassifyModal 
    ref="classifyModal"
    :visible="classifyVisible" 
    :modalTitle="classifyModalTitle"
    :currentClass="currentClass" 
    @classifyClose="classifyClose"
    />
    <ProductAuthorityNodal
    :visible="authorityVisible"
    :productId="authorityProductId"
    @authorityClose="authorityClose"
    />
</template>
<script>
import {
  SearchOutlined
} from '@ant-design/icons-vue';
import ProductEntry from '@/views/entry/productEntry.vue'
import ProductVersion from '@/views/entry/productVersion.vue'
import CommonEntry from '@/views/entry/commonEntry.vue'
import ClassifyModal from '@/views/entry/classifyModal.vue'
import ProductAuthorityNodal from '@/views/entry/productAuthorityModal.vue'
import { 
    getClassTree,
    deleteEntryClassfy,
    updateEntryClassfy
} from "@/http/api/entryManage";
import { 
    deleteProduct,
    getUserProduct
} from "@/http/api/product";
import { message } from 'ant-design-vue';
export default {
    components:{
        SearchOutlined,
        ProductEntry,
        ProductVersion,
        CommonEntry,
        ClassifyModal,
        ProductAuthorityNodal
    },
    data(){
        return{
            name:"entry",
            user:{},
            boxHeight:0,
            keyWords:"",
            isProduct:true,
            activeKey:'1',
            treeData:[],
            expandedKeys:[],
            selectedTreeKeys:[],
            classifyVisible:false,
            classifyModalTitle:"",
            currentClass:{},
            currentClickProduct:{},
            authorityVisible:false,
            authorityProductId:"",
            productEdit:false
        }
    },
    mounted () {
        this.user = this.$store.state.user
        let _this = this
        this.$nextTick(() => {
            this.init()
            _this.boxHeight = _this.$refs.box.offsetHeight
            /** 控制table的高度 */
            window.onresize = function () {
                 _this.boxHeight = _this.$refs.box.offsetHeight
            }
        })
    },
    unmounted() {
        //注销window.onresize事件
        window.onresize = null;
    },
    methods:{

        init(){
            this.getClassTree()
        },
        // 查询分类树
        getClassTree(){
            let params = {
                department:this.$store.state.admin ? '' : this.user.department,
                className: this.keyWords
            }
            getClassTree(params).then((res) => {
                this.treeData = res.data.list
            })
        },
        classifyClose(){
            this.classifyVisible = false
            this.getClassTree()
        },
        // 新增分类或产品
        addClassify(treeKey,type){
            this.currentClass = {
                parentId: treeKey,
                title:'',
                type:type
            }
            this.classifyVisible = true
            if(type === 'product'){
                this.classifyModalTitle = "添加产品"
            }else if(type === 'classify'){
                this.classifyModalTitle = "添加分类"
            }else if(type === 'module'){
                this.classifyModalTitle = "添加模块"
            }
        },
        // 编辑分类或产品
        editClassify(treeKey,title,type,maxByte){
            this.currentClass = {
                key: treeKey,
                title: title,
                maxByte:maxByte
            }
            this.classifyVisible = true
            if(type === 'product'){
                this.classifyModalTitle = "编辑产品"
            }else if(type === 'classify'){
                this.classifyModalTitle = "编辑分类"
            }else if(type === 'module'){
                this.classifyModalTitle = "编辑模块"
            }
        },
        // 删除分类或产品
        deleteClassify(treeKey,type){
            if(type === 'product'){
                let data = [treeKey]
                deleteEntryClassfy(data).then((res) => {
                    
                })
                deleteProduct(data).then((res) => {
                    message.success("删除成功！")
                    this.getClassTree()
                })

            }else if(type === 'classify' || type === 'module'){
                let data = [treeKey]
                deleteEntryClassfy(data).then((res) => {
                    message.success("删除成功！")
                    this.getClassTree()
                })
            }
        },
        // 产品权限分配
        productAuthority(treeKey){
            // message.info("权限设置！")
            this.authorityProductId = treeKey
            this.authorityVisible = true
        },
        authorityClose(){
            this.authorityVisible = false
        },

        // 词条分类点击事件
        clickTree(selectedKeys,e){
            // console.log(e)
            if(e.selected){
                this.selectedTreeKeys = selectedKeys
            }else{
                this.selectedTreeKeys = [e.node.key]
            }
            let node = e.node.dataRef
            // console.log(node)
            if(node.type === 'product'){
                this.isProduct = true
                this.currentClickProduct = node
                if(this.$store.state.admin){
                    this.productEdit = true
                }else{
                    let params = {productId:node.key}
                    getUserProduct(params).then((res) => {
                        if(res.data && res.data.write === 1){
                            this.productEdit = true
                        }else{
                            this.productEdit = false
                        }
                    })
                }
                
            }else if(node.type === 'common'){
                this.isProduct = false
                this.currentClickProduct = node
            }else if(node.type === 'module'){
                this.isProduct = true
                // let product = e.node.parent.node
                // product.filter = node.title
                // this.currentClickProduct = product
                node.key = node.parentId
                this.currentClickProduct = node
            }else {
                this.currentClickProduct = node
            }
        },
        // 分类拖拽
        onDragEnter(info){
            // console.log("onDragEnter:",info)
        },
        onDrop(info){
            // console.log("onDrop",info)
            // 拖拽的节点
            let dragNode = info.dragNode.dataRef
            // 目标节点
            let node = info.node.dataRef
            if(info.dropToGap){
                // 和目标分类同级
                dragNode.parentId = node.parentId
            }else{
                // 是目标分类的子集
                dragNode.parentId = node.key
            }
            if(node.type === 'common'){// 公共库分类无子类
                return
            }

            // updateEntryClassfy(dragNode).then((res) => {
            //     message.success('已保存！') 
            //     this.getClassTree()
            // }).catch((err) => {
            //     message.error("操作失败！")
            // })
        },
        viewEntry(versionId){
            this.activeKey = '1'
            this.$refs.productEntry.getProductVersion()
            this.$refs.productEntry.changeVersion(versionId)
        },
        
    }
}
</script>

<style scoped lang="less">
.box{
    width: 100%;
    height: 100%;
    padding:16px;
    // border: 1px solid red;

    .ant-row{
        height: 100%;
    }

    .treeBox{
        display: flex;
        padding: 16px;
        flex-direction: column;
        align-items: flex-start;
        gap: 8px;
        align-self: stretch;
        border: 1px solid #DCDCDC;
    }

    .dataBox{
        display: flex;
        padding-bottom: 0px;
        flex-direction: column;
        align-items: flex-start;
        gap: 16px;
        flex: 1 0 0;
        align-self: stretch;
        border: 1px solid#DCDCDC;
        border-left: none;

        .entryBox{
            width: 100%;
            height: 100%;
            // border: 1px solid red;
        }
    }
    
}
.ant-tabs{
    height: 100%;
}
:deep(.ant-tabs-content){
    height: 100%;
}
:deep(.ant-tabs-nav-wrap){
    background-color: rgba(250, 250, 250, 1);
}
:deep(.ant-tabs-tabpane){
    height: 100%;
}
:deep(.ant-tree .ant-tree-node-content-wrapper.ant-tree-node-selected){
    background-color: #EEF7FF;
}
:deep(.ant-tree-node-selected .ant-dropdown-trigger){
    color: #369FFF;
}
</style>