package com.tiamo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiamo.entity.Books;
import com.tiamo.entity.RecycleApproval;
import com.tiamo.mapper.BooksMapper;
import com.tiamo.mapper.RecycleApprovalMapper;
import com.tiamo.service.BooksService;
import com.tiamo.service.CacheService;
import com.tiamo.service.RecycleApprovalService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回收站审批 Service 实现
 */
@Service
public class RecycleApprovalServiceImpl extends ServiceImpl<RecycleApprovalMapper, RecycleApproval> implements RecycleApprovalService {

    @Autowired
    private BooksService booksService;
    @Autowired
    private BooksMapper booksMapper;
    @Autowired
    private CacheService cacheService;

    // 待审批列表缓存key
    private static final String PENDING_CACHE_KEY = "tiamo:approval:pending:list";

    @PostConstruct
    public void init() {
        try {
            baseMapper.createTableIfNotExists();
            System.out.println("[审批] recycle_approval 表已就绪");
        } catch (Exception e) {
            System.out.println("[审批] 建表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public RecycleApproval submitApproval(Integer bookId, String approvalType, Long applicantId, String applicantName) {
        // 检查商品是否在回收站（用原生SQL绕过逻辑删除过滤）
        Books book = booksMapper.selectDeletedById(bookId);
        if (book == null) {
            throw new RuntimeException("商品不存在或不在回收站中");
        }
        // 检查是否已有待审批的申请
        long pendingCount = this.count(new LambdaQueryWrapper<RecycleApproval>()
                .eq(RecycleApproval::getBookId, bookId)
                .eq(RecycleApproval::getStatus, "PENDING"));
        if (pendingCount > 0) {
            throw new RuntimeException("该商品已有待审批的申请，请等待管理员处理");
        }

        RecycleApproval approval = new RecycleApproval();
        approval.setBookId(bookId);
        approval.setBookName(book.getName());
        approval.setApprovalType(approvalType);
        approval.setApplicantId(applicantId);
        approval.setApplicantName(applicantName);
        approval.setStatus("PENDING");
        approval.setApplyTime(LocalDateTime.now());
        this.save(approval);
        // 清除待审批列表缓存
        cacheService.delete(PENDING_CACHE_KEY);
        return approval;
    }

    @Override
    @Transactional
    public boolean approve(Long id, Long approverId, String approverName, String remark) {
        RecycleApproval approval = this.getById(id);
        if (approval == null) {
            throw new RuntimeException("审批记录不存在");
        }
        if (!"PENDING".equals(approval.getStatus())) {
            throw new RuntimeException("该申请已处理，不能重复审批");
        }

        // 执行实际操作
        if ("RESTORE".equals(approval.getApprovalType())) {
            booksService.restore(approval.getBookId());
        } else if ("DELETE".equals(approval.getApprovalType())) {
            booksService.hardDelete(approval.getBookId());
        }

        // 更新审批状态
        approval.setStatus("APPROVED");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setRemark(remark);
        approval.setApproveTime(LocalDateTime.now());
        boolean result = this.updateById(approval);
        // 清除待审批列表缓存
        cacheService.delete(PENDING_CACHE_KEY);
        return result;
    }

    @Override
    @Transactional
    public boolean reject(Long id, Long approverId, String approverName, String remark) {
        RecycleApproval approval = this.getById(id);
        if (approval == null) {
            throw new RuntimeException("审批记录不存在");
        }
        if (!"PENDING".equals(approval.getStatus())) {
            throw new RuntimeException("该申请已处理，不能重复审批");
        }

        approval.setStatus("REJECTED");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setRemark(remark);
        approval.setApproveTime(LocalDateTime.now());
        boolean result = this.updateById(approval);
        // 清除待审批列表缓存
        cacheService.delete(PENDING_CACHE_KEY);
        return result;
    }

    @Override
    public List<RecycleApproval> getPendingList() {
        // 先从缓存获取
        List<RecycleApproval> cachedList = cacheService.getList(PENDING_CACHE_KEY, RecycleApproval.class);
        if (cachedList != null) {
            return cachedList;
        }
        // 缓存未命中，查询数据库
        List<RecycleApproval> list = this.list(new LambdaQueryWrapper<RecycleApproval>()
                .eq(RecycleApproval::getStatus, "PENDING")
                .orderByDesc(RecycleApproval::getApplyTime));
        // 写入缓存，过期时间1分钟
        cacheService.set(PENDING_CACHE_KEY, list, 1, java.util.concurrent.TimeUnit.MINUTES);
        return list;
    }

    @Override
    public List<RecycleApproval> getMyApplications(Long applicantId) {
        return this.list(new LambdaQueryWrapper<RecycleApproval>()
                .eq(RecycleApproval::getApplicantId, applicantId)
                .orderByDesc(RecycleApproval::getApplyTime));
    }

    @Override
    public long countPending() {
        return this.count(new LambdaQueryWrapper<RecycleApproval>()
                .eq(RecycleApproval::getStatus, "PENDING"));
    }

    @Override
    public boolean deleteById(Long id, Long userId, Integer userRole) {
        RecycleApproval approval = this.getById(id);
        if (approval == null) {
            throw new RuntimeException("申请记录不存在");
        }
        // 仅申请人本人或管理员可删除
        boolean isOwner = approval.getApplicantId() != null && approval.getApplicantId().equals(userId);
        boolean isAdmin = userRole != null && userRole == 1;
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("无权限删除，仅申请人本人或管理员可操作");
        }
        return this.removeById(id);
    }
}
