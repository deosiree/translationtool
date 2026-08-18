<template>
  <div class="table-cell-editor" :class="{ 'table-cell-editor--error': !!errorMessage }">
    <TextArea
      :value="value"
      @update:value="$emit('update:value', $event)"
      :autoSize="autoSize"
    />
    <div v-if="errorMessage" class="table-cell-editor-error">
      {{ errorMessage }}
    </div>
  </div>
</template>

<script>
import TextArea from "@/components/cellEditor/textarea_IME.vue";

export default {
  name: "TableCellTextArea",
  components: { TextArea },
  props: {
    value: {
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
  emits: ["update:value"],
};
</script>
