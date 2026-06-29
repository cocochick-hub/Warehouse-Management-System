package com.example.wms.service;

import com.example.wms.dto.check.*;

import java.util.List;

public interface CheckService {

    /** PC端：创建盘点任务（根据筛选条件从库存表初始化明细） */
    CheckTaskDTO createTask(CreateTaskRequest request, String username);

    /** PC端：查询盘点任务列表 */
    List<CheckTaskDTO> listTasks();

    /** PC端：查询任务详情（含明细列表） */
    List<CheckDetailDTO> getTaskDetails(Long taskId);

    /** PC端：完成任务（锁定差异） */
    void completeTask(Long taskId);

    /** PC端：差异调整（更新库存+记录历史） */
    void adjustDetail(Long detailId, Integer adjustQty, String username);

    /** 移动端：获取进行中的盘点任务列表 */
    List<CheckProgressDTO> listActiveTasks();

    /** 移动端：获取盘点明细（扫码前查看，明盘时返回system_qty） */
    CheckDetailDTO getDetail(Long detailId);

    /** 移动端：扫码盘点（填入实际数量） */
    CheckProgressDTO scanCheck(ScanRequest request);
}