<template>
  <div class="productEntryBox" ref="productEntryBox">
    <SearchBox ref="search" @change="setTableHeight" :operate="false">
      <template v-slot:form>
        <a-form :model="search" layout="inline" autocomplete="off" :label-col="labelCol">
          <a-row style="width:100%" class="search-row">
            <a-form-item label="词条" name="entry" style="margin-top: 8px">
              <a-textarea v-model:value="search.entry" placeholder="请输入内容" :auto-size="{ minRows: 1 }"></a-textarea>
            </a-form-item>
            <a-form-item label="词条状态" name="state" style="margin-top: 8px">
              <a-select v-model:value="search.entryState" placeholder="请选择" allowClear>
                <a-select-option value="0">新建</a-select-option>
                <a-select-option value="1">审核中</a-select-option>
                <a-select-option value="2">审核不通过</a-select-option>
                <a-select-option value="3">已审核</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="tag" name="tag" style="margin-top: 8px">
              <a-input v-model:value="search.tag" placeholder="请输入内容"></a-input>
            </a-form-item>
            <a-form-item label="二级分类" name="classfy2" style="margin-top: 8px">
              <!-- <a-input v-model:value="search.classfy2" placeholder="请输入内容"></a-input> -->
              <a-select v-model:value="search.classfy2" placeholder="请选择" :fieldNames="{label:'name',value:'name'}" :options='classify2Option'
                allowClear>
              </a-select>
            </a-form-item>
            <a-form-item label="词条来源" name="entrySource" style="margin-top: 8px">
              <a-input v-model:value="search.entrySource" placeholder="请输入内容"></a-input>
            </a-form-item>
            <a-form-item label="翻译语言" name="language" style="margin-top: 8px">
              <a-select v-model:value="search.language" placeholder="请选择" :fieldNames="{label:'name',value:'name'}" :options='translateTypes'
                allowClear>
              </a-select>
            </a-form-item>
            <a-form-item label="翻译状态" name="translateState" style="margin-top: 8px">
              <a-select v-model:value="search.translateState" placeholder="请选择" :options='translateStates' allowClear>
              </a-select>
            </a-form-item>
            <a-form-item label="翻译结果" name="translate" style="margin-top: 8px">
              <a-input v-model:value="search.translate" placeholder="请输入内容"></a-input>
            </a-form-item>
            <!-- <a-form-item label="翻译释义" name="interpretation" style="margin-top: 8px">
              <a-input v-model:value="search.interpretation" placeholder="请输入内容"></a-input>
            </a-form-item> -->
            <a-form-item label="comment" name="comment" style="margin-top: 8px">
              <a-input v-model:value="search.comment" placeholder="请输入内容"></a-input>
            </a-form-item>
            <a-form-item label="开始时间" name="startTime" style="margin-top: 8px">
              <a-date-picker v-model:value="startTime" />
            </a-form-item>
            <a-form-item label="结束时间" name="endTime" style="margin-top: 8px">
              <a-date-picker v-model:value="endTime" />
            </a-form-item>
          </a-row>
          <a-row style="width:100%" class="search-row" justify="end">
            <a-button type="primary" size="middle" class="resetBtn" @click="reset">重置</a-button>
            <a-button type="primary" size="middle" @click="conditionalQuery" style="margin-left:8px">查询</a-button>
          </a-row>
        </a-form>
      </template>
      <!-- <template v-slot:operate>
                <a-button type="primary" size="middle" class="resetBtn" @click="reset" style="margin-left:8px">重置</a-button>
                <a-button type="primary" size="middle" @click="conditionalQuery">查询</a-button>
            </template> -->
    </SearchBox>
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <template v-slot:label>
        产品版本： <a-select v-model:value="currentVersion" style="width: 150px" placeholder="请选择版本" :options='productVersions'
          :fieldNames="{label:'name',value:'id'}" size="small" @change="changeVersion" allowClear>
        </a-select>
      </template>
      <template v-slot:operate>
        <div ref="button" v-if="true" style="margin-bottom:8px;display:flex;gap:10px">
          <a-button type="primary" size="small" @click="createVersion" v-if="!createVersionFlag">批量选择</a-button>

          <a-button type="primary" size="small" @click="selectAllEntry" v-if="createVersionFlag" :loading="selectAllLoading">选择全部</a-button>
          <a-button type="primary" size="small" @click="cancelCreate" class="yellowBtn" v-if="createVersionFlag">取消选择</a-button>
          <a-badge :count="selectEntry.length" :overflow-count="99" v-if="createVersionFlag">
            <a-button type="primary" size="small" class="resetBtn" @click="viewCreateVersionEntry">已选词条</a-button>
          </a-badge>
          <!-- <a-button type="primary" size="small" @click="viewDictionary" v-if="user.department === '通用平台部' || user.department === '监控系统部'">查看辞典</a-button> -->
          <a-button type="primary" size="small" @click="addEntry"><template #icon>
              <PlusOutlined />
            </template>新增</a-button>
          <!-- <a-button type="primary" size="small" danger @click="deleteEntry" v-if="edit"><template #icon><DeleteOutlined /></template>删除</a-button> -->
          <!-- <a-button type="primary" size="small" @click="batchSave" v-if="edit"><template #icon><SaveOutlined /></template>保存</a-button> -->
          <!-- <a-button type="primary" size="small" class="resetBtn" ><template #icon><UpSquareOutlined /></template>升级</a-button> -->
          <a-button type="primary" size="small" @click="setSecondClassify" v-if="admin">二级分类管理</a-button>
          <!-- <a-button type="primary" size="small" v-if="admin" @click="importEntry">导入</a-button> -->
          <ImportButton @importSuccess="refreshTable" v-if="admin" :translateTypes="translateTypes" size="small" buttonTitle="更新翻译" />

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
            <a-button type="primary" size="small"><template #icon>
                <SettingOutlined />
              </template>展示列</a-button>
          </a-popover>
        </div>
      </template>
      <template v-slot:data>
        <div style="width:100%;position: absolute;">
          <a-config-provider :locale="locale">
            <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource"
              :row-selection="batchSelectFlag ? { selectedRowKeys: selectedRowKeys, onChange: onSelectChange,onSelect:onSelect,onSelectAll:onSelectAll} : null"
              :row-key="record => record.id" :scroll="tableHeight" :loading="loading" :rowClassName="getRowClassName" :pagination="pagination"
              ref="entryTable" @resizeColumn="handleResizeColumn" :customRow="customRow" @change="handleTableChange">
              <template #bodyCell="{ column, record, text }">

                <template v-if="column.dataIndex === 'entry'">
                  <div>
                    <template v-if="editableData[record.id]">
                      <a-form :model="editableData[record.id]" :rules="rules[record.id]" :ref="'form'+record.id.replaceAll('-','')+column.dataIndex"
                        autocomplete="off">
                        <a-form-item :name="column.dataIndex">
                          <a-textarea v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" @click="clickInput"
                            :auto-size="{ minRows: 1}" />
                        </a-form-item>
                      </a-form>
                    </template>
                    <template v-else>
                      <!-- {{ text }} -->
                      <span v-html="text"></span>
                      <!-- <span v-text="text.replace(/\n/g, '\\n')"></span> -->
                    </template>
                  </div>
                </template>
                <template v-if="inputColumn.includes(column.dataIndex)">
                  <div>
                    <template v-if="editableData[record.id]">
                      <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" @click="clickInput" />
                    </template>
                    <template v-else>
                      {{ text }}
                    </template>
                  </div>
                </template>
                <template v-if="translateColumn.includes(column.dataIndex)">
                  <div>
                    <template v-if="editableData[record.id]">
                      <a-form :model="editableData[record.id]" :rules="rules[record.id]" :ref="'form'+record.id.replaceAll('-','')+column.dataIndex"
                        autocomplete="off">

                        <a-form-item :name="column.dataIndex">
                          <a-textarea v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0" @click="clickInput"
                            :auto-size="{ minRows: 1}" />
                        </a-form-item>
                      </a-form>
                    </template>
                    <template v-else>
                      {{ text }}
                    </template>
                  </div>
                </template>
                <template v-if="column.dataIndex === 'classfy1'">
                  <div>
                    <template v-if="editableData[record.id]">
                      <a-select v-model:value="editableData[record.id][column.dataIndex]" style="width: 100%" placeholder="请选择"
                        :fieldNames="{label:'title',value:'title'}" :options='classify1Option' @change="getRowClassify2Option(record)" allowClear>
                      </a-select>
                    </template>
                    <template v-else>
                      {{ text }}
                    </template>
                  </div>
                </template>
                <template v-if="column.dataIndex === 'classfy2'">
                  <div>
                    <template v-if="editableData[record.id]">
                      <a-select v-model:value="editableData[record.id][column.dataIndex]" style="width: 100%" placeholder="请选择"
                        :fieldNames="{label:'name',value:'name'}" :options='rowClassify2Option[record.id]' allowClear>
                      </a-select>
                    </template>
                    <template v-else>
                      {{ text }}
                    </template>
                  </div>
                </template>
                <template v-if="column.dataIndex === 'tag'">
                  <div>
                    <template v-if="editableData[record.id]">
                      <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0;width:90%" @click="clickInput" />
                      <a-tooltip placement="top">
                        <template #title>
                          <span>多个tag按分号分割！</span>
                        </template>
                        <InfoCircleOutlined style="margin-left:3px" />
                      </a-tooltip>
                    </template>
                    <template v-else>
                      <!-- {{ text }} -->
                      <span>
                        <a-tag v-for="(tag, index) in companyCut(text)" :key="index" color="cyan" class="tag-content">
                          <span>{{ tag }}</span>
                        </a-tag>
                      </span>
                    </template>
                  </div>
                </template>
                <template v-if="column.dataIndex === 'entryState'">
                  <template v-if="record.entryState === 0">
                    <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
                  </template>
                  <template v-if="record.entryState === 1">
                    <a-badge color="#FBB31F" /><span style="color:#FBB31F">审核中</span>
                  </template>
                  <template v-if="record.entryState === 2">
                    <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
                  </template>
                  <template v-if="record.entryState === 3">
                    <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
                  </template>
                </template>
                <template
                  v-if="['englishTranslateState','russianTranslateState','spanishTranslateState','frenchTranslateState'].includes(column.dataIndex)">
                  <template v-if="record[column.dataIndex] === '0'">
                    <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">未翻译</span>
                  </template>
                  <template v-if="record[column.dataIndex] === '1'">
                    <a-badge color="#FBB31F" /><span style="color:#FBB31F">待审核</span>
                  </template>
                  <template v-if="record[column.dataIndex] === '2'">
                    <a-badge color="#ff0000" /><span style="color:#ff0000">审核不通过</span>
                  </template>
                  <template v-if="record[column.dataIndex] === '3'">
                    <a-badge color="#36BF7D" /><span style="color:#36BF7D">已审核</span>
                  </template>
                </template>
                <template v-if="column.dataIndex === 'operation'">
                  <div class="editable-row-operations">
                    <span v-if="editableData[record.id]">
                      <a-button type="primary" ghost size="small" @click.stop="save(record.id)">保存</a-button>
                      <a-button type="primary" ghost size="small" danger @click.stop="cancel(record.id)">取消</a-button>
                    </span>
                    <span v-else>
                      <a-button type="primary" ghost size="small" @click.stop="entryDetails(record)">详情</a-button>
                      <!-- <a-button type="primary" ghost size="small" @click.stop="entryUpgrade(record)">升级</a-button> -->
                    </span>
                  </div>
                </template>
              </template>
            </a-table>
          </a-config-provider>
          <!-- <a-pagination
                    style="float:right"
                    v-model:current="pagination.current"
                    v-model:page-size="pagination.pageSize"
                    :total="pagination.total"
                    :pageSizeOptions="pagination.pageSizeOptions"
                    :show-total="total => `共 ${total} 条`"
                    @change="this.pageChange"
                    /> -->
        </div>
      </template>
    </DataBox>
    <OperationArea ref="operationArea" :title="operationAreaTitle" :height="operationAreaHeight" v-if="showOperationArea" @close="closeOperationArea">
      <template v-slot:content>
        <div class="entryDetails">
          <table>
            <tr>
              <td class="tableTitle">翻译语种</td>
              <td>英文</td>
              <td>俄文</td>
              <td>西文</td>
              <td>法文</td>
            </tr>
            <tr>
              <td class="tableTitle">翻译结果</td>
              <td>{{currentEntry.english}}</td>
              <td>{{currentEntry.russian}}</td>
              <td>{{currentEntry.spanish}}</td>
              <td>{{currentEntry.french}}</td>
            </tr>
            <tr>
              <td class="tableTitle">翻译状态</td>
              <td>
                <template v-if="currentEntry.englishTranslateState === '3'">
                  <a-badge color="#36BF7D" /><span style="color:#36BF7D">{{currentEntry.englishChineseState}}</span>
                </template>
                <template v-else-if="currentEntry.englishTranslateState != null">
                  <a-badge color="#FBB31F" /><span style="color:#FBB31F">{{currentEntry.englishChineseState}}</span>
                </template>
              </td>
              <td>
                <template v-if="currentEntry.russianTranslateState === '3'">
                  <a-badge color="#36BF7D" /><span style="color:#36BF7D">{{currentEntry.russianChineseState}}</span>
                </template>
                <template v-else-if="currentEntry.russianTranslateState != null">
                  <a-badge color="#FBB31F" /><span style="color:#FBB31F">{{currentEntry.russianChineseState}}</span>
                </template>
              </td>
              <td>
                <template v-if="currentEntry.spanishTranslateState === '3'">
                  <a-badge color="#36BF7D" /><span style="color:#36BF7D">{{currentEntry.spanishChineseState}}</span>
                </template>
                <template v-else-if="currentEntry.spanishTranslateState != null">
                  <a-badge color="#FBB31F" /><span style="color:#FBB31F">{{currentEntry.spanishChineseState}}</span>
                </template>
              </td>
              <td>
                <template v-if="currentEntry.frenchTranslateState === '3'">
                  <a-badge color="#36BF7D" /><span style="color:#36BF7D">{{currentEntry.frenchChineseState}}</span>
                </template>
                <template v-else-if="currentEntry.frenchTranslateState != null">
                  <a-badge color="#FBB31F" /><span style="color:#FBB31F">{{currentEntry.frenchChineseState}}</span>
                </template>

              </td>
            </tr>
            <tr>
              <td class="tableTitle">选择</td>
              <td>
                <a-checkbox :disabled="currentEntry.englishTranslateState != '3'" v-model:checked="currentEntry.englishChecked">
                </a-checkbox>
              </td>
              <td>
                <a-checkbox :disabled="currentEntry.russianTranslateState != '3'" v-model:checked="currentEntry.russianChecked">
                </a-checkbox>
              </td>
              <td>
                <a-checkbox :disabled="currentEntry.spanishTranslateState != '3'" v-model:checked="currentEntry.spanishChecked">
                </a-checkbox>
              </td>
              <td>
                <a-checkbox :disabled="currentEntry.frenchTranslateState != '3'" v-model:checked="currentEntry.frenchChecked">
                </a-checkbox>
              </td>
            </tr>
          </table>
          <div class="details">
            <div>中文释义：{{currentEntry.chineseInterpretation}}</div>
            <div>英文释义：{{currentEntry.englishInterpretation}}</div>
            <div class="btnBox" v-if="admin">
              <a-button type="primary" size="small" @click="addPublic('1')"><template #icon>
                  <PlusOutlined />
                </template>添加到部门公共库</a-button>
              <a-button type="primary" size="small" style="margin-left:16px" @click="addPublic('2')"><template #icon>
                  <PlusOutlined />
                </template>添加到公司公共库</a-button>
            </div>
          </div>
        </div>
      </template>
    </OperationArea>
    <EditReason :visible="editVisible" :entry="editEntry" @editClose="editClose" @editOk="editOk" />
  </div>
  <CreateVersionModal :visible="createVisible" :dataSource="selectEntry" :currentProduct="product" @createClose="createClose"
    @removeEntry="removeEntry" @refresh="refreshTable" @cancelCreate="cancelCreate" />

  <SecondClassify ref="secondClassifyRef" :visible="secondClassifyVisible" :currentProduct="product" @secondClassifyClose="secondClassifyClose" />
  <Dictionary ref="dictionaryRef" :visible="dictionaryVisible" :currentProduct="product" @dictionaryClose="dictionaryClose" />

  <!-- <CustomModal :visible="importVisible" :okLoading="importLoading" modalTitle="导入" @handleClose="importClose" @handleOK="importOK"
    @afterClose="importAfterClose">
    <div class="content">
      <a-form ref="formRef" name="custom-validation" :model="importModal">
        <a-form-item label="文件类型" name="importType" :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="importModal.importType" placeholder="请选择文件类型" :options='importTypes' allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="语言" name="language" :rules="[{ required: true, message: '请选择!' }]">
          <a-select v-model:value="importModal.language" placeholder="请选择语言" :options='translateTypes' :fieldNames="{label:'name',value:'name'}"
            allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="文件" name="file" :rules="[{required: true, validator: this.checkFile() }]">
          <a-upload name="file" :accept="accept" :max-count="1" :fileList="fileList" @change="handleChange" @remove="removeFile"
            :disabled="!importModal.language || !importModal.importType">
            <a-button type="primary" size="small" @click="getAccept">选择</a-button>
          </a-upload>
        </a-form-item>
      </a-form>
    </div>
  </CustomModal> -->
</template>
<script>
import "@/assets/style/common.less";
import CustomModal from "@/components/modal/index.vue";
// import tableParam from "@/views/entry/tableParam.js";
import { entryParams as tableParam } from "@/utils/commonParam.js";
import commonParam from "@/utils/commonParam.js";
// import common from "./common.js";
import { byteLength } from "@/utils/commonUtils.js";
import zhCN from "ant-design-vue/es/locale/zh_CN";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import OperationArea from "@/components/operationArea/index.vue";
import ImportButton from "@/components/Button/importButton.vue";
import EditReason from "@/views/entry/editReason.vue";
import CreateVersionModal from "@/views/entry/createVersionModal.vue";
import SecondClassify from "@/views/entry/secondClassify.vue";
import Dictionary from "@/views/entry/dictionary.vue";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import { cloneDeep, iteratee } from "lodash-es";
import { getLanguage } from "@/http/api/translate";
import { getProductVersion } from "@/http/api/product";
import { getVersionByName } from "@/http/api/productVersion";
import {
  getEntryByVersion,
  deleteEntryInfo,
  updatePublicEntry,
  addSingleEntry,
  getClassfy,
  entryImportExcle,
  getEntryByClassfy,
} from "@/http/api/entryManage";
import { getSecondClassify } from "@/http/api/secondClassify";
import {
  queryUserPartiality,
  updateUserPartiality,
} from "@/http/api/userPartiality";
import {
  PlusOutlined,
  DeleteOutlined,
  CopyOutlined,
  SaveOutlined,
  UpSquareOutlined,
  PlusCircleOutlined,
  SettingOutlined,
  SwapOutlined,
  InfoCircleOutlined,
  ExclamationCircleOutlined,
} from "@ant-design/icons-vue";
import {
  onSelectChange,
  onSelect,
  onSelectAll,
  pageChange,
  getColPref,
  changeColumn,
  setModalAriaHidden,
} from "@/utils/commonUtils";
export default {
  components: {
    CustomModal,
    SearchBox,
    DataBox,
    OperationArea,
    ImportButton,
    EditReason,
    CreateVersionModal,
    SecondClassify,
    Dictionary,
    PlusOutlined,
    DeleteOutlined,
    CopyOutlined,
    SaveOutlined,
    UpSquareOutlined,
    PlusCircleOutlined,
    SettingOutlined,
    SwapOutlined,
    InfoCircleOutlined,
    ExclamationCircleOutlined,
  },
  emits: [],
  props: {
    boxHeight: 0,
    currentProduct: {},
    productEdit: false,
  },
  data() {
    // 从本地缓存读取展示列偏好
    const cachedDisplayColumn = localStorage.getItem("colPref-productEntry");
    return {
      locale: zhCN,
      box: 0,
      user: {},
      admin: false,
      edit: false, // 用户对该产品是否有编辑权限
      product: {},
      labelCol: { style: { width: "84px" } },
      search: {
        entry: "",
        abbr: "",
        partOfSpeech: "",
        translateType: null,
        classfy2: null,
        entryState: null,
        tag: "",
        entrySource: "",
        language: null,
        translateState: null,
        translate: "",
        comment: "",
        startTime: null,
        endTime: null,
      },
      startTime: null,
      endTime: null,
      translateStates: [
        { label: "未翻译", value: "0" },
        { label: "待审核", value: "1" },
        { label: "审核不通过", value: "2" },
        { label: "已审核", value: "3" },
      ],
      translateTypes: [],
      tableTitle: "词条列表",
      copyVisible: false,
      copyNumber: 1,
      dataHeight: 200,
      tableHeight: { x: "100%", y: 0 },
      loading: false,
      columns: [
        {
          title: "序号",
          dataIndex: "index",
          align: "center",
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
          title: "词条状态",
          dataIndex: "entryState",
          align: "center",
          width: 130,
          fixed: "left",
          index: 1,
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
          title: "tag",
          dataIndex: "tag",
          align: "center",
          width: 130,
          index: 3,
        },
        {
          title: "Comment",
          dataIndex: "comment",
          align: "center",
          width: 130,
          index: 4,
        },
        {
          title: "词条版本",
          dataIndex: "entryVersion",
          align: "center",
          width: 130,
          index: 5,
        },
        {
          title: "英文翻译",
          dataIndex: "english",
          align: "center",
          width: 180,
          resizable: true,
          index: 10,
        },
        {
          title: "俄文翻译",
          dataIndex: "russian",
          align: "center",
          width: 180,
          resizable: true,
          index: 16,
        },
        {
          title: "西文翻译",
          dataIndex: "spanish",
          align: "center",
          width: 180,
          resizable: true,
          index: 20,
        },
        {
          title: "法文翻译",
          dataIndex: "french",
          align: "center",
          width: 180,
          resizable: true,
          index: 23,
        },
        {
          title: "abbr",
          dataIndex: "abbr",
          align: "center",
          width: 150,
          fixed: "right",
          resizable: true,
          index: 99,
        },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 150,
          fixed: "right",
          index: 100,
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
      overlayStyle: tableParam.overlayStyle,
      checkboxList: tableParam.checkboxList,
      checkedColumn: cachedDisplayColumn
        ? cachedDisplayColumn.split(",")
        : tableParam.checkedColumn,
      inputColumn: tableParam.inputColumn,
      translateColumn: tableParam.translateColumn,
      editableData: {},
      selectedRowKeys: [],
      selectedRows: [],
      selectedRowIndex: null,
      currentEntry: {},
      showOperationArea: false,
      operationAreaTitle: "详情信息",
      operationAreaHeight: 190,
      currentVersion: null,
      productVersions: [],
      editVisible: false,
      editEntry: [],
      createVersionFlag: false,
      selectEntry: [],
      createVisible: false,
      rules: {},
      batchSelectFlag: false,
      limitMap: {},
      classify1Option: [],
      secondClassifyVisible: false,
      classify2Option: [],
      rowClassify2Option: {},
      dictionaryVisible: false,
      selectAllLoading: false,
      filters: null,
      // 都封在importButton中了
      // accept: null,
      // importVisible: false,
      // importLoading: false,
      // importModal: {
      //   language: null,
      //   importType: null,
      // },
      // importTypes: [
      //   { label: "csv", value: "csv", accept: ".csv" },
      //   { label: "excel", value: "excel", accept: ".xls,.xlsx" },
      // ],
      // importFile: null,
      // fileList: [],
    };
  },

  created() {},
  mounted() {
    this.$nextTick(() => {
      this.user = this.$store.state.user;
      this.admin = this.$store.state.admin;
      //保证初次传的值给到
      this.box = this.boxHeight;
      this.edit = this.productEdit;
      this.setTableHeight();
      this.product = this.currentProduct;
      this.getLanguage();
      this.init();
      // 读取本地存储的用户偏好
      getColPref("colPref-productEntry", 200, this);
      // const storedPreferences = localStorage.getItem("colPref-productEntry");
      // if (storedPreferences) {
      //   const preferences = JSON.parse(storedPreferences);
      //   this.checkedColumn = preferences.displayColumn.split(",");
      //   // 调用 changeColumn 方法更新列显示
      //   this.changeColumn(this.checkedColumn);
      // }
    });
  },
  watch: {
    boxHeight(newval, oldval) {
      this.box = newval;
      // console.log(newval)
      this.setTableHeight();
    },
    currentProduct(newval, oldval) {
      this.currentVersion = null;
      this.product = newval;
      this.showOperationArea = false;
      this.pagination.current = 1;
      this.selectEntry = [];
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.init();
      // console.log(newval)
      let limitMap = {};
      this.classify1Option = [];
      newval.children.forEach((item) => {
        limitMap[item.title] = item;
        this.classify1Option.push(item);
      });
      this.limitMap = limitMap;
    },
    productEdit(newval, oldval) {
      this.edit = newval;
    },
    startTime(newValue) {
      // console.log("日期格式",newValue)
      if (newValue) {
        this.search.startTime = `${newValue.$y}-${newValue.$M + 1}-${
          newValue.$D
        }`; // 格式化日期为 YYYY-MM-DD 格式;
        // console.log("日期格式",this.search.startTime)
        if (this.endTime) {
          if (this.startTime > this.endTime) {
            message.error("开始时间不能大于结束时间！");
            this.search.startTime = null;
            this.startTime = null;
          }
        }
      } else {
        this.search.startTime = null;
      }
    },
    endTime(newValue) {
      if (newValue) {
        this.search.endTime = `${newValue.$y}-${newValue.$M + 1}-${
          newValue.$D
        }`; // 格式化日期为 YYYY-MM-DD 格式;
        if (this.startTime) {
          if (this.startTime > this.endTime) {
            message.error("结束时间不能小于开始时间！");
            this.endTime = null;
            this.search.endTime = null;
          }
        }
      } else {
        this.search.endTime = null;
      }
    },
  },
  unmounted() {},
  methods: {
    init() {
      this.getProductVersion();
      this.getEntryByVersion();
      this.setTableHeight();
      this.selectSecondClassify();
    },
    format(text) {
      return text.replace(/\n/g, "\\n");
    },
    // // （弃用）调用后端的接口获取用户偏好，并调用changeColumn方法更新表格列显示
    // getUserPartiality() {
    //   queryUserPartiality().then((res) => {
    //     if (res.data.list && res.data.list.length > 0) {
    //       let displayColumn = res.data.list[0].displayColumn;
    //       if (displayColumn != null && displayColumn != "") {
    //         this.changeColumn(displayColumn.split(","));
    //       }
    //     }
    //   });
    // },
    // 获取翻译语言
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
      });
    },
    // 动态设置表格高度
    setTableHeight() {
      this.$nextTick(() => {
        // 设置列表父元素高度
        let searchHeight = this.$refs.search.$el.offsetHeight;
        const len = 84;
        try {
          let operationAreaHeight = this.$refs.operationArea.$el.offsetHeight;
          this.dataHeight = this.box - searchHeight - operationAreaHeight - len;
        } catch (error) {
          this.dataHeight = this.box - searchHeight - len;
        }

        // 设置表格高度
        let buttonHeight = 0;
        try {
          buttonHeight = this.$refs.button.offsetHeight - 8;
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

    // 查询产品的所有版本
    getProductVersion() {
      if (Object.keys(this.product).length === 0) {
        return;
      }
      let params = {
        versionName: "",
        productID:
          this.product.type === "module"
            ? this.product.parentId
            : this.product.key,
      };
      getVersionByName(params).then((res) => {
        this.productVersions = res.data.list;
        // if(this.productVersions.length > 0){
        //     this.currentVersion = this.productVersions[0].id
        // }else{
        //     this.currentVersion = null
        // }
        // 获取版本下的词条
        // this.getEntryByVersion()
      });
    },
    // 条件查询
    conditionalQuery() {
      // 将页码变为第一页
      this.pagination.current = 1;
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.selectEntry = [];
      this.createVersionFlag = false;
      this.getEntryByVersion();
    },
    // 获取版本词条
    getEntryByVersion() {
      // console.log("获取版本词条",this.search);
      if (Object.keys(this.product).length === 0) {
        return;
      }
      if (
        (this.search.translate != "" || this.search.translateState != null) &&
        !this.search.language
      ) {
        message.info("请选择翻译语言！");
        return;
      }
      let data = {
        abbr: this.search.abbr,
        entry: this.search.entry,
        classfy2: this.search.classfy2,
        classfy1: this.product.type === "module" ? this.product.title : "",
        entryState: this.search.entryState,
        tag: this.search.tag,
        entrySource: this.search.entrySource,
        comment: this.search.comment,
      };
      // data.entry = data.entry.replace(/\\n/g, '\n')
      // console.log("data:",data)
      if (!this.currentVersion) {
        data.productID =
          this.product.type === "module"
            ? this.product.parentId
            : this.product.key;
      } else {
        data.versionID = this.currentVersion;
      }
      commonParam.languageList.forEach((item) => {
        if (this.search.language === item.name) {
          data[item.value] = this.search.translate;
          data[item.state] = this.search.translateState;
        }
      });
      let params = {
        classfyID: this.product.key,
        translateType: this.search.language,
        pageIndex: this.pagination.current,
        pageSize: this.pagination.pageSize,
        startTime: this.search.startTime,
        endTime: this.search.endTime,
      };
      this.loading = true;

      getEntryByClassfy(params, data)
        .then((res) => {
          this.dataSource = res.data.list;
          // console.log("总词条dataSource", res);
          this.pagination.total = res.data.totalNum;
        })
        .catch((err) => {
          message.error(err.message);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    // 获取词条分类
    getEntryClassfy() {
      let params = {
        classfyID: this.product.key,
      };
      getEntryClassfy(params).then((res) => {
        this.entryClassfy = res.data;
      });
    },
    // 组装表格数据
    assemblyTableData(data) {
      let dataSource = [];
      let version = this.productVersions.find(
        (item) => item.id === this.currentVersion
      );
      data.forEach((element) => {
        let item = element.entryInfoEntity;
        // item.tableName = element.tableName
        item.tableName = version.tableName;
        if (element.translateEntity) {
          element.translateEntity.forEach((tran) => {
            commonParam.languageList.forEach((lang) => {
              if (tran.type === lang.name) {
                item[lang.value] = tran.translate;
                item[lang.id] = tran.id;
                item[lang.state] = tran.translateState;
                item[lang.publicState] = tran.publicState;
                item[lang.checked] = false;
              }
            });
          });
        }
        dataSource.push(item);
      });
      this.dataSource = dataSource;
      // console.log(this.dataSource)
      this.loading = false;
    },
    changeVersion(version) {
      // console.log(version)
      if (version === undefined) {
        this.currentVersion = null;
      } else {
        this.currentVersion = version;
      }
      this.pagination.current = 1;
      // 查询版本词条
      this.getEntryByVersion();
    },

    // 添加表格行点击事件
    customRow(record, index) {
      return {
        // onClick: (event) => {
        //     let _this = this
        //     clearTimeout(this.timer)
        //     this.timer = setTimeout(function () {
        //         _this.selectedRowIndex = record.id
        //         _this.currentEntry = record
        //         _this.showOperationArea = true
        //         _this.setTableHeight()
        //     }, 300);
        // },
        onDblclick: (event) => {
          // clearTimeout(this.timer)
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }
          if (this.edit) {
            this.editableData[record.id] = cloneDeep(
              this.dataSource.filter((item) => record.id === item.id)[0]
            );
            // 设置校验规则
            this.rules[record.id] = {
              entry: [
                { validator: this.vilidFildLength(record, "chinese") },
                { required: true, message: "请输入!" },
              ],
              english: [{ validator: this.vilidFildLength(record, "english") }],
              french: [{ validator: this.vilidFildLength(record, "french") }],
              russian: [{ validator: this.vilidFildLength(record, "russian") }],
              spanish: [{ validator: this.vilidFildLength(record, "spanish") }],
            };
            // 获取表格操作行的classify2Option
            this.getRowClassify2Option(record);
          }
        },
      };
    },
    // 校验输入数据的长度
    vilidFildLength(record, language) {
      return (rule, value) => {
        let type = "";
        if (language === "chinese") {
          type = "maxByte";
        } else {
          type = "foreignMaxByte";
        }
        let maxLength = null;
        if (!this.limitMap[record.classfy1]) {
          if (record.maxLength != null && record.maxLength != "") {
            maxLength = record.maxLength;
          } else {
            return Promise.resolve();
          }
        } else {
          maxLength = this.limitMap[record.classfy1][type];
        }
        if (!maxLength || maxLength === "") {
          return Promise.resolve();
        }
        // 获取输入数据的长度
        // let length = common.byteLength(value);
        let length = byteLength(value);
        if (length > maxLength) {
          return Promise.reject("允许最大字符数为" + maxLength + "！");
        }
        return Promise.resolve();
      };
    },
    // 详情
    entryDetails(record) {
      this.selectedRowIndex = record.id;
      this.currentEntry = record;
      // 将翻译状态转换为中文
      commonParam.languageList.forEach((item) => {
        if (this.currentEntry[item.state] != null) {
          switch (this.currentEntry[item.state]) {
            case "1":
              this.currentEntry[item.chineseState] = "待审核";
              break;
            case "2":
              this.currentEntry[item.chineseState] = "审核不通过";
              break;
            case "3":
              this.currentEntry[item.chineseState] = "已审核";
              break;
          }
        }
      });

      this.showOperationArea = true;
      this.setTableHeight();
      // console.log(this.currentEntry)
    },
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
    // 取消
    cancel(id) {
      delete this.editableData[id];
      delete this.rules[id];
      delete this.rowClassify2Option[id];
      if (id.startsWith("new" || id.startsWith("copy"))) {
        this.dataSource.some((item, i) => {
          if (item.id === id) {
            this.dataSource.splice(i, 1);
            return true;
          }
        });
        this.pagination.pageSize = this.pagination.pageSize - 1;
      }
    },
    // 保存
    save(id) {
      // 校验字段长度是否超限
      let flagArr = ["entry", "english", "russian", "spanish", "french"];
      let list = [];
      this.columns.forEach((column) => {
        if (flagArr.includes(column.dataIndex)) {
          list.push(
            eval(
              "this.$refs.form" + id.replaceAll("-", "") + column.dataIndex
            ).validate()
          );
        }
      });

      Promise.all(list)
        .then(() => {
          // 校验成功
          if (id.startsWith("new") || id.startsWith("copy")) {
            // 新增词条/升级词条
            addSingleEntry(this.editableData[id]).then((res) => {
              message.success("新增成功!");
              // this.getEntryByVersion()
              let index = this.dataSource.findIndex((item) => item.id === id);
              this.dataSource.splice(index, 1);
              this.dataSource.splice(index, 0, res.data);
              delete this.editableData[id];
              delete this.rowClassify2Option[id];
              this.pagination.total = this.pagination.total + 1;
            });
          } else {
            this.editEntry = [this.editableData[id]];
            this.editVisible = true;
            setModalAriaHidden(this, document);
          }

          // 更新选中的值
          const selectedIndex = this.selectedRows.findIndex(
            (item) => item.id === id
          );
          if (selectedIndex !== -1) {
            // 更新选中行数据
            this.selectedRows[selectedIndex] = { ...this.editableData[id] };
          }
        })
        .catch((err) => {
          message.error(err.message);
        });
    },
    // 校验输入数据是否合规
    checkedData(record) {
      //       if (
      //   common.byteLength(record.entry) > record.maxLength ||
      //   common.byteLength(record.english) > record.maxLength ||
      //   common.byteLength(record.russian) > record.maxLength ||
      //   common.byteLength(record.spanish) > record.maxLength ||
      //   common.byteLength(record.french) > record.maxLength
      // ) {
      // 缺少新加的中文
      const checkValue = () => {
        // 若 record 不存在、record.entry 不存在或 record.maxLength 不存在，视为校验通过
        if (!record || !record.entry || !record.maxLength) return false;

        // 检查词条本身长度是否超过限制
        if (byteLength(record.entry) > record.maxLength) return true;

        // 检查各语言翻译长度是否超过限制
        return commonParam.languageList.some((item) => {
          const value = record[item.value];
          return value && byteLength(value) > record.maxLength;
        });
      };
      if (checkValue()) {
        message.error("输入的数据超长！");
        return false;
      } else {
        return true;
      }
    },
    // 批量保存
    batchSave() {
      let edit = [];
      for (let key in this.editableData) {
        edit.push(this.editableData[key]);
      }
      this.editEntry = edit;
      this.editVisible = true;
      setModalAriaHidden(this, document);
    },
    editOk(entry) {
      delete this.editableData[entry.id];
      delete this.rules[entry.id];
      // this.getEntryByVersion()
      let index = this.dataSource.findIndex((item) => item.id === entry.id);
      this.dataSource.splice(index, 1);
      this.dataSource.splice(index, 0, entry);

      this.editVisible = false;
      delete this.rowClassify2Option[entry.id];
      this.getEntryByVersion();
    },
    editClose() {
      this.editVisible = false;
    },

    // 表格列可伸缩
    handleResizeColumn: (w, col) => {
      col.width = w;
    },
    // 关闭流程操作区
    closeOperationArea() {
      this.showOperationArea = false;
      this.setTableHeight();
      this.selectedRowIndex = null;
    },
    // 添加公共库
    addPublic(type) {
      // type 1 部门公共库  2公司公共库
      let data = [];
      if (this.currentEntry.englishChecked) {
        let ele = {
          id: this.currentEntry.enTransId,
        };
        data.push(ele);
      }
      if (this.currentEntry.russianChecked) {
        let ele = {
          id: this.currentEntry.ruTransId,
        };
        data.push(ele);
      }
      if (this.currentEntry.spanishChecked) {
        let ele = {
          id: this.currentEntry.spaTransId,
        };
        data.push(ele);
      }
      if (this.currentEntry.frenchChecked) {
        let ele = {
          id: this.currentEntry.fraTransId,
        };
        data.push(ele);
      }
      data.forEach((element) => {
        let data = {
          id: element.id,
          publicState: 1,
          visualRange: type === "1" ? this.currentProduct.department : "公司",
          translateState: "3",
        };
        updatePublicEntry(data).then((res) => {});
      });
      message.success("添加成功！");
    },
    reset() {
      this.search = {
        entry: "",
        abbr: "",
        partOfSpeech: "",
        translateType: null,
        entrySource: "",
        entryState: null,
        tag: "",
        language: null,
        translateState: null,
        translate: "",
      };
      // this.getEntryByVersion();
      this.conditionalQuery();
    },
    // 展示列切换并保存用户偏好
    changeColumn(checkedValue) {
      changeColumn("colPref-productEntry", 200, checkedValue, this);
      // this.checkedColumn = checkedValue;

      // this.checkboxList.forEach((value) => {
      //   // 查找当前勾选列表中是否存在该列
      //   let checkedIndex = this.checkedColumn.findIndex(
      //     (item) => item === value.value
      //   );
      //   // 查找当前表格列中是否存在该列
      //   let nowColumnIndex = this.columns.findIndex(
      //     (item) => item.dataIndex === value.value
      //   );
      //   // 若勾选状态和列存在状态一致，则跳过
      //   if (
      //     (nowColumnIndex !== -1 && checkedIndex !== -1) ||
      //     (nowColumnIndex === -1 && checkedIndex === -1)
      //   ) {
      //     return;
      //   }
      //   // 若勾选了但列不存在，则添加列
      //   if (nowColumnIndex === -1 && checkedIndex !== -1) {
      //     let newCol = {
      //       title: value.label,
      //       dataIndex: value.value,
      //       align: "center",
      //       width: 200,
      //       ellipsis: true,
      //       resizable: true,
      //       index: value.index,
      //     };
      //     if (
      //       ["isExist", "translateState", "entry"].includes(newCol.dataIndex)
      //     ) {
      //       newCol.fixed = "left";
      //     }
      //     if (["auditSuggess", "entryState"].includes(newCol.dataIndex)) {
      //       newCol.fixed = "right";
      //     }
      //     if (newCol.dataIndex === "entrySource") {
      //       // 添加词条来源可筛选
      //       newCol.customFilterDropdown = true;
      //       newCol.filteredValue = null;
      //       newCol.onFilter = (value, record) =>
      //         record.entrySource
      //           .toString()
      //           .toLowerCase()
      //           .includes(value.toLowerCase());
      //     }
      //     this.columns.splice(-1, 0, newCol);
      //   }
      //   // 若未勾选但列存在，则移除列
      //   if (nowColumnIndex !== -1 && checkedIndex === -1) {
      //     this.columns.splice(nowColumnIndex, 1);
      //   }
      // });
      // this.columns.sort((a, b) => a.index - b.index);

      // // 记录
      // let data = {
      //   displayColumn: checkedValue.join(","),
      // };
      // // this.recordPartiality(data);
      // localStorage.setItem("colPref-productEntry", JSON.stringify(data)); // localStorage存储用户偏好
      // // console.log("已保存列偏好设置", data);
    },
    clickInput(event) {
      event.stopPropagation();
    },
    // 创建版本
    createVersion() {
      this.createVersionFlag = true;
      this.selectEntry = [];
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.batchSelectFlag = true;
    },
    // 已选词条按钮点击事件
    viewCreateVersionEntry() {
      this.createVisible = true;
      setModalAriaHidden(this, document);
    },
    createClose() {
      this.createVisible = false;
      this.getProductVersion();
    },
    refreshTable() {
      this.getEntryByVersion();
    },
    // 移除已选择词条
    removeEntry(record) {
      this.selectEntry = this.selectEntry.filter((item) => {
        return item.id != record.id;
      });
      this.selectedRowKeys = this.selectedRowKeys.filter((item) => {
        return item.id != record.id;
      });
      this.selectedRows = this.selectedRows.filter((item) => {
        return item.id != record.id;
      });
    },
    // 选择全部词条
    selectAllEntry() {
      // 获取所有的词条
      if (Object.keys(this.product).length === 0) {
        return;
      }
      this.selectAllLoading = true;
      let data = {
        abbr: this.search.abbr,
        entry: this.search.entry,
        classfy2: this.search.classfy2,
        classfy1: this.product.type === "module" ? this.product.title : "",
        entryState: this.search.entryState,
        tag: this.search.tag,
        entrySource: this.search.entrySource,
      };
      if (!this.currentVersion) {
        data.productID =
          this.product.type === "module"
            ? this.product.parentId
            : this.product.key;
      } else {
        data.versionID = this.currentVersion;
      }
      commonParam.languageList.forEach((item) => {
        if (this.search.language === item.name) {
          data[item.value] = this.search.translate;
          data[item.state] = this.search.translateState;
        }
      });
      let params = {
        classfyID: this.product.key,
        translateType: this.search.language,
        startTime: this.search.startTime,
        endTime: this.search.endTime,
        pageIndex: -1,
        pageSize: -1,
      };
      this.loading = true;
      getEntryByClassfy(params, data)
        .then((res) => {
          this.selectEntry = [];
          this.selectedRowKeys = [];
          this.selectedRows = res.data.list;
          this.selectEntry = res.data.list;
          // console.log("全选词条dataSource", res);
          res.data.list.forEach((item) => {
            this.selectedRowKeys.push(item.id);
          });
        })
        .catch((err) => {
          message.error(err.message);
        })
        .finally(() => {
          this.loading = false;
          this.selectAllLoading = false;
        });
    },
    // 取消创建版本
    cancelCreate() {
      this.selectEntry = [];
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.createVersionFlag = false;
      this.createVisible = false;
      this.batchSelectFlag = false;
    },
    //新增词条
    addEntry() {
      if (
        Object.keys(this.product).length === 0 ||
        this.product.type === "classify"
      ) {
        message.info("请选择产品！");
        return;
      }
      this.pagination.pageSize = this.pagination.pageSize + 1;
      let newData = {
        id: `new${this.dataSource.length + 1}`,
        entryState: 0,
        classfy1: this.product.type === "module" ? this.product.title : "",
        maxLength: this.product.type === "module" ? this.product.maxByte : "",
        versionID: this.currentVersion,
        productID:
          this.product.type === "module"
            ? this.product.parentId
            : this.product.key,
        entryVersion: 0,
      };
      // console.log(newData)
      // 设置校验规则
      if (newData.maxLength != "") {
        this.rules[newData.id] = {
          entry: [
            { validator: this.vilidFildLength(newData, "chinese") },
            { required: true, message: "请输入!" },
          ],
          english: [{ validator: this.vilidFildLength(newData, "english") }],
          french: [{ validator: this.vilidFildLength(newData, "french") }],
          russian: [{ validator: this.vilidFildLength(newData, "russian") }],
          spanish: [{ validator: this.vilidFildLength(newData, "spanish") }],
        };
      }

      if (this.pagination.total >= this.pagination.pageSize) {
        this.dataSource.splice(this.pagination.pageSize - 1, 0, newData);
      } else {
        this.dataSource.push(newData);
      }

      this.editableData[newData.id] = newData;
      // 获取二级分类数据
      this.getRowClassify2Option(newData);
      // 滚动到最底部
      this.$nextTick(() => {
        let container =
          this.$refs.entryTable.$el.querySelector(".ant-table-body");
        container.scrollTop = container.scrollHeight;
      });
    },
    // 词条升级
    entryUpgrade(entry) {
      this.pagination.pageSize = this.pagination.pageSize + 1;
      let copyEntry = cloneDeep(entry);
      copyEntry.id = "copy_" + copyEntry.id;
      // copyEntry.entryVersion = entry.entryVersion === null || entry.entryVersion === "" ? 0 : entry.entryVersion + 1
      copyEntry.entryState = 0; // 新建状态
      copyEntry.upgrade = true;
      copyEntry.maxLength = entry.maxLength;
      // copyEntry.entryVersion = 'new'
      // 设置校验规则
      if (copyEntry.maxLength != "") {
        this.rules[copyEntry.id] = {
          entry: [
            { validator: this.vilidFildLength(copyEntry, "chinese") },
            { required: true, message: "请输入!" },
          ],
          english: [{ validator: this.vilidFildLength(copyEntry, "english") }],
          french: [{ validator: this.vilidFildLength(copyEntry, "french") }],
          russian: [{ validator: this.vilidFildLength(copyEntry, "russian") }],
          spanish: [{ validator: this.vilidFildLength(copyEntry, "spanish") }],
        };
      }
      let index = this.dataSource.indexOf(entry);
      this.dataSource.splice(index + 1, 0, copyEntry);
      this.editableData[copyEntry.id] = copyEntry;
      this.getRowClassify2Option(copyEntry);
    },
    // 二级分类管理
    setSecondClassify() {
      this.secondClassifyVisible = true;
      setModalAriaHidden(this, document);
      this.$refs.secondClassifyRef.init();
    },
    secondClassifyClose() {
      this.secondClassifyVisible = false;
      this.selectSecondClassify();
    },
    // 获取二级分类
    selectSecondClassify() {
      if (this.product.type != "module") {
        this.classify2Option = [];
        return;
      }
      let params = {
        parentId: this.product.key,
      };
      getSecondClassify(params).then((res) => {
        this.classify2Option = res.data.list;
      });
    },
    // 获取表格当前操作行的Classify2Option
    getRowClassify2Option(record) {
      if (
        this.editableData[record.id] === undefined ||
        !this.editableData[record.id].classfy1 ||
        this.editableData[record.id].classfy1 === ""
      ) {
        return;
      }
      let classify1 = this.classify1Option.find(
        (item) => item.title === this.editableData[record.id].classfy1
      );
      if (classify1 === undefined || classify1 === null) {
        return;
      }
      let params = {
        parentId: classify1.key,
      };
      getSecondClassify(params)
        .then((res) => {
          this.rowClassify2Option[record.id] = res.data.list;
        })
        .catch((err) => {
          this.rowClassify2Option[record.id] = [];
          message.error(err.message);
        });
    },
    // 切割字符串
    companyCut(message) {
      let res = [];
      if (!message || message === "") {
        return res;
      }
      const regex = /[;；]/;
      res = message.split(regex);
      res = res.filter((item) => item != "");
      return res;
    },
    // 查看辞典
    viewDictionary() {
      this.dictionaryVisible = true;
      setModalAriaHidden(this, document);
      this.$refs.dictionaryRef.init();
    },
    dictionaryClose() {
      this.dictionaryVisible = false;
    },
    // 刷新翻译长度限制
    refresh(product) {
      let params = {
        type: "module",
      };
      if (product && product.type === "module") {
        params.parentId = product.parentId;
      } else if (product && product.type === "product") {
        params.parentId = product.key;
      } else if (product && product.type === "classify") {
        return;
      }
      getClassfy(params)
        .then((res) => {
          let limitMap = {};
          res.data.list.forEach((item) => {
            limitMap[item.title] = item;
          });
          this.limitMap = limitMap;
        })
        .catch((err) => {
          message.err(err.message);
        });
    },
    // // 记录用户偏好
    // recordPartiality(data) {
    //   updateUserPartiality(data).then((res) => {});
    // },

    // // 全都由importBUtton来实现
    // // 获得导入文件类型
    // getAccept() {
    //   if (!this.importModal.importType) {
    //     message.error("请选择文件类型！");
    //     return;
    //   }
    //   if (!this.importModal.language) {
    //     message.error("请选择语言！");
    //     return;
    //   }
    //   for (let key in this.importTypes) {
    //     if (this.importModal.importType === this.importTypes[key].value) {
    //       this.accept = this.importTypes[key].accept;
    //       break;
    //     }
    //   }
    // },
    // // 导入词条
    // importEntry() {
    //   this.importVisible = true;
    //   setModalAriaHidden(this, document);
    // },
    // importClose() {
    //   this.importVisible = false;
    // },
    // importOK() {
    //   this.$refs.formRef
    //     .validate()
    //     .then(() => {
    //       this.importLoading = true;
    //       let formData = new FormData();
    //       formData.append("file", this.importFile);
    //       formData.append("transType", this.importModal.language);
    //       formData.append("importType", this.importModal.importType);
    //       entryImportExcle(formData)
    //         .then((res) => {
    //           message.success("导入成功！");
    //           this.getEntryByVersion();
    //           this.importVisible = false;
    //           this.importLoading = false;
    //         })
    //         .catch((err) => {
    //           message.error("导入失败！", err.message);
    //           this.importLoading = false;
    //         });
    //     })
    //     .catch((err) => {
    //       message.error(err.message);
    //     });
    // },
    // importAfterClose() {
    //   this.importModal.language = null;
    //   this.importFile = null;
    //   this.fileList = [];
    //   this.$refs.formRef.clearValidate();
    // },
    // handleChange(info) {
    //   this.fileList = info.fileList;
    //   if (info.fileList.length === 0) {
    //     this.importFile = null;
    //   } else {
    //     this.importFile = info.file;
    //   }
    // },
    // removeFile(file) {
    //   this.importFile = null;
    //   return true;
    // },
    // // 校验上传文件是否为空
    // checkFile() {
    //   return (rule, value) => {
    //     if (!this.importFile) {
    //       return Promise.reject("请选择文件！");
    //     }
    //     return Promise.resolve();
    //   };
    // },

    // 表格change事件
    handleTableChange(pagination, filters) {
      this.filters = filters;
      // console.log(filters)
      for (let key in filters) {
        this.columns.forEach((col) => {
          if (col.dataIndex === key) {
            col.filteredValue = filters[key];
          }
        });
      }
      // console.log(this.columns)
      // 获取筛选后的数据
      let isExistData = this.dataSource.filter((item) => {
        return filters.isExist && filters.isExist.includes(item.isExist);
      });
      let sourceData = this.dataSource.filter((item) => {
        return (
          filters.entrySource && item.entrySource.includes(filters.entrySource)
        );
      });
      this.filteredData = this.intersection(isExistData, sourceData);
    },
    // 两个数组取并集
    intersection(nums1, nums2) {
      if (nums1.length === 0) {
        return nums2;
      }
      if (nums2.length === 0) {
        return nums1;
      }
      let a = new Set(nums1);
      let b = new Set(nums2);
      let arr = Array.from(new Set([...b].filter((x) => a.has(x))));
      return arr;
    },
    // 复选框选择事件
    onSelectChange(selectedRowKeys, selectedRows) {
      onSelectChange(this, selectedRowKeys, selectedRows);
    },
    // 复选框点击事件
    onSelect(record, selected) {
      onSelect(this, record, selected, this.createVersionFlag);
    },
    // 复选框当前页全选/反选框点击事件
    onSelectAll(selected, selectedRows, changeRows) {
      onSelectAll(
        this,
        selected,
        selectedRows,
        changeRows,
        this.createVersionFlag
      );
    },
    // 分页切换
    pageChange(page, pageSize) {
      pageChange(this, page, pageSize, this.getEntryByVersion);
    },
  },
};
</script>
<style scoped lang="less">
// :deep(.ant-table-striped tr td.ant-table-cell) {
//   padding: 2px !important;
// }
.productEntryBox {
  padding: 0px 16px 16px 16px;
  width: 100%;
  height: 100%;
  // border: 1px solid red;
}
.entryDetails {
  width: 100%;
  height: 100%;
  // border: 1px solid red;
  display: flex;
  flex-direction: row;

  table {
    width: 736px;
    height: 100%;
    border: 1px solid #e7e7e7;

    tr {
      border: 1px solid #e7e7e7;
    }
    td {
      border: 1px solid #e7e7e7;
      text-align: center;
      // color: var(--text-icon-font-gy-340-placeholder, rgba(0, 0, 0, 0.40));
      /* 五级文字/常规 */
      font-family: Microsoft YaHei;
      font-size: 12px;
      font-style: normal;
      font-weight: 400;
    }

    .tableTitle {
      width: 96px;
      background-color: #f9f9f9;
    }
  }

  .details {
    height: 100%;
    width: calc(100% - 736px);
    // border: 1px solid red;
    padding-left: 16px;
    position: relative;

    div {
      color: var(--text-icon-font-gy-190-primary, rgba(0, 0, 0, 0.9));
      /* 五级文字/常规 */
      font-family: Microsoft YaHei;
      font-size: 12px;
      font-style: normal;
      font-weight: 400;
      line-height: 20px;
    }

    .btnBox {
      position: absolute;
      bottom: 0px;
    }
  }
}
.ant-table-cell .ant-form-item {
  margin-bottom: 0%;
}
:deep(.ant-pagination) {
  margin: 8px 0px 0px 0px;
}
</style>