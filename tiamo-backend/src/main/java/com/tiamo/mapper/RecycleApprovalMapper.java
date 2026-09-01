package com.tiamo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiamo.entity.RecycleApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 回收站审批 Mapper
 */
@Mapper
public interface RecycleApprovalMapper extends BaseMapper<RecycleApproval> {

    /**
     * 自动建表
     */
    @Update("CREATE TABLE IF NOT EXISTS recycle_approval (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "book_id INT NOT NULL, " +
            "book_name VARCHAR(255), " +
            "approval_type VARCHAR(20) NOT NULL COMMENT 'RESTORE-恢复 DELETE-彻底删除', " +
            "applicant_id BIGINT, " +
            "applicant_name VARCHAR(100), " +
            "status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING-待审批 APPROVED-已通过 REJECTED-已拒绝', " +
            "approver_id BIGINT, " +
            "approver_name VARCHAR(100), " +
            "remark VARCHAR(500), " +
            "apply_time DATETIME, " +
            "approve_time DATETIME, " +
            "is_read TINYINT DEFAULT 0 COMMENT '0-未阅读 1-已阅读', " +
            "read_time DATETIME, " +
            "INDEX idx_status (status), " +
            "INDEX idx_book_id (book_id), " +
            "INDEX idx_applicant (applicant_id), " +
            "INDEX idx_apply_time (apply_time), " +
            "INDEX idx_is_read (is_read)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回收站审批表'")
    void createTableIfNotExists();

    /**
     * 添加is_read和read_time字段（兼容旧表）
     */
    @Update("ALTER TABLE recycle_approval ADD COLUMN IF NOT EXISTS is_read TINYINT DEFAULT 0 COMMENT '0-未阅读 1-已阅读'")
    void addIsReadColumn();

    @Update("ALTER TABLE recycle_approval ADD COLUMN IF NOT EXISTS read_time DATETIME")
    void addReadTimeColumn();
}
