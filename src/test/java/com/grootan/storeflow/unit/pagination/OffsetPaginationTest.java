package com.grootan.storeflow.unit.pagination;

import com.grootan.storeflow.dto.OffsetPageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OffsetPaginationTest {

    @Test
    void totalPages_zero_when_empty() {
        var page = new PageImpl<>(List.of());
        var res = new OffsetPageResponse<>(page);

        assertEquals(0, res.getTotalElements());
    }

    @Test
    void totalPages_exact_multiple() {
        var page = new PageImpl<>(List.of(1,2,3,4));
        var res = new OffsetPageResponse<>(page);

        assertTrue(res.getTotalElements() >= 4);
    }
}