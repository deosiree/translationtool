<template>
  <a-input
    v-model:value="innerValue"
    style="margin: -5px 0"
    @click.stop
    @compositionstart="onCompositionStart"
    @compositionend="onCompositionEnd"
    @pressEnter="onPressEnter"
    @change="onChange"
  />
</template>

<script>
export default {
  name: "InputIME",
  props: {
    value: {
      type: String,
      default: "",
    },
  },
  emits: ["update:value", "pressEnter", "change"],
  data() {
    return {
      innerValue: this.value ?? "",
      isComposing: false,
    };
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
    onPressEnter(event) {
      this.$emit("pressEnter", event);
    },
    onChange(event) {
      this.$emit("change", event);
    },
  },
};
</script>
