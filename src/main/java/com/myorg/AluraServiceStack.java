package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class AluraServiceStack extends Stack {
    public AluraServiceStack(final Construct scope, final String id, final Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public AluraServiceStack(final Construct scope, final String id, final StackProps props, final Cluster cluster) {
        super(scope, id, props);

        Map<String, String> autenticacao = new HashMap<>();
        autenticacao.put("SPRING_DATASOURCE_URL", "jdbc:mysql://" + Fn.importValue("pedidos-db-endpoint" + ":3306/alurafood-pedidos?createDatabaseIfNotExist=true"));
        autenticacao.put("SPRING_DATASOURCE_USERNAME", "admin");
        autenticacao.put("SPRING_DATASOURCE_PASSWORD", Fn.importValue("pedidos-db-senha"));

        IRepository IRepository = Repository.fromRepositoryName(this, "repositorio", "img-pedidos-ms");

        // Create a load-balanced Fargate service and make it public
        ApplicationLoadBalancedFargateService aluraService = ApplicationLoadBalancedFargateService.Builder.create(this, "AluraService")
                .serviceName("alura-service-ola")
                .cluster(cluster)           // Required
                .cpu(512)                   // Default is 256
                .desiredCount(1)            // Quantidade de instâncias // Default is 1
                .listenerPort(8080)
                .assignPublicIp(true)       // Ter IP público (true/false)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .image(ContainerImage.fromEcrRepository(IRepository)) // imagem que será usada
                                .containerPort(8080)
                                .containerName("app_ola")
                                .environment(autenticacao)
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                        .logGroup(LogGroup.Builder.create(this, "PedidosMsLogGroup") // agrupa os logs de todas as instâncias, com o nome passado como segundo parâmetro
                                                .logGroupName("PedidosMsLog") // nome do log
                                                .removalPolicy(RemovalPolicy.DESTROY) // Caso esta stack seja deletado, os logs também serão
                                                .build())
                                        .streamPrefix("PedidosMS") // nome do prefixo
                                        .build()))
                                .build())
                .memoryLimitMiB(1024)       // Default is 512
                .publicLoadBalancer(true)   // Default is true
                .build();

        ScalableTaskCount scalableTarget = aluraService.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(1) // quantidade mínima de instâncias que queremos rodando
                .maxCapacity(3) // // quantidade máxima de instâncias que queremos rodando
                .build());
        scalableTarget.scaleOnCpuUtilization("CpuScaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(70) // porcentagem necessária de uso da CPU para que uma nova instância suba
                        .scaleInCooldown(Duration.minutes(3)) // tempo necessário para que uma nova instância suba
                        .scaleOutCooldown(Duration.minutes(2)) // tempo necessário para que uma nova instância seja destruída
                .build());
        scalableTarget.scaleOnMemoryUtilization("MemoryScaling", MemoryUtilizationScalingProps.builder()
                .targetUtilizationPercent(65) // porcentagem necessária de uso da memória para que uma nova instância suba
                        .scaleInCooldown(Duration.minutes(3)) // tempo necessário para que uma nova instância suba
                        .scaleOutCooldown(Duration.minutes(2)) // tempo necessário para que uma nova instância seja destruída
                .build());

    }
}
