#! /bin/bash

if [ $# -ne 1 ]; then
    echo 第1引数（セーブデータパス）が不足しています
    exit 1
fi

echo
echo "#######################"
echo "# Factorioサーバの停止"
echo "#######################"

sudo systemctl stop factorio
#systemctl status --no-pager factorio


# savedataが保存されたことを確認する 3秒間隔 1分間
for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20
do
    goodbyeCount=`grep -ic "Goodbye" /opt/factorio-headless/factorio/factorio-current.log`
    if [ $goodbyeCount -gt 0 ]; then
	break
    fi
    sleep 3
done


if [ $i -ge 20 ]; then
    echo "Secure savedata error"
    exit 1
fi

echo "#####################"
echo "# セーブデータの確保"
echo "#####################"
sudo -u factorio aws s3 cp /opt/factorio-headless/savedata/savedata.zip $1savedata_`date +%Y%m%d%H%M%S`.zip
#cp /opt/factorio-headless/savedata/savedata.zip $1

exit 0

