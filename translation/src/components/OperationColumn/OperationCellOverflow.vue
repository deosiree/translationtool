<template>
  <div class="operation-buttons operation-buttons--overflow" :style="{ gap: `${gap}px` }">
    <div ref="inlineEl" class="operation-buttons-inline" :style="inlineStyle">
      <slot />
    </div>
    <a-dropdown
      v-if="overflowNodes.length > 0"
      :trigger="['click']"
      placement="bottomRight"
      :getPopupContainer="popupContainer"
    >
      <a-button type="link" size="small" class="operation-column-more-trigger" @click.stop>
        更多
      </a-button>
      <template #overlay>
        <a-menu class="operation-column-more-menu" @click="onMenuClick">
          <a-menu-item
            v-for="(entry, idx) in oflowItems"
            :key="String(idx)"
            class="operation-column-more-item"
          >
            <OpItemContent
              :label="entry.meta.label"
              :icon="entry.meta.icon"
              :icon-class="entry.meta.iconClass"
              :type="entry.meta.type"
              variant="menu"
            />
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>

<script>
/**
 * @file 表格操作列溢出区：按 inlineVisibleCount 切分行内/「更多」。
 * @module OperationColumn/OperationCellOverflow
 */
import { computed, nextTick, onMounted, onUpdated, ref, shallowRef, watch } from "vue";
import OpItemContent from "./OpItemContent.vue";
import { calcOpStrip, readOpMeta } from "./operationWidth";

export default {
  name: "OperationCellOverflow",
  components: { OpItemContent },
  props: {
    /** 行内条总槽位数（含「更多」占 1 槽），最小 1 */
    inlineVisibleCount: { type: Number, default: 1 },
    gap: { type: Number, default: 8 },
    cellMaxHeight: { type: Number, default: undefined },
    /** 列宽探针代际；变化时重新切分各行溢出 */
    widthEpoch: { type: Number, default: 0 },
  },
  setup(props) {
    const inlineEl = ref(null);
    const overflowNodes = shallowRef([]);
    const lastOpSig = ref("");

    const oflowItems = computed(() =>
      overflowNodes.value.map((el) => ({
        el,
        meta: readOpMeta(el),
      }))
    );

    const inlineStyle = computed(() => {
      const base = { gap: `${props.gap}px` };
      if (props.cellMaxHeight == null) return base;
      return {
        ...base,
        maxHeight: `${props.cellMaxHeight}px`,
        flexWrap: "wrap",
        overflow: "hidden",
      };
    });

    /** @param {HTMLElement} inline */
    function getInlineOps(inline) {
      return Array.from(inline.querySelectorAll(".operation-column-op-item"));
    }

    /** @param {HTMLElement} inline */
    function inlineOpSig(inline) {
      return getInlineOps(inline)
        .map(
          (el) =>
            `${el.dataset.opLabel ?? ""}|${
              el.classList.contains("operation-column-op-item--hidden") ? 1 : 0
            }`
        )
        .join("\x1f");
    }

    function refreshSplit() {
      const inline = inlineEl.value;
      if (!inline) return;

      const items = getInlineOps(inline);
      const { inlineOpCount, showMore } = calcOpStrip(
        items.length,
        props.inlineVisibleCount ?? 1
      );

      items.forEach((el, i) => {
        if (i < inlineOpCount) {
          el.classList.remove("operation-column-op-item--hidden");
        } else {
          el.classList.add("operation-column-op-item--hidden");
        }
      });

      const next = showMore ? items.slice(inlineOpCount) : [];
      const prev = overflowNodes.value;
      if (prev.length === next.length && prev.every((el, i) => el === next[i])) {
        return;
      }
      overflowNodes.value = next;
    }

    function applyOverflowLayout() {
      refreshSplit();
      const inline = inlineEl.value;
      if (inline) {
        lastOpSig.value = inlineOpSig(inline);
      }
    }

    function schedOvSync() {
      nextTick(() => {
        nextTick(applyOverflowLayout);
      });
    }

    /**
     * @param {HTMLElement} node
     */
    function triggerAction(node) {
      const wasHidden = node.classList.contains("operation-column-op-item--hidden");
      if (wasHidden) node.classList.remove("operation-column-op-item--hidden");
      node.click();
      if (wasHidden) node.classList.add("operation-column-op-item--hidden");
    }

    /**
     * @param {{ key?: string | number, domEvent?: Event }} info
     */
    function onMenuClick(info) {
      info?.domEvent?.stopPropagation?.();
      const entryIdx = Number(info?.key);
      const entry = oflowItems.value[entryIdx];
      if (entry?.el) triggerAction(entry.el);
    }

    onMounted(schedOvSync);

    onUpdated(() => {
      const inline = inlineEl.value;
      if (!inline) return;
      if (inlineOpSig(inline) === lastOpSig.value) return;
      schedOvSync();
    });

    watch(() => props.inlineVisibleCount, schedOvSync);
    watch(() => props.widthEpoch, schedOvSync);

    return {
      inlineEl,
      overflowNodes,
      oflowItems,
      inlineStyle,
      onMenuClick,
      popupContainer: () => document.body,
    };
  },
};
</script>

<style scoped>
.operation-buttons--overflow {
  display: inline-flex;
  align-items: center;
  width: max-content;
  min-width: 0;
  height: 24px;
}

.operation-buttons-inline {
  display: inline-flex;
  flex-shrink: 0;
  flex-wrap: nowrap;
  align-items: center;
}

.operation-column-more-trigger {
  flex-shrink: 0;
  min-width: 28px;
  height: 24px;
  padding: 0 2px;
  font-size: 14px;
  line-height: 24px;
}

.operation-column-more-trigger.ant-btn-sm {
  font-size: 14px;
}
</style>

<style>
.operation-column-more-menu .operation-column-more-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 4px 8px;
}
</style>
