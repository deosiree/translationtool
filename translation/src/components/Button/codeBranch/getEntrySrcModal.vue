<template>
  <CustomModal :visible="visible" :okLoading="loading" :modalTitle="modalTitle" @handleClose="handleClose" @handleOK="handleOK">
    <div>{{currentClass.title}}</div>
    <div class="search">
      <a-form :model="search" layout="inline" autocomplete="off" :label-col="labelCol">
        <a-row class="search-row" style="width:100%;display:flex;gap:8px">
          <a-form-item label="i18nURL" name="i18nURL">
            <a-select placeholder="请选择i18nURL" allowClear style="width: 240px" :options='ipOptions' v-model:value="search.i18nURL">
            </a-select>
          </a-form-item>
          <a-form-item label="翻译语种" name="translateType">
            <a-select mode="multiple" placeholder="请选择翻译语种" allowClear style="width: 240px" :fieldNames="{label:'name',value:'name'}"
              :options='translateTypes' v-model:value="search.translateType">
            </a-select>
          </a-form-item>
        </a-row>
        <a-row style="width:100%;margin-top:8px;display:flex;gap:8px;" class="search-row" justify="end">
          <a-button type="primary" size="middle" class="resetBtn" @click="reset" :loading="loading">重置</a-button>
          <a-button type="primary" size="middle" @click="query" :loading="loading">查询</a-button>
        </a-row>
        <a-row style="width:100%;margin-top:8px;display:flex;gap:8px;" class="search-row" justify="end">
          <div class="existSrcTable">
            <h2>现有文件（曾导入）</h2>
            <a-table class="ant-table-striped" :columns="cols" :dataSource="existedSource" :scroll="{y: '280px'}"
              :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="createBranchTable" bordered
              :pagination='existedPagination' :loading="loading">
            </a-table>
            <a-button type="primary" size="small" :loading="loading" @click="exportExisted">导出</a-button>
          </div>
          <div class="allSrcTable">
            <h2>所有文件</h2>
            <a-table class="ant-table-striped" :columns="cols" :dataSource="allSource" :scroll="{y: '280px'}"
              :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="createBranchTable" bordered
              :pagination='allPagination' :loading="loading">
            </a-table>
            <a-button type="primary" size="small" :loading="loading" @click="exportAll">导出</a-button>
          </div>
          <div class="ignoredSrcTable">
            <h2 style="color:red">被忽略文件(所有-现有)</h2>
            <a-table class="ant-table-striped" :columns="cols" :dataSource="ignoredSource" :scroll="{y: '280px'}"
              :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="createBranchTable" bordered
              :pagination='ignoredPagination' :loading="loading">
            </a-table>
            <a-button type="primary" size="small" :loading="loading" @click="exportIgnored">导出</a-button>
          </div>
          <div class="forbiddenedSrcTable">
            <h2>被废弃文件(现有-所有)</h2>
            <a-table class="ant-table-striped" :columns="cols" :dataSource="forbiddenedSource" :scroll="{y: '280px'}"
              :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="createBranchTable" bordered
              :pagination='forbiddenedPagination' :loading="loading">
            </a-table>
            <a-button type="primary" size="small" :loading="loading" @click="exportForbiddened">导出</a-button>
          </div>
          <div class="commonSrcTable">
            <h2>共有文件(现有∩所有)</h2>
            <a-table class="ant-table-striped" :columns="cols" :dataSource="commonSource" :scroll="{y: '280px'}"
              :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="createBranchTable" bordered
              :pagination='commonPagination' :loading="loading">
            </a-table>
            <a-button type="primary" size="small" :loading="loading" @click="exportCommon">导出</a-button>
          </div>
        </a-row>
      </a-form>
    </div>
    <template #leftBottomBtn>
      <GenConfigButton size="middle" buttonTitle="配置文件生成工具" />
    </template>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import GenConfigButton from "@/components/Button/codeBranch/generateConfigButton.vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import { getLanguage } from "@/http/api/translate";
import { getI18nAdress } from "@/http/api/workbench";
import {
  getEntrySourcesByClassify,
  getWriteFileNamesByClassify,
} from "@/http/api/entryManage";
import { getFileListUsingI18nServer } from "@/http/api/task";
import { setModalAriaHidden } from "@/utils/domUtils";

export default {
  components: {
    CustomModal,
    GenConfigButton,
    PlusOutlined,
  },
  emits: ["configList", "handleClose"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
      default: "该节点下所有词条来源的汇总",
    },
    currentClass: {
      type: Object,
      default: {},
    },
  },
  data() {
    return {
      loading: false,
      labelCol: { style: { width: "84px" } },
      cols: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 50,
          customRender: (text) => {
            const currentIndex =
              text.index +
              1 +
              this.existedPagination.pageSize *
                (this.existedPagination.current - 1);
            return currentIndex;
          },
          fixed: "left",
        },
        {
          title: "lang目录",
          dataIndex: "link",
          align: "center",
          width: 200,
          resizable: true,
        },
        {
          title: "文件名称",
          dataIndex: "title",
          align: "center",
          width: 200,
          resizable: true,
        },
      ],
      existedSource: [],
      existedPagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.existedPageChange,
      },
      allSource: [],
      allPagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.allPageChange,
      },
      ignoredSource: [],
      ignoredPagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.ignoredPageChange,
      },
      forbiddenedSource: [],
      forbiddenedPagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.forbiddenedPageChange,
      },
      commonSource: [],
      commonPagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.commonPageChange,
      },
      search: {
        i18nURL: null,
        translateType: ["英文"],
      },
      ipOptions: [],
      translateTypes: [],
    };
  },
  watch: {
    visible(newVal, oldVal) {
      if (newVal !== oldVal && newVal) {
        this.init();
      }
    },
  },
  methods: {
    init() {
      this.existedSource = [];
      this.reset();
      this.getI18nAdress();
      this.getLanguage();
      this.getExistedSource();
    },
    reset() {
      this.search = { i18nURL: null, translateType: ["英文"] };
      this.allSource = [];
      this.ignoredSource = [];
      this.forbiddenedSource = [];
      this.commonSource = [];
    },
    //查询
    query() {
      console.log("查询接口");
      if (!this.search.i18nURL) {
        message.error("请输入词条来源");
        return;
      }
      if (
        !this.search.translateType ||
        this.search.translateType.length === 0
      ) {
        message.error("请选择翻译语种");
        return;
      }
      this.loading = true;
      getFileListUsingI18nServer({
        ip: this.search.i18nURL,
        translateTypes: this.search.translateType,
      })
        .then((res) => {
          console.log("查询词条来源成功:", res);
          this.allSource = res.data;
          this.allPagination.total = res.data.total;

          this.commonSource = this.allSource.filter((item) => {
            return this.existedSource.some(
              (existedItem) => existedItem.title === item.title
            );
          });
          this.commonPagination.total = this.commonSource.length;

          this.ignoredSource = this.allSource.filter((item) => {
            return !this.existedSource.some(
              (existedItem) => existedItem.title === item.title
            );
          });
          this.ignoredPagination.total = this.ignoredSource.length;

          this.forbiddenedSource = this.existedSource.filter((item) => {
            return !this.allSource.some(
              (allItem) => allItem.title === item.title
            );
          });
          this.forbiddenedPagination.total = this.forbiddenedSource.length;
        })
        .catch((err) => {
          console.log("获取词条来源失败:", err);
          message.error("获取词条来源失败:", err);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    // 获取翻译语种
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
      });
    },
    // 获取i18服务器ip
    getI18nAdress() {
      this.ipOptions = [];
      getI18nAdress().then((res) => {
        res.data.list.forEach((item) => {
          let ip = {
            label: item.ip,
            value: item.ip,
          };
          this.ipOptions.push(ip);
        });
      });
    },
    // 导出表格数据
    exportTable(src, post = null) {
      console.log("导出src:", src);
      if (!src || src.length === 0) {
        message.warning("没有数据可以导出");
        return;
      }

      // 使用传入的分支名或默认分支名
      const branchName = this.currentClass.title;
      // 构建完整的文件名
      const fullFileName = `${branchName}${post ? "_" + post : ""}.json`;

      // 转换数据为JSON字符串
      let jsonData;
      try {
        jsonData = JSON.stringify(src, null, 2); // 第三个参数2表示缩进2个空格，便于阅读
      } catch (error) {
        message.error("数据转换失败：" + error.message);
        return;
      }

      // 创建Blob对象
      const blob = new Blob([jsonData], {
        type: "application/json;charset=utf-8",
      });

      // 创建下载链接
      const link = document.createElement("a");
      link.href = URL.createObjectURL(blob);
      link.download = fullFileName;

      // 触发下载
      document.body.appendChild(link);
      link.click();

      // 清理
      setTimeout(() => {
        document.body.removeChild(link);
        URL.revokeObjectURL(link.href);
      }, 100);

      message.success("导出成功");
    },
    exportExisted() {
      // this.exportTable(this.existedSource);
      this.exportTable(this.existedSource, "现有");
    },
    exportAll() {
      // this.exportTable(this.allSource);
      this.exportTable(this.allSource, "所有");
    },
    exportIgnored() {
      // this.exportTable(this.ignoredSource);
      this.exportTable(this.ignoredSource, "忽略");
    },
    exportForbiddened() {
      // this.exportTable(this.forbiddenedSource);
      this.exportTable(this.forbiddenedSource, "禁用");
    },
    exportCommon() {
      // this.exportTable(this.commonSource);
      this.exportTable(this.commonSource, "共有");
    },
    // 查询产品的所有词条来源
    async getExistedSource() {
      this.loading = true;
      // console.log("classifyID", this.currentClass, this.currentClass.key);
      // 获取词条来源
      await getEntrySourcesByClassify({
        classifyID: this.currentClass.key,
        writeType: "TS",
      })
        .then((res) => {
          this.existedSource = res.data.map((item) => ({
            title: item,
            link: "ts",
          }));
        })
        .catch((err) => {
          console.log("获取词条来源失败:", err);
          message.error("获取词条来源失败:", err);
        });
      // 获取字典来源
      await getWriteFileNamesByClassify({
        classifyID: this.currentClass.key,
        writeType: "DI",
      })
        .then((res) => {
          this.existedSource = this.existedSource.concat(
            res.data.map((item) => ({
              title: item,
              link: "dic",
            }))
          );
        })
        .catch((err) => {
          console.log("获取辞典名称失败:", err);
          message.error("获取辞典名称失败:", err);
        });
      this.existedPagination.total = this.existedSource.length;
      this.loading = false;
    },
    // 分页切换
    existedPageChange(page, pageSize) {
      this.existedPagination.current = page;
      this.existedPagination.pageSize = pageSize;
    },
    allPageChange(page, pageSize) {
      this.allPagination.current = page;
      this.allPagination.pageSize = pageSize;
    },
    ignoredPageChange(page, pageSize) {
      this.ignoredPagination.current = page;
      this.ignoredPagination.pageSize = pageSize;
    },
    forbiddenedPageChange(page, pageSize) {
      this.forbiddenedPagination.current = page;
      this.forbiddenedPagination.pageSize = pageSize;
    },
    commonPageChange(page, pageSize) {
      this.commonPagination.current = page;
      this.commonPagination.pageSize = pageSize;
    },
    // 确认模态框
    handleOK() {
      this.handleClose();
    },
    // 关闭模态框
    handleClose() {
      this.$emit("handleClose");
    },
  },
};
</script>
<style scoped lang="less">
.existSrcTable,
.allSrcTable,
.ignoredSrcTable,
.forbiddenedSrcTable,
.commonSrcTable {
  flex: 1;
  min-width: 300px; /* 每个表格的最小宽度 */
  margin: 0 10px;
  box-sizing: border-box;

  h2 {
    text-align: center;
    margin-bottom: 16px;
    font-size: 16px;
    color: #333;
  }

  .ant-table {
    width: 100%;
    table-layout: fixed; /* 确保表格列宽固定 */
  }
}
</style>