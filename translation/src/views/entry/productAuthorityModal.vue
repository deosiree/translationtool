<template>
  <Modal :visible="visible" :modalTitle="modalTitle" :modalWidth="modalWidth" @handleClose="handleClose" @handleOK="handleOK"
    @afterClose="afterClose">
    <div class="content">
      <a-input v-model:value="keyWords" placeholder="关键字搜索" @pressEnter="getPermissonByUserProduct">
        <template #suffix>
          <SearchOutlined style="color: #DCDCDC;" />
        </template>
      </a-input>
      <a-table class="ant-table-striped" :columns="columns" :data-source="dataSource" :scroll="tableHeight" :pagination='false'
        :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" :row-key="record => record.id" :defaultExpandAllRows="true"
        ref="authorTable" :loading="loading" bordered>
        <template #bodyCell="{ column, record }">
          <template v-if="['writeState', 'readState'].includes(column.dataIndex)">
            <a-checkbox v-model:checked="record[column.dataIndex]" @change="clickCheckBox(record,column)"></a-checkbox>
          </template>
        </template>
        <template #expandIcon="props">
          <span v-if="props.record.children != null">
            <div v-if="props.expanded" style="display: inline-block; margin-right: 10px" @click="(e) => {props.onExpand(props.record, e);}">
              <CaretDownOutlined />
            </div>
            <div v-else style="display: inline-block; margin-right: 10px" @click="(e) => {props.onExpand(props.record, e);}">
              <CaretRightOutlined />
            </div>
          </span>
          <span v-else style="margin-right:23px"></span>
        </template>
      </a-table>
    </div>
  </Modal>
</template>
<script>
import Modal from "@/components/modal/index.vue";
import { message } from "ant-design-vue";
import { v4 as uuidv4 } from "uuid";
import {
  SearchOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
} from "@ant-design/icons-vue";
import {
  getPermissonByUserProduct,
  bindtPermissonByUserProduct,
} from "@/http/api/product";
export default {
  components: {
    Modal,
    SearchOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
  },
  emits: ["authorityClose", "editOk"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
      default: "产品权限配置",
    },
    productId: "",
  },

  data() {
    return {
      modalWidth: "720px",
      keyWords: "",
      currentProductId: "",
      columns: [
        { title: "用户名", dataIndex: "name", width: "40%" },
        { title: "查看", dataIndex: "readState", align: "center" },
        { title: "修改", dataIndex: "writeState", align: "center" },
      ],
      dataSource: [],
      // tableHeight: { x:'100%',y: '400px' },
      tableHeight: { x: "max-content", y: "400px" },
      loading: false,
      changeAuthor: {},
    };
  },

  created() {},
  mounted() {
    this.currentProductId = this.productId;
    this.$nextTick(() => {
      this.getPermissonByUserProduct();
    });
  },
  watch: {
    productId(newval, oldval) {
      this.currentProductId = newval;
      // console.log(newval)
      this.getPermissonByUserProduct();
    },
  },
  methods: {
    // 查询用户权限
    getPermissonByUserProduct() {
      if (this.currentProductId === null || this.currentProductId === "") {
        return;
      }

      this.loading = true;
      let params = {
        userName: this.keyWords,
        productId: this.currentProductId,
      };
      this.changeAuthor = {};
      getPermissonByUserProduct(params).then((res) => {
        this.loading = false;
        this.dataSource = res.data.list;
        this.ergodicTree(this.dataSource);
      });
    },
    // 遍历树结构生成id
    ergodicTree(data) {
      data.forEach((item) => {
        item.id = uuidv4();
        if (item.children != null) {
          this.ergodicTree(item.children);
        }
      });
    },
    // 权限列表复选框点击事件
    clickCheckBox(record, column) {
      this.changeAuthor[record.id] = record;
      // 设置子节点选中状态
      this.setChildren(record, column.dataIndex);
    },
    // 设置子的选中状态
    setChildren(record, field) {
      if (record.children != null) {
        record.children.forEach((item) => {
          item[field] = record[field];
          this.setChildren(item, field);
        });
      } else {
        return;
      }
    },
    handleOK() {
      let data = [];
      for (let key in this.changeAuthor) {
        if (this.changeAuthor[key].type === "department") {
          let arr = this.getChangeUser(this.changeAuthor[key]);
          data = data.concat(arr);
        } else {
          data.push(this.changeAuthor[key]);
        }
      }
      if (data.length > 0) {
        // 保存
        let params = {
          productID: this.currentProductId,
        };
        bindtPermissonByUserProduct(data, params).then((res) => {
          message.success("保存成功！");
          this.$emit("authorityClose");
        });
      }
    },
    getChangeUser(data, arr = []) {
      if (data.type === "department") {
        if (data.children != null) {
          data.children.forEach((item) => {
            this.getChangeUser(item, arr);
          });
        }
      } else {
        arr.push(data);
      }
      return arr;
    },
    handleClose() {
      this.$emit("authorityClose");
    },
    afterClose() {
      this.keyWords = "";
    },
  },
};
</script>
<style scoped>
.content {
  width: 100%;
  height: 100%;
  padding: 16px;
  background-color: #f3f3f3;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  align-self: stretch;
}
</style>