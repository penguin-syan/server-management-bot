#! /bin/bash

echo
echo "#########################"
echo "# アップデートの要否確認"
echo "#########################"

filename=/tmp/factorio_linux64_`date +%Y%m%d%H%M%S`.tar.xz
needUpdate=1

wget -O $filename https://factorio.com/get-download/stable/headless/linux64

if [ -e /var/tmp/factorio_linux64_hash.txt ]; then
   factorio_now_hash=`cat /var/tmp/factorio_linux64_hash.txt | cut -d ' ' -f 1`
   factorio_dl_hash=`md5sum $filename | cut -d ' ' -f 1`
   echo
   echo "##############################"
   echo "# サーバデータ ハッシュ値確認"
   echo "##############################"
   echo   now: $factorio_now_hash
   echo   dl : $factorio_dl_hash 

   if [ $factorio_now_hash = $factorio_dl_hash ]; then
      needUpdate=0
   fi
fi


if [ $needUpdate = 1 ]; then
   echo
   echo "##################################"
   echo "# Factorioサーバ アップデート開始"
   echo "##################################"
   tar Jxfv $filename -C /opt/factorio-headless/
   md5sum $filename > /var/tmp/factorio_linux64_hash.txt
fi

rm $filename


if [ $# = 1 ]; then
   echo
   echo "###################"
   echo "# セーブデータ配置"
   echo "###################"
   if [ ! -d /opt/factorio-headless/savedata ]; then
      mkdir -p /opt/factorio-headless/savedata
   fi
   if [ "${1:0:5}" == "s3://" ] ; then
      sudo -u factorio aws s3 cp $1 /opt/factorio-headless/savedata/savedata.zip
   else
      cp $1 /opt/factorio-headless/savedata/savedata.zip
   fi
fi

ls -l /opt/factorio-headless/savedata/savedata.zip

echo
echo "##########################"
echo "# Factorioサーバ 起動開始"
echo "##########################"
sudo systemctl start factorio
systemctl status --no-pager factorio
