# まいにちサポート Web管理画面

Androidアプリ「まいにちサポート」を運営するためのNext.js管理ダッシュボードです。

## 技術構成

- Next.js 16 / TypeScript
- Tailwind CSS / shadcn/ui互換コンポーネント
- Firebase Authentication
- Cloud Firestore
- Cloud Functions for Firebase
- Recharts

## 画面

- `/login` 管理者ログイン
- `/dashboard` ユーザー・課金・利用状況
- `/users` 検索、絞り込み、CSV、利用停止
- `/users/[id]` ユーザー詳細
- `/subscriptions` 課金状態
- `/analytics` 機能利用分析
- `/settings` メンテナンス、最低バージョン、強制更新、お知らせ
- `/admins` owner専用の管理者設定

## まずデモを確認

```bash
cd web-admin
cp .env.example .env.local
npm install
npm run dev
```

`.env.local`の`NEXT_PUBLIC_USE_MOCK_DATA=true`ではFirebaseなしで全画面を確認できます。

## Firebase設定

1. Firebaseプロジェクトを作成します。
2. Authenticationでメール/パスワードを有効にします。
3. Firestoreを作成します。
4. Firebase Web Appを追加し、`.env.local`へ`NEXT_PUBLIC_FIREBASE_*`を入力します。
5. サーバー用サービスアカウント値を`FIREBASE_*`へ入力します。
6. `NEXT_PUBLIC_USE_MOCK_DATA=false`へ変更します。

秘密鍵には`NEXT_PUBLIC_`を付けないでください。ブラウザへ公開されます。

## 最初のownerを登録

Firebase Authenticationで管理者ユーザーを作成し、そのUIDを使ってシードします。

```bash
export SEED_OWNER_UID="Firebase Auth UID"
npm run seed
```

シードせず手動で作成する場合、`admins/{UID}`へ次を保存します。

```json
{
  "email": "owner@example.com",
  "displayName": "オーナー",
  "role": "owner",
  "active": true,
  "createdAt": "Firestore Timestamp"
}
```

## Firebase CLI

```bash
npm install -g firebase-tools
firebase login
cp .firebaserc.example .firebaserc
```

`.firebaserc`のプロジェクトIDを変更してから、ルールとインデックスを配信します。

```bash
firebase deploy --only firestore
```

## Cloud Functions

```bash
cd functions
npm install
cd ..
firebase functions:secrets:set GOOGLE_PLAY_SERVICE_ACCOUNT_JSON
firebase functions:secrets:set ANDROID_PACKAGE_NAME
firebase deploy --only functions
```

`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`にはGoogle Play Consoleへ権限付与したサービスアカウントJSON、`ANDROID_PACKAGE_NAME`には本番applicationIdを設定します。

実装済みFunctions:

- `setUserDisabled`
- `updateAppSettings`
- `upsertAdminRole`
- `removeAdmin`
- `exportUsersCsv`
- `verifyGooglePlayPurchase`
- `aggregateDailyUsage`

## Android課金連携

Androidアプリは購入完了後と起動時に`verifyGooglePlayPurchase`を呼び、次を渡します。

```json
{
  "purchaseToken": "Google Play purchase token",
  "productId": "premium_monthly",
  "purchaseType": "subscription"
}
```

買い切りは`productId: "premium_lifetime"`、`purchaseType: "product"`です。

プレミアム判定はFunctionsがGoogle Play Developer APIで検証し、`users`と`subscriptions`へ保存します。Androidアプリ内のフラグだけを信用しないでください。

本番ではReal-time Developer Notificationsも追加し、更新、返金、期限切れをサーバー側で即時反映してください。

## 権限

- `owner`: 全操作、管理者追加・削除
- `admin`: ユーザー停止、設定更新、CSV出力
- `viewer`: 閲覧のみ

Firestoreの直接書き込みは原則拒否し、重要な変更はCloud Functionsだけが行います。

## 検証

```bash
npm run typecheck
npm run lint
npm run build
npm run functions:build
```
