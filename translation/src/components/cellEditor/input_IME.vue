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
      this.innerValue =
        (event && event.target && event.target.value) ?? this.innerValue ?? "";
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
