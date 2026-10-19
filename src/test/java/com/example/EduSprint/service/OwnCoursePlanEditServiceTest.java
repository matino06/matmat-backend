package com.example.EduSprint.service;

import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OwnCoursePlanEditServiceTest {

    private final CourseRepository courses = mock(CourseRepository.class);
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final PlanFlow flow = mock(PlanFlow.class);
    private final OwnCoursePlanEditService service = new OwnCoursePlanEditService(courses, jdbc, mock(BuildJobQueue.class), flow, mock(OwnCourseAnthropic.class));
    private final Account creator = account();

    private static Account account() {
        Account a = new Account("k@x.hr", "K", null);
        ReflectionTestUtils.setField(a, "accountId", 7L);
        a.setIsCreator(true);
        return a;
    }

    private void course(String status) {
        Course c = new Course("Fizika", null);
        c.setCourseId(5L);
        c.setOwnerAccountId(7L);
        c.setStatus(status);
        when(courses.findCourseByCourseId(5L)).thenReturn(Optional.of(c));
        when(jdbc.queryForList(contains("SELECT position FROM course_objective"), any(SqlParameterSource.class), eq(Integer.class)))
                .thenReturn(List.of(3));
    }

    private static HttpStatus status(Runnable r) {
        return HttpStatus.valueOf(assertThrows(ResponseStatusException.class, r::run).getStatusCode().value());
    }

    @Test
    void lessonNamesAreTrimmedAndLimited() {
        assertEquals("Kosina s trenjem", OwnCoursePlanEditService.validName("  Kosina   s trenjem "));
        assertThrows(ResponseStatusException.class, () -> OwnCoursePlanEditService.validName(" "));
        assertThrows(ResponseStatusException.class, () -> OwnCoursePlanEditService.validName("x".repeat(201)));
    }

    @Test
    void confirmedPlanIsLockedExceptForRenaming() {
        course("potvrden");
        assertEquals(HttpStatus.CONFLICT, status(() -> service.move(creator, 5, 11, 1)));
        assertEquals(HttpStatus.CONFLICT, status(() -> service.delete(creator, 5, 11)));
        assertEquals(HttpStatus.CONFLICT, status(() -> service.addPrerequisite(creator, 5, 11, 12)));
        assertEquals(HttpStatus.CONFLICT, status(() -> service.recheck(creator, 5, List.of(11L))));
        assertDoesNotThrow(() -> service.rename(creator, 5, 11, "Novi naziv"));
        verify(jdbc).update(contains("UPDATE learning_objective SET objective_name"), any(SqlParameterSource.class));
    }

    @Test
    void lessonCannotBeItsOwnPrerequisite() {
        course(PlanFinalizeStep.STATUS_DRAFT);
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> service.addPrerequisite(creator, 5, 11, 11)));
    }

    @Test
    void recheckCoversChangedLessonsAndTheirDependentsOnly() {
        course(PlanFinalizeStep.STATUS_DRAFT);
        when(jdbc.queryForList(contains("SELECT objective_id FROM course_objective"), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(List.of(11L, 12L, 13L, 14L));
        when(jdbc.queryForList(contains("WHERE prerequisite_id IN"), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(List.of(13L));
        service.recheck(creator, 5, List.of(11L, 99L)); // 99 nije u predmetu (npr. obrisana)
        verify(flow).recheck(eq(5L), eq(7L), anyLong(), eq(new TreeSet<>(Set.of(11L, 13L))));
    }

    @Test
    void currentOrderReportsLessonsBeforeTheirPrerequisite() {
        List<Set<Integer>> pre = List.of(Set.of(2), Set.of(0), Set.of());
        PlanRepair.Order order = PlanCheckRepairStep.currentOrder(pre);
        assertEquals(List.of(0, 1, 2), order.order());
        assertEquals(1, order.backwards().size());
        assertArrayEquals(new int[]{0, 2}, order.backwards().get(0));
    }

    private void instructionJob(String status, String output) {
        Map<String, Object> job = new HashMap<>();
        job.put("status", status);
        job.put("output", output);
        when(jdbc.queryForList(contains("FROM build_job WHERE job_id"), any(SqlParameterSource.class))).thenReturn(List.of(job));
        when(jdbc.queryForList(contains("SELECT objective_id FROM course_objective"), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(List.of(11L, 12L, 13L));
    }

    @Test
    void unfinishedOrAppliedProposalCannotBeAccepted() {
        course(PlanFinalizeStep.STATUS_DRAFT);
        instructionJob(BuildJobQueue.RUNNING, null);
        assertEquals(HttpStatus.CONFLICT, status(() -> service.accept(creator, 5, 70)));
        instructionJob(BuildJobQueue.DONE, "{\"poruka\":\"\",\"operacije\":[],\"primijenjeno\":true}");
        assertEquals(HttpStatus.CONFLICT, status(() -> service.accept(creator, 5, 70)));
    }

    @Test
    void acceptedProposalSkipsOperationsOnLessonsItAlreadyDeleted() {
        course(PlanFinalizeStep.STATUS_DRAFT);
        instructionJob(BuildJobQueue.DONE, """
                {"poruka":"","operacije":[{"op":"obrisi","id":12},{"op":"preimenuj","id":12,"naziv":"X"},{"op":"preimenuj","id":11,"naziv":"Kosina"}]}""");
        assertEquals(List.of(11L), service.accept(creator, 5, 70));
        verify(jdbc, times(1)).update(contains("UPDATE learning_objective SET objective_name"), any(SqlParameterSource.class));
        verify(jdbc).update(contains("UPDATE build_job SET output"), any(SqlParameterSource.class));
    }

    private void conceptLessons(int n) {
        when(jdbc.queryForObject(contains("SELECT count(*)"), any(SqlParameterSource.class), eq(Integer.class))).thenReturn(n);
    }

    @Test
    void quickPrepIsRefusedAboveFiftyConceptLessons() {
        course(PlanFinalizeStep.STATUS_DRAFT);
        conceptLessons(51);
        java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> service.chooseLearningMode(creator, 5, OwnCoursePlanEditService.MODE_QUICK, tomorrow)));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> service.chooseLearningMode(creator, 5, OwnCoursePlanEditService.MODE_QUICK, null)));
        assertDoesNotThrow(() -> service.chooseLearningMode(creator, 5, OwnCoursePlanEditService.MODE_FULL, null));
    }

    @Test
    void quickPrepQueuesApplicationLessonsAndModeIsChosenOnce() {
        BuildJobQueue queue = mock(BuildJobQueue.class);
        OwnCoursePlanEditService s = new OwnCoursePlanEditService(courses, jdbc, queue, flow, mock(OwnCourseAnthropic.class));
        course(PlanFinalizeStep.STATUS_DRAFT);
        conceptLessons(26);
        s.chooseLearningMode(creator, 5, OwnCoursePlanEditService.MODE_QUICK, java.time.LocalDate.now().plusDays(2));
        verify(queue).enqueue(eq(5L), eq(7L), eq(PlanApplicationStep.KIND), anyString(), anyInt(), eq(true), isNull());
        assertEquals(HttpStatus.CONFLICT, status(() -> s.chooseLearningMode(creator, 5, OwnCoursePlanEditService.MODE_FULL, null)));
    }

    private BuildJobQueue confirmable(int lessons, int serious) {
        Course c = new Course("Fizika", null);
        c.setCourseId(5L);
        c.setOwnerAccountId(7L);
        c.setStatus(PlanFinalizeStep.STATUS_DRAFT);
        c.setLearningMode(OwnCoursePlanEditService.MODE_FULL);
        when(courses.findCourseByCourseId(5L)).thenReturn(Optional.of(c));
        when(jdbc.queryForObject(contains("serious AND NOT ignored"), any(SqlParameterSource.class), eq(Integer.class))).thenReturn(serious);
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= lessons; i++) ids.add(100 + i);
        when(jdbc.queryForList(contains("SELECT objective_id FROM course_objective"), any(SqlParameterSource.class), eq(Long.class))).thenReturn(ids);
        return mock(BuildJobQueue.class);
    }

    @Test
    void seriousWarningsMustBeAcceptedBeforeConfirming() {
        BuildJobQueue queue = confirmable(10, 2);
        OwnCoursePlanEditService s = new OwnCoursePlanEditService(courses, jdbc, queue, flow, mock(OwnCourseAnthropic.class));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> s.confirm(creator, 5, false)));
        verify(queue, never()).enqueue(anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyBoolean(), any());
        verify(courses, never()).save(any());
    }

    @Test
    void smallPlanQueuesFirstVariationForEveryLessonFirstFiveWaitedOn() {
        BuildJobQueue queue = confirmable(12, 0);
        new OwnCoursePlanEditService(courses, jdbc, queue, flow, mock(OwnCourseAnthropic.class)).confirm(creator, 5, false);
        verify(queue, times(5)).enqueue(eq(5L), eq(7L), eq(TaskVariationStep.KIND), anyString(), eq(BuildJobQueue.PRIORITY_WAITED_ON), eq(false), anyString());
        verify(queue, times(7)).enqueue(eq(5L), eq(7L), eq(TaskVariationStep.KIND), anyString(), eq(BuildJobQueue.PRIORITY_BACKGROUND), eq(false), anyString());
        verify(queue, never()).enqueue(anyLong(), anyLong(), eq(VariationBatchStep.KIND), anyString(), anyInt(), anyBoolean(), any());
        verify(courses).save(argThat(c -> TaskVariationStep.STATUS_CONFIRMED.equals(c.getStatus())));
    }

    @Test
    void bigPlanQueuesFirstFiveAndSendsTheRestAsBatch() {
        BuildJobQueue queue = confirmable(60, 0);
        new OwnCoursePlanEditService(courses, jdbc, queue, flow, mock(OwnCourseAnthropic.class)).confirm(creator, 5, false);
        verify(queue, times(5)).enqueue(eq(5L), eq(7L), eq(TaskVariationStep.KIND), anyString(), anyInt(), eq(false), anyString());
        verify(queue).enqueue(eq(5L), eq(7L), eq(VariationBatchStep.KIND), eq("v1"), anyInt(), eq(false),
                argThat(in -> in.startsWith("{\"objectiveIds\":[106,") && in.endsWith("160]}")));
    }

    @Test
    void bigPlanSkipsTheBatchWhileAnthropicCreditLasts() {
        BuildJobQueue queue = confirmable(60, 0);
        OwnCourseAnthropic anthropic = mock(OwnCourseAnthropic.class);
        when(anthropic.available()).thenReturn(true);
        new OwnCoursePlanEditService(courses, jdbc, queue, flow, anthropic).confirm(creator, 5, false);
        verify(queue, times(60)).enqueue(eq(5L), eq(7L), eq(TaskVariationStep.KIND), anyString(), anyInt(), eq(false), anyString());
        verify(queue, never()).enqueue(anyLong(), anyLong(), eq(VariationBatchStep.KIND), anyString(), anyInt(), anyBoolean(), any());
    }

    @Test
    void confirmingGivesTheCreatorReviewRowsAndDropsPrerequisitesThatComeLaterInThePlan() {
        BuildJobQueue queue = confirmable(8, 0);
        new OwnCoursePlanEditService(courses, jdbc, queue, flow, mock(OwnCourseAnthropic.class)).confirm(creator, 5, false);
        verify(jdbc).update(contains("DELETE FROM objective_prerequisite op USING course_objective"), any(SqlParameterSource.class));
        verify(jdbc).update(contains("INSERT INTO account_objective"), argThat((SqlParameterSource p) -> Long.valueOf(7L).equals(p.getValue("a"))));
    }

    private static void row(org.springframework.jdbc.core.RowCallbackHandler h, long a, long b) throws java.sql.SQLException {
        java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
        when(rs.getLong(1)).thenReturn(a);
        when(rs.getLong(2)).thenReturn(b);
        h.processRow(rs);
    }

    @Test
    void acceptedItemMoveAndPrerequisiteChangeTheirLessons() {
        course(PlanFinalizeStep.STATUS_DRAFT);
        instructionJob(BuildJobQueue.DONE, """
                {"poruka":"","operacije":[{"op":"premjesti_stavku","stavka":200,"u":11},{"op":"dodaj_preduvjet","id":13,"preduvjet":11}]}""");
        doAnswer(inv -> { row(inv.getArgument(2), 200L, 12L); return null; })
                .when(jdbc).query(contains("SELECT k.item_id, k.objective_id"), any(SqlParameterSource.class), any(org.springframework.jdbc.core.RowCallbackHandler.class));
        List<Long> changed = service.accept(creator, 5, 70);
        assertTrue(changed.containsAll(List.of(11L, 12L, 13L)), changed.toString());
        verify(jdbc).update(contains("UPDATE knowledge_item SET objective_id"), argThat((SqlParameterSource p) -> Long.valueOf(11L).equals(p.getValue("o"))));
        verify(jdbc).update(contains("INSERT INTO objective_prerequisite"), argThat((SqlParameterSource p) -> Long.valueOf(11L).equals(p.getValue("p"))));
    }

    private void warning(String kind, Long lesson, Long other) {
        Map<String, Object> w = new HashMap<>();
        w.put("kind", kind);
        w.put("objective_id", lesson);
        w.put("prerequisite_id", other);
        when(jdbc.queryForList(contains("FROM plan_warning WHERE warning_id"), any(SqlParameterSource.class))).thenReturn(List.of(w));
    }

    @Test
    void fixingAZeroAssumptionWarningChecksTheLessonAgainAndRepairsIt() {
        course(PlanFinalizeStep.STATUS_DRAFT);
        warning("nulta", 11L, 12L);
        service.fixWarning(creator, 5, 37);
        verify(flow).fix(eq(5L), eq(7L), anyLong(), eq(List.of(11L)), eq(true));
    }

    @Test
    void fixingAnOrderWarningInACycleDropsTheEdgeFirst() throws Exception {
        course(PlanFinalizeStep.STATUS_DRAFT);
        warning("redoslijed", 11L, 12L);
        // 12 ovisi o 11, a 11 o 12: krug
        doAnswer(inv -> { row(inv.getArgument(2), 12L, 11L); row(inv.getArgument(2), 11L, 12L); return null; })
                .when(jdbc).query(contains("SELECT op.objective_id, op.prerequisite_id"), any(SqlParameterSource.class), any(org.springframework.jdbc.core.RowCallbackHandler.class));
        service.fixWarning(creator, 5, 40);
        verify(jdbc).update(contains("DELETE FROM objective_prerequisite WHERE objective_id = :o AND prerequisite_id = :p"),
                argThat((SqlParameterSource p) -> Long.valueOf(11L).equals(p.getValue("o")) && Long.valueOf(12L).equals(p.getValue("p"))));
        verify(flow).fix(eq(5L), eq(7L), anyLong(), eq(List.of(11L)), eq(true));
    }

    @Test
    void holesAndExamWarningsAreNotFixedByCode() {
        course(PlanFinalizeStep.STATUS_DRAFT);
        warning("rupa", 11L, null);
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> service.fixWarning(creator, 5, 35)));
        verifyNoInteractions(flow);
    }
}
