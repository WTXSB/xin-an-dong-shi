package com.example.Ece.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.Ece.common.Result;
import com.example.Ece.entity.HealingConversation;
import com.example.Ece.mapper.HealingConversationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiChatController {
    private static final String SYSTEM_PROMPT =
            "你是“心安动识”的陪伴式心灵疗愈助手安小宁。"
                    + "你的语气要温柔、具体、稳定、有陪伴感，帮助用户看见情绪、放松身体、恢复内驱力。"
                    + "不要给用户贴标签，不要做定性判断，不要使用吓人的表达，不要把暂时的状态说成固定结论。"
                    + "优先使用短段落回应，先接住感受，再给一个当下能完成的小练习。"
                    + "当用户表达强烈危险、失控或无法照顾自己时，先温柔安抚，并建议尽快联系身边可信任的人或当地紧急支持资源。"
                    + "不要声称你能读取图片、摄像头或识别记录；只有用户主动写出的文字可以进入本次对话。";

    @Value("${deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String deepseekApiUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Resource
    HealingConversationMapper healingConversationMapper;

    @PostMapping("/chat")
    public Result<?> chat(@RequestBody Map<String, Object> body) {
        String message = readString(body, "message").trim();
        String username = readString(body, "username").trim();
        boolean saveConversation = body.get("saveConversation") == null
                || Boolean.parseBoolean(String.valueOf(body.get("saveConversation")));

        if (message.length() == 0) {
            return Result.error("-1", "可以先写下一点点想说的话，我会慢慢听。");
        }
        if (message.length() > 2000) {
            return Result.error("-1", "这段话有点长。可以先挑最想被看见的一小段发给我。");
        }

        Map<String, Object> reply;
        if (deepseekApiKey == null || deepseekApiKey.trim().length() == 0) {
            System.out.println("[AiChat] DeepSeek API key is not configured. Using local fallback.");
            reply = localGentleReply(message);
        } else {
            System.out.println("[AiChat] Calling DeepSeek API. model=" + deepseekModel);
            reply = remoteGentleReply(message, body.get("messages"));
        }

        if (saveConversation) {
            saveConversation(username, message, reply);
        }
        return Result.success(reply);
    }

    private Map<String, Object> remoteGentleReply(String message, Object context) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepseekApiKey.trim());

            JSONObject request = new JSONObject();
            request.put("model", deepseekModel);
            request.put("temperature", 0.65);
            request.put("max_tokens", 900);
            request.put("stream", false);
            request.put("messages", buildMessages(message, context));

            HttpEntity<String> entity = new HttpEntity<>(request.toJSONString(), headers);
            ResponseEntity<String> response = new RestTemplate().exchange(deepseekApiUrl, HttpMethod.POST, entity, String.class);
            JSONObject responseJson = JSONObject.parseObject(response.getBody());
            String answer = responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            if (answer == null || answer.trim().length() == 0) {
                return localGentleReply(message);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("reply", answer.trim());
            data.put("provider", "deepseek");
            data.put("model", deepseekModel);
            data.put("privacy", "本次对话由后端转发到 DeepSeek，前端不会接触 API Key。");
            return data;
        } catch (Exception e) {
            System.out.println("[AiChat] DeepSeek API call failed: " + e.getMessage());
            return localGentleReply(message);
        }
    }

    private JSONArray buildMessages(String message, Object context) {
        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", SYSTEM_PROMPT);
        messages.add(system);

        if (context instanceof List<?>) {
            List<?> list = (List<?>) context;
            int start = Math.max(0, list.size() - 8);
            for (int i = start; i < list.size(); i++) {
                Object item = list.get(i);
                if (!(item instanceof Map<?, ?>)) {
                    continue;
                }
                Map<?, ?> map = (Map<?, ?>) item;
                String role = String.valueOf(map.get("role"));
                String content = map.get("content") == null ? "" : String.valueOf(map.get("content")).trim();
                if (!("assistant".equals(role) || "user".equals(role)) || content.length() == 0) {
                    continue;
                }
                JSONObject contextMessage = new JSONObject();
                contextMessage.put("role", role);
                contextMessage.put("content", limit(content, 1200));
                messages.add(contextMessage);
            }
        }

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", message);
        messages.add(user);
        return messages;
    }

    private Map<String, Object> localGentleReply(String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("provider", "local-fallback");
        data.put("reply", buildLocalReply(message));
        data.put("privacy", "当前没有配置 DeepSeek API Key，因此这次对话没有发送到外部模型。");
        return data;
    }

    private String buildLocalReply(String message) {
        String lower = message.toLowerCase();
        String opening = "我先轻轻接住你刚才说的这些。愿意把它说出来，已经是在照顾自己了。";
        if (lower.contains("学") || lower.contains("任务") || lower.contains("动力") || lower.contains("效率")) {
            return opening + "现在先别急着要求自己一下子恢复状态。可以把今天的事拆成一个很小的动作：只打开资料、只写三行、只做五分钟。完成以后停一下，告诉自己：我已经开始了。";
        }
        if (lower.contains("睡") || lower.contains("累") || lower.contains("疲")) {
            return opening + "如果身体已经很累，先把目标从“继续撑住”换成“让身体回一点能量”。可以做三轮慢呼吸，再喝一点温水，给自己十分钟不被任务追赶的时间。";
        }
        if (lower.contains("急") || lower.contains("慌") || lower.contains("紧") || lower.contains("怕")) {
            return opening + "你可以先把双脚踩稳，慢慢吸气 4 秒，停 2 秒，再呼气 6 秒。然后轻轻问自己：此刻真正需要我处理的一小步是什么？";
        }
        return opening + "现在可以先做一个很小的动作：吸气 4 秒，停留 2 秒，再呼气 6 秒。等身体稍微松一点，再问问自己：此刻我最需要的是休息、支持，还是把事情拆小一点？";
    }

    private void saveConversation(String username, String message, Map<String, Object> reply) {
        HealingConversation conversation = new HealingConversation();
        conversation.setUsername(username);
        conversation.setProvider(String.valueOf(reply.get("provider")));
        conversation.setUserMessage(limit(message, 2048));
        conversation.setAssistantReply(limit(String.valueOf(reply.get("reply")), 4096));
        conversation.setSafetyNote("对话记录用于帮助你回看自己的照顾过程；你可以在对话前取消保存。");
        conversation.setCreatedAt(LocalDateTime.now());
        healingConversationMapper.insert(conversation);
    }

    private String readString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private String limit(String text, int length) {
        if (text == null) {
            return "";
        }
        return text.length() <= length ? text : text.substring(0, length);
    }
}
