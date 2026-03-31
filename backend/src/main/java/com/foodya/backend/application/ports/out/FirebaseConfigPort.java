package com.foodya.backend.application.ports.out;

import java.util.Map;

public interface FirebaseConfigPort {

    Map<String, String> getFirebaseWebConfig();
}