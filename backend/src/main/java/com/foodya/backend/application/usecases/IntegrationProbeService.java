package com.foodya.backend.application.usecases;

import com.foodya.backend.application.ports.out.FirebaseConfigPort;
import com.foodya.backend.application.ports.out.SupabaseConfigPort;

import java.util.Map;

public class IntegrationProbeService {

    private final FirebaseConfigPort firebaseConfigPort;
    private final SupabaseConfigPort supabaseConfigPort;

    public IntegrationProbeService(FirebaseConfigPort firebaseConfigPort,
                                   SupabaseConfigPort supabaseConfigPort) {
        this.firebaseConfigPort = firebaseConfigPort;
        this.supabaseConfigPort = supabaseConfigPort;
    }

    public Map<String, String> firebaseWebConfig() {
        return firebaseConfigPort.getFirebaseWebConfig();
    }

    public Map<String, String> supabaseConfigSummary() {
        return supabaseConfigPort.getSupabaseConfigSummary();
    }
}