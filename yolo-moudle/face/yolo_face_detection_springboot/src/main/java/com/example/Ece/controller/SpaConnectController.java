package com.example.Ece.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.Ece.common.Result;
import com.example.Ece.entity.AwarenessRecord;
import com.example.Ece.entity.SpaIdentity;
import com.example.Ece.entity.SpaMessage;
import com.example.Ece.entity.SpaRequest;
import com.example.Ece.entity.User;
import com.example.Ece.mapper.AwarenessRecordMapper;
import com.example.Ece.mapper.SpaIdentityMapper;
import com.example.Ece.mapper.SpaMessageMapper;
import com.example.Ece.mapper.SpaRequestMapper;
import com.example.Ece.mapper.UserMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 「心有灵犀」医患联动：实名认证、联动单与留言
 * 文案保持温柔克制，身份证号只保存掩码
 */
@RestController
@RequestMapping("/spa")
public class SpaConnectController {
    private static final String IDENTITY_PATIENT = "patient";
    private static final String IDENTITY_PRACTITIONER = "practitioner";
    private static final String AUDIT_PENDING = "pending";
    private static final String AUDIT_APPROVED = "approved";
    private static final String AUDIT_REJECTED = "rejected";
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");

    @Resource
    SpaIdentityMapper spaIdentityMapper;
    @Resource
    SpaRequestMapper spaRequestMapper;
    @Resource
    SpaMessageMapper spaMessageMapper;
    @Resource
    UserMapper userMapper;
    @Resource
    AwarenessRecordMapper awarenessRecordMapper;

    /**
     * 提交 / 重新提交实名认证，提交后回到待审核状态
     */
    @PostMapping("/identity/apply")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> applyIdentity(@RequestBody JSONObject params) {
        String username = StrUtil.trim(params.getString("username"));
        String identityType = StrUtil.trim(params.getString("identityType"));
        String realName = StrUtil.trim(params.getString("realName"));
        String idCard = StrUtil.trim(params.getString("idCard"));
        if (StrUtil.isBlank(username)) {
            return Result.error("400", "请先登录后再提交认证信息");
        }
        if (!IDENTITY_PATIENT.equals(identityType) && !IDENTITY_PRACTITIONER.equals(identityType)) {
            return Result.error("400", "请选择正确的身份类型");
        }
        if (StrUtil.isBlank(realName)) {
            return Result.error("400", "请填写真实姓名");
        }

        SpaIdentity identity = findIdentity(username);
        boolean isNew = identity == null;
        if (isNew) {
            identity = new SpaIdentity();
            identity.setUsername(username);
            identity.setCreatedAt(LocalDateTime.now());
        }
        identity.setIdentityType(identityType);
        identity.setRealName(limit(realName, 64));

        if (IDENTITY_PRACTITIONER.equals(identityType)) {
            // 心灵SPA师需要补充执业信息，便于平台核验
            String licenseNo = StrUtil.trim(params.getString("licenseNo"));
            String hospital = StrUtil.trim(params.getString("hospital"));
            String department = StrUtil.trim(params.getString("department"));
            if (StrUtil.isBlank(licenseNo) || StrUtil.isBlank(hospital) || StrUtil.isBlank(department)) {
                return Result.error("400", "心灵SPA师认证需要填写资格证号、所在机构与科室");
            }
            identity.setLicenseNo(limit(licenseNo, 64));
            identity.setHospital(limit(hospital, 128));
            identity.setDepartment(limit(department, 128));
            identity.setTitle(limit(StrUtil.trim(params.getString("title")), 64));
            identity.setBio(limit(StrUtil.trim(params.getString("bio")), 1024));
            if (StrUtil.isNotBlank(idCard)) {
                if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
                    return Result.error("400", "身份证号格式不正确，请填写18位身份证号");
                }
                identity.setIdCardMasked(maskIdCard(idCard));
            }
        } else {
            // 记录者只需要实名与身份证号，身份证号仅存掩码
            if (StrUtil.isBlank(idCard) || !ID_CARD_PATTERN.matcher(idCard).matches()) {
                return Result.error("400", "请填写18位身份证号完成实名认证");
            }
            identity.setIdCardMasked(maskIdCard(idCard));
        }

        identity.setAuditStatus(AUDIT_PENDING);
        identity.setAuditNote(null);
        identity.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            spaIdentityMapper.insert(identity);
        } else {
            spaIdentityMapper.updateById(identity);
        }
        return Result.success(toIdentityResponse(identity));
    }

    /**
     * 查看自己的认证档案
     */
    @GetMapping("/identity/mine")
    public Result<?> myIdentity(@RequestParam String username) {
        SpaIdentity identity = findIdentity(StrUtil.trim(username));
        if (identity == null) {
            return Result.error("404", "还没有提交过认证信息");
        }
        return Result.success(toIdentityResponse(identity));
    }

    /**
     * 待审核列表，仅管理员可见
     */
    @GetMapping("/identity/pending")
    public Result<?> pendingIdentities(@RequestParam String username) {
        if (!isAdmin(username)) {
            return Result.error("403", "只有管理员可以查看待审核列表");
        }
        List<SpaIdentity> pending = spaIdentityMapper.selectList(
                Wrappers.<SpaIdentity>lambdaQuery()
                        .eq(SpaIdentity::getAuditStatus, AUDIT_PENDING)
                        .orderByAsc(SpaIdentity::getCreatedAt)
        );
        List<Map<String, Object>> data = new ArrayList<>();
        for (SpaIdentity identity : pending) {
            data.add(toIdentityResponse(identity));
        }
        return Result.success(data);
    }

    /**
     * 审核认证申请，仅管理员可操作
     */
    @PostMapping("/identity/review")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> reviewIdentity(@RequestBody JSONObject params) {
        String adminUsername = StrUtil.trim(params.getString("adminUsername"));
        if (!isAdmin(adminUsername)) {
            return Result.error("403", "只有管理员可以进行审核");
        }
        String username = StrUtil.trim(params.getString("username"));
        Boolean approve = params.getBoolean("approve");
        if (StrUtil.isBlank(username) || approve == null) {
            return Result.error("400", "审核参数不完整");
        }
        SpaIdentity identity = findIdentity(username);
        if (identity == null) {
            return Result.error("404", "未找到对应的认证申请");
        }
        identity.setAuditStatus(approve ? AUDIT_APPROVED : AUDIT_REJECTED);
        identity.setAuditNote(limit(StrUtil.trim(params.getString("note")), 512));
        identity.setUpdatedAt(LocalDateTime.now());
        spaIdentityMapper.updateById(identity);
        return Result.success(toIdentityResponse(identity));
    }

    /**
     * 已完成认证的心灵SPA师列表，只展示公开信息
     */
    @GetMapping("/practitioners")
    public Result<?> practitioners() {
        List<SpaIdentity> approved = spaIdentityMapper.selectList(
                Wrappers.<SpaIdentity>lambdaQuery()
                        .eq(SpaIdentity::getIdentityType, IDENTITY_PRACTITIONER)
                        .eq(SpaIdentity::getAuditStatus, AUDIT_APPROVED)
                        .orderByAsc(SpaIdentity::getCreatedAt)
        );
        List<Map<String, Object>> data = new ArrayList<>();
        for (SpaIdentity identity : approved) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("username", identity.getUsername());
            item.put("realName", identity.getRealName());
            item.put("hospital", identity.getHospital());
            item.put("department", identity.getDepartment());
            item.put("title", identity.getTitle());
            item.put("bio", identity.getBio());
            data.add(item);
        }
        return Result.success(data);
    }

    /**
     * 记录者向心灵SPA师发出联动邀请
     */
    @PostMapping("/requests")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createRequest(@RequestBody JSONObject params) {
        String patientUsername = StrUtil.trim(params.getString("patientUsername"));
        String practitionerUsername = StrUtil.trim(params.getString("practitionerUsername"));
        String initialMessage = StrUtil.trim(params.getString("initialMessage"));
        if (StrUtil.isBlank(patientUsername) || StrUtil.isBlank(practitionerUsername)) {
            return Result.error("400", "联动双方信息不完整");
        }
        SpaIdentity patient = findIdentity(patientUsername);
        if (patient == null || !AUDIT_APPROVED.equals(patient.getAuditStatus())) {
            return Result.error("403", "请先完成实名认证，再向心灵SPA师说出你的心事");
        }
        SpaIdentity practitioner = findIdentity(practitionerUsername);
        if (practitioner == null || !IDENTITY_PRACTITIONER.equals(practitioner.getIdentityType())
                || !AUDIT_APPROVED.equals(practitioner.getAuditStatus())) {
            return Result.error("404", "这位心灵SPA师暂时还无法接收联动邀请");
        }
        SpaRequest request = new SpaRequest();
        request.setPatientUsername(patientUsername);
        request.setPractitionerUsername(practitionerUsername);
        request.setInitialMessage(limit(initialMessage, 1024));
        request.setStatus("active");
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        spaRequestMapper.insert(request);

        // 开场白作为第一条留言保存，让陪伴从第一句话开始
        if (StrUtil.isNotBlank(initialMessage)) {
            SpaMessage opening = new SpaMessage();
            opening.setRequestId(request.getId());
            opening.setSenderUsername(patientUsername);
            opening.setSenderIdentity(IDENTITY_PATIENT);
            opening.setContent(limit(initialMessage, 2048));
            opening.setCreatedAt(LocalDateTime.now());
            spaMessageMapper.insert(opening);
        }
        return Result.success(toRequestResponse(request, patientUsername));
    }

    /**
     * 我的联动单列表，按身份自动区分视角
     */
    @GetMapping("/requests/mine")
    public Result<?> myRequests(@RequestParam String username) {
        String trimmed = StrUtil.trim(username);
        SpaIdentity identity = findIdentity(trimmed);
        List<SpaRequest> requests;
        if (identity != null && IDENTITY_PRACTITIONER.equals(identity.getIdentityType())) {
            requests = spaRequestMapper.selectList(
                    Wrappers.<SpaRequest>lambdaQuery()
                            .eq(SpaRequest::getPractitionerUsername, trimmed)
                            .orderByDesc(SpaRequest::getUpdatedAt)
            );
        } else if (identity != null && IDENTITY_PATIENT.equals(identity.getIdentityType())) {
            requests = spaRequestMapper.selectList(
                    Wrappers.<SpaRequest>lambdaQuery()
                            .eq(SpaRequest::getPatientUsername, trimmed)
                            .orderByDesc(SpaRequest::getUpdatedAt)
            );
        } else {
            // 尚未认证的用户没有可展示的联动单
            requests = Collections.emptyList();
        }
        List<Map<String, Object>> data = new ArrayList<>();
        for (SpaRequest request : requests) {
            data.add(toRequestResponse(request, trimmed));
        }
        return Result.success(data);
    }

    /**
     * 在联动单里留下一句话，仅双方可写
     */
    @PostMapping("/requests/{id}/messages")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> postMessage(@PathVariable long id, @RequestBody JSONObject params) {
        SpaRequest request = spaRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("404", "未找到这张联动单");
        }
        String username = StrUtil.trim(params.getString("username"));
        String content = StrUtil.trim(params.getString("content"));
        if (StrUtil.isBlank(content)) {
            return Result.error("400", "想说的话还没有填写");
        }
        if (!isParticipant(request, username)) {
            return Result.error("403", "只有联动的双方才能在这里留言");
        }
        SpaMessage message = new SpaMessage();
        message.setRequestId(request.getId());
        message.setSenderUsername(username);
        message.setSenderIdentity(resolveSenderIdentity(request, username));
        message.setContent(limit(content, 2048));
        message.setCreatedAt(LocalDateTime.now());
        spaMessageMapper.insert(message);

        request.setUpdatedAt(LocalDateTime.now());
        spaRequestMapper.updateById(request);
        return Result.success(toMessageResponse(message));
    }

    /**
     * 按 afterId 增量读取留言，仅双方可读
     */
    @GetMapping("/requests/{id}/messages")
    public Result<?> listMessages(@PathVariable long id,
                                  @RequestParam String username,
                                  @RequestParam(defaultValue = "0") long afterId) {
        SpaRequest request = spaRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("404", "未找到这张联动单");
        }
        if (!isParticipant(request, StrUtil.trim(username))) {
            return Result.error("403", "只有联动的双方才能查看这些留言");
        }
        List<SpaMessage> messages = spaMessageMapper.selectList(
                Wrappers.<SpaMessage>lambdaQuery()
                        .eq(SpaMessage::getRequestId, id)
                        .gt(SpaMessage::getId, afterId)
                        .orderByAsc(SpaMessage::getId)
        );
        List<Map<String, Object>> data = new ArrayList<>();
        for (SpaMessage message : messages) {
            data.add(toMessageResponse(message));
        }
        return Result.success(data);
    }

    /**
     * 心灵SPA师查看联动对方的觉察记录，仅该单的SPA师本人可见
     */
    @GetMapping("/requests/{id}/patient-records")
    public Result<?> patientRecords(@PathVariable long id, @RequestParam String username) {
        SpaRequest request = spaRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("404", "未找到这张联动单");
        }
        if (!request.getPractitionerUsername().equals(StrUtil.trim(username))) {
            return Result.error("403", "只有这张联动单的心灵SPA师本人可以查看对方的觉察记录");
        }
        List<AwarenessRecord> records = awarenessRecordMapper.selectList(
                Wrappers.<AwarenessRecord>lambdaQuery()
                        .eq(AwarenessRecord::getUsername, request.getPatientUsername())
                        .orderByDesc(AwarenessRecord::getCreatedAt)
                        .last("LIMIT 20")
        );
        List<Map<String, Object>> data = new ArrayList<>();
        for (AwarenessRecord record : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", record.getId());
            item.put("sourceType", record.getSourceType());
            item.put("emotionLabel", record.getEmotionLabel());
            item.put("gentleSummary", record.getGentleSummary());
            item.put("createdAt", record.getCreatedAt());
            data.add(item);
        }
        return Result.success(data);
    }

    private SpaIdentity findIdentity(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return spaIdentityMapper.selectOne(
                Wrappers.<SpaIdentity>lambdaQuery()
                        .eq(SpaIdentity::getUsername, username)
                        .last("LIMIT 1")
        );
    }

    private boolean isAdmin(String username) {
        if (StrUtil.isBlank(username)) {
            return false;
        }
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, StrUtil.trim(username))
                        .last("LIMIT 1")
        );
        return user != null && "admin".equals(user.getRole());
    }

    private boolean isParticipant(SpaRequest request, String username) {
        if (StrUtil.isBlank(username)) {
            return false;
        }
        return request.getPatientUsername().equals(username)
                || request.getPractitionerUsername().equals(username);
    }

    private String resolveSenderIdentity(SpaRequest request, String username) {
        SpaIdentity identity = findIdentity(username);
        if (identity != null && StrUtil.isNotBlank(identity.getIdentityType())) {
            return identity.getIdentityType();
        }
        // 没有认证记录时按联动单中的角色兜底
        return request.getPatientUsername().equals(username) ? IDENTITY_PATIENT : IDENTITY_PRACTITIONER;
    }

    private Map<String, Object> toRequestResponse(SpaRequest request, String viewerUsername) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", request.getId());
        data.put("patientUsername", request.getPatientUsername());
        data.put("practitionerUsername", request.getPractitionerUsername());
        data.put("initialMessage", request.getInitialMessage());
        data.put("status", request.getStatus());
        data.put("createdAt", request.getCreatedAt());
        data.put("updatedAt", request.getUpdatedAt());

        // 对方信息：记录者看到心灵SPA师的公开资料，心灵SPA师看到对方的称呼
        Map<String, Object> counterpart = new LinkedHashMap<>();
        SpaIdentity practitioner = findIdentity(request.getPractitionerUsername());
        if (request.getPatientUsername().equals(viewerUsername)) {
            counterpart.put("username", request.getPractitionerUsername());
            if (practitioner != null) {
                counterpart.put("realName", practitioner.getRealName());
                counterpart.put("hospital", practitioner.getHospital());
                counterpart.put("department", practitioner.getDepartment());
                counterpart.put("title", practitioner.getTitle());
            }
        } else {
            SpaIdentity patient = findIdentity(request.getPatientUsername());
            counterpart.put("username", request.getPatientUsername());
            counterpart.put("realName", patient != null && StrUtil.isNotBlank(patient.getRealName())
                    ? patient.getRealName() : maskUsername(request.getPatientUsername()));
        }
        data.put("counterpart", counterpart);

        SpaMessage latest = spaMessageMapper.selectOne(
                Wrappers.<SpaMessage>lambdaQuery()
                        .eq(SpaMessage::getRequestId, request.getId())
                        .orderByDesc(SpaMessage::getId)
                        .last("LIMIT 1")
        );
        if (latest != null) {
            Map<String, Object> preview = new LinkedHashMap<>();
            preview.put("content", limit(latest.getContent(), 60));
            preview.put("senderUsername", latest.getSenderUsername());
            preview.put("createdAt", latest.getCreatedAt());
            data.put("latestMessage", preview);
        } else {
            data.put("latestMessage", null);
        }
        return data;
    }

    private Map<String, Object> toIdentityResponse(SpaIdentity identity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", identity.getId());
        data.put("username", identity.getUsername());
        data.put("identityType", identity.getIdentityType());
        data.put("realName", identity.getRealName());
        data.put("idCardMasked", identity.getIdCardMasked());
        data.put("licenseNo", identity.getLicenseNo());
        data.put("hospital", identity.getHospital());
        data.put("department", identity.getDepartment());
        data.put("title", identity.getTitle());
        data.put("bio", identity.getBio());
        data.put("auditStatus", identity.getAuditStatus());
        data.put("auditNote", identity.getAuditNote());
        data.put("createdAt", identity.getCreatedAt());
        data.put("updatedAt", identity.getUpdatedAt());
        return data;
    }

    private Map<String, Object> toMessageResponse(SpaMessage message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", message.getId());
        data.put("requestId", message.getRequestId());
        data.put("senderUsername", message.getSenderUsername());
        data.put("senderIdentity", message.getSenderIdentity());
        data.put("content", message.getContent());
        data.put("createdAt", message.getCreatedAt());
        return data;
    }

    /**
     * 身份证号只保留前3位和后4位，中间用星号代替，绝不保存明文
     */
    private String maskIdCard(String idCard) {
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    private String maskUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return "";
        }
        return username.charAt(0) + "***";
    }

    private String limit(String text, int length) {
        if (text == null) return null;
        return text.length() <= length ? text : text.substring(0, length);
    }
}
