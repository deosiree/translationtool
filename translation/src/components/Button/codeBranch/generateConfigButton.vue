<template>
  <a-button type="primary" @click="showModal" :size="size" :class="buttonClass">{{ buttonTitle }}</a-button>
  <CustomModal :modalTitle="buttonTitle" width="1000px" :modalVisible="visible" :showCancel="false" :showOk="false" @handleClose="handleClose">
    <div class="content">
      <a-table class="ant-table-striped" :columns="columns" :dataSource="dataSource" :scroll="{y: '280px'}"
        :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="table" bordered :pagination='pagination'
        :loading="loading">
      </a-table>
    </div>
    <template #leftBottomBtn>
      <div style="display:flex;gap: 8px;justify-content: flex-end;">
        <a-button key="back" @click="handleClose">关闭</a-button>
        <a-button type="primary" @click="cleanFile" :loading="loading">清除文件</a-button>
        <a-upload name="addConfig" :beforeUpload="beforeUpload" :accept="accept" :fileList="addfileList" @change="handleChangeAdd"
          @remove="removeFileAdd">
          <a-button type="primary" :loading="loading">+</a-button>
        </a-upload>
        <a-upload name="delConfig" :beforeUpload="beforeUpload" :accept="accept" :fileList="delfileList" @change="handleChangeDel"
          @remove="removeFileDel">
          <a-button type="primary" :loading="loading">-</a-button>
        </a-upload>
        <a-button type="primary" @click="clean" :loading="loading">清除</a-button>
        <a-button type="primary" @click="genConfig" :loading="loading">生成</a-button>
        <a-button type="primary" @click="exportConfig" :loading="loading">导出</a-button>
      </div>
    </template>
  </CustomModal>
</template>

<script>
import { message } from "ant-design-vue";
import CustomModal from "@/components/modal/index.vue";
import { setModalAriaHidden } from "@/utils/domUtils";
export default {
  components: {
    CustomModal,
  },
  props: {
    size: {
      type: String,
      default: "small",
    },
    buttonClass: {
      type: String,
      default: null,
    },
    buttonTitle: {
      type: String,
      default: "生成配置文件",
    },
  },
  data() {
    return {
      visible: false,
      loading: false,
      accept: ".json",
      addfileList: [],
      delfileList: [],
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
      dataSource: [],
      pagination: {
        pageSizeOptions: ["20", "50", "100"],
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
    };
  },
  methods: {
    readJsonFile(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (e) => {
          try {
            const data = JSON.parse(e.target.result);
            resolve(data);
          } catch (error) {
            reject(error);
          }
        };
        reader.onerror = (error) => {
          reject(error);
        };
        reader.readAsText(file, "utf-8");
      });
    },
    async genConfig() {
      this.loading = true;
      this.clean();

      try {
        // 处理添加的配置文件
        for (let addFileObj of this.addfileList) {
          // 获取真正的文件对象（Blob） - 当beforeUpload返回false时，文件会存储在originFileObj中
          const file = addFileObj.file || addFileObj.originFileObj;
          if (!file) {
            message.error("文件格式错误，请重新选择");
            continue;
          }

          try {
            const data = await this.readJsonFile(file);
            this.dataSource.push(...data);
          } catch (error) {
            message.error(`读取文件失败: ${error.message}`);
          }
        }

        // 去重：基于link和title字段的组合
        const uniqueMap = new Map();
        this.dataSource.forEach((item) => {
          const key = `${item.link}-${item.title}`;
          uniqueMap.set(key, item);
        });
        this.dataSource = Array.from(uniqueMap.values());

        // 处理删除的配置文件
        for (let delFileObj of this.delfileList) {
          // 获取真正的文件对象（Blob） - 当beforeUpload返回false时，文件会存储在originFileObj中
          const file = delFileObj.file || delFileObj.originFileObj;
          if (!file) {
            message.error("文件格式错误，请重新选择");
            continue;
          }

          try {
            const data = await this.readJsonFile(file);
            // 过滤掉需要删除的数据
            this.dataSource = this.dataSource.filter(
              (item) =>
                !data.some(
                  (delItem) =>
                    delItem.link == item.link && delItem.title == item.title
                )
            );
          } catch (error) {
            message.error(`读取文件失败: ${error.message}`);
            console.log("读取文件失败", error);
          }
        }

        // 更新分页总数
        this.pagination.total = this.dataSource.length;
        message.success("配置文件处理完成");
      } finally {
        this.loading = false;
      }
    },
    cleanFile() {
      this.clean();
      this.addfileList = [];
      this.delfileList = [];
    },
    clean() {
      this.dataSource = [];
    },
    exportConfig() {
      if (!this.dataSource || this.dataSource.length === 0) {
        message.warning("没有数据可以导出");
        return;
      }

      const fullFileName = `develop.json`;

      // 转换数据为JSON字符串
      let jsonData;
      try {
        jsonData = JSON.stringify(this.dataSource, null, 2); // 第三个参数2表示缩进2个空格，便于阅读
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
    // 移除文件
    removeFileAdd(file) {
      this.addfileList = this.addfileList.filter(
        (item) => item.uid !== file.uid
      );
      // console.log("removeFileAdd", this.addfileList);
      return true;
    },
    removeFileDel(file) {
      this.delfileList = this.delfileList.filter(
        (item) => item.uid !== file.uid
      );
      // console.log("removeFileDel", this.delfileList);
      return true;
    },
    // 文件变化处理
    handleChangeAdd(info) {
      this.addfileList = info.fileList; // max-count=1,一个文件一个文件地上传
      // console.log("handleChangeAdd", info, this.addfileList);
    },
    handleChangeDel(info) {
      this.delfileList = info.fileList; // max-count=1,一个文件一个文件地上传
      // console.log("handleChangeDel", info, this.addfileList);
    },
    // 在文件开始上传之前阻止文件上传操作
    beforeUpload(file, fileList) {
      return false;
    },
    showModal() {
      this.visible = true;
      this.cleanFile();
      setModalAriaHidden(this, document);
    },
    // 关闭导出模态框
    handleClose() {
      this.visible = false;
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
    },
  },
};
</script>