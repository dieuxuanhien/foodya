package com.foodya.backend.application.port.out;

import java.util.Map;

public interface SupabaseConfigPort {

    Map<String, String> getSupabaseConfigSummary();
}