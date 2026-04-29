package com.example.patent.skill;

import com.example.patent.agent.TaskAgent;
import com.example.patent.skill.domain.SkillRoutingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SkillRouter {

    private final TaskAgent taskAgent;

    public SkillRoutingResult route(String userQuery) {
        return taskAgent.plan(userQuery, null);
    }

    public SkillRoutingResult route(String userQuery, String historyContext) {
        return taskAgent.plan(userQuery, historyContext);
    }
}
