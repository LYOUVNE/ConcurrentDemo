package com.mine.server.cluster;

import com.mine.server.web.AbstractRequest;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PeersReplicateBatch {
    private List<AbstractRequest> requests = new ArrayList<>();

    public void add(AbstractRequest request) {
        requests.add(request);
    }
}
