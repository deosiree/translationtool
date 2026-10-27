<template>
  <PipeShell
    :visible="visible"
    :modalTitle="modalTitle"
    :showCancel="false"
    :okLoading="loading"
    okText="保存"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    @setTableHeight="setTableHeight"
  >
    <div class="content">
      <!-- 工具栏壳：任务信息 + 导入区 + 表格查询 + trailing 展示列 -->
      <PipelinePanel
        :task="task"
        :table-host="this"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading || queryLoading"
        :pagination="pagination"
        :scroll="tableHeight"
        :row-selection="importRowSelection"
        :custom-row="customRow"
        :edit="importEditCols"
        :editable-data="editableData"
        :cell-errors="cellErrors"
        @cell-input="onCellInput"
        @save-edit="editSave"
        @cancel-edit="cancel"
        ref="workTable"
      >
        <template #taskExtra>
          <RulesDropdown
            :options="rulesOptions"
            @update:options="rulesOptions"
          ></RulesDropdown>
        </template>
        <template #beforeFormBar>
          <!-- 方案 B：语义组两段（数据类型+IP / 词条文件+回写辞典+导入），组内可 wrap -->
          <div class="platformBox">
            <div class="import-form-row import-form-row--source" ref="fileRef">
              <a-form layout="inline" class="import-flow-item">
                <a-form-item label="数据类型">
                  <a-radio-group
                    v-model:value="dataType"
                    @change="dataTypeChange"
                  >
                    <a-radio
                      v-if="
                        $currentDepartment &&
                        $currentDepartment.importTypes.includes('file')
                      "
                      :value="'file'"
                      >文件</a-radio
                    >
                    <a-radio
                      v-if="
                        $currentDepartment &&
                        $currentDepartment.importTypes.includes('ts')
                      "
                      :value="'ts'"
                      >TS</a-radio
                    >
                    <a-radio
                      v-if="
                        $currentDepartment &&
                        $currentDepartment.importTypes.includes('database')
                      "
                      :value="'database'"
                      >实时库</a-radio
                    >
                    <a-radio
                      v-if="
                        $currentDepartment &&
                        $currentDepartment.importTypes.includes('dictionary')
                      "
                      :value="'dictionary'"
                      >辞典</a-radio
                    >
                    <a-radio
                      v-if="
                        $currentDepartment &&
                        $currentDepartment.importTypes.includes('config')
                      "
                      :value="'config'"
                      >配置文件</a-radio
                    >
                    <a-radio
                      v-if="
                        $currentDepartment &&
                        $currentDepartment.importTypes.includes('enum')
                      "
                      :value="'enum'"
                      >枚举文件</a-radio
                    >
                  </a-radio-group>
                </a-form-item>
              </a-form>
              <a-form
                v-if="
                  $currentDepartment && $currentDepartment.ops.has('needIP')
                "
                layout="inline"
                class="import-flow-item"
              >
                <a-form-item label="IP">
                  <a-select
                    v-model:value="ip"
                    :options="ips"
                    @change="ipChange"
                    style="width: 200px"
                    placeholder="请选择IP"
                    allowClear
                    size="small"
                  ></a-select>
                </a-form-item>
              </a-form>
            </div>
            <div
              v-if="dataType === 'file'"
              class="import-form-row import-form-row--file"
            >
              <a-form-item
                label="词条文件"
                name="filefilename"
                class="import-flow-item file-import-item"
              >
                <div class="file-import-controls">
                  <FileSelectWithEncoding
                    v-model:encoding="fileEncoding"
                    v-model:filePath="filePath"
                    :accept="accept"
                    :encoding-locked="true"
                    showPathInput
                    path-placeholder="请选择词条文件"
                    :path-input-style="{
                      width: '140px',
                      maxWidth: '140px',
                      flexShrink: 0,
                    }"
                    button-text="选择文件"
                    size="small"
                    :before-upload="beforeUpload"
                    @change="handleChange"
                  />
                  <a
                    class="template-download-link"
                    @click="templateFileDownload"
                    >下载模板</a
                  >
                </div>
              </a-form-item>
              <a-form-item
                v-if="
                  $currentDepartment && $currentDepartment.ops.has('needIP')
                "
                label="回写辞典"
                name="diFileName"
                class="import-flow-item file-import-item"
              >
                <a-select
                  v-model:value="filediFileName"
                  allowClear
                  placeholder="请选择回写辞典目录"
                  style="width: 160px"
                  :options="dictionaryOptions"
                  size="small"
                  show-search
                  :filter-option="
                    (input, option) =>
                      option.label
                        .toLowerCase()
                        .includes(input.toLowerCase())
                  "
                />
                <a-tooltip placement="top">
                  <template #title>
                    <span>添加辞典</span>
                  </template>
                  <PlusSquareOutlined
                    @click="createDictionary"
                    style="color: #369fff; margin-left: 6px"
                  />
                </a-tooltip>
              </a-form-item>
              <a-form-item
                v-else-if="templateTypes"
                label="选择模板"
                name="templateType"
                class="import-flow-item file-import-item"
              >
                <a-select
                  v-model:value="templateType"
                  allowClear
                  placeholder="请选择模板类型"
                  style="width: 160px"
                  size="small"
                  :options="templateTypes"
                />
              </a-form-item>
              <a-button
                type="primary"
                ghost
                size="small"
                class="import-flow-item import-flow-item--action"
                :loading="loading"
                @click="importEntryData"
                >导入</a-button
              >
            </div>
            <div class="dataTypeBox" v-if="dataType === 'ts'">
              <a-form ref="tsFormRef" :model="tsFile" style="width: 100%">
                <a-form-item
                  :label="selectTitle"
                  name="tsFileValue"
                  :rules="[{ required: true, message: '请选择ts文件!' }]"
                >
                  <a-select
                    v-model:value="tsFile.tsFileValue"
                    v-model:searchValue="searchTSValue"
                    mode="multiple"
                    :max-tag-count="4"
                    allowClear
                    style="width: 70%; margin-left: 10px"
                    placeholder="请选择"
                    size="small"
                    :options="tsOptions"
                    @search="onTSSearch"
                    @change="onTSChange"
                    @blur="onTSBlur"
                  >
                    <template #dropdownRender="{ menuNode: menu }">
                      <v-nodes :vnodes="menu" />
                      <a-divider style="margin: 4px 0" />
                      <div
                        style="padding: 4px 8px; cursor: pointer"
                        @mousedown="(e) => e.preventDefault()"
                      >
                        <a-button type="link" @click="selectAllTs"
                          >全选</a-button
                        >
                        <a-button type="link" @click="clearAllTs"
                          >清空</a-button
                        >
                      </div>
                    </template>
                  </a-select>
                  <a-button
                    type="primary"
                    ghost
                    size="small"
                    style="float: right"
                    :loading="loading"
                    @click="importEntryData"
                    >导入</a-button
                  >
                </a-form-item>
              </a-form>
            </div>
            <div
              class="dataTypeBox"
              v-if="dataType === 'dictionary'"
              ref="dicRef"
            >
              <a-form
                ref="dictSelectRef"
                name="advanced_search"
                class="ant-advanced-search-form"
                :model="dict"
                style="width: 100%"
              >
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="辞典"
                      name="dictionaryType"
                      :rules="[{ required: true, message: '请选择辞典!' }]"
                    >
                      <a-tree-select
                        v-model:value="dict.dictionaryType"
                        v-model:searchValue="searchDicValue"
                        show-search
                        tree-checkable
                        style="width: 100%"
                        :dropdown-style="{
                          maxHeight: '400px',
                          overflow: 'auto',
                        }"
                        placeholder="请选择"
                        allow-clear
                        multiple
                        :tree-data="notEffectiveDicts"
                        :max-tag-count="2"
                        size="small"
                        tree-node-filter-prop="label"
                        @search="onDicSearch"
                        @change="onDicChange"
                        @blur="onDicBlur"
                      >
                      </a-tree-select>
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-button
                      type="primary"
                      ghost
                      size="small"
                      style="float: right"
                      :loading="loading"
                      @click="importEntryData"
                      >导入</a-button
                    >
                  </a-col>
                </a-row>
              </a-form>
            </div>
            <div
              class="dataTypeBox"
              v-if="dataType === 'database'"
              style="padding-top: 0px"
              ref="dataSourceRef"
            >
              <a-tabs
                v-model:activeKey="dataLibrary.type"
                size="small"
                style="width: 100%"
                @change="changeDataLibraryType"
              >
                <a-tab-pane key="field" tab="对象数据"></a-tab-pane>
                <a-tab-pane key="alias" tab="元数据"></a-tab-pane>
                <a-tab-pane key="allData" tab="全量"></a-tab-pane>
              </a-tabs>
              <a-form
                ref="fieldFormRef"
                name="advanced_search"
                class="ant-advanced-search-form"
                :model="dataLibrary"
                style="width: 100%"
                v-if="dataLibrary.type === 'field'"
              >
                <a-row :gutter="24">
                  <a-col :span="8">
                    <a-form-item
                      label="数据库"
                      name="table"
                      :rules="[{ required: true, message: '请选择数据库!' }]"
                    >
                      <a-tree-select
                        v-model:value="dataLibrary.table"
                        v-model:searchValue="searchValue"
                        allowClear
                        tree-data-simple-mode
                        show-search
                        style="width: 100%"
                        :dropdown-style="{
                          maxHeight: '400px',
                          overflow: 'auto',
                        }"
                        :tree-data="treeData"
                        placeholder="请选择表"
                        :load-data="onLoadData"
                        :show-checked-strategy="SHOW_PARENT"
                        @select="treeSelect"
                        tree-node-filter-prop="title"
                        size="small"
                      >
                        <template #title="{ title }">
                          <template
                            v-for="(fragment, i) in title
                              .toString()
                              .split(
                                new RegExp(
                                  `(?<=${searchValue})|(?=${searchValue})`,
                                  'i',
                                ),
                              )"
                          >
                            <span
                              v-if="
                                fragment.toLowerCase() ===
                                searchValue.toLowerCase()
                              "
                              :key="i"
                              style="color: #08c"
                            >
                              {{ fragment }}
                            </span>
                            <template v-else>{{ fragment }}</template>
                          </template>
                        </template>
                      </a-tree-select>
                    </a-form-item>
                  </a-col>
                  <a-col :span="8">
                    <a-form-item
                      label="字段"
                      name="field"
                      :rules="[{ required: true, message: '请选择字段!' }]"
                    >
                      <a-select
                        v-model:value="dataLibrary.field"
                        mode="multiple"
                        allowClear
                        placeholder="请选择字段"
                        :options="fieldOptions"
                        :max-tag-count="3"
                        size="small"
                      >
                        <template #dropdownRender="{ menuNode: menu }">
                          <v-nodes :vnodes="menu" />
                          <a-divider style="margin: 4px 0" />
                          <div
                            style="padding: 4px 8px; cursor: pointer"
                            @mousedown="(e) => e.preventDefault()"
                          >
                            <a-button type="link" @click="selectAllField"
                              >全选</a-button
                            >
                            <a-button type="link" @click="clearAllField"
                              >清空</a-button
                            >
                          </div>
                        </template>
                      </a-select>
                    </a-form-item>
                  </a-col>
                  <a-col :span="8">
                    <a-button
                      type="primary"
                      ghost
                      size="small"
                      style="float: right"
                      :loading="loading"
                      @click="importEntryData"
                      >导入</a-button
                    >
                  </a-col>
                </a-row>
              </a-form>
              <a-form
                ref="aliasFormRef"
                name="advanced_search"
                class="ant-advanced-search-form"
                :model="dataLibrary"
                style="width: 100%"
                v-if="dataLibrary.type === 'alias'"
              >
                <a-row :gutter="24">
                  <a-col :span="8">
                    <a-form-item
                      label="数据库"
                      name="table"
                      :rules="[{ required: true, message: '请选择库!' }]"
                    >
                      <a-tree-select
                        v-model:value="dataLibrary.table"
                        v-model:searchValue="searchValue"
                        tree-data-simple-mode
                        allowClear
                        show-search
                        style="width: 100%"
                        :dropdown-style="{
                          maxHeight: '400px',
                          overflow: 'auto',
                        }"
                        :tree-data="treeData"
                        placeholder="请选择库"
                        :load-data="onLoadData"
                        @select="treeSelect"
                        tree-node-filter-prop="title"
                        size="small"
                      >
                        <template #title="{ title }">
                          <template
                            v-for="(fragment, i) in title
                              .toString()
                              .split(
                                new RegExp(
                                  `(?<=${searchValue})|(?=${searchValue})`,
                                  'i',
                                ),
                              )"
                          >
                            <span
                              v-if="
                                fragment.toLowerCase() ===
                                searchValue.toLowerCase()
                              "
                              :key="i"
                              style="color: #08c"
                            >
                              {{ fragment }}
                            </span>
                            <template v-else>{{ fragment }}</template>
                          </template>
                        </template>
                      </a-tree-select>
                    </a-form-item>
                  </a-col>
                  <a-col :span="8">
                    <a-form-item>
                      <template #label>
                        <span>翻译最大长度</span>
                        <a-tooltip>
                          <template #title>
                            译文字符数上限，最多可输入相应数量的字或字母；空格也计数，首尾空格不忽略。
                          </template>
                          <InfoCircleOutlined style="margin-left: 4px; color: #00000066" />
                        </a-tooltip>
                      </template>
                      <a-input-number
                        v-model:value="dataLibrary.maxLength"
                        style="width: 100%"
                        size="small"
                        placeholder="请输入翻译最大长度"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="8">
                    <a-button
                      type="primary"
                      ghost
                      size="small"
                      style="float: right"
                      :loading="loading"
                      @click="importEntryData"
                      >导入</a-button
                    >
                  </a-col>
                </a-row>
              </a-form>
              <a-form
                ref="allDataFormRef"
                name="advanced_search"
                class="ant-advanced-search-form"
                :model="dataLibrary"
                style="width: 100%"
                v-if="dataLibrary.type === 'allData'"
              >
                <a-row :gutter="24">
                  <a-col :span="12">
                    <a-form-item
                      label="数据库"
                      name="tables"
                      :rules="[{ required: true, message: '请选择!' }]"
                    >
                      <a-tree-select
                        v-model:value="dataLibrary.tables"
                        v-model:searchValue="searchValue"
                        show-search
                        allowClear
                        tree-data-simple-mode
                        style="width: 100%"
                        :dropdown-style="{
                          maxHeight: '400px',
                          overflow: 'auto',
                        }"
                        :tree-data="treeData"
                        placeholder="请选择"
                        :load-data="onLoadData"
                        :maxTagCount="3"
                        tree-checkable
                        :show-checked-strategy="SHOW_PARENT"
                        @select="treeBatchSelect"
                        tree-node-filter-prop="title"
                        size="small"
                      >
                        <template #title="{ title }">
                          <template
                            v-for="(fragment, i) in title
                              .toString()
                              .split(
                                new RegExp(
                                  `(?<=${searchValue})|(?=${searchValue})`,
                                  'i',
                                ),
                              )"
                          >
                            <span
                              v-if="
                                fragment.toLowerCase() ===
                                searchValue.toLowerCase()
                              "
                              :key="i"
                              style="color: #08c"
                            >
                              {{ fragment }}
                            </span>
                            <template v-else>{{ fragment }}</template>
                          </template>
                        </template>
                      </a-tree-select>
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-button
                      type="primary"
                      ghost
                      size="small"
                      style="float: right"
                      :loading="loading"
                      @click="importEntryData"
                      >导入</a-button
                    >
                  </a-col>
                </a-row>
              </a-form>
            </div>
            <div
              class="dataTypeBox2"
              v-if="dataType === 'config'"
              ref="configRef"
            >
              <a-form
                ref="configFormRef"
                name="advanced_search"
                class="ant-advanced-search-form"
                :model="configFile"
                style="width: 100%"
              >
                <a-form-item
                  label="配置文件"
                  name="config"
                  :rules="[{ required: true, message: '请选择配置文件!' }]"
                >
                  <a-select
                    v-model:value="configFile.config"
                    v-model:searchValue="searchConfigValue"
                    mode="multiple"
                    :max-tag-count="3"
                    allowClear
                    placeholder="请选择配置文件"
                    :options="configFile.configOptions"
                    style="width: 50%"
                    size="small"
                    @search="onConfigSearch"
                    @change="onConfigChange"
                    @blur="onConfigBlur"
                  >
                    <template #dropdownRender="{ menuNode: menu }">
                      <v-nodes :vnodes="menu" />
                      <a-divider style="margin: 4px 0" />
                      <div
                        style="padding: 4px 8px; cursor: pointer"
                        @mousedown="(e) => e.preventDefault()"
                      >
                        <a-button type="link" @click="selectAllConfig"
                          >全选</a-button
                        >
                        <a-button type="link" @click="clearAllConfig"
                          >清空</a-button
                        >
                      </div>
                    </template>
                  </a-select>
                </a-form-item>
              </a-form>
              <a-button
                type="primary"
                ghost
                size="small"
                style="margin-left: auto"
                :loading="loading"
                @click="importEntryData"
                >导入</a-button
              >
            </div>
            <div class="dataTypeBox2" v-if="dataType === 'enum'" ref="enumRef">
              <a-form
                ref="enumFormRef"
                name="advanced_search"
                class="ant-advanced-search-form"
                :model="enumFile"
                style="width: 100%"
              >
                <a-form-item
                  label="枚举文件"
                  name="enum"
                  :rules="[{ required: true, message: '请选择枚举文件!' }]"
                >
                  <a-select
                    v-model:value="enumFile.enum"
                    v-model:searchValue="searchEnumValue"
                    mode="multiple"
                    :max-tag-count="3"
                    allowClear
                    placeholder="请选择枚举文件"
                    :options="enumFile.enumOptions"
                    style="width: 50%"
                    size="small"
                    @search="onEnumSearch"
                    @change="onEnumChange"
                    @blur="onEnumBlur"
                  >
                    <template #dropdownRender="{ menuNode: menu }">
                      <v-nodes :vnodes="menu" />
                      <a-divider style="margin: 4px 0" />
                      <div
                        style="padding: 4px 8px; cursor: pointer"
                        @mousedown="(e) => e.preventDefault()"
                      >
                        <a-button type="link" @click="selectAllEnum"
                          >全选</a-button
                        >
                        <a-button type="link" @click="clearAllEnum"
                          >清空</a-button
                        >
                      </div>
                    </template>
                  </a-select>
                </a-form-item>
              </a-form>
              <a-button
                type="primary"
                ghost
                size="small"
                style="margin-left: auto"
                :loading="loading"
                @click="importEntryData"
                >导入</a-button
              >
            </div>
          </div>
        </template>
        <!-- 表格上方：词条/存在状态筛选 -->
        <div class="import-table-filters">
          <a-form
            layout="inline"
            autocomplete="off"
            style="display: flex; gap: 8px; align-items: center"
          >
            <a-form-item
              label="词条"
              name="entry"
              style="width: auto; height: auto; margin: 0"
            >
              <a-input
                v-model:value="keyWords"
                size="small"
                placeholder="词条"
                style="width: 100px"
              />
            </a-form-item>
            <a-form-item
              label="存在状态"
              name="isExist"
              style="width: auto; height: auto; margin: 0"
            >
              <a-select
                v-model:value="isExist"
                style="width: 120px"
                placeholder="请选择存在状态"
                :options="isExistOptions"
                allowClear
                size="small"
              />
            </a-form-item>
          </a-form>
        </div>

        <!-- 词条：
        <a-input v-model:value="keyWords" style="width:30%" size="small" placeholder='请输入词条搜索' /> -->
        <a-button
          type="primary"
          size="small"
          style="margin-left: 8px"
          @click="select"
        >
          <template #icon> <SearchOutlined /> </template>查询
        </a-button>
        <a-button
          type="primary"
          danger
          size="small"
          style="margin-left: 8px"
          @click="deleteEntry"
        >
          <template #icon> <DeleteOutlined /> </template>删除
        </a-button>
        <template #trailing>
          <!-- 展示列 + 释义覆盖（工具栏最右侧，非表头放大镜） -->
          <ColumnActions
            v-model="checkedColumn"
            :columns="columnSettingsList"
            :overlay-style="overlayStyle"
            col-pref-name="colPref-importModal"
            :normal-width="100"
            show-cover-button
            :cover-button-props="{
              translate: task.translateType,
              dataSource,
              oldEditableData: editableData,
            }"
            @update:old-editable-data="editableData = $event"
            @show-edit-operation="showEditOperation"
            @change="syncColumnsFromPref"
          />
        </template>
        <template #subToolbar>
          <div class="select">
            <LanguageFilter
              v-model="filterLanguage"
              @change="filterLanguageChange"
            />
          </div>
        </template>
      </PipelinePanel>
    </div>
    <template #leftBottomBtn>
      <a-button
        type="primary"
        size="small"
        style="margin-left: 8px; float: left"
        class="resetBtn"
        @click="aggregation"
        >聚合</a-button
      >
      <a-button
        type="primary"
        size="small"
        style="margin-left: 8px; float: left"
        class="yellowBtn"
        @click="cancelAggregation"
        >取消聚合</a-button
      >
    </template>
  </PipeShell>
  <Dict
    :visible="createDictVisible"
    :currentIP="ip"
    @modalClose="createDictClose"
    @modalOK="createDictOk"
  />
  <CustomModal
    :visible="templateVisible"
    modalTitle="模板下载"
    @handleOK="templateDownload"
    @handleClose="templateClose"
    :okLoading="loading"
  >
    <div class="condent templateForm">
      <a-form
        ref="dictRef"
        name="advanced_search"
        class="ant-advanced-search-form"
        :model="templateObj"
        style="width: 100%"
      >
        <a-form-item
          label="模板类型"
          name="type"
          :rules="[{ required: true, message: '请选择模板类型!' }]"
        >
          <a-select
            v-model:value="templateObj.type"
            placeholder="请选择"
            :options="departmentList"
            allowClear
          >
          </a-select>
        </a-form-item>
        <a-form-item
          label="文件类型"
          name="exportType"
          :rules="[{ required: true, message: '请选择文件类型!' }]"
        >
          <a-select
            v-model:value="templateObj.exportType"
            placeholder="请选择文件类型"
            :options="exportTypes"
            allowClear
          >
          </a-select>
        </a-form-item>
        <!-- <a-form-item label="导出字段" name="exportFields" :rules="[{ required: true, message: '请选择!' }]">
          <div style="display: flex; justify-content: space-between;">
            <a-select mode="multiple" v-model:value="exportFields" :options="exportFieldOptions" placeholder="请选择导出字段" allowClear
              style="flex: 1; margin-right: 8px;" />
            <a-button type="link" size="small" @click="selectAllFields" style="
              font-size: smaller;margin-top:0">全选</a-button>
          </div>
        </a-form-item> -->
      </a-form>
    </div>
  </CustomModal>
</template>
<script>
import "@/assets/style/common.less";
import CustomModal from "@/components/modal/index.vue";
import PipeShell from "@/views/workbench/components/PipeShell.vue";
import Dict from "@/views/dictionary/dictModal.vue";
import RulesDropdown from "@/components/Dropdown/rulesDropdown.vue";
import IsExistBadge from "@/components/stateBadge/isExistBadge.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import CoverButton from "@/components/Button/coverButton/inter2value.vue";
import { add, cloneDeep, iteratee } from "lodash-es";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import { v4 as uuidv4 } from "uuid";
import { TreeSelect, Table } from "ant-design-vue";
import {
  CheckOutlined,
  CloseOutlined,
  ExclamationCircleOutlined,
  PlusSquareOutlined,
  SettingOutlined,
  SearchOutlined,
  DeleteOutlined,
  DownOutlined,
  UpOutlined,
  InfoCircleOutlined,
} from "@ant-design/icons-vue";
import {
  getFileListByLang,
  getTsWords,
  getDictionary,
  getDictionaryEntry,
  getAllNode,
  getAppByNode,
  getdbByApp,
  getTableByApp,
  getFieldByTable,
  getFieldData,
  getAlias,
  getConfigEntry,
  getEnumEntry,
  createDic,
  getDBALLEntryByApp,
  getDBALLEntryByNode,
  getDBALLEntryByDB,
  getInvalidDictionary,
  valDictionary,
  importDictionaryEntry,
  getConfigList,
  getEnumList,
} from "@/http/api/i18Server";
import {
  insertEntry,
  getEntryTempByTaskID,
  importExcle,
  readZZExcle,
  deleteEntryTempByID,
  getEntryInfoList,
  updateEntryList,
  deleteEntryInfoByTaskID,
  getI18nAdress,
} from "@/http/api/workbench";
import { templateFileDownload } from "@/http/api/download";
import commonParam, {
  entryParams,
  workbenchParams,
} from "@/constants/commonParam.js";
import {
  applyTableSelectionById,
  invalidateSelectionWhenFiltersChange,
  clearTableSelection,
  clearTableRowsContext,
  resetTableQuerySession,
  flattenRowsById,
  getCurrentSelectionRows,
  removeSelectedEntriesById,
  pageChange,
  refreshTableSelectionById,
  updateTableSelectionById,
} from "@/utils/selectionUtils";
import { createQueryVersionGuard } from "@/utils/queryVersionGuard";
import { partitionSubmittedRows } from "@/utils/batchUpdateResults.js";
import {
  getMaxLength,
  byteLength,
  verifyArray_workbench,
  openSetEdit,
  clearCellErrorsForRecords,
  onEditableCellInput,
  getMethods, // RulesDropdown 勾选 → toLong/special
  revalidateLoaded,
  saveEdit,
  cancelEdit,
  // as 别名：避免 methods 里 showEditOperation() 递归调用自身
  showEditOperation as showEditOp,
  hideEditOperation as hideEditOp,
} from "@/utils/validationUtils";
import { loading, startLoading, endLoading, resetLoading } from "@/composables/useLoading";
import { interpretation2value } from "@/utils/translationUtils";
import InputIME from "@/components/cellEditor/input_IME.vue";
import TableCellTextArea from "@/components/table/TableCellTextArea/TableCellTextArea.vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip/CellOverflowTooltip.vue";
import {
  formatEntryText,
  formatCellText,
  formatMaxLengthText,
} from "@/components/table/cellText";
import {
  applyTable,
  syncColumnsFromPref as applyTableColumnsFromPref,
} from "@/components/ColumnFilter";
import { filterWbColsForCtx } from "@/components/ColumnFilter/columnBuilder.js";
import { wbAllCols, wbPresets } from "@/constants/commonParam.js";
import { getIsExistLabel } from "@/constants/statusMaps.js";
import {
  PipelinePanel,
  ColumnActions,
  LanguageFilter,
} from "@/views/workbench/components";
import { filterLanguageChange as applyLanguageFilter } from "@/composables/workbench/useLanguageFilter";
import {
  defaultPagination,
  pageChange as wbPageChange,
} from "@/views/workbench/composables/page";
import {
  handleFilterSearch,
  resetOnClose,
} from "@/views/workbench/composables/filterClear";
import { editTextCols } from "@/views/workbench/utils/editCols";
import {
  getRowClassName,
  handleReset as tableHandleReset,
} from "@/utils/tableUtils";
import { setModalAriaHidden } from "@/utils/domUtils";
import { handleAsyncRequest } from "@/utils/requestUtils";
import FileSelectWithEncoding from "@/components/FileSelectWithEncoding/index.vue";
import {
  DEFAULT_ENCODING,
  CSV_ONLY_ACCEPT,
  shouldShowEncoding,
} from "@/components/FileSelectWithEncoding/constants";
import { assertCsvEncodingMatch } from "@/utils/encodingDetectUtils";
const filteredInfo = {};
const ALL_ISEXIST = "-1";

export default {
  // 全局单一 loading 状态（hook 导出，响应式）：表格/保存/导入/模板下载按钮统一绑定
  setup() {
    return { loading };
  },
  components: {
    CheckOutlined,
    CloseOutlined,
    CustomModal,
    PipeShell,
    ExclamationCircleOutlined,
    PlusSquareOutlined,
    SettingOutlined,
    SearchOutlined,
    DeleteOutlined,
    DownOutlined,
    UpOutlined,
    InfoCircleOutlined,
    Dict,
    RulesDropdown,
    IsExistBadge,
    EntryStateBadge,
    TransStateBadge,
    InputIME,
    TableCellTextArea,
    CellOverflowTooltip,
    PipelinePanel,
    ColumnActions,
    LanguageFilter,
    FileSelectWithEncoding,
    VNodes: (_, { attrs }) => {
      return attrs.vnodes;
    },
  },
  emits: ["handleClose", "handleOK", "afterSave"],
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    modalTitle: {
      type: String,
      default: "词条导入",
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
      // 当前登录用户信息（从 Vuex 注入），用于一些接口参数（如 departmentType）
      user: null,
      task: {},
      filePath: "",
      fileEncoding: DEFAULT_ENCODING,
      filediFileName: null,
      keyWords: "",
      isExist: ALL_ISEXIST,
      isExistOptions: [
        { label: "全部", value: ALL_ISEXIST },
        { label: getIsExistLabel("1"), value: "1" },
        { label: getIsExistLabel("0"), value: "0" },
      ],
      dataType: "file",
      tableHeight: { x: "max-content", y: "300px" },
      // tableHeight: { x: "100%", y: "300px" },
      columns: [],
      dataSource: [],
      editableData: {},
      allData: [],
      pagination: defaultPagination(this.pageChange),
      selectedRowKeys: [],
      selectedRows: [],
      accept: CSV_ONLY_ACCEPT,
      tsOptions: [],
      selectTitle: "",
      tsFile: {
        tsFileValue: [],
      },
      dict: {
        dictionaryType: [],
      },
      dataLibrary: {
        type: "field",
        table: null,
        tables: [],
        field: [],
        diFileName: null,
        selectNode: [],
        maxLength: null,
      },
      configFile: {
        config: [],
        configOptions: [],
      },
      enumFile: {
        enum: [],
        enumOptions: [],
      },
      nodeOptions: [],
      serverOptions: [],
      libraryOptions: [],
      tableOptions: [],
      fieldOptions: [],
      dictionaryOptions: [],
      file: {},
      treeData: [],
      rules: {},
      cellErrors: {},
      // classifyLimit:{
      //     间隔: 10
      // },
      SHOW_PARENT: TreeSelect.SHOW_PARENT,
      createDictVisible: false,
      createDict: {
        name: "",
      },
      exportFields: [], // 下载模板的导出字段
      exportFieldOptions: entryParams.exportFields,
      overlayStyle: workbenchParams.overlayStyle, // 展示列样式
      columnSettingsList: [],
      checkboxList: [], // 展示列可选的值
      checkedColumn: [], // 展示列已选的值
      editList_needValidate: null, // 可编辑且需要表单校验的list(工作台只有任务的翻译语种可编辑,并且需要进行表单校验)
      editList: null, // 可编辑的list[...commonParam.langInterList, "comment"]
      translateStateList: [
        ...commonParam.langTranslateStateList,
        "translateState",
      ],
      state: {
        searchText: "",
        searchedColumn: "",
      },
      filters: null,
      // 历史字段 filteredData：曾由 handleTableChange 写入，仓库内无读取方；批量勾选走 this.filters + selectionUtils（AND）。
      antClearFilter: null,
      queryGuard: createQueryVersionGuard(),
      importGuard: createQueryVersionGuard(),
      queryScope: "",
      queryLoading: false,
      filterLanguage: null,
      filterSource: [],
      templateVisible: false,
      templateObj: {
        type: null,
        exportType: null,
        // field: [], // 导出模板的字段
      },
      platformKey: "device",
      searchValue: "",
      notEffectiveDict: [],
      notEffectiveDicts: [],
      importedDic: ["平台/svc_sec_usermgr"],
      defaultExpandedKeys: [],
      searchDicValue: "",
      ip: null,
      ips: [],
      searchTSValue: "",
      searchConfigValue: "",
      searchEnumValue: "",
      departmentList: commonParam.departmentList, // 当前用户所在部门
      templateTypes: null,
      templateType: null,
      exportTypes: [
        { label: "excel", value: "excel" },
        { label: "csv", value: "csv" },
      ],
      rulesOptions: commonParam.rulesOptions,
    };
  },
  created() {
    // 从全局 store 注入当前用户，避免直接读取 this.$store.state.user 为空时报错
    this.user =
      (this.$store && this.$store.state && this.$store.state.user) || {};
  },
  computed: {
    importEditCols() {
      return [
        ...this.editableTextAreaColumns,
        "diFileName",
        "tag",
        "maxLength",
      ];
    },
    editableTextAreaColumns() {
      return editTextCols(this, { withEditList: true });
    },
    importRowSelection() {
      return {
        columnWidth: 48,
        selectedRowKeys: this.selectedRowKeys,
        onSelect: this.onSelect,
        onSelectAll: this.onSelectAll,
        checkStrictly: false,
      };
    },
  },
  watch: {
    /**
     * 切换导入任务时清空旧选择并在弹窗打开时加载新任务。
     * @param {Object} newTask - 当前任务。
     * @param {Object} oldTask - 上一个任务。
     * @returns {void} 无返回值。
     */
    currentTask(newval, oldval) {
      this.task = newval;
      this.task.transMap = commonParam.languageMap[this.task.translateType];
      if (newval?.id !== oldval?.id) {
        resetTableQuerySession(this, [this.queryGuard, this.importGuard]);
        if (this.visible) this.initTaskEntry();
      }
    },
    rulesOptions: {
      deep: true,
      async handler() {
        const transCol = this.task?.transMap?.value;
        if (!transCol) return;
        await revalidateLoaded(this, transCol);
      },
    },
    visible: {
      /**
       * 响应导入弹窗开关并使关闭前的列表请求失效。
       * @param {boolean} newValue - 弹窗当前是否打开。
       * @returns {Promise<void>} 可见状态处理完成后结束。
       */
      async handler(newVal) {
        // console.log("打开工作台-导入", newVal);
        if (newVal) {
          this.$nextTick(() => {
            // 1.设置模板类型
            // 获取模板类型（暂时只有装置部使用多个模板）
            if (
              this.$currentDepartment &&
              Object.keys(this.$currentDepartment).includes("templateType")
            )
              this.templateTypes = this.$currentDepartment.templateType;
            // 设置默认的模板类型为本部门的
            if (this.$currentDepartment) {
              this.templateObj.type = this.$currentDepartment.value;
            }
            if (this.templateObj.type === "default")
              this.templateObj.type = null; // 如果是默认部门，则不设置模板类型，否则会报错

            // 2.获取IP地址
            this.getIPs();

            // 3.设置翻译列展示的语种
            // 设置翻译列可编辑&可校验
            this.editList_needValidate = [this.task.transMap.value];
            // 设置对应的翻译释义列可编辑
            this.editList = [this.task.transMap.interpretation, "comment"];
            applyTable(this, {
              allCols: wbAllCols,
              preset: wbPresets.importModal,
              ctx: {
                task: this.task,
                transMap: this.task.transMap,
                pagination: this.pagination,
              },
              colPrefName: "colPref-importModal",
              normalWidth: 100,
              needFilter: true,
              filterCols: filterWbColsForCtx,
            });
          });
        } else {
          resetTableQuerySession(this, [this.queryGuard, this.importGuard]);
        }
      },
    },
  },
  methods: {
    /**
     * 应用展示列偏好并通知共享表格重新建立列布局。
     * @returns {void} 无返回值。
     */
    syncColumnsFromPref() {
      applyTableColumnsFromPref(this);
      this.columns = [...this.columns];
    },
    // 全选导出字段方法
    selectAllFields() {
      this.exportFields = this.exportFieldOptions;
      // console.log("this.exportFields", this.exportFields);
    },
    // 释义覆盖翻译
    interpretation2value() {
      interpretation2value(this);
    },
    handleOK() {
      if (this.selectedRows.length === 0) {
        message.info("请勾选需要保存的词条！");
        return;
      }
      // saveEntrys 内部通过 useLoading 管理 loading，勿在此处手动关闭（否则按钮 loading 会被提前覆盖）
      this.saveEntrys();
    },
    /**
     * 关闭导入弹窗并使当前列表请求失效。
     * @returns {void} 无返回值。
     */
    handleClose() {
      this.queryGuard.invalidate();
      this.importGuard.invalidate();
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.selectedRowIndex = null;
      this.$emit("handleClose");
    },
    getRowClassName(record, index) {
      return getRowClassName(record, index, this.selectedRowIndex);
    },
    onCellInput(value, record, column) {
      onEditableCellInput(this, record.id, column.dataIndex, value);
    },
    formatEntryText,
    formatCellText,
    formatMaxLengthText,
    /**
     * 校验当前选择并提交可保存词条；校验失败行保留编辑状态。
     * @returns {Promise<void>} 校验及保存请求完成后结束。
     */
    async saveEntrys() {
      // 全局单一 loading（引用计数）：表格遮罩/保存按钮统一开启，各结束路径 endLoading()
      startLoading();
      // 先移除旧红字；本次校验会重新标记仍需用户处理的单元格。
      clearCellErrorsForRecords(this, this.selectedRowKeys);
      const currentLang = this.task.transMap.value;
      let hasNoInter = false;
      let arr = {
        acceptIds: new Set(), // 所有校验通过
        errorIds: new Set(), // 所有校验不通过
        toLongIds: new Set(), // 校验长度
        specialIds: new Set(), // 校验特殊字符
      };
      // 1.先校验（classifyArr 优先读 editableData；失败行保持编辑）
      const methods = getMethods(this);
      try {
        arr = await verifyArray_workbench(
          this,
          this.selectedRows,
          currentLang,
          methods,
        );
      } catch (err) {
        message.error("词条校验失败", err.message);
        endLoading();
        return;
      }
      let arrCount = {
        updateArr: [],
        insertArr: [],
        toLongNum: arr.toLongIds.size,
        specialNum: arr.specialIds.size,
        errorNum: arr.errorIds.size,
        addNum: 0,
        addChildNum: 0,
        updateNum: 0,
        updateChildNum: 0,
      };

      if (this.allData.length === 0) {
        endLoading();
        return;
      }

      // 2.创建提交快照，避免接口确认前污染表格和失败行的编辑缓存。
      const selectedRows = this.getCurrentSaveRows();
      const stateKey = this.task.transMap.state;
      for (const row of flattenRowsById(selectedRows)) {
        if (
          arr.acceptIds.has(row.id) &&
          this.editableData[row.id]?.[currentLang] != null
        ) {
          row[stateKey] = "1";
        }
      }

      // 3.修改状态
      // 去除this.selectedRows中的子词条，因为父词条中已经包含了
      const childRowIDs = new Set(
        selectedRows
          .filter((item) => item.children && item.children.length > 0)
          .flatMap((item) => item.children.map((child) => child.id)),
      );
      const selectFatherRows = selectedRows.filter(
        (item) => !childRowIDs.has(item.id),
      );
      // 聚合父子行只提交父行，避免平铺选择导致同一子词条被重复写入。
      //this.selectedRows是把父子词条都平铺开了，以便显示选中状态
      for (const record of selectFatherRows) {
        if (arr.acceptIds.has(record.id)) {
          if (
            !hasNoInter &&
            !record.englishInterpretation &&
            !record.chineseInterpretation
          ) {
            hasNoInter = true;
          }
          if (record.entryState === 2) {
            // 如果是审核不通过的词条，重置为待审核状态
            record.entryState = 1;
            arrCount.updateArr.push(record);
            arrCount.updateNum++;
            if (record.children && record.children.length > 0) {
              record.children.forEach((child) => {
                child.entryState = 1;
              });
              arrCount.updateChildNum += record.children.length;
            }
          } else if (record.entryState === 1) {
            // 如果是待审核的词条，则直接更新
            arrCount.insertArr.push(record);
            arrCount.addNum++;
            if (record.children && record.children.length > 0) {
              arrCount.addChildNum += record.children.length;
            }
          }
        }
      }

      if (arrCount.insertArr.length === 0 && arrCount.updateArr.length === 0) {
        endLoading();
        if (arrCount.errorNum > 0) {
          message.warning("没有可保存的词条，请修正校验失败项");
        }
        return;
      }

      // 4.更新词条;弹窗;校验当前页数据
      if (hasNoInter) {
        // 释义缺失需要用户确认；取消时不清空列表和编辑态，用户可继续补录。
        Modal.confirm({
          title:
            "保存数据中含有中文释义和英文释义都不存在的词条，是否继续保存?",
          icon: createVNode(ExclamationCircleOutlined),
          content: "",
          okText: "是",
          cancelText: "否",
          style: { top: "30%" },
          onOk: () => {
            return this.insertOrUpdateEntrys(
              arrCount.insertArr,
              arrCount.updateArr,
              arrCount,
            );
          },
          onCancel: () => {
            endLoading();
          },
        });
      } else {
        return this.insertOrUpdateEntrys(
          arrCount.insertArr,
          arrCount.updateArr,
          arrCount,
        );
      }
    },

    /**
     * 按最新列表对象叠加编辑缓存，生成隔离于表格的保存快照。
     * @returns {Array<Object>} 当前已选词条及其子行的编辑后副本。
     */
    getCurrentSaveRows() {
      // 以当前列表的最新对象为底稿，避免筛选、复检后仍保存已失效的行引用。
      const currentById = new Map(
        flattenRowsById(this.dataSource).map((row) => [row.id, row]),
      );
      const mergeCurrentRow = (row) => {
        const current = currentById.get(row.id) || row;
        const merged = {
          ...cloneDeep(current),
          ...cloneDeep(this.editableData[row.id] || {}),
        };
        const childKey = Array.isArray(current.children)
          ? "children"
          : Array.isArray(current.child)
            ? "child"
            : null;
        if (childKey) {
          // 递归合并子行，使保存快照与界面上父子聚合关系保持一致。
          merged[childKey] = current[childKey].map(mergeCurrentRow);
        }
        return merged;
      };
      return this.selectedRows.map(mergeCurrentRow);
    },

    /**
     * 解析工作台批量保存响应，并仅从本地状态移除接口确认成功的行。
     * @param {Array<Object>} submittedRows - 本次接口提交的父级行。
     * @param {Object} response - 工作台批量保存接口响应。
     * @param {string} actionLabel - 本次操作文案，如新增或重置。
     * @param {number} childCount - 随父级行提交的聚合子行数。
     * @param {Array<string>} messageParts - 汇总提示的消息片段数组。
     * @returns {{successRows: Array<Object>, hasFailures: boolean}} 保存结果。
     */
    applySaveResponse(submittedRows, response, actionLabel, childCount, messageParts) {
      const {
        successRows,
        failedCount,
      } = partitionSubmittedRows(submittedRows, response);
      const successCount = successRows.length;

      if (successCount > 0) messageParts.push(`${actionLabel}${successCount}条`);
      if (failedCount > 0) messageParts.push(`${actionLabel}失败${failedCount}条`);
      if (childCount > 0) messageParts.push(`其中聚合的数据${childCount}条`);

      if (successRows.length > 0) {
        // 仅移除接口确认成功的记录；失败行及其编辑值留在当前 UI 中供再次处理。
        const successIds = flattenRowsById(successRows).map((row) => row.id);
        const successIdSet = new Set(successIds);
        this.dataSource = this.dataSource.filter((row) => !successIdSet.has(row.id));
        this.selectedRows = removeSelectedEntriesById(
          this.selectedRows,
          successIds,
        );
        this.selectedRowKeys = this.selectedRowKeys.filter(
          (id) => !successIdSet.has(id),
        );
        for (const id of successIds) delete this.editableData[id];
      }

      return {
        successRows,
        hasFailures: failedCount > 0,
      };
    },

    /**
     * 提交新增和状态重置两类词条，只清理服务端确认成功的数据。
     * @param {Array<Object>} insertArr - 待新增行。
     * @param {Array<Object>} updateArr - 待重置状态行。
     * @param {Object} arrCount - 校验与父子行统计结果。
     * @returns {Promise<void>} 两类批量保存请求完成后结束。
     */
    async insertOrUpdateEntrys(insertArr, updateArr, arrCount) {
      // 表格遮罩/保存按钮已在 saveEntrys 的 startLoading 中统一开启，这里不再重复开关
      let params = {
        taskID: this.task.id,
      };
      const promises = [];
      let messageTextParts = [];
      let hasFailures = arrCount.errorNum > 0;

      if (arrCount.errorNum > 0) {
        let errorNumText = `校验不通过${arrCount.errorNum}条`;
        messageTextParts.push(errorNumText);
      }
      // if (arrCount.toLongNum > 0) {
      //   let toLongNumText = `字符长度超限${arrCount.toLongNum}条`;
      //   messageTextParts.push(toLongNumText);
      // }
      // if (arrCount.specialNum > 0) {
      //   let specialNumText = `特殊字符不一致${arrCount.specialNum}条`;
      //   messageTextParts.push(specialNumText);
      // }

      if (arrCount.addNum > 0) {
        // 新增与重置状态分别请求，才能在部分失败时给出准确的数量反馈。
        // 新增
        const addPromise = insertEntry(params, insertArr)
          .then((res) => {
            const result = this.applySaveResponse(
              insertArr,
              res,
              "新增数据",
              arrCount.addChildNum,
              messageTextParts,
            );
            hasFailures ||= result.hasFailures;
            return result;
          })
          .catch((err) => {
            hasFailures = true;
            message.error("新增失败", err.message);
            return { successRows: [], hasFailures: true };
          });
        promises.push(addPromise);
      }
      if (updateArr.length > 0) {
        // 编辑
        const updatePromise = updateEntryList(params, updateArr)
          .then((res) => {
            const result = this.applySaveResponse(
              updateArr,
              res,
              "重置已审核数据",
              arrCount.updateChildNum,
              messageTextParts,
            );
            hasFailures ||= result.hasFailures;
            return result;
          })
          .catch((err) => {
            hasFailures = true;
            message.error("重置失败", err.message);
            return { successRows: [], hasFailures: true };
          });
        promises.push(updatePromise);
      }

      try {
        // 等待两类请求结束后统一提示，避免先返回的一类造成“全部成功”的错觉。
        const results = await Promise.all(promises);
        const savedRows = results.flatMap((result) => result.successRows);
        if (messageTextParts.length > 0) {
          const text = "数据已保存！" + messageTextParts.join("，");
          if (hasFailures) message.warning(text);
          else message.success(text);
        }
        if (savedRows.length === 0) return;

        this.allData = this.dataSource;

        // 更新成功：刷新所有任务的小红点
        this.$emit("afterSave", this.currentTask);
        // 有失败行时保留本地结果，避免刷新查询覆盖其编辑状态。
        if (!hasFailures) this.initTaskEntry();

        this.selectedRowIndex = null;
        if (this.dataSource.length === 0) this.handleClose();
      } finally {
        endLoading();
        // 只有全量成功才复检页面；部分失败仍以编辑缓存为准，不能用查询结果覆盖。
        // 失败行保留编辑缓存，避免复检把未保存值写回原始行。
        if (!hasFailures && this.dataSource.length > 0) {
          revalidateLoaded(this, this.task.transMap.value).catch((err) => {
            // 仅兜底：不阻断保存流程
            // eslint-disable-next-line no-console
            console.warn("[importModal] revalidateLoaded failed", err);
          });
        }
      }
    },
    /**
     * 查询当前任务中审核未通过的词条，仅提交最新任务响应。
     * @returns {Promise<void>} 查询完成后结束。
     */
    async initTaskEntry() {
      const params = {
        taskID: this.task.id,
        entryState: "2",
        entry: this.keyWords,
      };
      const scope = JSON.stringify(params);
      if (scope !== this.queryScope) {
        // 查询范围变化时仅清空列表和编辑上下文，保留跨查询的已选词条参与保存。
        this.queryScope = scope;
        clearTableRowsContext(this);
      }
      // 版本令牌保证快速输入筛选时只让最后一次请求替换导入列表。
      const token = this.queryGuard.next();
      const taskId = this.task.id;
      this.queryLoading = true;
      try {
        const res = await getEntryInfoList(params, []);
        // 弹窗复用到另一任务时，迟到响应不能写入新的导入会话。
        if (!this.queryGuard.isCurrent(token) || this.task.id !== taskId) return;
        if (res.data?.list?.length > 0) {
          this.dataSource = res.data.list;
          this.allData = this.dataSource;
          refreshTableSelectionById(this, this.dataSource);
        }
      } catch (err) {
        if (this.queryGuard.isCurrent(token)) message.error("3", err.message);
      } finally {
        if (this.queryGuard.isCurrent(token)) this.queryLoading = false;
      }
    },
    beforeUpload(file, fileList) {
      // 扩展名校验由 FileSelectWithEncoding（validateAcceptOnSelect）统一 notification
      if (file.path != undefined) {
        this.filePath = file.path;
      } else {
        this.filePath = file.name;
      }
      return false;
    },
    handleChange(info) {
      // console.log(info)
      this.file = info.file;
    },
    /**
     * 应用本地导入筛选并使旧列表请求和选择失效。
     * @returns {void} 无返回值。
     */
    select() {
      this.queryGuard.invalidate();
      clearTableSelection(this);
      if (this.filterLanguage === null) {
        this.dataSource = this.allData.filter(
          (item) =>
            item.entry.includes(this.keyWords) &&
            (this.isExist == ALL_ISEXIST || item.isExist == this.isExist),
        );
      } else {
        this.dataSource = this.filterSource.filter(
          (item) =>
            item.entry.includes(this.keyWords) &&
            (this.isExist == ALL_ISEXIST || item.isExist == this.isExist),
        );
      }
      this.pagination.current = 1;
      this.pagination.total = this.dataSource.length;
    },
    /**
     * 同步导入表格的单行操作，同时保留完整结果范围外的选择。
     * @param {Object} record - 本次点击的行。
     * @param {boolean} selected - 点击后的行选择状态。
     * @returns {void} 无返回值。
     */
    onSelect(record, selected) {
      updateTableSelectionById(this, [record], selected);
    },
    /**
     * 将表头复选框作用于当前完整查询结果。
     * @param {boolean} selected - 是否选中当前结果。
     * @returns {void} 无返回值。
     */
    onSelectAll(selected) {
      applyTableSelectionById(
        this,
        getCurrentSelectionRows(this),
        selected
      );
    },
    // 删除
    deleteEntry() {
      // console.log("task", this.task);
      if (this.selectedRowKeys.length === 0) {
        return;
      }
      Modal.confirm({
        title: "是否确定删除?",
        icon: createVNode(ExclamationCircleOutlined),
        okText: "确定",
        cancelText: "取消",
        style: { top: "30%" },
        onOk: () => {
          this.selectedRowKeys.forEach((id) => {
            this.dataSource = this.dataSource.filter((item) => {
              return item.id != id;
            });
            this.allData = this.allData.filter((item) => {
              return item.id != id;
            });
          });
          let deleteID = [];
          let delCount = {
            num: 0,
            childNum: 0,
          };
          this.selectedRows.forEach((item) => {
            if (item.entryState === 2) {
              // 词条审核未通过
              delCount.num++;
              deleteID.push(item.id);
              // 若存在子词条  则删除子词条
              if (item.children && item.children.length > 0) {
                delCount.childNum += item.children.length;
                item.children.forEach((child) => {
                  deleteID.push(child.id);
                });
              }
            }
          });
          this.selectedRowKeys = [];
          this.selectedRows = [];
          if (deleteID.length > 0) {
            deleteEntryInfoByTaskID({ taskID: this.task.id }, deleteID).then(
              (res) => {
                // 更新成功：刷新所有任务的小红点
                this.$emit("afterSave", this.currentTask);

                let text = `删除成功${delCount.num}条`;
                if (delCount.childNum > 0) {
                  text += `(其中聚合的数据${delCount.childNum}条)`;
                }
                message.success(text); // 最后一列的词条状态
              },
            );
          } else {
            message.success("已删除！"); // 看最后一列的词条状态：未审核
          }
        },
      });
    },
    // 数据类型选择事件
    dataTypeChange() {
      // 获取IP地址的表单校验
      if (this.ip === null || this.ip === undefined || this.ip === "") {
        message.info("请选择IP！");
        return;
      }

      this.dataSource = [];
      this.allData = [];
      this.pagination.current = 1;
      this.pagination.pageSize = 20;
      if (this.dataType === "file") {
        this.filePath = "";
        this.filediFileName = null;
        this.getDictionary();
      } else if (this.dataType === "database") {
        // 数据库
        this.dataLibrary.table = null;
        this.dataLibrary.field = [];
        this.dataLibrary.tables = [];
        this.dataLibrary.diFileName = null;
        this.dataLibrary.maxLength = null;
        this.dataLibrary.diFileName = null;
        this.filediFileName = null;
        this.getAllNode();
        this.getDictionary();
      } else if (this.dataType === "dictionary") {
        // 辞典
        this.filediFileName = null;
        this.dict.dictionaryType = [];
        this.getDictionary();
      } else if (this.dataType === "ts") {
        // TS
        this.selectTitle = "选择文件";
        this.filediFileName = null;
        this.getTsFiles();
      } else if (this.dataType === "config") {
        this.configFile.config = [];
        this.filediFileName = null;
        // this.getDictionary()
        this.getConfigList();
      } else if (this.dataType === "enum") {
        this.enumFile.enum = [];
        this.filediFileName = null;
        // this.getDictionary()
        this.getEnumList();
      }
    },
    // 获取ts文件
    getTsFiles() {
      let params = {
        language: this.task.translateType,
        i18nUrl: this.ip,
      };
      getFileListByLang(params).then((res) => {
        this.tsOptions = [];
        res.data.list.forEach((item) => {
          let option = {
            label: item,
            value: item,
          };
          this.tsOptions.push(option);
        });
      });
    },
    // 获取辞典文件
    getDictionary() {
      // 获取已生效的辞典
      let params = {
        i18nUrl: this.ip,
      };
      getDictionary(params).then((res) => {
        this.dictionaryOptions = [];
        if (res.data.list === null) {
          return;
        }
        res.data.list.forEach((item) => {
          let option = {
            label: item,
            value: item,
          };
          this.dictionaryOptions.push(option);
        });
      });
      //  获取未生效的辞典
      getInvalidDictionary(params).then((res) => {
        this.notEffectiveDicts = [];
        if (res.data.list === null) {
          return;
        }
        res.data.list.forEach((item) => {
          let temp = {
            label: item.label,
            value: item.label,
            // disabled: true
          };
          let list = [];
          item.options.forEach((op) => {
            let operate = {
              label: op,
              value: item.label + "/" + op,
            };
            list.push(operate);
          });
          temp.children = list;
          this.notEffectiveDicts.push(temp);
        });
        // console.log(this.notEffectiveDicts)
      });
    },
    // 获取数据库节点信息
    getAllNode() {
      // 获取IP地址的表单校验
      if (this.ip === null || this.ip === undefined || this.ip === "") {
        message.info("请选择IP！");
        return;
      }
      let params = {
        i18nUrl: this.ip,
      };
      getAllNode(params).then((res) => {
        this.treeData = [];
        res.data.list.forEach((item) => {
          let node = {
            id: uuidv4(),
            pId: 0,
            value: item,
            title: item,
            isLeaf: false,
            type: "node",
            key: item,
          };
          this.treeData.push(node);
        });
      });
    },
    // treeData加载子数据
    onLoadData(node) {
      // console.log("onLoad",node)
      const id = node.dataRef.id;
      const type = node.dataRef.type;
      return new Promise((resolve) => {
        if (type === "node") {
          // 获取应用
          let params = {
            nodeName: node.dataRef.value,
            i18nUrl: this.ip,
          };
          getAppByNode(params).then((res) => {
            res.data.list.forEach((item) => {
              const newId = uuidv4();
              let app = {
                id: newId,
                pId: id,
                value: newId,
                key: newId,
                title: item.appName,
                type: "app",
                appId: item.appName,
                node: node.value,
                modeName: item.modeName,
                isLeaf: false,
              };
              this.treeData.push(app);
            });
          });
        } else if (type === "app") {
          // 获取库
          // console.log(node.dataRef)
          let params = {
            nodeName: node.dataRef.node,
            appName: node.dataRef.title,
            // modeType: node.dataRef.appId
            modeName: node.dataRef.modeName,
            i18nUrl: this.ip,
          };
          getdbByApp(params).then((res) => {
            res.data.list.forEach((item) => {
              const newId = uuidv4();
              let temp = {
                id: newId,
                pId: id,
                value: newId,
                key: newId,
                title: item,
                type: "db",
                node: node.dataRef.node,
                app: node.dataRef.title,
                appId: node.dataRef.appId,
                isLeaf: this.dataLibrary.type === "field" ? false : true,
              };
              this.treeData.push(temp);
            });
          });
        } else if (type === "db") {
          // 获取表
          let params = {
            nodeName: node.dataRef.node,
            appName: node.dataRef.app,
            dbName: node.dataRef.title,
            i18nUrl: this.ip,
          };
          getTableByApp(params).then((res) => {
            if (res.data.list) {
              res.data.list.forEach((item) => {
                const newId = uuidv4();
                let table = {
                  id: newId,
                  pId: id,
                  value: newId,
                  key: newId,
                  tableId: item.tableId,
                  title: item.tableName,
                  type: "table",
                  node: node.dataRef.node,
                  app: node.dataRef.app,
                  db: node.dataRef.title,
                  isLeaf: true,
                };
                this.treeData.push(table);
              });
            }
          });
        }
        resolve(true);
      });
    },
    // 树选择事件
    treeSelect(value, node, extra) {
      // console.log("treeSelect",node)
      if (node.isLeaf) {
        this.dataLibrary.table = value;

        if (this.dataLibrary.type === "field") {
          let params = {
            dbName: node.db,
            nodeName: node.node,
            appName: node.app,
            tbName: node.title,
            i18nUrl: this.ip,
          };
          this.fieldOptions = [];
          this.dataLibrary.field = [];
          getFieldByTable(params).then((res) => {
            res.data.list.forEach((item) => {
              let table = {
                label: item.fieldName,
                value: item.fieldID,
                size: item.size,
              };
              this.fieldOptions.push(table);
            });
          });
        }
      } else {
        this.dataLibrary.table = null;
      }
    },
    // 树批量选择事件
    treeBatchSelect(value, node, extra) {
      // console.log(extra)
      // console.log(node)
    },
    // tabs切换
    changeDataLibraryType(activeKey) {
      this.treeData = [];
      this.dataLibrary.table = null;
      this.dataLibrary.field = [];
      this.getAllNode();
    },
    // 全选字段
    selectAllField() {
      this.dataLibrary.field = [];
      this.dataLibrary.field = this.fieldOptions.map((item, index) => {
        return item.value;
      });
    },
    // 清空选中的表
    clearAllField() {
      this.dataLibrary.field = [];
    },
    // 全选ts文件
    selectAllTs() {
      this.tsFile.tsFileValue = [];
      this.tsOptions.forEach((item) => {
        if (item.value.includes(this.searchTSValue)) {
          this.tsFile.tsFileValue.push(item.value);
        }
      });
    },
    // 清空选中的ts文件
    clearAllTs() {
      this.tsFile.tsFileValue = [];
    },
    /**
     * 导入词条数据（文件类型为 CSV 时先做编码一致性校验）
     * @returns {Promise<void>}
     */
    async importEntryData() {
      startLoading();
      // 导入链路可能较长；以任务和令牌约束异步结果，防止写入已切换的任务。
      const token = this.importGuard.next();
      const taskId = this.task.id;

      // 定义公共处理函数
      const handleCommonOperations = (data) => {
        // console.log("data数据", data);
        this.dataSource = []; // 清空数据
        if (data.length > 0) {
          this.dataSource = data;
          this.sortArray(this.dataSource, "isExist");
          this.dataSource.forEach((item) => {
            item.auditState = 1;
            // 装置部：行上展示模块上限（词条表不落这些字段）
            item.maxByte = this.classifyLimit[item.classfy1]?.["maxByte"];
            item.foreignMaxByte =
              this.classifyLimit[item.classfy1]?.["foreignMaxByte"];
            // console.log("打印词条", item,this.classifyLimit);
            // 一体化平台，文件导入且选择了回写词典时，修改diFileName和importType
            if (this.platformKey === "unify" && this.filediFileName != null) {
              // item.diFileName = this.filediFileName;// 通过接口readZZExcle已将diFileName传递给后端了，后端赋值后返回，所以不用前端再刷了
              item.writeType = "DI";
              if (item.children && item.children.length > 0) {
                item.children.forEach((child) => {
                  // child.diFileName = this.filediFileName;
                  child.writeType = "DI";
                });
              }
            }
          });
        }
        this.allData = this.dataSource;
      };

      // 生成通用 params 的函数
      const getCommonParams = (extraParams = {}) => {
        return {
          taskID: this.task.id,
          versionID: this.task.versionId ? this.task.versionId : "",
          translateType: this.task.translateType,
          i18nUrl: this.ip,
          ...extraParams,
        };
      };
      // // 处理异步请求的通用函数
      // const handleAsyncRequest = (
      //   validateRef,
      //   getDataFn,
      //   params,
      //   data = null
      // ) => {
      //   return validateRef
      //     .validate()
      //     .then(() => {
      //       return getDataFn(params, data)
      //         .then((res) => res.data.list)
      //         .catch((err) => {
      //           message.error("数据获取失败！", err.message);
      //           return [];
      //         });
      //     })
      //     .catch((err) => {
      //       // message.error("10",err.message);// 校验参数未通过，不提示错误信息
      //       return [];
      //     });
      // };

      let asyncTask = false;

      if (this.dataType === "file") {
        // 文件
        if (Object.keys(this.file).length === 0) {
          endLoading();
          message.info("请选择文件！");
          return;
        }
        if (this.templateTypes && !this.templateType) {
          endLoading();
          message.info("请选择模板类型！");
          return;
        }
        if (
          shouldShowEncoding({
            fileName: this.file?.name || this.filePath,
          })
        ) {
          const encodingCheck = await assertCsvEncodingMatch(
            this.file,
            this.fileEncoding,
          );
          if (!encodingCheck.ok) {
            endLoading();
            return;
          }
        }
        let formData = new FormData();
        formData.append("file", this.file);
        formData.append("taskID", this.task.id);
        if (
          shouldShowEncoding({
            fileName: this.file?.name || this.filePath,
          })
        ) {
          formData.append("encoding", this.fileEncoding);
        }
        const params = {
          diFileName: this.filediFileName,
          // 兼容 user 还未初始化的情况，避免读取 undefined.department 报错
          departmentType:
            (this.user && this.user.department) ||
            (this.$store?.state?.user?.department ?? ""),
          templateType: this.templateType,
        };
        asyncTask = readZZExcle(params, formData)
          .then((res) => res.data.list)
          .catch((err) => {
            message.error("导入失败！", err.message);
            return [];
          });
      } else if (this.dataType === "ts") {
        // TS
        const params = getCommonParams();
        asyncTask = handleAsyncRequest(
          this.$refs.tsFormRef,
          getTsWords,
          params,
          this.tsFile.tsFileValue,
        );
      } else if (this.dataType === "database") {
        // 实时库：子方法返回 list，由下方统一 handleCommonOperations + revalidate + endLoading
        if (this.dataLibrary.type === "field") {
          // 实时库-对象数据（原 字段）
          asyncTask = this.$refs.fieldFormRef
            .validate()
            .then(() => this.getFieldData())
            .catch((err) => {
              // message.error(err.message);// 校验参数未通过，不提示错误信息
              return [];
            });
          // asyncTask = handleAsyncRequest(
          //   this.$refs.fieldFormRef,
          //   this.getFieldData,
          //   null,
          //   null,
          //   null
          // );
        } else if (this.dataLibrary.type === "alias") {
          // 实时库-元数据
          asyncTask = this.$refs.aliasFormRef
            .validate()
            .then(() => this.getAlias())
            .catch((err) => {
              // message.error("7", err.message);
              return [];
            });
          // asyncTask = handleAsyncRequest(
          //   this.$refs.aliasFormRef,
          //   this.getAlias,
          //   null,
          //   null,
          //   null
          // );
        } else if (this.dataLibrary.type === "allData") {
          // 实时库-全量
          asyncTask = this.$refs.allDataFormRef
            .validate()
            .then(() => this.batchImportDatabase())
            .catch((err) => {
              // message.error("8", err.message);
              return [];
            });
          // asyncTask = handleAsyncRequest(
          //   this.$refs.allDataFormRef,
          //   this.batchImportDatabase,
          //   null,
          //   null,
          //   null
          // );
        }
      } else if (this.dataType === "dictionary") {
        // 辞典
        const params = getCommonParams({
          transType: this.task.translateType,
        });
        asyncTask = handleAsyncRequest(
          this.$refs.dictSelectRef,
          importDictionaryEntry,
          params,
          this.dict.dictionaryType,
        );
      } else if (this.dataType === "config") {
        // 配置文件
        const params = getCommonParams({ diFileName: "" });
        asyncTask = handleAsyncRequest(
          this.$refs.configFormRef,
          getConfigEntry,
          params,
          this.configFile.config,
        );
      } else if (this.dataType === "enum") {
        // 枚举文件
        const params = getCommonParams({ diFileName: "" });
        asyncTask = handleAsyncRequest(
          this.$refs.enumFormRef,
          getEnumEntry,
          params,
          this.enumFile.enum,
        );
      }
      if (asyncTask) {
        asyncTask
          .then((data) => {
            if (
              !this.importGuard.isCurrent(token) ||
              this.task.id !== taskId
            ) {
              return;
            }
            // 各数据源统一在此转为表格行，保证排序、长度提示和回写状态一致。
            handleCommonOperations(data);
          })
          .finally(async () => {
            if (
              this.importGuard.isCurrent(token) &&
              this.task.id === taskId
            ) {
              await revalidateLoaded(
                this,
                this.task.transMap.value,
                () =>
                  this.importGuard.isCurrent(token) &&
                  this.task.id === taskId,
              );
            }
            // 导入成功、失败或被取消都要结束遮罩，允许用户继续选择数据源。
            endLoading();
          });
      } else {
        endLoading();
      }
    },
    sortArray(arr, key) {
      return arr.sort((a, b) => {
        let x = a[key];
        let y = b[key];
        return x > y ? -1 : x < y ? 1 : 0;
      });
    },
    // 获取对象数据（原 字段）内容；返回 list，loading 由 importEntryData 统一管理
    async getFieldData() {
      let table = this.treeData.find(
        (item) => item.id === this.dataLibrary.table,
      );
      let params = {
        dbName: table.db,
        appName: table.app,
        nodeName: table.node,
        tbID: table.tableId,
        tbName: table.title,
        taskID: this.task.id,
        versionID: this.task.versionId ? this.task.versionId : "",
        translateType: this.task.translateType,
        // diFileName: this.dataLibrary.diFileName
        diFileName: "",
        i18nUrl: this.ip,
      };
      let data = [];
      this.dataLibrary.field.forEach((fieldId) => {
        let fieldObj = this.fieldOptions.find((item) => item.value === fieldId);
        let field = {
          fieldName: fieldObj.label,
          fieldID: fieldObj.value,
          size: fieldObj.size,
        };
        data.push(field);
      });
      try {
        const res = await getFieldData(params, data);
        return res.data?.list ?? [];
      } catch (err) {
        message.error("数据获取失败！", err.message);
        return [];
      }
    },
    // 获取别名；返回 list，loading 由 importEntryData 统一管理
    async getAlias() {
      let table = this.treeData.find(
        (item) => item.id === this.dataLibrary.table,
      );
      let params = {
        dbName: table.title,
        appName: table.app,
        nodeName: table.node,
        taskID: this.task.id,
        versionID: this.task.versionId ? this.task.versionId : "",
        translateType: this.task.translateType,
        // diFileName: this.dataLibrary.diFileName,
        diFileName: "",
        maxLength: this.dataLibrary.maxLength,
        i18nUrl: this.ip,
      };
      try {
        const res = await getAlias(params);
        return res.data?.list ?? [];
      } catch (err) {
        message.error("数据获取失败！", err.message);
        return [];
      }
    },
    // 批量导入数据库；返回 list，loading 由 importEntryData 统一管理
    async batchImportDatabase() {
      let selectNode = [];
      this.dataLibrary.tables.forEach((item) => {
        let node = this.treeData.find((d) => d.value === item);
        selectNode.push(node);
      });
      if (selectNode.length === 0) {
        return [];
      }
      let nodes = [];
      let apps = [];
      let dbs = [];
      let tables = [];

      selectNode.forEach((item) => {
        if (item.type === "node") {
          nodes.push(item);
        } else if (item.type === "app") {
          apps.push(item);
        } else if (item.type === "db") {
          dbs.push(item);
        } else if (item.type === "table") {
          tables.push(item);
        }
      });
      let params = {
        taskID: this.task.id,
        versionID: this.task.versionId ? this.task.versionId : "",
        translateType: this.task.translateType,
        // diFileName: this.dataLibrary.diFileName
        diFileName: "",
        i18nUrl: this.ip,
      };
      let mergedList = [];
      // if (nodes.length > 0) {
      //   nodes.forEach((item) => {
      //     item.node = item.value;
      //   });
      //   getDBALLEntryByNode(params, nodes).then((res) => {
      //     this.dataSource = this.dataSource.concat(res.data.list);
      //     this.sortArray(this.dataSource, "isExist");
      //     this.allData = this.dataSource;
      //     // endLoading();
      //     // endLoading();
      //   });
      // }
      // if (apps.length > 0) {
      //   apps.forEach((item) => {
      //     // item.modeType = item.appId
      //     item.app = item.title;
      //   });
      //   getDBALLEntryByApp(params, apps).then((res) => {
      //     this.dataSource = this.dataSource.concat(res.data.list);
      //     this.sortArray(this.dataSource, "isExist");
      //     this.allData = this.dataSource;
      //     // endLoading();
      //   });
      // }
      // if (dbs.length > 0) {
      //   dbs.forEach((item) => {
      //     item.db = item.title;
      //     item.modeType = item.appId;
      //   });
      //   getDBALLEntryByDB(params, dbs).then((res) => {
      //     this.dataSource = this.dataSource.concat(res.data.list);
      //     this.sortArray(this.dataSource, "isExist");
      //     this.allData = this.dataSource;
      //     // endLoading();
      //   });
      // }
      // 定义映射对象
      const typeMap = {
        nodes: {
          list: nodes,
          modifyFn: (item) => {
            item.node = item.value;
          },
          apiFn: getDBALLEntryByNode,
        },
        apps: {
          list: apps,
          modifyFn: (item) => {
            item.app = item.title;
          },
          apiFn: getDBALLEntryByApp,
        },
        dbs: {
          list: dbs,
          modifyFn: (item) => {
            item.db = item.title;
            item.modeType = item.appId;
          },
          apiFn: getDBALLEntryByDB,
        },
      };

      const promises = [];

      // 遍历映射对象
      for (const key in typeMap) {
        const { list, modifyFn, apiFn } = typeMap[key];
        if (list.length > 0) {
          list.forEach(modifyFn);
          promises.push(
            apiFn(params, list).then((res) => {
              mergedList = mergedList.concat(res.data?.list ?? []);
            }),
          );
        }
      }

      await Promise.all(promises);
      return mergedList;
    },
    // 添加表格行点击事件
    customRow(record, index) {
      return {
        onDblclick: async (event) => {
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }
          // 打开编辑态并设置翻译列规则（openSetEdit）；不在此处 applyCell
          await openSetEdit(record, [this.task.transMap.value], this);
          this.showEditOperation(); // 显示编辑操作列
        },
      };
    },
    // 行内 ✓ / 编辑框回车：公共 saveEdit；本页只写非空字段
    async editSave(record) {
      const transCol = this.task.transMap.value;
      await saveEdit(this, record, {
        transCol,
        commit: (rec, row) => {
          for (const [key, value] of Object.entries(row)) {
            if (rec.hasOwnProperty(key) && value != null && value !== "") {
              rec[key] = value;
            }
          }
        },
      });
    },
    // 取消编辑
    cancel(record) {
      cancelEdit(this, record.id);
    },
    // 显示编辑操作列
    showEditOperation() {
      showEditOp(this);
    },
    // 删除操作列
    hideEditOperation() {
      hideEditOp(this);
    },
    // 聚合
    aggregation() {
      if (this.selectedRows.length < 2) {
        message.warn("请选择两条及以上词条聚合！");
      }
      let children = [];
      for (let i = 1; i < this.selectedRows.length; i++) {
        let child = this.selectedRows[i];
        if (child.children && child.children.length > 0) {
          child.children.forEach((item) => {
            children.push(item);
          });
        }
        child.children = [];
        children.push(child);
      }
      children.forEach((item) => {
        item.parentID = this.selectedRows[0].id;

        this.dataSource = this.dataSource.filter((data) => data.id != item.id);
      });
      if (this.selectedRows[0].children) {
        this.selectedRows[0].children =
          this.selectedRows[0].children.concat(children);
      } else {
        this.selectedRows[0].children = children;
      }

      this.allData = this.dataSource;
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    // 取消聚合
    cancelAggregation() {
      // console.log(this.selectedRows)
      this.selectedRows.forEach((item) => {
        if ((item.parentID === "" || item.parentID === null) && item.children) {
          let index = this.dataSource.findIndex(
            (entry) => entry.id === item.id,
          );
          for (let i = 0; i < item.children.length; i++) {
            let child = item.children[i];
            child.parentID = "";
            this.dataSource.splice(index + i + 1, 0, child);
          }
          item.children = [];
        } else {
          let parent = this.dataSource.find(
            (data) => data.id === item.parentID,
          );
          parent.children = parent.children.filter(
            (child) => child.id != item.id,
          );
          let index = this.dataSource.findIndex(
            (data) => data.id === item.parentID,
          );
          item.parentID = "";
          this.dataSource.splice(index + 1, 0, item);
        }
      });
      this.allData = this.dataSource;
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    /**
     * 重置导入弹窗状态并使关闭前的请求失效。
     * @returns {void} 无返回值。
     */
    afterClose() {
      this.queryGuard.invalidate();
      this.importGuard.invalidate();
      resetLoading();
      // 关闭后重置编辑缓存、结果集和文件信息，避免下次打开继承上一次导入会话。
      this.editableData = {};
      this.keyWords = "";
      this.isExist = ALL_ISEXIST;
      this.dataType = "file";
      this.dataSource = [];
      this.allData = [];
      this.tsFile.tsFileValue = [];
      this.file = {};
      this.filePath = "";
      this.fileEncoding = DEFAULT_ENCODING;
      this.dataLibrary = {
        node: null,
        server: null,
        library: null,
        table: [],
        type: "field",
        maxLength: null,
      };
      this.pagination.current = 1;
      this.pagination.pageSize = 20;
      // 关弹窗：Ant 原生清列头筛选 + 搜索态复位
      resetOnClose(this);
    },
    // 创建辞典
    createDictionary() {
      this.createDictVisible = true;
      setModalAriaHidden(this, document);
      this.createDict.name = "";
    },
    createDictOk() {
      this.getDictionary();
    },
    createDictClose() {
      this.createDictVisible = false;
    },
    // 列头自定义筛选 + 保存 Ant clearFilters
    handleSearch(selectedKeys, confirm, dataIndex, clearFilters) {
      handleFilterSearch(selectedKeys, confirm, dataIndex, clearFilters, this);
    },
    // 列筛选重置
    handleReset(clearFilters) {
      tableHandleReset(clearFilters, this);
    },
    // 动态设置表格高度
    setTableHeight(height, type) {
      if (type === "full") {
        this.tableHeight.y = height - 370;
      } else if (type === "reduce") {
        this.tableHeight.y = "300px";
      }
    },
    // 模板下载
    templateFileDownload() {
      this.templateVisible = true;
      setModalAriaHidden(this, document);
    },
    // 模板下载
    templateDownload() {
      startLoading();
      this.$refs.dictRef
        .validate()
        .then(() => {
          let params = {
            fileType: this.templateObj.type,
            translateType: this.task.translateType,
            // exportFields: this.exportFields,
            exportType: this.templateObj.exportType,
          };
          return templateFileDownload(params).then((res) => {
            let fileName = res.headers["content-disposition"]
              .split(";")[1]
              .split("filename=")[1];
            let contentType = res.headers["content-type"];
            const blob = new Blob([res.data], { type: contentType });
            const a = document.createElement("a");
            a.download = decodeURI(fileName);
            a.href = window.URL.createObjectURL(blob);
            a.click();
            a.remove();
          });
        })
        .finally(() => {
          endLoading();
        });
    },
    templateClose() {
      this.templateVisible = false;
      if (this.$currentDepartment) {
        this.templateObj.type = this.$currentDepartment.value;
      }
      if (this.templateObj.type === "default") this.templateObj.type = null; // 如果是默认部门，则不设置模板类型，否则会报错
      this.templateObj.exportType = null;
    },
    // 分页 + 当前页校验（见 composables/page）
    pageChange(page, pageSize) {
      wbPageChange(this, page, pageSize, () => this.task.transMap.value);
    },
    // 语种切换
    /**
     * 按语种重新筛选导入结果并清空旧范围选择。
     * @returns {void} 无返回值。
     */
    filterLanguageChange() {
      this.queryGuard.invalidate();
      clearTableSelection(this);
      applyLanguageFilter(this);
    },
    // 同步 filters / filteredValue（供全部选择）
    /**
     * 应用导入表格筛选并清空已变化范围的选择。
     * @param {Object} pagination - 表格分页状态。
     * @param {Object} filters - 表格列筛选值。
     * @returns {void} 无返回值。
     */
    handleTableChange(pagination, filters) {
      // 筛选条件改变即改变“全选”范围，先失效旧选择避免跨范围保存。
      invalidateSelectionWhenFiltersChange(this, filters);
      this.filters = filters;
      // 回写 Ant 受控筛选值，确保列头筛选图标与当前数据范围同步。
      for (let key in filters) {
        this.columns.forEach((col) => {
          if (col.dataIndex === key) {
            col.filteredValue = filters[key];
          }
        });
      }
      // 以下 filteredData 计算为历史遗留（import 并集）；均未被消费。保留便于对照；确认无回归后可删除。
      // let isExistData = this.dataSource.filter((item) => {
      //   return filters.isExist && filters.isExist.includes(item.isExist);
      // });
      // let sourceData = this.dataSource.filter((item) => {
      //   return (
      //     filters.entrySource && item.entrySource.includes(filters.entrySource)
      //   );
      // });
      // this.filteredData = Array.from(new Set([...isExistData, ...sourceData]));
    },
    // 获取i18服务器ip
    getIPs() {
      if (
        this.$currentDepartment &&
        this.$currentDepartment.ops.has("needIP")
      ) {
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

          // this.getDictionary()
        });
      }
    },
    // ip change事件
    ipChange(value) {
      this.dataTypeChange();
    },

    // 搜索框内容变化时更新 searchDicValue
    onDicSearch(value) {
      this.searchDicValue = value;
    },
    // 选中项变化时，不清空搜索框内容
    onDicChange(value) {
      // console.log('Selected value:', value);
      // 这里可以根据需要进行额外的操作，但不会清空搜索内容
    },
    onTSSearch(value) {
      this.searchTSValue = value;
    },
    onDicBlur() {
      this.searchDicValue = "";
    },
    // 选中项变化时，不清空搜索框内容
    onTSChange(value) {
      // console.log('Selected value:', value);
      // 这里可以根据需要进行额外的操作，但不会清空搜索内容
    },
    onTSBlur() {
      this.searchTSValue = "";
    },
    // 获取配置文件列表
    getConfigList() {
      let param = {
        i18nUrl: this.ip,
      };
      getConfigList(param).then((res) => {
        this.configFile.configOptions = [];
        if (res.data) {
          res.data.forEach((item) => {
            let config = {
              value: item,
              label: item,
            };
            this.configFile.configOptions.push(config);
          });
        }
      });
    },
    // 获取枚举文件列表
    getEnumList() {
      let param = {
        i18nUrl: this.ip,
      };
      getEnumList(param).then((res) => {
        this.enumFile.enumOptions = [];
        if (res.data) {
          res.data.forEach((item) => {
            let enumItem = {
              value: item,
              label: item,
            };
            this.enumFile.enumOptions.push(enumItem);
          });
        }
      });
    },
    // 全选config文件
    selectAllConfig() {
      this.configFile.config = [];
      this.configFile.configOptions.forEach((item) => {
        if (item.value.includes(this.searchConfigValue)) {
          this.configFile.config.push(item.value);
        }
      });
    },
    // 清空选中的ts文件
    clearAllConfig() {
      this.configFile.config = [];
    },
    onConfigSearch(value) {
      this.searchConfigValue = value;
    },
    // 选中项变化时，不清空搜索框内容
    onConfigChange(value) {
      // console.log('Selected value:', value);
      // 这里可以根据需要进行额外的操作，但不会清空搜索内容
    },
    onConfigBlur() {
      this.searchConfigValue = "";
    },
    // 全选Enum文件
    selectAllEnum() {
      this.enumFile.enum = [];
      this.enumFile.enumOptions.forEach((item) => {
        if (item.value.includes(this.searchEnumValue)) {
          this.enumFile.enum.push(item.value);
        }
      });
    },
    // 清空选中的ts文件
    clearAllEnum() {
      this.enumFile.enum = [];
    },
    onEnumSearch(value) {
      this.searchEnumValue = value;
    },
    // 选中项变化时，不清空搜索框内容
    onEnumChange(value) {
      // console.log('Selected value:', value);
      // 这里可以根据需要进行额外的操作，但不会清空搜索内容
    },
    onEnumBlur() {
      this.searchEnumValue = "";
    },
  },
};
</script>
<style scoped lang="less">
.modalContent .templateForm form div {
  margin-bottom: 10px !important;
}
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
  align-items: stretch;
  gap: 16px;
  align-self: stretch;

  :deep(.ant-table-wrapper) {
    width: 100%;
    min-width: 0;
  }

  .platformBox {
    width: 100%;
    box-sizing: border-box;
    background-color: #fff;
    padding: 12px 16px 16px;
    border-radius: 4px;

    :deep(.ant-form-item) {
      margin-bottom: 0;
    }
    :deep(.ant-row) {
      height: auto;
      min-height: 0;
      margin-bottom: 0;
      row-gap: 12px;
      align-items: center;
    }

    /* 方案 B：两段语义行，组内 flex-wrap */
    .import-form-row {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px 16px;
      width: 100%;
    }
    .import-form-row--source {
      margin-bottom: 8px;
    }
    .import-form-row--file {
      margin-bottom: 0;
    }
    .import-flow-item {
      margin: 0;
    }
    .import-flow-item--action {
      margin-left: auto;
    }
    .file-import-item {
      margin-bottom: 0;
    }
    .file-import-controls {
      display: inline-flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
    }
    .template-download-link {
      font-size: 12px;
      white-space: nowrap;
      flex-shrink: 0;
    }
  }

  .dataTypeBox {
    width: 100%;
    background-color: white;
    :deep(.ant-tabs-nav) {
      margin-bottom: 10px;
    }
  }
  .dataTypeBox2 {
    display: flex;
    width: 100%;
    background-color: white;
    :deep(.ant-tabs-nav) {
      margin-bottom: 10px;
    }
  }
  .import-table-filters {
    width: auto;
    display: flex;
    align-items: center;
  }
}
:deep(.ant-pagination) {
  margin: 8px 0;
}
</style>
