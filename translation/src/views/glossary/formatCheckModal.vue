<template>
  <!-- <a-spin :spinning="visible"> -->
  <Modal ref="formatCheckBox" :modalWidth="modalWidth" :visible="visible" :modalTitle="modalTitle" @handleClose="handleClose" @handleOK="handleOK">
    <div class="content">
      <div class="table">
        <a-table class="ant-table-striped" :columns="columns" :dataSource="dataSource" :scroll="{x:'100%' , y: '280px'}"
          :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)"
          :row-selection=" { selectedRowKeys: selectedRowKeys, onChange: onSelectChange,onSelect:onSelect,onSelectAll:onSelectAll}"
          :row-key="record => record.id" ref="formatCheckTable" bordered :pagination='pagination' :loading="loading" :customRow="customRow">
          <template #bodyCell="{ column, record, text }">
            <template v-if="['deleteState','publicState'].includes(column.dataIndex)">
              <template v-if="text === 0 || text === '0'">
                <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
              </template>
              <template v-if="text === 1 || text === '1'">
                <a-badge color="#FBB31F" /><span style="color:#FBB31F">审核中</span>
              </template>
              <template v-if="text === 2 || text === '2'">
                <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
              </template>
              <template v-if="text === 3 || text === '3'">
                <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
              </template>
            </template>
            <template v-if="column.dataIndex === 'translateState'">
              <template v-if="record[column.dataIndex] === '0'">
                <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">未翻译</span>
              </template>
              <template v-if="record[column.dataIndex] === '1'">
                <a-badge color="#FBB31F" /><span style="color:#FBB31F">待审核</span>
              </template>
              <template v-if="record[column.dataIndex] === '2'">
                <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
              </template>
              <template v-if="record[column.dataIndex] === '3'">
                <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
              </template>
            </template>
            <template v-if="['entry','translate'].includes(column.dataIndex)">
              <div>
                <template v-if="editableData[record.id]">
                  <a-form :model="editableData[record.id]" :rules="rules[record.id]" :ref="'form'+record.id.replaceAll('-','')+column.dataIndex"
                    autocomplete="off">
                    <a-form-item :name="column.dataIndex">
                      <a-textarea v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" @click="clickInput"
                        :auto-size="{ minRows: 1}" />
                    </a-form-item>
                  </a-form>
                </template>
                <template v-else>
                  <!-- {{ text }} -->
                  <span v-html="text"></span>
                  <!-- <span v-text="text.replace(/\n/g, '\\n')"></span> -->
                </template>
              </div>
            </template>
            <template v-if="column.dataIndex === 'operation'">
              <div class="editable-row-operations">
                <span v-if="editableData[record.id]">
                  <a-button type="primary" ghost size="small" @click.stop="save(record.id)">保存</a-button>
                  <a-button type="primary" ghost size="small" danger @click.stop="cancel(record.id)">取消</a-button>
                </span>
                <!-- <span v-else>
                  <a-button type="primary" ghost size="small" @click.stop="entryDetails(record)">详情</a-button>
                  <a-button type="primary" ghost size="small" @click.stop="entryUpgrade(record)">升级</a-button>
                </span> -->
              </div>
            </template>
          </template>
        </a-table>
      </div>
    </div>
    <EditReason :visible="editVisible" :entry="editEntry" @editClose="editClose" @editOk="editOk" />
  </Modal>
  <!-- </a-spin> -->
</template>
<script>
import Modal from "@/components/modal/index.vue";
import EditReason from "@/views/entry/editReason.vue";
import { message } from "ant-design-vue";
import { checkSykEntry } from "@/http/api/glossary";
import common from "@/views/workbench/common.js";
import { cloneDeep, iteratee } from "lodash-es";
import { v4 as uuidv4 } from "uuid";
import { clickInput, pageChange, vilidFildLength } from "@/utils/tableUtils"; // 引入工具函数
export default {
  components: {
    Modal,
    EditReason,
  },
  emits: ["formatCheckClose"],
  props: {
    // 传递来的数据放这儿，不能再在data中定义了
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
    },
    // edit: true,// 是否可以编辑(应该做权限管理，从父组件取得是否可编辑的权限)
  },
  data() {
    return {
      labelCol: { style: { width: "80px" } },
      modalWidth: "400px",
      loading: false,
      columns: [
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 200,
        },
        {
          title: "翻译",
          dataIndex: "translate",
          align: "center",
          width: 200,
        },
        {
          title: "翻译类型",
          dataIndex: "type",
          align: "center",
          width: 100,
        },
        {
          title: "翻译状态",
          dataIndex: "translateState",
          align: "center",
          width: 100,
        },
        {
          title: "翻译字符数",
          dataIndex: "charLength",
          align: "center",
          width: 100,
        },
        {
          title: "上次使用时间",
          dataIndex: "lastUseTime",
          align: "center",
          width: 200,
        },
        {
          title: "可见范围",
          dataIndex: "visualRange",
          align: "center",
          width: 100,
        },
        {
          title: "删除状态",
          dataIndex: "deleteState",
          align: "center",
          width: 100,
        },
        {
          title: "最大限制长度",
          dataIndex: "maxLength",
          align: "center",
          width: 150,
        },
        {
          title: "公开状态",
          dataIndex: "publicState",
          align: "center",
          width: 100,
        },
        {
          title: "审核意见",
          dataIndex: "auditSuggest",
          align: "center",
          width: 100,
        },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 150,
          fixed: "right",
          // index: 100,
        },
      ],
      dataSource: [], // 展示的数据
      rules: {},
      limitMap: {},
      classify1Option: [],
      rowClassify2Option: {},
      edit: false, // 用户对该产品是否有编辑权限
      editableData: {}, // 编辑的数据
      editVisible: false,
      editEntry: [],
      selectedRowKeys: [],
      selectedRows: [],
      selectedRowIndex: null,
      currentEntry: {},
      pagination: {
        showSizeChanger: true,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    init() {
      this.dataSource = [];
      // this.checkSykEntry();
    },
    // 更新窗口
    checkSykEntry() {
      this.loading = true; //开始加载
      const data = {
        auditSuggest: "",
        charLength: 0,
        deleteState: 0,
        entry: null,
        id: null,
        lastUseTime: null,
        maxLength: 0,
        productName: null,
        publicState: 0,
        remark: null,
        taskName: "",
        translate: null,
        translateState: "",
        type: "",
        unique: "",
        versionID: null,
        versionName: "",
        visualRange: null,
      };
      // setTimeout(() => {
      //   this.loading = false; //结束加载
      //   this.dataSource = [];
      //   this.selectedRows = [];
      //   this.selectedRowKeys = [];
      // }, 3000);
      checkSykEntry(data)
        .then((res) => {
          // 都放到.then内，可以确保执行顺序
          // 设置默认全选
          const formatCheckEntries = Object.values(res.data);
          this.dataSource = [];
          this.selectedRows = [];
          this.selectedRowKeys = [];
          if (formatCheckEntries.length != 0) {
            formatCheckEntries.forEach((item) => {
              this.selectedRows.push({ ...item });
              this.selectedRowKeys.push(item.id);
            });
          }
          this.dataSource = this.selectedRows;
        })
        .catch((error) => {
          message.warn("error", error);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    // 窗口关闭
    handleClose() {
      this.$emit("formatCheckClose");
    },
    // 窗口确认
    handleOK() {
      this.$emit("formatCheckClose");
    },
    // 添加表格行点击事件
    customRow(record, index) {
      return {
        onDblclick: (event) => {
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }
          this.editableData[record.id] = cloneDeep(
            this.dataSource.filter((item) => record.id === item.id)[0]
          ); // 深拷贝当前行数据
          console.log(
            "editableData",
            this.editableData,
            common.languageMap[record.type].code
          );
          // 设置校验规则
          this.rules[record.id] = {
            entry: [
              { validator: vilidFildLength(this.limitMap, record, "chinese") },
              { required: true, message: "请输入!" },
            ],
            translate: [
              {
                validator: vilidFildLength(
                  this.limitMap,
                  record,
                  common.languageMap[record.type].code
                ),
              },
            ],
          };
        },
      };
    },
    // 操作编辑数据-保存
    save(id) {
      let flagArr = ["entry", "translate"];
      let list = [];
      this.columns.forEach((column) => {
        if (flagArr.includes(column.dataIndex)) {
          // 动态获取表单引用并调用 validate 方法进行验证（ a-form 组件提供的表单验证函数）
          // 将验证结果的 Promise 对象添加到 list 数组中
          list.push(
            eval(
              "this.$refs.form" + id.replaceAll("-", "") + column.dataIndex
            ).validate()
          );
        }
      });
      Promise.all(list)
        .then(() => {
          // 校验成功
          this.editEntry = [this.editableData[id]];
          this.editVisible = true;
          console.log("校验成功", list, this.editEntry);
        })
        .catch((err) => {});
    },
    // 操作编辑数据-取消
    cancel(id) {
      delete this.editableData[id];
      delete this.rules[id];
    },
    // 编辑框-确定
    editOk(entry) {
      delete this.editableData[entry.id];
      delete this.rules[entry.id];
      let index = this.dataSource.findIndex((item) => item.id === entry.id);
      this.dataSource.splice(index, 1);
      this.dataSource.splice(index, 0, entry);
      setTimeout(() => {
        message.warn("此处应使用接口更新到术语库");
      }, 3000);
      this.editVisible = false;
    },
    // 编辑框-取消
    editClose() {
      this.editVisible = false;
    },
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
      console.log("选择事件selectedRows", this.selectedRows);
      console.log("选择事件selectedRowKeys", this.selectedRowKeys);
    },
    // 表格复选框点击事件
    onSelect(record, selected) {
      // record是被点击的行数据，selected是是否被选中
      if (selected) {
        this.selectedRows.push(record);
      } else {
        this.selectedRows = this.selectedRows.filter((item) => {
          return item !== record;
        });
      }
    },
    // 表格全选/反选框点击事件
    onSelectAll(selected, changeRows) {
      console.log("进入全选事件");
      this.loading = true;
      new Promise((resolve) => {
        setTimeout(() => {
          if (selected) {
            // 直接赋值，确保 selectedRows 包含所有数据
            // this.selectedRows = [...changeRows];
            this.selectedRows = this.selectedRows.concat(changeRows);
          } else {
            changeRows.forEach((item) => {
              this.selectedRows = this.selectedRows.filter((entry) => {
                return entry !== item;
              });
            });
          }
          resolve();
        }, 0);
      }).then(() => {
        this.loading = false;
        console.log("全选selectedRows", this.selectedRows);
        console.log(
          "全选selectedRowKeys，后面会调用选择事件补齐keys",
          this.selectedRowKeys
        );
      });
      // new Promise((resolve) => {
      //   setTimeout(() => {
      //     console.log("loading效果");
      //     resolve();
      //   }, 3000);
      // }).then(() => {
      //   this.loading = false;
      // });
    },
    // 阻止事件冒泡，防止事件传播到父元素
    clickInput(event) {
      clickInput(this, event);
    },
    // 分页切换
    pageChange(page, pageSize) {
      pageChange(this, page, pageSize, this.checkSykEntry);
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