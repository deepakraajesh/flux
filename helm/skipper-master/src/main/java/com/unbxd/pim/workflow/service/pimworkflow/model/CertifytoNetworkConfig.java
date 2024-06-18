package com.unbxd.pim.workflow.service.pimworkflow.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class CertifytoNetworkConfig {
    private List<String> tags;

    public CertifytoNetworkConfig() {
        tags = new ArrayList<>();
    }
}

