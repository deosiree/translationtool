<template>
  <!-- <a-spin :spinning="visible"> -->
  <Modal ref="createBranchBox" :modalWidth="modalWidth" :visible="visible" :createBranchClassfyID="createBranchClassfyID" :modalTitle="modalTitle"
    @handleClose="handleClose" @handleOK="handleOK">
    <div class="content">
      <div class="table">
        <a-form ref="params" name="custom-validation">
          <a-form-item label="IP" name="ip">
            <a-select v-model:value="ip" :options="ipOptions" placeholder="请选择IP" allowClear></a-select>
          </a-form-item>
          <a-form-item label="分支名" name="codeBranch">
            <a-input v-model:value="codeBranch" placeholder="请输入分支名"></a-input>
          </a-form-item>
          <a-form-item label="导入语种" name="translateTypes">
            <!-- 修改为多选 -->
            <a-select mode="multiple" v-model:value="translateTypes" :options="langOptions" placeholder="请输入各任务需要的导入翻译语种" @change="languageChange"
              allowClear></a-select>
          </a-form-item>
        </a-form>
        <a-table class="ant-table-striped" :columns="columns" :dataSource="dataSource" :scroll="{x:'100%' , y: '280px'}"
          :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="createBranchTable" bordered :pagination='pagination'
          :loading="loading" :customRow="customRow">
          <template #bodyCell="{ column, text, record }">
            <template v-if="['name','title', 'versionName'].includes(column.dataIndex)">
              <template v-if="editableData[record.id]">
                <a-input @click="clickInput" v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0"
                  @pressEnter="save(record.id)" />
              </template>
              <template v-else>
                {{ text }}
              </template>
            </template>
            <template v-else-if="['developer','entryAuditor','translator','translationAuditor','translateType'].includes(column.dataIndex)">
              <template v-if="editableData[record.id]">
                <a-select v-model:value="editableData[record.id][column.dataIndex]" style="width: 100%" placeholder="请选择"
                  :options='options[record.id][column.dataIndex]' @click="clickInput" allowClear>
                </a-select>
              </template>
              <template v-else>
                {{ text }}
              </template>
            </template>
            <template v-else-if="column.dataIndex === 'operation'">
              <span v-if="editableData[record.id]">
                <a-button type="primary" ghost size="small" @click.stop="save(record.id)">保存</a-button>
                <a-button type="primary" ghost size="small" danger @click.stop="cancel(record.id)">取消</a-button>
              </span>
              <span v-else>
                <a-button type="primary" ghost size="small" @click.stop="edit(record)">编辑</a-button>
              </span>
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </Modal>
  <!-- </a-spin> -->
</template>
<script>
import Modal from "@/components/modal/index.vue";
import VersionModal from "@/views/task/versionModal.vue";
import { message } from "ant-design-vue";
import { getI18nAdress } from "@/http/api/workbench";
import { getRoleUserByDepartment } from "@/http/api/user";
import { createProductByLang } from "@/http/api/entryManage";
import { createVersion } from "@/http/api/productVersion";
import { createTaskByLang } from "@/http/api/task";
import commonParam from "@/utils/commonParam";
import { v4 as uuidv4 } from "uuid";
import { setModalAriaHidden, randomError } from "@/utils/commonUtils";
import { cloneDeep } from "lodash-es";
export default {
  components: {
    Modal,
    VersionModal,
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
      user: null,
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
      linkList: [
        ["db", "数据库-元数据"],
        ["meta", "数据库-对象数据"],
        ["enum", "枚举"],
        ["config", "配置文件"],
        ["ts", "工具-ts"],
        ["tr", "工具-tr"],
      ],
      dataSource: [], // 展示列=任务信息+产品信息
      taskSource: [],
      productSource: [],
      editableData: {},
      loading: false,
      productIds: [], // 创建产品后返回的产品id列表
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
      this.productIds = [];
    },
    // 获取i18服务器ip
    getIPs() {
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
      const newTask = {
        state: "0",
        department: this.user.department,
        creator: this.user.userName, // 创建人-归档
        developer: this.user.userName, // 开发员
        entryAuditor: this.user.userName, // 词条审核员
        translator: this.user.userName, // 翻译员
        translationAuditor: this.user.userName, // 翻译审核员
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
        // console.log("部门下的人员信息data", data);
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
        }
        if (data.ENTRY_AUDITOR) {
          let auditor = [];
          data.ENTRY_AUDITOR.forEach((item) => {
            let op = {
              label: item.userName,
              value: item.userName,
            };
            auditor.push(op);
          });
          auditor.push({ label: "无", value: "" });
          this.options[record.id].entryAuditor = auditor;
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
          let translateAuditor = [];
          data.TRANSLATE_AUDITOR.forEach((item) => {
            let op = {
              label: item.userName,
              value: item.userName,
            };
            translateAuditor.push(op);
          });
          translateAuditor.push({ label: "无", value: "" });
          this.options[record.id].translatorAuditor = translateAuditor;
        }
      });
      // console.log(this.options);
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
      console.log(
        "保存值",
        this.dataSource,
        this.taskSource,
        this.productSource
      );
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
    // 添加版本
    addVersion(record) {
      console.log("添加版本", record);
      // let productId = this.editableData[record.id].productId;
      // if (productId === null || productId === "" || productId === undefined) {
      //   message.info("请先选择产品！");
      //   return;
      // }
      // this.addProductTask = this.editableData[record.id];
      // this.addProductTask.allVersions = this.options[record.id].versions;
      // this.addVersionVisible = true;
      // setModalAriaHidden(this, document);

      // let data = {
      //   name: this.version.name,
      //   details: this.version.details,// details备注
      //   productId: this.record.productId,
      // };
      // createVersion(data).then((res) => {
      //   message.success("添加成功！");
      //   this.$emit("versionOk", this.record);
      // });
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
        this.productIds = ["1", "2", "3", "4", "5", "6"];
        await randomError("创建产品失败");
        console.log(
          "创建产品成功",
          this.productSource,
          "return",
          this.productIds
        );
        // await createProductByLang(this.productSource).then((res) => {
        //   this.productIds = res.data.list;
        // });

        // 2.若某产品写入版本名称，则需创建版本
        for (let i = 0; i < this.dataSource.length; i++) {
          const verName = this.dataSource[i].versionName;
          if (verName) {
            const versionData = {
              name: verName,
              details: "",
              productId: this.productIds[i],
            };
            await randomError("创建产品版本失败");
            const verId = uuidv4();
            this.dataSource[i].versionId = verId;
            this.taskSource[i].versionId = verId;
            console.log("创建产品版本成功", this.dataSource, this.taskSource);
            // await createVersion(versionData).then((res) => {
            //   this.dataSource[i].versionId = res.data;
            //   this.taskSource[i].versionId = res.data;
            // });
          }
        }

        // 3.创建任务，该接口既实现任务的创建，又实现词条的导入，并且还会修改相应状态：任务-流程中，有翻译的词条-已审核，没翻译的词条-新建
        const link_map = new Map();
        for (let i = 0; i < this.linkList.length; i++) {
          const srcDIR = this.linkList[i][0];
          const srcTask = this.taskSource[i]["name"];
          this.taskSource[i]["productId"] = this.productIds[i]; // 把任务对应的产品信息写入
          link_map.set(srcTask, srcDIR);
        }
        const params = {
          ip: this.ip,
          link: link_map,
          translateTypes: this.translateTypes,
        };
        await randomError("创建任务失败");
        console.log("创建任务成功", params, this.taskSource);
        // await createTaskByLang(params, this.taskSource).then((res) => {});

        // 3.执行完毕重新初始化并关闭窗口
        this.handleClose();
      } catch (err) {
        message.error(`分支创建失败：${err}`);
        console.log("分支创建失败", err);
      }
    },
    handleClose() {
      this.init();
      this.$emit("createBranchClose");
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
    },
    clickInput(event) {
      event.stopPropagation();
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