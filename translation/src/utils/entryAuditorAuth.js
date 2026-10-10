/**
 * 词条审核员删除门禁：须同时具备角色且为本任务词条审核指派人。
 */
export function canDeleteAsEntryAuditor(user, task) {
  if (!user?.roleName || !task) return false;
  return (
    user.roleName.indexOf("词条审核员") !== -1 &&
    user.userName === task.entryAuditor
  );
}
