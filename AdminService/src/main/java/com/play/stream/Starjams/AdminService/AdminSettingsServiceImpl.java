package com.play.stream.Starjams.AdminService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.CassandraTemplate;

public class AdminSettingsServiceImpl {

    public AdminSettingsServiceImpl() {
    }

    @Autowired
    private CassandraTemplate cassandraTemplate;

    public AdminSettingsModel createSettings(AdminSettingsModel settings) {
        return cassandraTemplate.insert(settings);
    }
}
