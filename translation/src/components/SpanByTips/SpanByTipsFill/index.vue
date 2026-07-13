<template>
  <span class="span-by-tips-fill-host" :style="hostStyle">
    <a-tooltip
      :title="isOverflow ? tooltipContent : ''"
      :placement="placement"
      :mouse-enter-delay="mouseEnterDelay"
      overlay-class-name="span-by-tips-fill-overlay"
    >
      <component
        :is="tag"
        ref="textRef"
        :class="innerClass"
        @mouseenter="onMouseEnter"
        @mouseleave="onMouseLeave"
      >
        {{ display }}
      </component>
    </a-tooltip>
  </span>
</template>

<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  useAttrs,
  watch,
} from "vue";

defineOptions({ inheritAttrs: false });

const props = defineProps({
  /** 展示与 tooltip 全文；空/空白时界面显示 emptyText */
  content: {
    type: String,
    default: "",
  },
  tag: {
    type: String,
    default: "span",
    validator: (v) => ["span", "div"].includes(v),
  },
  placement: {
    type: String,
    default: "top",
  },
  mouseEnterDelay: {
    type: Number,
    default: 0.3,
  },
  emptyText: {
    type: String,
    default: "-",
  },
  /** 文案区最大宽度；须落在外层 host 上（不能写在 a-tooltip 上） */
  maxWidth: {
    type: [Number, String],
    default: 100,
  },
});

const attrs = useAttrs();
const textRef = ref(null);
const isOverflow = ref(false);
let resizeObserver = null;

const display = computed(() =>
  props.content?.trim() ? props.content : props.emptyText
);
const tooltipContent = computed(() => props.content?.trim() ?? "");
const innerClass = computed(() => ["span-by-tips-fill", attrs.class]);

const maxWidthCss = computed(() => {
  const value = props.maxWidth;
  if (value == null || value === "") return "100px";
  return typeof value === "number" ? `${value}px` : String(value);
});

const hostStyle = computed(() => ({
  display: "block",
  minWidth: 0,
  overflow: "hidden",
  verticalAlign: "middle",
  maxWidth: maxWidthCss.value,
}));

function checkOverflow() {
  const el = textRef.value;
  if (!el) {
    isOverflow.value = false;
    return;
  }
  isOverflow.value =
    Boolean(tooltipContent.value) && el.scrollWidth > el.clientWidth;
}

function onMouseEnter() {
  checkOverflow();
}

function onMouseLeave() {
  isOverflow.value = false;
}

watch(
  () => props.content,
  () => nextTick(checkOverflow)
);

onMounted(() => {
  nextTick(() => {
    checkOverflow();
    const el = textRef.value;
    if (typeof ResizeObserver === "undefined" || !el) return;
    resizeObserver = new ResizeObserver(checkOverflow);
    resizeObserver.observe(el);
  });
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  resizeObserver = null;
});
</script>

<style scoped lang="less">
.span-by-tips-fill-host {
  max-width: 100%;
}

.span-by-tips-fill {
  display: block;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

<style lang="less">
.span-by-tips-fill-overlay {
  max-width: 400px;
  word-break: break-word;
}
</style>
