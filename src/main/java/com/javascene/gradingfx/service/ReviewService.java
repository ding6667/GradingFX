package com.javascene.gradingfx.service;

import com.javascene.gradingfx.model.GradingTask;
import com.javascene.gradingfx.model.StudentHomework;
import com.javascene.gradingfx.model.StudentResult;
import com.javascene.gradingfx.model.StudentResultProperty;

import java.util.List;
import java.util.Map;

public interface ReviewService {
    Map<String, List<String>> wordConvertToMd(List<String> fileUrls);
    Map<String, String> mdConvertToWord(String mdContent);
    List<GradingTask> loadAllTasks();
    List<StudentResultProperty> loadTask(String taskId);

    String runWorkflowWithCommonFiles(List<String> fileUrls, String rubric);
    String runWorkflowWithProjectZip(String zipFilePath, String rubric);

    // ==================== 多线程批阅引擎 ====================

    /**
     * 从 zip 包提取学生作业并启动批阅
     * @param zipFilePath zip 文件路径
     * @param rubric 评分标准
     * @param callback 进度回调
     * @return 生成的 taskId
     */
    String startBatchReviewFromZip(String zipFilePath, String rubric, ReviewProgressCallback callback);

    /**
     * 批量批阅主流程：将学生列表拆分为每 5 个一批，并行调用 Dify
     * @param allStudents 所有待批阅学生列表
     * @param taskId 本次任务唯一 ID
     * @param rubric 评分标准
     * @param callback 进度回调
     */
    void startBatchReview(List<StudentResult> allStudents, String taskId, String rubric, ReviewProgressCallback callback);

    /**
     * 暂停批阅：当前批次完成后不再提交新批次
     */
    void pauseReview();

    /**
     * 继续批阅：恢复提交下一批
     */
    void resumeReview();

    /**
     * 停止批阅：中断调度线程，持久化已批阅结果
     */
    void stopReview();

    /**
     * 重试失败项：加载 JSON，筛选 PENDING/FAILED 学生重新批阅
     * @param taskId 任务 ID
     * @param rubric 评分标准
     * @param callback 进度回调
     */
    void retryFailed(String taskId, String rubric, ReviewProgressCallback callback);

    /**
     * 批阅是否正在运行
     */
    boolean isReviewRunning();

    /**
     * 按需导出 Excel：从轻量 JSON 读取最新成绩，生成 Excel 文件
     * 如果评分未变更（scoreChanged=0）且 Excel 已存在，直接返回路径；
     * 如果评分已变更（scoreChanged=1），重新生成 Excel 并重置标记
     * @param taskId 任务 ID
     * @return 生成的 Excel 文件路径，失败返回 null
     */
    String exportExcel(String taskId);

    /**
     * 标记评分已变更（教师在 UI 上修改了评分后调用）
     * @param taskId 任务 ID
     */
    void markScoreChanged(String taskId);

    /**
     * 导出 Word：返回已生成的 Word 文件路径，若不存在则尝试重新生成
     * @param taskId 任务 ID
     * @return Word 文件路径，失败返回 null
     */
    String exportWord(String taskId);

    /**
     * 从 zip 提取学生作业列表
     */
    List<StudentHomework> extractFromTotalZip(String totalZipPath) throws Exception;

    /**
     * 批阅进度回调接口
     */
    interface ReviewProgressCallback {
        /**
         * 一批批阅完成时回调
         * @param completedCount 已完成学生数
         * @param failedCount 失败学生数
         * @param totalCount 总学生数
         * @param batchResults 本批次的结果列表
         */
        void onBatchCompleted(int completedCount, int failedCount, int totalCount, List<StudentResult> batchResults);

        /**
         * 全部批阅完成时回调
         * @param allResults 全部结果列表
         */
        void onReviewFinished(List<StudentResult> allResults);

        /**
         * 批阅发生错误时回调
         * @param error 错误信息
         */
        void onReviewError(String error);
    }
}
