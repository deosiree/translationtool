<template>
  <a-button type="primary" :size="size" style="margin-left:10px" @click="addRepository">
    <template #icon>
      <BookOutlined v-if="icon === 'BookOutlined'" />
      <PlusOutlined v-if="icon === 'PlusOutlined'" />
    </template>
    {{title}}
  </a-button>
  <Modal :visible="visible" :modalTitle="modalTitle" @handleClose="close" @handleOK="ok" @afterClose="afterClose">
    <div class="content">
      <a-form :model="formState" :label-col="labelCol" ref="formRef" :rules="rules">
        <a-form-item label="版本名称" name="version">
          <a-input v-model:value="formState.version" placeholder="请输入版本名" />
        </a-form-item>
        <a-form-item label="类型">
          <a-radio-group v-model:value="formState.type" @change="radioChange">
            <a-radio value="1">全覆盖升级</a-radio>
            <a-radio value="2">增量升级</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="选择版本库" v-if="formState.type === '2'">
          <a-select v-model:value="formState.repository" placeholder="请选择版本库" :options='repositoryList' show-search allowClear></a-select>
        </a-form-item>
        <a-form-item label="选择词条" name="selectRows">
          <a-button type="primary" size="small" @click="selectData" style="margin-right:10px">选择</a-button>
          <span class="tip" v-if="selectedRows.length > 0">已选择 {{selectedRows.length}} 项</span>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="formState.remark" type="textarea" placeholder="请输入版本备注" />
        </a-form-item>
      </a-form>
    </div>

  </Modal>
  <Modal :modalWidth="dataWidth" :visible="dataVisible" :modalTitle="dataModalTitle" @handleClose="closeData" @handleOK="okData">
    <a-form :model="search" layout="inline" style="margin-bottom:5px">
      <a-form-item label="分类">
        <a-tree-select v-model:value="search.classify" style="width: 200px" :tree-data="treeData" allow-clear tree-checkable
          :show-checked-strategy="SHOW_PARENT" placeholder="请选择" tree-node-filter-prop="label" :field-names="{
                        children: 'children',
                        label: 'title',
                        value: 'key',
                    }" @change="selectTree" />
      </a-form-item>
      <a-form-item label="Tag">
        <a-input v-model:value="search.tag" placeholder="请输入Tag" />
      </a-form-item>
      <a-form-item label="创建人">
        <a-input v-model:value="search.creator" placeholder="请输入创建人" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" size="small" @click="getEntryToVersion">查询</a-button>
      </a-form-item>
    </a-form>
    <a-table class="ant-table-striped" :loading="loading" :columns="columns" :data-source="dataSource"
      :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange, getCheckboxProps: getCheckboxProps}"
      :row-key="record => record.id" :scroll="{x:'100%' , y: '350px'}" :pagination='false'
      :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="table" bordered>
      <template #bodyCell="{ column, text , record}">
        <template v-if="column.dataIndex === 'version'">
          <div>
            <template v-if="editableData[record.id]">
              <a-select v-model:value="editableData[record.id][column.dataIndex]" placeholder="请选择版本" :options='versionList' style="width:100%"
                @select="versionSelect(record)" allowClear></a-select>
            </template>
            <template v-else>
              {{ text }}
            </template>
          </div>
        </template>
        <!--Tag列 -->
        <template v-if="column.dataIndex === 'tag'">
          <span>
            <a-tag color="cyan" class="tag-content">
              <span>{{ text }}</span>
            </a-tag>
          </span>
        </template>
      </template>

    </a-table>
  </Modal>
</template>
<script>
import '@/assets/style/common.less'
import { message, TreeSelect } from "ant-design-vue";
import Modal from "@/components/modal/index.vue";
import { cloneDeep } from "lodash-es";
import { BookOutlined, PlusOutlined } from "@ant-design/icons-vue";
import {
  importExcle,
  getEntryClassfy,
  getKindEntryVersion,
  searchEntry,
} from "@/http/api/entry";
import {
  getEntryToVersion,
  createVersionTable,
  getVersionTableByCondition,
} from "@/http/api/versionTable";
export default {
  components: {
    BookOutlined,
    PlusOutlined,
    Modal,
  },
  emits: ["afterClose"],
  props: {
    size: {
      type: String,
      default: "default",
    },
    title: {
      type: String,
    },
    icon: {
      type: String,
      default: "PlusOutlined",
    },
  },
  data() {
    return {
      SHOW_PARENT: TreeSelect.SHOW_PARENT,
      user: {},
      visible: false,
      modalWidth: "450px",
      modalTitle: "生成版本库",
      formState: {
        version: "",
        type: "1",
        repository: null,
        remark: "",
      },
      labelCol: { style: { width: "80px" } },
      repositoryList: [],
      dataWidth: "70%",
      dataVisible: false,
      dataModalTitle: "选择词条",
      loading: false,
      dataSource: [],
      columns: [
        // {title: "序号",dataIndex: 'index',align:'center',width:70,customRender: (text, record, index, column) => {
        //     return text.index + 1
        // },fixed: 'left'},
        {
          title: "Abbr",
          dataIndex: "abbr",
          width: 150,
          fixed: "left",
          customCell: (record, rowIndex, column) => ({
            colSpan: record.flag != "col" ? 1 : 17,
          }),
        },
        {
          title: "版本",
          dataIndex: "version",
          width: 150,
          align: "center",
          fixed: "left",
          customCell: this.versionClick,
        },
        {
          title: "创建日期",
          dataIndex: "createTime",
          align: "center",
          width: 150,
        },
        { title: "创建人", dataIndex: "creator", align: "center", width: 150 },
        {
          title: "词性备注",
          dataIndex: "partOfSpeech",
          align: "center",
          width: 150,
        },
        {
          title: "中文释义",
          dataIndex: "chineseInterpretation",
          align: "center",
          width: 200,
          ellipsis: true,
        },
        {
          title: "英文释义",
          dataIndex: "englishInterpretation",
          align: "center",
          width: 200,
          ellipsis: true,
        },
        {
          title: "中文术语",
          children: [
            {
              title: "术语",
              dataIndex: "chinese",
              align: "center",
              width: 200,
              ellipsis: true,
            },
            {
              title: "字符数",
              dataIndex: "chineseLength",
              align: "center",
              width: 150,
            },
          ],
        },
        {
          title: "英文术语",
          children: [
            {
              title: "术语",
              dataIndex: "english",
              align: "center",
              width: 200,
              ellipsis: true,
            },
            {
              title: "字符数",
              dataIndex: "englishLength",
              align: "center",
              width: 150,
            },
          ],
        },
        {
          title: "西文术语",
          children: [
            {
              title: "术语",
              dataIndex: "spanish",
              align: "center",
              width: 200,
              ellipsis: true,
            },
            {
              title: "字符数",
              dataIndex: "spanishLength",
              align: "center",
              width: 150,
            },
          ],
        },
        {
          title: "俄文术语",
          children: [
            {
              title: "俄文术语",
              dataIndex: "russian",
              align: "center",
              width: 200,
              ellipsis: true,
            },
            {
              title: "俄文字符数",
              dataIndex: "russianLength",
              align: "center",
              width: 150,
            },
          ],
        },
        {
          title: "法文术语",
          children: [
            {
              title: "法文术语",
              dataIndex: "french",
              align: "center",
              width: 200,
              ellipsis: true,
            },
            {
              title: "法文字符数",
              dataIndex: "frenchLength",
              align: "center",
              width: 150,
            },
          ],
        },
        {
          title: "备注",
          dataIndex: "remark",
          align: "center",
          width: 200,
          ellipsis: true,
        },
      ],
      pagination: {
        total: 0,
        current: 1,
        pageSize: 1,
        showTotal: (total) => `共 ${total} 条数据`,
      },
      selectedRowKeys: [],
      selectedRows: [],
      search: {
        classify: undefined,
        tag: "",
        creator: "",
      },
      treeData: [],
      rules: {
        version: [{ required: true, message: "请输入版本名称！" }],
        selectRows: [{ required: true, validator: this.checkField }],
      },
      editableData: {},
      versionEntries: [],
      fuzzyEntries: [],
      versionList: [
        { label: "1.0", value: "1.0" },
        { label: "2.0", value: "2.0" },
        { label: "3.0", value: "3.0" },
      ],
    };
  },
  mounted() {
    this.user = this.$store.state.user;
  },
  methods: {
    // 添加版本单元格双击可编辑事件
    versionClick(record) {
      return {
        style: {
          color: "blue", //这里将名称变了下色
        },
        onDblclick: () => {
          // 增量升级时 已属于某个版本的 不可编辑
          let flag = this.fuzzyEntries.find((item) => {
            return item.id === record.id;
          });
          if (flag) {
            this.editableData = {};
            this.editableData[record.id] = cloneDeep(record);
            // 查询该词条的所有版本
            let params = {
              typeID: record.typeId,
            };
            getKindEntryVersion(params).then((res) => {
              let versionList = [];
              res.data.list.forEach((item) => {
                let version = {
                  label: item,
                  value: item,
                };
                versionList.push(version);
              });
              this.versionList = versionList;
            });
          }
        },
      };
    },
    // 表单校验
    checkField(rule, value, callback) {
      if (this.selectedRows.length === 0) {
        return Promise.reject("请选择词条！");
      }
      return Promise.resolve();
    },
    // 合并单元格
    getCheckboxProps(record) {
      if (record.flag != "col") {
      } else {
        return {
          props: {
            disabled: false,
          },
          style: {
            display: "none",
          },
        };
      }
    },
    // 词条版本选择事件
    versionSelect(record) {
      // 查询对应版本的词条并替换
      let data = {
        typeId: record.typeId,
        version: this.editableData[record.id].version,
      };
      let params = {};
      searchEntry(data, params).then((res) => {
        let list = res.data.list;
        if (list.length > 0) {
          Object.assign(
            this.dataSource.filter((item) => record.id === item.id)[0],
            list[0]
          );
          delete this.editableData[record.id];
        } else {
          message.error("操作失败！");
        }
      });
    },
    // 新增按钮弹窗事件
    addRepository() {
      this.visible = true;
      this.getHistoryVersionTable();
      this.getEntryClassify();
    },
    // 查询历史版本库名称
    getHistoryVersionTable() {
      let params = {
        pageIndex: -1,
        pageSize: -1,
      };
      let data = {
        department: this.user.department,
      };
      getVersionTableByCondition(params, data).then((res) => {
        this.repositoryList = [];
        res.data.list.forEach((item) => {
          let version = {
            label: item.version,
            value: item.version,
          };
          this.repositoryList.push(version);
        });
      });
    },
    // 查询测条分类
    getEntryClassify() {
      getEntryClassfy().then((res) => {
        // console.log(res)
        this.treeData = res.data.list;
      });
    },
    // 树形下拉菜单选择事件
    selectTree(value, label, extra) {
      console.log(value);
      // console.log(label)
      // console.log(extra)
      console.log(this.getChild(this.treeData, value[0]));
    },
    // 弹窗确认事件
    ok() {
      this.$refs.formRef
        .validate()
        .then(() => {
          // 字段校验通过  生成版本
          this.createVersionTable();
        })
        .catch((err) => {
          console.log("error", err);
        });
    },
    // 新增版本库
    createVersionTable() {
      let params = {
        version: this.formState.version,
        remark: this.formState.remark,
      };
      // console.log(this.selectedRows)
      createVersionTable(params, this.selectedRows).then((res) => {
        console.log(res);
        message.success("已生成版本库！");
        this.visible = false;
      });
    },
    // 弹窗关闭事件
    close() {
      this.visible = false;
      this.$refs.formRef.resetFields();
    },
    afterClose() {
      this.formState = {
        version: "",
        type: "1",
        repository: null,
        remark: "",
      };
      this.search = {
        classify: undefined,
        tag: "",
        creator: "",
      };
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.$emit("afterClose");
    },
    // 选择词条点击事件
    selectData() {
      this.dataVisible = true;
      this.getEntryToVersion();
    },
    // 查询词条
    getEntryToVersion() {
      let params = {
        version: this.formState.repository,
        tag: this.search.tag,
        creator: this.search.creator,
      };
      let classifyKeys = [];
      if (this.search.classify != undefined) {
        this.search.classify.forEach((item) => {
          classifyKeys = classifyKeys.concat(
            this.getChild(this.treeData, item)
          );
        });
      }
      getEntryToVersion(params, classifyKeys).then((res) => {
        // console.log(res)
        this.versionEntries = res.data.versionEntries;
        this.fuzzyEntries = res.data.fuzzyEntries;
        if (res.data.versionEntries.length === 0) {
          this.dataSource = res.data.versionEntries.concat(
            res.data.fuzzyEntries
          );
        } else {
          // 增量升级时 表格内容分类展示
          this.dataSource = [];
          let versionEntries = {
            id: "1",
            abbr: this.formState.repository + "_版本库词条",
            flag: "col",
            children: res.data.versionEntries,
          };
          let fuzzyEntries = {
            id: "2",
            abbr: "其他词条",
            flag: "col",
            children: res.data.fuzzyEntries,
          };

          this.dataSource.push(versionEntries);
          this.dataSource.push(fuzzyEntries);
        }
        // 增量升级时 默认选中的词条
        let selected = [];
        let selectedData = [];
        res.data.versionEntries.forEach((item) => {
          selected.push(item.id);
          selectedData.push(item);
        });
        this.selectedRowKeys = selected;
        this.selectedRows = selectedData;
      });
    },
    getChild(nodes, key, arr = []) {
      for (let el of nodes) {
        if (el.key === key) {
          arr.push(el.key);
          if (el.children) {
            this.childNodesDeep(el.children, arr);
          }
        } else if (el.children) {
          this.getChild(el.children, key, arr);
        }
      }
      return arr;
    },
    childNodesDeep(nodes, arr) {
      if (nodes) {
        nodes.forEach((ele) => {
          arr.push(ele.key);
          if (ele.children) {
            this.childNodesDeep(ele.children, arr);
          }
        });
      }
    },
    // 升级方式点击事件
    radioChange(e) {
      if (e.target.value === "1") {
        this.formState.repository = null;
      }
    },
    // 词条选择弹窗 确认事件
    okData() {
      this.dataVisible = false;
      this.$refs.formRef.validate("selectRows");
      this.editableData = {};
    },
    // 词条选择弹窗 关闭事件
    closeData() {
      this.dataVisible = false;
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.editableData = {};
    },
    // 词条勾选事件
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
    },
    // 分页选择事件
    handleTableChange(pagination) {
      this.pagination.current = pagination.current;
      this.pagination.total = pagination.total;
    },
  },
};
</script>
<style lang="less" scoped>
.tip {
  font-size: 0.9em;
  color: #c5c5c5;
}
.content {
  width: 100%;
  height: 100%;
  padding: 10px;
  background-color: #f3f3f3;
}
</style>>
