<template>
  <div class="dictBox" ref="box">
    <a-row type="flex">
      <a-col flex="240px" class="dictionaryBox" ref="treeBox">
        <div class="dictSearch">
          <a-select v-model:value="ip" :options="ips" @change="ipChange" style="width:90%" placeholder="请选择IP" allowClear></a-select>
          <a-input v-model:value="keyWords" placeholder="关键字搜索" style="width:90%;margin-top:5px" @pressEnter="getDictionarys">
            <template #suffix>
              <SearchOutlined style="color: #DCDCDC;" />
            </template>
          </a-input>
          <a-tooltip placement="top">
            <template #title>
              <span>添加辞典</span>
            </template>
            <PlusSquareOutlined style="color:#369fff;margin-left: auto;font-size:16px;" @click="addDict" />
          </a-tooltip>
        </div>
        <div class="dictionary">
          <a-tree v-model:expandedKeys="expandedKeys" :defaultExpandAll="true" :selectedKeys="selectedTreeKeys" block-node :tree-data="dicts"
            @select="clickTree">
            <template #title="{ key: treeKey, title}">
              <template v-if="editVersion[treeKey]">
                <a-input v-model:value="editVersion[treeKey]" size="small" style="width:80%" @click="clickInput" @pressEnter="editVersionOk(treeKey)">
                </a-input>
              </template>
              <template v-else>
                <a-dropdown :trigger="['contextmenu']">
                  <span>{{ title }}</span>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item>
                        <a-popconfirm title="确定要删除吗?" ok-text="确定" cancel-text="取消" @confirm="removeDic(treeKey)">删除辞典
                        </a-popconfirm>
                      </a-menu-item>
                      <a-menu-item>
                        <a-popconfirm title="确定要清空吗?" ok-text="确定" cancel-text="取消" @confirm="clearDic(treeKey)">清空数据
                        </a-popconfirm>
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </template>

            </template>
          </a-tree>
          <span v-if="dicts.length === 0" style="color: rgba(0, 0, 0, 0.40);margin-left: 40%;">暂无数据</span>
        </div>
      </a-col>
      <a-col flex="auto" class="taskBox">
        <div class="dicBox">
          <SearchBox ref="search">
            <template v-slot:form>
              <a-form :model="search" name="horizontal_login" layout="inline" autocomplete="off" :label-col="labelCol">
                <a-form-item label="词条" name="entry">
                  <a-input v-model:value="search.entry" placeholder="请输入内容"></a-input>
                </a-form-item>
                <a-form-item label="Tag" name="tag">
                  <a-input v-model:value="search.tag" placeholder="请输入内容"></a-input>
                </a-form-item>
                <a-form-item label="来源" name="common">
                  <a-input v-model:value="search.common" placeholder="请输入内容"></a-input>
                </a-form-item>
              </a-form>
            </template>
            <template v-slot:operate>
              <a-button type="primary" size="middle" class="resetBtn" @click="reset">重置</a-button>
              <a-button type="primary" size="middle" @click="queryDictronary">查询</a-button>
            </template>
          </SearchBox>
          <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
            <template v-slot:operate>
              <div ref="button" v-if="true" style="margin-bottom:8px;display:flex;gap:10px">
                <a-button type="primary" size="small" danger @click="deleteDictData">
                  <template #icon>
                    <DeleteOutlined />
                  </template>
                  删除
                </a-button>
                <a-button type="primary" size="small" class="yellowBtn" @click="editData">
                  <template #icon>
                    <EditOutlined />
                  </template>
                  编辑
                </a-button>
                <a-button type="primary" size="small" @click="addDictTerm">
                  <template #icon>
                    <PlusOutlined />
                  </template>
                  新增
                </a-button>
              </div>
            </template>
            <template v-slot:data>
              <div style="width:100%;position: absolute;">
                <a-table class="ant-table-striped" :columns="columns" :data-source="dataSource" :pagination="pagination" :scroll="tableHeight"
                  :row-class-name="(_record, index) => (index % 2 === 1 ? 'table-striped' : null)" :row-key="record => record.id" :loading="loading"
                  :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange}" ref="dictionaryTable" bordered>
                  <template #bodyCell="{ column, text }">
                    <template v-if="column.dataIndex === 'translation'">
                      <pre>{{ text }}</pre>
                    </template>
                    <!--Tag列 -->
                    <template v-if="column.dataIndex === 'tag'">
                      <span>
                        <a-tag color="cyan" class="tag-content">
                          <span>{{ text }}</span>
                        </a-tag>
                      </span>
                    </template>
                  </template>
                </a-table>
              </div>
            </template>
          </DataBox>
        </div>

      </a-col>
    </a-row>
  </div>
  <Dict :visible="dictVisible" :currentIP="ip" @modalClose="dictClose" />
  <DictTerm ref="dictTermRef" :visible="dictTermVisible" :currentDict="currentDict" :modalTitle="dictTitle" :currentData="currentData" :currentIP="ip"
    @modalClose="dictTermClose" />
</template>
<script>
import '@/assets/style/common.less'
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import Dict from "@/views/dictionary/dictModal.vue";
import DictTerm from "@/views/dictionary/dictTermModal.vue";
import {
  SearchOutlined,
  PlusOutlined,
  DeleteOutlined,
  PlusSquareOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import {
  getDictionary,
  clearDic,
  removeDic,
  removeDicTerms,
  getAllDictionary,
  valDictionary,
} from "@/http/api/i18Server";
import { getDictory, getI18nAdress } from "@/http/api/workbench";
import { v4 as uuidv4 } from "uuid";
export default {
  components: {
    SearchBox,
    DataBox,
    SearchOutlined,
    PlusOutlined,
    DeleteOutlined,
    PlusSquareOutlined,
    ExclamationCircleOutlined,
    EditOutlined,
    Dict,
    DictTerm,
  },
  emits: ["viewEntry"],
  props: {},
  data() {
    return {
      box: 0,
      keyWords: "",
      expandedKeys: [],
      selectedTreeKeys: [],
      dicts: [],
      editVersion: {},
      search: {
        entry: "",
        tag: "",
        common: "",
      },
      labelCol: { style: { width: "84px" } },
      tableTitle: "数据列表",
      dataHeight: 400,
      tableHeight: { x: "100%", y: 0 },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
          width: 60,
          customRender: (text, record, index, column) => {
            return text.index + 1;
          },
        },
        { title: "词条", dataIndex: "source", width: 200 },
        { title: "来源", dataIndex: "comments", width: 200 },
        { title: "Tag", dataIndex: "tag", width: 150 },
        { title: "翻译", dataIndex: "translation" },
      ],
      dataSource: [],
      dictVisible: false,
      selectedRowKeys: [],
      selectedRows: [],
      dictTermVisible: false,
      currentDict: "",
      pagination: {
        showSizeChanger: true,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      dictTitle: "",
      currentData: {},
      dictTypes: [
        { label: "数据库", value: "db" },
        { label: "枚举", value: "enum" },
        { label: "配置文件", value: "config" },
        { label: "i18n_tr", value: "tr" },
        { label: "其他", value: "other" },
      ],
      ip: null,
      ips: [],
    };
  },

  created() {},
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
  watch: {},
  methods: {
    init() {
      this.setTableHeight();
      // this.getDictionarys()
      this.getI118IPs();
    },
    clickInput(event) {
      event.stopPropagation();
    },
    // 动态设置表格高度
    setTableHeight() {
      this.$nextTick(() => {
        // 设置列表父元素高度
        let box = this.$refs.box.offsetHeight;
        let searchHeight = this.$refs.search.$el.offsetHeight;
        try {
          let operationAreaHeight = this.$refs.operationArea.$el.offsetHeight;
          this.dataHeight = box - searchHeight - operationAreaHeight;
        } catch (error) {
          this.dataHeight = box - searchHeight;
        }

        // 设置表格高度
        let buttonHeight = 0;
        try {
          buttonHeight = this.$refs.button.offsetHeight + 8;
        } catch (error) {}
        this.tableHeight.y = this.dataHeight - buttonHeight - 150;
      });
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
    // 获取辞典列表
    getDictionarys() {
      let params = {
        i18nUrl: this.ip,
      };
      getDictionary(params).then((res) => {
        this.dicts = [];
        if (res.data.list === null) {
          return;
        }
        res.data.list.forEach((item) => {
          let option = {
            title: item,
            key: item,
          };
          if (this.keyWords.trim() != "") {
            if (item.includes(this.keyWords.trim())) {
              this.dicts.push(option);
            }
          } else {
            this.dicts.push(option);
          }
        });
        this.listToTree();
      });
    },
    // 将辞典数据转为树结构
    listToTree() {
      let tree = [];
      // this.dicts = [
      //     {key:'tr/svc_filemgr',title:'tr/svc_filemgr1111111111111111111111'},
      //     {key:'tr/filemagr',title:'tr/filemagr'},
      //     {key:'db/sysmdl',title:'db/sysmdl'},
      //     {key:'db/fileinfo',title:'db/fileinfo'},
      //     {key:'enum/secinfo',title:'enum/secinfo'},
      //     {key:'enum/sysmdl',title:'enum/sysmdl'},
      //     {key:'config/aa',title:'config/aa'},
      //     {key:'config/bb',title:'config/bb'},
      //     {key:'pingtai',title:'pingtai'},
      //     {key:'user',title:'user'},
      //     {key:'user1',title:'user'},
      //     {key:'user2',title:'user'},
      //     {key:'user3',title:'user'},
      //     {key:'user4',title:'user'},
      //     {key:'user5',title:'user'},
      //     {key:'user6',title:'user'},
      //     {key:'user7',title:'user'},
      //     {key:'user8',title:'user'},
      //     {key:'user9',title:'user'},
      //     {key:'user10',title:'user'},
      //     {key:'user11',title:'user'},
      //     {key:'user12',title:'user'},
      //     {key:'user13',title:'user'},
      //     {key:'user14',title:'user'},
      //     {key:'user14',title:'user'},
      //     {key:'user16',title:'user'},
      //     {key:'user17',title:'user'},
      //     {key:'user18',title:'user'},
      //     ]
      this.dicts.forEach((item) => {
        if (item.title.includes("/")) {
          let paras = item.title.split("/");
          let parentTitle = this.dictTypes.find(
            (type) => type.value === paras[0]
          ).label;
          let childTitle = paras[1];
          let parent = tree.find((t) => t.title === parentTitle);
          if (!parent) {
            parent = {
              title: parentTitle,
              key: paras[0],
              selectable: false,
              children: [],
            };
            tree.push(parent);
          }
          parent.children.push({ title: childTitle, key: item.key });
        } else {
          let parent = tree.find((t) => t.title === "其他");
          if (!parent) {
            parent = {
              title: "其他",
              key: "other",
              selectable: false,
              children: [],
            };
            tree.push(parent);
          }
          parent.children.push({ title: item.title, key: item.key });
        }
      });
      this.dicts = tree;
    },
    // 历史版本点击事件
    clickTree(selectedKeys, e) {
      if (e.selected) {
        this.selectedTreeKeys = selectedKeys;
      } else {
        this.selectedTreeKeys = [e.node.key];
      }
      this.queryDictronary();
    },
    // 获取辞典内容
    queryDictronary() {
      // console.log(this.selectedTreeKeys)
      if (this.selectedTreeKeys.length === 0) {
        return;
      }
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.loading = true;
      this.search.fileName = this.selectedTreeKeys[0];
      this.search.i18nUrl = this.ip;
      getDictory(this.search)
        .then((res) => {
          this.dataSource = res.data.list;
          this.dataSource.forEach((item) => {
            item.id = uuidv4();
          });
          this.pagination.total = this.dataSource.length;
          this.loading = false;
        })
        .catch((err) => {
          this.loading = false;
          message.error("查询失败！");
        });
    },
    reset() {
      this.search = {
        entry: "",
        tag: "",
        common: "",
      };
      this.queryDictronary();
    },
    addDict() {
      this.dictVisible = true;
    },
    dictClose(val) {
      this.dictVisible = false;
      if (val) {
        this.getDictionarys();
      }
    },
    // 删除辞典
    removeDic(dicName) {
      let param = {
        dicName: dicName,
        i18nUrl: this.ip,
      };
      removeDic(param)
        .then((res) => {
          message.success("删除成功！");
          this.getDictionarys();
        })
        .catch((err) => {
          message.error("删除失败！");
        });
    },
    // 清空辞典
    clearDic(dicName) {
      let params = {
        dicName: dicName,
        i18nUrl: this.ip,
      };
      clearDic(params)
        .then((res) => {
          message.success("清空成功！");
          this.queryDictronary();
        })
        .catch((err) => {
          message.error("清空失败！");
        });
    },
    // 辞典生效
    valDictionary(dicName) {
      let params = {
        dicName: dicName,
      };
      valDictionary(params)
        .then((res) => {
          message.success("已生效！");
          this.queryDictronary();
        })
        .catch((err) => {
          message.error("生效失败！");
        });
    },
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
    },
    // 删除辞典中的数据
    deleteDictData() {
      if (
        this.selectedRows.length === 0 ||
        this.selectedTreeKeys.length === 0
      ) {
        message.info("请选择！");
        return;
      }
      let dict = this.selectedTreeKeys[0];
      let data = this.selectedRows;
      let _this = this;
      Modal.confirm({
        title: "是否确定删除当前选择的数据?",
        icon: createVNode(ExclamationCircleOutlined),
        content: "",
        okText: "确定",
        okType: "danger",
        cancelText: "取消",
        onOk() {
          let param = {
            dicName: dict,
            i18nUrl: _this.ip,
          };
          removeDicTerms(param, data)
            .then((res) => {
              message.info("删除成功！");
              _this.queryDictronary();
              _this.selectedRowKeys = [];
              _this.selectedRows = [];
            })
            .catch((err) => {
              message.error("删除失败！");
            });
        },
        onCancel() {},
      });
    },
    // 添加辞典内容
    addDictTerm() {
      if (this.selectedTreeKeys.length === 0) {
        message.info("请选择辞典！");
        return;
      }
      this.currentDict = this.selectedTreeKeys[0];
      this.dictTermVisible = true;
      this.dictTitle = "新增辞典内容";
      this.$refs.dictTermRef.init();
    },
    dictTermClose(flag) {
      this.dictTermVisible = false;
      if (flag) {
        this.queryDictronary();
      }
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
    },
    // 编辑
    editData() {
      if (this.selectedTreeKeys.length === 0 || this.selectedRows.length != 1) {
        message.info("请选择一条数据！");
        return;
      }
      this.currentDict = this.selectedTreeKeys[0];
      this.currentData = this.selectedRows[0];
      this.dictTermVisible = true;
      this.dictTitle = "编辑辞典内容";
      this.$refs.dictTermRef.init();
    },
    // IP切换事件
    ipChange(value) {
      this.getDictionarys();
    },
    getI118IPs() {
      this.ips = [];
      getI18nAdress().then((res) => {
        res.data.list.forEach((item) => {
          let ip = {
            label: item.ip,
            value: item.ip,
          };
          // if(item.state === '1'){
          //     this.ip = item.ip
          // }
          this.ips.push(ip);
        });
        // this.getDictionarys()
      });
    },
  },
};
</script>
<style scoped lang="less">
.dictBox {
  width: 100%;
  height: 100%;

  .ant-row {
    height: 100%;
  }

  .dictionaryBox {
    display: flex;
    padding: 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    align-self: stretch;
    border: 1px solid#DCDCDC;
    border-right: 0px;
    height: 100%;
    position: relative;

    .dictionary {
      width: calc(100% - 32px);
      height: calc(100% - 110px);
      overflow: auto;
      position: absolute;
      bottom: 16px;
    }

    .dictSearch {
      width: 100%;
      // display:flex;
      // align-items:center;
      // justify-content:center;
    }
  }

  .taskBox {
    // display: flex;
    // flex-direction: column;
    // align-items: flex-start;
    // gap: 16px;
    // flex: 1 0 0;
    // align-self: stretch;
    // border-left: none;
    position: relative;

    .dicBox {
      width: 100%;
      height: 100%;
      position: absolute;
    }
  }
}
.treeIcon {
  float: right;
  font-size: 16px;
  margin-top: 4px;
}
:deep(.ant-dropdown-trigger) {
  color: black;
}
</style>
