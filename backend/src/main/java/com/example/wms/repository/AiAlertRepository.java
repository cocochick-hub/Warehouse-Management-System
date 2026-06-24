package com.example.wms.repository;

import com.example.wms.entity.AiAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiAlertRepository extends JpaRepository<AiAlert, Long> {

    /** 查询最新一批预警（按类型筛选，可选） */
    List<AiAlert> findByAlertTypeOrderByCreatedAtDesc(String alertType);

    /** 查询所有最新预警 */
    List<AiAlert> findAllByOrderByCreatedAtDesc();

    /** 清除旧预警记录 */
    @Modifying
    @Query("DELETE FROM AiAlert a WHERE a.createdAt < (SELECT MIN(a2.createdAt) FROM AiAlert a2 WHERE a2.alertType = a.alertType)")
    void deleteOldAlerts();

    /** 删除所有预警记录 */
    @Modifying
    @Query("DELETE FROM AiAlert")
    void deleteAllAlerts();
}
