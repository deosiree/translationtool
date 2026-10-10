<template>
  <div style="padding: 16px;">
    <a-typography-title :level="4" style="margin-bottom: 12px;">
      原型：回写校验弹窗
    </a-typography-title>
    <a-typography-paragraph type="secondary" style="margin-bottom: 16px; max-width: 860px;">
      用 Mock 数据跑通"回写前校验 → 弹窗2 处理重复组（父/子行、展示列设置、勾选删除、重校验、回写）"的交互；不依赖真实接口。
    </a-typography-paragraph>
  </div>
  <div style="padding: 16px;">
    <a-space direction="vertical" :size="12">
      <a-space>
        <WriteBackButton buttonTitle="打开回写弹窗（原型）" size="middle" :submitMode="'emit'" :dataSource="mockDataSource"
          :enableValidation="true" :isPrototype="true"
          @update:dataSource="handleUpdateSelectedEntries($event)" />
        <WriteBackValidateModal mode="button" buttonTitle="冗余校验" buttonType="danger" :validationData="tableData"
          :deleteMode="'hard'" :isPrototype="true" :dataSource="mockDataSource"
          @update:dataSource="handleUpdateSelectedEntries($event)" />
        <a-button @click="resetAll">重置 Mock 数据</a-button>
      </a-space>
      <a-space>
        <a-button type="primary" @click="openSelectedEntries">已选词条</a-button>
      </a-space>
    </a-space>
  </div>
  <div style="padding: 16px;">
    <a-alert message="重要提示" type="warning" show-icon style="margin-bottom: 12px;">
      <template #description>
        <div style="line-height: 1.8; font-size: 14px;">
          <div style="margin-bottom: 8px; font-weight: bold; color: #1890ff;">
            1. 词条管理-已选词条-回写-回写校验
          </div>
          <div style="margin-left: 16px; margin-bottom: 4px;">
            删除按钮需要删除：
          </div>
          <div style="margin-left: 24px; margin-bottom: 4px;">
            1. 当前表单中的显示数据
          </div>
          <div style="margin-left: 24px; margin-bottom: 8px;">
            2. 父组件传来的已选词条
          </div>
          <div style="margin-left: 16px; margin-bottom: 12px; color: #52c41a; font-weight: 500;">
            删除不执行写库操作，只是控制回写时的准确性
          </div>
          <div style="margin-bottom: 8px; font-weight: bold; color: #1890ff;">
            2. 词条管理-已选词条-删除重复
          </div>
          <div style="margin-left: 16px; margin-bottom: 4px;">
            删除按钮需要删除：
          </div>
          <div style="margin-left: 24px; margin-bottom: 4px;">
            1. 当前表单中的显示数据
          </div>
          <div style="margin-left: 24px; margin-bottom: 4px;">
            2. 父组件传来的已选词条
          </div>
          <div style="margin-left: 24px; margin-bottom: 8px;">
            3. 数据库中的对应词条
          </div>
          <div style="margin-left: 16px; color: #ff4d4f; font-weight: 500;">
            删除时执行写库操作，用于删除冗余词条、维持指定版本下词条的纯净（删除了可能的重复导入、其他版本的词条等）
          </div>
        </div>
      </template>
    </a-alert>
  </div>
  <!-- 已选词条（原型） -->
  <CreateVersionModal :visible="selectedEntriesVisible" :dataSource="selectedEntries" :currentProduct="{}"
    :selectedRowKeys="selectedRowKeys" :selectedRows="selectedRows" :selectedProducts="selectedProducts"
    @createClose="selectedEntriesVisible = false" @cancelCreate="handleCancelSelectedEntries"
    @update:dataSource="selectedEntries = $event" @update:selectedRowKeys="selectedRowKeys = $event"
    @update:selectedRows="selectedRows = $event" @update:selectedProducts="selectedProducts = $event" />
</template>

<script>
import { message } from 'ant-design-vue'
import WriteBackButton from './writeBackButton.vue'
import WriteBackValidateModal from './WriteBackValidateModal.vue'
import CreateVersionModal from './createVersionModal.vue'
import { buildMockValidationResult, buildMockSelectedEntries } from './mockUtils'

// ==================== Mock 常量与初始数据 ====================
const langOptions = [
  { label: '英文', value: '英文' },
  { label: '俄文', value: '俄文' },
  { label: '西文', value: '西文' },
  { label: '法文', value: '法文' },
]

const DEFAULT_VISIBLE_COLUMNS = [
  // 父行默认
  'entry',
  'tag',
  'comment',
  // 子行默认（核心字段）
  'translateType',
  'translation',
  'entrySource',
  'diFileName',
  'updatedAt', // 修改时间
]

export default {
  components: {
    WriteBackButton,
    WriteBackValidateModal,
    CreateVersionModal,
  },

  data() {
    // 原型入口：先 mock 出 dataA（selectedRows），再用它生成校验结果
    const initialSelected = buildMockSelectedEntries(24)
    const initialValidation = buildMockValidationResult(initialSelected)
    return {
      // ==================== Mock 常量与初始数据 ====================
      langOptions,
      // ==================== 状态 ====================
      tableData: initialValidation, // Mock 校验结果数据，传递给 WriteBackValidateModal
      // dataA：父组件传给回写模态框的已选词条（selectedRows）
      mockDataSource: initialSelected,
      selectedEntriesVisible: false,
      selectedEntries: initialSelected,
      // 与 dataA 一致的 selectedRows / selectedRowKeys，方便后续扩展
      selectedRowKeys: initialSelected.map((item) => item.id),
      selectedRows: initialSelected,
      selectedProducts: { products: new Map(), totalNum: 0 },
      // 注意：列配置、表格展示等逻辑已移至 WriteBackValidateModal 组件
      // 这里只保留必要的数据管理
    }
  },

  // 注意：表格相关的计算属性和 watch 已移至 WriteBackValidateModal 组件

  watch: {
    // dataA：监听已选词条变化，便于排查“删除去重后回写无数据”
    mockDataSource: {
      deep: true,
      immediate: true,
      handler(nextVal) {
        const dataA = Array.isArray(nextVal) ? nextVal : []
        // eslint-disable-next-line no-console
        console.log('【PrototypePage】dataA(mockDataSource)变化：', {
          length: dataA.length,
          sampleIds: dataA.slice(0, 8).map((r) => (r && r.id) || null),
        })
      },
    },
  },

  methods: {
    // ==================== 原型：模拟“词条管理页”接入 update:dataSource ====================
    handleUpdateSelectedEntries(nextSelected) {
      // dataA：更新后的已选词条（会由校验弹窗删除逻辑驱动变化）
      this.mockDataSource = Array.isArray(nextSelected) ? nextSelected : []
      // eslint-disable-next-line no-console
      console.log('【原型】已更新选中词条（dataA）:', this.mockDataSource)
    },

    // 注意：删除、重校验、回写等业务逻辑已迁移至 WriteBackValidateModal 组件
    // mock API 函数已迁移至对应组件中

    // ==================== 交互：已选词条（原型） ====================
    openSelectedEntries() {
      // 每次打开都刷新一份 mock（便于演示“取消选择”）
      this.selectedEntries = buildMockSelectedEntries(24)
      this.selectedEntriesVisible = true
    },
    handleCancelSelectedEntries() {
      this.selectedEntriesVisible = false
      // 原型：关闭即清空（模拟“确认关闭后，已选择的词条将被清空”）
      this.selectedEntries = []
      this.selectedRowKeys = []
      this.selectedRows = []
      this.selectedProducts = { products: new Map(), totalNum: 0 }
    },

    resetAll() {
      // 重置 Mock 数据：先重建 dataA(selectedRows)，再生成校验结果
      const nextSelected = buildMockSelectedEntries(24)
      this.tableData = buildMockValidationResult(nextSelected)
      this.mockDataSource = nextSelected
      this.selectedEntries = nextSelected
      this.selectedRowKeys = nextSelected.map((item) => item.id)
      this.selectedRows = nextSelected
      message.success('已重置（原型）')
    },
  },

  // 注意：列配置初始化已移至 WriteBackValidateModal 组件
}
</script>
