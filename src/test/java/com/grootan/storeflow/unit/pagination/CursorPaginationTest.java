package com.grootan.storeflow.unit.pagination;

import com.grootan.storeflow.dto.CursorPageResponse;
import com.grootan.storeflow.dto.ProductDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CursorPaginationTest {

    @Test
    void cursor_hasMore_true_when_extra_record_exists() {
        List<ProductDto> list = List.of(
                new ProductDto(1L, "A", null, null, null, null, null,null,null),
                new ProductDto(2L, "B", null, null, null, null, null,null,null),
                new ProductDto(3L, "C", null, null, null, null, null,null,null)
        );

        CursorPageResponse<ProductDto> res =
                new CursorPageResponse<>(list.subList(0, 2), 2L, true, 2);

        assertTrue(res.isHasMore());
        assertEquals(2L, res.getNextCursor());
        assertEquals(2, res.getSize());
    }

    @Test
    void cursor_hasMore_false_when_no_extra() {
        List<ProductDto> list = List.of(
                new ProductDto(1L, "A", null, null, null, null, null,null,null)
        );

        CursorPageResponse<ProductDto> res =
                new CursorPageResponse<>(list, 1L, false, 1);

        assertFalse(res.isHasMore());
        assertEquals(1L, res.getNextCursor());
    }
}