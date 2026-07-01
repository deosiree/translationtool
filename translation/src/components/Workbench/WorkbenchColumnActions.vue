<template>
  <template v-if="showCoverButton">
    <CoverButton
      v-bind="coverButtonProps"
      size="small"
      button-title="释义覆盖翻译"
      @update:oldEditableData="$emit('update:oldEditableData', $event)"
      @showEditOperation="$emit('showEditOperation', $event)"
    />
  </template>
  <ColumnFilter
    :model-value="modelValue"
    :columns="columns"
    :overlay-style="overlayStyle"
    :col-pref-name="colPrefName"
    :normal-width="normalWidth"
    :need-filter="needFilter"
    @update:model-value="$emit('update:modelValue', $event)"
    @change="$emit('change', $event)"
  />
</template>

<script>
import CoverButton from "@/components/Button/coverButton/inter2value.vue";
import ColumnFilter from "@/components/ColumnFilter/ColumnFilter.vue";

export default {
  name: "WorkbenchColumnActions",
  components: {
    CoverButton,
    ColumnFilter,
  },
  props: {
    modelValue: {
      type: Array,
      default: () => [],
    },
    columns: {
      type: Array,
      default: () => [],
    },
    overlayStyle: {
      type: Object,
      default: () => ({}),
    },
    colPrefName: {
      type: String,
      required: true,
    },
    normalWidth: {
      type: Number,
      default: 100,
    },
    needFilter: {
      type: Boolean,
      default: false,
    },
    showCoverButton: {
      type: Boolean,
      default: false,
    },
    coverButtonProps: {
      type: Object,
      default: () => ({}),
    },
  },
  emits: [
    "update:modelValue",
    "change",
    "update:oldEditableData",
    "showEditOperation",
  ],
};
</script>
