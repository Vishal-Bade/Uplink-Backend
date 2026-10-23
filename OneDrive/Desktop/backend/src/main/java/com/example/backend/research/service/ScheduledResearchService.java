package com.example.backend.research.service;

import com.example.backend.project.entity.Project;
import com.example.backend.project.repository.ProjectRepository;
import com.example.backend.research.entity.Notification;
import com.example.backend.research.entity.ResearchRun;
import com.example.backend.research.entity.ScheduledResearch;
import com.example.backend.research.enums.ResearchDepth;
import com.example.backend.research.repository.NotificationRepository;
import com.example.backend.research.repository.ResearchRunRepository;
import com.example.backend.research.repository.ScheduledResearchRepository;
import com.example.backend.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ScheduledResearchService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledResearchService.class);

    private final ScheduledResearchRepository scheduledResearchRepository;
    private final ProjectRepository projectRepository;
    private final ResearchRunRepository researchRunRepository;
    private final ResearchWorkflowCoordinator workflowCoordinator;
    private final NotificationRepository notificationRepository;

    public ScheduledResearchService(
            ScheduledResearchRepository scheduledResearchRepository,
            ProjectRepository projectRepository,
            ResearchRunRepository researchRunRepository,
            ResearchWorkflowCoordinator workflowCoordinator,
            NotificationRepository notificationRepository) {
        this.scheduledResearchRepository = scheduledResearchRepository;
        this.projectRepository = projectRepository;
        this.researchRunRepository = researchRunRepository;
        this.workflowCoordinator = workflowCoordinator;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public ScheduledResearch createSchedule(UUID projectId, String topic, String cronExpression, ResearchDepth depth, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        ScheduledResearch schedule = new ScheduledResearch(project, topic, cronExpression, depth, user);
        schedule.setNextRunAt(LocalDateTime.now().plusHours(24)); // Default next run
        return scheduledResearchRepository.save(schedule);
    }

    public List<ScheduledResearch> listByProject(UUID projectId) {
        return scheduledResearchRepository.findByProjectId(projectId);
    }

    @Transactional
    public ScheduledResearch updateSchedule(UUID scheduleId, Boolean active, String topic, String cronExpression, ResearchDepth depth) {
        ScheduledResearch schedule = scheduledResearchRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        if (active != null) schedule.setActive(active);
        if (topic != null && !topic.isBlank()) schedule.setTopic(topic);
        if (cronExpression != null && !cronExpression.isBlank()) schedule.setCronExpression(cronExpression);
        if (depth != null) schedule.setDepth(depth);

        return scheduledResearchRepository.save(schedule);
    }

    @Transactional
    public void deleteSchedule(UUID scheduleId) {
        scheduledResearchRepository.deleteById(scheduleId);
    }

    @Scheduled(fixedDelay = 60000) // Check every minute
    @Transactional
    public void executeDueSchedules() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledResearch> dueSchedules = scheduledResearchRepository.findByActiveTrueAndNextRunAtBefore(now);

        for (ScheduledResearch schedule : dueSchedules) {
            log.info("Executing scheduled recurring research for topic: '{}' (ID: {})", schedule.getTopic(), schedule.getId());
            schedule.setLastRunAt(now);
            schedule.setNextRunAt(now.plusDays(1)); // Schedule next recurrence
            scheduledResearchRepository.save(schedule);

            // Create research run
            ResearchRun run = new ResearchRun(
                    schedule.getProject(),
                    schedule.getTopic(),
                    schedule.getDepth(),
                    true,
                    schedule.getCreatedBy()
            );
            run = researchRunRepository.save(run);

            // Execute research pipeline asynchronously
            workflowCoordinator.executeResearchPipelineAsync(run.getId());

            // Notify user
            Notification notification = new Notification(
                    schedule.getCreatedBy(),
                    "Scheduled Research Triggered",
                    "Automated research on '" + schedule.getTopic() + "' has commenced.",
                    "SCHEDULE_TRIGGERED"
            );
            notificationRepository.save(notification);
        }
    }
}
