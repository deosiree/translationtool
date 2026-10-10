<template>
  <a-textarea
    v-model:value="innerValue"
    :style="textareaStyle"
    :auto-size="autoSize"
    @click.stop
    @blur="$emit('blur', $event)"
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
  emits: ["update:value", "blur"],
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
      const next =
        (event && event.target && event.target.value) ?? this.innerValue ?? "";
      this.innerValue = next;
      // 组字期间 v-model 可能已把终值写进 innerValue；赋值相同不会触发 watch，必须强制回写父级
      this.$emit("update:value", next);
    },
  },
};
</script>
