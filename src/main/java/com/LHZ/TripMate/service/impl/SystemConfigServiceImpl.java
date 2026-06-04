package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.entity.SystemConfig;
import com.LHZ.TripMate.repository.SystemConfigRepository;
import com.LHZ.TripMate.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public List<SystemConfig> listAll() {
        return systemConfigRepository.findAll();
    }

    @Override
    public SystemConfig update(String key, String value) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "配置项不存在: " + key));
        config.setConfigValue(value);
        return systemConfigRepository.save(config);
    }
}
