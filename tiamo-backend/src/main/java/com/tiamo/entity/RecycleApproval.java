package com.tiamo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 回收站审批实体
 * 普通用户提交恢复/彻底删除申请，管理员审批
 */
@Data
@TableName("recycle_approval")
public class RecycleApproval implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品ID */
    @TableField("book_id")
    private Integer bookId;

    /** 商品名称（冗余，方便显示） */
    @TableField("book_name")
    private String bookName;

    /** 申请类型: RESTORE-恢复 DELETE-彻底删除 */
    @TableField("approval_type")
    private String approvalType;

    /** 申请人ID */
    @TableField("applicant_id")
    private Long applicantId;

    /** 申请人用户名 */
    @TableField("applicant_name")
    private String applicantName;

    /** 申请状态: PENDING-待审批 APPROVED-已通过 REJECTED-已拒绝 */
    @TableField("status")
    private String status;

    /** 审批人ID */
    @TableField("approver_id")
    private Long approverId;

    /** 审批人用户名 */
    @TableField("approver_name")
    private String approverName;

    /** 审批备注 */
    @TableField("remark")
    private String remark;

    /** 申请时间 */
    @TableField("apply_time")
    private LocalDateTime applyTime;

    /** 审批时间 */
    @TableField("approve_time")
    private LocalDateTime approveTime;
}
