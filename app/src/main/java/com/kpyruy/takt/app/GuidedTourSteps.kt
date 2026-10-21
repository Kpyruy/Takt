package com.kpyruy.takt.app

import com.kpyruy.takt.core.ui.i18n.t
import com.kpyruy.takt.core.model.UiKey
import com.kpyruy.takt.core.model.AppLanguage
import com.kpyruy.takt.core.ui.i18n.TaktI18n

internal data class TourCopy(val source: String) {
    fun text(language: AppLanguage = TaktI18n.language) = TaktI18n.text(source, language)
}

/** Fixed order and stable IDs also make saved progress independent of translated labels. */
internal enum class GuidedTourStep(
    val scene: String,
    val target: String,
    val title: TourCopy,
    val body: TourCopy,
    val tab: Int = 0,
) {
    TODAY_WEEK("home", "home-week",
        TourCopy(UiKey.TEXT_YOUR_WEEK_ONE_SWIPE_AWAY),
        TourCopy(UiKey.TEXT_SWIPE_THE_DAYS_AT_THE_TOP_LEFT_OR_RIGHT_TO_CHANGE_THE_WEEK_TAP_26C01E0C)),
    TODAY_COURSE("home", "home-lessons",
        TourCopy(UiKey.TEXT_A_CLASS_OPENS_ITS_SUBJECT),
        TourCopy(UiKey.TEXT_TAP_A_CLASS_TO_OPEN_ITS_SUBJECT_TASKS_EXAM_AND_NOTES_PRESS_AND_0C3AEBCA)),
    CALENDAR_DAY("calendar", "calendar-modes",
        TourCopy(UiKey.TEXT_CALENDAR_DAY),
        TourCopy(UiKey.TEXT_DAY_VIEW_SHOWS_THE_SELECTED_DAY_S_SCHEDULE_AND_DEADLINES_USE_T_D711218E)),
    CALENDAR_WEEK("calendar", "calendar-modes",
        TourCopy(UiKey.TEXT_THE_WHOLE_WEEK_TOGETHER),
        TourCopy(UiKey.TEXT_WEEK_VIEW_HELPS_YOU_SPOT_BUSY_HOURS_AND_FREE_TIME_TAP_A_DAY_IN_51C08BD8), 1),
    CALENDAR_MONTH("calendar", "calendar-modes",
        TourCopy(UiKey.TEXT_A_MONTH_OF_IMPORTANT_DATES),
        TourCopy(UiKey.TEXT_MONTH_VIEW_MARKS_CLASSES_TESTS_AND_DEADLINES_SELECT_A_DATE_TO_CE8C4062), 2),
    CALENDAR_TODAY("calendar", "calendar-modes",
        TourCopy(UiKey.TEXT_BACK_TO_TODAY_WITH_A_DOUBLE_TAP),
        TourCopy(UiKey.TEXT_LOST_AMONG_DATES_QUICKLY_DOUBLE_TAP_THE_ALREADY_SELECTED_DAY_W_A83DA237), 2),
    CALENDAR_LAYOUT("calendar", "calendar-week-layout",
        TourCopy(UiKey.TEXT_TWO_WAYS_TO_SEE_YOUR_WEEK),
        TourCopy(UiKey.TEXT_TAP_THE_ARROW_BESIDE_WEEK_OR_PRESS_AND_HOLD_THE_BUTTON_CHOOSE_2BA25171), 1),
    SUBJECTS("subjects", "subjects-list",
        TourCopy(UiKey.TEXT_YOUR_ACTIVE_SUBJECTS),
        TourCopy(UiKey.TEXT_HERE_ARE_THE_CURRENT_SEMESTER_S_ACTIVE_SUBJECTS_POINTS_ADMISSI_47063E5B)),
    COURSE_ICON("subject", "subject-icon",
        TourCopy(UiKey.TEXT_MAKE_EACH_SUBJECT_RECOGNIZABLE),
        TourCopy(UiKey.TEXT_TAP_THE_ICON_BESIDE_THE_SUBJECT_NAME_PICK_ONE_FROM_THE_EXPANDE_66E977E7)),
    COURSE_TASKS("subject", "subject-tabs",
        TourCopy(UiKey.TEXT_YOUR_SUBJECT_CLEARLY_ORGANIZED),
        TourCopy(UiKey.TEXT_TASKS_CONTAINS_ASSIGNMENTS_TESTS_POINTS_AND_ADMISSION_MINIMUMS_C4AAB8D0), 1),
    COURSE_NOTES("subject", "subject-tabs",
        TourCopy(UiKey.TEXT_WHERE_YOUR_NOTES_LIVE),
        TourCopy(UiKey.TEXT_A_NOTE_ADDED_WITH_IS_SAVED_INSIDE_THE_SELECTED_SUBJECT_OPEN_TH_AD5268FB), 3),
    PROGRESS_PLAN("progress", "progress-tabs",
        TourCopy(UiKey.TEXT_PROGRESS_STARTS_WITH_YOUR_PLAN),
        TourCopy(UiKey.TEXT_PLAN_GROUPS_SUBJECTS_BY_SEMESTER_EXPAND_A_SEMESTER_OPEN_SUBJEC_6785C890)),
    PROGRESS_ACTIVE("progress", "progress-tabs",
        TourCopy(UiKey.TEXT_WHAT_YOU_ARE_WORKING_ON_NOW),
        TourCopy(UiKey.TEXT_ACTIVE_SHOWS_THE_SUBJECTS_YOU_ARE_TAKING_NOW_CHANGE_A_SUBJECT_8DF8ABA9), 1),
    PROGRESS_COMPLETED("progress", "progress-tabs",
        TourCopy(UiKey.TEXT_COMPLETED_SUBJECTS_AND_GRADES),
        TourCopy(UiKey.TEXT_COMPLETED_CONTAINS_FINISHED_SUBJECTS_THEIR_GRADES_AND_EARNED_C_940BC17A), 2),
    ADD_BUTTON("subjects", "add-button",
        TourCopy(UiKey.TEXT_ADD_SOMETHING_NEW),
        TourCopy(UiKey.TEXT_THIS_PLUS_IS_ALWAYS_NEARBY_ON_THE_MAIN_SCREENS_IT_OPENS_ALL_TH_6ABD1F18)),
    ADD("add", "add-actions",
        TourCopy(UiKey.TEXT_ONE_PLUS_FOR_EVERYTHING_NEW),
        TourCopy(UiKey.TEXT_THE_BUTTON_AT_THE_BOTTOM_OPENS_CLASSES_EVENTS_ASSIGNMENTS_TEST_F3C3CD8B)),
    DOCUMENTS("settings-documents", "settings-documents",
        TourCopy(UiKey.TEXT_A_FOLDER_TO_KEEP_YOUR_DATA_SAFE),
        TourCopy(UiKey.TEXT_SETTINGS_DATA_DOCUMENTS_TAKT_CHOOSE_DOCUMENTS_OR_DOCUMENTS_TAK_E23535EB)),
    FILTERS("settings-filters", "screen-header",
        TourCopy(UiKey.TEXT_CHOOSE_WHAT_APPEARS_IN_TODAY),
        TourCopy(UiKey.TEXT_SETTINGS_FOR_YOURSELF_TASKS_CHOOSE_A_DATE_RANGE_AND_WHETHER_TO_54A2F0D3)),
    REMINDERS("settings-reminders", "screen-header",
        TourCopy(UiKey.TEXT_REMINDERS_AT_YOUR_PACE),
        TourCopy(UiKey.TEXT_SETTINGS_FOR_YOURSELF_NOTIFICATIONS_CHOOSE_CLASSES_EVENTS_OR_S_BEE665F1)),
    APPEARANCE("settings-appearance", "screen-header",
        TourCopy(UiKey.TEXT_TAKT_IN_YOUR_COLORS),
        TourCopy(UiKey.TEXT_SETTINGS_FOR_YOURSELF_APPEARANCE_CHOOSE_LIGHT_DARK_OR_SYSTEM_M_71609186)),
    PERIODS("settings-periods", "screen-header",
        TourCopy(UiKey.TEXT_ONE_LAST_THING_STUDY_DATES),
        TourCopy(UiKey.TEXT_SETTINGS_FOR_YOURSELF_STUDY_PERIODS_CHECK_TEACHING_AND_EXAM_DA_DA720036));
}
