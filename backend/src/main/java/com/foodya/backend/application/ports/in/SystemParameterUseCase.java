package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.SystemParameterData;
import com.foodya.backend.application.dto.SystemParameterPatchRequest;
import com.foodya.backend.application.dto.SystemParameterPutRequest;

import java.util.List;

public interface SystemParameterUseCase {

    List<SystemParameterData> listAll();

    SystemParameterData replace(String key, SystemParameterPutRequest request, String actorRole, String actorId);

    SystemParameterData patch(String key, SystemParameterPatchRequest request, String actorRole, String actorId);
}
