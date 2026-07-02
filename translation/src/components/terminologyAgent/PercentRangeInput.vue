<template>
  <a-form-item-rest>
    <div class="percent-range">
      <a-input-number
        :value="min"
        :min="0"
        :max="100"
        placeholder="最小"
        class="percent-range__input"
        :formatter="percentFormatter"
        :parser="percentParser"
        @update:value="onMinChange"
        @blur="onBlur"
      />
      <span class="percent-range__sep">~</span>
      <a-input-number
        :value="max"
        :min="0"
        :max="100"
        placeholder="最大"
        class="percent-range__input"
        :formatter="percentFormatter"
        :parser="percentParser"
        @update:value="onMaxChange"
        @blur="onBlur"
      />
    </div>
  </a-form-item-rest>
</template>

<script>
/**
 * 百分数开放区间输入：最小 ~ 最大，任一端可留空。
 */
export default {
  name: "PercentRangeInput",
  props: {
    min: {
      type: Number,
      default: null,
    },
    max: {
      type: Number,
      default: null,
    },
  },
  emits: ["update:min", "update:max"],
  methods: {
    percentFormatter(value) {
      if (value == null || value === "") return "";
      return `${value}%`;
    },
    percentParser(value) {
      if (value == null || value === "") return null;
      const parsed = String(value).replace(/%/g, "").trim();
      if (parsed === "") return null;
      const num = Number(parsed);
      return Number.isNaN(num) ? null : num;
    },
    onMinChange(value) {
      this.$emit("update:min", value);
    },
    onMaxChange(value) {
      this.$emit("update:max", value);
    },
    onBlur() {
      const min = this.min;
      const max = this.max;
      if (min != null && max != null && min > max) {
        this.$emit("update:min", max);
        this.$emit("update:max", min);
      }
    },
  },
};
</script>

<style scoped lang="less">
.percent-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.percent-range__input {
  width: 80px;
}

.percent-range__sep {
  color: rgba(0, 0, 0, 0.45);
  line-height: 32px;
  user-select: none;
}
</style>
