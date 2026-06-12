# Google Play プレミアム商品の設定

アプリ側ではGoogle Play Billing Library 9を使い、月額プランと買い切りプランの購入、復元、購入確認を実装済みです。

## 固定ID

- パッケージ名: `com.example.seniorhelper`
- 定期購入の商品ID: `premium_monthly`
- 基本プランID: `monthly`
- 定期購入価格: 月額500円
- 買い切り商品ID: `premium_lifetime`
- 買い切り価格: 3,000円

これらのIDはPlay Consoleとアプリで完全に一致させてください。商品IDは作成後に変更できません。

## Play Console

1. Google Play Consoleでアプリを作成します。
2. 収益化のためのGoogle Paymentsプロファイルを設定します。
3. `収益化 > 商品 > 定期購入` で商品 `premium_monthly` を作成します。
4. 基本プラン `monthly` を作成し、自動更新、1か月、500円に設定します。
5. 定期購入と基本プランを有効にします。
6. `収益化 > 商品 > アプリ内アイテム` で商品 `premium_lifetime` を作成し、3,000円に設定します。
7. 買い切り商品を有効にします。
8. 署名済みAndroid App Bundleを内部テストへアップロードします。
9. テスターのGoogleアカウントを内部テストとライセンステストへ登録します。
10. テスターがPlayストアの内部テスト用リンクからアプリをインストールし、両方の購入を確認します。

USBやWebリンクから直接入れたAPKでは、Play Consoleの商品取得や本番課金を正しく試せない場合があります。課金テストはPlayストア配信版で行ってください。

## 公開前

- プライバシーポリシー、利用規約、定期購入の説明をストア掲載情報へ追加します。
- 購入トークンのサーバー検証とReal-time Developer Notificationsを追加すると、不正利用、返金、期限切れへの追従が強くなります。
- Play Consoleで価格と税、販売地域、支払いプロファイルを最終確認します。
