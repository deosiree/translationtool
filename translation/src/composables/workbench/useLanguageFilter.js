import { message } from "ant-design-vue";
import { filterSourceLanguage } from "@/http/api/workbench";

/**
 * 工作台语种过滤（Options API vm 调用）
 * @param {Object} vm 含 filterLanguage / allData / dataSource / filterSource / loading
 */
export function filterLanguageChange(vm) {
  if (vm.filterLanguage === "全部") {
    vm.dataSource = vm.allData;
    vm.filterSource = vm.allData;
  } else {
    const params = {
      languageType: vm.filterLanguage,
    };
    vm.loading = true;
    filterSourceLanguage(params, vm.allData)
      .then((res) => {
        vm.dataSource = res.data.list;
        vm.filterSource = res.data.list;
        vm.loading = false;
      })
      .catch((err) => {
        vm.loading = false;
        message.error("12", err.message);
      });
  }
}
