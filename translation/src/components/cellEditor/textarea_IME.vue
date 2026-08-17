<template>
  <a-textarea
    v-model:value="innerValue"
    :style="textareaStyle"
    :auto-size="autoSize"
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
        margin: "-5px 0",
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
  },
  methods: {
    onCompositionStart() {
      this.isComposing = true;
    },
    onCompositionEnd(event) {
      this.isComposing = false;
      const v =
        (event && event.target && event.target.value) ?? this.innerValue ?? "";
      this.innerValue = v;
      this.$emit("update:value", v);
    },
    onBlur() {
      if (!this.isComposing) {
        this.$emit("update:value", this.innerValue ?? "");
      }
    },
  },
};
</script>
