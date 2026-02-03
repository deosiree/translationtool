<template>
  <div style="padding: 16px;">
    <a-typography-title :level="4" style="margin-bottom: 12px;">
      原型：回写前校验（重复组处理）
    </a-typography-title>
    <a-typography-paragraph type="secondary" style="margin-bottom: 16px; max-width: 860px;">
      用 Mock 数据跑通“回写前校验 → 弹窗2 处理重复组（父/子行、展示列设置、勾选删除、重校验、回写）”的交互；不依赖真实接口。
    </a-typography-paragraph>

    <a-space>
      <WriteBackButton buttonTitle="打开回写弹窗（原型）" size="middle" :submitMode="'emit'" :dataSource="mockDataSource"
        @submit="handleWriteBackFormSubmit" />
      <a-button @click="resetAll">重置 Mock 数据</a-button>
    </a-space>

    <a-card size="small" title="当前选中 keys（Mock）" style="margin-top: 16px; max-width: 860px;">
      <a-typography-paragraph style="margin-bottom: 0;">
        {{ selectedRowKeys.length ? selectedRowKeys.join(', ') : '（空）' }}
      </a-typography-paragraph>
    </a-card>

    <!-- 弹窗2：重复组处理 -->
    <CustomModal modalTitle="回写校验结果（重复组处理 - 原型）" modalWidth="1100px" :bodyMaxHeight="'80vh'"
      :modalVisible="detailModalOpen" :showCancel="false" :showOk="false" @handleClose="detailModalOpen = false">
      <div style="display:flex; justify-content: space-between; gap: 12px; margin-bottom: 12px;">
        <a-space>
          <a-typography-text type="secondary">
            父行不可选，子行可选；删除/回写/重校验均基于“最新 selectedRowKeys”。
          </a-typography-text>
        </a-space>

        <a-space>
          <a-button size="small" :disabled="!allChildRowKeys.length" @click="invertSelectedRowKeys">
            反选
          </a-button>
          <a-popover trigger="click" placement="bottomRight" :overlayStyle="{ width: '260px' }">
            <template #content>
              <a-checkbox-group v-model:value="visibleColumnKeys" style="width: 100%;">
                <a-row v-for="col in allColumnOptions" :key="col.value">
                  <a-col :span="24">
                    <a-checkbox :value="col.value">{{ col.label }}</a-checkbox>
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
        <a-table size="small" :columns="tableColumns" :dataSource="tableData" :pagination="false" :rowKey="rowKey"
          :rowSelection="rowSelection" :expandable="{ defaultExpandAllRows: true }">
          <!-- 折叠箭头 + 父行序号：放在独立列中，不与词条列共用 -->
          <template #expandIcon="props">
            <span v-if="props.record.children != null && props.record.children.length > 0">
              <div v-if="props.expanded" style="display: inline-block; margin-right: 6px"
                @click="(e) => { props.onExpand(props.record, e); }">
                <CaretDownOutlined />
              </div>
              <div v-else style="display: inline-block; margin-right: 6px"
                @click="(e) => { props.onExpand(props.record, e); }">
                <CaretRightOutlined />
              </div>
              <span style="display:inline-block; min-width: 28px;">
                {{ parentIndexMap[props.record.groupId] || '' }}
              </span>
            </span>
            <span v-else style="margin-right:34px"></span>
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
        <a-button type="primary" :loading="detailLoading" @click="handleWriteBack">回写</a-button>
      </template>
    </CustomModal>
  </div>
</template>

<script>
import { h, resolveComponent } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SettingOutlined, DeleteOutlined, CaretDownOutlined, CaretRightOutlined } from '@ant-design/icons-vue'
import WriteBackButton from '@/components/Button/writeBackButton.vue'
import CustomModal from '@/components/modal/index.vue'

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
]

function randInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

function buildChildSignature(child) {
  return [
    child.entry || '',
    child.translateType || '',
    child.translation || '',
    child.entrySource || '',
    child.diFileName || '',
  ].join('|')
}

function buildMockValidationResult() {
  // 初始 mock：2~10 个重复组，每组子项 2~5 条（且至少包含重复子项）
  const now = Date.now()
  const groupCount = randInt(2, 10)
  const entryPool = ['SAVE', 'CANCEL', 'OK', 'NEXT', 'PREV', 'SUBMIT', 'SEARCH', 'DELETE', 'UPLOAD', 'DOWNLOAD']
  const translationPool = ['Save', 'Store', 'Cancel', 'Abort', 'OK', 'Next', 'Previous', 'Submit', 'Search', 'Delete']
  const sourcePool = ['ts', 'dictionary', 'file']
  const dictPool = ['common.dict', 'ui.dict', 'core.dict']

  let keySeq = 1000
  const groups = []

  for (let i = 0; i < groupCount; i += 1) {
    const entry = entryPool[i % entryPool.length]
    const childCount = randInt(2, 5)

    // 基准子项：后续会刻意复制它，制造“重复”
    const baseTranslation = translationPool[randInt(0, translationPool.length - 1)]
    const baseSource = sourcePool[randInt(0, sourcePool.length - 1)]
    const baseDi = baseSource === 'dictionary' ? dictPool[randInt(0, dictPool.length - 1)] : '-'

    const children = []
    const baseChild = {
      _type: 'child',
      id: `id-${keySeq++}`, // 模拟真实词条唯一ID
      realKey: null,
      entry,
      translateType: '英文',
      translation: baseTranslation,
      entrySource: baseSource,
      diFileName: baseDi,
    }
    children.push(baseChild)

    // 至少再加一条“重复 baseChild”的子项
    const dupChild = {
      ...baseChild,
      id: `id-${keySeq++}`,
    }
    children.push(dupChild)

    // 剩余子项：部分重复、部分不同，确保 2~5 之间
    while (children.length < childCount) {
      const makeDup = Math.random() < 0.5
      const child = makeDup
        ? { ...baseChild, id: `id-${keySeq++}` }
        : {
            _type: 'child',
            id: `id-${keySeq++}`,
            realKey: null,
            entry,
            translateType: '英文',
            translation: translationPool[randInt(0, translationPool.length - 1)],
            entrySource: sourcePool[randInt(0, sourcePool.length - 1)],
            diFileName: '-',
          }
      if (child.entrySource === 'dictionary') {
        child.diFileName = dictPool[randInt(0, dictPool.length - 1)]
      }
      children.push(child)
    }

    groups.push({
      _type: 'parent',
      groupId: `g-${i + 1}`,
      entry,
      tag: i % 2 === 0 ? 'core' : '-',
      comment: i % 2 === 0 ? 'from ts' : 'from file',
      children,
      updatedAt: now - 1000 * 60 * randInt(0, 30),
    })
  }

  return groups
}

function deepClone(obj) {
  return JSON.parse(JSON.stringify(obj))
}

export default {
  components: {
    WriteBackButton,
    CustomModal,
    SettingOutlined,
    DeleteOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
  },

  data() {
    return {
      // ==================== Mock 常量与初始数据 ====================
      langOptions,
      // ==================== 状态 ====================
      detailModalOpen: false,
      validateLoading: false,
      detailLoading: false,
      lastWriteBackForm: null, // 记录第一次回写模态框提交的表单（用于回写参数透传，如 writeType）
      // 列表勾选：仅与 UI 复选框联动
      selectedRowKeys: [],
      // 回写目标：真正要参与回写的“真实词条 id 集合”
      writeBackDataKeys: [],
      tableData: buildMockValidationResult(),
      mockDataSource: [],
      // 展示列配置（popover + checkboxGroup）
      visibleColumnKeys: [...DEFAULT_VISIBLE_COLUMNS],
    }
  },

  computed: {
    allColumnOptions() {
      return [
        { label: 'entry', value: 'entry' },
        { label: 'tag', value: 'tag' },
        { label: 'comment', value: 'comment' },
        { label: 'translateType', value: 'translateType' },
        { label: 'translation', value: 'translation' },
        { label: 'entrySource', value: 'entrySource' },
        { label: 'diFileName', value: 'diFileName' },
        { label: 'realKey', value: 'realKey' },
      ]
    },

    allChildRowKeys() {
      const keys = []
      ;(this.tableData || []).forEach((g) => {
        if (!Array.isArray(g.children)) return
        g.children.forEach((c) => {
          if (c && c.id) keys.push(c.id)
        })
      })
      return keys
    },

    // 父行序号：根据当前 tableData 顺序生成，供折叠箭头一同展示
    parentIndexMap() {
      const map = {}
      ;(this.tableData || []).forEach((g, index) => {
        if (Array.isArray(g.children) && g.groupId) {
          map[g.groupId] = index + 1
        }
      })
      return map
    },

    // ==================== 表格配置 ====================
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

    tableColumns() {
      const cols = []
      const visibleSet = new Set(this.visibleColumnKeys || [])
      const pushIfVisible = (key, title, customRender) => {
        if (!visibleSet.has(key)) return
        cols.push({
          title,
          dataIndex: key,
          key,
          ellipsis: true,
          customRender,
        })
      }

      pushIfVisible('entry', '词条')
      pushIfVisible('tag', 'tag', ({ record, text }) => (Array.isArray(record.children) ? (text || '-') : '-'))
      pushIfVisible('comment', 'comment', ({ record, text }) => (Array.isArray(record.children) ? (text || '-') : '-'))
      pushIfVisible('translateType', '语种', ({ record, text }) =>
        !Array.isArray(record.children) ? (text || '-') : '-'
      )
      pushIfVisible('translation', '翻译', ({ record, text }) =>
        !Array.isArray(record.children) ? (text || '-') : '-'
      )
      pushIfVisible('entrySource', '来源', ({ record, text }) =>
        !Array.isArray(record.children) ? (text || '-') : '-'
      )
      pushIfVisible('diFileName', '辞典', ({ record, text }) =>
        !Array.isArray(record.children) ? (text || '-') : '-'
      )
      pushIfVisible('realKey', '真实key', ({ record }) =>
        !Array.isArray(record.children) ? record.id || '-' : '-'
      )

      // 操作列：仅子行显示垃圾箱
      cols.push({
        title: '操作',
        key: 'actions',
        width: 80,
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
      })

      return cols
    },
  },

  watch: {
    selectedRowKeys: {
      handler(val) {
        console.log('selectedRowKeys 发生变化：', val)
      },
      deep: true,
    },
    tableData: {
      handler(val) {
        console.log('tableData 发生变化：', val)
      },
      deep: true,
    },
    writeBackDataKeys: {
      handler(val) {
        console.log('writeBackDataKeys 发生变化：', val)
      },
      deep: true,
    },
  },

  methods: {
    // ==================== 表格辅助 ====================
    rowKey(record) {
      if (Array.isArray(record.children)) return record.groupId
      return record.id
    },

    resetColumns() {
      this.visibleColumnKeys = [...DEFAULT_VISIBLE_COLUMNS]
    },

    isChildRecord(record) {
      return !Array.isArray(record.children)
    },

    // ==================== 交互：回写表单提交（业务回写模态框） ====================
    async handleWriteBackFormSubmit(payload) {
      this.lastWriteBackForm = payload && payload.writeBack ? payload.writeBack : null
      const needValidate = await new Promise((resolve) => {
        Modal.confirm({
          title: '提示',
          content: '是否需要校验？',
          okText: '是',
          cancelText: '否',
          onOk: () => resolve(true),
          onCancel: () => resolve(false),
        })
      })

      if (!needValidate) {
        message.success('已选择不校验（原型）：将直接执行回写')
        const writeType = (this.lastWriteBackForm && this.lastWriteBackForm.type) || 'DEFAUT'
        const allIds = this.allChildRowKeys.slice()
        if (!allIds.length) {
          message.warning('当前无可回写的数据（原型）')
          return
        }
        const targetSet = new Set(allIds)
        await this.handleWriteBackOK(targetSet, writeType)
        return
      }

      this.validateLoading = true
      setTimeout(() => {
        this.validateLoading = false
        this.detailModalOpen = true
        const allIds = this.allChildRowKeys.slice()
        this.selectedRowKeys = allIds
        this.writeBackDataKeys = allIds.slice()
        message.success('校验完成（原型），请在弹窗2处理重复组')
      }, 800)
    },

    // ==================== 交互：弹窗2（删除/重校验/回写） ====================
    handleSingleDelete(childRecord) {
      const group = this.tableData.find(
        (g) => Array.isArray(g.children) && g.children.some((c) => c.id === childRecord.id)
      )
      if (!group) return
      if (group.children.length === 1) {
        message.warning('该组必须至少保留一条')
        return
      }

      this.detailLoading = true
      setTimeout(() => {
        this.selectedRowKeys = this.selectedRowKeys.filter((k) => k !== childRecord.id)
        this.writeBackDataKeys = this.writeBackDataKeys.filter((k) => k !== childRecord.id)
        group.children = group.children.filter((c) => c.id !== childRecord.id)
        if (group.children.length === 1) {
          this.tableData = this.tableData.filter((g) => g.groupId !== group.groupId)
        }
        this.detailLoading = false
        message.success('删除成功（原型）')
      }, 800)
    },

    handleBatchDelete() {
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

      this.detailLoading = true
      setTimeout(() => {
        const nextGroups = []
        this.tableData.forEach((g) => {
          if (!Array.isArray(g.children)) return
          const nextChildren = g.children.filter((c) => !selectedSet.has(c.id))
          if (nextChildren.length <= 1) return
          nextGroups.push({ ...g, children: nextChildren })
        })
        this.tableData = nextGroups
        this.writeBackDataKeys = this.writeBackDataKeys.filter((id) => !selectedSet.has(id))
        this.selectedRowKeys = []
        this.detailLoading = false
        message.success('批量删除完成（原型）')
      }, 800)
    },

    invertSelectedRowKeys() {
      const allKeys = this.allChildRowKeys || []
      const selectedSet = new Set(this.selectedRowKeys || [])
      this.selectedRowKeys = allKeys.filter((k) => !selectedSet.has(k))
    },

    handleRevalidate() {
      this.detailLoading = true
      setTimeout(() => {
        const nextGroups = []
        ;(this.tableData || []).forEach((group) => {
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

        this.tableData = nextGroups
        this.selectedRowKeys = this.allChildRowKeys.slice()
        this.detailLoading = false
        message.success('重校验完成（原型）')
      }, 800)
    },

    async handleWriteBack() {
      if (!this.writeBackDataKeys || this.writeBackDataKeys.length === 0) {
        message.warning('当前无可回写的数据（原型）')
        return
      }

      const targetSet = new Set(this.writeBackDataKeys || [])
      const writeType = (this.lastWriteBackForm && this.lastWriteBackForm.type) || 'DEFAUT'

      if (this.tableData.length > 0) {
        Modal.confirm({
          title: '提示',
          content:
            '回写数据中含有重复数据，若不去重则无法保证哪个数据将被回写入文件，是否继续回写？',
          okText: '继续回写',
          cancelText: '取消',
          onOk: async () => {
            await this.handleWriteBackOK(targetSet, writeType)
          },
        })
      } else {
        await this.handleWriteBackOK(targetSet, writeType)
      }
    },

    async handleWriteBackOK(targetSet, writeType) {
      this.detailLoading = true
      try {
        const rows = []
        ;(this.tableData || []).forEach((g) => {
          if (!Array.isArray(g.children)) return
          g.children.forEach((c) => {
            if (!this.isChildRecord(c)) return
            if (!targetSet.has(c.id)) return
            rows.push(c)
          })
        })

        // 1) 回写
        await this.mockWriteBackApi({
          writeType,
          selectedKeys: Array.from(targetSet),
          rows,
        })
        message.success('回写完成（原型）!')

        // 2) 是否需要后续 git 提交/推送（由“回写后推送”勾选决定）
        const needPush = !!(this.lastWriteBackForm && this.lastWriteBackForm.needPush)
        if (needPush) {
          // 顺序：commit -> push
          await this.mockGitCommit()
          await this.mockGitPush()
        }
      } finally {
        this.detailLoading = false
        // 勾选 / 未勾选“回写后推送”都在全部流程结束后关闭弹窗
        this.detailModalOpen = false
      }
    },

    // mock 回写接口：仅用于验证“回写动作已触发”，不真正调用后端
    mockWriteBackApi(params) {
      console.log('【mockWriteBackApi】调用参数：', params)
      return new Promise((resolve) => {
        setTimeout(() => {
          console.log('【mockWriteBackApi】返回：success', {
            writeType: params.writeType,
            selectedCount: params.selectedKeys?.length || 0,
            rowCount: params.rows?.length || 0,
          })
          resolve({ success: true })
        }, 500)
      })
    },

    // mock git commit：仅模拟顺序调用，不真正调用后端
    mockGitCommit() {
      return new Promise((resolve) => {
        setTimeout(() => {
          message.success('commit完成（原型）!')
          resolve({ success: true })
        }, 400)
      })
    },

    // mock git push：仅模拟顺序调用，不真正调用后端
    mockGitPush() {
      return new Promise((resolve) => {
        setTimeout(() => {
          message.success('push完成（原型）!')
          resolve({ success: true })
        }, 400)
      })
    },

    resetAll() {
      this.selectedRowKeys = []
      this.writeBackDataKeys = []
      this.tableData = buildMockValidationResult()
      this.visibleColumnKeys = [...DEFAULT_VISIBLE_COLUMNS]
      message.success('已重置（原型）')
    },
  },

  mounted() {
    // 预留：如需在挂载后自动触发一次校验或日志，可在此扩展
  },
}
</script>
