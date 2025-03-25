<template>
  <div class="box" ref="box">
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-form-item label="词条" name="entry">
            <a-input v-model:value="search.entry" placeholder="请输入词条"></a-input>
          </a-form-item>
          <a-form-item label="翻译" name="translate">
            <a-input v-model:value="search.translate" placeholder="请输入翻译"></a-input>
          </a-form-item>
          <a-form-item label="翻译类型" name="translateType">
            <a-select v-model:value="search.translateType" style="width: 186px" placeholder="请选择翻译类型" :options='translateTypes'
              :fieldNames="{label:'name',value:'name'}" allowClear>
            </a-select>
          </a-form-item>
          <a-form-item label="翻译状态" name="translateState">
            <a-select v-model:value="search.translateState" style="width: 186px" placeholder="请选择翻译状态" :options='translateStates' allowClear>
            </a-select>
          </a-form-item>
          <a-form-item label="可见范围" name="visualRange">
            <a-select v-model:value="search.visualRange" style="width: 186px" placeholder="请选择可见范围" :options='visualRanges' allowClear>
            </a-select>
          </a-form-item>
          <a-form-item label="校验类型" name="searchType">
            <a-select v-model:value="search.searchType" style="width: 186px" placeholder="请选择校验类型" :options='searchTypes' allowClear>
            </a-select>
          </a-form-item>
        </a-form>
      </template>
      <template v-slot:operate>
        <ResetButton :size="'middle'" :search="search" :currentPage="pagination.current" @resetData="onResetData" />
        <a-button type="primary" size="middle" @click="getSearchClick">查询</a-button>
      </template>
    </SearchBox>
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <template v-slot:operate>
        <div ref="button" v-if="true" style="margin-bottom:8px;display:flex;gap:10px">
          <BatchSelectButton v-if="!isGetSykEntry" :size="'middle'" :columns="columns" :dataSource="dataSource" :getSearch="getSearch"
            v-model:search="search" v-model:lastSearch="lastSearch" v-model:loading="loading" v-model:selectEntry="selectEntry"
            v-model:selectedRows="selectedRows" v-model:selectedRowKeys="selectedRowKeys" v-model:batchSelectFlag="batchSelectFlag"
            v-model:batchSelectVisible="batchSelectVisible" />

          <a-popover trigger="click" placement="leftTop" :overlayStyle="overlayStyle">
            <template #content>
              <a-checkbox-group v-model:value="checkedColumn" @change="changeColumn">
                <a-row v-for="item in checkboxList" :key="item.value">
                  <a-col :span="24">
                    <a-checkbox :value="item.value">
                      {{ item.label }}
                    </a-checkbox>
                  </a-col>
                </a-row>
              </a-checkbox-group>
            </template>
            <a-button type="primary" size="middle"><template #icon>
                <SettingOutlined />
              </template>展示列</a-button>
          </a-popover>
        </div>
      </template>
      <template v-slot:data>
        <div style="width:100%;position: absolute;">
          <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :row-key="record => record.id"
            :scroll="tableHeight" :pagination='pagination' :loading="loading" :rowClassName="getRowClassName" ref="glossaryTable"
            @resizeColumn="handleResizeColumn" :customRow="customRow"
            :row-selection="batchSelectFlag ? { selectedRowKeys: selectedRowKeys, onChange: onSelectChange,onSelect:onSelect,onSelectAll:onSelectAll} : null">
            <template #bodyCell="{ column, record,text}">
              <template v-if="column.dataIndex === 'translate'">
                <div>
                  <template v-if="editableData[record.id]">
                    <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" @click="clickInput" />
                  </template>
                  <template v-else>
                    {{ text }}
                  </template>
                </div>
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
              <template v-if="column.dataIndex === 'operation'">
                <div class="editable-row-operations">
                  <span v-if="editableData[record.id]">
                    <a-button type="primary" ghost size="small" @click.stop="save(record.id)">保存</a-button>
                    <a-button type="primary" ghost size="small" danger @click.stop="cancel(record.id)">取消</a-button>
                  </span>
                  <span v-else>
                    <a-button type="primary" ghost size="small" @click.stop="viewRelation(record)">详情({{record.relationCount}})</a-button>
                  </span>
                </div>
              </template>
            </template>
          </a-table>
        </div>
      </template>
    </DataBox>
  </div>
  <RelationModal :visible="relationVisible" :currentData="relationData" @relationClose="relationClose"></RelationModal>
</template>
<script>
import { message, Modal } from "ant-design-vue";
import locale from "ant-design-vue/es/date-picker/locale/zh_CN";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import ResetButton from "@/components/Button/resetButton.vue";
import BatchSelectButton from "@/components/Button/batchSelectButton.vue";
import commen from "@/views/entry/common.js";
import RelationModal from "@/views/glossary/relationModal.vue";
import { updateUserPartiality } from "@/http/api/userPartiality";
import tableParam from "@/views/glossary/tableParam.js";

import { cloneDeep, flatMap } from "lodash-es";
import {
  PlusOutlined,
  DeleteOutlined,
  CopyOutlined,
  SaveOutlined,
  SendOutlined,
  PlusCircleOutlined,
  ExclamationCircleOutlined,
  SettingOutlined,
} from "@ant-design/icons-vue";

import { getLanguage } from "@/http/api/translate";
import {
  getSykEntry,
  getSykNotUsed,
  checkSykEntry,
  updateSykEntry,
  getSykEntryRelation,
} from "@/http/api/glossary";
import { getSearch } from "@/utils/tableUtils";
import {
  onSelectChange,
  onSelect,
  onSelectAll,
  pageChange,
  clickInput,
  setTableHeight,
  handleResizeColumn,
  getRowClassName,
} from "@/utils/tableUtils";
import { defineComponent, ref, createVNode } from "vue";
export default {
  components: {
    SearchBox,
    DataBox,
    ResetButton,
    BatchSelectButton,
    RelationModal,
    PlusOutlined,
    DeleteOutlined,
    CopyOutlined,
    SaveOutlined,
    SendOutlined,
    PlusCircleOutlined,
    SettingOutlined,
  },
  data() {
    return {
      locale: locale,
      labelCol: { style: { width: "84px" } },
      search: {
        entry: "",
        translate: null,
        translateState: null,
        type: null,
        state: null,
        visualRange: null,
        searchType: null,
      },
      lastSearch: {}, // 存储上一次的查询条件
      translateStates: [
        { label: "未翻译", value: "0" },
        { label: "待审核", value: "1" },
        { label: "审核不通过", value: "2" },
        { label: "已审核", value: "3" },
      ],
      visualRanges: [
        { label: "通用平台部", value: "通用平台部" },
        { label: "监控系统部", value: "监控系统部" },
        { label: "装置开发部", value: "装置开发部" },
        { label: "柔性输电系统部", value: "柔性输电系统部" },
      ],
      searchTypes: [
        { label: "格式校验", value: "checkSykEntry" },
        { label: "空挂术语", value: "getSykNotUsed" },
        { label: "条件查询", value: "getSykEntry" },
      ],
      tableTitle: "术语列表",
      dataHeight: 400,
      tableHeight: { x: "100%", y: 0 },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 80,
          customRender: (text, record, index, column) => {
            return (
              text.index +
              1 +
              this.pagination.pageSize * (this.pagination.current - 1)
            );
          },
          fixed: "left",
          index: 0.1,
        },
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 150,
          fixed: "left",
          resizable: true,
          index: 1,
        },
        {
          title: "翻译",
          dataIndex: "translate",
          align: "center",
          width: 150,
          resizable: true,
          index: 2,
        },
        {
          title: "翻译类型",
          dataIndex: "type",
          align: "center",
          width: 230,
          resizable: true,
          index: 3,
        },
        {
          title: "翻译状态",
          dataIndex: "translateState",
          align: "center",
          width: 180,
          resizable: true,
          index: 4,
        },
        // {
        //   title: "翻译字符数",
        //   dataIndex: "charLength",
        //   align: "center",
        //   width: 150,
        //   index: 5,
        // },
        {
          title: "可见范围",
          dataIndex: "visualRange",
          align: "center",
          width: 150,
          index: 6,
        },
        {
          title: "词条审核员",
          dataIndex: "entryAuditor",
          align: "center",
          width: 150,
          index: 7,
        },
        // {
        //   title: "公开状态",
        //   dataIndex: "publicState",
        //   align: "center",
        //   width: 150,
        //   index: 8,
        // },
        // {
        //   title: "最大限制长度",
        //   dataIndex: "maxLength",
        //   align: "center",
        //   width: 150,
        //   index: 9,
        // },
        // {
        //   title: "审核意见",
        //   dataIndex: "auditSuggest",
        //   align: "center",
        //   width: 230,
        //   ellipsis: true,
        //   resizable: true,
        //   index: 10,
        // },
        // {
        //   title: "备注",
        //   dataIndex: "remark",
        //   align: "center",
        //   width: 200,
        //   index: 11,
        // },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 150,
          fixed: "right",
          index: 12,
        },
      ],
      dataSource: [],
      selectedRowKeys: [],
      selectedRows: [],
      selectEntry: [], // 已存词条，很重要，用于批量选择
      editableData: {},
      translateTypes: [],
      pagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      relationVisible: false,
      relationData: [],
      overlayStyle: tableParam.overlayStyle, // 展示列相关
      checkboxList: tableParam.checkboxList,
      checkedColumn: tableParam.checkedColumn,
      batchSelectFlag: false, // 批量选择的显示（全选/反选）
      isGetSykEntry: true,
      batchSelectVisible: false,
      apiFunctions: {
        getSykEntry: this.getSykEntry,
        getSykNotUsed: this.getSykNotUsed,
        checkSykEntry: this.checkSykEntry,
      },
      requestId: null, // 存储校验按钮的http请求
    };
  },
  mounted() {
    let _this = this;
    this.$nextTick(() => {
      this.init();
      // 读取本地存储的用户偏好
      const storedPreferences = localStorage.getItem(
        "glossaryColumnPreferences"
      );
      if (storedPreferences) {
        const preferences = JSON.parse(storedPreferences);
        this.checkedColumn = preferences.displayColumn.split(",");
        this.changeColumn(this.checkedColumn);
      }
      /** 控制table的高度 */
      window.onresize = function () {
        _this.setTableHeight();
      };
    });
  },
  unmounted() {
    //注销window.onresize事件
    window.onresize = null;
  },
  watch: {
    "search.searchType": {
      immediate: true,
      handler(newVal) {
        if (newVal && newVal !== "getSykEntry") {
          this.isGetSykEntry = false;
        } else {
          this.isGetSykEntry = true;
        }
      },
    },
  },
  methods: {
    // 初始化
    init() {
      this.setTableHeight();
      this.getLanguage();
      this.getSearchClick(); // 需要增加取消请求，再在初始化中调用，否则切换查询条件会发生覆盖
    },

    // 展示列切换
    changeColumn(checkedValue) {
      this.checkedColumn = checkedValue;
      this.checkboxList.forEach((value) => {
        let checkedIndex = this.checkedColumn.findIndex(
          (item) => item === value.value
        );
        let nowColumnIndex = this.columns.findIndex(
          (item) => item.dataIndex === value.value
        );
        if (
          (nowColumnIndex !== -1 && checkedIndex !== -1) ||
          (nowColumnIndex === -1 && checkedIndex === -1)
        ) {
          return;
        }
        if (nowColumnIndex === -1 && checkedIndex !== -1) {
          let newCol = {
            title: value.label,
            dataIndex: value.value,
            align: "center",
            width: 100,
            resizable: true,
            index: value.index,
          };
          this.columns.splice(-1, 0, newCol);
        }
        if (nowColumnIndex !== -1 && checkedIndex === -1) {
          this.columns.splice(nowColumnIndex, 1);
        }
      });
      this.columns.sort(function (a, b) {
        return a.index - b.index;
      });

      // 记录
      let data = {
        displayColumn: checkedValue.join(","),
      };
      // this.recordPartiality(data);
      localStorage.setItem('glossaryColumnPreferences', JSON.stringify(data)); // localStorage存储用户偏好
    },

    // 获取翻译语言
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
      });
    },
    // 查询按钮点击事件
    getSearchClick() {
      this.batchSelectFlag = false;
      this.pagination.current = 1;
      this.getSearch();
    },
    // 查询事件
    getSearch() {
      this.dataSource = [];
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.selectEntry = [];
      // 接口方法集合
      const apiFunctions = {
        getSykEntry: this.getSykEntry,
        getSykNotUsed: this.getSykNotUsed,
        checkSykEntry: this.checkSykEntry,
      };
      // 选项赋值
      let option = "";
      if (!this.search.searchType) {
        option = "getSykEntry"; // 默认选项
      } else {
        option = this.search.searchType;
      }
      // 记录上次查询条件
      const currentSearch = { ...this.search };
      currentSearch.searchType = option;
      // console.log("当前查询条件：", currentSearch);
      this.lastSearch = currentSearch;

      // 入参+请求体
      this.search.type = this.search.translateType;
      let params = {
        params: {
          pageIndex: this.pagination.current,
          pageSize: this.pagination.pageSize,
          requestId: `${option}-${Date.now().toString(16)}`,
        },
        data: this.search,
        lastRequestId: this.requestId, // 获取上一次的请求对象requestId
      };
      this.requestId = params.params.requestId; // 保存当前请求对象的requestId
      // console.log(`上次id:${params.lastRequestId};当前id:${this.requestId}`);

      // 调用getSearch方法
      getSearch(this, params, option, apiFunctions);
    },
    // 条件查询
    getSykEntry(params, data, lastRequestId) {
      return getSykEntry(params, data, lastRequestId).then((res) => {
        if (!res) {
          return;
        }
        this.dataSource = res.data.list;
        this.pagination.total = res.data.totalNum;
        for (let item of this.dataSource) {
          if (!item.type) item.type = "英文"; // 后端BUG，type字段为空
          getSykEntryRelation([item]).then((res) => {
            item["relationCount"] = res.data.list.length;
            item["reslations"] = res.data.list;
          });
        }
      });
    },
    // 空挂术语查询
    getSykNotUsed(params, data, lastRequestId) {
      return getSykNotUsed(params, data, lastRequestId).then((res) => {
        if (!res) {
          return;
        }
        this.dataSource = res.data.list;
        this.pagination.total = res.data.totalNum;
        for (let item of this.dataSource) {
          if (!item.type) item.type = "英文"; // 后端BUG，type字段为空
          // 空挂术语的详情都是0
          item["relationCount"] = 0;
          item["reslations"] = [];
        }
      });
    },
    // 格式校验查询
    checkSykEntry(params, data, lastRequestId) {
      return checkSykEntry(params, data, lastRequestId).then((res) => {
        if (!res) {
          return;
        }
        this.dataSource = res.data;
        this.pagination.total = this.dataSource.length;
        for (let item of this.dataSource) {
          if (!item.type) item.type = "英文"; // 后端BUG，type字段为空
          if (item.notUsedByEntryInfo) {
            item["relationCount"] = 0;
            item["reslations"] = [];
          } else {
            getSykEntryRelation([item]).then((res) => {
              item["relationCount"] = res.data.list.length;
              item["reslations"] = res.data.list;
            });
          }
        }
      });
    },
    // 保存
    save(id) {
      updateSykEntry([this.editableData[id]])
        .then((res) => {
          message.success("编辑成功！");
          delete this.editableData[id];
          this.getSearch();
        })
        .catch((err) => {
          message.error("编辑失败！");
        });
    },
    // 取消
    cancel(id) {
      delete this.editableData[id];
    },

    // 查看详情
    viewRelation(record) {
      this.relationData = record.reslations;
      // console.log(this.relationData);
      this.relationVisible = true;
    },
    // 关闭详情
    relationClose() {
      this.relationVisible = false;
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
          );
        },
      };
    },

    // 重置
    onResetData(newSearch, newPage) {
      this.search = newSearch;
      this.pagination.current = newPage;
    },
    // 阻止事件冒泡，防止事件传播到父元素
    clickInput(event) {
      clickInput(this, event);
    },
    // 动态设置表格高度
    setTableHeight() {
      setTableHeight(this, 8, 150, 30); // 调用工具函数
    },
    // 表格列可伸缩
    handleResizeColumn(w, col) {
      return handleResizeColumn(w, col); // 调用工具函数
    },
    // 设置表格每一行的 class
    getRowClassName(record, index) {
      return getRowClassName(record, index, this.selectedRowIndex); // 调用工具函数
    },
    // 复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      onSelectChange(this, selectedRowKeys, selectedRows);
    },
    // 复选框点击事件
    onSelect(record, selected) {
      onSelect(this, record, selected, this.batchSelectFlag);
    },
    // 复选框当前页全选/反选框点击事件
    onSelectAll(selected, selectedRows, changeRows) {
      onSelectAll(
        this,
        selected,
        selectedRows,
        changeRows,
        this.batchSelectFlag
      );
    },
    // 分页切换
    pageChange(page, pageSize) {
      console.log("pageChange", page, pageSize);
      if (this.isGetSykEntry)
        // if (!this.search.searchType || this.search.searchType == "getSykEntry")
        pageChange(this, page, pageSize, this.getSearch);
      // 需要回调查询接口，否则一次查询出所有数据，对前端压力太大了，所以每次分页查询只查询当前页的数据
      else pageChange(this, page, pageSize); // 不能回调查询接口，否则若使用了全选功能的话，切换到下一页全选又没了
    },
  },
};
</script>
<style lang="less">
@import url("@/assets/style/common.less");
</style>
<style scoped lang="less">
.box {
  width: 100%;
  height: 100%;
  padding: 16px;
  // border: 1px solid red;
}
</style>
<style lang="less">
.editable-cell {
  position: relative;
  .editable-cell-input-wrapper,
  .editable-cell-text-wrapper {
    padding-right: 24px;
  }

  .editable-cell-text-wrapper {
    padding: 5px 24px 5px 5px;
  }

  .editable-cell-icon,
  .editable-cell-icon-check {
    position: absolute;
    right: 0;
    width: 20px;
    cursor: pointer;
  }

  .editable-cell-icon {
    margin-top: 4px;
    display: none;
  }

  .editable-cell-icon-check {
    line-height: 28px;
  }

  .editable-cell-icon:hover,
  .editable-cell-icon-check:hover {
    color: #108ee9;
  }

  .editable-add-btn {
    margin-bottom: 8px;
  }
}
.editable-cell:hover .editable-cell-icon {
  display: inline-block;
}
.ant-table-cell {
  .ant-form-item {
    margin-bottom: 0px;
  }
}
</style>