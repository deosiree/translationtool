<template>
  <span class="span-by-tips-fill-host" :style="hostStyle">
    <!-- 可复制 / 黑色长文：用 popover，鼠标可移入选中与点「复制」 -->
    <a-popover
      v-if="richTip"
      :visible="popoverOpen"
      :placement="placement"
      :mouse-enter-delay="mouseEnterDelay"
      :overlay-class-name="overlayClassName"
      trigger="hover"
      destroy-tooltip-on-hide
      @visibleChange="onPopoverVisible"
    >
      <template #content>
        <div
          class="span-by-tips-fill-tip"
          :class="{ 'span-by-tips-fill-tip--dark': theme === 'dark' }"
          @mousedown.stop
          @click.stop
        >
          <pre class="span-by-tips-fill-tip-text">{{ tooltipContent }}</pre>
          <button
            v-if="copyable"
            type="button"
            class="span-by-tips-fill-copy"
            @click.stop.prevent="onCopy"
          >
            {{ copied ? "已复制" : "复制" }}
          </button>
        </div>
      </template>
      <component
        :is="tag"
        ref="textRef"
        :class="innerClass"
        @mouseenter="onMouseEnter"
      >
        {{ display }}
      </component>
    </a-popover>

    <!-- 原浅色省略 tip：仅溢出时展示 -->
    <a-tooltip
      v-else
      :title="tipTitle"
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
import { message } from "ant-design-vue";

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
  /** light=默认浅色；dark=黑色悬浮 */
  theme: {
    type: String,
    default: "light",
    validator: (v) => ["light", "dark"].includes(v),
  },
  /** 悬浮层可点选复制（黑色浮层 + 复制按钮） */
  copyable: {
    type: Boolean,
    default: false,
  },
  /**
   * true：有内容即出 tip（便于复制长文）；
   * false：仅省略溢出时出 tip（原行为）
   */
  alwaysTip: {
    type: Boolean,
    default: false,
  },
});

const attrs = useAttrs();
const textRef = ref(null);
const isOverflow = ref(false);
const popoverOpen = ref(false);
const copied = ref(false);
let resizeObserver = null;
let copiedTimer = null;

const display = computed(() =>
  props.content?.trim() ? props.content : props.emptyText
);
const tooltipContent = computed(() => props.content?.trim() ?? "");
const hasTipContent = computed(() => Boolean(tooltipContent.value));
const tipActive = computed(
  () =>
    hasTipContent.value &&
    (props.alwaysTip || props.copyable || isOverflow.value)
);
const tipTitle = computed(() => (tipActive.value ? tooltipContent.value : ""));
const richTip = computed(
  () => props.copyable || props.theme === "dark" || props.alwaysTip
);
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

const overlayClassName = computed(() => {
  const parts = ["span-by-tips-fill-popover"];
  if (props.theme === "dark") parts.push("span-by-tips-fill-popover--dark");
  if (props.copyable) parts.push("span-by-tips-fill-popover--copyable");
  return parts.join(" ");
});

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

/**
 * @param {boolean} open
 * @returns {void}
 */
function onPopoverVisible(open) {
  if (open && !tipActive.value) {
    popoverOpen.value = false;
    return;
  }
  popoverOpen.value = open;
  if (!open) copied.value = false;
}

/**
 * 复制全文到剪贴板
 * @returns {Promise<void>}
 */
async function onCopy() {
  const text = tooltipContent.value;
  if (!text) return;
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
    } else {
      const ta = document.createElement("textarea");
      ta.value = text;
      ta.style.position = "fixed";
      ta.style.left = "-9999px";
      document.body.appendChild(ta);
      ta.select();
      document.execCommand("copy");
      document.body.removeChild(ta);
    }
    copied.value = true;
    message.success("已复制到剪贴板");
    if (copiedTimer) clearTimeout(copiedTimer);
    copiedTimer = setTimeout(() => {
      copied.value = false;
    }, 1500);
  } catch (e) {
    message.error(e?.message || "复制失败");
  }
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
  if (copiedTimer) clearTimeout(copiedTimer);
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

.span-by-tips-fill-popover {
  max-width: 520px;
}

.span-by-tips-fill-popover--dark {
  .ant-popover-inner {
    background: #141414;
  }
  .ant-popover-inner-content {
    color: rgba(255, 255, 255, 0.92);
  }
  .ant-popover-arrow-content {
    background: #141414;
  }
}

.span-by-tips-fill-popover--copyable {
  .ant-popover-inner-content {
    user-select: text;
    cursor: text;
  }
}

.span-by-tips-fill-tip {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 480px;
}

.span-by-tips-fill-tip--dark {
  color: rgba(255, 255, 255, 0.92);
}

.span-by-tips-fill-tip-text {
  margin: 0;
  max-height: 280px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
  user-select: text;
  cursor: text;
}

.span-by-tips-fill-copy {
  align-self: flex-end;
  padding: 2px 10px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 4px;
  background: transparent;
  color: rgba(255, 255, 255, 0.92);
  font-size: 13px;
  line-height: 1.5;
  cursor: pointer;

  &:hover {
    border-color: rgba(255, 255, 255, 0.65);
    background: rgba(255, 255, 255, 0.08);
  }
}
</style>
