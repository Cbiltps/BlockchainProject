package com.example.gobang.fabric;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * @description:
 * @projectName:chess
 * @see:com.example.gobang.fabric
 * @author:yst
 * @createTime:2023/11/19 12:23 version:
 */
@Configuration
@AllArgsConstructor
@Slf4j
public class HyperLedgerFabricGatewayJavaConfig {

    @Bean
    public Gateway gateway() {
        // 主要读取java的配置文件
        Properties properties = new Properties();
        InputStream inputStream = Gateway.class.getResourceAsStream("/fabric.config.properties");
        try {
            properties.load(inputStream);
            String networkConfigPath = properties.getProperty("networkConfigPath");
            String certificatePath = properties.getProperty("certificatePath");
            X509Certificate certificate = readX509Certificate(Paths.get(certificatePath));
            String privateKeyPath = properties.getProperty("privateKeyPath");
            PrivateKey privateKey = getPrivateKey(Paths.get(privateKeyPath));
            Wallet wallet = Wallets.newInMemoryWallet();
            wallet.put("user1", Identities.newX509Identity("Org2MSP", certificate, privateKey));

            Gateway.Builder builder =
                    Gateway.createBuilder()
                            .identity(wallet, "user1")
                            .networkConfig(Paths.get(networkConfigPath));

            Gateway gateway = builder.connect();

            log.info("=========================================== connected fabric gateway {} ", gateway);

            return gateway;
        } catch (IOException | CertificateException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*获取合约的办法*/
    @Bean
    public Contract userContract(Gateway gateway) {
        // 获取mychannel的网络 也就是通道
        Network network = gateway.getNetwork("mychannel");
        // 获取contract的链码 名字为fabcar
        Contract contract = network.getContract("UserContract");
        return contract;
    }

    /*获取证书的办法*/
    private static X509Certificate readX509Certificate(final Path certificatePath)
            throws IOException, CertificateException {
        // 通过路径获取到文件 如果成功获取 返回身份识别证书的结果
        try (Reader certificateReader =
                     Files.newBufferedReader(certificatePath, StandardCharsets.UTF_8)) {
            return Identities.readX509Certificate(certificateReader);
        }
    }

    /*获取私钥的办法 */
    private static PrivateKey getPrivateKey(final Path privateKeyPath)
            throws IOException, InvalidKeyException {
        // 通过路径获取到文件 如果成功获取 返回身份识别私钥的结果
        try (Reader privateKeyReader =
                     Files.newBufferedReader(privateKeyPath, StandardCharsets.UTF_8)) {
            return Identities.readPrivateKey(privateKeyReader);
        }
    }
}
