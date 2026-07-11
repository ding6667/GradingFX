package com.javascene.gradingfx.constant;

public class ErrorMessageConstant {
    public static final String FILES_NOT_FOUND = "文件路径列表为空";
    public static final String MD_CONTENT_EMPTY = "服务端文档解析错误，Markdown内容为空";
    public static final String MD_CONTENT_INVALID = "服务端文档解析错误，Markdown内容无效";
    //文件为空
    public static final String FILE_IS_EMPTY = "文件为空或未成功上传有效文件";
    //未知错误
    public static final String UNKNOWN_ERROR = "未知错误";
    //删除失败
    public static final String DELETE_FAILED = "删除文件失败";
    //未选中文件
    public static final String FILE_NOT_SELECTED = "未选中文件";

    // 文件上传校验
    public static final String FILE_SIZE_OVER_LIMIT = "文件大小超出限制";
    public static final String FILE_TYPE_NOT_SUPPORT = "不支持该文件格式，仅允许md文件";
    public static final String FILE_UPLOAD_IO_ERROR = "文件上传读写失败，请检查文件";
    public static final String FILE_NAME_DUPLICATE = "存在同名文件，请修改文件名后重新上传";

    // 文件本地读写
    public static final String FILE_READ_FAILED = "本地文件读取失败，文件可能损坏或被占用";
    public static final String FILE_NOT_EXIST = "目标文件不存在";
    public static final String FILE_PERMISSION_DENY = "无文件读写权限，请检查文件夹权限";

    // 打分核心业务异常
    public static final String GRADE_PARSE_ERROR = "打分规则解析失败，文档格式不符合评分标准";
    public static final String GRADE_CALC_ERROR = "分数计算出错，请检查文档内容";
    public static final String GRADE_ITEM_MISSING = "文档缺少必填评分项，无法打分";

    // 批量操作
    public static final String BATCH_OP_EMPTY = "未选择任何文件，无法执行批量操作";
    public static final String BATCH_PART_FAILED = "批量操作部分文件执行失败";

    // 文件夹操作
    public static final String DIR_CREATE_FAILED = "文件夹创建失败";
    public static final String DIR_NOT_EXIST = "目标文件夹不存在";

    // 接口/网络交互
    public static final String REQUEST_FAIL = "服务接口请求失败，请检查网络";
    public static final String SERVER_RESPONSE_ERROR = "服务端返回异常数据";
    public static final String AUTH_ERROR = "身份验证失败，请重新登录";

    // 导出报告
    public static final String EXPORT_FAILED = "评分报告导出失败";
    public static final String EXPORT_DATA_EMPTY = "暂无评分数据，无法导出报告";

    // 通用参数校验
    public static final String PARAM_EMPTY = "请求参数不能为空";
    public static final String PARAM_INVALID = "传入参数格式非法";
    public static final String LOAD_TEMPLATE_FAIL ="加载模块失败" ;

    public static final String CURRENT_SAVE_SUCCESS ="当前评分标准保存成功" ;
    public static final String CURRENT_SAVE_FAIL ="加载默认标准模板失败" ;

    public static final String TEMPLATE_NAME_EMPTY = "模板名称不能为空";
    public static final String SAVE_TEMPLATE_SUCCESS = "自定义模板保存成功";

    public static final String SAVE_TEMPLATE_FAIL ="自定义模板保存失败" ;
    public static final String NO_SELECT_TEMPLATE ="请选择要加载的模板" ;
    public static final String TEMPLATE_FILE_MISS = "模板文件不存在，加载失败";
    public static final String DELETE_NO_SELECT="请选择要删除的模板";
    public static final String DELETE_SUCCESS ="模板删除成功" ;
    public static final String DELETE_FAIL = "模板删除失败";
    // 加载其他模板相关提示
    public static final String NO_SAVE_TEMPLATE = "暂无保存的自定义模板，请先保存模板";
    public static final String SELECT_TEMPLATE = "请选择要加载的模板";
    public static final String LOAD_OTHER_TEMPLATE_SUCCESS = "模板加载完成";

    public static final String EXPORT_CONFIG_LOAD_FAILED = "导出配置加载失败";
    public static final String EXPORT_CONFIG_PATH_MISSING = "导出配置路径不能为空";
    public static final String EXPORT_CONFIG_SAVE_FAILED = "导出配置保存失败";
    public static final String EXPORT_DATA_NOT_FOUND = "导出数据不存在";
    public static final String REVIEW_RUNNING = "当前有评分任务正在运行，无法导出报告";
    public static final String NO_TASK_TO_RETRY = "没有可重试的评分任务";
    public static final String REVIEW_RUNNING_CANNOT_RETRY = "评分任务正在运行中，无法重试";
    public static final String REVIEW_ERROR_PREFIX = "评分出错：";
    public static final String NO_TASK_TO_EXPORT = "没有可导出的评分任务";
    public static final String VIEW_LOAD_FAILED = "视图加载失败";
   }
