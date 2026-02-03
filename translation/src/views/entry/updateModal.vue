<template>
  <!-- <a-spin :spinning="visible"> -->
  <Modal ref="updateBox" :modalWidth="modalWidth" :visible="visible" :updateClassfyID="updateClassfyID"
    :modalTitle="modalTitle" :cancelText="taskVisible ? '关闭' : '取消'" :showOk="false" @handleClose="handleClose">
    <template #leftBottomBtn>
      <!-- 查询更新：仅在任务未完成时展示，避免结果已出时再次“查询更新”清空结果 -->
      <a-button v-if="!taskVisible && currentTaskStatus !== '2'" type="primary" size="middle" :loading="loading"
        :disabled="!i18nURL" @click="handleQueryUpdate">
        查询更新
      </a-button>
      <!-- 重新查询：仅在任务完成后展示，重新查询任务状态但不刷新结果列表 -->
      <a-button v-if="!taskVisible && currentTaskStatus === '2'" type="primary" size="middle" class="yellowBtn"
        @click="handleReQuery">
        重新查询
      </a-button>
      <!-- 更新写库：仅在任务完成后展示，从原“确定”主按钮迁移到左下角，便于统一维护 -->
      <a-button v-if="!taskVisible && currentTaskStatus === '2'" type="primary" size="middle"
        @click="handleWriteUpdate">
        更新写库
      </a-button>
    </template>
    <div class="content" v-if="!taskVisible">
      <div class="table">
        <a-form ref="i18nURL" name="custom-validation">
          <a-form-item label="IP" name="ip">
            <a-select placeholder="请选择IP" allowClear v-model:value="i18nURL" :options="ipOptions"></a-select>
          </a-form-item>
        </a-form>
        <a-table ref="updateTable" bordered class="ant-table-striped" v-if="currentTaskStatus === '2'"
          :columns="columns" :dataSource="dataSource" :scroll="{ x: '100%', y: '280px' }" :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)
            " :row-selection="{
              selectedRowKeys: selectedRowKeys,
              onChange: onSelectChange,
            }" :row-key="(record) => record.sourceFile" :pagination="pagination" :loading="loading">
        </a-table>
      </div>
    </div>
    <div class="content" v-if="taskVisible">
      <a-table ref="updateTaskTable" bordered class="ant-table-striped" :columns="taskColumns" :dataSource="taskSource"
        :scroll="{ x: '100%', y: '280px' }" :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)
          " :row-key="(record) => record.sourceFile" :pagination="pagination">
      </a-table>
    </div>
  </Modal>
  <!-- </a-spin> -->
</template>
<script>
import Modal from "@/components/modal/index.vue";
import { message, notification } from "ant-design-vue";
import { getI18nAdress } from "@/http/api/workbench";
import {
  updateEntryByClassfy,
  createEntrysourceListByClassfyTask,
} from "@/http/api/entryManage";
import {
  getEntrysourceListByClassfyTaskState,
  getEntrysourceListByClassfyResult,
} from "@/http/api/backendInfo";
import { MultiRequestPolling } from "@/utils/pollingUtils";
import commonParam, { entryParams } from "@/constants/commonParam";
import { v4 as uuidv4 } from "uuid";
import { setModalAriaHidden } from "@/utils/domUtils";
import { getCachedI18nUrl, setCachedI18nUrl } from "@/utils/dataUtils";
import { handleTaskFailureStatusNotification } from "@/utils/notificationUtils";
export default {
  components: {
    Modal,
  },
  emits: ["updateClose", "taskCompleted"],
  props: {
    // 传递来的数据放这儿，不能再在data中定义了
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
    },
    updateClassfyID: {
      type: String,
    },
    taskStatus: {
      type: String,
      default: null, // 任务状态（0/1/2/3/4/5/6）
    },
  },
  data() {
    return {
      labelCol: { style: { width: "80px" } },
      modalWidth: "400px",
      i18nURL: null,
      ipOptions: [], // ip下拉选项
      columns: [
        {
          title: "词条来源",
          dataIndex: "sourceFile",
          align: "center",
          width: 400,
        },
        {
          title: "来源类型",
          dataIndex: "sourceType",
          align: "center",
          width: 100,
        },
      ],
      updateEntries: [], // 发给我的原始数据（数组）
      dataSource: [], // 展示的数据
      selectedRowKeys: [],
      taskVisible: false,// 控制更新模态框 (updateModal) 中显示的内容：true(显示任务状态)false(显示词条来源列表)
      taskColumns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: "10%",
        },
        {
          title: "更新的任务",
          dataIndex: "task",
          align: "center",
          width: "90%",
        },
      ],
      taskSource: [],
      loading: false,
      pagination: {
        showSizeChanger: true,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      // 新流程相关
      currentTaskStatus: null, // 当前任务状态（0/1/2/3/4/5/6）
      pollingController: null, // 多请求轮询管理器
      pollingRequestIds: new Map(), // 存储每个任务的请求ID，key为 classifyID+i18nUrl
    };
  },
  mounted() {
    this.getIPs();
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.onModalOpen();
      }
    },
  },
  beforeUnmount() {
    // 清理所有轮询请求
    if (this.pollingController) {
      this.pollingController.stop();
      this.pollingController = null;
    }
    this.pollingRequestIds.clear();
  },
  methods: {
    // ==================== 生命周期和初始化相关 ====================
    init() {
      this.updateEntries = [];
      this.dataSource = [];
      this.i18nURL = null;
      this.taskSource = [];
      this.taskVisible = false;
      this.currentTaskStatus = null;
      this.loading = false; // 重置loading状态
      // 不清理轮询请求，让任务在后台继续运行，直到状态变为1
    },
    async onModalOpen() {
      // 弹窗打开时，根据当前任务状态重置loading
      // 避免多任务更新时，第一个任务的loading状态影响其他任务
      this.currentTaskStatus = this.taskStatus;
      
      // 根据当前任务状态设置loading
      // 只有状态为"1"（执行中）时才显示loading，其他状态都重置为false
      if (this.currentTaskStatus === "1") {
        this.loading = true;
      } else {
        this.loading = false;
      }
      
      // 优先使用缓存 i18nUrl
      const cachedI18nUrl = getCachedI18nUrl();
      if (cachedI18nUrl) {
        this.i18nURL = cachedI18nUrl;
      }

      // 若父组件已传入状态，直接使用，不再查询
      if (this.currentTaskStatus !== null && this.currentTaskStatus !== undefined) {
        // 若已知任务已完成，直接加载结果
        if (this.currentTaskStatus === "2" && this.i18nURL) {
          await this.loadTaskResultAndDisplay(this.updateClassfyID, this.i18nURL);
          return;
        }
        
        // 若任务正在执行中，启动轮询
        if (this.currentTaskStatus === "1" && this.i18nURL) {
          this.startTaskPolling();
          return;
        }
        
        // 其他状态，直接返回，不进行额外查询
        return;
      }

      // 若未传入状态，但有 i18nURL：主动查询一次状态，减少"空状态直接写库"的风险
      if (!this.currentTaskStatus && this.i18nURL) {
        try {
          const state = await this.fetchTaskState(
            this.updateClassfyID,
            this.i18nURL
          );
          this.currentTaskStatus = state;
          if (state === "2") {
            await this.loadTaskResultAndDisplay(this.updateClassfyID, this.i18nURL);
          } else if (state === "1") {
            // 如果查询到状态为"1"，启动轮询
            this.startTaskPolling();
          }
        } catch (e) {
          // 状态查询失败不阻断弹窗使用（用户仍可手动选择IP触发流程）
        }
      }
    },
    // 获取i18服务器ip
    getIPs() {
      this.ipOptions = [];
      getI18nAdress().then((res) => {
        res.data.list.forEach((item) => {
          let ip = {
            label: item.ip,
            value: item.ip,
          };
          this.ipOptions.push(ip);
        });
      });
    },
    // ==================== 数据转换相关 ====================
    // 将 entries 转换为 dataSource、selectedRows、selectedRowKeys
    transformEntriesToDataSource(entries) {
      this.updateEntries = entries;
      this.dataSource = [];
      this.selectedRowKeys = [];
      if (Array.isArray(this.updateEntries) && this.updateEntries.length !== 0) {
        this.updateEntries.forEach((item) => {
          const EntryVO = Object.values(item.sourceFileAndEntryVO);
          const sourceType = item.type;
          if (EntryVO.length != 0) {
            EntryVO.forEach((item) => {
              this.selectedRowKeys.push(item.sourceFile);
              this.dataSource.push({
                sourceFile: item.sourceFile,
                sourceType: sourceType,
              });
            });
          }
        });
      }
    },
    // ==================== UI交互控制 ====================
    handleClose() {
      // 不停止轮询控制器，让任务在后台继续运行
      // 轮询请求会在状态变为1时自动清理
      // 无论状态值是什么，都传递状态和classifyID给父组件
      this.$emit("updateClose", {
        status: this.currentTaskStatus,
        classifyID: this.updateClassfyID,
        i18nUrl: this.i18nURL,
      });
      this.init();
    },
    async handleQueryUpdate() {
      if (!this.i18nURL) {
        message.warning("请选择IP地址");
        return;
      }

      // 保存i18nUrl到localStorage
      if (this.i18nURL) {
        setCachedI18nUrl(this.i18nURL);
      }

      // 根据状态执行查询/创建任务/轮询/拉结果（不写库）
      if (this.currentTaskStatus === "0") {
        await this.handleStatus0();
        return;
      }
      else if (this.currentTaskStatus === "1") {
        this.startTaskPolling();
        message.info("任务执行中，已开始轮询，请稍候...");
      }
      else if (this.currentTaskStatus === "2") {
        await this.loadTaskResultAndDisplay(this.updateClassfyID, this.i18nURL);
        message.success("查询完成，可点击\"更新写库\"写入数据库");
        return;
      }
      else if (this.currentTaskStatus) {
        // 状态3/4/5/6：告警状态
        handleTaskFailureStatusNotification(this.currentTaskStatus);
        return;
      }
      else {
        // currentTaskStatus 为 null/undefined，说明没有任务状态，应该创建新任务
        await this.handleStatus0();
      }
    },
    async handleWriteUpdate() {
      // 必须先完成“查询更新”，再允许写库
      if (!this.i18nURL) {
        message.warning("请选择IP地址");
        return;
      }
      if (this.currentTaskStatus !== "2") {
        message.warning("请先点击“查询更新”，待任务完成后再进行写库");
        return;
      }

      this.handleWriteDB();
    },
    // 重新查询任务状态：仅更新 currentTaskStatus，不重新拉取结果，避免清空已展示的更新结果
    async handleReQuery() {
      if (!this.i18nURL) {
        message.warning("请选择IP地址");
        return;
      }

      try {
        const state = await this.fetchTaskState(
          this.updateClassfyID,
          this.i18nURL
        );
        this.currentTaskStatus = state;
        if (state === "0") {
          await this.handleStatus0();
        } else {
          notification.error({
            message: "任务状态清除失败",
            duration: 0,
          });
        }

        message.success("任务状态已重新查询");
      } catch (e) {
        message.error("任务状态查询失败");
      }
    },
    // ==================== 业务逻辑处理 ====================
    async fetchTaskState(classifyID, i18nUrl) {
      const res = await getEntrysourceListByClassfyTaskState({
        classifyID,
        i18nUrl,
      });
      // console.log("fetchTaskState查询任务状态", res.data.state);
      return res.data.state;
    },
    // 处理状态0：创建任务并启动轮询
    async handleStatus0() {
      if (!this.i18nURL) {
        message.warning("请选择IP地址");
        return;
      }

      this.loading = true;
      try {
        // console.log("开始创建更新任务")
        // 创建更新任务
        await createEntrysourceListByClassfyTask({
          classifyID: this.updateClassfyID,
          i18nUrl: this.i18nURL,
        });

        message.success("更新任务已创建，正在执行中...");

        // 启动定时器轮询
        this.startTaskPolling();
      } catch (error) {
        console.error("创建更新任务失败:", error);
        message.error("创建更新任务失败");
        this.loading = false;
      }
    },
    // 处理状态1：只加载结果并显示，不写库（在打开弹窗时调用）
    async handleStatus1() {
      if (!this.i18nURL) {
        message.warning("请选择IP地址");
        return;
      }

      // 加载任务结果并显示
      await this.loadTaskResultAndDisplay(this.updateClassfyID, this.i18nURL);
    },
    // 根据更新结果进行数据库数据更新（写库操作，更新v1是同步操作，直接执行）
    handleWriteDB() {
      if (!Array.isArray(this.updateEntries) || this.updateEntries.length === 0) {
        message.warning("暂无可更新的数据，请先获取更新结果");
        return;
      }
      // 点击确认就发送http请求，更新词条
      const data = [];
      this.updateEntries.forEach((item) => {
        const res = {
          type: "",
          sourceFileAndEntryVO: [],
        };
        let resFlag = false; // 标记是否已经添加过
        res.type = item.type;
        Object.values(item.sourceFileAndEntryVO).forEach((file) => {
          if (this.selectedRowKeys.includes(file.sourceFile)) {
            res.sourceFileAndEntryVO.push(file);
            resFlag = true;
          }
        });
        if (resFlag) data.push(res);
      });

      updateEntryByClassfy(data)
        .then((res) => {
          notification.success({
            message: "更新成功！",
            duration: 0,
          });
          this.dataSource = [];
          this.taskSource = Object.values(res.data.list).map((item, index) => ({
            index: index + 1,
            task: item,
          }));
          this.taskVisible = true; // 先弹出其他弹窗
          setModalAriaHidden(this, document);
        })
        .catch((error) => {
          notification.error({
            message: "请求失败",
            description: error?.message || "未知错误",
            duration: 0,
          });
          console.log(`请求失败: ${error}`);
        });
    },
    // ==================== 轮询相关 ====================
    // 启动定时器轮询
    startTaskPolling() {
      // 如果轮询管理器不存在，创建它
      if (!this.pollingController) {
        this.pollingController = new MultiRequestPolling({
          interval: entryParams.updateEntry.pollingInterval,
          onError: (error, requestId) => {
            console.error("轮询检查失败:", error, requestId);
            // 错误处理：移除失败的请求
            if (requestId) {
              this.pollingController.removeRequest(requestId);
              // 从map中移除
              for (const [key, id] of this.pollingRequestIds.entries()) {
                if (id === requestId) {
                  this.pollingRequestIds.delete(key);
                  break;
                }
              }
            }
          },
        });
      }

      // 生成请求的唯一标识
      const requestKey = `${this.updateClassfyID}_${this.i18nURL}`;

      // 如果该任务已经在轮询中，不重复添加
      if (this.pollingRequestIds.has(requestKey)) {
        return;
      }

      // 创建检查函数（闭包保存当前任务的参数）
      const classifyID = this.updateClassfyID;
      const i18nUrl = this.i18nURL;
      // console.log("轮询检查前")
      const checkFn = async () => {
        return await this.fetchTaskState(classifyID, i18nUrl);
      };

      // 创建状态变化回调
      const onStatusChange = (status, requestId) => {
        // console.log("onStatusChange状态变化", status);
        // 只处理当前任务的回调
        if (requestKey === `${classifyID}_${i18nUrl}`) {
          this.currentTaskStatus = status;
          if (status === "2") {
            // 状态变为2，移除该请求
            this.pollingController.removeRequest(requestId);
            this.pollingRequestIds.delete(requestKey);
            this.loading = false;

            // 通知父组件：任务已完成（由父组件决定是否打开弹窗）
            this.$emit("taskCompleted", {
              classifyID: classifyID,
              i18nUrl: i18nUrl,
              status: status,
            });

            // 若弹窗当前仍打开，则自动加载结果
            if (this.visible) {
              this.loadTaskResultAndDisplay(classifyID, i18nUrl);
            }

            message.success("任务执行完成，请打开更新弹窗查看结果并写库");
          } else if (status === "1") {
            // 继续轮询
          } else {
            // 其他状态，移除该请求并报错
            this.pollingController.removeRequest(requestId);
            this.pollingRequestIds.delete(requestKey);
            this.loading = false;
            handleTaskFailureStatusNotification(this.currentTaskStatus);
          }
        }
      };

      // 添加请求到轮询列表
      const requestId = this.pollingController.addRequest(
        checkFn,
        onStatusChange
      );
      this.pollingRequestIds.set(requestKey, requestId);
    },
    // ==================== 辅助功能 ====================
    // 获取任务结果
    async getTaskResult() {
      const res = await getEntrysourceListByClassfyResult({
        classifyID: this.updateClassfyID,
        i18nUrl: this.i18nURL,
      });
      return res.data;
    },
    // 加载任务结果并显示
    async loadTaskResultAndDisplay(classifyID, i18nUrl) {
      // console.log("加载任务结果并显示", classifyID, i18nUrl);
      if (!classifyID || !i18nUrl) {
        return;
      }

      this.loading = true;
      try {
        // 获取任务结果
        const result = await getEntrysourceListByClassfyResult({
          classifyID: classifyID,
          i18nUrl: i18nUrl,
        });

        // 将结果转换为显示格式
        const resultData = result.data;
        if (resultData && resultData.list) {
          const entries = Object.values(resultData.list);
          // 复用 handleUpdate() 的数据转换逻辑
          this.transformEntriesToDataSource(entries);
        }
      } catch (error) {
        console.error("加载任务结果失败:", error);
        message.error("加载任务结果失败");
      } finally {
        this.loading = false;
      }
    },
    // ==================== 表格交互相关 ====================
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys;
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
    },
  },
};
</script>
<style scoped>
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
</style>