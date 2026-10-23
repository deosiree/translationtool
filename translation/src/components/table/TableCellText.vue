<!-- @fileoverview 统一表格文本单元格的浏览省略与多行编辑呈现。 -->
<template>
  <div
    class="table-cell-text"
    :class="{ 'table-cell-text--editing': editing }"
    @dblclick="!editing && $emit('dblclick', $event)"
  >
    <TableCellTextArea
      v-if="editing"
      class="table-cell-text__editor"
      :value="value"
      :auto-size="autoSize"
      :error-message="errorMessage"
      @update:value="$emit('update:value', $event)"
      @blur="$emit('blur', $event)"
    />
    <CellOverflowTooltip
      v-else
      :content="text"
      :empty-text="emptyText"
    >
      <span class="table-cell-text__display">{{ text }}</span>
    </CellOverflowTooltip>
  </div>
</template>

<script>
import CellOverflowTooltip from "./CellOverflowTooltip.vue";
import TableCellTextArea from "./TableCellTextArea.vue";

export default {
  name: "TableCellText",
  components: { CellOverflowTooltip, TableCellTextArea },
  props: {
    text: {
      type: [String, Number],
      default: "",
    },
    value: {
      type: String,
      default: "",
    },
    editing: {
      type: Boolean,
      default: false,
    },
    emptyText: {
      type: String,
      default: "",
    },
    autoSize: {
      type: [Object, Boolean],
      default: () => ({ minRows: 1, maxRows: 5 }),
    },
    errorMessage: {
      type: String,
      default: "",
    },
  },
  emits: ["dblclick", "update:value", "blur"],
};
</script>

<style scoped lang="less">
.table-cell-text {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  text-align: left;
}

.table-cell-text__display {
  display: block;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-cell-text--editing :deep(.table-cell-editor),
.table-cell-text--editing :deep(.ant-input-textarea) {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
}

.table-cell-text--editing :deep(textarea.ant-input) {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  resize: none;
  overflow-x: hidden !important;
  word-break: break-word;
  white-space: pre-wrap;
}
</style>
