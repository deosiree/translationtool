<template>
  <div class="productEntryBox" ref="productEntryBox">
    <SearchBox ref="search" @change="setTableHeight" :operate="false">
      <template v-slot:form>
        <a-form
          :model="search"
          layout="inline"
          autocomplete="off"
          :label-col="labelCol"
        >
          <a-row
            class="search-row"
            style="width: 100%; display: flex; gap: 8px"
          >
            <a-form-item
              v-if="checkedSearchCondition.includes('entry')"
              label="词条"
              name="entry"
            >
              <a-textarea
                v-model:value="search.entry"
                placeholder="请输入内容"
                :auto-size="{ minRows: 1 }"
              ></a-textarea>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('state')"
              label="词条状态"
              name="state"
            >
              <EntryStateSelect
                :entryState="search.entryState"
                @update:entryState="search.entryState = $event"
                :showForbbiden="showForbbiden"
                @update:showForbbiden="showForbbiden = $event"
              />
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('tag')"
              label="tag"
              name="tag"
            >
              <a-input
                v-model:value="search.tag"
                placeholder="请输入内容"
              ></a-input>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('classfy1')"
              label="一级分类"
              name="classfy1"
            >
              <a-select
                v-model:value="search.classfy1"
                mode="tags"
                placeholder="请输入一级分类"
                :fieldNames="{ label: 'title', value: 'title' }"
                :options="classify1Option"
                allowClear
                :maxTagTextLength="3"
                :maxTagCount="1"
              >
              </a-select>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('classfy2')"
              label="二级分类"
              name="classfy2"
            >
              <a-select
                v-model:value="search.classfy2"
                mode="tags"
                placeholder="请输入二级分类"
                :fieldNames="{ label: 'title', value: 'title' }"
                :options="classify2Option"
                allowClear
                :maxTagTextLength="3"
                :maxTagCount="1"
              >
              </a-select>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('entrySource')"
              label="词条来源"
              name="entrySource"
            >
              <!-- <a-input v-model:value="search.entrySource" placeholder="请输入内容"></a-input> -->
              <a-select
                v-model:value="search.entrySource"
                show-search
                placeholder="请输入词条来源"
                :options="entrySourceOptions"
                allowClear
                @search="handleEntrySourceSearch"
              >
              </a-select>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('language')"
              label="翻译语种"
              name="language"
            >
              <a-select
                v-model:value="search.language"
                placeholder="请选择"
                :fieldNames="{ label: 'name', value: 'name' }"
                :options="translateTypes"
                allowClear
              >
              </a-select>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('translateState')"
              label="翻译状态"
              name="translateState"
            >
              <TransStateSelect
                :translateState="search.translateState"
                @update:translateState="search.translateState = $event"
              />
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('translate')"
              label="翻译结果"
              name="translate"
            >
              <a-input
                v-model:value="search.translate"
                placeholder="请输入内容"
              ></a-input>
            </a-form-item>
            <!-- <a-form-item v-if="checkedSearchCondition.includes('filter_translate')" label="翻译过滤" name="filter_translate">
              <a-input v-model:value="search.filter_translate" placeholder="请输入内容"></a-input>
            </a-form-item> -->
            <!-- <a-form-item label="翻译释义" name="interpretation">
              <a-input v-model:value="search.interpretation" placeholder="请输入内容"></a-input>
            </a-form-item> -->
            <a-form-item
              v-if="checkedSearchCondition.includes('comment')"
              label="comment"
              name="comment"
            >
              <a-input
                v-model:value="search.comment"
                placeholder="请输入内容"
              ></a-input>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('diFileName')"
              label="辞典名称"
              name="diFileName"
            >
              <a-select
                v-model:value="search.diFileName"
                show-search
                placeholder="请输入辞典名称"
                :options="diFileNameOptions"
                allowClear
                @search="handleDiFileNameSearch"
              >
              </a-select>
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('startTime')"
              label="开始时间"
              name="startTime"
            >
              <a-date-picker
                v-model:value="search.startTime_"
                style="width: 186px"
              />
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('endTime')"
              label="结束时间"
              name="endTime"
            >
              <a-date-picker
                v-model:value="search.endTime_"
                style="width: 186px"
              />
            </a-form-item>
            <a-form-item
              v-if="checkedSearchCondition.includes('update')"
              label="修改人"
              name="update"
            >
              <a-input
                v-model:value="search.update"
                placeholder="请输入内容"
              ></a-input>
            </a-form-item>
            <a-form-item
              v-if="
                checkedSearchCondition.includes('searchType') &&
                currentProduct.key &&
                $currentDepartment &&
                $currentDepartment.ops.has('needBranch')
              "
              label="校验类型"
              name="searchType"
            >
              <a-select
                v-model:value="search.searchType"
                style="width: 186px"
                placeholder="请选择校验类型"
                :options="searchTypes"
                allowClear
              >
              </a-select>
            </a-form-item>
            <a-form-item
              v-if="search.searchType == 'checkNotUseEntry'"
              label="i18nURL"
              name="i18nURL"
            >
              <a-select
                v-model:value="search.i18nURL"
                style="width: 186px"
                placeholder="请选择i18nURL"
                :options="ipOptions"
                allowClear
              >
              </a-select>
            </a-form-item>
          </a-row>
          <a-row
            style="width: 100%; margin-top: 8px; display: flex; gap: 8px"
            class="search-row"
            justify="end"
          >
            <a-button
              type="primary"
              size="middle"
              v-if="admin"
              :danger="!showForbbiden"
              :class="{ yellowBtn: showForbbiden }"
              @click="changeForbbiden"
            >
              {{ showForbbiden ? "隐藏禁用" : "显示禁用" }}
            </a-button>
            <a-button
              type="primary"
              size="middle"
              class="resetBtn"
              @click="reset"
              >重置</a-button
            >
            <a-button type="primary" size="middle" @click="conditionalQuery"
              >查询</a-button
            >
            <AccurSearchButton
              v-if="
                !search.searchType || search.searchType == 'getEntryByClassfy'
              "
              size="middle"
              buttonTitle="全量查询"
              :fieldOptions="exportFields"
              @update:accurSearch="accurSearch = $event"
              @searchFunction="conditionalQuery($event)"
            />
            <a-button
              type="primary"
              size="middle"
              class="yellowBtn"
              v-if="search.hasRedundantRls"
              @click="reGetCheckNotUseEntry"
              >重新查询</a-button
            >
            <a-popover
              trigger="click"
              placement="leftTop"
              :overlayStyle="overlayStyle"
            >
              <template #content>
                <a-checkbox-group
                  v-model:value="checkedSearchCondition"
                  @change="changeSearchCondition"
                >
                  <a-row v-for="item in searchConditionList" :key="item.value">
                    <a-col :span="24">
                      <a-checkbox :value="item.value">
                        {{ item.label }}
                      </a-checkbox>
                    </a-col>
                  </a-row>
                </a-checkbox-group>
              </template>
              <a-button type="primary" size="middle" ghost
                ><template #icon>
                  <SettingOutlined /> </template
                >展示条件</a-button
              >
            </a-popover>
          </a-row>
        </a-form>
      </template>
    </SearchBox>
    <DataBox :title="tableTitle" :height="dataHeight" :showOperate="true">
      <template v-if="isProduct()" v-slot:label>
        产品版本：
        <a-select
          v-model:value="currentVersion"
          style="width: 150px"
          placeholder="请选择版本"
          :options="productVersions"
          :fieldNames="{ label: 'name', value: 'id' }"
          size="small"
          @change="changeVersion"
          allowClear
        >
        </a-select>
      </template>
      <template v-slot:operate>
        <div ref="button" style="margin-bottom: 8px; display: flex; gap: 10px">
          <GitCommitButton
            v-if="$currentDepartment && $currentDepartment.ops.has('needIP')"
            size="small"
            buttonTitle="git推送"
            buttonClass="yellowBtn"
            :treeTitle="product.title"
          />
          <a-button
            type="primary"
            size="small"
            @click="createVersion"
            v-if="!createVersionFlag"
            >批量选择</a-button
          >
          <a-button
            type="primary"
            size="small"
            @click="selectAllEntry"
            v-if="createVersionFlag"
            :loading="selectAllLoading"
            >选择全部</a-button
          >
          <a-button
            type="primary"
            size="small"
            @click="cancelCreate"
            class="yellowBtn"
            v-if="createVersionFlag"
            >取消选择</a-button
          >
          <a-badge
            :count="selectEntry.length"
            :overflow-count="99"
            v-if="createVersionFlag"
          >
            <a-button
              type="primary"
              size="small"
              class="resetBtn"
              @click="viewCreateVersionEntry"
              >已选词条</a-button
            >
          </a-badge>

          <!-- <a-button type="primary" size="small" @click="viewDictionary" v-if="user.department === '通用平台部' || user.department === '监控系统部'">查看辞典</a-button> -->
          <a-button
            type="primary"
            size="small"
            :disabled="!isProduct()"
            @click="addEntry"
          >
            <template #icon>
              <PlusOutlined /> </template
            >新增
          </a-button>

          <!-- <a-button type="primary" size="small" danger @click="deleteEntry" v-if="edit"><template #icon><DeleteOutlined /></template>删除</a-button> -->
          <!-- <a-button type="primary" size="small" @click="batchSave" v-if="edit"><template #icon><SaveOutlined /></template>保存</a-button> -->
          <!-- <a-button type="primary" size="small" class="resetBtn" ><template #icon><UpSquareOutlined /></template>升级</a-button> -->
          <a-button
            type="primary"
            size="small"
            @click="setSecondClassify"
            v-if="admin"
            :disabled="!isProduct()"
            >二级分类管理</a-button
          >
          <!-- <BackFillModal v-if="admin" mode="button" :translateTypes="translateTypes" :showFileTypeSelect="true"
            :defaultAccept="'.csv'" size="small" buttonTitle="更新翻译 v1" modalTitle="更新翻译 v1" @importSuccess="refreshTable" />
          <BackFillModal_v2 v-if="admin" mode="button" :showFileTypeSelect="true" :defaultAccept="'.csv'" size="small"
            buttonTitle="更新翻译 v2" modalTitle="更新翻译 v2" :functionMode="'updateTranslation'"
            @importSuccess="refreshTable" /> -->
          <BackFillModal_v2_5
            v-if="
              admin &&
              $currentDepartment &&
              $currentDepartment.ops.has('needIP')
            "
            mode="button"
            buttonTitle="更新翻译"
            modalTitle="更新翻译"
            size="small"
            :showFileTypeSelect="true"
            :defaultAccept="'.csv'"
            @importSuccess="refreshTable"
          />
          <BackFillModal_v3
            v-if="
              admin &&
              $currentDepartment &&
              $currentDepartment.ops.has('needIP')
            "
            mode="button"
            buttonTitle="更新翻译(异步)"
            modalTitle="更新翻译(异步)"
            size="small"
            :showFileTypeSelect="true"
            :defaultAccept="'.csv'"
            @importSuccess="refreshTable"
          />

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
                <a-row v-for="item in checkboxList" :key="item.value">
                  <a-col :span="24">
                    <a-checkbox :value="item.value">
                      {{ item.label }}
                    </a-checkbox>
                  </a-col>
                </a-row>
              </a-checkbox-group>
            </template>
            <a-button type="primary" size="small"
              ><template #icon>
                <SettingOutlined /> </template
              >展示列</a-button
            >
          </a-popover>
        </div>
      </template>
      <template v-slot:data>
        <div style="width: 100%; position: absolute">
          <a-config-provider :locale="locale">
            <a-table
              bordered
              class="ant-table-striped"
              :columns="columns"
              :data-source="dataSource"
              :row-selection="
                batchSelectFlag
                  ? {
                      selectedRowKeys: selectedRowKeys,
                      onChange: onSelectChange,
                      onSelect: onSelect,
                      onSelectAll: onSelectAll,
                    }
                  : null
              "
              :row-key="(record) => record.id"
              :scroll="tableHeight"
              :loading="loading"
              :rowClassName="getRowClassName"
              :pagination="pagination"
              ref="entryTable"
              @resizeColumn="handleResizeColumn"
              :customRow="customRow"
              @change="handleTableChange"
            >
              <template #bodyCell="{ column, record, text }">
                <template v-if="column.dataIndex === 'entry'">
                  <div>
                    <template v-if="editableData[record.id]">
                      <a-form
                        :model="editableData[record.id]"
                        :rules="rules[record.id]"
                        :ref="
                          'form' +
                          record.id.replaceAll('-', '') +
                          column.dataIndex
                        "
                        autocomplete="off"
                      >
                        <a-form-item :name="column.dataIndex">
                          <!-- <a-textarea
                            v-model:value.lazy="
                              editableData[record.id][column.dataIndex]
                            "
                            style="margin: -5px 0"
                            @click="clickInput"
                            :auto-size="{ minRows: 1 }"
                          /> -->
                          <TextArea
                            v-model:value="editableData[record.id][column.dataIndex]"
                          />
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
                      <!-- <a-input
                        v-model:value="
                          editableData[record.id][column.dataIndex]
                        "
                        style="margin: -5px 0"
                        @click="clickInput"
                      /> -->
                      <Input
                        v-model:value="editableData[record.id][column.dataIndex]"
                      />
                    </template>
                    <template v-else>
                      {{ text }}
                    </template>
                  </div>
                </template>
                <template v-if="translateColumn.includes(column.dataIndex)">
                  <div>
                    <template v-if="editableData[record.id]">
                      <a-form
                        :model="editableData[record.id]"
                        :rules="rules[record.id]"
                        :ref="
                          'form' +
                          record.id.replaceAll('-', '') +
                          column.dataIndex
                        "
                        autocomplete="off"
                      >
                        <a-form-item :name="column.dataIndex">
                          <!-- <a-textarea
                            v-model:value="
                              editableData[record.id][column.dataIndex]
                            "
                            style="margin: -5px 0"
                            @click="clickInput"
                            :auto-size="{ minRows: 1 }"
                          /> -->
                          <TextArea
                            v-model:value="editableData[record.id][column.dataIndex]"
                          />
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
                      <a-select
                        v-model:value="
                          editableData[record.id][column.dataIndex]
                        "
                        style="width: 100%"
                        placeholder="请选择"
                        :fieldNames="{ label: 'title', value: 'title' }"
                        :options="classify1Option"
                        @change="getRowClassify2Option(record)"
                        allowClear
                      >
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
                      <a-select
                        v-model:value="
                          editableData[record.id][column.dataIndex]
                        "
                        style="width: 100%"
                        placeholder="请选择"
                        :fieldNames="{ label: 'name', value: 'name' }"
                        :options="rowClassify2Option[record.id]"
                        allowClear
                      >
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
                      <!-- <a-input
                        v-model:value="
                          editableData[record.id][column.dataIndex]
                        "
                        style="margin: -5px 0; width: 90%"
                        @click="clickInput"
                      /> -->
                      <Input
                        v-model:value="editableData[record.id][column.dataIndex]"
                      />
                      <a-tooltip placement="top">
                        <template #title>
                          <span>多个tag按分号分割！</span>
                        </template>
                        <InfoCircleOutlined style="margin-left: 3px" />
                      </a-tooltip>
                    </template>
                    <template v-else>
                      <!-- {{ text }} -->
                      <span>
                        <a-tag
                          v-for="(tag, index) in companyCut(text)"
                          :key="index"
                          color="cyan"
                          class="tag-content"
                        >
                          <span>{{ tag }}</span>
                        </a-tag>
                      </span>
                    </template>
                  </div>
                </template>
                <template v-if="column.dataIndex === 'entryState'">
                  <EntryStateBadge :entryState="text" />
                </template>
                <!-- ['englishTranslateState','russianTranslateState','spanishTranslateState','frenchTranslateState']-->
                <template
                  v-if="langTranslateStateList.includes(column.dataIndex)"
                >
                  <TransStateBadge :translateState="text" />
                </template>
                <template v-if="column.dataIndex === 'operation'">
                  <div class="editable-row-operations">
                    <span v-if="editableData[record.id]">
                      <a-button
                        type="primary"
                        ghost
                        size="small"
                        @click.stop="editSave(record.id)"
                        >保存</a-button
                      >
                      <a-button
                        type="primary"
                        ghost
                        size="small"
                        danger
                        @click.stop="editCancel(record.id)"
                        >取消</a-button
                      >
                    </span>
                    <span v-else>
                      <a-button
                        type="primary"
                        ghost
                        size="small"
                        @click.stop="entryDetails(record)"
                        >详情</a-button
                      >
                      <!-- <a-button type="primary" ghost size="small" @click.stop="entryUpgrade(record)">升级</a-button> -->
                    </span>
                  </div>
                </template>
              </template>
              <!-- 设置表格行展开子行的样式 -->
              <template #expandIcon="props">
                <span
                  v-if="
                    props.record.children != null &&
                    props.record.children.length > 0
                  "
                >
                  <div
                    v-if="props.expanded"
                    style="display: inline-block; margin-right: 10px"
                    @click="
                      (e) => {
                        props.onExpand(props.record, e);
                      }
                    "
                  >
                    <CaretDownOutlined />
                  </div>
                  <div
                    v-else
                    style="display: inline-block; margin-right: 10px"
                    @click="
                      (e) => {
                        props.onExpand(props.record, e);
                      }
                    "
                  >
                    <CaretRightOutlined />
                  </div>
                </span>
                <span v-else style="margin-right: 23px"></span>
              </template>
              <!-- 设置筛选菜单 -->
              <template
                #customFilterDropdown="{
                  setSelectedKeys,
                  selectedKeys,
                  confirm,
                  clearFilters,
                  column,
                }"
              >
                <div style="padding: 8px">
                  <a-input
                    ref="searchInput"
                    :placeholder="`搜索 ${column.title}`"
                    :value="selectedKeys[0]"
                    style="width: 188px; margin-bottom: 8px; display: block"
                    @change="
                      (e) =>
                        setSelectedKeys(e.target.value ? [e.target.value] : [])
                    "
                    @pressEnter="
                      handleSearch(selectedKeys, confirm, column.dataIndex)
                    "
                  />
                  <a-button
                    type="primary"
                    size="small"
                    style="width: 90px; margin-right: 8px"
                    @click="
                      handleSearch(selectedKeys, confirm, column.dataIndex)
                    "
                  >
                    <template #icon>
                      <SearchOutlined /> </template
                    >搜索</a-button
                  >
                  <a-button
                    size="small"
                    style="width: 90px"
                    @click="handleReset(clearFilters)"
                    >重置</a-button
                  >
                </div>
              </template>
              <!-- 设置筛选图标 -->
              <template #customFilterIcon="{ filtered }">
                <SearchOutlined
                  :style="{ color: filtered ? '#108ee9' : undefined }"
                />
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
    <OperationArea
      ref="operationArea"
      :title="operationAreaTitle"
      :height="operationAreaHeight"
      v-if="showOperationArea"
      @close="closeOperationArea"
    >
      <template v-slot:content>
        <div class="entryDetails">
          <table>
            <tr>
              <td class="tableTitle">翻译语种</td>
              <td v-for="lang in commonParam.languageList" :key="lang.name">
                {{ lang.name }}
              </td>
            </tr>
            <tr>
              <td class="tableTitle">翻译结果</td>
              <td
                v-for="lang in commonParam.languageList"
                :key="currentEntry[lang.value]"
              >
                {{ currentEntry[lang.value] }}
              </td>
            </tr>
            <tr>
              <td class="tableTitle">翻译状态</td>
              <td v-for="lang in commonParam.languageList" :key="lang.value">
                <template v-if="currentEntry[lang.state] === '3'">
                  <a-badge color="#36BF7D" /><span style="color: #36bf7d">{{
                    currentEntry[lang.chineseState]
                  }}</span>
                </template>
                <template v-else-if="currentEntry[lang.state] != null">
                  <a-badge color="#FBB31F" /><span style="color: #fbb31f">{{
                    currentEntry[lang.chineseState]
                  }}</span>
                </template>
              </td>
            </tr>
            <tr>
              <td class="tableTitle">选择</td>
              <td v-for="lang in commonParam.languageList" :key="lang.value">
                <a-checkbox
                  :disabled="currentEntry[lang.state] != '3'"
                  v-model:checked="currentEntry[lang.checked]"
                >
                </a-checkbox>
              </td>
            </tr>
          </table>
          <div class="details">
            <div>中文释义：{{ currentEntry.chineseInterpretation }}</div>
            <div>英文释义：{{ currentEntry.englishInterpretation }}</div>
            <div class="btnBox" v-if="admin">
              <a-button type="primary" size="small" @click="addPublic('1')"
                ><template #icon>
                  <PlusOutlined /> </template
                >添加到部门公共库</a-button
              >
              <a-button
                type="primary"
                size="small"
                style="margin-left: 16px"
                @click="addPublic('2')"
                ><template #icon>
                  <PlusOutlined /> </template
                >添加到公司公共库</a-button
              >
            </div>
          </div>
        </div>
      </template>
    </OperationArea>
    <EditReason
      :visible="editVisible"
      :entry="editEntry"
      @editClose="editClose"
      @editOk="editOk"
    />
  </div>
  <CreateVersionModal
    :visible="createVisible"
    :selectedProducts="selectedProducts"
    :dataSource="selectEntry"
    :currentProduct="product"
    :selectedRowKeys="selectedRowKeys"
    :selectedRows="selectedRows"
    @update:dataSource="selectEntry = $event"
    @update:selectedRowKeys="selectedRowKeys = $event"
    @update:selectedRows="selectedRows = $event"
    @update:selectedProducts="selectedProducts = $event"
    @createClose="createClose"
    @refresh="refreshTable"
    @cancelCreate="cancelCreate"
  />

  <SecondClassify
    ref="secondClassifyRef"
    :visible="secondClassifyVisible"
    :currentProduct="product"
    @secondClassifyClose="secondClassifyClose"
  />
  <Dictionary
    ref="dictionaryRef"
    :visible="dictionaryVisible"
    :currentProduct="product"
    @dictionaryClose="dictionaryClose"
  />
</template>
<script>
import "@/assets/style/common.less";
import CustomModal from "@/components/modal/index.vue";
import zhCN from "ant-design-vue/es/locale/zh_CN";
import SearchBox from "@/components/search/searchBox.vue";
import DataBox from "@/components/dataBox/index.vue";
import OperationArea from "@/components/operationArea/index.vue";
import BackFillModal from "@/components/Button/fileManage/backFill/modal.vue";
import BackFillModal_v2 from "@/components/Button/fileManage/backFill/modal_v2.vue";
import BackFillModal_v2_5 from "@/components/Button/fileManage/backFill/modal_v2.5.vue";
import BackFillModal_v3 from "@/components/Button/fileManage/backFill/modal_v3.vue";
import AccurSearchButton from "@/components/Button/accurSearchButton.vue";
import GitCommitButton from "@/components/Button/gitCommitButton.vue";
import EntryStateSelect from "@/components/select/entryStateSelect.vue";
import TransStateSelect from "@/components/select/transStateSelect.vue";
import EntryStateBadge from "@/components/stateBadge/entryStateBadge.vue";
import TransStateBadge from "@/components/stateBadge/transStateBadge.vue";
import TextArea from "@/components/cellEditor/textarea_IME.vue";
import Input from "@/components/cellEditor/input_IME.vue";
import EditReason from "@/views/entry/editReason.vue";
import CreateVersionModal from "@/views/entry/createVersionModal.vue";
import SecondClassify from "@/views/entry/secondClassify.vue";
import Dictionary from "@/views/entry/dictionary.vue";
import { message, Modal, notification } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import { cloneDeep, iteratee } from "lodash-es";
import { getLanguage } from "@/http/api/translate";
import { getProductVersion } from "@/http/api/product";
import { getVersionByName } from "@/http/api/productVersion";
import {
  deleteEntryInfo,
  updatePublicEntry,
  addSingleEntry,
  getClassfy,
  getClassTree,
  entryImportExcle,
  getEntryByClassfy,
  getEntrySourcesByClassify,
  getWriteFileNamesByClassify,
} from "@/http/api/entryManage";
import { getSecondClassify } from "@/http/api/secondClassify";
import {
  queryUserPartiality,
  updateUserPartiality,
} from "@/http/api/userPartiality";
import { getI18nAdress } from "@/http/api/workbench";
import { getCheckNotUseEntry, checkNotUseEntry } from "@/http/api/check";
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
  SearchOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
} from "@ant-design/icons-vue";
import {
  onSelectChange,
  onSelect,
  onSelectAll,
  pageChange,
  selectAllEntry,
  clearAllEntry,
} from "@/utils/selectionUtils";
import {
  getColPref,
  changeColumn,
  handleSearch,
  handleReset,
  clearFilters,
  handleTableChange,
  setTableHeight,
} from "@/utils/tableUtils";
import { setModalAriaHidden } from "@/utils/domUtils";
import { normalizeEditableRow } from "@/utils/dataUtils";
import { getCurrentFormattedTime } from "@/utils/dateUtils";
import {
  byteLength,
  getMaxLength,
  setRefRules,
  useRefRules,
  openSetEdit,
} from "@/utils/validationUtils";
import commonParam, { entryParams } from "@/constants/commonParam.js";
import transStateBadgeVue from "@/components/stateBadge/transStateBadge.vue";
export default {
  components: {
    CustomModal,
    SearchBox,
    DataBox,
    OperationArea,
    BackFillModal,
    BackFillModal_v2,
    BackFillModal_v2_5,
    BackFillModal_v3,
    AccurSearchButton,
    GitCommitButton,
    EntryStateSelect,
    TransStateSelect,
    EntryStateBadge,
    TransStateBadge,
    TextArea,
    Input,
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
    SearchOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
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
    const cachedSearchCondition = localStorage.getItem(
      "searchCondition-productEntry"
    );
    return {
      locale: zhCN,
      box: 0,
      admin: false,
      edit: false, // 用户对该产品是否有编辑权限
      product: {},
      labelCol: { style: { width: "84px" } },
      search: {
        entry: "",
        abbr: "",
        partOfSpeech: "",
        translateType: null,
        classfy1: [],
        classfy2: [],
        entryState_: [0, 1, 2, 3], // 如果查询条件为空即为全选，则使用这个词条状态来进行查询
        entryState: null, // 查询条件中的词条状态
        tag: "",
        entrySource: null,
        language: null,
        translateState: null,
        translate: "",
        filter_translate: "",
        comment: "",
        startTime_: null, // 时间戳格式
        endTime_: null, // 时间戳格式
        startTime: null,
        endTime: null,
        diFileName: null,
        update: null,
        searchType: null,
        i18nURL: null,
        hasRedundantRls: false, // 是否有冗余词条校验结果,有的话显示按钮“重新查询”
      },
      searchConditionList: entryParams.searchConditionList,
      checkedSearchCondition: cachedSearchCondition
        ? JSON.parse(cachedSearchCondition).displayColumn.split(",")
        : entryParams.checkedSearchCondition, // (可选)显示的查询条件框
      exportFields: [
        "词条",
        "tag",
        "词条来源",
        "翻译结果",
        "Comment",
        "辞典名称",
      ],
      translateTypes: [],
      tableTitle: "词条列表",
      copyVisible: false,
      copyNumber: 1,
      dataHeight: 200,
      // tableHeight: { x: "100%", y: 0 },
      tableHeight: { x: "max-content", y: 0 },
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
          resizable: true,
          fixed: "left",
          index: 1,
        },
        {
          title: "词条",
          dataIndex: "entry",
          align: "center",
          width: 160,
          resizable: true,
          fixed: "left",
          index: 2,
          // // 添加筛选功能(但是查询做了分页，只能获得当前页的数据)
          // customFilterDropdown: true, // 使用自定义筛选下拉框
          // filteredValue: null, // 初始状态下没有筛选条件
          // onFilter: (filterValue, record) => {
          //   // 精确匹配，不忽略大小写
          //   return record.entry.toString() === filterValue;
          // },
        },
        {
          title: "tag",
          dataIndex: "tag",
          align: "center",
          width: 130,
          resizable: true,
          index: 3,
        },
        {
          title: "comment",
          dataIndex: "comment",
          align: "center",
          width: 130,
          resizable: true,
          index: 4,
        },
        {
          title: "词条版本",
          dataIndex: "entryVersion",
          align: "center",
          width: 130,
          resizable: true,
          index: 5,
        },
        {
          title: "英文翻译",
          dataIndex: "english",
          align: "center",
          width: 180,
          resizable: true,
          index: 12,
        },
        {
          title: "俄文翻译",
          dataIndex: "russian",
          align: "center",
          width: 180,
          resizable: true,
          index: 15,
        },
        {
          title: "西文翻译",
          dataIndex: "spanish",
          align: "center",
          width: 180,
          resizable: true,
          index: 18,
        },
        {
          title: "法文翻译",
          dataIndex: "french",
          align: "center",
          width: 180,
          resizable: true,
          index: 21,
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
      overlayStyle: entryParams.overlayStyle, // 展示列样式
      checkboxList: entryParams.checkboxList, // 展示列可选的值
      checkedColumn: cachedDisplayColumn
        ? JSON.parse(cachedDisplayColumn).displayColumn.split(",")
        : [], // 展示列已选的值
      inputColumn: entryParams.inputColumn,
      translateColumn: entryParams.translateColumn,
      commonParam: commonParam,
      langTranslateStateList: commonParam.langTranslateStateList,
      langNameList: commonParam.langNameList,
      editableData: {},
      selectedRowKeys: [],
      selectedRows: [],
      selectedRowIndex: null,
      selectedProducts: {
        products: new Map(), // 切换到当前产品前的已选词条的产品记录
        totalNum: 0, // 切换到当前产品前的已选词条总数
      },
      currentEntry: {},
      showOperationArea: false,
      operationAreaTitle: "详情信息",
      operationAreaHeight: 190,
      currentVersion: null,
      productVersions: [],
      editVisible: false,
      editEntry: [],
      createVersionFlag: false,
      selectEntry: [], // 已选词条（可能会跨产品，还涉及了分页）
      createVisible: false,
      rules: {},
      batchSelectFlag: false,
      classify1Option: [],
      secondClassifyVisible: false,
      classify2Option: [],
      rowClassify2Option: {},
      dictionaryVisible: false,
      selectAllLoading: false,
      filters: null,
      accurSearch: [], // 用于分页时的查询参数
      lastSearchType: "getEntryByClassfy", // 记录最近一次查询类型
      lastAccurate: [], // 记录最近一次全量查询的accurate参数
      showForbbiden: false, // 显示/隐藏禁用
      classifyLimit: {},
      entrySourceOptions: [], // 词条来源下拉框
      diFileNameOptions: [], // 辞典名称下拉框
      entrySourceOptions_copy: [], // 词条来源下拉框
      diFileNameOptions_copy: [], // 辞典名称下拉框
      searchTypes: [
        { label: "冗余词条校验", value: "checkNotUseEntry" },
        { label: "条件查询", value: "getEntryByClassfy" },
      ],
      ipOptions: [],
    };
  },
  created() {},
  mounted() {
    this.$nextTick(() => {
      this.user = this.$store.state.user;
      if (this.$currentDepartment) {
        this.search.entryState = this.$currentDepartment.ops.has("entryState3")
          ? "3"
          : null;
      }
      this.admin = this.$store.state.admin;
      //保证初次传的值给到
      this.box = this.boxHeight;
      this.edit = this.productEdit;
      this.setTableHeight();
      this.product = this.currentProduct;

      this.getLanguage();
      // 读取本地存储的用户偏好
      getColPref("colPref-productEntry", 200, this);
    });
  },
  watch: {
    boxHeight(newval, oldval) {
      this.box = newval;
      this.setTableHeight();
    },
    currentProduct: {
      handler(newval, oldval) {
        // 切换前判断上个产品是否有已选词条
        if (oldval.title) {
          const changedNum =
            this.selectEntry.length - this.selectedProducts.totalNum; // 上一次切换中变化的词条数
          // console.log("changedNum =", changedNum);
          this.selectedProducts.totalNum += changedNum;
          const productID =
            oldval.type === "module" ? oldval.parentId : oldval.key; // 产品的ID
          if (this.selectedProducts.products.has(productID)) {
            const selectedNum =
              this.selectedProducts.products.get(productID) + changedNum;
            // console.log("当前数量", selectedNum);
            if (selectedNum > 0) {
              this.selectedProducts.products.set(productID, selectedNum);
              // console.log(
              //   `${oldval.title}产品存在，已选词条数量变化为：${selectedNum}`,
              //   this.selectedProducts
              // );
            } else if (selectedNum == 0) {
              this.selectedProducts.products.delete(productID);
              // console.log(
              //   `${oldval.title}产品存在，已选词条数量变化为0,已删除`,
              //   this.selectedProducts
              // );
            } else {
              console.log(
                `${oldval.title}产品存在，已选词条数量<0,有问题`,
                this.selectedProducts
              );
            }
          } else {
            if (changedNum > 0) {
              this.selectedProducts.products.set(productID, changedNum);
              // console.log(
              //   `新增${oldval.title}产品，已选词条数量变化为：${changedNum}`,
              //   this.selectedProducts
              // );
            } else if (changedNum < 0) {
              console.log(
                `不新增${oldval.title}产品，已选词条数量变化为：${changedNum},error`,
                this.selectedProducts
              );
            }
            // else {
            //   console.log(
            //     `不新增${oldval.title}产品，已选词条数量变化为：${changedNum}`,
            //     this.selectedProducts
            //   );
            // }
          }
        }
        // 切换后的初始化
        this.currentVersion = null;
        this.product = newval;
        // 查询产品的所有词条来源
        getEntrySourcesByClassify({ classifyID: newval.key }).then((res) => {
          this.entrySourceOptions = res.data
            .filter((item) => item && item !== "")
            .map((item) => ({
              label: item,
              value: item,
            }));
          this.entrySourceOptions_copy = cloneDeep(this.entrySourceOptions);
        });
        // 查询产品的所有辞典名称
        getWriteFileNamesByClassify({ classifyID: newval.key }).then((res) => {
          this.diFileNameOptions = res.data
            .filter((item) => item && item !== "")
            .map((item) => ({
              label: item,
              value: item,
            }));
          this.diFileNameOptions_copy = cloneDeep(this.diFileNameOptions);
        });
        this.showOperationArea = false;
        this.pagination.current = 1;
        // this.selectEntry = [];// 存在跨产品，所以不归零
        this.selectedRowKeys = [];
        this.selectedRows = [];
        this.pageChange(
          this.pagination.current,
          this.pagination.pageSize,
          null
        ); // 切换产品时第一页的已选未显示(初始化版分页)
        this.init();
      },
      deep: true, // 添加深度监听配置
    },
    "search.classfy1": function (newValue) {
      this.selectSecondClassify(); // 获取二级分类
      // if (newValue && newValue.length) {
      //   this.selectSecondClassify(); // 获取二级分类
      // } else {
      //   this.classify2Option = [];
      //   this.search.classfy2 = [];
      // }
    },
    productEdit(newval, oldval) {
      this.edit = newval;
    },
    "search.startTime_": function (newValue) {
      if (newValue) {
        this.search.startTime =
          newValue.$y +
          "-" +
          (newValue.$M + 1).toString().padStart(2, "0") +
          "-" +
          newValue.$D.toString().padStart(2, "0"); // 格式化日期为 YYYY-MM-DD 格式
        if (this.search.endTime_) {
          if (this.search.startTime_ > this.search.endTime_) {
            message.error("开始时间不能大于结束时间！");
            this.search.startTime = null;
            this.search.startTime_ = null;
          }
        }
      } else {
        this.search.startTime = null;
      }
    },
    "search.endTime_": function (newValue) {
      if (newValue) {
        this.search.endTime =
          newValue.$y +
          "-" +
          (newValue.$M + 1).toString().padStart(2, "0") +
          "-" +
          (newValue.$D + 1).toString().padStart(2, "0"); // 结束日期+1，以达到当天的24:00的效果）
        if (this.search.startTime_) {
          if (this.search.startTime_ > this.search.endTime_) {
            message.error("结束时间不能小于开始时间！");
            this.search.endTime = null;
            this.search.endTime_ = null;
          }
        }
      } else {
        this.search.endTime = null;
      }
    },
  },
  unmounted() {},
  methods: {
    async init() {
      // 获取一级分类
      await this.selectFirstClassify();
      // 查询产品的所有版本,并查询词条
      if (this.product.type == "product") {
        await this.getProductVersion({ isInit: true, accurate: [] });
      }
      // 查询i18nURL
      await this.getI18nAdress();
      // 查询词条
      // await this.getEntryByClassfy(true); // isInit=true
      await this.conditionalQuery([], true);
      // 设置表的高度
      this.setTableHeight();
    },
    format(text) {
      return text.replace(/\n/g, "\\n");
    },
    // 获取翻译语种
    getLanguage() {
      let data = {};
      getLanguage(data).then((res) => {
        this.translateTypes = res.data.list;
      });
    },
    // 获取i18服务器ip
    getI18nAdress() {
      this.ipOptions = [];
      getI18nAdress().then((res) => {
        res.data.list.forEach((item) => {
          let ip = {
            label: item.ip,
            value: item.ip,
          };
          this.ipOptions.push(ip);
        });
      });
    },
    // 切换显示/隐藏禁用
    changeForbbiden() {
      this.showForbbiden = this.showForbbiden ? false : true;
      if (this.showForbbiden) {
        this.search.entryState_ = [0, 1, 2, 3, -1];
      } else {
        this.search.entryState_ = [0, 1, 2, 3];
      }
      this.getEntryByClassfy();
    },
    // 处理词条来源的搜索输入
    handleEntrySourceSearch(value) {
      const option = {
        label: value,
        value: value,
      };
      this.entrySourceOptions = this.entrySourceOptions_copy.concat([option]);
    },
    // 处理辞典名称的搜索输入
    handleDiFileNameSearch(value) {
      const option = {
        label: value,
        value: value,
      };
      this.diFileNameOptions = this.diFileNameOptions_copy.concat([option]);
    },
    // 动态设置表格高度
    setTableHeight() {
      this.$nextTick(() => {
        setTableHeight(this, -8, 166, 84, { ok: true, h: this.box });
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
    getProductVersion(searchparams = { isInit: false, accurate: [] }) {
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
        // // 当前产品版本设置为第一个版本
        // if (this.productVersions.length > 0) {
        //   this.currentVersion = this.productVersions[0].id;
        // } else {
        //   this.currentVersion = null;
        // }
        // 获取版本下的词条
        this.getEntryByClassfy(searchparams.isInit, searchparams.accurate);
      });
    },
    // 条件查询
    conditionalQuery(accurate = [], isInit = false) {
      // 将页码变为第一页
      this.pagination.current = 1;
      // this.selectedRowKeys = [];
      // this.selectedRows = [];
      // this.selectEntry = [];
      // this.createVersionFlag = false;
      // this.batchSelectFlag = false;

      // 接口方法集合
      if (
        !this.search.searchType ||
        this.search.searchType == "getEntryByClassfy"
      ) {
        // 条件查询
        this.lastSearchType = "getEntryByClassfy";
        if (accurate && accurate.length > 0) {
          this.accurSearch = accurate;
        } else {
          this.accurSearch = [];
        }
        this.lastAccurate = this.accurSearch;
        this.getEntryByClassfy(isInit, this.accurSearch);
      } else if (this.search.searchType == "checkNotUseEntry") {
        // 冗余校验查询
        this.lastSearchType = "checkNotUseEntry";
        this.lastAccurate = [];
        this.getCheckNotUseEntry();
      } else {
        console.log("未执行查询", this.search.searchType);
      }
    },
    /**
     * 条件查询：根据一级分类（即状态树key）获取词条
     * isInit：是否是点击状态树的首次（部门级不用查询）；
     * accurate：是否全量查询
     */
    getEntryByClassfy(isInit = false, accurate = []) {
      if (isInit) {
        if (this.product.type == "department") {
          this.dataSource = [];
          this.pagination.total = 0;
          return; // 如果是部门，则不在点击状态树时查询版本
        }
      }
      if (Object.keys(this.product).length === 0) {
        return;
      }
      if (
        (this.search.translate != "" || this.search.translateState != null) &&
        !this.search.language
      ) {
        message.info("请选择翻译语种！");
        return;
      }
      let data = {
        abbr: this.search.abbr,
        entry: this.search.entry,
        classfy1: this.search.classfy1,
        classfy2: this.search.classfy2,
        // classfy1: this.product.type === "module" ? this.product.title : "",
        entryState:
          this.search.entryState == null
            ? this.search.entryState_
            : [this.search.entryState],
        tag: this.search.tag,
        entrySource: this.search.entrySource,
        comment: this.search.comment,
        diFileName: this.search.diFileName,
        update: this.search.update,
        filter_translate: this.search.filter_translate, // 翻译结果过滤
      };
      // data.entry = data.entry.replace(/\\n/g, '\n')
      // console.log("data:", data);
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
        classfyID:
          this.product.type === "module"
            ? this.product.parentId
            : this.product.key, // 模块就使用对应产品的classfyID进行查询
        // classfyID: this.product.key,
        translateType: this.search.language,
        pageIndex: this.pagination.current,
        pageSize: this.pagination.pageSize,
        startTime: this.search.startTime,
        endTime: this.search.endTime,
        update: this.search.update,
      };
      if (accurate.length > 0) {
        params.accurate = accurate;
      }
      this.loading = true;
      // 获取对应分类的词条
      getEntryByClassfy(params, data)
        .then((res) => {
          this.dataSource = res.data.list;
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
      // 复用 entryManage 里的“查询分类树”接口（避免 no-undef）
      getClassTree(params).then((res) => {
        this.entryClassfy = res.data;
      });
    },
    // 查询冗余词条校验状态
    async getCheckNotUseEntry() {
      if (!this.search.i18nURL) {
        message.info("请选择i18nURL！");
        return;
      }
      this.loading = true;
      let params = {
        i18nURL: this.search.i18nURL,
        classfyID: this.currentProduct.key,
      };
      await getCheckNotUseEntry(params).then(async (res) => {
        console.log("getCheckNotUseEntry:", res);
        if (res.data.state === 1) {
          // 有结果
          this.search.hasRedundantRls = true; // 显示“重新执行”
          this.dataSource = res.data.list;
          this.pagination.total = res.data.totalNum;
        } else if (res.data.state === 2) {
          // 有结果,但校验异常
          this.search.hasRedundantRls = true; // 显示“重新执行”
          message.info(`任务执行异常`, 1);
        } else if (res.data.state === 0) {
          // 没结果没执行
          this.dataSource = [];
          this.pagination.total = 0;
          message.info("查询无结果,开始校验", 1);
          await checkNotUseEntry(params).catch((err) => {
            console.log("冗余校验执行失败", err);
          }); // 没执行所以需要执行
        } else if (res.data.state === 3) {
          // 没结果有执行
          this.dataSource = [];
          this.pagination.total = 0;
          message.info("查询无结果,正在校验", 1);
        }
      });
      this.loading = false;
    },
    // 重新查询冗余词条校验状态
    async reGetCheckNotUseEntry() {
      if (!this.search.i18nURL) {
        message.info("请选择i18nURL！");
        return;
      }
      this.loading = true;
      let params = {
        i18nURL: this.search.i18nURL,
        classfyID: this.currentProduct.key,
      };
      await checkNotUseEntry(params)
        .then(async (res) => {
          this.dataSource = [];
          this.pagination.total = 0;
          await getCheckNotUseEntry(params).then(async (res) => {
            if (res.data.state === 3) {
              // 没结果有执行
              this.search.hasRedundantRls = false; // 隐藏“重新执行”
              message.info("正在重新校验中", 1);
            } else {
              let state = "";
              switch (res.data.state) {
                case 0:
                  state = "不在执行中";
                  break;
                case 1:
                  state = "有结果";
                  break;
                case 2:
                  state = "任务执行异常";
                  break;
                default:
                  state = "未知状态";
                  break;
              }
              message.info(`重新校验失败,任务状态：${state}`, 1);
            }
          });
        })
        .catch((err) => {
          console.log("冗余校验执行失败", err);
        });
      this.loading = false;
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
      this.loading = false;
    },
    changeVersion(version) {
      if (version === undefined) {
        this.currentVersion = null;
      } else {
        this.currentVersion = version;
      }
      this.pagination.current = 1;
      // 查询版本词条
      this.getEntryByClassfy(false, this.accurSearch);
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
        onDblclick: async (event) => {
          // clearTimeout(this.timer)
          if (this.editableData.hasOwnProperty(record.id)) {
            // 当前行在编辑状态
            return;
          }
          if (this.edit) {
            this.editableData[record.id] = normalizeEditableRow(
              cloneDeep(this.dataSource.find((item) => record.id === item.id))
            );
            console.log(
              "this.editableData[record.id]",
              this.editableData[record.id]
            );
            // 打开编辑态;设置校验规则(词条管理为多列配置)
            await openSetEdit(
              record,
              ["entry", ...commonParam.langValList],
              this
            );
            let cols = new Set(this.columns.map((item) => item.dataIndex));
            for (const col of ["entry", ...commonParam.langValList]) {
              // 对应的每一列都校验一遍
              if (record[col] && cols.has(col)) {
                // 对应列有值且展示了对应列（展示的才有表单引用）
                useRefRules(
                  this.$refs,
                  `form${record.id.replaceAll("-", "")}${col}`
                );
              }
            }
            // this.editableData[record.id] = cloneDeep(
            //   this.dataSource.filter((item) => record.id === item.id)[0]
            // );
            // // 设置校验规则
            // // this.rules[record.id] = {};
            // // this.rules[record.id]["entry"] = [
            // //   { validator: this.vilidFildLength(record, "maxByte") },
            // //   { required: true, message: "请输入!" },
            // // ];
            // // commonParam.langValList.forEach((item) => {
            // //   this.rules[record.id][item] = [
            // //     { validator: this.vilidFildLength(record, "foreignMaxByte") },
            // //   ];
            // // });
            // setRefRules(this, record, ["entry", ...commonParam.langValList]);

            // 获取表格操作行的classify2Option
            this.getRowClassify2Option(record);
          }
        },
      };
    },
    // 获得限制长度[遍历分类级-产品级-模块级]
    getClassfy(PID) {
      let params = {
        parentId: PID,
        type: "module",
      };
      getClassfy(params)
        .then((res) => {
          this.classifyLimit = {};
          res.data.list.forEach((element) => {
            this.classifyLimit[element.title] = element;
          });
        })
        .catch((err) => {
          message.err(err.message);
        });
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
    // 删除按钮
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
            this.getEntryByClassfy(false, this.accurSearch);
            this.selectedRowKeys = [];
            this.selectedRows = [];
          });
        },
      });
    },
    // 取消
    editCancel(id) {
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
    // 编辑-保存
    async editSave(id) {
      const validateArrSum = ["entry", ...commonParam.langValList];
      const formRefNameList = [];
      this.columns.forEach((column) => {
        if (validateArrSum.includes(column.dataIndex)) {
          formRefNameList.push({
            refName: `form${id.replaceAll("-", "")}${column.dataIndex}`,
            columnValue: column.dataIndex,
          });
        }
      });
      try {
        for (const { refName, columnValue } of formRefNameList) {
          // 使用校验规则
          await useRefRules(this.$refs, refName, columnValue);
        }
        // 所有表单校验通过，执行后续逻辑
        if (id.startsWith("new") || id.startsWith("copy")) {
          // 新增词条/升级词条
          addSingleEntry(this.editableData[id]).then((res) => {
            message.success("新增成功!");
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
      } catch (err) {
        console.error(err);
        // 校验失败：用 notification.error 聚合展示（兼容 string/object/array）
        const normalizeErrors = (e) => {
          if (!e) return [];
          if (typeof e === "string")
            return [{ columnName: "", errorMessage: e }];
          if (Array.isArray(e)) return e;
          if (typeof e === "object") return [e];
          return [{ columnName: "", errorMessage: String(e) }];
        };
        const errorList = normalizeErrors(err).filter(Boolean);
        const description = errorList
          .map((item) => {
            const col = item.columnName ? `${item.columnName}：` : "";
            const msg = item.errorMessage || item.message || "";
            return `${col}${msg}`;
          })
          .filter(Boolean)
          .join("\n");
        notification.error({
          message: "校验失败",
          description: description || "请检查输入内容",
          duration: 5,
        });
        // 阻止后续保存
        return;
      }

      // // 校验字段长度是否超限
      // let flagArr = ["entry", ...commonParam.langValList];
      // let list = [];
      // this.columns.forEach((column) => {
      //   if (flagArr.includes(column.dataIndex)) {
      //     list.push(
      //       eval(
      //         "this.$refs.form" + id.replaceAll("-", "") + column.dataIndex
      //       ).validate()
      //     );
      //   }
      // });
      // Promise.all(list)
      //   .then(() => {
      //     // 校验成功
      //     if (id.startsWith("new") || id.startsWith("copy")) {
      //       // 新增词条/升级词条
      //       addSingleEntry(this.editableData[id]).then((res) => {
      //         message.success("新增成功!");
      //         // this.getEntryByClassfy()
      //         let index = this.dataSource.findIndex((item) => item.id === id);
      //         this.dataSource.splice(index, 1);
      //         this.dataSource.splice(index, 0, res.data);
      //         delete this.editableData[id];
      //         delete this.rowClassify2Option[id];
      //         this.pagination.total = this.pagination.total + 1;
      //       });
      //     } else {
      //       this.editEntry = [this.editableData[id]];
      //       this.editVisible = true;
      //       setModalAriaHidden(this, document);
      //     }

      //     // 更新选中的值
      //     const selectedIndex = this.selectedRows.findIndex(
      //       (item) => item.id === id
      //     );
      //     if (selectedIndex !== -1) {
      //       // 更新选中行数据
      //       this.selectedRows[selectedIndex] = { ...this.editableData[id] };
      //     }
      //   })
      //   .catch((err) => {
      //     // console.log(err,"保存失败")
      //     // message.error(err.message);校验不通过，不用弹窗提示，通过ref进行提示
      //   });
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
      let index = this.dataSource.findIndex((item) => item.id === entry.id);
      this.dataSource.splice(index, 1);
      this.dataSource.splice(index, 0, entry);

      this.editVisible = false;
      delete this.rowClassify2Option[entry.id];
      this.getEntryByClassfy(false, this.accurSearch);
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
        classfy1: [],
        classfy2: [],
        entryState_: [0, 1, 2, 3], // 如果查询条件为空即为全选，则使用这个词条状态来进行查询
        entryState:
          this.$currentDepartment &&
          this.$currentDepartment.ops.has("entryState3")
            ? "3"
            : null, // 查询条件中的词条状态
        tag: "",
        entrySource: null,
        language: null,
        translateState: null,
        translate: "",
        filter_translate: "",
        comment: "",
        startTime_: null, // 时间戳格式
        endTime_: null, // 时间戳格式
        startTime: null,
        endTime: null,
        diFileName: null,
        update: null,
        i18nURL: null,
        hasRedundantRls: false,
      };
      this.showForbbiden = false; // 默认不显示禁用
      // this.getEntryByClassfy();
      this.conditionalQuery();
    },
    // 展示条件切换并保存用户偏好
    changeSearchCondition(checkedValue) {
      changeColumn(
        "searchCondition-productEntry",
        200,
        checkedValue,
        this,
        false,
        entryParams.searchConditionList
      );
    },
    // 展示列切换并保存用户偏好
    changeColumn(checkedValue) {
      changeColumn(
        "colPref-productEntry",
        200,
        checkedValue,
        this,
        false,
        commonParam.checkboxList
      );
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
    // 判断当前状态树是否是产品及以下（以便封掉部门级使用不了的按钮）
    isProduct() {
      // console.log("当前产品", this.product.title);
      return this.product.type == "product" || this.product.type == "module";
    },
    // 批量选择
    createVersion() {
      this.createVersionFlag = true;
      // this.selectEntry = [];
      // this.selectedRowKeys = [];
      // this.selectedRows = [];
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
      console.log("刷新查询", this.lastSearchType);
      if (this.lastSearchType === "checkNotUseEntry") {
        // 上一次为冗余词条校验查询，则刷新冗余校验结果
        this.getCheckNotUseEntry();
      } else {
        // 其他情况（包含普通条件查询与全量查询），沿用上一次的accurate参数
        const accurate =
          this.lastAccurate && this.lastAccurate.length
            ? this.lastAccurate
            : this.accurSearch;
        this.getEntryByClassfy(false, accurate);
      }
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
        classfy1: this.search.classfy1,
        classfy2: this.search.classfy2,
        // classfy1: this.product.type === "module" ? this.product.title : "",
        entryState:
          this.search.entryState == null
            ? this.search.entryState_
            : [this.search.entryState],
        tag: this.search.tag,
        entrySource: this.search.entrySource,
        comment: this.search.comment,
        diFileName: this.search.diFileName,
        update: this.search.update,
        filter_translate: this.search.filter_translate, // 翻译结果过滤
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
        classfyID:
          this.product.type === "module"
            ? this.product.parentId
            : this.product.key,
        translateType: this.search.language,
        startTime: this.search.startTime,
        endTime: this.search.endTime,
        update: this.search.update,
        pageIndex: -1,
        pageSize: -1,
      };
      if (this.accurSearch && this.accurSearch.length > 0) {
        params.accurate = this.accurSearch;
      }
      this.loading = true;
      getEntryByClassfy(params, data)
        .then((res) => {
          this.selectedRowKeys = [];
          this.selectedRows = [...this.selectedRows, ...res.data.list];
          this.selectEntry = this.selectedRows;
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
    // 取消批量选择
    cancelCreate() {
      this.selectEntry = [];
      this.selectedRowKeys = [];
      this.selectedRows = [];
      this.selectedProducts = {
        products: new Map(), // 切换到当前产品前的已选词条的产品记录
        totalNum: 0, // 切换到当前产品前的已选词条总数
      };
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
      // if (newData.maxLength != "") {
      //   this.rules[newData.id] = {};
      //   this.rules[newData.id]["entry"] = [
      //     { validator: this.vilidFildLength(newData, "maxByte") },
      //     { required: true, message: "请输入!" },
      //   ];
      //   commonParam.langValList.forEach((item) => {
      //     this.rules[newData.id][item] = [
      //       { validator: this.vilidFildLength(newData, "foreignMaxByte") },
      //     ];
      //   });
      // }
      setRefRules(this, newData, ["entry", ...commonParam.langValList]);

      if (this.pagination.total >= this.pagination.pageSize) {
        this.dataSource.splice(this.pagination.pageSize - 1, 0, newData);
      } else {
        this.dataSource.push(newData);
      }

      this.editableData[newData.id] = normalizeEditableRow(newData);
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
      // if (copyEntry.maxLength != "") {
      //   this.rules[copyEntry.id] = {};
      //   this.rules[copyEntry.id]["entry"] = [
      //     { validator: this.vilidFildLength(copyEntry, "maxByte") },
      //     { required: true, message: "请输入!" },
      //   ];
      //   commonParam.langValList.forEach((item) => {
      //     this.rules[copyEntry.id][item] = [
      //       { validator: this.vilidFildLength(copyEntry, "foreignMaxByte") },
      //     ];
      //   });
      // }
      setRefRules(this, copyEntry, ["entry", ...commonParam.langValList]);

      let index = this.dataSource.indexOf(entry);
      this.dataSource.splice(index + 1, 0, copyEntry);
      this.editableData[copyEntry.id] = normalizeEditableRow(copyEntry);
      this.getRowClassify2Option(copyEntry);
    },
    // 获取一级分类
    selectFirstClassify() {
      // 重置一级分类、二级分类
      this.search.classfy1 = [];
      this.classify1Option = [];
      this.search.classfy2 = [];
      this.classify2Option = [];
      // 更新一级分类
      if (this.product.type === "module") {
        this.classify1Option = [this.product]; // 可选项只有当前产品
        this.search.classfy1 = [this.product.title]; // 已选项默认为当前产品的名称
        this.getClassfy(this.product.parentId); // 获取当前模块的限制长度
      } else if (this.product.type === "product") {
        this.product.children.forEach((item) => {
          this.classify1Option.push(item);
        });
        this.getClassfy(this.product.key); // 获取当前产品的限制长度
      }
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
      // if (this.product.type != "module") {
      //   this.classify2Option = [];
      //   return;
      // }
      // let params = {
      //   parentId: this.product.key,
      // };
      // getSecondClassify(params).then((res) => {
      //   this.classify2Option = res.data.list;
      //   console.log(
      //     this.product.key,
      //     `这是${this.product.title}对应的二级分类3`,
      //     res.data.list
      //   );
      // });

      this.classify2Option = [];
      if (this.search.classfy1 && this.search.classfy1.length > 0) {
        const classfy1Set = new Set(this.search.classfy1);
        const matchedItems = this.classify1Option.filter((item) =>
          Array.from(classfy1Set).some((classfy1) =>
            classfy1.includes(item.title)
          )
        );
        const requestedParentIds = new Set();
        const requests = [];
        matchedItems.forEach((item) => {
          if (!requestedParentIds.has(item.key)) {
            requestedParentIds.add(item.key);
            requests.push(
              getSecondClassify({ parentId: item.key })
                .then((res) => ({ res, item }))
                .catch((error) => {
                  console.error(`获取${item.title}的二级分类失败:`, error);
                  return { res: { data: { list: [] } }, item };
                })
            );
          }
        });

        // 并行处理所有请求
        Promise.all(requests).then((results) => {
          // 合并所有结果
          const classify2Set = results.reduce((acc, { res, item }) => {
            res.data.list.forEach((subItem) => {
              acc.add(subItem.name);
            });
            return acc;
          }, new Set());

          // 二级分类名称去重
          Array.from(classify2Set).forEach((item) => {
            this.classify2Option.push({ title: item });
          });
          // 根据一级分类去除已选二级分类
          this.search.classfy2 = this.search.classfy2.filter(
            (value) => value && classify2Set.has(value)
          );
        });
      } else {
        // 一级分类为空，则清空二级分类
        this.classify2Option = [];
        this.search.classfy2 = [];
      }
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
    // 记录用户偏好
    recordPartiality(data) {
      updateUserPartiality(data).then((res) => {});
    },
    handleSearch(selectedKeys, confirm, dataIndex) {
      handleSearch(selectedKeys, confirm, dataIndex, this);
    },
    handleReset(clearFilters) {
      handleReset(clearFilters, this);
    },
    // 清空表格筛选条件
    clearFilters() {
      clearFilters(this);
    },
    // 表格change事件
    handleTableChange(pagination, filters) {
      handleTableChange(pagination, filters, this);
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
    pageChange(page, pageSize, refreshFn = this.getEntryByClassfy) {
      pageChange(
        this,
        page,
        pageSize,
        // this.getEntryByClassfy,
        refreshFn,
        "selectEntry",
        false,
        this.accurSearch
      );
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