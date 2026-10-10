<template>
  <div style="padding: 16px;">
    <a-typography-title :level="4" style="margin-bottom: 12px;">
      原型：词条导入工作台（整页 + 配置文件 XML/JSON 子模块）
    </a-typography-title>
    <a-typography-paragraph type="secondary" style="margin-bottom: 16px; max-width: 720px;">
      本页用于在不改动正式工作台页面的前提下，体验「导入模态框」整组件 + 新配置文件（XML/JSON）导入原型。
    </a-typography-paragraph>

    <a-space>
      <a-button type="primary" @click="openModal">
        打开词条导入模态框（原型）
      </a-button>
      <a-button @click="resetMockTask">
        重置 Mock 任务数据
      </a-button>
    </a-space>

    <!-- 这里展示当前 Mock 任务的一些关键信息，便于确认上下文 -->
    <a-card size="small" title="当前任务（Mock）" style="margin-top: 16px; max-width: 720px;">
      <a-descriptions size="small" :column="2" bordered>
        <a-descriptions-item label="任务名称">{{ mockTask.name }}</a-descriptions-item>
        <a-descriptions-item label="产品名称">{{ mockTask.productName }}</a-descriptions-item>
        <a-descriptions-item label="上级分类名称">{{ mockTask.classifyName }}</a-descriptions-item>
        <a-descriptions-item label="翻译语种">{{ mockTask.translateType }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <!-- 导入模态框原型：完整复用业务组件，实现「整组件拷贝」的体验验证 -->
    <ImportModalPrototype
      :visible="modalVisible"
      modalTitle="原型：词条导入（Mock 任务）"
      :currentTask="mockTask"
      :classifyLimit="mockClassifyLimit"
      @handleClose="handleModalClose"
      @handleOK="handleModalOk"
      @afterSave="handleAfterSave"
    />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
// 这里直接引用业务侧的导入模态框组件：
// - 满足“整组件拷贝”的需求（UI/交互一致）
// - 原型只负责提供 Mock 任务上下文 + 打开/关闭控制
import ImportModalPrototype from './ImportModalPrototype.vue'

// ==================== Mock 任务上下文 ====================
const modalVisible = ref(false)

const mockTask = reactive({
  id: 'task-mock-001',
  name: '【原型】工作台导入流程联调',
  productName: '原型环境产品',
  classifyName: '原型分类 A-1',
  translateType: '中文-英文',
  versionId: 'v-mock-001',
  // 与 importModal 中使用的 transMap 结构保持一致（简化版）
  transMap: {
    value: 'english',
    interpretation: 'englishInterpretation',
    state: 'translateState',
  },
})

// classifyLimit 在原组件中用于控制字节长度等，这里给一个最小可用的 Mock
const mockClassifyLimit = reactive({
  // key 为 classfy1，value 内可配置 foreignMaxByte 等，这里留空对象即可满足访问
})

// ==================== 事件与交互 ====================
function openModal() {
  modalVisible.value = true
}

function handleModalClose() {
  modalVisible.value = false
}

function handleModalOk() {
  // 原组件内部会负责保存逻辑，这里简单提示一下
  message.success('已触发保存（原型）')
}

function handleAfterSave() {
  // afterSave 用于刷新任务小红点等，这里仅做日志与轻提示，避免影响原组件内部逻辑
  // eslint-disable-next-line no-console
  console.log('[prototype] importModal afterSave with mock task', mockTask)
  message.success('原型：afterSave 回调已触发')
}

function resetMockTask() {
  mockTask.name = '【原型】工作台导入流程联调'
  mockTask.productName = '原型环境产品'
  mockTask.classifyName = '原型分类 A-1'
  mockTask.translateType = '中文-英文'
  mockTask.versionId = 'v-mock-001'
  message.success('已重置 Mock 任务信息')
}
</script>
