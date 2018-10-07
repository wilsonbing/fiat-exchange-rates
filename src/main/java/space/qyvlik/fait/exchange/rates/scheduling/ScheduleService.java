package space.qyvlik.fait.exchange.rates.scheduling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import space.qyvlik.fait.exchange.rates.base.BaseService;
import space.qyvlik.fait.exchange.rates.data.FiatExchangeRateService;

import javax.annotation.PostConstruct;

@Profile("scheduling")
@Service
public class ScheduleService extends BaseService {

    @Value("${startup.initRate}")
    private Boolean startupInit;

    @Autowired
    private FiatExchangeRateService fiatExchangeRateService;

    @PostConstruct
    public void initExchangeRateData() {
        if (startupInit == null || !startupInit) {
            return;
        }
        fiatExchangeRateService.syncFiatExchangeRateList();
    }

    // 1 hour update the fiat exchange rate
    @Scheduled(cron = "0 0 0/1 * * ? ")
    public void executeExchangeRate() {
        fiatExchangeRateService.syncFiatExchangeRateList();
    }

}
