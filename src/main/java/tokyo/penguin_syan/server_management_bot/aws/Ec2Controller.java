package tokyo.penguin_syan.server_management_bot.aws;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStatus;
import software.amazon.awssdk.services.ec2.model.RebootInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.model.SummaryStatus;
import tokyo.penguin_syan.server_management_bot.PropertiesReader;

public class Ec2Controller {
    private Region region;
    private Ec2Client ec2Client;

    private static PropertiesReader propertiesReader;
    private static Logger logger = LogManager.getLogger();

    /**
     * Ec2Controllerのコンストラクタ
     */
    public Ec2Controller() {
        propertiesReader = new PropertiesReader();
        region = Region.of(propertiesReader.getProperty("awsRegion"));

        if ("1".equals(propertiesReader.getProperty("awsEc2Flag"))) {
            ec2Client = Ec2Client.builder().region(region)
                    .credentialsProvider(InstanceProfileCredentialsProvider.create()).build();
        } else {
            ec2Client = Ec2Client.builder().region(region)
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create()).build();
        }
    }

    /**
     * インスタンスを起動する
     * 
     * @param instanceId 起動するインスタンスのインスタンスID
     * @throws Exception
     */
    public void startInstance(String instanceId) throws Exception {
        logger.info("AWS#startInstance start");
        int instanceStatus = instanceStatus(instanceId);

        if (instanceStatus <= 16) {
            logger.info("AWS#startInstance canceled (pending or running)");
            throw new Ec2ControlException("既に起動済みです");
        } else if (instanceStatus == 48) {
            logger.warn("AWS#startInstance canceled (terminated)");
            throw new Ec2ControlException("インスタンスが削除されています");
        }

        try {
            StartInstancesRequest startRequest =
                    StartInstancesRequest.builder().instanceIds(instanceId).build();
            ec2Client.startInstances(startRequest);
        } catch (Exception e) {
            logger.error("AWS#startInstance abort");
            throw e;
        }

        logger.info(String.format("AWS#startInstance end [Instance-id:%s]", instanceId));
    }

    /**
     * インスタンスを停止する
     * 
     * @param instanceId 停止するインスタンスのインスタンスID
     * @throws Exception
     */
    public void stopInstance(String instanceId) throws Exception {
        logger.info("AWS#stopInstance start");
        int instanceStatus = instanceStatus(instanceId);

        if (instanceStatus >= 32) {
            logger.info("AWS#stopInstance canceled (shutting-down or stopped)");
            throw new Ec2ControlException("既に停止済みです");
        }

        try {
            StopInstancesRequest stopRequest =
                    StopInstancesRequest.builder().instanceIds(instanceId).build();
            ec2Client.stopInstances(stopRequest);
        } catch (Exception e) {
            logger.error("AWS#stopInstance abort");
            throw e;
        }

        logger.info(String.format("AWS#endInstance end [Instance-id:%s]", instanceId));
    }

    /**
     * インスタンスを再起動する
     * 
     * @param instanceId 停止するインスタンスのインスタンスID
     * @throws Exception
     */
    public void rebootInstance(String instanceId) throws Exception {
        logger.info("AWS#rebootInstance start");
        int instanceStatus = instanceStatus(instanceId);

        if (instanceStatus != 16) {
            logger.info("AWS#stopInstance canceled (not running)");
            throw new Ec2ControlException("インスタンスが実行中ではありません");
        }

        try {
            RebootInstancesRequest rebootRequest =
                    RebootInstancesRequest.builder().instanceIds(instanceId).build();
            ec2Client.rebootInstances(rebootRequest);
        } catch (Exception e) {
            logger.error("AWS#rebootInstance abort");
            throw e;
        }

        logger.info(String.format("AWS#rebootInstance end [Instance-id:%s]", instanceId));
    }

    /**
     * 指定したインスタンスのステータスコードを取得する
     * 
     * @param instanceId ステータスを取得するインスタンスのID
     * @return インスタンスのステータスコード （0:pending, 16:running, 32:shutting-down, 48:terminated, 64:stopping,
     *         80:stopped）
     */
    public int instanceStatus(String instanceId) throws Exception {
        logger.info("AWS#instanceStatus start");

        DescribeInstancesRequest request =
                DescribeInstancesRequest.builder().instanceIds(instanceId).build();
        DescribeInstancesResponse response = ec2Client.describeInstances(request);

        // インスタンスIDを指定しており検索結果も1つのみのため、Listで0番目を指定して設定値を取得する。
        Instance instanceInfo = response.reservations().get(0).instances().get(0);
        int instanceStatusCode = instanceInfo.state().code();

        logger.info("AWS#instanceStatus end");
        return instanceStatusCode;
    }


    /**
     * 指定したインスタンスのステータスチェックを確認する
     * 
     * @param instanceId
     * @return
     */
    public boolean isAllInstanceCheckPassed(String instanceId) {
        logger.info("AWS#instanceCheckResult start");

        DescribeInstanceStatusRequest request2 =
                DescribeInstanceStatusRequest.builder().instanceIds(instanceId).build();
        DescribeInstanceStatusResponse response2 = ec2Client.describeInstanceStatus(request2);

        InstanceStatus instanceStatus = response2.instanceStatuses().get(0);

        if (instanceStatus.systemStatus().status() != SummaryStatus.OK) {
            return false;
        }

        if (instanceStatus.attachedEbsStatus().status() != SummaryStatus.OK) {
            return false;
        }

        if (instanceStatus.instanceStatus().status() != SummaryStatus.OK) {
            return false;
        }

        return true;
    }


    /**
     * 指定したインスタンスのパブリックIP（IPv4）を取得する
     * 
     * @param instanceId パブリックIPを取得するインスタンスのID
     * @return 指定したインスタンスのパブリックIP（IPv4）
     */
    public String instancePublicIpv4(String instanceId) {
        logger.info("AWS#instancePublicIpv4 start");

        DescribeInstancesRequest request =
                DescribeInstancesRequest.builder().instanceIds(instanceId).build();
        DescribeInstancesResponse response = ec2Client.describeInstances(request);

        // インスタンスIDを指定しており検索結果も1つのみのため、Listで0番目を指定して設定値を取得する。
        Instance instanceInfo = response.reservations().get(0).instances().get(0);
        String instancePublicIpv4 = instanceInfo.publicIpAddress();

        logger.info("AWS#instancePublicIpv4 end");
        return instancePublicIpv4;
    }

    /**
     * 指定したインスタンスのパブリックIP（IPv6）を取得する
     * 
     * @param instanceId パブリックIPを取得するインスタンスのID
     * @return 指定したインスタンスのパブリックIP（IPv6）
     */
    public String instancePublicIpv6(String instanceId) {
        logger.info("AWS#instancePublicIpv6 start");

        DescribeInstancesRequest request =
                DescribeInstancesRequest.builder().instanceIds(instanceId).build();
        DescribeInstancesResponse response = ec2Client.describeInstances(request);

        // インスタンスIDを指定しており検索結果も1つのみのため、Listで0番目を指定して設定値を取得する。
        Instance instanceInfo = response.reservations().get(0).instances().get(0);
        String instancePublicIpv6 = instanceInfo.ipv6Address();

        logger.info("AWS#instancePublicIpv6 end");
        return instancePublicIpv6;
    }

}