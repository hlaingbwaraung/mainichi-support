"use client";

import {
  Activity,
  BadgeJapaneseYen,
  BarChart3,
  ChevronRight,
  LayoutDashboard,
  LogOut,
  Menu,
  Settings,
  ShieldCheck,
  Users,
  X
} from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";

import { ThemeToggle } from "@/components/theme-toggle";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { AdminProfile } from "@/lib/types";
import { cn } from "@/lib/utils";

const navigation = [
  { href: "/dashboard", label: "概要", icon: LayoutDashboard },
  { href: "/users", label: "ユーザー", icon: Users },
  { href: "/subscriptions", label: "サブスクリプション", icon: BadgeJapaneseYen },
  { href: "/analytics", label: "機能分析", icon: BarChart3 },
  { href: "/settings", label: "アプリ設定", icon: Settings },
  { href: "/admins", label: "管理者", icon: ShieldCheck, ownerOnly: true }
];

const roleLabels = {
  owner: "オーナー",
  admin: "管理者",
  viewer: "閲覧者"
};

export function AppShell({
  admin,
  children
}: {
  admin: AdminProfile;
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();
  const [mobileOpen, setMobileOpen] = useState(false);

  async function logout() {
    await fetch("/api/session", { method: "DELETE" });
    router.replace("/login");
    router.refresh();
  }

  const sidebar = (
    <div className="flex h-full flex-col bg-[#171b1c] text-white">
      <div className="flex h-20 items-center gap-3 border-b border-white/10 px-5">
        <div className="grid h-10 w-10 place-items-center rounded-md bg-[#b9974f] text-[#171b1c]">
          <Activity className="h-6 w-6" />
        </div>
        <div>
          <p className="font-bold">まいにちサポート</p>
          <p className="text-xs text-white/55">運営ダッシュボード</p>
        </div>
      </div>

      <nav className="flex-1 space-y-1 p-3">
        {navigation
          .filter((item) => !item.ownerOnly || admin.role === "owner")
          .map((item) => {
            const active =
              pathname === item.href ||
              (item.href !== "/dashboard" && pathname.startsWith(item.href));
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setMobileOpen(false)}
                className={cn(
                  "flex h-12 items-center gap-3 rounded-md px-3 text-sm font-semibold text-white/70 transition-colors hover:bg-white/8 hover:text-white",
                  active && "bg-white/10 text-white"
                )}
              >
                <Icon className="h-5 w-5" />
                <span>{item.label}</span>
                {active ? <ChevronRight className="ml-auto h-4 w-4" /> : null}
              </Link>
            );
          })}
      </nav>

      <div className="border-t border-white/10 p-4">
        <p className="truncate text-sm font-semibold">{admin.displayName}</p>
        <div className="mt-1 flex items-center justify-between gap-2">
          <Badge className="border-white/10 bg-white/10 text-white">
            {roleLabels[admin.role]}
          </Badge>
          <Button
            variant="ghost"
            size="icon"
            className="text-white/65 hover:bg-white/10 hover:text-white"
            onClick={logout}
            title="ログアウト"
          >
            <LogOut className="h-5 w-5" />
          </Button>
        </div>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-background">
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-64 lg:block">
        {sidebar}
      </aside>

      {mobileOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            className="absolute inset-0 bg-black/55"
            aria-label="メニューを閉じる"
            onClick={() => setMobileOpen(false)}
          />
          <aside className="relative h-full w-72 shadow-2xl">
            <Button
              variant="ghost"
              size="icon"
              className="absolute right-2 top-2 z-10 text-white"
              onClick={() => setMobileOpen(false)}
            >
              <X className="h-5 w-5" />
            </Button>
            {sidebar}
          </aside>
        </div>
      ) : null}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b bg-background/95 px-4 backdrop-blur md:px-6">
          <div className="flex items-center gap-3">
            <Button
              variant="ghost"
              size="icon"
              className="lg:hidden"
              onClick={() => setMobileOpen(true)}
              title="メニュー"
            >
              <Menu className="h-5 w-5" />
            </Button>
            <div>
              <p className="text-sm font-bold">管理画面</p>
              <p className="hidden text-xs text-muted-foreground sm:block">
                利用状況と運営設定
              </p>
            </div>
          </div>
          <ThemeToggle />
        </header>

        <main className="mx-auto w-full max-w-[1500px] p-4 md:p-6 lg:p-8">
          {children}
        </main>
      </div>
    </div>
  );
}
