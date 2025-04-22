<template>
  <div class="box" ref="box">
    <!-- 搜索框组件 -->
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <!-- <a-form-item label="词条" name="entry">
            <a-input v-model:value="search.entry" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
          <a-form-item label="词条状态" name="state">
            <a-select v-model:value="search.entryState" style="width: 186px" placeholder="请选择" size="small" :options='entryStates' @click="clickInput"
              allowClear></a-select>
          </a-form-item>
          <a-form-item label="tag" name="tag">
            <a-input v-model:value="search.tag" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
          <a-form-item label="二级分类" name="classfy2">
            <a-select v-model:value="search.classfy2" style="width: 186px" placeholder="请选择" size="small" :fieldNames="{label:'name',value:'name'}"
              :options='classify2Option' @click="clickInput" allowClear></a-select>
          </a-form-item>

          <a-form-item label="翻译状态" name="translateState">
            <a-select v-model:value="search.translateState" style="width: 186px" placeholder="请选择" :options='translateStates' size="small"
              @click="clickInput" allowClear></a-select>
          </a-form-item>
          <a-form-item label="翻译结果" name="translate">
            <a-input v-model:value="search.translate" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item> -->
          <a-form-item label="i18n" name="i18n">
            <a-select v-model:value="search.i18nURL" :options="i18nOptions" placeholder="请选择i18n" allowClear></a-select>
          </a-form-item>
          <a-form-item label="部门" name="classfyID">
            <a-select v-model:value="search.classfyID" style="width: 186px" placeholder="请选择" size="small" :options='classfyIDs' @click="clickInput"
              allowClear></a-select>
          </a-form-item>
          <a-form-item label="词条来源" name="entrySource" v-if="hasRedundantRls">
            <a-select v-model:value="search.entrySource" :options='entrySources' placeholder="请选择" size="small" @click="clickInput"
              allowClear></a-select>
          </a-form-item>
          <a-form-item label="修改人" name="update" v-if="hasRedundantRls">
            <a-select v-model:value="search.update" :options='updates' placeholder="请选择" size="small" @click="clickInput" allowClear></a-select>
          </a-form-item>
        </a-form>
      </template>
      <!-- 操作按钮模板 -->
      <template v-slot:operate>
        <a-button type="primary" size="middle" class="yellowBtn" v-if="hasRedundantRls" @click="resetResult">重新执行</a-button>
        <!-- <a-button type="primary" size="middle" v-if="hasRedundantRls" @click="viewResult">查看结果</a-button> -->
        <a-button type="primary" size="middle" @click="check">查询</a-button>

      </template>
    </SearchBox>
    <!-- 数据展示框组件 -->
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <!-- <template v-slot:label>
        产品版本： <a-select v-model:value="currentVersion" allowClear style="width: 150px" placeholder="请选择版本" :options='productVersions'
          :fieldNames="{label:'name',value:'id'}" size="small" @click="clickInput">
        </a-select>
      </template> -->
      <template v-slot:operate>
        <div ref="button" v-if="true" style="margin-bottom:8px;display:flex;gap:10px">
          <a-button type="primary" size="small" @click="selectAllEntry">选择全部</a-button>
          <a-button type="primary" size="small" class="yellowBtn" @click="clearAllEntry">取消选择</a-button>
          <a-badge :count="selectEntry.length" :overflow-count="99">
            <a-button type="primary" size="small" class="resetBtn" @click="viewSelectEntry">已选词条</a-button>
          </a-badge>
          <BatchSelectModal :visible="batchSelectVisible" :dataSource="selectEntry" :search="search" @update:dataSource="selectEntry = $event"
            :selectedRowKeys="selectedRowKeys" @update:selectedRowKeys="selectedRowKeys = $event" :selectedRows="selectedRows"
            @update:selectedRows="selectedRows = $event" :columns="columns" @batchSelectClose="batchSelectClose"
            @batchSelectCancel="batchSelectCancel" @refresh="getCheckNotUseEntry" />
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
            <a-button type="primary" size="small"><template #icon>
                <SettingOutlined />
              </template>展示列</a-button>
          </a-popover>
        </div>
      </template>
      <!-- 数据展示模板 -->
      <template v-slot:data>
        <div style="width:100%;position: absolute;">
          <a-form ref="tableFormRef" :model="dataSource" :label-col="{ style: { width: '10px' } }" :wrapper-col="{ span: 0 }">
            <!-- 表格组件 -->
            <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="tableHeight"
              :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange,onSelect:onSelect,onSelectAll:onSelectAll}"
              :row-key="record => record.id" :pagination='pagination' :loading="loading" :rowClassName="getRowClassName"
              ref="redundantEntryCheckTable" @resizeColumn="handleResizeColumn">
              <!-- 表格单元格模板 -->
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'entryState'">
                  <template v-if="record.entryState === 0">
                    <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
                  </template>
                  <template v-if="record.entryState === 1">
                    <a-badge color="#FBB31F" /><span style="color:#FBB31F">审核中</span>
                  </template>
                  <template v-if="record.entryState === 2">
                    <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
                  </template>
                  <template v-if="record.entryState === 3">
                    <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
                  </template>
                </template>
                <template
                  v-if="['translateState', 'englishTranslateState','russianTranslateState','spanishTranslateState','frenchTranslateState'].includes(column.dataIndex)">
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

                <!--tag列 -->
                <template v-if="column.dataIndex === 'tag'">
                  <span>
                    <a-tag color="cyan" class="tag-content">
                      <span>{{ record.tag }}</span>
                    </a-tag>
                  </span>
                </template>
              </template>
            </a-table>
          </a-form>
        </div>
      </template>
    </DataBox>
  </div>
</template>
<script>
import "@/assets/style/common.less";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import stateBadge from "@/components/stateBadge/index.vue";
import BatchSelectModal from "@/views/check/redundantBatchSelectModal.vue";
import { getLanguage } from "@/http/api/translate";
import { getSecondClassify } from "@/http/api/secondClassify";
import { getI18nAdress } from "@/http/api/workbench";
import {
  mockSearchCheckInfo,
  getCheckNotUseEntry,
  checkNotUseEntry,
} from "@/http/api/check";
import { message, Modal } from "ant-design-vue";
import { SettingOutlined } from "@ant-design/icons-vue";
import tableParam from "@/views/check/redundantTableParam.js";
import {
  clickInput,
  setTableHeight,
  handleResizeColumn,
  getRowClassName,
  pageChange,
  onSelectChange,
  onSelect,
  onSelectAll,
  clearAllEntry,
} from "@/utils/tableUtils"; // 引入工具函数
import { setModalAriaHidden } from "@/utils/commonUtils";

export default {
  components: {
    SearchBox,
    DataBox,
    stateBadge,
    BatchSelectModal,
    SettingOutlined,
  },
  data() {
    return {
      search: {
        entry: "", //词条
        entryState: null, //词条状态
        entrySource: null, //词条来源
        tag: "", //tag
        classfy2: null, //二级分类
        entrySource: "", //词性来源
        translateType: null, //翻译语言
        translateState: null, //翻译状态
        translate: "", //翻译结果
        i18nURL: null,
        classfyID: null, // 部门级的classfyID
        pageIndex: 1,
        pageSize: 20,
      },
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
          width: 160,
          resizable: true,
          index: 1,
        },
        {
          title: "词条来源",
          dataIndex: "entrySource",
          align: "center",
          width: 130,
          resizable: true,
          index: 2,
        },
        {
          title: "词条状态",
          dataIndex: "entryState",
          align: "center",
          width: 130,
          resizable: true,
          index: 3,
        },
        {
          title: "导入类型",
          dataIndex: "importType",
          align: "center",
          width: 130,
          resizable: true,
          index: 4,
        },
        {
          title: "tag",
          dataIndex: "tag",
          align: "center",
          width: 150,
          resizable: true,
          index: 5,
        },
        {
          title: "修改人",
          dataIndex: "update",
          align: "center",
          width: 150,
          resizable: true,
          index: 6,
        },
        {
          title: "修改时间",
          dataIndex: "updateTime",
          align: "center",
          width: 150,
          resizable: true,
          index: 7,
        },
        // {
        //   title: "upgrade",
        //   dataIndex: "upgrade",
        //   align: "center",
        //   width: 150,
        //   resizable: true,
        //   index: 8,
        // },
        {
          title: "写入类型",
          dataIndex: "writeType",
          align: "center",
          width: 150,
          resizable: true,
          index: 9,
        },
      ],
      dataSource: [], // 表格数据
      i18nOptions: [], // i18n状态
      classfyIDs: [
        { label: "通用平台部", value: "1" },
        { label: "柔性输电系统部", value: "101" },
        { label: "装置开发部", value: "2" },
        { label: "监控系统部", value: "6" },
        { label: "公共库", value: "3" },
      ], // 词条状态
      entryStates: [
        { label: "新建", value: "0" },
        { label: "审核中", value: "1" },
        { label: "审核不通过", value: "2" },
        { label: "已审核", value: "3" },
      ], // 词条状态
      classify2Option: [], // 二级分类
      translateTypes: [], // 翻译语言
      translateStates: [
        { label: "未翻译", value: "0" },
        { label: "待审核", value: "1" },
        { label: "审核不通过", value: "2" },
        { label: "已审核", value: "3" },
      ], // 翻译状态
      entrySources: [], // 词条来源
      updates: [], // 修改人
      currentVersion: null, // 当前产品版本
      productVersions: [], // 产品版本
      labelCol: { style: { width: "84px" } },
      tableTitle: "冗余词条列表",
      dataHeight: 400,
      tableHeight: { x: "100%", y: 0 },
      loading: false,
      selectedRowKeys: [], // 表格选中项
      selectedRows: [], // 表格选中项
      selectEntry: [], // 已存词条，很重要，用于批量选择
      selectedRowIndex: null, // 表格选中项
      pagination: {
        showSizeChanger: true,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      pageChangeSearch: {},
      hasRedundantRls: false, // 是否有冗余词条的结果
      overlayStyle: tableParam.overlayStyle, // 展示列相关
      checkboxList: tableParam.checkboxList,
      checkedColumn: tableParam.checkedColumn,
      batchSelectVisible: false,
    };
  },
  mounted() {
    let _this = this;
    this.$nextTick(() => {
      this.init();
      // 读取本地存储的用户偏好
      const storedPreferences = localStorage.getItem(
        "redundantEntryCheckColumnPreferences"
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
    this.getOpitons();
  },
  unmounted() {
    //注销window.onresize事件
    window.onresize = null;
  },
  methods: {
    // 初始化
    init() {
      this.setTableHeight();
      // this.getOpitons();
    },
    // 获取下拉框信息
    getOpitons() {
      this.getI18nAdress();
      // this.getLanguage();
      // this.getSecondClassify();
      // this.getProductVersions();
    },
    // 获取翻译语言
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
      });
    },
    // 获取二级分类
    getSecondClassify() {
      let data = {};
      getSecondClassify(data).then((res) => {
        this.classify2Option = res.data.list;
      });
    },
    // 获取产品版本(?为啥是产品版本，要求的入参也对不上)
    getProductVersions() {
      let data = {};
      getSecondClassify(data).then((res) => {
        this.productVersions = res.data.list;
      });
    },
    // 获取i18服务器ip
    getI18nAdress() {
      this.i18nOptions = [];
      getI18nAdress().then((res) => {
        res.data.list.forEach((item) => {
          let ip = {
            label: item.ip,
            value: item.ip,
          };
          this.i18nOptions.push(ip);
        });
      });
    },
    getEntrySources() {
      this.entrySources = [];
      this.dataSource.forEach((item) => {
        if (!this.entrySources.find((src) => src.label === item.entrySource)) {
          let entrySource = {
            label: item.entrySource,
            value: item.entrySource,
          };
          this.entrySources.push(entrySource);
        }
      });
    },
    getUpdate() {
      this.updates = [];
      this.dataSource.forEach((item) => {
        if (!this.updates.find((src) => src.label === item.update)) {
          let update = {
            label: item.update,
            value: item.update,
          };
          this.updates.push(update);
        }
      });
    },
    // 过滤查询条件
    filter() {
      const ignoreKeys = ["i18nURL", "classfyID", "pageIndex", "pageSize"];
      Object.keys(this.search).forEach((key) => {
        if (this.search[key] && !ignoreKeys.includes(key)) {
          this.dataSource = this.dataSource.filter(
            (data) => data[key] === this.search[key]
          );
        }
      });
      this.pagination.total = this.dataSource.length;
      this.pagination.current = 1; // 重置当前页
      this.getEntrySources(); // 根据dataSource获取词条来源
      this.getUpdate(); // 根据dataSource获取修改人
    },
    // 校验按钮点击事件
    check() {
      this.dataSource = []; // 清空数据
      this.pageChangeSearch = this.search;
      this.getCheckNotUseEntry();
    },
    // 查询条件下的冗余词条是否已存库中
    getCheckNotUseEntry() {
      if (!this.search.i18nURL) {
        message.error("请选择i18n服务器");
        return;
      }
      if (!this.search.classfyID) {
        message.error("请选择部门");
        return;
      }
      this.loading = true;
      // this.search.pageIndex = this.pagination.current;
      // this.search.pageSize = this.pagination.pageSize;
      let params = {
        i18nURL: this.search.i18nURL,
        classfyID: this.search.classfyID,
        // pageIndex: this.search.pageIndex,
        // pageSize: this.search.pageSize,
      };
      getCheckNotUseEntry(params).then((res) => {
        // console.log("getCheckNotUseEntry!查询任务状态为", res.data.state);
        if (res.data.state === 1) {
          // 有结果
          this.hasRedundantRls = true; // 显示“重新执行”
          this.dataSource = res.data.list;
          this.filter(); // 过滤查询条件
          message.info(`查询有结果,共有${this.pagination.total}条冗余词条`, 1);
          this.loading = false;
        } else if (res.data.state === 2) {
          // 有结果
          this.hasRedundantRls = true; // 显示“重新执行”
          message.info(`任务执行异常`, 1);
          this.loading = false;
        } else if (res.data.state === 0) {
          // 没结果没执行
          message.info("查询无结果,开始校验", 1);
          this.resetResult(); // 没执行所以需要执行
          this.loading = false;
        } else if (res.data.state === 3) {
          // 没结果有执行
          message.info("查询无结果,正在校验", 1);
          this.loading = false;
        }
      });
      // .finally(() => {
      //   this.loading = false;
      // });
    },
    // 重新执行
    resetResult() {
      this.loading = true;
      this.pagination.current = 1;
      this.pagination.total = 0;
      // this.search.pageIndex = this.pagination.current;
      // this.search.pageSize = this.pagination.pageSize;
      let params = {
        i18nURL: this.search.i18nURL,
        classfyID: this.search.classfyID,
        // pageIndex: this.search.pageIndex,
        // pageSize: this.search.pageSize,
      };
      checkNotUseEntry(params)
        .then((res) => {
          // console.log("checkNotUseEntry!");
          if (res.data.state === 1) {
            message.info("重新开始校验！");
            this.hasRedundantRls = false;
          } else {
            if (res.data.state === 0) message.error("校验失败！");
          }
        })
        .finally(() => {
          this.loading = false;
        });
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
      localStorage.setItem(
        "redundantEntryCheckColumnPreferences",
        JSON.stringify(data)
      ); // localStorage存储用户偏好
    },
    // 全部选择
    selectAllEntry() {
      this.loading = true;
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.selectEntry = [];
      this.dataSource.forEach((item) => {
        this.selectedRowKeys.push(item.id);
        this.selectedRows.push(item);
        this.selectEntry.push(item);
      });
      this.loading = false;
    },
    // 取消选择
    clearAllEntry() {
      clearAllEntry(this);
    },
    // 打开已选词条
    viewSelectEntry() {
      this.batchSelectVisible = true;
      setModalAriaHidden(this, document);
    },
    // 关闭已选词条
    batchSelectClose() {
      this.batchSelectVisible = false;
    },
    // 取消选择（批量选择取消展开，清空已选词条）
    batchSelectCancel() {
      this.pagination.current = 1;
      this.selectEntry = [];
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.batchSelectClose(); // 关闭已选词条弹窗
    },
    // 复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      onSelectChange(this, selectedRowKeys, selectedRows);
    },
    // 复选框点击事件
    onSelect(record, selected) {
      onSelect(this, record, selected);
    },
    // 复选框当前页全选/反选框点击事件
    onSelectAll(selected, selectedRows, changeRows) {
      onSelectAll(this, selected, selectedRows, changeRows);
    },
    // 阻止事件冒泡，防止事件传播到父元素
    clickInput(event) {
      clickInput(this, event);
    },
    // 动态设置表格高度
    setTableHeight() {
      setTableHeight(this, 24); // 调用工具函数
    },
    // 表格列可伸缩
    handleResizeColumn(w, col) {
      return handleResizeColumn(w, col); // 调用工具函数
    },
    // 设置表格每一行的 class
    getRowClassName(record, index) {
      return getRowClassName(record, index, this.selectedRowIndex); // 调用工具函数
    },
    // 分页切换
    pageChange(page, pageSize) {
      pageChange(this, page, pageSize);
      // pageChange(this, page, pageSize, this.getCheckNotUseEntry);
    },
  },
};
</script>
<style scoped lang="less">
.box {
  width: 100%;
  height: 100%;
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