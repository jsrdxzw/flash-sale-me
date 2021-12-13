package com.jsrdxzw.flashsale.app.model.converter;

import com.jsrdxzw.flashsale.app.model.dto.StockBucketDTO;
import com.jsrdxzw.flashsale.domain.model.Bucket;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author xuzhiwei
 * @date 2021/12/13 2:09 PM
 */
@Mapper
public interface FlashBucketAppMapping {
    FlashBucketAppMapping INSTANCE = Mappers.getMapper(FlashBucketAppMapping.class);

    List<StockBucketDTO> toDTOList(List<Bucket> buckets);
}
