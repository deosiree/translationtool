<template>
  <div class="box" ref="box">
    <!-- 搜索框组件 -->
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-form-item label="词条" name="entry">
            <a-input v-model:value="search.entry" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
          <a-form-item label="词条状态" name="state">
            <a-select v-model:value="search.entryState" style="width: 186px" placeholder="请选择" size="small" :options='entryStates'
              @click="clickInput"></a-select>
          </a-form-item>
          <a-form-item label="Tag" name="tag">
            <a-input v-model:value="search.tag" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
          <a-form-item label="二级分类" name="classfy2">
            <a-select v-model:value="search.classfy2" style="width: 186px" placeholder="请选择" size="small" :fieldNames="{label:'name',value:'name'}"
              :options='classify2Option' @click="clickInput"></a-select>
          </a-form-item>
          <a-form-item label="词条来源" name="entrySource">
            <a-input v-model:value="search.entrySource" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
          <a-form-item label="翻译语言" name="translateType">
            <a-select v-model:value="search.translateType" style="width: 186px" placeholder="请选择" size="small"
              :fieldNames="{label:'name',value:'name'}" :options='translateTypes' @click="clickInput"></a-select>
          </a-form-item>
          <a-form-item label="翻译状态" name="translateState">
            <a-select v-model:value="search.translateState" style="width: 186px" placeholder="请选择" :options='translateStates' size="small"
              @click="clickInput"></a-select>
          </a-form-item>
          <a-form-item label="翻译结果" name="translate">
            <a-input v-model:value="search.translate" style="width: 186px" placeholder="请输入内容" size="small" @click="clickInput"></a-input>
          </a-form-item>
        </a-form>
      </template>
      <!-- 操作按钮模板 -->
      <template v-slot:operate>
        <a-button type="primary" size="middle" class="checkBtn" @click="check">校验</a-button>
      </template>
    </SearchBox>
    <!-- 数据展示框组件 -->
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <template v-slot:label>
        产品版本： <a-select v-model:value="currentVersion" allowClear style="width: 150px" placeholder="请选择版本" :options='productVersions'
          :fieldNames="{label:'name',value:'id'}" size="small" @click="changeVersion">
        </a-select>
      </template>
      <template v-slot:operate>
        <div ref="button" v-if="true" style="margin-bottom:8px;display:flex;gap:10px">
          <!-- <a-button type="primary" danger size="small" @click="selectDelEntry" v-if="!selectDelEntryFlag">删除</a-button> -->
          <!-- <a-button type="primary" size="small" @click="selectAllEntry" v-if="!selectAllEntryFlag">全选</a-button> -->
        </div>
      </template>
      <!-- 数据展示模板 -->
      <template v-slot:data>
        <div style="width:100%;position: absolute;">
          <a-form ref="tableFormRef" :model="dataSource" :label-col="{ style: { width: '10px' } }" :wrapper-col="{ span: 0 }">
            <!-- 表格组件 -->
            <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource"
              :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }" :row-key="record => record.id" :pagination='pagination'
              :loading="loading" :rowClassName="getRowClassName" ref="taskTable" @resizeColumn="handleResizeColumn">
              <!-- 表格单元格模板 -->
              <template #bodyCell="{ column, record }">
                <!-- 词条状态列 -->
                <template v-if="column.dataIndex === 'entryState'">
                  <stateBadge :entry-state="record.entryState" />
                </template>
                <!-- 词条列 -->
                <template v-if="column.dataIndex === 'entry'">
                  <span>{{ record.entry }}</span>
                </template>
                <!-- 词条版本列 -->
                <template v-if="column.dataIndex === 'entryVersion'">
                  <span>{{ record.entryVersion }}</span>
                </template>
                <!-- 词性来源列 -->
                <template v-if="column.dataIndex === 'entrySource'">
                  <span>{{ record.entrySource }}</span>
                </template>
                <!--Tag列 -->
                <template v-if="column.dataIndex === 'tag'">
                  <span class="tag">{{ record.tag }}</span>
                </template>
                <!--英文翻译列 -->
                <template v-if="column.dataIndex === 'english'">
                  <span>{{ record.english }}</span>
                </template>
                <!-- 英文翻译状态列 -->
                <template v-if="column.dataIndex === 'translateState'">
                  <stateBadge :entry-state="record.translateState" />
                </template>
                <!-- 辞典名称列 -->
                <template v-if="column.dataIndex === 'dicName'">
                  <span>{{ record.dicName }}</span>
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
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import stateBadge from "@/components/stateBadge/index.vue";
import { getLanguage } from "@/http/api/translate";
import { getSecondClassify } from "@/http/api/secondClassify";
import { message, Modal } from "ant-design-vue";

export default {
  components: {
    SearchBox,
    DataBox,
    stateBadge,
  },
  data() {
    return {
      search: {
        entry: "", //词条
        entryState: null, //词条状态
        tag: "", //Tag
        classfy2: null, //二级分类
        entrySource: "", //词性来源
        translateType: null, //翻译语言
        translateState: null, //翻译状态
        translate: "", //翻译结果
      },
      columns: [
        {
          title: "词条状态",
          dataIndex: "entryState",
          align: "center",
          width: 130,
          index: 0.1,
        },
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 160,
          resizable: true,
          index: 2,
        },
        {
          title: "词条版本",
          dataIndex: "entryVersion",
          align: "center",
          width: 130,
          index: 6,
        },
        {
          title: "词条来源",
          dataIndex: "entrySource",
          align: "center",
          width: 150,
          resizable: true,
          index: 1,
        },
        {
          title: "Tag",
          dataIndex: "tag",
          align: "center",
          width: 150,
          resizable: true,
          index: 1,
        },
        {
          title: "英文翻译",
          dataIndex: "english",
          align: "center",
          width: 180,
          resizable: true,
          index: 10,
        }, // 只是英文的翻译吗？为何不是对应语言的翻译{{translateType}}
        {
          title: "英文翻译状态",
          dataIndex: "translateState",
          align: "center",
          width: 180,
          resizable: true,
          index: 16,
        },
        {
          title: "辞典名称",
          dataIndex: "dicName",
          align: "center",
          width: 180,
          resizable: true,
          index: 19,
        },
      ],
      // dataSource: [],// 表格数据
      dataSource: [
        {
          id: 1,
          entryState: "已审核",
          entry: "词条",
          entryVersion: "词条版本",
          entrySource: "词条来源",
          tag: "Tag",
          english: "英文翻译",
          translateState: "审核不通过",
          dicName: "辞典名称",
        },
        {
          id: 2,
          entryState: "审核不通过",
          entry: "词条",
          entryVersion: "词条版本",
          entrySource: "词条来源",
          tag: "Tag",
          english: "英文翻译",
          translateState: "审核中",
          dicName: "辞典名称",
        },
        // ... 其他示例数据
      ],
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
      currentVersion: null, // 当前产品版本
      productVersions: [], // 产品版本
      labelCol: { style: { width: "84px" } },
      tableTitle: "校验日志",
      dataHeight: 400,
      tableHeight: { x: "100%", y: 0 },
      loading: false,
      editableData: {}, // 可编辑数据
      selectedRowKeys: [], // 表格选中项
      selectedRows: [], // 表格选中项
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
    };
  },
  mounted() {
    let _this = this;
    this.$nextTick(() => {
      this.init();
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
      // this.searchTaskInfo();
      this.getOpitons();
    },
    // 删除按钮要用的？没细看
    deleteEntry() {
      if (this.selectedRowKeys.length === 0) {
        return;
      }
      Modal.confirm({
        title: "是否确定删除?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          let version = this.productVersions.find(
            (item) => item.id === this.currentVersion
          );
          let params = {
            tableName: version.tableName,
          };
          deleteEntryInfo(this.selectedRowKeys, params).then((res) => {
            message.success("删除成功！");
            this.getEntryByVersion();
            this.selectedRowKeys = [];
            this.selectedRows = [];
          });
        },
      });
    },

    changeVersion(version) {
      if (version === undefined) {
        this.currentVersion = null;
      } else {
        this.currentVersion = version;
      }
      this.pagination.current = 1;
      // 查询版本词条
      this.getEntryByVersion();
    },
    // 校验按钮点击事件(待)
    check() {
      this.pageChangeSearch = this.search;
      this.searchTaskInfo();
    },
    // 获取校验信息（待）
    searchCheckInfo() {
      this.searchCheckByCondition(this.search);
    },
    searchCheckByCondition(data) {
      this.loading = true;
      let params = {
        pageIndex: this.pagination.current,
        pageSize: this.pagination.pageSize,
      };
      searchCheckInfo(data, params)
        .then((res) => {
          // this.dataSource = res.data.list
          this.loading = false;
          this.pagination.total = res.data.totalNum;
        })
        .catch((err) => {
          this.loading = false;
        });
    },
    // 获取下拉框信息
    getOpitons() {
      this.getLanguage();
      this.getSecondClassify();
    },
    // 获取产品版本
    getEntryByVersion() {
      // console.log("翻译结果：", this.search.translate);
      // console.log("翻译状态：", this.search.translateState);
      // console.log("翻译语言：", this.search.translateType);
      if (
        (this.search.translate != "" || this.search.translateState != null) &&
        this.search.translateType === null
      ) {
        message.info("请选择翻译语言！");
        return;
      }

      // let data = {
      //   entry: this.search.entry,
      //   entryState: this.search.entryState,
      //   tag: this.search.tag,
      //   entrySource: this.search.entrySource,
      //   translateType: this.search.translateType,
      //   translateState: this.search.translateState,
      //   translate: this.search.translate,
      // };
      // if (this.currentVersion === null) {
      //   data.productID =
      //     this.product.type === "module"
      //       ? this.product.parentId
      //       : this.product.key;
      // } else {
      //   data.versionID = this.currentVersion;
      // }
      // if (this.search.language === "英文") {
      //   data.english = this.search.translate;
      //   data.englishTranslateState = this.search.translateState;
      // } else if (this.search.language === "俄文") {
      //   data.russian = this.search.translate;
      //   data.russianTranslateState = this.search.translateState;
      // } else if (this.search.language === "西文") {
      //   data.spanish = this.search.translate;
      //   data.spanishTranslateState = this.search.translateState;
      // } else if (this.search.language === "法文") {
      //   data.french = this.search.translate;
      //   data.frenchTranslateState = this.search.translateState;
      // }
      // let params = {
      //   classfyID: this.product.key,
      //   pageIndex: this.pagination.current,
      //   pageSize: this.pagination.pageSize,
      // };
      // this.loading = true;

      // // getEntryByVersion(data,params).then((res) => {
      // //     this.dataSource = res.data.list
      // //     this.loading = false
      // //     this.pagination.total = res.data.totalNum
      // // }).catch((err) => {
      // //     this.loading = false
      // // })
      // getEntryByClassfy(params, data)
      //   .then((res) => {
      //     this.dataSource = res.data.list;
      //     this.loading = false;
      //     this.pagination.total = res.data.totalNum;
      //   })
      //   .catch((err) => {
      //     this.loading = false;
      //   });
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
    // 表单单元格的点击事件
    clickInput(event) {
      event.stopPropagation();
    },
    // 设置表格每一行的class
    getRowClassName(record, index) {
      let className = null;
      if (index % 2 === 1) {
        className = "table-striped";
        if (this.selectedRowIndex === record.id) {
          className = className + " highlighted-row";
        }
      } else {
        if (this.selectedRowIndex === record.id) {
          className = "highlighted-row";
        }
      }
      return className;
    },
    // 动态设置表格高度
    setTableHeight() {
      this.$nextTick(() => {
        // 设置列表父元素高度
        let box = this.$refs.box.offsetHeight;
        let searchHeight = this.$refs.search.$el.offsetHeight;
        this.dataHeight = box - searchHeight;

        // 设置表格高度
        let buttonHeight = 0;
        try {
          buttonHeight = this.$refs.button.offsetHeight + 8;
        } catch (error) {}
        this.tableHeight.y = this.dataHeight - buttonHeight - 150;
      });
    },
    // 表格列可伸缩
    handleResizeColumn: (w, col) => {
      col.width = w;
    },
    // 设置表格每一行的class
    getRowClassName(record, index) {
      let className = null;
      if (index % 2 === 1) {
        className = "table-striped";
        if (this.selectedRowIndex === record.id) {
          className = className + " highlighted-row";
        }
      } else {
        if (this.selectedRowIndex === record.id) {
          className = "highlighted-row";
        }
      }
      return className;
    },
    // 表格复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;

      this.searchTaskByCondition(this.pageChangeSearch);
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
.tag {
  font-size: 12px;
  padding: 4px 8px;
  background-color: #eefffb;
  border: 1px solid #beede5;
  border-radius: 4px;
  color: #77b3c9;
}

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