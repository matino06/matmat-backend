package com.example.EduSprint.service;

import com.example.EduSprint.dto.OwnCourseCreateDTO;
import com.example.EduSprint.dto.OwnCourseDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.entity.Course;
import com.example.EduSprint.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OwnCourseServiceTest {

    private final CourseRepository courses = mock(CourseRepository.class);
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final OwnCourseService service = new OwnCourseService(courses, jdbc,
            mock(BuildQuotaService.class), mock(BuildJobQueue.class));

    private static Account account(long id, boolean creator) {
        Account a = new Account("a" + id + "@x.hr", "A", null);
        ReflectionTestUtils.setField(a, "accountId", id);
        a.setIsCreator(creator);
        return a;
    }

    private static Course owned(long id, long owner, String name) {
        Course c = new Course(name, null);
        c.setCourseId(id);
        c.setOwnerAccountId(owner);
        return c;
    }

    private static HttpStatus status(Executable e) {
        return HttpStatus.valueOf(assertThrows(ResponseStatusException.class, e::run).getStatusCode().value());
    }

    interface Executable {
        void run();
    }

    @Test
    void validInputIsNormalised() {
        OwnCourseCreateDTO valid = OwnCourseService.validate(new OwnCourseCreateDTO("  Kemija   test ", " Kemija ", "SS"));
        assertEquals("Kemija test", valid.name());
        assertEquals("kemija", valid.subject());
        assertEquals("ss", valid.level());
    }

    @Test
    void invalidInputIsRejected() {
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> OwnCourseService.validate(null)));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> OwnCourseService.validate(new OwnCourseCreateDTO(" ", "fizika", "ss"))));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> OwnCourseService.validate(new OwnCourseCreateDTO("x".repeat(101), "fizika", "ss"))));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> OwnCourseService.validate(new OwnCourseCreateDTO("Fizika", "povijest", "ss"))));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> OwnCourseService.validate(new OwnCourseCreateDTO("Fizika", "fizika", "fakultet"))));
    }

    @Test
    void onlyCreatorsMayUseOwnCourses() {
        Account regular = account(1, false);
        assertEquals(HttpStatus.FORBIDDEN, status(() -> service.list(regular)));
        assertEquals(HttpStatus.FORBIDDEN, status(() -> service.create(regular, new OwnCourseCreateDTO("Fizika", "fizika", "ss"))));
        assertEquals(HttpStatus.FORBIDDEN, status(() -> service.delete(regular, 5L, "Fizika")));
        verifyNoInteractions(jdbc);
    }

    @Test
    void createdCourseBelongsToCreatorAndStartsAsNew() {
        when(courses.existsByOwnerAccountIdAndCourseNameIgnoreCase(7L, "Fizika")).thenReturn(false);
        when(courses.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));

        OwnCourseDTO dto = service.create(account(7, true), new OwnCourseCreateDTO("Fizika", "fizika", "ss"));

        assertEquals("Fizika", dto.courseName());
        assertEquals("fizika", dto.subject());
        assertEquals("ss", dto.level());
        assertEquals(OwnCourseService.STATUS_NEW, dto.status());
        verify(courses).save(argThat(c -> Long.valueOf(7).equals(c.getOwnerAccountId())));
    }

    @Test
    void sameNameTwiceForOneCreatorIsAConflict() {
        when(courses.existsByOwnerAccountIdAndCourseNameIgnoreCase(7L, "Fizika")).thenReturn(true);
        assertEquals(HttpStatus.CONFLICT, status(() -> service.create(account(7, true), new OwnCourseCreateDTO("Fizika", "fizika", "ss"))));
        verify(courses, never()).save(any());
    }

    @Test
    void someoneElsesCourseIsNotFound() {
        when(courses.findCourseByCourseId(5L)).thenReturn(Optional.of(owned(5, 99, "Fizika")));
        assertEquals(HttpStatus.NOT_FOUND, status(() -> service.delete(account(7, true), 5L, "Fizika")));
        verifyNoInteractions(jdbc);
    }

    @Test
    void deleteNeedsTheExactName() {
        when(courses.findCourseByCourseId(5L)).thenReturn(Optional.of(owned(5, 7, "Fizika")));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> service.delete(account(7, true), 5L, "fizika")));
        assertEquals(HttpStatus.BAD_REQUEST, status(() -> service.delete(account(7, true), 5L, null)));
        verifyNoInteractions(jdbc);
    }

    @Test
    void deleteRemovesTheCourseRow() {
        when(courses.findCourseByCourseId(5L)).thenReturn(Optional.of(owned(5, 7, "Fizika")));
        when(courses.findFirstByOwnerAccountIdIsNullOrderByCourseIdAsc()).thenReturn(Optional.empty());

        service.delete(account(7, true), 5L, " Fizika ");

        verify(jdbc).update(eq("DELETE FROM course WHERE course_id = :c"), any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class));
    }

    @Test
    void confirmIgnoresSurroundingSpaces() {
        assertTrue(OwnCourseService.confirmMatches("Fizika", " Fizika "));
        assertFalse(OwnCourseService.confirmMatches("Fizika", "Fizik"));
        verify(jdbc, never()).update(anyString(), any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class));
    }
}
