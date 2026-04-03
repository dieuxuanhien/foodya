package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.SystemParameterModel;
import com.foodya.backend.application.dto.SystemParameterPatchRequest;
import com.foodya.backend.application.dto.SystemParameterPutRequest;

import java.util.List;

public interface SystemParameterUseCase {

    List<SystemParameterModel> listAll();

    SystemParameterModel replace(String key, SystemParameterPutRequest request, String actorRole, String actorId);

    SystemParameterModel patch(String key, SystemParameterPatchRequest request, String actorRole, String actorId);
}
