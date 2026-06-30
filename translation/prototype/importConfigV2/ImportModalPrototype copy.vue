<template>
  <CustomModal :visible="visible" :modalTitle="modalTitle" :modalWidth="modalWidth" :showCancel="false"
    :okLoading="saveLoading" :fullFlag="true" okText="保存" @handleClose="handleClose" @handleOK="handleOK"
    @afterClose="afterClose" @setTableHeight="setTableHeight">
    <div class="content">
      <div class="taskInfo">
        <div class="taskItem">任务名称：{{ task.name }}</div>
        <div class="taskItem">产品名称：{{ task.productName }}</div>
        <div class="taskItem">上级分类名称：{{ task.classifyName }}</div>
        <div class="taskItem">翻译语种：{{ task.translateType }}</div>
        <RulesDropdown :options="rulesOptions" @update:options="rulesOptions"></RulesDropdown>
      </div>
      <div class="platformBox">
        <div style="width:100%;">
          <a-form layout="inline" style="margin-top: 10px;">
            <a-form-item label="数据类型">
              <a-radio-group v-model:value="dataType" @change="dataTypeChange">
                <a-radio v-if="$currentDepartment && $currentDepartment.importTypes.includes('file')"
                  :value="'file'">文件</a-radio>
                <a-radio v-if="$currentDepartment && $currentDepartment.importTypes.includes('ts')"
                  :value="'ts'">TS</a-radio>
                <a-radio v-if="$currentDepartment && $currentDepartment.importTypes.includes('database')"
                  :value="'database'">实时库</a-radio>
                <a-radio v-if="$currentDepartment && $currentDepartment.importTypes.includes('dictionary')"
                  :value="'dictionary'">辞典</a-radio>
                <a-radio v-if="$currentDepartment && $currentDepartment.importTypes.includes('config')"
                  :value="'config'">配置文件</a-radio>
                <a-radio v-if="$currentDepartment && $currentDepartment.importTypes.includes('enum')"
                  :value="'enum'">枚举文件</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item v-if="$currentDepartment && $currentDepartment.ops.has('needIP')" label="IP">
              <a-select v-model:value="ip" :options="ips" @change="ipChange" style="width:250px" placeholder="请选择IP"
                allowClear></a-select>
            </a-form-item>
          </a-form>
        </div>
        <div class="dataTypeBox" v-if="dataType === 'file'" ref="fileRef">
          <a-row :gutter="24">
            <a-col :span="16">
              <a-form-item label="词条文件" name="filefilename">
                <a-input v-model:value="filePath" style="width:70%" size="small" placeholder="请选择词条文件" />
                <a-upload name="file" :beforeUpload="beforeUpload" :accept="accept" :showUploadList="false"
                  @change="handleChange">
                  <a-button type="primary" size="small" style="margin-left:8px">选择文件</a-button>
                </a-upload>
                <a style="font-size:12px;margin-left:10px" @click="templateFileDownload">下载模板</a>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-row type="flex" align="middle" justify="space-between">
                <a-col :flex="1">
                  <a-form-item v-if="$currentDepartment && $currentDepartment.ops.has('needIP')" label="回写辞典"
                    name="diFileName">
                    <a-select v-model:value="filediFileName" allowClear placeholder="请选择回写辞典目录" style="width:70%"
                      :options="dictionaryOptions" size="small" show-search
                      :filter-option="(input, option) => option.label.toLowerCase().includes(input.toLowerCase())">
                    </a-select>
                    <a-tooltip placement="top">
                      <template #title>
                        <span>添加辞典</span>
                      </template>
                      <PlusSquareOutlined @click="createDictionary" style="color:#369FFF;margin-left:8px" />
                    </a-tooltip>
                  </a-form-item>
                  <a-form-item v-if="templateTypes" label="选择模板" name="templateType">
                    <a-select v-model:value="templateType" allowClear placeholder="请选择模板类型" style="width:70%"
                      size="small" :options="templateTypes">
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :span="2">
                  <a-form-item>
                    <a-button type="primary" ghost size="small" :loading="importBtnLoading" style="float:right"
                      @click="importEntryData">导入</a-button>
                  </a-form-item>
                </a-col>
              </a-row>
            </a-col>
          </a-row>
        </div>
        <div class="dataTypeBox" v-if="dataType === 'ts'">
          <a-form ref="tsFormRef" :model="tsFile" style="width:100%">
            <a-form-item :label="selectTitle" name="tsFileValue" :rules="[{ required: true, message: '请选择ts文件!' }]">
              <a-select v-model:value="tsFile.tsFileValue" v-model:searchValue="searchTSValue" mode="multiple"
                :max-tag-count="4" allowClear style="width: 70%;margin-left:10px" placeholder="请选择" size="small"
                :options="tsOptions" @search="onTSSearch" @change="onTSChange" @blur="onTSBlur">
                <template #dropdownRender="{ menuNode: menu }">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0" />
                  <div style="padding: 4px 8px; cursor: pointer;" @mousedown="e => e.preventDefault()">
                    <a-button type="link" @click="selectAllTs">全选</a-button>
                    <a-button type="link" @click="clearAllTs">清空</a-button>
                  </div>
                </template>
              </a-select>
              <a-button type="primary" ghost size="small" style="float:right" :loading="importBtnLoading"
                @click="importEntryData">导入</a-button>
            </a-form-item>
          </a-form>
        </div>
        <div class="dataTypeBox" v-if="dataType === 'dictionary'" ref="dicRef">
          <a-form ref="dictSelectRef" name="advanced_search" class="ant-advanced-search-form" :model="dict"
            style="width:100%">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="辞典" name="dictionaryType" :rules="[{ required: true, message: '请选择辞典!' }]">
                  <a-tree-select v-model:value="dict.dictionaryType" v-model:searchValue="searchDicValue" show-search
                    tree-checkable style="width: 100%" :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
                    placeholder="请选择" allow-clear multiple :tree-data="notEffectiveDicts" :max-tag-count="2"
                    size="small" tree-node-filter-prop="label" @search="onDicSearch" @change="onDicChange"
                    @blur="onDicBlur">
                  </a-tree-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-button type="primary" ghost size="small" style="float:right" :loading="importBtnLoading"
                  @click="importEntryData">导入</a-button>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <div class="dataTypeBox" v-if="dataType === 'database'" style="padding-top:0px" ref="dataSourceRef">
          <a-tabs v-model:activeKey="dataLibrary.type" size="small" style="width:100%" @change="changeDataLibraryType">
            <a-tab-pane key="field" tab="对象数据"></a-tab-pane>
            <a-tab-pane key="alias" tab="元数据"></a-tab-pane>
            <a-tab-pane key="allData" tab="全量"></a-tab-pane>
          </a-tabs>
          <a-form ref="fieldFormRef" name="advanced_search" class="ant-advanced-search-form" :model="dataLibrary"
            style="width:100%" v-if="dataLibrary.type === 'field'">
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="数据库" name="table" :rules="[{ required: true, message: '请选择数据库!' }]">
                  <a-tree-select v-model:value="dataLibrary.table" v-model:searchValue="searchValue" allowClear
                    tree-data-simple-mode show-search style="width: 100%"
                    :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }" :tree-data="treeData" placeholder="请选择表"
                    :load-data="onLoadData" :show-checked-strategy="SHOW_PARENT" @select="treeSelect"
                    tree-node-filter-prop="title" size="small">
                    <template #title="{ title }">
                      <template v-for="(fragment, i) in title
                        .toString()
                        .split(new RegExp(`(?<=${searchValue})|(?=${searchValue})`, 'i'))">
                        <span v-if="fragment.toLowerCase() === searchValue.toLowerCase()" :key="i" style="color: #08c">
                          {{ fragment }}
                        </span>
                        <template v-else>{{ fragment }}</template>
                      </template>
                    </template>
                  </a-tree-select>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="字段" name="field" :rules="[{ required: true, message: '请选择字段!' }]">
                  <a-select v-model:value="dataLibrary.field" mode="multiple" allowClear placeholder="请选择字段"
                    :options="fieldOptions" :max-tag-count="3" size="small">
                    <template #dropdownRender="{ menuNode: menu }">
                      <v-nodes :vnodes="menu" />
                      <a-divider style="margin: 4px 0" />
                      <div style="padding: 4px 8px; cursor: pointer;" @mousedown="e => e.preventDefault()">
                        <a-button type="link" @click="selectAllField">全选</a-button>
                        <a-button type="link" @click="clearAllField">清空</a-button>
                      </div>
                    </template>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-button type="primary" ghost size="small" style="float:right" :loading="importBtnLoading"
                  @click="importEntryData">导入</a-button>
              </a-col>
            </a-row>
          </a-form>
          <a-form ref="aliasFormRef" name="advanced_search" class="ant-advanced-search-form" :model="dataLibrary"
            style="width:100%" v-if="dataLibrary.type === 'alias'">
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="数据库" name="table" :rules="[{ required: true, message: '请选择库!' }]">
                  <a-tree-select v-model:value="dataLibrary.table" v-model:searchValue="searchValue"
                    tree-data-simple-mode allowClear show-search style="width: 100%"
                    :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }" :tree-data="treeData" placeholder="请选择库"
                    :load-data="onLoadData" @select="treeSelect" tree-node-filter-prop="title" size="small">
                    <template #title="{ title }">
                      <template v-for="(fragment, i) in title
                        .toString()
                        .split(new RegExp(`(?<=${searchValue})|(?=${searchValue})`, 'i'))">
                        <span v-if="fragment.toLowerCase() === searchValue.toLowerCase()" :key="i" style="color: #08c">
                          {{ fragment }}
                        </span>
                        <template v-else>{{ fragment }}</template>
                      </template>
                    </template>
                  </a-tree-select>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="限制长度">
                  <a-input-number v-model:value="dataLibrary.maxLength" style="width:100%" size="small"
                    placeholder="请输入限制长度" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-button type="primary" ghost size="small" style="float:right" :loading="importBtnLoading"
                  @click="importEntryData">导入</a-button>
              </a-col>
            </a-row>
          </a-form>
          <a-form ref="allDataFormRef" name="advanced_search" class="ant-advanced-search-form" :model="dataLibrary"
            style="width:100%" v-if="dataLibrary.type === 'allData'">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="数据库" name="tables" :rules="[{ required: true, message: '请选择!' }]">
                  <a-tree-select v-model:value="dataLibrary.tables" v-model:searchValue="searchValue" show-search
                    allowClear tree-data-simple-mode style="width: 100%"
                    :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }" :tree-data="treeData" placeholder="请选择"
                    :load-data="onLoadData" :maxTagCount="3" tree-checkable :show-checked-strategy="SHOW_PARENT"
                    @select="treeBatchSelect" tree-node-filter-prop="title" size="small">
                    <template #title="{ title }">
                      <template v-for="(fragment, i) in title
                        .toString()
                        .split(new RegExp(`(?<=${searchValue})|(?=${searchValue})`, 'i'))">
                        <span v-if="fragment.toLowerCase() === searchValue.toLowerCase()" :key="i" style="color: #08c">
                          {{ fragment }}
                        </span>
                        <template v-else>{{ fragment }}</template>
                      </template>
                    </template>
                  </a-tree-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-button type="primary" ghost size="small" style="float:right" :loading="importBtnLoading"
                  @click="importEntryData">导入</a-button>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <div class="dataTypeBox2 config-box" v-if="dataType === 'config'" ref="configRef">
          <a-tabs v-model:activeKey="configType" size="small" style="width:100%">
            <a-tab-pane key="xml" tab="XML" />
            <a-tab-pane key="json" tab="JSON" />
          </a-tabs>
          <a-form ref="configFormRef" name="advanced_search" class="ant-advanced-search-form" :model="activeConfigModel"
            style="width:100%">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="配置文件" name="config" :rules="[{ required: true, message: '请选择配置文件!' }]">
                  <!-- XML：保留多选 + 全选/清空 -->
                  <a-select v-if="configType === 'xml'" v-model:value="activeConfigModel.config"
                    v-model:searchValue="activeSearchConfigValue" mode="multiple" :max-tag-count="3" allowClear
                    placeholder="请选择配置文件" :options="activeConfigModel.configOptions" style="width:100%" size="small"
                    @search="onConfigSearch" @change="onConfigChange" @blur="onConfigBlur">
                    <template #dropdownRender="{ menuNode: menu }">
                      <v-nodes :vnodes="menu" />
                      <a-divider style="margin: 4px 0" />
                      <div style="padding: 4px 8px; cursor: pointer;" @mousedown="e => e.preventDefault()">
                        <a-button type="link" @click="selectAllConfig">全选</a-button>
                        <a-button type="link" @click="clearAllConfig">清空</a-button>
                      </div>
                    </template>
                  </a-select>
                  <!-- JSON：改为单选，移除全选/清空 -->
                  <a-select v-else v-model:value="activeConfigModel.config"
                    v-model:searchValue="activeSearchConfigValue" allowClear placeholder="请选择配置文件"
                    :options="activeConfigModel.configOptions" style="width:100%" size="small" @search="onConfigSearch"
                    @change="onConfigChange" @blur="onConfigBlur" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-button type="primary" ghost size="small" style="float:right" :loading="importBtnLoading"
                  @click="importEntryData">
                  导入
                </a-button>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <div class="dataTypeBox2" v-if="dataType === 'enum'" ref="enumRef">
          <a-form ref="enumFormRef" name="advanced_search" class="ant-advanced-search-form" :model="enumFile"
            style="width:100%">
            <a-form-item label="枚举文件" name="enum" :rules="[{ required: true, message: '请选择枚举文件!' }]">
              <a-select v-model:value="enumFile.enum" v-model:searchValue="searchEnumValue" mode="multiple"
                :max-tag-count="3" allowClear placeholder="请选择枚举文件" :options="enumFile.enumOptions" style="width:50%"
                size="small" @search="onEnumSearch" @change="onEnumChange" @blur="onEnumBlur">
                <template #dropdownRender="{ menuNode: menu }">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0" />
                  <div style="padding: 4px 8px; cursor: pointer;" @mousedown="e => e.preventDefault()">
                    <a-button type="link" @click="selectAllEnum">全选</a-button>
                    <a-button type="link" @click="clearAllEnum">清空</a-button>
                  </div>
                </template>
              </a-select>
            </a-form-item>
          </a-form>
          <a-button type="primary" ghost size="small" style="margin-left:auto" :loading="importBtnLoading"
            @click="importEntryData">导入</a-button>
        </div>
      </div>

      <div class="form">
        <div style="width: auto; display: flex; align-items: center;justify-content: center;">
          <a-form layout="inline" autocomplete="off" style="display: flex;gap: 8px;">
            <a-form-item label="词条" name="entry" style="width: auto;height: auto; margin: 0;">
              <a-input v-model:value="keyWords" size="small" placeholder='词条' style="width: 100px;" />
            </a-form-item>
            <a-form-item label="存在状态" name="isExist" style="width: auto;height: auto; margin: 0;">
              <a-select v-model:value="isExist" style="width: 120px" placeholder="请选择存在状态" :options="isExistOptions"
                allowClear size="small" />
            </a-form-item>
          </a-form>
        </div>

        <!-- 词条：
        <a-input v-model:value="keyWords" style="width:30%" size="small" placeholder='请输入词条搜索' /> -->
        <a-button type="primary" size="small" style="margin-left:8px" :loading="loading" @click="select">
          <template #icon>
            <SearchOutlined />
          </template>查询
        </a-button>
        <a-button type="primary" danger size="small" style="margin-left:8px" @click="deleteEntry">
          <template #icon>
            <DeleteOutlined />
          </template>删除
        </a-button>
        <span style="margin-left:10px">过滤语种：</span>
        <a-radio-group v-model:value="filterLanguage" name="radioGroup" @change="filterLanguageChange">
          <a-radio value="全部">全部</a-radio>
          <a-radio value="中文">中文</a-radio>
          <a-radio value="英文">英文</a-radio>
        </a-radio-group>
        <div style="margin-left:auto">
          <CoverButton :translate="task.translateType" :dataSource="dataSource" :oldEditableData="editableData"
            @update:oldEditableData="editableData = $event" @showEditOperation="showEditOperation" size="small"
            buttonTitle="释义覆盖翻译" />
          <!-- <a-button type="primary" size="small" style="margin-left:8px" @click="interpretation2value">释义覆盖翻译</a-button> -->
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
            <a-button type="primary" size="small" style="margin-left:8px"><template #icon>
                <SettingOutlined />
              </template>展示列</a-button>
          </a-popover>
        </div>
      </div>
      <a-table bordered class="ant-table-striped" :columns="columns" :data-source="dataSource"
        :row-key="record => record.id" :scroll="tableHeight" :pagination='pagination' :loading="loading"
        :rowClassName="getRowClassName" :customRow="customRow" :expandIconColumnIndex="2" :row-selection="{
          selectedRowKeys: selectedRowKeys,
          onChange: onSelectChange,
          checkStrictly: false,
          selections: [
            { key: 'selectAll', text: '全部选择', onSelect: selectAllEntry },
            { key: 'clearAll', text: '取消选择', onSelect: clearAllEntry }
          ]
        }" ref="workTable" @resizeColumn="handleResizeColumn" @change="handleTableChange">
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'entry'">
            <span v-text="text ? text.replace(/\n/g, '\\n') : text"></span>
          </template>
          <template v-if="editList_needValidate.includes(column.dataIndex)">
            <div>
              <template v-if="editableData[record.id]">
                <a-form :model="editableData[record.id]" :rules="rules[record.id]"
                  :ref="'form' + record.id.replaceAll('-', '') + column.dataIndex" autocomplete="off">
                  <a-form-item :name="column.dataIndex">
                    <a-input v-model:value="editableData[record.id][column.dataIndex]"
                      style="margin: -5px 0;margin-top:10px" @pressEnter="editSave(record)" />
                  </a-form-item>
                </a-form>
              </template>
              <template v-else>
                {{ text }}
              </template>
            </div>
          </template>
          <template v-if="editList.includes(column.dataIndex)">
            <div>
              <template v-if="editableData[record.id]">
                <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0"
                  @pressEnter="editSave(record)" />
              </template>
              <template v-else>
                {{ text }}
              </template>
            </div>
          </template>
          <template v-if="column.dataIndex === 'tag'">
            <div>
              <template v-if="editableData[record.id]">
                <a-input v-model:value="editableData[record.id][column.dataIndex]" style="margin: -5px 0;width:90%"
                  @pressEnter="editSave(record)" />
                <a-tooltip placement="top">
                  <template #title>
                    <span>多个Tag按分号分割！</span>
                  </template>
                  <InfoCircleOutlined style="margin-left:3px" />
                </a-tooltip>
              </template>
              <template v-else>
                <span>
                  <a-tag v-for="(tag, index) in companyCut(text)" :key="index" color="cyan" class="tag-content">
                    {{ tag }}
                  </a-tag>
                </span>
              </template>
            </div>
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
          <!-- <template v-else-if="column.dataIndex === 'label'">
                        <div class="editable-row-operations">
                            <span>
                                <a-checkable-tag :checked="record.auditState === 1" :class="record.auditState === 1 ? 'passTagChecked' : 'passTag' " >通过</a-checkable-tag>
                                <a-checkable-tag :checked="record.auditState === 0" :class="record.auditState === 0 ? 'rejectTagChecked' : 'rejectTag'" >驳回</a-checkable-tag>
                            </span>
                        </div>
                    </template> -->
          <template v-else-if="column.dataIndex === 'editOperation'">
            <div class="editable-row-operations">
              <span v-if="editableData[record.id]">
                <a-tooltip placement="top">
                  <template #title>
                    <span>保存</span>
                  </template>
                  <CheckOutlined style="color:#369FFF;margin-left:8px" @click="editSave(record)" />
                </a-tooltip>
                <a-tooltip placement="top">
                  <template #title>
                    <span>取消</span>
                  </template>
                  <CloseOutlined style="color:red;margin-left:8px" @click="cancel(record)" />
                </a-tooltip>
              </span>
            </div>
          </template>
        </template>
        <template #expandIcon="props">
          <span v-if="props.record.children != null && props.record.children.length > 0">
            <div v-if="props.expanded" style="display: inline-block; margin-right: 10px"
              @click="(e) => { props.onExpand(props.record, e); }">
              <CaretDownOutlined />
            </div>
            <div v-else style="display: inline-block; margin-right: 10px"
              @click="(e) => { props.onExpand(props.record, e); }">
              <CaretRightOutlined />
            </div>
          </span>
          <span v-else style="margin-right:23px"></span>
        </template>
        <!-- 设置筛选菜单 -->
        <template #customFilterDropdown="{ setSelectedKeys, selectedKeys, confirm, clearFilters, column }">
          <div style="padding: 8px">
            <a-input ref="searchInput" :placeholder="`搜索 ${column.title}`" :value="selectedKeys[0]"
              style="width: 188px; margin-bottom: 8px; display: block"
              @change="e => setSelectedKeys(e.target.value ? [e.target.value] : [])"
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
      <a-button type="primary" size="small" style="margin-left:8px;float:left" class="resetBtn"
        @click="aggregation">聚合</a-button>
      <a-button type="primary" size="small" style="margin-left:8px;float:left" class="yellowBtn"
        @click="cancelAggregation">取消聚合</a-button>
    </template>
  </CustomModal>
  <!-- JSON 配置文件导入：编辑 json 导入词条 -->
  <CustomModal :visible="jsonEditModalVisible" modalTitle="编辑json导入词条" :modalWidth="'80%'" :showCancel="false"
    okText="关闭" @handleClose="closeJsonEditModal" @handleOK="closeJsonEditModal">
    <div class="json-edit-modal">
      <!-- 左侧：导入json -->
      <div class="json-pane">
        <div class="json-pane-header">
          <span class="json-pane-title">导入json</span>
          <a-button size="small" type="primary" @click="handleJsonNeedTranslate">
            需要翻译
          </a-button>
        </div>
        <div class="json-pane-body">
          <a-typography-paragraph type="secondary" class="json-pane-tip">
            左侧展示从配置文件中读取的原始 JSON 内容（原型：本地 Mock 数据，可在后续接入真实接口）。
          </a-typography-paragraph>
          <div class="json-view-container">
            <!-- 左列：行号 + 勾选（冻结，纵向滚动与右列同步） -->
            <div
              ref="jsonLineNumbersRef"
              class="json-line-numbers"
              @scroll="handleLeftScroll"
            >
              <a-checkbox-group v-model:value="selectedLineIndices" class="json-checkbox-group">
                <div
                  v-for="(line, index) in jsonLines"
                  :key="index"
                  class="json-line-number-row"
                  :class="{
                    'json-line-number-row--selected': selectedLineIndices.includes(index + 1),
                    'json-line-number-row--translatable': jsonLineMeta[index]?.isTranslatable
                  }"
                >
                  <span class="json-line-index">
                    {{ String(index + 1).padStart(3, '0') }}
                  </span>
                  <a-tooltip
                    v-if="jsonLineMeta[index]?.isTranslatable"
                    :title="`${jsonLineMeta[index].key}: ${jsonLineMeta[index].value}`"
                  >
                    <a-checkbox
                      :value="index + 1"
                      :disabled="false"
                    />
                  </a-tooltip>
                  <a-tooltip
                    v-else
                    title="当前行不是字符串值，无法作为翻译词条"
                  >
                    <a-checkbox
                      :value="index + 1"
                      :disabled="true"
                    />
                  </a-tooltip>
                </div>
              </a-checkbox-group>
            </div>
            <!-- 右列：完整 JSON 代码块（横向+纵向滚动，纵向与左列同步） -->
            <div
              ref="jsonCodeScrollWrapperRef"
              class="json-code-scroll-wrapper"
              @scroll="handleRightScroll"
            >
              <pre class="json-code-block" v-html="highlightedJsonContent"></pre>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：抽取json -->
      <div class="json-pane">
        <div class="json-pane-header">
          <span class="json-pane-title">抽取json</span>
          <div class="json-pane-actions">
            <a-button v-if="!jsonExtractEditMode" size="small" @click="enterJsonExtractEdit">
              编辑
            </a-button>
            <a-button v-if="jsonExtractEditMode" size="small" type="primary" ghost @click="saveJsonExtract">
              保存
            </a-button>
            <a-button size="small" type="primary" :loading="jsonImportLoading" @click="handleJsonImportConfirm">
              导入
            </a-button>
          </div>
        </div>
        <div class="json-pane-body">
          <a-typography-paragraph type="secondary" class="json-pane-tip">
            右侧为待导入的抽取 JSON；当点击左侧「需要翻译」时，右侧会模拟加载并补全对应词条。
          </a-typography-paragraph>
          <a-spin :spinning="jsonSyncingToExtract">
            <div class="json-code-wrapper">
              <a-textarea v-if="jsonExtractEditMode" v-model:value="jsonExtractContent"
                :auto-size="{ minRows: 10, maxRows: 18 }" class="json-code-editor" />
              <pre v-else class="json-code-block">{{ jsonExtractContent }}</pre>
            </div>
          </a-spin>
        </div>
      </div>
    </div>
  </CustomModal>
  <Dict :visible="createDictVisible" @modalClose="createDictClose" @modalOK="createDictOk" />
  <CustomModal :visible="templateVisible" modalTitle="模板下载" @handleOK="templateDownload" @handleClose="templateClose"
    :okLoading="templateLoading">
    <div class="condent templateForm">
      <a-form ref="dictRef" name="advanced_search" class="ant-advanced-search-form" :model="templateObj"
        style="width:100%">
        <a-form-item label="模板类型" name="type" :rules="[{ required: true, message: '请选择模板类型!' }]">
          <a-select v-model:value="templateObj.type" placeholder="请选择" :options='departmentList' allowClear>
          </a-select>
        </a-form-item>
        <a-form-item label="文件类型" name="exportType" :rules="[{ required: true, message: '请选择文件类型!' }]">
          <a-select v-model:value="templateObj.exportType" placeholder="请选择文件类型" :options='exportTypes' allowClear>
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
  CaretDownOutlined,
  CaretRightOutlined,
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
  filterSourceLanguage,
  getI18nAdress,
} from "@/http/api/workbench";
import { templateFileDownload } from "@/http/api/download";
import commonParam, {
  entryParams,
  workbenchParams,
} from "@/constants/commonParam.js";
import {
  onSelectChange,
  onSelect,
  onSelectAll,
  pageChange,
} from "@/utils/selectionUtils";
import {
  verifyArray_workbench_page,
  getMaxLength,
  byteLength,
  verifyArray_workbench,
  openSetEdit,
  useRefRules,
} from "@/utils/validationUtils";
import { interpretation2value } from "@/utils/translationUtils";
import { getColPref, changeColumn } from "@/components/ColumnFilter";
import { setModalAriaHidden } from "@/utils/domUtils";
import { filter_arr, filter_arr_keys } from "@/utils/dataStructureUtils";
import { handleAsyncRequest } from "@/utils/requestUtils";
const filteredInfo = {};
const ALL_ISEXIST = "-1";

// ==================== 配置文件 JSON 导入 Mock 数据 ====================
// Mock 内容来源：f:\DownLoads\gateway.json
const MOCK_JSON_IMPORT_CONTENT = `{
  "history_ips": {
    "last_used": "172.16.0.30",
    "list": [
      "172.16.0.33",
      "172.16.0.34",
      "172.16.0.35",
      "172.16.0.30"
    ]
  },
  "sever_package_info_linux": {
    "file_name": "命名无要求，file_path有且只能存在一个.syzip文件",
    "file_path": "tools/linx99"
  },
  "sever_package_info_windows": {
    "file_name": "命名无要求，file_path有且只能存在一个.syzip文件",
    "file_path": "tools/windows"
  },
  "toolBox_info": {
    "time": "2025-10-30",
    "version": "1.0.0"
  },
  "tools": [
    {
      "cmd_str": "--log console,file",
      "description": "网关机配置工具",
      "icon": "run/res/icon/logo64/enumerate_max.png",
      "name": "gui_app_config",
      "offline": true,
      "online": true
    },
    {
      "cmd_str": "--log console,file",
      "description": "网关机监视工具",
      "icon": "run/res/icon/logo64/operation_and_maintenance_analysis_max.png",
      "name": "gui_app_monitor",
      "offline": true,
      "online": true
    },
    {
      "cmd_str": "--log console,file",
      "description": "基础配置工具",
      "icon": "run/res/icon/logo32/pdc.png",
      "name": "gui_smart_setup_tool",
      "offline": false,
      "online": true
    },
    {
      "cmd_str": "--log console,file --login remote",
      "description": "系统备份工具",
      "icon": "run/res/icon/logo64/backup_logo_max.png",
      "name": "gui_ops_backup",
      "offline": false,
      "online": true
    },
    {
      "cmd_str": "--log console,file --login remote",
      "description": "系统还原工具",
      "icon": "run/res/icon/logo64/recover_logo_max.png",
      "name": "gui_ops_recover",
      "offline": true,
      "online": true
    },
    {
      "cmd_str": "--log console,file --login remote",
      "description": "系统管理界面",
      "icon": "run/res/icon/logo64/manager.ico",
      "name": "gui_manager",
      "offline": false,
      "online": true
    }
  ]
}`;

const MOCK_JSON_EXTRACT_INITIAL = `{
  "login.title": "",
  "workbench.import.config.json": "",
  "entry.table.column.name": ""
}`;

export default {
  components: {
    CheckOutlined,
    CloseOutlined,
    CustomModal,
    ExclamationCircleOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
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
    CoverButton,
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
      modalWidth: "70%",
      task: {},
      filePath: "",
      filediFileName: null,
      keyWords: "",
      isExist: ALL_ISEXIST,
      isExistOptions: [
        { label: "全部", value: ALL_ISEXIST },
        { label: "已存在", value: "1" },
        { label: "新建", value: "0" },
      ],
      dataType: "file",
      tableHeight: { x: "max-content", y: "300px" },
      // tableHeight: { x: "100%", y: "300px" },
      loading: false,
      templateLoading: false,
      columns: [],
      dataSource: [],
      editableData: {},
      allData: [],
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
      selectedRowKeys: [],
      selectedRows: [],
      accept: ".xls,.xlsx,.csv,.xml",
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
      // ==================== 配置文件（原型增强：XML/JSON 子 Tab） ====================
      configType: "xml", // xml | json
      configFileByType: {
        xml: {
          config: [],
          configOptions: [],
        },
        json: {
          config: [],
          configOptions: [],
        },
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
      saveLoading: false,
      rules: {},
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
      checkboxList: [], // 展示列可选的值
      checkedColumn: [], // 展示列已选的值
      editList_needValidate: null, // 可编辑且需要表单校验的list(工作台只有任务的翻译语种可编辑,并且需要进行表单校验)
      editList: null, // 可编辑的list[...commonParam.langInterList, "comment"]
      translateStateList: [
        ...commonParam.langTranslateStateList,
        "translateState",
      ],
      importBtnLoading: false,
      state: {
        searchText: "",
        searchedColumn: "",
      },
      filters: null,
      filteredData: [],
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
      searchConfigValueByType: {
        xml: "",
        json: "",
      },
      searchEnumValue: "",
      departmentList: commonParam.departmentList, // 当前用户所在部门
      templateTypes: null,
      templateType: null,
      exportTypes: [
        { label: "excel", value: "excel" },
        { label: "csv", value: "csv" },
      ],
      rulesOptions: commonParam.rulesOptions,
      // ==================== JSON 配置文件导入：编辑弹窗状态 ====================
      jsonEditModalVisible: false,
      jsonImportSourceContent: MOCK_JSON_IMPORT_CONTENT,
      jsonExtractContent: MOCK_JSON_EXTRACT_INITIAL,
      jsonSyncingToExtract: false,
      jsonImportLoading: false,
      jsonExtractEditMode: false,
      // 行级 JSON 视图与勾选
      jsonFlatNodes: [], // [{ id,index,path,key,value,type,lineText }]（保留用于兼容）
      selectedJsonNodeIds: [], // （保留用于兼容）
      // 完整格式化 JSON 文本与行数组（用于完整视图与高亮）
      jsonFormattedText: "",
      jsonLines: [],
      jsonLineMeta: [], // [{ lineIndex, isTranslatable, path?, value?, type? }]
      selectedLineIndices: [], // 基于行号的勾选状态（行号从 1 开始）
      isSyncingScroll: false, // 同步滚动标志，防止无限循环
    };
  },
  computed: {
    // 当前激活的配置文件表单模型（用于模板绑定）
    activeConfigModel() {
      return this.configFileByType[this.configType] || this.configFileByType.xml;
    },
    // 当前激活 Tab 的搜索值（用于 v-model:searchValue 绑定）
    activeSearchConfigValue: {
      get() {
        return this.searchConfigValueByType[this.configType] || "";
      },
      set(v) {
        this.searchConfigValueByType[this.configType] = v || "";
      },
    },
    // 生成带语法高亮和选中高亮的 JSON HTML（使用原始 lineText）
    highlightedJsonContent() {
      if (!this.jsonLineMeta.length) {
        return "";
      }
      const selectedSet = new Set(this.selectedLineIndices);
      const lines = this.jsonLineMeta.map((meta, index) => {
        const lineIndex = index + 1;
        const isSelected = selectedSet.has(lineIndex);
        // 使用 lineText（保留原始缩进和标点）
        const lineText = meta.lineText || this.jsonLines[index] || "";
        // 简单的 token 化：匹配 "key": "value" 模式
        const keyValueMatch = lineText.match(/^(\s*)("([^"]+)")\s*:\s*("([^"]*)")\s*(,?)\s*$/);
        if (keyValueMatch) {
          const indent = this.escapeHtml(keyValueMatch[1]);
          const keyQuotes = this.escapeHtml(keyValueMatch[2]);
          const key = this.escapeHtml(keyValueMatch[3]);
          const valueQuotes = this.escapeHtml(keyValueMatch[4]);
          const value = this.escapeHtml(keyValueMatch[5]);
          const comma = this.escapeHtml(keyValueMatch[6]);
          const valueClass = meta?.isTranslatable
            ? `json-value ${isSelected ? "json-value--selected" : ""}`
            : "json-value json-value--non-string";
          // 整行高亮（如果被选中）
          const lineClass = isSelected ? "json-line--selected" : "";
          return `<span class="${lineClass}">${indent}<span class="json-symbol">${keyQuotes}</span><span class="json-key">${key}</span><span class="json-symbol">"</span><span class="json-symbol">: </span><span class="${valueClass}">${valueQuotes}</span><span class="json-symbol">${comma}</span></span>`;
        }
        // 非 key-value 行（结构行），直接转义 HTML，但保留选中高亮
        const isSelectedLine = isSelected;
        const lineClass = isSelectedLine ? "json-line--selected" : "";
        const escaped = this.escapeHtml(lineText);
        return `<span class="${lineClass}">${escaped}</span>`;
      });
      return lines.join("\n");
    },
  },
  watch: {
    currentTask(newval, oldval) {
      this.task = newval;
      this.task.transMap = commonParam.languageMap[this.task.translateType];
    },
    visible: {
      async handler(newVal) {
        // console.log("打开工作台-导入", newVal);
        if (newVal) {
          this.$nextTick(() => {
            // 1.设置模板类型
            // 获取模板类型（暂时只有装置部使用多个模板）
            if (this.$currentDepartment && Object.keys(this.$currentDepartment).includes("templateType"))
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
            // 移除翻译列和固定列对应的展示项
            this.checkboxList = commonParam.checkboxList.filter(
              (item) =>
                ![
                  "isExist",
                  "translateState",
                  "entryState",
                  "entry",
                  "translate",
                  this.task.transMap.value,
                  this.task.transMap.interpretation,
                ].includes(item.value)
            );
            // 赋值：当前列的默认值
            this.columns = [
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
                title: "翻译",
                dataIndex: "translate",
                align: "center",
                width: 100,
                resizable: true,
                index: 5,
                // 添加 sorter 属性实现排序功能
                sorter: (a, b) => a.entry.localeCompare(b.entry),
                sortDirections: ["ascend", "descend"],
              },
              {
                title: "释义",
                dataIndex: "interpretation",
                align: "center",
                width: 100,
                resizable: true,
                index: 6,
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
            ];
            // 读取本地存储的用户偏好
            getColPref("colPref-importModal", 100, this);
            // 设置翻译列展示的语种
            this.columns.forEach((item) => {
              if (item.title === "翻译") {
                item.dataIndex = this.task.transMap.value;
              }
              if (item.title === "释义") {
                item.dataIndex = this.task.transMap.interpretation;
              }
            });
          });
        }
      },
    },
  },
  methods: {
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
      this.saveEntrys();
      this.saveLoading = false;
    },
    handleClose() {
      this.selectedRows = [];
      this.selectedRowKeys = [];
      this.selectedRowIndex = null;
      this.$emit("handleClose");
    },
    getRowClassName(record, index) {
      let className = null;
      if (index % 2 === 1) {
        className = "table-striped";
        if (this.selectedRowIndex === index) {
          className = className + " highlighted-row";
        }
      } else {
        if (this.selectedRowIndex === index) {
          className = "highlighted-row";
        }
      }
      return className;
    },
    handleResizeColumn: (w, col) => {
      col.width = w;
    },
    // 保存词条
    async saveEntrys() {
      this.saveLoading = true;
      const currentLang = this.task.transMap.value;
      let hasNoInter = false;
      let arr = {
        acceptIds: new Set(), // 所有校验通过
        errorIds: new Set(), // 所有校验不通过
        toLongIds: new Set(), // 校验长度
        specialIds: new Set(), // 校验特殊字符
      };
      // 1.保存编辑框中的所有信息
      for (let key in this.editableData) {
        if (this.selectedRowKeys.includes(key)) {
          // let entry = this.dataSource.find((item) => item.id === key);
          // entry = cloneDeep(this.editableData[key]);

          // if (entry[currentLang] != null) {
          //   // 翻译存在  则状态为待审核状态
          //   entry[this.task.transMap.state] = "1";
          // }

          // 保存编辑框中的所有信息
          const index = this.dataSource.findIndex((item) => item.id === key);
          if (index != -1) {
            if (this.editableData[key][currentLang] != null) {
              // 翻译存在  则状态为待审核状态
              this.editableData[key][currentLang] = "1";
            }
            this.dataSource[index] = cloneDeep(this.editableData[key]);
          }

          delete this.editableData[key];
        }
      }
      // 2.保存前校验
      const verifyMethods = this.rulesOptions
        .filter((option) => option.checked)
        .map((option) => option.key);
      arr = await verifyArray_workbench(
        this,
        this.selectedRows,
        currentLang,
        verifyMethods
      );
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
        return;
      }

      // 3.修改状态
      // 去除this.selectedRows中的子词条，因为父词条中已经包含了
      const childRowIDs = new Set(
        this.selectedRows
          .filter((item) => item.children && item.children.length > 0)
          .flatMap((item) => item.children.map((child) => child.id))
      );
      const selectFatherRows = this.selectedRows.filter(
        (item) => !childRowIDs.has(item.id)
      );
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
      // 4.更新词条;弹窗;校验当前页数据
      if (hasNoInter) {
        Modal.confirm({
          title:
            "保存数据中含有中文释义和英文释义都不存在的词条，是否继续保存?",
          icon: createVNode(ExclamationCircleOutlined),
          content: "",
          okText: "是",
          cancelText: "否",
          style: { top: "30%" },
          onOk: () => {
            this.insertOrUpdateEntrys(
              arrCount.insertArr,
              arrCount.updateArr,
              arrCount
            );
          },
          onCancel: () => {
            this.saveLoading = false;
          },
        });
      } else {
        this.insertOrUpdateEntrys(
          arrCount.insertArr,
          arrCount.updateArr,
          arrCount
        );
      }
    },
    //
    insertOrUpdateEntrys(insertArr, updateArr, arrCount) {
      this.loading = true;
      let params = {
        taskID: this.task.id,
      };
      const promises = [];
      let messageTextParts = [];

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
        // 新增
        const addPromise = insertEntry(params, insertArr)
          .then((res) => {
            const successCount = arrCount.addNum - res.data.totalNum;
            const failCount = res.data.totalNum;
            if (successCount > 0) {
              messageTextParts.push(`新增数据${successCount}条`);
            }
            if (failCount > 0) {
              messageTextParts.push(`新增失败${failCount}条`);
            }
            if (arrCount.addChildNum > 0) {
              messageTextParts.push(`其中聚合的数据${arrCount.addChildNum}条`);
            }

            // 从 insertArr 中移除保存失败的数据
            const acArr = filter_arr(insertArr, res.data.list);
            // 从 this.dataSource 中移除保存成功的数据
            this.dataSource = filter_arr(this.dataSource, acArr);
            // 从this.selectedRows 中移除保存成功的数据
            this.selectedRows = filter_arr(this.selectedRows, acArr);
            this.selectedRowKeys = filter_arr_keys(this.selectedRowKeys, acArr);
          })
          .catch((err) => {
            console.log("新增失败", err);
            message.error("新增失败", err.message);
          });
        promises.push(addPromise);
      }
      if (updateArr.length > 0) {
        // 编辑
        const updatePromise = updateEntryList(params, updateArr)
          .then((res) => {
            const successCount = updateArr.length - res.data.totalNum;
            const failCount = res.data.totalNum;
            if (successCount > 0) {
              messageTextParts.push(`重置已审核数据${successCount}条`);
            }
            if (failCount > 0) {
              messageTextParts.push(`重置失败${failCount}条`);
            }
            if (arrCount.updateChildNum > 0) {
              messageTextParts.push(
                `其中聚合的数据${arrCount.updateChildNum}条`
              );
            }
            // console.log("arrCount", arrCount);

            // 从 updateArr 中移除保存失败的数据
            const acArr = filter_arr(updateArr, res.data.list);
            // 从 this.dataSource 中移除保存成功的数据
            this.dataSource = filter_arr(this.dataSource, acArr);
            // 从this.selectedRows 中移除保存成功的数据
            this.selectedRows = filter_arr(this.selectedRows, acArr);
            this.selectedRowKeys = filter_arr_keys(this.selectedRowKeys, acArr);
          })
          .catch((err) => {
            message.error("2", err.message);
          });
        promises.push(updatePromise);
      }

      Promise.all(promises)
        .then(async () => {
          if (messageTextParts.length > 0) {
            message.success("数据已保存！" + messageTextParts.join("，"));
          }
          this.allData = this.dataSource;

          // 更新成功：刷新所有任务的小红点
          this.$emit("afterSave", this.currentTask);
          // 刷新无审核未通过的词条
          this.initTaskEntry();

          // 清空选中
          this.selectedRows = [];
          this.selectedRowKeys = [];
          this.selectedRowIndex = null;
          // 无数据则关闭弹窗；有数据则检验当前页
          if (this.dataSource.length === 0) {
            this.handleClose();
          } else {
            try {
              // 校验当前页数据
              await verifyArray_workbench_page(
                this.pagination,
                this.task.transMap.value,
                this
              );
            } catch (err) { }
          }
        })
        .finally(() => {
          this.saveLoading = false;
          this.loading = false;
        });
    },
    // 获取该任务有无审核未通过的词条
    initTaskEntry() {
      let params = {
        taskID: this.task.id,
        entryState: "2",
        entry: this.keyWords,
      };
      this.loading = true;
      getEntryInfoList(params, [])
        .then((res) => {
          // console.log("数据", this.dataSource, this.selectedRows);
          if (res.data.list.length > 0) {
            this.dataSource = res.data.list;
            this.allData = this.dataSource;
          }
          this.loading = false;
        })
        .catch((err) => {
          this.loading = false;
          message.error("3", err.message);
        });
    },
    beforeUpload(file, fileList) {
      // console.log("before");
      // 去掉后缀必须是_zz\_common的限制，由入参来进行部门之间的隔离
      // if (
      //   !file.name.includes("zz.xlsx") &&
      //   !file.name.includes("common.xlsx") &&
      //   !file.name.includes("jk.xlsx")
      // ) {
      //   message.info("请选择正确的文件！");
      //   console.log("请选择正确的文件！",file);
      //   return;
      // }
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
    // 模糊查询
    select() {
      if (this.filterLanguage === null) {
        this.dataSource = this.allData.filter(
          (item) =>
            item.entry.includes(this.keyWords) &&
            (this.isExist == ALL_ISEXIST ||
              item.isExist == this.isExist)
        );
      } else {
        this.dataSource = this.filterSource.filter(
          (item) =>
            item.entry.includes(this.keyWords) &&
            (this.isExist == ALL_ISEXIST ||
              item.isExist == this.isExist)
        );
      }
    },
    onSelectChange(selectedRowKeys, selectedRows) {
      onSelectChange(this, selectedRowKeys, selectedRows);
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
              }
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
        // 配置文件（XML/JSON 子 Tab）
        this.configType = "xml";
        this.configFileByType.xml.config = [];
        this.configFileByType.json.config = [];
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
    // 导入词条数据
    importEntryData() {
      this.loading = true;
      this.importBtnLoading = true;

      // 定义公共处理函数
      const handleCommonOperations = (data) => {
        // console.log("data数据", data);
        this.dataSource = []; // 清空数据
        if (data.length > 0) {
          this.dataSource = data;
          this.sortArray(this.dataSource, "isExist");
          this.dataSource.forEach((item) => {
            item.auditState = 1;
            // 装置部的需求
            // 配置最大字符长度(此处对应的是翻译的最大字符长度，所以不用maxLength这个属性)
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
      const closeLoading = () => {
        this.loading = false;
        this.importBtnLoading = false;
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

      let asyncTask = false,
        otherFn = false;

      if (this.dataType === "file") {
        // 文件
        // console.log(this.file)
        if (Object.keys(this.file).length === 0) {
          this.loading = false;
          this.importBtnLoading = false;
          message.info("请选择文件！");
          return;
        }
        if (this.templateTypes && !this.templateType) {
          this.loading = false;
          this.importBtnLoading = false;
          message.info("请选择模板类型！");
          return;
        }
        let formData = new FormData();
        formData.append("file", this.file);
        formData.append("taskID", this.task.id);
        const params = {
          diFileName: this.filediFileName,
          departmentType: this.user.department,
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
          closeLoading,
          this.$refs.tsFormRef,
          getTsWords,
          params,
          this.tsFile.tsFileValue
        );
      } else if (this.dataType === "database") {
        // 实时库
        otherFn = true;
        if (this.dataLibrary.type === "field") {
          // 实时库-对象数据（原 字段）
          asyncTask = this.$refs.fieldFormRef
            .validate()
            .then(() => {
              return new Promise((resolve) => {
                this.getFieldData();
                resolve([]);
              });
            })
            .catch((err) => {
              // message.error(err.message);// 校验参数未通过，不提示错误信息
              closeLoading();
              return [];
            });
          // asyncTask = handleAsyncRequest(
          // closeLoading,
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
            .then(() => {
              return new Promise((resolve) => {
                this.getAlias();
                resolve([]);
              });
            })
            .catch((err) => {
              // message.error("7", err.message);
              closeLoading();
              return [];
            });
          // asyncTask = handleAsyncRequest(
          // closeLoading,
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
            .then(() => {
              return new Promise((resolve) => {
                this.batchImportDatabase();
                resolve([]);
              });
            })
            .catch((err) => {
              // message.error("8", err.message);
              closeLoading();
              return [];
            });
          // asyncTask = handleAsyncRequest(
          // closeLoading,
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
          closeLoading,
          this.$refs.dictSelectRef,
          importDictionaryEntry,
          params,
          this.dict.dictionaryType
        );
      } else if (this.dataType === "config") {
        // 配置文件：XML 调用真实接口，JSON 进入“编辑 json 导入词条”原型弹窗
        if (this.configType === "xml") {
          const params = getCommonParams({ diFileName: "" });
          asyncTask = handleAsyncRequest(
            closeLoading,
            this.$refs.configFormRef,
            getConfigEntry,
            params,
            this.activeConfigModel.config
          );
        } else if (this.configType === "json") {
          // JSON：不直接拉取词条列表，而是打开编辑弹窗（原型交互）
          otherFn = true;
          asyncTask = this.openJsonEditModal(closeLoading);
        }
      } else if (this.dataType === "enum") {
        // 枚举文件
        const params = getCommonParams({ diFileName: "" });
        asyncTask = handleAsyncRequest(
          closeLoading,
          this.$refs.enumFormRef,
          getEnumEntry,
          params,
          this.enumFile.enum
        );
      }
      if (asyncTask) {
        asyncTask
          .then((data) => {
            handleCommonOperations(data);
          })
          .finally(() => {
            if (!otherFn) {
              // 不进去其他函数（即非实时库）
              closeLoading();
              // 校验当前页数据
              verifyArray_workbench_page(
                this.pagination,
                this.task.transMap.value,
                this
              );
            }
          });
      } else {
        closeLoading();
      }
    },
    sortArray(arr, key) {
      return arr.sort((a, b) => {
        let x = a[key];
        let y = b[key];
        return x > y ? -1 : x < y ? 1 : 0;
      });
    },
    // 获取对象数据（原 字段）内容
    async getFieldData() {
      let table = this.treeData.find(
        (item) => item.id === this.dataLibrary.table
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
      await getFieldData(params, data)
        .then((res) => {
          this.dataSource = res.data.list;
          this.sortArray(this.dataSource, "isExist");
          this.allData = this.dataSource;
        })
        .catch((err) => {
          message.error("数据获取失败！", err.message);
        })
        .finally(() => {
          this.loading = false;
          this.importBtnLoading = false;
        });
    },
    // 获取别名
    async getAlias() {
      this.loading = true;
      this.importBtnLoading = true;
      let table = this.treeData.find(
        (item) => item.id === this.dataLibrary.table
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
      await getAlias(params)
        .then((res) => {
          this.dataSource = res.data.list;
          this.sortArray(this.dataSource, "isExist");
          this.allData = this.dataSource;
        })
        .catch((err) => {
          message.error("数据获取失败！", err.message);
        })
        .finally(() => {
          this.loading = false;
          this.importBtnLoading = false;
        });
    },
    // 批量导入数据库
    async batchImportDatabase() {
      this.dataSource = [];
      this.allData = [];
      let selectNode = [];
      this.dataLibrary.tables.forEach((item) => {
        let node = this.treeData.find((d) => d.value === item);
        selectNode.push(node);
      });
      if (selectNode.length === 0) {
        this.loading = false;
        this.importBtnLoading = false;
        return;
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
      this.dataSource = [];
      let params = {
        taskID: this.task.id,
        versionID: this.task.versionId ? this.task.versionId : "",
        translateType: this.task.translateType,
        // diFileName: this.dataLibrary.diFileName
        diFileName: "",
        i18nUrl: this.ip,
      };
      // if (nodes.length > 0) {
      //   nodes.forEach((item) => {
      //     item.node = item.value;
      //   });
      //   getDBALLEntryByNode(params, nodes).then((res) => {
      //     this.dataSource = this.dataSource.concat(res.data.list);
      //     this.sortArray(this.dataSource, "isExist");
      //     this.allData = this.dataSource;
      //     // this.loading = false;
      //     // this.importBtnLoading = false;
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
      //     // this.loading = false;
      //     // this.importBtnLoading = false;
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
      //     // this.loading = false;
      //     // this.importBtnLoading = false;
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
          const promise = await apiFn(params, list).then((res) => {
            this.dataSource = this.dataSource.concat(res.data.list);
            this.sortArray(this.dataSource, "isExist");
            this.allData = this.dataSource;
          });
          promises.push(promise);
        }
      }

      // 等待所有请求完成后更新加载状态
      Promise.all(promises).finally(() => {
        this.loading = false;
        this.importBtnLoading = false;
      });
    },
    // 添加表格行点击事件
    customRow(record, index) {
      return {
        onDblclick: async (event) => {
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }
          // 打开编辑态;设置校验规则(工作台只为翻译列配置)
          await openSetEdit(record, [this.task.transMap.value], this);
          // 使用校验规则-翻译列
          useRefRules(
            this.$refs,
            `form${record.id.replaceAll("-", "")}${this.task.transMap.value}`
          );
          this.showEditOperation(); // 显示编辑操作列
        },
      };
    },
    // 编辑-保存
    editSave(record) {
      for (const [key, value] of Object.entries(this.editableData[record.id])) {
        if (record.hasOwnProperty(key) && value != null && value !== "") {
          // 如果record中存在该键，并且值不为空，则更新record
          record[key] = value;
          delete this.editableData[record.id];
          this.hideEditOperation();
        }
      }
    },
    // 取消编辑
    cancel(record) {
      delete this.editableData[record.id];
      this.hideEditOperation();
    },
    // 显示编辑操作列
    showEditOperation() {
      if (this.columns.at(-1).dataIndex === "editOperation") {
        // 如果编辑操作列已经存在，则不再添加
        return;
      }
      const editOperationColumn = {
        title: "编辑操作",
        dataIndex: "editOperation",
        align: "center",
        width: 100,
        resizable: true,
        fixed: "right",
        index: 101, // 确保该列在最右侧，可根据实际情况调整
      };
      this.columns.push(editOperationColumn);
    },
    // 删除操作列
    hideEditOperation() {
      if (Object.keys(this.editableData).length === 0) {
        this.columns = this.columns.filter((item) => {
          return item.dataIndex != "editOperation";
        });
      }
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
            (entry) => entry.id === item.id
          );
          for (let i = 0; i < item.children.length; i++) {
            let child = item.children[i];
            child.parentID = "";
            this.dataSource.splice(index + i + 1, 0, child);
          }
          item.children = [];
        } else {
          let parent = this.dataSource.find(
            (data) => data.id === item.parentID
          );
          parent.children = parent.children.filter(
            (child) => child.id != item.id
          );
          let index = this.dataSource.findIndex(
            (data) => data.id === item.parentID
          );
          item.parentID = "";
          this.dataSource.splice(index + 1, 0, item);
        }
      });
      this.allData = this.dataSource;
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    afterClose() {
      this.editableData = {};
      this.keyWords = "";
      this.isExist = ALL_ISEXIST;
      this.dataType = "file";
      this.dataSource = [];
      this.allData = [];
      this.tsFile.tsFileValue = [];
      this.file = {};
      this.filePath = "";
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
      this.clearFilters();
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
    // 展示列切换并保存用户偏好
    changeColumn(checkedValue) {
      changeColumn(
        "colPref-importModal",
        100,
        checkedValue,
        this,
        false,
        entryParams.checkboxList,
        true
      );
    },
    // 列筛选
    handleSearch(selectedKeys, confirm, dataIndex) {
      confirm();
      this.state.searchText = selectedKeys[0];
      this.state.searchedColumn = dataIndex;
    },
    handleReset(clearFilters) {
      clearFilters({ confirm: true });
      this.state.searchText = "";
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
      this.templateLoading = true;
      this.$refs.dictRef.validate().then(() => {
        let params = {
          fileType: this.templateObj.type,
          translateType: this.task.translateType,
          // exportFields: this.exportFields,
          exportType: this.templateObj.exportType,
        };
        templateFileDownload(params)
          .then((res) => {
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
          })
          .finally(() => {
            this.templateLoading = false;
          });
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
    // 分页切换
    pageChange(page, pageSize) {
      this.pagination.current = page;
      this.pagination.pageSize = pageSize;

      // 校验当前页数据
      verifyArray_workbench_page(
        this.pagination,
        this.task.transMap.value,
        this
      );
    },
    // 语种切换
    filterLanguageChange() {
      if (this.filterLanguage === "全部") {
        this.dataSource = this.allData;
        this.filterSource = this.allData;
      } else {
        let params = {
          languageType: this.filterLanguage,
        };
        this.loading = true;
        filterSourceLanguage(params, this.allData)
          .then((res) => {
            this.dataSource = res.data.list;
            this.filterSource = res.data.list;
            this.loading = false;
          })
          .catch((err) => {
            this.loading = false;
            message.error("12", err.message);
          });
      }
    },
    // 表格change事件
    handleTableChange(pagination, filters) {
      this.filters = filters;
      for (let key in filters) {
        this.columns.forEach((col) => {
          if (col.dataIndex === key) {
            col.filteredValue = filters[key];
          }
        });
      }
      // 获取筛选后的数据
      let isExistData = this.dataSource.filter((item) => {
        return filters.isExist && filters.isExist.includes(item.isExist);
      });
      let sourceData = this.dataSource.filter((item) => {
        return (
          filters.entrySource && item.entrySource.includes(filters.entrySource)
        );
      });
      this.filteredData = Array.from(new Set([...isExistData, ...sourceData]));
    },
    // 清空表格筛选条件
    clearFilters() {
      if (this.filters) {
        for (let key in this.filters) {
          this.columns.forEach((col) => {
            if (col.dataIndex === key) {
              col.filteredValue = null;
            }
          });
        }
      }
    },
    selectAllEntry() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
      let dataToSelect;
      if (this.filters && (this.filters.isExist || this.filters.entrySource)) {
        // 确保 filteredData 是最新的筛选结果
        dataToSelect = this.dataSource.filter((item) => {
          const isExistMatch =
            !this.filters.isExist ||
            this.filters.isExist.includes(item.isExist);
          const entrySourceMatch =
            !this.filters.entrySource ||
            item.entrySource.includes(this.filters.entrySource);
          return isExistMatch && entrySourceMatch;
        });
      } else {
        dataToSelect = this.dataSource;
      }
      dataToSelect.forEach((item) => {
        this.selectedRowKeys.push(item.id);
        this.selectedRows.push(item);
      });
    },

    clearAllEntry() {
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    // 切割字符串
    companyCut(message) {
      let res = [];
      if (message === null || message === "") {
        return res;
      }
      const regex = /[;；]/;
      res = message.split(regex);
      res = res.filter((item) => item != "");
      return res;
    },
    // 获取i18服务器ip
    getIPs() {
      if (this.$currentDepartment && this.$currentDepartment.ops.has("needIP")) {
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
        const xmlList = [];
        if (res.data) {
          res.data.forEach((item) => {
            xmlList.push({
              value: item,
              label: item,
            });
          });
        }
        // XML Tab 使用真实接口返回的配置文件列表
        this.configFileByType.xml.configOptions = xmlList;
        // JSON Tab 使用本地 Mock 的“生成 .json 配置文件”列表
        this.getJsonConfigListMock();
      });
    },
    // Mock：生成 JSON 配置文件列表（不调用真实接口，仅用于原型体验）
    getJsonConfigListMock() {
      const mockNames = [
        "base.json",
        "workflow.json",
        "glossary.json",
        "permission.json",
        "export.json",
        "import.json",
        "entry-rules.json",
      ];
      const options = mockNames.map((name, idx) => ({
        value: `mock-${idx + 1}-${name}`,
        label: name,
      }));
      this.configFileByType.json.configOptions = options;
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
      const model = this.activeConfigModel;
      const kw = this.activeSearchConfigValue || "";
      model.config = [];
      model.configOptions.forEach((item) => {
        if (item.value.includes(kw)) {
          model.config.push(item.value);
        }
      });
    },
    // 清空选中的ts文件
    clearAllConfig() {
      this.activeConfigModel.config = [];
    },
    onConfigSearch(value) {
      this.activeSearchConfigValue = value;
    },
    // 选中项变化时，不清空搜索框内容
    onConfigChange(value) {
      // console.log('Selected value:', value);
      // 这里可以根据需要进行额外的操作，但不会清空搜索内容
    },
    onConfigBlur() {
      this.activeSearchConfigValue = "";
    },
    // ==================== JSON 配置文件导入：编辑弹窗交互 ====================
    /**
     * 打开“编辑json导入词条”弹窗
     * - 校验已选择 JSON 配置文件
     * - 左侧展示「导入json」原始内容
     * - 右侧展示「抽取json」初始内容
     */
    openJsonEditModal(closeLoading) {
      // 先走表单校验（保持与其它导入方式一致）
      return this.$refs.configFormRef
        .validate()
        .then(() => {
          // 重置编辑态与 loading 状态
          this.jsonExtractEditMode = false;
          this.jsonSyncingToExtract = false;
          this.jsonImportLoading = false;
          this.selectedJsonNodeIds = [];
          this.selectedLineIndices = [];

          // 根据当前选中的配置文件简单 Mock 一份“导入 json”内容
          const selectedRaw = this.activeConfigModel.config;
          let fileLabels = [];
          if (Array.isArray(selectedRaw)) {
            fileLabels = selectedRaw;
          } else if (selectedRaw) {
            fileLabels = [selectedRaw];
          }
          const fileLabel =
            fileLabels.length > 0 ? fileLabels.join(", ") : "mock-config.json";
          this.jsonImportSourceContent = `// 导入配置文件（JSON）：${fileLabel}\n` +
            MOCK_JSON_IMPORT_CONTENT;

          // 抽取区初始化
          this.jsonExtractContent = MOCK_JSON_EXTRACT_INITIAL;

          // 基于 Mock JSON 内容构建行级视图（只读）
          try {
            const rawJson = JSON.parse(MOCK_JSON_IMPORT_CONTENT);
            // Task 1：生成完整格式化 JSON 文本与行数组
            this.jsonFormattedText = JSON.stringify(rawJson, null, 2);
            this.jsonLines = this.jsonFormattedText.split("\n");
            // Task 2：构建行元数据（识别哪些行是 string value 行）
            const stringNodes = this.collectStringNodes(rawJson);
            this.jsonLineMeta = this.buildLineMeta(this.jsonLines, stringNodes);
            // 现有行级视图仍基于扁平节点，后续任务再切换到基于行的视图
            this.jsonFlatNodes = this.buildJsonFlatNodes(rawJson);
          } catch (e) {
            // eslint-disable-next-line no-console
            console.error("[prototype][json-import] parse mock json failed", e);
            this.jsonFlatNodes = [];
            this.jsonFormattedText = "";
            this.jsonLines = [];
            this.jsonLineMeta = [];
          }

          this.jsonEditModalVisible = true;

          // 结束主界面 loading
          if (typeof closeLoading === "function") {
            closeLoading();
          } else {
            this.loading = false;
            this.importBtnLoading = false;
          }

          // 对主表格不返回数据（保持原有列表不变）
          return [];
        })
        .catch(() => {
          // 校验未通过，直接关闭 loading
          if (typeof closeLoading === "function") {
            closeLoading();
          } else {
            this.loading = false;
            this.importBtnLoading = false;
          }
          return [];
        });
    },
    // 关闭“编辑json导入词条”弹窗
    closeJsonEditModal() {
      this.jsonEditModalVisible = false;
      this.jsonSyncingToExtract = false;
      this.jsonImportLoading = false;
      this.selectedJsonNodeIds = [];
      this.selectedLineIndices = [];
    },
    // 构建 JSON 行级视图（仅叶子节点，一行一个 key）
    buildJsonFlatNodes(rawJson) {
      const nodes = [];
      let index = 1;
      const pushNode = (path, key, value) => {
        const type = value === null ? "null" : typeof value;
        const displayKey = key;
        let valuePreview;
        if (type === "string") {
          valuePreview = `"${value}"`;
        } else {
          valuePreview = JSON.stringify(value);
        }
        const lineText = `"${displayKey}": ${valuePreview},  // ${path}`;
        const depth = Math.max(path.split("/").length - 2, 0); // root/xxx -> depth 0
        nodes.push({
          id: `json-node-${index}`,
          index,
          path,
          key: displayKey,
          value,
          type,
          lineText,
          depth,
          displayValue: valuePreview,
        });
        index += 1;
      };
      const walk = (obj, currentPath) => {
        if (obj === null) {
          // null 作为叶子
          pushNode(currentPath, currentPath.split("/").slice(-1)[0] || "root", null);
          return;
        }
        if (Array.isArray(obj)) {
          obj.forEach((item, idx) => {
            const elemKey = `@idx_${idx}`;
            const basePath = currentPath ? `${currentPath}/${elemKey}` : elemKey;
            if (
              item !== null &&
              typeof item === "object"
            ) {
              walk(item, basePath);
            } else {
              pushNode(basePath, elemKey, item);
            }
          });
        } else if (typeof obj === "object") {
          Object.keys(obj).forEach((k) => {
            const basePath = currentPath ? `${currentPath}/${k}` : k;
            const v = obj[k];
            if (v !== null && typeof v === "object") {
              walk(v, basePath);
            } else {
              pushNode(basePath, k, v);
            }
          });
        } else {
          // 原子值挂在当前路径
          const lastKey = currentPath.split("/").slice(-1)[0] || "root";
          pushNode(currentPath || lastKey, lastKey, obj);
        }
      };
      walk(rawJson, "root");
      return nodes;
    },
    // Task 2：收集所有 string 叶子节点（DFS）
    collectStringNodes(rawJson) {
      const stringNodes = [];
      const walk = (obj, currentPath) => {
        if (obj === null || typeof obj !== "object") {
          return;
        }
        if (Array.isArray(obj)) {
          obj.forEach((item, idx) => {
            const elemKey = `@idx_${idx}`;
            const basePath = currentPath ? `${currentPath}/${elemKey}` : elemKey;
            if (typeof item === "string") {
              stringNodes.push({
                path: basePath,
                key: elemKey,
                value: item,
              });
            } else if (item !== null && typeof item === "object") {
              walk(item, basePath);
            }
          });
        } else {
          Object.keys(obj).forEach((k) => {
            const basePath = currentPath ? `${currentPath}/${k}` : k;
            const v = obj[k];
            if (typeof v === "string") {
              stringNodes.push({
                path: basePath,
                key: k,
                value: v,
              });
            } else if (v !== null && typeof v === "object") {
              walk(v, basePath);
            }
          });
        }
      };
      walk(rawJson, "root");
      return stringNodes;
    },
    // Task 2：构建行元数据（将 string 节点对齐到行号）
    buildLineMeta(lines, stringNodes) {
      const lineMeta = [];
      // 维护路径上下文栈（用于更精确匹配）
      const pathStack = [];
      // 构建 value -> stringNode[] 的映射（可能有重复值）
      const valueMap = new Map();
      stringNodes.forEach((node) => {
        const key = `${node.key}:${node.value}`;
        if (!valueMap.has(key)) {
          valueMap.set(key, []);
        }
        valueMap.get(key).push(node);
      });
      // 解析每一行，判断是否为 string value 行
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const lineIndex = i + 1; // 行号从 1 开始
        const trimmed = line.trim();
        // 更新路径上下文栈
        if (trimmed === "{" || trimmed.startsWith("{")) {
          // 对象开始，路径栈不变（等待下一个 key）
        } else if (trimmed === "}" || trimmed.startsWith("}")) {
          // 对象结束，弹出路径栈
          if (pathStack.length > 0) {
            pathStack.pop();
          }
        } else if (trimmed === "[" || trimmed.startsWith("[")) {
          // 数组开始
        } else if (trimmed === "]" || trimmed.startsWith("]")) {
          // 数组结束
          if (pathStack.length > 0 && pathStack[pathStack.length - 1].isArray) {
            pathStack.pop();
          }
        }
        // 尝试匹配 "key": "value" 模式
        const keyValueMatch = line.match(/^\s*"([^"]+)"\s*:\s*"([^"]*)"\s*,?\s*$/);
        if (keyValueMatch) {
          const key = keyValueMatch[1];
          const value = keyValueMatch[2];
          // 尝试匹配 stringNodes
          const candidates = valueMap.get(`${key}:${value}`) || [];
          let matchedNode = null;
          // 优先匹配：如果只有一个候选，直接使用
          if (candidates.length === 1) {
            matchedNode = candidates[0];
          } else if (candidates.length > 1) {
            // 多个候选：使用第一个（后续可通过路径上下文优化）
            matchedNode = candidates[0];
            // eslint-disable-next-line no-console
            console.warn(
              `[prototype][json-import] Multiple nodes match key="${key}" value="${value}", using first one`
            );
          }
          if (matchedNode) {
            lineMeta.push({
              lineIndex,
              isTranslatable: true,
              path: matchedNode.path,
              value: matchedNode.value,
              type: "string",
              key: matchedNode.key,
              lineText: lines[i], // 原始行文本，保留缩进和标点
            });
          } else {
            // 匹配到 key-value 模式但不是 string 节点
            lineMeta.push({
              lineIndex,
              isTranslatable: false,
              lineText: lines[i],
            });
          }
        } else {
          // 不是 key-value 行（可能是 { } [ ] 等结构行）
          lineMeta.push({
            lineIndex,
            isTranslatable: false,
            lineText: lines[i],
          });
        }
      }
      return lineMeta;
    },
    // 左侧「需要翻译」按钮：根据勾选的 JSON 行生成抽取 json
    // HTML 转义辅助方法
    escapeHtml(text) {
      if (!text) {
        return "";
      }
      const div = document.createElement("div");
      div.textContent = text;
      return div.innerHTML;
    },
    // 同步滚动：左列滚动时同步右列
    handleLeftScroll(event) {
      if (this.isSyncingScroll) {
        return;
      }
      this.isSyncingScroll = true;
      const leftElement = event.target;
      const rightElement = this.$refs.jsonCodeScrollWrapperRef;
      if (rightElement && rightElement.scrollTop !== leftElement.scrollTop) {
        rightElement.scrollTop = leftElement.scrollTop;
      }
      // 使用 requestAnimationFrame 确保在下一帧重置标志
      requestAnimationFrame(() => {
        this.isSyncingScroll = false;
      });
    },
    // 同步滚动：右列滚动时同步左列
    handleRightScroll(event) {
      if (this.isSyncingScroll) {
        return;
      }
      this.isSyncingScroll = true;
      const rightElement = event.target;
      const leftElement = this.$refs.jsonLineNumbersRef;
      if (leftElement && leftElement.scrollTop !== rightElement.scrollTop) {
        leftElement.scrollTop = rightElement.scrollTop;
      }
      // 使用 requestAnimationFrame 确保在下一帧重置标志
      requestAnimationFrame(() => {
        this.isSyncingScroll = false;
      });
    },
    handleJsonNeedTranslate() {
      if (!this.selectedLineIndices.length) {
        message.warning("请先勾选需要翻译的行");
        return;
      }
      this.jsonSyncingToExtract = true;
      // 使用 setTimeout 模拟后端抽取逻辑
      setTimeout(() => {
        const selectedSet = new Set(this.selectedLineIndices);
        // 基于行号从 jsonLineMeta 中筛选可翻译的行
        const selectedMeta = this.jsonLineMeta.filter(
          (meta) => selectedSet.has(meta.lineIndex) && meta.isTranslatable
        );

        if (!selectedMeta.length) {
          this.jsonSyncingToExtract = false;
          message.warning("当前勾选的行中没有可翻译的字符串值");
          return;
        }

        // 合并到现有抽取 json（若解析失败则从空对象开始）
        let currentExtract = {};
        try {
          if (this.jsonExtractContent) {
            currentExtract = JSON.parse(this.jsonExtractContent);
          }
        } catch (e) {
          // ignore parse error, start new
          currentExtract = {};
        }

        selectedMeta.forEach((meta) => {
          // 以 path 作为 key，value 为原始字符串值
          if (meta.path && meta.value !== undefined) {
            currentExtract[meta.path] = meta.value;
          }
        });

        this.jsonExtractContent = JSON.stringify(currentExtract, null, 2);
        this.jsonSyncingToExtract = false;
      }, 800);
    },
    // 右侧「编辑」按钮：进入编辑态
    enterJsonExtractEdit() {
      this.jsonExtractEditMode = true;
    },
    // 右侧「保存」按钮：退出编辑态（不做额外校验，仅模拟流程）
    saveJsonExtract() {
      this.jsonExtractEditMode = false;
      message.success("已保存抽取json（原型，仅本地状态）");
    },
    // 右侧「导入」按钮：模拟调用后端接口，传入“导入json”和“抽取json”
    handleJsonImportConfirm() {
      this.jsonImportLoading = true;
      this.jsonSyncingToExtract = false;
      // 打印模拟的请求体，方便在控制台观察
      // eslint-disable-next-line no-console
      console.log("[prototype][json-import] request body:", {
        importJson: this.jsonImportSourceContent,
        extractJson: this.jsonExtractContent,
      });
      setTimeout(() => {
        this.jsonImportLoading = false;
        message.success("已模拟导入 json 词条（原型）");
      }, 1000);
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

  .platformBox {
    width: 100%;
    background-color: white;
    padding: 0px 16px 16px 16px;
    border-radius: 4px;
  }

  .dataTypeBox {
    // display: flex;
    // align-items: center;
    // align-self: stretch;
    width: 100%;
    // border-radius: 4px;
    background-color: white;

    // padding: 16px;
    // border: 1px solid #f0f0f0;
    :deep(.ant-tabs-nav) {
      margin-bottom: 10px;
    }
  }

  .dataTypeBox2 {
    display: flex;
    width: 100%;
    // border-radius: 4px;
    background-color: white;

    // padding: 16px;
    // border: 1px solid #f0f0f0;
    :deep(.ant-tabs-nav) {
      margin-bottom: 10px;
    }
  }

  .config-box {
    flex-direction: column;
    align-items: flex-start;
  }

  .form {
    display: flex;
    align-items: center;
    align-self: stretch;
    width: 100%;
  }

  .ant-row {
    height: 50px;
    // height: 38px;
  }

  .rejectBtn {
    background: #fbb31f;
    border-color: #fbb31f;
  }

  .rejectBtn:hover {
    background: #fbb31f;
    border-color: #fbb31f;
  }

  .rejectBtn:focus {
    background: #fbb31f;
    border-color: #fbb31f;
  }

  .passTag {
    border: 1px solid #36bf7d;
    color: #36bf7d;
  }

  .passTagChecked {
    background-color: #36bf7d;
    color: white;
  }

  .rejectTag {
    border: 1px solid #fbb31f;
    color: #fbb31f;
  }

  .rejectTagChecked {
    background-color: #fbb31f;
    color: white;
  }
}

/* ==================== JSON 配置文件导入：编辑弹窗样式 ==================== */
.json-edit-modal {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

.json-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.json-pane-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.json-pane-title {
  font-weight: 500;
}

.json-pane-actions {
  display: flex;
  gap: 8px;
}

.json-pane-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.json-pane-tip {
  margin-bottom: 0;
}

.json-code-wrapper {
  position: relative;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  background-color: #fafafa;
  padding: 8px;
  max-height: 380px;
  overflow: auto;
}

.json-code-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 12px;
  line-height: 1.5;
}

.json-code-editor {
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 12px;
}

/* 左侧 JSON 完整视图：两列布局（行号冻结 + 代码滚动） */
.json-view-container {
  display: flex;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  overflow: hidden;
  max-height: 500px;
}

.json-line-numbers {
  flex-shrink: 0;
  width: 80px;
  background-color: #fafafa;
  border-right: 1px solid #d9d9d9;
  padding: 0;
  overflow-y: auto;
  overflow-x: hidden;
  /* 隐藏滚动条，但保持滚动能力 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}

.json-line-numbers::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

.json-line-number-row {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 4px;
  height: 26px;
  font-size: 12px;
  line-height: 24px;
  box-sizing: border-box;
}

.json-line-number-row :deep(.ant-checkbox) {
  margin: 0;
  padding: 0;
  line-height: 24px;
}

.json-line-number-row :deep(.ant-checkbox-inner) {
  width: 14px;
  height: 14px;
}

.json-line-number-row--selected {
  background-color: #fff3cd;
}

.json-line-number-row--translatable:hover {
  background-color: #f0f0f0;
}

.json-code-scroll-wrapper {
  flex: 1;
  overflow-x: auto;
  overflow-y: auto;
  padding: 0 8px;
}

.json-code-block {
  margin: 0;
  padding: 0;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 12px;
  line-height: 24px;
  white-space: pre;
  word-break: normal;
  overflow-wrap: normal;
  user-select: text;
}

/* 兼容旧样式（保留） */
.json-code-list {
  padding: 4px 8px;
}

.json-checkbox-group {
  width: 100%;
}

.json-code-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.json-line-index {
  width: 32px;
  text-align: right;
  color: rgba(0, 0, 0, 0.45);
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 12px;
  line-height: 24px;
  display: inline-block;
  vertical-align: middle;
}

.json-line-text {
  flex: 1;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 12px;
  white-space: pre;
}

.json-key {
  color: #0066cc;
  font-weight: 600;
}

.json-value {
  color: #228b22;
}

.json-value--non-string {
  color: #999999;
}

.json-symbol {
  color: #999999;
}

.json-path-comment {
  margin-left: 8px;
}

.json-code-row--selected .json-line-text,
.json-value--selected {
  background-color: #fff3cd;
  border-radius: 2px;
}

.json-line--selected {
  background-color: #fff3cd;
  display: inline-block;
  width: 100%;
  border-radius: 2px;
}

.ant-form-item {
  margin-bottom: 0;
}

:deep(.ant-pagination) {
  margin: 8px 0;
}
</style>