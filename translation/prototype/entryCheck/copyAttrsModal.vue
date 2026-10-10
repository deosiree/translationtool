<template>
  <!--
    复制属性模态框（CopyAttrsModal）：
    - 专用于在版本之间复制词条属性（如：词性备注、备注、翻译最大长度）
    - 仅保留复制属性相关的交互，不再承载预翻译相关能力
  -->
  <Modal :visible="visible" :modalTitle="computedModalTitle" :modalWidth="'860px'" :okLoading="okLoading"
    @handleClose="handleClose" @handleOK="handleOKInternal">
    <div style="width: 100%; height: 100%;">
      <!-- 复制属性主体区域：左右 50% 容器 -->
      <div style="margin-top: 16px;">
        <a-row :gutter="16">
          <!-- 左侧：状态树（参考词条管理状态树样式） -->
          <a-col :span="12">
            <a-card size="small" title="产品状态树">
              <div class="productTree" style="max-height: 422px; overflow: auto;">
                <a-tree show-icon block-node :tree-data="entryTreeData" v-model:expandedKeys="expandedKeys"
                  :selectedKeys="selectedTreeKeys" @select="handleTreeSelect">
                  <template #title="{ title, type }">
                    <span v-if="type === 'common'" style="color: #001fb8">
                      {{ title }}
                    </span>
                    <span v-else-if="type === 'classify'" style="color: #7d7d7d">
                      {{ title }}
                    </span>
                    <span v-else-if="type === 'product'" style="color: #5ba584">
                      {{ title }}
                    </span>
                    <span v-else-if="type === 'module'" style="color: #a55b7c">
                      {{ title }}
                    </span>
                    <span v-else>{{ title }}</span>
                  </template>
                </a-tree>
                <span v-if="entryTreeData.length === 0" style="color: rgba(0, 0, 0, 0.4); margin-left: 40%;">
                  暂无数据（Mock）
                </span>
              </div>
              <div style="margin-top: 8px; font-size: 13px;">
                已选产品：
                <span v-if="selectedProduct">
                  {{ selectedProduct.title }}
                </span>
                <span v-else style="color: #999;">
                  请选择左侧树中的产品/模块节点
                </span>
              </div>
            </a-card>
          </a-col>

          <!-- 右侧：复制条件 -->
          <a-col :span="12">
            <a-card size="small" title="复制条件">
              <a-form layout="vertical" :model="entryManageForm">
                <a-form-item label="产品">
                  <span v-if="selectedProduct">
                    {{ selectedProduct.title }}
                  </span>
                  <span v-else style="color: #999;">
                    请先在左侧选择产品
                  </span>
                </a-form-item>

                <!-- 入参方式：全部词条 / 已选词条 -->
                <a-form-item label="入参方式">
                  <a-radio-group v-model:value="copyMode">
                    <a-radio value="all">全部词条</a-radio>
                    <a-radio :value="'selected'" :disabled="!hasSelectedEntries">已选词条</a-radio>
                  </a-radio-group>
                  <div style="margin-top: 4px; font-size: 12px; color: #999;">
                    全部词条：仅传 productID；已选词条：仅传选中词条列表。
                  </div>
                </a-form-item>

                <!-- 对照原属性：用于配置复制时的收敛条件（UI 侧提示，实际条件以后端为准） -->
                <a-row :gutter="8" align="middle">
                  <div class="inline-left-align">
                    <span style="margin-right: 8px;">对照原属性：</span>
                    <a-select v-model:value="entryManageForm.checkedAttributes" mode="multiple"
                      style="min-width: 220px;" placeholder="请选择对照原属性" @change="handleAttributesChange">
                      <a-select-option v-for="item in attributeOptions" :key="item.value" :value="item.value">
                        {{ item.label }}
                      </a-select-option>
                    </a-select>
                  </div>
                  <div style="margin-top: 4px; font-size: 12px; color: #999;">
                    对照原属性：用于在旧版本词条中筛选与当前词条在这些字段上取值完全相同的记录，作为复制源（可多选，至少保留一项）。
                  </div>
                </a-row>
                <!-- 复制目标属性 -->
                <a-row :gutter="8" align="middle" style="margin-top: 8px;">
                  <div class="inline-left-align">
                    <span style="margin-right: 8px;">复制目标属性：</span>
                    <a-select v-model:value="copyTargetAttributes" mode="multiple" style="min-width: 220px;"
                      placeholder="请选择复制目标属性">
                      <a-select-option v-for="item in copyTargetAttributeOptions" :key="item.value" :value="item.value">
                        {{ item.label }}
                      </a-select-option>
                    </a-select>
                  </div>
                  <div style="margin-top: 4px; font-size: 12px; color: #999;">
                    支持复制：词性备注、备注、翻译最大长度（可多选，至少选择一项）。
                  </div>
                </a-row>
              </a-form>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <a-alert style="margin-top: 16px;" type="info" show-icon message="复制属性说明">
        <template #description>
          <div style="font-size: 13px; line-height: 1.6;">
            将根据收敛条件在旧版本中查找与当前词条完全匹配的词条，
            并将选中的复制目标属性（词性备注、备注、翻译最大长度）一并复制到当前版本的对应词条中。
            若同一个目标词条命中多个源词条，应由后端返回错误信息提醒处理。
          </div>
        </template>
      </a-alert>
    </div>
  </Modal>
</template>

<script>
import Modal from '@/components/modal/index.vue'
import { getClassTree } from '@/http/api/entryManage'
import { message } from 'ant-design-vue'

export default {
  name: 'CopyAttrsModal',
  components: {
    Modal,
  },
  emits: ['handleClose', 'handleOK'],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
      default: '复制属性',
    },
    // 所有（候选）词条数据，用于 mock 统计“全部词条”场景下的影响数量
    dataPreTranslate: {
      type: Array,
      default: () => [],
    },
    // 当前产品/模块（用于复制属性模式下的 productID）
    currentProduct: {
      type: Object,
      default: () => ({}),
    },
    // 已选词条列表（用于复制属性模式下的 “已选词条” 入参）
    selectedEntries: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      okLoading: false,
      // ==================== 词条管理：状态树数据（真实接口） ====================
      entryTreeData: [],
      expandedKeys: [],
      selectedTreeKeys: [],
      selectedProduct: null, // 选中的产品/模块节点（完整对象）

      // ==================== 词条管理：翻译条件与用户偏好 ====================
      attributeOptions: [
        { label: '修改时间', value: 'updatedAt' },
        { label: '词条', value: 'entry' },
        { label: 'comment', value: 'comment' },
        { label: '英文', value: 'english' },
        { label: '中文翻译', value: 'chinese' },
      ],
      entryManageForm: {
        // 对照原属性：默认选中「词条」「comment」，允许用户修改，但至少保留 1 项
        checkedAttributes: ['entry', 'comment'],
        // 推荐排序：目前只有“最新修改时间优先”，仍保留字段以便后续扩展
        sortStrategy: 'latestUpdated',
        // 推荐时间周期
        useTimeRange: false,
        startTime: null,
        endTime: null,
      },
      treeLoaded: false,
      // ==================== 复制属性：入参方式与目标属性 ====================
      copyMode: 'all', // all | selected
      copyTargetAttributes: [],
      copyTargetAttributeOptions: [
        { label: '词性备注', value: 'partOfSpeech', index: 41 },
        { label: '备注', value: 'remark', index: 28 },
        { label: '翻译最大长度', value: 'maxLength', index: 47 },
      ],
    }
  },
  computed: {
    // 模态框标题
    computedModalTitle() {
      return this.modalTitle || '复制属性'
    },
    // 是否有已选词条
    hasSelectedEntries() {
      return Array.isArray(this.selectedEntries) && this.selectedEntries.length > 0
    },
  },
  methods: {
    // ==================== UI交互控制 ====================
    handleClose() {
      this.$emit('handleClose')
    },
    async handleOKInternal() {
      // 复制属性模式：仅进行复制相关校验与执行（当前为 mock）
      if (!this.copyTargetAttributes || this.copyTargetAttributes.length === 0) {
        message.warning('请至少选择一个复制目标属性！')
        return
      }
      if (this.copyMode === 'selected' && !this.hasSelectedEntries) {
        message.warning('当前无已选词条，请切换为“全部词条”或先在列表中选择词条。')
        return
      }

      this.okLoading = true
      try {
        const payload = this.buildCopyAttributesPayload()
        const result = await this.mockCopyAttributes(payload)
        this.$emit('handleOK', {
          success: true,
          data: result,
          payload,
        })
      } catch (err) {
        message.error('复制属性失败！' + (err && err.message ? err.message : ''))
        this.$emit('handleOK', {
          success: false,
          error: err && err.message ? err.message : '复制属性失败',
        })
      } finally {
        this.okLoading = false
      }
    },

    /**
     * 构建复制属性的请求入参（mock）
     */
    buildCopyAttributesPayload() {
      const product = this.currentProduct || {}
      const isModule = product.type === 'module'
      const productID = isModule ? product.parentId : product.key
      const entryIds = Array.isArray(this.selectedEntries)
        ? this.selectedEntries.map((e) => e.id).filter(Boolean)
        : []

      const payload = {
        mode: this.copyMode, // all | selected
        copyTargetAttributes: this.copyTargetAttributes.slice(),
        conditions: ['entry', 'tag', 'comment', 'entrySource', 'diFileName', 'writeBackType'],
      }

      if (this.copyMode === 'all') {
        payload.productID = productID
      } else if (this.copyMode === 'selected') {
        payload.entryIds = entryIds
      }

      return payload
    },

    /**
     * Mock 复制属性 API
     * 仅用于前端联调阶段，模拟一次复制任务执行结果
     */
    async mockCopyAttributes(payload) {
      // 模拟 API 延迟
      await new Promise((resolve) => setTimeout(resolve, 600))
      const total = this.copyMode === 'selected'
        ? (payload.entryIds ? payload.entryIds.length : 0)
        : (this.dataPreTranslate ? this.dataPreTranslate.length : 0)

      return {
        totalCount: total,
        successCount: total,
        conflictCount: 0,
        conflicts: [],
        copyTargetAttributes: payload.copyTargetAttributes,
        mode: payload.mode,
      }
    },

    // ==================== 词条管理：状态树选择 ====================
    handleTreeSelect(keys, info) {
      this.selectedTreeKeys = keys
      const node = info && info.node
      if (!node) return
      // 当节点类型为 product / module / classify 时，均认为是可用的筛选范围
      if (node.type === 'product' || node.type === 'module' || node.type === 'classify') {
        this.selectedProduct = {
          key: node.key,
          title: node.title,
          type: node.type,
          ...node,
        }
      }
    },

    // ==================== 词条管理：条件选择 + 用户偏好 ====================
    handleAttributesChange(nextChecked) {
      // 至少保留一项：若清空则恢复为默认 ['entry', 'comment']
      let next = Array.isArray(nextChecked) ? Array.from(new Set(nextChecked)) : []
      if (next.length === 0) {
        next = ['entry', 'comment']
      }
      this.entryManageForm.checkedAttributes = next
      this.savePreference()
    },
    savePreference() {
      try {
        const data = {
          checkedAttributes: this.entryManageForm.checkedAttributes,
        }
        localStorage.setItem('copyAttrs-entryManagePref', JSON.stringify(data))
      } catch (e) {
        // 忽略本地存储异常，避免影响主流程
      }
    },
    loadAttributePreference() {
      try {
        const stored = localStorage.getItem('copyAttrs-entryManagePref')
        if (!stored) return
        const parsed = JSON.parse(stored)
        const list = Array.isArray(parsed.checkedAttributes)
          ? parsed.checkedAttributes
          : []
        const valid = list.filter((v) =>
          this.attributeOptions.some((opt) => opt.value === v),
        )
        // 至少保留一项
        this.entryManageForm.checkedAttributes =
          valid.length > 0 ? Array.from(new Set(valid)) : ['entry', 'comment']
      } catch (e) {
        // 无需额外处理，使用默认配置
      }
    },

    // ==================== 词条管理：加载真实状态树 ====================
    async loadClassTree() {
      if (this.treeLoaded) return
      try {
        const res = await getClassTree({
          department: '',
          className: '',
        })
        const data = res && (res.data || res.result || res)
        const tree = data && (data.data || data.classTree || data.list || [])
        this.entryTreeData = Array.isArray(tree) ? tree : []
        // 默认展开第一层
        if (this.entryTreeData.length > 0 && this.entryTreeData[0].key) {
          this.expandedKeys = [this.entryTreeData[0].key]
        }
        this.treeLoaded = true
      } catch (e) {
        // eslint-disable-next-line no-console
        console.error('[CopyAttrsModal] 加载状态树失败', e)
      }
    },
  },
  mounted() {
    this.$nextTick(() => {
      this.loadClassTree()
    })
  },
  created() {
    // 初始化“对照原属性”的用户偏好
    this.loadAttributePreference()
  },

  watch: {
    // 根据已选词条数量自动设置默认入参方式：
    // - 无选中词条：默认“全部词条”
    // - 有选中词条：默认“已选词条”
    selectedEntries: {
      immediate: true,
      handler(val) {
        const hasSelected = Array.isArray(val) && val.length > 0
        this.copyMode = hasSelected ? 'selected' : 'all'
      },
    },
  },
}
</script>
