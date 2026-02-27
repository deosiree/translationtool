# TanStack 无头生态渐进迁移方案

项目当前使用 **Ant Design Vue 的 a-table**（`columns` + `dataSource` + 自定义 `#bodyCell`）。迁移到 **TanStack** 后，由 TanStack 负责**状态与表格逻辑**（列定义、排序、筛选、分页、行选择等），UI 仍可用现有组件或自定义 DOM，便于后续换肤、复用逻辑。

---

## 1. 可选用的 TanStack 包（Vue 3）

| 包 | 作用 | 与当前改动的对应关系 |
|----|------|----------------------|
| **@tanstack/vue-table** | 无头表格：列/行模型、排序、筛选、分页、行选择等 | 替代“手写 columns + dataSource + handleTableChange” |
| **@tanstack/vue-query** | 无头请求：缓存、重试、依赖请求、乐观更新 | 可选，替代部分“手动调 API + 设 dataSource” |

先接 **@tanstack/vue-table** 即可体现无头特性；需要统一管理服务端/列表状态时再引入 **@tanstack/vue-query**。

---

## 2. 安装

```bash
npm i @tanstack/vue-table
# 可选：列表数据拉取与缓存
# npm i @tanstack/vue-query
```

---

## 3. 无头用法要点

- **TanStack Table** 只提供：
  - 列定义（含 accessor、header、cell）
  - 行/列模型、排序/筛选/分页状态
  - 暴露 `getRowModel()`、`getHeaderGroups()` 等，**不渲染任何 DOM**。
- **你**负责用这些 API 去渲染：
  - 继续用 **a-table**：把 `table.getRowModel().rows` 转成 `dataSource`，`table.getHeaderGroups()` 转成 `columns`，分页用 `table.getState().pagination` 等；
  - 或改用原生 `<table>` + 自己写的 `<thead>/<tbody>`，样式完全自控。

这样“逻辑在 TanStack，UI 在 Ant Design 或自定义”就是无头用法。

---

## 4. 渐进迁移步骤

### 阶段一：新表或简单表先接 TanStack（推荐从 filterExcel 或新页开始）

1. 在**一个**页面里安装并引入 `@tanstack/vue-table`。
2. 用 `createColumnHelper()` 或 `columnHelper.accessor()` 定义列（等价于当前 `columns` 数组）。
3. 用 `useVueTable()` 传入 `data`（当前 `dataSource`）、`columns`、`getCoreRowModel()`，以及需要的 `getSortedRowModel()`、`getFilteredRowModel()`、`onPaginationChange` 等。
4. 模板里：
   - **方案 A（保留 a-table）**：用 `table.getRowModel().rows` 得到行数据，映射成 `dataSource`；用 `table.getHeaderGroups()` 或现有 columns 生成表头；分页用 `table.getState().pagination` 和 `table.setPageIndex` 等，继续传给 a-table。
   - **方案 B（完全无头）**：用 `table.getHeaderGroups()` / `table.getRowModel().rows` 自己渲染 `<table>`，不用 a-table。

先保证“一页一个表”行为与现在一致，再考虑排序/筛选/分页的迁移。

### 阶段二：词条列表（productEntry）接入

- productEntry 的表格有 **自定义 #bodyCell、可编辑单元格、多列**，建议：
  1. 列定义迁到 TanStack 的 column helper，cell 渲染仍可在列定义里返回 VNode 或组件（或继续用 slot）。
  2. `dataSource` 改为从 `table.getRowModel().rows` 推导，或直接让 TanStack 的 `data` 就是当前 `dataSource`，由 TanStack 管理分页/排序状态。
  3. `handleTableChange` 里对分页、排序、筛选的处理，逐步改为用 `table.getState()` / `table.setPagination()` / `table.setSorting()` 等，再同步到后端请求参数（若有）。

这样既用上 TanStack 的无头能力，又不必一次性重写所有 UI。

### 阶段三（可选）：列表数据用 TanStack Query

- 把“请求词条列表 / 文件列表”改成 `useQuery`，用返回的 `data` 作为 `useVueTable` 的 `data`。
- 分页/筛选参数放在 `queryKey` 里，服务端分页时由 Query 做缓存与重试，表格只消费 `data` 和状态。

---

## 5. 最小示例（保留 a-table 时）

```vue
<script setup>
import { useVueTable, getCoreRowModel, createColumnHelper } from '@tanstack/vue-table';

const dataSource = ref([/* 当前数据 */]);
const columnHelper = createColumnHelper();
const columns = [
  columnHelper.accessor('id', { header: 'ID' }),
  columnHelper.accessor('name', { header: '名称' }),
];

const table = useVueTable({
  get data() { return dataSource.value; },
  columns,
  getCoreRowModel: getCoreRowModel(),
  // getSortedRowModel: getSortedRowModel(), 等按需加
});

// 兼容 a-table：从 table 推导 rows
const tableRows = computed(() => table.getRowModel().rows.map(r => r.original));
</script>

<template>
  <a-table :data-source="tableRows" :columns="columns" />
</template>
```

之后再把排序、分页状态从 a-table 迁到 `table.getState()` / `table.set*`，逐步减少对 a-table 的依赖，或改为自绘 `<table>`。

---

## 6. 与本次 Git 修改的关系

- **excelUtils / dataUtils / 指令 / productEntry 的编辑与导入**等改动，与“是否用 TanStack”无关，可以全部保留。
- 迁移时只替换“表格状态与列定义”的来源（从手写 `columns`/`dataSource`/`handleTableChange` 改为 TanStack Table，可选 + Query），UI 层可以继续用现有 a-table 或逐步改成自绘，实现**逐步**切换到 TanStack 无头生态。

若你指定先迁哪一个页面（例如 `filterExcel.vue` 或 `productEntry.vue`），我可以按该文件当前结构写一版具体的 `useVueTable` + 列定义 + 与 a-table 的对接代码片段。
