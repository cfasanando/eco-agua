package com.ecoamazonas.eco_agua.academy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AcademyAssessmentService {

    private final AcademyCourseRepository courseRepository;
    private final AcademyEnrollmentRepository enrollmentRepository;
    private final AcademyAssessmentRepository assessmentRepository;
    private final AcademyAssessmentQuestionRepository questionRepository;
    private final AcademyAssessmentOptionRepository optionRepository;
    private final AcademyAssessmentAttemptRepository attemptRepository;
    private final AcademyAssessmentAnswerRepository answerRepository;

    public AcademyAssessmentService(AcademyCourseRepository courseRepository,
                                    AcademyEnrollmentRepository enrollmentRepository,
                                    AcademyAssessmentRepository assessmentRepository,
                                    AcademyAssessmentQuestionRepository questionRepository,
                                    AcademyAssessmentOptionRepository optionRepository,
                                    AcademyAssessmentAttemptRepository attemptRepository,
                                    AcademyAssessmentAnswerRepository answerRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional(readOnly = true)
    public AcademyCourse findCourse(Long courseId) {
        return courseRepository.findById(courseId).orElse(null);
    }

    @Transactional(readOnly = true)
    public AcademyAssessment findAssessment(Long assessmentId) {
        return assessmentId != null ? assessmentRepository.findByIdForAdmin(assessmentId).orElse(null) : null;
    }

    @Transactional(readOnly = true)
    public AcademyAssessment findAssessmentForm(Long courseId, Long assessmentId) {
        if (assessmentId != null) {
            return assessmentRepository.findById(assessmentId)
                    .filter(assessment -> assessment.getCourse() != null && assessment.getCourse().getId().equals(courseId))
                    .orElseGet(() -> newAssessmentForm(courseId));
        }
        return newAssessmentForm(courseId);
    }

    @Transactional(readOnly = true)
    public AcademyAssessmentQuestion findQuestionForm(Long assessmentId, Long questionId) {
        if (questionId != null) {
            return questionRepository.findById(questionId)
                    .filter(question -> question.getAssessment() != null && question.getAssessment().getId().equals(assessmentId))
                    .orElseGet(this::newQuestionForm);
        }
        return newQuestionForm();
    }

    @Transactional(readOnly = true)
    public String optionsTextForQuestion(AcademyAssessmentQuestion question) {
        if (question == null || question.getId() == null) {
            return "*Respuesta correcta\nRespuesta incorrecta";
        }
        return optionRepository.findByQuestionOrderByDisplayOrderAscIdAsc(question).stream()
                .map(option -> (option.isCorrect() ? "*" : "") + option.getOptionText())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("*Respuesta correcta\nRespuesta incorrecta");
    }

    @Transactional(readOnly = true)
    public List<AssessmentAdminView> findAssessmentAdminViews(AcademyCourse course) {
        if (course == null) {
            return List.of();
        }
        return assessmentRepository.findByCourseOrderByDisplayOrderAscIdAsc(course).stream()
                .map(assessment -> new AssessmentAdminView(
                        assessment,
                        Math.toIntExact(Math.min(questionRepository.countByAssessmentAndActiveTrue(assessment), Integer.MAX_VALUE)),
                        countAttemptsForAssessment(assessment)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionAdminView> findQuestionAdminViews(AcademyAssessment assessment) {
        if (assessment == null) {
            return List.of();
        }
        return questionRepository.findByAssessmentOrderByDisplayOrderAscIdAsc(assessment).stream()
                .map(question -> new QuestionAdminView(question, optionRepository.findByQuestionOrderByDisplayOrderAscIdAsc(question)))
                .toList();
    }

    @Transactional
    public AcademyAssessment saveAssessment(Long courseId, AcademyAssessment form) {
        AcademyCourse course = courseRepository.findById(courseId).orElseThrow();
        AcademyAssessment target = form;
        if (form.getId() != null) {
            target = assessmentRepository.findById(form.getId())
                    .filter(assessment -> assessment.getCourse() != null && assessment.getCourse().getId().equals(courseId))
                    .orElse(form);
            target.setTitle(form.getTitle());
            target.setDescription(form.getDescription());
            target.setPassingScore(form.getPassingScore());
            target.setMaxAttempts(form.getMaxAttempts());
            target.setDisplayOrder(form.getDisplayOrder());
            target.setStatus(form.getStatus());
            target.setActive(form.isActive());
        }
        target.setCourse(course);
        normalizeAssessment(target);
        return assessmentRepository.save(target);
    }

    @Transactional
    public void publishAssessment(Long courseId, Long assessmentId) {
        updateAssessmentStatus(courseId, assessmentId, AcademyAssessment.Status.PUBLISHED, true);
    }

    @Transactional
    public void draftAssessment(Long courseId, Long assessmentId) {
        updateAssessmentStatus(courseId, assessmentId, AcademyAssessment.Status.DRAFT, true);
    }

    @Transactional
    public void archiveAssessment(Long courseId, Long assessmentId) {
        updateAssessmentStatus(courseId, assessmentId, AcademyAssessment.Status.ARCHIVED, false);
    }

    @Transactional
    public AcademyAssessmentQuestion saveQuestion(Long assessmentId, AcademyAssessmentQuestion form, String optionsText) {
        AcademyAssessment assessment = assessmentRepository.findById(assessmentId).orElseThrow();
        AcademyAssessmentQuestion target = form;
        if (form.getId() != null) {
            target = questionRepository.findById(form.getId())
                    .filter(question -> question.getAssessment() != null && question.getAssessment().getId().equals(assessmentId))
                    .orElse(form);
            target.setQuestionText(form.getQuestionText());
            target.setQuestionType(form.getQuestionType());
            target.setExplanation(form.getExplanation());
            target.setPoints(form.getPoints());
            target.setDisplayOrder(form.getDisplayOrder());
            target.setActive(form.isActive());
        }
        target.setAssessment(assessment);
        normalizeQuestion(target);
        AcademyAssessmentQuestion saved = questionRepository.save(target);
        replaceOptions(saved, optionsText);
        return saved;
    }

    @Transactional
    public void toggleQuestion(Long assessmentId, Long questionId) {
        questionRepository.findById(questionId)
                .filter(question -> question.getAssessment() != null && question.getAssessment().getId().equals(assessmentId))
                .ifPresent(question -> {
                    question.setActive(!question.isActive());
                    questionRepository.save(question);
                });
    }

    @Transactional(readOnly = true)
    public List<StudentAssessmentCard> findStudentAssessmentCards(AcademyEnrollment enrollment) {
        if (enrollment == null || enrollment.getCourse() == null) {
            return List.of();
        }
        return assessmentRepository.findByCourseAndActiveTrueAndStatusOrderByDisplayOrderAscIdAsc(
                        enrollment.getCourse(), AcademyAssessment.Status.PUBLISHED)
                .stream()
                .map(assessment -> toStudentCard(enrollment, assessment))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssessmentTakeView buildTakeView(AcademyEnrollment enrollment, Long assessmentId, Long attemptId) {
        AcademyAssessment assessment = findPublishedAssessmentForEnrollment(enrollment, assessmentId);
        if (assessment == null) {
            return null;
        }
        List<AcademyAssessmentAttempt> attempts = attemptRepository.findByEnrollmentAndAssessmentOrderByAttemptNumberDescIdDesc(enrollment, assessment);
        AttemptResultView result = attemptId != null ? findAttemptResult(enrollment, attemptId).orElse(null) : null;
        List<QuestionTakeView> questions = questionRepository.findByAssessmentAndActiveTrueOrderByDisplayOrderAscIdAsc(assessment).stream()
                .map(question -> new QuestionTakeView(
                        question.getId(),
                        question.getQuestionText(),
                        question.getTypeLabel(),
                        question.getPoints(),
                        optionRepository.findByQuestionAndActiveTrueOrderByDisplayOrderAscIdAsc(question).stream()
                                .map(option -> new OptionTakeView(option.getId(), option.getOptionText()))
                                .toList()))
                .toList();
        boolean canAttempt = attempts.size() < assessment.getMaxAttempts() && !questions.isEmpty();
        return new AssessmentTakeView(
                assessment.getId(),
                assessment.getTitle(),
                defaultIfBlank(assessment.getDescription(), ""),
                assessment.getPassingScore(),
                assessment.getMaxAttempts(),
                attempts.size(),
                canAttempt,
                questions,
                attempts.stream().map(this::toAttemptSummary).toList(),
                result);
    }

    @Transactional
    public Long submitAssessment(String username, String courseSlug, Long assessmentId, Map<String, String> parameters) {
        AcademyEnrollment enrollment = enrollmentRepository.findByStudentUsernameAndCourseSlug(username, courseSlug)
                .filter(AcademyEnrollment::isActiveEnrollment)
                .orElseThrow();
        AcademyAssessment assessment = findPublishedAssessmentForEnrollment(enrollment, assessmentId);
        if (assessment == null) {
            throw new IllegalArgumentException("Assessment not available");
        }
        long previousAttempts = attemptRepository.countByEnrollmentAndAssessment(enrollment, assessment);
        if (previousAttempts >= assessment.getMaxAttempts()) {
            throw new IllegalStateException("No attempts available");
        }

        List<AcademyAssessmentQuestion> questions = questionRepository.findByAssessmentAndActiveTrueOrderByDisplayOrderAscIdAsc(assessment);
        Map<Long, List<AcademyAssessmentOption>> optionsByQuestion = new LinkedHashMap<>();
        for (AcademyAssessmentQuestion question : questions) {
            optionsByQuestion.put(question.getId(), optionRepository.findByQuestionAndActiveTrueOrderByDisplayOrderAscIdAsc(question));
        }

        AcademyAssessmentAttempt attempt = new AcademyAssessmentAttempt();
        attempt.setAssessment(assessment);
        attempt.setEnrollment(enrollment);
        attempt.setStudent(enrollment.getStudent());
        attempt.setCourse(enrollment.getCourse());
        attempt.setAttemptNumber((int) previousAttempts + 1);
        attempt.setStatus(AcademyAssessmentAttempt.Status.SUBMITTED);
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setSubmittedAt(LocalDateTime.now());

        int maxScore = questions.stream().mapToInt(AcademyAssessmentQuestion::getPoints).sum();
        int score = 0;
        attempt.setMaxScore(maxScore);
        attempt = attemptRepository.save(attempt);

        for (AcademyAssessmentQuestion question : questions) {
            List<AcademyAssessmentOption> options = optionsByQuestion.getOrDefault(question.getId(), List.of());
            String selectedValue = parameters.get("question_" + question.getId());
            AcademyAssessmentOption selected = findOption(options, selectedValue).orElse(null);
            boolean correct = selected != null && selected.isCorrect();
            int points = correct ? question.getPoints() : 0;
            score += points;

            AcademyAssessmentAnswer answer = new AcademyAssessmentAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setSelectedOption(selected);
            answer.setAnswerText(selected != null ? selected.getOptionText() : "");
            answer.setCorrect(correct);
            answer.setPointsAwarded(points);
            answerRepository.save(answer);
        }

        int percent = maxScore > 0 ? (int) Math.round((score * 100.0d) / maxScore) : 0;
        attempt.setScore(score);
        attempt.setPassed(percent >= assessment.getPassingScore());
        attemptRepository.save(attempt);
        return attempt.getId();
    }

    @Transactional(readOnly = true)
    public Optional<AttemptResultView> findAttemptResult(AcademyEnrollment enrollment, Long attemptId) {
        if (enrollment == null || attemptId == null) {
            return Optional.empty();
        }
        return attemptRepository.findById(attemptId)
                .filter(attempt -> attempt.getEnrollment() != null && attempt.getEnrollment().getId().equals(enrollment.getId()))
                .map(this::toAttemptResult);
    }

    @Transactional(readOnly = true)
    public List<AdminAttemptResultView> findAdminResults() {
        return attemptRepository.findAllForAdmin().stream()
                .map(attempt -> new AdminAttemptResultView(
                        attempt.getId(),
                        attempt.getStudent() != null ? attempt.getStudent().getUsername() : "-",
                        attempt.getCourse() != null ? attempt.getCourse().getTitle() : "-",
                        attempt.getAssessment() != null ? attempt.getAssessment().getTitle() : "-",
                        attempt.getAttemptNumber(),
                        attempt.getScore(),
                        attempt.getMaxScore(),
                        attempt.getPercentScore(),
                        attempt.isPassed(),
                        attempt.getSubmittedAt()))
                .toList();
    }

    private AcademyAssessment findPublishedAssessmentForEnrollment(AcademyEnrollment enrollment, Long assessmentId) {
        if (enrollment == null || enrollment.getCourse() == null || assessmentId == null) {
            return null;
        }
        return assessmentRepository.findById(assessmentId)
                .filter(AcademyAssessment::isPublished)
                .filter(assessment -> assessment.getCourse() != null && assessment.getCourse().getId().equals(enrollment.getCourse().getId()))
                .orElse(null);
    }

    private AcademyAssessment newAssessmentForm(Long courseId) {
        AcademyAssessment assessment = new AcademyAssessment();
        courseRepository.findById(courseId).ifPresent(assessment::setCourse);
        assessment.setTitle("Evaluación del curso");
        assessment.setDescription("Comprueba los aprendizajes principales del curso.");
        assessment.setPassingScore(70);
        assessment.setMaxAttempts(3);
        assessment.setDisplayOrder(1);
        assessment.setStatus(AcademyAssessment.Status.DRAFT);
        assessment.setActive(true);
        return assessment;
    }

    private AcademyAssessmentQuestion newQuestionForm() {
        AcademyAssessmentQuestion question = new AcademyAssessmentQuestion();
        question.setQuestionText("Nueva pregunta");
        question.setQuestionType(AcademyAssessmentQuestion.QuestionType.MULTIPLE_CHOICE);
        question.setPoints(1);
        question.setDisplayOrder(1);
        question.setActive(true);
        return question;
    }

    private void updateAssessmentStatus(Long courseId, Long assessmentId, AcademyAssessment.Status status, boolean active) {
        assessmentRepository.findById(assessmentId)
                .filter(assessment -> assessment.getCourse() != null && assessment.getCourse().getId().equals(courseId))
                .ifPresent(assessment -> {
                    assessment.setStatus(status);
                    assessment.setActive(active);
                    assessmentRepository.save(assessment);
                });
    }

    private void normalizeAssessment(AcademyAssessment assessment) {
        assessment.setTitle(defaultIfBlank(assessment.getTitle(), "Evaluación"));
        assessment.setDescription(clean(assessment.getDescription()));
        assessment.setPassingScore(Math.max(1, Math.min(100, assessment.getPassingScore())));
        assessment.setMaxAttempts(Math.max(1, Math.min(10, assessment.getMaxAttempts())));
        if (assessment.getDisplayOrder() <= 0) {
            assessment.setDisplayOrder(1);
        }
        if (assessment.getStatus() == null) {
            assessment.setStatus(AcademyAssessment.Status.DRAFT);
        }
    }

    private void normalizeQuestion(AcademyAssessmentQuestion question) {
        question.setQuestionText(defaultIfBlank(question.getQuestionText(), "Pregunta"));
        question.setExplanation(clean(question.getExplanation()));
        if (question.getQuestionType() == null) {
            question.setQuestionType(AcademyAssessmentQuestion.QuestionType.MULTIPLE_CHOICE);
        }
        if (question.getPoints() <= 0) {
            question.setPoints(1);
        }
        if (question.getDisplayOrder() <= 0) {
            question.setDisplayOrder(1);
        }
    }

    private void replaceOptions(AcademyAssessmentQuestion question, String optionsText) {
        optionRepository.deleteByQuestion(question);
        optionRepository.flush();
        List<OptionLine> lines = new ArrayList<>(parseOptionLines(optionsText));
        if (question.getQuestionType() == AcademyAssessmentQuestion.QuestionType.TRUE_FALSE && lines.size() < 2) {
            lines = new ArrayList<>(List.of(new OptionLine("Verdadero", true), new OptionLine("Falso", false)));
        }
        if (lines.isEmpty()) {
            lines = new ArrayList<>(List.of(new OptionLine("Respuesta correcta", true), new OptionLine("Respuesta incorrecta", false)));
        }
        if (lines.stream().noneMatch(OptionLine::correct)) {
            OptionLine first = lines.get(0);
            lines.set(0, new OptionLine(first.text(), true));
        }
        int order = 1;
        for (OptionLine line : lines) {
            AcademyAssessmentOption option = new AcademyAssessmentOption();
            option.setQuestion(question);
            option.setOptionText(line.text());
            option.setCorrect(line.correct());
            option.setDisplayOrder(order++);
            option.setActive(true);
            optionRepository.save(option);
        }
    }

    private List<OptionLine> parseOptionLines(String optionsText) {
        if (optionsText == null || optionsText.isBlank()) {
            return List.of();
        }
        return optionsText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> {
                    boolean correct = line.startsWith("*");
                    String text = correct ? line.substring(1).trim() : line;
                    return new OptionLine(defaultIfBlank(text, "Opción"), correct);
                })
                .toList();
    }

    private StudentAssessmentCard toStudentCard(AcademyEnrollment enrollment, AcademyAssessment assessment) {
        List<AcademyAssessmentAttempt> attempts = attemptRepository.findByEnrollmentAndAssessmentOrderByAttemptNumberDescIdDesc(enrollment, assessment);
        AcademyAssessmentAttempt best = attempts.stream()
                .max(Comparator.comparing(AcademyAssessmentAttempt::getPercentScore))
                .orElse(null);
        return new StudentAssessmentCard(
                assessment.getId(),
                assessment.getTitle(),
                defaultIfBlank(assessment.getDescription(), ""),
                assessment.getPassingScore(),
                assessment.getMaxAttempts(),
                attempts.size(),
                attempts.size() < assessment.getMaxAttempts(),
                best != null ? best.getPercentScore() : null,
                best != null && best.isPassed());
    }

    private int countAttemptsForAssessment(AcademyAssessment assessment) {
        return Math.toIntExact(Math.min(attemptRepository.countByAssessment(assessment), Integer.MAX_VALUE));
    }

    private AttemptSummaryView toAttemptSummary(AcademyAssessmentAttempt attempt) {
        return new AttemptSummaryView(
                attempt.getId(),
                attempt.getAttemptNumber(),
                attempt.getScore(),
                attempt.getMaxScore(),
                attempt.getPercentScore(),
                attempt.isPassed(),
                attempt.getSubmittedAt());
    }

    private AttemptResultView toAttemptResult(AcademyAssessmentAttempt attempt) {
        List<AnswerReviewView> answers = answerRepository.findByAttemptForReview(attempt).stream()
                .map(answer -> new AnswerReviewView(
                        answer.getQuestion() != null ? answer.getQuestion().getQuestionText() : "Pregunta",
                        answer.getSelectedOption() != null ? answer.getSelectedOption().getOptionText() : "Sin respuesta",
                        answer.isCorrect(),
                        answer.getPointsAwarded(),
                        answer.getQuestion() != null ? answer.getQuestion().getPoints() : 0,
                        answer.getQuestion() != null ? defaultIfBlank(answer.getQuestion().getExplanation(), "") : ""))
                .toList();
        return new AttemptResultView(
                attempt.getId(),
                attempt.getAttemptNumber(),
                attempt.getScore(),
                attempt.getMaxScore(),
                attempt.getPercentScore(),
                attempt.isPassed(),
                attempt.getSubmittedAt(),
                answers);
    }

    private Optional<AcademyAssessmentOption> findOption(List<AcademyAssessmentOption> options, String selectedValue) {
        if (selectedValue == null || selectedValue.isBlank()) {
            return Optional.empty();
        }
        try {
            Long selectedId = Long.valueOf(selectedValue);
            return options.stream().filter(option -> option.getId().equals(selectedId)).findFirst();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String clean(String value) {
        return value != null ? value.trim() : "";
    }

    private String defaultIfBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    public record AssessmentAdminView(AcademyAssessment assessment, int questionCount, int attemptCount) {
    }

    public record QuestionAdminView(AcademyAssessmentQuestion question, List<AcademyAssessmentOption> options) {
    }

    public record StudentAssessmentCard(Long assessmentId,
                                        String title,
                                        String description,
                                        int passingScore,
                                        int maxAttempts,
                                        int attemptCount,
                                        boolean canAttempt,
                                        Integer bestPercent,
                                        boolean bestPassed) {
    }

    public record AssessmentTakeView(Long assessmentId,
                                     String title,
                                     String description,
                                     int passingScore,
                                     int maxAttempts,
                                     int attemptCount,
                                     boolean canAttempt,
                                     List<QuestionTakeView> questions,
                                     List<AttemptSummaryView> attempts,
                                     AttemptResultView result) {
    }

    public record QuestionTakeView(Long questionId,
                                   String questionText,
                                   String typeLabel,
                                   int points,
                                   List<OptionTakeView> options) {
    }

    public record OptionTakeView(Long optionId, String optionText) {
    }

    public record AttemptSummaryView(Long attemptId,
                                     int attemptNumber,
                                     int score,
                                     int maxScore,
                                     int percent,
                                     boolean passed,
                                     LocalDateTime submittedAt) {
    }

    public record AttemptResultView(Long attemptId,
                                    int attemptNumber,
                                    int score,
                                    int maxScore,
                                    int percent,
                                    boolean passed,
                                    LocalDateTime submittedAt,
                                    List<AnswerReviewView> answers) {
    }

    public record AnswerReviewView(String questionText,
                                   String selectedOptionText,
                                   boolean correct,
                                   int pointsAwarded,
                                   int questionPoints,
                                   String explanation) {
    }

    public record AdminAttemptResultView(Long attemptId,
                                         String username,
                                         String courseTitle,
                                         String assessmentTitle,
                                         int attemptNumber,
                                         int score,
                                         int maxScore,
                                         int percent,
                                         boolean passed,
                                         LocalDateTime submittedAt) {
    }

    private record OptionLine(String text, boolean correct) {
    }
}
