package com.hongseob.openclass_ap.enrollment;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.worker.EnrollmentQueueWorker;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 접수 순서 보장 — AC-ENR-006(핵심 시나리오) + research.md §7 V1/V4/V8 (M1).
 *
 * <p>AC-ENR-007(잠금 제거 대조군)은 {@code app.enrollment.lock-enabled=false}
 * 프로퍼티 오버라이드가 필요해 별도 Spring 컨텍스트({@link
 * EnrollmentLockDisabledControlGroupIntegrationTest})로 분리한다.</p>
 *
 * <p>V2("잠금 없이 커밋 순서를 뒤집으면 순서 보장이 깨진다")·V3("잠금이 있으면
 * 순서가 지켜진다")는 각각 AC-ENR-007·AC-ENR-006과 동일한 명제이므로 별도
 * 테스트를 두지 않는다 — progress.md §E.2에 그 대응을 기록한다.</p>
 */
@SpringBootTest
class EnrollmentOrderGuaranteeIntegrationTest extends AbstractIntegrationTest {

    // 프로덕션 EnrollmentReceiptService와 동일한 잠금 네임스페이스(classid)를
    // 재사용해 V1/V4/V8이 실제 운영 경로와 동일한 잠금 메커니즘을 검증하게 한다.
    private static final int LOCK_CLASSID = 887711;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EnrollmentReceiptService receiptService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EnrollmentQueueWorker worker;

    @Autowired
    private EnrollmentRequestRepository requestRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void cleanUp() {
        requestRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
    }

    private Long createCourse(int capacity) {
        Course course = courseRepository.save(Course.create("동시성강좌", "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30)));
        return course.getId();
    }

    // ---- AC-ENR-006 -------------------------------------------------------

    // AC-ENR-006 — 이 SPEC의 단일 관문 ①. 커밋을 지연시킨 X가 여전히 확정되고,
    // 뒤늦게 도착했지만 먼저 커밋을 시도한 Y는 접수 잠금에서 대기하다가 X보다
    // 큰 순서값을 받아 대기명단으로 밀린다.
    @Test
    @Timeout(30)
    void AC_ENR_006_커밋_지연_하에서도_먼저_접수한_회원이_확정된다() throws Exception {
        Long courseId = createCourse(1);
        long memberXId = 501L;
        long memberYId = 502L;

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch xInsertedLatch = new CountDownLatch(1);
            CountDownLatch releaseXCommit = new CountDownLatch(1);

            // X: 접수 트랜잭션을 먼저 시작하고 커밋을 지연시킨다. try/finally로
            // 예외 경로에서도 반드시 커밋 또는 롤백되도록 한다 — 그러지 않으면
            // 이 트랜잭션이 점유한 JDBC 커넥션이 HikariCP 풀에 영원히 반환되지
            // 않아(누수) 이후 실행되는 다른 테스트 클래스까지 연쇄적으로 실패
            //시킨다(공유 Spring 컨텍스트 = 공유 커넥션 풀).
            Future<Long> xFuture = executor.submit(() -> {
                TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                boolean committed = false;
                try {
                    Long requestId = receiptService.receiveEnrollment(memberXId, courseId);
                    xInsertedLatch.countDown();
                    releaseXCommit.await(10, TimeUnit.SECONDS);
                    transactionManager.commit(status);
                    committed = true;
                    return requestId;
                } finally {
                    if (!committed) {
                        transactionManager.rollback(status);
                    }
                }
            });

            xInsertedLatch.await(5, TimeUnit.SECONDS);

            // Y: X가 아직 커밋되지 않았으므로 접수 잠금에서 대기한다.
            Future<Long> yFuture = executor.submit(() -> {
                TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                boolean committed = false;
                try {
                    Long requestId = receiptService.receiveEnrollment(memberYId, courseId);
                    transactionManager.commit(status);
                    committed = true;
                    return requestId;
                } finally {
                    if (!committed) {
                        transactionManager.rollback(status);
                    }
                }
            });

            // Y가 실제로 잠금 대기 상태에 진입할 시간을 확보한다(최선 노력).
            Thread.sleep(500);
            releaseXCommit.countDown();

            long xId = xFuture.get(10, TimeUnit.SECONDS);
            long yId = yFuture.get(10, TimeUnit.SECONDS);

            assertThat(yId).as("Y는 X가 커밋할 때까지 대기했으므로 순서값이 더 커야 한다").isGreaterThan(xId);

            worker.drainQueue();

            assertThat(requestRepository.findById(xId).orElseThrow().getResult())
                    .as("먼저 접수한 X가 확정되어야 한다")
                    .isEqualTo(RequestResult.SUCCESS);
            assertThat(requestRepository.findById(yId).orElseThrow().getResult())
                    .as("나중에 접수한 Y는 대기명단으로 밀려야 한다")
                    .isEqualTo(RequestResult.WAITLISTED);
            assertThat(enrollmentRepository.findAll())
                    .extracting(Enrollment::getMemberId)
                    .containsExactly(memberXId);
        } finally {
            executor.shutdownNow();
        }
    }

    // ---- research.md §7 V1 -------------------------------------------------

    // V1 — pg_advisory_xact_lock은 커밋·롤백 양쪽에서 자동 해제된다.
    @Test
    @Timeout(15)
    void V1_권고_잠금은_커밋과_롤백_양쪽에서_자동_해제된다() throws Exception {
        long lockKey = 910001L;

        // 커밋 경로
        try (Connection c1 = dataSource.getConnection()) {
            c1.setAutoCommit(false);
            acquireLock(c1, lockKey);
            c1.commit();
        }
        try (Connection c2 = dataSource.getConnection()) {
            c2.setAutoCommit(false);
            long start = System.currentTimeMillis();
            acquireLock(c2, lockKey);
            long elapsed = System.currentTimeMillis() - start;
            assertThat(elapsed).as("이전 잠금이 커밋으로 해제되었다면 즉시 획득되어야 한다").isLessThan(3000);
            c2.rollback();
        }

        // 롤백 경로
        try (Connection c3 = dataSource.getConnection()) {
            c3.setAutoCommit(false);
            acquireLock(c3, lockKey);
            c3.rollback();
        }
        try (Connection c4 = dataSource.getConnection()) {
            c4.setAutoCommit(false);
            long start = System.currentTimeMillis();
            acquireLock(c4, lockKey);
            long elapsed = System.currentTimeMillis() - start;
            assertThat(elapsed).as("이전 잠금이 롤백으로도 해제되었다면 즉시 획득되어야 한다").isLessThan(3000);
            c4.commit();
        }
    }

    // ---- research.md §7 V4 -------------------------------------------------

    // V4 — 미커밋 INSERT 행은 다른 트랜잭션의 SELECT ... FOR UPDATE SKIP LOCKED에
    // 나타나지 않는다("잠긴 것"이 아니라 "안 보이는 것"이라는 §2/§3의 구별을
    // 직접 관찰한다).
    @Test
    @Timeout(15)
    void V4_미커밋_INSERT_행은_다른_트랜잭션의_SKIP_LOCKED_조회에_나타나지_않는다() throws Exception {
        long courseId = 920001L;
        long memberId = 920002L;
        long insertedId;

        try (Connection uncommitted = dataSource.getConnection()) {
            uncommitted.setAutoCommit(false);
            insertedId = insertEnrollRequestRaw(uncommitted, courseId, memberId);

            try (Connection reader = dataSource.getConnection()) {
                reader.setAutoCommit(false);
                assertThat(selectPendingIds(reader))
                        .as("커밋되지 않은 행은 SKIP LOCKED 조회 결과에 나타나지 않아야 한다")
                        .doesNotContain(insertedId);
                reader.commit();
            }

            uncommitted.commit();
        }

        try (Connection reader2 = dataSource.getConnection()) {
            reader2.setAutoCommit(false);
            assertThat(selectPendingIds(reader2))
                    .as("커밋 후에는 같은 행이 보여야 한다")
                    .contains(insertedId);
            reader2.commit();
        }
    }

    // ---- research.md §7 V8 -------------------------------------------------

    // V8 — 커밋 중 행 가시화가 권고 잠금 해제보다 먼저 일어난다. T1이 잠금을
    // 잡고 INSERT 후 커밋하면, 잠금 대기 중이던 T2는 잠금을 획득한 "직후"
    // 반드시 T1의 행을 볼 수 있어야 한다. 10회 반복하여 역전이 관측되지
    // 않음을 확인한다(V1과 동일한 2-커넥션 하네스 재사용).
    @Test
    @Timeout(60)
    void V8_커밋_중_행_가시화가_잠금_해제보다_먼저_일어난다_10회_반복() throws Exception {
        long courseId = 930001L;

        for (int i = 0; i < 10; i++) {
            long memberId = 940000L + i;
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                CountDownLatch t1Locked = new CountDownLatch(1);
                CountDownLatch releaseT1 = new CountDownLatch(1);
                AtomicLong insertedId = new AtomicLong();

                Future<Void> t1 = executor.submit(() -> {
                    try (Connection c1 = dataSource.getConnection()) {
                        c1.setAutoCommit(false);
                        acquireLock(c1, courseId);
                        insertedId.set(insertEnrollRequestRaw(c1, courseId, memberId));
                        t1Locked.countDown();
                        releaseT1.await(10, TimeUnit.SECONDS);
                        c1.commit();
                    }
                    return null;
                });

                t1Locked.await(5, TimeUnit.SECONDS);

                Future<Boolean> t2 = executor.submit(() -> {
                    try (Connection c2 = dataSource.getConnection()) {
                        c2.setAutoCommit(false);
                        acquireLock(c2, courseId); // T1이 커밋할 때까지 여기서 블록
                        boolean visible = isRowVisible(c2, insertedId.get());
                        c2.commit();
                        return visible;
                    }
                });

                Thread.sleep(300);
                releaseT1.countDown();

                t1.get(10, TimeUnit.SECONDS);
                boolean visible = t2.get(10, TimeUnit.SECONDS);

                assertThat(visible)
                        .as("반복 %d회차: T2가 잠금을 획득한 직후 T1의 행이 항상 보여야 한다", i + 1)
                        .isTrue();
            } finally {
                executor.shutdownNow();
            }
        }
    }

    // ---- 원시 JDBC 헬퍼 -----------------------------------------------------

    private void acquireLock(Connection connection, long objectId) throws Exception {
        // 2-인자 형태는 두 인자 모두 32비트 int다(PostgreSQL 문서) — bigint를
        // 그대로 바인딩하면 "함수가 존재하지 않음"으로 거부된다
        // (EnrollmentReceiptService와 동일한 이유).
        try (PreparedStatement ps = connection.prepareStatement("SELECT pg_advisory_xact_lock(?, ?)")) {
            ps.setInt(1, LOCK_CLASSID);
            ps.setInt(2, (int) objectId);
            ps.execute();
        }
    }

    private long insertEnrollRequestRaw(Connection connection, long courseId, long memberId) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO enrollment_request (member_id, course_id, request_type, state, requested_at) "
                        + "VALUES (?, ?, 'ENROLL', 'PENDING', ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, memberId);
            ps.setLong(2, courseId);
            ps.setObject(3, LocalDateTime.now());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                // pgjdbc는 RETURN_GENERATED_KEYS에 특정 컬럼을 지정하지 않으면
                // "RETURNING *"(전체 컬럼)를 반환하고, 그 열 순서는 테이블의
                // 물리 컬럼 순서를 따른다 — 반드시 컬럼 "id"이 첫 번째라는
                // 보장이 없다(실측: course_id가 먼저 나타남). 위치(1)가 아닌
                // 컬럼명으로 조회해야 한다.
                return keys.getLong("id");
            }
        }
    }

    private List<Long> selectPendingIds(Connection connection) throws Exception {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM enrollment_request WHERE state = 'PENDING' FOR UPDATE SKIP LOCKED");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getLong("id"));
            }
        }
        return ids;
    }

    private boolean isRowVisible(Connection connection, long requestId) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM enrollment_request WHERE id = ?")) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
