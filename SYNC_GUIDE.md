# Mac と PC の同期方法

このプロジェクトは Git 管理済みです。

Mac と PC の両方で更新状態を合わせるには、GitHub などの同じリモートリポジトリを使います。

## 1. GitHub で空のリポジトリを作る

例:

```text
mainichi-support
```

Private repository 推奨です。

## 2. Mac 側で最初にアップロード

GitHub のリポジトリURLを使ってください。

```sh
git remote add origin https://github.com/YOUR_NAME/mainichi-support.git
git push -u origin main
```

## 3. PC 側で取得

PC の Desktop/projects などで:

```sh
git clone https://github.com/YOUR_NAME/mainichi-support.git
```

## 4. Mac で修正した後

```sh
git status
git add .
git commit -m "Update app"
git push
```

## 5. PC で最新にする

```sh
git pull
```

## 6. PC で修正した後

```sh
git status
git add .
git commit -m "Update from PC"
git push
```

## 7. Mac で最新にする

```sh
git pull
```

## 注意

- 同じファイルをMacとPCで同時に編集すると競合することがあります。
- 編集前に必ず `git pull`、編集後に `git push` してください。
- Androidの `build/` や `.gradle/` は同期しません。必要な時に各PCでビルドします。
