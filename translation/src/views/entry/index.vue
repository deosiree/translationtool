<template>
    <div ref="box" class="box">
        <a-row type="flex">
            <a-col :flex="boxFlex" class="treeBox" ref="treeBox">
                <a-input v-model:value="keyWords" placeholder="关键字搜索" @pressEnter="getClassTree" v-if="treeBoxOpen">
                    <template #suffix>
                        <SearchOutlined style="color: #DCDCDC;"/>
                    </template>
                </a-input>
                <div class="productTree">
                    <a-tree
                    v-if="treeBoxOpen"
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
                        <template #title="{ key: treeKey, title, type,maxByte ,foreignMaxByte}">
                            <a-dropdown :trigger="['contextmenu']">
                                <span v-if="type === 'product'" style="color: #5ba584">{{ title }}</span>
                                <span v-else>{{ title }}</span>
                                <template #overlay>
                                    <a-menu  v-if="$store.state.admin">
                                        <a-menu-item v-if="type !='common' && type != 'product'  && type != 'module'" @click="addClassify(treeKey,'classify')">添加分类</a-menu-item>
                                        <a-menu-item v-if="type !='common' && type != 'product'  && type != 'module'" @click="addClassify(treeKey, 'product')">添加产品</a-menu-item>
                                        <a-menu-item v-if="type === 'product'" @click="productAuthority(treeKey)">权限设置</a-menu-item>
                                        <a-menu-item v-if="type === 'product'" @click="addClassify(treeKey,'module')">添加模块</a-menu-item>
                                        <a-menu-item v-if="type !='department' && type !='common'" @click="editClassify(treeKey, title, type,maxByte,foreignMaxByte)">编辑</a-menu-item>
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
                    <span v-if="treeBoxOpen && treeData.length === 0" style="color: rgba(0, 0, 0, 0.40);margin-left: 40%;">暂无数据</span>
                </div>
                
                
            </a-col>
            <a-col flex="auto" class="dataBox">
                <div class="entryBox" v-if="isProduct">
                    <a-tabs v-model:activeKey="activeKey" type="card" @change="tabChange">
                        <a-tab-pane key="1" tab="词条详情">
                            <ProductEntry :boxHeight="boxHeight" :currentProduct="currentClickProduct" :productEdit="productEdit" ref="productEntry"/>
                        </a-tab-pane>
                        <a-tab-pane key="2" tab="产品版本">
                            <ProductVersion ref="productVersionRef" :boxHeight="boxHeight" :currentProduct="currentClickProduct" :productEdit="productEdit" @viewEntry="viewEntry"/>
                        </a-tab-pane>
                    </a-tabs>
                </div>
                <div class="entryBox" v-else>
                    <CommonEntry :boxHeight="boxHeight" :currentCommon="currentClickProduct"/>
                </div>
                <div class="floatBtn">
                    <left-outlined v-if="treeBoxOpen" @click="openOrCloseTree" title="收起树"/>
                    <right-outlined v-if="!treeBoxOpen" @click="openOrCloseTree" title="展开树"/>
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
  SearchOutlined,
  LeftOutlined,
  RightOutlined,
} from '@ant-design/icons-vue';
import ProductEntry from '@/views/entry/productEntry.vue'
import ProductVersion from '@/views/entry/productVersion.vue'
import CommonEntry from '@/views/entry/commonEntry.vue'
import ClassifyModal from '@/views/entry/classifyModal.vue'
import ProductAuthorityNodal from '@/views/entry/productAuthorityModal.vue'
import { cloneDeep, iteratee } from 'lodash-es';
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
        LeftOutlined,
        RightOutlined,
        ProductEntry,
        ProductVersion,
        CommonEntry,
        ClassifyModal,
        ProductAuthorityNodal
    },
    data(){
        return{
            name:"entry",
            boxFlex: "240px",
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
            productEdit:false,
            treeBoxOpen:true,
            treeHeight: 0
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
            // this.getTreeHeight()
        },

        // 计算树高度
        getTreeHeight(){
            let treeBox = this.$refs.treeBox.$el.offsetHeight
            this.treeHeight = treeBox - 80
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
            this.$refs.productEntry.refresh(this.currentClickProduct)
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
        editClassify(treeKey,title,type,maxByte,foreignMaxByte){
            this.currentClass = {
                key: treeKey,
                title: title,
                maxByte:maxByte,
                foreignMaxByte:foreignMaxByte
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
                this.getproductIsEdit(node.key)
            }else if(node.type === 'common'){
                this.isProduct = false
                this.currentClickProduct = node
            }else if(node.type === 'module'){
                this.isProduct = true
                let product = e.node.parent.node
                let newNode = cloneDeep(node)
                newNode.children = product.children
                this.currentClickProduct = newNode
                this.getproductIsEdit(node.parentId)
            }else {
                if(this.$store.state.admin){
                    this.productEdit = true
                }else{
                    this.productEdit = false
                }
                
                this.currentClickProduct = node
            }
        },
        // 查询产品 用户是否可编辑
        getproductIsEdit(productId){
            if(this.$store.state.admin){
                this.productEdit = true
            }else{
                let params = {productId:productId}
                getUserProduct(params).then((res) => {
                    if(res.data && res.data.write === 1){
                        this.productEdit = true
                    }else{
                        this.productEdit = false
                    }
                })
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
        tabChange(activeKey){
            if(activeKey === '2'){
                // this.$refs.productVersionRef.getProductVersion()
            }else {
                this.$refs.productEntry.setTableHeight()
            }
        },
        // treeBox展开与关闭
        openOrCloseTree(){
            this.treeBoxOpen = !this.treeBoxOpen

            if(this.treeBoxOpen){
                this.boxFlex = '240px'
            }else{
                this.boxFlex = '10px'
            }
        }
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
        padding: 16px;
        border: 1px solid #DCDCDC;
        position: relative;

        .productTree{
            width: calc(100% - 32px);
            height: calc(100% - 70px);
            overflow: auto;
            position: absolute;
            bottom: 16px;
            // border: 1px solid red;
        }
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
        width: calc(100% - 240px);
        height: 100%;
        position: relative;

        .entryBox{
            width: 100%;
            height: 100%;
            // border: 1px solid red;
        }

        .floatBtn{
            width: 16px;
            height: 32px;
            // border: 1px solid #DCDCDC;
            position: absolute;
            left: 0px;
            top: calc(50% - 16px);
            color: #d1d1c8;
            // box-shadow: 1px 6px 12px 0px rgba(241, 189, 46, 0.20), -1px 0px 8px 0px rgba(241, 189, 46, 0.20);

            .anticon{
                margin-top: 8px;
            }
        }
    }
    
}
:deep(.ant-tree){
    width: 100%;
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
// 树结构title超长时 滚动
:deep(.ant-tree-title){
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 200px;
}
</style>