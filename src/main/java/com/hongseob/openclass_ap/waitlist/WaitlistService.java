package com.hongseob.openclass_ap.waitlist;

import com.hongseob.openclass_ap.common.exception.WaitlistEntryNotFoundException;
import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.waitlist.dto.WaitlistListItemResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대기명단 취소 — 대기자 본인의 대기 신청을 취소한다(M4, REQ-WL-007/008,
 * design.md §8). 큐를 경유하지 않고 즉시 처리한다 — {@code enrolled_count}를
 * 건드리지 않으므로 정원 카운터 변경 경로 단일화(REQ-WRK-002)의 대상이
 * 아니다(design.md §8 "대기 취소가 큐를 경유하지 않는 이유").
 *
 * <p>순번 부여·조회는 {@link WaitlistEntryRepository}가 직접 담당한다
 * (design.md §7 — "순번 관리와 조회만 담당. {@code Enrollment}를 생성하지
 * 않는다"). 이 클래스는 그 원칙 위에 취소 유스케이스만 추가한다 — 확정
 * 생성 권한은 여전히 없다.</p>
 */
@Service
public class WaitlistService {

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final CourseRepository courseRepository;

    public WaitlistService(WaitlistEntryRepository waitlistEntryRepository, CourseRepository courseRepository) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * 대기명단 항목을 취소한다(REQ-WL-007). 소유자가 아니거나 존재하지
     * 않거나 이미 종단 상태(승격·취소·중복 종결)인 항목이면 {@link
     * WaitlistEntryNotFoundException}을 던진다 — "찾을 수 없음"과 "권한
     * 없음"을 구분하지 않는다(REQ-WL-008, AC-ENR-034 — {@code
     * EnrollmentRequestNotFoundException}과 동일한 IDOR 방지 원칙).
     *
     * <p>뒤 순번 대기자들의 상대 순서는 건드리지 않는다 — {@code position}
     * 값을 재계산하지 않고 이 항목만 {@code CANCELLED}로 전이한다
     * (AC-ENR-033).</p>
     */
    @Transactional
    public void cancel(Long entryId, Long memberId) {
        WaitlistEntry entry = waitlistEntryRepository.findById(entryId)
                .filter(found -> found.getMemberId().equals(memberId))
                .filter(found -> found.getStatus() == WaitlistStatus.WAITING)
                .orElseThrow(() -> new WaitlistEntryNotFoundException(entryId));
        entry.cancel();
    }

    /**
     * 내 대기명단 항목 목록 조회(M7, v0.3.0 개정 — REQ-LST-002, spec.md §A.6).
     * 요청자 본인의 활성 대기({@link WaitlistStatus#WAITING}) 항목만 {@code
     * position} 오름차순으로 반환한다. 부작용 없는 읽기 전용 조회다
     * (REQ-LST-004, AC-ENR-058) — 순번 재계산·승격·취소 등 어떤 도메인
     * 변경도 이 메서드가 수행하지 않는다.
     *
     * <p>{@code courseTitle}은 {@link com.hongseob.openclass_ap.enrollment.query.EnrollmentListQueryService}와
     * 동일한 방식으로 {@link CourseRepository} 배치 조회로 합친다 — {@link
     * WaitlistEntry}에 JPA 연관을 추가하지 않는다(plan.md §C.8 결정 3).</p>
     */
    @Transactional(readOnly = true)
    public List<WaitlistListItemResponse> listMine(Long memberId) {
        List<WaitlistEntry> entries =
                waitlistEntryRepository.findByMemberIdAndStatusOrderByPositionAsc(memberId, WaitlistStatus.WAITING);

        Map<Long, String> courseTitleById = courseTitleById(entries);

        return entries.stream()
                .map(entry -> new WaitlistListItemResponse(
                        entry.getId(),
                        entry.getCourseId(),
                        courseTitleById.get(entry.getCourseId()),
                        entry.getPosition(),
                        entry.getStatus().name()))
                .toList();
    }

    private Map<Long, String> courseTitleById(List<WaitlistEntry> entries) {
        List<Long> courseIds = entries.stream().map(WaitlistEntry::getCourseId).distinct().toList();
        return courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle, (left, right) -> left));
    }
}
