<template>
  <span class="workbench-language-filter">
    <span class="workbench-language-filter__label">过滤语种：</span>
    <a-radio-group
      :value="modelValue"
      name="radioGroup"
      @change="onChange"
    >
      <a-radio value="全部">全部</a-radio>
      <a-radio value="中文">中文</a-radio>
      <a-radio value="英文">英文</a-radio>
    </a-radio-group>
  </span>
</template>

<script>
export default {
  name: "WorkbenchLanguageFilter",
  props: {
    modelValue: {
      type: String,
      default: null,
    },
  },
  emits: ["update:modelValue", "change"],
  methods: {
    onChange(e) {
      const value = e?.target?.value ?? e;
      this.$emit("update:modelValue", value);
      this.$emit("change", value);
    },
  },
};
</script>

<style scoped>
.workbench-language-filter {
  margin-left: 10px;
}
.workbench-language-filter__label {
  margin-right: 0;
}
</style>
