package io.hhplus.tdd.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class PointServiceTest {
    private PointHistoryTable pointHistoryTable;
    private UserPointTable userPointTable;
    private PointService pointService;
    private final long time = System.currentTimeMillis();

    @BeforeEach
    void setUp() {
        pointHistoryTable = new PointHistoryTable();
        userPointTable = new UserPointTable();
        pointService = new PointService(pointHistoryTable, userPointTable);
    }

    @Test
    void 포인트조회실패() {
        UserPoint user1 = new UserPoint(1,10L,time);
        // given
        long userId = 2L;

        // when
        long restPoints = pointService.searchRestPoints(userId);

        // then
        assertThat(restPoints).isEqualTo(0L); // 사용자 정보가 없으면 기본값 0
    }

    @Test
    void 포인트조회성공() {
        // given
        UserPoint user1 = new UserPoint(1,5000L,time);
        userPointTable.insertOrUpdate(user1.id(),user1.point());
        // when
        long restPoints = pointService.searchRestPoints(user1.id());

        // then
        assertThat(restPoints).isEqualTo(5000L);
    }

    @Test
    void 충천사용테스트_0원() {
        UserPoint user1 = new UserPoint(1,10L,time);
        userPointTable.insertOrUpdate(user1.id(),user1.point());



    }

    @Test
    void 포인트사용실패_잔액부족() {
        UserPoint user1 = new UserPoint(1,10L,time);
        userPointTable.insertOrUpdate(user1.id(),user1.point());

        long use_amount = 12L;

        assertThatThrownBy(() -> pointService.usePoints(user1.id(), use_amount, System.currentTimeMillis()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("포인트가 부족합니다.");
    }

    @Test
    void 포인트사용성공() {
        UserPoint user1 = new UserPoint(1,10L,time);
        userPointTable.insertOrUpdate(user1.id(),user1.point());

        long use_amount = 9L;
        long expected_result = user1.point() - use_amount;
        long restPoints = pointService.usePoints(user1.id(),use_amount,time);

        assertThat(expected_result).isEqualTo(restPoints);

    }

    @Test
    void 포인트충전실패_검증() {
        UserPoint user1 = new UserPoint(1,9000L,time);
        userPointTable.insertOrUpdate(user1.id(), user1.point());
        long amount = 1001L;

        // when
        boolean isChargeable = pointService.isChargeable(user1.id(), amount);

        // then
        assertThat(isChargeable).isFalse();
    }

    @Test
    void 포인트충전성공_검증() {
        UserPoint user1 = new UserPoint(1,10L,time);
        userPointTable.insertOrUpdate(user1.id(), user1.point());
        long amount = 1000L;

        // when
        boolean isChargeable = pointService.isChargeable(user1.id(), amount);

        // then
        assertThat(isChargeable).isTrue();
    }

    @Test
    void 포인트충전성공() {
        UserPoint user1 = new UserPoint(1,10L,time);
        userPointTable.insertOrUpdate(user1.id(), user1.point());;

        long amount = 1000L;
        long expected = user1.point() + 1000L;
        long result = pointService.chargePoints(user1.id(),amount,time);

        //assertThat(expected).isEqualTo(result);
        assertThat(expected).isEqualTo(pointService.searchRestPoints(user1.id()));
    }

    @Test
    void 포인트히스토리조회_성공_충전후() {
        // given
        UserPoint user1 = new UserPoint(1,0L,time);
        userPointTable.insertOrUpdate(user1.id(), user1.point());

        long amount1 = 1000L;
        // when
        pointService.chargePoints(user1.id(),amount1,time);
        PointHistory[] histories = pointService.searchPointhistory(user1.id());

        // then
        assertThat(histories).hasSize(1);
        assertThat(histories[0].amount()).isEqualTo(1000L);
    }

    @Test
    void 포인트히스토리조회_성공_사용후() {
        // given
        UserPoint user1 = new UserPoint(1,1000L,time);
        userPointTable.insertOrUpdate(user1.id(), user1.point());

        long amount1 = 500L;
        // when
        pointService.usePoints(user1.id(),amount1,time);
        PointHistory[] histories = pointService.searchPointhistory(user1.id());

        // then
        assertThat(histories).hasSize(1);
        assertThat(histories[0].amount()).isEqualTo(-500L);
    }

    /**
     * n개의 동시성과 100% 커버되었을 시 expectedpoint를 비교
     * 테스트 시나리오 : 20개의 동시 동시 요칭이 들어왔을때 동시성 테스트
     * 10번 시행시 7번 성공 3번 실패
     * @throws InterruptedException
     */
    @Test
    void 동시성_포인트_사용테스트() throws InterruptedException {
        // given
        long userId = 1L;
        userPointTable.insertOrUpdate(userId, 10000L);

        int threadCount = 20; // 동시에 실행할 스레드 수
        long amount = 500L;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> pointService.usePoints(userId, amount, System.currentTimeMillis()));
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then
        long expectedPoints = 10000L - (threadCount * amount);
        assertThat(userPointTable.selectById(userId).point()).isEqualTo(expectedPoints);

        // 포인트 히스토리 검증
        assertThat(pointHistoryTable.selectAllByUserId(userId)).hasSize(threadCount);
    }

    /**
     * 테스트 시나리오 : 10개의 동시 요청이 순서대로 들어오고 이때 순서대로 요청이 처리되면 성공된 thread의 index가 순서대로 쌓임을 확인
     * 결과 : 3부터 터짐
     */
    @Test
    void 동시성_포인트_충전테스트() throws InterruptedException {
        long userId = 1L;
        userPointTable.insertOrUpdate(userId, 0); // 초기 포인트 설정
        int threadCount = 3; // 동시에 실행할 스레드 수

        List<Integer> processedOrder = Collections.synchronizedList(new ArrayList<>()); // 처리된 순서 기록

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        long amount = 100L;

        CountDownLatch latch = new CountDownLatch(threadCount); // 모든 스레드 동시 시작을 위한 래치
        AtomicInteger successCount = new AtomicInteger(0); // 성공 카운트

        // when
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    latch.countDown(); // 모든 스레드 준비 완료
                    latch.await(); // 동시 시작

                    pointService.chargePoints(userId, amount, System.currentTimeMillis());
                    processedOrder.add(index); // 처리된 순서를 기록
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then
        assertEquals(threadCount, successCount.get(), "모든 스레드가 성공적으로 실행되지 않았습니다.");
        for (int i = 0; i < threadCount; i++) {
            assertEquals(i, processedOrder.get(i), "처리된 순서가 올바르지 않습니다.");
        }
    }
}
