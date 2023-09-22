package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.Collections;

public class AluraRdsStack extends Stack {
    public AluraRdsStack(final Construct scope, final String id, final Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public AluraRdsStack(final Construct scope, final String id, final StackProps props, final Vpc vpc) {
        super(scope, id, props);

        CfnParameter senha = CfnParameter.Builder.create(this, "senha")
                .type("String")
                .description("Senha do database pedidos-ms")
                .build();

        ISecurityGroup iSecurityGroup = SecurityGroup.fromSecurityGroupId( this, id, vpc.getVpcDefaultSecurityGroup()); // Indicamos aqui que deverá ser pego as conigurações
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));                                                   // padrão de segurança da Vpc
//  As regras de entrada aqui são:
//  qualquer Ip dentro das aplicações que estiverem dentro da Vpc
//  e tendo acesso a porta 3306

        DatabaseInstance database = DatabaseInstance.Builder
                .create(this, "Rds-pedidos")
                .instanceIdentifier("alura-aws-pedido-db") // Identificação da instância
                .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder() // tipo do banco
                        .version(MysqlEngineVersion.VER_8_0) // versão
                        .build()))
                .vpc(vpc) // Qual Vpc será usada            // credenciais
                .credentials(Credentials.fromUsername("admin", // Fixamos que será o usuário Admin
                CredentialsFromUsernameOptions.builder()
                        .password(SecretValue.unsafePlainText(senha.getValueAsString())) // Informamos que a senha será por texto, sendo passada por meio da variável "senha" (CfnParameter)
                        .build())) //           Tipo da instância       // Tamanho
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .multiAz(false) // Zona de disponibilidade
                .allocatedStorage(10) // Tamanho de armazenamento
                .securityGroups(Collections.singletonList(iSecurityGroup)) // Configurações dos grupos de segurança - Aqui será pego todos os Security Group da Vpc
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(vpc.getPrivateSubnets()) // Subredes - Aqui são pegos da Vpc
                        .build())
                .build();

        /* CLOUDFORMATION OUTPUT */

//      Exposição do endpoint da database
        CfnOutput.Builder.create(this,"pedidos-db-endpoint")
                .exportName("pedidos-db-endpoint")
                .value(database.getDbInstanceEndpointAddress())
                .build();

//      Exposição do endpoint da senha
        CfnOutput.Builder.create(this,"pedidos-db-senha")
                .exportName("pedidos-db-senha")
                .value(senha.getValueAsString())
                .build();
    }
}
