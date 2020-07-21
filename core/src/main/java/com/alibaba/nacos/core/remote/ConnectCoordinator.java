/*
 * Copyright 1999-2020 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nacos.core.remote;

import com.alibaba.nacos.api.remote.connection.Connection;
import com.alibaba.nacos.core.utils.Loggers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ConnectCoordinator.
 *
 * @author liuzunfei
 * @version $Id: ConnectCoordinator.java, v 0.1 2020年07月14日 12:01 AM liuzunfei Exp $
 */

@Service
public class ConnectCoordinator implements ConnectionHeathyChecker {
    
    @Autowired
    private ClientConnectionEventListenerRegistry clientConnectionEventListenerRegistry;
    
    @Autowired
    ConnectionManager connectionManager;
    
    private ScheduledExecutorService executors = Executors.newScheduledThreadPool(1);
    
    private static final long EXPIRE_MILLSECOND = 10000L;
    
    /**
     * Start Task：Expel the connection which active Time expire.
     */
    @PostConstruct
    public void start() {
        
        // Start UnHeathy Conection Expel Task.
        executors.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    long currentStamp = System.currentTimeMillis();
                    Set<Map.Entry<String, Connection>> entries = connectionManager.connetions.entrySet();
        
                    List<String> toExpelCLients = new LinkedList<String>();
                    for (Map.Entry<String, Connection> entry : entries) {
                        Connection client = entry.getValue();
                        long lastActiveTimestamp = entry.getValue().getLastActiveTimestamp();
                        if (currentStamp - lastActiveTimestamp > EXPIRE_MILLSECOND) {
                            toExpelCLients.add(client.getConnectionId());
                        }
                    }
        
                    for (String expeledClient : toExpelCLients) {
                        connectionManager.unregister(expeledClient);
                        Loggers.GRPC.info("expire connection found ，success expel connectionid = {} ", expeledClient);
            
                    }
        
                } catch (Exception e) {
                    Loggers.GRPC.error("error occurs when heathy check... ", e);
                }
            }
        }, 500L, 3000L, TimeUnit.MILLISECONDS);
    }
    
}
