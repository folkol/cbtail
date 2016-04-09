package com.folkol.cb;

import com.couchbase.client.core.ClusterFacade;
import com.couchbase.client.core.CouchbaseCore;
import com.couchbase.client.core.config.CouchbaseBucketConfig;
import com.couchbase.client.core.env.DefaultCoreEnvironment;
import com.couchbase.client.core.message.cluster.GetClusterConfigRequest;
import com.couchbase.client.core.message.cluster.GetClusterConfigResponse;
import com.couchbase.client.core.message.cluster.OpenBucketRequest;
import com.couchbase.client.core.message.cluster.SeedNodesRequest;
import com.couchbase.client.core.message.dcp.*;
import rx.Observable;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;

public class Tail {
    public static void main(String[] args) {
        String host = System.getProperty("host", "localhost");
        String bucket = System.getProperty("bucket", "default");
        String password = System.getProperty("password", "");
        String connectionName = "cb-tail-" + randomUUID().toString();

        System.err.printf(
            "=== Tailing Couchbase @ %s (%s:%s) ===%n",
            host,
            password,
            bucket);

        DefaultCoreEnvironment env =
            DefaultCoreEnvironment
                .builder()
                .dcpEnabled(true)
                .build();
        ClusterFacade core = new CouchbaseCore(env);

        core.send(new SeedNodesRequest(host)).toBlocking().single();
        core.send(new OpenBucketRequest(bucket, password)).toBlocking().single();
        core.send(new OpenConnectionRequest(connectionName, bucket)).toBlocking().single();

        GetClusterConfigRequest req = new GetClusterConfigRequest();
        core.<GetClusterConfigResponse>send(req)
            .map(GetClusterConfigResponse::config)
            .map(config -> config.bucketConfig(bucket))
            .cast(CouchbaseBucketConfig.class)
            .flatMap(config -> Observable.range(0, config.numberOfPartitions()))
            .map(partition -> new StreamRequestRequest(partition.shortValue(), bucket))
            .flatMap(core::<StreamRequestResponse>send)
            .flatMap(StreamRequestResponse::stream)
            .toBlocking()
            .forEach(dcpMessage -> {
                if (dcpMessage instanceof MutationMessage) {
                    MutationMessage msg = (MutationMessage) dcpMessage;
                    System.out.printf(
                        "Mutation: key=%s, cas=%d, ttl=%d, content=%s%n",
                        msg.key(),
                        msg.cas(),
                        msg.expiration(),
                        msg.content().toString(UTF_8).replaceAll("\n", ""));
                }
                if (dcpMessage instanceof RemoveMessage) {
                    RemoveMessage msg = (RemoveMessage) dcpMessage;
                    System.out.printf(
                        "Deletion: key=%s, cas=%d%n",
                        msg.key(),
                        msg.cas());
                }
            });
    }
}
