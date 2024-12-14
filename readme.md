# import proxmox self sign cert
1. 証明書の抽出
    * `/etc/pve/nodes/<node_name>/pveproxy-ssl.pem` 抽出するか、ブラウザで管理コンソールにアクセスしてエクスポートする
1. 証明書のインポート
1. Javaのキーストアに自己証明書をインポートする
    Javaはキーストア（cacerts）で信頼できる証明書を管理します。これにProxmoxの自己証明書を追加します。

    cacerts の場所を確認 通常、Javaのcacertsファイルは以下にあります:

    Linux: /lib/security/cacerts または /etc/ssl/certs/java/cacerts
    Windows: <JAVA_HOME>\lib\security\cacerts
    keytool コマンドでインポート 以下のコマンドを実行します:

    bash
    ```
    keytool -importcert \
        -file ~/proxmox-cert.pem \
        -keystore <path_to_cacerts> \
        -alias proxmox-cert
    
    -file: エクスポートしたProxmoxの自己証明書のパス
    -keystore: Javaのcacertsファイルのパス
    -alias: 証明書のエイリアス（任意の名前）
    ```
    コマンドを実行すると、証明書を信頼するか尋ねられるので、「yes」と入力します。

1. インポートを確認 以下のコマンドで登録された証明書を確認できます:
keytool -list -keystore <path_to_cacerts>