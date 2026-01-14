<template>
  <div class="box" ref="box">
    <SearchBox ref="search" @change="setTableHeight">
      <template v-slot:form>
        <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
          <!-- <a-form-item label="用户名" name="userName" style="width:20%">
            <a-input v-model:value="search.userName" placeholder="请输入任务名称"></a-input>
          </a-form-item> -->
          <a-form-item label="角色" name="roleName" style="width:20%">
            <a-input v-model:value="search.roleName" placeholder="请输入任务名称"></a-input>
          </a-form-item>
          <!-- <a-form-item label="部门" name="department" style="width:20%">
            <a-input v-model:value="search.department" placeholder="请输入任务名称"></a-input>
          </a-form-item> -->
          <!-- <a-form-item>
            <a-button type="primary" size="middle" @click="getRole">查询</a-button>
          </a-form-item> -->
        </a-form>
      </template>
      <template v-slot:operate>
        <a-button type="primary" size="middle" @click="getRole">查询</a-button>
      </template>
    </SearchBox>
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <template v-slot:operate>
        <div ref="button" v-if="true" style="margin-bottom:8px;display:flex;gap:8px">
          <!-- <a-button type="primary" size="small">
            <template #icon><PlusOutlined /></template>新增
          </a-button>
          <a-button type="primary" size="small">
            <template #icon><DeleteOutlined /></template>删除
          </a-button> -->
          <a-button type="primary" size="small" @click="batchSave"><template #icon>
              <SaveOutlined />
            </template>保存</a-button>
        </div>
      </template>
      <template v-slot:data>
        <div style="width:100%;position: absolute;">
          <a-table class="ant-table-striped" :columns="columns" :data-source="dataSource" :customRow="doubleClick"
            :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" :row-key="record => record.id" :scroll="tableHeight"
            :pagination='false' :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" ref="roleTable" bordered>
            <template #bodyCell="{ column, text, record }">
              <template v-if="column.dataIndex === 'menus'">
                <div>
                  <template v-if="editableData[record.id]">
                    <a-tree-select v-model:value="editableData[record.id]['menuId']" style="width: 90%" :tree-data="menus" tree-checkable
                      :show-checked-strategy="SHOW_ALL" :fieldNames="{label:'menuName',value:'id'}" :treeCheckStrictly="true" placeholder="请选择"
                      tree-node-filter-prop="label" />
                    <!-- <a-select v-model:value="editableData[record.id]['menuId']" :options="menusList" mode="multiple" placeholder="请选择"
                      :fieldNames="{label:'menuName',value:'id'}" style="width: 90%"></a-select> -->
                    <a-tooltip placement="top">
                      <template #title>
                        <span>保存</span>
                      </template>
                      <CheckOutlined style="color:#369FFF;margin-left:8px" @click="save(record)" />
                    </a-tooltip>
                    <a-tooltip placement="top">
                      <template #title>
                        <span>取消</span>
                      </template>
                      <CloseOutlined style="color:red;margin-left:8px" @click="cancel(record)" />
                    </a-tooltip>
                  </template>
                  <template v-else>
                    {{ text }}
                  </template>
                </div>
              </template>
            </template>
          </a-table>
        </div>
      </template>
    </DataBox>
  </div>
</template>
<script>
import {
  PlusOutlined,
  DeleteOutlined,
  SaveOutlined,
  ExclamationCircleOutlined,
  CheckOutlined,
  CloseOutlined,
} from "@ant-design/icons-vue";
import { cloneDeep, iteratee } from "lodash-es";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import { TreeSelect } from "ant-design-vue";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import { getRoleAndMenu, changeRoleAndMenu, getMenu } from "@/http/api/user";
import { setTableHeight } from "@/utils/tableUtils";
export default {
  components: {
    PlusOutlined,
    DeleteOutlined,
    SaveOutlined,
    CheckOutlined,
    CloseOutlined,
    SearchBox,
    DataBox,
  },
  data() {
    return {
      SHOW_ALL: TreeSelect.SHOW_ALL,
      name: "role",
      labelCol: { style: { width: "60px" } },
      search: {
        userName: "",
        roleName: "",
        department: "",
      },
      tableTitle: "角色配置:",
      dataHeight: 0,
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
        {
          title: "角色名称",
          dataIndex: "roleName",
          align: "center",
          width: "20%",
        },
        { title: "角色描述", dataIndex: "describe", align: "center" },
        {
          title: "权限设置",
          dataIndex: "menus",
          align: "center",
          width: "30%",
        },
      ],
      dataSource: [],
      editableData: {},
      rowSelection: [],
      menus: [],
      menusList: [],
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
    init() {
      this.setTableHeight();
      this.getRole();
      this.getMenu();
    },
    setTableHeight() {
      this.$nextTick(() => {
        setTableHeight(this, 8, 126, 0);
      });
    },
    getRole() {
      let params = {
        pageIndex: -1,
        pageSize: -1,
      };
      let data = {
        roleName: this.search.roleName,
      };
      getRoleAndMenu(data, params).then((res) => {
        this.dataSource = res.data.list;

        this.dataSource.forEach((item) => {
          let menus = "";
          if (item.menuName && item.menuName.length > 0) {
            menus = item.menuName.join(", ");
          }
          item.menus = menus;
        });

        this.editableData = {};
      });
    },
    getMenu() {
      getMenu().then((res) => {
        this.menus = res.data.list;
        // console.log(this.menus)
        this.menusList = this.treeToArray(this.menus);
        // console.log(this.menusList)
      });
    },
    //双击表格行 可编辑
    doubleClick(record, index) {
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
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys;
    },
    save(record) {
      let role = this.editableData[record.id];
      let menuData = [];
      role.menuId.forEach((item) => {
        // 获取所有的父菜单
        let value = "";
        if (item instanceof Object) {
          value = item.value;
        } else {
          value = item;
        }
        let menu = this.getAllParentArr(this.menus, value);
        menuData = menuData.concat(menu);
      });
      // 去重
      let set = new Set(menuData);
      let arr = Array.from(set);
      // 获取菜单id
      let data = [];
      arr.forEach((item) => {
        data.push(item.id);
      });
      // 保存
      // console.log(data)
      let params = { roleId: role.id };
      changeRoleAndMenu(data, params).then((res) => {
        message.success("编辑成功！");
        this.getRole();
        delete this.editableData[record.id];
      });
    },
    batchSave() {
      for (let key in this.editableData) {
        this.save(this.editableData[key]);
      }
    },
    cancel(record) {
      delete this.editableData[record.id];
    },
    selectMenu(value) {
      // console.log(value)
    },

    getAllParentArr(list, id) {
      for (let i in list) {
        if (list[i]["id"] == id) {
          return [list[i]];
        }
        if (list[i]["children"]) {
          let node = this.getAllParentArr(list[i]["children"], id);
          if (node) {
            return node.concat(list[i]);
          }
        }
      }
    },
    treeToArray(tree, arr = []) {
      tree.forEach((item) => {
        arr.push(item);
        if (item.children.length > 0) {
          this.treeToArray(item.children, arr);
        }
      });
      return arr;
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
  // border: 1px solid red;
}
</style>