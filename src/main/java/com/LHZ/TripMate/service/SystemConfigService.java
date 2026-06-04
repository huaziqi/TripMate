package com.LHZ.TripMate.service;

import com.LHZ.TripMate.entity.SystemConfig;
import java.util.List;

public interface SystemConfigService {
    List<SystemConfig> listAll();
    SystemConfig update(String key, String value);
}
