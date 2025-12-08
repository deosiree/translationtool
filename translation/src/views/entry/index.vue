<template>
  <div ref="box" class="box">
    <a-row type="flex">
      <a-col :flex="boxFlex" class="treeBox" ref="treeBox">
        <a-input v-model:value="keyWords" placeholder="关键字搜索" @pressEnter="getClassTree" v-if="treeBoxOpen">
          <template #suffix>
            <SearchOutlined style="color: #DCDCDC;" />
          </template>
        </a-input>
        <div class="productTree">
          <a-tree v-if="treeBoxOpen" show-icon v-model:expandedKeys="expandedKeys" :defaultExpandAll="true" :selectedKeys="selectedTreeKeys"
            :tree-data="treeData" @select="clickTree" @rightClick="rightClickTree" draggable block-node @dragenter="onDragEnter" @drop="onDrop">
            <template #title="{ key: treeKey, title, type,maxByte ,foreignMaxByte,codeBranch}">
              <a-dropdown :trigger="['contextmenu']">
                <span v-if="type === 'common'" style="color: #001fb8">{{ title }}</span>
                <span v-else-if="type === 'classify'" style="color: #7d7d7d">{{ title }}</span>
                <span v-else-if="type === 'product'" style="color: #5ba584">{{ title }}</span>
                <span v-else-if="type === 'module'" style="color: #a55b7c">{{ title }}</span>
                <span v-else>{{ title }}</span>
                <template #overlay>
                  <a-menu v-if="$store.state.admin">
                    <a-menu-item v-if="currentDepartment.ops.has('needIP')" @click="update(treeKey)">更新</a-menu-item>
                    <!-- <a-menu-item v-if="currentDepartment.ops.has('needIP')" @click="redundantCheck(treeKey)">冗余校验</a-menu-item> -->
                    <a-menu-item v-if="type !='common' && type != 'product'  && type != 'module'"
                      @click="addClassify(treeKey,'classify')">添加分类</a-menu-item>
                    <a-menu-item v-if="type !='common' && type != 'product'  && type != 'module'"
                      @click="addClassify(treeKey, 'product')">添加产品</a-menu-item>
                    <a-menu-item v-if="type === 'product'" @click="productAuthority(treeKey)">权限设置</a-menu-item>
                    <a-menu-item v-if="type === 'product'" @click="addClassify(treeKey,'module')">添加模块</a-menu-item>
                    <a-menu-item v-if="type !='department' && type !='common'"
                      @click="editClassify(treeKey, title, type,maxByte,foreignMaxByte,codeBranch)">编辑</a-menu-item>
                    <a-menu-item v-if="type !='department' && type !='common'">
                      <a-popconfirm title="确定要删除吗?" ok-text="是" cancel-text="否" @confirm="deleteClassify(treeKey,type)">删除
                      </a-popconfirm>
                    </a-menu-item>
                    <a-menu-item v-if="currentDepartment.ops.has('dev') &&currentDepartment.ops.has('needIP') && type =='classify'"
                      @click="createBranch(treeKey)" :disabled="createbranchStatus=='执行中'" ref="createBranchMenu">
                      分支新建{{createbranchStatus=='执行中'?'(执行中)':''}}
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
              <ProductEntry :boxHeight="boxHeight" :currentProduct="currentClickProduct" :productEdit="productEdit" ref="productEntry" />
            </a-tab-pane>
            <a-tab-pane key="2" tab="产品版本">
              <ProductVersion ref="productVersionRef" :boxHeight="boxHeight" :currentProduct="currentClickProduct" :productEdit="productEdit"
                @viewEntry="viewEntry" />
            </a-tab-pane>
          </a-tabs>
        </div>
        <div class="entryBox" v-else>
          <CommonEntry :boxHeight="boxHeight" :currentCommon="currentClickProduct" modalTitle="更新详情" />
        </div>
        <div class="floatBtn">
          <left-outlined v-if="treeBoxOpen" @click="openOrCloseTree" title="收起树" />
          <right-outlined v-if="!treeBoxOpen" @click="openOrCloseTree" title="展开树" />
        </div>
      </a-col>
    </a-row>
  </div>
  <RedundantModal ref="redundantModal" :visible="redundantVisible" :modalTitle="classifyModalTitle" :redundantClassfyID="redundantClassfyID"
    @redundantClose="redundantClose" style="width:700px;" />
  <UpdateModal ref="updateModal" :visible="updateVisible" :modalTitle="classifyModalTitle" :updateClassfyID="updateClassfyID"
    @updateClose="updateClose" style="width:700px;" />
  <CreateBranchModal ref="createBranchModal" :treeNode="currentClickProduct" :visible="createBranchVisible" :modalTitle="classifyModalTitle"
    :createBranchClassfyID="createBranchClassfyID" @createBranchClose="createBranchClose" style="width:70%;" />
  <ClassifyModal ref="classifyModal" :visible="classifyVisible" :modalTitle="classifyModalTitle" :currentClass="currentClass"
    :treeNode="currentClickProduct" @classifyClose="classifyClose" />
  <ProductAuthorityModal :visible="authorityVisible" :productId="authorityProductId" @authorityClose="authorityClose" />
</template>
<script>
import {
  SearchOutlined,
  LeftOutlined,
  RightOutlined,
} from "@ant-design/icons-vue";
import ProductEntry from "@/views/entry/productEntry.vue";
import ProductVersion from "@/views/entry/productVersion.vue";
import CommonEntry from "@/views/entry/commonEntry.vue";
import ClassifyModal from "@/views/entry/classifyModal.vue";
import ProductAuthorityModal from "@/views/entry/productAuthorityModal.vue";
import UpdateModal from "@/views/entry/updateModal.vue";
import RedundantModal from "@/views/entry/redundantModal.vue";
import CreateBranchModal from "@/views/entry/createBranchModal.vue";
import { cloneDeep, iteratee } from "lodash-es";
import {
  getClassTree,
  deleteEntryClassfy,
  getUpdateEntryByClassfy,
  updateEntryByEntrySource,
  getEntrysourceListByClassfy,
} from "@/http/api/entryManage";
import { deleteProduct, getUserProduct } from "@/http/api/product";
import { message } from "ant-design-vue";
import { setModalAriaHidden, randomMsg } from "@/utils/commonUtils";
import commonParam from "@/utils/commonParam";
export default {
  components: {
    SearchOutlined,
    LeftOutlined,
    RightOutlined,
    ProductEntry,
    ProductVersion,
    CommonEntry,
    ClassifyModal,
    ProductAuthorityModal,
    UpdateModal,
    RedundantModal,
    CreateBranchModal,
  },
  data() {
    return {
      name: "entry",
      boxFlex: "240px",
      user: {},
      currentDepartment: {
        label: "部门名称",
        value: "name",
        ops: new Set(),
      }, // 当前用户所在部门的相关信息
      boxHeight: 0,
      keyWords: "",
      isProduct: true,
      activeKey: "1",
      treeData: [],
      expandedKeys: [],
      selectedTreeKeys: [],
      classifyVisible: false,
      updateVisible: false,
      updateClassfyID: "", // 传参给子组件，让子组件调用http请求
      redundantVisible: false,
      redundantClassfyID: "",
      createBranchVisible: false,
      createBranchClassfyID: "", // 传参给子组件，让子组件调用http请求
      createbranchStatus: "未执行", // 查询分支新建状态
      classifyModalTitle: "",
      currentClass: {},
      currentClickProduct: {},
      authorityVisible: false,
      authorityProductId: "",
      productEdit: false,
      treeBoxOpen: true,
      treeHeight: 0,
    };
  },
  mounted() {
    let _this = this;
    this.$nextTick(() => {
      this.user = this.$store.state.user;
      // 获取当前用户所在部门的相关信息
      if (
        Object.keys(commonParam.departmentMap).includes(this.user.department)
      ) {
        this.currentDepartment =
          commonParam.departmentMap[this.user.department];
      } else {
        this.currentDepartment = commonParam.departmentMap["default"];
      }
      this.init();
      _this.boxHeight = _this.$refs.box.offsetHeight;
      /** 控制table的高度 */
      window.onresize = function () {
        _this.boxHeight = _this.$refs.box.offsetHeight;
      };
    });
  },
  unmounted() {
    //注销window.onresize事件
    window.onresize = null;
  },
  methods: {
    init() {
      this.getClassTree();
      // this.getTreeHeight()
    },

    // 计算树高度
    getTreeHeight() {
      let treeBox = this.$refs.treeBox.$el.offsetHeight;
      this.treeHeight = treeBox - 80;
    },
    // 查询分类树
    getClassTree() {
      let params = {
        // department: this.$store.state.admin ? "" : this.user.department,// 分类树改结构了，所以返回有误，且分类树的权限由管理员变为了超管，由前端来判断分类树的展示
        department: "",
        className: this.keyWords,
      };
      getClassTree(params)
        .then((res) => {
          this.treeData = res.data.list;
          this.getClassTreeByUsed();
        })
        .catch((err) => {
          message.error(err.message);
        });
    },
    // 根据用户权限限制状态树的展示
    async getClassTreeByUsed() {
      let treeScoped = ["公共库"]; // 状态树可展示的字段
      if (this.user.roleName.includes("超级管理员"))
        treeScoped = commonParam.treeScoped.map((item) => item.title);
      else {
        treeScoped.push(this.user.department);
      }
      let treeData = cloneDeep(this.treeData);
      let treeDataScoped = [];
      // 遍历树数据，根据部门名过滤
      treeDataScoped = await treeData.filter((company) => {
        // 如果有公司信息（如：“公共库”）
        if (treeScoped.includes(company.title)) return true;
        if (company.children) {
          // 过滤出包含目标部门的公司
          company.children = company.children.filter((department) => {
            return treeScoped.includes(department.title);
          });
          // 若公司有符合条件的部门，则保留该公司
          return company.children.length > 0;
        }
        return false;
      });
      // if (!this.$store.state.admin) {
      //   // 管理员可以看到所有产品 ,非管理员还需要过滤无查看权限的产品
      //   let adminTreeData = cloneDeep(treeDataScoped);
      //   treeDataScoped = [];
      //   for (const companyData of adminTreeData) {
      //     if (companyData.title == "公共库") {
      //       treeDataScoped.push(companyData);
      //       continue;
      //     }
      //     // console.log(`进入公司的过滤`, companyData.children);
      //     // 公司->部门
      //     const company_children = [];
      //     for (const departmentData of companyData.children) {
      //       // console.log(`进入部门过滤`, departmentData.children);
      //       // 部门->分类
      //       const depart_children = [];
      //       for (let classifyData of departmentData.children) {
      //         if (classifyData.title == "公共库") {
      //           depart_children.push(classifyData);
      //           continue;
      //         }
      //         if (classifyData.type == "classify") {
      //           classifyData = await this.filterClassify(
      //             classifyData,
      //             treeScoped
      //           );
      //           if (classifyData.children.length > 0) {
      //             depart_children.push(classifyData); // 分类->部门
      //             // console.log("部门增加分类：", classifyData, depart_children);
      //           }
      //         } else if (classifyData.type == "product") {
      //           const depart_product = await this.filterProduct(classifyData);
      //           // console.log("产品过滤结果2：", depart_product);
      //           if (depart_product.canKeep) {
      //             depart_children.push(depart_product.node); // 产品->部门
      //             // console.log("部门增加产品：", depart_product);
      //           }
      //         }
      //       }
      //       if (depart_children.length > 0) {
      //         departmentData.children = depart_children;
      //         company_children.push(departmentData); // 部门->公司
      //       }
      //       // console.log("部门的分类变更为：", departmentData);
      //     }
      //     companyData.children = company_children;
      //     // console.log("公司的分类变更为：", companyData.children, companyData);
      //     if (companyData.children.length > 0) {
      //       treeDataScoped.push(companyData); // 公司->状态树
      //     }
      //   }
      // }
      this.treeData = treeDataScoped;
      // console.log("过滤后的分类树", this.treeData);
    },
    // 根据读写权限过滤分类级的状态树（非管理员）
    async filterClassify(classify, treeScoped) {
      // console.log(`开始过滤分类：${classify.title}`, classify);
      const filterData = cloneDeep(classify);
      // 1. 处理所有子节点：返回 { canKeep: 是否保留, node: 处理后的节点 }
      const processedChildren = await Promise.all(
        filterData.children.map(async (node) => {
          if (node.type === "classify") {
            // 递归过滤子分类
            const filteredSubClassify = await this.filterClassify(
              node,
              treeScoped
            );
            // 保留条件：过滤后的子分类仍有子节点（或根据需求调整，比如自身权限）
            const canKeep = filteredSubClassify.children.length > 0;
            return { canKeep: canKeep, node: filteredSubClassify };
          } else if (node.type === "product") {
            let res_ = await this.filterProduct(node);
            // console.log("产品过滤结果：", res_);
            return res_;
          }
          // 其他类型默认不保留
          return { canKeep: false, node };
        })
      );

      // console.log("过滤前的节点结构", processedChildren);
      // 2. 过滤出需要保留的节点，并还原为原始节点结构
      filterData.children = [];
      for (const child of processedChildren) {
        if (child.canKeep) {
          filterData.children.push(child.node);
        }
      }
      // console.log("过滤后的分类：", filterData);
      return filterData;
    },
    // 根据读写权限过滤产品级的状态树（非管理员）
    async filterProduct(product) {
      // console.log(`开始过滤产品：${product.title}`, product);
      // 保留条件：产品能被查看
      let filterProductRes = { canKeep: false, node: product };
      if (!this.$store.state.admin) {
        filterProductRes.canKeep = await getUserProduct({
          productId: product.key,
        })
          .then((res) => {
            return res.data && res.data.read === 1;
          })
          .catch((err) => {
            // console.log(`请联系管理员增加该产品的查看权限。`, product);
            return false;
          });
      }
      // console.log(product.title, "能否被查看", filterProductRes);
      return filterProductRes;
    },
    classifyClose(closeFlag) {
      this.classifyVisible = false;
      if (!closeFlag) {
        // 如果只是close的话，不用重新获取树和刷新页面
        this.getClassTree();
        // console.log("close",this.currentClickProduct)
        // this.$refs.productEntry.refresh(this.currentClickProduct);
      } else console.log("没进close，不用重新获取树和刷新页面");
    },
    updateClose() {
      this.updateVisible = false;
    },
    redundantClose() {
      this.redundantVisible = false;
    },
    createBranchClose(scflag) {
      if (scflag) {
        // 如果创建成功，重新获取树和刷新页面
        this.getClassTree();
        message.success("分支创建执行成功，正在拷贝lang中词条");
      } else {
        console.log("分支创建执行失败，不用重新获取树和刷新页面");
      }
      this.createBranchVisible = false;
    },
    // 更新
    update(treeKey) {
      this.classifyModalTitle = "更新详情";
      this.updateClassfyID = treeKey; // treeKey就是classfyID  有些是数字 有些是uuid
      this.updateVisible = true; // 显示弹窗
      setModalAriaHidden(this, document);
    },
    // 冗余校验
    redundantCheck(treeKey) {
      this.classifyModalTitle = "冗余校验";
      this.redundantClassfyID = treeKey; // treeKey就是classfyID  有些是数字 有些是uuid
      this.redundantVisible = true; // 显示弹窗
      setModalAriaHidden(this, document);
    },
    // 分支新建
    createBranch(treeKey) {
      this.classifyModalTitle = "分支新建";
      this.createBranchClassfyID = treeKey; // treeKey就是classfyID  有些是数字 有些是uuid
      this.createBranchVisible = true; // 显示弹窗
      setModalAriaHidden(this, document);
    },
    // 新增分类或产品
    addClassify(treeKey, type) {
      this.currentClass = {
        parentId: treeKey,
        title: "",
        type: type,
      };
      this.classifyVisible = true;
      setModalAriaHidden(this, document);
      if (type === "product") {
        this.classifyModalTitle = "添加产品";
      } else if (type === "classify") {
        this.classifyModalTitle = "添加分类";
      } else if (type === "module") {
        this.classifyModalTitle = "添加模块";
      }
    },
    // 编辑分类或产品
    editClassify(treeKey, title, type, maxByte, foreignMaxByte, codeBranch) {
      this.currentClass = {
        key: treeKey,
        title: title,
        maxByte: maxByte,
        foreignMaxByte: foreignMaxByte,
        codeBranch: codeBranch,
      };
      this.classifyVisible = true;
      setModalAriaHidden(this, document);
      if (type === "product") {
        this.classifyModalTitle = "编辑产品";
      } else if (type === "classify") {
        this.classifyModalTitle = "编辑分类";
      } else if (type === "module") {
        this.classifyModalTitle = "编辑模块";
      }
    },
    // 删除分类或产品
    deleteClassify(treeKey, type) {
      let data = [treeKey];
      deleteEntryClassfy(data).then((res) => {
        // 删除产品，分类，模块都是复用deleteEntryClassfy这个接口
        message.success("删除成功！");
        this.getClassTree();
      });
      // if (type === "product") {
      //   let data = [treeKey];
      //   deleteEntryClassfy(data).then((res) => {});
      //   deleteProduct(data).then((res) => {
      //     message.success("删除成功！");
      //     this.getClassTree();
      //   });
      // } else if (type === "classify" || type === "module") {
      //   let data = [treeKey];
      //   deleteEntryClassfy(data).then((res) => {
      //     message.success("删除成功！");
      //     this.getClassTree();
      //   });
      // }
    },
    // 产品权限分配
    productAuthority(treeKey) {
      // message.info("权限设置！")
      this.authorityProductId = treeKey;
      this.authorityVisible = true;
      setModalAriaHidden(this, document);
    },
    authorityClose() {
      this.authorityVisible = false;
    },

    // 目录树点击事件
    clickTree(selectedKeys, e) {
      // console.log("this.treeData", this.treeData);
      // console.log("触发点击事件", selectedKeys);
      if (e.selected) {
        this.selectedTreeKeys = selectedKeys;
      } else {
        this.selectedTreeKeys = [e.node.key];
      }
      // 为currentClickProduct赋值，根据当前目录树传给子组件
      let node = e.node.dataRef;
      // console.log(node)
      this.isProduct = true; // 除了公开库common展示CommonEntry，其他都是true展示productEntry和productVersion
      if (node.type === "product") {
        this.currentClickProduct = node;
        this.getproductIsEdit(node.key);
      } else if (node.type === "common") {
        this.isProduct = false;
        this.currentClickProduct = node;
      } else if (node.type === "module") {
        let product = e.node.parent.node;
        let newNode = cloneDeep(node);
        newNode.children = product.children;
        this.currentClickProduct = newNode;
        this.getproductIsEdit(node.parentId);
      } else {
        if (this.$store.state.admin) {
          this.productEdit = true;
        } else {
          this.productEdit = false;
        }

        this.currentClickProduct = node;
      }
    },
    // 目录树右击事件
    async rightClickTree(e) {
      // 阻止浏览器默认右键菜单
      e.event.preventDefault();

      // console.log("触发右击事件", e);
      const treeKey = e.node.dataRef.key;
      // console.log("classfyID",treeKey);
      this.selectedTreeKeys = [treeKey];

      // 模拟左键点击的参数结构，调用clickTree方法更新节点信息
      // 创建一个模拟的e对象，包含与clickTree方法兼容的结构
      const mockClickEvent = {
        selected: false, // 右键点击时不需要选中状态切换
        node: e.node, // 使用相同的节点对象
      };

      // 调用clickTree方法处理节点信息更新
      this.clickTree([treeKey], mockClickEvent);

      // 若是分类，执行一个查询分支新建状态的函数
      if (e.node.dataRef.type === "classify") {
        await this.getCreateBranchStatus(treeKey);
      }
    },
    async getCreateBranchStatus(treeKey) {
      // 查询分支新建状态
      // 随机返回执行中或未执行
      this.createbranchStatus = "未执行";
      // this.createbranchStatus = await randomMsg(["执行中", "未执行"], [0.5]);
      // console.log("分支新建状态", this.createbranchStatus);

      // getBranchStatus({ treeKey: treeKey })
      //   .then((res) => {
      //     this.createbranchStatus = res.data;
      //   })
      //   .catch((err) => {
      //     console.log("查询分支新建状态失败", err);
      //   });
    },
    // 查询产品 用户是否可编辑
    getproductIsEdit(productId) {
      // 统一返回 Promise
      return new Promise((resolve, reject) => {
        try {
          if (this.$store.state.admin) {
            this.productEdit = true;
            resolve(true);
          } else {
            let params = { productId: productId };
            getUserProduct(params)
              .then((res) => {
                if (res.data && res.data.write === 1) {
                  this.productEdit = true;
                  resolve(true);
                } else {
                  this.productEdit = false;
                  resolve(false);
                }
              })
              .catch((err) => {
                // console.log("请联系管理员增加该产品的编辑权限。");
                reject(err);
              });
          }
        } catch (error) {
          // 捕获同步代码中的错误
          reject(error);
        }
      });
    },
    // 分类拖拽
    onDragEnter(info) {
      // console.log("onDragEnter:",info)
    },
    onDrop(info) {
      // console.log("onDrop",info)
      // 拖拽的节点
      let dragNode = info.dragNode.dataRef;
      // 目标节点
      let node = info.node.dataRef;
      if (info.dropToGap) {
        // 和目标分类同级
        dragNode.parentId = node.parentId;
      } else {
        // 是目标分类的子集
        dragNode.parentId = node.key;
      }
      if (node.type === "common") {
        // 公共库分类无子类
        return;
      }

      // updateEntryClassfy(dragNode).then((res) => {
      //     message.success('已保存！')
      //     this.getClassTree()
      // }).catch((err) => {
      //     message.error("操作失败！")
      // })
    },
    viewEntry(versionId) {
      this.activeKey = "1";
      this.$refs.productEntry.getProductVersion();
      this.$refs.productEntry.changeVersion(versionId);
    },
    tabChange(activeKey) {
      if (activeKey === "2") {
        // this.$refs.productVersionRef.getProductVersion()
      } else {
        this.$refs.productEntry.setTableHeight();
      }
    },
    // treeBox展开与关闭
    openOrCloseTree() {
      this.treeBoxOpen = !this.treeBoxOpen;

      if (this.treeBoxOpen) {
        this.boxFlex = "240px";
      } else {
        this.boxFlex = "10px";
      }
    },
  },
};
</script>

<style scoped lang="less">
.box {
  width: 100%;
  height: 100%;
  padding: 16px;
  // border: 1px solid red;

  .ant-row {
    height: 100%;
  }

  .treeBox {
    padding: 16px;
    border: 1px solid #dcdcdc;
    position: relative;

    .productTree {
      width: calc(100% - 32px);
      height: calc(100% - 70px);
      overflow: auto;
      position: absolute;
      bottom: 16px;
      // border: 1px solid red;
    }
  }

  .dataBox {
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

    .entryBox {
      width: 100%;
      height: 100%;
      // border: 1px solid red;
    }

    .floatBtn {
      width: 16px;
      height: 32px;
      // border: 1px solid #DCDCDC;
      position: absolute;
      left: 0px;
      top: calc(50% - 16px);
      color: #d1d1c8;
      // box-shadow: 1px 6px 12px 0px rgba(241, 189, 46, 0.20), -1px 0px 8px 0px rgba(241, 189, 46, 0.20);

      .anticon {
        margin-top: 8px;
      }
    }
  }
}
:deep(.ant-tree) {
  width: 100%;
}
.ant-tabs {
  height: 100%;
}
:deep(.ant-tabs-content) {
  height: 100%;
}
:deep(.ant-tabs-nav-wrap) {
  background-color: rgba(250, 250, 250, 1);
}
:deep(.ant-tabs-tabpane) {
  height: 100%;
}
:deep(.ant-tree .ant-tree-node-content-wrapper.ant-tree-node-selected) {
  background-color: #eef7ff;
}
:deep(.ant-tree-node-selected .ant-dropdown-trigger) {
  color: #369fff;
}
// 树结构title超长时 滚动
:deep(.ant-tree-title) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}
</style>