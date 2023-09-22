package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.constructs.Construct;

public class AluraServiceStack extends Stack {
    public AluraServiceStack(final Construct scope, final String id, final Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public AluraServiceStack(final Construct scope, final String id, final StackProps props, final Cluster cluster) {
        super(scope, id, props);

        // Create a load-balanced Fargate service and make it public
        ApplicationLoadBalancedFargateService.Builder.create(this, "AluraService")
                .serviceName("alura-service-ola")
                .cluster(cluster)           // Required
                .cpu(512)                   // Default is 256
                .desiredCount(1)            // Quantidade de instâncias // Default is 1
                .listenerPort(8080)
                .assignPublicIp(true)       // Ter IP público (true/false)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .image(ContainerImage.fromRegistry("jacquelineoliveira/ola:1.0")) // imagem que será usada
                                .containerPort(8080)
                                .containerName("app_ola")
                                .build())
                .memoryLimitMiB(1024)       // Default is 512
                .publicLoadBalancer(true)   // Default is true
                .build();
    }
}
