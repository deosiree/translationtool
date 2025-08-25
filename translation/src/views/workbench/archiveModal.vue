<template>
  <CustomModal :visible="visible" :modalTitle="modalTitle" :modalWidth="modalWidth" :fullFlag="true" :okLoading="saveLoading" :showCancel="false"
    okText="归档并结束任务" @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div class="content">
      <div class="taskInfo">
        <div class="taskItem">任务名称：{{task.name}}</div>
        <div class="taskItem">产品名称：{{task.productName}}</div>
        <div class="taskItem">上级分类名称：{{task.classifyName}}</div>
        <div class="taskItem">翻译语种：{{task.translateType}}</div>
      </div>
      <div class="form">
        词条：
        <a-input v-model:value="keyWords" style="width:300px" size="small" placeholder='请输入词条搜索' />
        <span style="margin-left:10px">词条状态：</span>
        <a-select v-model:value="entryState" size="small" style="width: 300px" placeholder="请选择" allowClear>
          <a-select-option value="1">待审核</a-select-option>
          <a-select-option value="2">审核不通过</a-select-option>
          <a-select-option value="3">已审核</a-select-option>
        </a-select>
        <span style="margin-left:10px">翻译状态：</span>
        <a-select v-model:value="translateState" size="small" style="width: 300px" placeholder="请选择" allowClear>
          <a-select-option value="0">未翻译</a-select-option>
          <a-select-option value="1">未审核</a-select-option>
          <a-select-option value="2">审核不通过</a-select-option>
          <a-select-option value="3">已审核</a-select-option>
        </a-select>
        <a-button type="primary" size="small" style="margin-left:8px" @click="getTaskEntry">查询</a-button>
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
          <a-button type="primary" size="small" style="margin-left:auto"><template #icon>
              <SettingOutlined />
            </template>展示列</a-button>
        </a-popover>
      </div>
      <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource" :row-key="record => record.id" :scroll="tableHeight"
        :pagination='pagination' :loading="loading" :rowClassName="getRowClassName" :expandIconColumnIndex="2" :customRow="customRow" :row-selection="{ 
                selectedRowKeys: selectedRowKeys, 
                selectedRows: selectedRows,
                onChange: onSelectChange,
                selections:[
                    {key:'selectAll',text:'全部选择',onSelect:selectAllEntry},
                    {key:'clearAll',text:'取消选择',onSelect:clearAllEntry}
                ]
            }" ref="archiveTable" @resizeColumn="handleResizeColumn" @change="handleTableChange">
        <template #bodyCell="{ column, record,text }">
          <template v-if="column.dataIndex === 'entry'">
            <span v-text="text?text.replace(/\n/g, '\\n'):text"></span>
          </template>
          <template v-if="column.dataIndex === 'isExist'">
            <template v-if="record.isExist === 0">
              <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
            </template>
            <template v-if="record.isExist === 1">
              <a-badge color="#FBB31F" /><span style="color:#FBB31F">已存在</span>
            </template>
          </template>
          <template v-if="column.dataIndex === 'entryState'">
            <template v-if="record.entryState === 0">
              <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
            </template>
            <template v-if="record.entryState === 1">
              <a-badge color="#FBB31F" /><span style="color:#FBB31F">待审核</span>
            </template>
            <template v-if="record.entryState === 2">
              <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
            </template>
            <template v-if="record.entryState === 3">
              <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
            </template>
          </template>
          <template
            v-if="['englishTranslateState','frenchTranslateState','russianTranslateState','spanishTranslateState','translateState'].includes(column.dataIndex)">
            <template v-if="record[column.dataIndex] === '0' || record[column.dataIndex] === null">
              <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">未翻译</span>
            </template>
            <template v-if="record[column.dataIndex] === '1'">
              <a-badge color="#FBB31F" /><span style="color:#FBB31F">未审核</span>
            </template>
            <template v-if="record[column.dataIndex] === '2'">
              <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
            </template>
            <template v-if="record[column.dataIndex] === '3'">
              <a-badge color="#36BF7D" /><span style="color:#36BF7D">审核通过</span>
            </template>
          </template>
        </template>
        <template #expandIcon="props">
          <span v-if="props.record.children != null && props.record.children.length > 0">
            <div v-if="props.expanded" style="display: inline-block; margin-right: 10px" @click="(e) => {props.onExpand(props.record, e);}">
              <CaretDownOutlined />
            </div>
            <div v-else style="display: inline-block; margin-right: 10px" @click="(e) => {props.onExpand(props.record, e);}">
              <CaretRightOutlined />
            </div>
          </span>
          <span v-else style="margin-right:23px"></span>
        </template>
        <!-- 设置筛选菜单 -->
        <template #customFilterDropdown="{ setSelectedKeys, selectedKeys, confirm, clearFilters, column }">
          <div style="padding: 8px">
            <a-input ref="searchInput" :placeholder="`搜索 ${column.title}`" :value="selectedKeys[0]"
              style="width: 188px; margin-bottom: 8px; display: block" @change="e => setSelectedKeys(e.target.value ? [e.target.value] : [])"
              @pressEnter="handleSearch(selectedKeys, confirm, column.dataIndex)" />
            <a-button type="primary" size="small" style="width: 90px; margin-right: 8px"
              @click="handleSearch(selectedKeys, confirm, column.dataIndex)">
              <template #icon>
                <SearchOutlined />
              </template>搜索</a-button>
            <a-button size="small" style="width: 90px" @click="handleReset(clearFilters)">重置</a-button>
          </div>
        </template>
        <!-- 设置筛选图标 -->
        <template #customFilterIcon="{ filtered }">
          <SearchOutlined :style="{ color: filtered ? '#108ee9' : undefined }" />
        </template>
      </a-table>
    </div>
    <template v-slot:leftBottomBtn>
      <a-button @click="handleClose">取消</a-button>
      <a-button type="primary" ghost @click="placeOnFile" v-if="currentDepartment.needWriteBack">归档</a-button>
      <a-button type="primary" ghost @click="placeOnFile2" v-if="!currentDepartment.needWriteBack">结束任务</a-button>
    </template>
  </CustomModal>
  <CustomModal :visible="ipSelectModal" :okloading="writeBackLoading" modalTitle="回写服务器" @handleClose="ipSelectClose" @handleOK="ipSelectOK"
    @afterClose="ipSelectAfterClose">
    <div style="width:100%;height:100%">
      <a-form ref="ipModal" name="custom-validation" :model="ipModal">
        <a-form-item label="IP" name="ip" :rules="[{ required: true, message: '请选择IP!' }]">
          <a-select v-model:value="ipModal.ip" :options="ipOptions" placeholder="请选择IP" allowClear></a-select>
        </a-form-item>
      </a-form>
    </div>
  </CustomModal>
</template>
<script>
import CustomModal from "@/components/modal/index.vue";
import { cloneDeep, iteratee } from "lodash-es";
import { getEntryInfoList, getI18nAdress } from "@/http/api/workbench";
import { updateTaskInfo } from "@/http/api/task";
import { setInfo } from "@/http/api/i18Server";
import {
  CheckOutlined,
  CloseOutlined,
  ExclamationCircleOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
  SettingOutlined,
  SearchOutlined,
} from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import commonParam, { workbenchParams } from "@/utils/commonParam.js";
import {
  getColPref,
  changeColumn,
  getCurrentFormattedTime,
  handleSearch,
  handleReset,
  clearFilters,
  handleTableChange,
  selectAllEntry,
  clearAllEntry,
} from "@/utils/commonUtils";
import { defineComponent, ref, createVNode } from "vue";
export default {
  components: {
    CheckOutlined,
    CloseOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    SettingOutlined,
    SearchOutlined,
    ExclamationCircleOutlined,
    CustomModal,
  },
  emits: ["handleClose", "handleOK", "refresh"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
      default: "归档预览",
    },
    currentTask: {
      type: Object,
    },
    classifyLimit: {
      type: Object,
    },
  },

  data() {
    return {
      modalWidth: "70%",
      task: {},
      keyWords: "",
      // tableHeight: { x: "100%", y: "415px" },
      tableHeight: { x: "max-content", y: "415px" },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          width: 50,
          customRender: (text, record, index, column) => {
            return (
              text.index +
              1 +
              this.pagination.pageSize * (this.pagination.current - 1)
            );
          },
          fixed: "left",
          index: 0,
        },
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 200,
          resizable: true,
          fixed: "left",
          index: 1,
          // 添加 sorter 属性实现排序功能
          sorter: (a, b) => a.entry.localeCompare(b.entry),
          sortDirections: ["ascend", "descend"],
        },
        {
          title: "存在状态",
          dataIndex: "isExist",
          align: "center",
          width: 100,
          resizable: true,
          // fixed: "left",
          index: 2,
          filteredValue: null,
          filters: [
            { text: "已存在", value: 1 },
            { text: "新建", value: 0 },
          ],
          onFilter: (value, record) => record.isExist === value,
        },
        {
          title: "翻译状态",
          dataIndex: "translateState",
          align: "center",
          width: 100,
          resizable: true,
          // fixed: "left",
          index: 3,
        },
        {
          title: "翻译",
          dataIndex: "translate",
          align: "center",
          width: 200,
          resizable: true,
          index: 5,
          // 添加 sorter 属性实现排序功能
          sorter: (a, b) => a.entry.localeCompare(b.entry),
          sortDirections: ["ascend", "descend"],
        },
        {
          title: "tag",
          dataIndex: "tag",
          align: "center",
          width: 100,
          resizable: true,
          index: 6,
        },
        {
          title: "comment",
          dataIndex: "comment",
          align: "center",
          width: 100,
          resizable: true,
          index: 7,
        },
        {
          title: "abbr",
          dataIndex: "abbr",
          align: "center",
          width: 100,
          resizable: true,
          index: 14,
        },
        {
          title: "审核意见",
          dataIndex: "auditSuggess", // 动态的
          // dataIndex: "this.languageList.auditSuggest", // 翻译、翻译审核都是动态的
          align: "center",
          width: 100,
          resizable: true,
          fixed: "right",
          index: 99,
        },
        {
          title: "词条状态",
          dataIndex: "entryState",
          align: "center",
          width: 100,
          resizable: true,
          fixed: "right",
          index: 100,
        },
      ],
      dataSource: [],
      pagination: {
        pageSizeOptions: ["20", "50", "100"],
        showSizeChanger: true,
        defaultPageSize: 20,
        total: 0,
        current: 1,
        pageSize: 20,
        showTotal: (total) => `共 ${total} 条`,
        onChange: this.pageChange,
      },
      entryState: null,
      translateState: null,
      selectedRowIndex: null,
      overlayStyle: workbenchParams.overlayStyle,
      checkedColumn: workbenchParams.checkedColumn,
      // checkboxList: workbenchParams.checkboxList,
      // 移除固定列对应的配置项
      checkboxList: commonParam.checkboxList.filter(
        (item) =>
          ![
            "isExist",
            "translateState",
            "entryState",
            "entry",
            "translate",
          ].includes(item.value)
      ),
      user: null, // 当前用户的相关信息
      currentDepartment: {
        label: "部门名称",
        importTypes: [],
        needWriteBack: false,
        value: "name",
      }, // 当前用户所在部门的相关信息
      state: {
        searchText: "",
        searchedColumn: "",
      },
      filters: null,
      filteredData: [],
      saveLoading: false,
      selectedRowKeys: [],
      selectedRows: [],
      ipSelectModal: false,
      ipModal: {
        ip: null,
      },
      ipOptions: [],
      optionFlag: 0,
      writeBackLoading: false,
    };
  },

  created() {},
  mounted() {
    this.task = this.currentTask;
    this.$nextTick(() => {
      // 读取本地存储的用户偏好
      getColPref("colPref-archiveModal", 100, this, true);
      // 获取当前用户信息
      this.user = this.$store.state.user;
      // 获取当前用户所在部门的相关信息
      if (
        Object.keys(commonParam.departmentMap).includes(this.user.department)
      ) {
        this.currentDepartment =
          commonParam.departmentMap[this.user.department];
      } else {
        this.currentDepartment = commonParam.departmentMap["default"];
      }
    });
  },
  watch: {
    currentTask(newval, oldval) {
      this.task = newval;
      this.setTranslateColumn();
    },
  },
  methods: {
    // 设置翻译列展示的语言
    setTranslateColumn() {
      this.columns.forEach((item) => {
        if (item.title === "翻译") {
          item.dataIndex =
            workbenchParams.languageMap[this.task.translateType].code;
        }
        if (item.title === "翻译状态") {
          item.dataIndex =
            workbenchParams.languageMap[this.task.translateType].code +
            "TranslateState";
        }
      });
    },
    // 获取词条
    getTaskEntry() {
      let params = {
        taskID: this.task.id,
        entry: this.keyWords,
        entryState: this.entryState,
      };
      let data = [];
      if (this.translateState) {
        data.push(this.translateState);
      }
      this.loading = true;

      getEntryInfoList(params, data)
        .then((res) => {
          this.dataSource = res.data.list;
          this.loading = false;
        })
        .catch((err) => {
          this.loading = false;
          message.error(err.message);
        });
    },
    handleOK() {
      // 归档
      // 获取全部词条
      let params = {
        taskID: this.task.id,
        entry: "",
      };
      let data = [];
      this.saveLoading = true;
      getEntryInfoList(params, data).then((res) => {
        this.checkedEntry(res.data.list);
      });
    },
    checkedEntry(data) {
      if (!data) {
        return;
      }
      let code =
        workbenchParams.languageMap[this.task.translateType].code +
        "TranslateState";
      let flag = false;
      data.forEach((item) => {
        if (item.entryState != 3 || item[code] != 3) {
          // 含有未处理完的词条
          flag = true;
        }
      });

      if (flag) {
        this.saveLoading = false;
        Modal.error({
          title: "当前任务存在未处理完的词条，不可归档！",
          style: { top: "30%" },
        });
      } else {
        this.optionFlag = 1;
        this.ipSelectModal = true;
        this.getIPs();
        // Modal.confirm({
        //     title: '是否确定归档并结束任务?',
        //     icon: createVNode(ExclamationCircleOutlined),
        //     okText: '是',
        //     cancelText: '否',
        //     style: {top:'30%'},
        //     onOk: () => {
        //         this.saveLoading = false
        //         this.task.state = '6'
        //         this.task.endTime = new Date().toLocaleString().replaceAll('/','-')
        //         updateTaskInfo(this.task).then((res) => {
        //             message.success("已归档！")
        //             this.$emit('refresh')
        //         })
        //         // 回写数据
        //         let params = {
        //             taskID: this.task.id,
        //             translateType: this.task.translateType,
        //             isTag:0,
        //             isComment:0
        //         }
        //         setInfo(params,[]).then((res) => {

        //         }).catch((err) => {

        //         })
        //     },
        //     onCancel: () => {
        //         this.saveLoading = false
        //     }
        // });
      }
    },
    handleClose() {
      this.$emit("handleClose");
    },
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
    handleResizeColumn: (w, col) => {
      col.width = w;
    },
    // 添加表格行点击事件
    customRow(record, index) {
      return {
        onClick: (event) => {
          // this.selectedRowIndex = record.id
        },
      };
    },
    afterClose() {
      this.keyWords = "";
      this.pagination.current = 1;
      this.pagination.pageSize = 20;
      this.clearFilters();
    },
    // 展示列切换并保存用户偏好
    changeColumn(checkedValue) {
      changeColumn("colPref-archiveModal", 100, checkedValue, this, true);
    },
    // 筛选功能(可封装)
    // 列筛选
    handleSearch(selectedKeys, confirm, dataIndex) {
      handleSearch(selectedKeys, confirm, dataIndex, this);
      // confirm();
      // this.state.searchText = selectedKeys[0];
      // this.state.searchedColumn = dataIndex;
    },
    handleReset(clearFilters) {
      handleReset(clearFilters, this);
      // clearFilters({ confirm: true });
      // this.state.searchText = "";
    },
    // 清空表格筛选条件
    clearFilters() {
      clearFilters(this);
      // if (this.filters) {
      //   for (let key in this.filters) {
      //     this.columns.forEach((col) => {
      //       if (col.dataIndex === key) {
      //         col.filteredValue = null;
      //       }
      //     });
      //   }
      // }
    },
    // 表格change事件
    handleTableChange(pagination, filters) {
      handleTableChange(pagination, filters, this);
      // this.filters = filters;
      // for (let key in filters) {
      //   this.columns.forEach((col) => {
      //     if (col.dataIndex === key) {
      //       col.filteredValue = filters[key];
      //     }
      //   });
      // }
      // // 获取筛选后的数据
      // let isExistData = this.dataSource.filter((item) => {
      //   return filters.isExist && filters.isExist.includes(item.isExist);
      // });
      // let sourceData = this.dataSource.filter((item) => {
      //   return (
      //     filters.entrySource && item.entrySource.includes(filters.entrySource)
      //   );
      // });
      // this.filteredData = this.intersection(isExistData, sourceData);
    },
    // // 两个数组取并集(可封装)
    // intersection(nums1, nums2) {
    //   if (nums1.length === 0) {
    //     return nums2;
    //   }
    //   if (nums2.length === 0) {
    //     return nums1;
    //   }
    //   let a = new Set(nums1);
    //   let b = new Set(nums2);
    //   let arr = Array.from(new Set([...b].filter((x) => a.has(x))));
    //   return arr;
    // },

    // 动态设置表格高度
    setTableHeight(height, type) {
      if (type === "full") {
        this.tableHeight.y = height - 230;
      } else if (type === "reduce") {
        this.tableHeight.y = 415;
      }
    },
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;
    },
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      this.selectedRows = selectedRows;
    },
    selectAllEntry() {
      selectAllEntry(this);
      // this.selectedRowKeys = [];
      // this.selectedRows = [];
      // let dataToSelect;
      // if (this.filters && (this.filters.isExist || this.filters.entrySource)) {
      //   // 确保 filteredData 是最新的筛选结果
      //   dataToSelect = this.dataSource.filter((item) => {
      //     const isExistMatch =
      //       !this.filters.isExist ||
      //       this.filters.isExist.includes(item.isExist);
      //     const entrySourceMatch =
      //       !this.filters.entrySource ||
      //       item.entrySource.includes(this.filters.entrySource);
      //     return isExistMatch && entrySourceMatch;
      //   });
      // } else {
      //   dataToSelect = this.dataSource;
      // }
      // dataToSelect.forEach((item) => {
      //   this.selectedRowKeys.push(item.id);
      //   this.selectedRows.push(item);
      // });
    },
    clearAllEntry() {
      clearAllEntry(this);
      // this.selectedRowKeys = [];
      // this.selectedRows = [];
    },
    // 归档  回写数据
    writeBackFun() {
      if (this.selectedRows.length === 0) {
        message.info("请选择词条");
        return;
      }
      // 回写数据
      let params = {
        taskID: this.task.id,
        translateType: this.task.translateType,
        isTag: 0,
        isComment: 0,
        i18nUrl: this.ipModal.ip,
      };
      setInfo(params, this.selectedRows)
        .then((res) => {
          message.success("归档成功！");
          this.selectedRowKeys = [];
          this.selectedRows = [];
          this.ipSelectModal = false;
          this.writeBackLoading = false;
        })
        .catch((err) => {
          message.error("归档失败！", err.message);
        });
    },
    // 归档按钮点击事件
    placeOnFile() {
      if (this.selectedRows.length === 0) {
        message.info("请选择词条");
        return;
      }
      this.ipSelectModal = true;
      this.optionFlag = 0;
      this.getIPs();
    },
    // 结束任务按钮点击事件
    placeOnFile2() {
      this.task.state = "6";
      this.task.endTime = getCurrentFormattedTime();
      updateTaskInfo(this.task).then((res) => {
        message.success("已结束任务！（词条状态更新为'已归档'）");
        this.$emit("refresh");
      });
    },
    ipSelectAfterClose() {
      this.ipModal.ip === null;
    },
    ipSelectOK() {
      this.writeBackLoading = true;
      this.$refs.ipModal
        .validate()
        .then(() => {
          if (this.optionFlag === 0) {
            // 归档
            this.writeBackFun();
          } else if (this.optionFlag === 1) {
            // 归档并结束任务+回写
            this.saveLoading = false;
            this.task.state = "6";
            // this.task.endTime = common.getCurrentFormattedTime();
            this.task.endTime = getCurrentFormattedTime();
            updateTaskInfo(this.task).then((res) => {
              message.success("已归档！");
              this.$emit("refresh");
            });
            // 回写数据
            let params = {
              taskID: this.task.id,
              translateType: this.task.translateType,
              isTag: 0,
              isComment: 0,
              i18nUrl: this.ipModal.ip,
            };
            setInfo(params, [])
              .then((res) => {
                this.ipSelectModal = false;
              })
              .catch((err) => {
                message.error(err.message);
              });
          }
        })
        .catch((err) => {
          message.error(err.message);
        })
        .finally(() => {
          this.writeBackLoading = false;
        });
    },
    ipSelectClose() {
      this.ipSelectModal = false;
      this.saveLoading = false;
      this.ipModal.ip = null;
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
          // if(item.state === '1'){
          //     this.ipModal.ip = item.ip
          // }
          this.ipOptions.push(ip);
        });
      });
    },
  },
};
</script>
<style scoped lang="less">
.ant-divider {
  margin: 15px 0;
}
.content {
  width: 100%;
  height: 100%;
  padding: 10px;
  background-color: #f3f3f3;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 16px;
  align-self: stretch;

  .taskInfo {
    display: flex;
    padding: 4px 0px;
    align-items: center;
    gap: 32px;
    align-self: stretch;

    .taskItem {
      display: flex;
      align-items: center;
      flex: 1 0 0;
    }
  }
  .form {
    display: flex;
    align-items: center;
    align-self: stretch;
    width: 100%;
  }
}
.ant-table-cell .ant-form-item {
  margin-bottom: 0%;
}
:deep(.ant-pagination) {
  margin: 8px 0;
}
</style>