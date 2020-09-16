package io.kindx.gateway.dto;

import io.kindx.dto.BaseDto;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Builder
@Data
@EqualsAndHashCode(callSuper = true)
public class PaginatedContentDto<T> extends BaseDto {
    private Long count;
    private Long totalCount;
    private String pageToken;
    private String nextPageToken;
    private Long nextPageTokenTtl;
    private List<T> data;
}
