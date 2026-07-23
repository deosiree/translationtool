<template>
  <CustomModal :visible="visible" :modalTitle="modalTitle" :modalWidth="modalWidth" :fullFlag="true" :okLoading="saveLoading" :showCancel="false"
    okText="归档并结束任务" @handleClose="handleClose" @handleOK="handleOK" @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div class="content">
      <WorkbenchTaskInfo :task="task" />
      <WorkbenchFormBar>
        词条：
        <a-input v-model:value="keyWords" style="width:300px" size="small" placeholder='请输入词条搜索' />
        <span style="margin-left:10px">词条状态：</span>
        <EntryStateSelect :entryState="entryState" @update:entryState="entryState = $event" :size="'small'" :style="'width: 300px'" />
        <span style="margin-left:10px">翻译状态：</span>
        <TransStateSelect :translateState="translateState" @update:translateState="translateState = $event" :size="'small'" :style="'width: 300px'" />
        <a-button type="primary" size="small" style="margin-left:8px" @click="getTaskEntry">查询</a-button>
        <a-button v-if="canShowDelete" type="primary" size="small" danger style="margin-left:8px" @click="deleteTaskEntry">删除</a-button>
        <WorkbenchActionGroup inline-offset>
          <WorkbenchColumnActions
            v-model="checkedColumn"
            :columns="columnSettingsList"
            :overlay-style="overlayStyle"
            col-pref-name="colPref-archiveModal"
            :normal-width="100"
            :need-filter="true"
            @change="syncColumnsFromPref"
          />
        </WorkbenchActionGroup>
      </WorkbenchFormBar>
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
        <template #bodyCell="{ column,text }">
          <template v-if="column.dataIndex === 'entry'">
            <span v-text="text?text.replace(/\n/g, '\\n'):text"></span>
          </template>
          <template v-if="column.dataIndex === 'isExist'">
            <IsExistBadge :isExist="text" />
          </template>
          <template v-if="column.dataIndex === 'entryState'">
            <EntryStateBadge :entryState="text" />
          </template>
          <template v-if="translateStateList.includes(column.dataIndex)">
            <TransStateBadge :translateState="text" />
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
      <a-button type="primary" ghost @click="placeOnFile" v-if="$currentDepartment && $currentDepartment.ops.has('needIP')">归档</a-button>
      <a-button type="primary" ghost @click="placeOnFile2">结束任务</a-button>
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
import IsExistBadge from "@/components/stateBadge/isExistBadge.vue";
import EntryStateSelect from "@/components/select/entryStateSelect.vue";
import TransStateSelect from "@/components/select/transStateSelect.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import { cloneDeep, iteratee } from "lodash-es";
import { getEntryInfoList, getI18nAdress, deleteEntryInfoByTaskID } from "@/http/api/workbench";
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
import commonParam, { workbenchParams } from "@/constants/commonParam.js";
import { applyTable, syncColumnsFromPref as applyTableColumnsFromPref } from "@/components/ColumnFilter";
import { filterWbColsForCtx } from "@/components/ColumnFilter/columnBuilder.js";
import { wbAllCols, wbPresets } from "@/constants/commonParam.js";
import {
  handleSearch,
  handleReset,
  clearFilters,
  handleTableChange,
  handleResizeColumn,
  getRowClassName,
} from "@/utils/tableUtils";
import {
  WorkbenchFormBar,
  WorkbenchActionGroup,
  WorkbenchTaskInfo,
  WorkbenchColumnActions,
} from "@/components/Workbench";
import {
  selectAllEntry as selectAllEntryUtil,
  clearAllEntry as clearAllEntryUtil,
  onSelectChange as onSelectChangeUtil,
} from "@/utils/selectionUtils";
import { getCurrentFormattedTime } from "@/utils/dateUtils";
import { canDeleteAsEntryAuditor } from "@/utils/entryAuditorAuth";
import { defineComponent, ref, createVNode } from "vue";
export default {
  components: {
    CheckOutlined,
    CloseOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    SearchOutlined,
    ExclamationCircleOutlined,
    CustomModal,
    IsExistBadge,
    EntryStateSelect,
    TransStateSelect,
    EntryStateBadge,
    TransStateBadge,
    WorkbenchFormBar,
    WorkbenchActionGroup,
    WorkbenchTaskInfo,
    WorkbenchColumnActions,
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
      columns: [],
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
      overlayStyle: workbenchParams.overlayStyle, // 展示列样式
      columnSettingsList: [],
      checkboxList: [], // 展示列可选的值
      checkedColumn: [], // 展示列已选的值
      translateStateList: [
        ...commonParam.langTranslateStateList,
        "translateState",
      ],
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
    this.$nextTick(() => {
      // 获取当前用户信息
      this.user = this.$store.state.user;
    });
  },
  watch: {
    currentTask(newval, oldval) {
      this.task = newval;
      this.task.transMap = commonParam.languageMap[this.task.translateType];
    },
    visible: {
      async handler(newVal) {
        // console.log("打开工作台-归档", newVal);
        if (newVal) {
          this.$nextTick(() => {
            // 1.设置翻译列展示的语种
            applyTable(this, {
              allCols: wbAllCols,
              preset: wbPresets.archiveModal,
              ctx: {
                task: this.task,
                transMap: this.task.transMap,
                pagination: this.pagination,
              },
              colPrefName: "colPref-archiveModal",
              normalWidth: 100,
              needFilter: true,
              filterCols: filterWbColsForCtx,
            });
          });
        }
      },
    },
  },
  computed: {
    canShowDelete() {
      return canDeleteAsEntryAuditor(this.$store.state.user, this.task);
    },
  },
  methods: {
    syncColumnsFromPref() {
      applyTableColumnsFromPref(this);
    },
    // 删除词条（词条审核员 + 本任务指派人）
    deleteTaskEntry() {
      if (this.selectedRows.length === 0) {
        return;
      }
      Modal.confirm({
        title: "是否确定删除?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          let deleteIds = [];
          let delCount = {
            num: 0,
            childNum: 0,
          };
          this.selectedRows.forEach((item) => {
            deleteIds.push(item.id);
            delCount.num++;
            if (item.children && item.children.length > 0) {
              delCount.childNum += item.children.length;
              item.children.forEach((child) => {
                deleteIds.push(child.id);
              });
            }
          });
          this.selectedRowKeys = [];
          this.selectedRows = [];
          deleteEntryInfoByTaskID({ taskID: this.task.id }, deleteIds)
            .then((res) => {
              let text = `删除成功${delCount.num - delCount.childNum}条`;
              if (delCount.childNum > 0) {
                text += `(聚合${delCount.childNum}条)`;
              }
              message.success(text);
              this.getTaskEntry();
            })
            .catch((err) => {
              message.error("删除失败！", err.message);
            });
        },
        onCancel: () => {},
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
      let code = commonParam.languageMap[this.task.translateType].state;
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
      return getRowClassName(record, index, this.selectedRowIndex);
    },
    handleResizeColumn,
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
      onSelectChangeUtil(this, selectedRowKeys, selectedRows);
    },
    selectAllEntry() {
      selectAllEntryUtil(this);
    },
    clearAllEntry() {
      clearAllEntryUtil(this);
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
      Modal.confirm({
        title: "是否结束任务？",
        content: '词条状态将更新为"已归档"',
        icon: createVNode(ExclamationCircleOutlined),
        okText: "是",
        cancelText: "否",
        style: { top: "30%" },
        onOk: () => {
          this.task.state = "6";
          this.task.endTime = getCurrentFormattedTime();
          updateTaskInfo(this.task).then((res) => {
            message.success("已结束任务！（词条状态更新为'已归档'）");
            this.$emit("refresh");
          });
        },
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

}
.ant-table-cell .ant-form-item {
  margin-bottom: 0%;
}
:deep(.ant-pagination) {
  margin: 8px 0;
}
</style>