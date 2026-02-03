<template>
  <!--
    占位版预翻译模态框：
    - 先提供最小可运行骨架（优先级下拉 + 确认/取消）
    - 后续会从 prototype/preTranslate/translateModal.vue 中抽取真实预翻译表单与逻辑
  -->
  <Modal :visible="visible" :modalTitle="modalTitle" :modalWidth="'860px'" :okLoading="okLoading"
    @handleClose="handleClose" @handleOK="handleOKInternal">
    <div style="width: 100%; height: 100%;">
      <a-form name="pre-translate-form" autocomplete="off" :label-col="{ style: { width: '88px' } }" :model="formState">
        <a-form-item label="任务名称">
          <span>{{ currentTask && currentTask.name }}</span>
        </a-form-item>

        <a-form-item label="优先级" name="priority" :rules="[{ required: true, message: '请选择优先级!' }]">
          <a-select v-model:value="formState.priority" placeholder="请选择" allowClear>
            <!-- 新需求：词条管理优先级 -->
            <a-select-option value="entry-manage">词条管理</a-select-option>
            <a-select-option value="shuyuku">术语库</a-select-option>
            <a-select-option value="deepl">DeepL翻译</a-select-option>
            <a-select-option value="youdao">有道翻译</a-select-option>
            <a-select-option value="baidu">百度翻译</a-select-option>
            <a-select-option value="google">Google翻译</a-select-option>
            <a-select-option value="module">本地模型</a-select-option>
            <a-select-option value="synthesis">
              综合优先级
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>

      <!-- 词条管理优先级：从第二行开始显示左右 50% 容器 -->
      <div v-if="formState.priority === 'entry-manage'" style="margin-top: 16px;">
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

          <!-- 右侧：翻译条件 -->
          <a-col :span="12">
            <a-card size="small" title="翻译条件">
              <a-form layout="vertical" :model="entryManageForm">
                <a-form-item label="产品">
                  <span v-if="selectedProduct">
                    {{ selectedProduct.title }}
                  </span>
                  <span v-else style="color: #999;">
                    请先在左侧选择产品
                  </span>
                </a-form-item>

                <a-form-item label="推荐排序">
                  <a-select v-model:value="entryManageForm.sortStrategy" style="min-width: 220px;"
                    @change="savePreference">
                    <a-select-option value="latestUpdated">
                      最新修改时间优先
                    </a-select-option>
                  </a-select>
                </a-form-item>

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
                    对照原属性：先根据这些属性在历史词条中筛选出与当前预翻译词条在所有选中属性上取值完全相同的词条，再从匹配结果中选择一个候选翻译推荐给你（可多选，至少保留一项）。
                  </div>
                </a-row>
                <a-row :gutter="8" align="middle" style="margin-top: 8px;">
                  <div class="inline-left-align">
                    <span style="margin-right: 8px;">翻译目标属性：</span>
                    <span>{{ targetLanguageLabel }}</span>
                  </div>
                </a-row>

                <a-form-item>
                  <div class="inline-left-align" style="margin-top: 8px;">
                    <a-checkbox v-model:checked="entryManageForm.useTimeRange" @change="savePreference">
                      推荐时间周期
                    </a-checkbox>
                    <template v-if="entryManageForm.useTimeRange">
                      <a-row :gutter="8" align="middle" style="margin-top: 8px;">
                        <span style="margin-left: 12px; margin-right: 4px;">开始时间：</span>
                        <a-date-picker v-model:value="entryManageForm.startTime" style="width: 140px;"
                          @change="savePreference" />
                      </a-row>
                      <a-row :gutter="8" align="middle" style="margin-top: 8px;">
                        <span style="margin-left: 12px; margin-right: 4px;">结束时间：</span>
                        <a-date-picker v-model:value="entryManageForm.endTime" style="width: 140px;"
                          @change="savePreference" />
                      </a-row>
                    </template>
                  </div>
                </a-form-item>
              </a-form>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <a-alert style="margin-top: 16px;" type="info" show-icon message="说明">
        <template #description>
          <div style="font-size: 13px; line-height: 1.6;">
            当优先级选择为「词条管理」时，会根据左侧选中的产品，结合「对照原属性」中的条件，
            在指定产品的词条中查找最近修改的匹配术语，并回填到当前任务的翻译目标属性中。
          </div>
        </template>
      </a-alert>
    </div>
  </Modal>
</template>

<script>
import Modal from '@/components/modal/index.vue'
import { getClassTree } from '@/http/api/entryManage'

export default {
  name: 'PreTranslateModal',
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
      default: '预翻译',
    },
    currentTask: {
      type: Object,
      default: () => ({}),
    },
  },
  data() {
    return {
      okLoading: false,
      formState: {
        priority: null,
      },
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
    }
  },
  computed: {
    // 翻译目标属性标签：与任务翻译语种对应，例如「中文-俄文」取右侧「俄文」
    targetLanguageLabel() {
      const type = this.currentTask && this.currentTask.translateType
      if (!type || typeof type !== 'string') return ''
      const parts = type.split('-')
      return parts[1] || type
    },
  },
  methods: {
    // ==================== UI交互控制 ====================
    handleClose() {
      this.$emit('handleClose')
    },
    handleOKInternal() {
      // 这里先不接入真实校验逻辑，仅模拟一次 loading + 事件回调
      this.okLoading = true
      setTimeout(() => {
        this.okLoading = false
        this.$emit('handleOK', {
          priority: this.formState.priority,
          taskId: this.currentTask && this.currentTask.id,
          // 词条管理模式下，额外返回产品与条件配置，便于后续对接真实预翻译逻辑
          mode: this.formState.priority === 'entry-manage' ? 'entry-manage' : 'engine',
          entryManageConfig:
            this.formState.priority === 'entry-manage'
              ? {
                product: this.selectedProduct,
                attributes: this.entryManageForm.checkedAttributes.slice(),
                targetLang: this.targetLanguageLabel,
                sortStrategy: this.entryManageForm.sortStrategy,
                timeRange: this.entryManageForm.useTimeRange
                  ? {
                    start: this.entryManageForm.startTime,
                    end: this.entryManageForm.endTime,
                  }
                  : null,
              }
              : null,
        })
      }, 600)
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
          sortStrategy: this.entryManageForm.sortStrategy,
          useTimeRange: this.entryManageForm.useTimeRange,
          startTime: this.entryManageForm.startTime,
          endTime: this.entryManageForm.endTime,
        }
        localStorage.setItem('preTranslate-entryManagePref', JSON.stringify(data))
      } catch (e) {
        // 忽略本地存储异常，避免影响主流程
      }
    },
    loadAttributePreference() {
      try {
        const stored = localStorage.getItem('preTranslate-entryManagePref')
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
        if (parsed.sortStrategy) {
          this.entryManageForm.sortStrategy = parsed.sortStrategy
        }
        if (typeof parsed.useTimeRange === 'boolean') {
          this.entryManageForm.useTimeRange = parsed.useTimeRange
        }
        if (parsed.startTime) {
          this.entryManageForm.startTime = parsed.startTime
        }
        if (parsed.endTime) {
          this.entryManageForm.endTime = parsed.endTime
        }
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
        console.error('[PreTranslateModal] 加载状态树失败', e)
      }
    },
  },

  created() {
    // 初始化“对照原属性”的用户偏好
    this.loadAttributePreference()
  },

  watch: {
    'formState.priority'(val) {
      if (val === 'entry-manage') {
        this.loadClassTree()
      }
    },
  },
}
</script>
