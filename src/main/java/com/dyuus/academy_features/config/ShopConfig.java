package com.dyuus.academy_features.config;

import java.util.ArrayList;
import java.util.List;

public class ShopConfig {
    public String shopId;           // Unique identifier (filename without .json)
    public String displayName;      // Title shown in the GUI
    public List<ShopItem> items = new ArrayList<>();

    public ShopConfig() {}

    public ShopConfig(String shopId, String displayName) {
        this.shopId = shopId;
        this.displayName = displayName;
    }
}