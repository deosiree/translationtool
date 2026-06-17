# 词条文件导入/解析/更新接口文档

\#\# 文档说明

- \- 所有接口均为 POST 请求，支持跨域

- \- 文件编码统一规则：默认编码为 GBK，前端可传值：UTF\-8、GBK

- \- 接口参数中数组类型需以数组格式传递（示例：attributes\[\]、columnName\[\]）

\#\# 接口列表

\#\#\# 1\. 词条 Excel 导入 \(更新翻译\)

\*\*接口路径\*\*：/entryInfo/entryImportExcle

\*\*请求方式\*\*：POST

\*\*接口描述\*\*：词条 Excel 导入，执行翻译更新

\#\#\#\# 请求参数

|参数名|类型|是否必传|说明|
|---|---|---|---|
|file|MultipartFile|是|导入的 Excel 文件|
|transType|String|是|翻译类型|
|relationFile|MultipartFile|否|关联文件|
|encoding|String|否|文件编码，默认 GBK，可选 UTF\-8/GBK|

\*\*返回值\*\*：HttpResponse\&lt;UpdateEntryInfoByFileVO\&gt;

\-\-\-

\#\#\# 2\. 异步词条 Excel 导入 \(更新翻译\)

\*\*接口路径\*\*：/entryInfo/asyncEntryImportExcle

\*\*请求方式\*\*：POST

\*\*接口描述\*\*：异步执行词条 Excel 导入与翻译更新，不阻塞前端

\#\#\#\# 请求参数

|参数名|类型|是否必传|说明|
|---|---|---|---|
|file|MultipartFile|是|导入的 Excel 文件|
|transType|String|是|翻译类型|
|relationFile|MultipartFile|否|关联文件|
|encoding|String|否|文件编码，默认 GBK，可选 UTF\-8/GBK|

\*\*返回值\*\*：HttpResponse\&lt;String\&gt;

\-\-\-

\#\#\# 3\. 文件词条解析并分组

\*\*接口路径\*\*：/entryInfo/makeGroupForEntryInfosOnFiles

\*\*请求方式\*\*：POST

\*\*接口描述\*\*：解析文件中的词条，并按指定属性分组

\#\#\#\# 请求参数

|参数名|类型|是否必传|说明|
|---|---|---|---|
|file|MultipartFile|是|待解析文件|
|attributes\[\]|Collection\&lt;String\&gt;|否|分组属性数组|
|encoding|String|否|文件编码，默认 GBK，可选 UTF\-8/GBK|

\*\*返回值\*\*：HttpResponse\&lt;EntryInfoGroupVO\&gt;

\-\-\-

\#\#\# 4\. 解析文件转为词条对象

\*\*接口路径\*\*：/entryInfo/parseFileToEntryInfos

\*\*请求方式\*\*：POST

\*\*接口描述\*\*：将文件内容解析为词条实体对象集合

\#\#\#\# 请求参数

|参数名|类型|是否必传|说明|
|---|---|---|---|
|file|MultipartFile|是|待解析文件|
|encoding|String|否|文件编码，默认 GBK，可选 UTF\-8/GBK|

\*\*返回值\*\*：HttpResponse\&lt;Collection\&lt;EntryInfoEntity\&gt;\&gt;

\-\-\-

\#\#\# 5\. 根据文件更新词条信息

\*\*接口路径\*\*：/entryInfo/updateEntryInfosByFile

\*\*请求方式\*\*：POST

\*\*接口描述\*\*：根据文件词条，指定列更新至数据库

\#\#\#\# 请求参数

|参数名|类型|是否必传|说明|
|---|---|---|---|
|file|MultipartFile|是|词条文件|
|columnName\[\]|Collection\&lt;String\&gt;|是|需要更新的列名数组|
|encoding|String|否|文件编码，默认 GBK，可选 UTF\-8/GBK|

\*\*返回值\*\*：HttpResponse\&lt;UpdateEntryInfoByFileVO\&gt;

\-\-\-

\#\#\# 6\. 更新翻译前校验

\*\*接口路径\*\*：/entryInfo/checkBeforeUpdateTranslationByFile

\*\*请求方式\*\*：POST

\*\*接口描述\*\*：批量更新翻译前，进行文件与数据校验

\#\#\#\# 请求参数

|参数名|类型|是否必传|说明|
|---|---|---|---|
|dedupOriginExcel|MultipartFile|是|未翻译源文件|
|dedupUpdateExcel|MultipartFile|是|已翻译文件|
|mappingJson|MultipartFile|否|ID 映射关系文件|
|payload|String|是|任务信息 JSON 字符串|
|encodingForUnTrans|String|否|未翻译文件编码，默认 GBK|
|encodingForTrans|String|否|翻译文件编码，默认 GBK|

\*\*返回值\*\*：HttpResponse\&lt;TaskCheckResultVO\&gt;

\-\-\-

\#\#\# 7\. 词条 Excel 导入（通用和装置 \- 新版）

\*\*接口路径\*\*：/workbench/entryImportExcle

\*\*请求方式\*\*：POST

\*\*接口描述\*\*：新版词条导入，支持部门 / 任务 ID，带事务保证

\#\#\#\# 请求参数

|参数名|类型|是否必传|说明|
|---|---|---|---|
|file|MultipartFile|是|导入 Excel 文件|
|departmentType|String|否|部门类型，默认：通用平台部|
|taskID|String|是|任务 ID|
|encoding|String|否|文件编码，默认 GBK，可选 UTF\-8/GBK|

\*\*返回值\*\*：HttpResponse\&lt;ResponseListModel\&gt;

> （注：文档部分内容可能由 AI 生成）
