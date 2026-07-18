import { nextTick, onBeforeUnmount, onMounted, ref } from "vue";

/**
 * 根据表格容器实测高度，为表格提供动态像素高度。
 *
 * 使用 ResizeObserver 监听容器尺寸变化（窗口缩放、侧栏折叠、分页显隐等），
 * 降级时在 window resize 时补算一次。
 *
 * Ant Design `a-table` 的 `scroll.y` 只控制表体：传 `subtractHeader: true` 时会扣减表头高度。
 *
 * @param {import('vue').Ref<HTMLElement | null>} wrapperRef 表格外层容器（通常为 `.table-wrapper`）
 * @param {{ minHeight?: number, subtractHeader?: boolean, headerFallback?: number }} [options]
 * @returns {{ tableBodyHeight: import('vue').Ref<number>, tableScrollY: import('vue').Ref<number>, syncHeight: (attemptsLeft?: number) => void }}
 */
export function useTableBodyHeight(wrapperRef, options = {}) {
  const minHeight = options.minHeight ?? 200;
  const subtractHeader = options.subtractHeader === true;
  const headerFallback = options.headerFallback ?? 39;

  const tableBodyHeight = ref(minHeight);
  const tableScrollY = ref(minHeight);
  /** @type {ResizeObserver | null} */
  let resizeObserver = null;

  function resolveHeaderHeight() {
    const headerEl = wrapperRef.value?.querySelector?.(".ant-table-header");
    const h = headerEl?.offsetHeight ?? 0;
    return h > 0 ? h : headerFallback;
  }

  function syncHeight() {
    const measured = wrapperRef.value?.clientHeight ?? 0;
    if (measured <= 0) return;
    tableBodyHeight.value = Math.max(measured, minHeight);
    if (subtractHeader) {
      tableScrollY.value = Math.max(
        tableBodyHeight.value - resolveHeaderHeight(),
        minHeight
      );
    } else {
      tableScrollY.value = tableBodyHeight.value;
    }
  }

  function syncHeightWithRetry(attemptsLeft = 5) {
    syncHeight();
    if ((wrapperRef.value?.clientHeight ?? 0) > 0 || attemptsLeft <= 0) return;
    requestAnimationFrame(() => syncHeightWithRetry(attemptsLeft - 1));
  }

  function setupResizeObserver() {
    if (typeof ResizeObserver === "undefined") return;
    resizeObserver = new ResizeObserver(() => {
      syncHeightWithRetry();
    });
    if (wrapperRef.value) {
      resizeObserver.observe(wrapperRef.value);
    }
  }

  function teardownResizeObserver() {
    if (!resizeObserver) return;
    resizeObserver.disconnect();
    resizeObserver = null;
  }

  function handleWindowResize() {
    syncHeightWithRetry();
  }

  onMounted(() => {
    nextTick(() => {
      syncHeightWithRetry();
      setupResizeObserver();
    });
    if (typeof ResizeObserver === "undefined") {
      window.addEventListener("resize", handleWindowResize);
    }
  });

  onBeforeUnmount(() => {
    teardownResizeObserver();
    window.removeEventListener("resize", handleWindowResize);
  });

  return { tableBodyHeight, tableScrollY, syncHeight: syncHeightWithRetry };
}
