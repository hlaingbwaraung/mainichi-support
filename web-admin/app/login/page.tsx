import { Activity, ShieldCheck } from "lucide-react";
import { redirect } from "next/navigation";

import { LoginForm } from "@/components/auth/login-form";
import { getSessionAdmin } from "@/lib/auth";

export const metadata = { title: "ログイン" };

export default async function LoginPage() {
  const admin = await getSessionAdmin();
  if (admin) redirect("/dashboard");

  return (
    <main className="grid min-h-screen bg-[#171b1c] lg:grid-cols-[1.1fr_0.9fr]">
      <section className="hidden flex-col justify-between p-12 text-white lg:flex">
        <div className="flex items-center gap-3">
          <div className="grid h-12 w-12 place-items-center rounded-md bg-[#b9974f] text-[#171b1c]">
            <Activity className="h-7 w-7" />
          </div>
          <div>
            <p className="text-xl font-bold">まいにちサポート</p>
            <p className="text-sm text-white/55">管理者専用</p>
          </div>
        </div>
        <div className="max-w-xl">
          <p className="text-sm font-semibold text-[#d2b979]">運営を一か所に</p>
          <h1 className="mt-4 text-5xl font-bold leading-tight">
            利用状況を確認し、
            <br />
            安心してアプリを運営。
          </h1>
          <p className="mt-6 max-w-lg text-lg leading-8 text-white/65">
            ユーザー、課金、利用機能、アプリ設定を安全に管理できます。
          </p>
        </div>
        <div className="flex items-center gap-2 text-sm text-white/50">
          <ShieldCheck className="h-5 w-5" />
          Firebase Authと権限管理で保護
        </div>
      </section>

      <section className="flex items-center justify-center bg-background p-5 sm:p-10">
        <div className="w-full max-w-md">
          <div className="mb-8 lg:hidden">
            <div className="mb-4 grid h-11 w-11 place-items-center rounded-md bg-primary text-primary-foreground">
              <Activity className="h-6 w-6" />
            </div>
            <p className="font-bold">まいにちサポート</p>
          </div>
          <p className="text-sm font-semibold text-primary">ADMIN PORTAL</p>
          <h2 className="mt-2 text-3xl font-bold">管理画面にログイン</h2>
          <p className="mb-8 mt-3 text-sm leading-6 text-muted-foreground">
            登録済みの管理者アカウントを使用してください。
          </p>
          <LoginForm />
        </div>
      </section>
    </main>
  );
}
