# まいにちサポート

まいにちサポートは、70歳以上の方にも使いやすいシンプルなAndroidアプリです。大きな文字、見やすい配色、大きなボタンを使っています。

## 機能

- 歩数計: スマートフォンの歩数センサーで今日の歩数と7日間の歩数グラフを表示します。
- メモ: 端末にかんたんなメモを保存できます。
- 予定: 端末に予定のお知らせを保存できます。
- プレミアム会員: 月額500円、または3,000円の買い切りで広告を非表示にします。
- 大きな文字とボタンのシンプルなホーム画面。

## 課金と広告

Google Play Billing Library 9を使い、購入、購入復元、購入確認、定期購入管理への移動を実装しています。
Play Console側の設定は `PLAY_BILLING_SETUP.md` を参照してください。
実際にGoogle広告を配信するには、AdMobのアプリIDと広告ユニットIDを追加してください。

## 開き方

1. このフォルダをAndroid Studioで開きます。
2. Gradleの同期を待ちます。
3. `app` をエミュレーターまたはAndroidスマートフォンで実行します。

課金処理にはGoogle Play Billing Libraryを使用しています。

## Web管理画面

本番向けのNext.js管理画面は `web-admin/` にあります。

```sh
cd web-admin
cp .env.example .env.local
npm install
npm run dev
```

Firebase設定、Cloud Functions、Firestoreルール、Google Play課金検証の手順は `web-admin/README.md` を参照してください。
