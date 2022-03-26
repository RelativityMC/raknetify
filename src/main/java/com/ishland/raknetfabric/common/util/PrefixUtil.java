package com.ishland.raknetfabric.common.util;

import com.ishland.raknetfabric.Constants;

public class PrefixUtil {

    public static Info getInfo(String address) {
        if (address.startsWith(Constants.RAKNET_PREFIX)) {
            return new Info(true, address.substring(Constants.RAKNET_PREFIX.length()), false);
        } else if (address.startsWith(Constants.RAKNET_LARGE_MTU_PREFIX)) {
            return new Info(true, address.substring(Constants.RAKNET_LARGE_MTU_PREFIX.length()), true);
        } else {
            return new Info(false, address, false);
        }
    }

    public record Info(boolean useRakNet, String stripped, boolean largeMTU) {
    }

}
