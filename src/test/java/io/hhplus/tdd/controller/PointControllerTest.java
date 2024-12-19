package io.hhplus.tdd.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.point.PointController;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static io.hhplus.tdd.point.TransactionType.CHARGE;
import static io.hhplus.tdd.point.TransactionType.USE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@WebMvcTest(controllers = PointController.class)
public class PointControllerTest {

    @Autowired
    private PointController pointController;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @Autowired
    private ObjectMapper objectMapper;



    private final long time = System.currentTimeMillis();


    @Test
    public void 포인트조회() throws Exception {
        long userId = 1L;
        long points = 1000L;

        when(pointService.searchRestPoints(userId)).thenReturn(points);

        mockMvc.perform(MockMvcRequestBuilders.get("/point/" + userId))
                .andExpect(MockMvcResultMatchers.status().isOk())  // HTTP 상태 코드 200 OK
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(userId))  // 응답 본문에 id 값 확인
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(points));  // 응답 본문에 포인트 값 확인
    }

    @Test
    public void 포인트이용내역조회() throws Exception {
        long userId = 1L;

        List<PointHistory> PointHistories = List.of(
                new PointHistory(1L, userId, 100L, CHARGE,time),
                new PointHistory(2L, userId, 300L, USE,time)
        );

        when(pointService.searchPointhistory(userId)).thenReturn(PointHistories.toArray(new PointHistory[0]));

        mockMvc.perform(MockMvcRequestBuilders.get("/point/" + userId + "/histories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].amount").value(100L))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].type").value("CHARGE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(2L))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].amount").value(300L))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].type").value("USE"));


    }

    @Test
    void 포인트충전() throws Exception {
        // given
        long userId = 1L;

        long amount = 1000L;

        when(pointService.chargePoints(eq(userId),eq(amount),anyLong())).thenReturn(amount);

        // then
        mockMvc.perform(MockMvcRequestBuilders.patch("/point/" + userId + "/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(amount))
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(amount));
    }


    @Test
    void 포인트사용() throws Exception {
        long userId = 1L;
        long amount = 30L;

        // when
        when(pointService.usePoints(eq(userId),eq(amount),anyLong())).thenReturn(amount);
        // then
        mockMvc.perform(MockMvcRequestBuilders.patch("/point/" + userId + "/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(amount))
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(userId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.point").value(amount));
    }



}
