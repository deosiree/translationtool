<template>
  <div ref="box" class="box">
    <a-row type="flex">
      <a-col :flex="boxFlex" class="treeBox" ref="treeBox">
        <a-input
          placeholder="关键字搜索"
          v-if="treeBoxOpen"
          v-model:value="keyWords"
          @pressEnter="getClassTree"
        >
          <template #suffix>
            <SearchOutlined style="color: #dcdcdc" />
          </template>
        </a-input>
        <div
          class="productTree"
          @contextmenu.prevent="handleTreeAreaContextMenu"
        >
          <a-tree
            v-if="treeBoxOpen"
            show-icon
            draggable
            block-node
            v-model:expandedKeys="expandedKeys"
            :defaultExpandAll="true"
            :selectedKeys="selectedTreeKeys"
            :tree-data="treeData"
            @select="clickTree"
            @rightClick="rightClickTree"
            @dragenter="onDragEnter"
            @drop="onDrop"
            @expand="onExpandTree"
          >
            <template #title="{ title, type }">
              <span v-if="type === 'common'" style="color: #001fb8">{{
                title
              }}</span>
              <span v-else-if="type === 'classify'" style="color: #7d7d7d">{{
                title
              }}</span>
              <span v-else-if="type === 'product'" style="color: #5ba584">{{
                title
              }}</span>
              <span v-else-if="type === 'module'" style="color: #a55b7c">{{
                title
              }}</span>
              <span v-else>{{ title }}</span>
            </template>
          </a-tree>
          <span
            v-if="treeBoxOpen && treeData.length === 0"
            style="color: rgba(0, 0, 0, 0.4); margin-left: 40%"
            >暂无数据</span
          >
        </div>
      </a-col>
      <a-col flex="auto" class="dataBox">
        <div class="entryBox" v-if="isProduct">
          <a-tabs v-model:activeKey="activeKey" type="card" @change="tabChange">
            <a-tab-pane key="1" tab="词条详情">
              <ProductEntry
                ref="productEntry"
                :boxHeight="boxHeight"
                :currentProduct="currentClickProduct"
                :productEdit="productEdit"
              />
            </a-tab-pane>
            <a-tab-pane key="2" tab="产品版本">
              <ProductVersion
                ref="productVersionRef"
                :boxHeight="boxHeight"
                :currentProduct="currentClickProduct"
                :productEdit="productEdit"
                @viewEntry="viewEntry"
              />
            </a-tab-pane>
          </a-tabs>
        </div>
        <div class="entryBox" v-else>
          <CommonEntry
            modalTitle="更新详情"
            :boxHeight="boxHeight"
            :currentCommon="currentClickProduct"
          />
        </div>
        <div class="floatBtn">
          <left-outlined
            v-if="treeBoxOpen"
            title="收起树"
            @click="openOrCloseTree"
          />
          <right-outlined
            v-if="!treeBoxOpen"
            title="展开树"
            @click="openOrCloseTree"
          />
        </div>
      </a-col>
    </a-row>
  </div>
  <!-- 统一树右键菜单：基于 rightClickTree 和 treeContextmenu 控制 -->
  <div
    v-if="treeContextmenu.visible && treeContextmenu.node && $store.state.admin"
    :style="{
      position: 'fixed',
      left: treeContextmenu.clientX + 'px',
      top: treeContextmenu.clientY + 'px',
      zIndex: 3000,
      width: 0,
      height: 0,
    }"
    @contextmenu.prevent
    @click.stop
  >
    <a-dropdown :visible="treeContextmenu.visible">
      <template #overlay>
        <a-menu>
          <a-menu-item
            v-if="$currentDepartment && $currentDepartment.ops.has('needIP')"
            :disabled="updateTaskStatusMap[treeContextmenu.node.key] === '1'"
            :loading="updateTaskStatusMap[treeContextmenu.node.key] === '1'"
            @click="
              hideTreeContextmenu();
              handleUpdateClick(treeContextmenu.node.key);
            "
          >
            {{
              updateTaskStatusMap[treeContextmenu.node.key] === "1"
                ? "正在执行中"
                : "更新"
            }}
          </a-menu-item>
          <a-menu-item
            v-if="
              treeContextmenu.node.type != 'common' &&
              treeContextmenu.node.type != 'product' &&
              treeContextmenu.node.type != 'module'
            "
            @click="
              hideTreeContextmenu();
              addClassify(treeContextmenu.node.key, 'classify');
            "
          >
            添加分类
          </a-menu-item>
          <a-menu-item
            v-if="
              treeContextmenu.node.type != 'common' &&
              treeContextmenu.node.type != 'product' &&
              treeContextmenu.node.type != 'module'
            "
            @click="
              hideTreeContextmenu();
              addClassify(treeContextmenu.node.key, 'product');
            "
          >
            添加产品
          </a-menu-item>
          <a-menu-item
            v-if="treeContextmenu.node.type === 'product'"
            @click="
              hideTreeContextmenu();
              productAuthority(treeContextmenu.node.key);
            "
          >
            权限设置
          </a-menu-item>
          <a-menu-item
            v-if="treeContextmenu.node.type === 'product'"
            @click="
              hideTreeContextmenu();
              addClassify(treeContextmenu.node.key, 'module');
            "
          >
            添加模块
          </a-menu-item>
          <a-menu-item
            v-if="
              treeContextmenu.node.type != 'department' &&
              treeContextmenu.node.type != 'common'
            "
            @click="
              hideTreeContextmenu();
              editClassify(
                treeContextmenu.node.key,
                treeContextmenu.node.title,
                treeContextmenu.node.type,
                treeContextmenu.node.maxByte,
                treeContextmenu.node.foreignMaxByte,
                treeContextmenu.node.codeBranch
              );
            "
          >
            编辑
          </a-menu-item>
          <a-menu-item
            v-if="
              treeContextmenu.node.type != 'department' &&
              treeContextmenu.node.type != 'common'
            "
          >
            <a-popconfirm
              title="确定要删除吗?"
              ok-text="是"
              cancel-text="否"
              @confirm="
                deleteClassify(
                  treeContextmenu.node.key,
                  treeContextmenu.node.type
                )
              "
            >
              删除
            </a-popconfirm>
          </a-menu-item>
          <a-menu-item
            v-if="
              $currentDepartment &&
              $currentDepartment.ops.has('needBranch') &&
              treeContextmenu.node.type == 'classify'
            "
            ref="createBranchMenu"
            :disabled="createbranchStatus == '执行中'"
            @click="
              hideTreeContextmenu();
              createBranch(treeContextmenu.node.key);
            "
          >
            分支新建{{ createbranchStatus == "执行中" ? "(执行中)" : "" }}
          </a-menu-item>
          <a-menu-item
            v-if="
              $currentDepartment && $currentDepartment.ops.has('needBranch')
            "
            ref="entrySourceMenu"
            @click="
              hideTreeContextmenu();
              entrySourceOpen(treeContextmenu.node.key);
            "
          >
            来源汇总{{ createbranchStatus == "执行中" ? "(执行中)" : "" }}
          </a-menu-item>
        </a-menu>
      </template>
      <!-- 触发元素占位（不实际可见） -->
      <span />
    </a-dropdown>
  </div>
  <!-- <RedundantModal ref="redundantModal" :visible="redundantVisible" :modalTitle="classifyModalTitle" :redundantClassfyID="redundantClassfyID"
    @redundantClose="redundantClose" style="width:700px;" /> -->
  <UpdateModal
    ref="updateModal"
    style="width: 700px"
    :visible="updateVisible"
    :modalTitle="classifyModalTitle"
    :updateClassfyID="updateClassfyID"
    :taskStatus="updateTaskStatusMap[updateClassfyID]"
    @updateClose="updateClose"
    @taskCompleted="handleOpenModal"
  />
  <CreateBranchModal
    ref="createBranchModal"
    style="width: 90%"
    :treeNode="currentClickProduct"
    :visible="createBranchVisible"
    :modalTitle="classifyModalTitle"
    :createBranchClassfyID="createBranchClassfyID"
    @createBranchClose="createBranchClose"
  />
  <EntrySourceModal
    ref="entrySourceModal"
    style="width: 100%"
    :visible="entrySourceVisible"
    :modalTitle="classifyModalTitle"
    :currentClass="currentClickProduct"
    @handleClose="entrySourceClose"
  />
  <ClassifyModal
    ref="classifyModal"
    :visible="classifyVisible"
    :modalTitle="classifyModalTitle"
    :currentClass="currentClass"
    :treeNode="currentClickProduct"
    @classifyClose="classifyClose"
  />
  <ProductAuthorityModal
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
} from "@ant-design/icons-vue";
import ProductEntry from "@/views/entry/productEntry.vue";
import ProductVersion from "@/views/entry/productVersion.vue";
import CommonEntry from "@/views/entry/commonEntry.vue";
import ClassifyModal from "@/views/entry/classifyModal.vue";
import ProductAuthorityModal from "@/views/entry/productAuthorityModal.vue";
import UpdateModal from "@/views/entry/updateModal.vue";
import RedundantModal from "@/views/entry/redundantModal.vue";
import CreateBranchModal from "@/views/entry/createBranchModal.vue";
import EntrySourceModal from "@/components/Button/codeBranch/getEntrySrcModal.vue";
import { cloneDeep, iteratee } from "lodash-es";
import { getClassTree, deleteEntryClassfy } from "@/http/api/entryManage";
import { deleteProduct, getUserProduct } from "@/http/api/product";
import {
  getLangDirImportTaskState,
  getEntrysourceListByClassfyTaskState,
} from "@/http/api/backendInfo";
import { getI18nAdress } from "@/http/api/workbench";
import { message, notification, Modal as AntModal } from "ant-design-vue";
import { setModalAriaHidden } from "@/utils/domUtils";
import { randomMsg } from "@/utils/testUtils";
import commonParam, { entryParams } from "@/constants/commonParam";
import { getCachedI18nUrl } from "@/utils/dataUtils";
import { handleTaskFailureStatusNotification } from "@/utils/notificationUtils";
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
    EntrySourceModal,
  },
  data() {
    return {
      name: "entry",
      boxFlex: "240px",
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
      entrySourceVisible: false,
      entrySourceClassfyID: "", // 传参给子组件，让子组件调用http请求
      createbranchStatus: "未执行", // 查询分支新建状态
      classifyModalTitle: "",
      currentClass: {},
      currentClickProduct: {},
      authorityVisible: false,
      authorityProductId: "",
      productEdit: false,
      treeBoxOpen: true,
      treeHeight: 0,
      updateTaskStatusMap: {}, // 以classifyID为键存储更新任务状态
      treeContextmenu: {
        visible: false,
        node: null,
        clientX: 0,
        clientY: 0,
      },
      // 是否启用本地 Mock 分类树，方便在无法访问内网时调试交互
      useMockClassTree: false,
    };
  },
  mounted() {
    let _this = this;
    this.$nextTick(() => {
      this.user = this.$store.state.user;
      this.init();
      _this.boxHeight = _this.$refs.box.offsetHeight;
      /** 控制table的高度 */
      window.onresize = function () {
        _this.boxHeight = _this.$refs.box.offsetHeight;
      };
    });

    // 全局点击关闭右键菜单
    document.addEventListener("click", this.hideTreeContextmenu);
  },
  unmounted() {
    //注销window.onresize事件
    window.onresize = null;

    document.removeEventListener("click", this.hideTreeContextmenu);
  },
  methods: {
    // ==================== 生命周期和初始化相关 ====================
    init() {
      this.getClassTree();
      // this.getTreeHeight()
    },
    // ==================== 树结构相关 ====================
    handleTreeAreaContextMenu(e) {
      // 空白区域右键：仅阻止浏览器默认菜单，不展示自定义菜单
    },
    // 在methods中添加onExpandTree方法
    onExpandTree(expandedKeys, { expanded, node }) {
      // 展开状态树时，如果是分类节点，执行查询分支新建状态的函数
      if (expanded && node.dataRef.type === "classify") {
        this.getCreateBranchStatus(node.dataRef.key);
      }
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

      // 开发调试时可通过开关走本地 Mock 分类树
      if (this.useMockClassTree) {
        params.__mock = true;
      }

      getClassTree(params)
        .then((res) => {
          this.treeData = res.data.list || [];
          this.getClassTreeByUsed();
        })
        .catch((err) => {
          message.error(err.message);
        });
    },
    // 根据用户权限限制状态树的展示
    async getClassTreeByUsed() {
      let treeScoped = ["公共库"]; // 状态树可展示的字段
      if (this.user?.roleName?.includes("超级管理员"))
        treeScoped = commonParam.treeScoped.map((item) => item.title);
      else {
        if (this.user?.department) {
          treeScoped.push(this.user.department);
        }
      }
      let treeData = cloneDeep(this.treeData);
      let treeDataScoped = [];
      // 遍历树数据，根据部门名过滤
      treeDataScoped = await (treeData || []).filter((company) => {
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
    // ==================== 模态框控制 ====================
    classifyClose(closeFlag) {
      this.classifyVisible = false;
      if (!closeFlag) {
        // 如果只是close的话，不用重新获取树和刷新页面
        this.getClassTree();
        // console.log("close",this.currentClickProduct)
        // this.$refs.productEntry.refresh(this.currentClickProduct);
      } else console.log("没进close，不用重新获取树和刷新页面");
    },
    updateClose(statusInfo) {
      // 接收子组件传递的状态信息，存储到状态Map中
      if (statusInfo && statusInfo.classifyID) {
        const { status, classifyID } = statusInfo;
        // 存储或更新该分类的任务状态
        this.updateTaskStatusMap[classifyID] = status;
      }
      this.updateVisible = false;
    },
    // 处理打开模态框事件（后台任务完成时触发）
    // 后台轮询任务执行成功后，由子组件触发该事件：
    // - 自动打开更新模态框
    // - 子组件在弹窗打开时会根据任务状态=1自动拉取结果
    handleOpenModal({ classifyID, i18nUrl, status }) {
      this.openUpdateModal(classifyID, status);
    },
    // ==================== 模态框控制 ====================
    // 打开更新模态框的辅助方法（基于任务状态）
    openUpdateModal(treeKey, status) {
      this.classifyModalTitle = "更新详情";
      this.updateClassfyID = treeKey;
      // 如果未传入status参数，从状态Map中获取
      if (!status) {
        status = this.updateTaskStatusMap[treeKey] || null;
      }
      // 如果获取到状态，更新到Map中（确保状态同步）
      if (status !== null && status !== undefined) {
        this.updateTaskStatusMap[treeKey] = status;
      }
      this.updateVisible = true;
      setModalAriaHidden(this, document);
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
    entrySourceClose() {
      this.entrySourceVisible = false;
    },
    // ==================== 更新相关 ====================
    // 更新入口：只负责打开弹窗，状态查询/轮询/结果加载由子组件统一处理
    async handleUpdateClick(treeKey) {
      // 从状态Map中获取该分类的状态
      const status = this.updateTaskStatusMap[treeKey];
      // 如果状态为"1"（执行中），禁止打开模态框
      if (status === "1") {
        return;
      }
      this.openUpdateModal(treeKey, status);
    },
    // 查询更新任务状态
    async checkUpdateTaskState(classfyID, i18nUrl) {
      const res = await getEntrysourceListByClassfyTaskState({
        classifyID: classfyID,
        i18nUrl: i18nUrl,
      });
      return res.data.state;
    },
    // 处理不同状态的逻辑
    // - 右键查询状态：仅更新按钮显示（silent=true），不打开弹窗
    // - 点击"更新"：才打开弹窗（silent=false）
    handleUpdateTaskStatus(status, treeKey, i18nUrl, { silent = false } = {}) {
      // 将状态存储到状态Map中
      this.updateTaskStatusMap[treeKey] = status;

      if (status === "0" || status === "2") {
        // 状态0 (未执行) 或状态2 (执行完成)：打开弹窗
        if (!silent) {
          this.openUpdateModal(treeKey, status);
        }
      } else if (status === "1") {
        // 状态1 (执行中)：灰禁按钮，显示"正在执行中"，显示loading
        // 按钮已经在模板中通过disabled和loading控制
        // 禁止打开模态框（silent模式除外）
        if (!silent) {
          // 状态为"1"时，不打开模态框
          return;
        }
      } else {
        // 其他状态：右键时不打扰用户；点击更新时再提示
        if (!silent) {
          handleTaskFailureStatusNotification(status);
        }
      }
    },
    // ==================== 业务操作相关 ====================
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
    // 来源汇总
    entrySourceOpen(treeKey) {
      this.classifyModalTitle = "来源汇总";
      this.entrySourceClassfyID = treeKey; // treeKey就是classfyID  有些是数字 有些是uuid
      this.entrySourceVisible = true; // 显示弹窗
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
      console.log("删除分类或产品", treeKey, type);

      // 本地 Mock 模式：只在前端内存里删除节点，不调真实接口
      if (this.useMockClassTree) {
        const removeNode = (nodes, key) => {
          if (!Array.isArray(nodes)) return false;
          for (let i = 0; i < nodes.length; i++) {
            const node = nodes[i];
            if (node.key === key) {
              nodes.splice(i, 1);
              return true;
            }
            if (node.children && node.children.length) {
              const removed = removeNode(node.children, key);
              if (removed) return true;
            }
          }
          return false;
        };

        const cloned = cloneDeep(this.treeData);
        if (removeNode(cloned, treeKey)) {
          this.treeData = cloned;
          message.success("删除成功！（Mock）");
        } else {
          message.warning("未在 Mock 树中找到该节点");
        }
        return;
      }

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
      // console.log("node", node)
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

      // 若存在缓存 i18nUrl，右键时查询更新任务状态（无缓存则跳过）
      if (
        this.$store.state.admin &&
        this.$currentDepartment &&
        this.$currentDepartment.ops.has("needIP")
      ) {
        const cachedI18nUrl = getCachedI18nUrl();
        if (cachedI18nUrl) {
          try {
            const status = await this.checkUpdateTaskState(
              treeKey,
              cachedI18nUrl
            );
            // 将查询到的状态存储到状态Map中
            this.updateTaskStatusMap[treeKey] = status;
            // 右键：仅后台查询并更新按钮显示，不打开弹窗
            this.handleUpdateTaskStatus(status, treeKey, cachedI18nUrl, {
              silent: true,
            });
          } catch (err) {
            // 查询失败时，清除该分类的状态
            delete this.updateTaskStatusMap[treeKey];
          }
        }
      }

      // 若是分类，执行一个查询分支新建状态的函数
      if (e.node.dataRef.type === "classify") {
        await this.getCreateBranchStatus(treeKey);
      }

      // 更新右键菜单状态（节点右键时才打开菜单）
      this.treeContextmenu.node = e.node.dataRef;
      this.treeContextmenu.clientX = e.event.clientX;
      this.treeContextmenu.clientY = e.event.clientY;
      this.treeContextmenu.visible = true;
    },
    // 查询分支新建状态
    async getCreateBranchStatus(treeKey) {
      getLangDirImportTaskState({ id: treeKey })
        .then((res) => {
          switch (res.data.state) {
            case "0":
              this.createbranchStatus = "未执行";
              break;
            case "1":
              this.createbranchStatus = "执行中";
              break;
            case "2":
              this.createbranchStatus = "执行失败";
              message.error("‘分支新建’执行失败");
              // 分支新建失败时的“补偿删除”必须二次确认，避免误删
              if (
                Array.isArray(res.data.productIDs) &&
                res.data.productIDs.length
              ) {
                AntModal.confirm({
                  title: "检测到分支新建失败，是否删除相关产品分类？",
                  content: `将删除 ${
                    res.data.productIDs.length
                  } 个ID（建议先确认后端是否已自动补偿）：${res.data.productIDs.join(
                    ", "
                  )}`,
                  okText: "确认删除",
                  cancelText: "取消",
                  onOk: async () => {
                    try {
                      await deleteEntryClassfy(res.data.productIDs);
                      message.success("删除已提交");
                      this.getClassTree();
                    } catch (err) {
                      message.error(`删除分类失败：${err}`);
                      console.log("删除分类失败", err);
                    }
                  },
                });
              }
              break;
            default:
              this.createbranchStatus = "未知状态";
          }
        })
        .catch((err) => {
          console.log("查询分支新建状态失败", err);
        });
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
    // ==================== 拖拽相关 ====================
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
    hideTreeContextmenu(event) {
      // 兼容两种调用方式：
      // 1) 作为 document.click 的回调（带 event 参数）
      // 2) 在模板/代码中直接调用 hideTreeContextmenu()（无参数）

      // 如果是全局 document 点击触发，且点击发生在 Dropdown/Popconfirm 内部，则不关闭菜单
      if (event && event.target) {
        const target = event.target;
        const dropdown = document.querySelector(".ant-dropdown");
        const popover = document.querySelector(".ant-popover"); // Popconfirm 内部使用的类名

        if (
          (dropdown && dropdown.contains(target)) ||
          (popover && popover.contains(target))
        ) {
          return;
        }
      }

      this.treeContextmenu.visible = false;
    },
    // ==================== UI控制相关 ====================
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