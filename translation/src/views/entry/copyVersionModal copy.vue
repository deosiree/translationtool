<template>
  <Modal ref="copyBox" :modalWidth="modalWidth" :modalHeight="modalHeight" :visible="visible" :copyClassfyID="copyClassfyID" :modalTitle="modalTitle"
    @handleClose="handleClose" @handleOK="handleOK">
    <div class="content">
      <div class="horizontal-layout">
        <!-- 左侧状态树 -->
        <div class="tree-section">
          <div class="productTree">
            <a-tree show-icon v-model:expandedKeys="expandedKeys" :defaultExpandAll="true" :selectedKeys="selectedTreeKeys" :tree-data="treeData"
              @select="clickTree" draggable block-node>
              <template #title="{ title, type}">
                <a-dropdown :trigger="['contextmenu']">
                  <span v-if="type === 'common'" style="color: #001fb8">{{ title }}</span>
                  <span v-else-if="type === 'classify'" style="color: #7d7d7d">{{ title }}</span>
                  <span v-else-if="type === 'product'" style="color: #5ba584">{{ title }}</span>
                  <span v-else-if="type === 'module'" style="color: #a55b7c">{{ title }}</span>
                  <span v-else>{{ title }}</span>
                </a-dropdown>
              </template>
            </a-tree>
            <span v-if="treeData.length === 0" style="color: rgba(0, 0, 0, 0.40);margin-left: 40%;">暂无数据</span>
          </div>
        </div>
        <!-- 右侧表单控件 -->
        <div class="form-section">
          <div name="originalClassify" :class="original.type=='原产品'?'productNode':'classifyNode'">
            <div>{{original.type}}：</div>
            <div style="margin-left: 50px;">
              <a-button type="link" size="middle" @click="changed" :class="selected.dest=='原始'?'selected':''">{{ original.title }}</a-button>
              <div>{{ original.path }}</div>
            </div>
          </div>
          <div name="destinationClassify" :class="destination.type=='目标产品'?'productNode':'classifyNode'">
            <div style="margin-top:100px;">{{destination.type}}：</div>
            <div style="margin-left: 50px;">
              <a-button type="link" size="middle" @click="changed" :class="selected.dest=='目标'?'selected':''">{{ destination.title }}</a-button>
              <div>{{ destination.path }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <template v-slot:leftBottomBtn>
      <a-button @click="copyVersion" type="primary" :loading="loading">拷贝</a-button>
    </template>
  </Modal>
</template>
<script>
import Modal from "@/components/modal/index.vue";
import { message, notification } from "ant-design-vue";
import { getClassTree, copyEntryClassify } from "@/http/api/entryManage";
import { getPathByKey, setModalAriaHidden } from "@/utils/commonUtils";
import commonParam from "@/utils/commonParam";
// import backgroundTaskManager from "@/utils/backgroundTask";
import { cloneDeep } from "lodash-es";
import { v4 as uuidv4 } from "uuid";
export default {
  components: {
    Modal,
  },
  emits: ["copyClose"],
  props: {
    // 传递来的数据放这儿，不能再在data中定义了
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
    },
    copyClassfyID: {
      type: String,
    },
    originalNode: {
      type: Object,
    },
  },
  data() {
    return {
      labelCol: { style: { width: "80px" } },
      modalWidth: "400px",
      modalHeight: "1000px",
      loading: false,
      selected: {
        title: "选中分类路径",
        node: "",
        type: "选中分类",
        path: "",
        pathList: [],
        dest: "目标",
      },
      original: {
        title: "原始分类路径",
        node: "",
        type: "原始分类",
        path: "",
        pathList: [],
      },
      destination: {
        title: "目标分类路径",
        node: "",
        type: "目标分类",
        path: "",
        pathList: [],
      },
      user: {},
      keyWords: "",
      treeData: [],
      expandedKeys: [],
      selectedTreeKeys: [],
      prevPath: null,
    };
  },
  mounted() {
    this.$nextTick(() => {
      this.user = this.$store.state.user;
    });
  },
  watch: {
    visible: {
      async handler(newVal, oldVal) {
        if (newVal) {
          await this.getClassTree();
          // 初始化原产品路径
          // console.log("originalNode", this.originalNode);
          // switch (this.originalNode.type) {
          //   case "product":
          //     this.original.type = "原产品";
          //     break;
          //   case "classify":
          //     this.original.type = "原分类";
          //     break;
          // }
          this.original.node = this.originalNode;
          this.original.title = this.originalNode.title;
          this.original.pathList = getPathByKey(
            this.treeData,
            this.originalNode.key
          );
          this.original.path = this.original.pathList.join(" / ");

          // 检查是否有与当前原始节点相关的运行中任务
          this.checkRunningTasks();

          setModalAriaHidden(this, document);
        }
      },
      immediate: true,
    },
  },
  methods: {
    // 新增：检查是否有运行中的任务
    checkRunningTasks() {
      // const taskPaths = {
      //   og: this.original.path,
      //   dest: this.destination.path,
      // };
      // // 如果新路径与之前路径完全不一致，则清理相关的回调
      // if (
      //   this.prevPath &&
      //   this.prevPath.og != taskPaths.og &&
      //   this.prevPath.dest != taskPaths.dest
      // ) {
      //   // 找出与之前节点相关的所有任务ID
      //   const oldTasks = backgroundTaskManager
      //     .getTasks()
      //     .filter(
      //       (task) =>
      //         (task.og && task.og == this.prevPath.og) ||
      //         (task.dest && task.dest == this.prevPath.dest)
      //     );
      //   if (oldTasks.length > 0) {
      //     for(const task of oldTasks){
      //       backgroundTaskManager.removeTaskCallback(task.id);
      //     }
      //     if (
      //       this.prevPath.og == taskPaths.og &&
      //       this.prevPath.dest != taskPaths.dest
      //     ) {
      //       notification.success({
      //         message: "执行中",
      //         description: `原始路径：[${taskPaths.og}]正在执行中`,
      //         duration: 300,
      //         dangerouslyUseHTMLString: true,
      //       });
      //     } else if (
      //       this.prevPath.og != taskPaths.og &&
      //       this.prevPath.dest == taskPaths.dest
      //     ) {
      //       notification.success({
      //         message: "执行中",
      //         description: `目标路径：[${taskPaths.dest}]正在执行中`,
      //         duration: 300,
      //         dangerouslyUseHTMLString: true,
      //       });
      //     } else {
      //       // 原始路径、目标路径均不同，所以当前按钮loading=false，需要把之前的定时回调清除
      //       // 逐个删除相关任务的回调
      //       oldTasks.forEach((task) => {
      //         backgroundTaskManager.removeTaskCallback(task.id);
      //       });
      //       // 确保loading状态为false
      //       this.loading = false;
      //       console.log(
      //         `已清理原节点 ${taskPaths.og} 和目标节点 ${taskPaths.dest} 的 ${oldTasks.length} 个任务回调`
      //       );
      //     }
      //   }
      // }

      // // 更新上次路径
      // this.prevPath = cloneDeep(taskPaths);

      // // 检查当前节点的运行中任务
      // const runningTasks = backgroundTaskManager
      //   .getTasks()
      //   .filter(
      //     (task) =>
      //       task.status === "running" &&
      //       ((task.og && task.og == this.original.path) ||
      //         (task.dest && task.dest == this.destination.path))
      //   );

      // console.log(
      //   "previousOriginalKey",
      //   this.previousOriginalKey,
      //   this.loading,
      //   runningTasks
      // );

      // // 如果有与当前原始节点相关的运行中任务，设置loading为true
      // if (runningTasks.length > 0) {
      //   this.loading = true;
      //   // 为每个运行中的任务设置回调
      //   runningTasks.forEach((task) => {
      //     console.log(
      //       `已查到运行中节点 ${this.original.node.key} 的 ${runningTasks.length} 个任务回调`
      //     );
      //     backgroundTaskManager.setTaskCallback(task.id, (result) => {
      //       if (result.status === "completed") {
      //         this.loading = false;
      //         message.success("拷贝成功");
      //       } else if (result.status === "failed") {
      //         this.loading = false;
      //         message.error(result.error || "拷贝失败");
      //       }
      //     });
      //   });
      // } else {
      //   this.loading = false;
      // }
    },
    copyVersion() {
      if (this.destination.path === "") {
        message.error("请选择目标分类");
        return;
      }
      if (this.original.path === "") {
        message.error("请选择原始分类");
        return;
      }
      if (this.destination.pathList[0] != this.original.pathList[0]) {
        message.error("目标分类和原始分类必须在同一子公司");
        return;
      }
      if (this.destination.pathList[1] != this.original.pathList[1]) {
        message.error("目标分类和原始分类必须在同一部门");
        return;
      }
      if (this.destination.path == this.original.path) {
        message.error("目标分类和原始分类不能完全一样");
        return;
      }
      // 生成任务ID
      const taskId = uuidv4();
      const params = {
        original: this.original.node.key,
        destination: this.destination.node.key,
      };
      this.loading = true;

      // // 添加任务到任务管理器
      // backgroundTaskManager.addTask(
      //   taskId,
      //   this.original.path,
      //   this.destination.path
      // );
      // console.log(
      //   `已执行原路径 ${this.original.path} 到目标路径 ${this.destination.path} 的任务回调${taskId}`
      // );

      // // 设置任务回调
      // backgroundTaskManager.setTaskCallback(taskId, (result) => {
      //   console.log("任务回调", result);
      //   if (result.status === "completed") {
      //     this.loading = false;
      //     notification.success({
      //       message: "拷贝成功",
      //       description: `原始路径：[${result.original}]
      //                     --------------------------->
      //                     目标路径：[${result.destination}]`,
      //       duration: 300,
      //       dangerouslyUseHTMLString: true,
      //     });
      //   } else if (result.status === "failed") {
      //     this.loading = false;
      //     message.error(
      //       result.error ||
      //         `拷贝失败!(原始分类：${result.original}，目标分类：${result.destination})`
      //     );
      //   }
      // });

      // // 调用API（即使页面关闭，任务仍会在后台运行）
      // copyEntryClassify(params).then((res) => {
      //   console.log("拷贝成功", res);
      // }).catch((err) => {
      //   console.log("拷贝失败", err);
      //   message.error(err.message);
      // });

      // console.log("原始分类", this.original.node.key, this.original.node);
      // console.log("目标分类", this.destination.node.key, this.destination.node);
      // console.log("拷贝成功", params);
      setModalAriaHidden(this, document);
    },
    changed() {
      const dest = this.selected.dest;
      switch (dest) {
        case "目标":
          this.selected.dest = "原始";
          break;
        case "原始":
          this.selected.dest = "目标";
          break;
      }
      setModalAriaHidden(this, document);
    },
    handleClose() {
      this.$emit("copyClose");
      setModalAriaHidden(this, document);
    },
    handleOK() {
      this.$emit("copyClose");
      setModalAriaHidden(this, document);
    },
    // 目录树点击事件
    clickTree(selectedKeys, e) {
      // console.log("this.treeData", this.treeData);
      // console.log("触发点击事件", selectedKeys);
      if (e.selected) {
        this.selected.key = selectedKeys;
      } else {
        this.selected.key = [e.node.key];
      }
      // 为currentClickProduct赋值，根据当前目录树传给子组件
      let node = e.node.dataRef;
      // console.log(node)
      if (node.type === "classify") {
        this.selected.node = node;
        this.selected.title = node.title;
        this.selected.pathList = getPathByKey(
          this.treeData,
          this.selected.node.key
        );
        this.selected.path = this.selected.pathList.join(" / ");
      } else {
        message.error("请选择分类");
      }

      switch (this.selected.dest) {
        case "目标":
          this.destination = cloneDeep(this.selected);
          this.destination.type = "目标分类";
          break;
        case "原始":
          this.original = cloneDeep(this.selected);
          this.original.type = "原始分类";
          // 检查是否有与当前原始节点相关的运行中任务
          this.checkRunningTasks();
          break;
      }

      // if (node.type === "product" || node.type === "classify") {
      //   this.destination.node = node;
      //   this.destination.title = node.title;
      //   switch (node.type) {
      //     case "product":
      //       this.destination.type = "目标产品";
      //       break;
      //     case "classify":
      //       this.destination.type = "目标分类";
      //       break;
      //   }
      //   this.destination.pathList = this.getPathByKey(
      //     this.treeData,
      //     this.destination.node.key
      //   );
      //   this.destination.path = this.destination.pathList.join(" / ");
      //   console.log("目标路径", this.destination.path);
      // } else {
      //   message.error("请选择产品或分类");
      // }
      setModalAriaHidden(this, document);
    },
    // 查询分类树
    async getClassTree() {
      let params = {
        // department: this.$store.state.admin ? "" : this.user.department,// 分类树改结构了，所以返回有误，且分类树的权限由管理员变为了超管，由前端来判断分类树的展示
        department: "",
        className: this.keyWords,
      };
      try {
        // 使用Promise包装API调用，确保异步执行顺序
        const res = await new Promise((resolve, reject) => {
          getClassTree(params).then(resolve).catch(reject);
        });
        this.treeData = res.data.list;
        // 等待权限过滤完成
        await this.getClassTreeByUsed();
        return true;
      } catch (err) {
        message.error(err.message);
        return false;
      }
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
      this.treeData = treeDataScoped;
      // console.log("过滤后的分类树", this.treeData);
    },
  },
};
</script>
<style scoped>
.selected {
  color: #77b3c9;
}
.classifyNode {
  color: #7d7d7d;
}
.productNode {
  color: #5ba584;
}
:deep(.ant-form-item-label) {
  width: 85px;
}
.entries {
  font-size: 12px;
  padding: 4px 8px;
  background-color: #eefffb;
  border: 1px solid #beede5;
  border-radius: 4px;
  color: #77b3c9;
  margin-bottom: 2px;
}
.content {
  width: 100%;
  height: 100%;
  padding: 10px;
  background-color: #f3f3f3;
}
.horizontal-layout {
  display: flex;
  height: 100%;
  gap: 10px;
}
.tree-section {
  flex: 0 0 300px;
  height: 100%;
  background: white;
  border-radius: 4px;
  overflow-y: auto;
  padding: 10px;
}
.form-section {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  /* gap: 10px; */
}
.productTree {
  height: 800px;
  overflow-y: auto;
}
</style>