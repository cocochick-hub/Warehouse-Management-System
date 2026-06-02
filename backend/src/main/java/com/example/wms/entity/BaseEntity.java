package com.example.wms.entity;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

/**
 * 实体基类 - 包含统一的审计字段
 * 所有数据表均需包含这些字段（对应 README 要求）
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    /** 创建人 */
    @Column(name = "created_by", length = 50)
    private String createdBy;

    /** 更新人 */
    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
