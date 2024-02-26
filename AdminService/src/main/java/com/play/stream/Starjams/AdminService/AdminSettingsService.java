package com.play.stream.Starjams.AdminService;

import java.util.List;
import java.util.UUID;

public interface AdminSettingsService {

    AdminSettingsModel createSettings(AdminSettingsModel settings);

    AdminSettingsModel getSettingsById(UUID id);
    List<AdminSettingsModel> getAllSettings();
    AdminSettingsModel updateSettings(UUID id, AdminSettingsModel settings);
    void deleteSettingsById(UUID id);

    // Business-specific methods
    void toggleIOSOnline(UUID id, boolean status);
    void updateLandingPageFeatures(UUID id, boolean carousel, int tracks, int videos, boolean searchBox);
    void refreshMetaData(UUID id, String description, String keywords, String author);

}
