package com.destroystokyo.paper.network;

import net.minecraft.server.NetworkManager;

class PaperStatusClient extends PaperNetworkClient implements com.destroystokyo.paper.network.StatusClient {

    PaperStatusClient(NetworkManager networkManager) {
        super(networkManager);
    }

}
