import { message } from "ant-design-vue";
import { filterSourceLanguage } from "@/http/api/workbench";
import { startLoading, endLoading } from "@/composables/useLoading";

/**
 * 工作台语种过滤（Options API vm 调用）
 * @param {Object} vm 含 filterLanguage / allData / dataSource / filterSource
 */
export function filterLanguageChange(vm) {
  if (vm.filterLanguage === "全部") {
    vm.dataSource = vm.allData;
    vm.filterSource = vm.allData;
  } else {
    const params = {
      languageType: vm.filterLanguage,
    };
    startLoading();
    filterSourceLanguage(params, vm.allData)
      .then((res) => {
        vm.dataSource = res.data.list;
        vm.filterSource = res.data.list;
      })
      .catch((err) => {
        message.error("12", err.message);
      })
      .finally(() => {
        endLoading();
      });
  }
}
