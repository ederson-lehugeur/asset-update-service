package com.invest.application;

import com.invest.domain.events.UpdateAssetsEvent;

public interface UpdateAssetsUseCase {

    ProcessingResult execute(UpdateAssetsEvent event);
}
