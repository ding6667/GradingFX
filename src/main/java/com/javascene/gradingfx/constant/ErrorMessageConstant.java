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

    // ==================== 批阅相关 ====================
    /** 批阅任务正在运行中 */
    public static final String REVIEW_RUNNING = "批阅任务正在运行中，请先停止当前任务";
    /** 没有可重试的任务 */
    public static final String NO_TASK_TO_RETRY = "没有可重试的任务";
    /** 批阅任务正在运行中，无法重试 */
    public static final String REVIEW_RUNNING_CANNOT_RETRY = "批阅任务正在运行中，无法重试";
    /** 批阅异常前缀 */
    public static final String REVIEW_ERROR_PREFIX = "批阅异常: ";

    // ==================== 导出相关 ====================
    /** 没有可导出的任务 */
    public static final String NO_TASK_TO_EXPORT = "没有可导出的任务";
    /** 导出失败，未找到任务数据 */
    public static final String EXPORT_DATA_NOT_FOUND = "导出失败，未找到任务数据";

    // ==================== 导出配置相关 ====================
    /** 加载导出配置失败 */
    public static final String EXPORT_CONFIG_LOAD_FAILED = "加载导出配置失败，请重试";
    /** 未配置导出路径 */
    public static final String EXPORT_CONFIG_PATH_MISSING = "未配置导出路径，请先设置";
    /** 保存导出配置失败 */
    public static final String EXPORT_CONFIG_SAVE_FAILED = "保存导出配置失败，请重试";

    // ==================== 视图相关 ====================
    /** 加载视图失败 */
    public static final String VIEW_LOAD_FAILED = "加载视图失败，请重试";
}
