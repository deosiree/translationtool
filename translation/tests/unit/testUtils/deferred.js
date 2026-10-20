/**
 * 创建一个由测试显式控制完成时机的 Promise。
 * @returns {{ promise: Promise<any>, resolve: (value?: any) => void, reject: (reason?: any) => void }} 可手动完成的 Promise。
 */
export function createDeferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}
