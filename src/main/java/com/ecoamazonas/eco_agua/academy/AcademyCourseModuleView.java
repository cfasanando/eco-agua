package com.ecoamazonas.eco_agua.academy;

import java.util.List;

public class AcademyCourseModuleView {

    private final AcademyCourseModule module;
    private final List<AcademyLesson> lessons;

    public AcademyCourseModuleView(AcademyCourseModule module, List<AcademyLesson> lessons) {
        this.module = module;
        this.lessons = lessons != null ? lessons : List.of();
    }

    public AcademyCourseModule getModule() {
        return module;
    }

    public List<AcademyLesson> getLessons() {
        return lessons;
    }

    public int getLessonCount() {
        return lessons.size();
    }

    public long getPreviewCount() {
        return lessons.stream().filter(AcademyLesson::isPreview).count();
    }
}
