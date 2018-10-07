package space.qyvlik.fiat.exchange.rates.data;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import space.qyvlik.fiat.exchange.rates.base.AbstractFiatExchangeRatesProvider;
import space.qyvlik.fiat.exchange.rates.base.BaseService;
import space.qyvlik.fiat.exchange.rates.entity.Account;
import space.qyvlik.fiat.exchange.rates.entity.FiatExchangeRate;
import space.qyvlik.fiat.exchange.rates.provider.ProviderFactory;

import java.util.List;
import java.util.Set;

@Service
public class FiatExchangeRateService extends BaseService {
    private static final String RATE_KEY_PREFIX = "r:";
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProviderFactory providerFactory;
    @Autowired
    private Environment environment;

    public void syncFiatExchangeRateList() {
        List<AbstractFiatExchangeRatesProvider> providerList =
                providerFactory.providerList();

        if (providerList == null || providerList.size() == 0) {
            logger.warn("syncFiatExchangeRateList return, providerList is empty");
            return;
        }

        for (AbstractFiatExchangeRatesProvider provider : providerList) {
            syncFiatExchangeRateList(provider);
        }
    }

    public void syncFiatExchangeRateList(AbstractFiatExchangeRatesProvider provider) {
        String key = environment.getProperty("provider." + provider.getProvider() + ".key");
        String plan = environment.getProperty("provider." + provider.getProvider() + ".plan");
        String username = environment.getProperty("provider." + provider.getProvider() + ".username");

        if (StringUtils.isAnyBlank(key, plan, username)) {
            logger.warn("syncFiatExchangeRateList fail : key or plan or username is empty, provider:{}",
                    provider.getProvider());
            return;
        }

        Account account = new Account();
        account.setPlan(plan);
        account.setAccessKey(key);
        account.setProvider(provider.getProvider());
        account.setUserName(username);

        List<FiatExchangeRate> rateList = null;

        try {
            rateList = provider.findExchangeRateList(account);
        } catch (Exception e) {
            logger.error("syncFiatExchangeRateList fail : provider:{}, error:{}",
                    provider.getProvider(), e.getMessage());
            return;
        }

        if (rateList == null || rateList.size() == 0) {
            logger.warn("syncFiatExchangeRateList fail : rateList is empty");
            return;
        }

        for (FiatExchangeRate fiatRate : rateList) {
            String rateKey = RATE_KEY_PREFIX + fiatRate.getProvider() + ":" + fiatRate.getBase() + "/" + fiatRate.getQuote();
            String jsonStr = JSON.toJSONString(fiatRate);
            redisTemplate.opsForValue().setIfAbsent(rateKey, jsonStr);
        }
    }

    public List<FiatExchangeRate> findFiatExchangeRateList() {
        Set<String> rateKeySet = redisTemplate.keys(RATE_KEY_PREFIX + "*");

        List<FiatExchangeRate> rateList = Lists.newArrayList();

        for (String rateKey : rateKeySet) {
            String jsonStr = redisTemplate.opsForValue().get(rateKey);
            if (StringUtils.isNotBlank(jsonStr) && jsonStr.startsWith("{")) {
                rateList.add(JSON.parseObject(jsonStr).toJavaObject(FiatExchangeRate.class));
            }
        }

        return rateList;
    }
}