# 想定環境
## ゲームサーバ内の設定
### 接続用ユーザの設定
```bash
$ sudo useradd USER_NAME
$ sudo passwd USER_NAME
$ sudo usermod -aG GROUP_NAME USER_NAME

# ユーザの切り替え
$ sudo -u USER_NAME mkdir -m 700 /home/USER_NAME/.ssh
$ sudo -u USER_NAME ssh-keygent -t ed25519 -C /home/USER_NAME/.ssh/
$ sudo mv /home/USER_NAME/.ssh/id_ed25519.pub /home/USER_NAME/.ssh/authorized_keys

# 権限設定
$ ls -l ~/.ssh/authorized_keys
$ chmod 600 ~/.ssh/authorized_keys
$ ls -l ~/.ssh/authorized_keys

mkdir /opt/factorio-headless
sudo chown USER_NAME:GROUP_NAME -R factorio-headless
```
※その他、恐らくSELinuxは無効化 or 適切な設定が必要

### Factorioサーバ用のシェル等の配置
`/etc/systemd/system/`配下にFactorio headlessサーバのサービスファイルを配置する
※ファイル内容はlinux/etc/systemd/system/factorio.service参照

### systemd 権限付与
sudo visudo
```diff
+ USER_NAME ALL=(ALL)  NOPASSWD: /bin/systemctl start factorio, /bin/systemctl stop factorio
```

## DiscordAPIサーバの設定
### import proxmox self sign cert
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