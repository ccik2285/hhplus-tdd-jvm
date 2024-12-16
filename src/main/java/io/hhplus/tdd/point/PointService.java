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
    public long SearchRestPoints(long id) {
        return userPointTable.selectById(id).point();
    }

    /**
     * 포인트 이용내역 조회 서비스
     * @param id
     * @return
     */
    public PointHistory[] SearchPointhistory(long id) {
        return pointHistoryTable.selectAllByUserId(id).toArray(new PointHistory[0]);
    }

    /**
     * 포인트 충전 서비스
     * @param id
     * @param amount
     */
     public void ChargePoints(long id, long amount) {
         userPointTable.insertOrUpdate(id,SearchRestPoints(id) + amount);
         pointHistoryTable.insert(id,amount,TransactionType.CHARGE,System.currentTimeMillis());
     }

    /**
     * 포인트 사용 서비스
     * @param id
     * @param amount
     */
     public void UsePoints(long id, long amount) {
         userPointTable.insertOrUpdate(id,SearchRestPoints(id) - amount);
         pointHistoryTable.insert(id,amount,TransactionType.USE,System.currentTimeMillis());
     }

     public boolean IsAvailable(long id,long usage) {
         return SearchRestPoints(id) >= usage;
     }

     public boolean IsChargeable(long id,long amount) {
         return SearchRestPoints(id) + amount <= MAX_POINT;
     }

}
