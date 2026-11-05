package co.com.bancolombia.model.gateway;

import co.com.bancolombia.model.ApplicationInfo;

public interface ApplicationInfoGateway {
    ApplicationInfo findByAppCodeAndDate(String appCode, String alarmDate);
}
