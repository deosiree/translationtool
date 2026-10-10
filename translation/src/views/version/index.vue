<template>
  <div style="width:100%;height:100%;" ref="box">
    <SearchForm ref="searchForm" @change="setTableHeight">
      <template v-slot:form>
        <a-row>
          <a-col :span="16">
            <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off">
              <a-form-item label="版本名称" name="versionName">
                <a-input v-model:value="search.versionName" placeholder="请输入版本名称"></a-input>
              </a-form-item>
            </a-form>
          </a-col>
          <a-col :span="8">
            <div class="operation">
              <a-button type="primary" size="middle" style="margin-left:10px" @click="getDataSource">查询</a-button>
              <a-button type="primary" size="middle" @click="reset" style="margin-left:10px;background-color:#36BF7D;border:#36BF7D">重置</a-button>
              <a-button type="primary" size="middle" style="margin-left:10px" @click="handleAdd" v-if="authority.includes('addVersionInfo')">
                <template #icon>
                  <PlusOutlined />
                </template>
                新增
              </a-button>
              <a-button type="primary" size="middle" style="margin-left:10px" @click="deleteBatch" v-if="authority.includes('deleteVersionInfo')">
                <template #icon>
                  <DeleteOutlined />
                </template>
                批量删除
              </a-button>
            </div>
          </a-col>
        </a-row>
      </template>

    </SearchForm>
    <a-table class="ant-table-striped" :columns="columns" :data-source="dataSource" :customRow="doubleClick"
      :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" :row-key="record => record.id" :scroll="tableHeight"
      :pagination='false' :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="versionTable" bordered>
      <template #bodyCell="{ column, text, record }">
        <template v-if="['name', 'notes'].includes(column.dataIndex)">
          <div>
            <template v-if="editableData[record.id]">
              <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" />
            </template>
            <template v-else>
              {{ text }}
            </template>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'isDefault'">
          <a-switch :checked="editableData[record.id] ? editableData[record.id].isDefault : record.isDefault" :checked-value=1 :unchecked-value=0
            size="small" checked-children="是" un-checked-children="否" :disabled="!authority.includes('updateVersionInfo')"
            @change="handelDefaultChange(record)" />
        </template>
        <template v-else-if="column.dataIndex === 'isSelect'">
          <a-switch :checked="editableData[record.id] ? editableData[record.id].isSelect : record.isSelect" :checked-value=1 :unchecked-value=0
            size="small" checked-children="是" un-checked-children="否" :disabled="!authority.includes('updateVersionInfo')"
            @change="handelSelectChange(record)" />
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <div class="editable-row-operations">
            <span v-if="editableData[record.id]">
              <a-button type="primary" ghost size="small" @click="save(record.id)">保存</a-button>
              <a-popconfirm title="是否取消?" ok-text='是' cancel-text='否' @confirm="cancel(record.id)">
                <a-button type="primary" ghost size="small">取消</a-button>
              </a-popconfirm>
            </span>
            <span v-else>
              <a-button type="primary" ghost size="small" v-if="authority.includes('updateVersionInfo')" @click="edit(record.id)">编辑</a-button>
              <a-popconfirm title="确认删除?" ok-text='是' cancel-text='否' @confirm="deleteRole(record.id)" v-if="authority.includes('deleteVersionInfo')">
                <a-button type="primary" ghost size="small">删除</a-button>
              </a-popconfirm>
            </span>
          </div>
        </template>
      </template>
    </a-table>
    <Pagination ref="pagination" :total="pagination.total" @pageChange="pageChange" />
  </div>
</template>
<script>
import {
  PlusOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons-vue";
import { cloneDeep } from "lodash-es";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import {
  queryVersionInfo,
  addVersionInfo,
  updateVersionInfo,
  deleteVersionInfo,
} from "@/http/api/version";
import Pagination from "@/components/page/pagination.vue";
import SearchForm from "@/components/search/searchForm.vue";
export default {
  components: {
    PlusOutlined,
    DeleteOutlined,
    Pagination,
    SearchForm,
  },
  data() {
    return {
      name: "version",
      versionName: "",
      selectedRowKeys: [],
      // tableHeight: { x:'100%',y: 0 },
      tableHeight: { x: "max-content", y: 0 },
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 70,
          customRender: (text, record, index, column) => {
            return text.index + 1;
          },
        },
        { title: "版本名称", dataIndex: "name", align: "center", width: "20%" },
        {
          title: "是否默认版本",
          dataIndex: "isDefault",
          align: "center",
          width: "15%",
        },
        {
          title: "是否可选版本",
          dataIndex: "isSelect",
          align: "center",
          width: "15%",
        },
        { title: "备注", dataIndex: "notes", align: "center" },
        // {title: '操作',dataIndex: 'operation',align:'center',width:200},
      ],
      dataSource: [],
      editableData: {},
      rowSelection: [],
      pagination: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      authority: [],
      search: {
        versionName: "",
      },
      operationFlag: false,
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
  },
  unmounted() {
    //注销window.onresize事件
    window.onresize = null;
  },
  methods: {
    // 搜索框收起 展开时  设置表格高度
    setTableHeight() {
      this.$nextTick(() => {
        let box = this.$refs.box.offsetHeight;
        let searchHeight = this.$refs.searchForm.$el.offsetHeight;
        let paginationHeight = this.$refs.pagination.$el.offsetHeight;
        this.tableHeight.y = box - searchHeight - paginationHeight - 60;
      });
    },
    //初始化
    init() {
      this.setTableHeight();
      this.pagination.current = this.$refs.pagination.current;
      this.pagination.pageSize = this.$refs.pagination.pageSizeOptions[0];
      //获取用户权限
      this.getAuthority();
      this.getDataSource();
    },
    //获取用户权限
    getAuthority() {
      let authoritys = this.$store.state.authority;
      authoritys.filter((item) => {
        if (item.name === this.name) {
          item.authorities.filter((temp) => {
            this.authority.push(temp.authorityCode);
          });
        }
      });
      // 权限中含有 编辑  删除 表格展示操作栏
      if (
        this.authority.includes("updateVersionInfo") ||
        this.authority.includes("deleteVersionInfo")
      ) {
        this.addOperationColumn();
        this.operationFlag = true;
      }
    },
    // 添加操作栏
    addOperationColumn() {
      let operation = {
        title: "操作",
        dataIndex: "operation",
        align: "center",
        width: 150,
      };
      this.columns.push(operation);
    },
    // 删除操作栏
    deleteOperationColumn() {
      if (!this.operationFlag) {
        this.columns.some((item, i) => {
          if (item.dataIndex === "operation") {
            this.columns.splice(i, 1);
            return true;
          }
        });
      }
    },
    //获取版本列表
    getDataSource() {
      let params = {
        versionName: this.search.versionName,
        pageIndex: this.pagination.current,
        pageSize: this.pagination.pageSize,
      };
      queryVersionInfo(params).then((res) => {
        this.dataSource = res.data.list;
        this.pagination.total = res.data.totalNum;
      });
    },
    //分页
    pageChange(current, pageSize) {
      this.pagination.current = current;
      this.pagination.pageSize = pageSize;
      this.getDataSource();
    },
    //编辑
    edit(id) {
      this.editableData[id] = cloneDeep(
        this.dataSource.filter((item) => id === item.id)[0]
      );
    },
    //取消
    cancel(id) {
      delete this.editableData[id];
      if (id.startsWith("new")) {
        //从dataSource中删除
        this.dataSource.some((item, i) => {
          if (item.id === id) {
            this.dataSource.splice(i, 1);
            return true;
          }
        });
      }
      this.deleteOperationColumn();
    },
    //保存
    save(id) {
      if (
        this.editableData[id].name === "" ||
        this.editableData[id].name === null
      ) {
        message.warning("请输入版本名称！");
        return;
      }
      if (id.startsWith("new")) {
        //调用新增接口
        addVersionInfo(this.editableData[id]).then((res) => {
          this.dataSource.filter((item) => {
            if (item.id === id) {
              item.id = res.data;
              return true;
            }
          });
          message.success("新增成功！");
          this.changeDataSource(this.editableData[id]);
          delete this.editableData[id];
          this.deleteOperationColumn();
        });
      } else {
        //调用修改接口
        updateVersionInfo(this.editableData[id]).then((res) => {
          message.success("编辑成功！");
          Object.assign(
            this.dataSource.filter((item) => id === item.id)[0],
            this.editableData[id]
          );
          this.changeDataSource(this.editableData[id]);
          delete this.editableData[id];
          this.deleteOperationColumn();
        });
      }
    },
    // 新增和修改时 不刷新数据  临时修改
    changeDataSource(record) {
      if (record.isDefault === 1) {
        this.dataSource.filter((item) => {
          if (item.id !== record.id) {
            item.isDefault = 0;
          }
        });
      }
    },
    //双击表格行 可编辑
    doubleClick(record, index) {
      return {
        onDblclick: (event) => {
          if (this.authority.includes("updateVersionInfo")) {
            this.editableData[record.id] = cloneDeep(
              this.dataSource.filter((item) => record.id === item.id)[0]
            );
          }
        },
      };
    },
    //新增
    handleAdd() {
      if (!this.operationFlag) {
        this.addOperationColumn();
      }
      const newData = {
        id: `new${this.dataSource.length + 1}`,
        name: "",
        isSelect: 0,
        isDefault: 0,
        notes: "",
      };
      this.dataSource.push(newData);
      this.editableData[newData.id] = newData;
      // 滚动到最底部
      this.$nextTick(() => {
        let container =
          this.$refs.versionTable.$el.querySelector(".ant-table-body");
        container.scrollTop = container.scrollHeight;
      });
    },
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys;
    },
    // 批量删除
    deleteBatch() {
      if (this.selectedRowKeys.length === 0) {
        message.warn("请选择需要删除的版本！");
        return;
      }
      Modal.confirm({
        title: "是否确认删除?",
        icon: createVNode(ExclamationCircleOutlined),
        // content: 'Bla bla ...',
        okText: "确认",
        cancelText: "取消",
        style: { top: "30%" },
        onOk: () => {
          deleteVersionInfo(this.selectedRowKeys).then((res) => {
            message.success("删除成功！");
            this.getDataSource();
            this.selectedRowKeys = [];
          });
        },
      });
    },
    // 删除
    deleteRole(id) {
      let data = [id];
      deleteVersionInfo(data).then((res) => {
        message.success("删除成功！");
        this.getDataSource();
      });
    },
    //是否默认
    handelDefaultChange(record) {
      // console.log("default:",record.isDefault)

      let status = record.isDefault === 1 ? 0 : 1;
      // 非新增和编辑的版本
      if (!this.editableData[record.id]) {
        record.isDefault = status;
        updateVersionInfo(record)
          .then((res) => {
            message.success("设置成功！");
            this.getDataSource();
          })
          .catch((err) => {
            this.getDataSource();
          });
      } else {
        this.editableData[record.id].isDefault = status;
      }
    },
    //是否可选
    handelSelectChange(record) {
      let status = record.isSelect === 1 ? 0 : 1;
      // 非新增和编辑的角色
      if (!this.editableData[record.id]) {
        record.isSelect = status;
        updateVersionInfo(record)
          .then((res) => {
            message.success("设置成功！");
            this.getDataSource();
          })
          .catch((err) => {
            this.getDataSource();
          });
      } else {
        this.editableData[record.id].isSelect = status;
      }
    },
    reset() {
      this.search.versionName = "";
      this.pagination.current = 1;
      this.$refs.pagination.current = 1;
      this.getDataSource();
    },
  },
};
</script>
<style lang="less">
@import url("@/assets/style/common.less");
</style>
<style scoped lang="less">
.editable-row-operations button {
  margin-right: 8px;
}
.operation {
  text-align: right;

  .ant-btn {
    margin-left: 10px;
  }
}
</style>