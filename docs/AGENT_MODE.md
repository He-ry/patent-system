# Agent Mode Design

## Goal

Replace the old "router prompt decides everything" flow with a task-agent flow closer to modern coding tools:

1. Understand the user task
2. Decide task mode
3. Choose the right tool
4. Execute the tool
5. Observe the result
6. Produce the final answer

## Main Roles

- `TaskAgent`
  Responsible for task understanding and planning.
  It does not answer the user directly.
  It outputs a structured plan.

- `SkillExecutor`
  Responsible for executing tools such as:
  - `sql-generator`
  - `data-analyzer`
  - `report-preview`

- `ChatService`
  Responsible for conversation lifecycle:
  - create / reuse conversation
  - save user message
  - ask agent for a plan
  - execute tools
  - stream the final answer
  - persist assistant message

## Task Modes

- `CHAT`
  Normal dialog, explanation, capability Q&A. No tool required.

- `QUERY`
  Direct list / detail / count / filter / ranking requests.
  Preferred tool: `sql-generator`

- `ANALYSIS`
  Short analysis, trend summary, overview without a full report.
  Preferred tool: `data-analyzer`

- `REPORT`
  Full report generation.
  Preferred tool: `report-preview`

- `CLARIFY`
  Reserved for future use when the agent decides the request is too ambiguous.

## Prompt Management Rules

All prompts should live under:

`src/main/resources/prompts/`

Recommended layout:

- `prompts/agent/`
  Task planning prompts

- `prompts/chat/`
  Final answer / suggestions prompts

- `prompts/skill/`
  Tool repair prompts such as SQL fix

- `prompts/report/`
  Report intent, SQL task generation, chart selection, analysis and summary prompts

Java code should not contain large inline prompts.
Code should only:

1. Load a template
2. Fill variables
3. Send it to the model

## Current Migration Status

- Agent planner introduced:
  - `com.example.patent.agent.TaskAgent`
  - `com.example.patent.agent.domain.AgentTaskPlan`
  - `com.example.patent.agent.domain.AgentTaskMode`

- Prompt loader introduced:
  - `com.example.patent.prompt.PromptTemplateService`

- Router now delegates to the task agent:
  - `com.example.patent.skill.SkillRouter`

## Next Migration Steps

1. Move remaining inline prompts from `ChatService` to `prompts/chat/`
2. Move SQL repair prompts from `SkillExecutor` to `prompts/skill/`
3. Move report prompts from `IntentAnalyzer` and `AiReportGenerator` to `prompts/report/`
4. Add a second-step observe-and-retry policy for empty SQL results
5. Add agent execution traces for frontend progress visualization

## Why This Structure Helps

- Easier to maintain prompts
- Easier to version and review prompt changes
- Cleaner Java code
- More controllable task behavior
- Closer to modern task-based agent products
