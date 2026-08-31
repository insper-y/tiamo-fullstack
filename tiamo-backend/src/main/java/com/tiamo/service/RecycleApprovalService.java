package com.tiamo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiamo.entity.RecycleApproval;

import java.util.List;

/**
 * 回收站审批 Service 接口
 */
public interface RecycleApprovalService extends IService<RecycleApproval> {

    /**
     * 提交审批申请
     */
    RecycleApproval submitApproval(Integer bookId, String approvalType, Long applicantId, String applicantName);

    /**
     * 审批通过
     */
    boolean approve(Long id, Long approverId, String approverName, String remark);

    /**
     * 审批拒绝
     */
    boolean reject(Long id, Long approverId, String approverName, String remark);

    /**
     * 查询待审批列表
     */
    List<RecycleApproval> getPendingList();

    /**
     * 查询我的申请列表
     */
    List<RecycleApproval> getMyApplications(Long applicantId);

    /**
     * 获取待审批数量
     */
    long countPending();

    /**
     * 删除申请记录（仅申请人本人或管理员）
     */
    boolean deleteById(Long id, Long userId, Integer userRole);
}
