<template>
  <a-textarea
    v-model:value="innerValue"
    :style="textareaStyle"
    :auto-size="autoSize"
    @click.stop
    @compositionstart="onCompositionStart"
    @compositionend="onCompositionEnd"
  />
</template>

<script>
export default {
  name: "TextAreaIME",
  props: {
    value: {
      type: String,
      default: "",
    },
    autoSize: {
      type: [Object, Boolean],
      default: () => ({ minRows: 1 }),
    },
  },
  emits: ["update:value"],
  data() {
    return {
      innerValue: this.value ?? "",
      isComposing: false,
    };
  },
  computed: {
    textareaStyle() {
      return {
        margin: "2px 0",
        width: "100%",
        boxSizing: "border-box",
      };
    },
  },
  watch: {
    value(newVal) {
      if (this.isComposing) return;
      if (newVal !== this.innerValue) {
        this.innerValue = newVal ?? "";
      }
    },
    innerValue(newVal) {
      if (this.isComposing) return;
      this.$emit("update:value", newVal ?? "");
    },
  },
  methods: {
    onCompositionStart() {
      this.isComposing = true;
    },
    onCompositionEnd(event) {
      this.isComposing = false;
      this.innerValue =
        (event && event.target && event.target.value) ?? this.innerValue ?? "";
    },
  },
};
</script>
