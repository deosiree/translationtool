<template>
  <!-- <a-spin :spinning="visible"> -->
  <Modal
    ref="createBranchBox"
    :modalWidth="modalWidth"
    :visible="visible"
    :createBranchClassfyID="createBranchClassfyID"
    :modalTitle="modalTitle"
    @handleClose="handleClose"
    @handleOK="handleOK"
  >
    <div class="content">
      <div class="table">
        <a-form ref="params" name="custom-validation">
          <a-form-item label="IP" name="ip">
            <a-select
              v-model:value="ip"
              :options="ipOptions"
              placeholder="请选择IP"
              allowClear
            ></a-select>
          </a-form-item>
          <a-form-item label="分支名" name="codeBranch">
            <a-input
              v-model:value="codeBranch"
              placeholder="请输入分支名"
            ></a-input>
          </a-form-item>
          <a-form-item label="导入语种" name="translateTypes">
            <!-- 修改为多选 -->
            <a-select
              mode="multiple"
              v-model:value="translateTypes"
              :options="langOptions"
              placeholder="请输入各任务需要的导入翻译语种"
              @change="languageChange"
              allowClear
            ></a-select>
          </a-form-item>
        </a-form>
        <a-table
          class="ant-table-striped"
          :columns="columns"
          :dataSource="dataSource"
          :scroll="{ x: '100%', y: '280px' }"
          :row-class-name="
            (_record, index) => (index % 2 === 1 ? 'table-striped' : null)
          "
          ref="createBranchTable"
          bordered
          :pagination="pagination"
          :loading="loading"
          :customRow="customRow"
        >
          <template #bodyCell="{ column, text, record }">
            <template
              v-if="['name', 'title', 'versionName'].includes(column.dataIndex)"
            >
              <template v-if="editableData[record.id]">
                <a-input
                  @click="clickInput"
                  v-model:value="editableData[record.id][column.dataIndex]"
                  style="margin: -5px 0"
                  @pressEnter="save(record.id)"
                />
              </template>
              <template v-else>
                {{ text }}
              </template>
            </template>
            <template
              v-else-if="
                [
                  'developer',
                  'entryAuditor',
                  'translator',
                  'translationAuditor',
                  'translateType',
                  'creator',
                ].includes(column.dataIndex)
              "
            >
              <template v-if="editableData[record.id]">
                <a-select
                  v-model:value="editableData[record.id][column.dataIndex]"
                  style="width: 100%"
                  placeholder="请选择"
                  :options="options[record.id][column.dataIndex]"
                  @click="clickInput"
                  allowClear
                >
                </a-select>
              </template>
              <template v-else>
                {{ text }}
              </template>
            </template>
            <template v-else-if="column.dataIndex === 'operation'">
              <span v-if="editableData[record.id]">
                <div
                  style="
                    display: flex;
                    gap: 8px;
                    align-items: center;
                    justify-self: center;
                  "
                >
                  <a-button
                    type="primary"
                    ghost
                    size="small"
                    @click.stop="save(record.id)"
                    >保存</a-button
                  >
                  <a-button
                    type="primary"
                    ghost
                    size="small"
                    danger
                    @click.stop="cancel(record.id)"
                    >取消</a-button
                  >
                </div>
              </span>
              <span v-else>
                <a-button
                  type="primary"
                  ghost
                  size="small"
                  @click.stop="edit(record)"
                  >编辑</a-button
                >
              </span>
            </template>
          </template>
        </a-table>
      </div>
    </div>
    <div class="other" style="padding: 12px; background-color: #dbdbdb; display: flex; flex-direction: column; gap: 12px">
      <a-alert 
        message="重要提示" 
        description="分支新建后的 ts 支持更新，其他类型的文件需重走一遍工作台（使用分支新建导入进来的词条，词条来源与工作台的不一样）" 
        type="warning" 
        show-icon 
      />
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>需要忽略的文件(不处理翻译，不创建任务，不导入词条)：</span>
        <div style="display: flex; gap: 8px">
          <AnalysisButton
            size="small"
            buttonTitle="读取配置"
            @configList="createOtherProductByAnalysis"
          />
          <!-- <ImportButton
            size="small"
            buttonTitle="配置新增"
            @configList="createOtherProduct"
          /> -->
          <!-- <a-button type="primary" size="small" @click="addIgnore">
            <template #icon>
              <PlusOutlined />
            </template>新增
          </a-button> -->
          <a-button type="primary" size="small" @click="otherDelete" danger>
            <template #icon> <DeleteOutlined /> </template>删除
          </a-button>
        </div>
      </div>
      <div
        class="otherSearchBox"
        style="display: flex; gap: 8px; align-items: center"
      >
        <div ref="otherSearch" style="display: flex; gap: 8px; align-items: center">
          <a-form
            ref="otherForm"
            name="custom-validation"
            layout="inline"
            autocomplete="off"
          >
            <!-- <a-form-item label="子分类名" name="sub-classify">
                <a-input v-model:value="codeBranch" placeholder="所有其他文件将存入子分类中" :rules="[{ required: true, message: '请输入子分类名，所有其他文件将存入子分类中' }]"
                  style="width: 400px;"></a-input>
              </a-form-item> -->
          </a-form>
        </div>
      </div>
      <div class="table" style="display: flex; gap: 8px">
        <div style="width: 100%">
          <a-table
            ref="otherTable"
            bordered
            class="ant-table-striped"
            :columns="otherColumns"
            :customRow="otherCustomRow"
            :dataSource="otherDataSource"
            :loading="otherLoading"
            :scroll="{ x: '100%', y: '180px' }"
            :pagination="otherPagination"
            :row-key="(record) => record.id"
            :row-class-name="
              (_record, index) => (index % 2 === 1 ? 'table-striped' : null)
            "
            :row-selection="{
              selectedRowKeys: otherSelectedRowKeys,
              onSelect: otherOnSelect,
              onSelectAll: otherOnSelectAll,
              onChange: otherOnSelectChange,
              selections: [
                {
                  key: 'selectAll',
                  text: '全部选择',
                  onSelect: otherSelectAllEntry,
                },
                {
                  key: 'clearAll',
                  text: '取消选择',
                  onSelect: otherClearAllEntry,
                },
              ],
            }"
            @resizeColumn="handleResizeColumn"
          >
            <template #bodyCell="{ column, text, record }">
              <!-- <template v-if="['name','parentTitle','subTitle','title', 'versionName'].includes(column.dataIndex)">
                <template v-if="otherEditableData[record.id]">
                  <a-input @click="clickInput" v-model:value="otherEditableData[record.id][column.dataIndex]" style="margin: -5px 0"
                    @pressEnter="otherSave(record.id)" />
                </template>
                <template v-else>
                  {{ text }}
                </template>
              </template> -->
              <template v-if="['link', 'title'].includes(column.dataIndex)">
                <template v-if="otherEditableData[record.id]">
                  <template v-if="record.isNew && column.dataIndex == 'link'">
                    <a-select
                      style="width: 100%"
                      placeholder="请选择lang目录"
                      allowClear
                      :options="linkOptions"
                      @pressEnter="otherSave(record.id)"
                      @change="getIgnoreOptions(record)"
                      v-model:value="
                        otherEditableData[record.id][column.dataIndex]
                      "
                    ></a-select>
                  </template>
                  <template v-if="column.dataIndex == 'title'">
                    <a-select
                      style="width: 100%"
                      placeholder="请选择要忽略的文件"
                      allowClear
                      :options="ignoreOptionsMap[record.id] || []"
                      @pressEnter="otherSave(record.id)"
                      v-model:value="
                        otherEditableData[record.id][column.dataIndex]
                      "
                    ></a-select>
                  </template>
                </template>
                <template v-else>
                  {{ text }}
                </template>
              </template>
              <template v-else-if="column.dataIndex === 'operation'">
                <span v-if="otherEditableData[record.id]">
                  <div
                    style="
                      display: flex;
                      gap: 8px;
                      align-items: center;
                      justify-self: center;
                    "
                  >
                    <a-button
                      type="primary"
                      ghost
                      size="small"
                      @click.stop="otherSave(record.id)"
                      >保存</a-button
                    >
                    <a-button
                      type="primary"
                      ghost
                      size="small"
                      danger
                      @click.stop="otherCancel(record.id)"
                      >取消</a-button
                    >
                  </div>
                </span>
                <span v-else>
                  <div
                    style="
                      display: flex;
                      gap: 8px;
                      align-items: center;
                      justify-self: center;
                    "
                  >
                    <a-button
                      type="primary"
                      ghost
                      size="small"
                      @click.stop="otherEdit(record)"
                      >编辑</a-button
                    >
                    <!-- <a-button type="primary" ghost size="small" @click.stop="showSource(record)">词条来源</a-button> -->
                    <!-- <div class="editable-row-operations">
                    <DeleteOutlined style="color:#ff7070;font-size:16px" title="取消选择" />
                  </div> -->
                  </div>
                </span>
              </template>
            </template>
          </a-table>
        </div>
        <!-- <div style="width: 20%;">
          <a-table ref="otherSourceTable" bordered class="ant-table-striped" :columns="otherSourceColumns" :dataSource="otherSourceDataSource"
            :scroll="{x:'100%' , y: '180px'}" :pagination="false" @resizeColumn="handleResizeColumn">
          </a-table>
        </div> -->
      </div>
    </div>
  </Modal>
  <!-- </a-spin> -->
</template>
<script>
import Modal from "@/components/modal/index.vue";
import VersionModal from "@/views/task/versionModal.vue";
import ImportButton from "@/components/Button/codeBranch/configImportButton.vue";
import AnalysisButton from "@/components/Button/codeBranch/configAnalysisButton.vue";
import { message } from "ant-design-vue";
import { getI18nAdress } from "@/http/api/workbench";
import { getRoleUserByDepartment } from "@/http/api/user";
import { createProductByLang } from "@/http/api/entryManage";
import { createVersion } from "@/http/api/productVersion";
import { createTaskByLang } from "@/http/api/task";
import { deleteEntryClassfy, getSourceByLang } from "@/http/api/entryManage";
import commonParam, { createBranchParams } from "@/constants/commonParam";
import { v4 as uuidv4 } from "uuid";
import { setModalAriaHidden } from "@/utils/domUtils";
import { randomError } from "@/utils/testUtils";
import { cloneDeep } from "lodash-es";
import { PlusOutlined, DeleteOutlined } from "@ant-design/icons-vue";
export default {
  components: {
    Modal,
    VersionModal,
    ImportButton,
    AnalysisButton,
    PlusOutlined,
    DeleteOutlined,
  },
  emits: ["createBranchClose"],
  props: {
    // 传递来的数据放这儿，不能再在data中定义了
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
    },
    createBranchClassfyID: {
      type: String,
    },
    treeNode: {
      type: Object,
    },
  },
  data() {
    const cachedLanguages = localStorage.getItem("createBranchLanguages");
    return {
      modalWidth: "1000px",
      ip: null,
      codeBranch: "",
      translateTypes: cachedLanguages
        ? JSON.parse(cachedLanguages)
        : commonParam.langNameList,
      ipOptions: [], // ip下拉选项
      options: {}, // 编辑态时的下拉选项
      langOptions: Object.values(commonParam.languageMap).map((lang) => ({
        label: lang.name,
        value: lang.name,
      })),
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 50,
          customRender: (text) => {
            const currentIndex =
              text.index +
              1 +
              this.pagination.pageSize * (this.pagination.current - 1);
            return currentIndex;
          },
          fixed: "left",
        },
        {
          title: "lang目录",
          dataIndex: "link",
          align: "center",
          width: 80,
          fixed: "left",
          resizable: true,
        },
        {
          title: "产品名称",
          dataIndex: "title",
          align: "center",
          width: 150,
          resizable: true,
        },
        {
          title: "任务名称",
          dataIndex: "name",
          align: "center",
          width: 150,
          resizable: true,
        },
        {
          title: "开发员",
          dataIndex: "developer",
          align: "center",
          width: 100,
        },
        {
          title: "词条审核员",
          dataIndex: "entryAuditor",
          align: "center",
          width: 100,
        },
        {
          title: "翻译员",
          dataIndex: "translator",
          align: "center",
          width: 100,
        },
        {
          title: "翻译审核员",
          dataIndex: "translationAuditor",
          align: "center",
          width: 100,
        },
        {
          title: "任务管理员",
          dataIndex: "creator",
          align: "center",
          width: 100,
        },
        {
          title: "翻译语种",
          dataIndex: "translateType",
          align: "center",
          width: 80,
        },
        {
          title: "产品版本",
          dataIndex: "versionName",
          align: "center",
          width: 80,
          resizable: true,
        },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 100,
          fixed: "right",
        },
      ],
      linkList: createBranchParams.linkList,
      linkOptions: createBranchParams.linkOptions,
      ignoreOptionsMap: {}, // 存储每个record的忽略文件选项
      dataSource: [], // 展示列=任务信息+产品信息
      taskSource: [],
      productSource: [],
      editableData: {},
      loading: false,
      pagination: {
        showSizeChanger: true,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      otherColumns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 50,
          customRender: (text) => {
            const currentIndex =
              text.index +
              1 +
              this.otherPagination.pageSize *
                (this.otherPagination.current - 1);
            return currentIndex;
          },
          fixed: "left",
        },
        {
          title: "lang目录",
          dataIndex: "link",
          align: "center",
          width: 80,
          fixed: "left",
          resizable: true,
        },
        // {
        //   title: "分类名称",
        //   dataIndex: "parentTitle",
        //   align: "center",
        //   width: 150,
        //   resizable: true,
        // },
        // {
        //   title: "子分类名称",
        //   dataIndex: "subTitle",
        //   align: "center",
        //   width: 150,
        //   resizable: true,
        // },
        {
          title: "词条来源",
          // title: "产品名称",
          dataIndex: "title",
          align: "center",
          width: 150,
          resizable: true,
        },
        // {
        //   title: "产品版本",
        //   dataIndex: "versionName",
        //   align: "center",
        //   width: 80,
        //   resizable: true,
        // },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 100,
          fixed: "right",
        },
      ],
      otherDataSource: [],
      otherParentSource: [], // 分类名称（存储createProductByLang入参）
      otherSubSource: [], // 子分类名称（分类-子分类-产品，作为被忽略文件）
      otherParents: new Map(), // 分类名称
      otherSubs: new Map(), // 子分类名称（分类-子分类-产品，作为被忽略文件）
      otherSelectedRows: [],
      otherSelectedRowKeys: [],
      otherSelectedRowIndex: null,
      otherSelectEntry: new Map(), // 已选任务
      otherPagination: {
        showSizeChanger: true,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.otherPageChange,
      },
      otherEditableData: {},
      otherLoading: false,
      otherSourceColumns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 50,
          customRender: (text) => {
            return text.index + 1;
          },
          resizable: true,
        },
        {
          title: "词条来源",
          dataIndex: "file",
          align: "center",
          width: 150,
          resizable: true,
        },
      ],
      otherSourceDataSource: [],
    };
  },
  mounted() {
    this.$nextTick(() => {
      this.user = this.$store.state.user;
      this.getIPs();
    });
  },
  watch: {
    visible: {
      deep: true,
      handler(newVal) {
        if (newVal) {
          // console.log("treeNode", this.treeNode);
          // console.log("visible changed", newVal);
          this.codeBranch = this.treeNode.title;
          this.createTask();
          // this.createOtherProduct(); // 根据默认配置创建特殊处理的lang产品
        }
      },
    },
  },
  methods: {
    init() {
      this.i18nURL = null;
      this.dataSource = [];
      this.taskSource = [];
      this.productSource = [];
      this.otherDataSource = [];
    },
    // 获取i18服务器ip
    getIPs() {
      this.ipOptions = [];
      getI18nAdress().then((res) => {
        res.data?.list?.forEach((item) => {
          let ip = {
            label: item.ip,
            value: item.ip,
          };
          this.ipOptions.push(ip);
        });
      });
    },
    // 回写语种change事件
    languageChange() {
      // // 遍历执行xxx
      // this.translateTypes.forEach((language) => {
      //   this.getTsFile(language);
      // });

      // 保存用户偏好到本地缓存
      localStorage.setItem(
        "createBranchLanguages",
        JSON.stringify(this.translateTypes)
      );
    },
    // 根据默认配置创建lang任务
    createTask() {
      const newProduct = {
        parentId: this.treeNode.key,
        codeBranch: this.codeBranch,
      };
      const defaultUser = this.$store.state.user?.userName;
      const newTask = {
        state: "0",
        department: this.$store.state.user?.department,
        creator: defaultUser, // 任务管理员-创建人-归档
        developer: defaultUser, // 开发员
        entryAuditor: defaultUser, // 词条审核员
        translator: defaultUser, // 翻译员
        translationAuditor: defaultUser, // 翻译审核员
        translateType: "英文",
        versionId: null,
        versionName: null,
      };
      for (let i = 0; i < this.linkList.length; i++) {
        const product = {
          ...newProduct,
          title: this.linkList[i][1],
        };
        const task = {
          ...newTask,
          name: this.linkList[i][1],
          id: `createBranch${i}`,
          ignore: [],
        };
        this.taskSource.push(task);
        this.productSource.push(product);
        this.dataSource.push({
          ...product,
          ...task,
          link: this.linkList[i][0],
          index: i + 1,
        });
      }
    },
    // 添加表格行点击事件
    customRow(record, index) {
      return {
        onDblclick: (event) => {
          // clearTimeout(this.timer)
          // this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }

          this.edit(record);
        },
      };
    },
    // 编辑
    edit(record) {
      // 获取选择菜单数据
      this.getOptions(record);

      this.editableData[record.id] = cloneDeep(
        this.dataSource.filter((item) => record.id === item.id)[0]
      );
    },
    // 获取可编辑行下拉菜单的选项
    getOptions(record) {
      this.options[record.id] = {
        translateType: commonParam.langNameList.map((item) => ({
          label: item,
          value: item,
        })),
      };
      // 获取部门下的 开发员、词条审核员、翻译员、翻译审核员
      let params = {
        department: record.department,
      };
      getRoleUserByDepartment(params).then((res) => {
        let data = res.data;
        // console.log("getOptions", data);
        if (data.DEVELOPER) {
          let developer = [];
          data.DEVELOPER.forEach((item) => {
            let op = {
              label: item.userName,
              value: item.userName,
            };
            developer.push(op);
          });
          developer.push({ label: "无", value: "" });
          this.options[record.id].developer = developer;
          this.options[record.id].creator = developer; // 任务管理员-创建人-归档，没有data.CREATOR，所以这边我借用了开发者的options
        }
        if (data.ENTRY_AUDITOR) {
          let entryAuditor = [];
          data.ENTRY_AUDITOR.forEach((item) => {
            let op = {
              label: item.userName,
              value: item.userName,
            };
            entryAuditor.push(op);
          });
          entryAuditor.push({ label: "无", value: "" });
          this.options[record.id].entryAuditor = entryAuditor;
        }
        if (data.TRANSLATOR) {
          let translateor = [];
          data.TRANSLATOR.forEach((item) => {
            let op = {
              label: item.userName,
              value: item.userName,
            };
            translateor.push(op);
          });
          translateor.push({ label: "无", value: "" });
          this.options[record.id].translator = translateor;
        }
        if (data.TRANSLATE_AUDITOR) {
          let translationAuditor = [];
          data.TRANSLATE_AUDITOR.forEach((item) => {
            let op = {
              label: item.userName,
              value: item.userName,
            };
            translationAuditor.push(op);
          });
          translationAuditor.push({ label: "无", value: "" });
          this.options[record.id].translationAuditor = translationAuditor;
        }
      });
    },
    // 保存
    save(id) {
      // 保存前校验
      let falg = this.checkTask(id);
      if (!falg) {
        return;
      }

      let index = this.dataSource.findIndex((item) => item.id === id);
      if (index !== -1) {
        this.dataSource[index] = this.editableData[id];
        // 只同步productSource已有的属性
        for (let key of ["title", "codeBranch"]) {
          this.productSource[index][key] = this.editableData[id][key];
        }
        // 只同步taskSource已有的属性
        for (let key of [
          "name",
          "codeBranch",
          "developer",
          "entryAuditor",
          "translator",
          "translationAuditor",
          "translateType",
          "versionName",
        ]) {
          this.taskSource[index][key] = this.editableData[id][key];
        }
      }
      this.cancel(id);
    },
    checkTask(id) {
      //1、开发员和词条审核员必须成对出现
      //2、翻译员和翻译审核员必须成对出现
      //3、(开发员、词条审核员) 和 (翻译员、翻译审核员) 必须出现一对
      let newTask = this.editableData[id];
      function isEmptyString(value) {
        return value === null || value === "" || value === undefined;
      }
      if (isEmptyString(newTask.creator)) {
        message.info("请选择任务管理员（创建人，控制工作台-归档阶段）！");
        return false;
      }
      if (
        !isEmptyString(newTask.developer) &&
        isEmptyString(newTask.entryAuditor)
      ) {
        message.info("请选择词条审核员！");
        return false;
      }
      if (
        !isEmptyString(newTask.entryAuditor) &&
        isEmptyString(newTask.developer)
      ) {
        message.info("请选择开发员！");
        return false;
      }
      if (
        !isEmptyString(newTask.translator) &&
        isEmptyString(newTask.translationAuditor)
      ) {
        message.info("请选择翻译审核员！");
        return false;
      }
      if (
        !isEmptyString(newTask.translationAuditor) &&
        isEmptyString(newTask.translator)
      ) {
        message.info("请选择翻译员！");
        return false;
      }
      if (
        isEmptyString(newTask.translationAuditor) &&
        isEmptyString(newTask.translator) &&
        isEmptyString(newTask.developer) &&
        isEmptyString(newTask.entryAuditor)
      ) {
        message.info("请选择操作人员！");
        return false;
      }
      return true;
    },
    // 取消
    cancel(id) {
      delete this.editableData[id];
    },
    async handleOK() {
      if (!this.ip) {
        message.info("请选择lang文档的ip地址！");
        return;
      }
      if (!this.codeBranch) {
        message.info("请输入分支名称！");
        return;
      }
      if (this.translateTypes.length == 0) {
        message.info("请选择至少选择一种导入语种！");
        return;
      }

      try {
        // 1.创建产品，返回产品id
        const productIds = [];
        await createProductByLang(this.productSource).then((res) => {
          for (let i = 0; i < res.data.length; i++) {
            this.productSource[i]["id"] = res.data[i]["id"]; // 把任务对应的产品信息写入
            this.taskSource[i]["productId"] = res.data[i]["id"]; // 把任务对应的产品信息写入
            productIds.push(res.data[i]["id"]);
          }
        });

        // // 1.5 处理需要忽略的文件（创建一些空的产品，用于占位）
        // await this.handleOthers();

        // 2.若某产品写入版本名称，则需创建版本
        for (let i = 0; i < this.dataSource.length; i++) {
          const verName = this.dataSource[i].versionName;
          if (verName) {
            const versionData = {
              name: verName,
              details: "",
              productId: this.productSource[i]["id"],
            };
            await createVersion(versionData).then((res) => {
              this.dataSource[i].versionId = res.data;
              this.taskSource[i].versionId = res.data;
            });
          }
        }

        // 3.创建任务，该接口既实现任务的创建，又实现词条的导入，并且还会修改相应状态：任务-流程中，有翻译的词条-已审核，没翻译的词条-新建
        let link_str = "";
        for (let i = 0; i < this.linkList.length; i++) {
          const srcDIR = this.linkList[i][0];
          const srcTask = this.taskSource[i]["name"];
          link_str = link_str + `${srcTask}: ${srcDIR},`;
        }
        const params = {
          ip: this.ip,
          parentId: this.treeNode.key,
          link: `{${link_str.slice(0, -1)}}`,
          translateTypes: this.translateTypes,
        };
        // console.log("任务入参", params, "请求体", this.taskSource);
        await createTaskByLang(params, this.taskSource)
          .then((res) => {
            // 4.执行完毕重新初始化并关闭窗口
            this.handleClose(true);
          })
          .catch((err) => {
            message.error(`创建任务失败：${err}`);
            console.log("创建任务失败", err);
            // 如果任务创建失败，前端执行删除产品
            deleteEntryClassfy(productIds)
              .catch((err) => {
                message.error(`删除分类失败：${err}`);
                console.log("删除分类失败", err);
              })
              .finally(() => {
                // 4.执行完毕重新初始化并关闭窗口
                this.handleClose(false);
              });
          });
      } catch (err) {
        message.error(`分支创建失败：${err}`);
        console.log("分支创建失败", err);
      }
    },
    handleClose(scflag = false) {
      this.init();
      this.$emit("createBranchClose", scflag);
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
    },
    clickInput(event) {
      event.stopPropagation();
    },
    // 事件处理函数
    handleResizeColumn(w, col) {
      col.width = w;
    },
    // ===============第二个表的相关函数========================================
    // 解析配置获取需要忽略的文件，写入otherDataSource中给用户展示（哪些文件不导入）
    createOtherProductByAnalysis(configList) {
      // console.log("解析配置获取需要忽略的文件", configList);
      let ignoreMap = new Map();
      for (let i = 0; i < configList.length; i++) {
        const newProduct = {
          ...configList[i],
          id: "mock_" + uuidv4(),
        };
        this.otherDataSource.push(newProduct);
        if (!ignoreMap.has(configList[i].link)) {
          ignoreMap.set(configList[i].link, [configList[i].title]);
        } else {
          ignoreMap.get(configList[i].link).push(configList[i].title);
        }
      }
      for (let i = 0; i < this.dataSource.length; i++) {
        let ignore = [];
        if (ignoreMap.has(this.dataSource[i].link)) {
          ignore = ignoreMap.get(this.dataSource[i].link);
        }
        this.dataSource[i].ignore = ignore;
        this.taskSource[i].ignore = ignore;
      }

      // console.log("忽略的特殊处理文件", this.otherDataSource);
      // console.log(
      //   "根据map增加ignore属性",
      //   ignoreMap,
      //   this.dataSource,
      //   this.taskSource
      // );
    },
    // 根据默认配置创建特殊处理的lang产品
    createOtherProduct(configList) {
      const comProduct = {
        parentId: this.treeNode.key,
        codeBranch: this.codeBranch,
        versionId: null,
        versionName: null,
      };

      let index = 0;
      for (let i = 0; i < configList.length; i++) {
        // 不做创建忽略文件的产品/分类了
        for (let j = 0; j < configList[i].files.length; j++) {
          const newProduct = {
            ...comProduct,
            title: configList[i].files[j],
            link: configList[i].link,
            id: "mock_" + uuidv4(), // template中用的id，所以就不改成key了，改动有点大
            index: index,
          };
          this.otherDataSource.push(newProduct);
          index++;
        }

        const currentLink = configList[i].link;
        // console.log("currentLink", currentLink);
        const linkIndex = this.dataSource.findIndex(
          (item) => item.link === currentLink
        );
        // console.log("linkIndex", linkIndex);

        if (linkIndex !== -1 && this.dataSource[linkIndex]) {
          const data = this.dataSource[linkIndex];
          if (data.ignore) {
            data.ignore = data.ignore.concat(configList[i].files);
          } else {
            data.ignore = configList[i].files;
          }
          const task = this.taskSource[linkIndex];
          if (task.ignore) {
            this.taskSource[linkIndex].ignore = task.ignore.concat(
              configList[i].files
            );
          } else {
            this.taskSource[linkIndex].ignore = configList[i].files;
          }
        }
      }

      // console.log(
      //   "忽略的特殊处理文件",
      //   this.otherDataSource,
      //   this.dataSource,
      //   this.taskSource
      // );
    },
    // 根据默认配置创建特殊处理的lang产品
    createOtherProduct_old(configList) {
      const comProduct = {
        parentId: this.treeNode.key,
        // parentTitle: "other",
        // parentTitleId: null, // 存产品id
        // parentTitleKey: "mock_other_" + uuidv4(), // 存前端唯一字段
        codeBranch: this.codeBranch,
        versionId: null,
        versionName: null,
      };
      // 记录parentTitleId
      this.otherParentSource.push({
        title: "other",
        parentId: this.treeNode.key,
        codeBranch: this.codeBranch,
      });
      this.otherParents.set("other", []);

      let index = 0;
      for (let i = 0; i < configList.length; i++) {
        // 不做创建忽略文件的产品/分类了
        for (let j = 0; j < configList[i].files.length; j++) {
          const newProduct = {
            ...comProduct,
            title: configList[i].files[j],
            link: configList[i].link,
            // subTitle: configList[i].title,
            // subTitleId: null,
            // subTitleKey: `mock_${configList[i].title}_${uuidv4()}`, // 存前端唯一字段
            id: "mock_" + uuidv4(), // template中用的id，所以就不改成key了，改动有点大
            index: index,
          };
          this.otherDataSource.push(newProduct);

          // // 记录subTitleId
          // if (!this.otherSubs.get(newProduct.subTitle)) {
          //   // 记录parentTitleId
          //   this.otherParents.get("other").push(newProduct.subTitleKey);
          //   this.otherSubSource.push({
          //     title: configList[i].title,
          //     parentId: null, // 创建好产品后再遍历parents的value来写入
          //     parentTitle: comProduct.parentTitle, //(用于回写)
          //     codeBranch: this.codeBranch,
          //   });
          //   this.otherSubs.set(configList[i].title, [newProduct.id]);
          // } else {
          //   this.otherSubs.get(configList[i].title).push(newProduct.id);
          // }
          index++;
        }

        //   const newProduct = {
        //     ...comProduct,
        //     ...configList[i],
        //     id: "mock_" + uuidv4(),
        //   };
        //   this.otherDataSource.push(newProduct);

        const currentLink = configList[i].link;
        // console.log("currentLink", currentLink);
        const linkIndex = this.dataSource.findIndex(
          (item) => item.link === currentLink
        );
        // console.log("linkIndex", linkIndex);

        if (linkIndex !== -1 && this.dataSource[linkIndex]) {
          const data = this.dataSource[linkIndex];
          if (data.ignore) {
            data.ignore = data.ignore.concat(configList[i].files);
          } else {
            data.ignore = configList[i].files;
          }
          const task = this.taskSource[linkIndex];
          if (task.ignore) {
            task.ignore = task.ignore.concat(configList[i].files);
          } else {
            task.ignore = configList[i].files;
          }
        }
      }

      // console.log("忽略的特殊处理文件", this.otherDataSource, this.dataSource);
      // console.log("Parent", this.otherParentSource, this.otherParents);
      // console.log("Sub", this.otherSubSource, this.otherSubs);
    },
    // 根据link和当前已被忽略的文件获取还可以忽略的文件选项
    async getIgnoreOptions(record) {
      if (!record || !record.link) return [];

      await getSourceByLang(record.link)
        .then(async (res) => {
          const allFiles = res.data.list;
          const ignoredFiles = this.dataSource
            .filter((item) => item.link === record.link)
            .map((item) => item.ignore); // 获取当前link下已被忽略的文件
          const importFiles = allFiles.filter((item) => {
            return !ignoredFiles.includes(item);
          }); // 获取当前link下还可以忽略的文件
          // console.log("allFiles", allFiles);
          // console.log("ignoredFiles", ignoredFiles);
          // console.log("importFiles", importFiles);
          this.ignoreOptionsMap[record.id] = importFiles.map((item) => ({
            label: item,
            value: item,
          }));
        })
        .catch(() => {
          console.log("获取忽略文件失败");
          this.ignoreOptionsMap[record.id] = [];
        });
    },
    // 新增需要忽略的文件
    addIgnore() {
      this.otherPagination.pageSize = this.otherPagination.pageSize + 1;
      let newData = {
        id: "ignore_" + uuidv4(),
        parentId: this.treeNode.key,
        link: null,
        title: null,
        isNew: true,
      };

      this.otherDataSource.splice(0, 0, newData);
      this.otherEditableData[newData.id] = newData;
      this.otherPageChange(1, 20);
    },
    // 添加第二个表的表格行点击事件
    otherCustomRow(record, index) {
      return {
        onDblclick: (event) => {
          // clearTimeout(this.timer)
          // this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
          if (this.otherEditableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }

          this.otherEdit(record);
        },
      };
    },
    // 编辑
    otherEdit(record) {
      this.otherEditableData[record.id] = cloneDeep(
        this.otherDataSource.filter((item) => record.id === item.id)[0]
      );

      // 编辑时获取忽略文件选项
      this.getIgnoreOptions(record);
    },
    // 显示对应产品的词条来源
    showSource(record) {
      if (!record) {
        this.otherSourceDataSource = [];
      } else {
        // console.log("record.files", record.files);
        this.otherSourceDataSource = record.files.map((item) => {
          return {
            id: item + uuidv4(),
            file: item,
          };
        });
      }
    },
    // 保存
    otherSave(id) {
      // 保存前校验
      let index = this.otherDataSource.findIndex((item) => item.id === id);
      if (index === -1) {
        message.error("请先添加产品！");
        return;
      }
      // console.log(
      //   "待保存词条",
      //   this.otherEditableData[id],
      //   "旧词条",
      //   this.otherDataSource[index]
      // );
      if (!this.otherEditableData[id].link) {
        message.error("请选择对应的lang目录！");
        return;
      }
      // if (!this.otherEditableData[id].parentTitle) {
      //   message.error("请输入分类名称！");
      //   return;
      // }
      // if (!this.otherEditableData[id].subTitle) {
      //   message.error("请输入子分类名称！");
      //   return;
      // }
      if (!this.otherEditableData[id].title) {
        message.error("请选择要忽略的文件！");
        // message.error("请输入产品名称！");
        return;
      }

      this.otherDataSource[index] = this.otherEditableData[id];
      this.otherDataSource[index].isNew = false;

      // // 保存新旧的分类&&子分类名称
      // const pTitle_old = this.otherDataSource[index].parentTitle;
      // const sTitle_old = this.otherDataSource[index].subTitle;
      // const pTitle = this.otherEditableData[id].parentTitle;
      // const sTitle = this.otherEditableData[id].subTitle;
      // const pKey = this.otherDataSource[index].parentTitleKey;
      // const sKey = this.otherDataSource[index].subTitleKey;
      // let pIndex = this.otherParentSource.findIndex(
      //   (item) => item.title === pTitle_old
      // );
      // let sIndex = this.otherSubSource.findIndex(
      //   (item) => item.title === sTitle_old
      // );

      // // 分类名发生变化，更新parentTitleId
      // if (pTitle != pTitle_old) {
      //   // 更新子分类的parentTitle(用于回写)
      //   this.otherSubSource[sIndex].parentTitle = pTitle;
      //   // map:删除原来的subTitle
      //   const plinkSub = this.otherParents.get(pTitle_old);
      //   console.log("plinkSub", plinkSub);
      //   if (
      //     (plinkSub.length == 1 && plinkSub == sKey) ||
      //     plinkSub.length == 0
      //   ) {
      //     this.otherParents.delete(pTitle_old);
      //     // source:删除旧分类增加新分类：已无；否则就是还有旧分类，source不动
      //     if (pIndex == -1) {
      //       console.log("异常退出，pIndex不应该为-1", this.otherParentSource);
      //       return;
      //     }
      //     this.otherParentSource[pIndex].title = pTitle;
      //   } else {
      //     this.otherParents.get(pTitle_old).splice(plinkSub.indexOf(sKey), 1);
      //   }

      //   // map:添加新的subTitle
      //   if (!this.otherParents.has(pTitle)) {
      //     this.otherParents.set(pTitle, [sKey]);
      //     // source:原来没有该分类，增加新分类
      //     const newParent = {
      //       title: pTitle,
      //       parentId: this.treeNode.key,
      //       codeBranch: this.codeBranch,
      //     };
      //     this.otherParentSource.push(newParent);
      //   } else {
      //     this.otherParents.get(pTitle).push(sKey);
      //   }

      //   console.log(
      //     "分类名发生变化",
      //     this.otherParentSource,
      //     this.otherParents
      //   );
      // }

      // // 子分类名发生变化，更新subTitleId
      // if (sTitle != sTitle_old) {
      //   // map:删除原来的title
      //   const slinkProb = this.otherSubs.get(sTitle_old);
      //   console.log("slinkProb", slinkProb);
      //   if (
      //     (slinkProb.length == 1 && slinkProb == id) ||
      //     slinkProb.length == 0
      //   ) {
      //     this.otherSubs.delete(sTitle_old);
      //     // source:删除旧分类增加新分类：已无；否则就是还有旧分类，source不动
      //     if (sIndex == -1) {
      //       console.log("异常退出，sIndex不应该为-1", this.otherSubSource);
      //       return;
      //     }
      //     this.otherSubSource[sIndex].title = sTitle;
      //   } else {
      //     this.otherSubs.get(sTitle_old).splice(slinkProb.indexOf(id), 1);
      //   }

      //   // map:添加新的subTitle
      //   if (!this.otherSubs.has(sTitle)) {
      //     this.otherSubs.set(sTitle, [id]);
      //     // source:原来没有该分类，增加新分类
      //     const newSub = {
      //       title: sTitle,
      //       parentId: null,
      //       codeBranch: this.codeBranch,
      //     };
      //     this.otherSubSource.push(newSub);
      //   } else {
      //     this.otherSubs.get(sTitle).push(id);
      //   }

      //   console.log("子分类名发生变化", this.otherSubSource, this.otherSubs);
      // }

      this.otherCancel(id);
    },
    // handleOK确定执行创建任务前，先执行忽略文件的创建
    async handleOthers() {
      // 1.创建分类，返回分类id并回填到子分类的parentId中
      await createProductByLang(this.otherParentSource).then((res) => {
        const pIdMap = new Map();
        for (let i = 0; i < res.data.length; i++) {
          pIdMap.set(res.data[i]["title"], res.data[i]["id"]);
        }
        for (let sItem of this.otherSubSource) {
          sItem.parentId = pIdMap.get(sItem.parentTitle);
        }
        // console.log("分类创建完成", pIdMap, this.otherSubSource);
      });
    },
    // 第二个表的取消
    otherCancel(id) {
      // 新建的取消要直接删掉
      if (this.otherEditableData[id].isNew) {
        this.otherDataSource = this.otherDataSource.filter((item) => {
          return item.id !== id;
        });
      }
      delete this.otherEditableData[id];
    },
    // 批量删除选中的产品
    otherDelete() {
      this.otherSelectedRows.forEach((item) => {
        this.otherDataSource = this.otherDataSource.filter((entry) => {
          return entry !== item;
        });
      });
      this.otherClearAllEntry();
    },
    // 表格复选框点击事件
    otherOnSelect(record, selected) {
      if (selected) {
        this.otherSelectEntry.set(record.id, record);
      } else {
        this.otherSelectEntry.delete(record.id);
      }

      if (record.isBranch) {
        if (selected) {
          record.child.forEach((item) => {
            this.selectEntry.set(item.id, item);
          });
        } else {
          record.child.forEach((item) => {
            this.otherSelectEntry.delete(item.id);
          });
        }
      }

      // console.log("表格复选框点击事件", record, selected);
    },
    // 表格全选/反选框点击事件（当前页）
    otherOnSelectAll(selected, selectedRows, changeRows) {
      if (selected) {
        changeRows.forEach((item) => {
          this.otherSelectEntry.set(item.id, item);
        });
      } else {
        changeRows.forEach((item) => {
          this.otherSelectEntry.delete(item.id);
        });
      }
      // console.log(
      //   "表格全选/反选框点击事件",
      //   selected,
      //   selectedRows, // 全选->取选当前页数据时，这个是5个undefined
      //   changeRows
      // );
    },
    // 表格复选框选择事件的回调（全选/反选不会回调这个函数）
    otherOnSelectChange(selectedRowKeys, selectedRows) {
      // this.selectedRowKeys = selectedRowKeys;
      // this.selectedRows = selectedRows;
      // onSelect(单选/取选)、onSelectAll(全选/反选)后，更新selectedRows、selectedRowKeys
      this.otherSelectedRows = [...this.otherSelectEntry.values()];
      this.otherSelectedRowKeys = [...this.otherSelectEntry.keys()]; // selectEntryList.map((item) => item.id);
      // console.log("表格复选框选择事件", this.otherSelectedRows);
    },
    // 复选框全选事件
    otherSelectAllEntry() {
      this.otherDataSource.forEach((item) => {
        this.otherSelectedRowKeys.push(item.id);
        this.otherSelectedRows.push(item);
        this.otherSelectEntry.set(item.id, item);
      });
    },
    //复选框反选事件
    otherClearAllEntry() {
      this.otherSelectedRowKeys = [];
      this.otherSelectedRows = [];
      this.otherSelectEntry.clear();
    },
    // 分页
    otherPageChange(page, pageSize) {
      this.otherPagination.current = page;
      this.otherPagination.pageSize = pageSize;
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
