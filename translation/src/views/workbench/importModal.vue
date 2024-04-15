<template>
    <CustomModal
    :visible="visible" 
    :modalTitle="modalTitle"
    :modalWidth="modalWidth"
    :showCancel="false"
    :okLoading="saveLoading"
    :fullFlag="true"
    okText="保存"
    @handleClose="handleClose"
    @handleOK="handleOK"
    @afterClose="afterClose"
    @setTableHeight="setTableHeight"
    >
        <div class="content">
            <div class="taskInfo">
                <div class="taskItem">任务名称：{{task.name}}</div>
                <div class="taskItem">产品名称：{{task.productName}}</div>
                <div class="taskItem">翻译语种：{{task.translateType}}</div>
            </div>
            <div class="platformBox">
                <a-tabs v-model:activeKey="platformKey">
                    <a-tab-pane key="device" tab="装置平台">
                        词条文件：
                        <a-input
                            v-model:value="filePath"
                            style="width:65%"
                            size="small"
                            placeholder="文件名格式：装置: XXX_zz.xlsx ；通用: XXX_common.xlsx"
                        />
                        
                        <a-upload
                            name="file"
                            :beforeUpload="beforeUpload"
                            :accept="accept"
                            :showUploadList="false"
                            @change="handleChange"
                        >
                            <a-button type="primary" size="small" style="margin-left:8px">选择文件</a-button>
                        </a-upload>
                        <a style="font-size:12px;margin-left:10px" @click="templateFileDownload">下载模板</a>
                        <a-button type="primary" ghost size="small" :loading="importBtnLoading" style="float:right" @click="importEntryData">导入</a-button>
                    </a-tab-pane>
                    <a-tab-pane key="unify" tab="一体化平台">
                        <div style="width:100%;margin-bottom:5px">
                            数据类型：
                            <a-radio-group v-model:value="dataType" @change="dataTypeChange">
                                <a-radio :value="'file'">文件</a-radio>
                                <a-radio :value="'ts'">TS</a-radio>
                                <a-radio :value="'database'">实时库</a-radio>
                                <a-radio :value="'dictionary'">辞典</a-radio>
                                <a-radio :value="'config'">配置文件</a-radio>
                                <a-radio :value="'enum'">枚举文件</a-radio>
                            </a-radio-group>
                            <!-- <a-button type="primary" size="small" class="resetBtn" style="float:right" @click="importEntryData">导入</a-button> -->
                        </div>
                        <div class="dataTypeBox" v-if="dataType === 'file'" ref="fileRef">
                            词条文件：
                            <a-input
                                v-model:value="filePath"
                                style="width:65%"
                                size="small"
                                placeholder="文件名格式：装置: XXX_zz.xlsx ；通用: XXX_common.xlsx"
                            />
                            
                            <a-upload
                                name="file"
                                :beforeUpload="beforeUpload"
                                :accept="accept"
                                :showUploadList="false"
                                @change="handleChange"
                            >
                                <a-button type="primary" size="small" style="margin-left:8px">选择文件</a-button>
                            </a-upload>
                            <a style="font-size:12px;margin-left:10px" @click="templateFileDownload">下载模板</a>
                            <a-button type="primary" ghost size="small" :loading="importBtnLoading" style="float:right" @click="importEntryData">导入</a-button>
                        </div>
                        <div class="dataTypeBox" v-if="dataType === 'ts'">
                            <a-form
                            ref="tsFormRef"
                            :model="tsFile"
                            style="width:100%"
                            >
                                <a-form-item
                                :label="selectTitle" 
                                name="tsFileValue"
                                :rules="[{ required: true, message: '请选择ts文件!' }]"
                                >
                                    <a-select
                                    v-model:value="tsFile.tsFileValue"
                                    mode="multiple"
                                    :max-tag-count="4"
                                    allowClear
                                    style="width: 70%;margin-left:10px"
                                    placeholder="请选择"
                                    size="small"
                                    :options="tsOptions"
                                    >
                                        <template #dropdownRender="{ menuNode: menu }">
                                            <v-nodes :vnodes="menu" />
                                            <a-divider style="margin: 4px 0" />
                                            <div style="padding: 4px 8px; cursor: pointer;" @mousedown="e => e.preventDefault()">
                                                <a-button type="link" @click="selectAllTs">全选</a-button>
                                                <a-button type="link" @click="clearAllTs">清空</a-button>
                                            </div>
                                        </template>
                                    </a-select>
                                    <a-button type="primary" ghost size="small" style="float:right" :loading="importBtnLoading" @click="importEntryData">导入</a-button>
                                </a-form-item>
                            </a-form>
                        </div>
                        <div class="dataTypeBox" v-if="dataType === 'dictionary'" ref="dicRef">
                            <a-radio-group v-model:value="dictionaryType" :options="dictionaryOptions">
                            </a-radio-group>
                            <span v-if="dictionaryOptions.length === 0" style="font-size:12px;color:rgba(0, 0, 0, 0.40);margin-left:45%">暂无数据</span>
                            <a-button type="primary" ghost size="small" style="float:right" :loading="importBtnLoading" @click="importEntryData">导入</a-button>
                        </div>
                        <div class="dataTypeBox" v-if="dataType === 'database'" style="padding-top:0px" ref="dataSourceRef">
                            <a-tabs v-model:activeKey="dataLibrary.type" size="small" style="width:100%" @change="changeDataLibraryType">
                                <a-tab-pane key="field" tab="字段"></a-tab-pane>
                                <a-tab-pane key="alias" tab="元数据"></a-tab-pane>
                                <a-tab-pane key="allData" tab="全量"></a-tab-pane>
                            </a-tabs>
                            <a-form
                            ref="fieldFormRef"
                            name="advanced_search"
                            class="ant-advanced-search-form"
                            :model="dataLibrary"
                            style="width:100%"
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
                                                tree-data-simple-mode
                                                allowClear
                                                style="width: 100%"
                                                :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
                                                :tree-data="treeData"
                                                placeholder="请选择表"
                                                :load-data="onLoadData"
                                                :show-checked-strategy="SHOW_PARENT"
                                                @select="treeSelect"
                                                size="small"
                                            />
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
                                                    <div style="padding: 4px 8px; cursor: pointer;" @mousedown="e => e.preventDefault()">
                                                        <a-button type="link" @click="selectAllField">全选</a-button>
                                                        <a-button type="link" @click="clearAllField">清空</a-button>
                                                    </div>
                                                </template>
                                            </a-select>
                                        </a-form-item>
                                    </a-col>
                                    <a-col :span="8">
                                        <a-form-item 
                                        label="回写辞典"
                                        name="diFileName"
                                        :rules="[{ required: true, message: '请选择回写辞典!' }]"
                                        >
                                            <a-select
                                            v-model:value="dataLibrary.diFileName"
                                            allowClear
                                            placeholder="请选择翻译数据回写辞典目录"
                                            :options="dictionaryOptions"
                                            style="width:70%"
                                            size="small"
                                            >
                                            </a-select>
                                            <!-- <a-tooltip placement="top">
                                                <template #title>
                                                <span>添加辞典</span>
                                                </template>
                                                <PlusSquareOutlined @click="createDictionary" style="color:#369FFF;margin-left:8px"/>
                                            </a-tooltip> -->
                                            <a-button type="primary" ghost size="small" style="float:right" :loading="importBtnLoading" @click="importEntryData">导入</a-button>
                                        </a-form-item>
                                    </a-col>
                                </a-row>
                            </a-form>
                            <a-form
                            ref="aliasFormRef"
                            name="advanced_search"
                            class="ant-advanced-search-form"
                            :model="dataLibrary"
                            style="width:100%"
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
                                                tree-data-simple-mode
                                                allowClear
                                                style="width: 100%"
                                                :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
                                                :tree-data="treeData"
                                                placeholder="请选择库"
                                                :load-data="onLoadData"
                                                @select="treeSelect"
                                                size="small"
                                            />
                                        </a-form-item>
                                    </a-col>
                                    <a-col :span="8">
                                        <a-form-item label="限制长度">
                                            <a-input-number
                                                v-model:value="dataLibrary.maxLength"
                                                style="width:100%"
                                                size="small"
                                                placeholder="请输入限制长度"
                                            />
                                        </a-form-item>
                                    </a-col>
                                    <a-col :span="8">
                                        <a-form-item 
                                        label="回写辞典"
                                        name="diFileName"
                                        :rules="[{ required: true, message: '请选择回写辞典!' }]"
                                        >
                                            <a-select
                                            v-model:value="dataLibrary.diFileName"
                                            allowClear
                                            placeholder="请选择翻译数据回写辞典目录"
                                            :options="dictionaryOptions"
                                            style="width:80%"
                                            size="small"
                                            >
                                            </a-select>
                                            <!-- <a-tooltip placement="top">
                                                <template #title>
                                                <span>添加辞典</span>
                                                </template>
                                                <PlusSquareOutlined @click="createDictionary" style="color:#369FFF;margin-left:8px"/>
                                            </a-tooltip> -->
                                            <a-button type="primary" ghost size="small" style="float:right" :loading="importBtnLoading" @click="importEntryData">导入</a-button>
                                        </a-form-item>
                                    </a-col>
                                </a-row>
                            </a-form>
                            <a-form
                            ref="allDataFormRef"
                            name="advanced_search"
                            class="ant-advanced-search-form"
                            :model="dataLibrary"
                            style="width:100%"
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
                                                allowClear
                                                tree-data-simple-mode
                                                style="width: 100%"
                                                :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }"
                                                :tree-data="treeData"
                                                placeholder="请选择"
                                                :load-data="onLoadData"
                                                :maxTagCount="3"
                                                tree-checkable
                                                :show-checked-strategy="SHOW_PARENT"
                                                @select="treeBatchSelect"
                                                size="small"
                                            />
                                        </a-form-item>
                                    </a-col>
                                    <a-col :span="12">
                                        <a-form-item 
                                        label="回写辞典"
                                        name="diFileName"
                                        :rules="[{ required: true, message: '请选择回写辞典!' }]"
                                        >
                                            <a-select
                                            v-model:value="dataLibrary.diFileName"
                                            allowClear
                                            placeholder="请选择翻译数据回写辞典目录"
                                            :options="dictionaryOptions"
                                            style="width:80%"
                                            size="small"
                                            >
                                            </a-select>
                                            <!-- <a-tooltip placement="top">
                                                <template #title>
                                                <span>添加辞典</span>
                                                </template>
                                                <PlusSquareOutlined @click="createDictionary" style="color:#369FFF;margin-left:8px"/>
                                            </a-tooltip> -->
                                            <a-button type="primary" ghost size="small" style="float:right" :loading="importBtnLoading" @click="importEntryData">导入</a-button>
                                        </a-form-item>
                                    </a-col>
                                </a-row>
                            </a-form>
                        </div>
                        <div class="dataTypeBox" v-if="dataType === 'config' || dataType === 'enum'" ref="configRef">
                            <a-form
                            ref="configFormRef"
                            name="advanced_search"
                            class="ant-advanced-search-form"
                            :model="configFile"
                            style="width:100%"
                            >
                                <a-form-item
                                label="回写辞典目录"
                                name="dict"
                                :rules="[{ required: true, message: '请选择回写辞典!' }]"
                                >
                                    <a-select
                                    v-model:value="configFile.dict"
                                    allowClear
                                    placeholder="请选择翻译数据回写辞典目录"
                                    :options="dictionaryOptions"
                                    style="width:50%"
                                    size="small"
                                    >
                                    </a-select>
                                    <!-- <a-tooltip placement="top">
                                        <template #title>
                                        <span>添加辞典</span>
                                        </template>
                                        <PlusSquareOutlined @click="createDictionary" style="color:#369FFF;margin-left:8px"/>
                                    </a-tooltip> -->
                                    <a-button type="primary" ghost size="small" style="float:right" :loading="importBtnLoading" @click="importEntryData">导入</a-button>
                                </a-form-item>
                            </a-form>
                        </div>
                    </a-tab-pane>
                </a-tabs>
            </div>
            
            
            <div class="form">
                词条：
                <a-input
                    v-model:value="keyWords"
                    style="width:30%"
                    size="small"
                    placeholder='请输入词条搜索'
                />
                <a-button type="primary" size="small" style="margin-left:8px" @click="select">
                    <template #icon><SearchOutlined /></template>查询
                </a-button>
                <!-- <a-button type="primary" size="small" style="margin-left:8px" class="resetBtn" @click="aggregation">聚合</a-button>
                <a-button type="primary" size="small" style="margin-left:8px" class="yellowBtn" @click="cancelAggregation">取消聚合</a-button> -->
                <a-button type="primary" danger size="small" style="margin-left:8px" @click="deleteEntry">
                    <template #icon><DeleteOutlined /></template>删除
                </a-button>
                <!-- <a-button type="primary" size="small" class="resetBtn" style="margin-left:8px" @click="insertEntry" :loading="saveLoading">保存</a-button> -->
                <span style="margin-left:10px">过滤语言：</span>
                <a-radio-group v-model:value="filterLanguage" name="radioGroup" @change="filterLanguageChange">
                    <a-radio value="全部">全部</a-radio>
                    <a-radio value="中文">中文</a-radio>
                    <a-radio value="英文">英文</a-radio>
                </a-radio-group>
                <div style="margin-left:auto">

                    <!-- <a-button type="primary" size="small" style="margin-left:8px" @click="selectAllEntry">
                        选择全部
                    </a-button>
                    <a-button type="primary" size="small" style="margin-left:8px" @click="clearAllEntry">
                        取消选择
                    </a-button> -->
                    <a-popover
                        trigger="click"
                        placement="leftTop"
                        :overlayStyle="overlayStyle"
                    >
                        <template #content>
                            <a-checkbox-group
                                v-model:value="checkedColumn"
                                @change="changeColumn"
                            >
                                <a-row
                                    v-for="item in checkboxList"
                                    :key="item.value"
                                >
                                    <a-col :span="24">
                                        <a-checkbox :value="item.value">
                                            {{ item.label }}
                                        </a-checkbox>
                                    </a-col>
                                </a-row>
                            </a-checkbox-group>
                        </template>
                        <a-button type="primary" size="small" style="margin-left:8px"><template #icon><SettingOutlined /></template>展示列</a-button>
                    </a-popover>
                </div>
            </div>
            <a-table 
            bordered
            class="ant-table-striped"
            :columns="columns" 
            :data-source="dataSource" 
            :row-key="record => record.id"
            :scroll="tableHeight"
            :pagination='pagination'
            :loading="loading"
            :rowClassName="getRowClassName"
            :customRow="customRow"
            :expandIconColumnIndex="2"
            :row-selection="{selectedRowKeys: selectedRowKeys, 
                onChange: onSelectChange,
                selections:[
                    {key:'selectAll',text:'全部选择',onSelect:selectAllEntry},
                    {key:'clearAll',text:'取消选择',onSelect:clearAllEntry}
                ]
            }"
            ref="workTable"
            @resizeColumn="handleResizeColumn"
            @change="handleTableChange"
            >
                <template #bodyCell="{ column, text, record }">
                    <template v-if="['entry','english','russian','spanish','french'].includes(column.dataIndex)">
                        <div>
                            <template v-if="editableData[record.id]">
                                <a-form :model="editableData[record.id]" :rules="rules[record.id]" :ref="'form'+record.id.replaceAll('-','')+column.dataIndex" autocomplete="off">
                                    <a-form-item :name="column.dataIndex"
                                    >
                                        <a-input
                                            v-model:value="editableData[record.id][column.dataIndex]"
                                            style="margin: -5px 0"
                                            @pressEnter="inputPressEnter(record)"
                                        />
                                    </a-form-item>
                                </a-form>
                            </template>
                            <template v-else>
                                {{ text }}
                            </template>
                        </div>
                    </template>
                    <template v-if="['chineseInterpretation','englishInterpretation'].includes(column.dataIndex)">
                        <div>
                            <template v-if="editableData[record.id]">
                                <a-input
                                    v-model:value="editableData[record.id][column.dataIndex]"
                                    style="margin: -5px 0"
                                    @pressEnter="inputPressEnter(record)"
                                />
                            </template>
                            <template v-else>
                                {{ text }}
                            </template>
                        </div>
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
                    <template v-if="column.dataIndex === 'isExist'">
                        <template v-if="record.isExist === 0">
                            <a-badge color="#6BB8FF" /><span style="color:#6BB8FF">新建</span>
                        </template>
                        <template v-if="record.isExist === 1">
                            <a-badge color="#FBB31F" /><span style="color:#FBB31F">已存在</span>
                        </template>
                    </template>
                    <template v-if="column.dataIndex === 'entryLabel'">
                        <div>
                            <template v-if="editableData[record.id]">
                                <a-input
                                    v-model:value="editableData[record.id][column.dataIndex]"
                                    style="margin: -5px 0;width:90%"
                                    @pressEnter="inputPressEnter(record)"
                                />
                                <a-tooltip placement="top">
                                    <template #title>
                                    <span>多个Tag按分号分割！</span>
                                    </template>
                                    <InfoCircleOutlined style="margin-left:3px"/>
                                </a-tooltip>
                            </template>
                            <template v-else>
                                <!-- {{ text }} -->
                                <span>
                                    <a-tag
                                    v-for="(tag,index) in companyCut(text)"
                                    :key="index"
                                    color="cyan"
                                    >
                                        {{tag}}
                                    </a-tag>
                                </span>
                            </template>
                        </div>
                    </template>
                    <!-- <template v-else-if="column.dataIndex === 'label'">
                        <div class="editable-row-operations">
                            <span>
                                <a-checkable-tag :checked="record.auditState === 1" :class="record.auditState === 1 ? 'passTagChecked' : 'passTag' " >通过</a-checkable-tag>
                                <a-checkable-tag :checked="record.auditState === 0" :class="record.auditState === 0 ? 'rejectTagChecked' : 'rejectTag'" >驳回</a-checkable-tag>
                            </span>
                        </div>
                    </template> -->
                    <template v-else-if="column.dataIndex === 'operation'">
                        <div class="editable-row-operations">
                            <span v-if="editableData[record.id]">
                                <a-tooltip placement="top">
                                    <template #title>
                                    <span>保存</span>
                                    </template>
                                    <CheckOutlined style="color:#369FFF;margin-left:8px" @click="edit(record)"/>
                                </a-tooltip>
                                <a-tooltip placement="top">
                                    <template #title>
                                    <span>取消</span>
                                    </template>
                                    <CloseOutlined style="color:red;margin-left:8px" @click="cancel(record)"/>
                                </a-tooltip>
                            </span>
                        </div>
                    </template>
                </template>
                <template #expandIcon="props">
                    <span v-if="props.record.children != null && props.record.children.length > 0">
                        <div
                            v-if="props.expanded"
                            style="display: inline-block; margin-right: 10px"
                            @click="(e) => {props.onExpand(props.record, e);}"
                        >
                            <CaretDownOutlined />
                        </div>
                        <div
                            v-else
                            style="display: inline-block; margin-right: 10px"
                            @click="(e) => {props.onExpand(props.record, e);}"
                        >
                            <CaretRightOutlined />
                        </div>
                    </span>
                    <span v-else style="margin-right:23px"></span>
                </template>
                <!-- 设置筛选菜单 -->
                <template
                #customFilterDropdown="{ setSelectedKeys, selectedKeys, confirm, clearFilters, column }"
                >
                    <div style="padding: 8px">
                        <a-input
                        ref="searchInput"
                        :placeholder="`搜索 ${column.title}`"
                        :value="selectedKeys[0]"
                        style="width: 188px; margin-bottom: 8px; display: block"
                        @change="e => setSelectedKeys(e.target.value ? [e.target.value] : [])"
                        @pressEnter="handleSearch(selectedKeys, confirm, column.dataIndex)"
                        />
                        <a-button
                        type="primary"
                        size="small"
                        style="width: 90px; margin-right: 8px"
                        @click="handleSearch(selectedKeys, confirm, column.dataIndex)"
                        >
                        <template #icon><SearchOutlined /></template>搜索</a-button>
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
            <a-button type="primary" size="small" style="margin-left:8px;float:left" class="resetBtn" @click="aggregation">聚合</a-button>
            <a-button type="primary" size="small" style="margin-left:8px;float:left" class="yellowBtn" @click="cancelAggregation">取消聚合</a-button>
        </template>
    </CustomModal>
    <CustomModal
    :visible="createDictVisible" 
    modalTitle="新增辞典"
    @handleOK="createDictOk"
    @handleClose="createDictClose"
    style="top:30%"
    >
        <div class="condent">
            <a-form
            ref="dictRef"
            name="advanced_search"
            class="ant-advanced-search-form"
            :model="createDict"
            style="width:100%"
            >
                <a-form-item
                label="辞典名称"
                name="name"
                :rules="[{ required: true, message: '请输入辞典名称!' }]"
                >
                    <a-input
                        v-model:value="createDict.name"
                        placeholder='请输入辞典名称'
                    />
                </a-form-item>
            </a-form>
        </div>
    </CustomModal>
    <CustomModal
    :visible="templateVisible" 
    modalTitle="模板下载"
    @handleOK="templateDownload"
    @handleClose="templateClose"
    style="top:30%"
    >
        <div class="condent">
            <a-form
            ref="dictRef"
            name="advanced_search"
            class="ant-advanced-search-form"
            :model="templateObj"
            style="width:100%"
            >
                <a-form-item
                label="模板类型"
                name="type"
                :rules="[{ required: true, message: '请选择模板类型!' }]"
                >
                    <a-select v-model:value="templateObj.type" placeholder="请选择">
                        <a-select-option value="zz">装置</a-select-option>
                        <a-select-option value="common">通用</a-select-option>
                    </a-select>
                </a-form-item>
            </a-form>
        </div>
    </CustomModal>
</template>
<script>
import CustomModal from '@/components/modal/index.vue';
import { add, cloneDeep, iteratee } from 'lodash-es';
import { message ,Modal} from 'ant-design-vue';
import { defineComponent, ref, createVNode } from 'vue';
import { v4 as uuidv4 } from 'uuid';
import { TreeSelect,Table } from 'ant-design-vue';
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
    InfoCircleOutlined
} from '@ant-design/icons-vue';
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
    getDBALLEntryByDB
} from '@/http/api/i18Server';
import {
    insertEntry,
    getEntryTempByTaskID,
    importExcle,
    readZZExcle,
    deleteEntryTempByID,
    getEntryInfoList,
    updateEntryList,
    deleteEntryInfoByID,
    filterSourceLanguage
} from '@/http/api/workbench'
import{
    templateFileDownload
} from '@/http/api/download'
import workbenchCommon from '@/views/workbench/common.js';
import common from '../entry/common';
const filteredInfo = {};
export default {
    components:{
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
        VNodes: (_, { attrs }) => {
            return attrs.vnodes;
        },
    },
    emits:['handleClose','handleOK'],
    props: {
        visible:{
            type: Boolean,
            default: false
        },
        modalTitle:{
            type:String,
            default:'词条导入'
        },
        currentTask:{
            type:Object
        },
        classifyLimit:{
            type: Object
        }
    },
    
    data() {
        return{
            modalWidth:"70%",
            task:{},
            filePath:"",
            keyWords:"",
            dataType:'file',
            tableHeight: { x:'100%',y: '300px' },
            loading:false,
            columns: [
                {title: "序号",dataIndex: 'index',width:90,customRender: (text, record, index, column) => {
                    return text.index + 1
                },fixed: 'left',index:0,},
                {title: '存在状态',dataIndex: 'isExist',align:'center',width:120,fixed: 'left',index:1,
                    filteredValue: null,
                    filters: [{text: '已存在',value: 1,},{text: '新建',value: 0,}],
                    onFilter: (value, record) => record.isExist === value,
                },
                {title: 'Abbr',key:"abbr",dataIndex: 'abbr',align:'center',fixed: 'left',width:150,resizable: true,index:2},
                {title: '词条',dataIndex: 'entry',width:200,resizable: true,index:3,align:'center',},
                {title: '翻译',dataIndex: 'translate',align:'center',width:200,resizable: true,index:4},
                {title: '中文释义',dataIndex: 'chineseInterpretation',align:'center',width:200,resizable: true,index:5},
                {title: '英文释义',dataIndex: 'englishInterpretation',align:'center',width:200,resizable: true,index:6},
                // {title: 'Tag',dataIndex: 'entryLabel',align:'center',width:200},
                {title: '审核意见',dataIndex: 'auditSuggess',align:'center',width:150,resizable: true,index:99},
                // {title: '来源',dataIndex: 'entrySource',align:'center',width:150,resizable: true},
                {title: '词条状态',dataIndex: 'entryState',align:'center',width:100,fixed: 'right',index:100},
                // {title: '操作',dataIndex: 'operation',align:'center',width:50,fixed: 'right'}
            ],
            dataSource:[],
            editableData:{},
            allData:[],
            pagination:{
                pageSizeOptions:['20','50','100'],
                defaultPageSize:20,
                total:0,
                current:1,
                pageSize:20,
                showTotal:total => `共 ${total} 条`,
                onChange: this.pageChange
            },
            selectedRowKeys:[],
            selectedRows:[],
            accept:".xls,.xlsx",
            tsOptions:[],
            selectTitle:'',
            tsFile:{
                tsFileValue:[]
            },
            dataLibrary:{
                type:"field",
                table:null,
                tables:[],
                field:[],
                diFileName: null,
                selectNode:[],
                maxLength:null
            },
            configFile:{
                dict:null,
                dictOptions:[]
            },
            
            nodeOptions:[],
            serverOptions:[],
            libraryOptions:[],
            tableOptions:[],
            fieldOptions:[],
            dictionaryType:'',
            dictionaryOptions:[],
            file:{},
            treeData:[],
            saveLoading: false,
            rules:{},
            // classifyLimit:{
            //     间隔: 10
            // },
            SHOW_PARENT: TreeSelect.SHOW_PARENT,
            createDictVisible: false,
            createDict:{
                name:""
            },
            overlayStyle: workbenchCommon.overlayStyle,
            checkedColumn: workbenchCommon.checkedColumn,
            checkboxList: workbenchCommon.checkboxList,
            importBtnLoading: false,
            state:{
                searchText: '',
                searchedColumn: '',
            },
            filters: null,
            filteredData:[],
            filterLanguage:null,
            filterSource:[],
            templateVisible: false,
            templateObj:{
                type:null
            },
            platformKey:'device'
        }
    },
    
    created() {
    },
    mounted () {
        this.task = this.currentTask
    },
    watch:{
        currentTask(newval,oldval){
            this.task = newval
            this.setTranslateColumn()
        }
    },
    methods: {
        // 设置翻译列展示的语言
        setTranslateColumn(){
            this.columns.forEach(item => {
                if(item.title === '翻译'){
                    item.dataIndex = workbenchCommon.languageMap[this.task.translateType].code
                }
            })
        },
        handleOK(){
            // if(Object.keys(this.editableData).length != 0){
            //     Modal.confirm({
            //         title: '有编辑数据未保存，是否保存?',
            //         icon: createVNode(ExclamationCircleOutlined),
            //         okText: '保存',
            //         cancelText: '取消',
            //         onOk: () => {
            //             console.log("保存")
            //         },
            //         onCancel: () => {
            //             console.log("取消")
            //         }
            //     });
            // }
            // this.allData.forEach(item => {
            //     // 默认全部通过
            //     item.auditState = 1
            // })
            // insertEntry(this.allData).then((res) => {
            //     message.success('数据已保存！')
            //     this.$emit('handleClose')
            // })
            this.insertEntry()
        },
        handleClose(){
            this.$emit('handleClose')
        },
        getRowClassName(record, index){
            let className = null
            if(index % 2 === 1){
                className = 'table-striped'
                if(this.selectedRowIndex === index){
                    className = className + " highlighted-row"
                }
            }else{
                if(this.selectedRowIndex === index){
                    className = "highlighted-row"
                }
            }
            return className
        },
        handleResizeColumn: (w, col) => {
            col.width = w;
        },
        insertEntry(){
            if(this.selectedRows.length === 0){
                message.info('请勾选需要保存的词条！')
                return
            }
            // 校验字段
            let languageCode = workbenchCommon.languageMap[this.task.translateType].code

            let checkList = []
            for (let key in this.editableData) {
                let list = [eval("this.$refs.form"+ this.editableData[key].id.replaceAll('-','') + 'entry').validate(),
                        eval("this.$refs.form"+ this.editableData[key].id.replaceAll('-','') + languageCode).validate()]
                checkList = checkList.concat(list)
            }
            Promise.all(checkList).then(() => {
                // 校验成功 保存
                this.saveEntrys()
            }).catch((err) => {
                message.error('词条校验失败！')
            })
            
        },
        // 保存词条
        saveEntrys(){
            let languageCode = workbenchCommon.languageMap[this.task.translateType].code
            for (let key in this.editableData) {
				let entry = this.dataSource.find(item => item.id === key)
                // entry = this.editableData[key]
                entry.entry = this.editableData[key].entry
                entry[languageCode] = this.editableData[key][languageCode]

                entry.chineseInterpretation = this.editableData[key].chineseInterpretation
                entry.englishInterpretation = this.editableData[key].englishInterpretation
                entry.entryLabel = this.editableData[key].entryLabel

                if(entry[languageCode] != null && entry[languageCode] != null){
                    // 翻译存在  则状态为待审核状态
                    entry[languageCode+"TranslateState"] = '1'
                }
			}
            this.editableData = {}

            if(this.allData.length === 0){
                return
            }
            this.saveLoading = true

            let addArr = []
            let updateArr = []

            let notInterpretation = []

            // 保存操作 将保存所有词条(allData) 改为 保存已勾选的词条 
            this.selectedRows.forEach(item => {
                if(item.parentID != '' && item.parentID != null){
                    // 存在父id的过滤掉
                    return
                }
                
                if((item.englishInterpretation === null || item.englishInterpretation === '')
                && (item.chineseInterpretation === null || item.chineseInterpretation === '')){
                    notInterpretation.push(item)
                }
                // 聚合的子词条翻译和父一致
                if(item.children && item.children.length > 0){
                    item.children.forEach(child => {
                        child[languageCode] = item[languageCode]
                        child[languageCode+"TranslateState"] = item[languageCode+"TranslateState"]
                    })
                }

                if(item.entryState === 2){
                    item.entryState = 1
                    updateArr.push(item)
                }else if(item.entryState === 1){
                    addArr.push(item)
                }
                
            })
            if(notInterpretation.length > 0){
                Modal.confirm({
                    title: '保存数据中含有中文释义和英文释义都不存在的词条，是否继续保存?',
                    icon: createVNode(ExclamationCircleOutlined),
                    content: '',
                    okText: '是',
                    cancelText: '否',
                    style:{top:'30%'},
                    onOk: () => {
                        this.insertOrUpdateEntrys(addArr,updateArr)
                    },
                    onCancel: () => {
                        this.saveLoading = false
                    }
                });
            }else{
                this.insertOrUpdateEntrys(addArr,updateArr)
            }
        },
        // 
        insertOrUpdateEntrys(addArr,updateArr){
            this.loading = true
            let params = {
                taskID: this.task.id
            }
            if(addArr.length > 0){
                // 新增
                insertEntry(params,addArr).then((res) => {
                    message.success('数据已保存！')
                    this.saveLoading = false
                    // this.afterClose()
                    this.loading = false
                    
                    addArr.forEach(item => {
                        this.dataSource = this.dataSource.filter(d => {
                            return d.id != item.id
                        })
                    })
                    this.allData = this.dataSource
                }).catch((err) => {
                    this.saveLoading = false
                    this.loading = false
                })
            }
            if(updateArr.length > 0){
                // 编辑
                updateEntryList(params,updateArr).then((res) => {
                    message.success('数据已保存！')
                    this.saveLoading = false
                    // this.afterClose()
                    updateArr.forEach(item => {
                        this.dataSource = this.dataSource.filter(d => {
                            return d.id != item.id
                        })
                    })
                    this.allData = this.dataSource
                }).catch((err) => {
                    this.saveLoading = false
                })
            }
        },

        // 获取该任务有无审核未通过的词条
        initTaskEntry(){
            let params = {
                taskID: this.task.id,
                entryState: '2',
                entry: this.keyWords
            }
            this.loading = true
            getEntryInfoList(params,[]).then((res) => {
                if(res.data.list.length > 0){
                    this.dataSource = res.data.list
                    this.allData = this.dataSource
                }
                this.loading = false
            }).catch((err) => {
                this.loading = false
            })
        },

        beforeUpload(file, fileList){
            // console.log("before");
            if(!file.name.includes('_zz.xlsx') && !file.name.includes('_common.xlsx')){
                message.info("请选择正确的文件！")
                return
            }
            if(file.path != undefined){
                this.filePath = file.path
            }else{
                this.filePath = file.name
            }
            return false
        },
        handleChange(info){
            // console.log(info)
            this.file = info.file
        },
        // 模糊查询
        select(){
            if(this.filterLanguage === null){
                this.dataSource = this.allData.filter(item => item.entry.includes(this.keyWords))
            }else{
                this.dataSource = this.filterSource.filter(item => item.entry.includes(this.keyWords))
            }
            
        },
        onSelectChange(selectedRowKeys,selectedRows){
            this.selectedRowKeys = selectedRowKeys
            this.selectedRows = selectedRows
        },
        // 删除
        deleteEntry(){
            if(this.selectedRowKeys.length === 0){
                return
            }
            Modal.confirm({
                title: '是否确定删除?',
                icon: createVNode(ExclamationCircleOutlined),
                okText: '确定',
                cancelText: '取消',
                style:{top:'30%'},
                onOk: () => {
                    this.selectedRowKeys.forEach(id => {
                        this.dataSource = this.dataSource.filter(item => {
                            return item.id != id
                        })
                        this.allData = this.allData.filter(item => {
                            return item.id != id
                        })
                    })
                    let deleteID = []
                    this.selectedRows.forEach(item => {
                        if(item.entryState === 2){// 词条审核未通过
                            deleteID.push(item.id)
                        }
                    })
                    if(deleteID.length > 0){
                        deleteEntryInfoByID(deleteID).then((res) => {

                        })
                    }
                    message.success('已删除！')
                    this.selectedRowKeys = []
                    this.selectedRows = []
                }
            });
            
        },
        // 数据类型选择事件
        dataTypeChange(){
            this.dataSource = []
            this.allData = []
            this.pagination.current = 1
            this.pagination.pageSize = 20
            if(this.dataType === 'database'){
                // 数据库
                this.dataLibrary.table = null
                this.dataLibrary.field = []
                this.dataLibrary.tables = []
                this.dataLibrary.diFileName = null
                this.dataLibrary.maxLength = null
                this.dataLibrary.diFileName = null
                this.getAllNode()
                this.getDictionary()
            }else if(this.dataType === 'dictionary'){
                // 辞典
                this.getDictionary()

            }else if(this.dataType === 'ts'){
                // TS
                this.selectTitle = "选择文件"
                this.getTsFiles()
            }else if(this.dataType === 'config'){
                this.configFile.dict = null
                this.getDictionary()
            }else if(this.dataType === 'enum'){
                this.configFile.dict = null
                this.getDictionary()
            }
        },
        // 获取ts文件
        getTsFiles(){
            let params = {
                language: this.task.translateType
            }
            getFileListByLang(params).then((res) => {
                this.tsOptions = []
                res.data.list.forEach(item => {
                    let option = {
                        label: item,
                        value: item
                    }
                    this.tsOptions.push(option)
                })
            })
        },
        // 获取辞典文件
        getDictionary(){
            getDictionary().then((res) => {
                this.dictionaryOptions = []
                res.data.list.forEach(item => {
                    let option = {
                        label: item,
                        value: item
                    }
                    this.dictionaryOptions.push(option)
                })
                if(this.dictionaryOptions.length > 0){
                    this.dictionaryType = this.dictionaryOptions[0].value
                }
            })
        },
        // 获取数据库节点信息
        getAllNode(){
            getAllNode().then((res) => {
                this.treeData = []
                res.data.list.forEach(item => {
                    let node = {
                        id: uuidv4(),
                        pId: 0,
                        value: item,
                        title: item,
                        isLeaf: false,
                        type: 'node',
                        key: item
                    }
                    this.treeData.push(node)
                })
            })
        },
        // treeData加载子数据
        onLoadData(node){
            // console.log("onLoad",node)
            const id = node.dataRef.id;
            const type = node.dataRef.type;
            return new Promise(resolve => {
                if(type === 'node'){
                    // 获取应用
                    let params = {
                        nodeName: node.dataRef.value
                    }
                    getAppByNode(params).then((res) => {
                        res.data.list.forEach(item => {
                            const newId = uuidv4();
                            let app = {
                                id: newId,
                                pId: id,
                                value: newId,
                                key: newId,
                                title: item.name,
                                type: 'app',
                                appId: item.type,
                                node: node.value,
                                isLeaf: false
                            }
                            this.treeData.push(app)
                        })
                        
                    })
                }else if(type === 'app'){
                    // 获取库
                    // console.log(node.dataRef)
                    let params = {
                        nodeName: node.dataRef.node,
                        appName: node.dataRef.title,
                        modeType: node.dataRef.appId
                    }
                    getdbByApp(params).then((res) => {
                        res.data.list.forEach(item => {
                            const newId = uuidv4();
                            let temp = {
                                id: newId,
                                pId: id,
                                value: newId,
                                key: newId,
                                title: item,
                                type: 'db',
                                node: node.dataRef.node,
                                app: node.dataRef.title,
                                appId: node.dataRef.appId,
                                isLeaf: this.dataLibrary.type === 'field' ? false : true 
                            }
                            this.treeData.push(temp)
                        })
                    })
                }else if(type === 'db'){
                    // 获取表
                    let params = {
                        nodeName: node.dataRef.node,
                        appName: node.dataRef.app,
                        dbName: node.dataRef.title
                    }
                    getTableByApp(params).then((res) => {
                        res.data.list.forEach(item => {
                            const newId = uuidv4();
                            let table = {
                                id: newId,
                                pId: id,
                                value: newId,
                                key: newId,
                                tableId: item.tableId,
                                title: item.tableName,
                                type: 'table',
                                node: node.dataRef.node,
                                app: node.dataRef.app,
                                db: node.dataRef.title,
                                isLeaf: true
                            }
                            this.treeData.push(table)
                        })
                        
                    })
                }
               resolve(true);
            })
            
        },
        // 树选择事件
        treeSelect(value, node, extra){
            // console.log("treeSelect",node)
            if(node.isLeaf){
                this.dataLibrary.table = value

                if(this.dataLibrary.type === 'field'){
                    let params = {
                        dbName: node.db,
                        nodeName: node.node,
                        appName: node.app,
                        tbName: node.title
                    }
                    this.fieldOptions = []
                    this.dataLibrary.field = []
                    getFieldByTable(params).then((res) => {
                        res.data.list.forEach(item => {
                            let table = {
                                label: item.fieldName,
                                value: item.fieldID,
                                size: item.size
                            }
                            this.fieldOptions.push(table)
                        })
                    })
                }
            }else{
                this.dataLibrary.table = null
            }
        },
        // 树批量选择事件
        treeBatchSelect(value, node, extra){
            // console.log(extra)
            // console.log(node)
        },
        // tabs切换
        changeDataLibraryType(activeKey){
            this.treeData = []
            this.dataLibrary.table = null
            this.dataLibrary.field = []
            this.getAllNode()
        },
        // 全选字段
        selectAllField(){
            this.dataLibrary.field = []
            this.dataLibrary.field = this.fieldOptions.map((item,index)=>{
                return item.value
            })
        },
        // 清空选中的表
        clearAllField(){
            this.dataLibrary.field = []
        },
         // 全选ts文件
        selectAllTs(){
            this.tsFile.tsFileValue = []
            this.tsFile.tsFileValue = this.tsOptions.map((item,index)=>{
                return item.value
            })
        },
        // 清空选中的ts文件
        clearAllTs(){
            this.tsFile.tsFileValue = []
        },
        // 导入词条数据
        importEntryData(){
            this.loading = true
            this.importBtnLoading = true
            if(this.dataType === 'ts'){
                // ts文件导入
                this.$refs.tsFormRef.validate().then(() => {
                    let params = {
                        taskID: this.task.id,
                        translateType: this.task.translateType
                    }
                    getTsWords(params,this.tsFile.tsFileValue).then((res) => {
                        this.dataSource = res.data.list
                        this.sortArray(this.dataSource,'isExist')
                        this.dataSource.forEach(item => {
                            item.auditState = 1
                        })
                        this.allData = this.dataSource
                        this.loading = false
                        this.importBtnLoading = false
                    }).catch((err) => {
                        message.error("数据获取失败！")
                        this.loading = false
                        this.importBtnLoading = false
                    })
                }).catch((err) => {
                    this.loading = false
                    this.importBtnLoading = false
                })
                
            }else if(this.dataType === 'dictionary'){
                if(this.dictionaryType === '' || this.dictionaryType === null){
                    this.loading = false
                    this.importBtnLoading = false
                    return
                }
                // 辞典文件导入
                let params = {
                    type: this.dictionaryType,
                    taskID: this.task.id,
                    versionID: this.task.versionId,
                    transType : this.task.translateType, 
                }
                getDictionaryEntry(params).then((res) => {
                    this.dataSource = res.data.list
                    this.sortArray(this.dataSource,'isExist')
                    this.allData = this.dataSource
                    this.loading = false
                    this.importBtnLoading = false
                }).catch((err) => {
                    message.error("数据获取失败！")
                    this.loading = false
                    this.importBtnLoading = false
                })
            }else if(this.dataType === 'database'){
                // 数据库导入
                if(this.dataLibrary.type === 'field'){
                    // 字段
                    this.$refs.fieldFormRef.validate().then(() => {
                        this.getFieldData()
                    }).catch((err) => {
                        this.loading = false
                        this.importBtnLoading = false
                    })
                }else if(this.dataLibrary.type === 'alias'){
                    // 元数据
                    this.$refs.aliasFormRef.validate().then(() => {
                        this.getAlias()
                    }).catch((err) => {
                        this.loading = false
                        this.importBtnLoading = false
                    })
                }else if(this.dataLibrary.type === 'allData'){
                    // 全量
                    this.$refs.allDataFormRef.validate().then(() => {
                        this.batchImportDatabase()
                    }).catch((err) => {
                        this.loading = false
                        this.importBtnLoading = false
                    })
                    
                }
                
            }else if(this.dataType === 'file'){
                // console.log(this.file)
                if(Object.keys(this.file).length === 0){
                    this.loading = false
                    this.importBtnLoading = false
                    message.info("请选择文件！")
                    return
                }
                // 文件导入
                let formData = new FormData()
                formData.append('file',this.file)
                formData.append('taskID',this.task.id)
                this.loading = true
                readZZExcle(formData).then((res) => {
                    this.dataSource = res.data.list
                    this.sortArray(this.dataSource,'isExist')
                    this.allData = this.dataSource
                    this.loading = false
                    this.importBtnLoading = false
                }).catch((err) => {
                    message.error("导入失败！")
                    this.loading = false
                    this.importBtnLoading = false
                })
            }else if(this.dataType === 'config'){
                // 配置文件数据导入
                this.$refs.configFormRef.validate().then(() => {
                    let params = {
                        diFileName: this.configFile.dict,
                        taskID: this.task.id,
                        versionID: this.task.versionId ? this.task.versionId : "",
                        translateType : this.task.translateType, 
                    }
                    getConfigEntry(params).then((res) => {
                        this.dataSource = res.data.list
                        this.sortArray(this.dataSource,'isExist')
                        this.allData = this.dataSource
                        this.loading = false
                        this.importBtnLoading = false
                    }).catch((err) => {
                        this.loading = false
                        this.importBtnLoading = false
                        message.error("数据获取失败！")
                    })
                }).catch((err) => {
                    this.loading = false
                    this.importBtnLoading = false
                })
            }else if(this.dataType === 'enum'){
                // 配置文件数据导入
                this.$refs.configFormRef.validate().then(() => {
                    let params = {
                        diFileName: this.configFile.dict,
                        taskID: this.task.id,
                        versionID: this.task.versionId ? this.task.versionId : "",
                        translateType : this.task.translateType, 
                    }
                    getEnumEntry(params).then((res) => {
                        this.dataSource = res.data.list
                        this.sortArray(this.dataSource,'isExist')
                        this.allData = this.dataSource
                        this.loading = false
                        this.importBtnLoading = false
                    }).catch((err) => {
                        this.loading = false
                        this.importBtnLoading = false
                        message.error("数据获取失败！")
                    })
                }).catch((err) => {
                    this.loading = false
                    this.importBtnLoading = false
                })
            }
            
        },
        // 排序
        sortArray(arr,key){
            return arr.sort((a,b) => {
                let x = a[key]
                let y = b[key]
                return ((x>y) ? -1 : (x<y) ? 1 : 0)
            })
        },
        // 获取字段内容
        getFieldData(){
            let table = this.treeData.find(item => item.id === this.dataLibrary.table)
            let params = {
                dbName: table.db,
                appName: table.app,
                nodeName: table.node,
                tbID: table.tableId,
                tbName: table.title,
                taskID: this.task.id,
                versionID: this.task.versionId ? this.task.versionId : "",
                translateType: this.task.translateType,
                diFileName: this.dataLibrary.diFileName
            }
            let data = []
            this.dataLibrary.field.forEach(fieldId => {
                let fieldObj = this.fieldOptions.find(item => item.value === fieldId)
                let field = {
                    fieldName: fieldObj.label,
                    fieldID: fieldObj.value,
                    size: fieldObj.size
                }
                data.push(field)
            })
            getFieldData(params,data).then((res) => {
                this.dataSource = res.data.list
                this.sortArray(this.dataSource,'isExist')
                this.allData = this.dataSource
                this.loading = false
                this.importBtnLoading = false
            }).catch((err) => {
                this.loading = false
                this.importBtnLoading = false
                message.error("数据获取失败！")
            })
        },
        // 获取别名
        getAlias(){
            let table = this.treeData.find(item => item.id === this.dataLibrary.table)
            let params = {
                dbName: table.title,
                appName: table.app,
                nodeName: table.node,
                taskID: this.task.id,
                versionID: this.task.versionId ? this.task.versionId : "",
                translateType: this.task.translateType,
                diFileName: this.dataLibrary.diFileName,
                maxLength: this.dataLibrary.maxLength
            }
            getAlias(params).then((res) => {
                this.dataSource = res.data.list
                this.sortArray(this.dataSource,'isExist')
                this.allData = this.dataSource
                this.loading = false
                this.importBtnLoading = false
            }).catch((err) => {
                this.loading = false
                this.importBtnLoading = false
                message.error("数据获取失败！")
            })
        },
        // 批量导入数据库
        batchImportDatabase(){
            this.dataSource = []
            this.allData = []
            let selectNode = []
            this.dataLibrary.tables.forEach(item => {
                let node = this.treeData.find(d => d.value === item)
                selectNode.push(node)
            })
            if(selectNode.length === 0){
                this.loading = false
                this.importBtnLoading = false
                return
            }
            let nodes = []
            let apps = []
            let dbs = []
            let tables = []

            selectNode.forEach(item => {
                if(item.type === 'node'){
                    nodes.push(item)
                }else if(item.type === 'app'){
                    apps.push(item)
                }else if(item.type === 'db'){
                    dbs.push(item)
                }else if(item.type === 'table'){
                    tables.push(item)
                }
            })
            // console.log("nodes:",nodes)
            // console.log("apps:",apps)
            // console.log("dbs:",dbs)
            // console.log("tables:",tables)
            this.dataSource = []
            let params = {
                taskID: this.task.id,
                versionID: this.task.versionId ? this.task.versionId : "",
                translateType: this.task.translateType,
                diFileName: this.dataLibrary.diFileName
            }
            if(nodes.length > 0){
                nodes.forEach(item => {
                    item.node = item.value
                })
                getDBALLEntryByNode(params,nodes).then((res) => {
                    this.dataSource = this.dataSource.concat(res.data.list)
                    this.sortArray(this.dataSource,'isExist')
                    this.allData = this.dataSource
                    this.loading = false
                    this.importBtnLoading = false
                })
            }
            if(apps.length > 0){
                apps.forEach(item => {
                    item.modeType = item.appId
                    item.app = item.title
                })
                getDBALLEntryByApp(params,apps).then((res) => {
                    this.dataSource = this.dataSource.concat(res.data.list)
                    this.sortArray(this.dataSource,'isExist')
                    this.allData = this.dataSource
                    this.loading = false
                    this.importBtnLoading = false
                })
            }
            if(dbs.length > 0){
                dbs.forEach(item => {
                    item.db = item.title
                    item.modeType = item.appId
                })
                getDBALLEntryByDB(params,dbs).then((res) => {
                    this.dataSource = this.dataSource.concat(res.data.list)
                    this.sortArray(this.dataSource,'isExist')
                    this.allData = this.dataSource
                    this.loading = false
                    this.importBtnLoading = false
                })
            }
        },
        // 添加表格行点击事件
        customRow(record, index){
            return {
                onDblclick: (event) => {
                    if(this.editableData.hasOwnProperty(record.id)){
                        // 当前行在编辑状态
                        return
                    }
                    this.editableData[record.id] = cloneDeep(this.dataSource.filter(item => record.id === item.id)[0])
                    // 设置校验规则
                    this.rules[record.id] = {
                        entry:[{ validator: this.vilidFildLength(record,'chinese') },
                        { required: true, message: '请输入!' }]
                    }
                    let languageCode = workbenchCommon.languageMap[this.task.translateType].code
                    this.rules[record.id][languageCode] = [{ validator: this.vilidFildLength(record,languageCode) }]
                }
            }
        },
        // 校验输入数据的长度
        vilidFildLength(record,language){
            return (rule,value) =>{
                let type = ""
                if(language === 'chinese'){
                    type = 'maxByte'
                }else{
                    type = 'foreignMaxByte'
                }
                let maxLength = null
                if(this.classifyLimit[record.classfy1] === undefined || this.classifyLimit[record.classfy1] === null){
                    if(record.maxLength != null && record.maxLength != ""){
                        maxLength = record.maxLength
                    }else{
                        return Promise.resolve();
                    }
                }else{
                    maxLength = this.classifyLimit[record.classfy1][type]
                }
                if(maxLength === undefined || maxLength === null || maxLength === 0){
                    return Promise.resolve();
                }
                // 获取输入数据的长度
                let length = common.byteLength(value)
                if(length > maxLength){
                    return Promise.reject('允许最大字符数为'+maxLength+'！');
                }
                return Promise.resolve();
            }
        },
        // 输入框 回车事件
        inputPressEnter(record){
            record.chineseInterpretation =  this.editableData[record.id].chineseInterpretation
            record.englishInterpretation =  this.editableData[record.id].englishInterpretation
            record.entryLabel = this.editableData[record.id].entryLabel

            let languageCode = workbenchCommon.languageMap[this.task.translateType].code
            // 长度校验
            let list = [eval("this.$refs.form"+ record.id.replaceAll('-','') + 'entry').validate(),
                        eval("this.$refs.form"+ record.id.replaceAll('-','') + languageCode).validate()]
            Promise.all(list).then(() => {
                record[languageCode] = this.editableData[record.id][languageCode]
                record.entry = this.editableData[record.id].entry
                if(record[languageCode] != null && record[languageCode] != null){
                    // 翻译存在  则状态为待审核状态
                    record[languageCode+"TranslateState"] = '1'
                }
                delete this.editableData[record.id]
            }).catch((err) => {
                
            })
        },
        // 编辑
        edit(record){
            record.translate = this.editableData[record.id].translate
            delete this.editableData[record.id]
            this.deleteOperationColumns()
        },
        // 取消编辑
        cancel(record){
            delete this.editableData[record.id]
            this.deleteOperationColumns()
        },
        // 删除操作列
        deleteOperationColumns(){
            if(Object.keys(this.editableData).length === 0){
                this.columns = this.columns.filter(item => {
                    return item.dataIndex != 'operation'
                })
            }
        },
        // 聚合
        aggregation(){
            if(this.selectedRows.length < 2){
                message.warn("请选择两条及以上词条聚合！")
            }
            let children = []
            for(let i = 1; i < this.selectedRows.length; i++){
                let child = this.selectedRows[i]
                if(child.children && child.children.length > 0){
                    child.children.forEach(item => {
                        children.push(item)
                    })
                }
                child.children = []
                children.push(child)
            }
            children.forEach(item => {
                item.parentID = this.selectedRows[0].id

                this.dataSource = this.dataSource.filter(data => data.id != item.id)
            })
            if(this.selectedRows[0].children){
                this.selectedRows[0].children = this.selectedRows[0].children.concat(children)
            }else{
                this.selectedRows[0].children = children
            }
            
            this.allData = this.dataSource
            this.selectedRowKeys = []
            this.selectedRows = []
        },
        // 取消聚合
        cancelAggregation(){
            // console.log(this.selectedRows)
            this.selectedRows.forEach(item => {
                if((item.parentID === '' || item.parentID === null) && item.children){
                    let index = this.dataSource.findIndex(entry => entry.id === item.id)
                    for(let i = 0; i < item.children.length; i++){
                        let child = item.children[i]
                        child.parentID = ""
                        this.dataSource.splice(index + i + 1,0,child)
                    }
                    item.children = []
                }else{
                    let parent = this.dataSource.find(data => data.id === item.parentID)
                    parent.children = parent.children.filter(child => child.id != item.id)
                    let index = this.dataSource.findIndex(data => data.id === item.parentID)
                    item.parentID = ""
                    this.dataSource.splice(index + 1, 0 , item)
                }
            })
            this.allData = this.dataSource
            this.selectedRowKeys = []
            this.selectedRows = []
        },
        afterClose(){
            this.editableData = {}
            this.keyWords = ""
            this.dataType = 'file'
            this.dataSource = []
            this.allData = []
            this.tsFile.tsFileValue = []
            this.file = {}
            this.filePath = ""
            this.dataLibrary = {
                node:null,
                server:null,
                library:null,
                table:[],
                type:"field",
                maxLength: null
            }
            this.pagination.current = 1
            this.pagination.pageSize = 20
            this.clearFilters()
            
        },
        
        // 创建辞典
        createDictionary(){
            this.createDictVisible = true
            this.createDict.name = ""
        },
        createDictOk(){
            this.$refs.dictRef.validate().then(() => {
                let params = {
                    dicName: this.createDict.name
                }
                createDic(params).then((res) => {
                    message.success("创建成功！")
                    this.createDictVisible = false
                    this.getDictionary()
                }).catch((err) => {
                    message.error("创建失败！")
                })
            })
        },
        createDictClose(){
            this.createDictVisible = false
        },
        changeColumn(checkedValue) {
            this.checkedColumn = checkedValue;
            this.checkboxList.forEach((value) => {
                let checkedIndex = this.checkedColumn.findIndex(
                    (item) => item === value.value
                );
                let nowColumnIndex = this.columns.findIndex(
                    (item) => item.dataIndex === value.value
                );
                if (
                    (nowColumnIndex !== -1 && checkedIndex !== -1) ||
                    (nowColumnIndex === -1 && checkedIndex === -1)
                ) {
                    return;
                }
                if (nowColumnIndex === -1 && checkedIndex !== -1) {
                    let newCol = {
                        title: value.label,
                        dataIndex: value.value,
                        align: "center",
                        width: 200,
                        resizable: true,
                        index: value.index
                    };
                    if(newCol.dataIndex === 'abbr'){
                        newCol.fixed = 'left'
                    }
                    if(newCol.dataIndex === 'entrySource'){
                        // 添加词条来源可筛选
                        newCol.width = 250,
                        newCol.customFilterDropdown = true
                        newCol.filteredValue = null
                        newCol.onFilter = eval('(value, record) => record.entrySource.toString().toLowerCase().includes(value.toLowerCase())')
                    }
                    this.columns.splice(-1, 0, newCol);
                }
                if (nowColumnIndex !== -1 && checkedIndex === -1) {
                    this.columns.splice(nowColumnIndex, 1);
                }
            });
            this.columns.sort(function(a, b){
                return a.index - b.index
            })
        },
        // 列筛选
        handleSearch(selectedKeys, confirm, dataIndex){
            confirm();
            this.state.searchText = selectedKeys[0];
            this.state.searchedColumn = dataIndex;
        },
        handleReset(clearFilters){
            clearFilters({ confirm: true });
            this.state.searchText = '';
        },
        // 动态设置表格高度
        setTableHeight(height,type){
            if(type === 'full'){
                this.tableHeight.y = height - 370
            }else if(type === 'reduce'){
                this.tableHeight.y = '300px'
            }
            
        },
        // 模板下载
        templateFileDownload(){
            this.templateVisible = true
        },
        // 模板下载
        templateDownload(){
            this.$refs.dictRef.validate().then(() => {
                let params = {
                    fileType:this.templateObj.type
                }
                templateFileDownload(params).then((res) => {
                    let fileName = res.headers["content-disposition"].split(";")[1].split("filename=")[1]
                    let contentType = res.headers['content-type']
                    const blob = new Blob([res.data], {type: contentType})
                    const a = document.createElement('a')
                    a.download = decodeURI(fileName)
                    a.href = window.URL.createObjectURL(blob)
                    a.click()
                    a.remove()
                })
            })
        },
        templateClose(){
            this.templateVisible = false
            this.templateObj.type = null
        },
        // 分页切换
        pageChange(page,pageSize){
            this.pagination.current = page
            this.pagination.pageSize = pageSize
        },
        // 语言切换
        filterLanguageChange(){
            if(this.filterLanguage === '全部'){
                this.dataSource = this.allData
                this.filterSource = this.allData
            }else{
                let params = {
                    languageType: this.filterLanguage
                }
                this.loading = true
                filterSourceLanguage(params,this.allData).then((res) => {
                    this.dataSource = res.data.list
                    this.filterSource = res.data.list
                    this.loading = false
                }).catch((err) => {
                    this.loading = false
                })
            }
        },
        // 表格change事件
        handleTableChange(pagination, filters) {
            this.filters = filters
            for (let key in filters) {
                this.columns.forEach(col => {
                    if(col.dataIndex === key){
                        col.filteredValue = filters[key]
                    }
                })
			}
            // 获取筛选后的数据
            let isExistData = this.dataSource.filter(item => {
                return filters.isExist && filters.isExist.includes(item.isExist);
            });
            let sourceData = this.dataSource.filter(item => {
                return filters.entrySource && item.entrySource.includes(filters.entrySource);
            });
            this.filteredData = this.intersection(isExistData,sourceData)
        },
        // 两个数组取并集
        intersection(nums1, nums2) {
            if(nums1.length === 0){
                return nums2
            }
            if(nums2.length === 0){
                return nums1
            }
            let a=new Set(nums1);
            let b=new Set(nums2);
            let arr = Array.from(new Set([...b].filter(x => a.has(x))));
            return arr;
        },
        // 清空表格筛选条件
        clearFilters(){
            if(this.filters){
                for (let key in this.filters) {
                    this.columns.forEach(col => {
                        if(col.dataIndex === key){
                            col.filteredValue = null
                        }
                    })
                }
            }
        },
        selectAllEntry(){
            this.selectedRowKeys = []
            this.selectedRows = []
            if(this.filters && (this.filters.isExist || this.filters.entrySource)){
                this.filteredData.forEach(item => {
                    this.selectedRowKeys.push(item.id)
                    this.selectedRows.push(item)
                })
            }else{
                this.dataSource.forEach(item => {
                    this.selectedRowKeys.push(item.id)
                    this.selectedRows.push(item)
                })
            }
        },
        clearAllEntry(){
            this.selectedRowKeys = []
            this.selectedRows = []
        },
        // 切割字符串
        companyCut(message){
            let res = []
            if(message === null || message === ''){
                return res
            }
            const regex = /[;；]/;
            res = message.split(regex)
            res = res.filter(item => item != '')
            return res
        },
    }
}
</script>
<style scoped lang="less">
.ant-divider{
    margin: 15px 0;
}
.content{
    width: 100%;
    height: 100%;
    padding: 10px;
    background-color: #F3F3F3;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    align-self: stretch;

    .taskInfo{
        display: flex;
        padding: 4px 0px;
        align-items: center;
        gap: 32px;
        align-self: stretch;

        .taskItem{
            display: flex;
            align-items: center;
            flex: 1 0 0;
        }
    }
    .platformBox{
        width: 100%;
        background-color: white;
        padding: 0px 16px 16px 16px;
        border-radius: 4px;
    }


    .dataTypeBox{
        // display: flex;
        // align-items: center;
        // align-self: stretch;
        width: 100%;
        border-radius: 4px;
        background-color: white;
        padding: 16px;
        border: 1px solid #f0f0f0;
        :deep(.ant-tabs-nav){
            margin-bottom: 10px;
        }
    }
    .form{
        display: flex;
        align-items: center;
        align-self: stretch;
        width: 100%;
    }
    .ant-row{
        height: 38px;
    }

    .rejectBtn{
        background:#FBB31F;
        border-color:#FBB31F
    }
    .rejectBtn:hover{
        background:#FBB31F;
        border-color:#FBB31F
    }
    .rejectBtn:focus{
        background:#FBB31F;
        border-color:#FBB31F
    }
    .passTag{
        border: 1px solid #36BF7D;
        color: #36BF7D;
    }
    .passTagChecked{
        background-color: #36BF7D;
        color: white;
    }
    .rejectTag{
        border: 1px solid #FBB31F;
        color: #FBB31F;
    }
    .rejectTagChecked{
        background-color: #FBB31F;
        color: white;
    }
}
.ant-form-item{
    margin-bottom: 0%;
}
:deep(.ant-pagination) {
    margin: 8px 0;
}
</style>