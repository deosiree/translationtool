<template>
  <div
    class="productEntryBox"
    ref="productEntryBox"
  >
    <SearchBox
      ref="search"
      @change="setTableHeight"
      :operate="false"
    >
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
            <a-form-item
              v-if="checkedSearchCondition.includes('customCheckTransLength')"
              name="customCheckTransLength"
            >
              <template #label>
                <span>字节限制</span>
                <a-tooltip>
                  <template #title>
                    <div>筛选译文字节数大于输入值的记录。</div>
                    <div>常见 UTF-8 参考（估算）：</div>
                    <div>英文基础字母、数字：1 字符约 1 字节</div>
                    <div>俄文常见字母：1 字符约 2 字节</div>
                    <div>中文常用汉字：1 字符约 3 字节</div>
                    <div>
                      实际字节数取决于具体字符，标点、重音字母等可能不同。
                    </div>
                  </template>
                  <InfoCircleOutlined
                    style="margin-left: 4px; color: #00000066"
                  />
                </a-tooltip>
              </template>
              <a-input-number
                v-model:value="search.customCheckTransLength"
                :min="1"
                placeholder="请输入翻译的字节限制"
                style="width: 186px"
              />
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
              v-if="
                $currentDepartment &&
                $currentDepartment.ops.has('needForbidden') &&
                admin
              "
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
            <a-button
              type="primary"
              size="middle"
              @click="conditionalQuery"
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
            <ColumnFilter
              v-model="checkedSearchCondition"
              :columns="searchConditionList"
              :overlay-style="overlayStyle"
              col-pref-name="searchCondition-productEntry"
              title="展示条件"
              button-text="展示条件"
              button-size="middle"
              persist-mode="selectionOnly"
              @change="setTableHeight"
            />
          </a-row>
        </a-form>
      </template>
    </SearchBox>
    <DataBox
      :title="tableTitle"
      :height="dataHeight"
      :showOperate="true"
    >
      <template v-slot:operate>
        <div
          ref="button"
          class="entry-toolbar"
        >
          <div class="entry-toolbar-left">
            <div
              v-if="isProduct()"
              class="entry-toolbar-version"
            >
              <span class="entry-toolbar-version-label">产品版本：</span>
              <a-select
                v-model:value="currentVersion"
                class="entry-toolbar-version-select"
                placeholder="请选择版本"
                :options="productVersions"
                :fieldNames="{ label: 'name', value: 'id' }"
                size="small"
                @change="changeVersion"
                allowClear
              />
            </div>
            <div
              v-if="hasEditingRows"
              class="entry-toolbar-rules"
            >
              <RulesDropdown
                :options="rulesOptions"
                @update:options="rulesOptions = $event"
              />
            </div>
          </div>
          <SelectionToolbar class="entry-toolbar-right">
            <GitCommitButton
              v-if="$currentDepartment && $currentDepartment.ops.has('needIP')"
              size="small"
              buttonTitle="git推送"
              buttonClass="yellowBtn"
              :treeTitle="product.title"
            />

            <SelectionToolbarButton
              text="新增"
              preset="primary"
              :icon="toolbarIcons.add"
              button-class="entry-toolbar-add-button"
              :disabled="!isProduct()"
              @click="addEntry"
            />

            <SelectionToolbarButton
              text="二级分类管理"
              preset="primary"
              @click="setSecondClassify"
              v-if="admin"
              :disabled="!isProduct()"
            />
            <!-- 更新翻译(异步) -->
            <BackFillModal_v3
              v-if="
                admin &&
                $currentDepartment &&
                $currentDepartment.ops.has('fileUpdate')
              "
              mode="button"
              buttonTitle="更新翻译"
              modalTitle="更新翻译"
              size="small"
              :showFileTypeSelect="true"
              :defaultAccept="'.csv'"
              @importSuccess="refreshTable"
            />

            <SelectionToolbarButton
              text="已选词条"
              preset="selected"
              :badge-count="selectionToolbarState.badgeCount"
              :loading="selectionToolbarState.loading"
              :disabled="selectionToolbarState.loading"
              :aria-label="selectionToolbarState.loading ? '正在选择词条' : '已选词条'"
              @click="viewCreateVersionEntry"
            />

            <SelectionToolbarButton
              text="取消选择"
              preset="default"
              :disabled="selectionToolbarState.clearDisabled"
              @click="cancelSelection"
            />

            <ColumnFilter
              v-model="checkedColumn"
              :columns="columnSettingsList"
              :overlay-style="overlayStyle"
              col-pref-name="colPref-productEntry"
              :normal-width="200"
              :need-filter="false"
              button-type="text"
              @change="syncColumnsFromPref"
            />
          </SelectionToolbar>
        </div>
      </template>
      <template v-slot:data>
        <div class="entry-table-holder">
          <a-config-provider :locale="locale">
            <TableViewport
              bordered
              class="ant-table-striped table-cell-overflow"
              :empty-overlay="true"
              :columns="columns"
              :adaptive-column-keys="adaptiveColumnKeys"
              :data-source="dataSource"
              :row-selection="{
                columnWidth: 48,
                selectedRowKeys: tableSelectedRowKeys,
                onChange: onSelectChange,
                onSelect: onSelect,
                onSelectAll: onSelectAll,
              }"
              :row-key="(record) => record.id"
              :scroll="tableHeight"
              :loading="loading"
              :rowClassName="getRowClassName"
              :pagination="pagination"
              ref="entryTable"
              :customRow="customRow"
              @change="handleTableChange"
            >
              <template #headerCell="{ title, column }">
                <TableHeaderHint
                  v-if="['tag', 'comment'].includes(column.dataIndex)"
                  :title="title"
                  :column="column"
                />
                <a-tooltip v-else-if="column.dataIndex === 'maxLength'">
                  <template #title>
                    译文字符数上限，最多可输入相应数量的字或字母；空格也计数，首尾空格不忽略。
                  </template>
                  <span>{{ title }} <InfoCircleOutlined /></span>
                </a-tooltip>
                <CellOverflowTooltip
                  v-else-if="column.colValue"
                  :content="title"
                >
                  {{ title }}
                </CellOverflowTooltip>
                <span v-else>{{ title }}</span>
              </template>
              <template #bodyCell="{ column, record, text }">
                <template v-if="column.dataIndex === 'entry'">
                  <TableCellText
                    :text="formatEntryText(text)"
                    :value="editableData[record.id]?.[column.dataIndex] ?? ''"
                    :editing="!!editableData[record.id]"
                    :error-message="cellErrors[record.id]?.[column.dataIndex]"
                    @update:value="(val) => onCellInput(val, record, column)"
                    @blur="() => onCellBlur(record, column)"
                  />
                </template>
                <template
                  v-else-if="editableTextAreaColumns.includes(column.dataIndex)"
                >
                  <TableCellText
                    :text="formatCellText(text)"
                    :value="editableData[record.id]?.[column.dataIndex] ?? ''"
                    :editing="!!editableData[record.id]"
                    :error-message="cellErrors[record.id]?.[column.dataIndex]"
                    @update:value="(val) => onCellInput(val, record, column)"
                    @blur="() => onCellBlur(record, column)"
                  />
                </template>
                <template v-else-if="column.dataIndex === 'entryLength'">
                  <template v-if="editableData[record.id]">
                    <a-input-number
                      v-model:value="editableData[record.id].entryLength"
                      :min="0"
                      :precision="0"
                      style="width: 100%"
                    />
                  </template>
                  <template v-else>
                    <CellOverflowTooltip :content="formatCellText(text)" />
                  </template>
                </template>
                <template v-else-if="column.dataIndex === 'maxLength'">
                  <template v-if="editableData[record.id]">
                    <a-input-number
                      v-model:value="editableData[record.id].maxLength"
                      :min="0"
                      :precision="0"
                      style="width: 100%"
                      placeholder="0 表示无限制"
                    />
                  </template>
                  <template v-else>
                    <CellOverflowTooltip :content="formatMaxLengthText(text)" />
                  </template>
                </template>
                <template v-else-if="column.dataIndex === 'writeType'">
                  <template v-if="editableData[record.id]">
                    <a-select
                      v-model:value="editableData[record.id].writeType"
                      :options="writeTypeOptions"
                      placeholder="请选择"
                      allowClear
                      style="width: 100%"
                    />
                  </template>
                  <template v-else>
                    <CellOverflowTooltip :content="formatCellText(text)" />
                  </template>
                </template>
                <template v-else-if="inputColumn.includes(column.dataIndex)">
                  <template v-if="editableData[record.id]">
                    <Input
                      v-model:value="editableData[record.id][column.dataIndex]"
                    />
                  </template>
                  <template v-else>
                    <CellOverflowTooltip :content="formatCellText(text)" />
                  </template>
                </template>
                <template v-else-if="column.dataIndex === 'classfy1'">
                  <template v-if="editableData[record.id]">
                    <a-select
                      v-model:value="editableData[record.id][column.dataIndex]"
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
                    <CellOverflowTooltip :content="formatCellText(text)" />
                  </template>
                </template>
                <template v-else-if="column.dataIndex === 'classfy2'">
                  <template v-if="editableData[record.id]">
                    <a-select
                      v-model:value="editableData[record.id][column.dataIndex]"
                      style="width: 100%"
                      placeholder="请选择"
                      :fieldNames="{ label: 'name', value: 'name' }"
                      :options="rowClassify2Option[record.id]"
                      allowClear
                    >
                    </a-select>
                  </template>
                  <template v-else>
                    <CellOverflowTooltip :content="formatCellText(text)" />
                  </template>
                </template>
                <template v-else-if="column.dataIndex === 'tag'">
                  <template v-if="editableData[record.id]">
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
                    <TableTagCell :text="text" />
                  </template>
                </template>
                <template v-else-if="column.dataIndex === 'entryState'">
                  <CellOverflowTooltip :content="getEntryStateLabel(text)">
                    <EntryStateBadge :entryState="text" />
                  </CellOverflowTooltip>
                </template>
                <template
                  v-else-if="langTranslateStateList.includes(column.dataIndex)"
                >
                  <CellOverflowTooltip :content="getTranslateStateLabel(text)">
                    <TransStateBadge :translateState="text" />
                  </CellOverflowTooltip>
                </template>
                <template v-else-if="column.dataIndex === 'operation'">
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
                    </span>
                  </div>
                </template>
                <template
                  v-else-if="column.dataIndex && column.dataIndex !== 'index'"
                >
                  <CellOverflowTooltip :content="formatCellText(text)" />
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
                <span
                  v-else
                  style="margin-right: 23px"
                ></span>
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
                    <template #icon> <SearchOutlined /> </template
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
              <template #empty>
                <a-empty :image="simpleEmptyImage" />
              </template>
            </TableViewport>
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
              <td
                v-for="lang in commonParam.languageList"
                :key="lang.name"
              >
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
              <td
                v-for="lang in commonParam.languageList"
                :key="lang.value"
              >
                <template v-if="currentEntry[lang.chineseState]">
                  <a-badge :color="getTranslateStateStatus(currentEntry[lang.state]).color" />
                  <span :style="{ color: getTranslateStateStatus(currentEntry[lang.state]).color }">{{
                    currentEntry[lang.chineseState]
                  }}</span>
                </template>
              </td>
            </tr>
            <tr>
              <td class="tableTitle">选择</td>
              <td
                v-for="lang in commonParam.languageList"
                :key="lang.value"
              >
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
            <div
              class="btnBox"
              v-if="admin"
            >
              <a-button
                type="primary"
                size="small"
                @click="addPublic('1')"
                ><template #icon> <PlusOutlined /> </template
                >添加到部门公共库</a-button
              >
              <a-button
                type="primary"
                size="small"
                style="margin-left: 16px"
                @click="addPublic('2')"
                ><template #icon> <PlusOutlined /> </template
                >添加到公司公共库</a-button
              >
            </div>
          </div>
        </div>
      </template>
    </OperationArea>
  </div>
  <CreateVersionModal
    :visible="createVisible"
    :selectedProducts="selectedProducts"
    :dataSource="selectEntry"
    :currentProduct="product"
    :classifyLimit="classifyLimit"
    :selectedRowKeys="selectedRowKeys"
    :selectedRows="selectedRows"
    :rulesOptions="rulesOptions"
    @update:dataSource="updateSelectedEntries"
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
import TableViewport from "@/components/table/TableViewport/TableViewport.vue";
import TableCellText from "@/components/table/TableCellText/TableCellText.vue";
import TableHeaderHint from "@/components/table/TableHeaderHint/TableHeaderHint.vue";
import TableTagCell from "@/components/table/TableTagCell/TableTagCell.vue";
import { getAdaptiveColumnKeys } from "@/components/table/columnLayout.js";
import Input from "@/components/cellEditor/input_IME.vue";
import CellOverflowTooltip from "@/components/table/CellOverflowTooltip/CellOverflowTooltip.vue";
import CreateVersionModal from "@/views/entry/createVersionModal/index.vue";
import SecondClassify from "@/views/entry/secondClassify.vue";
import Dictionary from "@/views/entry/dictionary.vue";
import Empty from "ant-design-vue/es/empty";
import { message, Modal } from "ant-design-vue";
import { defineComponent, ref, createVNode } from "vue";
import { cloneDeep, iteratee } from "lodash-es";
import { getLanguage } from "@/http/api/translate";
import { getProductVersion } from "@/http/api/product";
import { getVersionByName } from "@/http/api/productVersion";
import {
  deleteEntryInfo,
  updatePublicEntry,
  addSingleEntry,
  updateEntryInfoList,
  getClassfy,
  getClassTree,
  entryImportExcle,
  getEntryByClassfy,
  getEntrySourcesByClassify,
  getWriteFileNamesByClassify,
} from "@/http/api/entryManage";
import { partitionBatchUpdateResults } from "@/utils/batchUpdateResults.js";
import { EDIT_NOTES } from "@/constants/editNotes.js";
import { getSecondClassify } from "@/http/api/secondClassify";
import {
  queryUserPartiality,
  updateUserPartiality,
} from "@/http/api/userPartiality";
import { getI18nAdress } from "@/http/api/workbench";
import { getCheckNotUseEntry, checkNotUseEntry } from "@/http/api/check";
import { MultiRequestPolling } from "@/utils/pollingUtils";
import {
  PlusOutlined,
  DeleteOutlined,
  CopyOutlined,
  SaveOutlined,
  UpSquareOutlined,
  PlusCircleOutlined,
  SwapOutlined,
  InfoCircleOutlined,
  ExclamationCircleOutlined,
  SearchOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
} from "@ant-design/icons-vue";
import { pageChange } from "@/utils/selectionUtils";
import SelectionToolbar from "@/components/selectToolBar/SelectionToolbar.vue";
import SelectionToolbarButton from "@/components/selectToolBar/SelectionToolbarButton.vue";
import { useSelectionOperations } from "@/components/selectToolBar/composables/useSelectionOperations";
import { useSelectedEntries } from "@/components/selectToolBar/composables/useSelectedEntries";
import { useSelectionToolbarState } from "@/components/selectToolBar/composables/useSelectionToolbarState";
import {
  applyTable,
  mergeColumnSelection,
  syncColumnsFromPref as applyTableColumnsFromPref,
} from "@/components/ColumnFilter";
import {
  handleSearch,
  handleReset,
  clearFilters,
  handleTableChange,
  setTableHeight,
} from "@/utils/tableUtils";
import { setModalAriaHidden } from "@/utils/domUtils";
import { normalizeEditableRow } from "@/utils/dataUtils";
import { normalizeOptionalID } from "@/utils/idUtils";
import { createQueryVersionGuard } from "@/utils/queryVersionGuard";
import {
  formatEntryText,
  formatCellText,
  formatMaxLengthText,
} from "@/components/table/cellText";
import { getCurrentFormattedTime } from "@/utils/dateUtils";
import {
  byteLength,
  getMaxLength,
  setRefRules,
  validateEditableCell,
  setCellError,
  clearCellError,
  openSetEdit,
  onEditableCellInput,
  applyCell,
  revalidateEditingRows,
} from "@/utils/validationUtils";
import commonParam, {
  entryParams,
  entryAllCols,
  entryPresets,
} from "@/constants/commonParam.js";
import {
  getEntryStateLabel,
  getTranslateStateLabel,
  getTranslateStateStatus,
} from "@/constants/statusMaps.js";
import ColumnFilter from "@/components/ColumnFilter/ColumnFilter.vue";
import RulesDropdown from "@/components/Dropdown/rulesDropdown.vue";

const DETAIL_TRANSLATE_STATE_CODES = new Set(["1", "2", "3"]);

/**
 * 解析产品或模块节点关联的产品 ID，过滤空值与字符串 null。
 * @param {Object} node 当前词条树节点。
 * @returns {string|number|undefined} 有效产品 ID；缺失时返回 undefined。
 */
function resolveRequestProductID(node) {
  const productID =
    node?.type === "module"
      ? node.parentId
      : node?.type === "product"
        ? node.key
        : undefined;
  return normalizeOptionalID(productID);
}

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
    TableViewport,
    TableCellText,
    TableHeaderHint,
    TableTagCell,
    Input,
    CellOverflowTooltip,
    CreateVersionModal,
    SecondClassify,
    Dictionary,
    ColumnFilter,
    RulesDropdown,
    PlusOutlined,
    DeleteOutlined,
    CopyOutlined,
    SaveOutlined,
    UpSquareOutlined,
    PlusCircleOutlined,
    SwapOutlined,
    InfoCircleOutlined,
    ExclamationCircleOutlined,
    SearchOutlined,
    CaretDownOutlined,
    CaretRightOutlined,
    SelectionToolbar,
    SelectionToolbarButton,
  },
  emits: [],
  props: {
    boxHeight: 0,
    currentProduct: {},
    productEdit: false,
  },
  data() {
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
        customCheckTransLength: null,
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
        ? mergeColumnSelection(
            JSON.parse(cachedSearchCondition)
              .displayColumn.split(",")
              .filter(Boolean),
            entryParams.searchConditionList
          )
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
      columns: [],
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
      overlayStyle: entryParams.overlayStyle,
      columnSettingsList: [],
      checkedColumn: [],
      inputColumn: entryParams.inputColumn,
      textareaInputColumn: entryParams.textareaInputColumn,
      translateColumn: entryParams.translateColumn,
      writeTypeOptions: [
        { label: "TS", value: "TS" },
        { label: "DI", value: "DI" },
      ],
      commonParam: commonParam,
      langTranslateStateList: commonParam.langTranslateStateList,
      langNameList: commonParam.langNameList,
      editableData: {},
      selectedRowKeys: [],
      selectedRows: [],
      selectedRowIndex: null,
      selectedProducts: {
        products: new Map(), // 全局已选词条按 productID 分组的数量
        totalNum: 0, // 全局已选词条数量
      },
      selectionOperations: useSelectionOperations(),
      selectedEntryOperations: useSelectedEntries(),
      toolbarIcons: { add: PlusOutlined },
      currentEntry: {},
      showOperationArea: false,
      operationAreaTitle: "详情信息",
      operationAreaHeight: 190,
      currentVersion: null,
      productVersions: [],
      selectEntry: [], // 已选词条（可能会跨产品，还涉及了分页）
      createVisible: false,
      rules: {},
      cellErrors: {},
      rulesOptions: commonParam.rulesOptions.map((item) => ({ ...item })),
      classify1Option: [],
      secondClassifyVisible: false,
      classify2Option: [],
      rowClassify2Option: {},
      dictionaryVisible: false,
      redundantPollingController: null,
      redundantPollingRequestId: null,
      redundantPollingToken: 0,
      queryVersionGuard: createQueryVersionGuard(),
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
  created() {
    this.applyColumnPreference();
  },
  computed: {
    /**
     * 返回当前词条查询范围，用于隔离切换范围前启动的选择请求。
     * @returns {string} 当前查询范围的序列化键。
     */
    selectionScopeKey() {
      return this.getCurrentSelectionScopeKey();
    },
    /**
     * 返回是否有全量选择操作仍在请求中。
     * @returns {boolean} 是否显示选择 loading。
     */
    selectAllLoading() {
      this.selectionOperations.revision;
      return this.selectionOperations.pendingCount > 0;
    },
    /**
     * 返回选择工具栏的数量、徽标和清空按钮状态。
     * @returns {Object} 工具栏选择状态。
     */
    selectionToolbarState() {
      this.selectionOperations.revision;
      return useSelectionToolbarState(
        this.selectEntry,
        this.selectionOperations.pendingCount > 0
      );
    },
    /**
     * 按操作顺序将当前查询的待全选和待取勾状态投影到当前页。
     * @param {undefined} [_unused] 计算属性不接收参数。
     * @returns {Array} 当前表格用于显示勾选状态的词条 ID。
     */
    tableSelectedRowKeys() {
      this.selectionOperations.revision;
      const selectedIds = new Set(this.selectEntry.map((item) => item.id));
      const currentScopeKey = this.selectionScopeKey;
      this.dataSource.forEach((row) => {
        if (row?.id == null) return;
        const selected = this.selectionOperations.project(
          row.id,
          currentScopeKey,
          selectedIds.has(row.id)
        );
        if (selected) selectedIds.add(row.id);
        else selectedIds.delete(row.id);
      });
      return Array.from(selectedIds);
    },
    /**
     * 返回与表格默认空态一致的简洁插画。
     * @param {undefined} [_unused] 计算属性不使用业务输入。
     * @returns {Object} Ant Design Vue 的简洁空态插画节点。
     */
    simpleEmptyImage(_unused) {
      return Empty.PRESENTED_IMAGE_SIMPLE;
    },
    editableTextAreaColumns() {
      return [
        ...(this.translateColumn || []),
        ...(this.textareaInputColumn || []),
      ];
    },
    /**
     * 返回词条、译文和说明等长文本列标识，供共享表格分配剩余宽度。
     * @returns {Array<string>} 当前显示的自适应列标识。
     */
    adaptiveColumnKeys() {
      return getAdaptiveColumnKeys(
        (this.columns || []).filter((column) => !column.fixed)
      );
    },
    /** 有行内编辑词条时才显示校验规则下拉 */
    hasEditingRows() {
      return Object.keys(this.editableData || {}).length > 0;
    },
  },
  mounted() {
    this.$nextTick(() => {
      this.applyColumnPreference();
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
    });
  },
  /**
   * 组件卸载时结束冗余校验状态轮询。
   * @param {void} none - 无参数。
   * @returns {void}
   */
  beforeUnmount() {
    this.queryVersionGuard.invalidate();
    this.selectionOperations.invalidate();
    this.stopRedundantPolling();
  },
  watch: {
    selectionScopeKey: {
      /**
       * 查询范围变化时立即作废旧范围内尚未完成的全量选择操作。
       * @param {string} newScope - 新的查询范围。
       * @param {string} oldScope - 原查询范围。
       * @returns {void}
       */
      handler(newScope, oldScope) {
        if (oldScope !== undefined && newScope !== oldScope) {
          this.selectionOperations.invalidate();
        }
      },
      flush: "sync",
    },
    "search.searchType": "handleRedundantSearchContextChange",
    "search.i18nURL": "handleRedundantSearchContextChange",
    hasEditingRows(editing) {
      this.syncOperationColumnWidth(editing);
    },
    rulesOptions: {
      deep: true,
      async handler() {
        // 勾选变更后按最新规则复检全部编辑行（不清空即放弃红字）
        await revalidateEditingRows(this);
      },
    },
    boxHeight(newval, oldval) {
      this.box = newval;
      this.setTableHeight();
    },
    currentProduct: {
      /**
       * 切换产品时同步产品状态并停止旧产品的校验轮询。
       * @param {Object} newval - 新选择的产品节点。
       * @returns {void}
       */
      handler(newval) {
        this.queryVersionGuard.invalidate();
        this.loading = false;
        this.stopRedundantPolling();
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
        this.selectedRows = this.selectEntry;
        this.selectedRowKeys = this.selectEntry.map((item) => item.id);
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
    /**
     * 初始化词条管理筛选项、版本及列表。
     * @returns {Promise<void>} 初始化流程。
     */
    async init() {
      // 获取一级分类
      await this.selectFirstClassify();
      // 版本列表和词条列表分别加载，避免初始化重复查询词条。
      if (this.product.type == "product") {
        this.getProductVersion();
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
    /**
     * 根据当前产品加载版本列表，不触发词条表查询。
     * @returns {Promise<void>} 版本列表请求。
     */
    getProductVersion() {
      if (Object.keys(this.product).length === 0) {
        return Promise.resolve();
      }
      const productID = resolveRequestProductID(this.product);
      if (productID === undefined) {
        this.productVersions = [];
        this.currentVersion = null;
        return Promise.resolve();
      }
      const params = { versionName: "", productID };
      return getVersionByName(params).then((res) => {
        this.productVersions = res.data.list;
        // // 当前产品版本设置为第一个版本
        // if (this.productVersions.length > 0) {
        //   this.currentVersion = this.productVersions[0].id;
        // } else {
        //   this.currentVersion = null;
        // }
      });
    },
    /**
     * 按当前查询类型执行查询，并让旧冗余校验轮询失效。
     * @param {Array} [accurate=[]] 准确查询条件。
     * @param {boolean} [isInit=false] 是否为首次点击状态树。
     * @returns {void}
     */
    conditionalQuery(accurate = [], isInit = false) {
      this.stopRedundantPolling();
      this.queryVersionGuard.invalidate();
      this.loading = false;
      // 将页码变为第一页
      this.pagination.current = 1;
      // this.selectedRowKeys = [];
      // this.selectedRows = [];
      // this.selectEntry = [];

      // 接口方法集合
      if (
        !this.search.searchType ||
        this.search.searchType == "getEntryByClassfy"
      ) {
        // 条件查询
        this.lastSearchType = "getEntryByClassfy";
        this.search.hasRedundantRls = false;
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
     * 根据当前分类和筛选条件查询词条。
     * @param {boolean} [isInit=false] 是否为首次点击状态树。
     * @param {Array} [accurate=[]] 全量查询条件。
     * @returns {void}
     */
    getEntryByClassfy(isInit = false, accurate = []) {
      this.stopRedundantPolling();
      const queryToken = this.queryVersionGuard.next();
      if (isInit) {
        if (this.product.type == "department") {
          this.dataSource = [];
          this.pagination.total = 0;
          this.loading = false;
          return; // 如果是部门，则不在点击状态树时查询版本
        }
      }
      if (Object.keys(this.product).length === 0) {
        this.loading = false;
        return;
      }
      if (
        (this.search.translate != "" ||
          this.search.translateState != null ||
          this.search.customCheckTransLength != null) &&
        !this.search.language
      ) {
        message.info("请选择翻译语种！");
        this.loading = false;
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
      if (this.search.customCheckTransLength != null) {
        data.customCheckTransLength = this.search.customCheckTransLength;
      }
      // data.entry = data.entry.replace(/\\n/g, '\n')
      // console.log("data:", data);
      if (!this.currentVersion) {
        const productID = resolveRequestProductID(this.product);
        if (productID !== undefined) data.productID = productID;
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
          if (!this.queryVersionGuard.isCurrent(queryToken)) return;
          this.dataSource = res.data.list;
          this.pagination.total = res.data.totalNum;
          this.paintModuleLimits();
        })
        .catch((err) => {
          if (!this.queryVersionGuard.isCurrent(queryToken)) return;
          message.error(err.message);
        })
        .finally(() => {
          if (this.queryVersionGuard.isCurrent(queryToken)) {
            this.loading = false;
          }
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
    /**
     * 查询冗余校验状态；执行中保持 loading 并轮询，无结果时等待用户明确发起。
     * @param {void} none - 无参数。
     * @returns {Promise<void>} 状态查询处理结果。
     */
    async getCheckNotUseEntry() {
      if (!this.search.i18nURL) {
        message.info("请选择i18nURL！");
        this.queryVersionGuard.invalidate();
        this.loading = false;
        return;
      }
      const queryToken = this.queryVersionGuard.next();
      const pollToken = this.redundantPollingToken;
      const params = {
        i18nURL: this.search.i18nURL,
        classfyID: this.currentProduct.key,
      };
      this.loading = true;
      this.search.hasRedundantRls = false;
      this.dataSource = [];
      this.pagination.total = 0;
      try {
        const res = await getCheckNotUseEntry(params);
        if (
          pollToken !== this.redundantPollingToken ||
          !this.queryVersionGuard.isCurrent(queryToken)
        )
          return;
        const state = Number(res.data?.state);
        this.handleRedundantCheckStatus(
          state,
          res,
          params,
          pollToken,
          false,
          queryToken
        );
      } catch (err) {
        if (
          pollToken !== this.redundantPollingToken ||
          !this.queryVersionGuard.isCurrent(queryToken)
        )
          return;
        this.loading = false;
        this.search.hasRedundantRls = true;
        message.error(err?.message || "冗余校验状态查询失败");
      }
    },
    /**
     * 重新发起当前产品和 i18nURL 的冗余校验。
     * @param {void} none - 无参数。
     * @returns {Promise<void>} 任务启动处理结果。
     */
    async reGetCheckNotUseEntry() {
      this.stopRedundantPolling();
      if (!this.search.i18nURL) {
        message.info("请选择i18nURL！");
        this.queryVersionGuard.invalidate();
        this.loading = false;
        return;
      }
      const queryToken = this.queryVersionGuard.next();
      const pollToken = this.redundantPollingToken;
      const params = {
        i18nURL: this.search.i18nURL,
        classfyID: this.currentProduct.key,
      };
      this.loading = true;
      this.search.hasRedundantRls = false;
      this.pagination.current = 1;
      this.dataSource = [];
      this.pagination.total = 0;
      await this.startRedundantCheck(params, pollToken, true, queryToken);
    },
    /**
     * 提交冗余校验任务，并根据接口状态决定提示或启动轮询。
     * @param {Object} params - 当前产品和 i18nURL 查询参数。
     * @param {number} pollToken - 用于忽略过期请求的轮询代次。
     * @param {boolean} isRestart - 是否为用户主动重新校验。
     * @param {number} queryToken - 当前列表查询的版本号。
     * @returns {Promise<void>} 任务提交处理结果。
     */
    async startRedundantCheck(params, pollToken, isRestart, queryToken) {
      try {
        const res = await checkNotUseEntry(params);
        if (
          pollToken !== this.redundantPollingToken ||
          !this.queryVersionGuard.isCurrent(queryToken)
        )
          return;
        const state = Number(res.data?.state);
        if (state === 3) {
          this.loading = true;
          this.search.hasRedundantRls = false;
          message.info("冗余词条正在校验中，将继续查询结果");
          this.startRedundantPolling(params, pollToken, queryToken);
          return;
        }
        if (state !== 0 && state !== 1) {
          this.loading = false;
          this.search.hasRedundantRls = true;
          message.error(
            state === 2
              ? "冗余词条校验执行异常，请重新查询"
              : `冗余校验未启动，任务状态：${this.getRedundantStatusLabel(state)}`
          );
          return;
        }
        this.loading = true;
        let startMessage = "冗余校验已启动，正在查询结果";
        if (state === 1) {
          startMessage = "冗余校验已提交，正在查询结果";
        } else if (isRestart) {
          startMessage = "已重新启动冗余校验，正在查询结果";
        }
        message.info(startMessage);
        this.startRedundantPolling(params, pollToken, queryToken);
      } catch (err) {
        if (
          pollToken !== this.redundantPollingToken ||
          !this.queryVersionGuard.isCurrent(queryToken)
        )
          return;
        this.loading = false;
        this.search.hasRedundantRls = true;
        message.error(err?.message || "冗余校验启动失败");
      }
    },
    /**
     * 每分钟查询一次冗余校验状态，直到结果返回或任务结束。
     * @param {Object} params - 当前产品和 i18nURL 查询参数。
     * @param {number} pollToken - 用于忽略过期请求的轮询代次。
     * @param {number} queryToken - 当前列表查询的版本号。
     * @returns {void}
     */
    startRedundantPolling(params, pollToken, queryToken) {
      if (
        pollToken !== this.redundantPollingToken ||
        !this.queryVersionGuard.isCurrent(queryToken)
      )
        return;
      this.stopRedundantPolling();
      this.loading = true;
      const activeToken = this.redundantPollingToken;
      let latestResponse = null;
      let hasNotifiedError = false;
      const controller = new MultiRequestPolling({
        interval: 60 * 1000,
        onError: (err) => {
          if (
            activeToken !== this.redundantPollingToken ||
            !this.queryVersionGuard.isCurrent(queryToken) ||
            hasNotifiedError
          )
            return;
          hasNotifiedError = true;
          message.error(err?.message || "冗余校验状态查询失败，将继续等待");
        },
      });
      this.redundantPollingController = controller;
      this.redundantPollingRequestId = controller.addRequest(
        async () => {
          if (
            activeToken !== this.redundantPollingToken ||
            !this.queryVersionGuard.isCurrent(queryToken)
          )
            return null;
          latestResponse = await getCheckNotUseEntry(params);
          if (
            activeToken !== this.redundantPollingToken ||
            !this.queryVersionGuard.isCurrent(queryToken)
          )
            return null;
          return Number(latestResponse.data?.state);
        },
        (state) => {
          if (
            activeToken !== this.redundantPollingToken ||
            !this.queryVersionGuard.isCurrent(queryToken)
          )
            return;
          this.handleRedundantCheckStatus(
            state,
            latestResponse,
            params,
            activeToken,
            true,
            queryToken
          );
        }
      );
    },
    /**
     * 应用冗余校验状态，统一更新结果、提示、轮询和表格 loading。
     * @param {number} state - 后端返回的校验状态。
     * @param {Object|null} response - 状态查询响应。
     * @param {Object} params - 当前产品和 i18nURL 查询参数。
     * @param {number} pollToken - 用于忽略过期响应的轮询代次。
     * @param {boolean} fromPolling - 当前状态是否来自定时轮询。
     * @param {number} queryToken - 当前列表查询的版本号。
     * @returns {void}
     */
    handleRedundantCheckStatus(
      state,
      response,
      params,
      pollToken,
      fromPolling,
      queryToken
    ) {
      if (
        pollToken !== this.redundantPollingToken ||
        !this.queryVersionGuard.isCurrent(queryToken)
      )
        return;
      if (state === 1) {
        this.stopRedundantPolling();
        this.search.hasRedundantRls = true;
        this.dataSource = response?.data?.list || [];
        this.pagination.total =
          response?.data?.totalNum ?? this.dataSource.length;
        this.pagination.current = 1;
        this.paintModuleLimits();
        this.loading = false;
        message.info(
          this.dataSource.length > 0
            ? `冗余校验完成，共发现 ${this.pagination.total} 条词条`
            : "冗余校验完成，没有发现冗余词条"
        );
        return;
      }
      if (state === 2) {
        this.stopRedundantPolling();
        this.dataSource = [];
        this.pagination.total = 0;
        this.search.hasRedundantRls = true;
        this.loading = false;
        message.error("冗余词条校验执行异常，请重新查询");
        return;
      }
      if (state === 3) {
        this.dataSource = [];
        this.pagination.total = 0;
        this.search.hasRedundantRls = false;
        this.loading = true;
        if (!fromPolling) {
          message.info("冗余词条正在校验中，将继续查询结果");
          this.startRedundantPolling(params, pollToken, queryToken);
        }
        return;
      }
      if (state === 0) {
        this.stopRedundantPolling();
        this.dataSource = [];
        this.pagination.total = 0;
        this.search.hasRedundantRls = true;
        this.loading = false;
        message.warning(
          fromPolling
            ? "冗余校验已结束或被其他任务替代，请重新查询"
            : "暂未查到当前结果（也可能其他产品或 URL 正在校验），如需发起校验请点击“重新查询”"
        );
        return;
      }
      this.stopRedundantPolling();
      this.dataSource = [];
      this.pagination.total = 0;
      this.search.hasRedundantRls = true;
      this.loading = false;
      message.warning(
        `冗余校验返回未知状态：${this.getRedundantStatusLabel(state)}`
      );
    },
    /**
     * 停止旧的冗余校验轮询并让尚未返回的响应失效。
     * @param {void} none - 无参数。
     * @returns {void}
     */
    stopRedundantPolling() {
      this.redundantPollingToken += 1;
      if (this.redundantPollingController) {
        if (this.redundantPollingRequestId) {
          this.redundantPollingController.removeRequest(
            this.redundantPollingRequestId
          );
        }
        this.redundantPollingController.stop();
      }
      this.redundantPollingController = null;
      this.redundantPollingRequestId = null;
      this.loading = false;
    },
    /**
     * 修改冗余校验类型或 i18nURL 时立即取消旧查询轮询。
     * @param {void} none - 无参数。
     * @returns {void}
     */
    handleRedundantSearchContextChange() {
      if (this.lastSearchType === "checkNotUseEntry") {
        this.stopRedundantPolling();
      }
    },
    /**
     * 返回冗余校验状态的用户可读名称。
     * @param {number} state - 后端返回的校验状态。
     * @returns {string} 状态名称。
     */
    getRedundantStatusLabel(state) {
      const labels = {
        0: "未启动或已被替代",
        1: "已完成",
        2: "执行异常",
        3: "正在校验",
      };
      return labels[state] || "未知";
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
      this.paintModuleLimits();
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
              if (
                record[col] &&
                cols.has(col) &&
                this.rules[record.id]?.[col]
              ) {
                try {
                  await validateEditableCell(this, record.id, col);
                } catch (err) {
                  setCellError(
                    this,
                    record.id,
                    col,
                    err.errorMessage || String(err)
                  );
                }
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
    // 从模块上限刷到词条行（只展示，不落词条表）
    paintModuleLimits(records = this.dataSource) {
      const walk = (list) => {
        if (!Array.isArray(list)) return;
        list.forEach((record) => {
          const limit = this.classifyLimit?.[record.classfy1];
          record.maxByte = limit?.maxByte ?? null;
          record.foreignMaxByte = limit?.foreignMaxByte ?? null;
          if (record.children?.length) walk(record.children);
        });
      };
      walk(records);
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
          this.paintModuleLimits();
        })
        .catch((err) => {
          message.err(err.message);
        });
    },
    // 详情
    entryDetails(record) {
      this.selectedRowIndex = record.id;
      this.currentEntry = record;
      // 仅投影原有的待审核、审核不通过、已审核三种详情状态。
      commonParam.languageList.forEach((item) => {
        const state = this.currentEntry[item.state];
        if (DETAIL_TRANSLATE_STATE_CODES.has(String(state))) {
          this.currentEntry[item.chineseState] = getTranslateStateLabel(state);
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
    async onCellInput(value, record, column) {
      onEditableCellInput(this, record.id, column.dataIndex, value);
      // change 即校验，与批量编辑一致；不必等失焦或保存才发现错误
      if (!this.editableData[record.id]) return;
      await applyCell(this, record.id, column.dataIndex);
    },
    async onCellBlur(record, column) {
      if (!this.editableData[record.id]) return;
      await applyCell(this, record.id, column.dataIndex);
    },
    /**
     * 校验并提交当前行编辑，成功状态由保存响应确认。
     * @param {string} id - 当前编辑词条 ID。
     * @returns {Promise<void>} 编辑保存流程。
     */
    async editSave(id) {
      const validateArrSum = ["entry", ...commonParam.langValList];
      const columnKeys = this.columns
        .map((column) => column.dataIndex)
        .filter((key) => validateArrSum.includes(key));

      for (const columnValue of columnKeys) {
        if (!this.rules[id]?.[columnValue]) continue;
        try {
          await validateEditableCell(this, id, columnValue);
        } catch (err) {
          setCellError(this, id, columnValue, err.errorMessage || String(err));
          return;
        }
      }

      if (id.startsWith("new") || id.startsWith("copy")) {
        // 新增词条/升级词条
        addSingleEntry(this.prepareEntryForSave(this.editableData[id])).then(
          (res) => {
            message.success("新增成功!");
            let index = this.dataSource.findIndex((item) => item.id === id);
            this.dataSource.splice(index, 1);
            this.dataSource.splice(index, 0, res.data);
            delete this.editableData[id];
            delete this.rowClassify2Option[id];
            this.pagination.total = this.pagination.total + 1;
          }
        );
      } else {
        this.confirmSave([this.prepareEntryForSave(this.editableData[id])]);
      }
    },
    // 批量保存
    batchSave() {
      const edit = [];
      for (let key in this.editableData) {
        edit.push(this.prepareEntryForSave(this.editableData[key]));
      }
      this.confirmSave(edit);
    },
    /**
     * 确认后保存；无输入框，仅确认。
     * @param {Object[]} rows
     */
    confirmSave(rows) {
      if (!rows || rows.length === 0) return;
      Modal.confirm({
        title: "确认保存当前修改？",
        okText: "确定",
        cancelText: "取消",
        style: { top: "30%" },
        onOk: () => this.saveRows(rows),
      });
    },
    /**
     * 批量落库并按逐条成功结果同步已选词条。
     * @param {Object[]} rows - 本次提交的词条。
     * @returns {Promise<void>} 批量保存流程。
     */
    async saveRows(rows) {
      try {
        const res = await updateEntryInfoList(rows, {
          notes: EDIT_NOTES,
        });
        const { successIds, failed } = partitionBatchUpdateResults(res);
        const successIdSet = new Set(successIds);

        for (const entry of rows) {
          if (successIdSet.has(entry.id)) {
            this.afterSave(entry);
          }
        }

        if (failed.length > 0) {
          const detail = failed
            .map((item) => `${item.id}: ${item.message}`)
            .join("；");
          message.warning(`有 ${failed.length} 条保存失败：${detail}`);
        }
        if (successIds.length > 0) {
          message.success(`已保存 ${successIds.length} 条！`);
        }
      } catch (err) {
        // 接口 reject：拦截器已提示
      }
    },
    /**
     * 单行保存成功后同步表格及已选词条缓存。
     * @param {Object} entry - 保存成功的词条数据。
     * @returns {void} 无返回值。
     */
    afterSave(entry) {
      delete this.editableData[entry.id];
      delete this.rules[entry.id];
      this.selectionOperations.refresh(entry);
      const index = this.dataSource.findIndex((item) => item.id === entry.id);
      if (index !== -1) {
        this.dataSource.splice(index, 1, entry);
      }
      const selectedIndex = this.selectEntry.findIndex(
        (item) => item.id === entry.id
      );
      if (selectedIndex !== -1) {
        this.selectEntry = this.selectedEntryOperations.update(
          this.selectEntry,
          entry
        );
        this.selectedRows = this.selectEntry;
        this.selectedRowKeys = this.selectEntry.map((item) => item.id);
      }
      this.syncSelectedProducts();
      delete this.rowClassify2Option[entry.id];
      this.getEntryByClassfy(false, this.accurSearch);
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
        customCheckTransLength: null,
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
    // 展示条件切换并保存用户偏好（由 ColumnFilter selectionOnly 持久化）
    applyColumnPreference() {
      applyTable(this, {
        allCols: entryAllCols,
        preset: entryPresets.productEntry,
        ctx: { pagination: this.pagination },
        colPrefName: "colPref-productEntry",
        normalWidth: 200,
        needFilter: false,
      });
      this.syncOperationColumnWidth();
    },
    syncColumnsFromPref() {
      applyTableColumnsFromPref(this);
      this.syncOperationColumnWidth();
    },
    /**
     * 按编辑状态更新不可拖拽操作列的业务基准宽度，避免按钮被裁切。
     * @param {boolean} [editing=this.hasEditingRows] 当前是否存在编辑行。
     * @returns {void} 无返回值。
     */
    syncOperationColumnWidth(editing = this.hasEditingRows) {
      const next = editing ? 130 : 80;
      const operationColumn = (this.columns || []).find(
        (column) => column.dataIndex === "operation"
      );
      if (
        !operationColumn ||
        (operationColumn.width === next && operationColumn.resizable === false)
      ) return;
      this.columns = this.columns.map((column) =>
        column.dataIndex === "operation"
          ? { ...column, width: next, resizable: false }
          : column
      );
    },
    clickInput(event) {
      event.stopPropagation();
    },
    // 判断当前状态树是否是产品及以下（以便封掉部门级使用不了的按钮）
    isProduct() {
      // console.log("当前产品", this.product.title);
      return this.product.type == "product" || this.product.type == "module";
    },
    /**
     * 返回选择计数使用的产品归属 ID。
     * @param {Object} product - 当前产品或模块。
     * @returns {string|number|undefined} 产品 ID。
     */
    getSelectionProductID(product) {
      return product.type === "module" ? product.parentId : product.key;
    },
    /**
     * 构造当前产品、版本及查询条件对应的全量词条请求。
     * @param {void} none - 本方法不接收参数。
     * @returns {{params: Object, data: Object}} 全量查询参数。
     */
    getCurrentSelectionQuery() {
      const data = {
        abbr: this.search.abbr,
        entry: this.search.entry,
        classfy1: cloneDeep(this.search.classfy1),
        classfy2: cloneDeep(this.search.classfy2),
        entryState:
          this.search.entryState == null
            ? cloneDeep(this.search.entryState_)
            : [this.search.entryState],
        tag: this.search.tag,
        entrySource: this.search.entrySource,
        comment: this.search.comment,
        diFileName: this.search.diFileName,
        update: this.search.update,
        filter_translate: this.search.filter_translate,
      };
      if (this.search.customCheckTransLength != null) {
        data.customCheckTransLength = this.search.customCheckTransLength;
      }
      if (!this.currentVersion) {
        const productID = resolveRequestProductID(this.product);
        if (productID !== undefined) data.productID = productID;
      } else {
        data.versionID = this.currentVersion;
      }
      commonParam.languageList.forEach((item) => {
        if (this.search.language === item.name) {
          data[item.value] = this.search.translate;
          data[item.state] = this.search.translateState;
        }
      });

      const params = {
        classfyID: this.getSelectionProductID(this.product),
        translateType: this.search.language,
        startTime: cloneDeep(this.search.startTime),
        endTime: cloneDeep(this.search.endTime),
        update: this.search.update,
        pageIndex: -1,
        pageSize: -1,
      };
      if (this.accurSearch && this.accurSearch.length > 0) {
        params.accurate = cloneDeep(this.accurSearch);
      }
      return { params, data };
    },
    /**
     * 生成识别当前完整选择查询的稳定键，用于隔离待取勾的表格即时反馈。
     * @param {void} none - 本方法不接收参数。
     * @returns {string} 当前查询范围的序列化键。
     */
    getCurrentSelectionScopeKey() {
      if (this.lastSearchType === "checkNotUseEntry") {
        return JSON.stringify({
          searchType: this.lastSearchType,
          classfyID: this.currentProduct?.key ?? this.product?.key,
          i18nURL: this.search.i18nURL,
        });
      }

      const { params, data } = this.getCurrentSelectionQuery();
      return JSON.stringify({
        searchType: this.lastSearchType,
        queryType: this.search.searchType,
        params,
        data,
      });
    },
    /**
     * 创建并登记一次全量选择或取勾请求。
     * @param {string} action - 请求类型，select 或 deselect。
     * @param {string} scopeKey - 发起请求时的完整查询范围键。
     * @returns {Object} 带顺序号和用户行级覆盖状态的请求记录。
     */
    createSelectionRequest(
      action,
      scopeKey = this.selectionScopeKey
    ) {
      return this.selectionOperations.begin(scopeKey, action === "select");
    },
    /**
     * 判断选择请求是否仍属于当前查询范围。
     * @param {Object} request - 需要校验的选择操作。
     * @returns {boolean} 请求是否仍有效。
     */
    isCurrentSelectionRequest(request) {
      return this.selectionOperations.isCurrent(
        request,
        this.selectionScopeKey
      );
    },
    /**
     * 从全局清单移除当前完整查询结果，并保护请求期间较新的行级勾选。
     * @param {Array} rows - 当前查询的完整结果集。
     * @param {Object} request - 当前取勾请求。
     * @returns {void}
     */
    removeCurrentQuerySelection(
      rows,
      request,
      currentScope = this.selectionScopeKey
    ) {
      this.selectEntry = this.selectionOperations.resolve(
        request,
        rows,
        this.selectEntry,
        currentScope
      );
      this.selectedRows = this.selectEntry;
      this.selectedRowKeys = this.selectEntry.map((item) => item.id);
      this.syncSelectedProducts();
    },
    /**
     * 移除当前搜索结果的已选词条，其他查询和产品的选择保持不变。
     * @param {void} none - 本方法不接收参数。
     * @returns {void}
     */
    clearCurrentQuerySelection() {
      const request = this.createSelectionRequest("deselect");
      if (this.lastSearchType === "checkNotUseEntry") {
        this.removeCurrentQuerySelection(this.dataSource, request);
        return;
      }

      const { params, data } = this.getCurrentSelectionQuery();
      getEntryByClassfy(params, data)
        .then((res) => {
          const currentScope = this.selectionScopeKey;
          if (!this.isCurrentSelectionRequest(request)) return;
          this.removeCurrentQuerySelection(
            res.data?.list || [],
            request,
            currentScope
          );
        })
        .catch((err) => {
          if (!this.isCurrentSelectionRequest(request)) return;
          this.selectionOperations.reject(request);
          message.error(err?.message || "取消当前查询选择失败");
        })
        .finally(() => {
          if (this.isCurrentSelectionRequest(request)) {
            this.selectionOperations.reject(request);
          }
        });
    },
    // 已选词条按钮点击事件
    viewCreateVersionEntry() {
      this.createVisible = true;
      setModalAriaHidden(this, document);
    },
    /**
     * 关闭已选词条弹窗并刷新版本和当前词条结果。
     * @returns {void}
     */
    createClose() {
      this.createVisible = false;
      this.getProductVersion();
      this.refreshTable();
    },
    /**
     * 刷新当前查询结果；冗余校验刷新会重新读取状态并按需恢复轮询。
     * @param {void} none - 无参数。
     * @returns {void}
     */
    refreshTable() {
      console.log("刷新查询", this.lastSearchType);
      if (this.lastSearchType === "checkNotUseEntry") {
        // 上一次为冗余词条校验查询，则刷新冗余校验结果
        this.stopRedundantPolling();
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
    /**
     * 按全局已选词条的 productID 重建弹窗需要的产品计数。
     * @param {void} none - 无参数。
     * @returns {void} 无返回值。
     */
    syncSelectedProducts() {
      const products = new Map();
      this.selectEntry.forEach((entry) => {
        const productID = normalizeOptionalID(entry?.productID);
        if (productID === undefined) return;
        products.set(productID, (products.get(productID) || 0) + 1);
      });
      this.selectedProducts = {
        products,
        totalNum: this.selectEntry.length,
      };
    },
    // 选择全部词条
    /**
     * 查询当前筛选下的全部词条并合并到已选列表。
     * @param {void} none 本方法不接收参数。
     * @returns {void}
     */
    selectAllEntry() {
      // 获取所有的词条
      if (Object.keys(this.product).length === 0) {
        return;
      }

      const request = this.createSelectionRequest("select");
      if (this.lastSearchType === "checkNotUseEntry") {
        this.selectEntry = this.selectionOperations.resolve(
          request,
          this.dataSource,
          this.selectEntry,
          request.scope
        );
        this.selectedRows = this.selectEntry;
        this.selectedRowKeys = this.selectEntry.map((item) => item.id);
        this.syncSelectedProducts();
        return;
      }

      const { params, data } = this.getCurrentSelectionQuery();
      getEntryByClassfy(params, data)
        .then((res) => {
          const currentScope = this.selectionScopeKey;
          if (!this.isCurrentSelectionRequest(request)) return;
          this.selectEntry = this.selectionOperations.resolve(
            request,
            res.data?.list || [],
            this.selectEntry,
            currentScope
          );
          this.selectedRows = this.selectEntry;
          this.selectedRowKeys = this.selectedRows.map((item) => item.id);
          this.syncSelectedProducts();
        })
        .catch((err) => {
          if (!this.isCurrentSelectionRequest(request)) return;
          this.selectionOperations.reject(request);
          message.error(err?.message || "全量选择失败");
        })
        .finally(() => {
          if (this.isCurrentSelectionRequest(request)) {
            this.selectionOperations.reject(request);
          }
        });
    },
    /**
     * 清空跨页、跨产品的已选词条数组。
     * @param {void} none - 本方法不接收参数。
     * @returns {void}
     */
    clearSelectedEntries() {
      this.selectionOperations.invalidate();
      this.selectEntry = this.selectedEntryOperations.clear();
      this.selectedRowKeys = [];
      this.selectedRows = [];
    },
    /**
     * 清空全部已选词条并关闭已选词条弹窗。
     * @param {void} none - 本方法不接收参数。
     * @returns {void}
     */
    cancelCreate() {
      this.cancelSelection();
      this.createVisible = false;
    },
    /**
     * 清空跨产品、跨查询的已选词条和产品计数。
     * @param {void} none - 本方法不接收参数。
     * @returns {void}
     */
    cancelSelection() {
      this.clearSelectedEntries();
      this.selectedProducts = {
        products: new Map(),
        totalNum: 0,
      };
    },
    /**
     * 同步已选列表更新，并记录全量选择请求期间被移除的词条。
     * @param {Array} rows - 弹窗更新后的已选词条列表。
     * @returns {void}
     */
    updateSelectedEntries(rows) {
      const nextIds = new Set(rows.map((item) => item.id));
      const removedRows = this.selectEntry.filter(
        (item) => !nextIds.has(item.id)
      );
      this.recordPendingSelectionChanges(
        removedRows.map((item) => item.id),
        rows.map((item) => item.id)
      );
      rows.forEach((item) => {
        this.selectionOperations.refresh(item);
      });
      this.selectEntry = this.selectedEntryOperations.merge([], rows);
      this.selectedRows = this.selectEntry;
      this.selectedRowKeys = this.selectEntry.map((item) => item.id);
      this.syncSelectedProducts();
    },
    /**
     * 将单行或弹窗中的选择变化同步到所有进行中的全量请求。
     * @param {Array} excludedIds - 用户主动取消选择的词条 ID。
     * @param {Array} includedIds - 用户主动恢复选择的词条 ID。
     * @param {string|null} scope - 查询范围；null 表示弹窗中的全局选择变更。
     * @returns {void}
     */
    recordPendingSelectionChanges(excludedIds = [], includedIds = [], scope = null) {
      excludedIds.forEach((id) =>
        this.selectionOperations.record(id, false, scope)
      );
      includedIds.forEach((id) =>
        this.selectionOperations.record(id, true, scope)
      );
    },
    //新增词条
    /**
     * 在当前产品下新增一条本地编辑行。
     * @param {void} none 本方法不接收参数。
     * @returns {void}
     */
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
        maxLength:
          this.product.type === "module" ? this.product.foreignMaxByte : "",
        maxByte: this.product.type === "module" ? this.product.maxByte : "",
        foreignMaxByte:
          this.product.type === "module" ? this.product.foreignMaxByte : "",
        versionID: this.currentVersion,
        entryVersion: 0,
      };
      const productID = resolveRequestProductID(this.product);
      if (productID !== undefined) newData.productID = productID;
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
        const container =
          this.$refs.entryTable?.$el?.querySelector(".ant-table-body");
        if (container) container.scrollTop = container.scrollHeight;
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
    prepareEntryForSave(row) {
      const payload = { ...row };
      for (const key of ["entryLength", "maxLength"]) {
        const v = payload[key];
        if (v == null || v === "") {
          payload[key] = "";
        } else if (typeof v === "number") {
          payload[key] = String(v);
        }
      }
      return payload;
    },
    formatEntryText,
    formatCellText,
    formatMaxLengthText,
    getEntryStateLabel,
    getTranslateStateLabel,
    getTranslateStateStatus,
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
    /**
     * 根据已选词条同步表格行键和行数据。
     * @returns {void}
     */
    onSelectChange() {
      this.selectedRows = this.selectEntry;
      this.selectedRowKeys = this.selectEntry.map((item) => item.id);
    },
    /**
     * 同步单行勾选到跨页、跨产品的已选词条列表。
     * @param {Object} record - 当前操作的词条。
     * @param {boolean} selected - 该词条是否被勾选。
     * @returns {void}
     */
    onSelect(record, selected) {
      this.recordPendingSelectionChanges(
        selected ? [] : [record.id],
        selected ? [record.id] : [],
        this.getCurrentSelectionScopeKey()
      );
      if (selected) {
        this.selectEntry = this.selectedEntryOperations.update(
          this.selectEntry,
          record
        );
      } else {
        this.selectEntry = this.selectedEntryOperations.remove(
          this.selectEntry,
          [record.id]
        );
      }
      this.selectedRows = this.selectEntry;
      this.selectedRowKeys = this.selectEntry.map((item) => item.id);
      this.syncSelectedProducts();
    },
    /**
     * 将表头勾选应用到当前筛选结果全集，取勾时仅移除当前查询结果。
     * @param {boolean} selected - 表头复选框是否处于勾选状态。
     * @param {Array} _selectedRows - 表格当前勾选行。
     * @param {Array} changeRows - 本次表头操作影响的当前页词条。
     * @returns {void}
     */
    onSelectAll(selected, _selectedRows, changeRows) {
      const changedIds = (changeRows || []).map((item) => item.id);
      const scope = this.getCurrentSelectionScopeKey();
      if (selected) {
        this.recordPendingSelectionChanges(
          [],
          changedIds,
          scope
        );
        this.selectEntry = this.selectedEntryOperations.merge(
          this.selectEntry,
          changeRows || []
        );
        this.selectedRows = this.selectEntry;
        this.selectedRowKeys = this.selectEntry.map((item) => item.id);
        this.syncSelectedProducts();
        this.selectAllEntry();
        return;
      }
      this.recordPendingSelectionChanges(changedIds, [], scope);
      this.clearCurrentQuerySelection();
    },
    /**
     * 切换表格分页；冗余校验结果已完整载入，只进行本地分页。
     * @param {number} page - 目标页码。
     * @param {number} pageSize - 每页条数。
     * @param {Function|null} [refreshFn] - 普通查询使用的数据刷新回调。
     * @returns {void}
     */
    pageChange(page, pageSize, refreshFn) {
      let fetchData = refreshFn;
      if (fetchData === undefined) fetchData = this.getEntryByClassfy;
      if (this.lastSearchType === "checkNotUseEntry") fetchData = null;
      pageChange(
        this,
        page,
        pageSize,
        fetchData,
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

.entry-table-holder {
  width: 100%;
  position: absolute;
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

:deep(.tag-content:hover) {
  white-space: nowrap;
}

:deep(.ant-pagination) {
  margin: 8px 0px 0px 0px;
}

.editable-row-operations {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.editable-row-operations > span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.entry-toolbar {
  display: flex;
  width: 100%;
  align-items: flex-start;
  gap: 8px;
}

.entry-toolbar-left {
  flex: 0 0 50%;
  max-width: 50%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
}

.entry-toolbar-version {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 24px;
  color: black;
  white-space: nowrap;
  max-width: 100%;
}

.entry-toolbar-version-label {
  flex-shrink: 0;
  line-height: 24px;
}

.entry-toolbar-version-select {
  width: 150px;
  max-width: 100%;
  flex: 1 1 auto;
}

.entry-toolbar-rules {
  display: flex;
  align-items: center;
  height: 24px;
}

.entry-toolbar-right {
  flex: 0 0 50%;
  max-width: 50%;
  justify-content: flex-end;
}

.entry-toolbar-right :deep(.entry-toolbar-add-button.ant-btn-primary:disabled),
.entry-toolbar-right
  :deep(.entry-toolbar-add-button.ant-btn-primary.ant-btn-disabled) {
  color: rgba(0, 0, 0, 0.25);
  background-color: #f5f5f5;
  border-color: #d9d9d9;
  box-shadow: none;
  cursor: not-allowed;
}

</style>
