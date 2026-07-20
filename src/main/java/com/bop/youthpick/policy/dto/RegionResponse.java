package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.Region;

public record RegionResponse(String regionCode, String provinceName, String districtName) {
    public static RegionResponse from(Region region) {
        return new RegionResponse(region.getCode(), region.getSidoName(), region.getName());
    }
}
