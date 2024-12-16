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
     * @return
     */
    public long searchRestPoints(long id) {
        return userPointTable.selectById(id).point();
    }

    /**
     * 포인트 이용내역 조회 서비스
     * @param id
     * @return
     */
    public PointHistory[] searchPointhistory(long id) {
        return pointHistoryTable.selectAllByUserId(id).toArray(new PointHistory[0]);
    }

    /**
     * 포인트 충전 서비스
     * @param id
     * @param amount
     */
     public long chargePoints(long id, long amount) {

         if(!isChargeable(id,amount)){
             throw new IllegalStateException("이미 최대 포인트 입니다.");
         }

         userPointTable.insertOrUpdate(id,searchRestPoints(id) + amount);
         pointHistoryTable.insert(id,amount,TransactionType.CHARGE,System.currentTimeMillis());

         return searchRestPoints(id);
     }

    /**
     * 포인트 사용 서비스
     * @param id
     * @param amount
     */
     public long usePoints(long id, long amount) {

         if(!isAvailable(id,amount)){
             throw new IllegalStateException("포인트가 부족합니다.");
         }

         userPointTable.insertOrUpdate(id,searchRestPoints(id) - amount);
         pointHistoryTable.insert(id,amount,TransactionType.USE,System.currentTimeMillis());

         return searchRestPoints(id);
     }

     public boolean isAvailable(long id,long usage) {
         return searchRestPoints(id) >= usage;
     }

     public boolean isChargeable(long id,long amount) {
         return searchRestPoints(id) + amount <= MAX_POINT;
     }

}
