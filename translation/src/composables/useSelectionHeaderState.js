import { computed, ref } from "vue";

const HEADER_STATES = {
  UNCHECKED: "unchecked",
  INDETERMINATE: "indeterminate",
  CHECKED: "checked",
  CHECKING: "checking",
  QUERYING: "querying",
};

/**
 * 为查询范围和选中 ID 集合生成稳定缓存键。
 * @param {string} scope - 已提交查询范围。
 * @param {Set<string|number>} selectedIds - 全局已选 ID 集合。
 * @returns {string} 可比较的状态缓存键。
 */
function createCacheKey(scope, selectedIds) {
  const ids = [...selectedIds]
    .map((id) => JSON.stringify([typeof id, id]))
    .sort();
  return JSON.stringify([scope, ids]);
}

/**
 * 判断查询返回的 ID 是否完整且没有重复。
 * @param {Array<string|number>} ids - 查询范围返回的 ID。
 * @param {number} total - 查询范围总数。
 * @returns {boolean} ID 集合是否完整有效。
 */
function hasCompleteIds(ids, total) {
  if (!Array.isArray(ids) || !Number.isInteger(total) || total < 0) return false;
  if (ids.length !== total || ids.some((id) => id == null)) return false;
  return new Set(ids).size === total;
}

/**
 * 根据完整查询 ID 与全局已选 ID 计算表头状态。
 * @param {Set<string|number>} queryIds - 当前查询范围 ID。
 * @param {Set<string|number>} selectedIds - 全局已选 ID。
 * @returns {string} checked、indeterminate 或 unchecked。
 */
function resolveState(queryIds, selectedIds) {
  let selectedCount = 0;
  queryIds.forEach((id) => {
    selectedCount += Number(selectedIds.has(id));
  });
  if (selectedCount === 0) return HEADER_STATES.UNCHECKED;
  return selectedCount === queryIds.size
    ? HEADER_STATES.CHECKED
    : HEADER_STATES.INDETERMINATE;
}

/**
 * 创建可由多个表格复用的全选表头状态判定器。
 * @returns {Object} 状态引用及失效、缓存和异步判定方法。
 */
export function useSelectionHeaderState() {
  const state = ref(HEADER_STATES.UNCHECKED);
  const isChecking = computed(() => state.value === HEADER_STATES.CHECKING);
  let cachedStateKey = "";
  let activeScope = "";
  let cachedQueryIds = null;
  let queryIdsUnavailable = false;
  let pendingQuery = null;
  let stateVersion = 0;
  let queryVersion = 0;

  /**
   * 清除当前查询范围的 ID、状态缓存及进行中的核对。
   * @returns {void} 无返回值。
   */
  function clearQueryCache() {
    cachedStateKey = "";
    cachedQueryIds = null;
    queryIdsUnavailable = false;
    pendingQuery = null;
  }

  /**
   * 切换到新查询范围并清除旧范围数据。
   * @param {string} scope - 新的已提交查询范围。
   * @returns {void} 无返回值。
   */
  function activateScope(scope) {
    if (activeScope === scope) return;
    stateVersion += 1;
    queryVersion += 1;
    activeScope = scope;
    clearQueryCache();
    state.value = HEADER_STATES.UNCHECKED;
  }

  /**
   * 使当前状态及 ID 缓存和进行中的核对失效。
   * @returns {void} 无返回值。
   */
  function invalidate() {
    stateVersion += 1;
    queryVersion += 1;
    activeScope = "";
    clearQueryCache();
    state.value = HEADER_STATES.UNCHECKED;
  }

  /**
   * 标记显式提交的新查询，强制之后重新获取当前范围 ID。
   * @param {string} scope - 新查询范围。
   * @returns {void} 无返回值。
   */
  function beginQuery(scope) {
    stateVersion += 1;
    queryVersion += 1;
    activeScope = scope || "";
    clearQueryCache();
    state.value = scope ? HEADER_STATES.QUERYING : HEADER_STATES.UNCHECKED;
  }

  /**
   * 缓存已由完整结果确认的查询 ID 集合。
   * @param {Array<string|number>|Set<string|number>} ids - 当前范围完整 ID。
   * @param {number} total - 当前查询结果总数。
   * @param {string} scope - 已提交查询范围。
   * @returns {boolean} 是否成功缓存完整 ID。
   */
  function setKnownIds(ids, total, scope) {
    if (!scope) return false;
    activateScope(scope);
    stateVersion += 1;
    queryVersion += 1;
    pendingQuery = null;
    cachedStateKey = "";

    const normalizedIds = ids instanceof Set ? [...ids] : ids;
    if (!hasCompleteIds(normalizedIds, Number(total))) {
      cachedQueryIds = null;
      queryIdsUnavailable = true;
      return false;
    }
    cachedQueryIds = new Set(normalizedIds);
    queryIdsUnavailable = false;
    return true;
  }

  /**
   * 缓存由完整查询结果或全量选择操作确定的表头状态。
   * @param {string} nextState - 已确认的表头状态。
   * @param {string} scope - 已提交查询范围。
   * @param {Array<string|number>|Set<string|number>} selectedIds - 全局已选 ID。
   * @returns {void} 无返回值。
   */
  function setKnownState(nextState, scope, selectedIds) {
    if (scope) activateScope(scope);
    stateVersion += 1;
    state.value = nextState;
    cachedStateKey = scope
      ? createCacheKey(scope, new Set(selectedIds || []))
      : "";
  }

  /**
   * 优先用当前页快速判定，必要时只获取一次完整查询 ID 集合。
   * @param {Object} options - 当前范围、选择集、表格数据及全量 ID 查询适配器。
   * @param {string} options.scope - 已提交查询范围。
   * @param {Array<string|number>|Set<string|number>} options.selectedIds - 全局已选 ID。
   * @param {Array<Object>} options.rows - 当前页或本地完整结果行。
   * @param {number} options.total - 当前查询结果总数。
   * @param {boolean} options.isCompleteResult - rows 是否包含完整查询结果。
   * @param {Function} options.fetchAllIds - 返回 { ids, total } 的全量 ID 查询函数。
   * @returns {Promise<string>} 最终或当前可确定的表头状态。
   */
  async function refresh({
    scope,
    selectedIds: rawSelectedIds,
    rows: rawRows,
    total: rawTotal,
    isCompleteResult = false,
    fetchAllIds,
  }) {
    const selectedIds = new Set(rawSelectedIds || []);
    const rows = Array.isArray(rawRows) ? rawRows : [];
    const total = Math.max(Number(rawTotal) || 0, 0);
    if (!scope) {
      invalidate();
      return state.value;
    }
    activateScope(scope);

    const requestStateVersion = ++stateVersion;
    const requestQueryVersion = queryVersion;
    const cacheKey = createCacheKey(scope, selectedIds);
    const isCurrent = () =>
      stateVersion === requestStateVersion &&
      queryVersion === requestQueryVersion &&
      activeScope === scope;
    const commit = (nextState) => {
      if (!isCurrent()) return;
      state.value = nextState;
      cachedStateKey = cacheKey;
    };

    if (
      cachedStateKey === cacheKey &&
      state.value !== HEADER_STATES.CHECKING &&
      state.value !== HEADER_STATES.QUERYING
    ) {
      return state.value;
    }

    const pageSelectedCount = rows.reduce(
      (count, row) => count + Number(selectedIds.has(row?.id)),
      0
    );
    if (pageSelectedCount > 0 && pageSelectedCount < rows.length) {
      commit(HEADER_STATES.INDETERMINATE);
      return state.value;
    }
    if (selectedIds.size === 0 || total === 0) {
      commit(HEADER_STATES.UNCHECKED);
      return state.value;
    }
    if (pageSelectedCount > 0 && selectedIds.size < total) {
      commit(HEADER_STATES.INDETERMINATE);
      return state.value;
    }

    if (isCompleteResult) {
      const ids = rows.map((row) => row?.id);
      if (!hasCompleteIds(ids, total)) {
        cachedQueryIds = null;
        queryIdsUnavailable = true;
        commit(HEADER_STATES.INDETERMINATE);
        return state.value;
      }
      cachedQueryIds = new Set(ids);
      queryIdsUnavailable = false;
      commit(resolveState(cachedQueryIds, selectedIds));
      return state.value;
    }

    if (cachedQueryIds) {
      commit(resolveState(cachedQueryIds, selectedIds));
      return state.value;
    }
    if (queryIdsUnavailable || typeof fetchAllIds !== "function") {
      queryIdsUnavailable = true;
      commit(HEADER_STATES.INDETERMINATE);
      return state.value;
    }

    cachedStateKey = "";
    state.value = HEADER_STATES.CHECKING;
    let pending = pendingQuery;
    if (!pending || pending.scope !== scope || pending.version !== queryVersion) {
      const queryVersionAtStart = queryVersion;
      let fullIdsRequest;
      try {
        fullIdsRequest = fetchAllIds();
      } catch (error) {
        fullIdsRequest = Promise.reject(error);
      }
      pending = {
        scope,
        version: queryVersionAtStart,
        promise: Promise.resolve(fullIdsRequest)
          .then((result) => {
            const ids = result?.ids;
            const responseTotal = Number(result?.total);
            const isValid = responseTotal === total && hasCompleteIds(ids, total);
            if (
              queryVersion === queryVersionAtStart &&
              activeScope === scope
            ) {
              cachedQueryIds = isValid ? new Set(ids) : null;
              queryIdsUnavailable = !isValid;
              if (pendingQuery === pending) pendingQuery = null;
            }
            return isValid;
          })
          .catch(() => {
            if (
              queryVersion === queryVersionAtStart &&
              activeScope === scope
            ) {
              cachedQueryIds = null;
              queryIdsUnavailable = true;
              if (pendingQuery === pending) pendingQuery = null;
            }
            return false;
          }),
      };
      pendingQuery = pending;
    }

    await pending.promise;
    if (!isCurrent()) return state.value;
    if (cachedQueryIds) {
      commit(resolveState(cachedQueryIds, selectedIds));
    } else {
      commit(HEADER_STATES.INDETERMINATE);
    }
    return state.value;
  }

  return {
    state,
    isChecking,
    invalidate,
    beginQuery,
    setKnownIds,
    setKnownState,
    refresh,
  };
}
