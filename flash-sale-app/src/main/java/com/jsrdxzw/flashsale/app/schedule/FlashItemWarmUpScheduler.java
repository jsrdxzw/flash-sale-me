package com.jsrdxzw.flashsale.app.schedule;

import com.jsrdxzw.flashsale.app.service.stock.ItemStockCacheService;
import com.jsrdxzw.flashsale.config.annotion.BetaTrace;
import com.jsrdxzw.flashsale.domain.model.PageResult;
import com.jsrdxzw.flashsale.domain.model.PagesQueryCondition;
import com.jsrdxzw.flashsale.domain.model.entity.FlashItem;
import com.jsrdxzw.flashsale.domain.service.FlashItemDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author xuzhiwei
 * @date 2021/12/4 5:22 PM
 */
@Slf4j
@Component
public class FlashItemWarmUpScheduler {
    @Autowired
    private ItemStockCacheService itemStockCacheService;

    @Autowired
    private FlashItemDomainService flashItemDomainService;

    /**
     * 预热库存的调度，因为加了分布式锁，所以可以保证安全
     * 当然也可以使用分布式任务调度
     */
    @Scheduled(cron = "*/5 * * * * ?")
    @BetaTrace
    public void warmUpFlashItemTask() {
        log.info("warmUpFlashItemTask|秒杀品预热调度");
        PagesQueryCondition pagesQueryCondition = new PagesQueryCondition();
        pagesQueryCondition.setStockWarmUp(0);
        PageResult<FlashItem> pageResult = flashItemDomainService.getFlashItems(pagesQueryCondition);
        pageResult.getData().forEach(flashItem -> {
            boolean initSuccess = itemStockCacheService.alignItemStocks(flashItem.getId());
            if (!initSuccess) {
                log.info("warmUpFlashItemTask|秒杀品库存已经初始化预热失败:{}", flashItem.getId());
                return;
            }
            flashItem.setStockWarmUp(1);
            flashItemDomainService.publishFlashItem(flashItem);
            log.info("warmUpFlashItemTask|秒杀品库存已经初始化预热成功:{}", flashItem.getId());
        });
    }
}
