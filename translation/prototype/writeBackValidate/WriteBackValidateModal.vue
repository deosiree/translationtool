<template>
  <!-- 按钮模式：显示按钮 -->
  <template v-if="mode === 'button'">
    <a-button :type="buttonType" :size="buttonSize" @click="showModal">
      {{ buttonTitle }}
    </a-button>

    <!-- 模态框（按钮模式下由内部 visible 控制） -->
    <CustomModal :modalTitle="modalTitle" :modalWidth="modalWidth" :bodyMaxHeight="'80vh'"
      :modalVisible="internalVisible" :showCancel="false" :showOk="false" @handleClose="handleClose">
      <!-- 模态框内容 -->
      <div style="display:flex; justify-content: space-between; gap: 12px; margin-bottom: 12px;">
        <a-space>
          <a-typography-text type="secondary">
            父行不可选，子行可选；删除/回写/重校验均基于"最新 selectedRowKeys"。
          </a-typography-text>
        </a-space>

        <a-space>
          <a-button size="small" :disabled="!allChildRowKeys.length" @click="invertSelectedRowKeys">
            反选
          </a-button>
          <a-popover trigger="click" placement="bottomRight" :overlayStyle="{ width: '260px' }">
            <template #content>
              <a-checkbox-group v-model:value="checkedColumn" @change="handleChangeColumn" style="width: 100%;">
                <a-row v-for="item in checkboxList" :key="item.value">
                  <a-col :span="24">
                    <a-checkbox :value="item.value">{{ item.label }}</a-checkbox>
                  </a-col>
                </a-row>
              </a-checkbox-group>
              <a-divider style="margin: 8px 0;" />
              <div style="display:flex; justify-content: flex-end; gap: 8px;">
                <a-button size="small" @click="resetColumns">恢复默认</a-button>
              </div>
            </template>
            <a-button>
              <template #icon>
                <SettingOutlined />
              </template>
              展示列
            </a-button>
          </a-popover>
        </a-space>
      </div>

      <a-spin :spinning="detailLoading">
        <a-table size="small" :columns="columns" :dataSource="tableData" :pagination="false" :rowKey="rowKey"
          :rowSelection="rowSelection" :expandable="expandableConfig" :scroll="tableScroll"
          :rowClassName="getRowClassName">
          <!-- 折叠箭头：只显示箭头，不显示序号（序号在序号列显示） -->
          <template #expandIcon="props">
            <span v-if="props.record.children != null && props.record.children.length > 0">
              <div v-if="props.expanded" style="display: inline-block; margin-right: 10px; cursor: pointer;"
                @click="(e) => { props.onExpand(props.record, e); }">
                <CaretDownOutlined />
              </div>
              <div v-else style="display: inline-block; margin-right: 10px; cursor: pointer;"
                @click="(e) => { props.onExpand(props.record, e); }">
                <CaretRightOutlined />
              </div>
            </span>
            <span v-else style="margin-right:23px"></span>
          </template>
        </a-table>
      </a-spin>

      <template #leftBottomBtn>
        <a-typography-text type="secondary" style="margin-right: 12px;">
          已选子行：{{ selectedRowKeys.length }}
        </a-typography-text>
        <a-button danger :disabled="!selectedRowKeys.length" :loading="detailLoading" @click="handleBatchDelete">
          删除
        </a-button>
        <a-button :loading="detailLoading" @click="handleRevalidate">重校验</a-button>
        <a-button v-if="deleteMode !== 'hard'" type="primary" :loading="detailLoading"
          @click="handleWriteBack">回写</a-button>
        <a-button v-else @click="handleClose">关闭</a-button>
      </template>
    </CustomModal>
  </template>

  <!-- 模态框模式：直接显示模态框 -->
  <CustomModal v-else :modalTitle="modalTitle" :modalWidth="modalWidth" :bodyMaxHeight="'80vh'" :modalVisible="visible"
    :showCancel="false" :showOk="false" @handleClose="handleClose">
    <!-- 模态框内容 -->
    <div style="display:flex; justify-content: space-between; gap: 12px; margin-bottom: 12px;">
      <a-space>
        <a-typography-text type="secondary">
          父行不可选，子行可选；删除/回写/重校验均基于"最新 selectedRowKeys"。
        </a-typography-text>
      </a-space>

      <a-space>
        <a-button size="small" :disabled="!allChildRowKeys.length" @click="invertSelectedRowKeys">
          反选
        </a-button>
        <a-popover trigger="click" placement="bottomRight" :overlayStyle="{ width: '260px' }">
          <template #content>
            <a-checkbox-group v-model:value="checkedColumn" @change="handleChangeColumn" style="width: 100%;">
              <a-row v-for="item in checkboxList" :key="item.value">
                <a-col :span="24">
                  <a-checkbox :value="item.value">{{ item.label }}</a-checkbox>
                </a-col>
              </a-row>
            </a-checkbox-group>
            <a-divider style="margin: 8px 0;" />
            <div style="display:flex; justify-content: flex-end; gap: 8px;">
              <a-button size="small" @click="resetColumns">恢复默认</a-button>
            </div>
          </template>
          <a-button>
            <template #icon>
              <SettingOutlined />
            </template>
            展示列
          </a-button>
        </a-popover>
      </a-space>
    </div>

    <a-spin :spinning="detailLoading">
      <a-table size="small" :columns="columns" :dataSource="tableData" :pagination="false" :rowKey="rowKey"
        :rowSelection="rowSelection" :expandable="expandableConfig" :scroll="tableScroll"
        :rowClassName="getRowClassName">
        <!-- 折叠箭头：只显示箭头，不显示序号（序号在序号列显示） -->
        <template #expandIcon="props">
          <span v-if="props.record.children != null && props.record.children.length > 0">
            <div v-if="props.expanded" style="display: inline-block; margin-right: 10px; cursor: pointer;"
              @click="(e) => { props.onExpand(props.record, e); }">
              <CaretDownOutlined />
            </div>
            <div v-else style="display: inline-block; margin-right: 10px; cursor: pointer;"
              @click="(e) => { props.onExpand(props.record, e); }">
              <CaretRightOutlined />
            </div>
          </span>
          <span v-else style="margin-right:23px"></span>
        </template>
      </a-table>
    </a-spin>

    <template #leftBottomBtn>
      <a-typography-text type="secondary" style="margin-right: 12px;">
        已选子行：{{ selectedRowKeys.length }}
      </a-typography-text>
      <a-button danger :disabled="!selectedRowKeys.length" :loading="detailLoading" @click="handleBatchDelete">
        删除
      </a-button>
      <a-button :loading="detailLoading" @click="handleRevalidate">重校验</a-button>
      <a-button v-if="deleteMode !== 'hard'" type="primary" :loading="detailLoading"
        @click="handleWriteBack">回写</a-button>
      <a-button v-else @click="handleClose">关闭</a-button>
    </template>
  </CustomModal>
</template>

<style>
/* 使用深度选择器，因为表格行是动态渲染的 */
/* Vue 2 兼容语法 - 确保整行包括 fixed 列都高亮 */
.ant-table-tbody>tr.parent-row-highlight,
.ant-table-body>tr.parent-row-highlight {
  background-color: #fffbe6 !important;
}

.ant-table-tbody>tr.parent-row-highlight:hover,
.ant-table-body>tr.parent-row-highlight:hover {
  background-color: #fffbe6 !important;
}

/* 确保 fixed 列也高亮 - 使用更具体的选择器 */
.ant-table-fixed-left .ant-table-tbody>tr.parent-row-highlight,
.ant-table-fixed-left .ant-table-body>tr.parent-row-highlight,
.ant-table-fixed-left table>tbody>tr.parent-row-highlight,
.ant-table-fixed-right .ant-table-tbody>tr.parent-row-highlight,
.ant-table-fixed-right .ant-table-body>tr.parent-row-highlight,
.ant-table-fixed-right table>tbody>tr.parent-row-highlight {
  background-color: #fffbe6 !important;
}

.ant-table-fixed-left .ant-table-tbody>tr.parent-row-highlight:hover,
.ant-table-fixed-left .ant-table-body>tr.parent-row-highlight:hover,
.ant-table-fixed-left table>tbody>tr.parent-row-highlight:hover,
.ant-table-fixed-right .ant-table-tbody>tr.parent-row-highlight:hover,
.ant-table-fixed-right .ant-table-body>tr.parent-row-highlight:hover,
.ant-table-fixed-right table>tbody>tr.parent-row-highlight:hover {
  background-color: #fffbe6 !important;
}

/* 确保 fixed 列中的单元格也高亮 */
.ant-table-tbody>tr.parent-row-highlight>td,
.ant-table-fixed-left .ant-table-tbody>tr.parent-row-highlight>td,
.ant-table-fixed-left .ant-table-body>tr.parent-row-highlight>td,
.ant-table-fixed-left table>tbody>tr.parent-row-highlight>td,
.ant-table-fixed-right .ant-table-tbody>tr.parent-row-highlight>td,
.ant-table-fixed-right .ant-table-body>tr.parent-row-highlight>td,
.ant-table-fixed-right table>tbody>tr.parent-row-highlight>td {
  background-color: #fffbe6 !important;
}

/* 确保 fixed 列的表头单元格也高亮（如果需要） */
.ant-table-fixed-left .ant-table-thead>tr.parent-row-highlight>th,
.ant-table-fixed-right .ant-table-thead>tr.parent-row-highlight>th {
  background-color: #fffbe6 !important;
}

/* 确保展开图标列不影响列顺序 - 更强的样式覆盖 */
.ant-table-row-expand-icon-cell {
  width: 0 !important;
  min-width: 0 !important;
  padding: 0 !important;
  position: absolute !important;
  left: 0 !important;
}

/* 调整第一个固定列的位置，为展开图标留出空间 */
.ant-table-fixed-left {
  left: 0 !important;
}
</style>

<script>
import { h, resolveComponent } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SettingOutlined, DeleteOutlined, CaretDownOutlined, CaretRightOutlined } from '@ant-design/icons-vue'
import CustomModal from '@/components/modal/index.vue'
import { changeColumn, getColPref } from '@/components/ColumnFilter'

// ==================== 辅助函数 ====================
function buildChildSignature(child) {
  return [
    child.entry || '',
    child.translateType || '',
    child.translation || '',
    child.entrySource || '',
    child.diFileName || '',
  ].join('|')
}

// ==================== Mock 函数（原型模式） ====================
// mock 回写校验接口：仅用于验证"校验动作已触发"，不真正调用后端
// 回写校验可以多语种，不需要分别调用，直接传递多语种数组
function mockValidateApi(params) {
  console.log('【mockValidateApi】调用参数：', params)
  return new Promise((resolve) => {
    setTimeout(() => {
      console.log('【mockValidateApi】返回：success', {
        writeType: params.writeType,
        translateTypes: params.translateTypes, // 回写语种数组（多语种，不需要分别调用）
        dataSourceCount: params.dataSource?.length || 0,
      })
      resolve({ success: true })
    }, 800)
  })
}

// ==================== 常量 ====================
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
  name: 'WriteBackValidateModal',
  components: {
    CustomModal,
    SettingOutlined,
    DeleteOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
  },
  props: {
    // ==================== 模式控制 ====================
    mode: {
      type: String,
      default: 'button', // 'button' | 'modal'
      validator: (v) => ['button', 'modal'].includes(v)
    },

    // ==================== 按钮模式相关 ====================
    buttonTitle: {
      type: String,
      default: '打开校验模态框'
    },
    buttonType: {
      type: String,
      default: 'default' // 'default' | 'primary' | 'danger' 等
    },
    buttonSize: {
      type: String,
      default: 'middle'
    },

    // ==================== 模态框模式相关 ====================
    visible: {
      type: Boolean,
      default: false // 仅在 mode="modal" 时生效
    },

    // ==================== 业务数据 ====================
    validationData: {
      type: Array,
      default: () => []
    },
    deleteMode: {
      type: String,
      default: 'soft', // 'soft' | 'hard'
      validator: (v) => ['soft', 'hard'].includes(v)
    },
    writeBackParams: {
      type: Object,
      default: () => ({})
    },

    // ==================== 其他配置 ====================
    modalTitle: {
      type: String,
      default: '回写校验结果（重复组处理）'
    },
    modalWidth: {
      type: String,
      default: '1100px'
    },
    /**
     * isPrototype: 是否为原型模式（使用 mock API）
     */
    isPrototype: {
      type: Boolean,
      default: false
    },
    /**
     * dataSource: 业务数据源（用于原型模式下的 mock API 调用）
     */
    dataSource: {
      type: Array,
      default: () => []
    }
  },
  emits: ['close', 'delete', 'revalidate', 'writeBack', 'update:dataSource'],
  data() {
    return {
      // 按钮模式下的内部 visible 状态
      internalVisible: false,
      // 列表勾选：仅与 UI 复选框联动
      selectedRowKeys: [],
      // 表格数据（从 validationData prop 初始化）
      tableData: [],
      // dataA：回写使用的业务数据源（本地镜像，始终跟随父组件的 dataSource 同步）
      writeBackDataSource: [],
      // 表格滚动配置
      tableScroll: { x: 'max-content', y: 'calc(80vh - 200px)' },
      // 展示列配置
      checkboxList: [], // 展示列可选的值，将在 created 中初始化
      checkedColumn: [...DEFAULT_VISIBLE_COLUMNS], // 展示列已选的值
      // 列配置映射：存储每个 dataIndex 对应的完整配置（包括 customRender），用于恢复自定义属性
      columnConfigMap: {},
      // 所有列的完整定义（包括序号列和操作列）
      allColumns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 100,
          fixed: "left",
          index: 0,
          customRender: ({ record }) => {
            // 父行显示父行序号
            if (Array.isArray(record.children)) {
              return this.parentIndexMap[record.groupId] || ''
            }
            // 子行显示子行在该组内的序号（通过查找父组）
            for (const group of this.tableData) {
              if (Array.isArray(group.children)) {
                const childIndex = group.children.findIndex((c) => c && c.id === record.id)
                if (childIndex !== -1) {
                  return childIndex + 1
                }
              }
            }
            return ''
          },
        },
        {
          title: '词条',
          dataIndex: 'entry',
          align: 'center',
          width: 160,
          resizable: true,
          index: 2,
          ellipsis: true,
          customRender: ({ record, text }) => {
            if (Array.isArray(record.children)) {
              // 父行：显示未被勾选子元素中相同的值（通常词条在父行和子行中都是相同的）
              const value = this.getParentRowValue(record, 'entry')
              return value !== '-' ? value : (text || '-')
            }
            // 子行：显示原始值
            return text || '-'
          },
        },
        {
          title: 'tag',
          dataIndex: 'tag',
          align: 'center',
          width: 130,
          resizable: true,
          index: 3,
          ellipsis: true,
          customRender: ({ record, text }) => {
            if (Array.isArray(record.children)) {
              // 父行：显示未被勾选子元素中相同的值
              return this.getParentRowValue(record, 'tag')
            }
            // 子行：显示原始值
            return text || '-'
          },
        },
        {
          title: 'comment',
          dataIndex: 'comment',
          align: 'center',
          width: 130,
          resizable: true,
          index: 4,
          ellipsis: true,
          customRender: ({ record, text }) => {
            if (Array.isArray(record.children)) {
              // 父行：显示未被勾选子元素中相同的值
              return this.getParentRowValue(record, 'comment')
            }
            // 子行：显示原始值
            return text || '-'
          },
        },
        {
          title: '语种',
          dataIndex: 'translateType',
          align: 'center',
          width: 100,
          resizable: true,
          index: 6,
          ellipsis: true,
          customRender: ({ record, text }) => {
            if (Array.isArray(record.children)) {
              // 父行：显示未被勾选子元素中相同的值
              return this.getParentRowValue(record, 'translateType')
            }
            // 子行：显示原始值
            return text || '-'
          },
        },
        {
          title: '翻译',
          dataIndex: 'translation',
          align: 'center',
          width: 180,
          resizable: true,
          index: 7,
          ellipsis: true,
          customRender: ({ record, text }) => {
            if (Array.isArray(record.children)) {
              // 父行：显示未被勾选子元素中相同的值
              return this.getParentRowValue(record, 'translation')
            }
            // 子行：显示原始值
            return text || '-'
          },
        },
        {
          title: '来源',
          dataIndex: 'entrySource',
          align: 'center',
          width: 130,
          resizable: true,
          index: 8,
          ellipsis: true,
          customRender: ({ record, text }) => {
            if (Array.isArray(record.children)) {
              // 父行：显示未被勾选子元素中相同的值
              return this.getParentRowValue(record, 'entrySource')
            }
            // 子行：显示原始值
            return text || '-'
          },
        },
        {
          title: '辞典',
          dataIndex: 'diFileName',
          align: 'center',
          width: 150,
          resizable: true,
          index: 9,
          ellipsis: true,
          customRender: ({ record, text }) => {
            if (Array.isArray(record.children)) {
              // 父行：显示未被勾选子元素中相同的值
              return this.getParentRowValue(record, 'diFileName')
            }
            // 子行：显示原始值
            return text || '-'
          },
        },
        {
          title: 'id',
          dataIndex: 'id',
          align: 'center',
          width: 200,
          resizable: true,
          index: 10,
          ellipsis: true,
          customRender: ({ record, text }) => {
            if (Array.isArray(record.children)) {
              // 父行：显示未被勾选子元素中相同的值
              return this.getParentRowValue(record, 'id')
            }
            // 子行：显示原始值（来自 dataA 的唯一标识）
            return text || '-'
          },
        },
        {
          title: '修改时间',
          dataIndex: 'updatedAt',
          align: 'center',
          width: 180,
          resizable: true,
          index: 11,
          ellipsis: true,
          customRender: ({ record, text }) => {
            if (Array.isArray(record.children)) {
              // 父行：显示未被勾选子元素中相同的值
              const value = this.getParentRowValue(record, 'updatedAt')
              if (value === '-') return '-'
              // 格式化日期
              if (!value) return '-'
              const date = new Date(value)
              return date.toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
              })
            }
            // 子行：显示原始值
            if (!text) return '-'
            const date = new Date(text)
            return date.toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit',
            })
          },
        },
        {
          title: '操作',
          dataIndex: 'operation',
          align: 'center',
          width: 80,
          fixed: 'right',
          resizable: true,
          index: 100,
          customRender: ({ record }) => {
            if (Array.isArray(record.children)) return null
            const AButton = resolveComponent('a-button')
            return h(
              AButton,
              {
                type: 'text',
                danger: true,
                size: 'small',
                title: '删除该子行',
                onClick: () => this.handleSingleDelete(record),
              },
              { default: () => h(DeleteOutlined) }
            )
          },
        },
      ],
      // 当前显示的列（从 allColumns 过滤得到，在 created 中初始化）
      columns: [],
      // 加载状态
      detailLoading: false,
    }
  },
  computed: {
    allChildRowKeys() {
      const keys = []
        ; (this.tableData || []).forEach((g) => {
          if (!Array.isArray(g.children)) return
          g.children.forEach((c) => {
            if (c && c.id) keys.push(c.id)
          })
        })
      return keys
    },

    // 父行序号：根据当前 tableData 顺序生成
    parentIndexMap() {
      const map = {}
        ; (this.tableData || []).forEach((g, index) => {
          if (Array.isArray(g.children) && g.groupId) {
            map[g.groupId] = index + 1
          }
        })
      return map
    },

    // ==================== 表格配置 ====================
    expandableConfig() {
      return {
        defaultExpandAllRows: true,
        columnWidth: 0, // 设置展开列宽度为0，避免占用空间
        fixed: false, // 不固定展开列
      }
    },

    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        onChange: (keys) => {
          this.selectedRowKeys = keys
        },
        getCheckboxProps: (record) => ({
          disabled: Array.isArray(record.children),
        }),
      }
    },
  },
  watch: {
    // 监听 validationData prop 变化，更新 tableData
    validationData: {
      handler(newVal) {
        if (newVal && newVal.length > 0) {
          this.tableData = newVal
          // 初始化默认选中逻辑
          this.initDefaultSelection()
          // 重置加载状态（重校验完成后）
          this.detailLoading = false
        }
      },
      immediate: true,
      deep: true,
    },

    // dataA：监听父组件传入的已选词条变化，保持本地 writeBackDataSource 镜像同步
    // dataA：监听父组件传入的已选词条变化，保持本地 writeBackDataSource 镜像同步，并打印前后对比
    dataSource: {
      handler(newVal, oldVal) {
        const prev = Array.isArray(oldVal) ? oldVal : []
        const next = Array.isArray(newVal) ? newVal : []
        this.writeBackDataSource = next
        // eslint-disable-next-line no-console
        console.log('【WriteBackValidateModal】dataA(dataSource) 变化：', {
          prevLength: prev.length,
          nextLength: next.length,
          prevIds: prev.map((r) => (r && r.id) || null),
          nextIds: next.map((r) => (r && r.id) || null),
          prev: prev,
          next: next,
        })
      },
      immediate: true,
      deep: true,
    },
  },
  methods: {
    // ==================== 模态框控制 ====================
    showModal() {
      // 按钮模式下，打开模态框前可能需要加载数据
      if (this.mode === 'button') {
        this.internalVisible = true
        // 如果 validationData 为空，可能需要触发数据加载
        this.loadValidationData()
      }
    },

    handleClose() {
      // 移除焦点，解决 aria-hidden 报错
      if (document.activeElement && document.activeElement.blur) {
        document.activeElement.blur()
      }

      if (this.mode === 'button') {
        this.internalVisible = false
      }
      // 模态框模式下，通知父组件关闭
      this.$emit('close')
    },

    // ==================== 数据加载 ====================
    loadValidationData() {
      // 如果 validationData prop 为空，可能需要调用 API
      // 或者由父组件通过 prop 传入
      if (this.validationData && this.validationData.length > 0) {
        this.tableData = this.validationData
        this.initDefaultSelection()
      }
    },

    // ==================== 初始化默认选中 ====================
    initDefaultSelection() {
      // 每个父组只保留第一个子元素（最新的），其他都勾选（待删除）
      const keysToKeep = []
      const keysToDelete = []
        ; (this.tableData || []).forEach((group) => {
          if (!Array.isArray(group.children) || group.children.length === 0) return
          // 确保子元素按 updatedAt 降序排列
          group.children.sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0))
          const firstChild = group.children[0]
          if (firstChild && firstChild.id) {
            keysToKeep.push(firstChild.id)
          }
          for (let i = 1; i < group.children.length; i += 1) {
            const child = group.children[i]
            if (child && child.id) {
              keysToDelete.push(child.id)
            }
          }
        })
      this.selectedRowKeys = keysToDelete
    },

    // ==================== 表格辅助 ====================
    rowKey(record) {
      if (Array.isArray(record.children)) return record.groupId
      return record.id
    },

    // 获取父行应该显示的值（基于未被勾选的子元素）
    getParentRowValue(parentRecord, dataIndex) {
      if (!Array.isArray(parentRecord.children) || parentRecord.children.length === 0) {
        return '-'
      }

      // 获取未被勾选的子元素（保留的子元素）
      const selectedSet = new Set(this.selectedRowKeys)
      const keptChildren = parentRecord.children.filter((child) => !selectedSet.has(child.id))

      if (keptChildren.length === 0) {
        return '-'
      }

      // 严格透传原则：
      // - 只看“未勾选子元素（保留项）”
      // - 若任意保留子元素该字段为空/缺失，则父行不透传（返回 '-'）
      // - 只有所有保留子元素该字段值完全一致，才透传该值
      const values = keptChildren.map((child) => (child ? child[dataIndex] : undefined))
      if (values.some((v) => v == null || v === '')) return '-'

      const firstValue = values[0]
      const allSame = values.every((v) => v === firstValue)
      return allSame ? firstValue : '-'
    },

    // 判断父行是否应该高亮（所有列都已去重，或只有一个保留的子元素）
    shouldHighlightParent(parentRecord) {
      if (!Array.isArray(parentRecord.children) || parentRecord.children.length === 0) {
        return false
      }

      // 获取未被勾选的子元素（保留的子元素）
      const selectedSet = new Set(this.selectedRowKeys)
      const keptChildren = parentRecord.children.filter((child) => !selectedSet.has(child.id))

      // 如果只有一个保留的子元素，高亮
      if (keptChildren.length === 1) {
        return true
      }

      // 如果所有列都已去重（所有列在保留的子元素中都是一致的），高亮
      // 需要检查的列：entry, translateType, translation, entrySource, diFileName
      const columnsToCheck = ['entry', 'translateType', 'translation', 'entrySource', 'diFileName']

      for (const dataIndex of columnsToCheck) {
        const value = this.getParentRowValue(parentRecord, dataIndex)
        if (value === '-') {
          // 如果有一列不一致，不高亮
          return false
        }
      }

      // 所有列都一致，高亮
      return true
    },

    // 获取行样式类名
    getRowClassName(record) {
      if (Array.isArray(record.children)) {
        // 父行：如果应该高亮，添加黄色背景
        if (this.shouldHighlightParent(record)) {
          return 'parent-row-highlight'
        }
        return ''
      }
      // 子行：不添加特殊样式
      return ''
    },

    // ==================== 展示列管理 ====================
    handleChangeColumn(checkedValue) {
      // 先保存序号列、操作列、词条列的完整配置（这些列必须永久保持）
      const indexCol = this.columns.find((col) => col.dataIndex === 'index')
      const operationCol = this.columns.find((col) => col.dataIndex === 'operation')
      const entryCol = this.columns.find((col) => col.dataIndex === 'entry')

      // 使用通用函数修改 columns
      changeColumn(
        'colPref-writeBackValidate',
        200,
        checkedValue,
        this,
        false,
        this.checkboxList
      )

      // 确保序号列、操作列、词条列始终存在（changeColumn 可能移除了它们）
      this.ensureFixedColumns(indexCol, operationCol, entryCol)

      // 恢复自定义的 customRender 等属性
      this.restoreCustomColumnProps()

      // 确保固定列的 fixed 和 index 属性正确（changeColumn 可能会改变它们）
      this.columns.forEach((col) => {
        if (col.dataIndex === 'index') {
          col.fixed = 'left'
          col.index = 0
        } else if (col.dataIndex === 'entry') {
          col.fixed = 'left'
          col.index = 2
        } else if (col.dataIndex === 'operation') {
          col.fixed = 'right'
          col.index = 100
        }
      })

      // 重新排序：先按 fixed 分组，再按 index 排序
      // 1. 固定左侧列（序号、词条等）
      const leftFixedCols = this.columns.filter((col) => col.fixed === 'left')
      // 2. 普通列
      const normalCols = this.columns.filter((col) => !col.fixed)
      // 3. 固定右侧列（操作列）
      const rightFixedCols = this.columns.filter((col) => col.fixed === 'right')

      // 对每组内部按 index 排序
      leftFixedCols.sort((a, b) => (a.index || 999) - (b.index || 999))
      normalCols.sort((a, b) => (a.index || 999) - (b.index || 999))
      rightFixedCols.sort((a, b) => (a.index || 999) - (b.index || 999))

      // 重新组合：左侧固定列 + 普通列 + 右侧固定列
      this.columns = [...leftFixedCols, ...normalCols, ...rightFixedCols]

      // 挂载列宽拖拽能力（仅本次会话内生效）
      this.enhanceColumnsWithResize()
    },

    // 确保序号列、操作列、词条列始终存在
    ensureFixedColumns(indexCol, operationCol, entryCol) {
      const hasIndexCol = this.columns.some((col) => col.dataIndex === 'index')
      const hasOperationCol = this.columns.some((col) => col.dataIndex === 'operation')
      const hasEntryCol = this.columns.some((col) => col.dataIndex === 'entry')

      // 如果序号列不存在，从 allColumns 恢复完整配置
      if (!hasIndexCol) {
        const indexConfig = indexCol || this.allColumns.find((col) => col.dataIndex === 'index')
        if (indexConfig) {
          // 确保序号列的 index 是 0，fixed 是 left
          const config = { ...indexConfig, index: 0, fixed: 'left' }
          this.columns.unshift(config)
        }
      } else {
        // 如果序号列存在，确保它的 index 和 fixed 正确
        const indexColInColumns = this.columns.find((col) => col.dataIndex === 'index')
        if (indexColInColumns) {
          indexColInColumns.index = 0
          indexColInColumns.fixed = 'left'
        }
      }

      // 如果词条列不存在，从 allColumns 恢复完整配置
      if (!hasEntryCol) {
        const entryConfig = entryCol || this.allColumns.find((col) => col.dataIndex === 'entry')
        if (entryConfig) {
          // 确保词条列的 index 是 2，fixed 是 left
          const config = { ...entryConfig, index: 2, fixed: 'left' }
          // 词条列应该在序号列之后，找到正确位置插入
          const indexColPos = this.columns.findIndex((col) => col.dataIndex === 'index')
          if (indexColPos !== -1) {
            this.columns.splice(indexColPos + 1, 0, config)
          } else {
            this.columns.unshift(config)
          }
        }
      } else {
        // 如果词条列存在，确保它的 index 和 fixed 正确
        const entryColInColumns = this.columns.find((col) => col.dataIndex === 'entry')
        if (entryColInColumns) {
          entryColInColumns.index = 2
          entryColInColumns.fixed = 'left'
        }
      }

      // 如果操作列不存在，从 allColumns 恢复完整配置
      if (!hasOperationCol) {
        const operationConfig = operationCol || this.allColumns.find((col) => col.dataIndex === 'operation')
        if (operationConfig) {
          // 确保操作列的 index 是 100，fixed 是 right
          const config = { ...operationConfig, index: 100, fixed: 'right' }
          this.columns.push(config)
        }
      } else {
        // 如果操作列存在，确保它的 index 和 fixed 正确
        const operationColInColumns = this.columns.find((col) => col.dataIndex === 'operation')
        if (operationColInColumns) {
          operationColInColumns.index = 100
          operationColInColumns.fixed = 'right'
        }
      }
    },

    // 恢复自定义列属性（customRender 等）
    restoreCustomColumnProps() {
      this.columns.forEach((col) => {
        const originalConfig = this.columnConfigMap[col.dataIndex]
        if (originalConfig) {
          // 恢复自定义属性
          if (originalConfig.customRender) {
            // 直接使用原始 customRender 即可
            col.customRender = originalConfig.customRender
          }
          if (originalConfig.fixed !== undefined) {
            col.fixed = originalConfig.fixed
          }
          if (originalConfig.width !== undefined) {
            col.width = originalConfig.width
          }
          if (originalConfig.resizable !== undefined) {
            col.resizable = originalConfig.resizable
          }
          if (originalConfig.ellipsis !== undefined) {
            col.ellipsis = originalConfig.ellipsis
          }
          // 恢复 index，确保排序正确
          if (originalConfig.index !== undefined) {
            col.index = originalConfig.index
          }
        }
      })
    },

    resetColumns() {
      this.checkedColumn = [...DEFAULT_VISIBLE_COLUMNS]
      this.handleChangeColumn(this.checkedColumn)
    },

    invertSelectedRowKeys() {
      const allKeys = this.allChildRowKeys || []
      const selectedSet = new Set(this.selectedRowKeys || [])
      this.selectedRowKeys = allKeys.filter((k) => !selectedSet.has(k))
    },

    // ==================== dataA：同步删除辅助 ====================
    syncWriteBackDataSourceAfterDelete(idSet) {
      if (!Array.isArray(this.writeBackDataSource)) return
      const nextDataSource = this.writeBackDataSource.filter((item) => item && !idSet.has(item.id))
      this.writeBackDataSource = nextDataSource
      this.$emit('update:dataSource', nextDataSource)
    },

    handleSingleDelete(record) {
      // 单个删除逻辑
      if (this.deleteMode === 'soft') {
        const deletedIds = [record.id]
        const deletedSet = new Set(deletedIds)
        // 软删除：只从选中列表中移除，并更新表格数据
        this.selectedRowKeys = this.selectedRowKeys.filter((k) => k !== record.id)
        // dataA：同步删除父组件传入的已选词条（仅删掉被删除的子元素）
        this.syncWriteBackDataSourceAfterDelete(deletedSet)

        // dataC：从表格数据中移除，并在组去重完成（<=1）时移除父行本身
        const keysToAutoClear = new Set()
        const nextGroups = []
          ; (this.tableData || []).forEach((g) => {
            if (!Array.isArray(g.children)) return
            const nextChildren = g.children.filter((c) => !deletedSet.has(c.id))
            // 如果删除后只剩一个或没有子元素：移除该组（父行）
            if (nextChildren.length <= 1) {
              if (nextChildren.length === 1 && nextChildren[0] && nextChildren[0].id) {
                // dataB：该组已不再出现在校验表单中，清掉勾选状态（但不影响 dataA 保留代表子元素）
                keysToAutoClear.add(nextChildren[0].id)
              }
              return
            }
            nextGroups.push({ ...g, children: nextChildren })
          })
        this.tableData = nextGroups
        if (keysToAutoClear.size) {
          this.selectedRowKeys = (this.selectedRowKeys || []).filter((k) => !keysToAutoClear.has(k))
        }
        message.success('删除成功（原型）')
      } else {
        // 硬删除：emit 事件，由父组件调用删除 API
        this.detailLoading = true
        this.$emit('delete', { selectedKeys: [record.id] })
        // 注意：硬删除后需要父组件更新 validationData，然后通过 watch 更新 tableData
      }
    },

    // ==================== 业务操作 ====================
    async handleBatchDelete() {
      // 根据 deleteMode 执行不同的删除逻辑
      // 此方法将在 Task 4 中实现
      if (!this.selectedRowKeys.length) return

      const selectedSet = new Set(this.selectedRowKeys)
      const violations = []
      this.tableData.forEach((g) => {
        if (!Array.isArray(g.children)) return
        const willDeleteCount = g.children.filter((c) => selectedSet.has(c.id)).length
        if (willDeleteCount > 0 && willDeleteCount === g.children.length) {
          violations.push(g.entry || g.groupId)
        }
      })
      if (violations.length) {
        message.warning(`以下组至少保留 1 条：${violations.join(', ')}`)
        return
      }

      if (this.deleteMode === 'soft') {
        // 软删除：只更新表格数据，不调用 API
        this.detailLoading = true
        setTimeout(() => {
          // dataA：同步删除父组件传入的已选词条（仅删掉被删除的子元素）
          this.syncWriteBackDataSourceAfterDelete(selectedSet)

          // dataC：移除被删除子元素；组去重完成（<=1）时移除父行本身
          const keysToAutoClear = new Set()
          const nextGroups = []
            ; (this.tableData || []).forEach((g) => {
              if (!Array.isArray(g.children)) return
              const nextChildren = g.children.filter((c) => !selectedSet.has(c.id))
              if (nextChildren.length <= 1) {
                if (nextChildren.length === 1 && nextChildren[0] && nextChildren[0].id) {
                  keysToAutoClear.add(nextChildren[0].id)
                }
                return
              }
              nextGroups.push({ ...g, children: nextChildren })
            })
          this.tableData = nextGroups

          // dataB：删除勾选项 + 被自动移除组的代表子元素勾选
          this.selectedRowKeys = (this.selectedRowKeys || []).filter(
            (k) => !selectedSet.has(k) && !keysToAutoClear.has(k)
          )
          this.detailLoading = false
          message.success('删除成功（原型）')
        }, 800)
      } else {
        // 硬删除：原型模式下内部处理，正常模式下 emit 事件
        this.detailLoading = true
        try {
          if (this.isPrototype) {
            // 原型模式：Mock 删除 API 调用
            await new Promise((resolve) => {
              setTimeout(() => {
                console.log('【批量删除】删除 keys：', this.selectedRowKeys)
                resolve({ success: true })
              }, 800)
            })

            // 更新表格数据：移除已删除的项
            // dataA：同步删除父组件传入的已选词条（仅删掉被删除的子元素）
            this.syncWriteBackDataSourceAfterDelete(selectedSet)

            // dataC：移除被删除子元素；组去重完成（<=1）时移除父行本身
            const keysToAutoClear = new Set()
            const nextGroups = []
              ; (this.tableData || []).forEach((g) => {
                if (!Array.isArray(g.children)) return
                const nextChildren = g.children.filter((c) => !selectedSet.has(c.id))
                if (nextChildren.length <= 1) {
                  if (nextChildren.length === 1 && nextChildren[0] && nextChildren[0].id) {
                    keysToAutoClear.add(nextChildren[0].id)
                  }
                  return
                }
                nextGroups.push({ ...g, children: nextChildren })
              })
            this.tableData = nextGroups

            // dataB：删除勾选项 + 被自动移除组的代表子元素勾选
            this.selectedRowKeys = (this.selectedRowKeys || []).filter(
              (k) => !selectedSet.has(k) && !keysToAutoClear.has(k)
            )
            message.success('批量删除完成（原型）')
          } else {
            // 正常模式：emit 事件，由父组件调用删除 API
            this.$emit('delete', { selectedKeys: this.selectedRowKeys })
            // 注意：硬删除后需要父组件更新 validationData，然后通过 watch 更新 tableData
          }
        } catch (error) {
          message.error(`批量删除失败：${error.message || error}`)
        } finally {
          this.detailLoading = false
        }
      }
    },

    async handleRevalidate() {
      // 重校验逻辑
      this.detailLoading = true

      try {
        if (this.isPrototype) {
          // 原型模式：使用 mock API
          const writeType = (this.writeBackParams && this.writeBackParams.type) || 'DEFAUT'
          const translateTypes = (this.writeBackParams && this.writeBackParams.language) || []

          // await mockValidateApi({
          //   writeType,
          //   translateTypes,
          //   dataSource: this.dataSource,
          // })
          // 不重新 mock 校验数据，仅用延时模拟接口耗时
          await new Promise((resolve) => setTimeout(resolve, 1500))
          message.success('await mockValidateApi（延时模拟）')

          // 模拟去重逻辑（简化版）
          const nextGroups = []
            ; (this.tableData || []).forEach((group) => {
              if (!Array.isArray(group.children)) return

              const seen = new Set()
              const nextChildren = []

              group.children.forEach((child) => {
                const sig = buildChildSignature(child)
                if (seen.has(sig)) {
                  return
                }
                seen.add(sig)
                nextChildren.push(child)
              })

              if (nextChildren.length <= 1) {
                return
              }

              nextGroups.push({ ...group, children: nextChildren })
            })

          // 更新表格数据
          this.tableData = nextGroups
          // 重新应用默认选中逻辑
          this.initDefaultSelection()
          message.success('重校验完成（原型）')
        } else {
          // 正常模式：emit 事件，由父组件调用校验 API
          this.$emit('revalidate', {
            selectedKeys: this.selectedRowKeys,
            writeBackParams: this.writeBackParams
          })
          // 注意：重校验后，父组件需要更新 validationData prop
          // 组件会通过 watch 自动更新 tableData，并重新应用默认选中逻辑
        }
      } finally {
        this.detailLoading = false
      }
    },

    async handleWriteBack() {
      // dataA：最终回写必须严格以父组件传入的已选词条为准（使用本地镜像 writeBackDataSource）
      const dataA = Array.isArray(this.writeBackDataSource) ? this.writeBackDataSource : []
      const keysToWrite = dataA.map((row) => row && row.id).filter(Boolean)
      console.log("要回写的数据为：", keysToWrite, dataA)

      if (!keysToWrite || keysToWrite.length === 0) {
        message.warning('当前无可回写的数据（原型）')
        return
      }

      // 如果有重复数据，提示用户
      if (this.tableData.length > 0) {
        Modal.confirm({
          title: '提示',
          content:
            '回写数据中含有重复数据，若不去重则无法保证哪个数据将被回写入文件，是否继续回写？',
          okText: '继续回写',
          cancelText: '取消',
          onOk: () => {
            this.emitWriteBack(keysToWrite)
          },
        })
      } else {
        this.emitWriteBack(keysToWrite)
      }
    },

    emitWriteBack(keysToKeep) {
      // emit 回写事件
      this.$emit('writeBack', {
        selectedKeys: keysToKeep,
        writeBackParams: this.writeBackParams
      })

      // 按钮模式下，回写成功后关闭模态框
      if (this.mode === 'button') {
        this.internalVisible = false
      }
    },

    // ==================== 列宽拖拽（仅本次会话） ====================
    enhanceColumnsWithResize() {
      const minWidth = 80
      // 为当前 columns 注入 customHeaderCell，用于处理表头拖拽
      this.columns = (this.columns || []).map((col, index) => {
        // 仅对存在宽度且未显式禁止 resizable 的列开启拖拽
        if (!col || col.resizable === false || col.dataIndex === 'index') {
          return col
        }
        const newCol = { ...col }
        newCol.customHeaderCell = () => {
          return {
            style: { cursor: 'col-resize' },
            onMousedown: (e) => this.handleHeaderMouseDown(e, index, minWidth),
          }
        }
        return newCol
      })
    },
    handleHeaderMouseDown(e, colIndex, minWidth) {
      // 简单实现：按下即开始拖拽，基于鼠标横向移动调整列宽
      const startX = e.clientX
      const startWidth = this.columns[colIndex]?.width || 100

      const onMouseMove = (event) => {
        const deltaX = event.clientX - startX
        let newWidth = startWidth + deltaX
        if (newWidth < minWidth) newWidth = minWidth
        if (!this.columns[colIndex]) return
        this.columns[colIndex].width = newWidth
      }

      const onMouseUp = () => {
        window.removeEventListener('mousemove', onMouseMove)
        window.removeEventListener('mouseup', onMouseUp)
      }

      window.addEventListener('mousemove', onMouseMove)
      window.addEventListener('mouseup', onMouseUp)
    },
  },
  created() {
    // 初始化 checkboxList（排除序号列、操作列、词条列，这些列永久保持）
    this.checkboxList = this.allColumns
      .filter((col) => col.dataIndex !== 'index' && col.dataIndex !== 'operation' && col.dataIndex !== 'entry')
      .map((col) => ({
        label: col.title,
        value: col.dataIndex,
        index: col.index,
      }))

    // 初始化 columnConfigMap
    this.allColumns.forEach((col) => {
      this.columnConfigMap[col.dataIndex] = {
        customRender: col.customRender,
        fixed: col.fixed,
        width: col.width,
        resizable: col.resizable,
        ellipsis: col.ellipsis,
        index: col.index, // 保存 index，确保排序正确
      }
    })

    // 初始化 columns（确保序号列、操作列、词条列永久保持）
    const visibleSet = new Set(this.checkedColumn)
    this.columns = this.allColumns.filter(
      (col) => visibleSet.has(col.dataIndex) || col.dataIndex === 'index' || col.dataIndex === 'operation' || col.dataIndex === 'entry'
    )
    // 确保固定列的 fixed 和 index 属性正确
    this.columns.forEach((col) => {
      if (col.dataIndex === 'index') {
        col.fixed = 'left'
        col.index = 0
      } else if (col.dataIndex === 'entry') {
        col.fixed = 'left'
        col.index = 2
      } else if (col.dataIndex === 'operation') {
        col.fixed = 'right'
        col.index = 100
      }
    })
    // 重新排序：先按 fixed 分组，再按 index 排序
    // 1. 固定左侧列（序号、词条等）
    const leftFixedCols = this.columns.filter((col) => col.fixed === 'left')
    // 2. 普通列
    const normalCols = this.columns.filter((col) => !col.fixed)
    // 3. 固定右侧列（操作列）
    const rightFixedCols = this.columns.filter((col) => col.fixed === 'right')

    // 对每组内部按 index 排序
    leftFixedCols.sort((a, b) => (a.index || 999) - (b.index || 999))
    normalCols.sort((a, b) => (a.index || 999) - (b.index || 999))
    rightFixedCols.sort((a, b) => (a.index || 999) - (b.index || 999))

    // 重新组合：左侧固定列 + 普通列 + 右侧固定列
    this.columns = [...leftFixedCols, ...normalCols, ...rightFixedCols]

    // 挂载列宽拖拽能力
    this.enhanceColumnsWithResize()
  },
  mounted() {
    // 读取用户列偏好
    getColPref('colPref-writeBackValidate', 200, this, false, this.checkboxList)
    // 恢复自定义属性
    this.$nextTick(() => {
      this.restoreCustomColumnProps()
      // 列偏好恢复后，再挂一次列宽拖拽，保证最新 columns 生效
      this.enhanceColumnsWithResize()
    })
  },
}
</script>
