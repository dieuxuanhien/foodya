package com.foodya.backend.application.ports.out;

import java.util.Map;

public interface SupabaseConfigPort {

    Map<String, String> getSupabaseConfigSummary();
}