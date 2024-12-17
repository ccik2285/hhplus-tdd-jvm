package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

@Service
public class PointService {

    private PointHistoryTable pointHistoryTable;
    private UserPointTable userPointTable;
    public long MAX_POINT = 10000L;

    /**
     * 포인트 조회 서비스
     * @param id
     * @return 현재 보유 금액
     */
    public long searchRestPoints(long id) {
        return userPointTable.selectById(id).point();
    }

    /**
     * 포인트 이용내역 조회 서비스
     * @param id
     * @return 포인트 히스토리
     */
    public PointHistory[] searchPointhistory(long id) {
        return pointHistoryTable.selectAllByUserId(id).toArray(new PointHistory[0]);
    }

    /**
     * 포인트 충전 서비스
     * @param id
     * @param amount
     * @return 충전 후 남은 금액
     */
     public long chargePoints(long id, long amount,long time) {

         if(!isChargeable(id,amount)){
             throw new IllegalStateException("이미 최대 포인트 입니다.");
         }

         return updatePoint(id,amount,TransactionType.CHARGE,time);
     }

    /**
     * 포인트 사용 서비스
     * @param id
     * @param amount
     * @return 사용 후 남은 금액
     */
     public long usePoints(long id, long amount,long time) {

         if(!isAvailable(id,amount)){
             throw new IllegalStateException("포인트가 부족합니다.");
         }

        return updatePoint(id,-amount,TransactionType.USE,time);
     }

     public synchronized long updatePoint(long id,long amount,TransactionType type,long time) {
         userPointTable.insertOrUpdate(id,searchRestPoints(id) + amount);
         pointHistoryTable.insert(id,amount,type,time);

         return searchRestPoints(id);
     }

     public boolean isAvailable(long id,long usage) {
         return searchRestPoints(id) >= usage;
     }

     public boolean isChargeable(long id,long amount) {
         return searchRestPoints(id) + amount <= MAX_POINT;
     }

}
