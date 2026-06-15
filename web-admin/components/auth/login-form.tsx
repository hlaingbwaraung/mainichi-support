"use client";

import { signInWithEmailAndPassword } from "firebase/auth";
import { ArrowRight, LockKeyhole } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { firebaseAuth, isMockClient } from "@/lib/firebase/client";

export function LoginForm() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      let token = "mock";
      if (!isMockClient) {
        if (!firebaseAuth) throw new Error("Firebase Authが初期化されていません。");
        const credential = await signInWithEmailAndPassword(
          firebaseAuth,
          email,
          password
        );
        token = await credential.user.getIdToken();
      }

      const response = await fetch("/api/session", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token })
      });
      if (!response.ok) {
        const body = await response.json().catch(() => null);
        throw new Error(body?.message || "ログインできませんでした。");
      }
      router.replace("/dashboard");
      router.refresh();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "ログインに失敗しました。");
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-5">
      {!isMockClient ? (
        <>
          <div className="space-y-2">
            <label htmlFor="email" className="text-sm font-semibold">
              メールアドレス
            </label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>
          <div className="space-y-2">
            <label htmlFor="password" className="text-sm font-semibold">
              パスワード
            </label>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </div>
        </>
      ) : (
        <div className="rounded-md border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-100">
          モックモードです。Firebaseを設定するまでは、デモデータで全ページを確認できます。
        </div>
      )}

      {error ? (
        <p className="rounded-md bg-red-50 p-3 text-sm font-medium text-red-800 dark:bg-red-950 dark:text-red-200">
          {error}
        </p>
      ) : null}

      <Button type="submit" size="lg" className="w-full" disabled={loading}>
        <LockKeyhole className="h-5 w-5" />
        {loading
          ? "確認中..."
          : isMockClient
            ? "デモ管理画面を開く"
            : "管理画面へログイン"}
        {!loading ? <ArrowRight className="ml-auto h-5 w-5" /> : null}
      </Button>
    </form>
  );
}
