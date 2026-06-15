# Mac と PC の Codex 同期方法

このプロジェクトは Git で管理し、次の GitHub リポジトリを共通の保存場所として使います。

```text
https://github.com/hlaingbwaraung/mainichi-support.git
```

## PC の Codex で最初に開く

PC で Codex を開き、プロジェクトを置きたいフォルダで次を実行します。

```sh
git clone https://github.com/hlaingbwaraung/mainichi-support.git
cd mainichi-support
```

その後、Codex の「フォルダを開く」から `mainichi-support` フォルダを選びます。

## 作業を始める前

Mac と PC のどちらでも、最初に最新版を取得します。

```sh
git pull
```

## 作業を保存して共有する

```sh
git status
git add .
git commit -m "変更内容を書く"
git push
```

別のPCでは、次の `git pull` でその変更を取得できます。

## Codex への頼み方

作業開始時:

```text
最初に git pull して、最新版を確認してから修正して
```

作業終了時:

```text
変更を確認してコミットし、GitHubへpushして
```

## 注意

- 同じファイルをMacとPCで同時に編集しないでください。
- 編集前に `git pull`、編集後に `git push` を実行してください。
- `build/` と `.gradle/` は同期しません。各PCで必要な時にビルドします。
- GitHubへの初回接続時は、各PCでGitHubへのログインまたはSSH鍵の登録が必要です。
