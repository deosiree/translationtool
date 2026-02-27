<template>
  <a-textarea
    v-model:value="innerValue"
    style="margin: -5px 0"
    :auto-size="{ minRows: 1 }"
    @click.stop
    @compositionstart="onCompositionStart"
    @compositionend="onCompositionEnd"
    @blur="onBlur"
  />
</template>

<script>
export default {
  name: "TextAreaIME",
  props: {
    // v-model:value 绑定的值
    value: {
      type: String,
      default: "",
    },
  },
  emits: ["update:value"],
  data() {
    return {
      innerValue: this.value ?? "",
      isComposing: false,
    };
  },
  watch: {
    // 外部值变更时，同步到内部，但在组合输入期间不打断输入法
    value(newVal) {
      if (this.isComposing) return;
      if (newVal !== this.innerValue) {
        this.innerValue = newVal ?? "";
      }
    },
  },
  methods: {
    onCompositionStart() {
      this.isComposing = true;
      // console.log("onCompositionStart");
    },
    onCompositionEnd(event) {
      this.isComposing = false;
      const v =
        (event && event.target && event.target.value) ?? this.innerValue ?? "";
      this.innerValue = v;
      // console.log("onCompositionEnd", v);
      // 组合结束时同步一次到父组件
      this.$emit("update:value", v);
    },
    onBlur() {
      // 失焦时再同步一次，确保最终值写回
      if (!this.isComposing) {
        this.$emit("update:value", this.innerValue ?? "");
        // console.log("onBlur", this.innerValue);
      }
    },
  },
};
</script>

