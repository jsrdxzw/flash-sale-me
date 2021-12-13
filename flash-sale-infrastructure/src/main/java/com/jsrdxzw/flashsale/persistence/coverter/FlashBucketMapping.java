package com.jsrdxzw.flashsale.persistence.coverter;

import com.jsrdxzw.flashsale.domain.model.Bucket;
import com.jsrdxzw.flashsale.persistence.model.BucketDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author xuzhiwei
 * @date 2021/12/13 11:53 AM
 */
@Mapper
public interface FlashBucketMapping {
    FlashBucketMapping INSTANCE = Mappers.getMapper(FlashBucketMapping.class);

    List<BucketDO> toBucketDOList(List<Bucket> buckets);

    List<Bucket> toBucketList(List<BucketDO> bucketDOS);
}
